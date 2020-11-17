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
package com.axway.ats.agent.core;

import java.io.File;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.configuration.ConfigurationManager;
import com.axway.ats.agent.core.configuration.ConfigurationSettings;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.loading.ComponentHotDeployTask;
import com.axway.ats.agent.core.loading.DynamicComponentLoader;
import com.axway.ats.core.utils.HostUtils;

/**
 * This class is responsible for initializing and configuring the Agent core
 * system and loading all components
 */
public class MainComponentLoader {

    private static Logger               log = LogManager.getLogger(MainComponentLoader.class);
    private static MainComponentLoader  instance;

    // hold the future of the scheduled hot deployment monitor
    private ScheduledFuture<?>          scheduledFuture;
    private ScheduledThreadPoolExecutor componentMonitor;

    // the loading mutex
    private Object                      loadingMutex;

    /**
     * Private constructor to prevent instantiation
     */
    private MainComponentLoader() {

        loadingMutex = new Object();
    }

    /**
     * Get the singleton instance of the main component loader
     *
     * @return the instance
     */
    public static synchronized MainComponentLoader getInstance() {

        if (instance == null) {
            instance = new MainComponentLoader();
        }

        return instance;
    }

    /**
     * Initialize the Agent system - this method will start the appropriate component
     * loader which will load all available components
     *
     * @param configurators list of configurators to be used for configuring the system
     * @throws AgentException on error
     */
    public void initialize( List<Configurator> configurators ) throws AgentException {

        // first initialize the configuration
        log.info("Configuring Agent system on " + HostUtils.getLocalHostIP() + "...");
        ConfigurationManager.getInstance().apply(configurators);
        log.info("Done configuring Agent system");

        // now start loading the components
        log.info("Start Agent component registration");
        startComponentLoader();
    }

    /**
     * Shut down the Agent system - all loaded components will be finalized and
     * unloaded
     *
     * @throws AgentException on error
     */
    public void destroy() throws AgentException {

        log.info("Deinitializing Agent ...");
        ConfigurationManager.getInstance().revert();
        log.info("Done deinitializing Agent");

        // stop the component loader first to prevent new component loading
        stopComponentLoader();

        // finalize all components
        ComponentRepository.getInstance().finalizeAllComponents();

    }

    /**
     * This method will block the execution of the current thread if components are
     * in the process of being loaded.
     */
    public void blockIfLoading() {

        // this will block if loading occurs
        synchronized (loadingMutex) {}
    }

    /**
     * Start the configured component loader
     *
     * @throws AgentException
     *             on error 
     */
    private void startComponentLoader() throws AgentException {

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        ConfigurationSettings settings = ConfigurationSettings.getInstance();

        log.info("Loading Agent component libraries from '" + settings.getComponentsFolder() + "'");

        // the default loader is the dynamic loader for hot deployment
        DynamicComponentLoader componentLoader = new DynamicComponentLoader(new File(settings.getComponentsFolder()),
                                                                            loadingMutex);

        log.info("Starting hot deployment thread");

        componentMonitor = new ScheduledThreadPoolExecutor(1);
        scheduledFuture = componentMonitor.scheduleAtFixedRate(new ComponentHotDeployTask(componentLoader,
                                                                                          componentRepository),
                                                               settings.getMonitorInitialDelay(),
                                                               settings.getMonitorPollInterval(),
                                                               TimeUnit.SECONDS);
    }

    /**
     * Stop the component loader
     */
    private void stopComponentLoader() {

        // now cancel the hot deployment thread
        log.info("Shutting down hot deployment thread");
        scheduledFuture.cancel(true);
        componentMonitor.shutdown();
    }
}
