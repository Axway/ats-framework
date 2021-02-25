/*
 * Copyright 2017-2020 Axway Software
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
package com.axway.ats.agent.webapp.restservice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.axway.ats.agent.core.context.ApplicationContext;
import com.axway.ats.agent.webapp.restservice.exceptions.SessionNotFoundException;
import com.axway.ats.agent.webapp.restservice.model.SessionData;
import com.axway.ats.agent.webapp.restservice.model.pojo.BasePojo;
import com.axway.ats.log.AtsDbLogger;

public class BaseRestServiceImpl {

    /* A new session is created on initializing a DB connection 
     * and is discarded again when initializing DB connection if it has expired.
    */

    /** skip test for checking if ActiveDbAppender is presented in test executor's log4j2.xml **/
    private AtsDbLogger                       dbLog    = AtsDbLogger.getLogger(BaseRestServiceImpl.class.getName(),
                                                                               true);

    public static Map<String, SessionData> sessions = Collections.synchronizedMap(new HashMap<String, SessionData>());

    protected String getCaller(
                                HttpServletRequest request,
                                BasePojo basePojo,
                                boolean generateNewIUD ) {

        String uid = null;

        try {
            uid = getUid(request, basePojo, generateNewIUD);
        } catch (Exception e) {
            dbLog.error("Could not get ATS UID for call from " + request.getRemoteAddr() + ".", e);
        }

        return "<Caller: " + request.getRemoteAddr() + "; ATS UID: " + uid + ">";
    }

    protected SessionData getSessionData(
                                          HttpServletRequest request,
                                          BasePojo basePojo ) throws Exception {

        String uid = null;
        SessionData sd = null;

        uid = getUid(request, basePojo, false);
        sd = sessions.get(uid);
        if (sd == null) {
            /*
             * new Session (SessionData) is created when:
             * <ul>
             * <li>initializeDbConnection is called - this is done, when monitoring is requested by REST API</li>
             * <li>initializeMonitoring is called - this is done when the ATS Framework is sending a system monitoring operation</li> 
             * </ul>
             *  
             * */
            if (request.getRequestURI().contains("initializeDbConnection") || request.getRequestURI().contains("initializeMonitoring")) {
                // create new session
                sd = new SessionData();
                sessions.put(uid, sd);
            } else {
                throw new SessionNotFoundException("Could not obtain session with uid '" + uid
                                                   + "'. It is possible that the agent had been restarted so no sessions are available");
            }
        }
        sd.updateLastUsedFlag();
        return sd;
    }

    private String getUid(
                           HttpServletRequest request,
                           BasePojo basePojo,
                           boolean generateNewIUD ) throws Exception {

        // get ATS_UID from request header
        String uid = request.getHeader(ApplicationContext.ATS_UID_SESSION_TOKEN);
        if (uid == null) {
            // get ATS_UID from request body
            uid = basePojo.getUid();
            if (uid == null) {
                /*
                * we create new uid only in initializeDbConnection REST method.
                */
                if (request.getRequestURI().contains("initializeDbConnection")) {
                    // because we use this method in each REST call,
                    // true is passed as a generateNewIUD argument only in
                    // initializeDbConnection REST method
                    if (generateNewIUD) {
                        uid = UUID.randomUUID().toString();
                    }
                } else {
                    throw new SessionNotFoundException(ApplicationContext.ATS_UID_SESSION_TOKEN
                                                       + " not found in neither request headers, nor request body.");
                }
            }
        }
        basePojo.setUid(uid);

        return uid;
    }

}
