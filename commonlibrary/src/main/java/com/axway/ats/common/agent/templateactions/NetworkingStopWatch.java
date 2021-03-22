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
package com.axway.ats.common.agent.templateactions;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tracks network(and server) times during execution of template actions.
 * Wraps state transitions
 *<pre>
 * 0. setNewContext - phase0
 *  1. Sending request
 *  1.1. 1st measure: urlConnection.getOutputStream(): Open connection for - resume and suspend NET timer (on 2-3 places - plain HTTP request and AMF ones)
 *     step1_start
 *     step2_end
 *  1.2. 2nd real data: resume &amp; suspend NET timer
 *     step3_start
 *     step4_end
 *  1.3. Store request time
 *
 * 2. Intermediate timer - for non-data actions after sending request and before asking for response data.
 * Primarily for new XML Document creation...
 *  2.1. Start right after request is sent (interim, 1)
 *     step5_
 * 3. Get response
 *   3.1.1 Get response code - suspend interm. timer and resume NET time; After get - resume back to interim. timer. (interim, 2)
 *     step6_start
 *     step7_end
 *   3.1.2  Get response data - stop interim timer and start NET timer. At the end stop NET timer.
 *     step8_start
 *     step9_end
 *  Note: For HTTP get response code and body are on same place so no interim timer resume.
 * </pre>
 *
 *
 */
public class NetworkingStopWatch {

    public static final String netTimeLoggerStr        = "com.axway.ats.common.agent.templateactions.wireTimer";
    public static final Logger logTimer                = LogManager.getLogger(netTimeLoggerStr);

    /**
     * Timer for network IO including possible server processing time
     */
    private StopWatch          timerNetAndServerProcessingTime;
    /**
     * Agent processing time. Works between request end and response start.
     */
    private StopWatch          timerBetweenReqAndResp;
    int                        currentActionStepNumber = 0;
    private String             currentActionName;

    public NetworkingStopWatch( String templateActionName ) {

        timerNetAndServerProcessingTime = new StopWatch();
        timerBetweenReqAndResp = new StopWatch();
        currentActionName = templateActionName;
    }

    public long getNetworkingTime() {

        return timerNetAndServerProcessingTime.getTime();

    }

    public long getTimeBetweenReqAndResponse() {

        return timerBetweenReqAndResp.getTime();

    }

    /**
     * METHODS FOR CHANGING COMMUNICATION PHASES
     */

    /**
     * Reset context for timer reuse. Starting new request/response
     * @param actionNameStepWithNumber
     */
    public void step0_SetNewContext(
                                     String actionNameStepWithNumber ) {

        if (logTimer.isTraceEnabled()) {
            logTimer.trace("Starting new step " + actionNameStepWithNumber + " and reset timers");
        }
        currentActionName = actionNameStepWithNumber;
        timerNetAndServerProcessingTime.reset();
        timerBetweenReqAndResp.reset();
    }

    public void step1_OpenConnectionForRequest() {

        timerNetAndServerProcessingTime.start();
    }

    public void step2_OpenedConnectionForRequest() {

        timerNetAndServerProcessingTime.suspend();
    }

    public void step3_StartSendingRequest() {

        timerNetAndServerProcessingTime.resume();

    }

    public void step4_EndSendingRequest() {

        timerNetAndServerProcessingTime.suspend();

    }

    /**
     * Set internal state from initial state (after step 0, before request) to state after step 4 (request data sent)
     */
    public void setStateFromBeforeStep1ToAfterStep4() {

        timerNetAndServerProcessingTime.start();
        timerNetAndServerProcessingTime.suspend();
        if (timerNetAndServerProcessingTime.getTime() > 20) { // if start/suspend delay is relatively big
            logTimer.warn("Due to thread delay network timer intially starts with "
                          + timerNetAndServerProcessingTime.getTime()
                          + "ms more. Probably the system is too much loaded.");
        }
    }

    /**
     * Start timer for work between end of request and begin to ask for response data
     */
    public void step5_StartInterimTimer() {

        if (logTimer.isTraceEnabled()) {
            logTimer.trace("This action step " + currentActionName + " request network time took "
                           + timerNetAndServerProcessingTime.getTime() + " ms");
        }
        timerBetweenReqAndResp.reset();
        timerBetweenReqAndResp.start();
    }

    public void step6_StartGetResponseCode() {

        timerBetweenReqAndResp.suspend();
        timerNetAndServerProcessingTime.resume();
    }

    /**
     * End get response code and resume back interim timer
     */
    public void step7_EndGetResponseCode() {

        timerNetAndServerProcessingTime.suspend();
        timerBetweenReqAndResp.resume();
    }

    /**
     * Stop timer between req. and resp. and resume timer for getting net data
     */
    public void step8_stopInterimTimeAndStartReceivingResponseData() {

        timerBetweenReqAndResp.stop();
        timerNetAndServerProcessingTime.resume();

    }

    public void step9_endReceivingResponseData() {

        timerNetAndServerProcessingTime.stop();

    }

    /**
     * Return used StopWatch for network. <br>
     * <em>Note:</em> To be used only to pass StopWatch to Common library methods
     * to serialize objects to AMF stream.
     * @return StopWatch
     */
    public StopWatch getNetTimer() {

        return timerNetAndServerProcessingTime;
    }

}
