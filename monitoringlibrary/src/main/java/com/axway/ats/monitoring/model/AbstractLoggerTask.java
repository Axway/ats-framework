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

import org.apache.log4j.Logger;

/**
 * Used to get the collected system statistical data from the Monitoring service
 * and log it into the logging system.
 * <br>
 * It is repeatedly called in intervals specified by the user 
 */
public abstract class AbstractLoggerTask implements Runnable {

    private static Logger log = Logger.getLogger( AbstractLoggerTask.class );

    /**
     * Task for logging the system statistics in the database
     */
    protected AbstractLoggerTask() {

    }

    protected long parseTimestamp(
                                   String resultsLine,
                                   String timestamp ) {

        try {
            return Long.valueOf( timestamp );
        } catch( NumberFormatException nfe ) {
            log.error( "Unable to parse timestamp '" + timestamp + "' in '" + resultsLine + "'", nfe );
            throw nfe;
        }
    }

    protected float parseReadingValue(
                                       String resultsLine,
                                       String readingValue ) {

        try {
            return parseReadingValue( resultsLine, Float.parseFloat( readingValue ) );

        } catch( NumberFormatException nfe ) {
            log.error( "Unable to parse reading value '" + readingValue + "'"
                       + ( resultsLine != null
                                              ? ( " in '" + resultsLine + "'" )
                                              : "" ), nfe );
            throw nfe;
        }
    }

    protected float parseReadingValue(
                                       String resultsLine,
                                       Float readingValue ) {

        float floatValue = readingValue;

        // check the validity of the value
        if( Float.isNaN( floatValue ) || Float.isInfinite( floatValue ) ) {
            log.error( "A monitor has returned an illegal float number '" + floatValue + "'"
                       + ( resultsLine != null
                                              ? ( " in '" + resultsLine + "'" )
                                              : "" ) + ". The value of -1.0 will be inserted instead" );
            floatValue = -1.0F;
        }

        return floatValue;
    }
}
