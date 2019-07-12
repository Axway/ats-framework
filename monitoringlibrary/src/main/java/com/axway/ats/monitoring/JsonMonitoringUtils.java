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
package com.axway.ats.monitoring;

import java.util.Map;

/**
 * Class, used to generate JSON request bodies for every REST monitoring method
 * on the agent
 */
public class JsonMonitoringUtils {

    public static final String   INITIALIZE_DB_CONNECTION_ACTION_NAME    = "Initialize Db Connection";

    public static final String   INITIALIZE_MONITORING_ACTION_NAME       = "Initialize Monitoring";

    public static final String   JOIN_TESTCASE_ACTION_NAME               = "Join Testcase";

    public static final String   SCHEDULE_SYSTEM_MONITORING_ACTION_NAME  = "Schedule System Monitoring";

    public static final String   SCHEDULE_MONITORING_ACTION_NAME         = "Schedule Monitoring";

    public static final String   SCHEDULE_PROCESS_MONITORING_ACTION_NAME = "Schedule Process Monitoring";

    public static final String   SCHEDULE_JVM_MONITORING_ACTION_NAME     = "Schedule JVM Monitoring";

    public static final String   SCHEDULE_CUSTOM_MONITORING_ACTION_NAME  = "Schedule Custom Monitoring";

    public static final String   SCHEDULE_USER_ACTIVITY_ACTION_NAME      = "Schedule User Activity";

    public static final String   START_MONITORING_ACTION_NAME            = "Start Monitoring";

    public static final String   STOP_MONITORING_ACTION_NAME             = "Stop Monitoring";

    public static final String   LEAVE_TESTCASE_ACTION_NAME              = "Leave Testcase";

    public static final String   DEINITIALIZE_DB_CONNECTION_ACTION_NAME  = "Deinitialize Db Connection";

    public static final String[] INITIALIZE_DB_CONNECTION_KEYS           = new String[]{ "uid",
                                                                                         "dbHost",
                                                                                         "dbPort",
                                                                                         "dbName",
                                                                                         "dbUser",
                                                                                         "dbPass",
                                                                                         "mode",
                                                                                         "loggingThreshold",
                                                                                         "maxNumberLogEvents",
                                                                                         "timestamp" };

    public static final String[] JOIN_TESTCASE_KEYS                      = new String[]{ "uid",
                                                                                         "runId",
                                                                                         "testcaseId",
                                                                                         "lastExecutedTestcaseId" };

    public static final String[] SCHEDULE_SYSTEM_MONITORING_KEYS         = new String[]{ "uid", "readings" };

    public static final String[] SCHEDULE_MONITORING_KEYS                = new String[]{ "uid",
                                                                                         "reading",
                                                                                         "readingParameters" };

    public static final String[] SCHEDULE_PROCESS_MONITORING_KEYS        = new String[]{ "uid",
                                                                                         "processPattern",
                                                                                         "processAlias",
                                                                                         "processUsername",
                                                                                         "parentProcess",
                                                                                         "processReadingTypes" };

    public static final String[] SCHEDULE_JVM_MONITORING_KEYS            = new String[]{ "uid",
                                                                                         "jvmPort",
                                                                                         "alias",
                                                                                         "jvmReadingTypes" };

    public static final String[] SCHEDULE_CUSTOM_JVM_MONITORING_KEYS     = new String[]{ "uid",
                                                                                         "jmxPort",
                                                                                         "alias",
                                                                                         "mbeanName",
                                                                                         "unit",
                                                                                         "mbeanAttributes" };

    public static final String[] SCHEDULE_USER_ACTIVITY_KEYS             = new String[]{ "uid" };

    public static final String[] START_MONITORING_KEYS                   = new String[]{ "uid",
                                                                                         "pollingInterval",
                                                                                         "startTimestamp" };

    public static final String[] UID_ONLY_KEYS                           = new String[]{ "uid" };

    public static String constructInitializeDbConnectionJson(
                                                              Object[] values ) throws IllegalArgumentException {

        return constructJson(INITIALIZE_DB_CONNECTION_ACTION_NAME, INITIALIZE_DB_CONNECTION_KEYS, values);
    }

    public static String constructJoinTestcaseJson(
                                                    Object[] values ) throws IllegalArgumentException {

        return constructJson(JOIN_TESTCASE_ACTION_NAME, JOIN_TESTCASE_KEYS, values);
    }

    public static String constructInitializeMonitoringJson(
                                                            Object[] values ) throws IllegalArgumentException {

        return constructJson(INITIALIZE_MONITORING_ACTION_NAME, UID_ONLY_KEYS, values);
    }

    public static String constructScheduleSystemMonitoringJson(
                                                                Object[] values ) throws IllegalArgumentException {

        return constructJson(SCHEDULE_SYSTEM_MONITORING_ACTION_NAME,
                             SCHEDULE_SYSTEM_MONITORING_KEYS,
                             values);
    }

    public static String constructScheduleMonitoringJson(
                                                          Object[] values ) throws IllegalArgumentException {

        return constructJson(SCHEDULE_MONITORING_ACTION_NAME, SCHEDULE_MONITORING_KEYS, values);
    }

