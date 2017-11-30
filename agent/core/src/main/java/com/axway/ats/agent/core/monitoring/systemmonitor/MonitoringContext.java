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
package com.axway.ats.agent.core.monitoring.systemmonitor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;

import com.axway.ats.core.monitoring.MonitoringException;

/**
 * Initializes the Monitoring service by reading the configurations files
 */
public class MonitoringContext {

    private static final Logger      log                               = Logger.getLogger(MonitoringContext.class);

    // the singleton instance
    private static MonitoringContext instance;

    private static final String      DEFAULT_PERFORMANCE_CONFIGURATION = "default.performance.configuration.xml";
    private static final String      CUSTOM_PERFORMANCE_CONFIGURATION  = "custom.performance.configuration.xml";

    /**
     * Private constructor to prevent instantiation
     */
    private MonitoringContext() {

    }

    public static MonitoringContext getInstance() {

        if (instance == null) {
            instance = new MonitoringContext();
        }
        return instance;
    }

    public void init() throws IOException {

        if (!ReadingsRepository.getInstance().isConfigured()) {

            log.info("Initializing the Monitoring library");

            List<String> configurationFiles = new ArrayList<String>();

            // 1. Load the configuration files

            // find the default configuration
            String defaultConfigurationFile = null;
            if (this.getClass().getResource(DEFAULT_PERFORMANCE_CONFIGURATION) != null) {
                defaultConfigurationFile = this.getClass()
                                               .getResource(DEFAULT_PERFORMANCE_CONFIGURATION)
                                               .getFile();
            }
            if (defaultConfigurationFile != null) {
                configurationFiles.add(defaultConfigurationFile);
            } else {
                throw new MonitoringException("Unable to initialize the default monitoring service configuration: Unable to find the "
                                              + DEFAULT_PERFORMANCE_CONFIGURATION + " file.");
            }

            // find the custom configuration searched in the classpath
            Enumeration<URL> customLinuxConfigurationURLs = this.getClass()
                                                                .getClassLoader()
                                                                .getResources(CUSTOM_PERFORMANCE_CONFIGURATION);
            int counter = 0;
            while (customLinuxConfigurationURLs.hasMoreElements()) {
                counter++;
                configurationFiles.add(customLinuxConfigurationURLs.nextElement().getPath());
            }
            if (counter == 0) {
                log.debug("No custom linux configuration detected. It is searched as a "
                          + CUSTOM_PERFORMANCE_CONFIGURATION + " file in the classpath");
            }

            // 2. Parse the configuration files
            ReadingsRepository.getInstance().loadConfigurarions(configurationFiles);

            log.info("Successfully initialized the Monitoring service");
        }
    }
}
