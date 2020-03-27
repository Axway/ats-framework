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

import java.text.ParseException;
import java.util.Date;

import com.axway.ats.log.autodb.io.AbstractDbAccess;

public class Statistic extends DbEntity {

    private static final long  serialVersionUID              = 1L;

    // name for container combining statistics from different action queues
    public static final String COMBINED_STATISTICS_CONTAINER = "ALL QUEUES";

    public long                statisticTypeId;

    public String              name;
    public String              parentName;

    public String              unit;

    public float               value;

    public float               avgValue;
    public float               sumValue;
    public float               countValue;
    public float               totalValue;

    public float               transferSize;

    // This date is in UTC format
    private String             date;

    public int                 machineId;

    public int                 testcaseId;

    public int                 result;

    public void setDate( String date ) {

        this.date = date;
    }

    public String getDate() {

        try {
            long utcMillis = AbstractDbAccess.DATE_FORMAT.parse(this.date).getTime();
            Date localDate = new Date(utcMillis - timeOffset);
            return AbstractDbAccess.DATE_FORMAT.format(localDate);
        } catch (ParseException e) {
            return "";
        }

    }

    @Override
    public String toString() {

        StringBuilder msg = new StringBuilder("Statistic description: ");
        msg.append("name=" + name);
        if (parentName != null) {
            msg.append(", parentName=" + parentName);
        }
        msg.append(", value=" + value);
        msg.append(", transferSize=" + transferSize);
        msg.append(", statisticTypeId=" + statisticTypeId);
        msg.append(", timestamp=" + getStartTimestamp());
        msg.append(", date=" + getDate());

        return msg.toString();
    }

    /**
     * Makes a new copy of this statistic, 
     * without setting values
     */
    public Statistic newInstance() {

        Statistic newStatistic = new Statistic();
        newStatistic.name = this.name;
        newStatistic.parentName = this.parentName;
        newStatistic.unit = this.unit;
        newStatistic.startTimestamp = this.startTimestamp;
        newStatistic.timeOffset = this.timeOffset;
        newStatistic.date = this.date;
        newStatistic.machineId = this.machineId;
        newStatistic.testcaseId = this.testcaseId;
        newStatistic.result = this.result;

        return newStatistic;
    }

    public String getUid() {

        return testcaseId + "->" + machineId + "->" + parentName + "->" + name;
    }

    public String getCombinedStatisticUid() {

        return testcaseId + "->" + machineId + "->" + COMBINED_STATISTICS_CONTAINER + "->" + name;
    }
}
