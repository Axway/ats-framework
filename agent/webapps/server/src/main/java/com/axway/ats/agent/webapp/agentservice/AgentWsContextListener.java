/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.agent.webapp.agentservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.axway.ats.agent.core.MainComponentLoader;
import com.axway.ats.agent.core.configuration.AgentConfigurator;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.configuration.TemplateActionsConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.ClasspathUtils;

/**
 * Context listener for the ATS Agent web application.
 * It configures the Agent instance and call the bootstrap loader
 */
public class AgentWsContextListener implements ServletContextListener {

    static {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("ContainerStarterConfig");
        builder.add(builder.newRootLogger(Level.INFO));
        // TODO add console appender if docker container does not log properly
        org.apache.logging.log4j.core.config.Configurator.initialize(builder.build());
        log = LogManager.getLogger(AgentWsContextListener.class);
        try {
            addAppender();
        } catch (IOException e) {
            new AtsConsoleLogger(AgentWsContextListener.class).error("Unable to add 'ats-audit-log' file appender to Log4j2 configuration!", e);
        }
    }
    
    private static final Logger log;
    
    private static void addAppender() throws IOException {

        Map<Object, Object> variables = System.getProperties();
        Level logLevel = Level.INFO;
        String pattern = (String) variables.get("logging.pattern");
        String agentPort = (String) variables.get("ats.agent.default.port");
        String agentSeverity = (String) variables.get("logging.severity");

        /*
         * If the log4j2.xml file has user-defined/third-party filters, they must be in the classpath (inside container directory)
         **/
        boolean enableLog4jConfigFile = Boolean.valueOf((String) variables.get("logging.enable.log4j2.file"));
        String log4JFileName = "log4j2.xml"; // on the same level as agent.sh/.bat
        File file = new File(log4JFileName);
        if (file.exists()) {
            System.out.println("ContainerStarter: Found " + log4JFileName + " file in current directory ("
                               + file.getAbsolutePath()
                               + ") and will be used instead of default ATS logging configuration.");
            ConfigurationSource source = new ConfigurationSource(new FileInputStream(file));
            org.apache.logging.log4j.core.config.Configurator.initialize(null, source);
            // possibly manage log level of Jetty
            return;
        }
        if (enableLog4jConfigFile) {
            String dir = System.getProperty("ats.agent.home");
            StringBuilder fullPath = new StringBuilder();
            if (dir != null && dir.trim().length() > 0) {
                fullPath.append(dir);
                if (!dir.endsWith("/") && !dir.endsWith("\\")) {
                    fullPath.append("/");
                }
            }
            fullPath.append("ats-agent/container/log4j2.xml");
            System.out.println("ContainerStarter: Loading log4j2.xml file from " + fullPath);
            ConfigurationSource source = new ConfigurationSource(new FileInputStream(fullPath.toString()));
            org.apache.logging.log4j.core.config.Configurator.initialize(null, source);
        }

        // check agent logging severity and set the appropriate level
        if (agentSeverity != null) {
            if ("INFO".equalsIgnoreCase(agentSeverity)) {
                logLevel = Level.INFO;
            } else if ("DEBUG".equalsIgnoreCase(agentSeverity)) {
                logLevel = Level.DEBUG;
            } else if ("WARN".equalsIgnoreCase(agentSeverity)) {
                logLevel = Level.WARN;
            } else if ("ERROR".equalsIgnoreCase(agentSeverity)) {
                logLevel = Level.ERROR;
            } else if ("FATAL".equalsIgnoreCase(agentSeverity)) {
                logLevel = Level.FATAL;
            } else {
                log.info("Unknown severity level is set: " + agentSeverity
                         + ". Possible values are: DEBUG, INFO, WARN, ERROR, FATAL.");
            }
        }

        String logPath = "./logs/ATSAgentAudit_" + agentPort + ".log";
        PatternLayout layout = PatternLayout.newBuilder().withPattern("%d{DEFAULT} - {%p} [%t] %c{2}: %m%n").build();

        Logger rootLogger = LogManager.getRootLogger();
        Appender attachedAppender = null;
        if (pattern != null && !pattern.trim().isEmpty()) {
            pattern = pattern.trim().toLowerCase();
            if ("day".equals(pattern)) {
                attachedAppender = RollingFileAppender.newBuilder()
                                                      .setName("ats-audit-log-appender")
                                                      .setLayout(layout)
                                                      .withFilePattern(
                                                                       logPath + "'.'MM-dd'.log'")
                                                      .build();
            } else if ("hour".equals(pattern)) {
                attachedAppender = RollingFileAppender.newBuilder()
                                                      .setName("ats-audit-log-appender")
                                                      .setLayout(layout)
                                                      .withFilePattern(
                                                                       logPath + "'.'MM-dd-HH'.log'")
                                                      .build();
            } else if ("minute".equals(pattern)) {
                attachedAppender = RollingFileAppender.newBuilder()
                                                      .setName("ats-audit-log-appender")
                                                      .setLayout(layout)
                                                      .withFilePattern(
                                                                       logPath + "'.'MM-dd-HH-mm'.log'")
                                                      .build();
            } else if (pattern.endsWith("kb") || pattern.endsWith("mb") || pattern.endsWith("gb")) {

                //attachedAppender = new SizeRollingFileAppender(layout, logPath, true);
                //((SizeRollingFileAppender) attachedAppender).setMaxFileSize(pattern);
                //((SizeRollingFileAppender) attachedAppender).setMaxBackupIndex(10);

                attachedAppender = RollingFileAppender.newBuilder()
                                                      .setName("ats-audit-log-appender")
                                                      //.withAdvertise(true|false)
                                                      //.withAdvertiseUri(advertiseUri)
                                                      .withAppend(true)
                                                      //.withBufferedIo(true|false)
                                                      //.withBufferSize(bufferSize)
                                                      //.setConfiguration(config)
                                                      //.withFileName(fileName)
                                                      .withFilePattern(pattern)
                                                      //.setFilter(filter)
                                                      //.setIgnoreExceptions(Booleans.parseBoolean(ignore, true))
                                                      //.withImmediateFlush(Booleans.parseBoolean(immediateFlush, true)).setLayout(layout)
                                                      //.withCreateOnDemand(false)
                                                      //.withLocking(false)
                                                      //.setName(name)
                                                      .withPolicy(SizeBasedTriggeringPolicy.createPolicy(pattern))
                                                      .withStrategy(DefaultRolloverStrategy.newBuilder()
                                                                                           .withMax("10")
                                                                                           .build())
                                                      .build();

            } else {
                System.err.println("ERROR: '" + pattern
                                   + "' is invalid pattern for log4j2 rolling file appender");
                System.exit(1);
            }
        }
        if (attachedAppender == null) {
            //  default overwrite
            attachedAppender = FileAppender.newBuilder()
                                           .setName("ats-audit-log-appender")
                                           .withFileName(logPath)
                                           .setLayout(layout)
                                           .withAppend(true)
                                           .build();

        }

        org.apache.logging.log4j.core.config.Configurator.setRootLevel(logLevel);
        LoggerContext context = LoggerContext.getContext(true);
        Configuration config = context.getConfiguration();
        //attachedAppender.activateOptions();
        // start() is almost the same as activateOptions()
        attachedAppender.start(); // Always start an Appender prior to adding it to a logger.
        ((org.apache.logging.log4j.core.Logger) rootLogger).addAppender(attachedAppender);

        // adding filter for Jetty messages
        Logger mortbayLogger = LogManager.getLogger("org.mortbay");
        ((org.apache.logging.log4j.core.Logger) mortbayLogger).setAdditive(false);
        org.apache.logging.log4j.core.config.Configurator.setLevel(mortbayLogger.getName(), Level.ERROR);
        ((org.apache.logging.log4j.core.Logger) mortbayLogger).addAppender(attachedAppender);

    }

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
