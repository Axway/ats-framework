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

import java.util.List;

import com.axway.ats.agent.core.action.ActionMethod;
import com.axway.ats.common.systemproperties.AtsSystemProperties;

@SuppressWarnings( "serial")
public class NoCompatibleMethodFoundException extends AgentException {

    private static final String LINE_SEPARATOR = AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

    //variable to hold the exception message
    private String              message;

    public NoCompatibleMethodFoundException( String message,
                                             Class<?> argTypes[],
                                             List<ActionMethod> actionMethods,
                                             String componentName,
                                             String actionName ) {

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(message);
        messageBuilder.append(", more information below:" + LINE_SEPARATOR);
        messageBuilder.append("action name '" + actionName + "', component name '" + componentName + "'"
                              + LINE_SEPARATOR);
        messageBuilder.append("argument types: { ");
        for (Class<?> argType : argTypes) {
            if (argType != null) {
                messageBuilder.append(argType.getName());
            } else {
                messageBuilder.append("null");
            }
            messageBuilder.append(",");
        }
        messageBuilder.delete(messageBuilder.length() - 1, messageBuilder.length());
        messageBuilder.append(" }" + LINE_SEPARATOR);
        messageBuilder.append("available implementing methods:" + LINE_SEPARATOR);

        for (ActionMethod actionMethod : actionMethods) {
            messageBuilder.append(actionMethod.getMethod().toString());
            messageBuilder.append(LINE_SEPARATOR);
        }

        this.message = messageBuilder.toString();
    }

    @Override
    public String getMessage() {

        return message;
    }
}
