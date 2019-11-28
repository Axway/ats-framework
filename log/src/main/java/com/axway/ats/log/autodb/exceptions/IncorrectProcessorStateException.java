/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.log.autodb.exceptions;

import com.axway.ats.log.autodb.logqueue.LifeCycleState;

@SuppressWarnings( "serial")
public class IncorrectProcessorStateException extends LoggingException {

    public IncorrectProcessorStateException( String message ) {

        super(message);
    }

    public IncorrectProcessorStateException( String message,
                                             LifeCycleState expectedState,
                                             LifeCycleState actualState ) {

        super(message + " expected state " + expectedState + ", actual state " + actualState);
    }
}
