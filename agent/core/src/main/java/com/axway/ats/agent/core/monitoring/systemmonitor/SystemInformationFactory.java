/*
 * Copyright 2020 Axway Software
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
package com.axway.ats.agent.core.monitoring.systemmonitor;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.ISystemInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.oshi.OshiSystemInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.sigar.SigarSystemInformation;
import com.axway.ats.common.systemproperties.AtsSystemProperties;

/**
 * Use this class to obtain {@link ISystemInformation} instance.</br>
 * */
public class SystemInformationFactory {

    private static final Logger LOG               = Logger.getLogger(SystemInformationFactory.class);

    public static final String  SIGAR_SYSTEM_INFO = SigarSystemInformation.class.getName();
    public static final String  OSHI_SYSTEM_INFO  = OshiSystemInformation.class.getName();

    /**
     * Depending on the System property {@link AtsSystemProperties.SYSTEM_INFORMATION_CLASS}, a {@link ISystemInformation} will be returned.</br>
     * */
    public synchronized static ISystemInformation get() {

        String className = AtsSystemProperties.getPropertyAsString(AtsSystemProperties.SYSTEM_INFORMATION_CLASS,
                                                                   SIGAR_SYSTEM_INFO);

        LOG.info("System information class is: '" + className + "'");

        if (className.equals(SIGAR_SYSTEM_INFO)) {
            return new SigarSystemInformation();
        } else if (className.equals(OSHI_SYSTEM_INFO)) {
            return new OshiSystemInformation();
        } else {
            throw new UnsupportedOperationException("Class '" + className
                                                    + "' is not supported as implementation for system information gathering class");
        }

    }

}
