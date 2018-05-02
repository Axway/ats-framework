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
package com.axway.ats.agent.webapp.restservice.api.model;

public class ActionPojo {

    private int      actionId;
    /** 
     * <p>The action method name.</p>
     * <p>Note that this is the value of the name attribute to the @Action annotation and not the actual Java method name</p>
     * <p>EXAMPLE:</p>
     * <p>@Action(name="Tests test1) public void testA()</p>
     * <p>In that case the actionMethod POJO field must be <strong>test1</strong> and not <strong>testA</strong></p>
     * **/
    private String   actionMethodName;
    private String   componentName;
    /**
     * Array of the fully qualified class name for each of the action method's arguments ( <strong>java.lang.String</strong>, <strong>com.product.model.User</strong>, etc )
     * */
    private String[] argumentsTypes;
    private String[] argumentsValues;

    public ActionPojo() {}

    public ActionPojo( int actionId, String componentName, String actionMethodName, String[] argumentsTypes,
                       String[] argumentsValues ) {

        this.actionId = actionId;
        this.componentName = componentName;
        this.actionMethodName = actionMethodName;
        this.argumentsTypes = argumentsTypes;
        this.argumentsValues = argumentsValues;
    }

    public int getActionId() {

        return actionId;
    }

    public void setActionId( int actionId ) {

        this.actionId = actionId;
    }

    public String getComponentName() {

        return componentName;
    }

    public void setComponentName( String componentName ) {

        this.componentName = componentName;
    }

    public String getActionMethodName() {

        return actionMethodName;
    }

    public void setActionMethodName( String actionMethodName ) {

        this.actionMethodName = actionMethodName;
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

}
