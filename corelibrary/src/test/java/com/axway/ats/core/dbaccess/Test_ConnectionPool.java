/*
 * Copyright 2019 Axway Software
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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.BaseTest;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;

public class Test_ConnectionPool extends BaseTest {

    @After
    public void after() {

        System.setProperty("com.axway.automation.ats.core.dbaccess.mssql_jdbc_prefix", "");
        System.setProperty("com.axway.automation.ats.core.dbaccess.mssql_jdbc_driver_class", "");
        System.setProperty("com.axway.automation.ats.core.dbaccess.mssql_jdbc_datasource_class", "");

    }

    @Test
    public void checkDbConnSQLServerWithDifferentDrivers() throws NoSuchFieldException, SecurityException,
                                                           IllegalArgumentException, IllegalAccessException,
                                                           ClassNotFoundException, InstantiationException {

        DbConnSQLServer jtdsDbConnSQLServerWithNullDriver = createDbConnSQLServer(null);
        validateJtdsConnection(jtdsDbConnSQLServerWithNullDriver);

        DbConnSQLServer jtdsDbConnSQLServerWithEmptyDriver = createDbConnSQLServer("");
        validateJtdsConnection(jtdsDbConnSQLServerWithEmptyDriver);

        DbConnSQLServer jtdsDbConnSQLServerWithJtdsDriver = createDbConnSQLServer(DbKeys.SQL_SERVER_DRIVER_JTDS);
        validateJtdsConnection(jtdsDbConnSQLServerWithJtdsDriver);

        DbConnSQLServer mssqlDbConnSQLServer = createDbConnSQLServer(DbKeys.SQL_SERVER_DRIVER_MICROSOFT);
        validateMssqlConnection(mssqlDbConnSQLServer);

        obtainConnection(jtdsDbConnSQLServerWithNullDriver); // creates one new connection
        obtainConnection(jtdsDbConnSQLServerWithEmptyDriver); // uses the already created one
        obtainConnection(jtdsDbConnSQLServerWithJtdsDriver); // creates second connection
        obtainConnection(mssqlDbConnSQLServer); // creates third connection

        int poolSize = ((Map) getFieldValue(ConnectionPool.class, "dataSourceMap")).size();

        Assert.assertEquals(3, poolSize);

    }

    private void obtainConnection( DbConnSQLServer dbConnection ) {

        try {
            ConnectionPool.getConnection(dbConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void validateMssqlConnection( DbConnSQLServer connection ) throws NoSuchFieldException, SecurityException,
                                                                       IllegalArgumentException,
                                                                       IllegalAccessException, ClassNotFoundException {

        // check driver class
        Class<?> expectedDriverClass = Class.forName((String) getFieldValue(connection,
                                                                            "MSSQL_JDBC_DRIVER_CLASS_NAME"));
        Class<?> actualDriverClass = (Class<?>) getFieldValue(connection, "jdbcDriverClass");
        Assert.assertEquals(expectedDriverClass, actualDriverClass);
        // check driver prefix
        String expectedDriverPrefix = (String) getFieldValue(connection,
                                                             "MSSQL_JDBC_DRIVER_PREFIX");
        String actualDriverPrefix = (String) getFieldValue(connection, "jdbcDriverPrefix");
        Assert.assertEquals(expectedDriverPrefix, actualDriverPrefix);
        // check datasource class
        Class<?> expectedDataSourceClass = Class.forName((String) getFieldValue(connection,
                                                                                "MSSQL_JDBC_DATASOURCE_CLASS_NAME"));
        Class<?> actualDataSourceClass = (Class<?>) getFieldValue(connection, "jdbcDataSourceClass");
        Assert.assertEquals(expectedDataSourceClass, actualDataSourceClass);

    }

    private void validateJtdsConnection( DbConnSQLServer connection ) throws NoSuchFieldException, SecurityException,
                                                                      IllegalArgumentException, IllegalAccessException,
                                                                      ClassNotFoundException {

        // check driver class
        Class<?> expectedDriverClass = Class.forName((String) getFieldValue(connection,
                                                                            "DEFAULT_JDBC_DRIVER_CLASS_NAME"));
        Class<?> actualDriverClass = (Class<?>) getFieldValue(connection, "jdbcDriverClass");
        Assert.assertEquals(expectedDriverClass, actualDriverClass);
        // check driver prefix
        String expectedDriverPrefix = (String) getFieldValue(connection,
                                                             "DEFAULT_JDBC_DRIVER_PREFIX");
        String actualDriverPrefix = (String) getFieldValue(connection, "jdbcDriverPrefix");
        Assert.assertEquals(expectedDriverPrefix, actualDriverPrefix);
        // check datasource class
        Class<?> expectedDataSourceClass = Class.forName((String) getFieldValue(connection,
                                                                                "DEFAULT_JDBC_DATASOURCE_CLASS_NAME"));
        Class<?> actualDataSourceClass = (Class<?>) getFieldValue(connection, "jdbcDataSourceClass");
        Assert.assertEquals(expectedDataSourceClass, actualDataSourceClass);

    }

    private Object getFieldValue( Object instanceOrClass, String fieldName ) throws NoSuchFieldException,
                                                                             SecurityException,
                                                                             IllegalArgumentException,
                                                                             IllegalAccessException {

        if (instanceOrClass.getClass().getName().equals("java.lang.Class")) {
            Field f = ((Class<?>) instanceOrClass).getDeclaredField(fieldName);
            f.setAccessible(true);

            return f.get(instanceOrClass);
        } else {
            Field f = instanceOrClass.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);

            return f.get(instanceOrClass);
        }

    }

    private DbConnSQLServer createDbConnSQLServer( String driver ) {

        Map<String, Object> customPropertiesJtds = new HashMap<>();
        customPropertiesJtds.put(DbKeys.DRIVER, driver);
        DbConnSQLServer connection = new DbConnSQLServer("localhost",
                                                         "test",
                                                         "AtsUser",
                                                         "AtsPassword",
                                                         customPropertiesJtds);

        return connection;

    }

}
