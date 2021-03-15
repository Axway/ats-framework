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
package com.axway.ats.agentapp.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

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
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import com.axway.ats.agentapp.standalone.exceptions.AgentException;
import com.axway.ats.agentapp.standalone.utils.AtsVersionExtractor;
import com.axway.ats.agentapp.standalone.utils.CleaningThread;
import com.axway.ats.agentapp.standalone.utils.ThreadUtils;

public class ContainerStarter {

    static {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("ContainerStarterConfig");
        builder.add(builder.newRootLogger(Level.INFO));
        Configurator.initialize(builder.build());
        log = LogManager.getLogger(ContainerStarter.class);
    }

    private static final Logger log;
    private static final String DEFAULT_AGENT_PORT_KEY   = "ats.agent.default.port"; // NOTE: on change sync with AtsSystemProperties
    private static final int    DEFAULT_AGENT_PORT_VALUE = 8089;                     // NOTE: on change sync with AtsSystemProperties

    /**
     * Entry point for the premain java agent starting the ATS Agent
     * @param agentArgs
     * @param inst
     * @throws IOException
     */
    public static void premain( String agentArgs, Instrumentation inst ) throws IOException {

        Server server = startServer();
        startCleanerThread(server);
    }

    /**
     * Entry point for a standalone application starting the ATS Agent
     * @param args
     * @throws IOException
     */
    public static void main( String[] args ) throws IOException {

        startServer();

        writePidFile();
    }

    /**
     * Method for starting the Jetty server with the ATS Agent webapp.
     * @return the started server.
     * @throws IOException
     */
    private static Server startServer() throws IOException {

        addAppender();

        final int agentPort = getAgentDefaultPort();
        log.info("Starting ATS agent at port: " + agentPort);

        final String jettyHome = getJettyHome();

        logSystemInformation(jettyHome);

        // start the server
        Connector connector = new SelectChannelConnector();
        connector.setPort(agentPort);

        Server server = new Server();
        server.setConnectors(new Connector[]{ connector });

        WebAppContext webApp = new WebAppContext();
        webApp.setContextPath("/agentapp");
        webApp.setWar(jettyHome + "/webapp/agentapp.war");
        webApp.setAttribute("org.eclipse.jetty.webapp.basetempdir",
                            getJettyWorkDir(jettyHome));

        server.setHandler(webApp);
        server.setStopAtShutdown(true);

        setExtraClasspath(webApp, jettyHome);

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        log.info("ATS agent started");
        return server;
    }

    /**
     * Method for starting the thread that will wait for all non daemon threads
     * to finish except the threads of the server and stop the server and JVM.
     * <i>This method is needed only when the server is started from a javaagent</i>
     * @param server
     */
    private static void startCleanerThread( Server server ) {

        // Wait for the server to start.
        // Make more attempts in short interval in order to stop this check
        // as soon as possible.
        int maxAttempts = 100;
        while (!server.isRunning()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for the agent to start.", e);
            }

            if (maxAttempts == 0) {
                throw new AgentException("Jetty server not running.");
            }

            --maxAttempts;
        }

