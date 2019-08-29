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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.testng.annotations.Test;

import com.axway.ats.action.http.HttpClient;
import com.axway.ats.action.http.HttpHeader;
import com.axway.ats.action.http.HttpResponse;
import com.axway.ats.action.json.JsonText;
import com.axway.ats.action.xml.XmlText;
import com.axway.ats.core.utils.IoUtils;

/**
 * Here we show different examples with the HttpClient class
 *
 * Rest operations are introduced at: 
 *      https://axway.github.io/ats-framework/HTTP-Operations.html
 *
 * The remote test application is the ats-all-in-one-test-rest-server
 * The resource right after the BASE_URI tells the class which provides the remote functionallity.
 *
 *  "resource"          - "class name"
 *  simplepage          - SimplePage
 *  peoplepage          - PeoplePage
 *
 * Note that some of the tests in this class are pretty much the same
 * as the tests in RestOperations class.
 */
public class HttpOperationTests extends HttpBaseClass {

    /**
     * A simple GET which returns some textual body
     */
    @Test
    public void get() {

        // create an instance of the HttpClient by providing the URI to work with
        HttpClient client = new HttpClient(BASE_URI + "simplepage/");

        // execute the HTTP method - in this case a GET
        // each HTTP method returns a response object
        HttpResponse response = client.get();

        // verify the response status in any of the following ways
        assertEquals(response.getStatusCode(), 200);
        response.verifyStatusCode(200);

        // this is how you can read the response headers
        log.info("We received the following response headers:");
        for (HttpHeader header : response.getHeaders()) {
            log.info(header.getKey() + ":" + header.getValue());
        }

        // there are different ways to get the body, here we get it as a simple text
        String responseBody = response.getBodyAsString();
        assertEquals(responseBody, "Some simple page response");
    }

    /**
     * A GET using some resource path parameters.
     *
     * The application we work with returns a body with the path parameters we have sent
     */
    @Test
    public void get_usingPathParams() {

        final String pathParam1 = "value1";
        final String pathParam2 = "value2";

        HttpClient client = new HttpClient(BASE_URI + "simplepage/");

        // add as many as needed path parameters
        client.addResourcePath(pathParam1);
        client.addResourcePath(pathParam2);

        HttpResponse response = client.get();

        assertEquals(response.getBodyAsString(),
                     "You passed " + pathParam1 + " and " + pathParam2 + " path params");
    }

    /**
     * A GET using some resource request(query) parameters.
     *
     * The application we work with returns a body with the request parameters we have sent
     */
    @Test
    public void get_usingRequestParams() {

        final String requestParam1Name = "from";
        final String requestParam1Value = "10";

        final String requestParam2Name = "to";
        final String requestParam2Value = "20";

        HttpClient client = new HttpClient(BASE_URI + "simplepage/query/");

        // add request parameters by specifying their name and value
        client.addRequestParameter(requestParam1Name, requestParam1Value);
        client.addRequestParameter(requestParam2Name, requestParam2Value);

        HttpResponse response = client.get();

        assertEquals(response.getBodyAsString(),
                     "You passed " + requestParam1Name + " " + requestParam1Value + " " + requestParam2Name
                     + " " + requestParam2Value + " request params");
    }

    /**
     * A GET using some request header.
     *
     * The application we work with returns a body with the header we have sent
     */
    @Test
    public void get_usingHeader() {

        final String header1 = "value1";

        HttpClient client = new HttpClient(BASE_URI + "simplepage/header");

        // add some important header(s)
        client.addRequestHeader("header1", header1);

        HttpResponse response = client.get();

        assertEquals(response.getBodyAsString(),
                     "You passed header with name header1 and value " + header1);
    }

    /**
     * A GET which returns a stream with data.
     *
     * This particular body is small, so we can save the content into a String,
     * but usual case would be to stream it on the file system
     */
    @Test
    public void get_downloadFileAsStream() throws IOException {

        HttpClient client = new HttpClient(BASE_URI + "simplepage/download");

        HttpResponse response = client.get();
        response.verifyStatusCode(200);

        // check the value of the content disposition header
        HttpHeader contentDispositionHeader = response.getHeader("Content-Disposition");
        assertNotNull(contentDispositionHeader);
        assertTrue(contentDispositionHeader.getValue().startsWith("attachment"));

        // Read the returned body as an stream
        // In the usual case this could be a significant amount of data
        InputStream responseStream = new ByteArrayInputStream(response.getBody());

        // The following method reads into a String and closes the stream
        // Note: this method is not a public one. It can be change at any moment without notice
        String fileContent = IoUtils.streamToString(responseStream);

        assertTrue(fileContent.contains("file line 1"));
    }

