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
package com.axway.ats.action.dbaccess;

import org.junit.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.dbaccess.DatabaseOperations;
import com.axway.ats.action.dbaccess.model.DatabaseRow;
import com.axway.ats.action.exceptions.DatabaseOperationsException;

/**
 * Test class for testing general database operations
 *
 */
public class Test_GeneralDatabaseOperations extends BaseTest {

    public static DatabaseOperations dbOps;

    @BeforeClass
    public static void init() {

        dbOps = new MockGeneralDatabaseOperations();
    }

    @Test
    public void testGettingDatabaseData() {

        DatabaseRow[] dbRows = dbOps.getDatabaseData( "tableWithManyRows",
                                                      "selectColumnName",
                                                      "whereColumnName",
                                                      "whereColumnValue" );
        Assert.assertEquals( dbRows.length, 2 );

        Assert.assertEquals( dbRows[0].getAllCells().length, 1 );
        Assert.assertEquals( dbRows[0].getCellValue( "selectColumnName" ), "value1" );

        Assert.assertEquals( dbRows[1].getAllCells().length, 1 );
        Assert.assertEquals( dbRows[1].getCellValue( "selectColumnName" ), "value2" );
    }

    @Test
    public void testGettingDatabaseData2() {

        DatabaseRow[] dbRows = dbOps.getDatabaseData( "Select * from tableWithManyRows" );

        Assert.assertEquals( dbRows.length, 2 );

        Assert.assertEquals( dbRows[0].getAllCells().length, 2 );
        Assert.assertEquals( dbRows[0].getCellValue( "firstColumnName" ), "value01" );
        Assert.assertEquals( dbRows[0].getCellValue( "secondColumnName" ), "value02" );

        Assert.assertEquals( dbRows[1].getAllCells().length, 2 );
        Assert.assertEquals( dbRows[1].getCellValue( "firstColumnName" ), "value11" );
        Assert.assertEquals( dbRows[1].getCellValue( "secondColumnName" ), "value12" );
    }

    @Test
    public void testGettingValue() {

        Assert.assertEquals( dbOps.getValue( "tRuns", "dateEnd", "runId", "7661" ), "value00" );
    }

    @Test
    public void testGettingMoreThanOneValues() {

        Assert.assertEquals( dbOps.getValue( "tRuns", "dateEnd", "runId", "moreValues" ), "value00" );
    }

    @Test
    public void testGettingNullValue() {

        Assert.assertNull( dbOps.getValue( "tRuns", "dateEnd", "runId", "nullValue" ) );
    }

    @Test(expected = DatabaseOperationsException.class)
    public void testGettingValueWithDbOperationException() {

        dbOps.getDatabaseData( "wrongTableName", "dateEnd", "runId", "7661" );
    }

    @Test(expected = DatabaseOperationsException.class)
    public void testGettingValueWithDbOperationException2() {

        dbOps.getValue( "wrongTableName", "dateEnd", "runId", "7661" );
    }

}
