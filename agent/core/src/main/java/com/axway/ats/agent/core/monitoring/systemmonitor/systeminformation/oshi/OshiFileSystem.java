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

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IFileSystem;

import oshi.software.os.OSFileStore;

public class OshiFileSystem implements IFileSystem {

    private OSFileStore internalImplementation;
    private String      devName;

    public OshiFileSystem( OSFileStore fileStore, String devName ) {

        this.internalImplementation = fileStore;
        this.internalImplementation.updateAttributes();
        this.devName = devName;
    }

    @Override
    public Type getType() {

        if (this.internalImplementation.getDescription().equalsIgnoreCase("Fixed drive") // WIN 10
            || this.internalImplementation.getDescription().equalsIgnoreCase("Local Disk")) { // Linux
            return Type.TYPE_LOCAL_DISK;
        } else if (this.internalImplementation.getDescription().equalsIgnoreCase("CD-ROM")) {
            return Type.TYPE_CDROM;
        } else if (this.internalImplementation.getDescription().equalsIgnoreCase("Network drive")) {
            return Type.TYPE_NETWORK;
        } else if (this.internalImplementation.getDescription().equalsIgnoreCase("Ram Disk")) {
            return Type.TYPE_RAM_DISK;
        } else {
            return Type.TYPE_UNKNOWN; // or TYPE_NONE
        }

    }

    @Override
    public String getDevName() {

        return this.devName;
    }

}
