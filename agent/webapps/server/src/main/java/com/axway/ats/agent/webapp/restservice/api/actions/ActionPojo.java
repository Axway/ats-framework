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
package com.axway.ats.agent.webapp.restservice.api.actions;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.agent.core.action.ActionRequest;
import com.google.gson.Gson;

public class ActionPojo {

    private String   sessionId;

    private int      resourceId;

    private String   componentName;
    /**
     * the value of the @Action's annotation name parameter, NOT the actual Java method name
     * Example:
     * @Action (name="FTP actions connect" ...)
     * public void connect(...)
     * In that situation the methodName must be "FTP actions connect"
     *  **/

    private String   methodName;

    /**
     * the fully qualified names of each of the arguments Java classes
     *  **/

    private String[] argumentsTypes;
    /**
     * the values of each argument.
     * Those values will be casted to the Java classes, specified in the argumentsTypes class member
     *  **/

    private String[] argumentsValues;

    public ActionPojo() {}

    public ActionPojo( ActionRequest actionRequest ) {

        this.componentName = actionRequest.getComponentName();
        this.methodName = actionRequest.getActionName();
        if (actionRequest.getArguments() != null) {
            Gson gson = new Gson();
            this.argumentsTypes = new String[actionRequest.getArguments().length];
            this.argumentsValues = new String[actionRequest.getArguments().length];
            for (int i = 0; i < actionRequest.getArguments().length; i++) {
                Class<?> argClass = actionRequest.getArguments()[i].getClass();
                this.argumentsTypes[i] = argClass.getName();
                this.argumentsValues[i] = gson.toJson(actionRequest.getArguments()[i], argClass);
            }
        }
    }

    /**
     * @param sessionId
     * @param resourceId
     * @param componentName
     * @param methodName
     * @param argumentsTypes array of Java class names
     * @param argumentsValues
     * */
    public ActionPojo( String sessionId, int resourceId, String componentName, String methodName,
                       String[] argumentsTypes,
                       String[] argumentsValues ) {

        this.sessionId = sessionId;
        this.resourceId = resourceId;
        this.componentName = componentName;
        this.methodName = methodName;
        this.argumentsTypes = argumentsTypes;
        this.argumentsValues = argumentsValues;
    }

    public String getSessionId() {

        return sessionId;
    }

    public void setSessionId( String sessionId ) {

        this.sessionId = sessionId;
    }

    public int getResourceId() {

        return resourceId;
    }

    public void setResourceId( int resourceId ) {

        this.resourceId = resourceId;
    }

    public String getComponentName() {

        return componentName;
    }

    public void setComponentName( String componentName ) {

        this.componentName = componentName;
    }

    public String getMethodName() {

        return methodName;
    }

    public void setMethodName( String methodName ) {

        this.methodName = methodName;
    }

    public String[] getArgumentsTypes() {

        return argumentsTypes;
    }

    public void setArgumentsTypes( String[] argumentsTypes ) {

        this.argumentsTypes = argumentsTypes;
    }

    public String[] getArgumentsValues() {

        return argumentsValues;
    }

    public void setArgumentsValues( String[] argumentsValues ) {

        this.argumentsValues = argumentsValues;
    }

    /**
     * Return the actual (casted to their appropriate Java class) arguments values
     * @throws ClassNotFoundException 
     * */
    public Object[] getActualArguments() throws ClassNotFoundException {

        List<Object> actualArgs = new ArrayList<>();
        Gson gson = new Gson();
        for (int i = 0; i < argumentsValues.length; i++) {
            String value = argumentsValues[i];
            String className = argumentsTypes[i];
            Class<?> actualClass = Class.forName(className);
            Object actualValue = gson.fromJson(value, actualClass);
            actualArgs.add(actualValue);
        }

        return actualArgs.toArray();
    }

}
