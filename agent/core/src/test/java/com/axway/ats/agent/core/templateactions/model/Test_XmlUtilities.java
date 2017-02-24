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

import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.axway.ats.agent.core.configuration.TemplateActionsResponseVerificationConfigurator;
import com.axway.ats.agent.core.context.ThreadContext;
import com.axway.ats.agent.core.templateactions.TemplateActionsBaseTest;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;
import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateHeaderMatchMode;
import com.axway.ats.agent.core.templateactions.model.objects.ActionParser;
import com.axway.ats.common.systemproperties.AtsSystemProperties;

public class Test_XmlUtilities extends TemplateActionsBaseTest {

    private final static String TEST_ACTIONS_HOME = TEST_RESOURCES_HOME + "TestXmlUtilities/";
    private Document            doc;

    @Before
    public void beforeMethod() throws Exception {

        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    @After
    public void afterMethod() {

        ThreadContext.clear();
    }

    @Test
    public void getFirstChildNode() throws Exception {

        Node parentNode = doc.createElement( "parent" );
        Node childNode1 = doc.createElement( "child1" );
        Node childNode2 = doc.createElement( "child2" );
        parentNode.appendChild( childNode1 );
        parentNode.appendChild( childNode2 );

        Node actualChild1 = XmlUtilities.getFirstChildNode( parentNode, "child1" );
        assertEquals( "child1", actualChild1.getNodeName() );

        Node actualChild2 = XmlUtilities.getFirstChildNode( parentNode, "child2" );
        assertEquals( "child2", actualChild2.getNodeName() );

        assertNull( XmlUtilities.getFirstChildNode( parentNode, "child3" ) );
    }

    @Test
    public void getAllChildrenNodes() throws Exception {

        Element parentNode = doc.createElement( "parent" );

        Element childNode1 = doc.createElement( "child" );
        childNode1.setAttribute( "internalName", "This is child1" );
        parentNode.appendChild( childNode1 );

        // this node should be skipped
        Text textChild = doc.createTextNode( "SOME TEXT ELEMENT THAT SHOULD BE SKIPPED" );
        parentNode.appendChild( textChild );

        Element childNode2 = doc.createElement( "child" );
        childNode2.setAttribute( "internalName", "This is child2" );
        parentNode.appendChild( childNode2 );

        // get all by specifying a name
        Node[] children = XmlUtilities.getChildrenNodes( parentNode, "child" );

        assertEquals( 2, children.length );
        assertEquals( "child", children[0].getNodeName() );
        assertEquals( "This is child1", ( ( Element ) children[0] ).getAttribute( "internalName" ) );
        assertEquals( "child", children[1].getNodeName() );
        assertEquals( "This is child2", ( ( Element ) children[1] ).getAttribute( "internalName" ) );

        // get all without specifying a name
        children = XmlUtilities.getChildrenNodes( parentNode, null );

        assertEquals( 2, children.length );
        assertEquals( "child", children[0].getNodeName() );
        assertEquals( "This is child1", ( ( Element ) children[0] ).getAttribute( "internalName" ) );
        assertEquals( "child", children[1].getNodeName() );
        assertEquals( "This is child2", ( ( Element ) children[1] ).getAttribute( "internalName" ) );
    }

    @Test
    public void getNodeAttribute() throws Exception {

        Element node = doc.createElement( TOKEN_HTTP_HEADER );
        node.setAttribute( "attributeName1", "attributeValue1" );
        node.setAttribute( "attributeName2", "attributeValue2" );

        XmlUtilities utils = new XmlUtilities();

        assertEquals( "attributeValue1", utils.getNodeAttribute( node, "attributeName1" ) );
        assertEquals( "attributeValue2", utils.getNodeAttribute( node, "attributeName2" ) );
        assertNull( utils.getNodeAttribute( node, "attributeName3" ) );
    }

    @Test
    public void applyUserParameters() throws Exception {

        ThreadContext.setAttribute( "hostIp", "127.0.0.1" );
        ThreadContext.setAttribute( "hostPort", "1000" );

        assertEquals( "http://127.0.0.1:1000/",
                      XmlUtilities.applyUserParameters( "http://${hostIp}:${hostPort}/" ) );
    }

    @Test
    public void applyUserParameters_usingValuesQueueForSingleParameter() throws Exception {

        ThreadContext.setAttribute( "serverHost", "127.0.0.1:8080" );
        ThreadContext.setAttribute( "value",
                                    new LinkedList<String>( Arrays.asList( new String[]{ "3", "4", "5",
                                                                                         "BCDefg" } ) ) );

        assertEquals( "http://127.0.0.1:8080/test/3/4/5/test.php\n" //
                      + "<host>127.0.0.1:8080</host>\n" //
                      + "<someId>BCDefg</someId>",
                      XmlUtilities.applyUserParameters( "http://${serverHost}/test/${value}/${value}/${value}/test.php\n"
                                                        + "<host>${serverHost}</host>\n"
                                                        + "<someId>${value}</someId>" ) );
    }

    @Test
    public void applyUserParameters_usingValuesQueueForSingleParameter_2() throws Exception {

        ThreadContext.setAttribute( "serverHost", "127.0.0.1:8080" );
        ThreadContext.setAttribute( "value",
                                    new LinkedList<String>( Arrays.asList( new String[]{ "3", "4", "5",
                                                                                         "6" } ) ) );

        assertEquals( "http://127.0.0.1:8080/test/3/4/test.php",
                      XmlUtilities.applyUserParameters( "http://${serverHost}/test/${value}/${value}/test.php" ) );
        assertEquals( "http://127.0.0.1:8080/test/5/6/test.php",
                      XmlUtilities.applyUserParameters( "http://${serverHost}/test/${value}/${value}/test.php" ) );
    }

    @Test
    public void applyUserParameters_usingValuesQueueForSingleParameter_negative() throws Exception {

        ThreadContext.setAttribute( "value",
                                    new LinkedList<String>( Arrays.asList( new String[]{ "3", "4" } ) ) );

        String errorMsg = "The number of parameters ${value} is more than the number of provided values for them.";
        try {
            XmlUtilities.applyUserParameters( "http://127.0.0.1/test/${value}/${value}/${value}/test.php" );
            fail( "Expecting exception with message: " + errorMsg );
        } catch( XmlUtilitiesException xue ) {
            assertEquals( errorMsg, xue.getMessage() );
        }
    }

    @Test
    public void applyUserParametersInCookieHeader() throws Exception {

        ThreadContext.setAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "JSESSIONID", "xdnf0batsesw" );
        ThreadContext.setAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "ppSession",
                                    "1349478258619:1349417059016:I3TDUCRht5LS" );

