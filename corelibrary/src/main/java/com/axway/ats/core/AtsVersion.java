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
package com.axway.ats.core;

import java.io.InputStream;
import java.util.Properties;

import com.axway.ats.core.log.AtsConsoleLogger;

/**
 * A file in the root of the classpath with name "ats.version"
 * is expected to contain just one line which is the version
 * of the ATS framework.
 *
 * The file content format is "version=2.2.2"
 */
public class AtsVersion {

    public final static String VERSION_KEY = "version";

    private static String      ATS_FRAMEWORK_VERSION;

    static {
        try (InputStream is = AtsVersion.class.getResourceAsStream("/ats.version")) {
            Properties versionProperties = new Properties();
            versionProperties.load(is);

            ATS_FRAMEWORK_VERSION = versionProperties.getProperty(VERSION_KEY);
        } catch (Exception e) {
            ATS_FRAMEWORK_VERSION = "";

            AtsConsoleLogger atsConsoleLogger = new AtsConsoleLogger(AtsVersion.class);

            atsConsoleLogger.warn("Unknown ATS framework version. Following is the error reading the internal version of the ATS framework");
            e.printStackTrace();
        }
    }

    public static String getAtsVersion() {

        return ATS_FRAMEWORK_VERSION;
    }
}
