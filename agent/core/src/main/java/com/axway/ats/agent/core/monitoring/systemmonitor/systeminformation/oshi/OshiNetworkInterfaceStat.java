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
package com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.oshi;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.INetworkInterfaceStat;

import oshi.hardware.NetworkIF;

public class OshiNetworkInterfaceStat implements INetworkInterfaceStat {

    private String    interfaceName;
    private NetworkIF internalImplementation;

    public OshiNetworkInterfaceStat( NetworkIF networkIF, String interfaceName ) {

        this.interfaceName = interfaceName;
        this.internalImplementation = networkIF;
        this.internalImplementation.updateAttributes();
    }

    @Override
    public String getInterfaceName() {

        return this.interfaceName;
    }

    @Override
    public long getTxBytes() {

        this.internalImplementation.updateAttributes();

        return this.internalImplementation.getBytesSent();
    }

    @Override
    public long getRxBytes() {

        this.internalImplementation.updateAttributes();

        return this.internalImplementation.getBytesRecv();
    }

}
