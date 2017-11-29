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
package com.axway.ats.agent.core.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.axway.ats.agent.core.ComponentActionMap;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.InternalComponentException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;

/**
 * This class is used for invoking an action for a given action class instance
 */
public class ActionInvoker {

    private ActionRequest        actionRequest;
    private ActionMethod         actionMethod;
    private Map<String, Integer> parameterPositions;
    private Object[]             args;

    public List<String> getActionMethodParameterNames() {

        return actionMethod.getParameterNames();
    }

    /**
     * @param actionRequest an action request
     * @throws NoSuchComponentException
     * @throws NoSuchActionException
     * @throws NoCompatibleMethodFoundException
     */
    public ActionInvoker( ActionRequest actionRequest ) throws NoSuchComponentException,
                                                       NoSuchActionException,
                                                       NoCompatibleMethodFoundException {

        this.actionRequest = actionRequest;

        String componentName = actionRequest.getComponentName();
        String actionName = actionRequest.getActionName();

        //clone the arguments, as we may need to change them later and
        //we do not want the same array to be modified by the different threads
        Object[] args = actionRequest.getArguments().clone();

        //get the component action map
        //if such does not exist, a NoSuchComponentException is thrown
        ComponentActionMap actionMap = ComponentRepository.getInstance()
                                                          .getComponentActionMap( componentName );

        //get the argument types
        Class<?>[] argTypes = new Class<?>[args.length];
        for( int i = 0; i < args.length; i++ ) {
            if( args[i] == null ) {
                argTypes[i] = Void.TYPE;
            } else {
                argTypes[i] = args[i].getClass();
            }
        }

        this.actionMethod = actionMap.getActionMethod( actionName, argTypes );

        //get the parameter names
        List<String> parameterNames = this.actionMethod.getParameterNames();

        //set the positions
        this.parameterPositions = new HashMap<String, Integer>();
        for( int i = 0; i < parameterNames.size(); i++ ) {
            this.parameterPositions.put( parameterNames.get( i ), i );
        }

        //set the initial arguments - they can later be changes with
        this.args = args;
    }

    /**
     * @return the action method to handle this action
     */
    public ActionMethod getActionMethod() {

        return actionMethod;
    }

    /**
     * @return the class which implements this action
     */
    public Class<?> getActionClass() {

        return actionMethod.getTheActualClass();
    }

    /**
     * @return the name of the action to execute
     */
    public String getActionName() {

        return actionRequest.getActionName();
    }

    /**
     * Set some of the argument with which this action invoker
     * will execute the action
     *
     * @param arguments list of argument values
     */
    public List<ArgumentValue> setArguments(
                                             List<ArgumentValue> argumentValues ) {

        for( Entry<String, Integer> paramPositon : parameterPositions.entrySet() ) {
            for( ArgumentValue argumentValue : argumentValues ) {
                if( paramPositon.getKey().equals( argumentValue.getName() ) ) {
                    args[paramPositon.getValue()] = argumentValue.getValue();
                    argumentValues.remove( argumentValue );
                    break;
                }
            }
        }

        return argumentValues;
    }

    /**
     * Invoke this action on the given action class instance - validation of the
     * input arguments will not be performed
     *
     * @param actionClassInstance the instance to invoke on
     * @throws ActionExecutionException if there was an error invoking the action
     * @throws InternalComponentException if there was an error during the action execution.
     * @return the result of this action invocation
     */
    public Object invoke(
                          Object actionClassInstance ) throws ActionExecutionException,
                                                      InternalComponentException {

        return invoke( actionClassInstance, false );
    }

    /**
     * Invoke this action on the given action class instance
     *
     * @param actionClassInstance the instance to invoke on
     * @param validateArguments whether to validate the input arguments
     * @param actionClassInstance the instance to invoke on
     * @throws ActionExecutionException if there was an erro invoking the action
     * @throws InternalComponentException if there was an error during the action execution.
     * @return the result of this action invocation
     */
    public Object invoke(
                          Object actionClassInstance,
                          boolean validateArguments ) throws ActionExecutionException,
                                                     InternalComponentException {

        return actionMethod.invoke( actionClassInstance, args, validateArguments );
    }
}
