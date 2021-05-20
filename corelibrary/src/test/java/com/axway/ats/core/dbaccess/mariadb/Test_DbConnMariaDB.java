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
package com.axway.ats.core.dbaccess.mariadb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mariadb.jdbc.Driver;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.BaseTest;

public class Test_DbConnMariaDB extends BaseTest {

    @Test
    public void accessors() {

        Map<String, Object> customProperties = new HashMap<String, Object>();
        customProperties.put(DbKeys.PORT_KEY, 123);

        DbConnMariaDB dbConnection = new DbConnMariaDB("host", "db", "user", "pass", customProperties);

        assertEquals(DbConnMariaDB.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:mariadb://host:123/db", dbConnection.getURL());
        assertTrue(dbConnection.getConnHash().startsWith("host_123_db"));
        assertEquals("MariaDB connection to host:123/db", dbConnection.getDescription());
        assertEquals(Driver.class, dbConnection.getDriverClass());
    }

    @Test
    public void getDataSource() {

        DbConnMariaDB dbConnection = new DbConnMariaDB("host", "db", "user", "pass");

        assertEquals(MariaDbPoolDataSource.class, dbConnection.getDataSource().getClass());
    }

}
