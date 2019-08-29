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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.action.http.HttpClient;
import com.axway.ats.agent.core.threading.patterns.FixedDurationAllAtOncePattern;
import com.axway.ats.agent.webapp.client.ActionQueue;
import com.axway.ats.agent.webapp.client.LoadClient;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.framework.examples.vm.actions.clients.FileTransferActions;
import com.axway.ats.examples.performance.common.PerfBaseTestClass;
import com.axway.ats.monitoring.SystemMonitor;

/**
 * Running performance tests root page:
 *      https://axway.github.io/ats-framework/Running-performance-tests.html
 *
 * When running many threads(users) you usually need to use different input parameters for your actions.
 * This is done by defining how some parameters get changed as presented at:
 *      https://axway.github.io/ats-framework/Parametrization.html
 */
public class OtherOptionsTests extends PerfBaseTestClass {

    // some constants about the target server
    private static final String SERVER_IP              = configuration.getServerIp();
    private static final String FTP_TRANSFER_PROTOCOL  = "FTP";
    private static final String HTTP_TRANSFER_PROTOCOL = "HTTP";
    private static final int    FTP_SERVER_PORT        = configuration.getFtpServerPort();
    private static final int    HTTP_SERVER_PORT       = configuration.getHttpServerPort();

    // the local directory with files to upload
    private static final String RESOURCES_ROOT_DIR = configuration.getResourcesRootDir();

    // folder where we keep the uploaded files
    private static final String UPLOADS_DIR = IoUtils.normalizeDirPath(RESOURCES_ROOT_DIR
                                                                       + "uploads");

    // some large file to use for some transfers
    private static final String LARGE_FILE      = RESOURCES_ROOT_DIR + "largeFile.txt";
    private static final long   LARGE_FILE_SIZE = 10 * 1024 * 1024;

    // the monitor is used to add some statistical info which helps understand about
    // the loading of the server
    private SystemMonitor systemMonitor;

    @BeforeClass
    public void beforeClass() {

        // This step is needed for the HTTP tests only.
        // We tell the remote HTTP server the folder to store to and read files from.
        // Of course, you can find a nicer way to do that.
        HttpClient httpClient = new HttpClient("http://" + configuration.getServerIp() + ":"
                                               + configuration.getHttpServerPort() + "/"
                                               + configuration.getHttpServerWebappWar() + "/transfers/");
        httpClient.addRequestHeader("repository", UPLOADS_DIR);
        httpClient.post();
        httpClient.close();

        // create some we will use for uploads
        new FileSystemOperations().createBinaryFile(LARGE_FILE, LARGE_FILE_SIZE, false);
    }

    /**
     * run before each test in this class
     */
    @BeforeMethod
    public void beforeMethod() throws Exception {

        // initialize the monitor
        systemMonitor = new SystemMonitor();

        // add as many as needed statistics for as many as needed hosts(Note that a running ATS Agent is needed on each host)

        // it is common to monitor the CPU
        // we also want to monitor the IO activity on the file system as we will be doing file reads and saves 
        systemMonitor.scheduleSystemMonitoring(AGENT1_ADDRESS,
                                               new String[]{ SystemMonitor.MONITOR_CPU.ALL,
                                                             SystemMonitor.MONITOR_IO.ALL });

        // we want to see how many users were running at any given moment
        // this is usually not needed in real tests, but it helps when learning about ATS performance testing
        systemMonitor.scheduleUserActivityMonitoring(AGENT1_ADDRESS);
        systemMonitor.scheduleUserActivityMonitoring(AGENT2_ADDRESS);

        // now is the moment when the monitoring actually starts
        systemMonitor.startMonitoring(1);

        // it is usually a good idea to have some 'silent' period before each test, 
        // so can see the system state before the test starts
        // of course this greatly increases the test length
        // Thread.sleep( 3000 );
    }

    /**
     * run after each test in this class
     */
    @AfterMethod
    public void afterMethod() throws Exception {

        // it is usually a good idea to have some 'silent' period after each test, 
        // so can see the system state after the test is over
        // of course this greatly increases the test length
        // Thread.sleep( 3000 );

        // stop the monitoring
        systemMonitor.stopMonitoring();
    }

