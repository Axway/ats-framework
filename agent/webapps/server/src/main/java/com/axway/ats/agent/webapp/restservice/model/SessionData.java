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
package com.axway.ats.agent.webapp.restservice.model;

import java.util.Date;

import com.axway.ats.agent.webapp.restservice.RestSystemMonitor;

public class SessionData {

    private RestSystemMonitor systemMonitor;

    /*
     * The last time this session was called. This flag is used to discover not
     * used sessions.
     */
    private long              lastUsed;

    public SessionData() {
        systemMonitor = new RestSystemMonitor();
        updateLastUsedFlag();
    }

    public SessionData( RestSystemMonitor systemMonitor ) {
        this.systemMonitor = systemMonitor;
    }

    public RestSystemMonitor getSystemMonitor() {

        return systemMonitor;
    }

    public void setSystemMonitor(
                                  RestSystemMonitor systemMonitor ) {

        this.systemMonitor = systemMonitor;
    }

    public void updateLastUsedFlag() {

        lastUsed = new Date().getTime();
    }

    public long getLastUsedFlag() {

        return lastUsed;
    }

}
