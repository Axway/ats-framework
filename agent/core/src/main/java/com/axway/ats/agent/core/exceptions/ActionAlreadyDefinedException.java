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
package com.axway.ats.agent.core.exceptions;

import java.lang.reflect.Method;

/**
 * Exception throws during component registration if the aciton
 * which is currently being registered has already been defined.
 * The exception provides information of the implementing method.
 */
@SuppressWarnings( "serial")
public class ActionAlreadyDefinedException extends AgentException {

    private String message;

    public ActionAlreadyDefinedException( String actionName,
                                          String componentName,
                                          Method implemenationMethod ) {

        super();

        StringBuilder sb = new StringBuilder();
        sb.append("Action '" + actionName + "' has already been defined for component ");
        sb.append(componentName);
        sb.append(" - implementation is ");
        sb.append(implemenationMethod.getDeclaringClass().getName());
        sb.append(".");
        sb.append(implemenationMethod.getName());

        message = sb.toString();
    }

    @Override
    public String getMessage() {

        return message;
    }
}
