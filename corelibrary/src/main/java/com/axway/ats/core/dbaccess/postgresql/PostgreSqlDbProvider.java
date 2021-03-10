/*
 * Copyright 2017-2020 Axway Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.core.dbaccess.postgresql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.dbaccess.AbstractDbProvider;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.IoUtils;

/**
 * Base class implementing PostgreSQL database access related methods
 *
 */
public class PostgreSqlDbProvider extends AbstractDbProvider {

    /**
     * All of the properties (their names) for the TABLE INDEXes
     * */
    public static class IndexProperties {

        public static final String INDEX_NAME      = "index_name";      // result set index 6
        public static final String COLUMN_NAME     = "column_name";     // result set index 9

        /**
         * Combines both INDEX_NAME and COLUMN_NAME. Format <strong>COLUMN_NAME=&ltvalue&gt, INDEX_NAME=&ltvalue&gt</strong>
         * */
        public static final String INDEX_UID       = "index_uid";
        public static final String TYPE            = "type";            // result set index 7
        public static final String COLUMN_POSITION = "column_position"; // result set index 8
        public static final String IS_UNIQUE       = "IsUnique";        // result set index 4
    }

    private static final Logger log               = LogManager.getLogger(PostgreSqlDbProvider.class);

    private Set<String>         partitionedTables = new HashSet<>();

    public PostgreSqlDbProvider( DbConnPostgreSQL dbConnection ) {

        super(dbConnection);

        obtainPartitionedTables();

    }

    private void obtainPartitionedTables() {

        String partitionCreatedTable = "partitioned_created_table";

        String query = "SELECT i.inhrelid::regclass AS " + partitionCreatedTable + " FROM pg_inherits i;";

        DbRecordValuesList[] recordsLists = this.select(query);

        for (DbRecordValuesList recordList : recordsLists) {
            for (DbRecordValue value : recordList) {
                String partitionedCreatedTableName = value.getValueAsString();
                partitionedTables.add(partitionedCreatedTableName);
            }
        }

    }

    @Override
    protected boolean isTableAccepted( ResultSet tableResultSet, String dbName, String tableName ) {

        // mark all partitioned created tables as REJECTED (NOT-ACCEPTED)
        return !this.partitionedTables.contains(tableName);
    }

    @Override
    protected String getResultAsEscapedString( ResultSet resultSet, int index,
                                               String columnTypeName ) throws SQLException, IOException {

        String value;
        Object valueAsObject = resultSet.getObject(index);
        if (valueAsObject == null) {
            return null;
        }
        if (valueAsObject != null && valueAsObject.getClass().isArray()) {
            if (! (valueAsObject instanceof byte[])) {
                // FIXME other array types might be needed to be tracked in a different way
                log.warn("Array type that needs attention");
            }
            // we have an array of primitive data type
            InputStream is = null;
            try {
                is = resultSet.getAsciiStream(index);
                value = IoUtils.streamToString(is);
            } finally {
                IoUtils.closeStream(is);
            }
        } else if (valueAsObject instanceof Blob) {
            // we have a blob
            log.debug("Blob detected. Will try to dump as hex");
            Blob blobValue = (Blob) valueAsObject;
            InputStream blobInputStream = blobValue.getBinaryStream();
            StringBuilder hexString = new StringBuilder();
            // Read the binary data from the stream and convert it to hex according to the sample from
            // '\x123ABC', according to https://www.postgresql.org/docs/current/datatype-binary.html,
            // Section 8.4.1. bytea Hex Format
            hexString.append("\\x");
            hexString = addBinDataAsHexAndCloseStream(hexString, blobInputStream);
            value = hexString.toString();
        } else {
            // treat as a string
            value = resultSet.getString(index);
            logDebugInfoForDBValue(value, index, resultSet);
        }

        return value;
    }

    @Override
    protected Map<String, String> extractTableIndexes( String tableName, DatabaseMetaData databaseMetaData,
                                                       String catalog ) throws DbException {

        Map<String, String> indexes = new HashMap<>();

        ResultSet indexInformation = null;
        try {
            indexInformation = databaseMetaData.getIndexInfo(catalog, null, tableName, false, true);
            ResultSetMetaData rsmd = indexInformation.getMetaData();
            int colCount = rsmd.getColumnCount();
            while (indexInformation.next()) {
                StringBuilder indexInfo = new StringBuilder();

                String indexUid = null;
                String columnName = null;
                String indexName = null;

                boolean firstTime = true;
                for (int i = 1; i <= colCount; i++) {
                    String name = null;
                    Object value = indexInformation.getObject(i);

                    if (i == 4) {
                        name = IndexProperties.IS_UNIQUE;
                        Boolean valBool = (Boolean) value;
                        if (valBool) {
                            value = "NON_UNIQUE";
                        } else {
                            value = "UNIQUE";
                        }
                    } else if (i == 6) {
                        name = IndexProperties.INDEX_NAME;
                        indexName = (String) value;
                        //continue;
                    } else if (i == 7) {
                        name = IndexProperties.TYPE;
                    } else if (i == 8) {
                        name = IndexProperties.COLUMN_POSITION;
                    } else if (i == 9) {
                        name = IndexProperties.COLUMN_NAME;
                        columnName = (String) value;
                        //continue;
                    } else {
                        // not supported property
                        continue;
                    }

                    if (firstTime) {
                        firstTime = false;
                        indexInfo.append(name + "=" + value);
                    } else {
                        indexInfo.append(", " + name + "=" + value);
                    }
                }

                if (columnName != null && indexName != null) {
                    indexUid = "" + IndexProperties.COLUMN_NAME + "=" + columnName + ", " + IndexProperties.INDEX_NAME
                               + "=" + indexName;
                }

                if (indexUid == null) {
                    indexUid = "NULL_UID_FOUND_FOR_INDEX_OF_TABLE_" + tableName;
                    log.warn(IndexProperties.INDEX_UID
                             + " column not found in query polling for index properties:\nWe will use the following as an index uid: "
                             + indexUid);
                }

                indexes.put(indexUid, indexInfo.toString());

            }
        } catch (SQLException e) {
            throw new DbException("Error extracting table indexes info", e);
        } finally {
            DbUtils.closeResultSet(indexInformation);
        }

        return indexes;
    }

}
