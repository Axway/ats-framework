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

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IProcessInformation;

import oshi.software.os.OSProcess;

public class OshiProcessInformation implements IProcessInformation {

    private OSProcess internalImplementation = null;

    public OshiProcessInformation( OSProcess proc ) {

        this.internalImplementation = proc;
        this.internalImplementation.updateAttributes();
    }

    @Override
    public long getPid() {

        return this.internalImplementation.getProcessID();
    }

    @Override
    public long getCpuUser() {

        this.internalImplementation.updateAttributes();
        return this.internalImplementation.getUserTime();
    }

    @Override
    public long getCpuKernel() {

        this.internalImplementation.updateAttributes();
        return this.internalImplementation.getKernelTime();
    }

    @Override
    public long getCpuTotal() {

        this.internalImplementation.updateAttributes();
        return this.getCpuUser() + this.getCpuKernel();
    }

    @Override
    public long getVirtualMemory() {

        this.internalImplementation.updateAttributes();
        return this.internalImplementation.getVirtualSize();
    }

    @Override
    public long getResidentMemory() {

        this.internalImplementation.updateAttributes();
        return this.internalImplementation.getResidentSetSize();
    }

    @Override
    public long getSharedMemory() {

        this.internalImplementation.updateAttributes();
        //throw new RuntimeException("Not implemented!");
        return 0;
    }

    @Override
    public String[] getArguments() {

        return this.internalImplementation.getCommandLine().split(" ");
    }

    @Override
    public long getMemoryPageFaults() {

        this.internalImplementation.updateAttributes();
        return this.internalImplementation.getMajorFaults() + this.internalImplementation.getMinorFaults();
    }

    @Override
    public String getUser() {

        return this.internalImplementation.getUser();
    }

}
