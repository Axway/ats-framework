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
package com.axway.ats.examples.monitoring;

import org.testng.annotations.Test;

import com.axway.ats.action.system.SystemOperations;
import com.axway.ats.examples.common.BaseTestClass;
import com.axway.ats.monitoring.SystemMonitor;

/**
 * System monitoring root page:
 *      https://axway.github.io/ats-framework/System-monitoring.html
 */
public class MonitoringTests extends BaseTestClass {

    // The agent IP and port.
    // In our case we run it on the local host, but the code will work in same way when
    // run the agent on any remote host.
    protected static final String AGENT1_ADDRESS = configuration.getAgent1Address();
    protected static final String AGENT2_ADDRESS = configuration.getAgent2Address();

    @Test
    public void basicTest() throws Exception {

        SystemOperations sysOps = new SystemOperations(AGENT1_ADDRESS);
        log.info("Will start monitoring on host: " + sysOps.getHostname());

        // the monitor is used to add some statistical info which helps understand about the loading of the server
        // 1. Initialize the monitor
        SystemMonitor systemMonitor = new SystemMonitor();

        // 2. Add as many as needed statistics for as many as needed hosts(Note that a running ATS Agent is needed on each host)

        // it is common to monitor the CPU, Memory and IO activity on the file system 
        systemMonitor.scheduleSystemMonitoring(AGENT1_ADDRESS,
                                               new String[]{ SystemMonitor.MONITOR_CPU.ALL,
                                                             SystemMonitor.MONITOR_MEMORY.ALL,
                                                             SystemMonitor.MONITOR_IO.ALL });

        // If you are dealing with a Java application, you can get some internal info.l It is important to note that JMX must be enabled.
        // In our case we just monitor some ATS Agent, we have enabled JMX connection in the agent.sh
        // but setting the flag "JMX=true"
        systemMonitor.scheduleJvmMonitoring(AGENT1_ADDRESS, "1099",
                                            new String[]{ SystemMonitor.MONITOR_JVM.CPU_USAGE,
                                                          SystemMonitor.MONITOR_JVM.MEMORY_HEAP,
                                                          SystemMonitor.MONITOR_JVM.THREADS_COUNT,
                                                          SystemMonitor.MONITOR_JVM.CLASSES_COUNT });

        // 3. Now is the moment when the monitoring actually starts
        systemMonitor.startMonitoring(1);

        // 4. here we just wait, but you should do some real work in your test
        Thread.sleep(60 * 1000);

        // 5. now is time to stop the monitoring
        systemMonitor.stopMonitoring();
    }
}
