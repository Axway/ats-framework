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
package com.axway.ats.agent.core.templateactions;

import java.lang.reflect.Method;

import com.axway.ats.agent.core.action.TemplateActionMethod;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.InternalComponentException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.model.UserActionInvoker;

/**
 * Allow users to call some template actions from within other actions.
 * This is all server side work.
 */
public class UserTemplateActionInvoker extends UserActionInvoker {

    public UserTemplateActionInvoker( String componentName ) throws NoSuchComponentException {

        super( componentName );
    }

    /**
     *
     * Execute a template action and return a single value from the action response.
     * <br>
     * For example if you specify you want "//NODE1/NODE2/NODE3/id" and a few values are matched,
     * only the first one is returned
     *
     * @param actionClass the action java class
     * @param actionMethodName the name of action java method
     * @param parameterValues values for the arguments this action accepts
     * @param wantedXpathEntry what response entry are we interested of
     * @return the first value matched by the XPath entry
     *
     * @throws ActionExecutionException
     * @throws NoSuchActionException
     * @throws NoSuchComponentException
     * @throws SecurityException
     * @throws InternalComponentException
     * @throws NoCompatibleMethodFoundException
     * @throws NoSuchMethodException
     */
    public String invokeAndReturnOneValue(
                                           Class<?> actionClass,
                                           String actionMethodName,
                                           Object[] parameterValues,
                                           String wantedXpathEntry ) throws ActionExecutionException,
                                                                    NoSuchActionException,
                                                                    NoSuchComponentException,
                                                                    SecurityException,
                                                                    InternalComponentException,
                                                                    NoCompatibleMethodFoundException,
                                                                    NoSuchMethodException {

        String[][] valueArrays = invoke( actionClass,
                                         actionMethodName,
                                         parameterValues,
                                         new String[]{ wantedXpathEntry } );
        if( valueArrays.length > 0 && valueArrays[0].length > 0 ) {
            return valueArrays[0][0];
        } else {
            throw new ActionExecutionException( "At least one value was expected, but nothing was returned" );
        }
    }

    /**
     * Execute a template action and return a single list of values from the action response.
     * <br>
     * For example if you specify you want "//NODE1/NODE2/NODE3/id" and a few values are matched,
     * all these are returned
     *
     * @param actionClass the action java class
     * @param actionMethodName the name of action java method
     * @param parameterValues values for the arguments this action accepts
     * @param wantedXpathEntry what response entry are we interested of
     * @return list with all values matched by the XPath entry
     *
     * @throws ActionExecutionException
     * @throws NoSuchActionException
     * @throws NoSuchComponentException
     * @throws SecurityException
     * @throws InternalComponentException
     * @throws NoCompatibleMethodFoundException
     * @throws NoSuchMethodException
     */
    public String[] invokeAndReturnOneArrayOfValues(
                                                     Class<?> actionClass,
                                                     String actionMethodName,
                                                     Object[] parameterValues,
                                                     String wantedXpathEntry )
                                                                              throws ActionExecutionException,
                                                                              NoSuchActionException,
                                                                              NoSuchComponentException,
                                                                              SecurityException,
                                                                              InternalComponentException,
                                                                              NoCompatibleMethodFoundException,
                                                                              NoSuchMethodException {

        String[][] valueArrays = invoke( actionClass,
                                         actionMethodName,
                                         parameterValues,
                                         new String[]{ wantedXpathEntry } );
        if( valueArrays.length > 0 ) {
            return valueArrays[0];
        } else {
            throw new ActionExecutionException( "At least one array of values was expected, but no one was returned" );
        }
    }

    public String invokeAndReturnTheLastResponseBody(
                                                      Class<?> actionClass,
                                                      String actionMethodName,
                                                      Object[] parameterValues )
                                                                                throws ActionExecutionException,
                                                                                NoSuchActionException,
                                                                                NoSuchComponentException,
                                                                                SecurityException,
                                                                                InternalComponentException,
                                                                                NoCompatibleMethodFoundException,
                                                                                NoSuchMethodException {

        Object actionObject = resolveActionObject( actionClass );
        Method actionMethod = resolveActionMethod( actionMethodName, parameterValues );
        String actionName = resolveActionName( false, actionMethod );

        // construct a template action and invoke it
        TemplateActionMethod templateActionMethod = new TemplateActionMethod( componentName,
                                                                              actionName,
                                                                              actionClass.getSimpleName(),
                                                                              actionMethod.getName(),
                                                                              actionMethod,
                                                                              actionClass );
        templateActionMethod.setReturnResponseBodyAsString( true );
        String content = ( String ) templateActionMethod.invoke( actionObject, parameterValues, false );

        if( content == null ) {
            throw new ActionExecutionException( "Can't return the last response body" );
        }
        return content;
    }

    /**
     * Execute a template action and return all requested list of values from the action response.
     * <br>
     * For example if you specify you want "//NODE1/NODE2/NODE3/id" and "//NODE1/NODE2/name",
     * you will get a list of 'id' values and another list of 'name' values
     *
     * @param actionClass the action java class
     * @param actionMethodName the name of action java method
     * @param parameterValues values for the arguments this action accepts
     * @param wantedXpathEntries what response entries are we interested of
     * @return list with list of values matched by all requested XPath entries
     *
     * @throws ActionExecutionException
     * @throws NoSuchActionException
     * @throws NoSuchComponentException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InternalComponentException
     * @throws NoCompatibleMethodFoundException
     */
    public String[][] invoke(
                              Class<?> actionClass,
                              String actionMethodName,
                              Object[] parameterValues,
                              String[] wantedXpathEntries ) throws ActionExecutionException,
                                                           NoSuchActionException, NoSuchComponentException,
                                                           SecurityException, NoSuchMethodException,
                                                           InternalComponentException,
                                                           NoCompatibleMethodFoundException {

        Object actionObject = resolveActionObject( actionClass );
        Method actionMethod = resolveActionMethod( actionMethodName, parameterValues );
        String actionName = resolveActionName( false, actionMethod );

        // construct a template action and invoke it
        TemplateActionMethod templateActionMethod = new TemplateActionMethod( componentName,
                                                                              actionName,
                                                                              actionClass.getSimpleName(),
                                                                              actionMethod.getName(),
                                                                              actionMethod,
                                                                              actionClass );
        templateActionMethod.setWantedXpathEntries( wantedXpathEntries );
        CompositeResult result = ( CompositeResult ) templateActionMethod.invoke( actionObject,
                                                                                  parameterValues,
                                                                                  false );
        return ( String[][] ) result.getReturnResult();
    }
}
