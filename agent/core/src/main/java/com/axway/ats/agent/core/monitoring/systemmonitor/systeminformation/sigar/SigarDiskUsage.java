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

import org.hyperic.sigar.DiskUsage;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IDiskUsage;

public class SigarDiskUsage implements IDiskUsage {

    private DiskUsage internalImplementation;

    private String    devName;

    public SigarDiskUsage( DiskUsage diskUsage, String devName ) {

        this.devName = devName;
        this.internalImplementation = diskUsage;
    }

    @Override
    public String getDevName() {

        return this.devName;
    }

    @Override
    public long getReadBytes() {

        return this.internalImplementation.getReadBytes();
    }

    @Override
    public long getWriteBytes() {

        return this.internalImplementation.getWriteBytes();
    }

}
