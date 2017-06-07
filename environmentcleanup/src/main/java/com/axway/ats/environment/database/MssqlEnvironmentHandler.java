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
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.ColumnDescription;
import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.MssqlColumnDescription;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.mssql.MssqlDbProvider;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;

class MssqlEnvironmentHandler extends AbstractEnvironmentHandler {

    private static final Logger log            = Logger.getLogger( MssqlEnvironmentHandler.class );
    private static final String HEX_PREFIX_STR = "0x";

    MssqlEnvironmentHandler( DbConnSQLServer dbConnection,
                             MssqlDbProvider dbProvider ) {

        super( dbConnection, dbProvider );
    }

    @Override
    protected List<ColumnDescription> getColumnsToSelect(
                                                          DbTable table,
                                                          String userName ) throws DbException,
                                                                           ColumnHasNoDefaultValueException {

        String selectColumnsInfo = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, "
                                   + "columnproperty(object_id('"
                                   + table.getTableName()
                                   + "'), COLUMN_NAME,'IsIdentity') as isIdentity "
                                   + "FROM information_schema.COLUMNS WHERE table_name LIKE '"
                                   + table.getTableName() + "'";
        ArrayList<ColumnDescription> columnsToSelect = new ArrayList<ColumnDescription>();
        DbRecordValuesList[] columnsMetaData = null;
        try {
            columnsMetaData = this.dbProvider.select( selectColumnsInfo );
        } catch( DbException e ) {
            log.error( "Could not get columns for table "
                       + table.getTableName()
                       + ". Check if the table is existing and that the user has permissions. See more details in the trace." );
            throw e;
        }

        table.setIdentityColumnPresent( false ); // the Identity column can be skipped(excluded)
        for( DbRecordValuesList columnMetaData : columnsMetaData ) {

            String columnName = ( String ) columnMetaData.get( "COLUMN_NAME" );

            //check if the column should be skipped in the backup
            if( !table.getColumnsToExclude().contains( columnName ) ) {

                ColumnDescription colDescription = new MssqlColumnDescription( columnName,
                                                                               ( String ) columnMetaData.get( "DATA_TYPE" ) );
                columnsToSelect.add( colDescription );
                if( ( Integer ) columnMetaData.get( "isIdentity" ) == 1 ) {
                    table.setIdentityColumnPresent( true );
                }
            } else {
                //if this column has no default value, we cannot skip it in the backup
                if( columnMetaData.get( "COLUMN_DEFAULT" ) == null ) {
                    log.error( "Cannot skip columns with no default values while creating backup" );
                    throw new ColumnHasNoDefaultValueException( table.getTableName(), columnName );
                }
            }
        }

        return columnsToSelect;
    }

    @Override
    protected void writeTableToFile(
                                     List<ColumnDescription> columns,
                                     DbTable table,
                                     DbRecordValuesList[] records,
                                     FileWriter fileWriter ) throws IOException, ParseException {

        if( this.includeDeleteStatements && !this.deleteStatementsInserted) {
            this.deleteStatementsInserted = true;
            for( Entry<String, DbTable> entry : dbTables.entrySet() ) {
                DbTable dbTable = entry.getValue();
                fileWriter.write( "DELETE FROM " + dbTable.getTableName() + ";" + EOL_MARKER
                                  + AtsSystemProperties.SYSTEM_LINE_SEPARATOR );
            }
        }

        if( table.getAutoIncrementResetValue() != null ) {
            fileWriter.write( "DBCC CHECKIDENT ('" + table.getTableName() + "', RESEED, "
                              + table.getAutoIncrementResetValue() + ");" + EOL_MARKER
                              + AtsSystemProperties.SYSTEM_LINE_SEPARATOR );
        }

        if( records.length > 0 ) {

            StringBuilder insertStatement = new StringBuilder();
            String insertBegin = "INSERT INTO " + table.getTableName() + "(" + getColumnsString( columns )
                                 + ") VALUES (";
            String insertEnd = null;
            if( table.isIdentityColumnPresent() ) {
                insertBegin = "SET IDENTITY_INSERT " + table.getTableName() + " ON; " + insertBegin;
                insertEnd = "); SET IDENTITY_INSERT " + table.getTableName() + " OFF;" + EOL_MARKER
                            + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
            } else {
                insertEnd = ");" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
            }

            for( DbRecordValuesList record : records ) {

                insertStatement.append( insertBegin );

                for( int i = 0; i < record.size(); i++ ) {

                    DbRecordValue recordValue = record.get( i );
                    String fieldValue = ( String ) recordValue.getValue();

                    // extract specific values depending on their type
                    insertStatement.append( extractValue( columns.get( i ), fieldValue ) );
                    insertStatement.append( "," );
                }
                //remove the last comma
                insertStatement.delete( insertStatement.length() - 1, insertStatement.length() );
                insertStatement.append( insertEnd );
            }
            fileWriter.write( insertStatement.toString() );
        }
    }

