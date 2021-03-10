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
package com.axway.ats.environment.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.ColumnDescription;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.cassandra.CassandraDbProvider;
import com.axway.ats.core.dbaccess.cassandra.DbConnCassandra;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;

/**
 * Cassandra implementation of the environment handler
 */
class CassandraEnvironmentHandler extends AbstractEnvironmentHandler {

    private static final Logger log            = LogManager.getLogger(CassandraEnvironmentHandler.class);

    private static final String HEX_PREFIX_STR = "0x";

    CassandraEnvironmentHandler( DbConnCassandra dbConnection,
                                 CassandraDbProvider dbProvider ) {

        super(dbConnection, dbProvider);
    }

    @Override
    protected List<ColumnDescription> getColumnsToSelect(
                                                          DbTable table,
                                                          String userName ) throws DbException,
                                                                            ColumnHasNoDefaultValueException {

        Map<String, String> columnInfo = ((CassandraDbProvider) this.dbProvider).getColumnInfo(table.getTableName());

        ArrayList<ColumnDescription> columnsToSelect = new ArrayList<ColumnDescription>();
        for (Entry<String, String> columnEntry : columnInfo.entrySet()) {

            //check if the column should be skipped in the backup
            if (!table.getColumnsToExclude().contains(columnEntry.getKey())) {

                String dataTypes = columnEntry.getValue();

                ColumnDescription colDescription;
                if (!dataTypes.contains("|")) {
                    colDescription = new ColumnDescription(columnEntry.getKey(), columnEntry.getValue());
                } else {
                    String[] dataTypesArray = dataTypes.split("\\|");
                    String[] subDataTypesArray = new String[dataTypesArray.length - 1];
                    for (int i = 0; i < subDataTypesArray.length; i++) {
                        subDataTypesArray[i] = dataTypesArray[i + 1];
                    }
                    colDescription = new ColumnDescription(columnEntry.getKey(), dataTypesArray[0], subDataTypesArray);
                }

                columnsToSelect.add(colDescription);
            }
        }

        return columnsToSelect;
    }

    @Override
    protected void writeTableToFile(
                                     List<ColumnDescription> columns,
                                     DbTable table,
                                     DbRecordValuesList[] records,
                                     Writer fileWriter ) throws IOException, ParseException {

        if (!this.deleteStatementsInserted) {
            writeDeleteStatements(fileWriter);
        }

        if (records.length > 0) {

            String counterColumnName = getCounterColumn(records);
            if (counterColumnName == null) {
                final String insertBegin = "INSERT INTO " + table.getTableName() + "("
                                           + getColumnsString(columns) + ") VALUES (";
                final String insertEnd = ");" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

                StringBuilder insertStatement = new StringBuilder();
                for (DbRecordValuesList dbRow : records) {
                    insertStatement.setLength(0);
                    insertStatement.append(insertBegin);

                    for (int i = 0; i < dbRow.size(); i++) {
                        // extract specific values depending on their type
                        insertStatement.append(extractValue(columns.get(i), dbRow.get(i).getValue()));
                        insertStatement.append(",");
                    }

                    //remove the last comma
                    insertStatement.delete(insertStatement.length() - 1, insertStatement.length());
                    insertStatement.append(insertEnd);

                    fileWriter.write(insertStatement.toString());
                }
            } else {
                // This is the way to insert rows for tables having counter data type. Only one column could have such type.
                // others are UUIDs. SET counterColumnName=+3/-2 just increases/decreases such counter. If not already existing
                // then 0 is assumed as initial value.
                String insertBegin = "UPDATE " + table.getTableName() + " SET " + counterColumnName + " = "
                                     + counterColumnName + " + <the counter value> WHERE ";
                final String insertEnd = EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

                StringBuilder insertStatement = new StringBuilder();
                for (DbRecordValuesList dbRow : records) {

                    insertStatement.setLength(0);
                    insertStatement.append(insertBegin);

                    String counterValue = "";

                    for (int i = 0; i < dbRow.size(); i++) {
                        DbRecordValue dbValue = dbRow.get(i);
                        String dbColumnName = dbValue.getDbColumn().getColumnName();
                        if (dbColumnName.equals(counterColumnName)) {
                            // this is a counter, we will apply it later
                            counterValue = dbValue.getValue().toString();
                        } else {
                            // extract specific values depending on their type
                            insertStatement.append(dbColumnName + " = "
                                                   + extractValue(columns.get(i), dbValue.getValue()));
                            insertStatement.append(" AND ");
                        }
                    }

                    //remove the last 'AND'
                    insertStatement.delete(insertStatement.length() - 5, insertStatement.length());
                    insertStatement.append(insertEnd);

                    fileWriter.write(insertStatement.toString()
                                                    .replace("<the counter value>", counterValue));
                }
            }
        }
    }

