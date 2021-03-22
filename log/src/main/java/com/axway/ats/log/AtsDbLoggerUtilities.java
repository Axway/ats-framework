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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.log.appenders.ActiveDbAppender;

/**
 * Utility methods for attaching info into ATS TestExplorer database. <br />
 * <em>Note</em>: Currently it is expected that DB and TestExplorer application run on the same host.
 */
@PublicAtsApi
public class AtsDbLoggerUtilities {

    private static final Logger logger                     = LogManager.getLogger(AtsDbLoggerUtilities.class);

    private final long          MAX_FILE_SIZE              = 10 * 1024 * 1024;                                                // 10MB

    private static String       ERR_MSG_PREFIX             = "Cannot attach file \"{FILE}\" to the testcase \"{testcaseID}\"";

    private static int          DEFAULT_TEST_EXPLORER_PORT = 80;

    private String              currentErrMsgPrefix        = null;

    /**
     * Attach a local file to the current test case in the Test Explorer DB.
     * <br>It is expected to have Test Explorer running on port 80.
     * <br>The file must not be bigger than 10MB
     * 
     * @param fileLocation the absolute path to the file
     * @param testExplorerContextName the name of the web application, e.g. "TestExplorer" or "TestExplorer-4.0.0" etc.
     * @return TRUE if the operation was successful and false if not. A warning will be logged on failure.
     */
    @PublicAtsApi
    public boolean attachFileToCurrentTest(
                                            String fileLocation,
                                            String testExplorerContextName ) {

        return attachFileToCurrentTest(fileLocation, testExplorerContextName, DEFAULT_TEST_EXPLORER_PORT);
    }

    /**
     * Attach a local file to the current test case in the Test Explorer DB.
     * <br>The file must not be bigger than 10MB
     * 
     * @param fileLocation the absolute path to the file
     * @param testExplorerContextName the name of the web application context, e.g. "TestExplorer" or "TestExplorer-4.0.0" etc.
     * @param testExplorerPort the port of the web application, e.g. 8080
     * @return TRUE if the operation was successful and false if not. A warning will be logged on failure.
     */
    @PublicAtsApi
    public boolean attachFileToCurrentTest(
                                            String fileLocation,
                                            String testExplorerContextName,
                                            int testExplorerPort ) {

        return attachFileToTestcase(ActiveDbAppender.getCurrentInstance().getTestCaseId(), fileLocation,
                                    testExplorerContextName, testExplorerPort);
    }

    /**
     * Attach( upload) a local file to the a testcase in the Test Explorer DB.
     * <br>It is expected to have Test Explorer running on port 80 or alternatively use {{@link #attachFileToTestcase(int, String, String, int)}}
     * <br>The file must not be bigger than 10MB
     * 
     * @param testcaseId the testcase ID to which the file will be attached
     * @param fileLocation the absolute path to the file
     * @param testExplorerContextName the name of the web application context, e.g. "TestExplorer" or "TestExplorer-4.0.0" etc.
     * @return TRUE if the operation was successful and false if not. A warning will be logged on failure.
     */
    @PublicAtsApi
    public boolean attachFileToTestcase( int testcaseId,
                                         String fileLocation,
                                         String testExplorerContextName ) {

        return attachFileToTestcase(testcaseId, fileLocation, testExplorerContextName, DEFAULT_TEST_EXPLORER_PORT);
    }

