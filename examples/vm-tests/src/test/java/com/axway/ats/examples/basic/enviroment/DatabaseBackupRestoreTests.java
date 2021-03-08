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
package com.axway.ats.examples.basic.enviroment;

import com.axway.ats.action.dbaccess.DatabaseOperations;
import com.axway.ats.action.dbaccess.snapshot.DatabaseSnapshot;
import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.database.DatabaseEnvironmentUnit;
import com.axway.ats.environment.database.model.DbTable;
import com.axway.ats.examples.common.BaseTestClass;
import com.axway.ats.harness.config.TestBox;
import org.apache.log4j.Logger;

import org.testng.annotations.*;

import java.util.*;

import static org.testng.Assert.assertEquals;

public class DatabaseBackupRestoreTests extends BaseTestClass {

    private static final Logger log          = Logger.getLogger(DatabaseBackupRestoreTests.class);
    // the simple table we work with
    // PostgreSQL requires additional enclosing of the table names when their names are case-sensitive
    private static final String TABLE        = "people";
    private static final String nameToFind   = "Chuck";
    private static final String idOfDataInDB = "123";
    private static final int    port         = configuration.getDbPort();
    TestBox serverBox;
    private DatabaseOperations dbOperations;
    private DbConnection       dbConn;
    private List<DbTable>      dbTables;

    @BeforeMethod
    public void init() {

        serverBox = new TestBox();
        serverBox.setHost(configuration.getDatabaseHost());
        serverBox.setDbType(configuration.getDatabaseType());
        serverBox.setDbName(configuration.getBackupDatabaseName());
        serverBox.setDbUser(configuration.getDatabaseUser());
        serverBox.setDbPass(configuration.getDatabasePassword());
        serverBox.setDbPort(String.valueOf(configuration.getDbPort()));
        // establish DB connection
        dbOperations = new DatabaseOperations(serverBox);
        dbConn = new DbConnPostgreSQL(configuration.getDatabaseHost(), port,
                                      configuration.getBackupDatabaseName(),
                                      configuration.getDatabaseUser(), configuration.getDatabasePassword(), null);

        // establish tables which to be inspected
        dbTables = new ArrayList<>();
        dbTables.add(new DbTable(TABLE));

    }

    @Test
    public void testDbDumpDropTrue() throws EnvironmentCleanupException {

        DatabaseEnvironmentUnit deu = new DatabaseEnvironmentUnit(
                ".",
                "DbEnvUnit.sql", dbConn, dbTables);

        deu.setDropTables(true);

        Map<String, Object> config = new HashMap<>();
        config.put(DbKeys.PORT_KEY, port);
        DatabaseSnapshot initialSnapshot = new DatabaseSnapshot("initial", serverBox, config);
        initialSnapshot.takeSnapshot();
        initialSnapshot.saveToFile("./initialSnapshotDropTrue.xml");

        deu.backup();

        dbOperations.insertValues(TABLE, new String[]{ "id", "firstName", "lastName", "age" },
                                  new String[]{ idOfDataInDB, nameToFind, "Smith", "80" });

        String firstNameNewPerson = dbOperations.getValue(
                "Select firstname from " + TABLE + " where id=" + idOfDataInDB);
        assertEquals(firstNameNewPerson, nameToFind);

        deu.restore();

        DatabaseSnapshot secondarySnapshot = new DatabaseSnapshot("secondary", serverBox, config);
        secondarySnapshot.takeSnapshot();
        secondarySnapshot.saveToFile("./secondarySnapshotDropTrue.xml");
        initialSnapshot.compare(secondarySnapshot);

        FileSystemOperations fso = new FileSystemOperations();
        fso.deleteFile("./initialSnapshotDropTrue.xml");
        fso.deleteFile("./secondarySnapshotDropTrue.xml");
    }

    @Test
    public void testDbDumpDropFalse() throws EnvironmentCleanupException {

        DatabaseEnvironmentUnit deu = new DatabaseEnvironmentUnit(
                ".",
                "DbEnvUnit.sql", dbConn, dbTables);

        deu.setDropTables(false);

        Map<String, Object> config = new HashMap<>();
        config.put(DbKeys.PORT_KEY, port);
        DatabaseSnapshot initialSnapshot = new DatabaseSnapshot("initial", serverBox, config);
        initialSnapshot.takeSnapshot();
        initialSnapshot.saveToFile("./initialSnapshotDropFalse.xml");

        deu.backup();

        dbOperations.insertValues(TABLE, new String[]{ "id", "firstName", "lastName", "age" },
                                  new String[]{ idOfDataInDB, nameToFind, "Smith", "80" });

        String firstNameNewPerson = dbOperations.getValue(
                "Select firstname from " + TABLE + " where id=" + idOfDataInDB);
        assertEquals(firstNameNewPerson, nameToFind);

        deu.restore();

        DatabaseSnapshot secondarySnapshot = new DatabaseSnapshot("secondary", serverBox, config);
        secondarySnapshot.takeSnapshot();
        secondarySnapshot.saveToFile("./secondarySnapshotDropFalse.xml");
        initialSnapshot.compare(secondarySnapshot);

        FileSystemOperations fso = new FileSystemOperations();
        fso.deleteFile("./initialSnapshotDropFalse.xml");
        fso.deleteFile("./secondarySnapshotDropFalse.xml");

    }

    @AfterMethod
    public void clear() {

        dbOperations.delete(TABLE, "id =" + idOfDataInDB);
    }

}
