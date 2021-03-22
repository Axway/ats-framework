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

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;

public class WrongActionClass {

    @Action(name = "action duplicate")
    public void actionDup1( @Parameter(name = "value") int value ) {

        LogManager.getLogger( WrongActionClass.class )
              .info( ( Object ) "Method action duplicate has been executed" );
    }

    @Action(name = "action duplicate")
    public void actionDup2( @Parameter(name = "value") int value ) {

        LogManager.getLogger( FirstActionClass.class )
              .info( ( Object ) "Method action duplicate has been executed" );
    }
}
