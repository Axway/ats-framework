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
package com.axway.ats.core.utils;

import com.axway.ats.common.systemproperties.AtsSystemProperties;

/**
 * Helps identifying who is running some job.
 * It contains the IP of the test executor, the project's directory and the thread ID
 */
public class ExecutorUtils {

    public final static String ATS_HOST_ID                   = "HOST_ID";
    public final static String ATS_WORKDIR                   = "WORKDIR";
    public final static String ATS_THREAD_ID                 = "THREAD_ID";
    public final static String ATS_CALLER_ID_TOKEN_DELIMITER = ";";
    public final static String ATS_TOKEN_DELIMITER           = ":";
    public final static String ATS_CALLER_ID                 = "CALLER_ID";

    public static String createCallerId() {
        
        return ATS_HOST_ID + ATS_TOKEN_DELIMITER + HostUtils.getLocalHostIP() + ATS_CALLER_ID_TOKEN_DELIMITER
                + ATS_WORKDIR + ATS_TOKEN_DELIMITER + IoUtils.normalizeUnixDir(AtsSystemProperties.SYSTEM_USER_DIR)
                + ATS_CALLER_ID_TOKEN_DELIMITER + ATS_THREAD_ID + ATS_TOKEN_DELIMITER + Thread.currentThread().getId();
        
    }

    public static String extractHost( String callerId ) {

        return extractToken(callerId, ATS_HOST_ID);
    }

    public static String extractThreadId( String callerId ) {

        return extractToken(callerId, ATS_THREAD_ID);
    }

    private static String extractToken( String callerId, String key ) {

        for (String token : callerId.split(ATS_CALLER_ID_TOKEN_DELIMITER)) {
            if (token.startsWith(key)) {
                return token.replace(key + ATS_TOKEN_DELIMITER, "").trim();
            }
        }

        return null;
    }
}
