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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.action.ActionMethod;
import com.axway.ats.agent.core.action.ActionMethodContainer;
import com.axway.ats.agent.core.action.TemplateActionMethod;
import com.axway.ats.agent.core.exceptions.ActionAlreadyDefinedException;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.EnvironmentCleanupHandler;
import com.axway.ats.agent.core.model.FinalizationHandler;
import com.axway.ats.agent.core.model.InitializationHandler;
import com.axway.ats.agent.core.model.TemplateAction;
import com.axway.ats.core.utils.StringUtils;

/**
 * Map which holds information for each component, including
 * initialization and finalization handler implementation, environment
 * cleanup handler, all action classes.
 */
public class ComponentActionMap {

    private static final Logger                        log = Logger.getLogger(ComponentActionMap.class);

    private String                                     componentName;
    private Class<? extends InitializationHandler>     initializationHandler;
    private Class<? extends FinalizationHandler>       finalizationHandler;
    private Class<? extends EnvironmentCleanupHandler> cleanupHandler;

    //map to hold the methods corresponding to the actions
    private Map<String, ActionMethodContainer>         actions;

    //map to hold the instances of the action classes
    //we need this map here and only here, because here is where
    //the action classes are registered, and if we use the dynamic
    //class loading we don't want instances where we can't destroy them
    private Map<String, Object>                        actionClassInstances;

    public ComponentActionMap( String componentName ) {

        this.componentName = componentName;
        this.actions = new HashMap<String, ActionMethodContainer>();
        this.actionClassInstances = new HashMap<String, Object>();
    }

    /**
     * Register a class with action methods
     *
     * @param actionClass   the action class
     */
    public void registerActionClass( Class<?> actionClass ) {

        log.info("Registering action class '" + actionClass.getName() + "'");

        for (Method classMethod : actionClass.getMethods()) {
            Action actionAnnotation = classMethod.getAnnotation(Action.class);
            TemplateAction templateActionAnnotation = classMethod.getAnnotation(TemplateAction.class);

            if (actionAnnotation != null || templateActionAnnotation != null) {
                String actionClassName = actionClass.getSimpleName();
                String actionMethodName = classMethod.getName();
                String actionName = actionAnnotation != null
                                                             ? actionAnnotation.name()
                                                             : templateActionAnnotation.name();
                if (StringUtils.isNullOrEmpty(actionName)) {
                    actionName = ActionMethod.buildActionMethodName(classMethod);
                }

                try {
                    addAction(actionName, actionClassName, actionMethodName, classMethod, actionClass,
                              actionAnnotation != null);
                } catch (ActionAlreadyDefinedException aaee) {
                    //log an error in case of a duplicate action and continue
                    log.error(aaee.getMessage());
                }
            }
        }
    }

    /**
     * Add an action to the component map
     *
     * @param actionName    name of the action
     * @param method        the method which implements the action
     * @throws ActionAlreadyDefinedException    if an action with this name has already been defined
     */
    private void addAction( String actionName, String actionClassName, String actionMethodName, Method method,
                            Class<?> actualClass,
                            boolean isRegularAction ) throws ActionAlreadyDefinedException {

        ActionMethodContainer methodContainer = actions.get(actionName);

        if (methodContainer == null) {
            methodContainer = new ActionMethodContainer(componentName, actionName);
            actions.put(actionName, methodContainer);
        }

        ActionMethod actionMethod;
        if (isRegularAction) {
            actionMethod = new ActionMethod(componentName, actionName, method, actualClass);
        } else {
            actionMethod = new TemplateActionMethod(componentName, actionName, actionClassName,
                                                    actionMethodName, method, actualClass);
        }

        methodContainer.add(actionMethod);
    }

    /**
     * Get the cached instance of the action class implementing the given action
     * If the instance is not cached, create it and cache it
     *
     * @param caller        the remote caller
     * @param actionName    name of the action
     * @return              the instance of the action class
     *
     * @throws ActionExecutionException     if the class instance cannot be created
     */
    public Object getCachedActionClassInstance( String caller,
                                                ActionMethod actionMethod ) throws ActionExecutionException,
                                                                            NoSuchActionException {

        Class<?> actionClass = actionMethod.getTheActualClass();
        return getCachedActionClassInstance(caller, actionClass);
    }

