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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IDiskUsage;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IFileSystem;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.INetworkInterfaceStat;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IProcessInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.ISystemInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.exceptions.SystemInformationException;
import com.axway.ats.common.systemproperties.AtsSystemProperties;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.VirtualMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OperatingSystem;

public class OshiSystemInformation implements ISystemInformation {

    /**
     * Oshi needs different ticks to calculate CPU-related data. This means that ATS needs to wait a little between getting two ticks readings.<br>
     * This system property is used to specify this wait time (in milliseconds) and by default is <strong>500ms</strong>
     * Anything less causes wrong data, so this is the minimum time required.
     * */
    public static final String      CPU_TICK_INTERVAL_MS                = "oshi.cpu.ticks.interval.ms";
    /**
     * Iterating over all processes each poll is too slow,<br>
     * so by default ATS load all currently running (in state, different than INVALID ) only once per the whole monitoring session.<br>
     * This means that if a process appears after ATS already cached the process PIDs, this process will not be monitored at all.<br>
     * If you want to always iterate over all of the processes, use this system property with true for a value<br>,
     * but note that this is very slow operation, so you will have to increase your monitor pooling interval a lot
     *  
     * */

    //public static final String      PROCESS_USE_CACHED_PIDS             = "oshi.process.use.cached.pids";

    /**
     * Since iterating all of the processes is very slow operation, use this property to specify interval (in milliseconds) for this kind of operation.<br/>
     * Default one is {@link OshiSystemInformation#DEFAULT_PROCESS_PIDS_CACHE_LIFETIME}
     * */
    public static final String      PROCESS_PIDS_CACHE_LIFETIME         = "oshi.process.pids.lifetime";
    /**
     * 60000 milliseconds
     * */
    public static final long        DEFAULT_PROCESS_PIDS_CACHE_LIFETIME = TimeUnit.MINUTES.toMillis(1);

    private static final Logger     log                                 = LogManager.getLogger(OshiSystemInformation.class);

    private SystemInfo              systemInfo                          = null;
    public HardwareAbstractionLayer hal                                 = null;
    public OperatingSystem          os                                  = null;

    private CentralProcessor        cpu                                 = null;

    private GlobalMemory            gm                                  = null;
    private VirtualMemory           vm                                  = null;

    private InternetProtocolStats   protocolStats                       = null;

    private long[]                  prevTicks                           = null;

    private long[]                  currTicks                           = null;

    private long                    prevTickTime                        = -1;

    /**
     * Keep track when was the last time ATS polls for all of the available processes
     * */
    private long                    previousPidsPollingTimestamp        = -1;

    /*
     *  This maps holds the faulty processes, so we do not poll them anymore
     */
    private Set<Long>               faultyProcesses                     = new HashSet<Long>();

    /*
     *  This maps holds the faulty devices, so we do not poll them anymore
     */
    private Set<String>             faultyDevices                       = new HashSet<String>();

    private Set<Long>               cachedPids                          = new HashSet<>();

    public OshiSystemInformation() {

        this.systemInfo = new SystemInfo();
        this.hal = this.systemInfo.getHardware();
        this.os = this.systemInfo.getOperatingSystem();
        this.cpu = this.hal.getProcessor();
        this.gm = this.hal.getMemory();
        this.vm = this.gm.getVirtualMemory();
        this.protocolStats = this.os.getInternetProtocolStats();
    }

