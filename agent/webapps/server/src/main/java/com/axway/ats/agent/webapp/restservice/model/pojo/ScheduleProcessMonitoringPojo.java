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

public class ScheduleProcessMonitoringPojo extends BasePojo {

    private String   processPattern;
    private String   processAlias;
    private String   processUsername;
    private String   parentProcess;
    private String[] processReadingTypes;

    public ScheduleProcessMonitoringPojo() {

    }

    public ScheduleProcessMonitoringPojo( String processPattern,
                                          String processAlias,
                                          String processUsername,
                                          String parentProcess,
                                          String[] processReadingTypes ) {
        this.processPattern = processPattern;
        this.processAlias = processAlias;
        this.processUsername = processUsername;
        this.parentProcess = parentProcess;
        this.processReadingTypes = processReadingTypes;
    }

    public String getProcessPattern() {

        return processPattern;
    }

    public void setProcessPattern(
                                   String processPattern ) {

        this.processPattern = processPattern;
    }

    public String getProcessAlias() {

        return processAlias;
    }

    public void setProcessAlias(
                                 String processAlias ) {

        this.processAlias = processAlias;
    }

    public String getProcessUsername() {

        return processUsername;
    }

    public void setProcessUsername(
                                    String processUsername ) {

        this.processUsername = processUsername;
    }

    public String getParentProcess() {

        return parentProcess;
    }

    public void setParentProcess(
                                  String parentProcess ) {

        this.parentProcess = parentProcess;
    }

    public String[] getProcessReadingTypes() {

        return processReadingTypes;
    }

    public void setProcessReadingTypes(
                                        String[] processReadingTypes ) {

        this.processReadingTypes = processReadingTypes;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("ProcessPattern: " + processPattern + ", ")
          .append("ProcessAlias: " + this.processAlias + ", ")
          .append("ProcessUsername: " + this.processUsername + ", ")
          .append("ParentProcess: " + this.parentProcess + ", ")
          .append("ProcessReadingTypes: " + Arrays.toString(this.processReadingTypes) + "]");

        return sb.toString();
    }

}
