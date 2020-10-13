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
package com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation;

public interface IProcessInformation {

    long getPid();

    /**
     * @return the number of milliseconds the process has executed in usermode.
     * */
    long getCpuUser();

    /**
     * @return the number of milliseconds the process has executed in usermode.
     * */
    long getCpuKernel();

    /**
     * @return the total number of milliseconds the process has executed in all modes (user, kernel, etc).
     * */
    long getCpuTotal();

    /**
     * @return the VM size in bytes
     * */
    long getVirtualMemory();

    /**
     * @return the Resident memory size in bytes
     * */
    long getResidentMemory();

    /**
     * @return the VM size in bytes
     * */
    long getSharedMemory();

    String[] getArguments();

    /**
     * @return the number of memory page faults
     * */
    long getMemoryPageFaults();

    /**
     * @return the user name
     * */
    String getUser();

}
