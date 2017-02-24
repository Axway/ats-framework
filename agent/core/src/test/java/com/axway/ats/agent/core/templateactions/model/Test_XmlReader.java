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
package com.axway.ats.agent.core.templateactions.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.axway.ats.agent.core.templateactions.TemplateActionsBaseTest;
import com.axway.ats.agent.core.templateactions.exceptions.XmlReaderException;
import com.axway.ats.agent.core.templateactions.model.objects.ActionHeader;

public class Test_XmlReader extends TemplateActionsBaseTest {

    private static final String                            TEST_ACTIONS_HOME = TEST_RESOURCES_HOME
                                                                               + "TestXmlReader/";

    /**
    * Keeps the data we will try to match against
    */
    private static final Map<String, List<MockHttpAction>> expectedActionsMap;
    static {
        expectedActionsMap = new HashMap<String, List<MockHttpAction>>();

        List<MockHttpAction> httpMessageWithFileResponseActions = new ArrayList<MockHttpAction>();
        MockHttpAction httpMessageWithFileResponseAction = new MockHttpAction();
        httpMessageWithFileResponseAction.setUrl( "http://${targetHost}:8080/webtop/api/website/8a4581842916cac2012916cb69c90165/resource/gfx/H2HMFT.png" );
        httpMessageWithFileResponseAction.setHttpMethod( "GET" );
        httpMessageWithFileResponseAction.addRequestHeader( "Host", "${targetHost}:8080" );
        httpMessageWithFileResponseActions.add( httpMessageWithFileResponseAction );
        expectedActionsMap.put( "httpMessageWithReceivedFile", httpMessageWithFileResponseActions );

        List<MockHttpAction> httpMessageSendingFileActions = new ArrayList<MockHttpAction>();
        MockHttpAction httpMessageSendingFileAction1 = new MockHttpAction();
        httpMessageSendingFileAction1.setUrl( "http://${targetHost}:8080/com/axway/cs/h2h/data/${senderMessageAttachmentId}" );
        httpMessageSendingFileAction1.setHttpMethod( "PUT" );
        httpMessageSendingFileAction1.addRequestHeader( "Accept",
                                                        "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2" );
        httpMessageSendingFileAction1.setRequestResourceFile( "HTTP_FILE_1.dat" );
        httpMessageSendingFileActions.add( httpMessageSendingFileAction1 );
        expectedActionsMap.put( "httpMessageSendingFile", httpMessageSendingFileActions );

        List<MockHttpAction> skippedHeadersActions = new ArrayList<MockHttpAction>();
        MockHttpAction skippedHeadersAction1 = new MockHttpAction();
        skippedHeadersAction1.setUrl( "http://${targetHost}:8080/com/axway/cs/h2h/data/${senderMessageAttachmentId}" );
        skippedHeadersAction1.setHttpMethod( "PUT" );
        skippedHeadersAction1.addRequestHeader( "Accept",
                                                "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2" );
        skippedHeadersAction1.setRequestResourceFile( "HTTP_FILE_1.dat" );
        skippedHeadersActions.add( skippedHeadersAction1 );
        expectedActionsMap.put( "skippedHeaders", skippedHeadersActions );

    }

    @Test
    public void httpMessageWithReceivedFile() throws Exception {

        processMessage();
    }

    @Test
    public void httpMessageSendingFile() throws Exception {

        processMessage();
    }

    @Test
    public void skippedHeaders() throws Exception {

        // we currently skip the "Transfer-Encoding"="chunked" header
        processMessage();
    }

    @Test
    public void noUrlSpecified() throws Exception {

        try {
            processMessage();
            assertTrue( false );
        } catch( XmlReaderException e ) {
            assertTrue( e.getCause().getMessage().endsWith( "No HTTP_REQUEST_URL node" ) );
        }
    }

    @Test
    public void noRequestMethodSpecified() throws Exception {

        try {
            processMessage();
            assertTrue( false );
        } catch( XmlReaderException e ) {
            assertTrue( e.getCause().getMessage().contains( "No method attribute in <HTTP_REQUEST>" ) );
        }
    }