    /**
     * Get the cached instance of the action class
     * If the instance is not cached, create it and cache it
     *
     * @param actionClass   the action class
     * @return              the instance of the action class
     *
     * @throws ActionExecutionException     if the class instance cannot be created
     */
    public Object getCachedActionClassInstance( String caller, Class<?> actionClass )
                                                                                      throws ActionExecutionException,
                                                                                      NoSuchActionException {

        //first check if we have such action defined
        String actionClassName = actionClass.getName();

        try {
            Object actionClassInstance = actionClassInstances.get(actionClassName);
            if (actionClassInstance == null) {
                actionClassInstance = actionClass.newInstance();
                actionClassInstances.put(actionClassName, actionClassInstance);
            }

            return actionClassInstance;
        } catch (IllegalAccessException iae) {
            throw new ActionExecutionException("Could not access action class " + actionClassName, iae);
        } catch (InstantiationException ie) {
            throw new ActionExecutionException("Could not instantiate action class " + actionClassName, ie);
        }
    }

    /**
     * Get a new instance of the action class implementing the given action.
     * This method will not use the cache, it will always create a new instance.
     *
     * @param actionName    name of the action
     * @return              the instance of the action class
     *
     * @throws ActionExecutionException     if the class instance cannot be created
     */
    public Object getActionClassInstance( ActionMethod actionMethod ) throws ActionExecutionException,
                                                                      NoSuchActionException {

        //first check if we have such action defined
        Class<?> actionClass = actionMethod.getMethod().getDeclaringClass();
        String actionClassName = actionClass.getName();

        try {
            return actionClass.newInstance();
        } catch (IllegalAccessException iae) {
            throw new ActionExecutionException("Could not access action class " + actionClassName, iae);
        } catch (InstantiationException ie) {
            throw new ActionExecutionException("Could not instantiate action class " + actionClassName, ie);
        }
    }

    /**
     * Get the implementation method for the given action
     *
     * @param actionName    name of the action
     * @param argTypes      array with parameter types
     * @return              implementation method
     * @throws NoSuchActionException    if no such action is registered
     * @throws NoCompatibleMethodFoundException
     */
    public ActionMethod getActionMethod( String actionName,
                                         Class<?>[] argTypes ) throws NoSuchActionException,
                                                               NoCompatibleMethodFoundException {

        //first check if we have such action defined
        if (!actions.containsKey(actionName)) {
            throw new NoSuchActionException(actionName, componentName);
        }

        return actions.get(actionName).get(argTypes);
    }

    /**
     * Get the name of the class which implements InitializationHandler
     *
     * @return  the class name
     */
    public Class<? extends InitializationHandler> getInitializationHandler() {

        return initializationHandler;
    }

    /**
     * Set the name of the class which implements InitializationHandler
     *
     * @param initializationHandler     the class name
     */
    public void setInitializationHandler( Class<? extends InitializationHandler> initializationHandler ) {

        this.initializationHandler = initializationHandler;
    }

    /**
     * Get the name of the class which implements FinalizationHandler
     *
     * @return  the class name
     */
    public Class<? extends FinalizationHandler> getFinalizationHandler() {

        return finalizationHandler;
    }

    /**
     * Set the name of the class which implements FinalizationHandler
     *
     * @param finalizationHandler     the class name
     */
    public void setFinalizationHandler( Class<? extends FinalizationHandler> finalizationHandler ) {

        this.finalizationHandler = finalizationHandler;
    }

    /**
     * Get the name of the class which handles the environment cleanup
     *
     * @return  the class name
     */
    public Class<? extends EnvironmentCleanupHandler> getCleanupHandler() {

        return cleanupHandler;
    }

    /**
     * Set the name of the class which handles the environment cleanup
     *
     * @param cleanupHandler     the class name
     */
    public void setCleanupHandler( Class<? extends EnvironmentCleanupHandler> cleanupHandler ) {

        this.cleanupHandler = cleanupHandler;
    }

    /**
     * Get the name of the component for this action map
     *
     * @return  the name of the component
     */
    public String getComponentName() {

        return componentName;
    }

    /**
     * Clear the action map - necessary when unloading the action classes
     */
    void clear() {

        actions.clear();
    }

    public ComponentActionMap getNewCopy() {

        ComponentActionMap newComponentActionMap = new ComponentActionMap(this.componentName);

        newComponentActionMap.initializationHandler = this.initializationHandler;
        newComponentActionMap.finalizationHandler = this.finalizationHandler;
        newComponentActionMap.cleanupHandler = this.cleanupHandler;

        for (Entry<String, ActionMethodContainer> actionEntry : this.actions.entrySet()) {
            newComponentActionMap.actions.put(actionEntry.getKey(), actionEntry.getValue().getNewCopy());
        }

        /* We do not clone
         *      actionClassInstances
         * This map will be filled incrementally when an actions are requested for execution
         */
        return newComponentActionMap;
    }
}
