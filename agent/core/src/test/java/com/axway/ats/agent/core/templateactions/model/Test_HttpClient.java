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

import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.axway.ats.agent.core.action.SampleTemplateActionClass;
import com.axway.ats.agent.core.context.ThreadContext;
import com.axway.ats.agent.core.templateactions.TemplateActionsBaseTest;
import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateHeaderMatchMode;
import com.axway.ats.agent.core.templateactions.model.objects.ActionParser;
import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;

public class Test_HttpClient extends TemplateActionsBaseTest {

    /** not final as overridden in sub test */
    protected static Class<?> TEST_ACTIONS_CLASS = SampleTemplateActionClass.class;

    protected static String   TEST_ACTIONS_HOME  = TEST_RESOURCES_HOME + TEST_COMPONENT_NAME + "/"
                                                   + TEST_ACTIONS_CLASS.getSimpleName() + "/";

    @Before
    public void before() {

        TEST_ACTIONS_HOME = TEST_RESOURCES_HOME + TEST_COMPONENT_NAME + "/"
                            + TEST_ACTIONS_CLASS.getSimpleName() + "/";
    }

    @After
    public void afterMethod() {

        ThreadContext.clear();
    }

    @Test
    public void httpMessagePng() throws Exception {

        httpMessage( "image/png", null, "png" );
    }

    @Test
    public void httpMessageJs() throws Exception {

        httpMessage( "text/javascript", null, "js" );
    }

    @Test
    public void httpMessageGif() throws Exception {

        httpMessage( "image/gif", null, "gif" );
    }

    @Test
    public void httpMessageSwf() throws Exception {

        httpMessage( "application/x-shockwave-flash", null, "swf" );
    }

    @Test
    public void httpMessageJar() throws Exception {

        httpMessage( "application/java-archive", null, "jar" );
    }

    @Test
    public void httpMessageBinary() throws Exception {

        httpMessage( "application/octet-stream", null, "dat" );
    }

    @Test
    public void httpMessageGzip() throws Exception {

        httpMessage( "text/html", "gzip", "html.gzip" );
    }

    @Test
    public void httpMessageFake() throws Exception {

        httpMessage( "fake/encoding", null, "bin" );
    }

