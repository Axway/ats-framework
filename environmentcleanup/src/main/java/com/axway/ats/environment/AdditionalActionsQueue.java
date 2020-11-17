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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This singleton instance keeps all the additional actions that are pending after restoring 
 * an environment unit.<br>
 * When all environment units have been restored, this instance is used to run all the
 * additional actions.<br>
 * Just one run of each action is executed even when the same action is requested from more than one environment units.<br>
 * After the execution of all actions, it sleeps for the longest sleep interval.
 */
public class AdditionalActionsQueue {

    private static final Logger           log = LogManager.getLogger(AdditionalActionsQueue.class);

    private static AdditionalActionsQueue instance;

    private Map<String, AdditionalAction> actionsMap;
    private Map<String, String>           actionParentsMap;

    private AdditionalActionsQueue() {

        this.actionsMap = new HashMap<String, AdditionalAction>();
        this.actionParentsMap = new HashMap<String, String>();
    }

    public static synchronized AdditionalActionsQueue getInstance() {

        if (instance == null) {
            instance = new AdditionalActionsQueue();
        }
        return instance;
    }

    /**
     * Add an action into the queue, if already in - it overrides the previous action instance.
     * 
     * @param additionalAction the action to add
     * @param environmentUnitDescription description of the environment unit this action belongs to. 
     * Used for better user messaging.
     */
    public void addActionToQueue(
                                  AdditionalAction additionalAction,
                                  String environmentUnitDescription ) {

        String actionIdentifier = additionalAction.getDescription();
        this.actionsMap.put(actionIdentifier, additionalAction);

        String actionParents = this.actionParentsMap.get(actionIdentifier);
        if (actionParents == null) {
            actionParents = "";
        } else {
            actionParents += ", ";
        }
        this.actionParentsMap.put(actionIdentifier, actionParents + "'" + environmentUnitDescription + "'");

        log.debug("Scheduled the execution of additional action '" + actionIdentifier + "' with "
                  + additionalAction.getSleepInterval() + " seconds sleep");
    }

    /**
     * Execute the collected actions and clean the queue
     * @throws EnvironmentCleanupException
     */
    public void flushAllActions() throws EnvironmentCleanupException {

        if (actionsMap.size() == 0) {
            return;
        }

        log.debug("Executing all scheduled additional actions");

        // Most actions have a sleep interval which must be honored after its execution.
        // We first execute all actions and then sleep for the longest sleep interval.
        int maxSleepInterval = 0;
        try {
            for (Entry<String, AdditionalAction> actionEntry : actionsMap.entrySet()) {
                log.debug("Executing additional action due to the restore of environment unit(s): "
                          + this.actionParentsMap.get(actionEntry.getKey()));

                AdditionalAction action = actionEntry.getValue();
                action.execute();

                if (action.getSleepInterval() > maxSleepInterval) {
                    maxSleepInterval = action.getSleepInterval();
                }
            }
        } finally {
            this.actionsMap.clear();
            this.actionParentsMap.clear();
        }

        if (maxSleepInterval > 0) {
            log.debug("Sleeping for " + maxSleepInterval
                      + " second(s) after the execution of all additional actions");

            //sleep for the given amount of seconds
            try {
                Thread.sleep(maxSleepInterval * 1000);
            } catch (InterruptedException ie) {
                log.debug("Interrupted exception caught");
            }
        }
    }
}
