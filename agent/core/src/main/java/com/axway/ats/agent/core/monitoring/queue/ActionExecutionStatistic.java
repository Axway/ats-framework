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
package com.axway.ats.agent.core.monitoring.queue;

import java.io.Serializable;

/**
 * Keeps info about one action execution results
 */
public class ActionExecutionStatistic implements Serializable {

    private static final long serialVersionUID = 1L;

    private String            actionName;

    private int               numberPassed     = 0;
    private int               numberFailed     = 0;

    public ActionExecutionStatistic( String actionName ) {

        this.actionName = actionName;
    }

    public String getActionName() {

        return actionName;
    }

    public int getNumberPassed() {

        return numberPassed;
    }

    public int getNumberFailed() {

        return numberFailed;
    }

    /**
     * Registers the action execution result.
     * 
     * This method is called on the agent side. 
     * Must be synchronized by the calling outer method.
     * 
     * @param passed
     */
    public void registerExecutionResult(
                                         boolean passed ) {

        if( passed ) {
            this.numberPassed++;
        } else {
            this.numberFailed++;
        }
    }

    /**
     * Here we are merge the info between same action
     * run by same queue on different agents
     * 
     * This method is called on the Test Executor side
     * 
     * @param that
     */
    public void merge(
                       ActionExecutionStatistic that ) {

        this.numberPassed += that.numberPassed;
        this.numberFailed += that.numberFailed;
    }
}
