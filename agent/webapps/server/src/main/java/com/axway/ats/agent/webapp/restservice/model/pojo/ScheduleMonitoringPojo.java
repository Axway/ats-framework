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

import java.util.Map;

public class ScheduleMonitoringPojo extends BasePojo {

    private String              reading;
    private Map<String, String> readingParameters;

    public ScheduleMonitoringPojo() {

    }

    public ScheduleMonitoringPojo( String reading,
                                   Map<String, String> readingParameters ) {
        this.reading = reading;
        this.readingParameters = readingParameters;
    }

    public String getReading() {

        return reading;
    }

    public void setReading(
                            String reading ) {

        this.reading = reading;
    }

    public Map<String, String> getReadingParameters() {

        return readingParameters;
    }

    public void setReadingParameters(
                                      Map<String, String> readingParameters ) {

        this.readingParameters = readingParameters;
    }

}
