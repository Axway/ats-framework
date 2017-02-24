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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.axway.ats.agent.webapp.client.AgentMonitoringClient;
import com.axway.ats.common.performance.monitor.beans.BasicReadingBean;
import com.axway.ats.common.performance.monitor.beans.MonitorResults;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.model.AutoLogger;
import com.axway.ats.monitoring.model.exceptions.MonitoringException;
import com.axway.ats.monitoring.model.readings.ReadingsRepository;

/**
 * Used to get the collected statistical data from one or more ATS Agents
 * and log it into the logging system.
 *
 * <br><br>Important things to keep in mind:
 *  <blockquote>
 *  There might be more than 1 Agent running same actions at the same time, but our customers want to get a
 *  consolidate view of the user activity as it was just 1 Agent
 *  <br>This means that if we get for example 10 users running same action on 1 loader and 5 on another, our customers
 *  need to see 15 users for same timestamps
 *  <br>That is why we do not insert the readings until we get the values from all monitored loaders
 *  </blockquote>
 *  <blockquote>
 *  We do not know the actions that will be run before we start monitoring,
 *  so we do not know the readings we will get(this is not the case when monitoring the system with the Performance Monitoring service)
 *  <br>So we cannot populate the readings to the database before starting the monitoring process
 *  </blockquote>
 *  <blockquote>
 *  All the actions do not start at the beginning so when unknown reading arrive we need to
 *  insert 0 values for the past timestamps and then continue monitoring this reading in the normal way
 *  <br>If we do not do that, this reading will be shifted to the beginning of the graph and the timestamps
 *  will not match
 *  </blockquote>
 */
public class UserActivityLoggerTask extends AbstractLoggerTask {

    private static AutoLogger       log             = AutoLogger.getLogger( UserActivityLoggerTask.class.getName() );

    // All Agents are presented as a single virtual host.
    // The reason is that user wants to know the total number of simulated users, he does not
    // care how many users are operating on each Agent.
    public static final String      ATS_AGENT_HOSTS = "ATS Agents";

    // the agents we monitor
    private final Set<String>       monitoredAgents;

    // how many times we have collected statistics about an Agent
    private Map<String, Integer>    collectTimesPerLoader;

    // As we cannot commit the readings as soon as we get them(mandatory for the case with more than 1 loader),
    // we store them in a temporarily storage
    private UserActivityTempStorage readingsTempStorage;

    /**
     * Task for logging the system statistics in the database
     *
     * @param monitoredAgents
     */
    public UserActivityLoggerTask( Set<String> monitoredAgents ) {

        this( monitoredAgents, new HashMap<String, Integer>() );
    }

    public UserActivityLoggerTask( Set<String> monitoredAgents, Map<String, Integer> collectTimes ) {

        this.monitoredAgents = monitoredAgents;
        this.collectTimesPerLoader = collectTimes;

        this.readingsTempStorage = new UserActivityTempStorage( this.monitoredAgents.size() );
    }