    /**
     * Attach a local file to the a testcase in the Test Explorer DB.
     * <br>The file must not be bigger than 10MB
     * 
     * @param testcaseId the testcase id to which the file will be attached
     * @param fileLocation the absolute path to the file
     * @param testExplorerContextName the name of the web application, e.g. "TestExplorer" or "TestExplorer-4.0.0" etc.
     * @param testExplorerPort the port of the web application, e.g. 8080
     * @return TRUE if the operation was successful and false if not. A warning will be logged on failure.
     */
    @PublicAtsApi
    public boolean attachFileToTestcase( int testcaseId,
                                         String fileLocation,
                                         String testExplorerContextName,
                                         int testExplorerPort ) {

        fileLocation = fileLocation.replace("\\", "/");
        currentErrMsgPrefix = ERR_MSG_PREFIX.replace("{FILE}", fileLocation).replace("{testcaseID}", testcaseId + "");

        if (!checkFileExist(fileLocation)) {
            return false;
        }
        if (!checkFileSizeIsNotTooLarge(fileLocation)) {
            return false;
        }

        ActiveDbAppender dbAppender = ActiveDbAppender.getCurrentInstance();
        if (dbAppender == null) {
            logger.warn(currentErrMsgPrefix + ". Perhaps the database logging is turned off");
            return false;
        }

        final int runId = dbAppender.getRunId();
        final int suiteId = dbAppender.getSuiteId();

        /* Since the user provides testcase ID, we have to validate it - whether it refers to a testcase part of 
         * the current run and suite
         */
        if (runId < 1 || suiteId < 1 || testcaseId < 1) {
            logger.warn(currentErrMsgPrefix + ". Perhaps the database logging is turned off or you are trying to "
                        + "log while a testcase is not yet started");
            return false;
        }

        final String database = dbAppender.getDatabase();
        final String host = dbAppender.getHost();
        final String URL = "http://" + host + ":" + testExplorerPort + "/" + testExplorerContextName
                           + "/AttachmentsServlet";

        URL url = null;
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(URL);
            url = post.getURI().toURL();

            if (!isURLConnetionAvailable(url)) {
                return false;
            }
            logger.debug("POSTing " + fileLocation + " to " + URL);

            File file = new File(fileLocation);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addBinaryBody("upfile", file, ContentType.DEFAULT_BINARY, fileLocation);
            builder.addTextBody("dbName", database);
            builder.addTextBody("runId", Integer.toString(runId));
            builder.addTextBody("suiteId", Integer.toString(suiteId));
            builder.addTextBody("testcaseId", Integer.toString(testcaseId));

            HttpEntity entity = builder.build();
            post.setEntity(entity);
            return checkPostExecutedSuccessfully(client.execute(post), fileLocation, testcaseId);
        } catch (FileNotFoundException fnfe) {
            logger.warn(currentErrMsgPrefix + ". It does not exist on the local file system", fnfe);
            return false;
        } catch (IOException ioe) {
            logger.warn(currentErrMsgPrefix + ". Upload to \"" + url + "\" failed", ioe);
            return false;
        }
    }

    private boolean checkFileExist(
                                    String fileLocation ) {

        boolean exists = new LocalFileSystemOperations().doesFileExist(fileLocation);

        if (!exists) {
            logger.warn(currentErrMsgPrefix + ". It does not exist on the local file system.");
        }
        return exists;
    }

    private boolean checkFileSizeIsNotTooLarge(
                                                String fileLocation ) {

        long fileSize = new LocalFileSystemOperations().getFileSize(fileLocation);
        boolean goodSize = fileSize <= MAX_FILE_SIZE;

        if (!goodSize) {
            logger.warn(currentErrMsgPrefix + ". Its size of \"" + fileSize
                        + "\" bytes is larger than the max allowed " + MAX_FILE_SIZE);
        }

        return goodSize;
    }

    private boolean checkPostExecutedSuccessfully(
                                                   HttpResponse response,
                                                   String fileLocation,
                                                   int testcaseId ) {

        if (response.getStatusLine().getStatusCode() != 200) {
            try {
                List<String> lines = IOUtils.readLines(response.getEntity().getContent());
                for (String line : lines) {
                    logger.info(line);
                }
            } catch (Exception e) {
                logger.error("Error while reading response for file attach", e);
            }
            logger.error("File attach error for file " + fileLocation);
            return false;
        } else {
            logger.info("Successfully attached \"" + fileLocation + "\" to testcase with ID \"" + testcaseId + "\"");
            return true;
        }
    }

    private boolean isURLConnetionAvailable(
                                             URL url ) {

        try {
            HttpURLConnection cc = (HttpURLConnection) url.openConnection();
            cc.setRequestMethod("HEAD");

            if (cc.getResponseCode() != 200) {
                logger.error(currentErrMsgPrefix + ". Upload URL \"" + url + "\" is not defined right. Check TestExplorer's "
                            + "context name, HTTP port and host/IP. Details: Connect successful but test HEAD request "
                            + "received HTTP status code " + cc.getResponseCode() + " instead of expected 200 (OK).");
                return false;
            }
        } catch (MalformedURLException mue) {
            logger.error(currentErrMsgPrefix + ". Upload URL \"" + url + "\" is malformed", mue);
            return false;
        } catch (IOException ioe) {
            logger.error(currentErrMsgPrefix + ". Check request to URL \"" + url + "\" failed", ioe);
            return false;
        }
        return true;
    }
}
