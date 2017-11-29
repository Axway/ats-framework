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
package com.axway.ats.agent.core;

import com.axway.ats.environment.EnvironmentCleanupException;
import com.axway.ats.environment.process.SystemProcessAction;

public class MockSystemProcessAction extends SystemProcessAction {

    private boolean executed = false;

    public MockSystemProcessAction( String shellCommand,
                                    int sleepInterval ) {

        super( shellCommand, sleepInterval );
    }

    public boolean isExecuted() {

        return this.executed;
    }

    @Override
    protected void executeAction() throws EnvironmentCleanupException {

        executed = true;
    }
}
