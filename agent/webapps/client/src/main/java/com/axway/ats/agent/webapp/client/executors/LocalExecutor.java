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
package com.axway.ats.agent.webapp.client.executors;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.agent.core.ActionHandler;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.EnvironmentHandler;
import com.axway.ats.agent.core.MainComponentLoader;
import com.axway.ats.agent.core.MultiThreadedActionHandler;
import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.configuration.DefaultLocalConfigurator;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.appenders.ActiveDbAppender;

/**
 * This class is responsible for executing an action in the current JVM
 */
public class LocalExecutor extends AbstractClientExecutor {

    //hold the state of the local agent instance
    private static boolean isLocalMachineConfigured = false;

    public LocalExecutor() throws AgentException {

        if (!isLocalMachineConfigured) {

            DefaultLocalConfigurator localConfigurator = new DefaultLocalConfigurator();
            List<Configurator> configurators = new ArrayList<Configurator>();
            configurators.add(localConfigurator);

            MainComponentLoader.getInstance().initialize(configurators);

            isLocalMachineConfigured = true;
        }
    }

    @Override
    public int initializeAction( ActionRequest actionRequest ) throws AgentException {

        /*
         * This method is left empty, due to its usage only when we are working with ATS agent and not locally
         * */
        return -1;

    }

    @Override
    public void deinitializeAction( int actionId ) throws AgentException {

        /*
         * This method is left empty, due to its usage only when we are working with ATS agent and not locally
         * */

    }

    @Override
    public Object executeAction( ActionRequest actionRequest ) throws AgentException {

        String actionName = actionRequest.getActionName();
        String componentName = actionRequest.getComponentName();
        Object[] arguments = actionRequest.getArguments();

        log.info("Start executing action '" + actionName + "' with arguments "
                 + StringUtils.methodInputArgumentsToString(arguments));

        //FIXME: swap with ActionRequest
        Object result = ActionHandler.executeAction(ComponentRepository.DEFAULT_CALLER,
                                                    componentName, actionName, arguments);
        log.info("Successfully executed action '" + actionName + "'");

        return result;
    }

    @Override
    public boolean isComponentLoaded( ActionRequest actionRequest ) throws AgentException {

        return ActionHandler.isComponentLoaded(ComponentRepository.DEFAULT_CALLER,
                                               actionRequest.getComponentName());
    }

    @Override
    public String getAgentHome() throws AgentException {

        return System.getProperty(AtsSystemProperties.AGENT_HOME_FOLDER);
    }

    @Override
    public int getNumberPendingLogEvents() throws AgentException {

        ActiveDbAppender appender = ActiveDbAppender.getCurrentInstance();

        if (appender != null) {
            return appender.getNumberPendingLogEvents();
        }

        return -1;
    }

    @Override
    public void restore( String componentName, String environmentName,
                         String folderPath ) throws AgentException {

        EnvironmentHandler.getInstance().restore(componentName, environmentName, folderPath);
    }

    @Override
    public void restoreAll( String environmentName ) throws AgentException {

        EnvironmentHandler.getInstance().restoreAll(environmentName);
    }

    @Override
    public void backup( String componentName, String environmentName,
                        String folderPath ) throws AgentException {

        EnvironmentHandler.getInstance().backup(componentName, environmentName, folderPath);
    }

    @Override
    public void backupAll( String environmentName ) throws AgentException {

        EnvironmentHandler.getInstance().backupAll(environmentName);
    }

    @Override
    public final void waitUntilQueueFinish() throws AgentException {

        MultiThreadedActionHandler.getInstance(ThreadsPerCaller.getCaller()).waitUntilAllQueuesFinish();
    }

}
