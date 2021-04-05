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
package com.axway.ats.core.dbaccess;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.common.dbaccess.OracleKeys;
import com.axway.ats.core.BaseTest;
import com.axway.ats.core.dbaccess.mariadb.DbConnMariaDB;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.mysql.DbConnMySQL;
import com.axway.ats.core.dbaccess.oracle.DbConnOracle;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;

public class Test_DatabaseProviderFactory_createDbConnection extends BaseTest {

    @Test
    public void createMysqlNoCustom() {

        DbConnMySQL dbConnection = (DbConnMySQL) DatabaseProviderFactory.createDbConnection(DbConnMySQL.DATABASE_TYPE,
                                                                                            "host",
                                                                                            DbConnMySQL.DEFAULT_PORT,
                                                                                            "db",
                                                                                            "user",
                                                                                            "pass");

        assertEquals(DbConnMySQL.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:mysql://host:" + DbConnMySQL.DEFAULT_PORT + "/db", dbConnection.getURL());
    }

    @Test
    public void createMariaDbNoCustom() {

        DbConnMariaDB dbConnection = (DbConnMariaDB) DatabaseProviderFactory.createDbConnection(DbConnMariaDB.DATABASE_TYPE,
                                                                                                "host",
                                                                                                DbConnMySQL.DEFAULT_PORT,
                                                                                                "db",
                                                                                                "user",
                                                                                                "pass");

        assertEquals(DbConnMariaDB.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:mariadb://host:" + DbConnMySQL.DEFAULT_PORT + "/db", dbConnection.getURL());
    }

    @Test
    public void createMSSQLNoCustom() {

        DbConnSQLServer dbConnection = (DbConnSQLServer) DatabaseProviderFactory.createDbConnection(DbConnSQLServer.DATABASE_TYPE,
                                                                                                    "host",
                                                                                                    DbConnSQLServer.DEFAULT_PORT,
                                                                                                    "db",
                                                                                                    "user",
                                                                                                    "pass");

        assertEquals(DbConnSQLServer.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:jtds:sqlserver://host:" + DbConnSQLServer.DEFAULT_PORT + "/db", dbConnection.getURL());
    }

    @Test
    public void createOracleNoCustom() {

        DbConnOracle dbConnection = (DbConnOracle) DatabaseProviderFactory.createDbConnection(DbConnOracle.DATABASE_TYPE,
                                                                                              "host",
                                                                                              DbConnOracle.DEFAULT_PORT,
                                                                                              "db",
                                                                                              "user",
                                                                                              "pass");

        assertEquals(DbConnOracle.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=host)(PORT="
                     + DbConnOracle.DEFAULT_PORT + "))(CONNECT_DATA=(SID=ORCL)))",
                     dbConnection.getURL());
    }

    @Test
    public void createPostgreSQLNoCustom() {

        DbConnPostgreSQL dbConnection = (DbConnPostgreSQL) DatabaseProviderFactory.createDbConnection(DbConnPostgreSQL.DATABASE_TYPE,
                                                                                                      "host",
                                                                                                      DbConnPostgreSQL.DEFAULT_PORT,
                                                                                                      "db",
                                                                                                      "user",
                                                                                                      "pass");

        assertEquals(DbConnPostgreSQL.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:postgresql://host:" + DbConnPostgreSQL.DEFAULT_PORT + "/db", dbConnection.getURL());
    }

    @Test
    public void createMysqlWithCustomProperties() {

        Map<String, Object> customProperties = new HashMap<String, Object>();
        customProperties.put(DbKeys.PORT_KEY, 123);

        DbConnMySQL dbConnection = (DbConnMySQL) DatabaseProviderFactory.createDbConnection(DbConnMySQL.DATABASE_TYPE,
                                                                                            "host",
                                                                                            DbConnMySQL.DEFAULT_PORT,
                                                                                            "db",
                                                                                            "user",
                                                                                            "pass",
                                                                                            customProperties);

        assertEquals(DbConnMySQL.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:mysql://host:123/db", dbConnection.getURL());
    }

    @Test
    public void createMariaDbWithCustomProperties() {

        Map<String, Object> customProperties = new HashMap<String, Object>();
        customProperties.put(DbKeys.PORT_KEY, 123);

        DbConnMariaDB dbConnection = (DbConnMariaDB) DatabaseProviderFactory.createDbConnection(DbConnMariaDB.DATABASE_TYPE,
                                                                                                "host",
                                                                                                DbConnMariaDB.DEFAULT_PORT,
                                                                                                "db",
                                                                                                "user",
                                                                                                "pass",
                                                                                                customProperties);

        assertEquals(DbConnMariaDB.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:mariadb://host:123/db", dbConnection.getURL());
    }

    @Test
    public void createMSSQLWithCustomProperties() {

        Map<String, Object> customProperties = new HashMap<String, Object>();

        DbConnSQLServer dbConnection = (DbConnSQLServer) DatabaseProviderFactory.createDbConnection(DbConnSQLServer.DATABASE_TYPE,
                                                                                                    "host",
                                                                                                    DbConnSQLServer.DEFAULT_PORT,
                                                                                                    "db",
                                                                                                    "user",
                                                                                                    "pass",
                                                                                                    customProperties);

        assertEquals(DbConnSQLServer.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:jtds:sqlserver://host:" + DbConnSQLServer.DEFAULT_PORT + "/db", dbConnection.getURL());
    }

    @Test
    public void createOracleWithCustomProperties() {

        Map<String, Object> customProperties = new HashMap<String, Object>();
        customProperties.put(DbKeys.PORT_KEY, 123);
        customProperties.put(OracleKeys.SID_KEY, "sid1");

        DbConnOracle dbConnection = (DbConnOracle) DatabaseProviderFactory.createDbConnection(DbConnOracle.DATABASE_TYPE,
                                                                                              "host",
                                                                                              -1,
                                                                                              "db",
                                                                                              "user",
                                                                                              "pass",
                                                                                              customProperties);

        assertEquals(DbConnOracle.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=host)(PORT=123))(CONNECT_DATA=(SID=sid1)))",
                     dbConnection.getURL());
    }

    @Test
    public void createPostgreSqlWithCustomProperties() {

        Map<String, Object> customProperties = new HashMap<String, Object>();
        customProperties.put(DbKeys.PORT_KEY, 123);

        DbConnPostgreSQL dbConnection = (DbConnPostgreSQL) DatabaseProviderFactory.createDbConnection(DbConnPostgreSQL.DATABASE_TYPE,
                                                                                                      "host",
                                                                                                      DbConnPostgreSQL.DEFAULT_PORT,
                                                                                                      "db",
                                                                                                      "user",
                                                                                                      "pass",
                                                                                                      customProperties);

        assertEquals(DbConnPostgreSQL.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:postgresql://host:123/db", dbConnection.getURL());
    }

}
