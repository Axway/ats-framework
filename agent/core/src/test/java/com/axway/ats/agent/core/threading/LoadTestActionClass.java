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
package com.axway.ats.agent.core.threading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;

public class LoadTestActionClass {

    public static int           numExecutions              = 0;
    static Map<String, Integer> numRunningActionsPerThread = new HashMap<String, Integer>();

    @Action(name = "sleep action")
    public void sleepAction(
                             @Parameter(name = "sleepTime" ) String sleepTime) throws InterruptedException {

        countExecutions();

        Thread.sleep( Integer.parseInt( sleepTime ) );
    }

    @Action(name = "running action")
    public void runningAction(
                               @Parameter(name = "runTime" ) String runTime) throws InterruptedException {

        countExecutions();
        countRunningActionsPerThread();

        int waiting = Integer.parseInt( runTime );
        long currentTime = System.currentTimeMillis();
        long endTime = currentTime + waiting;

        while( endTime >= currentTime ) {
            currentTime = System.currentTimeMillis();
        }
    }

    @Action
    public List<String> getAllThreads() {

        StringBuilder sb = new StringBuilder();
        sb.append( "THREADS: " );
        List<String> threads = new ArrayList<String>();

        for( Thread th : getAllThreadsOfInterest() ) {
            String threadName = th.getName();
            sb.append( threadName );
            sb.append( " - " );
            sb.append( th.getState() );
            sb.append( ", " );
            threads.add( th.getName() );
        }

        // LogManager.getLogger( LoadTestActionClass.class ).info( sb ); // uncomment if need to debug the current threads

        return threads;
    }

    private Set<Thread> getAllThreadsOfInterest() {

        Set<Thread> threadsOfInterest = new HashSet<Thread>();

        for( Thread th : Thread.getAllStackTraces().keySet() ) {
            String threadName = th.getName();
            if( threadName.startsWith( "pool" ) && th.isAlive() ) {
                threadsOfInterest.add( th );
            }
        }

        return threadsOfInterest;
    }

    private void countExecutions() {

        ++numExecutions;
    }

    private void countRunningActionsPerThread() {

        for( Thread th : getAllThreadsOfInterest() ) {
            if( !numRunningActionsPerThread.containsKey( th.getName() ) ) {
                numRunningActionsPerThread.put( th.getName(), 0 );
            }
            if( th.getName().equals( Thread.currentThread().getName() ) ) {
                int iterations = numRunningActionsPerThread.get( th.getName() );
                numRunningActionsPerThread.put( th.getName(), ++iterations );
            }
        }
    }
}
