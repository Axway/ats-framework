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
package com.axway.ats.core.process;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.winp.WinProcess;

import com.axway.ats.common.process.ProcessExecutorException;
import com.axway.ats.common.system.OperatingSystemType;

/**
 * Utility class for working with processes.
 *
 * Using only instance methods, so no problem with thread synchronizations.
 */
public class ProcessUtils {

    private static final Logger log = LogManager.getLogger(ProcessUtils.class);

    private Process             theProcess;
    private String              commandDescription;

    public ProcessUtils( Process theProcess,
                         String commandDescription ) {

        this.theProcess = theProcess;
        this.commandDescription = commandDescription;
    }

    /**
     * Tries to stop started process. Started child processes are not affected.
     */
    public void killProcess() {

        if (theProcess != null) {
            theProcess.destroy();
            log.debug("Destroy call is sent to the process '" + commandDescription + "'");
        }
    }

    /**
     * Kill started process and makes best effort to kill its children processes.<br />
     * Warning is logged if issues is detected.<br />
     * Note that this might be invoked after some time if not sure that all child processes are already started.
     */
    public void killProcessAndItsChildren() {

        if (OperatingSystemType.getCurrentOsType().isUnix()) {

            // first kill child processes because otherwise their parent ID is changed to 1
            int pid = getProcessId();
            String command = "pkill -P " + pid;
            if (log.isDebugEnabled()) {
                log.debug("Try to destroy child processes with '" + command + "'");
            }
            int exitCode = -1;
            try {
                exitCode = Runtime.getRuntime().exec(command).waitFor();
            } catch (Exception e) {
                throw new ProcessExecutorException("Could not kill the process with id '" + pid + "'", e);
            }

            killProcess(); // kill this process

            if (exitCode != 0) {

                log.warn("Error while trying to kill subprocesses. Exit code returned from '" + command
                         + "' command: " + exitCode);
            }
        } else if (OperatingSystemType.getCurrentOsType().isWindows()) { // Windows assumed

            // use org.jvnet.winp.WinProcess
            log.debug("Windows detected and will try to kill whole subtree.");
            new WinProcess(theProcess).killRecursively();
        } else {

            throw new IllegalStateException("Not supported operating system type. Report the case to ATS team");
        }
    }

    /**
     * Tries to get the ID of the process started.
     * <em>Note:</em>Currently may work only on some UNIX variants with SUN/Oracle JDK.
     * @return The ID of the process. -1 in case of error.
     */
    public int getProcessId() {

        if ("java.lang.UNIXProcess".equals(theProcess.getClass().getName())) {
            try {
                Class<?> proc = theProcess.getClass();
                Field field = proc.getDeclaredField("pid");
                field.setAccessible(true);
                Object pid = field.get(theProcess);

                int pidNumber = (Integer) pid;
                log.info("The PID of the sterted process is " + pidNumber);
                return pidNumber;
            } catch (Exception e) {
                throw new ProcessExecutorException("Error retrieving process ID", e);
            }
        } else {
            return -1;
        }
    }
}