        CleaningThread cleanerThread = new CleaningThread(server, ThreadUtils.getInstance()
                                                                             .getAllThreadIDsExceptMain());
        cleanerThread.start();
    }

    /**
     * @return the Jetty home directory
     */
    private static String getJettyHome() {

        // find the current class in the agent jar.
        String jettyHome = ContainerStarter.class.getClassLoader()
                                                 .getResource(ContainerStarter.class.getCanonicalName()
                                                                                    .replaceAll("\\.", "/")
                                                              + ".class")
                                                 .toString();
        // get rid of the 'jar:' prefix and the path after the jar's name
        jettyHome = jettyHome.substring(4, jettyHome.lastIndexOf('!'));
        // get rid of the jar name at the end
        jettyHome = jettyHome.substring(0, jettyHome.lastIndexOf('/'));
        // Directly read system property in order not to add dependency to common library
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");
        if (isWindows) {
            jettyHome = jettyHome.substring("file:/".length());
        } else {
            jettyHome = jettyHome.substring("file:".length());
        }
        try {
            jettyHome = URLDecoder.decode(jettyHome, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to decode Jetty home path", e);
        }

        return jettyHome;
    }

    /**
     * @param jettyHome
     * @return the folder where our web application will be deployed
     */
    private static String getJettyWorkDir( String jettyHome ) {

        // the folder where the web application will be deployed
        final String jettyWorkDir = jettyHome + "/work";

        /* Make the folder if does not exist.
         * If cannot make this folder for some reason, no error will be reported.
         * Then Jetty will see this folder does not exist and will use the folder
         * pointed by the java.io.tmpdir system property
         */
        File workDirF = new File(jettyWorkDir);
        if (! workDirF.exists()) {
            if (!workDirF.mkdir()) {
                System.err.println("Could not create work directory '" + jettyWorkDir
                                   + "'. Check current user's permissions for this directory. Jetty will generate its work "
                                   + "directory inside OS temp directory.");
            }
        }

        return jettyWorkDir;
    }

    private static void setExtraClasspath( WebAppContext webApp, String jettyHome ) {

        final String lineSeparator = System.getProperty("line.separator");

        String jarFilesReference = getJarFilesReference(jettyHome + "/actions_dependencies");
        webApp.setExtraClasspath(jarFilesReference);
        log.debug("Additional libraries inserted into Jetty's classpath: " + lineSeparator
                  + jarFilesReference.replaceAll(",;", lineSeparator));
    }

    /**
     * Browse a folder for jar files (recursively)
     *
     * @param folder the folder to search into
     * @return a list with all found jars
     */
    private static String getJarFilesReference( String folder ) {

        StringBuffer jarsReference = new StringBuffer();

        try {
            File[] files = new File(folder).listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        jarsReference.append(getJarFilesReference(file.getAbsolutePath()));
                    } else if (file.getName().endsWith(".jar")) {
                        jarsReference.append(folder);
                        jarsReference.append("/");
                        jarsReference.append(file.getName());
                        jarsReference.append(",;");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error searching for jar files into '" + folder + "' folder");
        }

        return jarsReference.toString();
    }

    /**
     * Read agent port to use - either the specified system property or the hardcoded default one
     * @return ATS agent port number to use
     */
    private static int getAgentDefaultPort() {

        Integer defaultPort = null;
        String portValueAsStr = null;
        try {
            portValueAsStr = System.getProperty(DEFAULT_AGENT_PORT_KEY);
            defaultPort = Integer.parseInt(portValueAsStr);
        } catch (NumberFormatException iae) {
            System.err.println("System property with name '" + DEFAULT_AGENT_PORT_KEY
                               + "' has a non integer value '" + portValueAsStr + "'");
        }

        if (defaultPort == null) {
            defaultPort = DEFAULT_AGENT_PORT_VALUE;
        }
        return defaultPort;
    }

    private static void writePidFile() {

        String pid = getCurrentProcessId();
        if (pid == null) {
            log.warn("Uable to get the current process ID, which means that we can't stop the agent later");
            return;
        }

        String jettyHome = getJettyHome();
        String logsFolderPath = new File(jettyHome).getParent();
        String pidFilePath = logsFolderPath + "/logs/atsAgent_" + getAgentDefaultPort() + ".pid";

        File pidFile = new File(pidFilePath);
        if (pidFile.exists()) {
            log.warn("PID file '" + pidFile.getAbsolutePath()
                     + "' already exists. We will overwrite it now!");
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(pidFile, false);
            writer.write(pid);
        } catch (Exception e) {
            log.warn("Error writing pid file");
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.warn("Error closing pid file");
                }
            }
        }
    }

    /**
     * This may fail on some JMV implementations
     *
     * @return the PID of this java process
     */
    private static String getCurrentProcessId() {

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            // part before '@' empty
            log.warn("Cannot extract the system process ID of this agent instance");
            return null;
        }

        try {
            return Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } catch (NumberFormatException e) {
            log.warn("Cannot extract the system process ID of this agent instance");
            return null;
        }
    }

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
            Configurator.initialize(null, source);
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
            Configurator.initialize(null, source);
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
                                           .withAppend(false)
                                           .build();

        }

        Configurator.setRootLevel(logLevel);
        LoggerContext context = LoggerContext.getContext(true);
        Configuration config = context.getConfiguration();
        //attachedAppender.activateOptions();
        // start() is almost the same as activateOptions()
        attachedAppender.start(); // Always start an Appender prior to adding it to a logger.
        ((org.apache.logging.log4j.core.Logger) rootLogger).addAppender(attachedAppender);

        // adding filter for Jetty messages
        Logger mortbayLogger = LogManager.getLogger("org.mortbay");
        ((org.apache.logging.log4j.core.Logger) mortbayLogger).setAdditive(false);
        Configurator.setLevel(mortbayLogger.getName(), Level.ERROR);
        ((org.apache.logging.log4j.core.Logger) mortbayLogger).addAppender(attachedAppender);

    }

    private static List<InetAddress> getAllIPAddresses() {

        List<InetAddress> ipList = new ArrayList<InetAddress>();
        try {

            // cycle all net interfaces
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            log.debug("---> Start Iterating All Network Interfaces!");
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) netInterfaces.nextElement();
                if (!netInterface.isLoopback()) {
                    log.debug("---> Start Iterating Interface '" + netInterface.getName() + "'!");
                    // for each net interface cycle all IP addresses
                    Enumeration<InetAddress> ipAddresses = netInterface.getInetAddresses();
                    InetAddress ipAddress = null;
                    while (ipAddresses.hasMoreElements()) {
                        ipAddress = (InetAddress) ipAddresses.nextElement();

                        if (ipAddress instanceof java.net.Inet4Address) {
                            Inet4Address ipv4 = (Inet4Address) ipAddress;
                            if (!ipv4.isLoopbackAddress()) {
                                // we found an appropriate IPv4 address
                                ipList.add(ipv4);
                            }
                        } else //if( ip instanceof java.net.Inet6Address )
                        {
                            Inet6Address ipv6 = (Inet6Address) ipAddress;
                            // FIXME: currently we do not filter out the temporary IPv6 addresses
                            if (!ipv6.isLinkLocalAddress()) {
                                // We found an appropriate IPv6 address. Remember it, but keep searching for an appropriate IPv4 address.
                                ipList.add(ipv6);
                            }
                        }
                    }
                }
            }
            log.debug("---> Finish Iterating All Network Interfaces!");
        } catch (SocketException se) {
            log.error("Error obtaining the local host address", se);
        }
        return ipList;
    }

    private static void logSystemInformation( String jettyHome ) {

        StringBuilder systemInformation = new StringBuilder();

        try {
            appendMessage(systemInformation, "ATS version: '",
                          AtsVersionExtractor.getATSVersion(jettyHome + "/webapp/agentapp.war"));
        } catch (Exception e) {
            log.warn("Could not parse ATS version. Agent will continue the start operation", e);
        }
        appendMessage(systemInformation, " os.name: '", System.getProperty("os.name"));
        appendMessage(systemInformation, " os.arch: '", System.getProperty("os.arch"));
        appendMessage(systemInformation, " java.version: '", System.getProperty("java.version"));
        appendMessage(systemInformation, " java.home: '", System.getProperty("java.home"));
        appendMessage(systemInformation, " current directory: '", System.getProperty("user.dir"));
        appendMessage(systemInformation, " current user name: '", System.getProperty("user.name"));

        List<String> ipList = new ArrayList<String>();
        for (InetAddress ip : getAllIPAddresses()) {
            ipList.add(ip.getHostAddress());
        }
        appendMessage(systemInformation, " IP addresses: '", ipList.toString());

        log.info("System information : " + systemInformation.toString());
    }

    private static void appendMessage( StringBuilder message, String valueDesc, String value ) {

        if (value != null && value.length() > 0) {
            if (message.length() > 0) {
                message.append(",");
            }
            message.append(valueDesc + value + "'");
        }
    }
}
