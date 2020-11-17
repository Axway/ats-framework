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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.Tcp;
import org.hyperic.sigar.jmx.SigarLoadAverage;

import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IDiskUsage;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IFileSystem;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.INetworkInterfaceStat;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.IProcessInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.ISystemInformation;
import com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation.exceptions.SystemInformationException;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.monitoring.MonitorConfigurationException;

public class SigarSystemInformation implements ISystemInformation {

    private static final Logger log             = LogManager.getLogger(SigarSystemInformation.class);

    private Sigar               sigar;

    private Swap                swap;
    private Mem                 memory;
    //private Cpu                 cpu;
    private CpuPerc             cpuPerc;
    private NetStat             netstat;
    private Tcp                 tcp;

    private SigarLoadAverage    loadAvrg;

    /*
     * When monitored process exit and we keep polling it,
     * Sigar gives us a sequence of
     *   1. the last data, for example memory usage bytes
     *   2. "No such process" error
     * and this cycle goes for ever
     *
     *  This maps holds the faulty processes, so we do not poll them anymore
     */
    private List<Long>          faultyProcesses = new ArrayList<Long>();

    public SigarSystemInformation() {

        try {
            sigar = new Sigar();
        } catch (Exception e) {
            final String errorMsg = "Error creating instance of Sigar front-end class";
            log.error(errorMsg, e);
            throw new MonitorConfigurationException(errorMsg, e);
        }

        try {
            refresh();
        } catch (UnsatisfiedLinkError ule) {
            final String errorMsg = "It seems that Sigar native libraries are not found on the system. They are expected in the same folder where the Sigar jar file is located.";
            log.error(errorMsg, ule);
            throw new MonitorConfigurationException(errorMsg, ule);
        }
    }

    @Override
    public void refresh() {

        if (this.sigar == null) {
            return;
        }

        try {
            this.swap = this.sigar.getSwap();
            this.memory = this.sigar.getMem();
            //this.cpu = this.sigar.getCpu();
            this.cpuPerc = this.sigar.getCpuPerc();
            if (!OperatingSystemType.getCurrentOsType().equals(OperatingSystemType.AIX)) {
                this.netstat = this.sigar.getNetStat();
                this.tcp = this.sigar.getTcp();
            }
            this.loadAvrg = new SigarLoadAverage(this.sigar);
        } catch (Exception e) {
            final String errorMsg = "Error retrieving data from the Sigar monitoring system";
            log.error(errorMsg, e);
            throw new MonitorConfigurationException(errorMsg, e);
        }

    }

    @Override
    public void destroy() {

        if (this.sigar != null) {
            this.sigar.close();
        }

    }

    @Override
    public int getCpuCount() {

        try {
            // What do we want to do here? Getting the cores per CPU, all CPUs, or all Cores?!?!
            return this.sigar.getCpuInfoList()[0].getTotalCores();
        } catch (Exception e) {
            throw new SystemInformationException("Error obtaining CPU count", e);
        }

    }

    @Override
    public IDiskUsage getDiskUsage( String devName ) {

        try {
            return new SigarDiskUsage(this.sigar.getDiskUsage(devName), devName);
        } catch (SigarException e) {
            throw new SystemInformationException("Could not obtain disk usage for device '" + devName + "'", e);
        }

    }

    @Override
    public IFileSystem getFileSystem( String devName ) {

        try {
            FileSystem[] fsList = this.sigar.getFileSystemList();
            for (FileSystem fs : fsList) {
                if (fs.getDevName().equals(devName)) {
                    return new SigarFileSystem(fs, devName);
                }
            }

            return null;
        } catch (Exception e) {
            throw new SystemInformationException("Could not obtain file system for/from device '" + devName + "'", e);
        }

    }

    @Override
    public IFileSystem[] listFileSystems() {

        List<IFileSystem> fileSystems = new ArrayList<IFileSystem>();
        try {
            FileSystem[] fsList = this.sigar.getFileSystemList();
            for (FileSystem fs : fsList) {
                fileSystems.add(new SigarFileSystem(fs, fs.getDevName()));
            }
        } catch (SigarException e) {
            throw new SystemInformationException("Unable to list file systems", e);
        }

        if (fileSystems.isEmpty()) {
            return new IFileSystem[0];
        } else {
            return fileSystems.toArray(new IFileSystem[fileSystems.size()]);
        }

    }

    @Override
    public long getSwapUsed() {

        return this.swap.getUsed();
    }

    @Override
    public long getSwapFree() {

        return this.swap.getFree();
    }

    @Override
    public long getSwapTotal() {

        return this.swap.getTotal();
    }

    @Override
    public long getSwapPageIn() {

        return this.swap.getPageIn();
    }

    @Override
    public long getSwapPageOut() {

        return this.swap.getPageOut();
    }

    @Override
    public long getMemoryUsed() {

        return this.memory.getUsed();
    }

    @Override
    public long getMemoryFree() {

        return this.memory.getFree();
    }

    @Override
    public long getMemoryActualUsed() {

        return this.memory.getActualUsed();
    }

    @Override
    public long getMemoryActualFree() {

        return this.memory.getActualFree();
    }

    @Override
    public double getLoadAvrgLastMinute() {

        return this.loadAvrg.getLastMinute();
    }

