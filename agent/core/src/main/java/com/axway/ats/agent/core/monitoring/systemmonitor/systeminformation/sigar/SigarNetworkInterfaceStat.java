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
package com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.sigar;

import org.hyperic.sigar.NetInterfaceStat;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.INetworkInterfaceStat;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.exceptions.SystemInformationException;
import com.axway.ats.core.utils.StringUtils;

public class SigarNetworkInterfaceStat implements INetworkInterfaceStat {

    private String           interfaceName;

    private NetInterfaceStat internalImplementation;

    public SigarNetworkInterfaceStat( NetInterfaceStat netInterfaceStat, String interfaceName ) {

        if (netInterfaceStat == null) {
            throw new SystemInformationException("Network interface is null!");
        }
        
        if(StringUtils.isNullOrEmpty(interfaceName)) {
            throw new SystemInformationException("Network interface name is null/empty!");
        }

        this.interfaceName = interfaceName;
        this.internalImplementation = netInterfaceStat;
    }

    @Override
    public String getInterfaceName() {

        return this.interfaceName;
    }

    @Override
    public long getTxBytes() {

        return this.internalImplementation.getTxBytes();
    }

    @Override
    public long getRxBytes() {

        return this.internalImplementation.getRxBytes();
    }

}
