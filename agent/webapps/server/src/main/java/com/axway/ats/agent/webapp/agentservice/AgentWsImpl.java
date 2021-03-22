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
package com.axway.ats.agent.webapp.agentservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import com.axway.ats.agent.core.ActionHandler;
import com.axway.ats.agent.core.EnvironmentHandler;
import com.axway.ats.agent.core.MainComponentLoader;
import com.axway.ats.agent.core.MultiThreadedActionHandler;
import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.action.CallerRelatedInfoRepository;
import com.axway.ats.agent.core.configuration.AgentConfigurator;
import com.axway.ats.agent.core.configuration.ConfigurationManager;
import com.axway.ats.agent.core.configuration.Configurator;
import com.axway.ats.agent.core.context.ApplicationContext;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.exceptions.InternalComponentException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.monitoring.queue.QueueExecutionStatistics;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.agent.webapp.restservice.BaseRestServiceImpl;
import com.axway.ats.agent.webapp.restservice.RestSystemMonitor;
import com.axway.ats.agent.webapp.restservice.model.SessionData;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.system.LocalSystemOperations;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.ClasspathUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.axway.ats.log.autodb.TestCaseState;

/**
 * Implementation of all public web methods used to communicate
 * with the ATS Agent.
 */
@WebService( name = "AgentService", targetNamespace = "http://agentservice/", serviceName = "AgentService", portName = "AgentServicePort")
public class AgentWsImpl {

    /** skip check whether ActiveDbAppender appender is presented in the test executors log4j2.xml in order to execute actions,
     * even if such appender is not presented
     */
    private static final AtsDbLogger log                               = AtsDbLogger.getLogger("com.axway.ats.agent.webapp.agentservice", true);

    private static int               lastRunId                         = -1;

    // flag to not log an error too often
    private static boolean           alreadyLoggedErrorAboutSessionUid = false;

    @Resource
    private WebServiceContext        wsContext;

    /**
     * Web method for starting a testcase
     *
     * @param testCaseState contains the id of the test case in the logging system (if passive logging is to be used, otherwise pass null)
     * @throws AgentException if any error occurs
     */
    /* TODO: 
     * add possibility for the user to set whether all queues from this agent have to be stopped 
     * each time a new run, from a different caller to the same agent are started
     */
    @WebMethod
    public void onTestStart(
                             @WebParam( name = "testCaseState") TestCaseState testCaseState ) throws AgentException {

        if (testCaseState == null) {
            // the user has not initialized our DB appender on the Test Executor side
            return;
        }

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            // cancel all action tasks, that are started on an agent, locate on the current caller host.
            // current caller and the agent must have the same IP, in order for queue to be cancelled
            MultiThreadedActionHandler.cancellAllQueuesFromAgent(caller);
            
            // cancel all monitoring, started previously by the current caller
            SessionData sd = BaseRestServiceImpl.sessions.get(caller);
            if(sd != null) {
                RestSystemMonitor sysMon = sd.getSystemMonitor();
                if(sysMon != null) {
                    log.info("Stopping previously started monitoring from caller '" + caller + "' ...");
                    sysMon.stopMonitoring(getAgentHostAddress());
                }
            }

            // get the current state on the remote machine
            TestCaseState currentState = log.getCurrentTestCaseState();
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
                if (!currentState.equals(testCaseState)) {

                    log.error("This test appears to be aborted by the user on the test executor side, but it kept running on the agent side."
                              + " Now we cancel any further logging from the agent.");
                    log.leaveTestCase();
                } else {
                    joinToNewTescase = false;

                }
            }

            if (joinToNewTescase) {
                // connect to the new test case
                log.joinTestCase(testCaseState);

                // take care of chained ATS agents(if there are any)
                TestcaseStateEventsDispacher.getInstance().onTestStart();
            }
            logClassPath(testCaseState);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Web method for ending a testcase
     */
    @WebMethod
    public void onTestEnd() {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        TestCaseState currentState = log.getCurrentTestCaseState();
        try {
            /* If the agent is not configured, this means we are coming here for second time during same test.
             * Ignore this event.
             */
            if (currentState != null && currentState.isInitialized()) {
                log.leaveTestCase();

                // take care of chained ATS agents(if there are any)
                TestcaseStateEventsDispacher.getInstance().onTestEnd();
            }
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
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

            log.info(classpath, true);
        }
    }

    /**
     * Cleanup the resources for a particular client side object instance
     *
     * @param internalProcessId
     */
    @WebMethod
    public void cleanupInternalObjectResources(
                                                String internalObjectResourceId ) {

        String caller = getCaller();

        CallerRelatedInfoRepository.getInstance(caller).removeObject(internalObjectResourceId);
    }