    /**
     * A POST which sends form parameters.
     *
     * In the particular case on the server side a new person will be added to the list
     * of known persons.
     * It returns info about the newly added person.
     */
    @Test
    public void post_usingFormParameters() {

        HttpClient client = new HttpClient(BASE_URI + "peoplepage/post");
        client.setRequestMediaType("application/x-www-form-urlencoded");

        // add parameters' info
        client.addRequestParameter("firstName", "Chuck");
        client.addRequestParameter("lastName", "Norris");
        client.addRequestParameter("age", "70");

        // send the data
        HttpResponse response = client.post();
        // check the status code is "201 CREATED"
        response.verifyStatusCode(201);

        // check the returned result to make sure the server accepted the data we sent
        assertTrue(response.getBodyAsString()
                           .startsWith("Saved Person with name Chuck Norris at 70 years"));
    }

    /**
     * A PUT which sends form parameters.
     *
     * The PUT is usually used to modify an existing resource
     * Thus this test first issues a POST to create the resource which will
     * be modified(updated) by the following PUT
     */
    @Test
    public void put_usingFormParameters() {

        createPersonToManipulateInTheTest();

        // update the already existing person using PUT
        HttpClient client = new HttpClient(BASE_URI + "peoplepage/put");
        client.setRequestMediaType("application/x-www-form-urlencoded");

        client.addRequestParameter("firstName", "Chuck");
        client.addRequestParameter("lastName", "Norris");
        client.addRequestParameter("age", "71");
        HttpResponse response = client.put();

        assertEquals(response.getStatusCode(), 200);
        // verify Chuck Norris grows up as expected
        assertTrue(response.getBodyAsString()
                           .startsWith("Chuck Norris was 70 years old, but now is 71 years old"));
    }

    /**
     * A DELETE using path parameters.
     *
     * In this case all needed is to point to the resource to be deleted. 
     *
     * Our server expects the the first name of the person to be deleted.
     */
    @Test
    public void delete_usingPathParam() {

        createPersonToManipulateInTheTest();

        final String firstNameParam = "Chuck";

        HttpClient client = new HttpClient(BASE_URI + "peoplepage/delete");
        client.addResourcePath("Chuck");

        HttpResponse response = client.delete();

        assertEquals(response.getStatusCode(), 200);
        assertTrue(response.getBodyAsString()
                           .startsWith("Deleted person with first name " + firstNameParam));
    }

    /**
     * A DELETE using request parameters.
     *
     * Our server expects the the last name of the person to be deleted.
     */
    @Test
    public void delete_usingRequestParam() {

        createPersonToManipulateInTheTest();

        final String lastNameParam = "lastName";
        final String lastNameParamValue = "Norris";

        HttpClient client = new HttpClient(BASE_URI + "peoplepage/delete");

        // add the request parameter
        client.addRequestParameter(lastNameParam, lastNameParamValue);

        HttpResponse response = client.delete();

        assertEquals(response.getStatusCode(), 200);
        assertTrue(response.getBodyAsString()
                           .startsWith("Deleted person with last name " + lastNameParamValue));
    }

    /**
     * A GET which returns some JSON body.
     *
     * We read the response body as JsonText and use its common methods to retrieve the needed data
     */
    @Test
    public void get_json() {

        createPersonToManipulateInTheTest();

        HttpClient client = new HttpClient(BASE_URI + "peoplepage/get_jsonPojo");
        client.addResourcePath("Chuck");

        HttpResponse response = client.get();

        // get the body as JsonText
        JsonText jsonResponse = response.getBodyAsJsonText();

        // now call the different getter methods to retrieve the needed data
        String firstName = jsonResponse.getString("firstName");
        String lastName = jsonResponse.getString("lastName");
        int age = jsonResponse.getInt("age");

        log.info("Got back a JSON body saying that " + firstName + " " + lastName + " is " + age + " old");
        log.info("A JSON body looks like that:\n" + jsonResponse.toString());
        log.info("A well formatter JSON body looks like that:\n" + jsonResponse.toFormattedString());
    }

    /**
     * A GET which returns some XML body.
     *
     * We read the response body as XmlText and use its common methods to retrieve the needed data
     */
    @Test
    public void get_xml() {

        createPersonToManipulateInTheTest();

        HttpClient client = new HttpClient(BASE_URI + "peoplepage/get_xmlBean");
        client.addResourcePath("Chuck");
        client.addResourcePath("Norris");

        HttpResponse response = client.get();

        // get the body as XmlText
        XmlText xmlResponse = response.getBodyAsXmlText();

        // now call the different getter methods to retrieve the needed data
        String firstName = xmlResponse.getString("firstName");
        String lastName = xmlResponse.getString("lastName");
        int age = xmlResponse.getInt("age");

        log.info("Got back a XML body saying that " + firstName + " " + lastName + " is " + age + " old");
        log.info("A XML body looks like that:\n" + xmlResponse.toString());
        log.info("A well formatter XML body looks like that:\n" + xmlResponse.toFormattedString());
    }
}
