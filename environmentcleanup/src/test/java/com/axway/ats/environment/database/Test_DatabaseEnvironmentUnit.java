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

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.mysql.DbConnMySQL;
import com.axway.ats.environment.BaseTest;
import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.database.model.BackupHandler;
import com.axway.ats.environment.database.model.DbTable;
import com.axway.ats.environment.database.model.RestoreHandler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ EnvironmentHandlerFactory.class })
public class Test_DatabaseEnvironmentUnit extends BaseTest {

	private DbConnection mockDbConnection;
	private EnvironmentHandlerFactory mockFactory;
	private RestoreHandler mockRestoreHandler;
	private BackupHandler mockBackupHandler;
	private File tempFile;

	@Before
	public void setUp() throws IOException {

		mockDbConnection = createMock(DbConnection.class);
		mockRestoreHandler = createMock(RestoreHandler.class);
		mockBackupHandler = createMock(BackupHandler.class);
		

		tempFile = File.createTempFile("auto", ".tmp");
	}

	@After
	public void tearDown() {

		if (tempFile.exists()) {
			tempFile.delete();
		}
	}

	@Test
	public void createBackupMysql() throws EnvironmentCleanupException, IOException {

		DbTable table1 = new DbTable("table1");
		DbTable table2 = new DbTable("table2");

		List<DbTable> tables = new ArrayList<DbTable>();
		tables.add(table1);
		tables.add(table2);

		mockFactory = createMock(EnvironmentHandlerFactory.class);
		mockStatic(EnvironmentHandlerFactory.class);
		expect(EnvironmentHandlerFactory.getInstance()).andReturn(mockFactory);
		
		expect(mockFactory.createDbBackupHandler(mockDbConnection)).andReturn(mockBackupHandler);

		mockBackupHandler.setLockTables(true);
		mockBackupHandler.setDropTables(false);
		mockBackupHandler.setSkipTablesContent(false);
		mockBackupHandler.setForeignKeyCheck(true);
		mockBackupHandler.setIncludeDeleteStatements(true);
		mockBackupHandler.addTable(table1);
		mockBackupHandler.addTable(table2);

		mockBackupHandler.createBackup(tempFile.getCanonicalPath());
		mockBackupHandler.disconnect();

		replay(EnvironmentHandlerFactory.class);
		replay(mockFactory);
		replay(mockBackupHandler);

		DatabaseEnvironmentUnit dbEnvironmentUnit = new DatabaseEnvironmentUnit(
				tempFile.getParentFile().getCanonicalPath(), tempFile.getName(), mockDbConnection, tables);
		dbEnvironmentUnit.backup();

		verify(mockFactory);
        verify(mockBackupHandler);
	}
	
    @Test
    public void executeRestore() throws EnvironmentCleanupException, IOException {

        //temp file is created and it exists, so backup will not be triggered
        DbTable table1 = new DbTable("table1");
        DbTable table2 = new DbTable("table2");

        List<DbTable> tables = new ArrayList<DbTable>();
        tables.add(table1);
        tables.add(table2);

        mockFactory = createMock(EnvironmentHandlerFactory.class);
		mockStatic(EnvironmentHandlerFactory.class);
		expect(EnvironmentHandlerFactory.getInstance()).andReturn(mockFactory);
        
        expect(mockFactory.createDbRestoreHandler(mockDbConnection)).andReturn(mockRestoreHandler);
        mockRestoreHandler.restore(tempFile.getCanonicalPath());
        mockRestoreHandler.disconnect();

        replay(EnvironmentHandlerFactory.class);
        replay(mockFactory);
        replay(mockBackupHandler);

        DatabaseEnvironmentUnit dbEnvironmentUnit = new DatabaseEnvironmentUnit(tempFile.getParentFile()
                                                                                        .getCanonicalPath(),
                                                                                tempFile.getName(),
                                                                                mockDbConnection,
                                                                                tables);
        dbEnvironmentUnit.executeRestoreIfNecessary();

        verify(mockFactory);
        verify(mockBackupHandler);
    }

    @Test
    public void getDescription() throws EnvironmentCleanupException, IOException {

        //temp file is created and it exists, so backup will not be triggered
        DbTable table1 = new DbTable("table1");
        DbTable table2 = new DbTable("table2");

        List<DbTable> tables = new ArrayList<DbTable>();
        tables.add(table1);
        tables.add(table2);

        expect(mockDbConnection.getDbType()).andReturn(DbConnMySQL.DATABASE_TYPE);

        replay(mockDbConnection);

        DatabaseEnvironmentUnit dbEnvironmentUnit = new DatabaseEnvironmentUnit(tempFile.getParentFile()
                                                                                        .getCanonicalPath(),
                                                                                tempFile.getName(),
                                                                                mockDbConnection,
                                                                                tables);
        assertEquals("MYSQL database in file " + tempFile.getCanonicalPath(),
                     dbEnvironmentUnit.getDescription());

        verify(mockDbConnection);
    }
}