    @Test
    public void testHeaders_moreThanOneSetCookieHeaders() throws Exception {

        // construct the fake client
        MockHttpURLConnection mockHttpURLConnection = new MockHttpURLConnection();

        Map<String, List<String>> fakeHeaderFields = new HashMap<String, List<String>>();
        List<String> setCookieHeaders = new ArrayList<String>();
        setCookieHeaders.add( "JSESSIONID=1dst5g74soql2;Path=/ui;HttpOnly" );
        setCookieHeaders.add( "JSESSIONID=pOsdf4L60Sd;Path=/ui;HttpOnly" );
        setCookieHeaders.add( "TRANSLATED_SESSIONID=\"ifoauo9iogmh1fzm510nwpd7l\"" );
        setCookieHeaders.add( "loggedInUserBeforeSessionExpired=admin; TRANSLATED_SESSIONID=\"SDklksdf8324nasdK\"" );
        fakeHeaderFields.put( "Set-Cookie", setCookieHeaders );
        mockHttpURLConnection.setFakeHeaderFields( fakeHeaderFields );

        HttpClient client = getHttpClient( mockHttpURLConnection );

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        Node responseNode = dom.createElement( TOKEN_HTTP_RESPONSE );
        dom.appendChild( responseNode );

        client.readHeaders( dom, responseNode, 1 );

        // verify the new Set-Cookie header merges all the Set-Cookie header in the right order
        XmlUtilities xmlUtilities = new XmlUtilities();
        String newSetCookieHeaderActual = xmlUtilities.getNodeAttribute( responseNode.getChildNodes()
                                                                                     .item( 0 ),
                                                                         "value" );
        String newSetCookieHeaderExpected = "JSESSIONID=1dst5g74soql2;Path=/ui;HttpOnly;JSESSIONID=pOsdf4L60Sd;Path=/ui;HttpOnly;TRANSLATED_SESSIONID=\"ifoauo9iogmh1fzm510nwpd7l\";loggedInUserBeforeSessionExpired=admin; TRANSLATED_SESSIONID=\"SDklksdf8324nasdK\";";
        assertEquals( newSetCookieHeaderExpected, newSetCookieHeaderActual );

        // now check whether we are getting the last cookie value
        HeaderMatcher matcher = new HeaderMatcher( HeaderMatcher.SET_COOKIE_HEADER_NAME, "",
                                                   TemplateHeaderMatchMode.EXTRACT );

        assertTrue( matcher.performMatch( null, newSetCookieHeaderActual ) );

        assertEquals( "pOsdf4L60Sd",
                      ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "JSESSIONID" ) );
        assertEquals( "SDklksdf8324nasdK", ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX
                                                                       + "TRANSLATED_SESSIONID" ) );
        assertEquals( null, ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "Path" ) );
        assertEquals( null, ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "HttpOnly" ) );
    }

    @Test
    public void testHeaders() throws Exception {

        resolveActionName( false );

        // construct the fake client
        MockHttpURLConnection mockHttpURLConnection = new MockHttpURLConnection();
        mockHttpURLConnection.setFakeInputStream( TEST_ACTIONS_HOME, actionName + "_response.bin" );
        mockHttpURLConnection.setFakeContentType( "text/javascript" );

        // add some headers
        Map<String, List<String>> fakeHeaderFields = new HashMap<String, List<String>>();
        // User-Agent header
        List<String> userAgentHeaders = new ArrayList<String>();
        userAgentHeaders.add( "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3" );
        fakeHeaderFields.put( "User-Agent", userAgentHeaders );
        // Accept-Charset header
        List<String> acceptCharset = new ArrayList<String>();
        acceptCharset.add( "ISO-8859-1,utf-8;q=0.7,*;q=0.7" );
        fakeHeaderFields.put( "Accept-Charset", acceptCharset );
        // Content-Length header
        List<String> contentLength = new ArrayList<String>();
        contentLength.add( "541" );
        fakeHeaderFields.put( "Content-Length", contentLength );
        // Status header
        List<String> responseStatusHeaders = new ArrayList<String>();
        responseStatusHeaders.add( "200 OK" );
        fakeHeaderFields.put( null, responseStatusHeaders );
        mockHttpURLConnection.setFakeHeaderFields( fakeHeaderFields );

        HttpClient client = getHttpClient( mockHttpURLConnection );
        XmlUtilities xmlUtilities = new XmlUtilities();

        String downloadsFolder = getDownloadsFolder();
        ActionParser actionParser = xmlUtilities.readActionResponse( client,
                                                                     downloadsFolder + actionName + ".xml", 1,
                                                                     false );
        Node actualResponseNode = actionParser.getActionNodeWithoutBody();
        String actualResponseBody = xmlUtilities.xmlNodeToString( actualResponseNode );
        String expectedResponseBody = readFileLineByLine( TEST_ACTIONS_HOME + actionName + "_response.xml" );

        verifyResponseMatch( expectedResponseBody, actualResponseBody, "response body", "response bodies" );
    }

    private void httpMessage( String contentType, String contentEncoding,
                              String downloadFileExtension ) throws Exception {

        resolveActionName( true );

        // construct the fake client
        MockHttpURLConnection mockHttpURLConnection = new MockHttpURLConnection();
        String expectedResponseFile = mockHttpURLConnection.setFakeInputStream( TEST_ACTIONS_HOME,
                                                                                actionName + "_response.bin" );
        mockHttpURLConnection.setFakeContentType( contentType );
        mockHttpURLConnection.setFakeContentEncoding( contentEncoding );

        HttpClient client = getHttpClient( mockHttpURLConnection );
        XmlUtilities xmlUtilities = new XmlUtilities();

        String downloadsFolder = getDownloadsFolder();
        /*Node actualResponseNode = xmlUtilities.readActionResponse( client, true, downloadsFolder + actionName
                                                                                 + ".xml", 1, false );
        String actualResponseBody = xmlUtilities.xmlToString( actualResponseNode );*/
        ActionParser actionParser = xmlUtilities.readActionResponse( client,
                                                                     downloadsFolder + actionName + ".xml", 1,
                                                                     false );
        Node actualResponseNode = actionParser.getActionNodeWithoutBody();
        String actualResponseBody = xmlUtilities.xmlNodeToString( actualResponseNode );
        actualResponseBody = actualResponseBody.replace( "<HTTP_RESPONSE>", "" )
                                               .replace( "</HTTP_RESPONSE>", "" )
                                               .trim();
        String expectedResponseBody = readFileLineByLine( TEST_ACTIONS_HOME + actionName + "_response.xml" );
        verifyResponseMatch( expectedResponseBody, actualResponseBody, "response body", "response bodies" );
        verifyDownloadedFileMatch( expectedResponseFile,
                                   downloadsFolder + actionName + "/actual/"
                                                         + Thread.currentThread().getName() + "/HTTP_FILE_1."
                                                         + downloadFileExtension );
    }

    private void verifyDownloadedFileMatch( String expectedFile, String actualFile ) throws IOException {

        // check the file contents
        String actualFileMD5Sum;
        String expectedFileMD5Sum;
        try {
            LocalFileSystemOperations lfso = new LocalFileSystemOperations();
            actualFileMD5Sum = lfso.computeMd5Sum( actualFile, Md5SumMode.BINARY );
            expectedFileMD5Sum = lfso.computeMd5Sum( expectedFile, Md5SumMode.BINARY );
        } catch( FileSystemOperationException fsoe ) {
            throw new RuntimeException( "Error calculating MD5 sum", fsoe );
        }

        if( !actualFileMD5Sum.equals( expectedFileMD5Sum ) ) {
            throw new RuntimeException( "The " + actualFile + " file is different than the expected "
                                        + expectedFile );
        } else {
            log.info( "Matched actual '" + actualFile + "' and expected '" + expectedFile + "' files" );
        }
    }
}
