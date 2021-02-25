/*
 * Copyright 2017-2021 Axway Software
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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;

import com.axway.ats.agent.core.MultiThreadedActionHandler;
import com.axway.ats.agent.core.context.ApplicationContext;
import com.axway.ats.agent.webapp.restservice.model.SessionData;
import com.axway.ats.agent.webapp.restservice.model.pojo.BasePojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.DbConnectionPojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.ErrorPojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.JoinTestcasePojo;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.ClasspathUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.Log4j2Utils;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.axway.ats.log.appenders.PassiveDbAppender.PassiveDbAppenderBuilder;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.TestCaseState;
import com.axway.ats.log.model.SystemLogLevel;

/**
 * Entry class for configurating the agent
 */
@Path( "configuration")
public class AgentConfigurationServiceImpl extends BaseRestServiceImpl {

    /** skip test for checking if ActiveDbAppender is presented in test executor's log4j2.xml **/
    protected AtsDbLogger     dbLog                 = AtsDbLogger.getLogger(AgentConfigurationServiceImpl.class.getName(),
                                                                            true);

    private static int        lastRunId             = -1;

    // A session will expire and will be discarded if not used for the specified
    // idle time - currently one week
    private static final long SESSION_MAX_IDLE_TIME = 7 * 24 * 60 * 60 * 1000;

