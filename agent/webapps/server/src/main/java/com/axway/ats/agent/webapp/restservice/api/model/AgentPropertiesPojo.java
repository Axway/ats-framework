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
package com.axway.ats.agent.webapp.restservice.api.model;

public class AgentPropertiesPojo {

    private String testcaseId_onAgent;
    private String sessionId;
    private String operation;
    private String value;

    public AgentPropertiesPojo() {}

    public AgentPropertiesPojo( String testcaseId_onAgent, String sessionId, String operation, String value ) {

        this.testcaseId_onAgent = testcaseId_onAgent;
        this.sessionId = sessionId;
        this.operation = operation;
        this.value = value;

    }

    public String getTestcaseId_onAgent() {

        return testcaseId_onAgent;
    }

    public void setTestcaseId_onAgent( String testcaseId_onAgent ) {

        this.testcaseId_onAgent = testcaseId_onAgent;
    }

    public String getSessionId() {

        return sessionId;
    }

    public void setSessionId( String sessionId ) {

        this.sessionId = sessionId;
    }

    public String getOperation() {

        return operation;
    }

    public void setOperation( String operation ) {

        this.operation = operation;
    }

    public String getValue() {

        return value;
    }

    public void setValue( String value ) {

        this.value = value;
    }

}
