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

import java.util.concurrent.TimeUnit;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IProcessInformation;
import com.axway.ats.common.systemproperties.AtsSystemProperties;

import oshi.software.os.OSProcess;

public class OshiProcessInformation implements IProcessInformation {

    private OSProcess          internalImplementation            = null;

    private long               previousUpdateAttributesTimestamp = -1;

    /**
     * Use this property to specify the update interval (in milliseconds) for the process information.<br/>
     * Default one is {@link OshiProcessInformation#DEFAULT_UPDATE_INTERVAL}
     * */
    public static final String UPDATE_INTERVAL                   = "oshi.process.update.interval";
    /**
     * 1000 ms
     * */
    public static final long   DEFAULT_UPDATE_INTERVAL           = TimeUnit.SECONDS.toMillis(1);

    private static final long  updateIntervalMillis;

    static {
        // for now, keep this property private
        updateIntervalMillis = AtsSystemProperties.getPropertyAsNonNegativeNumber(UPDATE_INTERVAL,
                                                                                  (int) TimeUnit.SECONDS.toMillis(DEFAULT_UPDATE_INTERVAL));

    }

    public OshiProcessInformation( OSProcess proc ) {

        this.internalImplementation = proc;
        updateAttributes();
    }

    @Override
    public long getPid() {

        return this.internalImplementation.getProcessID();
    }

    @Override
    public long getCpuUser() {

        updateAttributes();
        return this.internalImplementation.getUserTime();
    }

    @Override
    public long getCpuKernel() {

        updateAttributes();
        return this.internalImplementation.getKernelTime();
    }

    @Override
    public long getCpuTotal() {

        updateAttributes();
        return this.getCpuUser() + this.getCpuKernel();
    }

    @Override
    public long getVirtualMemory() {

        updateAttributes();
        return this.internalImplementation.getVirtualSize();
    }

    @Override
    public long getResidentMemory() {

        updateAttributes();
        return this.internalImplementation.getResidentSetSize();
    }

    @Override
    public long getSharedMemory() {

        //updateAttributes();
        //throw new RuntimeException("Not implemented!");
        return -1;
    }

    @Override
    public String[] getArguments() {

        return this.internalImplementation.getCommandLine().split(" ");
    }

    @Override
    public long getMemoryPageFaults() {

        updateAttributes();
        return this.internalImplementation.getMajorFaults() + this.internalImplementation.getMinorFaults();
    }

    @Override
    public String getUser() {

        return this.internalImplementation.getUser();
    }

    private void updateAttributes() {

        long currentTime = System.currentTimeMillis();

        if (previousUpdateAttributesTimestamp == -1
            || currentTime - previousUpdateAttributesTimestamp > TimeUnit.MINUTES.toMillis(updateIntervalMillis)) {
            this.internalImplementation.updateAttributes();
            this.previousUpdateAttributesTimestamp = currentTime;
        }

    }

}
