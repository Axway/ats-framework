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
package com.axway.ats.monitoring;

import com.axway.ats.action.rest.RestClient;
import com.axway.ats.action.rest.RestClient.RESTDebugLevel;
import com.axway.ats.action.rest.RestMediaType;
import com.axway.ats.action.rest.RestResponse;
import com.axway.ats.agent.core.context.ApplicationContext;
import com.axway.ats.agent.webapp.client.ExecutorUtils;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.log.appenders.ActiveDbAppender;

/**
 * This class is used to keep track of information, needed for each monitoring
 * host, such as ATS_UID, RestClient object and Agent IP
 */
public class RestHelper {

    public static final String BASE_MONITORING_REST_SERVICE_URI               = "/agentapp/restservice/monitoring";

    public static final String BASE_CONFIGURATION_REST_SERVICE_URI            = "/agentapp/restservice/configuration";

    public static final String INITIALIZE_DB_CONNECTION_RELATIVE_URI          = "/initializeDbConnection";

    public static final String JOIN_TESTCASE_RELATIVE_URI                     = "/joinTestcase";

    public static final String INITIALIZE_MONITORING_RELATIVE_URI             = "/initializeMonitoring";

    public static final String SCHEDULE_SYSTEM_MONITORING_RELATIVE_URI        = "/scheduleSystemMonitoring";

    public static final String SCHEDULE_MONITORING_RELATIVE_URI               = "/scheduleMonitoring";

    public static final String SCHEDULE_PROCESS_MONITORING_RELATIVE_URI       = "/scheduleProcessMonitoring";

    public static final String SCHEDULE_CHILD_PROCESS_MONITORING_RELATIVE_URI = "/scheduleChildProcessMonitoring";

    public static final String SCHEDULE_JVM_MONITORING_RELATIVE_URI           = "/scheduleJvmMonitoring";

    public static final String SCHEDULE_CUSTOM_JVM_MONITORING_RELATIVE_URI    = "/scheduleCustomJvmMonitoring";

    public static final String SCHEDULE_USER_ACTIVITY_RELATIVE_URI            = "/scheduleUserActivity";

    public static final String START_MONITORING_RELATIVE_URI                  = "/startMonitoring";

    public static final String STOP_MONITORING_RELATIVE_URI                   = "/stopMonitoring";

    public static final String LEAVE_TESTCASE_RELATIVE_URI                    = "/leaveTestcase";

    public static final String DEINITIALIZE_DB_CONNECTION_RELATIVE_URI        = "/deinitializeDbConnection";

    private RestClient         restClient;
    private String             uid;
    private String             agentVersion;

    public RestHelper() {}

    public RestResponse post( String atsAgentIp, String baseRestUri, String relativeRestUri, Object[] values ) {

        // check if ActiveDbAppender is attached
        if (!ActiveDbAppender.isAttached) {
            throw new IllegalStateException("Unable to execute monitoring operation.ATS DB Appender is not presented in log4j2.xml");
        }

        RestResponse response = null;

        initializeRestClient(atsAgentIp, baseRestUri, relativeRestUri);

        String jsonBody = null;

        if (relativeRestUri.endsWith(INITIALIZE_DB_CONNECTION_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructInitializeDbConnectionJson(values);
        } else if (relativeRestUri.endsWith(JOIN_TESTCASE_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructJoinTestcaseJson(values);
        } else if (relativeRestUri.endsWith(INITIALIZE_MONITORING_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructInitializeMonitoringJson(values);
        } else if (relativeRestUri.endsWith(SCHEDULE_SYSTEM_MONITORING_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructScheduleSystemMonitoringJson(values);
        } else if (relativeRestUri.endsWith(SCHEDULE_MONITORING_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructScheduleMonitoringJson(values);
        } else if (relativeRestUri.endsWith(SCHEDULE_PROCESS_MONITORING_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructScheduleProcessMonitoringJson(values);
        } else if (relativeRestUri.endsWith(SCHEDULE_CHILD_PROCESS_MONITORING_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructScheduleProcessMonitoringJson(values);
        } else if (relativeRestUri.endsWith(SCHEDULE_JVM_MONITORING_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructScheduleJvmProcessMonitoringJson(values);
        } else if (relativeRestUri.endsWith(SCHEDULE_CUSTOM_JVM_MONITORING_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructScheduleCustomJvmProcessMonitoringJson(values);
        } else if (relativeRestUri.endsWith(SCHEDULE_USER_ACTIVITY_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructScheduleUserActivityJson(values);
        } else if (relativeRestUri.endsWith(START_MONITORING_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructStartMonitoringJson(values);
        } else if (relativeRestUri.endsWith(STOP_MONITORING_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructStopMonitoringJson(values);
        } else if (relativeRestUri.endsWith(LEAVE_TESTCASE_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructLeaveTestcaseJson(values);
        } else if (relativeRestUri.endsWith(DEINITIALIZE_DB_CONNECTION_RELATIVE_URI)) {
            jsonBody = JsonMonitoringUtils.constructDeinitializeDbConnectionJson(values);
        } else {
            throw new IllegalArgumentException(
                                               "relativeRestUri does not lead to existing REST method. Please consult the documentation.");
        }

        response = this.restClient.postObject(jsonBody);

        return response;

    }

    /**
     * Create RestClient instance
     * 
     * @param atsAgentIp
     *            the IP of the agent, on which we want to perform some
     *            monitoring operation
     * @param relativeRestUri
     *            the relative URI for the desired monitoring operation
     */
    public void initializeRestClient( String atsAgentIp, String baseRestUri, String relativeRestUri ) {

        // create RestClient instance
        this.restClient = new RestClient("http://" + atsAgentIp + baseRestUri + relativeRestUri);
        if (AtsSystemProperties.getPropertyAsBoolean(AtsSystemProperties.SYSTEM_MONITOR_VERBOSE_MODE, false)) {
            // enable all logging (both REST headers and body)
            this.restClient.setVerboseMode(RESTDebugLevel.ALL);
        } else {
            // disable any logging (both REST headers and body)
            this.restClient.setVerboseMode(RESTDebugLevel.NONE);
        }
        this.restClient.setRequestMediaType(RestMediaType.APPLICATION_JSON);
        this.restClient.setResponseMediaType(RestMediaType.APPLICATION_JSON);
        // set ATS_UID header
        this.uid = ExecutorUtils.getUUID();
        this.restClient.addRequestHeader(ApplicationContext.ATS_UID_SESSION_TOKEN, this.uid);
    }

    public void disconnect() {

        if (this.restClient != null) {
            this.restClient.disconnect();
        } else {
            throw new RuntimeException("Could not disconnect, because RestClient is not initialized.");
        }

    }

    public String getAgentVersion() {

        return this.agentVersion;
    }

}
