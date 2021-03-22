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
package com.axway.ats.agent.core.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.Component;
import com.axway.ats.agent.core.ComponentEnvironment;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.ComponentAlreadyDefinedException;
import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.mysql.DbConnMySQL;
import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.EnvironmentUnit;
import com.axway.ats.environment.database.DatabaseEnvironmentUnit;
import com.axway.ats.environment.database.model.DbTable;

public class Test_EnvironmentConfigurator {

    private static final String COMPONENT_NAME = "testComponent";

    private Component           testComponent  = getTestComponent();

    @BeforeClass
    public static void beforeClass() {

        //BasicConfigurator.configure();
    }

    @Before
    public void beforeMethod() throws ComponentAlreadyDefinedException {

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.putComponent(testComponent);
    }

    @After
    public void afterMethod() {

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
    }

    @Test
    public void testChangingHost() throws ConfigurationException {

        Properties properties = new Properties();
        properties.put(EnvironmentConfigurator.DB_CONFIGURATION_INDEX, 0);
        properties.put(EnvironmentConfigurator.DB_HOST, "new_test_host");
        EnvironmentConfigurator environmentConfigurator = new EnvironmentConfigurator(COMPONENT_NAME,
                                                                                      Arrays.asList(properties));
        assertTrue(environmentConfigurator.needsApplying());
        environmentConfigurator.apply();

        DatabaseEnvironmentUnit dbEnvUnit = (DatabaseEnvironmentUnit) testComponent.getEnvironments()
                                                                                   .get(0)
                                                                                   .getEnvironmentUnits()
                                                                                   .get(0);
        assertEquals("new_test_host", dbEnvUnit.getDbConnection().getHost());
    }

    @Test
    public void testChangingDbName() throws ConfigurationException, ComponentAlreadyDefinedException {

        Properties properties = new Properties();
        properties.put(EnvironmentConfigurator.DB_CONFIGURATION_INDEX, 0);
        properties.put(EnvironmentConfigurator.DB_NAME, "new_test_dbName");
        EnvironmentConfigurator environmentConfigurator = new EnvironmentConfigurator(COMPONENT_NAME,
                                                                                      Arrays.asList(properties));
        environmentConfigurator.apply();

        DatabaseEnvironmentUnit dbEnvUnit = (DatabaseEnvironmentUnit) testComponent.getEnvironments()
                                                                                   .get(0)
                                                                                   .getEnvironmentUnits()
                                                                                   .get(0);
        assertEquals("new_test_dbName", dbEnvUnit.getDbConnection().getDb());
    }

    @Test
    public void testChangingDbPort() throws ConfigurationException, ComponentAlreadyDefinedException {

        Properties properties = new Properties();
        properties.put(EnvironmentConfigurator.DB_CONFIGURATION_INDEX, 0);
        properties.put(EnvironmentConfigurator.DB_PORT, 1234);
        EnvironmentConfigurator environmentConfigurator = new EnvironmentConfigurator(COMPONENT_NAME,
                                                                                      Arrays.asList(properties));
        environmentConfigurator.apply();

        DatabaseEnvironmentUnit dbEnvUnit = (DatabaseEnvironmentUnit) testComponent.getEnvironments()
                                                                                   .get(0)
                                                                                   .getEnvironmentUnits()
                                                                                   .get(0);
        assertEquals(1234, dbEnvUnit.getDbConnection().getCustomProperties().get(DbKeys.PORT_KEY));
    }

    @Test
    public void testChangingDbUsername() throws ConfigurationException, ComponentAlreadyDefinedException {

        Properties properties = new Properties();
        properties.put(EnvironmentConfigurator.DB_CONFIGURATION_INDEX, 0);
        properties.put(EnvironmentConfigurator.DB_USER_NAME, "new_test_username");
        EnvironmentConfigurator environmentConfigurator = new EnvironmentConfigurator(COMPONENT_NAME,
                                                                                      Arrays.asList(properties));
        environmentConfigurator.apply();

        DatabaseEnvironmentUnit dbEnvUnit = (DatabaseEnvironmentUnit) testComponent.getEnvironments()
                                                                                   .get(0)
                                                                                   .getEnvironmentUnits()
                                                                                   .get(0);
        assertEquals("new_test_username", dbEnvUnit.getDbConnection().getUser());
    }

    @Test
    public void testChangingDbPassword() throws ConfigurationException, ComponentAlreadyDefinedException {

        Properties properties = new Properties();
        properties.put(EnvironmentConfigurator.DB_CONFIGURATION_INDEX, 0);
        properties.put(EnvironmentConfigurator.DB_USER_PASSWORD, "new_test_password");
        EnvironmentConfigurator environmentConfigurator = new EnvironmentConfigurator(COMPONENT_NAME,
                                                                                      Arrays.asList(properties));
        environmentConfigurator.apply();

        DatabaseEnvironmentUnit dbEnvUnit = (DatabaseEnvironmentUnit) testComponent.getEnvironments()
                                                                                   .get(0)
                                                                                   .getEnvironmentUnits()
                                                                                   .get(0);
        assertEquals("new_test_password", dbEnvUnit.getDbConnection().getPassword());
    }

    @Test( expected = ConfigurationException.class)
    public void testWithUnknownComponent() throws ConfigurationException, ComponentAlreadyDefinedException {

        Properties properties = new Properties();
        properties.put(EnvironmentConfigurator.DB_CONFIGURATION_INDEX, 0);
        EnvironmentConfigurator environmentConfigurator = new EnvironmentConfigurator("UNKNOWN_COMPONENT",
                                                                                      Arrays.asList(properties));
        environmentConfigurator.apply();
    }

    @Test( expected = ConfigurationException.class)
    public void testApplyingWithWrongDatabaseIndex() throws ConfigurationException,
                                                     ComponentAlreadyDefinedException {

        List<Properties> dbPropertiesList = new ArrayList<Properties>();
        Properties properties = new Properties();
        properties.put(EnvironmentConfigurator.DB_CONFIGURATION_INDEX, 20);
        dbPropertiesList.add(properties);
        EnvironmentConfigurator environmentConfigurator = new EnvironmentConfigurator(COMPONENT_NAME,
                                                                                      dbPropertiesList);
        environmentConfigurator.apply();
    }

    private Component getTestComponent() {

        Component testComponent = new Component(COMPONENT_NAME);

        Map<String, Object> customProperties = new HashMap<String, Object>();
        customProperties.put(DbKeys.PORT_KEY, 3306);

        List<EnvironmentUnit> environmentUnits = new ArrayList<EnvironmentUnit>();
        environmentUnits.add(new MockDbEnvironmentUnit(new DbConnMySQL("host_1",
                                                                       "db_1",
                                                                       "user_1",
                                                                       "password_1",
                                                                       customProperties)));
        testComponent.setEnvironments(Arrays.asList(new ComponentEnvironment(COMPONENT_NAME,
                                                                             null,
                                                                             environmentUnits,
                                                                             "backupFolder")));
        return testComponent;
    }

}

class MockDbEnvironmentUnit extends DatabaseEnvironmentUnit {

    public MockDbEnvironmentUnit( DbConnection dbConnection ) {

        this("", "", dbConnection, null);
    }

    public MockDbEnvironmentUnit( String backupDirPath,
                                  String backupFileName,
                                  DbConnection dbConnection,
                                  List<DbTable> dbTables ) {

        super(backupDirPath, backupFileName, dbConnection, dbTables);
    }

    @Override
    public void backup() throws EnvironmentCleanupException {

    }
}