    @Override
    public double getLoadAvrgLastFiveMinutes() {

        return this.loadAvrg.getLastFiveMinutes();
    }

    @Override
    public double getLoadAvrgLast15Minutes() {

        return this.loadAvrg.getLast15Minutes();
    }

    @Override
    public double getCpuPercWait() {

        return this.cpuPerc.getWait();
    }

    @Override
    public double getCpuPercSys() {

        return this.cpuPerc.getSys();
    }

    @Override
    public double getCpuPercUser() {

        return this.cpuPerc.getUser();
    }

    @Override
    public String[] listNetworkInterface() {

        try {
            return this.sigar.getNetInterfaceList();
        } catch (SigarException e) {
            throw new SystemInformationException("Error listing network interfaces", e);
        }
    }

    @Override
    public INetworkInterfaceStat getNetworkInterfaceStat( String ifName ) {

        try {
            return new SigarNetworkInterfaceStat(this.sigar.getNetInterfaceStat(ifName), ifName);
        } catch (SigarException e) {
            throw new SystemInformationException("Could not obtain stats for network interface '" + ifName + "'", e);
        }
    }

    @Override
    public long getTcpActiveOpens() {

        return this.tcp.getActiveOpens();
    }

    @Override
    public long getTcpPassiveOpens() {

        return this.tcp.getPassiveOpens();
    }

    @Override
    public long getTcpAttemptFails() {

        return this.tcp.getAttemptFails();
    }

    @Override
    public long getTcpEstabResets() {

        return this.tcp.getEstabResets();
    }

    @Override
    public long getTcpCurrEstab() {

        return this.tcp.getCurrEstab();
    }

    @Override
    public long getTcpInSegs() {

        return this.tcp.getInSegs();
    }

    @Override
    public long getTcpOutSegs() {

        return this.tcp.getOutSegs();
    }

    @Override
    public long getTcpRetransSegs() {

        return this.tcp.getRetransSegs();
    }

    @Override
    public long getTcpOutRsts() {

        return this.tcp.getOutRsts();
    }

    @Override
    public long getTcpInErrs() {

        return this.tcp.getInErrs();
    }

    @Override
    public long getNetstatTcpClose() {

        return this.netstat.getTcpClose();
    }

    @Override
    public long getNetstatTcpListen() {

        return this.netstat.getTcpListen();
    }

    @Override
    public long getNetstatTcpSynSent() {

        return this.netstat.getTcpSynSent();
    }

    @Override
    public long getNetstatTcpSynRecv() {

        return this.netstat.getTcpSynRecv();
    }

    @Override
    public long getNetstatTcpEstablished() {

        return this.netstat.getTcpEstablished();
    }

    @Override
    public long getNetstatTcpCloseWait() {

        return this.netstat.getTcpCloseWait();
    }

    @Override
    public long getNetstatTcpLastAck() {

        return this.netstat.getTcpLastAck();
    }

    @Override
    public long getNetstatTcpFinWait1() {

        return this.netstat.getTcpFinWait1();
    }

    @Override
    public long getNetstatTcpFinWait2() {

        return this.netstat.getTcpFinWait2();
    }

    @Override
    public long getNetstatTcpClosing() {

        return this.netstat.getTcpClosing();
    }

    @Override
    public long getNetstatTcpTimeWait() {

        return this.netstat.getTcpTimeWait();
    }

    @Override
    public long getNetstatTcpBound() {

        return this.netstat.getTcpBound();
    }

    @Override
    public long getNetstatTcpIdle() {

        return this.netstat.getTcpIdle();
    }

    @Override
    public long getNetstatTcpInboundTotal() {

        return this.netstat.getTcpInboundTotal();
    }

    @Override
    public long getNetstatTcpOutboundTotal() {

        return this.netstat.getTcpOutboundTotal();
    }

    @Override
    public void loadProcs() {

        long[] pids = getProcList();

        for (long pid : pids) {
            try {
                sigar.getProcArgs(pid);
            } catch (SigarException e) {
                // some system processes can not be accessed
                // should this process be marked as faulty here?
                if (log.isDebugEnabled()) {
                    log.error("Error occured while getting args for process [" + pid + "]", e);
                }
            }
        }

    }

    @Override
    public long[] getProcList() {

        try {
            return sigar.getProcList();
        } catch (SigarException e) {
            throw new SystemInformationException("Unable to load PIDs for each process", e);
        }
    }

    @Override
    public IProcessInformation getProcessInformation( long pid ) {

        try {
            if (!isFaultyProcess(pid)) {
                String[] args = this.sigar.getProcArgs(pid);
                String userName = this.sigar.getProcCredName(pid).getUser();
                ProcCpu cpu = this.sigar.getProcCpu(pid);
                ProcMem mem = this.sigar.getProcMem(pid);
                return new SigarProcessInformation(userName, args, cpu, mem, pid);
            }
        } catch (SigarException sie) {
            log.error("Unable to collect information for process with PID '" + pid + "'", sie);
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
            // can/should we re-throw the exception?
        }

        return null;
    }

    private void updateFaultyProcessesList(
                                            long pid ) {

        log.error("Unable to collect data about process with ID " + pid
                  + ". Such process will not be monitored anymore!");
        faultyProcesses.add(pid);
    }

    private boolean isFaultyProcess(
                                     long pid ) {

        return faultyProcesses.contains(pid);
    }

}
