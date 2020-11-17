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

import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcMem;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IProcessInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.exceptions.SystemInformationException;

public class SigarProcessInformation implements IProcessInformation {

    private long     pid;
    private ProcCpu  cpu;
    private ProcMem  mem;
    private String   user;
    private String[] args;

    public SigarProcessInformation( String procUser, String[] procArgs, ProcCpu procCpu, ProcMem procMem, long pid ) {

        checkIsNotNull(pid, "pid is null");
        this.pid = pid;
        checkIsNotNull(procCpu, "process cpu is null");
        this.cpu = procCpu;
        checkIsNotNull(procCpu, "process memory is null");
        this.mem = procMem;
        checkIsNotNull(procCpu, "process user is null");
        this.user = procUser;
        checkIsNotNull(procCpu, "process args are null");
        this.args = procArgs;
    }

    @Override
    public long getPid() {

        return this.pid;
    }

    @Override
    public long getCpuUser() {

        return this.cpu.getUser();
    }

    @Override
    public long getCpuKernel() {

        return this.cpu.getSys();
    }

    @Override
    public long getCpuTotal() {

        return this.cpu.getTotal();
    }

    @Override
    public long getVirtualMemory() {

        return this.mem.getSize();
    }

    @Override
    public long getResidentMemory() {

        return this.mem.getResident();
    }

    @Override
    public long getSharedMemory() {

        return this.mem.getShare();
    }

    @Override
    public String[] getArguments() {

        return this.args;
    }

    @Override
    public long getMemoryPageFaults() {

        return this.mem.getPageFaults();
    }

    @Override
    public String getUser() {

        return this.user;
    }

    private void checkIsNotNull( Object obj, String errorMessage ) {

        if (obj == null) {
            throw new SystemInformationException(errorMessage);
        }
    }

}