    /**
     * This method must be called, in order a connection to the desired DB to be
     * initialized. If there are any previous initialized DB connections from
     * the same caller, they will be discarded.
     */
    @POST
    @Path( "initializeDbConnection")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response initializeDbConnection(
                                            @Context HttpServletRequest request,
                                            DbConnectionPojo dbConnectionPojo ) {

        // we do some cleanup here, it is not be very resource consuming
        cleanupExpiredSessions(dbConnectionPojo);

        final String caller = getCaller(request, dbConnectionPojo, true);
        ThreadsPerCaller.registerThread(caller);
        try {
            // we have to create a new session,
            // so we call getSessionData() only for that
            getSessionData(request, dbConnectionPojo);

            // create DbAppenderConfiguration
            DbAppenderConfiguration newAppenderConfiguration = new DbAppenderConfiguration();
            newAppenderConfiguration.setHost(dbConnectionPojo.getDbHost());
            newAppenderConfiguration.setPort(dbConnectionPojo.getDbPort());
            newAppenderConfiguration.setDatabase(dbConnectionPojo.getDbName());
            newAppenderConfiguration.setUser(dbConnectionPojo.getDbUser());
            newAppenderConfiguration.setPassword(dbConnectionPojo.getDbPass());
            newAppenderConfiguration.setMode(dbConnectionPojo.getMode());
            newAppenderConfiguration.setLoggingThreshold(SystemLogLevel.toLevel(dbConnectionPojo.getLoggingThreshold()));
            newAppenderConfiguration.setMaxNumberLogEvents(dbConnectionPojo.getMaxNumberLogEvents());

            PassiveDbAppender alreadyExistingAppender = PassiveDbAppender.getCurrentInstance(caller);
            // check whether PassiveDbAppender for this caller is already registered
            if (alreadyExistingAppender != null) {
                // check if the already registered PassiveDbAppender's apenderConfiguration is NOT the same and the new one
                if (!alreadyExistingAppender.getAppenderConfig().equals(newAppenderConfiguration)) {
                    /* we have a request for different DB configuration, 
                     * so remove the previous appender and append new one with the desired appender configuration
                     */
                    dbLog.debug("Remove previously attached PassiveDbAppender for caller '" + caller
                                + "'.");

                    PassiveDbAppender appender = PassiveDbAppender.getCurrentInstance(caller);
                    appender.stop();
                    Log4j2Utils.removeAppenderFromLogger(Log4j2Utils.getRootLogger().getName(), appender.getName());
                    attachPassiveDbAppender(newAppenderConfiguration, dbConnectionPojo.getTimestamp());
                    dbLog.debug("Successfully attached new PassiveDbAppender for caller '" + caller + "'.");
                }
            } else {
                attachPassiveDbAppender(newAppenderConfiguration, dbConnectionPojo.getTimestamp());
            }
        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        String uid = dbConnectionPojo.getUid();
        String agentVersion = AtsVersion.getAtsVersion();
        return Response.ok("{\"" + ApplicationContext.ATS_UID_SESSION_TOKEN + "\": " + "\"" + uid + "\",\""
                           + "agent_version" + "\": " + "\"" + agentVersion + "\"}")
                       .build();
    }

    /**
     * Calling this method, will discard the last DB connection
     */
    @POST
    @Path( "deinitializeDbConnection")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response deinitializeDbConnection(
                                              @Context HttpServletRequest request,
                                              BasePojo basePojo ) {

        final String caller = getCaller(request, basePojo, false);
        ThreadsPerCaller.registerThread(caller);
        try {
            PassiveDbAppender appender = PassiveDbAppender.getCurrentInstance(caller);
            appender.stop();
            Log4j2Utils.removeAppenderFromLogger(Log4j2Utils.getRootLogger().getName(), appender.getName());
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        dbLog.debug("PassiveDbAppender is successfully detached.");

        return Response.ok("{\"status\": \"db connection deinitialized.\"}").build();
    }

    /**
     * Tell the DbEventRequestProcess which run and test must receive the DB
     * messages/data
     */
    @POST
    @Path( "joinTestcase")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response joinTestcase(
                                  @Context HttpServletRequest request,
                                  JoinTestcasePojo testCaseStatePojo ) {

        final String caller = getCaller(request, testCaseStatePojo, false);
        ThreadsPerCaller.registerThread(caller);

        try {
            SessionData sd = getSessionData(request, testCaseStatePojo);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            // cancel all action tasks, that are started on an agent, located on
            // the current caller host.
            // current caller and the agent must have the same IP, in order for
            // the queue to be cancelled
            dbLog.debug("Cancelling all action task on the agent, that were started form the current caller.");
            MultiThreadedActionHandler.cancellAllQueuesFromAgent(ThreadsPerCaller.getCaller());

            // cancel all running system monitoring tasks on the agent
            dbLog.debug("Cancelling all running system monitoring tasks on the agent, that were started form the current caller.");
            String agent = request.getLocalAddr() + ":" + request.getLocalPort();
            restSystemMonitor.stopMonitoring(agent);

            TestCaseState newTestCaseState = new TestCaseState();
            newTestCaseState.setRunId(testCaseStatePojo.getRunId());
            newTestCaseState.setTestcaseId(testCaseStatePojo.getTestcaseId());
            newTestCaseState.setLastExecutedTestcaseId(testCaseStatePojo.getLastExecutedTestcaseId());

            // get the current state on the agent
            TestCaseState currentState = dbLog.getCurrentTestCaseState();
            boolean joinToNewTescase = true;
            if (currentState != null && currentState.isInitialized()) {
                /* This agent is already configured.
                 *
                 * Now check if the state is the same as the new one, this would mean we are trying to
                 * configure this agent for second time.
                 * This is normal as we get here when Test Executor or another agent calls this agent for first time.
                 *
                 * If the state is different, we hit an error which means this agent did not get On Test End event
                 * for the previous test case.
                 */
                if (!currentState.equals(newTestCaseState)) {

                    dbLog.error("This test appears to be aborted by the user on the test executor side, but it kept running on the agent side."
                                + " Now we cancel any further logging from the agent.");
                    dbLog.leaveTestCase();
                } else {
                    joinToNewTescase = false;

                }
            }

            if (joinToNewTescase) {

                /* previous RestSystemMonitor instance is still in the sessionData for that caller 
                 * so we create new RestSystemMonitor for this caller
                 * */
                restSystemMonitor = new RestSystemMonitor();
                sd.setSystemMonitor(restSystemMonitor);
                dbLog.joinTestCase(newTestCaseState);

                logClassPath(newTestCaseState);

                return Response.ok("{\"status\": \"testcase joined.\"}").build();
            } else {

                return Response.ok("{\"status\": \"testcase already joined.\"}").build();
            }

        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

    }

    /**
     * Clear the run and testcase data in the DbEventRequestProcess
     */
    @POST
    @Path( "leaveTestcase")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response leaveTestcase(
                                   @Context HttpServletRequest request,
                                   BasePojo basePojo ) {

        final String caller = getCaller(request, basePojo, false);
        ThreadsPerCaller.registerThread(caller);
        try {
            dbLog.leaveTestCase();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        return Response.ok("{\"status\": \"testcase left.\"}").build();
    }

    /**
     * @return the ATS framework version
     */
    @GET
    @Path( "getAtsVersion")
    @Produces( MediaType.TEXT_PLAIN)
    public Response getAtsVersion() {

        return Response.ok(AtsVersion.getAtsVersion()).build();
    }

    private void cleanupExpiredSessions(
                                         DbConnectionPojo dbConnectionPojo ) {

        Set<String> keySet = new HashSet<>(sessions.keySet());
        for (String uid : keySet) {
            long lastTimeUsed = sessions.get(uid).getLastUsedFlag();
            if (new Date().getTime() - lastTimeUsed > SESSION_MAX_IDLE_TIME) {

                StringBuilder sb = new StringBuilder();
                sb.append("The following session is discarded as it has not been used for the last ");
                sb.append(SESSION_MAX_IDLE_TIME / 1000);
                sb.append(" seconds:");
                sb.append("\nATS ID: ");
                sb.append(uid);

                dbLog.warn(sb.toString());

                sessions.remove(uid);
            }
        }
    }

    private void attachPassiveDbAppender(
                                          DbAppenderConfiguration appenderConfiguration,
                                          long timestamp ) {

        // create the new appender
        PassiveDbAppenderBuilder builder = PassiveDbAppender.newBuilder();
        //use a default pattern, as we log in the db
        builder.setLayout(org.apache.logging.log4j.core.layout.PatternLayout.newBuilder()
                                                                            .withPattern("%c{2}: %m%n")
                                                                            .build());

        builder.setChunkSize(appenderConfiguration.getChunkSize());
        builder.setDatabase(appenderConfiguration.getDatabase());
        builder.setDriver(appenderConfiguration.getDriver());
        builder.setEnableCheckpoints(appenderConfiguration.getEnableCheckpoints());
        builder.setEvents(appenderConfiguration.getMaxNumberLogEvents());
        builder.setFilter(ThresholdFilter.createFilter(appenderConfiguration.getLoggingThreshold(),
                                                       Filter.Result.ACCEPT, Filter.Result.DENY));
        builder.setHost(appenderConfiguration.getHost());
        builder.setMode( (appenderConfiguration.isBatchMode())
                                                               ? "batch"
                                                               : "");
        builder.setName(PassiveDbAppender.class.getSimpleName());
        builder.setPassword(appenderConfiguration.getPassword());
        builder.setPort(Integer.parseInt(appenderConfiguration.getPort()));
        builder.setUser(appenderConfiguration.getUser());

        PassiveDbAppender attachedAppender = builder.build();
        // calculate the time stamp offset, between the test executor and the agent
        attachedAppender.calculateTimeOffset(timestamp);

        attachedAppender.setAppenderConfig(appenderConfiguration);

        attachedAppender.start();

        // attach the appender to the logging system
        Logger log = Log4j2Utils.getRootLogger();
        Log4j2Utils.setRootLevel(SystemLogLevel.toLevel(appenderConfiguration.getLoggingThreshold().intLevel()));
        // yet again could this cast fail?!?
        ((org.apache.logging.log4j.core.Logger) log).addAppender(attachedAppender);

    }

    private void logClassPath(
                               TestCaseState testCaseState ) {

        // this check is made so we log the classpath just once per run
        if (testCaseState.getRunId() != lastRunId) {

            lastRunId = testCaseState.getRunId();
            StringBuilder classpath = new StringBuilder();

            classpath.append("ATS Agent classpath on \"");
            classpath.append(HostUtils.getLocalHostIP());
            classpath.append("\" : \n");
            classpath.append(new ClasspathUtils().getClassPathDescription());

            dbLog.info(classpath, true);
        }
    }

}
