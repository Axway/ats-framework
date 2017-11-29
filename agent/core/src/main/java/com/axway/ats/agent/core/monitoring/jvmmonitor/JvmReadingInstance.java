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
package com.axway.ats.agent.core.monitoring.jvmmonitor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.axway.ats.common.performance.monitor.beans.SharedReadingBean;

/**
 * The JVM bean lives in the environment of the monitoring service only.
 */
public abstract class JvmReadingInstance extends SharedReadingBean {

    private static final long          serialVersionUID    = 1L;

    protected MBeanServerConnection    connection;

    protected ObjectName               mBeanName;
    protected String                   attName;

    private boolean                    newInstanceFlag;

    // format the given float CPU usage values, output 4 digits after decimal point
    // which are later multiplied by 100
    private static final DecimalFormat CPU_USAGE_FORMATTER = new DecimalFormat( "#.####" );

    static {
        CPU_USAGE_FORMATTER.setDecimalFormatSymbols( new DecimalFormatSymbols( Locale.US ) );
    }

    public JvmReadingInstance( MBeanServerConnection connection,
                               String dbId,
                               String monitorClass,
                               String name,
                               String unit,
                               float normalizationFactor ) {

        super( monitorClass, name, unit, normalizationFactor );

        this.dbId = Integer.parseInt( dbId );
        this.connection = connection;

        setParameters( parameters );

        newInstanceFlag = true;

        init();
    }

    public boolean isNewInstance() {

        return newInstanceFlag;
    }

    public void setNewInstanceFlag(
                                    boolean newInstance ) {

        this.newInstanceFlag = newInstance;
    }

    public void init() throws RuntimeException {

    }

    /**
     * @return the new polled value
     * @throws Exception
     */
    abstract public float poll() throws Exception;

}
