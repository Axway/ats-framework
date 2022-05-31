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
package com.axway.ats.agent.core;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.exceptions.InternalComponentException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.model.EnvironmentCleanupHandler;

/**
 * The entry point for environment restore. This class can be used to restore
 * the environment of one or all registered components.
 */
public class EnvironmentHandler {

    private static final Logger                        log = Logger.getLogger(EnvironmentHandler.class);

    private static EnvironmentHandler                  instance;

    private HashMap<String, EnvironmentCleanupHandler> cleanupClassInstances;

    /**
     * Private constructor to prevent instantiation
     */
    private EnvironmentHandler() {

        cleanupClassInstances = new HashMap<String, EnvironmentCleanupHandler>();
    }

    public static synchronized EnvironmentHandler getInstance() {

        if (instance == null) {
            instance = new EnvironmentHandler();
        }

        return instance;
    }

    /**
     * Restore the environment for all components - this will restore the environment
     * with given name, described in the Agent descriptor file and also call the EnvironmentCleanupHandler.clean()
     * method for the component.
     * Keep in mind that all the components must have environment configuration with this name
     *
     * @param environmentName               The name of the environment configuration
     * @throws ActionExecutionException     If the EnvironmentCleanupHandler implementation cannot be found or accessed
     * @throws InternalComponentException   If the EnvironmentCleanupHandler.clean() implementation throws an exception
     * @throws AgentException                 If an exception occurs during restore of environment described in Agent descriptor
     */
    public void restoreAll(
                            String environmentName ) throws ActionExecutionException,
                                                     InternalComponentException, AgentException {

        //restore environment for all components
        for (Component component : ComponentRepository.getInstance().getAllComponents()) {
            String currentComponentName = component.getComponentName();
            restore(currentComponentName, environmentName, null);
        }
    }

    /**
     * Restore the environment for a given component - this will restore the environment
     * described in the Agent descriptor file and also call the EnvironmentCleanupHandler.clean()
     * method for the component
     *
     * @param componentName                 The name of the component
     * @param environmentName               The name of the environment configuration
     * @param folderPath                    The path of the backup folder. Used to execute restore from a backup folder
     *                                      other than the defined in the environment, that's why it's optional
     * @throws ActionExecutionException     If the EnvironmentCleanupHandler implementation cannot be found or accessed
     * @throws NoSuchComponentException     If there is no component with this name
     * @throws InternalComponentException   If the EnvironmentCleanupHandler.clean() implementation throws an exception
     * @throws AgentException                 If an exception occurs during restore of environment described in Agent descriptor
     */
    public void restore(
                         String componentName,
                         String environmentName,
                         String folderPath ) throws ActionExecutionException, NoSuchComponentException,
                                             InternalComponentException, AgentException {

        Component component = ComponentRepository.getInstance().getComponent(componentName);

        log.debug("Executing environment restore for component '" + componentName + "'"
                  + (environmentName == null
                                             ? ""
                                             : " using environment '" + environmentName + "'")
                  + (folderPath == null
                                        ? ""
                                        : " from folder '" + folderPath + "'"));

        //first restore the environment associated with the current component
        ComponentEnvironment componentEnvironment;
        if (environmentName == null) {
            componentEnvironment = ComponentRepository.getInstance().getComponentEnvironment(componentName);
        } else {
            componentEnvironment = ComponentRepository.getInstance()
                                                      .getComponentEnvironment(componentName,
                                                                               environmentName);
        }
        //check if there is environment configuration
        if (componentEnvironment != null) {

            componentEnvironment.restore(folderPath);
        }

        // FIXME: What about if environmentName != null, do we have to call cleanupHandler.clean()
        // or we have to add a new method in EnvironmentCleanupHandler, eg. clean( environmentName )

        //restore environment for the specified component
        EnvironmentCleanupHandler cleanupHandler = getEnvironmentCleanupHandlerInstance(component);
        if (cleanupHandler != null) {
            log.debug("Executing cleanup for component '" + componentName + "'");
            cleanupHandler.clean();
        } else {
            log.debug("No cleanup hanlder defined for component '" + componentName + "'");
        }
    }

    /**
     * Backup the environment of all registered components with a specific environment configuration
     *
     * @param environmentName the name of the environment configuration
     * @throws ActionExecutionException
     * @throws NoSuchComponentException
     * @throws InternalComponentException
     * @throws AgentException
     */
    public void backupAll(
                           String environmentName ) throws ActionExecutionException,
                                                    NoSuchComponentException, InternalComponentException,
                                                    AgentException {

        //backup environment for all components
        for (Component component : ComponentRepository.getInstance().getAllComponents()) {
            String currentComponentName = component.getComponentName();
            backup(currentComponentName, environmentName, null);
        }
    }

    /**
     * Backup the environment for the component with the given name and specific environment configuration
     *
     * @param componentName                 The name of the component
     * @param environmentName               The name of the environment configuration
     * @param folderPath                    The path of the backup folder. Used to execute backup to folder
     *                                      other than the defined in the environment, that's why it's optional
     * @throws ActionExecutionException
     * @throws NoSuchComponentException
     * @throws InternalComponentException
     * @throws AgentException
     */
    public void backup(
                        String componentName,
                        String environmentName,
                        String folderPath ) throws ActionExecutionException, NoSuchComponentException,
                                            InternalComponentException, AgentException {

        log.debug("Executing environment backup for component '" + componentName + "'"
                  + (environmentName == null
                                             ? ""
                                             : " using environment '" + environmentName + "'")
                  + (folderPath == null
                                        ? ""
                                        : " from folder '" + folderPath + "'"));

        //backup environment for the specified component
        ComponentEnvironment componentEnvironment = null;
        if (environmentName == null) {
            componentEnvironment = ComponentRepository.getInstance().getComponentEnvironment(componentName);
        } else {
            componentEnvironment = ComponentRepository.getInstance()
                                                      .getComponentEnvironment(componentName,
                                                                               environmentName);
        }
        //check if there is environment configuration
        if (componentEnvironment != null) {

            componentEnvironment.backup(folderPath);
        }
    }

    /**
     * Get the instance of the cleanup class for the given component. If such does not exist, create it.
     *
     * @param className     the FQDN of the cleanup class
     * @return              the instance of the cleanup class
     *
     * @throws ActionExecutionException     if the class instance cannot be created
     */
    private EnvironmentCleanupHandler getEnvironmentCleanupHandlerInstance(
                                                                            Component component )
                                                                                                  throws ActionExecutionException {

        String componentName = component.getComponentName();
        Class<? extends EnvironmentCleanupHandler> cleanupHandler = component.getActionMap()
                                                                             .getCleanupHandler();

        //no cleanup handler defined for the component
        if (cleanupHandler == null) {
            return null;
        }

        try {

            EnvironmentCleanupHandler cleanupClassInstance = cleanupClassInstances.get(componentName);
            if (cleanupClassInstance == null) {
                cleanupClassInstance = cleanupHandler.newInstance();
                cleanupClassInstances.put(componentName, cleanupClassInstance);
            }

            return cleanupClassInstance;
        } catch (IllegalAccessException iae) {
            throw new ActionExecutionException("Could not access cleanup class " + cleanupHandler.getName(),
                                               iae);
        } catch (InstantiationException ie) {
            throw new ActionExecutionException("Could not instantiate cleanup class "
                                               + cleanupHandler.getName(), ie);
        }
    }
}