    public Map<String, Integer> getcollectTimesPerLoader() {

        return collectTimesPerLoader;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {

        // read the user activity from all monitored Agents
        Iterator<String> monitoredAgentsIterator = monitoredAgents.iterator();
        while( monitoredAgentsIterator.hasNext() ) {
            String monitoredAgent = monitoredAgentsIterator.next();

            log.debug( "Getting user activity at " + monitoredAgent );

            try {
                List<MonitorResults> newResults = new AgentMonitoringClient( monitoredAgent ).getMonitoringResults();
                if( newResults.size() > 0 ) {
                    parseReadingsAndMoveToTempRepository( monitoredAgent, newResults );
                } else {
                    log.warn( "No new user activity on " + monitoredAgent );
                }
            } catch( Exception e ) {
                log.error( "Could not read user activity results for " + monitoredAgent
                           + ". Will skip to next iteration", e );
            }
        }

        // send the consolidated user activity to the logging database
        int resultsAddeed = this.readingsTempStorage.commitToDatabase();
        log.debug( "Successfully sent " + resultsAddeed + " user activity results to the logging database" );

    }

    /**
     * We cannot send a reading for a timestamp before we get it from all agents
     *
     * @param monitoredAgent
     * @param newResults
     */
    private void parseReadingsAndMoveToTempRepository( String monitoredAgent,
                                                       List<MonitorResults> newMonitorResults ) {

        // counter for user info
        int resultsRead = 0;

        for( MonitorResults newMonitorResult : newMonitorResults ) {

            // update the collect times for this loader
            Integer currentCollectTimes = this.collectTimesPerLoader.get( monitoredAgent );
            if( currentCollectTimes == null ) {
                // this is the first time
                this.collectTimesPerLoader.put( monitoredAgent, 0 );
            } else {
                // increment
                this.collectTimesPerLoader.put( monitoredAgent, currentCollectTimes + 1 );
            }

            // assign DB IDs to these readings if needed
            List<BasicReadingBean> newReadings = newMonitorResult.getReadings();
            updateDatabaseRepository( monitoredAgent, newReadings );

            // Map<reading ID, reading value>
            Map<String, Integer> readingsMap = new HashMap<String, Integer>();
            for( BasicReadingBean newReading : newReadings ) {
                readingsMap.put( newReading.getId(), Integer.valueOf( newReading.getValue() ) );

                resultsRead++;
            }

            // remember the readings info for later commit
            this.readingsTempStorage.addReadings( newMonitorResult.getTimestamp(), readingsMap );
        }

        log.debug( "Successfully read " + resultsRead + " user activity results from " + monitoredAgent );
    }

    /**
     * Populate these reading to the DB, so they have their own DB IDs
     *
     * @param monitoredHost
     * @param readings
     * @throws MonitoringException
     */
    private void updateDatabaseRepository( String monitoredHost,
                                           List<BasicReadingBean> readings ) throws MonitoringException {

        try {
            ReadingsRepository.getInstance().updateDatabaseRepository( monitoredHost, readings );
        } catch( DatabaseAccessException e ) {
            throw new MonitoringException( "Couldn't update the logging database with new statistic definitions for "
                                           + monitoredHost, e );
        }
    }

    class UserActivityTempStorage {

        // Timestamp and its readings. The map is sorted by the timestamps
        private SortedMap<Long, UserActivityReadingInfoPerTimestamp> pendingReadings = new TreeMap<Long, UserActivityReadingInfoPerTimestamp>();

        private int                                                  numberMonitoredAgents;

        public UserActivityTempStorage( int numberMonitoredAgents ) {

            this.numberMonitoredAgents = numberMonitoredAgents;
        }

        void addReadings( long timestamp, Map<String, Integer> readingsMap ) {

            if( !pendingReadings.containsKey( timestamp ) ) {
                // we did not have data for this timestamp
                pendingReadings.put( timestamp, new UserActivityReadingInfoPerTimestamp() );
            }

            // add new data for this timestamp
            pendingReadings.get( timestamp ).addReadings( readingsMap );
        }

        int commitToDatabase() {

            // counter to hold the number of results which have logged
            int resultsAddeed = 0;

            ReadingsRepository readingsRepository = ReadingsRepository.getInstance();

            // iterate all pending readings for all timestamps
            for( Entry<Long, UserActivityReadingInfoPerTimestamp> timestampEntry : pendingReadings.entrySet() ) {
                UserActivityReadingInfoPerTimestamp userActivityPerTimestamp = timestampEntry.getValue();

                // check if we got data from ALL loaders at this timestamp
                if( userActivityPerTimestamp.getNumberCollectedChunks() == numberMonitoredAgents ) {
                    // transform all reading info into 2 Strings: one for DB IDs and one for their values
                    Map<String, Integer> readingsMap = userActivityPerTimestamp.getReadingsMap();
                    StringBuilder statisticDbIds = new StringBuilder();
                    StringBuilder statisticValues = new StringBuilder();
                    for( Entry<String, Integer> readingIdEntry : readingsMap.entrySet() ) {

                        if( readingIdEntry.getKey() == null ) {
                            log.error( "This reading ['null' with value '" + readingIdEntry.getValue()
                                       + "'] does not have set a reading ID which indicates an error in some of the attached monitors. We will not insert this reading in the database." );
                            continue;
                        }
                        Integer readingDbId = readingsRepository.getReadingDbId( readingIdEntry.getKey() );
                        if( readingDbId == null ) {
                            log.error( "We do not have information in the database about this reading ['"
                                       + readingIdEntry.getKey() + "' with value '" + readingIdEntry.getValue()
                                       + "']. We will not insert this reading in the database." );
                            continue;
                        }
                        Integer readingValue = readingIdEntry.getValue();
                        if( readingValue == null ) {
                            log.error( "Null value is passed for this reading ['" + readingIdEntry.getKey()
                                       + "' with value '" + readingIdEntry.getValue() 
                                       + "']. We will not insert this reading in the database." );
                            continue;
                        }

                        statisticDbIds.append( readingDbId );
                        statisticDbIds.append( "_" );

                        statisticValues.append( parseReadingValue( null, String.valueOf( readingValue ) ) );
                        statisticValues.append( "_" );

                        resultsAddeed++;
                    }

                    if( statisticDbIds.length() > 0 ) {
                        statisticDbIds.setLength( statisticDbIds.length() - 1 );
                        statisticValues.setLength( statisticValues.length() - 1 );

                        log.insertSystemStatistcs( ATS_AGENT_HOSTS, statisticDbIds.toString(),
                                                   statisticValues.toString(), timestampEntry.getKey() );

                        // mark the flushed item, we do not remove the map item here, as we are iterating over this map
                        pendingReadings.put( timestampEntry.getKey(), null );
                    }
                }
            }

            // cleanup the map by constructing a new map without the ones that have null values
            SortedMap<Long, UserActivityReadingInfoPerTimestamp> cleanedPendingReadings = new TreeMap<Long, UserActivityReadingInfoPerTimestamp>();
            for( Entry<Long, UserActivityReadingInfoPerTimestamp> timestampEntry : pendingReadings.entrySet() ) {
                if( timestampEntry.getValue() != null ) {
                    cleanedPendingReadings.put( timestampEntry.getKey(), timestampEntry.getValue() );
                }
            }
            this.pendingReadings = cleanedPendingReadings;

            return resultsAddeed;
        }
    }

    /**
     * Collects all readings for all monitored agents for one time-stamp.
     * The reading values for same reading names are kept at one single place, e.g. we add them to one another
     */
    class UserActivityReadingInfoPerTimestamp {

        private int                  numberCollectedChunks;

        // Map<reading ID, reading value>
        private Map<String, Integer> readingsMap;

        public UserActivityReadingInfoPerTimestamp() {

            this.readingsMap = new HashMap<String, Integer>();
        }

        public Map<String, Integer> getReadingsMap() {

            return this.readingsMap;
        }

        public void addReadings( Map<String, Integer> newReadings ) {

            // iterate all the known readings
            for( Entry<String, Integer> readingIdEntry : readingsMap.entrySet() ) {
                Integer newReadingValue = newReadings.get( readingIdEntry.getKey() );
                if( newReadingValue != null ) {
                    // the new readings have this known reading
                    // 1. we add the new reading value to the current reading value
                    // 2. we remove this reading from the list of new readings
                    int newCalculateReadingValue = readingIdEntry.getValue() + newReadingValue;
                    readingsMap.put( readingIdEntry.getKey(), newCalculateReadingValue );
                    newReadings.remove( readingIdEntry.getKey() );
                }
            }

            // check if the new readings have some unknown reading
            for( Entry<String, Integer> readingIdEntry : newReadings.entrySet() ) {
                // add this new reading
                readingsMap.put( readingIdEntry.getKey(), readingIdEntry.getValue() );
            }

            numberCollectedChunks++;
        }

        public int getNumberCollectedChunks() {

            return numberCollectedChunks;
        }
    }
}
