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
package com.axway.ats.agent.webapp.restservice.model.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleMonitoringPojo extends BasePojo {

    private String             reading;
    private List<MapEntryPojo> readingParameters;

    public ScheduleMonitoringPojo() {

    }

    public ScheduleMonitoringPojo( String reading, List<MapEntryPojo> readingParameters ) {
        this.reading = reading;
        this.readingParameters = readingParameters;
    }

    public String getReading() {

        return reading;
    }

    public void setReading( String reading ) {

        this.reading = reading;
    }

    public List<MapEntryPojo> getReadingParameters() {

        return readingParameters;
    }

    public void setReadingParameters( List<MapEntryPojo> readingParameters ) {

        this.readingParameters = readingParameters;
    }

    public Map<String, String> getReadingParametersAsMap() {

        Map<String, String> map = new HashMap<>();

        for (MapEntryPojo readingParameter : readingParameters) {
            map.put(readingParameter.getKey(), readingParameter.getValue());
        }

        return map;
    }

}
