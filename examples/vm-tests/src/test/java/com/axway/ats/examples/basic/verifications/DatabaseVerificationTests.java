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
package com.axway.ats.examples.basic.verifications;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.ats.action.dbaccess.DatabaseOperations;
import com.axway.ats.examples.common.BaseTestClass;
import com.axway.ats.harness.config.TestBox;
import com.axway.ats.rbv.clients.DbVerification;

/**
 * Database verifications are used to verify some data is present in some database.
 *
 * Basic ATS verifications functionality is introduced at:
 *      https://axway.github.io/ats-framework/Common-test-verifications.html
 *
 * Database verifications are particularly introduced at:
 *      https://axway.github.io/ats-framework/Database-verifications.html
 *
 */
public class DatabaseVerificationTests extends BaseTestClass {

    // the ATS class used to interact with a database
    private DbVerification dbVerification;

    // we use it to do modify the DB data so our tests can work
    private DatabaseOperations dbOperations;

    // Here we keep all connection parameters
    private TestBox dbServerBox;

    // the simple table we work with
    private static final String TABLE = "\"People\"";

    /**
     * Prior to each test we make sure we have the table we worked with
     * is in same state
     */
    @BeforeMethod
    public void beforeMethod() {

        // initialize all connection parameters
        dbServerBox = new TestBox();
        dbServerBox.setHost(configuration.getDatabaseHost());
        dbServerBox.setDbType(configuration.getDatabaseType());
        dbServerBox.setDbName(configuration.getDatabaseName());
        dbServerBox.setDbUser(configuration.getDatabaseUser());
        dbServerBox.setDbPass(configuration.getDatabasePassword());
        dbServerBox.setDbPort(String.valueOf(configuration.getDbPort()));

        // initialize this helper class 
        dbOperations = new DatabaseOperations(dbServerBox);

        // cleanup the table we use and fill it with the needed data
        dbOperations.delete(TABLE, "1=1");
        dbOperations.insertValues(TABLE, new String[]{ "id", "firstName", "lastName", "age" },
                                  new String[]{ "1", "Chuck", "Norris", "70" });
        dbOperations.insertValues(TABLE, new String[]{ "id", "firstName", "lastName", "age" },
                                  new String[]{ "2", "Jackie", "Chan", "64" });
        // Initialize the class we will use in our tests
        dbVerification = new DbVerification(dbServerBox, TABLE);
    }

    @Test( dependsOnMethods = "verifyDataExists" )
    public void verifyTableExistsFromFirstTry() {

        // just verify there a table with this name 
        // without paying attention on its content

        dbVerification.verifyDbDataExists();
    }

    /**
     * The verification will succeed on the very first try as the content is present.
     *
     * The verification will succeed only if there is at least one row which
     * satisfies all required checks.
     */
    @Test
    public void verifyDataExistsFromFirstTry() {

        // value must be contained or not
        dbVerification.checkFieldValueContains("", "People.firstname", "Jackie");
        dbVerification.checkFieldValueDoesNotContain("", "People.firstname", "Jacc");

        // value must be exact match or not
        dbVerification.checkFieldValueEquals("", "People.age", 64);
        dbVerification.checkFieldValueDoesNotEqual("", "People.age", 65);

        // value must match a regular expression or not
        dbVerification.checkFieldValueRegex("", "People.lastname", "Cha.*");
        dbVerification.checkFieldValueRegexDoesNotMatch("", "People.lastname", "Chh.*");

        // start the verification process
        dbVerification.verifyDbDataExists();
    }

    /**
     * When started, the verification keeps failing because the searched person
     * is not available.
     *
     * Then the searched person is available, but his age is wrong.
     *
     * And finally the age is right and it all matches.
     *
     * In this test a background thread manipulates the DB data.
     */
    @Test
    public void verifyDataExists() {

        insertDbDataInBackground();

        dbVerification.checkFieldValueEquals("", "People.firstname", "Will");
        dbVerification.checkFieldValueEquals("", "People.lastname", "Smith");
        dbVerification.checkFieldValueEquals("", "People.age", 50);

        // start the verification process
        // it will pass on the first time when the search data is present
        dbVerification.verifyDbDataExists();
    }

    @Test
    public void verifyDataDisappears() {

        // insert a person
        dbOperations.insertValues(TABLE, new String[]{ "id", "firstname", "lastname", "age" },
                                  new String[]{ "3", "Will", "Smith", "50" });
        // deleted that person, but in a few seconds
        removeDbDataInBackground();

        // wait until that person disappear from the database
        dbVerification.checkFieldValueEquals("", "People.firstname", "Will");

        // start the verification process
        // it will pass on the first time when the search data is not present
        dbVerification.verifyDbDataDoesNotExist();
    }

    /**
     * Helper method used to insert data into DB from a background thread.
     * This way the main thread is not blocked and the verification works as expected.
     */
    private void insertDbDataInBackground() {

        new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    // insert the expected data, but with not expected age
                    Thread.sleep(2000);
                    dbOperations.insertValues(TABLE, new String[]{ "id", "firstName", "lastName", "age" },
                                              new String[]{ "3", "Will", "Smith", "49" });

                    // now fix the age
                    Thread.sleep(2000);
                    dbOperations.updateValue(TABLE, "age", "50", "firstName", "Will");
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    /**
     * Helper method used to remove data from DB from a background thread.
     * This way the main thread is not blocked and the verification works as expected.
     */
    private void removeDbDataInBackground() {

        new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    // delete the expected data
                    Thread.sleep(3000);
                    dbOperations.delete(TABLE, "firstName='Will'");
                } catch (InterruptedException e) {
                }

            }
        }).start();
    }
}
