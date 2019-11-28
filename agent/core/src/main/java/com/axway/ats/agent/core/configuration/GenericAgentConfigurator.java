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
package com.axway.ats.agent.core.configuration;

import com.axway.ats.log.autodb.io.SQLServerDbWriteAccess;
import com.axway.ats.log.model.CheckpointLogLevel;

/**
 * This configurator is used for various configurations of Agent which do not need reload of the Agent components
 */
@SuppressWarnings( "serial")
public class GenericAgentConfigurator implements Configurator {

    private CheckpointLogLevel checkpointLogLevel;

    public GenericAgentConfigurator() {

    }

    @Override
    public void apply() {

        /* Set the checkpoint log level. We have 2 options to apply it:
         *  1. Send event using the logging system.
         *      good - the event will be processed in the order it was received. So for
         *          example if the Agent queue has some pending checkpoint events
         *          from previous test, and we decide to change to checkpoint log level
         *          for the current test, we change the log level right between both test cases
         *      bad - if the Agent queue has some pending checkpoint events and we add
         *          the checkpoint log level set event, but we get errors processing any log
         *          event before the new checkpoint level is processed, the event queue will 
         *          stop working and this important event will be lost. Then the next test will 
         *          restore the log queue, but the important event will not recover
         *  2. Immediately set the log level in a static field
         *      good - it will be applied for sure
         *      bad - if the Agent queue has some pending checkpoint events from 
         *          last test case and we want to change the checkpoint log level between
         *          both test cases, the log level change will not happen exactly between
         *          both test cases. This case is very rare because:
         *              - usually our customers use same checkpoint log level for the whole run 
         *              - in many cases the events queue will not have pending events
         */
        if (this.checkpointLogLevel != null) {
            SQLServerDbWriteAccess.setCheckpointLogLevel(checkpointLogLevel);
        }
    }

    @Override
    public boolean needsApplying() {

        return true;
    }

    @Override
    public void revert() {

        // Currently this method must do nothing as each call to a setXYZ() method first calls this revert() method
        // which indeed wipes out any previous changes. This way only the last set operation is applied
    }

    /**
     * Set the checkpoint log level
     * 
     * @param checkpointLogLevel
     */
    public void setCheckpointLogLevel( CheckpointLogLevel checkpointLogLevel ) {

        this.checkpointLogLevel = checkpointLogLevel;
    }

    @Override
    public String getDescription() {

        return "generic Agent configuration";
    }
}
