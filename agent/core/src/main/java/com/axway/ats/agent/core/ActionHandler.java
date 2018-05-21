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

import com.axway.ats.agent.core.action.ActionMethod;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.InternalComponentException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;

/**
 * Class which performs the actual calls to the component actions.
 * The component and actions must first be registered using the
 * {@link DynamicRegistration}. This class is a singleton.
 * 
 */
public final class ActionHandler {

    /**
     * Execute an action on the remote host, pass the arguments as Objects 
     * 
     * @param caller            the remote caller
     * @param componentName     the name of the component
     * @param actionName        the name of the action
     * @param args              list of Object arguments
     * @return                  the result of the execution as an Object
     * 
     * @throws NoSuchComponentException     if the given component is not registered
     * @throws NoSuchActionException        if the given action is not registered
     * @throws ActionExecutionException     if exception occurred during action execution
     * @throws NoCompatibleMethodFoundException if no compatible method is found for this action
     */
    public static Object executeAction( String caller, String componentName, String actionName,
                                        Object[] args ) throws NoSuchComponentException, NoSuchActionException,
                                                        ActionExecutionException, InternalComponentException,
                                                        NoCompatibleMethodFoundException {

        //get the component action map for this caller
        //if component with this name does not exist, a NoSuchComponentException is thrown
        ComponentActionMap actionMap = ComponentRepository.getInstance()
                                                          .getComponentActionMap(caller, componentName);

        //get the argument types
        Class<?>[] argTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                argTypes[i] = Void.TYPE;
            } else {
                argTypes[i] = args[i].getClass();
            }
        }

        ActionMethod actionMethod = actionMap.getActionMethod(actionName, argTypes);
        Object actionClassInstance = actionMap.getCachedActionClassInstance(caller, actionMethod);

        //invoke the action
        return actionMethod.invoke(actionClassInstance, args, true);
    }

    /**
     * Tells if an Agent component is loaded, so its actions can be called 
     * 
     * @param caller            the remote caller
     * @param componentName     the name of the component
     * @return                  whether it is available
     */
    public static boolean isComponentLoaded( String caller, String componentName ) {

        try {
            // check if the component is loaded
            ComponentRepository.getInstance().getComponentActionMap(caller, componentName);
            return true;
        } catch (NoSuchComponentException e) {
            return false;
        }
    }
}
