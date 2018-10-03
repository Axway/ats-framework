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

import java.lang.reflect.Array;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.netty.util.internal.StringUtil;

/**
 * A class representing a request to execute a single action
 */
public class ActionRequest {

    private String   componentName;
    private String   actionName;
    private Object[] args;
    private boolean  registerAction;
    private String   transferUnit;
    private String   requestUrl;
    private String   requestMethod;
    private String   requestBody;
    private String[] argumentsNames;
    /**
     * the Java class that this request's response result will be
     * */
    private Class<?> returnType;

    /**
     * @param componentName name of the component
     * @param actionName name of the action
     * @param args arguments
     */
    public ActionRequest( String componentName,
                          String actionName,
                          Object[] args ) {

        this.componentName = componentName;
        this.actionName = actionName;
        this.args = args;
    }

    /**
     * @param componentName name of the component
     * @param actionName name of the action
     * @param args arguments
     * @param requestUrl the URL that will be used to communicate with the agent
     * @param requestMethod the HTTP method that will be used to communicate with the agent
     * @param requestBody the request body that will be send to the Agent
     * @param argumentsNames the names of each argument
     *        These names, along with the arguments values will construct the HTTP request JSON body
     * @param returnType the class object of this request's response result
     */
    public ActionRequest( String componentName,
                          String actionName,
                          Object[] args,
                          String requestUrl,
                          String requestMethod,
                          String[] argumentsNames,
                          Class<?> returnType ) {

        this.componentName = componentName;
        this.actionName = actionName;
        this.args = args;
        this.requestUrl = requestUrl;
        this.requestMethod = requestMethod;
        this.argumentsNames = argumentsNames;
        this.returnType = returnType;
        this.requestBody = createRequestBody();
    }

    /**
     * @return the component name
     */
    public String getComponentName() {

        return componentName;
    }

    /**
     * Get the name of the action
     *
     * @return the action name
     */
    public String getActionName() {

        return actionName;
    }

    /**
     * Get the arguments to execute the action with
     *
     * @return the argument to execute with
     */
    public Object[] getArguments() {

        return args;
    }

    /**
     * Set whether to register or not the actions
     * 
     * @param registerAction populate or not the action in the database
     */
    public void setRegisterActionExecution( boolean registerAction ) {

        this.registerAction = registerAction;
    }

    /**
     * Get whether to register or not the actions
     * 
     * @return argument to populate or not the action
     */
    public boolean getRegisterActionExecution() {

        return this.registerAction;
    }

    /**
     * @return transfer unit data transfer actions
     */
    public String getTransferUnit() {

        return transferUnit;
    }

    /**
     * Set the transfer unit data transfer actions
     * @param transferUnit
     */
    public void setTransferUnit( String transferUnit ) {

        this.transferUnit = transferUnit;
    }

    public String getRequestUrl() {

        return requestUrl;
    }

    public void setRequestUrl( String requestUrl ) {

        this.requestUrl = requestUrl;
    }

    public String getRequestMethod() {

        return requestMethod;
    }

    public void setRequestMethod( String httpMethod ) {

        this.requestMethod = httpMethod;
    }

    public String getRequestBody() {

        return requestBody;
    }

    public String[] getArgumentsNames() {

        return argumentsNames;
    }

    public void setArgumentsNames( String[] argumentsNames ) {

        this.argumentsNames = argumentsNames;
    }

    public Class<?> getReturnType() {

        return returnType;
    }

    public void setReturnType( Class<?> returnType ) {

        this.returnType = returnType;
    }

    private String createRequestBody() {

        if (argumentsNames == null) {
            // since there is no arguments names, return empty JSON object
            return "{}";
        }

        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        if (args != null) { // check only args, since we already know that argumentsNames are not null
            if (argumentsNames.length != args.length) {
                throw new IllegalArgumentException("Could not construct request body. "
                                                   + "Provided arguments and arguments' names have different length.");
            } else {
                // everything seems OK, proceed by adding each key-value to the JSON object)
                for (int i = 0; i < args.length; i++) {
                    String key = argumentsNames[i].trim();
                    if (StringUtil.isNullOrEmpty(key)) {
                        throw new IllegalArgumentException("Argument name at index '" + i + "' is empty/null");
                    }
                    if (args[i] == null) {
                        /* 
                         * this way if the argument is String and also null,
                         * the actual value will be null instead of "null"
                         */
                        jsonObject.add(key, null);
                    } else {
                        if (args[i].getClass().isArray()) {
                            /* arrays are "special"
                             * so they need special treatment
                            */
                            if (args[i] instanceof Object[]) {
                                JsonArray array = new JsonArray();
                                for (Object arrayEl : (Object[]) args[i]) {
                                    if (arrayEl instanceof String) {
                                        // GSON additionally escapes String, this line prevent that
                                        array.add(new JsonPrimitive((String) arrayEl));
                                    } else {
                                        array.add(gson.toJson(arrayEl));
                                    }
                                }
                                jsonObject.add(key, array);
                            } else {
                                // the array is of primitive type (int[], boolean[], etc)
                                JsonArray array = new JsonArray();
                                for (int k = 0; k < Array.getLength(args[i]); k++) {
                                    array.add(gson.toJson(Array.get(args[i], k)));
                                }
                                jsonObject.add(key, array);
                            }

                        } else if (args[i].getClass().getName().equals(String.class.getName())) {
                            // GSON additionally escapes String, this line prevent that
                            jsonObject.add(key, new JsonPrimitive((String) args[i]));
                        } else if (Map.class.isAssignableFrom(args[i].getClass())) {
                            //
                            JsonObject map = new JsonObject();
                            // map elements that have String objects as value are not properly serialized
                            for (Map.Entry<String, String> entry : ((Map<String, String>) args[i]).entrySet()) {
                                map.add(entry.getKey(), new JsonPrimitive(entry.getValue()));
                            }
                            jsonObject.add(key, map);
                        } else {
                            JsonElement value = null;
                            if (args[i] != null) {
                                value = gson.toJsonTree(args[i], args[i].getClass());
                            } else {
                                value = gson.toJsonTree(args[i]);
                            }
                            jsonObject.add(key, value);
                        }
                    }
                }
            }
        } else {
            // all of the arguments are null, so just iterate over the arguments names and add null value for each key/name
            for (int i = 0; i < argumentsNames.length; i++) {
                String key = argumentsNames[i].trim();
                jsonObject.add(key, null);
            }
        }

        return jsonObject.toString();
    }

}