    private void processMessage() throws Exception {

        resolveActionName( true );

        String actionsXml = TEST_ACTIONS_HOME + this.actionName + ".xml";
        log.info( "Testing over: " + actionsXml );

        XmlReader xmlReader = new XmlReader( actionsXml );

        List<MockHttpAction> actualActions = new ArrayList<MockHttpAction>();
        while( xmlReader.goToNextAction() ) {
            // REQUEST
            MockHttpAction actualAction = new MockHttpAction();
            System.out.println( xmlReader.getRequestHttpUrl() );
            actualAction.setUrl( xmlReader.getRequestHttpUrl() );
            actualAction.setHttpMethod( xmlReader.getRequestHttpMethod() );
            List<ActionHeader> requestHeaders = xmlReader.getRequestHttpHeaders();
            for( ActionHeader header : requestHeaders ) {
                actualAction.addRequestHeader( header.getHeaderName(), header.getHeaderValue() );
            }

            actualAction.setRequestResourceFile( xmlReader.getRequestResourceFile() );

            // RESPONSE
            // there is no need to read the response as it is simple kept a Node, no manipulations are made at all

            // next methods called just in order to increase the code coverage numbers
            xmlReader.isLastAction();
            assertNotNull( xmlReader.getResponse() );

            // add to the list of actions
            actualActions.add( actualAction );
        }

        verifyActionsList( this.actionName, actualActions );
    }

    private void verifyActionsList( String testName, List<MockHttpAction> actualActions ) {

        log.info( "Verify the actions list of: " + testName );

        List<MockHttpAction> expectedActions = expectedActionsMap.get( testName );
        assertNotNull( expectedActions );

        assertEquals( expectedActions.size(), actualActions.size() );
        for( int i = 0; i < expectedActions.size(); i++ ) {
            log.info( "Verify action number: " + ( i + 1 ) );
            verifyActions( expectedActions.get( i ), actualActions.get( i ) );
        }
    }

    private void verifyActions( MockHttpAction expectedAction, MockHttpAction actualAction ) {

        // CHECK REQUEST
        // connection parameters
        assertEquals( expectedAction.getUrl(), actualAction.getUrl() );
        assertEquals( expectedAction.getHttpMethod(), actualAction.getHttpMethod() );

        // headers
        List<ActionHeader> expectedRequestHeaders = expectedAction.getRequestHeaders();
        List<ActionHeader> actualRequestHeaders = actualAction.getRequestHeaders();
        assertEquals( expectedRequestHeaders.size(), actualRequestHeaders.size() );

        for( ActionHeader expectedHreader : expectedRequestHeaders ) {
            for( ActionHeader actualHreader : actualRequestHeaders ) {
                if( expectedHreader.getHeaderName().equalsIgnoreCase( actualHreader.getHeaderName() ) ) {

                    assertEquals( expectedHreader.getHeaderName(), actualHreader.getHeaderName() );
                    break;
                }
            }
        }

        assertEquals( expectedAction.getRequestObjectString(), actualAction.getRequestObjectString() );

        // file to send
        if( actualAction.getRequestResourceFile() != null ) {
            assertTrue( actualAction.getRequestResourceFile()
                                    .endsWith( expectedAction.getRequestResourceFile() ) );
        }

        // CHECK RESPONSE
    }
}

class MockHttpAction {

    MockHttpAction() {

    }

    // request data
    private String             url;
    private String             httpMethod;
    private List<ActionHeader> requestHeaders       = new ArrayList<ActionHeader>();
    private String             requestObjectString  = "";
    private String             requestResourceFile;

    // response data
    private String             httpResponseStatusLine;
    private List<ActionHeader> responseHeaders      = new ArrayList<ActionHeader>();
    private String             responseObjectString = "";

    String getUrl() {

        return url;
    }

    void setUrl( String url ) {

        this.url = url;
    }

    String getHttpMethod() {

        return httpMethod;
    }

    void setHttpMethod( String httpMethod ) {

        this.httpMethod = httpMethod;
    }

    List<ActionHeader> getRequestHeaders() {

        return requestHeaders;
    }

    void addRequestHeader( String name, String value ) {

        requestHeaders.add( new ActionHeader( name, value ) );
    }

    String getHttpResponseStatusLine() {

        return httpResponseStatusLine;
    }

    void setHttpResponseStatusLine( String httpResponseStatusLine ) {

        this.httpResponseStatusLine = httpResponseStatusLine;
    }

    List<ActionHeader> getResponseHeaders() {

        return responseHeaders;
    }

    void addResponseHeader( String name, String value ) {

        responseHeaders.add( new ActionHeader( name, value ) );
    }

    String getRequestObjectString() {

        return requestObjectString;
    }

    void setRequestObjectString( String requestObjectString ) {

        this.requestObjectString = requestObjectString;
    }

    void setRequestObject( Object requestObject ) {

        if( requestObject != null ) {
            this.requestObjectString = requestObject.toString();
        }
    }

    String getResponseObjectString() {

        return responseObjectString;
    }

    void setResponseObjectString( String responseObjectString ) {

        this.responseObjectString = responseObjectString;
    }

    String getRequestResourceFile() {

        return requestResourceFile;
    }

    void setRequestResourceFile( String requestResourceFile ) {

        this.requestResourceFile = requestResourceFile;
    }
}
