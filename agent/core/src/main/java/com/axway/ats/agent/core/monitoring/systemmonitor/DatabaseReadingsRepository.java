/*
 * Copyright 2017-2019 Axway Software
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.performance.monitor.beans.ParentProcessReadingBean;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.io.DbAccessFactory;
import com.axway.ats.log.autodb.io.SQLServerDbWriteAccess;

/**
 * Keeps info about all readings that are already populated to the DB.
 *
 * Once the reading is populated, we can then send just "reading id" and "reading value" to the
 * database
 */
public class DatabaseReadingsRepository {

    private static SQLServerDbWriteAccess            dbAccess          = null;

    //this map keeps track of the ReadingBean(s) that already have a dbId assigned per database
    private static Map<String, Map<String, Integer>> knownReadingBeans = Collections.synchronizedMap(new HashMap<String, Map<String, Integer>>());

    public DatabaseReadingsRepository() {}

    /**
     * Populate these reading to the DB, so they have their own DB IDs
     *
     * @param monitoredHost
     * @param readings
     * @param readingIdToDbIdMap
     * @throws DatabaseAccessException
     */
    public void updateDatabaseRepository(
                                          String monitoredHost,
                                          List<ReadingBean> readings ) throws DatabaseAccessException {

        Logger log = LogManager.getLogger(DatabaseReadingsRepository.class);

        String caller = ThreadsPerCaller.getCaller();

        if (dbAccess == null) {
            dbAccess = new DbAccessFactory().getNewDbWriteAccessObjectViaPassiveDbAppender(caller);
        }

        String dbConnectionHash = obtainDbConnectionHash(caller);

        Map<String, Integer> knownReadingBeansForCurrentDbConn = knownReadingBeans.get(dbConnectionHash);
        if (knownReadingBeansForCurrentDbConn == null) {
            knownReadingBeansForCurrentDbConn = new HashMap<String, Integer>();
            knownReadingBeans.put(dbConnectionHash, knownReadingBeansForCurrentDbConn);
        }

        for (ReadingBean reading : readings) {
            // check if the current reading already was flagged as known
            int dbId = getDbIdForReading(reading, knownReadingBeansForCurrentDbConn);
            reading.setDbId(dbId);
            if (reading.getDbId() == -1) {
                StringBuilder newReadingParameters = new StringBuilder();
                Map<String, String> readingParameters = reading.getParameters();
                if (readingParameters != null && readingParameters.size() > 0) {

                    if (readingParameters.containsKey(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS)) {

                        newReadingParameters.append("'");
                        newReadingParameters.append(readingParameters.get(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS));
                        newReadingParameters.append("'_user pattern is '");
                        newReadingParameters.append(readingParameters.get(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN));
                        newReadingParameters.append("'_reading=");
                        newReadingParameters.append(readingParameters.get(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_READING_ID));
                        newReadingParameters.append("_started by command '");
                        newReadingParameters.append(readingParameters.get(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_START_COMMAND));
                        newReadingParameters.append("'");
                    } else {

                        newReadingParameters.append(readingParameters.get(SystemMonitorDefinitions.PARAMETER_NAME__CUSTOM_MESSAGE));
                    }
                }

                int newReadingDatabaseId;
                if (reading instanceof ParentProcessReadingBean) {
                    String thisProcessName = "[process] "
                                             + ((ParentProcessReadingBean) reading).getTheNameOfThisParentProcess();
                    String thisReadingName = thisProcessName + " - " + reading.getName();
                    newReadingDatabaseId = dbAccess.populateSystemStatisticDefinition(thisReadingName,
                                                                                      reading.getParameter(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_PARENT_NAME),
                                                                                      thisProcessName,
                                                                                      reading.getUnit(),
                                                                                      newReadingParameters.toString());
                    log.debug("DB id " + newReadingDatabaseId + " for parent process reading: "
                              + thisReadingName);
                } else {
                    String parentName = reading.getParameter(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_PARENT_NAME);
                    if (parentName != null) {
                        parentName = "[process] " + parentName;
                    }
                    newReadingDatabaseId = dbAccess.populateSystemStatisticDefinition(reading.getName(),
                                                                                      parentName,
                                                                                      "",
                                                                                      reading.getUnit(),
                                                                                      newReadingParameters.toString());
                    log.debug("DB id " + newReadingDatabaseId + " for reading: " + reading.getName());
                }

                // remember the DB ID of this reading
                reading.setDbId(newReadingDatabaseId);
                knownReadingBeansForCurrentDbConn.put(reading.getDescription(), reading.getDbId());
            }
        }
    }

    private String obtainDbConnectionHash( String caller ) {

        DbAppenderConfiguration dbAppenderConfiguration = PassiveDbAppender.getCurrentInstance(caller)
                                                                           .getAppenderConfig();

        StringBuilder sb = new StringBuilder();

        sb.append(dbAppenderConfiguration.getHost() + "|__|" + dbAppenderConfiguration.getPort() + "|__|"
                  + dbAppenderConfiguration.getDatabase());

        return sb.toString();

    }

    private int getDbIdForReading(
                                   ReadingBean reading, Map<String, Integer> knownReadingBeans ) {

        String mapKey = reading.getDescription();
        Integer dbId = knownReadingBeans.get(mapKey);
        /*FIXME: ATS is caching readings for each database, that was created on the Agent, but if one of those databases is recreated, and the agent is not restarted,
         * the cached reading is NOT invalidated
         */
        return (dbId != null)
                              ? dbId
                              : -1;
    }
}
