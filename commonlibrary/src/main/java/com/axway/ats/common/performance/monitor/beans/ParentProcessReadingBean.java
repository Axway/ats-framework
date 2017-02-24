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

/**
 * A bean containing info about a virtual process, which acts as a parent of other processes.
 * It is intended to get a picture of a whole product which is run as many single processes
 * 
 * This type of beans are created an the service side and are send just once to the 
 * monitoring client in the same way as a regular FullReadingBean is
 */
public class ParentProcessReadingBean extends FullReadingBean {

    private static final long serialVersionUID = 1L;

    private boolean           newInstanceFlag  = true;

    private String            nameOfThisParentProcess;
    private double            currentValue;

    private ParentProcessReadingBean() {

    }

    public ParentProcessReadingBean( String id,
                                     String monitorClass,
                                     String nameOfThisParentProcess,
                                     String name,
                                     String unit ) {

        super( monitorClass, name, unit );

        this.id = id;
        this.nameOfThisParentProcess = nameOfThisParentProcess;
    }

    public String getTheNameOfThisParentProcess() {

        return nameOfThisParentProcess;
    }

    public ParentProcessReadingBean getNewCopy() {

        ParentProcessReadingBean newBean = new ParentProcessReadingBean();

        // the basic bean info
        newBean.id = this.id;
        newBean.value = this.value;

        // the full bean info
        newBean.monitorClass = this.monitorClass;
        newBean.name = this.name;
        newBean.unit = this.unit;
        newBean.dbId = this.dbId;
        newBean.dynamicReading = this.dynamicReading;

        // the parent process bean info
        newBean.nameOfThisParentProcess = nameOfThisParentProcess;

        return newBean;
    }

    /**
     * @return the current value
     */
    public float poll() {

        return new Float( currentValue ).floatValue();
    }

    /**
     * Increment the current value by providing the new value of a child process
     * @param newValue
     */
    public void addValue(
                          float newValue ) {

        // we filter out negative valuesnewValue
        if( newValue > 0 ) {
            currentValue += newValue;
        }
    }

    /**
     * We must cleanup the internal value prior to iterating the
     * children processes
     */
    public void resetValue() {

        currentValue = 0;
    }

    public boolean isNewInstance() {

        return newInstanceFlag;
    }

    public void setNewInstanceFlag(
                                    boolean newInstance ) {

        this.newInstanceFlag = newInstance;
    }
}