    protected String getColumnsString(
                                       List<ColumnDescription> columns ) {

        StringBuilder columnsBuilder = new StringBuilder();

        //create the columns string
        for( ColumnDescription column : columns ) {
            columnsBuilder.append( '[' + column.getName() );
            columnsBuilder.append( "]," );

        }
        //remove the last comma
        if( columnsBuilder.length() > 1 ) {
            columnsBuilder.delete( columnsBuilder.length() - 1, columnsBuilder.length() );
        }

        return columnsBuilder.toString();
    }

    @Override
    protected String disableForeignKeyChecksStart() {

        // Disable all constraints
        return "EXEC sp_msforeachtable \"ALTER TABLE ? NOCHECK CONSTRAINT all\";" + EOL_MARKER
               + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    @Override
    protected String disableForeignKeyChecksEnd() {

        // Enable all constraints
        return "EXEC sp_msforeachtable @command1=\"ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all\";"
               + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    // extracts the specific value, considering it's type and the specifics associated with it
    private StringBuilder extractValue(
                                        ColumnDescription column,
                                        String fieldValue ) throws ParseException {

        if( fieldValue == null ) {
            return new StringBuilder( "NULL" );
        }

        StringBuilder insertStatement = new StringBuilder();
        // non-string values. Should not be in quotes and do not need escaping
        if( column.isTypeNumeric() ) {

            // BIT type stores only two types of values - 0 and 1, we need to
            // extract them and pass them back as string
            if( column.isTypeBit() ) {
                // The value must be a hex number 0xnnnn
                if( fieldValue.startsWith( HEX_PREFIX_STR ) ) {
                    // value already in hex notation. This is because for BIT(>1) resultSet.getObject(col) currently
                    // returns byte[]
                    insertStatement.append( fieldValue );
                } else {
                    insertStatement.append( HEX_PREFIX_STR + fieldValue );
                }
            } else {
                insertStatement.append( fieldValue );
            }
        } else if( column.isTypeBinary() ) {

            if( fieldValue.startsWith( HEX_PREFIX_STR ) ) {
                insertStatement.append( fieldValue );
            } else {
                insertStatement.append( HEX_PREFIX_STR + fieldValue );
            }
        } else {

            insertStatement.append( '\'' );
            insertStatement.append( fieldValue.replace( "'", "''" ) );
            insertStatement.append( '\'' );
        }

        return insertStatement;
    }

    /**
     * @see com.axway.ats.environment.database.model.RestoreHandler#restore(java.lang.String)
     */
    public void restore(
                         String backupFileName ) throws DatabaseEnvironmentCleanupException {

        BufferedReader backupReader = null;
        Connection connection = null;

        //we need to preserve the auto commit option, as the connections are pooled
        boolean isAutoCommit = true;

        try {
            log.debug( "Starting restoring db backup from file '" + backupFileName + "'" );

            backupReader = new BufferedReader( new FileReader( new File( backupFileName ) ) );

            connection = ConnectionPool.getConnection( dbConnection );

            isAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit( false );

            StringBuilder sql = new StringBuilder();
            String line = backupReader.readLine();
            while( line != null ) {

                sql.append( line );

                if( line.endsWith( EOL_MARKER ) ) {

                    // remove the EOL marker
                    sql.delete( sql.length() - EOL_MARKER.length(), sql.length() );
                    PreparedStatement updateStatement = connection.prepareStatement( sql.toString() );

                    //catch the exception and rollback, otherwise we are locked
                    try {
                        updateStatement.execute();
                    } catch( SQLException sqle ) {
                        log.error( "Error invoking restore satement: " + sql.toString() );
                        //we have to roll back the transaction and re-throw the exception
                        connection.rollback();
                        throw sqle;
                    } finally {
                        try {
                            updateStatement.close();
                        } catch( SQLException sqle ) {
                            log.error( "Unable to close prepared statement", sqle );
                        }
                    }
                    sql = new StringBuilder();
                } else {
                    //add a new line
                    //FIXME: this code will add the system line ending - it
                    //is not guaranteed that this was the actual line ending
                    sql.append( AtsSystemProperties.SYSTEM_LINE_SEPARATOR );
                }

                line = backupReader.readLine();
            }

            try {
                //commit the transaction
                connection.commit();

            } catch( SQLException sqle ) {
                //we have to roll back the transaction and re-throw the exception
                connection.rollback();
                throw sqle;
            }

            log.debug( "Finished restoring db backup from file '" + backupFileName + "'" );

        } catch( IOException ioe ) {
            throw new DatabaseEnvironmentCleanupException( ERROR_RESTORING_BACKUP + backupFileName, ioe );
        } catch( SQLException sqle ) {
            throw new DatabaseEnvironmentCleanupException( ERROR_RESTORING_BACKUP + backupFileName, sqle );
        } catch( DbException dbe ) {
            throw new DatabaseEnvironmentCleanupException( ERROR_RESTORING_BACKUP + backupFileName, dbe );
        } finally {
            try {
                IoUtils.closeStream( backupReader, "Could not close reader for backup file "
                        + backupFileName);
                if( connection != null ) {
                    connection.setAutoCommit( isAutoCommit );
                    connection.close();
                }
            } catch( SQLException sqle ) {
                log.error( ERROR_RESTORING_BACKUP + backupFileName, sqle );
            }
        }
    }
}
