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
package com.axway.ats.agent.core.ant.component.agenttest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.ant.component.agenttest.SecondActionClass;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.core.validation.ValidationType;

public class SecondActionClass {

    public static int ACTION_VALUE = 0;

    @Action(name = "action 2")
    public void action1( @Parameter(name = "value") int value ) {

        ACTION_VALUE = value;
        LogManager.getLogger( SecondActionClass.class ).info( ( Object ) "Method action 1 has been executed" );
    }

    @Action(name = "action checked exception")
    public void actionCheckedException() throws Exception {

        throw new Exception( "Message - checked exception " );
    }

    @Action(name = "action unchecked exception")
    public void actionUncheckedException() {

        throw new RuntimeException( "Message - unchecked exception " );
    }

    @Action(name = "Create User")
    public void createUser( @Parameter(name = "emailAddress", validation = ValidationType.STRING_EMAIL_ADDRESS) String emailAddress ) {

        throw new RuntimeException( "Message - unchecked exception " );
    }
}
