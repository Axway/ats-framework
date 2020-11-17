/*
 * Copyright 2017-2020 Axway Software
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
package com.axway.ats.agent.core.monitoring.systemmonitor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperic.sigar.SigarException;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IFileSystem;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.ISystemInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.exceptions.SystemInformationException;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.IoUtils;

/**
 * Factory for creating instances of all needed readings
 */
public class IOReadingInstancesFactory {

    private static final Logger  LOG           = LogManager.getLogger(IOReadingInstancesFactory.class);

    private static final boolean IS_AIX_OS     = OperatingSystemType.getCurrentOsType()
                                                                    .equals(OperatingSystemType.AIX);
    private static final boolean IS_SOLARIS_OS = OperatingSystemType.getCurrentOsType()
                                                                    .equals(OperatingSystemType.SOLARIS);

    static ReadingInstance getReadBytesReadingInstance(
                                                        ISystemInformation systemInfo,
                                                        ReadingBean reading ) throws SystemInformationException {

        return new ReadingInstance(systemInfo,
                                   String.valueOf(reading.getDbId()),
                                   reading.getMonitorName(),
                                   reading.getName(),
                                   reading.getUnit(),
                                   0) {

            private static final long serialVersionUID = 1L;

            private List<String>      deviceNames;

            @Override
            public void init() throws SystemInformationException {

                applyMemoryNormalizationFactor();

                this.parameters = new HashMap<String, String>();
                deviceNames = getDevicesForIoMonitoring(systemInfo, true);
                if (deviceNames.size() > 0) {

                    StringBuilder devicesList = new StringBuilder("Monitored devices: ");
                    for (String deviceName : deviceNames) {
                        devicesList.append("'" + deviceName + "', ");
                    }
                    this.parameters.put(SystemMonitorDefinitions.PARAMETER_NAME__CUSTOM_MESSAGE,
                                        devicesList.substring(0, devicesList.length() - 2)); //cut the trailing ", "
                } else {
                    this.parameters.put(SystemMonitorDefinitions.PARAMETER_NAME__CUSTOM_MESSAGE,
                                        "No monitored devices!");
                }

                // after selecting available devices for monitoring we can continue with collecting the read bytes data
                this.lastLongValue = getReadBytes();
            }

            @Override
            public float poll() throws Exception {

                long newReadBytes = getReadBytes();
                double deltaReadBytes;
                if (newReadBytes != -1) {
                    deltaReadBytes = (newReadBytes - this.lastLongValue) * normalizationFactor;
                    this.lastLongValue = newReadBytes;
                } else {
                    return -1.0F;
                }
                // calculate read bytes per second
                deltaReadBytes = deltaReadBytes / ((double) getElapsedTime() / 1000);
                return new BigDecimal(deltaReadBytes).setScale(2, BigDecimal.ROUND_DOWN).floatValue();
            }

            private long getReadBytes() throws SystemInformationException {

                long readBytes = 0L;
                for (String devName : deviceNames) {

                    long newReadBytes = -1L;
                    long rbytes = getReadWriteBytes(systemInfo, devName, true);
                    newReadBytes = fixLongValue(fixOverflow(devName, rbytes));
                    if (newReadBytes == -1) {
                        return -1L;
                    }
                    readBytes += newReadBytes;
                }
                return readBytes;
            }
        };
    }

    static ReadingInstance getWriteBytesReadingInstance(
                                                         ISystemInformation systemInfo,
                                                         ReadingBean reading ) throws SystemInformationException {

        return new ReadingInstance(systemInfo,
                                   String.valueOf(reading.getDbId()),
                                   reading.getMonitorName(),
                                   reading.getName(),
                                   reading.getUnit(),
                                   0) {

            private static final long serialVersionUID = 1L;

            private List<String>      deviceNames;

            @Override
            public void init() throws SystemInformationException {

                this.parameters = new HashMap<String, String>();
                deviceNames = getDevicesForIoMonitoring(systemInfo, false);
                if (deviceNames.size() > 0) {

                    StringBuilder devicesList = new StringBuilder("Monitored devices: ");
                    for (String devName : deviceNames) {
                        devicesList.append("'" + devName + "', ");
                    }
                    this.parameters.put(SystemMonitorDefinitions.PARAMETER_NAME__CUSTOM_MESSAGE,
                                        devicesList.substring(0, devicesList.length() - 2)); //cut the trailing ", "
                } else {
                    this.parameters.put(SystemMonitorDefinitions.PARAMETER_NAME__CUSTOM_MESSAGE,
                                        "No monitored devices!");
                }

                // after selecting available devices for monitoring we can continue with collecting the write bytes data
                this.lastLongValue = getWriteBytes();
            }

            @Override
            public float poll() throws Exception {

                long newWriteBytes = getWriteBytes();
                double deltaWriteBytes;
                if (newWriteBytes != -1) {
                    deltaWriteBytes = (newWriteBytes - this.lastLongValue) * normalizationFactor;
                    this.lastLongValue = newWriteBytes;
                } else {
                    return -1.0F;
                }

                // calculate write bytes per second
                deltaWriteBytes = deltaWriteBytes / ((double) getElapsedTime() / 1000);
                return new BigDecimal(deltaWriteBytes).setScale(2, BigDecimal.ROUND_DOWN).floatValue();
            }

            private long getWriteBytes() throws SystemInformationException {

                long writeBytes = 0L;
                for (String devName : deviceNames) {

                    long newWriteBytes = -1L;
                    long wbytes = getReadWriteBytes(systemInfo, devName, false);
                    newWriteBytes = fixLongValue(fixOverflow(devName, wbytes));
                    if (newWriteBytes == -1) {
                        return -1L;
                    }
                    writeBytes += newWriteBytes;
                }
                return writeBytes;
            }
        };
    }

