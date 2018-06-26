/*
 * Copyright 2017 Axway Software
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
package com.axway.ats.agent.components.system.operations;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Properties;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.Sigar;

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.ActionRequestInfo;

public class MachineDescriptionOperations {

    private static final String[] OS_KEYS     = new String[]{ "os.name",
                                                              "os.version",
                                                              "sun.os.patch.level",
                                                              "os.arch",
                                                              "sun.arch.data.model" };
    private static final String[] OS_VALUES   = new String[]{ "name",
                                                              "version",
                                                              "patch",
                                                              "architecture",
                                                              "architecture model" };

    private static final String[] USER_KEYS   = new String[]{ "user.name",
                                                              "user.home",
                                                              "java.io.tmpdir",
                                                              "user.language",
                                                              "user.timezone" };
    private static final String[] USER_VALUES = new String[]{ "name",
                                                              "home folder",
                                                              "temp folder",
                                                              "language",
                                                              "time zone" };

    @Action(
            name = "Machine Description Operations Get Description")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "machine/description")
    public String getDescription() throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append(getInfoFromSigar());
        sb.append(getInfoFromJavaRuntime());

        return sb.toString();
    }

    private StringBuilder getInfoFromSigar() throws Exception {

        StringBuilder sb = new StringBuilder();
        Sigar sigar = new Sigar();

        sb.append("Fully Qualified Domain Name:\n\t" + sigar.getFQDN().toString());
        sb.append("\nSystem uptime:\n\t" + new Double(sigar.getUptime().getUptime()).intValue()
                  + " seconds");
        sb.append("\nDate:\n\t" + new Date());
        sb.append("\nNetwork settings:" + format(sigar.getNetInfo().toString(), 1));

        sb.append("\nCPU info:");
        CpuInfo[] cpuInfoList = sigar.getCpuInfoList();
        for (int i = 0; i < cpuInfoList.length; i++) {
            CpuInfo cpuInfo = cpuInfoList[i];
            sb.append("\n\tCPU " + i + ": ");
            sb.append(format(cpuInfo.toString(), 2));
        }

        double totalMemory = (new Long(sigar.getMem().getTotal()).doubleValue() / 1024 / 1024 / 1024);
        sb.append("\nTotal memory:\n\t"
                  + new BigDecimal(totalMemory).setScale(2, BigDecimal.ROUND_DOWN).floatValue() + " GB");

        String[] nicList = sigar.getNetInterfaceList();
        sb.append("\nNIC info: ");
        for (int i = 0; i < nicList.length; i++) {
            NetInterfaceConfig nic = sigar.getNetInterfaceConfig(nicList[i]);
            sb.append("\n\tNIC " + i + ": ");
            sb.append(format(nic.toString(), 2));
        }

        FileSystem[] fileSystemList = sigar.getFileSystemList();
        sb.append("\nFile system info: ");
        for (int i = 0; i < fileSystemList.length; i++) {
            FileSystem fileSystem = fileSystemList[i];

            sb.append("\n\t" + fileSystem.getDevName() + " is a");
            if (fileSystem.getType() == FileSystem.TYPE_LOCAL_DISK) {
                sb.append(" local");
            } else if (fileSystem.getType() == FileSystem.TYPE_NETWORK) {
                sb.append(" network");
            } else if (fileSystem.getType() == FileSystem.TYPE_RAM_DISK) {
                sb.append(" ram disk");
            } else if (fileSystem.getType() == FileSystem.TYPE_SWAP) {
                sb.append(" swap");
            }

            sb.append(" " + fileSystem.getSysTypeName() + ", dir name '" + fileSystem.getDirName()
                      + "', options '" + fileSystem.getOptions() + "'");
        }

        sb.append("\nResource limits:" + format(sigar.getResourceLimit().toString(), "max", 1));
        return sb;
    }

    private StringBuilder getInfoFromJavaRuntime() throws Exception {

        StringBuilder sb = new StringBuilder();
        Properties props = System.getProperties();

        sb.append("\nOS info:");
        for (int i = 0; i < OS_KEYS.length; i++) {
            String key = OS_KEYS[i];
            if (props.containsKey(key)) {
                sb.append("\n\t" + OS_VALUES[i] + " = " + props.getProperty(key));
            }
        }
        sb.append("\nUser info:");
        for (int i = 0; i < USER_KEYS.length; i++) {
            String key = USER_KEYS[i];
            if (props.containsKey(key)) {
                sb.append("\n\t" + USER_VALUES[i] + " = " + props.getProperty(key));
            }
        }
        return sb;
    }

    private StringBuilder format(
                                  String tokensString,
                                  int indentation ) {

        return format(tokensString, null, indentation);
    }

    private StringBuilder format(
                                  String tokensString,
                                  String searchedToken,
                                  int indentation ) {

        String indentationString = "\n";
        for (int i = 0; i < indentation; i++) {
            indentationString = indentationString + "\t";
        }

        StringBuilder sb = new StringBuilder();
        if (tokensString.startsWith("{")) {
            tokensString = tokensString.substring(1);
        }
        if (tokensString.endsWith("}")) {
            tokensString = tokensString.substring(0, tokensString.length() - 1);
        }
        for (String token : tokensString.split(",")) {
            if (searchedToken == null || token.toLowerCase().contains(searchedToken.toLowerCase())) {
                sb.append(indentationString + token.trim());
            }
        }
        return sb;
    }
}
