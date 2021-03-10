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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.ISystemInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.exceptions.SystemInformationException;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.oshi.OshiSystemInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.sigar.SigarSystemInformation;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.reflect.ReflectionUtils;

/**
 * Use this class to obtain {@link ISystemInformation} instance.</br>
 * */
public class SystemInformationFactory {

    private static Map<String, String> MONITORING_PROVIDERS_MAP;

    static {
        MONITORING_PROVIDERS_MAP = new HashMap<String, String>();
        MONITORING_PROVIDERS_MAP.put("oshi", OshiSystemInformation.class.getName());
        MONITORING_PROVIDERS_MAP.put("sigar", SigarSystemInformation.class.getName());
    }

    private static final Logger LOG = LogManager.getLogger(SystemInformationFactory.class);

    /**
     * Depending on the System property {@link AtsSystemProperties.SYSTEM_INFORMATION_CLASS}, a {@link ISystemInformation} will be returned.</br>
     * */
    public synchronized static ISystemInformation get() {

        String providerName = AtsSystemProperties.getPropertyAsString(AtsSystemProperties.SYSTEM_MONITORING_PROVIDER,
                                                                      "sigar");

        if (!MONITORING_PROVIDERS_MAP.containsKey(providerName)) {
            throw new UnsupportedOperationException("Provider '" + providerName
                                                    + "' is not supported as implementation for system information/monitoring provider. Currently available once are: "
                                                    + Arrays.toString(MONITORING_PROVIDERS_MAP.keySet()
                                                                                              .toArray(new String[MONITORING_PROVIDERS_MAP.size()])));
        }

        LOG.info("System information/monitoring provider will be: '" + providerName + "'");

        ISystemInformation providerInstance = null;
        try {
            Class<?> clazz = Class.forName(MONITORING_PROVIDERS_MAP.get(providerName));
            providerInstance = (ISystemInformation) clazz.getConstructors()[0].newInstance(new Object[] {});
        } catch (Exception e) {
            throw new SystemInformationException("Error while initializing monitoring provider '" + providerName + "'",
                                                 e);
        }

        return providerInstance;

    }

}
