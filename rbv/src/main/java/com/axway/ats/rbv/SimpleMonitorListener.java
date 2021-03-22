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
package com.axway.ats.rbv;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.model.RbvException;

public class SimpleMonitorListener implements MonitorListener {

    private static final Logger log = LogManager.getLogger(SimpleMonitorListener.class);

    private List<Monitor>       monitors;
    private int                 totalNumMonitors;
    private int                 numFinishedMonitors;
    private boolean             isEvaluating;

    public SimpleMonitorListener( List<Monitor> monitors ) {

        this.monitors = monitors;
        isEvaluating = false;
    }

    /**
     * Run evaluation on all monitors
     * 
     * @param timeout
     * @return the last error reported by each monitor or null if no error
     * @throws RbvException
     */
    public String evaluateMonitors(
                                    long timeout ) throws RbvException {

        if (isEvaluating) {
            throw new RbvException("Trying to start SimpleMonitor, but it is already running and is not finished");
        }

        totalNumMonitors = monitors.size();
        numFinishedMonitors = 0;
        isEvaluating = true;

        try {
            synchronized (this) {
                //start all monitors
                for (Monitor monitor : monitors) {
                    monitor.start(this);
                }

                wait(timeout);
            }

            //cancel any remaining monitors
            if (numFinishedMonitors != totalNumMonitors) {
                cancelAllMonitors();
            }

            //first check if the timeout has been exceeded
            if (isEvaluating) {
                log.error("Monitors did not finish in the given timeout " + timeout);

                isEvaluating = false;
                return "Monitors did not finish in the given timeout " + timeout;
            }

        } catch (InterruptedException ie) {
            log.debug("InterruptedException has been thrown");
        }

        // we will return null if no error appeared
        boolean first = true;
        StringBuilder error = new StringBuilder();
        for (Monitor monitor : monitors) {
            String lastError = monitor.getLastError();
            if (!StringUtils.isNullOrEmpty(lastError)) {
                if (first) {
                    first = false;
                } else {
                    error.append("; ");
                }
                error.append(lastError);
            }
        }

        if (error.length() == 0) {
            return null;
        } else {
            return error.toString();
        }
    }

    public void setFinished(
                             String monitorName,
                             boolean result ) {

        if (result == true) {

            numFinishedMonitors++;

            //check if all monitors have finished execution
            if (numFinishedMonitors == totalNumMonitors) {

                log.info("Matched all rules - evaluation passed!");

                synchronized (this) {
                    //notify the waiting thread
                    isEvaluating = false;

                    notify();
                }
            }

        } else {
            //if one of the monitors fails we stop execution
            log.info("Monitor '" + monitorName + "' finished unsuccessfully - evaluation failed");

            synchronized (this) {
                //notify the waiting thread
                isEvaluating = false;

                notify();
            }
        }
    }

    private void cancelAllMonitors() {

        //cancel all the other monitors
        for (Monitor monitor : monitors) {
            try {
                monitor.cancelExecution();
            } catch (RbvException e) {
                //just log a warning here, because we've already set the result to false
                log.warn("Monitor '" + monitor.getName() + "' could not be cancelled", e);
            }
        }
    }
}
