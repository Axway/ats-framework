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
package com.axway.ats.examples.performance.common;

import org.testng.annotations.BeforeClass;

import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.examples.common.BaseTestClass;

/**
 * A base class for all performance related tests.
 *
 * ATS performance testing is introduced at:
 *      https://axway.github.io/ats-framework/Performance-testing.html
 *
 */
public class PerfBaseTestClass extends BaseTestClass {

    // The agent IP and port.
    // In our case we run it on the local host, but the code will work in same way when
    // run the agent on any remote host.
    protected static final String AGENT1_ADDRESS = configuration.getAgent1Address();
    protected static final String AGENT2_ADDRESS = configuration.getAgent2Address();

    /**
     * This code is run just once before the first test.
     * We use it to update our custom actions on the Agent side.
     * In fact this is a not needed step if you did not do any action class changes.
     */
    @BeforeClass
    public void beforeClass() throws InterruptedException {

        // the name of the jar containing server side action classes
        final String actionsClientJar = "ats-all-in-one-agent-actions-autoserver.jar";

        // Copy the server side action classes jar in the right folder of the Agent.
        // They will be picked up in no more than a few seconds and then they are ready to be used.
        // If you want to see the Agent loaded them go an check the <AGENT HOME>/logs/ATSAgentAudit_25089.log file
        FileSystemOperations fileOperations = new FileSystemOperations();
        fileOperations.copyFileTo(configuration.getRootDir()
                                  + "eclipse_workspace/test-artifacts/"
                                  + actionsClientJar,
                                  configuration.getRootDir() + "agent_25089/ats-agent/actions/"
                                  + actionsClientJar);
        // Give some time to the Agent maintenance job to find the new file
        Thread.sleep(5000);
    }
}
