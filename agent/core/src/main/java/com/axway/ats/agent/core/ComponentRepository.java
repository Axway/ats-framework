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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.exceptions.ComponentAlreadyDefinedException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.exceptions.NoSuchEnvironmentException;
import com.axway.ats.agent.core.model.FinalizationHandler;
import com.axway.ats.agent.core.model.InitializationHandler;

public class ComponentRepository {

    private static final Logger                 log               = LogManager.getLogger(ComponentRepository.class);

    private static final ComponentRepository    actionMapInstance = new ComponentRepository();

    // < caller, < component name, component > >
    private Map<String, Map<String, Component>> componentsPerCaller;

    public static final String                  DEFAULT_CALLER    = "127.0.0.1";

    private ComponentRepository() {

        componentsPerCaller = new HashMap<String, Map<String, Component>>();
        componentsPerCaller.put(DEFAULT_CALLER, new HashMap<String, Component>());
    }

    public static ComponentRepository getInstance() {

        return actionMapInstance;
    }

    /**
     * Register a component in the global map of components
     *
     * @param component                             the component to add
     * @throws ComponentAlreadyDefinedException     if the component has already been registered
     */
    public void putComponent(
                              Component component ) throws ComponentAlreadyDefinedException {

        String componentName = component.getComponentName();

        Map<String, Component> components = componentsPerCaller.get(DEFAULT_CALLER);

        //first check if we have such component defined
        if (components.containsKey(componentName)) {
            throw new ComponentAlreadyDefinedException(componentName);
        }

        components.put(componentName, component);
    }

    /**
     * Get the component instance for the given name
     *
     * @param componentName                 the name of the component
     * @return                              the Component instance associated with this name
     * @throws NoSuchComponentException     if there is no such component
     */
    public Component getComponent(
                                   String componentName ) throws NoSuchComponentException {

        return getComponentPerCaller(DEFAULT_CALLER, componentName);
    }

    /**
     * Helper method for getting the action map associated with a given component for the default caller
     *
     * @param componentName                 the name of the component
     * @return                              the action map
     * @throws NoSuchComponentException     if there is no such component
     */
    public ComponentActionMap getComponentActionMap(
                                                     String componentName ) throws NoSuchComponentException {

        return getComponentActionMap(DEFAULT_CALLER, componentName);
    }

    /**
     * Helper method for getting the action map associated with a given component for a give caller
     *
     * @param caller                        the remote caller
     * @param componentName                 the name of the component
     * @return                              the action map
     * @throws NoSuchComponentException     if there is no such component
     */
    public ComponentActionMap getComponentActionMap(
                                                     String caller,
                                                     String componentName ) throws NoSuchComponentException {

        return getComponentPerCaller(caller, componentName).getActionMap();
    }

    /**
     * Helper method for getting the environment associated with a given component
     *
     * @param componentName                 the name of the component
     * @return                              the component environment
     * @throws NoSuchComponentException     if there is no such component
     */
    public ComponentEnvironment getComponentEnvironment(
                                                         String componentName )
                                                                                throws NoSuchComponentException {

        Component component = getComponentPerCaller(DEFAULT_CALLER, componentName);

        if (component.getEnvironments() == null || component.getEnvironments().isEmpty()) {
            return null;
        }

        return component.getEnvironments().get(0);
    }

    /**
     * Helper method for getting the environment associated with a given component
     *
     * @param componentName                 the name of the component
     * @param environmentName               the name of environment configuration
     * @return                              the component environment
     * @throws NoSuchComponentException     if there is no such component
     * @throws NoSuchEnvironmentException   if there is no such environment for this component
     */
    public ComponentEnvironment getComponentEnvironment(
                                                         String componentName,
                                                         String environmentName )
                                                                                  throws NoSuchComponentException,
                                                                                  NoSuchEnvironmentException {

        List<ComponentEnvironment> environments = getComponentPerCaller(DEFAULT_CALLER,
                                                                        componentName).getEnvironments();

        if (environments != null) {
            for (ComponentEnvironment env : environments) {
                if (env.getEnvironmentName().equals(environmentName)) {
                    return env;
                }
            }
        }

        throw new NoSuchEnvironmentException(componentName, environmentName);
    }

    /**
     * Return a list with all registered components
     *
     * @return  List of components
     */
    public List<Component> getAllComponents() {

        return new ArrayList<Component>(componentsPerCaller.get(DEFAULT_CALLER).values());
    }

