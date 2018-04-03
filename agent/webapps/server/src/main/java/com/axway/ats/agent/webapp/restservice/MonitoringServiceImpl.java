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
package com.axway.ats.agent.webapp.restservice;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.axway.ats.agent.webapp.restservice.model.SessionData;
import com.axway.ats.agent.webapp.restservice.model.pojo.BasePojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.ErrorPojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.ScheduleCustomJvmMonitoringPojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.ScheduleJvmMonitoringPojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.ScheduleMonitoringPojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.ScheduleProcessMonitoringPojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.ScheduleSystemMonitoringPojo;
import com.axway.ats.agent.webapp.restservice.model.pojo.StartMonitoringPojo;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.core.threads.ThreadsPerCaller;

/**
 * Entry class for any monitoring operations
 */
@Path( "monitoring")
public class MonitoringServiceImpl extends BaseRestServiceImpl {

    public static final String SESSION_DATA_ATTRIB_NAME = "sessionData";

    /**
     * Initialize Monitoring context Must be called before calling any
     * scheduleXYZMonitoring REST method
     */
    @POST
    @Path( "initializeMonitoring")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response initializeMonitoring(
                                          @Context HttpServletRequest request,
                                          BasePojo basePojo ) {

        final String caller = getCaller(request, basePojo);
        ThreadsPerCaller.registerThread(caller);

        try {

            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            restSystemMonitor.initializeMonitoringContext(agent);

            return Response.ok("{\"status\":\"monitoring context initialized.\"}").build();

        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

    }

    @POST
    @Path( "scheduleSystemMonitoring")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response scheduleSystemMonitoring(
                                              @Context HttpServletRequest request,
                                              ScheduleSystemMonitoringPojo monitoringPojo ) {

        final String caller = getCaller(request, monitoringPojo);
        ThreadsPerCaller.registerThread(caller);

        try {

            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            Set<ReadingBean> readings = restSystemMonitor.scheduleSystemMonitoring(agent,
                                                                                   monitoringPojo.getReadings());

            restSystemMonitor.setScheduledReadingTypes(readings);

            return Response.ok("{\"status\":\"scheduled system monitoring for readings '"
                               + Arrays.toString(monitoringPojo.getReadings()) + "'\"}")
                           .build();
        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    @POST
    @Path( "scheduleMonitoring")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response scheduleMonitoring(
                                        @Context HttpServletRequest request,
                                        ScheduleMonitoringPojo monitoringPojo ) {

        final String caller = getCaller(request, monitoringPojo);
        ThreadsPerCaller.registerThread(caller);

        try {

            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            Set<ReadingBean> readings = restSystemMonitor.scheduleMonitoring(agent,
                                                                             monitoringPojo.getReading(),
                                                                             monitoringPojo.getReadingParametersAsMap());

            restSystemMonitor.setScheduledReadingTypes(readings);

            String readingParametersAsString = entrySetAsString(monitoringPojo.getReadingParametersAsMap());

            return Response.ok("{\"status\":\"scheduled monitoring for reading '"
                               + monitoringPojo.getReading() + "' and readingParameters '"
                               + readingParametersAsString + "'\"}")
                           .build();

        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

    }

    @POST
    @Path( "scheduleProcessMonitoring")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response scheduleProcessMonitoring(
                                               @Context HttpServletRequest request,
                                               ScheduleProcessMonitoringPojo monitoringPojo ) {

        final String caller = getCaller(request, monitoringPojo);
        ThreadsPerCaller.registerThread(caller);

        try {
            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            Set<ReadingBean> readings = restSystemMonitor.scheduleProcessMonitoring(agent,
                                                                                    monitoringPojo.getProcessPattern(),
                                                                                    monitoringPojo.getProcessAlias(),
                                                                                    monitoringPojo.getProcessUsername(),
                                                                                    monitoringPojo.getProcessReadingTypes());

            restSystemMonitor.setScheduledReadingTypes(readings);

        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        String statusMessage = "{\"status \": \"scheduled process monitoring with parameters '"
                               + monitoringPojo.toString() + "'\"}";

        return Response.ok(statusMessage).build();
    }

    @POST
    @Path( "scheduleChildProcessMonitoring")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response scheduleChildProcessMonitoring(
                                                    @Context HttpServletRequest request,
                                                    ScheduleProcessMonitoringPojo monitoringPojo ) {

        final String caller = getCaller(request, monitoringPojo);
        ThreadsPerCaller.registerThread(caller);

        try {
            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            Set<ReadingBean> readings = restSystemMonitor.scheduleChildProcessMonitoring(agent,
                                                                                         monitoringPojo.getParentProcess(),
                                                                                         monitoringPojo.getProcessPattern(),
                                                                                         monitoringPojo.getProcessAlias(),
                                                                                         monitoringPojo.getProcessUsername(),
                                                                                         monitoringPojo.getProcessReadingTypes());

            restSystemMonitor.setScheduledReadingTypes(readings);

        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        String statusMessage = "{\"status \": \"scheduled child process monitoring with parameters '"
                               + monitoringPojo.toString() + "'\"}";

        return Response.ok(statusMessage).build();
    }

    @POST
    @Path( "scheduleJvmMonitoring")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response scheduleJvmMonitoring(
                                           @Context HttpServletRequest request,
                                           ScheduleJvmMonitoringPojo monitoringPojo ) {

        final String caller = getCaller(request, monitoringPojo);
        ThreadsPerCaller.registerThread(caller);

        try {

            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            Set<ReadingBean> readings = restSystemMonitor.scheduleJvmMonitoring(agent,
                                                                                monitoringPojo.getJvmPort(),
                                                                                (monitoringPojo.getAlias() == null)
                                                                                                                    ? ""
                                                                                                                    : monitoringPojo.getAlias(),
                                                                                monitoringPojo.getJvmReadingTypes());

            restSystemMonitor.setScheduledReadingTypes(readings);

        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        String statusMessage = "{\"status \": \"scheduled JVM monitoring with parameters '"
                               + monitoringPojo.toString() + "'\"}";

        return Response.ok(statusMessage).build();
    }

    @POST
    @Path( "scheduleCustomJvmMonitoring")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response scheduleCustomJvmMonitoring(
                                                 @Context HttpServletRequest request,
                                                 ScheduleCustomJvmMonitoringPojo monitoringPojo ) {

        final String caller = getCaller(request, monitoringPojo);
        ThreadsPerCaller.registerThread(caller);

        try {

            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            Set<ReadingBean> readings = restSystemMonitor.scheduleCustomJvmMonitoring(agent,
                                                                                      monitoringPojo.getJmxPort(),
                                                                                      monitoringPojo.getAlias(),
                                                                                      monitoringPojo.getMbeanName(),
                                                                                      monitoringPojo.getUnit(),
                                                                                      monitoringPojo.getMbeanAttributes());

            restSystemMonitor.setScheduledReadingTypes(readings);

        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        String statusMessage = "{\"status \": \"scheduled custom JVM monitoring with parameters '"
                               + monitoringPojo.toString() + "'\"}";

        return Response.ok(statusMessage).build();
    }

    @POST
    @Path( "scheduleUserActivity")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response scheduleUserActivity(
                                          @Context HttpServletRequest request,
                                          BasePojo basePojo ) {

        final String caller = getCaller(request, basePojo);
        ThreadsPerCaller.registerThread(caller);

        try {

            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            restSystemMonitor.scheduleUserActivity(agent);

        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        String statusMessage = "{\"status \": \"scheduled user activity monitoring.\"}";

        return Response.ok(statusMessage).build();
    }

    @POST
    @Path( "startMonitoring")
    @Consumes( MediaType.APPLICATION_JSON)
    @Produces( MediaType.APPLICATION_JSON)
    public Response startMonitoring(
                                     @Context HttpServletRequest request,
                                     StartMonitoringPojo monitoringPojo ) {

        final String caller = getCaller(request, monitoringPojo);
        ThreadsPerCaller.registerThread(caller);

        try {

            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            // calculate the time offset between the agent and the test executor
            long timeOffset = System.currentTimeMillis() - monitoringPojo.getStartTimestamp();

            restSystemMonitor.startMonitoring(agent,
                                              monitoringPojo.getStartTimestamp(),
                                              monitoringPojo.getPollingInterval(),
                                              timeOffset);

            return Response.ok("{\"status\":\"monitoring started on every "
                               + monitoringPojo.getPollingInterval() + " seconds.\"}")
                           .build();
        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

    }

    @POST
    @Path( "stopMonitoring")
    @Produces( MediaType.APPLICATION_JSON)
    public Response stopMonitoring(
                                    @Context HttpServletRequest request,
                                    BasePojo basePojo ) {

        final String caller = getCaller(request, basePojo);
        ThreadsPerCaller.registerThread(caller);
        try {
            SessionData sd = getSessionData(caller);

            RestSystemMonitor restSystemMonitor = sd.getSystemMonitor();

            String agent = request.getLocalAddr() + ":" + request.getLocalPort();

            restSystemMonitor.stopMonitoring(agent);
        } catch (Exception e) {
            return Response.serverError().entity(new ErrorPojo(e)).build();
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        return Response.ok("{\"status\":\"monitoring stopped.\"}").build();
    }

    private String entrySetAsString(
                                     Map<String, String> readingParameters ) {

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Iterator<String> it = readingParameters.values().iterator();
        while (it.hasNext()) {
            String value = it.next();
            sb.append(value).append(", ");
        }
        String str = sb.substring(0, sb.length() - 1) + "]";
        return str;
    }

}
