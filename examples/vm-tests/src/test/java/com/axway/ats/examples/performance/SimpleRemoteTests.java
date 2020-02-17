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

import org.testng.annotations.Test;

import com.axway.ats.framework.examples.vm.actions.clients.SimpleActions;
import com.axway.ats.examples.performance.common.PerfBaseTestClass;

import static org.testng.Assert.assertEquals;

/**
 * These are not performance tests indeed, but are put here as they
 * use an ATS Agent.
 * ATS Agent is used for all performance tests as well.
 *
 * These tests show how to run some code on any host where the agent is running.
 *
 * Related documentation:
 *      https://axway.github.io/ats-framework/Execute-any-custom-code-on-any-host.html
 *
 * The action class logs some information in the Agent's log file
 * which in our case is <AGENT HOME>/logs/ATSAgentAudit_8081.log file
 */
public class SimpleRemoteTests extends PerfBaseTestClass {

    /**
     * This test shows how to send a command to some ATS Agent located on any host
     * and get back some response.
     *
     * It is all up to you what the action will do.
     */
    @Test
    public void remoteExecution() throws Exception {

        // create an instance of your action class by providing the remote Agent address
        SimpleActions simpleAction = new SimpleActions(AGENT1_ADDRESS);

        // run your action on the host where the agent is located, this one returns some result
        String result = simpleAction.sayHi("Professional Tester", 20);

        log.info("The remote ATS agent answered with: " + result);
    }

    /**
     * You can also pass data between different actions, in other words there is session data for you.
     *
     * And this is as simple as defining a shared data object on the server side.
     */
    @Test
    public void callManyActionsSharingSameSessionData() throws Exception {

        // create an instance of your action class by providing the remote Agent address
        SimpleActions simpleAction = new SimpleActions(AGENT1_ADDRESS);

        // create a person on the server side
        simpleAction.createPerson("Chuck", "Norris", 70);

        // manipulate the data on the server side
        simpleAction.registerBirthdayTick();

        // get the new change on the server side
        int ageNow = simpleAction.getAge();
        assertEquals(ageNow, 71);

        // you can also see some messages in the Agent's log file
    }
}
