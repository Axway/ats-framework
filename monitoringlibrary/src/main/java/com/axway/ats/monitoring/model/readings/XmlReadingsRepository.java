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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.monitoring.model.exceptions.UnsupportedReadingException;

/**
 * Keeps info about all readings as they are loaded from the configuration XML files
 */
public class XmlReadingsRepository {

    private static final Logger          log = Logger.getLogger( XmlReadingsRepository.class );

    private Map<String, FullReadingBean> readings;

    XmlReadingsRepository() {

        readings = new HashMap<String, FullReadingBean>();
    }

    void addReading(
                     String radingName,
                     FullReadingBean reading ) {

        if( readings.containsKey( radingName ) ) {
            log.warn( "The reading [" + reading + "[ will be now replaced with  reading [" + reading + "]" );
        }
        readings.put( radingName, reading );
    }

    void cleanRepository() {

        this.readings.clear();
    }

    boolean isConfigured() {

        return this.readings.size() > 0;
    }

    FullReadingBean getReadingDefinition(
                                          String readingName ) throws UnsupportedReadingException {

        FullReadingBean readingBean = this.readings.get( readingName );
        if( readingBean == null ) {
            throw new UnsupportedReadingException( readingName );
        }

        /* 
         * We need some reading types more than once. Currently this is the case when the 
         * configuration file has a reading like "Process CPU" and we use this reading for more than
         * one process.
         * Our "this.repository" keeps 1 instance of this monitor as it reads it from the configuration file,
         * but runtime we need as many instances as the number of monitored processes.
         * 
         * The fix is to create a new instance of each reading configuration
         */
        return readingBean.getNewCopy();
    }
}
