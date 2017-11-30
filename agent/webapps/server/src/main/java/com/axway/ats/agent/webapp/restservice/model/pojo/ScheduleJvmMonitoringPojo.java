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

import java.util.Arrays;

public class ScheduleJvmMonitoringPojo extends BasePojo {

    private String   jvmPort;
    private String   alias;
    private String[] jvmReadingTypes;

    public ScheduleJvmMonitoringPojo() {

    }

    public ScheduleJvmMonitoringPojo( String jvmPort,
                                      String alias,
                                      String[] jvmReadingTypes ) {
        this.jvmPort = jvmPort;
        this.alias = alias;
        this.jvmReadingTypes = jvmReadingTypes;
    }

    public String getJvmPort() {

        return jvmPort;
    }

    public void setJvmPort(
                            String jvmPort ) {

        this.jvmPort = jvmPort;
    }

    public String getAlias() {

        return alias;
    }

    public void setAlias(
                          String alias ) {

        this.alias = alias;
    }

    public String[] getJvmReadingTypes() {

        return jvmReadingTypes;
    }

    public void setJvmReadingTypes(
                                    String[] jvmReadingTypes ) {

        this.jvmReadingTypes = jvmReadingTypes;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("JvmPort: " + this.jvmPort + ", ")
          .append("Alias: " + this.alias + ", ")
          .append("JvmReadingTypes: " + Arrays.toString(this.jvmReadingTypes));

        return sb.toString();

    }

}
