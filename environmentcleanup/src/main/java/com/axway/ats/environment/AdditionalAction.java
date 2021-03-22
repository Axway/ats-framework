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
package com.axway.ats.environment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;

@PublicAtsApi
public abstract class AdditionalAction {

    private final Logger log;

    private int          sleepInterval;

    public AdditionalAction() {

        this(0);
    }

    public AdditionalAction( int sleepInterval ) {

        this.sleepInterval = sleepInterval;
        this.log = LogManager.getLogger(this.getClass());
    }

    /**
     * @return  the sleep interval after the action is executed
     */
    public final int getSleepInterval() {

        return sleepInterval;
    }

    /**
     * Execute the given additional action
     *
     * @throws EnvironmentCleanupException
     */
    public final void execute() throws EnvironmentCleanupException {

        //execute the additional action
        executeAction();

        log.debug("Successfully executed " + getDescription());
    }

    protected abstract void executeAction() throws EnvironmentCleanupException;

    /**
     * The action description.
     *
     * Note that we expect all different actions to provide different descriptions.
     * This is used as a unique map key
     * @return
     */
    public abstract String getDescription();
}
