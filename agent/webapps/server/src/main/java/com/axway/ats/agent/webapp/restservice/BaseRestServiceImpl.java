/*
 * Copyright 2017-2022 Axway Software
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
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.log.AtsDbLogger;

public class BaseRestServiceImpl {

    /* A new session is created on initializing a DB connection 
     * and is discarded again when initializing DB connection if it has expired.
    */

    /** skip test for checking if ActiveDbAppender is presented in test executor's log4j.xml **/
    private AtsDbLogger                       dbLog    = AtsDbLogger.getLogger(BaseRestServiceImpl.class.getName(),
                                                                               true);

    public static Map<String, SessionData> sessions = Collections.synchronizedMap(new HashMap<String, SessionData>());

    /**
     * This is an initial call from this caller.
     * It is not expected to provide identity information with this request, and
     * we will not even check for this.
     *
     * Instead, we will create identity information for this caller and a new session
     * will be initialized.
     *
     * @param request
     * @param basePojo
     * @return
     */
    protected String getCallerForNewSession( HttpServletRequest request, BasePojo basePojo ) {

        // create new id, it is a random token
        String uid = UUID.randomUUID().toString();
        // apply it to the current communication
        basePojo.setUid( uid );

        // calculate the caller identity
        String caller = ExecutorUtils.createExecutorId(request.getRemoteAddr(), uid,
                                                       getExecutorThreadId( request, basePojo ) );

        // create a new session for this caller
        // due to the random part of the caller string, we do not expect to have an existing session
        sessions.put( caller, new SessionData() );
        dbLog.info( "A new session is created for '" + caller + "'" );

        return caller;
    }

    /**
     * It is expected that we already know this caller and this is proven
     * by the presence of a particular data in header or body.
     * Exception is thrown if this is not the case.
     *
     * @param request
     * @param basePojo
     * @return
     */
    protected String getCaller( HttpServletRequest request, BasePojo basePojo ) {

        String uid = getUid( request, basePojo );
        if ( uid == null ) {
            throw new SessionNotFoundException( "'" + ExecutorUtils.ATS_RANDOM_TOKEN
                                                + "' not found in neither request headers, nor request body." );
        }
        return ExecutorUtils.createExecutorId( request.getRemoteAddr(), uid,
                                               getExecutorThreadId( request, basePojo ) );
    }

    /**
     * Get SessionData for specific caller
     * @param caller
     * @return
     * @throws SessionNotFoundException
     */
    protected SessionData getSessionData( String caller ) throws SessionNotFoundException {

        SessionData sd = null;

        sd = sessions.get( caller );
        if( sd == null ) {
            throw new SessionNotFoundException( "Could not obtain session with id '" + caller
                                                + "'. Please check the documentation." );
        }
        sd.updateLastUsedFlag();
        return sd;
    }

    /*private String getUid(
                           HttpServletRequest request,
                           BasePojo basePojo,
                           boolean generateNewIUD ) throws Exception {

        // get ATS_UID from request header
        String uid = request.getHeader(ApplicationContext.ATS_UID_SESSION_TOKEN);
        if (uid == null) {
            // get ATS_UID from request body
            uid = basePojo.getUid();
            if (uid == null) {
                *//*
                * we create new uid only in initializeDbConnection REST method.
                *//*
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
*/
    private String getUid( HttpServletRequest request, BasePojo basePojo ) {

        // get UID from request header
        String uid = request.getHeader( ExecutorUtils.ATS_RANDOM_TOKEN );
        if( uid == null ) {
            // get UID from request body
            uid = basePojo.getUid();
        }

        basePojo.setUid( uid );
        return uid;
    }

    private String getExecutorThreadId( HttpServletRequest request, BasePojo basePojo ) {

        // get Executor Thread ID from request header
        String threadId = request.getHeader( ExecutorUtils.ATS_THREAD_ID );
        if( threadId == null ) {
            // get Executor Thread ID from request body
            return basePojo.getThreadId();
        }

        return threadId;
    }

}
