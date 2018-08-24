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
 * It is used in each XYZRestEntryPoint class, LocalExecutor and LocalLoadExecutor classes.
 */
public class ThreadsPerCaller {

    private static Map<String, String> threads = new HashMap<String, String>();

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

        return threads.get(Thread.currentThread().getName());
    }
}
