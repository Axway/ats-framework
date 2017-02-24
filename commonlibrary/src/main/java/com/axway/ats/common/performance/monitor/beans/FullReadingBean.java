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
package com.axway.ats.common.performance.monitor.beans;

import java.util.HashMap;
import java.util.Map;

import com.axway.ats.common.PublicAtsApi;

/**
 * A full reading info.
 * The monitor sends such reading back to the monitoring library only
 * the first time this reading is polled. From that point on only
 * Basic reading bean is returned
 */
@PublicAtsApi
public class FullReadingBean extends BasicReadingBean {

    private static final long     serialVersionUID = 1L;

    protected String              monitorClass;

    protected String              name;
    protected String              unit;
    protected Map<String, String> parameters       = new HashMap<String, String>();

    /**
     * The DB id under which this reading is persisted in the logging DB. 
     */
    protected int                 dbId;

    protected boolean             dynamicReading;

    @PublicAtsApi
    public FullReadingBean() {

    }

    @PublicAtsApi
    public FullReadingBean( String monitorClass,
                            String name,
                            String unit ) {

        this.monitorClass = monitorClass;
        this.name = name;
        this.unit = unit;
    }

    /**
     * A utility method which can be used in custom monitors
     * 
     * @return a basic reading bean with set ID
     */
    public BasicReadingBean getBasicReadingBeanCopy() {

        BasicReadingBean basicBean = new BasicReadingBean();
        basicBean.id = this.id;
        return basicBean;
    }

    public FullReadingBean getNewCopy() {

        FullReadingBean newBean = new FullReadingBean();

        // the basic bean info
        newBean.id = this.id;
        newBean.value = this.value;

        // the full bean info
        newBean.monitorClass = this.monitorClass;
        newBean.name = this.name;
        newBean.unit = this.unit;
        newBean.parameters = this.parameters;
        newBean.dbId = this.dbId;
        newBean.dynamicReading = this.dynamicReading;

        return newBean;
    }

    @PublicAtsApi
    public String getMonitorName() {

        return monitorClass;
    }

    @PublicAtsApi
    public String getName() {

        return name;
    }

    @PublicAtsApi
    public void setName(
                         String name ) {

        this.name = name;
    }

    @PublicAtsApi
    public String getUnit() {

        return unit;
    }

    public boolean isDynamicReading() {

        return dynamicReading;
    }

    public void setDynamicReading(
                                   boolean dynamicReading ) {

        this.dynamicReading = dynamicReading;
    }

    public int getDbId() {

        return dbId;
    }

    public void setDbId(
                         int dbId ) {

        this.dbId = dbId;
    }

    public String getParameter(
                                String parameterName ) {

        if( this.parameters != null ) {
            return this.parameters.get( parameterName );
        } else {
            return null;
        }
    }

    public Map<String, String> getParameters() {

        return this.parameters;
    }

    public void setParameters(
                               Map<String, String> parameters ) {

        this.parameters = parameters;
    }

    @PublicAtsApi
    public void setValue(
                          String value ) {

        this.value = value;
    }

    public String getDescription() {

        return "'" + monitorClass + "' collects the '" + name + "' in '" + unit + "' units";
    }

    @Override
    public String toString() {

        return name;
    }

    @Override
    public boolean equals(
                           Object obj ) {

        if( obj instanceof FullReadingBean ) {
            FullReadingBean that = ( FullReadingBean ) obj;
            if( name.equals( that.name ) ) {
                if( parameters != null && that.parameters != null
                    && parameters.toString().equals( that.parameters.toString() ) ) {
                    return true;
                }
            }
        }
        return false;
    }
}
