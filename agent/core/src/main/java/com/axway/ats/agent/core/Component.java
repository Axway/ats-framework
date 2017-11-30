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

import java.util.ArrayList;
import java.util.List;

public class Component {

    private String                     componentName;
    private ComponentActionMap         componentActionMap;
    private List<ComponentEnvironment> componentEnvironments;

    public Component( String componentName ) {

        this.componentName = componentName;
    }

    public String getComponentName() {

        return componentName;
    }

    public ComponentActionMap getActionMap() {

        return componentActionMap;
    }

    public void setActionMap(
                              ComponentActionMap componentActionMap ) {

        this.componentActionMap = componentActionMap;
    }

    public List<ComponentEnvironment> getEnvironments() {

        return componentEnvironments;
    }

    public void setEnvironments(
                                 List<ComponentEnvironment> componentEnvironments ) {

        this.componentEnvironments = componentEnvironments;
    }

    public ComponentEnvironment getEnvironment(
                                                String environmentName ) {

        if (componentEnvironments != null) {
            for (ComponentEnvironment componentEnvironment : componentEnvironments) {
                if (componentEnvironment.getEnvironmentName().equals(environmentName)) {
                    return componentEnvironment;
                }
            }
        }
        return null;
    }

    public Component getNewCopy() {

        Component newComponent = new Component(this.componentName);

        newComponent.componentActionMap = this.componentActionMap.getNewCopy();

        newComponent.componentEnvironments = new ArrayList<ComponentEnvironment>();
        if (componentEnvironments != null) {
            for (ComponentEnvironment environment : componentEnvironments) {
                newComponent.componentEnvironments.add(environment.getNewCopy());
            }
        }

        return newComponent;
    }
}
