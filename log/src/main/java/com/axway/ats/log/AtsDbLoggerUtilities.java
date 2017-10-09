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
package com.axway.ats.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.log.appenders.ActiveDbAppender;

@PublicAtsApi
public class AtsDbLoggerUtilities {
    
    private static final Logger logger            = Logger.getLogger( AtsDbLoggerUtilities.class );

    private final long          MAX_FILE_SIZE  = 10 * 1024 * 1024;                                             // 10MB

    private static String       ERR_MSG_PREFIX = "Cannot not attach file \"{FILE}\" to the current testcase: ";

    /**
     * Attach a local file to the current test case in the Test Explorer DB.
     * </br>It is expected to have Test Explorer running on port 80.
     * </br>The file must not be bigger than 10MB
     * 
     * @param fileLocation the absolute path to the file
     * @param testExplorerContextName the name of the web application, e.g. "TestExplorer" or "TestExplorer-3.11.0" etc.
     * @return TRUE if the operation was successful and false if not. A warning will be logged on failure.
     */
    @PublicAtsApi
    public boolean attachFileToCurrentTest(
                                            String fileLocation,
                                            String testExplorerContextName ) {

        return attachFileToCurrentTest( fileLocation, testExplorerContextName, 80 );
    }

    /**
     * Attach a local file to the current test case in the Test Explorer DB.
     * </br>The file must not be bigger than 10MB
     * 
     * @param fileLocation the absolute path to the file
     * @param testExplorerContextName the name of the web application, e.g. "TestExplorer" or "TestExplorer-3.11.0" etc.
     * @param testExplorerPort the port of the web application, e.g. 8080
     * @return TRUE if the operation was successful and false if not. A warning will be logged on failure.
     */
    @PublicAtsApi
    public boolean attachFileToCurrentTest(
                                            String fileLocation,
                                            String testExplorerContextName,
                                            int testExplorerPort ) {

        ERR_MSG_PREFIX = ERR_MSG_PREFIX.replace( "{FILE}", fileLocation );

        if( !checkFileExist( fileLocation ) ) {
            return false;
        }
        if( !checkFileSizeIsNotTooLarge( fileLocation ) ) {
            return false;
        }

        ActiveDbAppender dbAppender = ActiveDbAppender.getCurrentInstance();
        if( dbAppender == null ) {
            logger.warn( ERR_MSG_PREFIX + "Perhaps the database logging is turned off" );
            return false;
        }

        final int runId = dbAppender.getRunId();
        final int suiteId = dbAppender.getSuiteId();
        final int testcaseId = dbAppender.getTestCaseId();

        if( runId < 1 || suiteId < 1 || testcaseId < 1 ) {
            logger.warn( ERR_MSG_PREFIX
                         + "Perhaps the database logging is turned off or you are trying to log while a testcase is not yet started" );
            return false;
        }

        final String database = dbAppender.getDatabase();
        final String host = dbAppender.getHost();
        final String URL = "http://" + host + ":" + testExplorerPort + "/" + testExplorerContextName
                           + "/AttachmentsServlet";

        URL url = null;
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost( URL );
            url = post.getURI().toURL();

            if( !isURLConnetionAvailable( url ) ) {
                return false;
            }
            logger.debug( "POSTing " + fileLocation + " on " + URL );

            File file = new File( fileLocation );
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            builder.setMode( HttpMultipartMode.BROWSER_COMPATIBLE );
            builder.addBinaryBody( "upfile", file, ContentType.DEFAULT_BINARY, fileLocation );
            builder.addTextBody( "dbName", database );
            builder.addTextBody( "runId", Integer.toString( runId ) );
            builder.addTextBody( "suiteId", Integer.toString( suiteId ) );
            builder.addTextBody( "testcaseId", Integer.toString( testcaseId ) );

            HttpEntity entity = builder.build();
            post.setEntity( entity );
            checkPostExecutedSuccessfully( client.execute( post ), fileLocation );
        } catch( FileNotFoundException fnfe ) {
            logger.warn( ERR_MSG_PREFIX + "it does not exist on the local file system", fnfe );
            return false;
        } catch( ClientProtocolException cpe ) {
            logger.warn( ERR_MSG_PREFIX + "Upload to \"" + url + "\" failed", cpe );
            return false;
        } catch( ConnectException ce ) {
            logger.warn( ERR_MSG_PREFIX + "Upload to \"" + url + "\" failed", ce );
            return false;
        } catch( IOException ioe ) {
            logger.warn( ERR_MSG_PREFIX + "Upload to \"" + url + "\" failed", ioe );
            return false;
        }

        logger.info( "Successfully attached \"" + fileLocation + "\" to the current Test Explorer testcase" );
        return true;
    }

    private boolean checkFileExist(
                                    String fileLocation ) {

        boolean exists = new LocalFileSystemOperations().doesFileExist( fileLocation );

        if( !exists ) {
            logger.warn( ERR_MSG_PREFIX + "it does not exist on the local file system" );
        }
        return exists;
    }

    private boolean checkFileSizeIsNotTooLarge(
                                                String fileLocation ) {

        long fileSize = new LocalFileSystemOperations().getFileSize( fileLocation );
        boolean goodSize = fileSize <= MAX_FILE_SIZE;

        if( !goodSize ) {
            logger.warn( ERR_MSG_PREFIX + "as its size of \"" + fileSize
                         + "\" bytes is larger than the allowed 10MB" );
        }

        return goodSize;
    }

    private void checkPostExecutedSuccessfully(
                                                HttpResponse response,
                                                String fileLocation ) {

        if( response.getStatusLine().getStatusCode() != 200 ) {
            logger.warn( "File \"" + fileLocation
                         + "\" will not be attached to the current test, due to error in saving the file. " );
        }
    }

    private boolean isURLConnetionAvailable(
                                             URL url ) {

        try {
            HttpURLConnection cc = ( HttpURLConnection ) url.openConnection();
            cc.setRequestMethod( "HEAD" );

            if( cc.getResponseCode() != 200 ) {
                logger.warn( ERR_MSG_PREFIX + "Upload URL \"" + url + "\" is not defined right" );
                return false;
            }
        } catch( MalformedURLException mue ) {
            logger.warn( ERR_MSG_PREFIX + "Upload URL \"" + url + "\" is malformed", mue );
            return false;
        } catch( IOException ioe ) {
            logger.warn( ERR_MSG_PREFIX + "POST failed to URL \"" + url + "\"", ioe );
            return false;
        }
        return true;
    }
}
