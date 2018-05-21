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

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel( value = "Action details")
public class ActionPojo {

    /** 
     * the session ID string 
     * **/
    @ApiModelProperty( required = true, value = "sessionId", example = "1")
    private String   sessionId;
    
    /**
     * The resource id that will identify the action class that have execute the sprecified method from this pojo
     *  **/
    @ApiModelProperty( required = true, value = "resourceId", example = "1", notes = "When calling initialize, "
            + "this property may be skipped. Evety other call must contains in though")
    private int      resourceId;

    @ApiModelProperty( required = true, value = "componentName", example = "ftp-actions")
    private String   componentName;
    /**
     * the value of the @Action's annotation name parameter, NOT the actual Java method name
     * Example:
     * @Action (name="FTP actions connect" ...)
     * public void connect(...)
     * In that situation the methodName must be "FTP actions connect"
     *  **/
    @ApiModelProperty( required = true, value = "methodName", example = "Ftp Actions connect")
    private String   methodName;
    
    /**
     * the fully qualified names of each of the arguments Java classes
     *  **/
    @ApiModelProperty( required = true, value = "argumentsTypes", example = "[java.lang.String, com.custom.package.User]")
    private String[] argumentsTypes;
    /**
     * the values of each argument.
     * Those values will be casted to the Java classes, specified in the argumentsTypes class member
     *  **/
    @ApiModelProperty( required = true, value = "argumentsValues", example = "['test', '{'name':'AtsUser'}']")
    private String[] argumentsValues;

    public ActionPojo() {}

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

}
