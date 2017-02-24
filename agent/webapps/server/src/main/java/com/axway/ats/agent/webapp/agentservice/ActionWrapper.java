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
package com.axway.ats.agent.webapp.agentservice;

import java.util.ArrayList;
import java.util.List;

public class ActionWrapper {

    private String                componentName;
    private String                actionName;
    private List<ArgumentWrapper> args    = new ArrayList<ArgumentWrapper>();

    public String getComponentName() {

        return componentName;
    }

    public void setComponentName(
                                  String componentName ) {

        this.componentName = componentName;
    }

    public String getActionName() {

        return actionName;
    }

    public void setActionName(
                               String actionName ) {

        this.actionName = actionName;
    }

    public List<ArgumentWrapper> getArgs() {

        return args;
    }

    public void setArgs(
                         List<ArgumentWrapper> args ) {

        this.args = args;
    }
}
