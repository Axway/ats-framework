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

/**
 * A class representing a request to execute a single action
 */
public class ActionRequest {

    private String   componentName;
    private String   actionName;
    private Object[] args;
    private boolean  registerAction;
    private String   transferUnit;

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

}
