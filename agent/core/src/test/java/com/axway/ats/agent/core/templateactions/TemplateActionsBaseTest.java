/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.agent.core.templateactions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.powermock.reflect.Whitebox;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.configuration.ConfigurationSettings;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.templateactions.model.HttpClient;
import com.axway.ats.agent.core.templateactions.model.objects.ActionHeader;
import com.axway.ats.common.agent.templateactions.NetworkingStopWatch;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;

public class TemplateActionsBaseTest extends BaseTest {

    protected static final Logger log                 = LogManager.getLogger(TemplateActionsBaseTest.class);

    protected static final String TEST_COMPONENT_NAME = "agentTestComponent";

    protected String              actionName;

    static {
        org.apache.logging.log4j.core.layout.PatternLayout layout = org.apache.logging.log4j.core.layout.PatternLayout.newBuilder()
                                                                                                                      .withPattern("%m%n")
                                                                                                                      .build();
        org.apache.logging.log4j.core.appender.ConsoleAppender appender = org.apache.logging.log4j.core.appender.ConsoleAppender.newBuilder()
                                                                                                                                .setLayout(layout)
                                                                                                                                .setName("ConsoleAppender")
                                                                                                                                .build();

        //init log4j
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        appender.start();
        config.addAppender(appender);
        // context.getRootLogger().addAppender(config.getAppender(appender.getName())); Is this needed?!?
        context.updateLoggers(); // TODO is this needed

        ConfigurationSettings.getInstance().setTemplateActionsMatchFilesByContent(true);
    }

    private static String getTestResourcesHome( String currentDirPath ) {

        File currentDir = new File(currentDirPath);
        while (currentDir != null) {
            if (currentDir.isDirectory()
                // Could be replaced with target/test-classes if Ant won't be used
                && new File(currentDir.getAbsolutePath() + "/" + RELATIVE_PATH_TO_TEST_RESOURCES
                            + "/com/").exists()) {

                return currentDir.getAbsolutePath() + "/" + RELATIVE_PATH_TO_TEST_RESOURCES + "/";
            }
            currentDir = currentDir.getParentFile();
        }
        throw new RuntimeException("Can't find the Project Root directory, searchig backward from dir: "
                                   + currentDirPath);
    }

    protected static final String TEST_RESOURCES_HOME;
    static {
        URL thisClassUrl = TemplateActionsBaseTest.class.getClassLoader()
                                                        .getResource(TemplateActionsBaseTest.class.getCanonicalName()
                                                                                                  .replaceAll("\\.",
                                                                                                              "/")
                                                                     + ".class");

        String resourcesHome = getTestResourcesHome(thisClassUrl.toString().substring("file:".length()));
        TEST_RESOURCES_HOME = resourcesHome + "com/axway/ats/agent/core/templateactions/";
    }

    private static final String MATCHER_STRING;
    private static final String MATCHER_DESCRIPTION_MULTY  = "___MATCHER_DESCRIPTION_MULTY___";
    private static final String MATCHER_DESCRIPTION_SINGLE = "___MATCHER_DESCRIPTION_SINGLE___";

    private static final String MATCHER_EXPECTED_VALUE     = "___MATCHER_EXPECTED_VALUE___";
    private static final String MATCHER_ACTUAL_VALUE       = "___MATCHER_ACTUAL_VALUE___";

    static {
        StringBuilder matcherString = new StringBuilder();
        matcherString.append("Check error log. The actual and expected ");
        matcherString.append(MATCHER_DESCRIPTION_MULTY);
        matcherString.append(" are not the same:");

        matcherString.append("\nExpected ");
        matcherString.append(MATCHER_DESCRIPTION_SINGLE);
        matcherString.append(" - START\n");
        matcherString.append(MATCHER_EXPECTED_VALUE);
        matcherString.append("\nExpected ");
        matcherString.append(MATCHER_DESCRIPTION_SINGLE);
        matcherString.append(" - END");

        matcherString.append("\nActual ");
        matcherString.append(MATCHER_DESCRIPTION_SINGLE);
        matcherString.append(" - START\n");
        matcherString.append(MATCHER_ACTUAL_VALUE);
        matcherString.append("\nActual ");
        matcherString.append(MATCHER_DESCRIPTION_SINGLE);
        matcherString.append(" - END");

        MATCHER_STRING = matcherString.toString();
    }

    protected void verifyResponseMatch( String expected, String actual, String descriptionSingle,
                                        String descriptionMulty ) {

        if (!compareMixedLists(Arrays.asList(expected.split("\n")),
                               new LinkedList<String>(Arrays.asList(actual.split("\n"))))) {
            String errorMessage = MATCHER_STRING.replace(MATCHER_DESCRIPTION_MULTY, descriptionMulty)
                                                .replace(MATCHER_DESCRIPTION_SINGLE, descriptionSingle)
                                                .replace(MATCHER_EXPECTED_VALUE, expected)
                                                .replace(MATCHER_ACTUAL_VALUE, actual);
            if (expected != null && actual != null) {
                // clarify position with difference
                int shortestStringLen = Math.min(expected.length(), actual.length());
                int currentLineIdx = 0, currentColumnIdx = 0;
                StringBuilder currentLine = new StringBuilder();
                StringBuilder previousLine = new StringBuilder();
                int i = 0;
                boolean end = false;
                while (i < shortestStringLen && !end) {
                    char currentChar = expected.charAt(i);
                    currentLine.append(currentChar);

                    if (expected.charAt(i) != actual.charAt(i)) {
                        log.error("Difference found on line " + (currentLineIdx + 1) + ", column "
                                  + (currentColumnIdx + 1) + ", last matching text: \n" + previousLine
                                  + currentLine + "\n Full strings will be printed below.");
                        log.error(errorMessage);
                        end = true;
                        break;
                    }
                    if (isNewLineChar(currentChar)) {
                        previousLine.setLength(0); // delete
                        previousLine.append(currentLine);
                        // get all successive new line chars
                        do {
                            previousLine.append(currentChar);
                            currentChar = expected.charAt(i);
                            i++;
                        } while (isNewLineChar(currentChar) && i < shortestStringLen);
                        i--; // reset index so do not skip chars
                        currentLine.setLength(0); // reset for new current line
                        currentColumnIdx = -1;
                        currentLineIdx++;
                    }
                    i++;
                    currentColumnIdx++;
                }
            }

            throw new RuntimeException(errorMessage);
        }
    }

