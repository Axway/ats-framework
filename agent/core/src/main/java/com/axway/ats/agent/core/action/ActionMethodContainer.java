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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.axway.ats.agent.core.exceptions.ActionAlreadyDefinedException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.core.reflect.AmbiguousMethodException;
import com.axway.ats.core.reflect.MethodFinder;
import com.axway.ats.core.reflect.TypeComparisonRule;

/**
 * This class holds all methods which are associated with a given
 * action name. One action name can be associated with several methods for
 * the purposes of overloading and deprecation. This class is responsible for choosing
 * the proper method to execute based on the given action name and arguments.
 */
public class ActionMethodContainer {

    private final String                          componentName;
    private final String                          actionName;
    private final ArrayList<ActionMethod>         actionMethods;

    /**
     * The custom type comparison rules
     */
    private static final List<TypeComparisonRule> customComparisonRules;

    //init the custom type comparison rules
    static {
        customComparisonRules = new ArrayList<TypeComparisonRule>();
        customComparisonRules.add(new StringToEnumRule());
    }

    /**
     * @param componentName name of the component
     * @param actionName name of the action
     */
    public ActionMethodContainer( String componentName, String actionName ) {

        this.componentName = componentName;
        this.actionName = actionName;
        this.actionMethods = new ArrayList<ActionMethod>();
    }

    /**
     * Add a new action method
     *
     * @param actionMethod the action method to add
     * @return
     * @throws ActionAlreadyDefinedException if the new action has the exact same arguments of an existing one
     */
    public boolean add( ActionMethod actionMethod ) throws ActionAlreadyDefinedException {

        // if there is at least one method check for ambiguity
        if (actionMethods.size() > 0) {

            Class<?>[] newMethodParamTypes = actionMethod.getMethod().getParameterTypes();

            //find the action method implementation based on the arguments
            MethodFinder methodFinder = new MethodFinder("methods for action " + actionName, getMethods(),
                                                         customComparisonRules);

            //get the most specific method which accepts these parameters
            try {
                Method mostSpecificMethod = methodFinder.findMethod(newMethodParamTypes);

                //if the most specific method accepts the same parameters as our method
                //then we have ambiguity, which should not be allowed
                if (Arrays.equals(newMethodParamTypes, mostSpecificMethod.getParameterTypes())) {
                    throw new ActionAlreadyDefinedException(actionName, componentName, mostSpecificMethod);
                }

            } catch (NoSuchMethodException e) {
                //method which accepts there argument does not exist, we can safely add it

            } catch (AmbiguousMethodException e) {
                //this should not happen, as there cannot be ambiguity between two method parameters,
                //unless they are exactly the same, which we don't allow to happen
                throw new RuntimeException("AmbiguousMethodException caught while searching for action methods");
            }
        }

        //add the action method
        return actionMethods.add(actionMethod);
    }

    /**
     * Get an action method based on the type of the arguments
     *
     * @param argTypes array of argument types
     * @return the action method which best matches these arg types
     * @throws NoCompatibleMethodFoundException if there is no action method which matches these types
     */
    public ActionMethod get( Class<?>[] argTypes ) throws NoCompatibleMethodFoundException {

        //find the action method implementation based on the arguments
        MethodFinder methodFinder = new MethodFinder("methods for action " + actionName, getMethods(),
                                                     customComparisonRules);
        try {
            Method implementingMethod = methodFinder.findMethod(argTypes);
            for (ActionMethod actionMethod : actionMethods) {
                if (actionMethod.getMethod().equals(implementingMethod)) {

                    //we found the proper method, so return it for execution
                    return actionMethod;
                }
            }
        } catch (NoSuchMethodException e) {
            //obviously no method which accepts these argument has been found
            throw new NoCompatibleMethodFoundException("Could not find compatible action method", argTypes,
                                                       actionMethods, componentName, actionName);

        } catch (AmbiguousMethodException e) {
            //there was more than one method matching the given arguments, and not one of them was specific enough
            throw new NoCompatibleMethodFoundException("Ambigous methods", argTypes, actionMethods,
                                                       componentName, actionName);
        }

        //should never happen, but just in case
        throw new NoCompatibleMethodFoundException("Could not find compatible action method", argTypes,
                                                   actionMethods, componentName, actionName);
    }

    /**
     * @return a list of all methods associated with the given action name
     */
    public List<Method> getMethods() {

        ArrayList<Method> methods = new ArrayList<Method>();
        for (ActionMethod actionMethod : actionMethods) {
            methods.add(actionMethod.getMethod());
        }

        return methods;
    }

    public ActionMethodContainer getNewCopy() {

        ActionMethodContainer newActionMethodContainer = new ActionMethodContainer(this.componentName,
                                                                                   this.actionName);

        for (ActionMethod actionMethod : this.actionMethods) {
            try {
                newActionMethodContainer.add(actionMethod.getNewCopy());
            } catch (ActionAlreadyDefinedException e) {
                /*
                 * This cannot happen, because this action was already verified
                 * to be OK when loaded for first time for the default agent component
                 */
            }
        }

        return newActionMethodContainer;
    }
}
