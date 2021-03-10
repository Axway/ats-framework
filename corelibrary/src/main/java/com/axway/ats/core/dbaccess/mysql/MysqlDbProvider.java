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
package com.axway.ats.core.dbaccess.mysql;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.dbaccess.AbstractDbProvider;
import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.exceptions.DbException;

/**
 * MySQL implementation of the DB provider
 */
public class MysqlDbProvider extends AbstractDbProvider {

    /**
     * All of the properties (their names) for the TABLE INDEXes
     * */
    public static class IndexProperties {
        public static final String INDEX_UID    = "INDEX_UID";
        public static final String INDEX_NAME   = "INDEX_NAME";
        public static final String COLUMN_NAME  = "COLUMN_NAME";
        public static final String NON_UNIQUE   = "NON_UNIQUE";
        public static final String SEQ_IN_INDEX = "SEQ_IN_INDEX";
        public static final String SUB_PART     = "SUB_PART";
        public static final String PACKED       = "PACKED";
        public static final String NULLABLE     = "NULLABLE";
        public static final String INDEX_TYPE   = "INDEX_TYPE";
        public static final String COLLATION    = "COLLATION";

    }

    private static final Logger log                    = LogManager.getLogger(MysqlDbProvider.class);

    public final static String  FUNC_CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP()";
    public final static String  FUNC_NOW               = "NOW()";
    public final static String  FUNC_LOCALTIME         = "LOCALTIME()";
    public final static String  FUNC_LOCALTIMESTAMP    = "LOCALTIMESTAMP()";
    public final static String  FUNC_UNIX_TIMESTAMP    = "UNIX_TIMESTAMP()";

    /**
     * Constructor to create authenticated connection to a database.
     * Takes DbConnection object
     * 
     * @param dbConnection db-connection object
     */
    public MysqlDbProvider( DbConnMySQL dbConnection ) {

        super(dbConnection);

        this.reservedWords = new String[]{ FUNC_CURRENT_TIMESTAMP,
                                           FUNC_NOW,
                                           FUNC_LOCALTIME,
                                           FUNC_LOCALTIMESTAMP,
                                           FUNC_UNIX_TIMESTAMP };
    }

    /**
     * Returns the {@link Connection} associated with this {@link DbProvider}
     * 
     * @return the {@link Connection} associated with this {@link DbProvider}
     * @throws DbException in case of an DB error
     */
    public Connection getConnection() throws DbException {

        return ConnectionPool.getConnection(dbConnection);
    }

    @Override
    protected String getResultAsEscapedString(
                                               ResultSet resultSet,
                                               int index,
                                               String columnTypeName ) throws SQLException, IOException {

        String value;
        Object valueAsObject = resultSet.getObject(index);
        if (valueAsObject == null) {
            return null;
        }
        if (valueAsObject != null && valueAsObject.getClass().isArray()) {
            // we have an array of primitive data type
            // LONGBLOB types are returned as byte array and '1?' should be transformed to 0x313F

            InputStream blobInputStream = null;
            if (! (valueAsObject instanceof byte[])) {
                // FIXME other array types might be needed to be tracked in a different way 
                log.warn("Array type that needs attention");
            } else {
                // we have byte[] array
                // Despite working for both versions, more tests are needed, so just do it if the JDBC version is 8.xx.xx
                if (DbConnMySQL.MYSQL_JDBS_8_DATASOURCE_CLASS_NAME.equals( ((DbConnMySQL) this.dbConnection).getDataSourceClassName())) {
                    blobInputStream = new ByteArrayInputStream((byte[]) valueAsObject);
                }
            }

            StringBuilder hexString = new StringBuilder();
            hexString.append("0x");
            // read the binary data from the stream and convert it to hex
            if (blobInputStream == null) {
                blobInputStream = resultSet.getBinaryStream(index);
            }
            hexString = addBinDataAsHexAndCloseStream(hexString, blobInputStream);
            value = hexString.toString();

        } else if (valueAsObject instanceof Blob) {
            log.info("Blob detected. Will try to dump as hex");
            // we have a blob
            Blob blobValue = (Blob) valueAsObject;
            InputStream blobInputStream = blobValue.getBinaryStream();
            StringBuilder hexString = new StringBuilder();
            hexString.append("0x");
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

        String sql = "SELECT TABLE_NAME, CONCAT('" + IndexProperties.COLUMN_NAME + "=', " + IndexProperties.COLUMN_NAME
                     + ", ', " + IndexProperties.INDEX_NAME + "=', " + IndexProperties.INDEX_NAME + ") AS "
                     + IndexProperties.INDEX_UID + "," + IndexProperties.NON_UNIQUE
                     + "," + IndexProperties.SEQ_IN_INDEX
                     + "," + IndexProperties.COLUMN_NAME + "," + IndexProperties.COLLATION + ","
                     + IndexProperties.SUB_PART + "," + IndexProperties.PACKED + "," + IndexProperties.NULLABLE + ","
                     + IndexProperties.INDEX_TYPE + " "
                     + "FROM INFORMATION_SCHEMA.STATISTICS " + "WHERE TABLE_NAME='" + tableName
                     + "' AND TABLE_SCHEMA = '" + dbConnection.getDb() + "'";

        String indexUid = null;
        Map<String, String> indexes = new HashMap<>();
        for (DbRecordValuesList valueList : select(sql)) {
            StringBuilder info = new StringBuilder();
            boolean firstTime = true;
            for (DbRecordValue dbValue : valueList) {
                String value = dbValue.getValueAsString();
                String name = dbValue.getDbColumn().getColumnName();
                if (IndexProperties.INDEX_UID.equalsIgnoreCase(name)) {
                    indexUid = value;
                } else {
                    if (firstTime) {
                        firstTime = false;
                        info.append(name + "=" + value);
                    } else {
                        info.append(", " + name + "=" + value);
                    }
                }
            }

            if (indexUid == null) {
                indexUid = "NULL_UID_FOUND_FOR_INDEX_OF_TABLE_" + tableName;
                log.warn("" + IndexProperties.INDEX_UID
                         + " column not found in query polling for index properties:\nQuery: " + sql
                         + "\nQuery result: " + valueList.toString()
                         + "\nWe will use the following as an index uid: " + indexUid);
            }

            indexes.put(indexUid, info.toString());
        }

        return indexes;
    }
}
