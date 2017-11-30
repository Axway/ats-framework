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
package com.axway.ats.core.validation.exceptions;

/**
 * This type of {@link Exception} is used to collect all the
 * information about the specific validation error, so that
 * it can later on present it in a friendly manner.
 */
@SuppressWarnings( "serial")
public class TypeException extends Exception {

    String parameterName = null;

    /**
     * Constructor
     * @param errorMessage the message of the specific validation error
     * @param parameter the name of the parameter whose validation failed
     */
    public TypeException( String errorMessage,
                          String parameter ) {

        super(errorMessage);
        this.parameterName = parameter;
    }

    /**
     * Constructor
     * @param errorMessage the message of the specific validation error
     * @param parameter the name of the parameter whose validation failed
     * @param cause the cause of the exception
     */
    public TypeException( String errorMessage,
                          String parameter,
                          Throwable cause ) {

        super(errorMessage, cause);
        this.parameterName = parameter;
    }

    /**
     * @return the {@link String} value of the parameter name
     */
    public String getParameterName() {

        return this.parameterName;
    }
}
