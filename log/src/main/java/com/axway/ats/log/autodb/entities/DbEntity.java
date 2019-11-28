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
package com.axway.ats.log.autodb.entities;

import java.io.Serializable;
import java.util.Date;

import com.axway.ats.log.autodb.io.AbstractDbAccess;

public class DbEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    protected long            startTimestamp   = -1;
    protected long            endTimestamp     = -1;
    /*
     * Stores the UTC time offset
     * Since startTimestamp and endTimestamp's values are stored in UTC locale in the database,
     * by using the offset, each of these values is returned according to the caller locale
     * */
    protected long            timeOffset       = 0;

    public long getStartTimestamp() {

        return startTimestamp + timeOffset;
    }

    public void setStartTimestamp( long startTimestamp ) {

        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {

        return endTimestamp + timeOffset;
    }

    public void setEndTimestamp( long endTimestamp ) {

        this.endTimestamp = endTimestamp;
    }

    public long getDuration( long currentTimestamp ) {

        if (endTimestamp != -1) {

            // both start and end timestamp are received from the DB,
            // so they are both in UTC locale and timeOffset is not needed
            return endTimestamp - startTimestamp;

        } else {

            long duration = currentTimestamp - getStartTimestamp();

            return duration;
        }

    }

    public void setTimeOffset( long timeOffset ) {

        this.timeOffset = timeOffset;
    }

    public long getTimeOffset() {

        return timeOffset;

    }

    public String getDateStart() {

        if (startTimestamp != -1) {
            return AbstractDbAccess.DATE_FORMAT_NO_YEAR.format(new Date(getStartTimestamp()));
        } else {
            return "";
        }

    }

    public String getDateStartLong() {

        if (startTimestamp != -1) {
            return AbstractDbAccess.DATE_FORMAT.format(new Date(getStartTimestamp()));
        } else {
            return "";
        }

    }

    public String getDateEnd() {

        if (endTimestamp != -1) {
            return AbstractDbAccess.DATE_FORMAT_NO_YEAR.format(new Date(getEndTimestamp()));
        } else {
            return "";
        }

    }

    public String getDateEndLong() {

        if (endTimestamp != -1) {
            return AbstractDbAccess.DATE_FORMAT.format(new Date(getEndTimestamp()));
        } else {
            return "";
        }

    }

    public String getDurationAsString( long currentTimestamp ) {

        long durationSeconds = getDuration(currentTimestamp) / 1000;

        // any duration, less than one second is assumed to be one second
        if (durationSeconds <= 0) {
            durationSeconds = 1;
        }

        return AbstractDbAccess.formatTimeDiffereceFromSecondsToString((int) durationSeconds);

    }

}
