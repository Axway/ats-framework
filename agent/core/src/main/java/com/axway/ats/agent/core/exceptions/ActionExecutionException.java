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

/**
 * Exception to be thrown if something goes wrong during
 * execution of an action. If this exception is thrown, it
 * means that the action was found, but an error occurred
 * during its execution - this could be a problem with passing
 * the action arguments or instantiating the action class.
 */
@SuppressWarnings( "serial")
public class ActionExecutionException extends AgentException {

    public ActionExecutionException( String message ) {

        super(message);
    }

    public ActionExecutionException( String message,
                                     Throwable cause ) {

        super(message, cause);
    }
}
