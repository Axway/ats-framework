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

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;

/**
 * Thread for stopping the server and shutting down the JVM after
 * the execution of the main application finishes.
 */
public class CleaningThread extends Thread {

    private static final Logger log = LogManager.getLogger(CleaningThread.class);

    private final Server        server;
    private final Set<Long>     threadIDs;

    public CleaningThread( Server server,
                           Set<Long> threadIDs ) {

        super("JettyAgentCleaner");

        this.server = server;
        this.threadIDs = threadIDs;

        setDaemon(true);
    }

    @Override
    public void run() {

        log.info("Starting Cleaner Thread for the Jetty Web Container Agent.");

        int exitCode = 0;

        boolean loop = true;
        try {
            while (loop) {
                List<Thread> allThreads = ThreadUtils.getInstance().getAllThreads();
                loop = false;

                for (Thread t : allThreads) {
                    // daemon: skip it.
                    if (t.isDaemon()) {
                        log.debug("Skipping daemon thread: " + t.getName() + " [id=" + t.getId() + "]");
                        continue;
                    }

                    if (t.getName().startsWith("DestroyJavaVM")) {
                        log.debug("Skipping destroy thread thread: " + t.getName() + " [id=" + t.getId()
                                  + "]");
                        continue;
                    }

                    // skip the threads that were started with the Jetty server
                    if (threadIDs.contains(t.getId())) {
                        log.debug("Skipping Jetty thread: " + t.getName() + " [id=" + t.getId() + "]");
                        continue;
                    }

                    // Non daemon, non server: join it, break the for
                    // loop, continue in the while loop (loop=true)
                    loop = true;
                    try {
                        log.info("Waiting on thread " + t.getName() + " [id=" + t.getId() + "]");
                        t.join();
                    } catch (Exception ex) {
                        log.error("Error when joining thread.", ex);
                    }
                    break;
                }
            }
            // We went through a whole for-loop without finding any thread
            // to join. We can close the server and exit the JVM since all the
            // threads of the main application should have finished.
        } catch (Exception ex) {
            exitCode = -1;
            log.error("Unexpected exception while waitnig for the main application to finish.", ex);
        } finally {
            try {
                // if we reach here it means the only non-daemon threads
                // that remain are the Jetty server threads - or that we got an
                // unexpected exception/error.
                // Stop the server and exit the JVM.
                log.info("Stopping the Jetty server.");
                server.stop();
            } catch (Exception ex) {
                log.error("Error closing the server.", ex);
            }

            log.info("Exiting the JVM.");
            Runtime.getRuntime().exit(exitCode);
        }
    }
}
