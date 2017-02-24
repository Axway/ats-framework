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
package com.axway.ats.monitoring.model.readings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.axway.ats.common.performance.monitor.beans.BasicReadingBean;
import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.common.performance.monitor.beans.ParentProcessReadingBean;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.log.autodb.DbAccessFactory;
import com.axway.ats.log.autodb.DbWriteAccess;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;

/**
 * Keeps info about all readings that are already populated to the DB.
 * The info is kept per monitored host.
 *
 * Once the reading is populated, we can then send just "reading id" and "reading value" to the
 * database
 */
public class DatabaseReadingsRepository {

    private Map<String, List<BasicReadingBean>> repositoryPerHostMap;

    private static DbWriteAccess                dbAccess = null;

    DatabaseReadingsRepository() {

        repositoryPerHostMap = new HashMap<String, List<BasicReadingBean>>();
    }

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
                                          List<BasicReadingBean> readings,
                                          Map<String, Integer> readingIdToDbIdMap )
                                                                                   throws DatabaseAccessException {

        Logger log = Logger.getLogger( DatabaseReadingsRepository.class );

        if( dbAccess == null ) {
            dbAccess = new DbAccessFactory().getNewDbWriteAccessObject();
        }

        List<BasicReadingBean> repository = repositoryPerHostMap.get( monitoredHost );
        if( repository == null ) {
            repository = new ArrayList<BasicReadingBean>();
            repositoryPerHostMap.put( monitoredHost, repository );
        }

        for( BasicReadingBean reading : readings ) {
            if( reading instanceof FullReadingBean ) {
                FullReadingBean newReading = ( FullReadingBean ) reading;

                StringBuilder newReadingParameters = new StringBuilder();
                Map<String, String> readingParameters = newReading.getParameters();
                if( readingParameters != null && readingParameters.size() > 0 ) {

                    if( readingParameters.containsKey( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS ) ) {

                        newReadingParameters.append( "'" );
                        newReadingParameters.append( readingParameters.get( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS ) );
                        newReadingParameters.append( "'_user pattern is '" );
                        newReadingParameters.append( readingParameters.get( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_RECOGNITION_PATTERN ) );
                        newReadingParameters.append( "'_reading=" );
                        newReadingParameters.append( readingParameters.get( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_READING_ID ) );
                        newReadingParameters.append( "_started by command '" );
                        newReadingParameters.append( readingParameters.get( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_START_COMMAND ) );
                        newReadingParameters.append( "'" );
                    } else {

                        newReadingParameters.append( readingParameters.get( SystemMonitorDefinitions.PARAMETER_NAME__CUSTOM_MESSAGE ) );
                    }
                }

                int newReadingDatabaseId;
                if( reading instanceof ParentProcessReadingBean ) {
                    String thisProcessName = "[process] "
                                             + ( ( ParentProcessReadingBean ) newReading ).getTheNameOfThisParentProcess();
                    String thisReadingName = thisProcessName + " - " + newReading.getName();
                    newReadingDatabaseId = dbAccess.populateSystemStatisticDefinition( thisReadingName,
                                                                                       newReading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_PARENT_NAME ),
                                                                                       thisProcessName,
                                                                                       newReading.getUnit(),
                                                                                       newReadingParameters.toString() );
                    log.debug( "DB id " + newReadingDatabaseId + " for parent process reading: "
                               + thisReadingName );
                } else {
                    String parentName = newReading.getParameter( SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_PARENT_NAME );
                    if( parentName != null ) {
                        parentName = "[process] " + parentName;
                    }
                    newReadingDatabaseId = dbAccess.populateSystemStatisticDefinition( newReading.getName(),
                                                                                       parentName,
                                                                                       "",
                                                                                       newReading.getUnit(),
                                                                                       newReadingParameters.toString() );
                    log.debug( "DB id " + newReadingDatabaseId + " for reading: " + newReading.getName() );
                }

                // remember the DB ID of this reading, because the monitoring remote service gives us the
                // full reading information only the first time
                newReading.setDbId( newReadingDatabaseId );

                // we maintain this map so can easily find the DB id of each Basic reading(it has reading id only)
                readingIdToDbIdMap.put( newReading.getId(), newReading.getDbId() );
            }
        }
    }
}
