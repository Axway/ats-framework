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
package com.axway.ats.examples.performance;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.agent.core.threading.data.config.FileNamesDataConfig;
import com.axway.ats.agent.core.threading.data.config.ListDataConfig;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;
import com.axway.ats.agent.core.threading.data.config.RangeDataConfig;
import com.axway.ats.agent.core.threading.patterns.AllAtOncePattern;
import com.axway.ats.agent.webapp.client.LoadClient;
import com.axway.ats.framework.examples.vm.actions.clients.FileTransferActions;
import com.axway.ats.examples.performance.common.PerfBaseTestClass;

/**
 * Running performance tests root page:
 *      https://axway.github.io/ats-framework/Running-performance-tests.html
 *
 * When running many threads(users) you usually need to use different input parameters for your actions.
 * This is done by defining how some parameters get changed as presented at:
 *      https://axway.github.io/ats-framework/Parametrization.html
 */
public class ParametrizationTests extends PerfBaseTestClass {

    // some constants about the target server
    private static final String TRANSFER_PROTOCOL = "FTP";
    private static final String SERVER_IP         = configuration.getServerIp();
    private static final int    SERVER_PORT       = configuration.getFtpServerPort();

    // we prefer to use constants for the parameters that will be changing during the test execution
    // and we prefer to name them in the form PARAMETRIZED_...
    private static final String PARAMETRIZED_USER = "username";
    private static final String PARAMETRIZED_FILE = "localFilePath";

    // user info
    private static final String[] USER_TOKENS   = new String[]{ "user5", "user4", "user3", "user2",
                                                                "user1" };
    private static final String   USER_PASSWORD = configuration.getUserPassword();

    // the local directory with files to upload
    private static final String RESOURCES_ROOT_DIR     = configuration.getResourcesRootDir();
    private static final String LOCAL_FILE_NAME_PREFIX = "file";

    @BeforeClass
    public void beforeClass() {

        FileSystemOperations fileOperations = new FileSystemOperations();
        for (int i = 1; i <= USER_TOKENS.length; i++) {
            fileOperations.createFile(RESOURCES_ROOT_DIR + LOCAL_FILE_NAME_PREFIX + i + ".txt",
                                      "file with some simple content");
        }
    }

    /**
     * The actual values are changing each time according to some pattern.
     *
     * For example:
     *      - the user is changing from 'user1' to 'user100'
     *      - the file is changing from '<folder>/file1.txt' to ''<folder>/file100.txt''
     */
    @Test
    public void rangeDataConfigurator() throws Exception {

        LoadClient loader = new LoadClient(AGENT1_ADDRESS);
        loader.setThreadingPattern(new AllAtOncePattern(USER_TOKENS.length, true));

        // Set as many as needed data configurators
        // These will be changing in the specified range
        loader.addParameterDataConfigurator(new RangeDataConfig(PARAMETRIZED_USER, "user{0}", 1, 100));
        loader.addParameterDataConfigurator(new RangeDataConfig(PARAMETRIZED_FILE,
                                                                RESOURCES_ROOT_DIR + "file{0}.txt", 1,
                                                                100));

        loader.startQueueing("FTP transfers with parametrized username and upload file");
        FileTransferActions ftActions = new FileTransferActions();

        // user name is changing
        ftActions.connect(TRANSFER_PROTOCOL, SERVER_PORT, SERVER_IP, PARAMETRIZED_USER, USER_PASSWORD);
        // will upload another file each time
        ftActions.upload(PARAMETRIZED_FILE);

        ftActions.disconnect();
        loader.executeQueuedActions();
    }

    /**
     * The actual values are taken from a provided list
     *
     * So the user is coming as the next value of the USER_TOKENS list
     */
    @Test
    public void listDataConfigurator() throws Exception {

        LoadClient loader = new LoadClient(AGENT1_ADDRESS);
        loader.setThreadingPattern(new AllAtOncePattern(USER_TOKENS.length, true));

        // set as many as needed data configurators
        loader.addParameterDataConfigurator(new ListDataConfig(PARAMETRIZED_USER, USER_TOKENS));

        loader.startQueueing("FTP transfers");
        FileTransferActions ftActions = new FileTransferActions();

        // user name is changing
        ftActions.connect(TRANSFER_PROTOCOL, SERVER_PORT, SERVER_IP, PARAMETRIZED_USER, USER_PASSWORD);
        // the file is the same each time
        ftActions.upload(RESOURCES_ROOT_DIR + "file1.txt");

        ftActions.disconnect();
        loader.executeQueuedActions();
    }

    /**
     * Using the FileNamesDataConfig helps you pick the next file from a folder where all files are located.
     *
     * Note that we have also specified a parameter provider level.
     * In this case it is use to PER_INVOCATION which means each thread/user will pick the next file
     * in the folder for each of its iterations.
     *
     * Parameter provider level is explained at:
     *      https://axway.github.io/ats-framework/Parameter-provider-level.html
     */
    @Test
    public void fileNamesDataConfig() throws Exception {

        LoadClient loader = new LoadClient(AGENT1_ADDRESS);
        loader.setThreadingPattern(new AllAtOncePattern(5, true));

        // set as many as needed data configurators
        loader.addParameterDataConfigurator(new ListDataConfig(PARAMETRIZED_USER, USER_TOKENS));
        loader.addParameterDataConfigurator(new FileNamesDataConfig(PARAMETRIZED_FILE, RESOURCES_ROOT_DIR,
                                                                    ParameterProviderLevel.PER_INVOCATION));

        loader.startQueueing("FTP transfers");
        FileTransferActions ftActions = new FileTransferActions();

        // user name is changing
        ftActions.connect(TRANSFER_PROTOCOL, SERVER_PORT, SERVER_IP, PARAMETRIZED_USER, USER_PASSWORD);
        // the file is the next file in the give folder
        ftActions.upload(PARAMETRIZED_FILE);

        ftActions.disconnect();
        loader.executeQueuedActions();
    }
}
