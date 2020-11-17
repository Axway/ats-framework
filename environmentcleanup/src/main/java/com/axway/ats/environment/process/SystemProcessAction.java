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
package com.axway.ats.environment.process;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.process.LocalProcessExecutor;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.environment.AdditionalAction;
import com.axway.ats.environment.EnvironmentCleanupException;

@PublicAtsApi
public class SystemProcessAction extends AdditionalAction {

    private static final Logger log = LogManager.getLogger(SystemProcessAction.class);

    private String              shellCommand;

    //the additional action description
    private final String        description;

    @PublicAtsApi
    public SystemProcessAction( String shellCommand, int sleepInterval ) {

        super(sleepInterval);

        this.shellCommand = shellCommand;
        this.description = " shell command '" + shellCommand + "'";
    }

    @Override
    protected void executeAction() throws EnvironmentCleanupException {

        try {
            log.debug("Executing additional action with shell command '" + shellCommand + "'");

            LocalProcessExecutor processExecutor = new LocalProcessExecutor(HostUtils.LOCAL_HOST_IPv4,
                                                                            shellCommand);
            processExecutor.execute();
        } catch (Exception e) {
            throw new EnvironmentCleanupException("Could not execute command '" + shellCommand + "'", e);
        }
    }

    @Override
    public String getDescription() {

        return description;
    }
}
