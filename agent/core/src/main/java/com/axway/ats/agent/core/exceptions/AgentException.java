/*
 * Copyright 2017-2020 Axway Software
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

import com.axway.ats.core.utils.StringUtils;

@SuppressWarnings( "serial")
public class AgentException extends Exception {

    public AgentException() {

        super();
    }

    public AgentException( Throwable cause ) {

        super(cause);
    }

    public AgentException( String message ) {

        super(StringUtils.escapeNonPrintableAsciiCharacters(message));
    }

    public AgentException( String message,
                           Throwable cause ) {

        super(StringUtils.escapeNonPrintableAsciiCharacters(message), cause);
    }
}
