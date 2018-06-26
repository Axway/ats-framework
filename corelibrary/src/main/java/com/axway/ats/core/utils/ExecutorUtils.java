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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;

/**
 * Helps identifying who is running some job.
 * It may contain the name of the thread, but also some info about the host etc.
 */
public class ExecutorUtils {

    private static Logger      log                             = Logger.getLogger(ExecutorUtils.class);

    public final static String ATS_HOST_ID                     = "HOST_ID";
    public final static String ATS_RANDOM_TOKEN                = "RANDOM_TOKEN";
    public final static String ATS_THREAD_ID                   = "THREAD_ID";
    public final static String ATS_EXECUTOR_ID_TOKEN_DELIMITER = ";";

    // A universe wide ;) random token used to identify the Test Executor.
    // Test Executors are different when started on different hosts.
    // Test Executors are different when started on same hosts, but different work folders.
    private static String      userRandomToken;

    private static boolean     reuseUserRandomToken            = true;

    /**
     * @return random token for the current executor
     */
    public static String getUserRandomToken() {

        if (reuseUserRandomToken) {
            // token can be reused
            if (userRandomToken == null) {
                // we come here for first time, so we do not have a token

                // try to use the token from the previous execution
                // we keep the used tokens into a local temporary file
                String userWorkingDirectory = AtsSystemProperties.SYSTEM_USER_HOME_DIR;
                String uuidFileLocation = AtsSystemProperties.SYSTEM_USER_TEMP_DIR
                                          + AtsSystemProperties.SYSTEM_FILE_SEPARATOR
                                          + "ats_user_random_tokens.txt";
                File uuidFile = new File(uuidFileLocation);

                if (uuidFile.exists()) {
                    // try to extract the random token from the file
                    String uuidFileContent;
                    try {
                        uuidFileContent = IoUtils.streamToString(IoUtils.readFile(uuidFileLocation));
                    } catch (IOException e) {
                        throw new FileSystemOperationException("Unable to extract randome user token from file '"
                                                               + uuidFile.getPath() + "'", e);
                    }
                    if (uuidFileContent.contains(userWorkingDirectory)) {
                        // the needed token is in there, extract it
                        for (String line : uuidFileContent.split("\n")) {
                            if (line.contains(userWorkingDirectory)) {
                                userRandomToken = line.substring(userWorkingDirectory.length()).trim();
                                break;
                            }
                        }
                    } else {
                        // the file does not contain the needed token, create a new one
                        userRandomToken = generateNewRandomToken();
                        new LocalFileSystemOperations().appendToFile(uuidFileLocation,
                                                                     userWorkingDirectory + "\t"
                                                                                       + userRandomToken
                                                                                       + "\n");
                    }
                } else {
                    // no such file, create a new one
                    userRandomToken = generateNewRandomToken();
                    try {
                        uuidFile.createNewFile();
                    } catch (IOException e) {
                        log.warn("Unable to create file '" + uuidFile.getAbsolutePath() + "'");
                    }
                    if (uuidFile.exists()) {
                        new LocalFileSystemOperations().appendToFile(uuidFileLocation,
                                                                     userWorkingDirectory + "\t"
                                                                                       + userRandomToken
                                                                                       + "\n");
                    }
                }
            }
        } else {
            // token cannot be reused, create a new one
            userRandomToken = generateNewRandomToken();
        }

        return userRandomToken;
    }

    public static void setReuseUserRandomToken( boolean reuseUuId ) {

        ExecutorUtils.reuseUserRandomToken = reuseUuId;
    }

    public static String createExecutorId( String host, String randomToken, String threadId ) {

        return ATS_HOST_ID + ":" + host + ATS_EXECUTOR_ID_TOKEN_DELIMITER + ATS_RANDOM_TOKEN + ":" + randomToken
               + ATS_EXECUTOR_ID_TOKEN_DELIMITER + ATS_THREAD_ID
               + ": " + threadId;
    }

    public static String extractHost( String sessionId ) {

        return extractToken(sessionId, ATS_HOST_ID);
    }

    public static String extractThread( String sessionId ) {

        return extractToken(sessionId, ATS_THREAD_ID);
    }

    private static String extractToken( String sessionId, String key ) {

        for (String token : sessionId.split(ATS_EXECUTOR_ID_TOKEN_DELIMITER)) {
            if (token.startsWith(key)) {
                return token.replace(key + ":", "").trim();
            }
        }

        return null;
    }

    private static String generateNewRandomToken() {

        return UUID.randomUUID().toString();
    }
}
