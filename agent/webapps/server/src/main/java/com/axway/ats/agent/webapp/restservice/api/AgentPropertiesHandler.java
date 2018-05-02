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

import java.util.Arrays;

import javax.ws.rs.core.Response;

//import com.axway.ats.agent.core.ActionHandler;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.InternalComponentException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.model.ActionPojo;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.log.appenders.PassiveDbAppender;

/**
 * This class operates over the agent properties (e.g. getting and setting agent properties is done via this class)
 * */
public class AgentPropertiesHandler {

    public class Operation {
        public static final String GET_ATS_VERSION               = "getAtsVersion";
        public static final String GET_CLASSPATH                 = "getClasspath";
        public static final String LOG_CLASSPATH                 = "logClasspath";
        public static final String GET_AGENT_HOME                = "getAgentHome";
        public static final String GET_HOST_NAME                 = "getHostname";
        public static final String GET_NUMBER_PENDING_LOG_EVENTS = "getNumberPendingLogEvents";
        public static final String LOG_DUPLICATED_JARS           = "logDuplicatedJars";
        public static final String GET_DUPLICATED_JARS           = "getDuplicatedJars";
        public static final String IS_COMPONENT_LOADED           = "isComponentLoaded";
        public static final String GET_SYSTEM_PROPERTY           = "getSystemProperty";
    }

    public synchronized static Response executeOperation( String operation,
                                                          String value ) throws NoSuchComponentException,
                                                                         NoSuchActionException,
                                                                         ActionExecutionException,
                                                                         InternalComponentException,
                                                                         NoCompatibleMethodFoundException {

        switch (operation) {
            case Operation.GET_ATS_VERSION:
                return Response.ok("{\"ats_version\":\""
                                   + executeAction("auto-system-operations",
                                                   "Internal System Operations get Ats Version", new Object[]{})
                                   + "\"}")
                               .build();
            case Operation.GET_CLASSPATH:
                String[] classpath = (String[]) executeAction("auto-system-operations",
                                                              "Internal System Operations get Class Path",
                                                              new Object[]{});
                return Response.ok("{\"classpath\":" + Arrays.asList(classpath).toString() + "}")
                               .build();
            case Operation.LOG_CLASSPATH:
                executeAction("auto-system-operations", "Internal System Operations log Class Path", new Object[]{});
                return Response.ok("{\"status\":\"Operation '" + operation + "' successfully executed\"}")
                               .build();
            case Operation.GET_AGENT_HOME:
                return Response.ok("{\"agent_home\":\"" + System.getProperty(AtsSystemProperties.AGENT_HOME_FOLDER)
                                   + "\"}")
                               .build();
            case Operation.LOG_DUPLICATED_JARS:
                executeAction("auto-system-operations", "Internal System Operations log Duplicated Jars",
                              new Object[]{});
                return Response.ok("{\"status\":\"Operation '" + operation + "' successfully executed\"}")
                               .build();
            case Operation.GET_DUPLICATED_JARS:
                String[] duplicatedJars = (String[]) executeAction("auto-system-operations",
                                                                   "Internal System Operations get Duplicated Jars",
                                                                   new Object[]{});
                return Response.ok("{\"duplicated_jars\":" + Arrays.asList(duplicatedJars).toString()
                                   + "}")
                               .build();
            case Operation.GET_NUMBER_PENDING_LOG_EVENTS:
                PassiveDbAppender dbAppender = PassiveDbAppender.getCurrentInstance();
                int numOfEvents = (dbAppender != null)
                                                       ? dbAppender.getNumberPendingLogEvents()
                                                       : -1;
                return Response.ok("{\"number_of_pending_log_events\":" + numOfEvents
                                   + "}")
                               .build();
            case Operation.GET_HOST_NAME:
                String hostname = (String) executeAction("auto-system-operations",
                                                         "Internal System Operations get Hostname",
                                                         new Object[]{});
                return Response.ok("{\"hostname\":\"" + hostname
                                   + "\"}")
                               .build();
            case Operation.IS_COMPONENT_LOADED:
                boolean isLoaded = false;
                try {
                    // check if the component is loaded
                    ComponentRepository.getInstance().getComponentActionMap(null, value);
                    isLoaded = true;
                } catch (NoSuchComponentException e) {
                    isLoaded = false;
                }
                return Response.ok("{\"is_component_loaded\":" + isLoaded
                                   + "}")
                               .build();
            case Operation.GET_SYSTEM_PROPERTY:
                String systemProperty = (String) executeAction("auto-system-operations",
                                                               "Internal System Operations Get System Property",
                                                               value.split(","));
                return Response.ok("{\"system_property_value\":\"" + systemProperty
                                   + "\"}")
                               .build();
            default:
                throw new UnsupportedOperationException("Could not execute unsupported operation '" + operation
                                                        + "'");

        }

    }

    private static Object executeAction( String componentName, String actionName,
                                         Class<?>[] argumentsTypes, String[] argumentsValues ) throws NoSuchComponentException, NoSuchActionException,
                                                              ActionExecutionException, InternalComponentException,
                                                              NoCompatibleMethodFoundException {

        int actionId
        return ActionHandler.executeAction(null, componentName,
                                           actionName, arguments);
    }

}
