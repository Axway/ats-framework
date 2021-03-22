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
package com.axway.ats.agent.core.monitoring;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.axway.ats.agent.core.monitoring.systemmonitor.ReadingsRepository;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.common.performance.monitor.beans.MonitorResults;
import com.axway.ats.core.monitoring.MonitoringException;

import com.axway.ats.log.AtsDbLogger;

public class UserActivityLoggingUtils {

    /*
     * Skip checking if DB appender is attached, because we are on the agent and not the executor.
     */
    private static final AtsDbLogger dbLog = AtsDbLogger.getLogger(UserActivityLoggingUtils.class.getName(), true);

    // All Agents are presented as a single virtual host.
    // The reason is that user wants to know the total number of simulated
    // users, he does not
    // care how many users are operating on each Agent.
    public static final String       ATS_AGENT_HOSTS = "ATS Agents";

    public static void logCollectedResults(
                                            String monitoredAgent,
                                            MonitorResults collectedResults ) {

        Map<Long, Map<String, Integer>> readyToBeloggedReadingsMap = parseReadings(monitoredAgent,
                                                                                   collectedResults);
        // send the consolidated user activity to the logging database
        int resultsAddeed = commitToDatabase(readyToBeloggedReadingsMap);
        dbLog.debug("Successfully sent " + resultsAddeed
                    + " user activity results to the logging database");
    }

    private static Map<Long, Map<String, Integer>> parseReadings(
                                                                  String monitoredAgent,
                                                                  MonitorResults newMonitorResults ) {

        // counter for user info
        int resultsRead = 0;

        // create a map, to store the parsed user activity statistics
        Map<Long, Map<String, Integer>> readyToBeloggedReadingsMap = new HashMap<>();

        // assign DB IDs to these readings if needed
        List<ReadingBean> newReadings = newMonitorResults.getReadings();

        updateDatabaseRepository(monitoredAgent, newReadings);

        // Map<reading ID, reading value>
        Map<String, Integer> readingsMap = new HashMap<String, Integer>();
        for (ReadingBean newReading : newReadings) {
            readingsMap.put(String.valueOf(newReading.getDbId()),
                            Integer.valueOf(newReading.getValue()));
            resultsRead++;
        }

        readyToBeloggedReadingsMap.put(newMonitorResults.getTimestamp(), readingsMap);

        dbLog.debug("Successfully read " + resultsRead + " user activity results from " + monitoredAgent);

        return readyToBeloggedReadingsMap;
    }

    private static void updateDatabaseRepository(
                                                  String monitoredHost,
                                                  List<ReadingBean> readings ) throws MonitoringException {

        try {
            ReadingsRepository.getInstance().updateDatabaseRepository(monitoredHost, readings);
        } catch (Exception e) {
            throw new MonitoringException("Couldn't update the logging database with new statistic definitions for "
                                          + monitoredHost, e);
        }
    }

    private static int commitToDatabase(
                                         Map<Long, Map<String, Integer>> userActivityStatisticsMap ) {

        // counter to hold the number of results which have been logged
        int resultsAddeed = 0;

        Iterator<Long> it = userActivityStatisticsMap.keySet().iterator();
        while (it.hasNext()) {

            long timestamp = it.next();
            // transform all reading info into 2 Strings: one for DB IDs and
            // one for their values
            StringBuilder statisticDbIds = new StringBuilder();
            StringBuilder statisticValues = new StringBuilder();
            // get the first entry
            for (Entry<String, Integer> readingIdEntry : userActivityStatisticsMap.get(timestamp)
                                                                                  .entrySet()) {
                if (readingIdEntry.getKey() == null) {
                    dbLog.error("This reading ['null' with value '" + readingIdEntry.getValue()
                                + "'] does not have set a reading ID which indicates an error in the attached monitor. We will not insert this reading in the database.");
                }

                int readingDbId = Integer.parseInt(readingIdEntry.getKey());
                if (readingDbId == -1) {

                    dbLog.error("We do not have information in the database about this reading ['"
                                + readingIdEntry.getKey() + "' with value '" + readingIdEntry.getValue()
                                + "']. We will not insert this reading in the database.");
                }

                Integer readingValue = readingIdEntry.getValue();
                if (readingValue == null) {

                    dbLog.error("Null value is passed for this reading ['" + readingIdEntry.getKey()
                                + "' with value '" + readingIdEntry.getValue()
                                + "']. We will not insert this reading in the database.");
                }

                statisticDbIds.append(readingDbId);
                statisticDbIds.append("_");

                statisticValues.append(parseReadingValue(null,
                                                         Float.parseFloat(String.valueOf(readingValue))));
                statisticValues.append("_");

                resultsAddeed++;
            }

            if (statisticDbIds.length() > 0) {
                statisticDbIds.setLength(statisticDbIds.length() - 1);
                statisticValues.setLength(statisticValues.length() - 1);
                // we are logging [users] Total statistics
                dbLog.insertUserActivityStatistcs(ATS_AGENT_HOSTS,
                                                  statisticDbIds.toString(),
                                                  statisticValues.toString(),
                                                  timestamp);
            }

        }

        return resultsAddeed;
    }

    private static float parseReadingValue(
                                            String resultsLine,
                                            Float readingValue ) {

        float floatValue = readingValue;

        // check the validity of the value
        if (Float.isNaN(floatValue) || Float.isInfinite(floatValue)) {
            dbLog.error("A monitor has returned an illegal float number '" + floatValue + "'"
                        + (resultsLine != null
                                               ? (" in '" + resultsLine + "'")
                                               : "")
                        + ". The value of -1.0 will be inserted instead");
            floatValue = -1.0F;
        }

        return floatValue;
    }

}
