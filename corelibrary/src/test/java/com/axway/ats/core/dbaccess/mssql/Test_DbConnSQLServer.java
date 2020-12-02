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
package com.axway.ats.core.dbaccess.mssql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Test;

import com.axway.ats.core.BaseTest;

public class Test_DbConnSQLServer extends BaseTest {

    @Test
    public void accessors() {

        DbConnSQLServer dbConnection = new DbConnSQLServer("host", "db", "user", "pass");

        assertEquals(DbConnSQLServer.DATABASE_TYPE, dbConnection.getDbType());
        assertEquals("host", dbConnection.getHost());
        assertEquals("db", dbConnection.getDb());
        assertEquals("user", dbConnection.getUser());
        assertEquals("pass", dbConnection.getPassword());
        assertEquals("jdbc:jtds:sqlserver://host:1433/db", dbConnection.getURL());
        assertTrue(dbConnection.getConnHash().startsWith("host_1433_db"));
        assertEquals("MSSQL connection to host:1433/db", dbConnection.getDescription());
        assertEquals(net.sourceforge.jtds.jdbc.Driver.class, dbConnection.getDriverClass());
    }

    @Test
    public void getDataSource() {

        DbConnSQLServer dbConnection = new DbConnSQLServer("host", "db", "user", "pass");

        assertEquals(BasicDataSource.class, dbConnection.getDataSource().getClass());
        
        // so we can test that the customProperties map is modifiable
        dbConnection.getCustomProperties().put("test_key", "test_val");
    }
    
    

}
