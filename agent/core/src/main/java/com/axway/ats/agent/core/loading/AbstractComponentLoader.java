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
package com.axway.ats.agent.core.loading;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.Component;
import com.axway.ats.agent.core.ComponentActionMap;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.ConfigurationParser;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.exceptions.ComponentAlreadyDefinedException;
import com.axway.ats.agent.core.model.EnvironmentCleanupHandler;
import com.axway.ats.agent.core.model.FinalizationHandler;
import com.axway.ats.agent.core.model.InitializationHandler;

/**
 * Base class for all component loaders
 */
public abstract class AbstractComponentLoader implements ComponentLoader {

    protected final Logger log;

    /**
     * Mutex used for synchronization when loading - client code
     * should try to acquire the monitor of this mutex in order to
     * guarantee that no action is executed during loading of components
     */
    private final Object   loadingMutex;

    /**
     * Constructor
     *
     * @param loadingMutex the loading mutex - it will be locked
     * during component loading
     */
    public AbstractComponentLoader( Object loadingMutex ) {

        this.log = LogManager.getLogger(this.getClass());
        this.loadingMutex = loadingMutex;
    }

    @Override
    public final void loadAvailableComponents( ComponentRepository repository ) throws AgentException {

        //synchronize on the mutex - this mutex is used for
        //delaying the execution of Agent actions until all components have been loaded
        synchronized (loadingMutex) {

            //finalize all components prior to unloading
            repository.finalizeAllComponents();

            log.info("Loading available components");
            loadComponents(repository);
            log.info("Component loading complete");

            repository.initializeAllComponents();
        }
    }

    /**
     * Abstract method for loading the components - to be implemented
     * by each specific loader. This method should register the the components
     * into the specified repository
     *
     * @param repository the repository to load components into
     * @throws AgentException on error
     */
    protected abstract void loadComponents( ComponentRepository repository ) throws AgentException;

    /**
     * Abstract method for loading a class
     *
     * @param className name of the class
     * @return the loaded class
     * @throws ClassNotFoundException if the class cannot be found
     */
    protected abstract Class<?> loadClass( String className ) throws ClassNotFoundException;

    /**
     * Register a component into a component repository
     *
     * @param configParser the configuration parser which describes the component
     * @param componentRepository the component repository
     * @throws ComponentAlreadyDefinedException if the component has already been defined
     * @throws ComponentLoadingException if the component cannot be loaded for some reason
     */
    @SuppressWarnings( "unchecked")
    protected void registerComponent( ConfigurationParser configParser,
                                      ComponentRepository componentRepository ) throws ComponentAlreadyDefinedException,
                                                                                ComponentLoadingException {

        String componentName = configParser.getComponentName();

        //first add the action classes to the global map
        ComponentActionMap componentActionMap = new ComponentActionMap(componentName);

        try {

            //initialization and finalization handlers are optional
            if (configParser.getInitializationHandler() == null) {
                componentActionMap.setInitializationHandler(null);
            } else {
                Class<?> initHandlerClass = loadClass(configParser.getInitializationHandler());

                //check if this class implements the proper interface
                if (InitializationHandler.class.isAssignableFrom(initHandlerClass)) {
                    componentActionMap.setInitializationHandler((Class<? extends InitializationHandler>) initHandlerClass);
                } else {
                    throw new ComponentLoadingException(componentName,
                                                        "Initialization handler '"
                                                                       + initHandlerClass.getName()
                                                                       + "' does not implement the InitializationHandler interface");
                }
            }

            if (configParser.getFinalizationHandler() == null) {
                componentActionMap.setFinalizationHandler(null);
            } else {
                Class<?> finalHandlerClass = loadClass(configParser.getFinalizationHandler());

                //check if this class implements the proper interface
                if (FinalizationHandler.class.isAssignableFrom(finalHandlerClass)) {
                    componentActionMap.setFinalizationHandler((Class<? extends FinalizationHandler>) finalHandlerClass);
                } else {
                    //fatal error
                    throw new ComponentLoadingException(componentName,
                                                        "Finalization handler '"
                                                                       + finalHandlerClass.getName()
                                                                       + "' does not implement the FinalizationHandler interface");
                }
            }

            Class<?> cleanupHandlerClass = loadClass(configParser.getCleanupHandler());

            //check if this class implements the proper interface
            if (EnvironmentCleanupHandler.class.isAssignableFrom(cleanupHandlerClass)) {
                componentActionMap.setCleanupHandler((Class<? extends EnvironmentCleanupHandler>) cleanupHandlerClass);
            } else {
                //fatal error
                throw new ComponentLoadingException(componentName,
                                                    "Cleanup handler '" + cleanupHandlerClass.getName()
                                                                   + "' does not implement the EnvironmentCleanupHandler interface");
            }

        } catch (ClassNotFoundException cnfe) {
            //fatal error
            throw new ComponentLoadingException(componentName,
                                                "Component class or a referred class could not be loaded, check configuration for component '"
                                                               + componentName + "': " + cnfe.getMessage());
        }

        Set<String> actionClassNames = configParser.getActionClassNames();
        for (String actionClassName : actionClassNames) {
            try {
                componentActionMap.registerActionClass(loadClass(actionClassName));
            } catch (ClassNotFoundException | NoClassDefFoundError cnfe) {
                //this is a non-fatal error so we can just log it
                log.error("Action class or a referred class could not be loaded, check configuration for component '"
                          + componentName + "': " + cnfe.getMessage());
            }
        }

        //create the component object
        Component component = new Component(componentName);
        component.setActionMap(componentActionMap);
        component.setEnvironments(configParser.getEnvironments());

        componentRepository.putComponent(component);
    }
}