    private boolean compareMixedLists( List<String> expected, List<String> actual ) {

        for (String name : expected) {
            for (int i = 0; i < actual.size(); i++) {
                if (actual.get(i).trim().equals(name.trim())) {
                    actual.remove(i);
                    break;
                } else if (i == actual.size() - 1) {
                    return false;
                }
            }
        }
        return actual.isEmpty();
    }

    protected String readFileLineByLine( String filePath ) throws IOException {

        StringBuilder result = new StringBuilder();

        FileInputStream fstream = new FileInputStream(filePath);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;

        while ( (strLine = br.readLine()) != null) {
            result.append(strLine);
            result.append("\n");
        }
        IoUtils.closeStream(in);

        return result.toString().replace("\r\n", "\n").trim();
    }

    protected void resolveActionName( boolean deep ) {

        // if this method is called directly from a test case, the stack trace deep level is 2, otherwise it is 3
        int deepLevel = deep
                             ? 3
                             : 2;

        this.actionName = Thread.currentThread().getStackTrace()[deepLevel].getMethodName();
    }

    protected String getDownloadsFolder() {

        String tempDownloadsDir = IoUtils.normalizeDirPath(AtsSystemProperties.SYSTEM_USER_TEMP_DIR);
        File tempDir = new File(tempDownloadsDir + this.actionName);
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        return tempDownloadsDir;
    }

    private static boolean isNewLineChar( char currentChar ) {

        if (currentChar == '\n' || currentChar == '\r') {
            return true;
        } else {
            return false;
        }
    }

    protected static HttpClient
            getHttpClient( MockHttpURLConnection mockHttpURLConnection ) throws AgentException {

        // construct the client
        List<ActionHeader> httpHeaders = new ArrayList<ActionHeader>();
        httpHeaders.add(new ActionHeader("User-Agent",
                                         "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3"));
        NetworkingStopWatch stopWatch = new NetworkingStopWatch("action");
        stopWatch.step0_SetNewContext("my_action[1]");
        stopWatch.setStateFromBeforeStep1ToAfterStep4(); /// so it can be suspended for getting response
        stopWatch.step5_StartInterimTimer();
        HttpClient client = new HttpClient("http://", "GET", httpHeaders, stopWatch);

        // inject a mock connection object
        Whitebox.setInternalState(client, "urlConnection", mockHttpURLConnection);

        return client;
    }

    protected static class MockHttpURLConnection extends HttpURLConnection {

        private String                    fakeContentType;
        private String                    fakeContentEncoding;
        private InputStream               fakeInputStream;

        private Map<String, List<String>> fakeHeaderFields = Collections.emptyMap();

        public MockHttpURLConnection() {

            super(null);
        }

        @Override
        public URL getURL() {

            try {
                return new URL("http://wwww.test.com");
            } catch (MalformedURLException e) {}
            return super.url;
        }

        @Override
        public void disconnect() {

        }

        @Override
        public boolean usingProxy() {

            return false;
        }

        @Override
        public void connect() throws IOException {

        }

        @Override
        public String getContentType() {

            return this.fakeContentType;
        };

        public void setFakeContentType( String contentType ) {

            this.fakeContentType = contentType;
        };

        public void setFakeContentEncoding( String contentEncoding ) {

            this.fakeContentEncoding = contentEncoding;
        };

        @Override
        public String getContentEncoding() {

            return this.fakeContentEncoding;
        }

        public void setFakeHeaderFields( Map<String, List<String>> headerFields ) {

            this.fakeHeaderFields = headerFields;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {

            return this.fakeHeaderFields;
        }

        @Override
        public InputStream getInputStream() throws IOException {

            return this.fakeInputStream;
        }

        public String setFakeInputStream( String responseFilePath,
                                          String responseFileName ) throws IOException {

            String responseBytesFile = responseFilePath + AtsSystemProperties.SYSTEM_FILE_SEPARATOR
                                       + responseFileName;

            byte[] bytes = getBytesFromFile(new File(responseBytesFile));
            this.fakeInputStream = new ByteArrayInputStream(bytes);

            return responseBytesFile;
        }

        private byte[] getBytesFromFile( File file ) throws IOException {

            InputStream is = null;
            byte[] bytes = null;
            try {
                is = new FileInputStream(file);

                long length = file.length();
                if (length > Integer.MAX_VALUE) {}

                bytes = new byte[(int) length];

                int offset = 0;
                int numRead = 0;
                while (offset < bytes.length
                       && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                    offset += numRead;
                }

                if (offset < bytes.length) {
                    throw new IOException("Could not completely read file " + file.getName());
                }
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            return bytes;
        }
    }
}
