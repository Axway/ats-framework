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
package com.axway.ats.agent.webapp.restservice.api.processes.talker;

import java.lang.reflect.InvocationTargetException;

import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.ResourcesManager;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.google.gson.Gson;

public class ProcessesTalkersManager {

    private static final int    UNINITIALIZED_RESOURCE_ID = -1;
    private static final String COMPONENT_NAME            = "auto-system-operations";

    private static final Gson   GSON                      = new Gson();

    public static synchronized long
            initializeProcessTalker( String callerId,
                                     String command ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                      NoSuchComponentException, ClassNotFoundException,
                                                      InstantiationException, IllegalAccessException,
                                                      IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, UNINITIALIZED_RESOURCE_ID, COMPONENT_NAME,
                                         "InternalProcessTalker init Process Talker",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(command) });

        // create only the action class instance first
        long resourceId = ResourcesManager.initializeResource(pojo);
        pojo.setResourceId(resourceId);

        // then execute the action method
        ResourcesManager.executeOverResource(pojo);

        return resourceId;
    }

    public static synchronized void
            setDefaultOperationTimeout( String callerId, long resourceId,
                                        int timeout ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                      NoSuchComponentException, ClassNotFoundException,
                                                      InstantiationException, IllegalAccessException,
                                                      IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker set Default Operation Timeout",
                                         new String[]{ int.class.getName() },
                                         new String[]{ GSON.toJson(timeout) });

        // then execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized void
            setCommand( String callerId, long resourceId,
                        String command ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                         NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                         IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker set Command",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(command) });

        // then execute the action method
        ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized String
            getPendingToMatchContent( String callerId,
                                      long resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                       NoSuchComponentException, ClassNotFoundException,
                                                       InstantiationException, IllegalAccessException,
                                                       IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker get Pending To Match Content",
                                         null,
                                         null);

        // then execute the action method
        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public static synchronized String
            getCurrentStdout( String callerId,
                              long resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                               NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                               IllegalAccessException, IllegalArgumentException,
                                               InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker get Current Standard Out Contents",
                                         null,
                                         null);

        // then execute the action method
        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public static synchronized String
            getCurrentStderr( String callerId,
                              long resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                               NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                               IllegalAccessException, IllegalArgumentException,
                                               InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker get Current Standard Err Contents",
                                         null,
                                         null);

        // then execute the action method
        return (String) ResourcesManager.executeOverResource(pojo);
    }

    public static synchronized void
            expectErr( String callerId, long resourceId, String pattern,
                       int timeoutSeconds ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                            NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                            IllegalAccessException, IllegalArgumentException,
                                            InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect Err",
                                         new String[]{ String.class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(pattern), GSON.toJson(timeoutSeconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void expectErrByRegex( String callerId, long resourceId, String pattern,
                                         int timeoutSeconds ) throws NoSuchActionException,
                                                              NoCompatibleMethodFoundException,
                                                              NoSuchComponentException,
                                                              ClassNotFoundException, InstantiationException,
                                                              IllegalAccessException, IllegalArgumentException,
                                                              InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker expect Err By Regex",
                                         new String[]{ String.class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(pattern), GSON.toJson(timeoutSeconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void expectErrAll( String callerId, long resourceId, String[] patterns,
                                     int timeoutSeconds ) throws NoSuchActionException,
                                                          NoCompatibleMethodFoundException,
                                                          NoSuchComponentException, ClassNotFoundException,
                                                          InstantiationException, IllegalAccessException,
                                                          IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect Err All",
                                         new String[]{ String[].class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(patterns), GSON.toJson(timeoutSeconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void expectErrAllByRegex( String callerId, long resourceId, String[] patterns,
                                            int timeoutSeconds ) throws NoSuchActionException,
                                                                 NoCompatibleMethodFoundException,
                                                                 NoSuchComponentException, ClassNotFoundException,
                                                                 InstantiationException, IllegalAccessException,
                                                                 IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker expect Err All By Regex",
                                         new String[]{ String[].class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(patterns), GSON.toJson(timeoutSeconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static int expectErrAny( String callerId, long resourceId, String[] patterns,
                                    int timeoutSeconds ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                         NoSuchComponentException, ClassNotFoundException,
                                                         InstantiationException, IllegalAccessException,
                                                         IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect Err Any",
                                         new String[]{ String[].class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(patterns), GSON.toJson(timeoutSeconds) });

        return (int) ResourcesManager.executeOverResource(pojo);

    }

    public static int expectErrAnyByRegex( String callerId, long resourceId, String[] patterns,
                                           int timeoutSeconds ) throws NoSuchActionException,
                                                                NoCompatibleMethodFoundException,
                                                                NoSuchComponentException,
                                                                ClassNotFoundException, InstantiationException,
                                                                IllegalAccessException, IllegalArgumentException,
                                                                InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker expect Err Any By Regex",
                                         new String[]{ String[].class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(patterns), GSON.toJson(timeoutSeconds) });

        return (int) ResourcesManager.executeOverResource(pojo);
    }

    public static synchronized void
            expect( String callerId, long resourceId, String pattern,
                    int timeoutSeconds ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                         NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                         IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect",
                                         new String[]{ String.class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(pattern), GSON.toJson(timeoutSeconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static synchronized void
            expect( String callerId, long resourceId,
                    String pattern ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                     NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                     IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(pattern) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void expectByRegex( String callerId, long resourceId, String pattern,
                                      int timeoutSeconds ) throws NoSuchActionException,
                                                           NoCompatibleMethodFoundException, NoSuchComponentException,
                                                           ClassNotFoundException, InstantiationException,
                                                           IllegalAccessException, IllegalArgumentException,
                                                           InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect By Regex",
                                         new String[]{ String.class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(pattern), GSON.toJson(timeoutSeconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void expectByRegex( String callerId, long resourceId,
                                      String pattern ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                       NoSuchComponentException, ClassNotFoundException,
                                                       InstantiationException, IllegalAccessException,
                                                       IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect By Regex",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(pattern) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void expectAll( String callerId, long resourceId,
                                  String[] patterns ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                      NoSuchComponentException, ClassNotFoundException,
                                                      InstantiationException, IllegalAccessException,
                                                      IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect All",
                                         new String[]{ String[].class.getName() },
                                         new String[]{ GSON.toJson(patterns) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void expectAll( String callerId, long resourceId, String[] patterns,
                                  int timeoutSeconds ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                       NoSuchComponentException, ClassNotFoundException,
                                                       InstantiationException, IllegalAccessException,
                                                       IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect All",
                                         new String[]{ String[].class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(patterns), GSON.toJson(timeoutSeconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void expectAllByRegex( String callerId, long resourceId,
                                         String[] patterns ) throws NoSuchActionException,
                                                             NoCompatibleMethodFoundException, NoSuchComponentException,
                                                             ClassNotFoundException, InstantiationException,
                                                             IllegalAccessException, IllegalArgumentException,
                                                             InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker expect All By Regex",
                                         new String[]{ String[].class.getName() },
                                         new String[]{ GSON.toJson(patterns) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void expectAllByRegex( String callerId, long resourceId, String[] patterns,
                                         int timeoutSeconds ) throws NoSuchActionException,
                                                              NoCompatibleMethodFoundException,
                                                              NoSuchComponentException, ClassNotFoundException,
                                                              InstantiationException, IllegalAccessException,
                                                              IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker expect All By Regex",
                                         new String[]{ String[].class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(patterns), GSON.toJson(timeoutSeconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static int expectAny( String callerId, long resourceId, String[] patterns,
                                 int timeoutSeconds ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                      NoSuchComponentException, ClassNotFoundException,
                                                      InstantiationException, IllegalAccessException,
                                                      IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect Any",
                                         new String[]{ String[].class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(patterns), GSON.toJson(timeoutSeconds) });

        return (int) ResourcesManager.executeOverResource(pojo);

    }

    public static int expectAny( String callerId, long resourceId,
                                 String[] patterns ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                     NoSuchComponentException, ClassNotFoundException,
                                                     InstantiationException, IllegalAccessException,
                                                     IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME, "InternalProcessTalker expect Any",
                                         new String[]{ String[].class.getName() },
                                         new String[]{ GSON.toJson(patterns) });

        return (int) ResourcesManager.executeOverResource(pojo);

    }

    public static int expectAnyByRegex( String callerId, long resourceId,
                                        String[] patterns ) throws NoSuchActionException,
                                                            NoCompatibleMethodFoundException, NoSuchComponentException,
                                                            ClassNotFoundException, InstantiationException,
                                                            IllegalAccessException, IllegalArgumentException,
                                                            InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker expect Any By Regex",
                                         new String[]{ String[].class.getName() },
                                         new String[]{ GSON.toJson(patterns) });

        return (int) ResourcesManager.executeOverResource(pojo);
    }

    public static int expectAnyByRegex( String callerId, long resourceId, String[] patterns,
                                        int timeoutSeconds ) throws NoSuchActionException,
                                                             NoCompatibleMethodFoundException, NoSuchComponentException,
                                                             ClassNotFoundException, InstantiationException,
                                                             IllegalAccessException, IllegalArgumentException,
                                                             InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker expect Any By Regex",
                                         new String[]{ String[].class.getName(), int.class.getName() },
                                         new String[]{ GSON.toJson(patterns), GSON.toJson(timeoutSeconds) });

        return (int) ResourcesManager.executeOverResource(pojo);
    }

    public static void expectClose( String callerId,
                                    long resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                     NoSuchComponentException, ClassNotFoundException,
                                                     InstantiationException, IllegalAccessException,
                                                     IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker expect Close",
                                         null,
                                         null);

        ResourcesManager.executeOverResource(pojo);
    }

    public static void expectClose( String callerId, long resourceId,
                                    int timeoutSeconds ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                         NoSuchComponentException, ClassNotFoundException,
                                                         InstantiationException, IllegalAccessException,
                                                         IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker expect Close",
                                         new String[]{ int.class.getName() },
                                         new String[]{ GSON.toJson(timeoutSeconds) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static boolean isClosed( String callerId,
                                    long resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                     NoSuchComponentException, ClassNotFoundException,
                                                     InstantiationException, IllegalAccessException,
                                                     IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker is Closed",
                                         null,
                                         null);

        return (boolean) ResourcesManager.executeOverResource(pojo);
    }

    public static int getExitValue( String callerId,
                                    long resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                     NoSuchComponentException, ClassNotFoundException,
                                                     InstantiationException, IllegalAccessException,
                                                     IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker get Exit Value",
                                         null,
                                         null);

        return (int) ResourcesManager.executeOverResource(pojo);
    }

    public static void kill( String callerId, long resourceId ) throws NoSuchActionException,
                                                                NoCompatibleMethodFoundException,
                                                                NoSuchComponentException, ClassNotFoundException,
                                                                InstantiationException, IllegalAccessException,
                                                                IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker kill External Process",
                                         null,
                                         null);

        ResourcesManager.executeOverResource(pojo);

    }

    public static void killWithChildren( String callerId, long resourceId ) throws NoSuchActionException,
                                                                            NoCompatibleMethodFoundException,
                                                                            NoSuchComponentException,
                                                                            ClassNotFoundException,
                                                                            InstantiationException,
                                                                            IllegalAccessException,
                                                                            IllegalArgumentException,
                                                                            InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker kill External Process With Children",
                                         null,
                                         null);

        ResourcesManager.executeOverResource(pojo);

    }

    public static void send( String callerId, long resourceId,
                             String text ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                           NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                           IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker send",
                                         new String[]{ String.class.getName() },
                                         new String[]{ GSON.toJson(text) });

        ResourcesManager.executeOverResource(pojo);

    }

    public static void
            sendEnter( String callerId,
                       long resourceId ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                        NoSuchComponentException, ClassNotFoundException, InstantiationException,
                                        IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker send Enter Key",
                                         null,
                                         null);

        ResourcesManager.executeOverResource(pojo);

    }

    public static void
            sendEnterInLoop( String callerId, long resourceId, String intermediatePattern, String finalPattern,
                             int maxLoopTimes ) throws NoSuchActionException, NoCompatibleMethodFoundException,
                                                NoSuchComponentException, ClassNotFoundException,
                                                InstantiationException,
                                                IllegalAccessException, IllegalArgumentException,
                                                InvocationTargetException {

        ActionPojo pojo = new ActionPojo(callerId, resourceId, COMPONENT_NAME,
                                         "InternalProcessTalker send Enter Key In Loop",
                                         new String[]{ String.class.getName(), String.class.getName(),
                                                       int.class.getName() },
                                         new String[]{ GSON.toJson(intermediatePattern), GSON.toJson(finalPattern),
                                                       GSON.toJson(maxLoopTimes) });

        ResourcesManager.executeOverResource(pojo);

    }

}