    public void initializeAllComponents() {

        //initialize the components
        List<Component> components = getAllComponents();
        for (Component component : components) {
            ComponentActionMap actionMap = component.getActionMap();
            Class<? extends InitializationHandler> initClass = actionMap.getInitializationHandler();

            //skip the initialization phase if the component has not declared a handler
            if (initClass != null) {
                String initClassName = initClass.getName();

                try {
                    InitializationHandler initHandler = initClass.newInstance();
                    initHandler.initializeComponent();

                    log.info("Component '" + actionMap.getComponentName() + "' initialized successfully");
                } catch (IllegalAccessException iae) {
                    log.error("Could not instantiate initialization handler '" + initClassName
                              + "' - it should have a no-argument public constructor");
                } catch (InstantiationException ie) {
                    log.error("Could not instantiate initialization handler '" + initClassName
                              + "' - it should have a no-argument public constructor");
                } catch (Exception e) {
                    log.error("Exception during initialization for component '"
                              + actionMap.getComponentName() + "'",
                              e);
                }
            } else {
                log.debug("Component '" + actionMap.getComponentName()
                          + "' does not have an initialization handler");
            }

            //create the component backup if it does not exist yet
            try {
                List<ComponentEnvironment> componentEnvironments = component.getEnvironments();
                if (componentEnvironments != null) {
                    for (ComponentEnvironment componentEnvironment : componentEnvironments) {

                        componentEnvironment.backupOnlyIfNotAlreadyDone();
                    }
                }
            } catch (AgentException ae) {
                log.error(ae.getMessage(), ae);
            }
        }
    }

    public void finalizeAllComponents() {

        //finalize each component
        List<Component> components = getAllComponents();
        for (Component component : components) {
            ComponentActionMap actionMap = component.getActionMap();
            if (actionMap == null) {
                log.warn("Action map for component '" + component.getComponentName() + "' is null");
            }
            Class<? extends FinalizationHandler> finalizationClass = actionMap.getFinalizationHandler();

            //skip the finalization phase if the component has not declared a handler
            if (finalizationClass == null) {
                log.debug("Component '" + actionMap.getComponentName()
                          + "' does not have a finalization handler");
                continue;
            }

            String finalizationClassName = finalizationClass.getName();

            try {
                FinalizationHandler finalizationHandler = finalizationClass.newInstance();
                // TODO Add some maximum finalization processing time and log error with interrupt otherwise
                finalizationHandler.finalizeComponent();

                log.info("Component '" + actionMap.getComponentName() + "' finalized successfully");
            } catch (IllegalAccessException iae) {
                log.error("Could not instantiate finalization handler '" + finalizationClassName
                          + "' - it should have a no-argument public constructor");
            } catch (InstantiationException ie) {
                log.error("Could not instantiate finalization handler '" + finalizationClassName
                          + "' - it should have a no-argument public constructor");
            } catch (Exception e) {
                log.error("Exception during initialization for component '" + actionMap.getComponentName()
                          + "'", e);
            }
        }
    }

    /**
     * Clear the global component map and destroy all component references
     */
    public void clear() {

        for (Entry<String, Map<String, Component>> componentEntry : componentsPerCaller.entrySet()) {

            Map<String, Component> components = componentEntry.getValue();

            //clear the action maps
            for (Component component : components.values()) {
                if (component.getActionMap() != null) {
                    component.getActionMap().clear();
                }
            }

            components.clear();
        }
    }

    private Component getComponentPerCaller(
                                             String caller,
                                             String componentName ) throws NoSuchComponentException {

        if (!componentsPerCaller.get(DEFAULT_CALLER).containsKey(componentName)) {
            // we do not know about component with such name
            throw new NoSuchComponentException(componentName);
        }

        // find components for this caller
        Map<String, Component> components = componentsPerCaller.get(caller);
        if (components == null) {

            synchronized (componentsPerCaller) {

                components = componentsPerCaller.get(caller);
                if (components == null) {
                    components = new HashMap<String, Component>();
                    componentsPerCaller.put(caller, components);
                }
            }
        }

        // find component by its name
        Component component = components.get(componentName);
        if (component == null) {

            // Here we need to sync on the 'atsAgent' String, which is not good solution at all,
            // because there can be another sync on the same string in the JVM (possible deadlocks).
            // We will try to prevent that adding a 'unique' prefix
            synchronized ( ("ATS_STRING_LOCK-" + componentName).intern()) {

                component = components.get(componentName);
                if (component == null) {

                    // this caller needs a fresh component copy
                    log.info("Create a new instance of '" + componentName + "' component for calls from "
                             + caller);
                    Component defaultComponent = componentsPerCaller.get(DEFAULT_CALLER)
                                                                    .get(componentName);
                    component = defaultComponent.getNewCopy();
                    components.put(componentName, component);
                }
            }
        }

        return component;
    }
}
