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
package com.axway.ats.examples.basic.http;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;

import com.axway.ats.action.rest.RestClient;
import com.axway.ats.action.rest.RestForm;
import com.axway.ats.action.rest.RestResponse;
import com.axway.ats.examples.common.BaseTestClass;

/**
 * Some common test shared by both RestOperations and HttpOperations
 */
public class HttpBaseClass extends BaseTestClass {

    protected static final String BASE_URI = "http://" // protocol
                                             + configuration.getServerIp() // IP of the application we work with
                                             + ":" + configuration.getHttpServerPort() // the port to connect through
                                             + "/" + configuration.getHttpServerWebappWar()
                                             // name of the web application(e.g. name of the war file)
                                             + "/services/"; // servlet name

    /**
     * Do some clean up, so the tested application is in the same good
     * initial state to guarantee each test starts on clean.
     *
     * It is true that not all tests here need this cleanup, but it will not hurt anyway.
     */
    @BeforeMethod
    public void beforeMethod() {

        // some of the tests in this class add users on the remote server
        // so we clean them up here
        RestClient client = new RestClient(BASE_URI + "peoplepage/deleteAllPeople");

        RestResponse response = client.delete();
        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getBodyAsString(), "Nobody is left in the community");
    }

    protected void createPersonToManipulateInTheTest() {

        // create a new person using POST
        RestClient client = new RestClient(BASE_URI + "peoplepage/post");
        RestResponse response = client.postForm(new RestForm().addParameter("firstName", "Chuck")
                                                              .addParameter("lastName", "Norris")
                                                              .addParameter("age", "70"));
        assertEquals(response.getStatusCode(), 201);
        assertTrue(response.getBodyAsString()
                           .startsWith("Saved Person with name Chuck Norris at 70 years"));
    }
}
