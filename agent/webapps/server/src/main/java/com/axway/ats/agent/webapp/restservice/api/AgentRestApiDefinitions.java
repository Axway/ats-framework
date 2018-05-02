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
package com.axway.ats.agent.webapp.restservice.api;

public class AgentRestApiDefinitions {

    public static final String REST_API_SERVICE_PATH    = "/agentapp/api/v1";
    public static final String INITIALIZE_ACTION_CLASS_PATH   = REST_API_SERVICE_PATH + "/" + "actions";
    public static final String EXECUTE_ACTION_METHOD_PATH     = REST_API_SERVICE_PATH + "/" + "actions/execute";
    public static final String DEINITIALIZE_ACTION_CLASS_PATH = REST_API_SERVICE_PATH + "/" + "actions";

}
