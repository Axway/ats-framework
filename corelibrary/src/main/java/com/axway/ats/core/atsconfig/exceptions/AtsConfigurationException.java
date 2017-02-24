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

public class AtsConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 
     * @param message the exception message
     */
    public AtsConfigurationException( String message ) {

        super( message );
    }

    /**
     * 
     * @param cause the cause
     */
    public AtsConfigurationException( Throwable cause ) {

        super( cause );
    }

    /**
     * 
     * @param message the exception message
     * @param cause the cause
     */
    public AtsConfigurationException( String message,
                                      Throwable cause ) {

        super( message, cause );
    }

}
