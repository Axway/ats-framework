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
package com.axway.ats.core.atsconfig.exceptions;

public class AtsManagerException extends Exception {

    private static final long serialVersionUID = 1L;

    private String            commandStdout;
    private String            commandStderr;

    /**
     * 
     * @param message the exception message
     */
    public AtsManagerException( String message ) {

        super(message);
    }

    /**
     * 
     * @param message the exception message
     * @param cause the cause
     */
    public AtsManagerException( String message,
                                Throwable cause ) {

        super(message, cause);
    }

    /**
     * 
     * @param message the exception message
     * @param commandStdout command STDOUT content
     * @param commandStderr command STDERR content
     */
    public AtsManagerException( String message,
                                String commandStdout,
                                String commandStderr ) {

        super(message);
        this.commandStdout = commandStdout;
        this.commandStderr = commandStderr;
    }

    /**
     * 
     * @return command STDOUT content
     */
    public String getCommandStdout() {

        return commandStdout;
    }

    /**
     * 
     * @return command STDERR content
     */
    public String getCommandStderr() {

        return commandStderr;
    }

    @Override
    public void printStackTrace() {

        super.printStackTrace();

        if (commandStdout != null || commandStderr != null) {

            System.err.println("\nCommand execution output:");
            if (commandStdout != null) {
                System.err.println("STDOUT: " + commandStdout);
            }
            if (commandStderr != null) {
                System.err.println("STDERR: " + commandStderr);
            }
        }
    }

}