    /**
     * Get device names for monitoring - disk(AIX) or logical volume names
     * <p><em>Note for Sigar</em>: For AIX for example a disk names should be returned.
     * As later Sigar invokes getDiskUsage(name). Check on Linux
     * </p>
     * @param systemInfo
     * @param readBytes
     * @return
     * @throws SigarException
     */
    private static List<String> getDevicesForIoMonitoring(
                                                           ISystemInformation systemInfo,
                                                           boolean readBytes ) throws SystemInformationException {

        List<String> devices = new LinkedList<String>();
        StringBuilder problematicMounts = new StringBuilder();

        if (IS_AIX_OS) {
            String[] cmdCommand = new String[]{ "/bin/sh", "-c", "lspv 2>&1" };
            String result = null;
            try {
                Process p = Runtime.getRuntime().exec(cmdCommand);
                result = IoUtils.streamToString(p.getInputStream()).trim();
            } catch (Exception e) {
                throw new RuntimeException("Error getting devices to monitor for IO (Read/Write) transfer. lspv command invoke error",
                                           e);
            }
            String[] lines = result.split("[\r\n]+");
            int lineNum = 0;
            for (String line : lines) {
                lineNum++;
                LOG.trace("Get AIX device[" + lineNum + "]: " + line);
                String[] words = line.split("[\\s]+");
                if (words.length > 0) {
                    LOG.trace(" Found disk: " + words[0]);
                    if (getReadWriteBytes(systemInfo, words[0], readBytes) == -1) {
                        problematicMounts.append("'" + words[0] + "', ");
                    } else {
                        devices.add(words[0]);
                    }
                }
            }
        } else {
            IFileSystem[] fslist = systemInfo.listFileSystems();
            for (int i = 0; i < fslist.length; i++) {
                if (fslist[i].getType() == IFileSystem.Type.TYPE_LOCAL_DISK) {
                    if (getReadWriteBytes(systemInfo, fslist[i].getDevName(), readBytes) == -1) {
                        problematicMounts.append("'" + fslist[i].getDevName() + "', ");
                    } else {
                        devices.add(fslist[i].getDevName());
                    }
                }
            }
        }

        if (problematicMounts.length() > 0) {

            LOG.warn("Unable to get " + (readBytes
                                                   ? "Read"
                                                   : "Write")
                     + "Bytes on devices: "
                     + problematicMounts.substring(0, problematicMounts.length() - 2));
        }
        return devices;
    }

    private static long getReadWriteBytes(
                                           ISystemInformation systemInfo,
                                           String devName,
                                           boolean readBytes ) {

        try {
            // TODO - device name is different from disk name on AIX. Probably on other OSes too. Check
            if (readBytes) {
                return systemInfo.getDiskUsage(devName).getReadBytes();
            } else {
                return systemInfo.getDiskUsage(devName).getWriteBytes();
            }
        } catch (Exception se) { // can be quite different since here we HAD SigarException

            if (IS_SOLARIS_OS) { // TODO - if this is observed always on SOLARIS then there is no need to cause exception always before that

                // NOTE: this strip is needed because 'iostat' doesn't show the real full device names (like Sigar
                // gets it) for example if the real device name is: "/dev/md/dsk/d0", 'iostat' command shows "md/d0"
                if (devName != null && devName.contains("/")) {
                    devName = devName.substring(devName.lastIndexOf('/') + 1);
                }
                String[] cmdCommand = new String[]{ "/bin/sh",
                                                    "-c",
                                                    "iostat -xInpr | grep '" + devName + "$' 2>&1" };
                try {
                    Process p = Runtime.getRuntime().exec(cmdCommand);
                    String result = IoUtils.streamToString(p.getInputStream()).trim();
                    String[] lines = result.split("[\r\n]+");
                    String matchedLine = null;
                    // first try to find the target device line if ends with "/deviceName"  (read the NOTE above)
                    for (String line : lines) {
                        if (line.trim().endsWith("/" + devName)) {
                            matchedLine = line.trim();
                            break;
                        }
                    }
                    if (matchedLine == null) {
                        // now try to find the target device line if ends with "deviceName"
                        for (String line : lines) {
                            if (line.trim().endsWith(devName)) {
                                matchedLine = line.trim();
                                break;
                            }
                        }
                    }
                    if (matchedLine == null) {
                        throw new Exception("Unable to find the target line from the command results for device '"
                                            + devName + "'. Command is '" + cmdCommand[2]
                                            + "' and the result: \n" + result);
                    }
                    String[] parts = matchedLine.split("[\\,]+");
                    String rwBytesString = null;
                    if (readBytes) {
                        rwBytesString = parts[2];
                    } else {
                        rwBytesString = parts[3];
                    }
                    return (long) (Double.parseDouble(rwBytesString.trim()) * 1024);
                } catch (Exception e) {

                    LOG.error("Unable to get " + (readBytes
                                                            ? "Read"
                                                            : "Write")
                              + "Bytes on device '" + devName + "'. Unable to parse results from command '"
                              + cmdCommand[2] + "'", e);
                }
            } else {

                if (!ExceptionUtils.containsMessage("Faulty device", se)) {
                    LOG.error("Unable to get " + (readBytes
                                                            ? "Read"
                                                            : "Write")
                              + "Bytes on device '" + devName + "'", se);
                }

            }
        }
        return -1L;
    }
}