    /**
     * Sometimes you will need to use more than one performance queue.
     * In our example we will have 2 queues for running file transfer over different protocols.
     *
     * In order to do that, you need to specify 'false' for the 'blockUntilCompletion' parameter
     * which is present as the second parameter of each pattern constructor.
     * When you do so, the 'executeQueuedActions()' will start the queued actions, but will not wait
     * for the queue completion. 
     * This way the test goes to the next line where you can do whatever is needed - for example you
     * can start one more queue.
     *
     * After running the test you can go to test case in Test Explorer and its 'Statistic' tab,
     * and select '[users] Total' under 'User activities'. This way you will how many users were running
     * during the test. As the queue are with different length, there will different number of users during the 
     * test execution. 
     */
    @Test
    public void runTwoQueuesAtSameTime() throws Exception {

        // 1. Start the FTP transfers
        LoadClient ftpLoader = new LoadClient(AGENT1_ADDRESS);
        // Set the threading pattern. Note the second parameter - it is false which means the test code
        // will go on without waiting the performance queue completion
        ftpLoader.setThreadingPattern(new FixedDurationAllAtOncePattern(5, false, 10, 0));

        ftpLoader.startQueueing("FTP transfers");
        FileTransferActions ftpTransferActions = new FileTransferActions();
        ftpTransferActions.connect(FTP_TRANSFER_PROTOCOL, FTP_SERVER_PORT, SERVER_IP,
                                   configuration.getUserName(), configuration.getUserPassword());
        ftpTransferActions.upload(LARGE_FILE);
        ftpTransferActions.disconnect();
        // Now start the FTP transfers, but do not wait, just go to the next code line
        ftpLoader.executeQueuedActions();

        // 2. Start the HTTP transfers
        LoadClient httpLoader = new LoadClient(AGENT1_ADDRESS);
        // Again, we are using a non-blocking queue here
        httpLoader.setThreadingPattern(new FixedDurationAllAtOncePattern(5, false, 30, 0));

        httpLoader.startQueueing("HTTP transfers");
        FileTransferActions httpTransferActions = new FileTransferActions();
        httpTransferActions.connect(HTTP_TRANSFER_PROTOCOL, HTTP_SERVER_PORT, SERVER_IP,
                                    configuration.getUserName(), configuration.getUserPassword());
        httpTransferActions.upload(LARGE_FILE, "/" + configuration.getHttpServerWebappWar() + "/transfers/",
                                   "remoteFile.txt");
        httpTransferActions.disconnect();
        // Now start the FTP transfers, but do not wait, just go to the next code line
        httpLoader.executeQueuedActions();

        // 3. Right at this moment both queue are working
        // Next line will make us wait until both queues finish
        ActionQueue.waitUntilAllQueuesFinish();
    }

    /**
     * There are rare cases when 1 loader(ATS Agent) will not be enough.
     * This depends on:
     *      1. The available hardware resources
     *      2. The number of running users(threads)
     *      3. The kind of work to be done by each action
     *
     * Our advice is to always start with 1 loader and if it turns out it cannot handle all the load,
     * then add another one.
     * This is very easy as all you need to do is to add another loader address when constructing the LoadClient.
     * The rest of the test code remains as before.
     *
     * NOTE: Of course, you are supposed to first have started the additional loader. 
     * The additional loader should run on a different host.
     */
    @Test
    public void runQueueOnMoreThanOneLoader() throws Exception {

        // Provide as many as needed Agent addresses, each one of them will serve as a loader.
        // The number of users(threads) will be equally(as much as possible) distributed among all loaders 
        LoadClient ftpLoader = new LoadClient(new String[]{ configuration.getAgent1Address(),
                                                            configuration.getAgent2Address() });

        // The rest of the test code is as usual
        ftpLoader.setThreadingPattern(new FixedDurationAllAtOncePattern(5, true, 10, 0));

        ftpLoader.startQueueing("FTP transfers");
        FileTransferActions ftpTransferActions = new FileTransferActions();
        ftpTransferActions.connect(FTP_TRANSFER_PROTOCOL, FTP_SERVER_PORT, SERVER_IP,
                                   configuration.getUserName(), configuration.getUserPassword());
        ftpTransferActions.upload(LARGE_FILE);
        ftpTransferActions.disconnect();

        ftpLoader.executeQueuedActions();
    }
}
