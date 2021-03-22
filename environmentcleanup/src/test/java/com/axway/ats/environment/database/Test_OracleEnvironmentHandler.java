/*
 * Copyright 2017-2021 Axway Software
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

import static org.easymock.EasyMock.createMock; //TODO replace with org.easymock.EasyMock.createMockBuilder
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbReturnModes;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.oracle.DbConnOracle;
import com.axway.ats.core.dbaccess.oracle.OracleDbProvider;
import com.axway.ats.environment.BaseTest;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;

public class Test_OracleEnvironmentHandler extends BaseTest {

    private static final String LINE_SEPARATOR = AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    private static final String EOL_MARKER     = AbstractEnvironmentHandler.EOL_MARKER;

    private DbConnOracle        mockDbConnection;
    private OracleDbProvider    mockDbProvider;
    private DataSource          mockDataSource;
    private Connection          mockConnection;
    private PreparedStatement   mockStatement;
    private FileWriter          mockFileWriter;

    @Before
    public void setUp() throws IOException {

        mockDbConnection = createMock(DbConnOracle.class);
        mockDbProvider = createMock(OracleDbProvider.class);
        mockDataSource = createMock(DataSource.class);
        mockConnection = createMock(Connection.class);
        mockStatement = createMock(PreparedStatement.class);
        mockFileWriter = createMock(FileWriter.class);
    }

    @Test
    public void createBackupPositive() throws DatabaseEnvironmentCleanupException, DbException, IOException,
                                       ParseException {

        DbTable table1 = new DbTable("table1");
        DbTable table2 = new DbTable("table2");

        //the columns meta data
        DbRecordValuesList column1MetaData = new DbRecordValuesList();
        column1MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name1"));
        column1MetaData.add(new DbRecordValue("", "DATA_TYPE", "varchar(32)"));
        column1MetaData.add(new DbRecordValue("", "DATA_DEFAULT", true));
        DbRecordValuesList column2MetaData = new DbRecordValuesList();
        column2MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name2"));
        column2MetaData.add(new DbRecordValue("", "DATA_TYPE", "varchar(32)"));
        column2MetaData.add(new DbRecordValue("", "DATA_DEFAULT", true));
        DbRecordValuesList column3MetaData = new DbRecordValuesList();
        column3MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name3"));
        column3MetaData.add(new DbRecordValue("", "DATA_TYPE", "bit"));
        column3MetaData.add(new DbRecordValue("", "DATA_DEFAULT", 1));
        DbRecordValuesList[] columnsMetaData = new DbRecordValuesList[]{ column1MetaData,
                                                                         column2MetaData,
                                                                         column3MetaData };

        DbRecordValuesList record1Value = new DbRecordValuesList();
        record1Value.add(new DbRecordValue("table1", "name1", "value1"));
        record1Value.add(new DbRecordValue("table1", "name2", null));
        record1Value.add(new DbRecordValue("table1", "name3", new String(new char[]{ 1 })));
        DbRecordValuesList[] recordValues = new DbRecordValuesList[]{ record1Value };

        OracleEnvironmentHandler envHandler = new OracleEnvironmentHandler(mockDbConnection,
                                                                           mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);

        expect(mockDbConnection.getUser()).andReturn("myUserName").atLeastOnce();
        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData).times(2);
        expect(mockDbProvider.select(isA(DbQuery.class),
                                     eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues)
                                                                       .times(2);

        //expect the file writer calls

        //foreign keys check start
        mockFileWriter.write("SET CONSTRAINTS ALL DEFERRED;" + EOL_MARKER + LINE_SEPARATOR);
        
        // lock table1
        mockFileWriter.write("LOCK TABLE table1 IN EXCLUSIVE MODE NOWAIT;" + EOL_MARKER + LINE_SEPARATOR);

        //table1
        mockFileWriter.write("DELETE FROM table1;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("INSERT INTO table1(name1,name2,name3) VALUES ('value1',NULL,'"
                             + new String(new char[]{ 1 }) + "');" + EOL_MARKER + LINE_SEPARATOR);
        // lock table2
        mockFileWriter.write("LOCK TABLE table2 IN EXCLUSIVE MODE NOWAIT;" + EOL_MARKER + LINE_SEPARATOR);
        
        //table2
        mockFileWriter.write("DELETE FROM table2;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("INSERT INTO table2(name1,name2,name3) VALUES ('value1',NULL,'"
                             + new String(new char[]{ 1 }) + "');" + EOL_MARKER + LINE_SEPARATOR);

        //foreign keys check end
        mockFileWriter.write("");

        replay(mockDbConnection);
        replay(mockDbProvider);
        replay(mockFileWriter);

        envHandler.writeBackupToFile(mockFileWriter);

        verify(mockDbConnection);
        verify(mockDbProvider);
        verify(mockFileWriter);

    }

    @Test
    public void createBackupSkipColumnsPositive() throws DatabaseEnvironmentCleanupException, DbException,
                                                  IOException, ParseException {

        DbTable table1 = new DbTable("table1");

        List<String> columnsToSkip = new ArrayList<String>();
        columnsToSkip.add("name2");
        DbTable table2 = new DbTable("table2", "dbo", columnsToSkip);

        //the columns meta data
        DbRecordValuesList column1MetaData = new DbRecordValuesList();
        column1MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name1"));
        column1MetaData.add(new DbRecordValue("", "DATA_TYPE", "varchar(32)"));
        column1MetaData.add(new DbRecordValue("", "DATA_DEFAULT", true));
        DbRecordValuesList column2MetaData = new DbRecordValuesList();
        column2MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name2"));
        column2MetaData.add(new DbRecordValue("", "DATA_TYPE", "varchar(32)"));
        column2MetaData.add(new DbRecordValue("", "DATA_DEFAULT", true));
        DbRecordValuesList column3MetaData = new DbRecordValuesList();
        column3MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name3"));
        column3MetaData.add(new DbRecordValue("", "DATA_TYPE", "bit"));
        column3MetaData.add(new DbRecordValue("", "DATA_DEFAULT", true));
        DbRecordValuesList[] columnsMetaData = new DbRecordValuesList[]{ column1MetaData,
                                                                         column2MetaData,
                                                                         column3MetaData };

        //to be returned for the first table
        DbRecordValuesList recordValue1 = new DbRecordValuesList();
        recordValue1.add(new DbRecordValue("table1", "name1", "value1"));
        recordValue1.add(new DbRecordValue("table1", "name2", null));
        recordValue1.add(new DbRecordValue("table1", "name3", new String(new char[]{ 5 })));
        DbRecordValuesList[] recordValues1 = new DbRecordValuesList[]{ recordValue1 };

        //to be returned for the seconds table - one of the columns should be skipped
        DbRecordValuesList recordValue2 = new DbRecordValuesList();
        recordValue2.add(new DbRecordValue("table1", "name1", "value1"));
        recordValue2.add(new DbRecordValue("table1", "name3", new String(new char[]{ 5 })));
        DbRecordValuesList[] recordValues2 = new DbRecordValuesList[]{ recordValue2 };

        OracleEnvironmentHandler envHandler = new OracleEnvironmentHandler(mockDbConnection,
                                                                           mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);

        expect(mockDbConnection.getUser()).andReturn("myUserName").atLeastOnce();
        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class),
                                     eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues1);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class),
                                     eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues2);

        //expect the file writer calls

        //foreign keys check start
        mockFileWriter.write("SET CONSTRAINTS ALL DEFERRED;" + EOL_MARKER + LINE_SEPARATOR);

        // lock table1
        mockFileWriter.write("LOCK TABLE table1 IN EXCLUSIVE MODE NOWAIT;" + EOL_MARKER + LINE_SEPARATOR);
        
        //table1
        mockFileWriter.write("DELETE FROM table1;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("INSERT INTO table1(name1,name2,name3) VALUES ('value1',NULL,'"
                             + new String(new char[]{ 5 }) + "');" + EOL_MARKER + LINE_SEPARATOR);

        // lock table2
        mockFileWriter.write("LOCK TABLE table2 IN EXCLUSIVE MODE NOWAIT;" + EOL_MARKER + LINE_SEPARATOR);

        //table2
        mockFileWriter.write("DELETE FROM table2;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("INSERT INTO table2(name1,name3) VALUES ('value1','"
                             + new String(new char[]{ 5 }) + "');" + EOL_MARKER + LINE_SEPARATOR);

        //foreign keys check end
        mockFileWriter.write("");

        replay(mockDbConnection);
        replay(mockDbProvider);
        replay(mockFileWriter);

        envHandler.writeBackupToFile(mockFileWriter);

        verify(mockDbConnection);
        verify(mockDbProvider);
        verify(mockFileWriter);

    }

    @Test
    public void createBackupNoForeignKeysNoLockNoDelete() throws DatabaseEnvironmentCleanupException,
                                                          DbException, IOException, ParseException {

        DbTable table1 = new DbTable("table1");
        DbTable table2 = new DbTable("table2");

        //the columns meta data
        DbRecordValuesList column1MetaData = new DbRecordValuesList();
        column1MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name1"));
        column1MetaData.add(new DbRecordValue("", "DATA_TYPE", "varchar(32)"));
        column1MetaData.add(new DbRecordValue("", "DATA_DEFAULT", true));
        DbRecordValuesList[] columnsMetaData = new DbRecordValuesList[]{ column1MetaData };

        DbRecordValuesList record1Value = new DbRecordValuesList();
        record1Value.add(new DbRecordValue("table1", "name1", "value1"));
        DbRecordValuesList[] recordValues = new DbRecordValuesList[]{ record1Value };

        OracleEnvironmentHandler envHandler = new OracleEnvironmentHandler(mockDbConnection,
                                                                           mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);

        expect(mockDbConnection.getUser()).andReturn("myUserName").atLeastOnce();
        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class),
                                     eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class),
                                     eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        //expect the file writer calls

        //table1
        mockFileWriter.write("INSERT INTO table1(name1) VALUES ('value1');" + EOL_MARKER + LINE_SEPARATOR);

        //table2
        mockFileWriter.write("INSERT INTO table2(name1) VALUES ('value1');" + EOL_MARKER + LINE_SEPARATOR);

        replay(mockDbConnection);
        replay(mockDbProvider);
        replay(mockFileWriter);

        envHandler.setForeignKeyCheck(false);
        envHandler.setIncludeDeleteStatements(false);
        envHandler.setLockTables(false);

        envHandler.writeBackupToFile(mockFileWriter);

        verify(mockDbConnection);
        verify(mockDbProvider);
        verify(mockFileWriter);

    }

    @Test( /* expected = DbException */)
    public void createBackupNegativeNoColumns() throws DatabaseEnvironmentCleanupException, DbException,
                                                IOException, ParseException {

        DbTable table1 = new DbTable("table1");
        DbTable table2 = new DbTable("table2");

        OracleEnvironmentHandler envHandler = new OracleEnvironmentHandler(mockDbConnection,
                                                                           mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);

        expect(mockDbConnection.getUser()).andReturn("myUserName");
        expect(mockDbProvider.select(isA(String.class))).andReturn(new DbRecordValuesList[]{});

        replay(mockDbConnection);
        replay(mockDbProvider);

        try {
            envHandler.writeBackupToFile(mockFileWriter);
        } catch (DbException e) {
            Assert.assertTrue(e.getMessage() != null
                              && e.getMessage().startsWith("Could not get columns for table"));
        }
    }

    @Test( expected = ColumnHasNoDefaultValueException.class)
    public void createBackupNegativeSkippedColumnIsNotNullable() throws DatabaseEnvironmentCleanupException,
                                                                 DbException, IOException, ParseException {

        DbTable table1 = new DbTable("table1");
        List<String> columnsToSkip = new ArrayList<String>();
        columnsToSkip.add("name2");
        DbTable table2 = new DbTable("table2", "dbo", columnsToSkip);

        //the columns meta data
        DbRecordValuesList column1MetaData = new DbRecordValuesList();
        column1MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name1"));
        column1MetaData.add(new DbRecordValue("", "DATA_TYPE", "varchar(32)"));
        column1MetaData.add(new DbRecordValue("", "DATA_DEFAULT", true));
        DbRecordValuesList column2MetaData = new DbRecordValuesList();
        column2MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name2"));
        column2MetaData.add(new DbRecordValue("", "DATA_TYPE", "varchar(32)"));
        column2MetaData.add(new DbRecordValue("", "DATA_DEFAULT", null));
        DbRecordValuesList column3MetaData = new DbRecordValuesList();
        column3MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name3"));
        column3MetaData.add(new DbRecordValue("", "DATA_TYPE", "bit"));
        column3MetaData.add(new DbRecordValue("", "DATA_DEFAULT", 1));
        DbRecordValuesList[] columnsMetaData = new DbRecordValuesList[]{ column1MetaData,
                                                                         column2MetaData,
                                                                         column3MetaData };

        DbRecordValuesList record1Value = new DbRecordValuesList();
        record1Value.add(new DbRecordValue("table1", "name1", "value1"));
        record1Value.add(new DbRecordValue("table1", "name2", null));
        record1Value.add(new DbRecordValue("table1", "name3", new String(new char[]{ 1 })));
        DbRecordValuesList[] recordValues = new DbRecordValuesList[]{ record1Value };

        OracleEnvironmentHandler envHandler = new OracleEnvironmentHandler(mockDbConnection,
                                                                           mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);

        expect(mockDbConnection.getUser()).andReturn("myUserName").atLeastOnce();
        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class),
                                     eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class),
                                     eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        replay(mockDbConnection);
        replay(mockDbProvider);

        envHandler.writeBackupToFile(mockFileWriter);

        verify(mockDbConnection);
        verify(mockDbProvider);
    }

    @Test
    public void restoreBackupPositive() throws DatabaseEnvironmentCleanupException, SQLException {

        expect(mockDbConnection.getConnHash()).andReturn(Long.toString(System.nanoTime()));
        expect(mockDbConnection.getDataSource()).andReturn(mockDataSource);
        expect(mockDbConnection.getUser()).andReturn("user");
        expect(mockDbConnection.getPassword()).andReturn("password");
        expect(mockDataSource.getConnection("user", "password")).andReturn(mockConnection);

        //now the restore begins
        expect(mockConnection.getAutoCommit()).andReturn(true);
        mockConnection.setAutoCommit(false);

        //the backup restore
        expect(mockConnection.prepareStatement("SET FOREIGN_KEY_CHECKS = 0")).andReturn(mockStatement);
        expect(mockConnection.prepareStatement(LINE_SEPARATOR
                                               + "LOCK TABLES `Revision` WRITE")).andReturn(mockStatement);
        expect(mockConnection.prepareStatement("DELETE FROM `Revision`")).andReturn(mockStatement);
        expect(mockConnection.prepareStatement("INSERT INTO `Revision` (id,SchemaVersion,STVersion,InstallDate) "
                                               + LINE_SEPARATOR
                                               + "VALUES(1,2,'4.9  206','2008-11-27 12:35:49.0')")).andReturn(mockStatement);
        expect(mockConnection.prepareStatement("UNLOCK TABLES")).andReturn(mockStatement);
        expect(mockStatement.execute()).andReturn(true).times(5);
        mockStatement.close();
        expectLastCall().times(5);

        mockConnection.commit();

        mockConnection.setAutoCommit(true);
        mockConnection.close();

        replay(mockDbConnection);
        replay(mockDataSource);
        replay(mockConnection);
        replay(mockStatement);
        replay(mockDbProvider);

        OracleEnvironmentHandler envHandler = new OracleEnvironmentHandler(mockDbConnection,
                                                                           mockDbProvider);
        envHandler.restore(Test_OracleEnvironmentHandler.class.getResource("backupFile.txt").getFile());
        //        envHandler.disconnect();

        verify(mockDbConnection);
        verify(mockDataSource);
        verify(mockConnection);
        verify(mockStatement);
        verify(mockDbProvider);
    }

    @Test( expected = DatabaseEnvironmentCleanupException.class)
    public void restoreBackupNegativeExceptionThrownOnExecuteBatch() throws DatabaseEnvironmentCleanupException,
                                                                     SQLException {

        expect(mockDbConnection.getConnHash()).andReturn(Long.toString(System.nanoTime()));
        expect(mockDbConnection.getDataSource()).andReturn(mockDataSource);
        expect(mockDbConnection.getUser()).andReturn("user");
        expect(mockDbConnection.getPassword()).andReturn("password");
        expect(mockDataSource.getConnection("user", "password")).andReturn(mockConnection);

        //now the restore begins
        expect(mockConnection.getAutoCommit()).andReturn(true);
        mockConnection.setAutoCommit(false);
        expect(mockConnection.createStatement()).andReturn(mockStatement);

        //the backup restore
        expect(mockConnection.prepareStatement("SET FOREIGN_KEY_CHECKS = 0")).andReturn(mockStatement);
        expect(mockStatement.execute()).andThrow(new SQLException());
        mockStatement.close();
        mockConnection.rollback();

        mockConnection.setAutoCommit(true);
        mockConnection.close();

        replay(mockDbConnection);
        replay(mockDataSource);
        replay(mockConnection);
        replay(mockStatement);
        replay(mockDbProvider);

        OracleEnvironmentHandler envHandler = new OracleEnvironmentHandler(mockDbConnection,
                                                                           mockDbProvider);
        envHandler.restore(Test_OracleEnvironmentHandler.class.getResource("backupFile.txt").getFile());
        envHandler.disconnect();

        verify(mockDbConnection);
        verify(mockDataSource);
        verify(mockConnection);
        verify(mockStatement);
        verify(mockDbProvider);
    }
}
