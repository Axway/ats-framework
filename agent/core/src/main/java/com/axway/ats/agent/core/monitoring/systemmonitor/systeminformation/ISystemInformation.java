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

/**
 * Interface that should be implemented by any class that wants to act as a monitoring provider<br>
 * <strong>Note</strong> that each of the implementation classes <strong>must</strong> have only one constructor with no arguments
 * */
public interface ISystemInformation {

    void refresh();

    void destroy();

    int getCpuCount();

    IDiskUsage getDiskUsage( String devName );

    IFileSystem getFileSystem( String devName );

    IFileSystem[] listFileSystems();

    long getSwapUsed();

    long getSwapFree();

    long getSwapTotal();

    long getSwapPageIn();

    long getSwapPageOut();

    long getMemoryUsed();

    long getMemoryFree();

    long getMemoryActualUsed();

    long getMemoryActualFree();

    double getLoadAvrgLastMinute();

    double getLoadAvrgLastFiveMinutes();

    double getLoadAvrgLast15Minutes();

    /**
     * @return then CPU wait in % since the last poll
     * */
    double getCpuPercWait();

    /**
     * @return then CPU SYS in % since the last poll
     * */
    double getCpuPercSys();

    /**
     * @return then CPU USER in % since the last poll
     * */
    double getCpuPercUser();

    String[] listNetworkInterface();

    INetworkInterfaceStat getNetworkInterfaceStat( String ifName );

    long getTcpActiveOpens();

    long getTcpPassiveOpens();

    long getTcpAttemptFails();

    long getTcpEstabResets();

    long getTcpCurrEstab();

    long getTcpInSegs();

    long getTcpOutSegs();

    long getTcpRetransSegs();

    long getTcpOutRsts();

    long getTcpInErrs();

    long getNetstatTcpClose();

    long getNetstatTcpListen();

    long getNetstatTcpSynSent();

    long getNetstatTcpSynRecv();

    long getNetstatTcpEstablished();

    long getNetstatTcpCloseWait();

    long getNetstatTcpLastAck();

    long getNetstatTcpFinWait1();

    long getNetstatTcpFinWait2();

    long getNetstatTcpClosing();

    long getNetstatTcpTimeWait();

    long getNetstatTcpBound();

    long getNetstatTcpIdle();

    long getNetstatTcpInboundTotal();

    long getNetstatTcpOutboundTotal();

    long getProcessCpuTimeRunningUser( Long pid );

    long getProcessCpuTimeRunningKernel( Long pid );

    long getProcessCpuTimeRunningTotal( Long pid );

    double getProcessVirtualMemory( Long pid );

    double getProcessResidentMemory( Long pid );

    double getProcessSharedMemory( Long pid );

    long getProcessMemoryPageFaults( Long pid );

    void loadProcs();

    long[] getProcList();

    String[] getProcArgs( long pid );

    IProcessInformation getProcessInformation( long pid );

}
