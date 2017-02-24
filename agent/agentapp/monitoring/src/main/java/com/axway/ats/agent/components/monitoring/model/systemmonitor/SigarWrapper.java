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
package com.axway.ats.agent.components.monitoring.model.systemmonitor;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.Tcp;
import org.hyperic.sigar.jmx.SigarLoadAverage;

import com.axway.ats.agent.components.monitoring.model.exceptions.MonitorConfigurationException;
import com.axway.ats.common.system.OperatingSystemType;

/**
 * A wrapper around Sigar library
 */
public class SigarWrapper {

    private static final Logger log             = Logger.getLogger( SigarWrapper.class );

    private Sigar               sigar;

    Swap                        swap;
    Mem                         memory;
    Cpu                         cpu;
    CpuPerc                     cpuPerc;
    NetStat                     netstat;
    Tcp                         tcp;

    SigarLoadAverage            loadAvrg;

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

    SigarWrapper() throws SigarException, MonitorConfigurationException {

        try {
            sigar = new Sigar();
        } catch( Exception e ) {
            final String errorMsg = "Error creating instance of Sigar front-end class";
            log.error( errorMsg, e );
            throw new MonitorConfigurationException( errorMsg, e );
        }

        try {
            refresh();
        } catch( UnsatisfiedLinkError ule ) {
            final String errorMsg = "It seems that Sigar native libraries are not found on the system. They are expected in the same folder where the Sigar jar file is located.";
            log.error( errorMsg, ule );
            throw new MonitorConfigurationException( errorMsg, ule );
        }
    }

    /**
     * called on each poll
     *
     * @throws SigarException
     * @throws MonitorConfigurationException
     */
    void refresh() throws SigarException, MonitorConfigurationException {

        try {
            this.swap = this.sigar.getSwap();
            this.memory = this.sigar.getMem();
            this.cpu = this.sigar.getCpu();
            this.cpuPerc = this.sigar.getCpuPerc();
            if( !OperatingSystemType.getCurrentOsType().equals( OperatingSystemType.AIX ) ) {
                this.netstat = this.sigar.getNetStat();
                this.tcp = this.sigar.getTcp();
            }
            this.loadAvrg = new SigarLoadAverage( this.sigar );
        } catch( Exception e ) {
            final String errorMsg = "Error retrieving data from the Sigar monitoring system";
            log.error( errorMsg, e );
            throw new MonitorConfigurationException( errorMsg, e );
        }

    }

    /*
     * TODO we should probably avoid using this method
     * as the best way to assure same time stamp for all readings is
     * to only call the refresh method
     */
    Sigar getSigarInstance() {

        return this.sigar;
    }

    void stopUsingSigar() {

        this.sigar.close();
    }

    double getProcessCpuPercent(
                                 long pid ) {

        try {
            if( !isFaultyProcess( pid ) ) {
                return sigar.getProcCpu( pid ).getPercent();
            }
        } catch( SigarException se ) {
            updateFaultyProcessesList( pid );
        }
        return -1;
    }

    long getProcessCpuTimeRunningKernel(
                                         long pid ) {

        try {
            if( !isFaultyProcess( pid ) ) {
                return sigar.getProcCpu( pid ).getSys();
            }
        } catch( SigarException se ) {
            updateFaultyProcessesList( pid );
        }
        return -1;
    }

    long getProcessCpuTimeRunningUser(
                                       long pid ) {

        try {
            if( !isFaultyProcess( pid ) ) {
                return sigar.getProcCpu( pid ).getUser();
            }
        } catch( SigarException se ) {
            updateFaultyProcessesList( pid );
        }
        return -1;
    }

    long getProcessCpuTimeRunningTotal(
                                        long pid ) {

        try {
            if( !isFaultyProcess( pid ) ) {
                return sigar.getProcCpu( pid ).getTotal();
            }
        } catch( SigarException se ) {
            updateFaultyProcessesList( pid );
        }
        return -1;
    }

    long getProcessVirtualMemory(
                                  long pid ) {

        try {
            if( !isFaultyProcess( pid ) ) {
                return sigar.getProcMem( pid ).getSize();
            }
        } catch( SigarException se ) {
            updateFaultyProcessesList( pid );
        }

        return -1;
    }

    long getProcessResidentMemory(
                                   long pid ) {

        try {
            if( !isFaultyProcess( pid ) ) {
                return sigar.getProcMem( pid ).getResident();
            }
        } catch( SigarException se ) {
            updateFaultyProcessesList( pid );
        }

        return -1;
    }

    long getProcessSharedMemory(
                                 long pid ) {

        try {
            if( !isFaultyProcess( pid ) ) {
                return sigar.getProcMem( pid ).getShare();
            }
        } catch( SigarException se ) {
            updateFaultyProcessesList( pid );
        }

        return -1;
    }

    long getProcessMemoryPageFaults(
                                     long pid ) {

        try {
            if( !isFaultyProcess( pid ) ) {
                return sigar.getProcMem( pid ).getPageFaults();
            }
        } catch( SigarException se ) {
            updateFaultyProcessesList( pid );
        }
        return -1;
    }

    /**
     * may happen if the process has stopped
     *
     * @param pid
     */
    private void updateFaultyProcessesList(
                                            long pid ) {

        log.error( "Unable to collect data about process with ID " + pid
                   + ". We will not be monitoring this process anymore!" );
        faultyProcesses.add( pid );
    }

    private boolean isFaultyProcess(
                                     long pid ) {

        return faultyProcesses.contains( pid );
    }
}
