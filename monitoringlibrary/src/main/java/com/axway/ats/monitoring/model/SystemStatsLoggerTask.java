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
package com.axway.ats.monitoring.model;

import java.util.List;

import com.axway.ats.agent.components.monitoring.operations.clients.InternalSystemMonitoringOperations;
import com.axway.ats.common.performance.monitor.beans.BasicReadingBean;
import com.axway.ats.common.performance.monitor.beans.MonitorResults;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.model.AutoLogger;
import com.axway.ats.monitoring.model.exceptions.MonitoringException;
import com.axway.ats.monitoring.model.readings.ReadingsRepository;

/**
 * Used to get the collected system statistical data from the Monitoring service
 * and log it into the logging system.
 * <br>
 * It is repeatedly called in intervals specified by the user
 */
public class SystemStatsLoggerTask extends AbstractLoggerTask {

    private static AutoLogger log                         = AutoLogger.getLogger( SystemStatsLoggerTask.class.getName() );

    /*
     * If the system statistics are too many, we have to send the data to the
     * DB in chunks.
     *
     * The DB stored procedure accepts "statisticIds" long 1000 chars
     * and "statisticValues" long 8000 chars
     */
    private static final int  MAX_LENGTH_STATISTIC_IDS    = 950;
    private static final int  MAX_LENGTH_STATISTIC_VALUES = 7950;

    private String            monitoredHost;

    /**
     * Task for logging the system statistics in the database
     *
     * @param monitoredHost
     */
    public SystemStatsLoggerTask( String monitoredHost ) {

        this.monitoredHost = monitoredHost;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {

        log.debug( "Getting system monitoring results for " + monitoredHost );
        try {
            InternalSystemMonitoringOperations sysMonitoringActions = new InternalSystemMonitoringOperations( monitoredHost );
            List<MonitorResults> monitorResults = sysMonitoringActions.getCollectedResults();

            if( monitorResults.size() > 0 ) {

                // update the DB definitions if needed
                for( MonitorResults monitorResult : monitorResults ) {
                    updateDatabaseRepository( monitoredHost, monitorResult.getReadings() );
                }

                //log the results to the database
                int resultsAddeed = logResults( monitorResults );
                log.debug( "Successfully sent " + resultsAddeed
                           + " system monitoring results to the logging database" );
            } else {
                log.warn( "No new system monitoring results to log" );
            }
        } catch( Exception e ) {
            log.error( "Could not log system monitoring results for " + monitoredHost
                       + ". Will skip to next iteration", e );
        }
    }

    private int logResults(
                            List<MonitorResults> monitorResults ) {

        // counter to hold the number of results which have logged
        int resultsAddeed = 0;

        for( MonitorResults newResultsLine : monitorResults ) {
            resultsAddeed += logResultsForOneTimestamp( newResultsLine );
        }

        return resultsAddeed;
    }

    private int logResultsForOneTimestamp(
                                           MonitorResults resultsLine ) {

        ReadingsRepository readingsRepository = ReadingsRepository.getInstance();

        int resultsAddeed = 0;

        // transform all reading info into 2 Strings: one for DB IDs and one for their values
        StringBuilder statisticDbIds = new StringBuilder();
        StringBuilder statisticValues = new StringBuilder();
        for( BasicReadingBean reading : resultsLine.getReadings() ) {
            String readingId = reading.getId();
            if( readingId == null ) {
                log.error( "This reading ["
                           + reading.toString()
                           + "] does not have set a reading ID which indicates an error in some of the attached monitors. We will not insert this reading in the database." );
                continue;
            }
            Integer readingDbId = readingsRepository.getReadingDbId( readingId );
            if( readingDbId == null ) {
                log.error( "We do not have information in the database about this reading ["
                           + reading.toString() + "]. We will not insert this reading in the database." );
                continue;
            }
            String readingValue = reading.getValue();
            if( readingValue == null ) {
                log.error( "Null value is passed for this reading [" + reading.toString()
                           + "]. We will not insert this reading in the database." );
                continue;
            }
            statisticDbIds.append( readingDbId );
            statisticDbIds.append( "_" );

            statisticValues.append( readingValue );
            statisticValues.append( "_" );

            resultsAddeed++;

            if( statisticDbIds.length() > MAX_LENGTH_STATISTIC_IDS
                || statisticValues.length() > MAX_LENGTH_STATISTIC_VALUES ) {
                // we have to send a chunk
                statisticDbIds.setLength( statisticDbIds.length() - 1 );
                statisticValues.setLength( statisticValues.length() - 1 );

                log.insertSystemStatistcs( monitoredHost,
                                           statisticDbIds.toString(),
                                           statisticValues.toString(),
                                           resultsLine.getTimestamp() );

                statisticDbIds.setLength( 0 );
                statisticValues.setLength( 0 );
            }
        }

        if( statisticDbIds.length() > 0 ) {
            // send the last(or only) chunk
            statisticDbIds.setLength( statisticDbIds.length() - 1 );
            statisticValues.setLength( statisticValues.length() - 1 );

            log.insertSystemStatistcs( monitoredHost,
                                       statisticDbIds.toString(),
                                       statisticValues.toString(),
                                       resultsLine.getTimestamp() );
        }
        return resultsAddeed;
    }

    private void updateDatabaseRepository(
                                           String monitoredHost,
                                           List<BasicReadingBean> readings ) throws MonitoringException {

        try {
            ReadingsRepository.getInstance().updateDatabaseRepository( monitoredHost, readings );
        } catch( DatabaseAccessException e ) {
            throw new MonitoringException( "Could update the logging database with new statistic definitions for "
                                                   + monitoredHost,
                                           e );
        }
    }
}
