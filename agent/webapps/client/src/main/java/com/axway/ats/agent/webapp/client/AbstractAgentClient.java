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
package com.axway.ats.agent.webapp.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.utils.HostUtils;

public abstract class AbstractAgentClient {

    protected final Logger        log;

    /**
     * Make the call through the current VM 
     */
    protected static final String LOCAL_JVM = "local";

    protected final String        atsAgent;
    protected final String        component;

    protected AbstractAgentClient( String atsAgent, String component ) {

        this.log = LogManager.getLogger(this.getClass());

        if (LOCAL_JVM.equals(atsAgent)) {
            // we will work in the local JVM, so no running external instance of ATS Agent
            this.atsAgent = atsAgent;
        } else {
            // add default port in case none is not provided by the user
            this.atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        }

        this.component = component;
    }
}