        XmlUtilities xmlUtilities = new XmlUtilities();

        String before = "JSESSIONID=1kjwnen12345; TRANSLATED_SESSIONID=0nwpd7l; ppSession=\"FSD001@8300576:134941:VL2OPg9jyc+Pzv/kt8rA==\"";
        String after = "JSESSIONID=\"xdnf0batsesw\"; TRANSLATED_SESSIONID=0nwpd7l; ppSession=\"1349478258619:1349417059016:I3TDUCRht5LS\"";

        assertEquals( after, xmlUtilities.applyUserParametersInCookieHeader( before ) );
    }

    @Test
    public void extractXpathEntries() throws Exception {

        doc = DocumentBuilderFactory.newInstance()
                                    .newDocumentBuilder()
                                    .parse( new File( TEST_ACTIONS_HOME + "extractXpathEntries.xml" ) );

        assertEquals( 0, XmlUtilities.extractXpathEntries( doc, new String[]{} ).length );

        String[][] ids = XmlUtilities.extractXpathEntries( doc,
                                                           new String[]{ "//HTTP_RESPONSE/AMF_OBJECT/body/AMF_OBJECT/id1",
                                                                         "//HTTP_RESPONSE/AMF_OBJECT/body/AMF_OBJECT/id2" } );
        assertEquals( 2, ids.length );

        String[] ids1 = ids[0];
        assertEquals( 2, ids1.length );
        assertEquals( "609", ids1[0] );
        assertEquals( "610", ids1[1] );

        String[] ids2 = ids[1];
        assertEquals( 1, ids2.length );
        assertEquals( "1000", ids2[0] );
    }

    @SuppressWarnings("serial")
    @Test
    public void verifyContentLengthZero() throws Exception {

        String actionsXmlFileNameWoPath = "verifyContentLengthZero.xml";
        String actionsXml = TEST_ACTIONS_HOME + actionsXmlFileNameWoPath;
        XmlReader xmlReader = new XmlReader( actionsXml );
        xmlReader.goToNextAction(); // non-zero content
        xmlReader.goToNextAction(); // empty body of the response/content

        TemplateActionsResponseVerificationConfigurator verificationConfigurator = new TemplateActionsResponseVerificationConfigurator( "queue name" );

        // add globally applicable header matchers
        final HeaderMatcher globalContentRange = new HeaderMatcher( "Content-Length", "100",
                                                                    TemplateHeaderMatchMode.RANGE_OFFSET );
        verificationConfigurator.addGlobalHeaderMatchers( new HashSet<HeaderMatcher>() {
            {
                add( globalContentRange );
            }
        } );

        resolveActionName( false );

        // construct the fake client
        MockHttpURLConnection mockHttpURLConnection = new MockHttpURLConnection();
        mockHttpURLConnection.setFakeInputStream( TEST_ACTIONS_HOME, actionName + "_response.bin" );
        mockHttpURLConnection.setFakeContentType( "text/html" );
        //mockHttpURLConnection.setFakeContentEncoding( contentEncoding );

        HttpClient client = Test_HttpClient.getHttpClient( mockHttpURLConnection );
        // add some headers
        Map<String, List<String>> fakeHeaderFields = new HashMap<String, List<String>>();
        List<String> acceptCharset = new ArrayList<String>();
        acceptCharset.add( "ISO-8859-1,utf-8;q=0.7,*;q=0.7" );
        fakeHeaderFields.put( "Accept-Charset", acceptCharset );
        // Content-Length header
        List<String> contentLength = new ArrayList<String>();
        contentLength.add( "0" );
        fakeHeaderFields.put( "Content-Length", contentLength );
        fakeHeaderFields.put( "Content-Type", new LinkedList<String>() {
            {
                add( "text/html" );
            }
        } );
        fakeHeaderFields.put( "Accept-Ranges", new LinkedList<String>() {
            {
                add( "bytes" );
            }
        } );
        fakeHeaderFields.put( "Server", new LinkedList<String>() {
            {
                add( "Apache-Coyote/1.1" );
            }
        } );

        // Status header
        List<String> responseStatusHeaders = new ArrayList<String>();
        responseStatusHeaders.add( "HTTP/1.1 200 OK" );

        fakeHeaderFields.put( null, responseStatusHeaders );
        mockHttpURLConnection.setFakeHeaderFields( fakeHeaderFields );

        XmlUtilities xmlUtilities = new XmlUtilities();
        ActionParser actionParser = xmlUtilities.readActionResponse( client, getDownloadsFolder() + actionName
                                                                             + ".xml",
                                                                     1, false );
        Node actualResponseNode = actionParser.getActionNodeWithoutBody();
        String actualResponse = xmlUtilities.xmlNodeToString( actualResponseNode );
        actualResponse = actualResponse.replace( "<HTTP_RESPONSE>", "" )
                                       .replace( "</HTTP_RESPONSE>", "" )
                                       .trim();
        String expectedResponse = readFileLineByLine( TEST_ACTIONS_HOME + actionName + "_response.xml" );
        verifyResponseMatch( expectedResponse, actualResponse, "response body", "response bodies" );

        // Verify against HeaderMatchers
        XmlUtilities utils = new XmlUtilities();
        // Output directory for actual response bodies. Ending ".xml" is removed in verifyResponse
        String outDirNamePathWithDotXML = getDownloadsFolder() + AtsSystemProperties.SYSTEM_FILE_SEPARATOR
                                          + actionsXmlFileNameWoPath;
        // reset the stop watch interim state - just before reading response
        client.getNetworkingStopWatch().step0_SetNewContext( actionsXmlFileNameWoPath /*some name*/ );
        client.getNetworkingStopWatch().setStateFromBeforeStep1ToAfterStep4();
        client.getNetworkingStopWatch().step5_StartInterimTimer();
        utils.verifyResponse( outDirNamePathWithDotXML, "verifyContentLengthZero", 1, xmlReader.getResponse(),
                              client, verificationConfigurator );

    }

