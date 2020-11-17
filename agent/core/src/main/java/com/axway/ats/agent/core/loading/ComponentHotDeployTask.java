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
package com.axway.ats.agent.core.loading;

import java.io.File;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.ComponentRepository;

public class ComponentHotDeployTask implements Runnable {

    private static final Logger    log = LogManager.getLogger(ComponentHotDeployTask.class);

    private File                   componentLocation;
    private DynamicComponentLoader componentLoader;
    private ComponentRepository    componentRepository;

    //variable to hold the last update timestamp
    private HashMap<String, Long>  lastModificationTimes;

    public ComponentHotDeployTask( DynamicComponentLoader componentLoader,
                                   ComponentRepository componentRepository ) {

        this.lastModificationTimes = new HashMap<String, Long>();
        this.componentLocation = componentLoader.getComponentsLocation();
        this.componentLoader = componentLoader;
        this.componentRepository = componentRepository;
    }

    public void run() {

        if (needReload()) {
            try {
                log.info("Detected modified Agent component libraries, reloading...");

                //load the available components
                componentLoader.loadAvailableComponents(componentRepository);

                log.info("Done loading Agent component libraries");

            } catch (Throwable e) {
                //we should catch all exceptions otherwise the deployment thread
                //will stop - this way we give the user the chance to fix the error
                log.error("Exception while deploying Agent components - deployment cancelled", e);
            }
        }
    }

    private boolean needReload() {

        if (!componentLocation.exists() || !componentLocation.isDirectory()) {
            log.error("Component location '" + componentLocation.getAbsolutePath()
                      + "' does not exist or is not a directory - skipping it");
            return false;
        }

        boolean result = false;

        File[] jarFiles = componentLocation.listFiles(new JarFilenameFilter());
        if (jarFiles != null) {
            HashMap<String, Long> currentModificationTimes = getLastModificationTimes(jarFiles);
            if (!currentModificationTimes.equals(lastModificationTimes)) {

                lastModificationTimes = currentModificationTimes;
                result = true;
            }
        }

        return result;
    }

    private HashMap<String, Long> getLastModificationTimes( File[] jarFiles ) {

        HashMap<String, Long> modificationTimes = new HashMap<String, Long>();
        for (File jarFile : jarFiles) {
            modificationTimes.put(jarFile.getAbsolutePath(), jarFile.lastModified());
        }

        return modificationTimes;
    }
}
