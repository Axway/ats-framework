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

import org.hyperic.sigar.FileSystem;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IFileSystem;

public class SigarFileSystem implements IFileSystem {

    private FileSystem internalImplementation;

    private String     devName;

    public SigarFileSystem( FileSystem fs, String devName ) {

        this.internalImplementation = fs;
        this.devName = devName;
        /*Sigar sigar = ((Sigar) this.systemInfo.getInternalImplementation());
        FileSystem[] fsList = sigar.getFileSystemList();
        for (FileSystem fs : fsList) {
            if (fs.getDevName().equals(devName)) {
                ;
                break;
            }
        }*/

    }

    @Override
    public Type getType() {
        return Type.fromInt( (this.internalImplementation.getType()));
    }

    @Override
    public String getDevName() {

        return this.devName;
    }

}
