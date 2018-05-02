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
import java.util.HashMap;
import java.util.Map;

import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.model.ActionPojo;
import com.google.gson.Gson;

public class ActionsManager {

    private static Map<Integer, Object> actions  = new HashMap<>();
    private static int                  actionId = -1;

    public synchronized static int initializeAction( ActionPojo pojo ) throws NoSuchActionException,
                                                                       NoCompatibleMethodFoundException,
                                                                       NoSuchComponentException, ClassNotFoundException,
                                                                       InstantiationException, IllegalAccessException {

        Method method = getActionMethod(pojo);

        // get the Actual Action class instance
        Object actionClassInstance = method.getDeclaringClass().newInstance();
        // put it in the map
        actions.put(++actionId, actionClassInstance);

        return actionId;

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
                                      .getActionMethod(pojo.getActionMethodName(),
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
                                      .getActionMethod(pojo.getActionMethodName(),
                                                       actionMethodActualArgumentsClasses)
                                      .getMethod();
        }
    }

    public synchronized static int deinitializeAction( int actionId ) {

        Object actionClassInstance = actions.remove(actionId);
        if (actionClassInstance == null) {
            throw new RuntimeException("Unable to delete action with id '" + actionId + "'. No such actionId exists");
        }
        return actionId;
    }

    public synchronized static Object executeAction( ActionPojo pojo ) throws NoSuchActionException,
                                                                       NoCompatibleMethodFoundException,
                                                                       NoSuchComponentException, ClassNotFoundException,
                                                                       InstantiationException, IllegalAccessException,
                                                                       IllegalArgumentException,
                                                                       InvocationTargetException {

        Object actionClassInstance = actions.get(pojo.getActionId());
        Method method = getActionMethod(pojo);
        if (pojo.getArgumentsTypes() == null) {
            return method.invoke(actionClassInstance, new Object[]{}); // or null
        }
        if (pojo.getArgumentsTypes().length != pojo.getArgumentsValues().length) {
            throw new RuntimeException("Provided action method arguments types and arguments values have different length");
        }
        Object[] args = getActualArgumentsValues(pojo);
        return method.invoke(actionClassInstance, args);

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
                return byte.class;
            case "short.class":
                return short.class;
            case "int.class":
                return int.class;
            case "long.class":
                return long.class;
            case "float.class":
                return float.class;
            case "double.class":
                return double.class;
            case "boolean.class":
                return boolean.class;
            case "char.class":
                return char.class;
            default:
                return Class.forName(argClassName);
        }

    }

}
