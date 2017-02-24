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
package com.axway.ats.agent.core.context;

/**
 * Context to be used by all components when they any state to be
 * available across all sessions. 
 */
public class ApplicationContext extends Context {

    private static ApplicationContext appContext;

    // used for maintaining session between Agent and its caller
    public final static String        ATS_UID_SESSION_TOKEN = "ATS_UID";
    /**
     * separates IP address from UID
     * Used to determine which queues to be cancelled on AgentWsImpl.onTestStart() method invocation
     **/
    public final static String CALLER_ID_TOKEN_SEPARATOR ="; ";

    private ApplicationContext() {

    }

    public static synchronized ApplicationContext getInstance() {

        if( appContext == null ) {
            appContext = new ApplicationContext();
        }

        return appContext;
    }
    
    public static String createCallerID(String agentIpAddress, String uid){
        return "<Caller: " + agentIpAddress + CALLER_ID_TOKEN_SEPARATOR + "ATS UID: " + uid + ">";
    }
    
    public static String extractCallerIP(String callerID){
        String[] tokens = callerID.split( CALLER_ID_TOKEN_SEPARATOR );
        String ip = tokens[0].replace( "<Caller:", "" ).trim();
        return ip;
    }
    
}
