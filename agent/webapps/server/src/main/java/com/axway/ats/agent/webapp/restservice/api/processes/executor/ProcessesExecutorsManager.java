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
package com.axway.ats.agent.webapp.restservice.api.processes.executor;

import java.lang.reflect.InvocationTargetException;

import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.google.gson.Gson;

public class ProcessesExecutorsManager {

    private static final int    UNINITIALIZED_RESOURCE_ID = -1;
    private static final String COMPONENT_NAME            = "auto-system-operations";

    private static final Gson   GSON                      = new Gson();

    public static synchronized int
            initializeProcessExecutor( String sessionId, String command,
                                       String[] commandArguments ) throws NoSuchActionException,
                                                                   NoCompatibleMethodFoundException,
                                                                   NoSuchComponentException, ClassNotFoundException,
                                                                   InstantiationException, IllegalAccessException,
                                                                   IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, UNINITIALIZED_RESOURCE_ID, COMPONENT_NAME,
                                         "Internal Process Operations init Process Executor",
                                         new String[]{ String.class.getName(), String[].class.getName() },
                                         new String[]{ GSON.toJson(command), GSON.toJson(commandArguments) });

        // create only the action class instance first
        int resourceId = ResourcesManager.initializeResource(pojo);
        pojo.setResourceId(resourceId);

        // then execute the action method
        ResourcesManager.executeOverResource(pojo);

        return resourceId;

    }

    public static synchronized Object startProcess( String sessionId,
                                                    int resourceId,
                                                    String workDirectory,
                                                    String standardOutputFile,
                                                    String errorOutputFile,
                                                    boolean logStandardOutput,
                                                    boolean logErrorOutput,
                                                    boolean waitForCompletion ) throws NoSuchActionException,
                                                                                NoCompatibleMethodFoundException,
                                                                                NoSuchComponentException,
                                                                                ClassNotFoundException,
                                                                                InstantiationException,
                                                                                IllegalAccessException,
                                                                                IllegalArgumentException,
                                                                                InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId,
                                         resourceId,
                                         COMPONENT_NAME,
                                         "Internal Process Operations start Process",
                                         new String[]{ String.class.getName(),
                                                       String.class.getName(),
                                                       String.class.getName(),
                                                       boolean.class.getName(),
                                                       boolean.class.getName(),
                                                       boolean.class.getName() },
                                         new String[]{ GSON.toJson(workDirectory),
                                                       GSON.toJson(standardOutputFile),
                                                       GSON.toJson(errorOutputFile),
                                                       GSON.toJson(logStandardOutput),
                                                       GSON.toJson(logErrorOutput),
                                                       GSON.toJson(waitForCompletion) });

        return ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized String getStandardOutput( String sessionId,
                                                         int resourceId ) throws NoSuchActionException,
                                                                          NoCompatibleMethodFoundException,
                                                                          NoSuchComponentException,
                                                                          ClassNotFoundException,
                                                                          InstantiationException,
                                                                          IllegalAccessException,
                                                                          IllegalArgumentException,
                                                                          InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations get Process Standard Output", null, null);

        return (String) ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized String getCurrentStandardOutput( String sessionId,
                                                                int resourceId ) throws NoSuchActionException,
                                                                                 NoCompatibleMethodFoundException,
                                                                                 NoSuchComponentException,
                                                                                 ClassNotFoundException,
                                                                                 InstantiationException,
                                                                                 IllegalAccessException,
                                                                                 IllegalArgumentException,
                                                                                 InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations get Process Current Standard Output", null, null);

        return (String) ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized boolean isStandardOutputFullyRead( String sessionId,
                                                                 int resourceId ) throws NoSuchActionException,
                                                                                  NoCompatibleMethodFoundException,
                                                                                  NoSuchComponentException,
                                                                                  ClassNotFoundException,
                                                                                  InstantiationException,
                                                                                  IllegalAccessException,
                                                                                  IllegalArgumentException,
                                                                                  InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations is Standard Output Fully Read", null, null);

        return (boolean) ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized String getStandardErrorOutput( String sessionId,
                                                              int resourceId ) throws NoSuchActionException,
                                                                               NoCompatibleMethodFoundException,
                                                                               NoSuchComponentException,
                                                                               ClassNotFoundException,
                                                                               InstantiationException,
                                                                               IllegalAccessException,
                                                                               IllegalArgumentException,
                                                                               InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations get Process Error Output", null, null);

        return (String) ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized String getCurrentStandardErrorOutput( String sessionId,
                                                                     int resourceId ) throws NoSuchActionException,
                                                                                      NoCompatibleMethodFoundException,
                                                                                      NoSuchComponentException,
                                                                                      ClassNotFoundException,
                                                                                      InstantiationException,
                                                                                      IllegalAccessException,
                                                                                      IllegalArgumentException,
                                                                                      InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations get Process Current Error Output", null, null);

        return (String) ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized boolean isStandardErrorOutputFullyRead( String sessionId,
                                                                      int resourceId ) throws NoSuchActionException,
                                                                                       NoCompatibleMethodFoundException,
                                                                                       NoSuchComponentException,
                                                                                       ClassNotFoundException,
                                                                                       InstantiationException,
                                                                                       IllegalAccessException,
                                                                                       IllegalArgumentException,
                                                                                       InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations is Error Output Fully Read", null, null);

        return (boolean) ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized int getProcessExitCode( String sessionId, int resourceId ) throws NoSuchActionException,
                                                                                          NoCompatibleMethodFoundException,
                                                                                          NoSuchComponentException,
                                                                                          ClassNotFoundException,
                                                                                          InstantiationException,
                                                                                          IllegalAccessException,
                                                                                          IllegalArgumentException,
                                                                                          InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations get Process Exit Code", null, null);

        return (Integer) ResourcesManager.executeOverResource(pojo);
    }

    public static synchronized int getProcessId( String sessionId, int resourceId ) throws NoSuchActionException,
                                                                                    NoCompatibleMethodFoundException,
                                                                                    NoSuchComponentException,
                                                                                    ClassNotFoundException,
                                                                                    InstantiationException,
                                                                                    IllegalAccessException,
                                                                                    IllegalArgumentException,
                                                                                    InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations get Process Id", null, null);

        return (Integer) ResourcesManager.executeOverResource(pojo);
    }

    public static synchronized void killProcess( String sessionId, int resourceId ) throws NoSuchActionException,
                                                                                    NoCompatibleMethodFoundException,
                                                                                    NoSuchComponentException,
                                                                                    ClassNotFoundException,
                                                                                    InstantiationException,
                                                                                    IllegalAccessException,
                                                                                    IllegalArgumentException,
                                                                                    InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations kill Process", null, null);

        ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized void killProcessWithChildren( String sessionId,
                                                             int resourceId ) throws NoSuchActionException,
                                                                              NoCompatibleMethodFoundException,
                                                                              NoSuchComponentException,
                                                                              ClassNotFoundException,
                                                                              InstantiationException,
                                                                              IllegalAccessException,
                                                                              IllegalArgumentException,
                                                                              InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations kill Process And Its Children", null, null);

        ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized int killExternalProcess( String sessionId,
                                                         int resourceId,
                                                         String startCommandSnippet ) throws NoSuchActionException,
                                                                                      NoCompatibleMethodFoundException,
                                                                                      NoSuchComponentException,
                                                                                      ClassNotFoundException,
                                                                                      InstantiationException,
                                                                                      IllegalAccessException,
                                                                                      IllegalArgumentException,
                                                                                      InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations kill External Process",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(startCommandSnippet) });

        return (Integer) ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized String getEnvironmentVariable( String sessionId, int resourceId, String variableName )
                                                                                                                      throws NoSuchActionException,
                                                                                                                      NoCompatibleMethodFoundException,
                                                                                                                      NoSuchComponentException,
                                                                                                                      ClassNotFoundException,
                                                                                                                      InstantiationException,
                                                                                                                      IllegalAccessException,
                                                                                                                      IllegalArgumentException,
                                                                                                                      InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations get Env Variable",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(variableName) });

        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public static synchronized void setEnvironmentVariable( String sessionId, int resourceId, String variableName,
                                                            String variableValue )
                                                                                   throws NoSuchActionException,
                                                                                   NoCompatibleMethodFoundException,
                                                                                   NoSuchComponentException,
                                                                                   ClassNotFoundException,
                                                                                   InstantiationException,
                                                                                   IllegalAccessException,
                                                                                   IllegalArgumentException,
                                                                                   InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations set Env Variable",
                                         new String[]{ String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(variableName),
                                                       GSON.toJson(variableValue) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized void appendToEnvironmentVariable( String sessionId, int resourceId, String variableName,
                                                                 String variableValue )
                                                                                        throws NoSuchActionException,
                                                                                        NoCompatibleMethodFoundException,
                                                                                        NoSuchComponentException,
                                                                                        ClassNotFoundException,
                                                                                        InstantiationException,
                                                                                        IllegalAccessException,
                                                                                        IllegalArgumentException,
                                                                                        InvocationTargetException {

        ActionPojo pojo = new ActionPojo(sessionId, resourceId, COMPONENT_NAME,
                                         "Internal Process Operations append To Env Variable",
                                         new String[]{ String.class.getName(),
                                                       String.class.getName() },
                                         new String[]{ GSON.toJson(variableName),
                                                       GSON.toJson(variableValue) });

        ResourcesManager.executeOverResource(pojo);

    }

}
