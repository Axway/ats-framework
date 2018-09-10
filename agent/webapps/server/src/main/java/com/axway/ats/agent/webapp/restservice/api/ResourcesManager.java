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
import java.util.Set;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.google.gson.Gson;

/**
 * Class that is responsible for initialization, execution and deinitialization
 * of resources ( Action class instances ).
 **/
public class ResourcesManager {

    private static final Logger LOG = Logger.getLogger(ResourcesManager.class);

    /**
     * Initialize resource to some InternalXYZOperation's class
     * 
     * @param pojo the Action information
     */
    public synchronized static long initializeResource( ActionPojo pojo )
                                                                          throws NoSuchActionException,
                                                                          NoCompatibleMethodFoundException,
                                                                          NoSuchComponentException,
                                                                          ClassNotFoundException,
                                                                          InstantiationException,
                                                                          IllegalAccessException {

        Method method = getActionMethod(pojo);

        // get the actual Action class instance
        Object actionClassInstance = method.getDeclaringClass().newInstance();

        // put it in the resource map for that caller
        // also return resource ID for this action class instance
        long resourceId = ResourcesRepository.getInstance().putResource(actionClassInstance);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Action class '" + method.getDeclaringClass().getName() + "' added to caller '"
                      + pojo.getCallerId() + ". Its resource ID is '" + resourceId + "'");
        }

        return resourceId;

    }

    /**
     * <p>
     * Add non-action resource to the current caller
     * </p>
     * <p>
     * This method is used when we want to access a resource (an JAVA object) that
     * is not an action class
     * </p>
     * 
     * @param resource
     *            the resource that we want to add to this caller
     */
    public synchronized static long addResource( Object resource ) {

        long resourceId = ResourcesRepository.getInstance().putResource(resource);
        if (LOG.isDebugEnabled()) {
            LOG.debug("New object from class '" + resource.getClass().getName() + "' added to caller '"
                      + ThreadsPerCaller.getCaller()
                      + "'. Its resourceID is '" + resourceId + "'");
        }

        return resourceId;

    }

    /**
     * Deinitialize resource to some InternalXYZOperation's class
     */
    public synchronized static long deinitializeResource( long resourceId ) {

        Object actionClassInstance = ResourcesRepository.getInstance().deleteResource(resourceId);
        if (actionClassInstance == null) {
            throw new NoSuchElementException("Unable to delete resource with id '" + resourceId
                                             + "' for caller with id '" + ThreadsPerCaller.getCaller()
                                             + "'. No such actionId exists");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Resource with id '" + resourceId + "' deleted");
        }

        return resourceId;
    }

    /**
     * Deinitialize all testcase resources by using the current caller ID and testcase ID
     * */
    public synchronized static void deinitializeTestcaseResources() {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting all testcase resources from caller '" + ThreadsPerCaller.getCaller()
                      + "' and testcase '" + PassiveDbAppender.getCurrentInstance().getTestCaseId() + "' ...");
        }
        Set<Long> deletedResources = ResourcesRepository.getInstance().deleteResources();
        if (deletedResources != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resources with IDs " + deletedResources.toString() + " were deleted");
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No resources where found and nothing was deleted");
            }
        }

    }

    /**
     * Execute some action method using some InternalXYZOperation action object
     */
    public synchronized static Object executeOverResource( ActionPojo pojo ) throws NoSuchActionException,
                                                                             NoCompatibleMethodFoundException,
                                                                             NoSuchComponentException,
                                                                             ClassNotFoundException,
                                                                             InstantiationException,
                                                                             IllegalAccessException,
                                                                             IllegalArgumentException,
                                                                             InvocationTargetException {

        Object actionClassInstance = ResourcesRepository.getInstance().getResource(
                                                                                   pojo.getResourceId());

        if (actionClassInstance == null) {
            throw new RuntimeException("There is no initialized action class for action '" + pojo.getMethodName()
                                       + "'");
        }
        Method method = getActionMethod(pojo);
        if (pojo.getArgumentsTypes() == null) {
            return method.invoke(actionClassInstance, new Object[]{});
        }
        if (pojo.getArgumentsTypes().length != pojo.getArgumentsValues().length) {
            throw new RuntimeException(
                                       "Provided action method arguments types and arguments values have different length");
        }

        Object[] args = getActualArgumentsValues(pojo);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Execution of action method '" + method.getDeclaringClass().getName() + "@" + method.getName()
                      + "' with arguments {"
                      + Arrays.asList(args).toString().substring(1, Arrays.asList(args).toString().length() - 1)
                      + "} using resource with id '" + pojo.getResourceId() + "' from caller with id '"
                      + pojo.getCallerId() + "'");
        }

        return method.invoke(actionClassInstance, args);

    }

    public synchronized static Object getResource( long resourceId ) {

        return ResourcesRepository.getInstance().getResource(resourceId);

    }

    private static Method getActionMethod( ActionPojo pojo )
                                                             throws NoSuchActionException,
                                                             NoCompatibleMethodFoundException, NoSuchComponentException,
                                                             ClassNotFoundException, InstantiationException,
                                                             IllegalAccessException {

        if (pojo.getArgumentsTypes() == null) {
            // get actual Action method object
            return ComponentRepository.getInstance()
                                      .getComponentActionMap(pojo.getComponentName())
                                      .getActionMethod(pojo.getMethodName(), new Class<?>[]{})
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
                                      .getActionMethod(pojo.getMethodName(), actionMethodActualArgumentsClasses)
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
