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
package com.axway.ats.core.threads;

import java.util.HashMap;
import java.util.Map;

/**
 * This class keeps track of all the threads, that each caller has triggered.
 * It is used in AgentWsImpl, LocalExecutor and LocalLoadExecutor and pending in each XYZRestEntryPoint classes.
 */
public class ThreadsPerCaller {

    /**
     * Map <thread name : caller>
     */
    private static Map<String, String> threads = new HashMap<String, String>();

    /**
     * Keeps track of all the callers related to each still running thread on the agent
     * <br>Note that this map is never cleared. 
     * <br>This can cause some long-running tests to log messages in another test that was created by the same
     * Jetty request processing thread as the one that started the long-running test. So it is important to use
     * registerThread which adds caller (changes thread name)
     * 
     */
    private static InheritableThreadLocal<String> callers = new InheritableThreadLocal<>();

    /**
     * Register this thread for the given caller.
     * 
     * Should be called right after the thread start.
     * 
     * @param caller
     */
    synchronized public static void registerThread(
                                                    String caller ) {

        String currentThreadName = Thread.currentThread().getName();

        if (!currentThreadName.endsWith(caller)) {
            Thread.currentThread().setName(currentThreadName + "___" + caller);
        }

        callers.set(caller);
        threads.put(Thread.currentThread().getName(), caller);
    }

    /**
     * Unregister this thread.
     * 
     * Should be called right before existing the thread.
     */
    synchronized public static void unregisterThread() {
        String caller = getCaller();

        threads.remove(Thread.currentThread().getName());
        String currentThreadName = Thread.currentThread().getName();

        if (currentThreadName.endsWith(caller)) {
            Thread.currentThread().setName(currentThreadName.replace(caller, "").replace("___", ""));
        }
    }

    /**
     * Return the caller of this thread.
     * 
     * @return
     */
    synchronized public static String getCaller() {

        String caller = threads.get(Thread.currentThread().getName());
        if (caller != null) {
            return caller;
        } else {
            /*
             * If a thread starts or continues to log even after the request that created it is already processed
             * we have no registered caller.
             * We then will have to check if this thread or any of its parent thread have a caller in their ThreadLocal map.
             * Once a thread has such a caller, we will use it as a current caller.
             */
            caller = callers.get();
        }
        return caller;
    }
}