    public static String constructScheduleProcessMonitoringJson(
                                                                 Object[] values ) throws IllegalArgumentException {

        return constructJson(SCHEDULE_PROCESS_MONITORING_ACTION_NAME,
                             SCHEDULE_PROCESS_MONITORING_KEYS,
                             values);
    }

    public static String constructScheduleJvmProcessMonitoringJson(
                                                                    Object[] values ) throws IllegalArgumentException {

        return constructJson(SCHEDULE_JVM_MONITORING_ACTION_NAME, SCHEDULE_JVM_MONITORING_KEYS, values);
    }

    public static String constructScheduleCustomJvmProcessMonitoringJson(
                                                                          Object[] values ) throws IllegalArgumentException {

        return constructJson(SCHEDULE_CUSTOM_MONITORING_ACTION_NAME,
                             SCHEDULE_CUSTOM_JVM_MONITORING_KEYS,
                             values);
    }

    public static String constructScheduleUserActivityJson(
                                                            Object[] values ) throws IllegalArgumentException {

        return constructJson(SCHEDULE_USER_ACTIVITY_ACTION_NAME, SCHEDULE_USER_ACTIVITY_KEYS, values);
    }

    public static String constructStartMonitoringJson(
                                                       Object[] values ) throws IllegalArgumentException {

        return constructJson(START_MONITORING_ACTION_NAME, START_MONITORING_KEYS, values);
    }

    public static String constructStopMonitoringJson(
                                                      Object[] values ) throws IllegalArgumentException {

        return constructJson(STOP_MONITORING_ACTION_NAME, UID_ONLY_KEYS, values);
    }

    public static String constructLeaveTestcaseJson(
                                                     Object[] values ) throws IllegalArgumentException {

        return constructJson(LEAVE_TESTCASE_ACTION_NAME, UID_ONLY_KEYS, values);
    }

    public static String constructDeinitializeDbConnectionJson(
                                                                Object[] values ) throws IllegalArgumentException {

        return constructJson(DEINITIALIZE_DB_CONNECTION_ACTION_NAME, UID_ONLY_KEYS, values);
    }

    /**
     * Constructs a JSON request for monitoring operations
     * @param action the name of monitoring action which will be executed on the Agent
     * @param keys the JSON keys
     * @param values the JSON values
     * */
    public static String constructJson(
                                        String action,
                                        String[] keys,
                                        Object[] values ) throws IllegalArgumentException {

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        if (keys.length != values.length) {
            throw new IllegalArgumentException("Error running '" + action
                                               + "'. The number of expected keys is " + keys.length
                                               + ", but we got " + values.length
                                               + " indeed. Please consult the documentation.");
        }

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i].replace("\"", "\\\"");
            sb.append("\"").append(key).append("\"").append(": ");
            Object value = values[i];
            if (value == null) {
                sb.append(value);
            } else {
                if (value instanceof String) {
                    sb.append("\"").append( ((String) value).replace("\"", "\\\"")).append("\"");
                } else if (value instanceof Number) {
                    sb.append((Number) value);
                } else if (value instanceof String[]) {
                    sb.append("[");
                    for (String obj : (String[]) value) {
                        sb.append("\"").append(obj.replace("\"", "\\\"")).append("\"").append(",");
                    }
                    sb = new StringBuilder(sb.subSequence(0, sb.length() - 1));
                    sb.append("]");
                } else if (value instanceof Boolean) {
                    sb.append((Boolean) value);
                } else if (value instanceof Map) {
                    /* 
                     * In order to serialize java.util.Map object to JSON,
                     * transform it to a JSON array, and each array element will contains a JSON object (the key-value pair of each map entry)
                     * Example: 
                     *     Map<String, String> someMap = new HashMap<String, String>();
                    	   someMap.put("dbHost", "127.0.0.1");
                    	   someMap.put("dbPort", "5555");
                       will be transformed to
                       { 
                           ...
                           'map':[
                             {'key':'dbHost',value':'127.0.0.1'},
                             {'key':'dbPort',value':'5555'}
                           ]
                           ...
                       }
                     * */
                    sb.append("[");
                    Map<String, String> map = ((Map<String, String>) value);
                    if (map.isEmpty()) {
                        sb.append("]");
                    } else {
                        for (Map.Entry<String, String> mapEntry : map.entrySet()) {
                            sb.append("{");
                            sb.append("\"")
                              .append("key")
                              .append("\"")
                              .append(":")
                              .append("\"")
                              .append(mapEntry.getKey())
                              .append("\"")
                              .append(",");
                            sb.append("\"")
                              .append("value")
                              .append("\"")
                              .append(":")
                              .append("\"")
                              .append(mapEntry.getValue().replace("\"", "\\\""))
                              .append("\"");
                            sb.append("},");
                        }
                        sb = new StringBuilder(sb.subSequence(0, sb.length() - 1));
                        sb.append("]");
                    }

                } else {
                    throw new IllegalArgumentException("Error running '" + action
                                                       + "'. Invallid value type '"
                                                       + value.getClass().getSimpleName()
                                                       + "'. String, String[], Number, Boolean and Object are supported.");
                }
            }

            sb.append(",");
        }

        String json = sb.toString().substring(0, sb.length() - 1);
        json += "}";
        return json;

    }

}
