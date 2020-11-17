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

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * This exception will be thrown whenever an exception occurs during action execution
 * It will hold the simple name of the exception thrown, as well as the message
 */
@SuppressWarnings( "serial")
public class InternalComponentException extends AgentException {

    private String hostIp;
    private String componentName;
    private String actionName;
    private String exceptionMessage;

    // This constructor is called on the Agent side
    public InternalComponentException( String componentName, String actionName, Throwable cause ) {

        super("Exception during execution of action '" + actionName + "' for component '" + componentName
              + "' " + cause.getMessage(), cause);

        this.componentName = componentName;
        this.actionName = actionName;
        this.exceptionMessage = StringUtils.escapeNonPrintableAsciiCharacters(getStackTrace(cause));

        // remember the Agent host
        this.hostIp = HostUtils.getLocalHostIP();
    }

    // this constructor is called on the Test Executor side
    public InternalComponentException( String componentName, String actionName, String exceptionMessage,
                                       String hostIp ) {

        super("Exception during execution of action '" + actionName + "' for component '" + componentName
              + "' at " + hostIp + "\n" + exceptionMessage);

        this.componentName = componentName;
        this.actionName = actionName;
        this.exceptionMessage = StringUtils.escapeNonPrintableAsciiCharacters(exceptionMessage);
    }

    /**
     * Extract nested exception stack trace and return it to formated string
     * @param th the exception cause
     */
    private static String getStackTrace(
                                         Throwable th ) {

        Throwable exc = th;
        // StringBuilder containing the formated exception stack trace
        StringBuilder causes = new StringBuilder();
        // list containing StringBuilders with all exception causes
        List<StringBuilder> causesList = new ArrayList<StringBuilder>();

        if (exc == null) {
            return causes.toString();
        }

        // append the machine IP, where the exception happened
        causes.append("\n\t[" + HostUtils.getLocalHostIP() + " error]\n");

        // if chained agents are used this line could already be appended, so we check it
        String locationErrorMsg = "\n[" + HostUtils.getLocalHostIP() + " stacktrace]";
        String formattedExceptionMsg = exc.getMessage();
        if( StringUtils.isNullOrEmpty( formattedExceptionMsg ) ) {
            formattedExceptionMsg = "<No message details on this exception>";
        }
        
        if (!formattedExceptionMsg.trim().endsWith(locationErrorMsg)) {
            formattedExceptionMsg = formatMessage(formattedExceptionMsg + locationErrorMsg);
        } else {
            formattedExceptionMsg = formatMessage(formattedExceptionMsg);
        }

        // append the exception type and the formated exception message 
        causes.append( "\t" )
              .append( exc.getClass().getName() )
              .append( ": " )
              .append( formattedExceptionMsg );

        do {
            for (StackTraceElement sck : exc.getStackTrace()) {
                causes.append("\t").append(sck).append("\n");
                if (sck.toString().startsWith("com.sun.xml.ws")) {
                    break;
                }
            }
            if (exc == exc.getCause() || exc.getCause() == null) {
                causesList.add(causes);
                break;
            } else {
                causesList.add(causes);
                exc = exc.getCause();
                causes = new StringBuilder();
                causes.append("\tCaused by \n\t" + exc.getClass().getName() + ": " + exc.getMessage() + "\n");
            }

        } while (true);

        return reduceStackTraceLenth(causesList);
    }

    /**
     * format the exception message
     */
    private static String formatMessage(
                                         String exceptionMessage ) {

        StringBuilder formatedMessage = new StringBuilder();
        for (String row : exceptionMessage.split("\n")) {
            formatedMessage.append(row).append("\n").append("\t");
        }

        // remove the last '\t'
        formatedMessage.deleteCharAt(formatedMessage.length() - 1);

        return formatedMessage.toString();
    }

    /**
     * remove the common parts from the exception
     */
    private static String reduceStackTraceLenth(
                                                 List<StringBuilder> causesList ) {

        StringBuilder reducedStackTrace = new StringBuilder();
        
        for (StringBuilder cause : causesList) {
            reducedStackTrace.append(cause);
        }
        
        return reducedStackTrace.toString();
    }

    public String getComponentName() {

        return componentName;
    }

    public String getActionName() {

        return actionName;
    }

    public String getExceptionMessage() {

        return exceptionMessage;
    }

    public String getHostIp() {

        return hostIp;
    }
}
