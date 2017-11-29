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
package com.axway.ats.common.performance.monitor.beans;

import java.io.Serializable;
import java.util.List;

/**
 * A container for all readings for 1 timestamp
 */
public class MonitorResults implements Serializable {

    private static final long      serialVersionUID = 1L;

    private long                   timestamp;
    private List<ReadingBean> readings;

    public MonitorResults( long timestamp,
                           List<ReadingBean> readingBeans ) {

        this.timestamp = timestamp;
        this.readings = readingBeans;
    }

    public long getTimestamp() {

        return timestamp;
    }

    public List<ReadingBean> getReadings() {

        return readings;
    }

    @Override
    public String toString() {

        StringBuilder result = new StringBuilder();
        result.append( timestamp );
        result.append( ": " );
        for( ReadingBean reading : readings ) {
            result.append( reading.getName() );
            result.append( " = " );
            result.append( reading.getValue() );
            result.append( ", " );
        }

        return result.toString();
    }
}
