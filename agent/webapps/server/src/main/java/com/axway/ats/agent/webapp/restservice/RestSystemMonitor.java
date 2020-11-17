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
package com.axway.ats.agent.webapp.restservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.monitoring.AgentSystemMonitor;
import com.axway.ats.agent.core.monitoring.UserActionsMonitoringAgent;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.core.monitoring.MonitorConfigurationException;
import com.axway.ats.core.monitoring.MonitoringException;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

public class RestSystemMonitor {

    public static final String  ATS_JVM_MONITOR_CLASS_FULL_NAME = "com.axway.ats.agent.core.monitoring.jvmmonitor.AtsJvmMonitor";

    private static final Logger log                             = LogManager.getLogger(RestSystemMonitor.class);

    private boolean             isStarted                       = false;

    // whether we have to log user activity data
    private boolean             logUserActivity                 = false;

    // whether we have to log system statistics data
    private boolean             logSystemStatistics             = false;

    private Set<ReadingBean>    readingTypes;

    private AgentSystemMonitor  systemMonitor;

    public RestSystemMonitor() {

        readingTypes = new HashSet<>();
        systemMonitor = new AgentSystemMonitor();
    }

    public void initializeMonitoringContext(
                                             @Validate(
                                                     name = "monitoredHost",
                                                     type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        new Validator().validateMethodParameters("Could not schedule monitoring system statistics on '"
                                                 + monitoredHost + "'", new Object[]{ monitoredHost });
        try {
            log.debug("Initializing monitoring context...");
            systemMonitor.initializeMonitoringContext();
            log.info("Monitoring context initialized.");
        } catch (Exception e) {
            log.error("Could not initialize monitoring context.", e);
            throw new MonitorConfigurationException("Could not initialize monitoring context", e);
        }

    }

    public Set<ReadingBean> scheduleSystemMonitoring(
                                                      @Validate(
                                                              name = "monitoredHost",
                                                              type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                      @Validate(
                                                              name = "systemReadingTypes",
                                                              type = ValidationType.NOT_NULL) String[] systemReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        new Validator().validateMethodParameters("Could not schedule monitoring system statistics on '"
                                                 + monitoredHost + "'",
                                                 new Object[]{ monitoredHost, systemReadingTypes });

        Set<ReadingBean> readingTypes = new HashSet<ReadingBean>();
        try {
            log.debug("Scheduling system monitoring...");
            readingTypes.addAll(systemMonitor.scheduleSystemMonitoring(systemReadingTypes));
            logSystemStatistics = true;
            log.info("System monitoring scheduled.");
        } catch (Exception e) {
            log.error("Could not schedule system monitoring.", e);
            throw new MonitoringException("Could not schedule system monitoring. Did you initialize the monitoring context?",
                                          e);
        }

        return readingTypes;
    }

    public Set<ReadingBean> scheduleMonitoring(
                                                @Validate(
                                                        name = "monitoredHost",
                                                        type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                @Validate(
                                                        name = "readingType",
                                                        type = ValidationType.STRING_NOT_EMPTY) String readingType,
                                                @Validate(
                                                        name = "readingParameters",
                                                        type = ValidationType.NOT_NULL) Map<String, String> readingParameters ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        new Validator().validateMethodParameters("Could not schedule monitoring a statistic on '"
                                                 + monitoredHost + "'",
                                                 new Object[]{ monitoredHost,
                                                               readingType,
                                                               readingParameters });

        Set<ReadingBean> readingTypes = new HashSet<ReadingBean>();

        Set<String> readingNames = new HashSet<String>();
        readingNames.add(readingType);

        ReadingBean reading = null;
        try {
            log.debug("Scheduling monitoring...");
            reading = systemMonitor.scheduleMonitoring(readingType, readingParameters);
            logSystemStatistics = true;
            log.info("Monitoring scheduled.");
        } catch (Exception e) {
            log.error("Could not schedule monitoring.", e);
            throw new MonitoringException("Could not schedule monitoring. Did you initialize the monitoring context?",
                                          e);
        }
        readingTypes.add(reading);

        return readingTypes;
    }

    public Set<ReadingBean> scheduleProcessMonitoring(
                                                       @Validate(
                                                               name = "monitoredHost",
                                                               type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                       @Validate(
                                                               name = "processPattern",
                                                               type = ValidationType.STRING_NOT_EMPTY) String processPattern,
                                                       @Validate(
                                                               name = "processAlias",
                                                               type = ValidationType.STRING_NOT_EMPTY) String processAlias,
                                                       @Validate(
                                                               name = "processReadingTypes",
                                                               type = ValidationType.NOT_NULL) String[] processReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        new Validator().validateMethodParameters(new Object[]{ monitoredHost,
                                                               processPattern,
                                                               processAlias,
                                                               processReadingTypes });

        return scheduleProcessMonitoring(monitoredHost,
                                         null,
                                         processPattern,
                                         processAlias,
                                         null,
                                         processReadingTypes);
    }

    public Set<ReadingBean> scheduleProcessMonitoring(
                                                       @Validate(
                                                               name = "monitoredHost",
                                                               type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                       @Validate(
                                                               name = "processPattern",
                                                               type = ValidationType.STRING_NOT_EMPTY) String processPattern,
                                                       @Validate(
                                                               name = "processAlias",
                                                               type = ValidationType.STRING_NOT_EMPTY) String processAlias,
                                                       @Validate(
                                                               name = "processUsername",
                                                               type = ValidationType.NONE) String processUsername,
                                                       @Validate(
                                                               name = "processReadingTypes",
                                                               type = ValidationType.NOT_NULL) String[] processReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        new Validator().validateMethodParameters(new Object[]{ monitoredHost,
                                                               processPattern,
                                                               processAlias,
                                                               processUsername,
                                                               processReadingTypes });

        return scheduleProcessMonitoring(monitoredHost,
                                         null,
                                         processPattern,
                                         processAlias,
                                         processUsername,
                                         processReadingTypes);
    }

    public Set<ReadingBean> scheduleChildProcessMonitoring(
                                                            @Validate(
                                                                    name = "monitoredHost",
                                                                    type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                            @Validate(
                                                                    name = "parentProcess",
                                                                    type = ValidationType.STRING_NOT_EMPTY) String parentProcess,
                                                            @Validate(
                                                                    name = "processPattern",
                                                                    type = ValidationType.STRING_NOT_EMPTY) String processPattern,
                                                            @Validate(
                                                                    name = "processAlias",
                                                                    type = ValidationType.STRING_NOT_EMPTY) String processAlias,
                                                            @Validate(
                                                                    name = "processReadingTypes",
                                                                    type = ValidationType.NOT_NULL) String[] processReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        new Validator().validateMethodParameters(new Object[]{ monitoredHost,
                                                               parentProcess,
                                                               processPattern,
                                                               processAlias,
                                                               processReadingTypes });

        return scheduleProcessMonitoring(monitoredHost,
                                         parentProcess,
                                         processPattern,
                                         processAlias,
                                         null,
                                         processReadingTypes);
    }

    public Set<ReadingBean> scheduleChildProcessMonitoring(
                                                            @Validate(
                                                                    name = "monitoredHost",
                                                                    type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                            @Validate(
                                                                    name = "parentProcess",
                                                                    type = ValidationType.STRING_NOT_EMPTY) String parentProcess,
                                                            @Validate(
                                                                    name = "processPattern",
                                                                    type = ValidationType.STRING_NOT_EMPTY) String processPattern,
                                                            @Validate(
                                                                    name = "processAlias",
                                                                    type = ValidationType.STRING_NOT_EMPTY) String processAlias,
                                                            @Validate(
                                                                    name = "processUsername",
                                                                    type = ValidationType.NONE) String processUsername,
                                                            @Validate(
                                                                    name = "processReadingTypes",
                                                                    type = ValidationType.NOT_NULL) String[] processReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        new Validator().validateMethodParameters("Could not schedule a process for monitoring '"
                                                 + monitoredHost + "'",
                                                 new Object[]{ monitoredHost,
                                                               parentProcess,
                                                               processPattern,
                                                               processAlias,
                                                               processUsername,
                                                               processReadingTypes });

        return scheduleProcessMonitoring(monitoredHost,
                                         parentProcess,
                                         processPattern,
                                         processAlias,
                                         processUsername,
                                         processReadingTypes);
    }

    private Set<ReadingBean> scheduleProcessMonitoring(
                                                        String monitoredHost,
                                                        String parentProcess,
                                                        String processPattern,
                                                        String processAlias,
                                                        String processUsername,
                                                        String[] processReadingTypes ) {

        Set<ReadingBean> readingTypes = new HashSet<ReadingBean>();

        try {
            log.debug("Scheduling process monitoring...");
            readingTypes.addAll(systemMonitor.scheduleProcessMonitoring(parentProcess,
                                                                        processPattern,
                                                                        processAlias,
                                                                        processUsername,
                                                                        processReadingTypes));
            logSystemStatistics = true;
            log.info("Process monitoring scheduled.");

            return readingTypes;
        } catch (Exception e) {
            log.error("Could not schedule process monioring.");
            throw new MonitoringException("Could not schedule process monioring. Did you initialize the monitoring context?",
                                          e);
        }
    }

    public Set<ReadingBean> scheduleJvmMonitoring(
                                                   @Validate(
                                                           name = "monitoredHost",
                                                           type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                   @Validate(
                                                           name = "jvmPort",
                                                           type = ValidationType.NUMBER_PORT_NUMBER) String jvmPort,
                                                   @Validate(
                                                           name = "jvmReadingTypes",
                                                           type = ValidationType.NOT_NULL) String[] jvmReadingTypes ) {

        return scheduleJvmMonitoring(monitoredHost, jvmPort, "", jvmReadingTypes);

    }

    public Set<ReadingBean> scheduleJvmMonitoring(
                                                   @Validate(
                                                           name = "monitoredHost",
                                                           type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                   @Validate(
                                                           name = "jvmPort",
                                                           type = ValidationType.NUMBER_PORT_NUMBER) String jvmPort,
                                                   @Validate(
                                                           name = "alias",
                                                           type = ValidationType.NOT_NULL) String alias,
                                                   @Validate(
                                                           name = "jvmReadingTypes",
                                                           type = ValidationType.NOT_NULL) String[] jvmReadingTypes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);
        new Validator().validateMethodParameters("Could not schedule monitoring JVM statistics on '"
                                                 + monitoredHost + "' at " + jvmPort + " port",
                                                 new Object[]{ monitoredHost, jvmPort, jvmReadingTypes });

        Set<ReadingBean> readingTypes = new HashSet<ReadingBean>();

        Map<String, String> readingParameters = new HashMap<String, String>();
        readingParameters.put("JMX_PORT", jvmPort);
        if (!StringUtils.isNullOrEmpty(alias)) {
            readingParameters.put(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS, alias);
        }

        for (String readingType : jvmReadingTypes) {
            ReadingBean reading = null;
            try {
                log.debug("Scheduling JVM monitoring...");
                reading = systemMonitor.scheduleJvmMonitoring(readingType, readingParameters);
                logSystemStatistics = true;
                log.info("JVM monitoring scheduled.");
            } catch (Exception e) {
                log.error("Could not schedule JVM monitoring.", e);
                throw new MonitoringException("Could not schedule JVM monitoring. Did you initialize the monitoring context?",
                                              e);
            }
            readingTypes.add(reading);
        }

        return readingTypes;
    }

    public Set<ReadingBean> scheduleCustomJvmMonitoring(
                                                         @Validate(
                                                                 name = "monitoredHost",
                                                                 type = ValidationType.STRING_SERVER_WITH_PORT) String monitoredHost,
                                                         @Validate(
                                                                 name = "jmxPort",
                                                                 type = ValidationType.NUMBER_PORT_NUMBER) String jmxPort,
                                                         @Validate(
                                                                 name = "alias",
                                                                 type = ValidationType.NOT_NULL) String alias,
                                                         @Validate(
                                                                 name = "mbeanName",
                                                                 type = ValidationType.NOT_NULL) String mbeanName,
                                                         @Validate(
                                                                 name = "unit",
                                                                 type = ValidationType.NOT_NULL) String unit,
                                                         @Validate(
                                                                 name = "mbeanAttributes",
                                                                 type = ValidationType.NOT_NULL) String... mbeanAttributes ) {

        // validate input parameters
        monitoredHost = HostUtils.getAtsAgentIpAndPort(monitoredHost);

        Set<ReadingBean> readingTypes = new HashSet<ReadingBean>();

        Map<String, String> readingParameters = new LinkedHashMap<String, String>();
        readingParameters.put("JMX_PORT", jmxPort);
        readingParameters.put("MBEAN_NAME", mbeanName);
        if (!StringUtils.isNullOrEmpty(alias)) {
            readingParameters.put(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS, alias);
        }

        // we just need a list of values, so we are using only the keys of the
        // already existing map
        if (mbeanAttributes.length > 1) {
            for (String att : mbeanAttributes) {
                readingParameters.put(att, "");
            }
        }

        // the first element in the array is always the mbean name
        ReadingBean reading = new ReadingBean(ATS_JVM_MONITOR_CLASS_FULL_NAME, mbeanAttributes[0], unit);
        try {
            log.debug("Scheduling custom JVM monitoring...");
            int newReadingId = systemMonitor.scheduleCustomJvmMonitoring();
            reading.setDbId(newReadingId);
            logSystemStatistics = true;
            log.info("Custom JVM monitoring scheduled.");
        } catch (Exception e) {
            log.error("Could not schedule custom JVM monitoring.", e);
            throw new MonitoringException("Could not schedule custom JVM monitoring. Did you initialize the monitoring context?",
                                          e);
        }

        reading.setParameters(readingParameters);
        readingTypes.add(reading);

        return readingTypes;
    }

    public void scheduleUserActivity(
                                      String host ) {

        // maybe loggingInterval must be passed when starting user activity
        // logging in startMonitoring()

        logUserActivity = true;

        log.info("Scheduling user activity on '" + host + "' started.");

    }

    public void startMonitoring(
                                 String monitoredHost,
                                 long startTimestamp,
                                 int pollInterval,
                                 long executorTimeOffset ) {

        if (isStarted) {

            throw new MonitoringException("System monitoring is already started from this caller on this agent.");
        }

        if (logSystemStatistics) {
            List<MonitoringException> errorsStartingMonitoringPhysicalHost = startMonitoringPhysicalHost(monitoredHost,
                                                                                                         pollInterval,
                                                                                                         executorTimeOffset);
            if (errorsStartingMonitoringPhysicalHost.size() > 0) {
                for (MonitoringException e : errorsStartingMonitoringPhysicalHost) {
                    log.error("The following error occured while starting the system monitoring process",
                              e);
                }

                cancelAnyMonitoringActivity(ThreadsPerCaller.getCaller());
                // clear the readings because an error occurred
                this.readingTypes.clear();
                throw new MonitoringException("There were error starting the system monitoring process");
            }
        }

        if (logUserActivity) {

            List<MonitoringException> errorsStartingMonitoringAgent = startMonitoringAgent(monitoredHost,
                                                                                           startTimestamp,
                                                                                           pollInterval);
            if (errorsStartingMonitoringAgent.size() > 0) {
                for (MonitoringException e : errorsStartingMonitoringAgent) {
                    log.error("The following error occured while starting the monitoring process on ATS Agent",
                              e);
                }

                cancelAnyMonitoringActivity(ThreadsPerCaller.getCaller());
                // clear the readings because an error occurred
                this.readingTypes.clear();
                throw new MonitoringException("There were errors starting the monitoring process on ATS Agent");
            }
        }

        isStarted = true;

    }

    public void stopMonitoring(
                                String monitoredHost ) {

        if (!isStarted) {
            log.debug("No previously started system monitoring operations were found.");
            return;
        }

        boolean successfullStoppingOfPhysicalHost = true;

        if (logSystemStatistics) {
            successfullStoppingOfPhysicalHost = stopMonitoringPhysicalHost(monitoredHost);
        }

        boolean successfullStoppingOfAgent = true;

        if (logUserActivity) {
            successfullStoppingOfAgent = stopMonitoringAgent(ThreadsPerCaller.getCaller());
        }

        if (!successfullStoppingOfPhysicalHost || !successfullStoppingOfAgent) {
            throw new MonitoringException("There were errors while stopping monitoring. See agent log.");
        }

        // clear last schedules readingTypes
        this.readingTypes = null;

        isStarted = false;
        logUserActivity = false;
        logSystemStatistics = false;
    }

    public Set<ReadingBean> getReadingTypes() {

        return this.readingTypes;
    }

    public void setScheduledReadingTypes(
                                          Set<ReadingBean> readingTypes ) {

        if (this.readingTypes == null) {
            this.readingTypes = new HashSet<>();
        }

        // see which readings are already scheduled and log a warning

        // add those that are new

        Iterator<ReadingBean> scheduledReadingsIretator = this.readingTypes.iterator();

        Iterator<ReadingBean> newReadingsIretator = readingTypes.iterator();

        while (newReadingsIretator.hasNext()) {

            ReadingBean newReadingBean = newReadingsIretator.next();

            while (scheduledReadingsIretator.hasNext()) {
                ReadingBean scheduledReadingBean = scheduledReadingsIretator.next();
                if (newReadingBean.equals(scheduledReadingBean)) {
                    log.warn("The requested reading '" + newReadingBean.toString()
                             + "' has already being scheduled.");
                }
            }
            // reset iterator by making the object again
            scheduledReadingsIretator = this.readingTypes.iterator();
        }

        /*
         * since this is a set, already presented readings, will be replaced, so
         * we add only the new ones
         */
        this.readingTypes.addAll(readingTypes);
    }

    private List<MonitoringException> startMonitoringPhysicalHost(
                                                                   String monitoredHost,
                                                                   int pollingInterval,
                                                                   long executorTimeOffset ) {

        List<MonitoringException> errors = new ArrayList<MonitoringException>();

        // initialize the monitoring processes

        log.debug("Initializing system monitoring on " + monitoredHost);
        final String ERR_MSG = "Could not initialize monitoring " + monitoredHost + ". ";
        if (this.readingTypes == null || this.readingTypes.size() == 0) {
            errors.add(new MonitoringException(ERR_MSG + " as no monitor types are provided"));
        }

        try {
            initializeSystemMonitoringProcess(monitoredHost,
                                              pollingInterval,
                                              executorTimeOffset);
        } catch (MonitoringException e) {
            errors.add(e);
        }

        log.info("Successfully initialized monitoring " + monitoredHost);

        // run the monitoring processes
        log.debug("Starting system monitoring on " + monitoredHost);

        try {
            startSystemMonitoringProcess(monitoredHost);
        } catch (MonitoringException e) {
            errors.add(e);
        }

        log.info("Successfully started monitoring " + monitoredHost + ".");

        return errors;
    }

    private void initializeSystemMonitoringProcess(
                                                    String monitoredHost,
                                                    int pollInterval,
                                                    long executorTimeOffset ) throws MonitoringException {

        log.debug("Initializing the system monitoring process on " + monitoredHost);
        try {
            systemMonitor.initializeMonitoring(new ArrayList<ReadingBean>(this.readingTypes),
                                               pollInterval,
                                               executorTimeOffset);
        } catch (Exception e) {
            throw new MonitoringException("Could not start the system monitoring process on " + monitoredHost
                                          + ". For more details check loader logs on that machine", e);
        }
    }

    private void startSystemMonitoringProcess(
                                               String monitoredHost ) throws MonitoringException {

        log.debug("Starting the system monitoring process on " + monitoredHost);
        try {
            systemMonitor.startMonitoring();
        } catch (Exception e) {
            throw new MonitoringException("Could not start the system monitoring process on "
                                          + monitoredHost, e);
        }
    }

    private List<MonitoringException> startMonitoringAgent(
                                                            String monitoredAgent,
                                                            long startTimestamp,
                                                            int pollInterval ) {

        List<MonitoringException> errors = new ArrayList<MonitoringException>();
        log.debug("Starting ATS Agent monitoring on " + monitoredAgent);

        UserActionsMonitoringAgent userActionsMonitoringAgent = UserActionsMonitoringAgent.getInstance(ThreadsPerCaller.getCaller());

        userActionsMonitoringAgent.setAgentAddress(monitoredAgent);

        userActionsMonitoringAgent.startMonitoring(startTimestamp, pollInterval);

        return errors;

    }

    private void cancelAnyMonitoringActivity(
                                              String monitoredAgent ) {

        log.info("Canceling any system monitoring activity, if there is such");
        stopMonitoringPhysicalHost(monitoredAgent);
        log.info("Canceling any agent monitoring activity, if there is such");
        stopMonitoringAgent(monitoredAgent);

    }

    private boolean stopMonitoringPhysicalHost(
                                                String monitoredHost ) {

        boolean successfulOperation = true;

        // stop the monitoring process
        try {
            stopSystemMonitoringProcess(monitoredHost);
        } catch (MonitoringException e) {
            successfulOperation = false;
            log.error("Could not stop monitoring " + monitoredHost, e);
        }

        return successfulOperation;
    }

    private void stopSystemMonitoringProcess(
                                              String monitoredHost ) throws MonitoringException {

        log.debug("Stopping system monitoring on " + monitoredHost);
        try {
            systemMonitor.stopMonitoring();
            log.debug("Successfully stopped system monitoring on " + monitoredHost);
        } catch (Exception e) {
            throw new MonitoringException("Could not stop the system monitoring process on " + monitoredHost,
                                          e);
        }
    }

    private boolean stopMonitoringAgent(
                                         String monitoredAgent ) {

        boolean successfulOperation = true;

        // Stop the monitoring process
        try {
            stopMonitoringProcessOnAgent(monitoredAgent);
        } catch (MonitoringException e) {
            successfulOperation = false;
            log.error(e);
        }

        return successfulOperation;
    }

    private void stopMonitoringProcessOnAgent(
                                               String monitoredAgent ) throws MonitoringException {

        log.debug("Stopping system monitoring on " + monitoredAgent + " agent");
        UserActionsMonitoringAgent.getInstance(ThreadsPerCaller.getCaller()).stopMonitoring();
        log.debug("Successfully stopped monitoring " + monitoredAgent + " agent");

    }

}