    @Override
    public void refresh() {

        final long minSleepTime = AtsSystemProperties.getPropertyAsNumber(CPU_TICK_INTERVAL_MS, 500);

        if (prevTickTime == -1) {
            try {
                this.prevTicks = this.cpu.getSystemCpuLoadTicks();
                Thread.sleep(minSleepTime);
                this.currTicks = this.cpu.getSystemCpuLoadTicks();
                this.prevTickTime = System.currentTimeMillis();
            } catch (Exception e) {}
        } else {
            long currTickTime = System.currentTimeMillis();

            long tickTimeDiff = currTickTime - this.prevTickTime;
            long sleepTime = minSleepTime - tickTimeDiff;
            this.prevTicks = this.currTicks;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (Exception e) {}
            }
            this.currTicks = this.cpu.getSystemCpuLoadTicks();
            this.prevTickTime = System.currentTimeMillis();
        }
    }

    @Override
    public void destroy() {

        // not implemented

    }

    @Override
    public int getCpuCount() {

        // or return this.cpu.getLogicalProcessorCount();
        return this.cpu.getPhysicalProcessorCount();
    }

    @Override
    public IDiskUsage getDiskUsage( String devName ) {

        try {
            List<HWDiskStore> disks = this.hal.getDiskStores();
            for (HWDiskStore ds : disks) {
                if (!isFaultyDevice(devName)) {
                    if (ds.getName().equals(devName)) {
                        return new OshiDiskUsage(ds, devName);
                    }
                } else {
                    throw new SystemInformationException("Faulty device detected '" + devName + "'");
                }
            }
            throw new SystemInformationException("No such disk device '" + devName + "'");
        } catch (Exception e) {
            updateFaultyDevicesList(devName);
            throw new SystemInformationException("Could not obtain disk usage for device '" + devName + "'", e);
        }
    }

    @Override
    public IFileSystem getFileSystem( String devName ) {

        try {
            FileSystem fs = this.os.getFileSystem();
            List<OSFileStore> fileStores = fs.getFileStores();
            for (OSFileStore fileStore : fileStores) {
                if (fileStore.getName().equals(devName)) {
                    return new OshiFileSystem(fileStore, fileStore.getName());
                }
            }
            throw new SystemInformationException("No such file system drive device '" + devName + "'");
        } catch (Exception e) {
            throw new SystemInformationException("Could not obtain file system for/from device '" + devName + "'", e);
        }

    }

    @Override
    public IFileSystem[] listFileSystems() {

        FileSystem fs = this.systemInfo.getOperatingSystem().getFileSystem();
        List<OSFileStore> fileStores = fs.getFileStores(true); // enumerate only local drives. (Network ones are excluded)
        List<IFileSystem> fileSystems = new ArrayList<IFileSystem>();
        for (OSFileStore fileStore : fileStores) {
            fileSystems.add(new OshiFileSystem(fileStore, fileStore.getMount()));
        }
        return fileSystems.toArray(new IFileSystem[fileSystems.size()]);
    }

    @Override
    public long getSwapUsed() {

        return this.vm.getSwapUsed();
    }

    @Override
    public long getSwapFree() {

        return this.vm.getSwapTotal() - this.vm.getSwapUsed();
    }

    @Override
    public long getSwapTotal() {

        return this.vm.getSwapTotal();
    }

    @Override
    public long getSwapPageIn() {

        return this.vm.getSwapPagesIn();
    }

    @Override
    public long getSwapPageOut() {

        return this.vm.getSwapPagesOut();
    }

    @Override
    public long getMemoryUsed() {

        return this.gm.getTotal() - this.gm.getAvailable();
    }

    @Override
    public long getMemoryFree() {

        return this.gm.getAvailable();
    }

    @Override
    public long getMemoryActualUsed() {

        return this.getMemoryUsed();
    }

    @Override
    public long getMemoryActualFree() {

        return this.getMemoryFree();
    }

    @Override
    public double getLoadAvrgLastMinute() {

        return this.cpu.getSystemLoadAverage(1)[0];
    }

    @Override
    public double getLoadAvrgLastFiveMinutes() {

        return this.cpu.getSystemLoadAverage(2)[1];
    }

    @Override
    public double getLoadAvrgLast15Minutes() {

        return this.cpu.getSystemLoadAverage(3)[2];
    }

    @Override
    public double getCpuPercWait() {

        long wait = currTicks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long total = calculateCpuTotal();

        if (total == 0) {
            return 0;
        } else {
            return (wait / (total * 1.0));
        }
    }

    @Override
    public double getCpuPercSys() {

        long sys = currTicks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long total = calculateCpuTotal();

        if (total == 0) {
            return 0;
        } else {
            return (sys / (total * 1.0));
        }

    }

    @Override
    public double getCpuPercUser() {

        long user = currTicks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long total = calculateCpuTotal();

        if (total == 0) {
            return 0;
        } else {
            return (user / (total * 1.0));
        }
    }

    private long calculateCpuTotal() {

        long user = currTicks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long nice = currTicks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long sys = currTicks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long idle = currTicks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long iowait = currTicks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long irq = currTicks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = currTicks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long steal = currTicks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];
        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;

        return totalCpu;
    }

    @Override
    public String[] listNetworkInterface() {

        List<NetworkIF> networkInterfaces = this.hal.getNetworkIFs();
        List<String> ifNames = new ArrayList<String>();
        for (NetworkIF networkInterface : networkInterfaces) {
            ifNames.add(networkInterface.getName());
            // or ifNames.add(networkInterface.getDisplayName());

        }
        return ifNames.toArray(new String[ifNames.size()]);
    }

    @Override
    public INetworkInterfaceStat getNetworkInterfaceStat( String ifName ) {

        List<NetworkIF> networkInterfaces = this.hal.getNetworkIFs();
        for (NetworkIF networkInterface : networkInterfaces) {
            if (networkInterface.getName().equals(ifName)) {
                return new OshiNetworkInterfaceStat(networkInterface, ifName);
            }
        }
        return null;
    }

    @Override
    public long getTcpActiveOpens() {

        return this.protocolStats.getTCPv4Stats().getConnectionsActive()
               + this.protocolStats.getTCPv6Stats().getConnectionsActive();
    }

    @Override
    public long getTcpPassiveOpens() {

        return this.protocolStats.getTCPv4Stats().getConnectionsPassive()
               + this.protocolStats.getTCPv6Stats().getConnectionsPassive();
    }

    @Override
    public long getTcpAttemptFails() {

        return this.protocolStats.getTCPv4Stats().getConnectionFailures()
               + this.protocolStats.getTCPv6Stats().getConnectionFailures();
    }

    @Override
    public long getTcpEstabResets() {

        return this.protocolStats.getTCPv4Stats().getConnectionsReset()
               + this.protocolStats.getTCPv6Stats().getConnectionsReset();
    }

    @Override
    public long getTcpCurrEstab() {

        return this.protocolStats.getTCPv4Stats().getConnectionsEstablished()
               + this.protocolStats.getTCPv6Stats().getConnectionsEstablished();
    }

    @Override
    public long getTcpInSegs() {

        // TODO exclude error segs?
        return this.protocolStats.getTCPv4Stats().getSegmentsReceived()
               + this.protocolStats.getTCPv6Stats().getSegmentsReceived();
    }

    @Override
    public long getTcpOutSegs() {

        // TODO exclude error segs?
        return this.protocolStats.getTCPv4Stats().getSegmentsSent()
               + this.protocolStats.getTCPv6Stats().getSegmentsSent();
    }

    @Override
    public long getTcpRetransSegs() {

        return this.protocolStats.getTCPv4Stats().getSegmentsRetransmitted()
               + this.protocolStats.getTCPv6Stats().getSegmentsRetransmitted();
    }

    @Override
    public long getTcpOutRsts() {

        return this.protocolStats.getTCPv4Stats().getOutResets()
               + this.protocolStats.getTCPv6Stats().getOutResets();
    }

    @Override
    public long getTcpInErrs() {

        return this.protocolStats.getTCPv4Stats().getInErrors()
               + this.protocolStats.getTCPv6Stats().getInErrors();
    }

    @Override
    public long getNetstatTcpClose() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpListen() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpSynSent() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpSynRecv() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpEstablished() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpCloseWait() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpLastAck() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpFinWait1() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpFinWait2() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpClosing() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpTimeWait() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpBound() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpIdle() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpInboundTotal() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public long getNetstatTcpOutboundTotal() {

        throw new RuntimeException("Not Supported by OSHI");
    }

    @Override
    public void loadProcs() {

        this.cachedPids.clear();

        List<OSProcess> procs = this.os.getProcesses();
        for (int i = 0; i < procs.size(); i++) {
            OSProcess process = procs.get(i);
            if (process != null) {
                long pid = process.getProcessID();
                this.cachedPids.add(pid);
            }
        }
    }

    @Override
    public long[] getProcList() {

        /*boolean useCashedProcesses = AtsSystemProperties.getPropertyAsBoolean(PROCESS_USE_CACHED_PIDS, true);*/
        long processPidsCacheLifetime = AtsSystemProperties.getPropertyAsNonNegativeNumber(PROCESS_PIDS_CACHE_LIFETIME,
                                                                                           (int) DEFAULT_PROCESS_PIDS_CACHE_LIFETIME);
        List<OSProcess> procs = null;

        long currTime = System.currentTimeMillis();

        if (previousPidsPollingTimestamp != -1 && currTime - previousPidsPollingTimestamp < processPidsCacheLifetime) {
            long[] pids = new long[this.cachedPids.size()];
            int i = 0;
            Iterator<Long> it = this.cachedPids.iterator();
            while (it.hasNext()) {
                pids[i++] = it.next();
            }
            return pids;
        } else {
            previousPidsPollingTimestamp = currTime;
            procs = this.os.getProcesses();
            long[] pids = new long[procs.size()];
            for (int i = 0; i < procs.size(); i++) {
                OSProcess process = procs.get(i);
                if (process != null) {
                    long pid = process.getProcessID();
                    pids[i] = pid;
                }
            }
            return pids;
        }
    }

    @Override
    public IProcessInformation getProcessInformation( long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                OSProcess proc = this.os.getProcess((int) pid);
                if (proc == null) {
                    throw new SystemInformationException("Process '" + pid + "' does not exist");
                }
                if (proc.getState() == State.INVALID) {
                    if (log.isDebugEnabled()) {
                        log.debug("Process [" + pid
                                  + "] is in invalid state and will be marked as faulty. No more data will be obtained from it.");
                    }
                    updateFaultyProcessesList(pid);
                }

                return new OshiProcessInformation(proc);
            }
        } catch (Exception e) {
            log.error("Unable to collect information for process with PID '" + pid + "'", e);
            updateFaultyProcessesList(pid);
        }

        return null;
    }

    @Override
    public long getProcessCpuTimeRunningUser( Long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                return getProcessInformation(pid).getCpuUser();
            }
        } catch (SystemInformationException sie) {
            updateFaultyProcessesList(pid);
        }

        return -1;
    }

    @Override
    public long getProcessCpuTimeRunningKernel( Long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                return getProcessInformation(pid).getCpuKernel();
            }
        } catch (SystemInformationException sie) {
            updateFaultyProcessesList(pid);
        }

        return -1;
    }

    @Override
    public long getProcessCpuTimeRunningTotal( Long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                return getProcessInformation(pid).getCpuTotal();
            }
        } catch (SystemInformationException sie) {
            updateFaultyProcessesList(pid);
        }

        return -1;
    }

    @Override
    public double getProcessVirtualMemory( Long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                return getProcessInformation(pid).getVirtualMemory();
            }
        } catch (SystemInformationException sie) {
            updateFaultyProcessesList(pid);
        }

        return -1;
    }

    @Override
    public double getProcessResidentMemory( Long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                return getProcessInformation(pid).getResidentMemory();
            }
        } catch (SystemInformationException sie) {
            updateFaultyProcessesList(pid);
        }

        return -1;
    }

    @Override
    public double getProcessSharedMemory( Long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                return getProcessInformation(pid).getSharedMemory();
            }
        } catch (SystemInformationException sie) {
            updateFaultyProcessesList(pid);
        }

        return -1;
    }

    @Override
    public long getProcessMemoryPageFaults( Long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                return getProcessInformation(pid).getMemoryPageFaults();
            }
        } catch (SystemInformationException sie) {
            updateFaultyProcessesList(pid);
        }

        return -1;
    }

    @Override
    public String[] getProcArgs( long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                return getProcessInformation(pid).getArguments();
            }
        } catch (SystemInformationException sie) {
            updateFaultyProcessesList(pid);
        }

        return null;
    }

    private void updateFaultyProcessesList(
                                            long pid ) {

        log.error("Unable to collect data about process with ID " + pid
                  + ". No further monitoring will be done for this process!");
        faultyProcesses.add(pid);
    }

    private boolean isFaultyProcess(
                                     long pid ) {

        return faultyProcesses.contains(pid);
    }

    private void updateFaultyDevicesList(
                                          String devName ) {

        log.error("Unable to collect data about device with name " + devName
                  + ". No further monitoring will be done for this device!");
        faultyDevices.add(devName);
    }

    private boolean isFaultyDevice(
                                    String devName ) {

        return faultyDevices.contains(devName);
    }

}
