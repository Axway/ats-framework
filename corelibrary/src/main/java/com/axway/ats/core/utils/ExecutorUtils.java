/*
 * Copyright 2017-2022 Axway Software
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
package com.axway.ats.core.utils;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.log.AtsConsoleLogger;

/**
 * Helps to identify who has initiated/started some job.
 * It contains the IP of the test executor/caller, the project's directory, the thread ID and the thread name
 */
public class ExecutorUtils {
    public final static String ATS_HOST_ID                   = "HOST_ID";
    public final static String ATS_WORKDIR                   = "WORKDIR";
    public final static String ATS_THREAD_ID                 = "THREAD_ID";
    public final static String ATS_THREAD_NAME               = "THREAD_NAME";
    public final static String ATS_CALLER_ID_TOKEN_DELIMITER = ";";
    public final static String ATS_TOKEN_DELIMITER           = ":";
    public final static String ATS_CALLER_ID                 = "CALLER_ID";

    /**
     * Create unique caller ID for the originator of calls on Agents
     * @return
     */
    public static String createCallerId() {
        // TODO - Add agent IP as destination if there are multiple network interfaces on the test executor
        //    Replace HostUtils.getLocalHostIP() with HostUtils.getPublicLocalHostIp(atsAgentAddr)
        // Create unique caller ID even for cases like multiple simultaneous JVMs/executors from same host (like Jenkins)
        return ATS_HOST_ID + ATS_TOKEN_DELIMITER + HostUtils.getLocalHostIP() + ATS_CALLER_ID_TOKEN_DELIMITER
               + ATS_WORKDIR + ATS_TOKEN_DELIMITER + IoUtils.normalizeUnixDir(AtsSystemProperties.USER_CURRENT_DIR)
               + ATS_CALLER_ID_TOKEN_DELIMITER + ATS_THREAD_ID + ATS_TOKEN_DELIMITER + Thread.currentThread().getId()
               + ATS_CALLER_ID_TOKEN_DELIMITER + ATS_THREAD_NAME + ATS_TOKEN_DELIMITER
               + Thread.currentThread().getName();

    }

    public static String extractHost( String callerId ) {

        return extractToken(callerId, ATS_HOST_ID);
    }

    public static String extractWorkDir( String callerId ) {

        return extractToken(callerId, ATS_WORKDIR);
    }

    public static String extractThreadId( String callerId ) {

        return extractToken(callerId, ATS_THREAD_ID);
    }

    public static String extractThreadName( String callerId ) {

        return extractToken(callerId, ATS_THREAD_NAME);
    }

    private static String extractToken( String callerId, String key ) {

        for (String token : callerId.split(ATS_CALLER_ID_TOKEN_DELIMITER)) {
            if (token.startsWith(key)) {
                return token.replace(key + ATS_TOKEN_DELIMITER, "").trim();
            }
        }

        return null;
    }


    public final static String ATS_RANDOM_TOKEN = "RANDOM_TOKEN"; // replace usage with caller and parsing
    /**
     * Get ID for agent communication
     */
    public static String createExecutorId( String host, Thread thread ) {

        AtsConsoleLogger log = new AtsConsoleLogger(ExecutorUtils.class);
        // TODO - replace as thread ID could be the same for two different JVMs from same host
        log.warn("Replace executorId with caller ID");
        return ATS_HOST_ID + ":" + host + ";" + ATS_THREAD_ID + ":" + thread.getId();
    }
    public static String createExecutorId( String host, String randomToken, String threadId ) {
        AtsConsoleLogger log = new AtsConsoleLogger(ExecutorUtils.class);
        // TODO - replace with caller
        log.warn("Replace executorId with caller ID");
        return ATS_HOST_ID + ":" + host + ";" + ATS_RANDOM_TOKEN + ":" + randomToken + ";" + ATS_THREAD_ID
               + ": " + threadId;
    }
}
