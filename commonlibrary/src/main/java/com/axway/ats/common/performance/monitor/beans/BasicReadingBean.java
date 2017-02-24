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

import com.axway.ats.common.PublicAtsApi;

/**
 * A reading bean with which is used when the monitoring library
 * already knows what this reading is about. It is detected
 * by its unique id
 */
@PublicAtsApi
public class BasicReadingBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 
     * A unique reading id.
     *  
     * This id is passed all the time between Test Executor and Agent in order to identify the reading bean.
     * This id is not the actual DB id, the Test Executor maintains a mapping between "reading id <-> DB id"
     */
    protected String          id;

    /**
     * The bean value
     */
    protected String          value;

    @PublicAtsApi
    public BasicReadingBean() {

    }

    @PublicAtsApi
    public BasicReadingBean( String id,
                             String value ) {

        this.id = id;
        this.value = value;
    }

    public BasicReadingBean getNewCopy() {

        BasicReadingBean newBean = new BasicReadingBean();
        newBean.id = this.id;
        newBean.value = this.value;

        return newBean;
    }

    public String getId() {

        return id;
    }

    public void setId(
                       String id ) {

        this.id = id;
    }

    public String getValue() {

        return value;
    }

    @PublicAtsApi
    public void setValue(
                          String value ) {

        this.value = value;
    }

    @Override
    public String toString() {

        return "'" + id + "' with value '" + value + "'";
    }
}
