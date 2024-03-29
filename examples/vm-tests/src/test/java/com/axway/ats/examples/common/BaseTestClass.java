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
package com.axway.ats.examples.common;

import java.io.File;
import java.io.IOException;

import com.axway.ats.action.ftp.FtpClient;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import com.axway.ats.action.rest.RestClient;
import com.axway.ats.action.rest.RestResponse;
import com.axway.ats.action.system.SystemOperations;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.harness.testng.AtsTestngListener;

/**
 * This class is the Base class of all test classes in this project. It is used
 * to keep data used by most of the tests.
 *
 * It attaches the TestNG listener which will inform ATS about the start and end
 * of each test. This will be used internally to control the logging on the Test
 * Explorer side.
 */
@Listeners( AtsTestngListener.class )
public class BaseTestClass {

    // a common logger which can be used by each test to log some message
    protected Logger log;

    // A handler to this project configuration
    // We use it to read some data in order to initialize some constants used in
    // different tests
    protected static final AllInOneConfiguration configuration;

    // some tests need to know the temporary user directory
    protected static String userTempDir;

    // this static section will do some one-time configuration
    static {
        configuration = AllInOneConfiguration.getInstance();

        initializeUserTempDir();
    }

    @BeforeSuite
    public void beforeSuite() {

        boolean hasServiceException = false;
        // quick sanity check if all servers are working properly

        log.info("Running SANITY check for Example servers...");
        // check agents
        log.info("ATS Agents sanity check START");
        try {
            String agent1AtsVersion = new SystemOperations(configuration.getAgent1Address()).getAtsVersion();
            log.info("Agent 1 is working and its version is: " + agent1AtsVersion);
            String agent2AtsVersion = new SystemOperations(configuration.getAgent1Address()).getAtsVersion();
            log.info("Agent 2 is working and its version is: " + agent2AtsVersion);
        } catch (Exception e) {
            hasServiceException = true;
            log.error(e);
        }
        log.info("ATS Agents sanity check END");

        log.info("HTTP Server sanity check START");
        // check HTTP server
        RestClient restClient = null;
        try {
            restClient = new RestClient(
                    "http://" + configuration.getServerIp() + ":" + configuration.getHttpServerPort());
            RestResponse rr = restClient.get();
            if (rr.getStatusCode() != 200) {
                throw new RuntimeException(
                        "HTTP Server server maybe in trouble or not started properly. Status of HTTP sanity check response is :\n\tStatus Message: "
                        + rr.getStatusMessage() + "\n\tBody: " + rr.getBodyAsString());
            } else {
                log.info("HTTP server working");
            }
        } catch (Exception e) {
            hasServiceException = true;
            log.error(e);
        } finally {
            if (restClient != null) {
                restClient.disconnect();
                restClient = null;
            }

        }
        log.info("HTTP Server sanity check END");

        log.info("FTP/FTPS Server sanity check START");
        // check FTP/FTPS server (TODO)
        FtpClient ftpClient = null;
        try {
            ftpClient = new FtpClient();
            ftpClient.setCustomPort(configuration.getFtpServerPort());
            ftpClient.connect(configuration.getServerIp(), configuration.getUserName(),
                              configuration.getUserPassword());
        } catch (Exception e) {
            hasServiceException = true;
            log.error(e);
        } finally {
            if (ftpClient != null) {
                ftpClient.disconnect();
                ftpClient = null;
            }

        }
        log.info("FTP/FTPS Server sanity check END");

        log.info("DONE running SANITY check for Examples servers");

        if (hasServiceException) {
            throw new RuntimeException(
                    "Not all servers started or working properly. Did you run 'Start servers' from the Desktop directory?");
        }

    }

    /**
     * This constructor is called for each executed test. This is how TestNG works.
     */
    public BaseTestClass() {

        // Initialize the logger for the particular test class instance.
        // This way your messages will show the exact class which issued the message
        log = Logger.getLogger(this.getClass());
    }

    /**
     * Initialize the temporary user directory
     */
    private static void initializeUserTempDir() {

        // Get the current user's temporary folder, we will use it when working with
        // some files and folders.
        File tempDir = new File(AtsSystemProperties.SYSTEM_USER_TEMP_DIR);
        try {
            // this path may come as "C:\Users\MYUSER~1\AppData\Local\Temp"
            // but we prefer "C:\Users\MYUSERNAME\AppData\Local\Temp"
            // otherwise some assertions regarding file paths fail in some of the following
            // tests
            userTempDir = tempDir.getCanonicalPath();
        } catch (IOException ignored) {
        }

        // Note: the IoUtils methods are not a public. They may get changed at any
        // moment without a notice
        userTempDir = IoUtils.normalizeDirPath(userTempDir);
    }
}
