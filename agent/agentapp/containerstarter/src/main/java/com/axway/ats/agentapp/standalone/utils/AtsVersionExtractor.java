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
package com.axway.ats.agentapp.standalone.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class AtsVersionExtractor {

    private static final String WAR_FILE_RELATIVE_LOCATION = "webapp/agentapp.war";
    private static final String ATS_VERSION_FILE           = "ats.version";

    public static void main( String[] args ) throws Exception {

        System.out.println(getATSVersion(getWarLocation()));
    }

    public static String getATSVersion( String warFileLocation ) throws Exception {

        ZipInputStream coreJarInputStream = null;
        ZipFile war = new ZipFile(new File(warFileLocation));
        try {
            Enumeration<? extends ZipEntry> warEntries = war.entries();
            ZipEntry autoCoreJar = null;
            while (warEntries.hasMoreElements()) {
                ZipEntry zipEntry = warEntries.nextElement();
                if (zipEntry.getName().contains("WEB-INF/lib/ats-core")) { // including
                                                                           // ats-corelibrary
                    autoCoreJar = zipEntry;
                    break;
                }
            }
            if (autoCoreJar == null) {
                throw new RuntimeException("Unable to find ats-core JAR in WEB-INF/lib/ folder of the war file");
            }

            coreJarInputStream = new ZipInputStream(war.getInputStream(autoCoreJar));
            ZipEntry zipEntry = null;
            while ( (zipEntry = coreJarInputStream.getNextEntry()) != null) {
                if (ATS_VERSION_FILE.equals(zipEntry.getName())) {

                    // Could be used Properties for parsing if more values are expected in the future
                    byte[] bytes = new byte[100];
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    int bytesRead = 0;

                    do {
                        outStream.write(bytes, 0, bytesRead);
                        bytesRead = coreJarInputStream.read(bytes);
                    } while (bytesRead > 0);
                    return new String(outStream.toByteArray(), StandardCharsets.UTF_8);
                }
            }
        } finally {
            if (coreJarInputStream != null) {
                coreJarInputStream.close();
            }
            if (war != null) {
                war.close();
            }
        }
        return "File named " + ATS_VERSION_FILE + " is not found";
    }

    private static String getWarLocation() {

        try {
            String currentJarPath = AtsVersionExtractor.class.getProtectionDomain()
                                                             .getCodeSource()
                                                             .getLocation()
                                                             .getPath();
            String decodedPath = URLDecoder.decode(currentJarPath, "UTF-8"); // solve the problem with spaces and
                                                                             // special characters
            File currentJarFile = new File(decodedPath);
            decodedPath = currentJarFile.getParentFile().getCanonicalPath();
            if (!decodedPath.endsWith("/") && !decodedPath.endsWith("\\")) {
                decodedPath = decodedPath + "/";
            }
            return decodedPath + WAR_FILE_RELATIVE_LOCATION;
        } catch (Exception e) {
            throw new RuntimeException("Unable to find " + WAR_FILE_RELATIVE_LOCATION + " file", e);
        }
    }
}
