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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

//import com.axway.ats.core.dbaccess.db2.DbConnDb2;
import com.axway.ats.core.dbaccess.mysql.DbConnMySQL;
import com.axway.ats.core.dbaccess.oracle.DbConnOracle;
import com.axway.ats.environment.BaseTest;

public class Test_EnvrionmentHandlerFactory extends BaseTest {

    @Test
    public void createDbBackupHandlerPositive() {

        EnvironmentHandlerFactory factory = EnvironmentHandlerFactory.getInstance();

        assertEquals(MysqlEnvironmentHandler.class,
                     factory.createDbBackupHandler(new DbConnMySQL("host", "db", "user", "pass"))
                            .getClass());
        assertEquals(OracleEnvironmentHandler.class,
                     factory.createDbBackupHandler(new DbConnOracle("host", "db", "user", "pass"))
                            .getClass());
    }

    @Test
    public void createDbRestoreHandlerPositive() {

        EnvironmentHandlerFactory factory = EnvironmentHandlerFactory.getInstance();

        assertEquals(MysqlEnvironmentHandler.class,
                     factory.createDbRestoreHandler(new DbConnMySQL("host", "db", "user", "pass"))
                            .getClass());
        assertEquals(OracleEnvironmentHandler.class,
                     factory.createDbRestoreHandler(new DbConnOracle("host", "db", "user", "pass"))
                            .getClass());
    }

}
