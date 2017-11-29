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
package com.axway.ats.agentapp.standalone.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Singleton containing helper methods for browsing started threads in the JVM.
 */
public class ThreadUtils {

    /**
     * Reference to the root thread group so it wouldn't have to be
     * searched for every the the getRootThreadGroup method is invoked.
     */
    private ThreadGroup        rootThreadGroup;

    /**
     * The singleton instance.
     */
    private static ThreadUtils instance;

    private ThreadUtils() {

    }

    public static synchronized ThreadUtils getInstance() {

        if( instance == null ) {
            instance = new ThreadUtils();
        }
        return instance;
    }

    public Thread getThread(
                             final long id ) {

        List<Thread> threads = getAllThreads();
        for( Thread thread : threads ) {
            if( thread.getId() == id ) {
                return thread;
            }
        }

        return null;
    }

    /**
     * @return Set of the IDs of all threads that are started except the "main" thread.
     */
    public Set<Long> getAllThreadIDsExceptMain() {

        ThreadMXBean thbean = ManagementFactory.getThreadMXBean();
        // get the IDs of all live threads.
        long[] threadIDs = thbean.getAllThreadIds();
        Set<Long> res = new HashSet<Long>();

        // get the IDs of all threads except main
        for( long id : threadIDs ) {
            Thread t = getThread( id );
            if( t != null && !"main".equals( t.getName() ) ) {
                res.add( id );
            }
        }

        return res;
    }

    /**
     * @return a list of all threads started in the JVM.
     */
    public List<Thread> getAllThreads() {

        final ThreadGroup root = getRootThreadGroup();
        final ThreadMXBean thbean = ManagementFactory.getThreadMXBean();
        // get the number of all live threads
        int nAlloc = thbean.getThreadCount();
        int n = 0;
        Thread[] threads;

        do {
            nAlloc *= 2; // increase the size since more threads may have been created
            threads = new Thread[nAlloc];
            n = root.enumerate( threads, true ); // get all active threads from this thread group
        } while( n == nAlloc ); // stop if all active threads are enumerated

        List<Thread> res = new ArrayList<Thread>();
        for( Thread th : threads ) {
            res.add( th );
        }

        return res;
    }

    /**
     * @return the root thread group which has no parent thread group.
     */
    public ThreadGroup getRootThreadGroup() {

        if( rootThreadGroup != null ) {
            return rootThreadGroup;
        }

        rootThreadGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup ptg;

        while( ( ptg = rootThreadGroup.getParent() ) != null ) {
            rootThreadGroup = ptg;
        }

        return rootThreadGroup;
    }
}