    /**
     * Checks and gets column name which has type <em>counter</em>.
     * Column with such type could be at most one
     * @return name of the column with counter type
     */
    private String getCounterColumn(
                                     DbRecordValuesList[] dbRows ) {

        for (int i = 0; i < dbRows[0].size(); i++) {
            DbRecordValue dbValue = dbRows[0].get(i);
            if ("counter".equalsIgnoreCase(dbValue.getDbColumn().getColumnType())) {
                return dbValue.getDbColumn().getColumnName();
            }
        }

        return null;
    }

    @Override
    protected void writeDeleteStatements( Writer fileWriter ) throws IOException {

        if (this.includeDeleteStatements) {
            for (Entry<String, DbTable> entry : dbTables.entrySet()) {
                DbTable dbTable = entry.getValue();
                fileWriter.write("TRUNCATE " + dbTable.getTableName() + ";" + EOL_MARKER
                                 + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
            }
            this.deleteStatementsInserted = true;
        }

    }

    @Override
    protected String getColumnsString(
                                       List<ColumnDescription> columns ) {

        StringBuilder columnsBuilder = new StringBuilder();

        //create the columns string
        for (ColumnDescription column : columns) {
            columnsBuilder.append(column.getName());
            columnsBuilder.append(",");
        }
        //remove the last comma
        if (columnsBuilder.length() > 1) {
            columnsBuilder.delete(columnsBuilder.length() - 1, columnsBuilder.length());
        }

        return columnsBuilder.toString();
    }

    @Override
    protected String disableForeignKeyChecksStart() {

        return "";
    }

    @Override
    protected String disableForeignKeyChecksEnd() {

        return "";
    }

    /**
     * Disconnect after backup process
     */
    @Override
    public void createBackup( String backupFileName ) throws DatabaseEnvironmentCleanupException {

        try {
            super.createBackup(backupFileName);
        } finally {
            ((CassandraDbProvider) dbProvider).disconnect();
        }
    }

    // extracts the specific value, considering it's type and the specifics associated with it
    @SuppressWarnings( "unchecked")
    private StringBuilder extractValue(
                                        ColumnDescription column,
                                        Object fieldValue ) throws ParseException {

        if (fieldValue == null) {
            return new StringBuilder("NULL");
        }

        StringBuilder insertStatement = new StringBuilder();
        // non-string values. Should not be in quotes and do not need escaping
        if ("UUID".equalsIgnoreCase(column.getType())) {
            insertStatement.append(fieldValue.toString());
        } else if ("Set".equalsIgnoreCase(column.getType())) {

            final ColumnDescription subElementDescription = new ColumnDescription(null,
                                                                                  column.getSubsType()[0]);

            insertStatement.append("{");
            Object[] values = ((Set<Object>) fieldValue).toArray();
            for (int i = 0; i < values.length; i++) {
                insertStatement.append(extractValue(subElementDescription, values[i]));
                if (i < values.length - 1) {
                    insertStatement.append(',');
                }
            }
            insertStatement.append("}");
        } else if ("List".equalsIgnoreCase(column.getType())) {

            final ColumnDescription subElementDescription = new ColumnDescription(null,
                                                                                  column.getSubsType()[0]);

            insertStatement.append("[");
            Object[] values = ((List<Object>) fieldValue).toArray();
            for (int i = 0; i < values.length; i++) {
                insertStatement.append(extractValue(subElementDescription, values[i]));
                if (i < values.length - 1) {
                    insertStatement.append(',');
                }
            }
            insertStatement.append("]");
        } else if ("Map".equalsIgnoreCase(column.getType())) {
            final ColumnDescription subElementKeyDescription = new ColumnDescription(null,
                                                                                     column.getSubsType()[0]);
            final ColumnDescription subElementValueDescription = new ColumnDescription(null,
                                                                                       column.getSubsType()[1]);

            insertStatement.append("{");
            Map<String, Object> valuesMap = (Map<String, Object>) fieldValue;
            Set<Entry<String, Object>> fieldEntries = valuesMap.entrySet();
            int i = 0;
            for (Entry<String, Object> fieldEntry : fieldEntries) {
                insertStatement.append(extractValue(subElementKeyDescription, fieldEntry.getKey()));
                insertStatement.append(':');
                insertStatement.append(extractValue(subElementValueDescription, fieldEntry.getValue()));
                ++i;
                if (i < fieldEntries.size()) {
                    insertStatement.append(',');
                }
            }
            insertStatement.append("}");
        } else if ("Date".equalsIgnoreCase(column.getType()) || "Timestamp".equalsIgnoreCase(column.getType())) {
            insertStatement.append('\'');
            insertStatement.append( ((java.util.Date) fieldValue).getTime());
            insertStatement.append('\'');
        } else if ("String".equalsIgnoreCase(column.getType())
                   || "varchar".equalsIgnoreCase(column.getType())) {
            insertStatement.append('\'');
            insertStatement.append(fieldValue.toString().replace("'", "''"));
            insertStatement.append('\'');
        } else if ("blob".equalsIgnoreCase(column.getType())) {
            insertStatement.append(HEX_PREFIX_STR);
            byte[] bytes = null;
            if (fieldValue instanceof ByteBuffer) {
                bytes = convertByteBufferToArray((ByteBuffer) fieldValue); // possible OOM if blob is too large
            } else if (fieldValue instanceof byte[]) {
                bytes = (byte[]) fieldValue;
            } else {
                throw new DbException("Could not process value from column '" + column.toString()
                                      + "' that is deserialized/mapped to Java class '"
                                      + fieldValue.getClass().getName() + "'. Only "
                                      + ByteBuffer.class.getName()
                                      + " and byte[] are the supported");
            }
            insertStatement.append(byteArrayToHex(bytes));
        } else {
            insertStatement.append(fieldValue.toString().replace("'", "''"));
        }

        return insertStatement;
    }

    private byte[] convertByteBufferToArray( ByteBuffer buffer ) {

        if (buffer == null) {
            return null;
        }

        byte[] bytes = new byte[buffer.remaining()];
        /* The ByteBuffer class has method array() that also returns the underlying byte[] array,
         * but this buffer contains not only the Cassandra column value, 
         * but also other values, that are needed only to the ByteBuffer class.
         * So using ByteBuffer.array() is not the same as using ByteBuffer.get(byte[])
         * */
        buffer.get(bytes);
        return bytes;
    }

    public void restore(
                         String backupFileName ) throws DatabaseEnvironmentCleanupException {

        BufferedReader backupReader = null;

        try {
            log.info("Started restore of database backup from file '" + backupFileName + "'");

            backupReader = new BufferedReader(new FileReader(new File(backupFileName)));

            StringBuilder sql = new StringBuilder();
            String line = backupReader.readLine();
            while (line != null) {

                sql.append(line);
                if (line.endsWith(EOL_MARKER)) {

                    // remove the EOL marker
                    sql.delete(sql.length() - EOL_MARKER.length(), sql.length());
                    dbProvider.executeUpdate(sql.toString());

                    sql.delete(0, sql.length());
                } else {
                    //add a new line
                    //FIXME: this code will add the system line ending - it
                    //is not guaranteed that this was the actual line ending
                    sql.append(AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                }

                line = backupReader.readLine();
            }

            log.info("Completed restore of database backup from file '" + backupFileName + "'");

        } catch (IOException ioe) {
            throw new DatabaseEnvironmentCleanupException(ERROR_RESTORING_BACKUP + backupFileName, ioe);
        } catch (DbException dbe) {
            throw new DatabaseEnvironmentCleanupException(ERROR_RESTORING_BACKUP + backupFileName, dbe);
        } finally {
            try {
                if (backupReader != null) {
                    backupReader.close();
                }
            } catch (IOException ioe) {
                log.error(ERROR_RESTORING_BACKUP + backupFileName, ioe);
            }

            ((CassandraDbProvider) dbProvider).disconnect();
        }
    }

    private String byteArrayToHex(
                                   byte[] bytes ) {

        if (bytes == null) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                buf.append("0");
            }

            buf.append(hexString);
        }
        return buf.toString();
    }
}