    /**
     * Web method for execution of Agent actions.
     *
     * @param componentName name of the Agent component
     * @param actionName name of the action to perform
     * @param args arguments - array of ArgumentWrapper
     * @return serialized returned result
     * @throws AgentException if any error occurs
     * @throws InternalComponentException if an exception occurs in the Agent action
     */
    @WebMethod
    public byte[] executeAction(
                                 @WebParam( name = "componentName") String componentName,
                                 @WebParam( name = "actionName") String actionName,
                                 @WebParam( name = "args") ArgumentWrapper[] args ) throws AgentException,
                                                                                    InternalComponentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            Object result = executeAction(caller, componentName, actionName, args);

            ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);
            objectOutStream.writeObject(result);

            return byteOutStream.toByteArray();
        } catch (Exception e) {
            handleExceptions(e);

            // should never reach this line because handleExceptions() will
            // always throw
            // but the compiler is not aware of this
            return null;
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * @param caller the IP of the WS caller
     * @param componentName name of the Agent component
     * @param actionName name of the action to perform
     * @param args arguments - array of ArgumentWrapper
     * @return action returned result (not serialized)
     */
    private Object executeAction(
                                  String caller,
                                  String componentName,
                                  String actionName,
                                  ArgumentWrapper[] args ) throws IOException, NoSuchComponentException,
                                                           NoSuchActionException, ActionExecutionException,
                                                           InternalComponentException,
                                                           NoCompatibleMethodFoundException,
                                                           ClassNotFoundException {

        Object[] arguments = null;
        if (args == null) { // Apache CXF impl. provides null instead of empty array
            arguments = new Object[0];
        } else {
            int numArguments = args.length;
            arguments = new Object[numArguments];

            // unwrap the action arguments
            for (int i = 0; i < numArguments; i++) {
                ArgumentWrapper argWrapper = args[i];

                ByteArrayInputStream byteInStream = new ByteArrayInputStream(argWrapper.getArgumentValue());
                ObjectInputStream objectInStream = new ObjectInputStream(byteInStream);

                arguments[i] = objectInStream.readObject();
            }
        }

        return ActionHandler.executeAction(caller, componentName, actionName, arguments);
    }

    /**
     * Tells if an Agent component is loaded, so its actions can be called
     *
     * @param componentName the name of the component
     * @return whether it is available
     */
    @WebMethod
    public boolean isComponentLoaded(
                                      @WebParam( name = "componentName") String componentName ) {

        String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            boolean isLoaded = ActionHandler.isComponentLoaded(caller, componentName);
            log.info("Agent component '" + componentName + "' is " + (isLoaded
                                                                               ? ""
                                                                               : "not ")
                     + "loaded for caller with IP " + caller);
            return isLoaded;
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Restore the environment for a given component. This will cause the
     * declared component environment (DB, files, etc.) to be restored. Also, the
     * EnvironmentCleanupHandler for the component will be called (if there is such)
     *
     * @param componentName name of the component, pass null to clean all components
     * @param environmentName name of the environment configuration
     * @param folderPath backup folder path
     * @throws AgentException on error
     * @throws InternalComponentException
     *             if an exception is thrown by the component while executing
     *             the cleanup
     */
    @WebMethod
    public void restoreEnvironment(
                                    @WebParam( name = "componentName") String componentName,
                                    @WebParam( name = "environmentName") String environmentName,
                                    @WebParam( name = "folderPath") String folderPath ) throws AgentException,
                                                                                        InternalComponentException {

        try {
            if (componentName == null) {
                // clean for all components
                EnvironmentHandler.getInstance().restoreAll(environmentName);
            } else {
                EnvironmentHandler.getInstance().restore(componentName, environmentName, folderPath);
            }

        } catch (Exception e) {
            handleExceptions(e);
        }
    }

    /**
     * Backup the environment for a given component. This will cause the
     * declared component environment (DB, files, etc.) to be backed up.
     *
     * @param componentName name of the component, pass null to backup all components
     * @param environmentName name of the environment configuration
     * @param folderPath backup folder path
     * @throws AgentException on error
     * @throws InternalComponentException if an exception is thrown by the component while creating the backup
     */
    @WebMethod
    public void backupEnvironment(
                                   @WebParam( name = "componentName") String componentName,
                                   @WebParam( name = "environmentName") String environmentName,
                                   @WebParam( name = "folderPath") String folderPath ) throws AgentException,
                                                                                       InternalComponentException {

        try {
            if (componentName == null) {
                // backup for all components
                EnvironmentHandler.getInstance().backupAll(environmentName);
            } else {
                EnvironmentHandler.getInstance().backup(componentName, environmentName, folderPath);
            }

        } catch (Exception e) {
            handleExceptions(e);
        }
    }

    /**
     * Schedule a set of actions (queue) in multiple threads. The actions
     * will not be executed until a call to startQueue is made
     *
     * @param queueName the name of the action queue
     * @param actions the actions in that queue
     * @param serializedThreadingPattern the serialized threading pattern to be used
     * @param testCaseState the test case state
     * @throws AgentException on error
     * @throws InternalComponentException if an exception is thrown while the actions are executed
     */
    @WebMethod
    public void scheduleActionsInMultipleThreads(
                                                  @WebParam( name = "name") String queueName,
                                                  @WebParam( name = "queueId") int queueId,
                                                  @WebParam( name = "actions") ActionWrapper[] actions,
                                                  @WebParam( name = "serializedThreadingPattern") byte[] serializedThreadingPattern,
                                                  @WebParam( name = "serializedLoaderDataConfig") byte[] serializedLoaderDataConfig,
                                                  boolean isUseSynchronizedIterations ) throws AgentException,
                                                                                        InternalComponentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            ArrayList<ActionRequest> actionRequests = new ArrayList<ActionRequest>();

            for (ActionWrapper actionWrapper : actions) {

                List<ArgumentWrapper> args = actionWrapper.getArgs();

                int numArguments = args.size();
                Object[] arguments = new Object[numArguments];

                // unwrap the action arguments
                for (int i = 0; i < numArguments; i++) {
                    ArgumentWrapper argWrapper = args.get(i);

                    ByteArrayInputStream byteInStream = new ByteArrayInputStream(argWrapper.getArgumentValue());
                    ObjectInputStream objectInStream = new ObjectInputStream(byteInStream);

                    arguments[i] = objectInStream.readObject();
                }

                // construct the action request
                ActionRequest actionRequest = new ActionRequest(actionWrapper.getComponentName(),
                                                                actionWrapper.getActionName(),
                                                                arguments);
                actionRequests.add(actionRequest);
            }

            ByteArrayInputStream byteInStream;
            ObjectInputStream objectInStream;

            // de-serialize the threading configuration
            byteInStream = new ByteArrayInputStream(serializedThreadingPattern);
            objectInStream = new ObjectInputStream(byteInStream);
            ThreadingPattern threadingPattern = (ThreadingPattern) objectInStream.readObject();

            // de-serialize the loader data configuration
            byteInStream = new ByteArrayInputStream(serializedLoaderDataConfig);
            objectInStream = new ObjectInputStream(byteInStream);
            LoaderDataConfig loaderDataConfig = (LoaderDataConfig) objectInStream.readObject();

            MultiThreadedActionHandler.getInstance(caller).scheduleActions(caller,
                                                                           queueName,
                                                                           queueId,
                                                                           actionRequests,
                                                                           threadingPattern,
                                                                           loaderDataConfig,
                                                                           isUseSynchronizedIterations);
        } catch (Exception e) {
            handleExceptions(e);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Start an action queue
     *
     * @param queueName the name of the action queue
     * @param testCaseState the test case state
     * @throws AgentException on error
     * @throws InternalComponentException if an exception is thrown while the actions are executed
     */
    @WebMethod
    public void startQueue(
                            @WebParam( name = "name") String queueName ) throws AgentException,
                                                                         InternalComponentException {

        // initialize the structure which will keep info about the execution results of this queue
        QueueExecutionStatistics.getInstance().initActionExecutionResults(queueName);

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            MultiThreadedActionHandler.getInstance(caller).startQueue(queueName);
        } catch (Exception e) {
            handleExceptions(e);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Resume a queue which was paused
     *
     * @param queueName the name of the action queue
     * @throws AgentException
     * @throws InternalComponentException
     */
    @WebMethod
    public void resumeQueue(
                             @WebParam( name = "name") String queueName ) throws AgentException,
                                                                          InternalComponentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            MultiThreadedActionHandler.getInstance(caller).resumeQueue(queueName);
        } catch (Exception e) {
            handleExceptions(e);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Cancel all currently running action queues
     *
     * @throws AgentException on error
     * @throws InternalComponentException on error
     */
    @WebMethod
    public void cancelAllQueues() throws AgentException, InternalComponentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            MultiThreadedActionHandler.getInstance(caller).cancelAllQueues();
        } catch (Exception e) {
            handleExceptions(e);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Cancel action queue
     *
     * @throws AgentException on error
     * @throws InternalComponentException on error
     */
    @WebMethod
    public void cancelQueue(
                             @WebParam( name = "name") String queueName ) throws AgentException,
                                                                          InternalComponentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            MultiThreadedActionHandler.getInstance(caller).cancelQueue(queueName);
        } catch (Exception e) {
            handleExceptions(e);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Check whether some queue is currently running
     * 
     * @param queueName the name of the queue
     * @return true/false
     */
    @WebMethod
    public boolean isQueueRunning(
                                   @WebParam( name = "name") String queueName ) {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        return MultiThreadedActionHandler.getInstance(caller).isQueueRunning(queueName);
    }

    /**
     * Wait until an action queue is paused or finished
     *
     * @param queueName the name of the action queue
     * @return if it was PAUSED. For example it could be also FINISHED or interrupted while RUNNING
     *
     * @throws AgentException on error
     * @throws InternalComponentException if an exception is thrown while the actions are executed
     */
    @WebMethod
    public boolean waitUntilQueueIsPaused(
                                           @WebParam( name = "name") String queueName ) throws AgentException,
                                                                                        InternalComponentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            return MultiThreadedActionHandler.getInstance(caller).waitUntilQueueIsPaused(queueName);
        } catch (Exception e) {
            handleExceptions(e);

            // can't come here
            return false;
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Wait until an action queue finishes execution
     *
     * @param queueName the name of the action queue
     * @throws AgentException on error
     * @throws InternalComponentException if an exception is thrown while the actions are executed
     */
    @WebMethod
    public void waitUntilQueueFinish(
                                      @WebParam( name = "name") String queueName ) throws AgentException,
                                                                                   InternalComponentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            MultiThreadedActionHandler.getInstance(caller).waitUntilQueueFinish(queueName);
        } catch (Exception e) {
            handleExceptions(e);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Wait until all started action queues finish execution
     *
     * @throws AgentException on error
     * @throws InternalComponentException if an exception is thrown while the actions are executed
     */
    @WebMethod
    public void waitUntilAllQueuesFinish() throws AgentException, InternalComponentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            MultiThreadedActionHandler.getInstance(caller).waitUntilAllQueuesFinish();
        } catch (Exception e) {
            handleExceptions(e);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * Queue has already finished and the Test Executor is asking for the
     * queue execution results.
     *
     * There is theoretical chance to be called by more than one thread on Test Executor side,
     * when more than one queue is started in non-blocking mode. That's why it is synchronized
     *
     * @param queueName
     * @return
     * @throws AgentException
     */
    @WebMethod
    public synchronized byte[] getActionExecutionResults(
                                                          @WebParam( name = "name") String queueName ) throws AgentException {

        try {
            ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);
            objectOutStream.writeObject(QueueExecutionStatistics.getInstance()
                                                                .getActionExecutionResults(queueName));

            return byteOutStream.toByteArray();
        } catch (Exception e) {
            handleExceptions(e);

            // should never reach this line because handleExceptions() will
            // always throw but the compiler is not aware of this
            return null;
        }
    }

    /**
     * Apply client configuration to the server
     *
     * @param configurators the serialized configurators to be applied
     * @return the agent version
     * @throws AgentException on error
     */
    @SuppressWarnings( "unchecked")
    @WebMethod
    public String pushConfiguration(
                                   @WebParam( name = "configurators") byte[] serializedConfigurators ) throws AgentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        ByteArrayInputStream byteInStream = new ByteArrayInputStream(serializedConfigurators);
        ObjectInputStream objectInStream;

        try {
            objectInStream = new ObjectInputStream(byteInStream);

            List<Configurator> configurators = (List<Configurator>) objectInStream.readObject();

            // Check if AgentConfigurator is set. In such case we will need to reload the
            // Agent components as this configuration defines the way Agent components are loaded.
            boolean needToReloadComponents = false;
            for (Configurator configurator : configurators) {
                if (configurator instanceof AgentConfigurator) {
                    needToReloadComponents = true;
                    break;
                }
            }

            if (needToReloadComponents) {
                // the already loaded Agent components are first unloaded
                MainComponentLoader.getInstance().destroy();

                // the initialization procedure will implicitly apply the new configurations
                // and then will load up again the Agent components
                MainComponentLoader.getInstance().initialize(configurators);
            } else {
                // just apply the configurations
                ConfigurationManager.getInstance().apply(configurators);
            }
        } catch (IOException ioe) {
            final String msg = "IO error while serializing configurators";
            log.error(msg, ioe); // log on the monitored machine
            throw new AgentException(msg, ioe);
        } catch (ClassNotFoundException cnfe) {
            final String msg = "Could not deserialize configurators";
            log.error(msg, cnfe); // log on the monitored machine
            throw new AgentException(msg, cnfe);
        } catch (Exception e) {
            final String msg = "Error applying configurators";
            log.error(msg, e); // log on the monitored machine
            throw new AgentException(msg, e);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
        
        return AtsVersion.getAtsVersion();
    }

    /**
     * @return path to agent home
     * @throws AgentException
     */
    @WebMethod
    public String getAgentHome() throws AgentException {

        return System.getProperty(AtsSystemProperties.AGENT_HOME_FOLDER);
    }
    
    @WebMethod
    public String getAgentVersion() {
        return AtsVersion.getAtsVersion();
    }
    
    @WebMethod
    public synchronized int getNumberPendingLogEvents() throws AgentException {

        final String caller = getCaller();
        ThreadsPerCaller.registerThread(caller);

        try {
            PassiveDbAppender appender = PassiveDbAppender.getCurrentInstance(caller);

            if (appender != null) {
                return appender.getNumberPendingLogEvents();
            }
        } finally {
            ThreadsPerCaller.unregisterThread();
        }

        return -1;
    }

    /**
     * Return array of all detected JARs from classpath
     */
    @WebMethod
    public String[] getClassPath() {

        LocalSystemOperations operations = new LocalSystemOperations();
        return operations.getClassPath();
    }

    /**
     * Log all JARs in current application's ClassPath
     */
    @WebMethod
    public void logClassPath() {

        LocalSystemOperations operations = new LocalSystemOperations();
        operations.logClassPath();
    }

    /**
     * Return array containing all duplicated jars in the ClassPath
     */
    @WebMethod
    public String[] getDuplicatedJars() {

        LocalSystemOperations operations = new LocalSystemOperations();
        return operations.getDuplicatedJars();
    }

    /**
     * Log all duplicated JARs in current application's ClassPath
     */
    @WebMethod
    public void logDuplicatedJars() {

        LocalSystemOperations operations = new LocalSystemOperations();
        operations.logDuplicatedJars();
    }

    /**
     * @return a token which contains a random Unique ID per caller and the caller IP as well
     */
    private String getCaller() {

        MessageContext msgx = wsContext.getMessageContext();

        HttpServletRequest request = ((HttpServletRequest) msgx.get(MessageContext.SERVLET_REQUEST));

        String uid = "";
        try {
            Map<String, List<String>> headers = (Map<String, List<String>>) msgx.get(MessageContext.HTTP_REQUEST_HEADERS);
            uid = headers.get(ApplicationContext.ATS_UID_SESSION_TOKEN).get(0);
        } catch (Exception e) {
            if (!alreadyLoggedErrorAboutSessionUid) {
                log.warn("Could not get ATS UID for call from " + request.getRemoteAddr()
                         + ". This error will not be logged again before Agent restart.", e);
                alreadyLoggedErrorAboutSessionUid = true;
            }
        }

        return "<Caller: " + request.getRemoteAddr() + "; ATS UID: " + uid + ">";
    }
    
    private String getAgentHostAddress() {
        
        MessageContext msgx = wsContext.getMessageContext();

        HttpServletRequest request = ((HttpServletRequest) msgx.get(MessageContext.SERVLET_REQUEST));


        return request.getLocalAddr() + ":" + request.getLocalPort();
        
    }

    private void handleExceptions(
                                   Exception e ) throws AgentException, InternalComponentException {

        if (e instanceof InternalComponentException) {
            InternalComponentException ice = (InternalComponentException) e;

            // this was an internal component exception so we
            // need to log it on the server, otherwise we loose the stack trace
            if (ice.getCause() instanceof InterruptedException) {
                log.error("InterruptedException exception: ", ice.getCause());
            } else {
                log.error("Internal component exception: ", ice.getCause());
            }

            throw ice;

        } else if (e instanceof AgentException) {

            // log the stack trace
            log.error("Agent exception: ", e);

            // need to create a new instance of the parent class, otherwise it will not be
            // correctly handled by JAXB
            throw new AgentException(e.getMessage());

        } else {
            // all other exceptions should be logged and wrapped to AgentException
            log.error("Unhandled exception thrown during action execution", e);
            throw new AgentException(e.getMessage());
        }
    }
}
