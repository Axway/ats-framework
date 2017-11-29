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
package com.axway.ats.agent.core.model;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.axway.ats.agent.core.ComponentActionMap;
import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;

/**
 * Class that can be used by users to call some actions from within other actions. 
 * This is all server side work.
 */
public abstract class UserActionInvoker {

    protected String            componentName;

    private ComponentActionMap  componentActionMap;

    private Map<String, Method> actionMethodsMap;

    /**
     * Generic constructor specifying which Agent component we are working with
     * 
     * @param componentName the name of the Agent component
     * @throws NoSuchComponentException
     */
    public UserActionInvoker( String componentName ) throws NoSuchComponentException {

        this.componentName = componentName;
        this.componentActionMap = ComponentRepository.getInstance().getComponentActionMap( componentName );
        this.actionMethodsMap = new HashMap<String, Method>();
    }

    /**
     * Return the needed java method
     * 
     * @param actionMethodName
     * @param parameterValues
     * @return
     * @throws NoSuchComponentException
     * @throws ActionExecutionException
     * @throws NoSuchActionException
     * @throws NoCompatibleMethodFoundException
     */
    protected Method resolveActionMethod(
                                          String actionMethodName,
                                          Object[] parameterValues ) throws NoSuchComponentException,
                                                                    ActionExecutionException,
                                                                    NoSuchActionException,
                                                                    NoCompatibleMethodFoundException {

        //get the argument types and construct a key used for caching the found methods 
        StringBuilder actionMethodReference = new StringBuilder( actionMethodName );
        Class<?>[] argTypes = new Class<?>[parameterValues.length];
        for( int i = 0; i < parameterValues.length; i++ ) {
            if( parameterValues[i] == null ) {
                argTypes[i] = Void.TYPE;
            } else {
                argTypes[i] = parameterValues[i].getClass();
            }
            actionMethodReference.append( argTypes[i].getSimpleName() );
        }
        final String actionMethodReferenceString = actionMethodReference.toString();

        Method actionMethod = actionMethodsMap.get( actionMethodReferenceString );
        if( actionMethod == null ) {
            actionMethod = componentActionMap.getActionMethod( actionMethodName, argTypes ).getMethod();
            actionMethodsMap.put( actionMethodReferenceString, actionMethod );
        }
        return actionMethod;
    }

    /**
     * Return the instance object of the action class
     * 
     * @param actionClass
     * @return
     * @throws ActionExecutionException
     * @throws NoSuchActionException
     */
    protected Object resolveActionObject(
                                          Class<?> actionClass ) throws ActionExecutionException,
                                                                NoSuchActionException {

        return componentActionMap.getCachedActionClassInstance( ComponentRepository.DEFAULT_CALLER,
                                                                actionClass );
    }

    /**
     * Get the name of the action annotation
     * 
     * @param isGenericAction true when this is a generic Action, false when it is a TemplateAction
     * @param actionMethod
     * @return
     */
    protected String resolveActionName(
                                        boolean isGenericAction,
                                        Method actionMethod ) {

        if( isGenericAction ) {
            return actionMethod.getAnnotation( Action.class ).name();
        } else {
            return actionMethod.getAnnotation( TemplateAction.class ).name();
        }
    }
}