//        @Test
//        public void wrongResponseResult() throws Exception {
//    
//            XmlReader expectedXmlReader = new XmlReader( TEST_ACTIONS_HOME + "response1.xml" );
//            expectedXmlReader.goToNextAction();
//    
//            XmlReader actualXmlReader = new XmlReader( TEST_ACTIONS_HOME + "wrongResponseResult.xml" );
//            actualXmlReader.goToNextAction();
//    
//            XmlUtilities utils = new XmlUtilities();
//    
//            try {
//                utils.verifyResponse( expectedXmlReader.getActionResponse(), actualXmlReader.getActionResponse() );
//                assertTrue( false );
//            } catch( XmlUtilitiesException e ) {
//                assertEquals( "expected response result '200 OK' is different than the actual '1200 OK'",
//                              e.getMessage().replace( "\n", "" ) );
//            }
//        }
    //
    //    @Test
    //    public void wrongNumberOfHeaders() throws Exception {
    //
    //        XmlReader expectedXmlReader = new XmlReader( TEST_ACTIONS_HOME + "response1.xml" );
    //        expectedXmlReader.goToNextAction();
    //
    //        XmlReader actualXmlReader = new XmlReader( TEST_ACTIONS_HOME + "wrongNumberOfHeaders.xml" );
    //        actualXmlReader.goToNextAction();
    //
    //        XmlUtilities utils = new XmlUtilities();
    //
    //        try {
    //            utils.verifyResponse( expectedXmlReader.getResponse(), actualXmlReader.getActionResponse() );
    //            assertTrue( false );
    //        } catch( XmlUtilitiesException e ) {
    //            assertEquals( "Expected 4 response headers, but got 3", e.getMessage() );
    //        }
    //    }
    //
    //    @Test
    //    public void wrongHeaderValue() throws Exception {
    //
    //        XmlReader expectedXmlReader = new XmlReader( TEST_ACTIONS_HOME + "response1.xml" );
    //        expectedXmlReader.goToNextAction();
    //
    //        XmlReader actualXmlReader = new XmlReader( TEST_ACTIONS_HOME + "wrongHeaderValue.xml" );
    //        actualXmlReader.goToNextAction();
    //
    //        XmlUtilities utils = new XmlUtilities();
    //
    //        try {
    //            utils.verifyResponse( expectedXmlReader.getActionResponse(), actualXmlReader.getActionResponse() );
    //            assertTrue( false );
    //        } catch( XmlUtilitiesException e ) {
    //            assertEquals( "Expected response header 'Content-Type' with value 'application/x-amf', but got 'wrong_header_value' value instead",
    //                          e.getMessage() );
    //        }
    //    }
    //
    //    @Test
    //    public void missingHeader() throws Exception {
    //
    //        XmlReader expectedXmlReader = new XmlReader( TEST_ACTIONS_HOME + "response1.xml" );
    //        expectedXmlReader.goToNextAction();
    //
    //        XmlReader actualXmlReader = new XmlReader( TEST_ACTIONS_HOME + "missingHeader.xml" );
    //        actualXmlReader.goToNextAction();
    //
    //        XmlUtilities utils = new XmlUtilities();
    //
    //        try {
    //            utils.verifyResponse( expectedXmlReader.getActionResponse(), actualXmlReader.getActionResponse() );
    //            assertTrue( false );
    //        } catch( XmlUtilitiesException e ) {
    //            assertEquals( "Did not receive the expected response header with name 'Cache-Control'",
    //                          e.getMessage() );
    //        }
    //    }
    //
    //    @Test
    //    public void missingResponseFile() throws Exception {
    //
    //        XmlReader expectedXmlReader = new XmlReader( TEST_ACTIONS_HOME + "responseWithFile.xml" );
    //        expectedXmlReader.goToNextAction();
    //
    //        XmlReader actualXmlReader = new XmlReader( TEST_ACTIONS_HOME + "response1.xml" );
    //        actualXmlReader.goToNextAction();
    //
    //        XmlUtilities utils = new XmlUtilities();
    //
    //        try {
    //            utils.verifyResponse( expectedXmlReader.getActionResponse(), actualXmlReader.getActionResponse() );
    //            assertTrue( false );
    //        } catch( XmlUtilitiesException e ) {
    //            assertEquals( "Expected to receive the HTTP_FILE_1.png file, but did not receive a response file",
    //                          e.getMessage() );
    //        }
    //    }
    //
    //    @Test
    //    public void unexpectedResponseFile() throws Exception {
    //
    //        XmlReader expectedXmlReader = new XmlReader( TEST_ACTIONS_HOME + "response1.xml" );
    //        expectedXmlReader.goToNextAction();
    //
    //        XmlReader actualXmlReader = new XmlReader( TEST_ACTIONS_HOME + "responseWithFile.xml" );
    //        actualXmlReader.goToNextAction();
    //
    //        XmlUtilities utils = new XmlUtilities();
    //
    //        try {
    //            utils.verifyResponse( expectedXmlReader.getActionResponse(), actualXmlReader.getActionResponse() );
    //            assertTrue( false );
    //        } catch( XmlUtilitiesException e ) {
    //            assertEquals( "Expected to not receive a response file, but received the HTTP_FILE_1.png file",
    //                          e.getMessage() );
    //        }
    //    }
    //
    //    @Test
    //    public void httpResponses() throws Exception {
    //
    //        XmlReader expectedXmlReader = new XmlReader( TEST_ACTIONS_HOME + "responseWithFile.xml" );
    //        expectedXmlReader.goToNextAction();
    //
    //        XmlReader actualXmlReader = new XmlReader( TEST_ACTIONS_HOME + "responseWithFile.xml" );
    //        actualXmlReader.goToNextAction();
    //
    //        XmlUtilities utils = new XmlUtilities();
    //        utils.verifyResponse( expectedXmlReader.getActionResponse(), actualXmlReader.getActionResponse() );
    //    }
    //
    //    @Test
    //    public void extractNewParametersFromResponse() throws Exception {
    //
    //        XmlReader expectedXmlReader = new XmlReader( TEST_ACTIONS_HOME
    //                                                     + "responseWithNewParameters_expected.xml" );
    //        expectedXmlReader.goToNextAction();
    //
    //        XmlReader actualXmlReader = new XmlReader( TEST_ACTIONS_HOME + "responseWithNewParameters_actual.xml" );
    //        actualXmlReader.goToNextAction();
    //
    //        XmlUtilities utils = new XmlUtilities();
    //
    //        assertEquals( null, ThreadContext.getAttribute( "new_id" ) );
    //        assertEquals( null, ThreadContext.getAttribute( "new_sender_mail" ) );
    //
    //        utils.verifyResponse( expectedXmlReader.getActionResponse(), actualXmlReader.getActionResponse() );
    //
    //        assertEquals( "1000", ThreadContext.getAttribute( "new_id" ) );
    //        assertEquals( "sender@test.com", ThreadContext.getAttribute( "new_sender_mail" ) );
    //    }
    //
    //    @Test
    //    public void wrongNumberOfHeaders() throws Exception {
    //
    //        //        XmlReader expectedXmlReader = new XmlReader( TEST_ACTIONS_HOME + "response1.xml" );
    //        //        expectedXmlReader.goToNextAction();
    //
    //        XmlReader actualXmlReader = new XmlReader( TEST_ACTIONS_HOME + "wrongNumberOfHeaders.xml" );
    //        actualXmlReader.goToNextAction();
    //
    //        XmlUtilities utils = new XmlUtilities();
    //        //
    //        //        try {
    //        //            utils.verifyResponse( expectedXmlReader.getResponse(), actualXmlReader.getActionResponse() );
    //        //            fail();
    //        //        } catch( XmlUtilitiesException e ) {
    //        //            assertEquals( "Expected 4 response headers, but got 3", e.getMessage() );
    //        //        }
    //    }
}
