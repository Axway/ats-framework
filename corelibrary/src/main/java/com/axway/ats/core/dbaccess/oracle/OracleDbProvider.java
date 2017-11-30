/*
 * Copyright 2017 Axway Software
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
package com.axway.ats.core.dbaccess.oracle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.axway.ats.core.dbaccess.AbstractDbProvider;
import com.axway.ats.core.dbaccess.DbColumn;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.OracleColumnDescription;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.IoUtils;

/**
 * Provides Oracle specific database queries.
 */
public class OracleDbProvider extends AbstractDbProvider {

    private static final Logger log = Logger.getLogger(OracleDbProvider.class);

    /**
     * Constructor to create authenticated connection to a database.
     * Takes DbConnection object
     * 
     * @param dbConnection db-connection object
     */
    public OracleDbProvider(
                             DbConnection dbConnection ) {

        super(dbConnection);
    }

    @Override
    protected InputStream getResultAsInputStream( ResultSet resultSet, int index, String columnTypeName )
                                                                                                          throws SQLException {

        Object valueAsObject = resultSet.getObject(index);
        if (valueAsObject == null) {
            return null;
        }
        InputStream is = null;
        if (valueAsObject != null && valueAsObject.getClass().isArray()) {

            if (! (valueAsObject instanceof byte[])) {
                // FIXME other array types might be needed to be tracked in a different way 
                log.warn("Array type that needs attention");
            }
            // we have an array of primitive data type
            is = resultSet.getBinaryStream(index);
        } else if (valueAsObject instanceof Blob) {

            Blob blobValue = (Blob) valueAsObject;
            return blobValue.getBinaryStream();

        } else if (columnTypeName.startsWith("TIMESTAMP")) {

            Timestamp timestamp = resultSet.getTimestamp(index);
            is = new ByteArrayInputStream(timestamp.toString().getBytes());
        } else if (valueAsObject instanceof Timestamp) {

            //Oracle JDBC drivers maps the DATE SQL type to java.sql.Timestamp
            Timestamp timestamp = (Timestamp) valueAsObject;
            is = new ByteArrayInputStream(timestamp.toString().getBytes());

        } else {
            // treat as a string
            String value = resultSet.getString(index);
            is = new ByteArrayInputStream(value.getBytes());
            logDebugInfoForDBValue(value, index, resultSet);
        }
        return is;
    }

    @Override
    protected String getResultAsEscapedString( ResultSet resultSet, int index, String columnTypeName )
                                                                                                       throws SQLException,
                                                                                                       IOException {

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
                is = resultSet.getBinaryStream(index);
                StringBuilder hexString = new StringBuilder();
                hexString = addBinDataAsHexAndCloseStream(hexString, is);
                value = hexString.toString();
            } finally {
                IoUtils.closeStream(is);
            }
        } else if (valueAsObject instanceof Blob) {
            // we have a blob
            log.debug("Blob detected. Will try to dump as hex sequence");
            Blob blobValue = (Blob) valueAsObject;
            InputStream blobInputStream = blobValue.getBinaryStream();
            StringBuilder hexString = new StringBuilder();

            //read the binary data from the stream and convert it to hex
            hexString = addBinDataAsHexAndCloseStream(hexString, blobInputStream);
            value = hexString.toString();

        } else if (columnTypeName.startsWith("TIMESTAMP")) {
            Timestamp timestamp = resultSet.getTimestamp(index);
            value = timestamp.toString();

        } else if (valueAsObject instanceof Timestamp) {
            //Oracle JDBC drivers maps the DATE SQL type to java.sql.Timestamp
            Timestamp timestamp = (Timestamp) valueAsObject;
            value = timestamp.toString();

        } else {
            // treat as a string
            value = resultSet.getString(index);
            logDebugInfoForDBValue(value, index, resultSet);
        }
        return value;
    }

    @Override
    protected DbRecordValue parseDbRecordAsObject( DbColumn dbColumn, ResultSet res,
                                                   int columnIndex ) throws IOException, SQLException {

        DbRecordValue recordValue = null;

        Object valueAsObject = res.getObject(columnIndex);
        if (valueAsObject == null) {
            // null object
            recordValue = new DbRecordValue(dbColumn, null);
        } else if (valueAsObject instanceof Blob) {
            // a blob, it is binary type, but we need to read in some special way as hex string
            StringBuilder stringValue = addBinDataAsHexAndCloseStream(new StringBuilder(),
                                                                      ((Blob) valueAsObject).getBinaryStream());
            recordValue = new DbRecordValue(dbColumn, stringValue.toString());
        } else {
            // it is not binary or it is a non-blob binary
            OracleColumnDescription columnDescription = new OracleColumnDescription(dbColumn.getColumnName(),
                                                                                    dbColumn.getColumnType());
            if (columnDescription.isTypeBinary()) {
                recordValue = new DbRecordValue(dbColumn, res.getString(columnIndex));
            } else {
                recordValue = new DbRecordValue(dbColumn, valueAsObject);
            }
        }

        return recordValue;
    }

    @Override
    protected Map<String, String> extractTableIndexes( String tableName, DatabaseMetaData databaseMetaData,
                                                       String catalog ) throws DbException {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT");
        sql.append(" ais.INDEX_NAME,");
        sql.append(" ais.PARTITION_NAME,");
        sql.append(" ais.PARTITION_POSITION,");
        sql.append(" ais.OBJECT_TYPE");
        sql.append(" FROM");
        sql.append(" ALL_IND_STATISTICS ais");
        sql.append(" WHERE TABLE_OWNER='" + dbConnection.getUser() + "'");
        sql.append(" AND TABLE_NAME='" + tableName + "'");
        sql.append(" AND ais.INDEX_NAME NOT LIKE 'SYS_%'"); // skip system indexes

        String indexName = null;
        Map<String, String> indexes = new HashMap<>();
        for (DbRecordValuesList valueList : select(sql.toString())) {
            StringBuilder info = new StringBuilder();
            boolean firstTime = true;
            for (DbRecordValue dbValue : valueList) {
                String value = dbValue.getValueAsString();
                String name = dbValue.getDbColumn().getColumnName();
                if ("INDEX_NAME".equalsIgnoreCase(name)) {
                    indexName = value;
                } else {
                    info.append(", " + name + "=" + value);
                    if (firstTime) {
                        firstTime = false;
                        info.append(name + "=" + value);
                    } else {
                        info.append(", " + name + "=" + value);
                    }
                }
            }

            if (indexName == null) {
                indexName = "NULL_NAME_FOUND_FOR_INDEX_OF_TABLE_" + tableName;
                log.warn("IndexName column not found in query polling for index properties:\nQuery: "
                         + sql.toString() + "\nQuery result: " + valueList.toString()
                         + "\nWe will use the following as an index name: " + indexName);
            }

            indexes.put(indexName, info.toString());
        }

        return indexes;
    }
}
