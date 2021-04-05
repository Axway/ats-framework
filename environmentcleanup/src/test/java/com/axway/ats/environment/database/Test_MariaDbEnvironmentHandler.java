/*
 * Copyright 2021 Axway Software
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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbReturnModes;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.mariadb.DbConnMariaDB;
import com.axway.ats.core.dbaccess.mariadb.MariaDbDbProvider;
import com.axway.ats.environment.BaseTest;
import com.axway.ats.environment.database.exceptions.ColumnHasNoDefaultValueException;
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.DbTable;

@RunWith( PowerMockRunner.class)
public class Test_MariaDbEnvironmentHandler extends BaseTest {

    private static final String LINE_SEPARATOR = AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    private static final String EOL_MARKER     = AbstractEnvironmentHandler.EOL_MARKER;

    private DbConnMariaDB       mockDbConnection;
    private MariaDbDbProvider   mockDbProvider;
    private DataSource          mockDataSource;
    private Connection          mockConnection;
    private PreparedStatement   mockStatement;
    private FileWriter          mockFileWriter;
    private DatabaseMetaData    metaData;

    @Before
    public void setUp() throws IOException {

        mockDbConnection = createMock(DbConnMariaDB.class);
        mockDbProvider = createMock(MariaDbDbProvider.class);
        mockDataSource = createMock(DataSource.class);
        mockConnection = createMock(Connection.class);
        mockStatement = createMock(PreparedStatement.class);
        mockFileWriter = createMock(FileWriter.class);
        metaData = createMock(DatabaseMetaData.class);
    }

    @Test
    public void createBackupPositive() throws DatabaseEnvironmentCleanupException, DbException, IOException,
                                       ParseException, SQLException {

        //the columns meta data
        DbRecordValuesList column1MetaData = new DbRecordValuesList();
        column1MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name1"));
        column1MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "varchar(32)"));
        column1MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", true));
        DbRecordValuesList column2MetaData = new DbRecordValuesList();
        column2MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name2"));
        column2MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "varchar(32)"));
        column2MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", true));
        DbRecordValuesList column3MetaData = new DbRecordValuesList();
        column3MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name3"));
        column3MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "bit"));
        column3MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", 1));
        DbRecordValuesList[] columnsMetaData = new DbRecordValuesList[]{ column1MetaData,
                                                                         column2MetaData,
                                                                         column3MetaData };

        DbRecordValuesList record1Value = new DbRecordValuesList();
        record1Value.add(new DbRecordValue("table1", "name1", "value1"));
        record1Value.add(new DbRecordValue("table1", "name2", null));
        record1Value.add(new DbRecordValue("table1", "name3", "1"));
        DbRecordValuesList[] recordValues = new DbRecordValuesList[]{ record1Value };

        //expect(mockDbProvider.getConnection()).andReturn(mockConnection);
        //expect(mockConnection.getMetaData()).andReturn(metaData);
        expect(mockDbConnection.getUser()).andReturn("myUserName").atLeastOnce();
        //expect(metaData.getDriverMajorVersion()).andReturn(5);
        //expect(metaData.getDriverMinorVersion()).andReturn(1);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class), eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class), eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        //expect the file writer calls

        //foreign keys check start
        mockFileWriter.write("SET FOREIGN_KEY_CHECKS = 0;" + EOL_MARKER + LINE_SEPARATOR);

        //table1
        mockFileWriter.write("LOCK TABLES `table1` WRITE;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("DELETE FROM `table1`;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("INSERT INTO `table1` (name1,name2,name3) VALUES('value1',NULL,0x1);"
                             + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.flush();
        mockFileWriter.write("UNLOCK TABLES;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write(LINE_SEPARATOR);

        //table2
        mockFileWriter.write("LOCK TABLES `table2` WRITE;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("DELETE FROM `table2`;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("INSERT INTO `table2` (name1,name2,name3) VALUES('value1',NULL,0x1);"
                             + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.flush();
        mockFileWriter.write("UNLOCK TABLES;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write(LINE_SEPARATOR);

        //foreign keys check end
        mockFileWriter.write("");

        replayAll();

        DbTable table1 = new DbTable("table1");
        DbTable table2 = new DbTable("table2");
        MariaDbEnvironmentHandler envHandler = new MariaDbEnvironmentHandler(mockDbConnection, mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);
        //envHandler.writeBackupToFile(new PrintWriter(new File("backup.txt")));
        envHandler.writeBackupToFile(mockFileWriter);

        verifyAll();

    }

    @Test
    public void createBackupSkipColumnsPositive() throws DatabaseEnvironmentCleanupException, DbException,
                                                  IOException, ParseException, SQLException {

        //the columns meta data
        DbRecordValuesList column1MetaData = new DbRecordValuesList();
        column1MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name1"));
        column1MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "varchar(32)"));
        column1MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", true));
        DbRecordValuesList column2MetaData = new DbRecordValuesList();
        column2MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name2"));
        column2MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "varchar(32)"));
        column2MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", true));
        DbRecordValuesList column3MetaData = new DbRecordValuesList();
        column3MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name3"));
        column3MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "bit(2)"));
        column3MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", true));
        DbRecordValuesList[] columnsMetaData = new DbRecordValuesList[]{ column1MetaData,
                                                                         column2MetaData,
                                                                         column3MetaData };

        //to be returned for the first table
        DbRecordValuesList recordValue1 = new DbRecordValuesList();
        recordValue1.add(new DbRecordValue("table1", "name1", "value1"));
        recordValue1.add(new DbRecordValue("table1", "name2", null));
        recordValue1.add(new DbRecordValue("table1", "name3", "0x00"));
        DbRecordValuesList[] recordValues1 = new DbRecordValuesList[]{ recordValue1 };

        //to be returned for the seconds table - one of the columns should be skipped
        DbRecordValuesList recordValue2 = new DbRecordValuesList();
        recordValue2.add(new DbRecordValue("table1", "name1", "value1"));
        recordValue2.add(new DbRecordValue("table1", "name3", "0x10"));
        DbRecordValuesList[] recordValues2 = new DbRecordValuesList[]{ recordValue2 };

        //expect(mockDbProvider.getConnection()).andReturn(mockConnection);
        //expect(mockConnection.getMetaData()).andReturn(metaData);
        expect(mockDbConnection.getUser()).andReturn("myUserName").atLeastOnce();
        //expect(metaData.getDriverMajorVersion()).andReturn(5);
        //expect(metaData.getDriverMinorVersion()).andReturn(1);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class), eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues1);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class), eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues2);

        //expect the file writer calls

        //foreign keys check start
        mockFileWriter.write("SET FOREIGN_KEY_CHECKS = 0;" + EOL_MARKER + LINE_SEPARATOR);

        //table1
        mockFileWriter.write("LOCK TABLES `table1` WRITE;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("DELETE FROM `table1`;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("INSERT INTO `table1` (name1,name2,name3) VALUES('value1',NULL,0x00);"
                             + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.flush();
        mockFileWriter.write("UNLOCK TABLES;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write(LINE_SEPARATOR);

        //table2
        mockFileWriter.write("LOCK TABLES `table2` WRITE;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("DELETE FROM `table2`;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("INSERT INTO `table2` (name1,name3) VALUES('value1',0x10);" + EOL_MARKER
                             + LINE_SEPARATOR);
        mockFileWriter.flush();
        mockFileWriter.write("UNLOCK TABLES;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write(LINE_SEPARATOR);

        //foreign keys check end
        mockFileWriter.write("");

        replayAll();

        DbTable table1 = new DbTable("table1");
        List<String> columnsToSkip = new ArrayList<String>();
        columnsToSkip.add("name2");
        DbTable table2 = new DbTable("table2", "dbo", columnsToSkip);

        MariaDbEnvironmentHandler envHandler = new MariaDbEnvironmentHandler(mockDbConnection, mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);

        envHandler.writeBackupToFile(mockFileWriter);

        verifyAll();

    }

    @Test
    public void createBackupNoForeignKeysNoLockNoDelete() throws DatabaseEnvironmentCleanupException,
                                                          DbException, IOException, ParseException,
                                                          SQLException {

        //the columns meta data
        DbRecordValuesList column1MetaData = new DbRecordValuesList();
        column1MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name1"));
        column1MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "varchar(32)"));
        column1MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", true));
        DbRecordValuesList[] columnsMetaData = new DbRecordValuesList[]{ column1MetaData };

        DbRecordValuesList record1Value = new DbRecordValuesList();
        record1Value.add(new DbRecordValue("table1", "name1", "value1"));
        DbRecordValuesList[] recordValues = new DbRecordValuesList[]{ record1Value };

        //expect(mockDbProvider.getConnection()).andReturn(mockConnection);
        //expect(mockConnection.getMetaData()).andReturn(metaData);
        expect(mockDbConnection.getUser()).andReturn("myUserName").atLeastOnce();
        //expect(metaData.getDriverMajorVersion()).andReturn(5);
        //expect(metaData.getDriverMinorVersion()).andReturn(1);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class), eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class), eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        //expect the file writer calls

        //table1
        mockFileWriter.write("INSERT INTO `table1` (name1) VALUES('value1');" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.flush();
        mockFileWriter.write(LINE_SEPARATOR);

        //table2
        mockFileWriter.write("INSERT INTO `table2` (name1) VALUES('value1');" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.flush();
        mockFileWriter.write(LINE_SEPARATOR);

        replayAll();

        DbTable table1 = new DbTable("table1");
        DbTable table2 = new DbTable("table2");

        MariaDbEnvironmentHandler envHandler = new MariaDbEnvironmentHandler(mockDbConnection, mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);

        envHandler.setForeignKeyCheck(false);
        envHandler.setIncludeDeleteStatements(false);
        envHandler.setLockTables(false);

        envHandler.writeBackupToFile(mockFileWriter);

        verifyAll();

    }

    @Test( expected = DatabaseEnvironmentCleanupException.class)
    public void createBackupNegativeNoColumns() throws DatabaseEnvironmentCleanupException, DbException,
                                                IOException, ParseException, SQLException {

        expect(mockDbProvider.getConnection()).andReturn(mockConnection);
        expect(mockConnection.getMetaData()).andReturn(metaData);
        expect(mockDbConnection.getUser()).andReturn("myUserName");
        expect(metaData.getDriverMajorVersion()).andReturn(5);
        expect(metaData.getDriverMinorVersion()).andReturn(1);

        expect(mockDbProvider.select(isA(String.class))).andReturn(new DbRecordValuesList[]{});

        mockFileWriter.write("SET FOREIGN_KEY_CHECKS = 0;" + EOL_MARKER + LINE_SEPARATOR);

        replayAll();

        DbTable table1 = new DbTable("table1");
        DbTable table2 = new DbTable("table2");

        MariaDbEnvironmentHandler envHandler = new MariaDbEnvironmentHandler(mockDbConnection, mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);

        envHandler.writeBackupToFile(mockFileWriter);
    }

    @Test( expected = ColumnHasNoDefaultValueException.class)
    public void createBackupNegativeSkippedColumnIsNotNullable() throws DatabaseEnvironmentCleanupException,
                                                                 DbException, IOException, ParseException,
                                                                 SQLException {

        //the columns meta data
        DbRecordValuesList column1MetaData = new DbRecordValuesList();
        column1MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name1"));
        column1MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "varchar(32)"));
        column1MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", true));
        DbRecordValuesList column2MetaData = new DbRecordValuesList();
        column2MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name2"));
        column2MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "varchar(32)"));
        column2MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", null));
        DbRecordValuesList column3MetaData = new DbRecordValuesList();
        column3MetaData.add(new DbRecordValue("", "COLUMN_NAME", "name3"));
        column3MetaData.add(new DbRecordValue("", "COLUMN_TYPE", "bit(1)"));
        column3MetaData.add(new DbRecordValue("", "COLUMN_DEFAULT", 1));
        DbRecordValuesList[] columnsMetaData = new DbRecordValuesList[]{ column1MetaData,
                                                                         column2MetaData,
                                                                         column3MetaData };

        DbRecordValuesList record1Value = new DbRecordValuesList();
        record1Value.add(new DbRecordValue("table1", "name1", "value1"));
        record1Value.add(new DbRecordValue("table1", "name2", null));
        record1Value.add(new DbRecordValue("table1", "name3", "1"));
        DbRecordValuesList[] recordValues = new DbRecordValuesList[]{ record1Value };

        expect(mockDbProvider.getConnection()).andReturn(mockConnection);
        expect(mockConnection.getMetaData()).andReturn(metaData);
        expect(mockDbConnection.getUser()).andReturn("myUserName").atLeastOnce();
        expect(metaData.getDriverMajorVersion()).andReturn(5);
        expect(metaData.getDriverMinorVersion()).andReturn(1);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class), eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        expect(mockDbProvider.select(isA(String.class))).andReturn(columnsMetaData);
        expect(mockDbProvider.select(isA(DbQuery.class), eq(DbReturnModes.ESCAPED_STRING))).andReturn(recordValues);

        mockFileWriter.write("SET FOREIGN_KEY_CHECKS = 0;" + EOL_MARKER + LINE_SEPARATOR);

        mockFileWriter.write("DELETE FROM `table1`;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("DELETE FROM `table2`;" + EOL_MARKER + LINE_SEPARATOR);

        mockFileWriter.write("LOCK TABLES `table1` WRITE;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write("INSERT INTO `table1` (name1,name2,name3) VALUES('value1',NULL,0x1);"
                             + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.flush();
        mockFileWriter.write("UNLOCK TABLES;" + EOL_MARKER + LINE_SEPARATOR);
        mockFileWriter.write(LINE_SEPARATOR);

        replayAll();

        DbTable table1 = new DbTable("table1");
        List<String> columnsToSkip = new ArrayList<String>();
        columnsToSkip.add("name2");
        DbTable table2 = new DbTable("table2", "dbo", columnsToSkip);

        MariaDbEnvironmentHandler envHandler = new MariaDbEnvironmentHandler(mockDbConnection, mockDbProvider);
        envHandler.addTable(table1);
        envHandler.addTable(table2);

        envHandler.writeBackupToFile(mockFileWriter);

        verifyAll();
    }

    @Test
    public void restoreBackupPositive() throws DatabaseEnvironmentCleanupException, SQLException, DbException {

        //expect(mockDbProvider.getConnection()).andReturn(mockConnection);
        //expect(mockConnection.getMetaData()).andReturn(metaData);
        //expect(metaData.getDriverMajorVersion()).andReturn(5);
        //expect(metaData.getDriverMinorVersion()).andReturn(1);

        expect(mockDbConnection.getConnHash()).andReturn(Long.toString(System.nanoTime()));
        expect(mockDbConnection.getDataSource()).andReturn(mockDataSource);
        expect(mockDbConnection.getUser()).andReturn("user");
        expect(mockDbConnection.getPassword()).andReturn("password");
        expect(mockDataSource.getConnection("user", "password")).andReturn(mockConnection);

        //now the restore begins
        expect(mockConnection.getAutoCommit()).andReturn(true);
        mockConnection.setAutoCommit(false);

        //the backup restore
        expect(mockConnection.prepareStatement("SET FOREIGN_KEY_CHECKS = 0;")).andReturn(mockStatement);
        expect(mockStatement.execute()).andReturn(true);
        mockStatement.close();

        PreparedStatement mockStatement2 = createMock(PreparedStatement.class);
        expect(mockConnection.prepareStatement(LINE_SEPARATOR
                                               + "LOCK TABLES `Revision` WRITE;")).andReturn(mockStatement2);
        expect(mockStatement2.execute()).andReturn(true);
        mockStatement2.close();

        PreparedStatement mockStatement3 = createMock(PreparedStatement.class);
        expect(mockConnection.prepareStatement("DELETE FROM `Revision`;")).andReturn(mockStatement3);
        expect(mockStatement3.execute()).andReturn(true);
        mockStatement3.close();

        PreparedStatement mockStatement4 = createMock(PreparedStatement.class);
        expect(mockConnection.prepareStatement("INSERT INTO `Revision` (id,SchemaVersion,STVersion,InstallDate) "
                                               + LINE_SEPARATOR
                                               + "VALUES(1,2,'4.9  206','2008-11-27 12:35:49.0');")).andReturn(mockStatement4);
        expect(mockStatement4.execute()).andReturn(true);
        mockStatement4.close();

        PreparedStatement mockStatement5 = createMock(PreparedStatement.class);
        expect(mockConnection.prepareStatement("UNLOCK TABLES;")).andReturn(mockStatement5);
        expect(mockStatement5.execute()).andReturn(true);
        mockStatement5.close();

        mockConnection.commit();

        mockConnection.setAutoCommit(true);
        mockConnection.close();

        replayAll();

        MariaDbEnvironmentHandler envHandler = new MariaDbEnvironmentHandler(mockDbConnection, mockDbProvider);
        envHandler.restore(Test_MariaDbEnvironmentHandler.class.getResource("backupFile.txt").getFile());
        //        envHandler.disconnect();

        verifyAll();
    }

    @Test( expected = DatabaseEnvironmentCleanupException.class)
    public void restoreBackupNegativeExceptionThrownOnExecuteBatch()
                                                                     throws DatabaseEnvironmentCleanupException,
                                                                     SQLException, DbException {

        expect(mockDbProvider.getConnection()).andReturn(mockConnection);
        expect(mockConnection.getMetaData()).andReturn(metaData);
        expect(metaData.getDriverMajorVersion()).andReturn(5);
        expect(metaData.getDriverMinorVersion()).andReturn(1);

        expect(mockDbConnection.getConnHash()).andReturn(Long.toString(System.nanoTime()));
        expect(mockDbConnection.getDataSource()).andReturn(mockDataSource);
        expect(mockDbConnection.getUser()).andReturn("user");
        expect(mockDbConnection.getPassword()).andReturn("password");
        expect(mockDataSource.getConnection("user", "password")).andReturn(mockConnection);

        //now the restore begins
        expect(mockConnection.getAutoCommit()).andReturn(true);
        mockConnection.setAutoCommit(false);

        //the backup restore
        expect(mockConnection.prepareStatement("SET FOREIGN_KEY_CHECKS = 0;")).andReturn(mockStatement);
        expect(mockStatement.execute()).andReturn(true);
        mockStatement.close();

        PreparedStatement mockStatement2 = createMock(PreparedStatement.class);
        expect(mockConnection.prepareStatement(LINE_SEPARATOR
                                               + "LOCK TABLES `Revision` WRITE;")).andReturn(mockStatement2);
        expect(mockStatement2.execute()).andReturn(true);
        mockStatement2.close();

        PreparedStatement mockStatement3 = createMock(PreparedStatement.class);
        expect(mockConnection.prepareStatement("DELETE FROM `Revision`;")).andReturn(mockStatement3);
        expect(mockStatement3.execute()).andReturn(true);
        mockStatement3.close();

        PreparedStatement mockStatement4 = createMock(PreparedStatement.class);
        expect(mockConnection.prepareStatement("INSERT INTO `Revision` (id,SchemaVersion,STVersion,InstallDate) "
                                               + LINE_SEPARATOR
                                               + "VALUES(1,2,'4.9  206','2008-11-27 12:35:49.0');")).andReturn(mockStatement4);
        expect(mockStatement4.execute()).andReturn(true);
        mockStatement4.close();

        PreparedStatement mockStatement5 = createMock(PreparedStatement.class);
        expect(mockConnection.prepareStatement("UNLOCK TABLES;")).andReturn(mockStatement5);
        expect(mockStatement5.execute()).andThrow(new SQLException());
        mockStatement5.close();
        mockConnection.rollback();

        mockConnection.setAutoCommit(true);
        mockConnection.close();

        replayAll();

        MariaDbEnvironmentHandler envHandler = new MariaDbEnvironmentHandler(mockDbConnection, mockDbProvider);
        envHandler.restore(Test_MariaDbEnvironmentHandler.class.getResource("backupFile.txt").getFile());
        envHandler.disconnect();

        verifyAll();
    }

    @Test
    public void escapeSQL() throws DatabaseEnvironmentCleanupException, DbException, IOException,
                            ParseException, SQLException {

        //expect(mockDbProvider.getConnection()).andReturn(mockConnection);
        //expect(mockConnection.getMetaData()).andReturn(metaData);
        //expect(metaData.getDriverMajorVersion()).andReturn(5);
        //expect(metaData.getDriverMinorVersion()).andReturn(1);

        replayAll();

        String source = "\\\\NO_FP_NO_IP){0}\\W((ip)|(subnet))\" \"?((no)|(number)|(mask)|(range)|(addr)|(address)|(net)|(network)|(subnet))?.{0,8}\\W\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\W";
        String expected = "\\\\\\\\NO_FP_NO_IP){0}\\\\W((ip)|(subnet))\\\" \\\"?((no)|(number)|(mask)|(range)|(addr)|(address)|(net)|(network)|(subnet))?.{0,8}\\\\W\\\\d{1,3}\\\\.\\\\d{1,3}\\\\.\\\\d{1,3}\\\\.\\\\d{1,3}\\\\W";

        MariaDbEnvironmentHandler envHandler = new MariaDbEnvironmentHandler(mockDbConnection, mockDbProvider);
        String result = envHandler.escapeValue(source);

        Assert.assertEquals(expected, result);

        verifyAll();
    }
}
