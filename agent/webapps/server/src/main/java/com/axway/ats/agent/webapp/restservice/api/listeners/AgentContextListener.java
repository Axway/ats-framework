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
package com.axway.ats.agent.webapp.restservice.api.listeners;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.MainComponentLoader;
import com.axway.ats.agent.core.configuration.AgentConfigurator;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.configuration.TemplateActionsConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.core.utils.ClasspathUtils;

/**
 * Context listener for the ATS Agent web application.
 * It configures the Agent instance and call the bootstrap loader
 */
public class AgentContextListener implements ServletContextListener {

    private static final Logger log = Logger.getLogger(AgentContextListener.class);

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.servlet.ServletContextListener#contextInitialized(javax.servlet
     * .ServletContextEvent)
     */
    @Override
    public void contextInitialized( ServletContextEvent servletEvent ) {

        ServletContext servletContext = servletEvent.getServletContext();
        servletContext.log("Servlet context initialized event is received. Starting registering configurators");
        try {
            new ClasspathUtils().logProblematicJars();
        } catch (RuntimeException e) {
            log.warn("Error caught while trying to get all JARs in classpath", e);
            // do not rethrow exception as this will stop deployment on incompliant servers like JBoss
        }

        // create the default web service configurator
        String pathToConfigFile = servletContext.getRealPath("/WEB-INF");
        AgentConfigurator defaultConfigurator = new AgentConfigurator(pathToConfigFile);
        TemplateActionsConfigurator templateActionsConfigurator = new TemplateActionsConfigurator(pathToConfigFile);
        List<Configurator> configurators = new ArrayList<Configurator>();
        configurators.add(defaultConfigurator);
        configurators.add(templateActionsConfigurator);

        log.info("Initializing ATS Agent web service, start component registration");

        try {
            MainComponentLoader.getInstance().initialize(configurators);
        } catch (AgentException ae) {
            throw new RuntimeException("Unable to initialize Agent component loader", ae);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seejavax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
     * ServletContextEvent)
     */
    @Override
    public void contextDestroyed( ServletContextEvent servletEvent ) {

        // stop the component loader
        try {
            MainComponentLoader.getInstance().destroy();
        } catch (AgentException ae) {
            throw new RuntimeException("Unable to de-initialize Agent web service", ae);
        }
    }
}
