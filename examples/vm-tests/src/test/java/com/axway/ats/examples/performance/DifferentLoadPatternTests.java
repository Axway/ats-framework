/*
 * Copyright 2018-2021 Axway Software
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
import com.axway.ats.agent.core.threading.patterns.AllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.FixedDurationAllAtOncePattern;
import com.axway.ats.agent.core.threading.patterns.FixedDurationRampUpPattern;
import com.axway.ats.agent.core.threading.patterns.RampUpPattern;
import com.axway.ats.agent.webapp.client.LoadClient;
import com.axway.ats.examples.performance.common.PerfBaseTestClass;
import com.axway.ats.framework.examples.vm.actions.clients.FileTransferActions;
import com.axway.ats.framework.examples.vm.actions.clients.SimpleActions;
import com.axway.ats.monitoring.SystemMonitor;

/**
 * Running performance tests root page:
 *      https://axway.github.io/ats-framework/Running-performance-tests.html
 *
 * All threading patterns used in these tests are presented at:
 *      https://axway.github.io/ats-framework/Threading-patterns.html
 */
public class DifferentLoadPatternTests extends PerfBaseTestClass {

    // the local folder where we keep needed files
    private static final String RESOURCES_ROOT_DIR = configuration.getResourcesRootDir();

    // the name of the file to upload
    private static final String LOCAL_FILE_NAME = "file.txt";

    // the monitor is used to add some statistical info which helps understand about
    // the loading of the server
    private SystemMonitor systemMonitor;

    @BeforeClass
    public void beforeClass() {

        FileSystemOperations fileOperations = new FileSystemOperations();
        // create a file that will be uploaded
        fileOperations.createFile(RESOURCES_ROOT_DIR + LOCAL_FILE_NAME, "file with some simple content");
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
        systemMonitor.scheduleSystemMonitoring(AGENT1_ADDRESS, new String[]{ SystemMonitor.MONITOR_CPU.ALL,
                                                                             SystemMonitor.MONITOR_MEMORY.ALL,
                                                                             SystemMonitor.MONITOR_IO.ALL });

        // we want to see how many users were running at any given moment
        // this is usually not needed in real tests, but it helps when learning about ATS performance testing
        systemMonitor.scheduleUserActivityMonitoring(AGENT1_ADDRESS);

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
     * This test uses the already presented SimpleActions class.
     *
     * It starts 10 users at once, each user runs 5 iterations
     *
     */
    @Test
    public void basicTest() throws Exception {

        LoadClient loader = new LoadClient(AGENT1_ADDRESS);
        loader.setThreadingPattern(new AllAtOncePattern(5, true, 5, 0));

        loader.startQueueing("A simple performance queue");

        SimpleActions simpleAction = new SimpleActions(AGENT1_ADDRESS);
        simpleAction.sayHi("Professional Tester", 20);

        loader.executeQueuedActions();
    }

    /**
     * Example with users that all start at the same moment and run a number of iterations
     *
     * You should go and review the results on Test Explorer
     */
    @Test
    public void allUsersAtOncePattern() throws Exception {

        // create a load client and provide the loader it works on
        LoadClient loader = new LoadClient(AGENT1_ADDRESS);

        // set AllAtOncePattern threading pattern
        // this one will run 5 threads(users) which will start together and execute 10 iterations each
        // there will no delay between each iteration
        loader.setThreadingPattern(new AllAtOncePattern(5, true, 10, 0));

        // give a name to your action queue
        // from that point on, your actions will be queued
        loader.startQueueing("Some FTP transfer");

        // create an instance of your action class
        FileTransferActions ftActions = new FileTransferActions();

        // list all the actions you want to run
        ftActions.connect("FTP", configuration.getFtpServerPort(), configuration.getServerIp(),
                          configuration.getUserName(),
                          configuration.getUserPassword());
        ftActions.upload(RESOURCES_ROOT_DIR + LOCAL_FILE_NAME);
        ftActions.disconnect();

        // now execute the queued actions according to the threading pattern
        loader.executeQueuedActions();
    }

    /**
     * Example with users that all start at the same moment and run for a given period of time
     */
    @Test
    public void fixedDurationPattern() throws Exception {

        LoadClient loader = new LoadClient(AGENT1_ADDRESS);

        // set FixedDurationAllAtOncePattern threading pattern
        // this one will run 5 threads(users) which will start together and will work for 30 seconds
        // there will be 100ms delay between each iteration
        loader.setThreadingPattern(new FixedDurationAllAtOncePattern(5, true, 30, 100));

        loader.startQueueing("Some FTP transfers");

        FileTransferActions ftActions = new FileTransferActions();
        ftActions.connect("FTP", configuration.getFtpServerPort(), configuration.getServerIp(),
                          configuration.getUserName(),
                          configuration.getUserPassword());
        ftActions.upload(RESOURCES_ROOT_DIR + LOCAL_FILE_NAME);
        ftActions.disconnect();

        loader.executeQueuedActions();
    }

    /**
     * Example with users that start at some intervals. Each user runs same number of iterations
     */
    @Test
    public void rampUpPattern() throws Exception {

        LoadClient loader = new LoadClient(AGENT1_ADDRESS);

        // Set RampUpPattern threading pattern.
        // This one will start a total number of 5 threads(users), but not together.
        // Each 2000 ms it will start 1 more threads until all 5 threads are started.
        // Each thread will execute 10 iteration with no delay between each of them
        loader.setThreadingPattern(new RampUpPattern(5, true, 10, 0, 2000, 1));

        loader.startQueueing("Some FTP transfers");

        FileTransferActions ftActions = new FileTransferActions();
        ftActions.connect("FTP", configuration.getFtpServerPort(), configuration.getServerIp(),
                          configuration.getUserName(),
                          configuration.getUserPassword());
        ftActions.upload(RESOURCES_ROOT_DIR + LOCAL_FILE_NAME);
        ftActions.disconnect();

        loader.executeQueuedActions();
    }

    /**
     * Example with users that start at some intervals. Each user runs for a given period of time
     */
    @Test
    public void fixedDurationRampUpPattern() throws Exception {

        LoadClient loader = new LoadClient(AGENT1_ADDRESS);

        // Set FixedDurationRampUpPattern threading pattern.
        // This one will start a total number of 5 threads(users), but not together.
        // Each 2000 ms it will start 1 more threads until start all the 5 threads.
        // Each thread will work for 30 seconds
        // there will be 100ms delay between each iteration
        loader.setThreadingPattern(new FixedDurationRampUpPattern(5, true, 30, 100, 2000, 1));

        loader.startQueueing("Some FTP transfers");

        FileTransferActions ftActions = new FileTransferActions();
        ftActions.connect("FTP", configuration.getFtpServerPort(), configuration.getServerIp(),
                          configuration.getUserName(),
                          configuration.getUserPassword());
        ftActions.upload(RESOURCES_ROOT_DIR + LOCAL_FILE_NAME);
        ftActions.disconnect();

        loader.executeQueuedActions();
    }
}
