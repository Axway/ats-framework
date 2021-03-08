/*
 * Copyright 2018-2019 Axway Software
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
package com.axway.ats.examples.basic;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.ats.action.dbaccess.DatabaseOperations;
import com.axway.ats.action.dbaccess.model.DatabaseCell;
import com.axway.ats.action.dbaccess.model.DatabaseRow;
import com.axway.ats.examples.common.BaseTestClass;
import com.axway.ats.harness.config.TestBox;

/**
 * Here we show different examples when doing basic database operations.
 * These are examples that can work with any of the supported database types.
 *
 * Database operations are introduced at: 
 *      https://axway.github.io/ats-framework/Database-Operations.html
 */
public class DatabaseOperationTests extends BaseTestClass {

    // the ATS class used to interact with a database
    private DatabaseOperations dbOperations;

    // the simple table we work with
    // PostgreSQL requires additional enclosing of the table names when their names are case-sensitive
    private static final String TABLE = "\"People\"";

    /**
     * Prior to each test we make sure we have the table we worked with
     * is in same state
     */
    @BeforeMethod
    public void beforeMethod() {

        // Here we define all connection parameters
        TestBox serverBox = new TestBox();
        serverBox.setHost(configuration.getDatabaseHost());
        serverBox.setDbType(configuration.getDatabaseType());
        serverBox.setDbName(configuration.getDatabaseName());
        serverBox.setDbUser(configuration.getDatabaseUser());
        serverBox.setDbPass(configuration.getDatabasePassword());
        serverBox.setDbPort(String.valueOf(configuration.getDbPort()));
        // establish DB connection
        dbOperations = new DatabaseOperations(serverBox);

        // cleanup the table we use and fill it with the needed data
        dbOperations.delete(TABLE, "1=1");
        dbOperations.insertValues(TABLE, new String[]{ "id", "firstName", "lastName", "age" },
                                  new String[]{ "1", "Chuck", "Norris", "70" });
        dbOperations.insertValues(TABLE, new String[]{ "id", "firstName", "lastName", "age" },
                                  new String[]{ "2", "Jackie", "Chan", "64" });
    }

    @Test
    public void insertAndDelete() {

        // we first have 2 table rows
        DatabaseRow[] dbRows = dbOperations.getDatabaseData("SELECT firstName FROM " + TABLE);
        assertEquals(2, dbRows.length);

        // insert another row
        dbOperations.insertValues(TABLE, new String[]{ "id", "firstName", "lastName", "age" },
                                  new String[]{ "3", "Will", "Smith", "50" });

        // verify the new data
        dbRows = dbOperations.getDatabaseData("SELECT firstName FROM " + TABLE);
        assertEquals(3, dbRows.length);
        assertEquals("Chuck", dbRows[0].getCellValue("firstName"));
        assertEquals("Jackie", dbRows[1].getCellValue("firstName"));
        assertEquals("Will", dbRows[2].getCellValue("firstName"));

        // delete the row we just inserted
        dbOperations.delete(TABLE, "firstName='Will'");
        // OR you can use the excute method instead
        // dbOperations.execute( "Delete from " + TABLE + " where firstName='Will'" );

        // again we have 2 rows only
        dbRows = dbOperations.getDatabaseData("SELECT firstName FROM " + TABLE);
        assertEquals(2, dbRows.length);
    }

    @Test
    public void selectSingleValue() {

        // select a single cell value by specifying table, column of interest and 2 tokens for a WHERE condition
        String firstNameFirstPerson = dbOperations.getValue(TABLE, "firstName", "id", "1");
        assertEquals("Chuck", firstNameFirstPerson);

        // select a single value by specifying an SQL query
        String lastNameFirstPerson = dbOperations.getValue("Select lastName from " + TABLE + " where id=1");
        assertEquals("Norris", lastNameFirstPerson);
    }

    @Test
    public void selectMultipleValues() {

        /*
         * when you extract not just a single value, but many rows and columns
         * it is convenient to use the next example which shows the usage
         * of very simple classes called DatabaseRow and DatabaseCell
         */
        DatabaseRow[] dbRows = dbOperations.getDatabaseData("SELECT firstName, lastName, age FROM "
                                                            + TABLE);

        // iterate all rows
        for (DatabaseRow dbRow : dbRows) {

            // get some cells from the current row, then read its name and value
            DatabaseCell firstNameCell = dbRow.getCell("firstName");
            firstNameCell.getName();
            firstNameCell.getValue();

            log.info(dbRow.getCellValue("firstName") + " " + dbRow.getCellValue("lastName") + " is "
                     + dbRow.getCellValue("age") + " years old");
        }
    }
}
