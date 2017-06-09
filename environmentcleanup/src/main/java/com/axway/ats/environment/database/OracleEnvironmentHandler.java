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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.ColumnDescription;
import com.axway.ats.core.dbaccess.ConnectionPool;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.OracleColumnDescription;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.oracle.DbConnOracle;
import com.axway.ats.core.dbaccess.oracle.OracleDbProvider;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;

class OracleEnvironmentHandler extends AbstractEnvironmentHandler {

    private static final Logger log = Logger.getLogger( OracleEnvironmentHandler.class );

    OracleEnvironmentHandler( DbConnOracle dbConnection,
                              OracleDbProvider dbProvider ) {

        super( dbConnection, dbProvider );
    }

    @Override
    protected List<ColumnDescription> getColumnsToSelect(
                                                          DbTable table,
                                                          String userName ) throws DbException,
                                                                           ColumnHasNoDefaultValueException {

        // TODO Implementation might be replaced with JDBC DatabaseMetaData.getColumns() but should be verified
        // with default column values

        // ALL_TAB_COLS - All columns of tables accessible by this user. OWNER restriction is used because user might
        // have access to other user's tables and columns
        String selectColumnsInfo = "SELECT * FROM ALL_TAB_COLS WHERE TABLE_NAME='"
                                   + table.getTableName().toUpperCase() + "' AND OWNER='"
                                   + userName.toUpperCase() + "'";
        ArrayList<ColumnDescription> columnsToSelect = new ArrayList<ColumnDescription>();
        DbRecordValuesList[] columnsMetaData = null;
        try {
            columnsMetaData = this.dbProvider.select( selectColumnsInfo );
        } catch( DbException e ) {
            log.error( "Could not get columns for table "
                       + table.getTableName()
                       + ". You may check if the table exists, if the you are using the right user and it has the right permissions. See more details in the trace." );
            throw e;
        }

        if( columnsMetaData.length == 0 ) {
            throw new DbException( "Could not get columns for table "
                                   + table.getTableName()
                                   + ". You may check if the table exists, if the you are using the right user and it has the right permissions." );
        }

        for( DbRecordValuesList columnMetaData : columnsMetaData ) {

            String columnName = ( String ) columnMetaData.get( "COLUMN_NAME" );

            //check if the column should be skipped in the backup
            if( !table.getColumnsToExclude().contains( columnName ) ) {

                ColumnDescription colDescription = new OracleColumnDescription( columnName,
                                                                                ( String ) columnMetaData.get( "DATA_TYPE" ) );

                columnsToSelect.add( colDescription );
            } else {
                //if this column has no default value, we cannot skip it in the backup
                if( columnMetaData.get( "DATA_DEFAULT" ) == null ) {
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

        // TODO : exclusive table locks START

        if( !this.deleteStatementsInserted ) {
            writeDeleteStatements( fileWriter );
        }

        /*
         *  For reseting some sequence to given value (in this case 100), we need to execute something like this:
         *
            DECLARE
                currVal NUMBER;
            BEGIN
                SELECT test_seq.NEXTVAL INTO currVal FROM dual;
                EXECUTE IMMEDIATE 'ALTER SEQUENCE test_seq INCREMENT BY -' || TO_CHAR(currVal - 100);
                SELECT test_seq.NEXTVAL INTO currVal FROM dual;
                EXECUTE IMMEDIATE 'ALTER SEQUENCE test_seq INCREMENT BY 1';
                COMMIT;
            END;
         */

        if( records.length > 0 ) {

            if( containsBinaryTypes( columns ) ) {

                /*
                 * If the binary value is too long the INSERT operation will fail.
                 * The workaround is to assign the binary value to a variable and then use it in the INSERT(or UPDATE) statement
                 * We have to use DECLARE/BEGIN/END anonymous block statement like this:
                 *
                   DECLARE
                      binValue_0 TABLE_NAME.BLOB_COLUMN_NAME%type;
                    BEGIN
                      binValue_0 := to_blob('3C3F786D6C2076657273696F6E3D22312E3022');
                      INSERT INTO TABLE_NAME(NAME,BLOB_COLUMN_NAME,DESCRIPTION) VALUES ('index.xhtml', binValue_0, 'Index file');
                    END; -- ATS EOL;
                 */

                final String INDENTATION = "  ";
                final String VAR_PREFIX = "binValue_";
                StringBuilder stmtBlockBuilder = new StringBuilder( "DECLARE" + AtsSystemProperties.SYSTEM_LINE_SEPARATOR );

                int variableIndex = 0;
                for( ColumnDescription column : columns ) {
                    if( column.isTypeBinary() ) {
                        stmtBlockBuilder.append( INDENTATION + VAR_PREFIX + ( variableIndex++ ) + " "
                                                 + table.getTableName() + "." + column.getName() + "%type;"
                                                 + AtsSystemProperties.SYSTEM_LINE_SEPARATOR );
                    }
                }
                stmtBlockBuilder.append( "BEGIN" + AtsSystemProperties.SYSTEM_LINE_SEPARATOR );
                int stmtBlockStart = stmtBlockBuilder.length();
                String insertBegin = INDENTATION + "INSERT INTO " + table.getTableName() + "("
                                     + getColumnsString( columns ) + ") VALUES (";
                String insertEnd = ");" + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

                for( DbRecordValuesList record : records ) {

                    StringBuilder insertStatement = new StringBuilder();
                    variableIndex = 0;
                    for( int i = 0; i < record.size(); i++ ) {

                        ColumnDescription column = columns.get( i );
                        DbRecordValue recordValue = record.get( i );
                        // extract the value depending on the column type
                        String fieldValue = extractValue( column, ( String ) recordValue.getValue() ).toString();

                        if( column.isTypeBinary() ) {
                            String varName = VAR_PREFIX + ( variableIndex++ );
                            stmtBlockBuilder.append( INDENTATION + varName + " := " + fieldValue + ";"
                                                     + AtsSystemProperties.SYSTEM_LINE_SEPARATOR );
                            insertStatement.append( varName );
                        } else {
                            insertStatement.append( fieldValue );
                        }
                        insertStatement.append( "," );
                    }
                    //remove the last comma
                    insertStatement.delete( insertStatement.length() - 1, insertStatement.length() );

                    stmtBlockBuilder.append( insertBegin );
                    stmtBlockBuilder.append( insertStatement.toString() );
                    stmtBlockBuilder.append( insertEnd );
                    stmtBlockBuilder.append( "END;" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR );
                    fileWriter.write( stmtBlockBuilder.toString() );
                    fileWriter.flush();

                    // clear to block BEGIN tag
                    stmtBlockBuilder.delete( stmtBlockStart, stmtBlockBuilder.length() );
                }
            } else {

                StringBuilder insertStatement = new StringBuilder();
                String insertBegin = "INSERT INTO " + table.getTableName() + "(" + getColumnsString( columns )
                                     + ") VALUES (";
                String insertEnd = ");" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

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

        // TODO : exclusive table locks END
    }

    @Override
    protected void writeDeleteStatements(
                                          FileWriter fileWriter ) throws IOException {

        if( this.includeDeleteStatements ) {
            for( Entry<String, DbTable> entry : dbTables.entrySet() ) {
                DbTable dbTable = entry.getValue();
                fileWriter.write( "DELETE FROM " + dbTable.getTableName() + ";" + EOL_MARKER
                                  + AtsSystemProperties.SYSTEM_LINE_SEPARATOR );
            }
            this.deleteStatementsInserted = true;
        }
    }

    private boolean containsBinaryTypes(
                                         List<ColumnDescription> columns ) {

        for( ColumnDescription column : columns ) {
            if( column.isTypeBinary() ) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String disableForeignKeyChecksStart() {

        return "SET CONSTRAINTS ALL DEFERRED;" + EOL_MARKER + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    }

    @Override
    protected String disableForeignKeyChecksEnd() {

        return "";
    }

    // extracts the specific value, considering it's type and the specifics associated with it
    private StringBuilder extractValue(
                                        ColumnDescription column,
                                        String fieldValue ) throws ParseException {

        if( fieldValue == null ) {
            return new StringBuilder( "NULL" );
        }

        StringBuilder insertStatement = new StringBuilder();

        String typeInUpperCase = column.getType().toUpperCase();
        if( "DATE".equals( typeInUpperCase ) ) {
            SimpleDateFormat inputDateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.0" );
            SimpleDateFormat outputDateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

            insertStatement.append( "to_date('" );
            insertStatement.append( outputDateFormat.format( inputDateFormat.parse( fieldValue ) ) );
            insertStatement.append( "','YYYY-MM-DD hh24:mi:ss')" );

        } else if( typeInUpperCase.startsWith( "TIMESTAMP" ) ) {
            insertStatement.append( "to_timestamp('" );
            insertStatement.append( fieldValue );
            insertStatement.append( "','YYYY-MM-DD hh24:mi:ss.FF')" );
        } else if( "BLOB".equals( typeInUpperCase ) ) {
            insertStatement.append( "to_blob('" );
            insertStatement.append( fieldValue );
            insertStatement.append( "')" );
        } else if( "CLOB".equals( typeInUpperCase ) ) {
            insertStatement.append( "to_clob('" );
            insertStatement.append( fieldValue.replace( "'", "''" ) );
            insertStatement.append( "')" );
        } else {
            insertStatement.append( "'" );
            insertStatement.append( fieldValue.replace( "'", "''" ) );
            insertStatement.append( "'" );
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

        //we need to preserve the auto commit option, as
        //the connections are pooled
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

                    // remove the EOL marker and the trailing semicolon because, strangely, Oracle JDBC driver
                    // does not require it, as opposing to any other, excluding blocks ([DECLARE]BEGIN-END;)
                    if( line.contains( "END;" ) ) { // in this case semicolon is mandatory
                        sql.delete( sql.length() - EOL_MARKER.length(), sql.length() );
                    } else {
                        sql.delete( sql.length() - EOL_MARKER.length() - 1, sql.length() );
                    }

                    PreparedStatement updateStatement = connection.prepareStatement( sql.toString() );

                    //catch the exception and rollback, otherwise we are locked
                    try {
                        updateStatement.execute();
                    } catch( SQLException sqle ) {
                        log.error( "Error invoking restore satement: " + sql.toString() );
                        //we have to roll back the transaction and re throw the exception
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
                //we have to roll back the transaction and re throw the exception
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
