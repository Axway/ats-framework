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
package com.axway.ats.agent.webapp.restservice.model.pojo;

public class ScheduleCustomJvmMonitoringPojo extends BasePojo {

    private String   jmxPort;
    private String   alias;
    private String   mbeanName;
    private String   unit;
    private String[] mbeanAttributes;

    public ScheduleCustomJvmMonitoringPojo() {

    }

    public ScheduleCustomJvmMonitoringPojo( String jmxPort,
                                            String alias,
                                            String mbeanName,
                                            String unit,
                                            String[] mbeanAttributes ) {

        this.jmxPort = jmxPort;
        this.alias = alias;
        this.mbeanName = mbeanName;
        this.unit = unit;
        this.mbeanAttributes = mbeanAttributes;
    }

    public String getJmxPort() {

        return jmxPort;
    }

    public void setJmxPort(
                            String jmxPort ) {

        this.jmxPort = jmxPort;
    }

    public String getAlias() {

        return alias;
    }

    public void setAlias(
                          String alias ) {

        this.alias = alias;
    }

    public String getMbeanName() {

        return mbeanName;
    }

    public void setMbeanName(
                              String mbeanName ) {

        this.mbeanName = mbeanName;
    }

    public String getUnit() {

        return unit;
    }

    public void setUnit(
                         String unit ) {

        this.unit = unit;
    }

    public String[] getMbeanAttributes() {

        return mbeanAttributes;
    }

    public void setMbeanAttributes(
                                    String[] mbeanAttributes ) {

        this.mbeanAttributes = mbeanAttributes;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("JmxPort: " + this.jmxPort + ", ")
          .append("Alias: " + this.alias + ", ")
          .append("MBeanName: " + this.mbeanName + ", ")
          .append("Unit: " + this.unit + ", ")
          .append("MBeanAttributes" + this.mbeanAttributes);

        return sb.toString();
    }

}
