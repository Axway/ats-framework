/*
 * Copyright 2019 Axway Software
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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.utils.IoUtils;

/*
 * Class responsible for managing of the UUID that the executor uses to keep session with the ATS Agents
 * */
public class ExecutorUtils {

    private static String uniqueId;

    private static Logger log = LogManager.getLogger(ExecutorUtils.class);

    public static synchronized String getUUID() {

        return getUUID(false);
    }

    public static synchronized String getUUID( boolean useNewUuId ) {

        try {
            // check if new unique id must be generated each time
            if (!useNewUuId) {
                // create temp file containing caller working directory and the unique id
                String userWorkingDirectory = new File(AtsSystemProperties.USER_CURRENT_DIR).getAbsolutePath();
                String uuiFileLocation = AtsSystemProperties.SYSTEM_USER_TEMP_DIR
                                         + AtsSystemProperties.SYSTEM_FILE_SEPARATOR + "ats_uid.txt";
                File uuiFile = new File(uuiFileLocation);

                // check if the file exist and if exist check if the data we need is in, 
                // otherwise add it to the file 
                if (uuiFile.exists()) {
                    String uuiFileContent = IoUtils.streamToString(IoUtils.readFile(uuiFileLocation));
                    if (uuiFileContent.contains(userWorkingDirectory)) {
                        for (String line : uuiFileContent.split("\n")) {
                            if (line.contains(userWorkingDirectory)) {
                                uniqueId = line.substring(userWorkingDirectory.length()).trim();
                                break;
                            }
                        }
                    } else {
                        generateNewUUID();
                        new LocalFileSystemOperations().appendToFile(uuiFileLocation,
                                                                     userWorkingDirectory + "\t" + uniqueId + "\n");
                    }
                } else {
                    generateNewUUID();
                    try {
                        uuiFile.createNewFile();
                    } catch (IOException e) {
                        log.warn("Unable to create UUID file '" + uuiFile.getAbsolutePath() + "'");
                    }
                    if (uuiFile.exists()) {
                        new LocalFileSystemOperations().appendToFile(uuiFileLocation,
                                                                     userWorkingDirectory + "\t" + uniqueId + "\n");
                    }
                }
            } else {
                generateNewUUID();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain UUID", e);
        }

        return uniqueId;
    }

    private static void generateNewUUID() {

        uniqueId = UUID.randomUUID().toString().trim();
    }

}
