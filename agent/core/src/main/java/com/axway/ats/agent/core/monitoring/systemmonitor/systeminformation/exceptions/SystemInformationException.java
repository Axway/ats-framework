/*
 * Copyright 2020 Axway Software
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
package com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.exceptions;

public class SystemInformationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SystemInformationException() {

        super();
    }

    public SystemInformationException( String message ) {

        super(message);
    }

    public SystemInformationException( Throwable cause ) {

        super(cause);
    }

    public SystemInformationException( String message, Throwable cause ) {

        super(message, cause);
    }

}
