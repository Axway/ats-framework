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
package com.axway.ats.agent.webapp.restservice.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.google.gson.Gson;

/**
 * Class that is responsible for initialization, execution and deinitialization of resources ( Action class instances ).
 *  **/
public class ResourcesManager {

    private static final Logger LOG = Logger.getLogger(ResourcesManager.class);

    public synchronized static int initializeResource( ActionPojo pojo ) throws NoSuchActionException,
                                                                         NoCompatibleMethodFoundException,
                                                                         NoSuchComponentException,
                                                                         ClassNotFoundException,
                                                                         InstantiationException,
                                                                         IllegalAccessException {

        Method method = getActionMethod(pojo);

        // get the actual Action class instance
        Object actionClassInstance = method.getDeclaringClass().newInstance();
        // put it in the resource map for that session

        int actionId = ResourcesRepository.getInstance().putResource(pojo.getSessionId(), actionClassInstance);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Action class '" + method.getDeclaringClass().getName() + "' received resource id '" + actionId
                      + "'");
        }

        return actionId;

    }

    public synchronized static int deinitializeResource( String sessionId, int resourceId ) {

        Object actionClassInstance = ResourcesRepository.getInstance().deleteResource(sessionId, resourceId);
        if (actionClassInstance == null) {
            throw new NoSuchElementException("Unable to delete resource with id '" + resourceId
                                             + "' for session with id '"
                                             + sessionId + "'. No such actionId exists");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Resource with id '" + resourceId + "' deleted");
        }

        return resourceId;
    }

    public synchronized static Object executeOverResource( ActionPojo pojo ) throws NoSuchActionException,
                                                                             NoCompatibleMethodFoundException,
                                                                             NoSuchComponentException,
                                                                             ClassNotFoundException,
                                                                             InstantiationException,
                                                                             IllegalAccessException,
                                                                             IllegalArgumentException,
                                                                             InvocationTargetException {

        Object actionClassInstance = ResourcesRepository.getInstance().getResource(pojo.getSessionId(),
                                                                                   pojo.getResourceId());
        Method method = getActionMethod(pojo);
        if (pojo.getArgumentsTypes() == null) {
            return method.invoke(actionClassInstance, new Object[]{});
        }
        if (pojo.getArgumentsTypes().length != pojo.getArgumentsValues().length) {
            throw new RuntimeException("Provided action method arguments types and arguments values have different length");
        }
        
        Object[] args = getActualArgumentsValues(pojo);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Execution of action method '" + method.getDeclaringClass().getName() + "@" + method.getName()
                      + "' with arguments {"
                      + Arrays.asList(args).toString().substring(1, Arrays.asList(args).toString().length() - 1)
                      + "} using resource with id '"
                      + pojo.getResourceId() + "' from session with id '"
                      + pojo.getSessionId() + "'");
        }

        return method.invoke(actionClassInstance, args);

    }

    private static Method getActionMethod( ActionPojo pojo ) throws NoSuchActionException,
                                                             NoCompatibleMethodFoundException,
                                                             NoSuchComponentException,
                                                             ClassNotFoundException, InstantiationException,
                                                             IllegalAccessException {

        if (pojo.getArgumentsTypes() == null) {
            // get actual Action method object
            return ComponentRepository.getInstance()
                                      .getComponentActionMap(pojo.getComponentName())
                                      .getActionMethod(pojo.getMethodName(),
                                                       new Class<?>[]{})
                                      .getMethod();
        } else {
            Class<?>[] actionMethodActualArgumentsClasses = new Class<?>[pojo.getArgumentsTypes().length];
            for (int i = 0; i < pojo.getArgumentsTypes().length; i++) {
                // load argument class
                actionMethodActualArgumentsClasses[i] = loadArgType(pojo.getArgumentsTypes()[i]);
            }
            // get actual Action method object
            return ComponentRepository.getInstance()
                                      .getComponentActionMap(pojo.getComponentName())
                                      .getActionMethod(pojo.getMethodName(),
                                                       actionMethodActualArgumentsClasses)
                                      .getMethod();
        }
    }

    private static Object[] getActualArgumentsValues( ActionPojo pojo ) throws ClassNotFoundException {

        Gson gson = new Gson();
        Class<?>[] actionMethodActualArgumentsClasses = new Class<?>[pojo.getArgumentsTypes().length];
        for (int i = 0; i < pojo.getArgumentsTypes().length; i++) {
            // load argument class
            actionMethodActualArgumentsClasses[i] = loadArgType(pojo.getArgumentsTypes()[i]);
        }
        Object[] actionMethodActualArgumentsValues = new Object[actionMethodActualArgumentsClasses.length];
        for (int i = 0; i < actionMethodActualArgumentsValues.length; i++) {
            actionMethodActualArgumentsValues[i] = gson.fromJson(pojo.getArgumentsValues()[i],
                                                                 actionMethodActualArgumentsClasses[i]);
        }
        return actionMethodActualArgumentsValues;
    }

    private static Class<?> loadArgType( String argClassName ) throws ClassNotFoundException {

        switch (argClassName) {
            case "byte.class":
            case "byte":
                return byte.class;
            case "short.class":
            case "short":
                return short.class;
            case "int.class":
            case "int":
                return int.class;
            case "long.class":
            case "long":
                return long.class;
            case "float.class":
            case "float":
                return float.class;
            case "double.class":
            case "double":
                return double.class;
            case "boolean.class":
            case "boolean":
                return boolean.class;
            case "char.class":
            case "char":
                return char.class;
            default:
                return Class.forName(argClassName);
        }

    }

}
