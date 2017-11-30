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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.axway.ats.common.PublicAtsApi;

/**

* A full reading info.

* It contains all reading info.

*/
@PublicAtsApi
public class ReadingBean implements Serializable {

    private static final long     serialVersionUID = 1L;

    protected String              monitorClass;

    protected String              name;
    protected String              unit;
    protected Map<String, String> parameters       = new HashMap<String, String>();

    /**
     * The id that is assigned to this ReadingBean from the ReadingsRepository
     * */
    protected int                 id;

    /**
     * The DB id under which this reading is persisted in the logging DB. 
     * The default value is -1, which means that this ReadingBean is not populated to DB
     */
    protected int                 dbId             = -1;

    protected boolean             dynamicReading;

    /**
     * The bean value
     */
    protected String              value;

    @PublicAtsApi
    public ReadingBean() {

    }

    @PublicAtsApi
    public ReadingBean( String monitorClass,
                        String name,
                        String unit ) {

        this.monitorClass = monitorClass;
        this.name = name;
        this.unit = unit;
    }

    public ReadingBean getNewCopy() {

        ReadingBean newBean = new ReadingBean();

        newBean.value = this.value;
        newBean.monitorClass = this.monitorClass;
        newBean.name = this.name;
        newBean.unit = this.unit;
        newBean.parameters = this.parameters;
        newBean.dbId = this.dbId;
        newBean.id = this.id;
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

    public int getId() {

        return id;
    }

    public void setId(
                       int id ) {

        this.id = id;
    }

    public String getParameter(
                                String parameterName ) {

        if (this.parameters != null) {
            return this.parameters.get(parameterName);
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
    public String getValue() {

        return value;
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

        return "'" + id + "' with value '" + value + "'";
    }

    @Override
    public boolean equals(
                           Object obj ) {

        if (obj instanceof ReadingBean) {
            ReadingBean that = (ReadingBean) obj;
            if (name.equals(that.name)) {
                if (parameters != null && that.parameters != null
                    && parameters.toString().equals(that.parameters.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

}
