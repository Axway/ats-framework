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
package com.axway.ats.core.dbaccess.oracle;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.BaseTest;

import oracle.jdbc.driver.OracleDriver;
import oracle.jdbc.pool.OracleDataSource;

public class Test_DbConnOracle extends BaseTest {

    @Test
    public void accessors() {

        Map<String, Object> customProperties = new HashMap<String, Object>();
        customProperties.put( DbKeys.PORT_KEY, 123 );

        DbConnOracle dbConnection = new DbConnOracle( "host", "db", "user", "pass", customProperties );

        assertEquals( DbConnOracle.DATABASE_TYPE, dbConnection.getDbType() );
        assertEquals( "host", dbConnection.getHost() );
        assertEquals( "db", dbConnection.getDb() );
        assertEquals( "user", dbConnection.getUser() );
        assertEquals( "pass", dbConnection.getPassword() );
        assertEquals( "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=host)(PORT=123))(CONNECT_DATA=(SID=ORCL)))", dbConnection.getURL() );
        assertEquals( "host123db", dbConnection.getConnHash() );
        assertEquals( "Oracle connection to host:123/db", dbConnection.getDescription() );
        assertEquals( OracleDriver.class, dbConnection.getDriverClass() );
    }

    @Test
    public void getDataSource() {

        DbConnOracle dbConnection = new DbConnOracle( "host", "db", "user", "pass" );

        assertEquals( OracleDataSource.class, dbConnection.getDataSource().getClass() );
    }

}
