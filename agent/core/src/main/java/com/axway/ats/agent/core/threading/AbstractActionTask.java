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
package com.axway.ats.agent.core.threading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.axway.ats.agent.core.action.ActionInvoker;
import com.axway.ats.agent.core.action.ActionMethod;
import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.agent.core.action.TemplateActionMethod;
import com.axway.ats.agent.core.configuration.TemplateActionsResponseVerificationConfigurator;
import com.axway.ats.agent.core.context.ThreadContext;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.monitoring.UserActionsMonitoringAgent;
import com.axway.ats.agent.core.monitoring.queue.QueueExecutionStatistics;
import com.axway.ats.agent.core.templateactions.CompositeResult;
import com.axway.ats.agent.core.threading.data.ParameterDataProvider;
import com.axway.ats.agent.core.threading.data.config.UsernameDataConfig;
import com.axway.ats.agent.core.threading.listeners.ActionTaskListener;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.PassiveDbAppender;
import com.axway.ats.log.model.CheckpointResult;

/**
 * The base for all action tasks - all inheriting classes need to
 * implement execute()
 */
public abstract class AbstractActionTask implements Runnable {

    public static final String            ATS_ACTION__QUEUE_EXECUTION_TIME = "Queue execution time";
    /**
     * Property to enable fill action time logging in addition to net time
     */
    public static final boolean           REGISTER_FULL_AND_NET_ACTION_TIME_FOR_TEMPLATE_ACTIONS;

    protected final AtsDbLogger           log;

    protected String                      queueName;

    //the thread iterations manager for the task
    private ThreadsManager                threadsManager;

    protected List<ActionRequest>         actionRequests;
    protected List<ActionInvoker>         actionInvokers;
    protected List<ParameterDataProvider> dataProviders;
    protected List<Object>                actionClassInstances;

    //listeners
    protected List<ActionTaskListener>    listeners;

    //parameters used when have set speed of execution
    protected long                        timeFrameLength;
    protected long                        timeFrameStartTimestamp;
    protected int                         totalExecutionsPerTimeFrame;
    protected int                         currentIterationsInThisTimeFrame;

    //time to sleep between iterations
    private long                          intervalBetweenIterations;
    private long                          minIntervalBetweenIterations;
    private long                          maxIntervalBetweenIterations;
    private Random                        intervalTimeGenerator;

    //if this thread's iterations are synchronized with the other running threads
    protected boolean                     isUseSynchronizedIterations;

    //remember if we are logging some events in batch mode
    private boolean                       isLoggingInBatchMode;

    // manager watching for too long iterations
    protected IterationTimeoutManager     itManager;

    // flags about thread time outs
    private boolean                       timedOut                         = false;
    private int                           timedOutSeconds;
    private boolean                       externallyInterrupted            = false;

    // used for telling the user how many queue iterations are started
    private int                           nIterations                      = 0;

    static {
        REGISTER_FULL_AND_NET_ACTION_TIME_FOR_TEMPLATE_ACTIONS = AtsSystemProperties.getPropertyAsBoolean(AtsSystemProperties.AGENT__REGISTER_FULL_AND_NET_ACTION_TIME_FOR_TEMPLATE_ACTIONS_KEY,
                                                                                                          false);
    }

    // remember the remote caller which initiates this action
    private String caller;

    /**
     * @param caller the remote caller
     * @param queueName name of the Load queue
     * @param threadsManager manager of threads
     * @param itManager iterations
     * @param listeners
     * @throws ActionExecutionException
     * @throws NoCompatibleMethodFoundException
     * @throws NoSuchActionException
     * @throws NoSuchComponentException
     */
    public AbstractActionTask( String caller, String queueName, ThreadsManager threadsManager,
                               IterationTimeoutManager itManager, List<ActionRequest> actionRequests,
                               List<ParameterDataProvider> dataProviders, long intervalBetweenIterations,
                               long minIntervalBetweenIterations, long maxIntervalBetweenIterations,
                               List<ActionTaskListener> listeners ) throws ActionExecutionException,
                                                                    NoSuchComponentException,
                                                                    NoSuchActionException,
                                                                    NoCompatibleMethodFoundException {

        this.caller = caller;

        /*
         * Skip checking in db appender is attached, because we are on the agent and not the executor.
         * Also we want for actions to be executed on the agent even if data will not be sent to ATS Log database
         * */
        this.log = AtsDbLogger.getLogger(this.getClass().getName(), true);

        PassiveDbAppender dbAppender = PassiveDbAppender.getCurrentInstance(ThreadsPerCaller.getCaller());
        if (dbAppender != null) {
            this.isLoggingInBatchMode = dbAppender.isBatchMode();
        }

        this.queueName = queueName;

        this.threadsManager = threadsManager;
        this.actionRequests = actionRequests;

        //create the invokers based on these requests
        actionInvokers = new ArrayList<ActionInvoker>();

        for (ActionRequest actionRequest : actionRequests) {
            ActionInvoker actionInvoker = new ActionInvoker(actionRequest);
            actionInvokers.add(actionInvoker);
        }

        this.dataProviders = dataProviders;
        this.listeners = listeners;
        this.actionClassInstances = new ArrayList<Object>();

        //a temporary map to hold the action class instances
        //we want a single action class to have only one instance per thread
        HashMap<String, Object> createdInstances = new HashMap<String, Object>();

        //cache the instances of the action classes
        for (ActionInvoker actionInvoker : actionInvokers) {
            String actionClassName = actionInvoker.getActionClass().getName();

            try {
                Class<?> actionClass = actionInvoker.getActionClass();
                Object actionClassInstance = createdInstances.get(actionClass.getName());
                //create a new instance only if necessary
                if (actionClassInstance == null) {
                    actionClassInstance = actionClass.newInstance();
                    createdInstances.put(actionClass.getName(), actionClassInstance);
                }

                actionClassInstances.add(actionClassInstance);

            } catch (IllegalAccessException iae) {
                throw new ActionExecutionException("Could not access action class " + actionClassName, iae);
            } catch (InstantiationException ie) {
                throw new ActionExecutionException("Could not instantiate action class " + actionClassName,
                                                   ie);
            }
        }

        checkForUnusedProvidedParameters();

        this.intervalBetweenIterations = intervalBetweenIterations;
        this.minIntervalBetweenIterations = minIntervalBetweenIterations;
        this.maxIntervalBetweenIterations = maxIntervalBetweenIterations;
        if (minIntervalBetweenIterations != -1) {
            intervalTimeGenerator = new Random();
        }

        this.itManager = itManager;
    }

    /**
     * @return whether this iteration has timed out
     */
    public boolean isTimedOut() {

        return this.timedOut;
    }

    /**
     * Set this iteration as timed out
     * @param timedOutSeconds how long time it lived
     */
    public void setTimedOut( int timedOutSeconds ) {

        this.timedOut = true;
        this.timedOutSeconds = timedOutSeconds;
    }

    /**
     * Tell if iteration was interrupted from outside.
     * Typically this is when user cancels the queue
     * @return 
     */
    public boolean isExternallyInterrupted() {

        return externallyInterrupted;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public final void run() {

        renameThread();
        ThreadsPerCaller.registerThread(caller);

        try {
            // set TemplateActionsResponseVerificationConfigurator to the ThreadContext (it is per Actions Queue)
            TemplateActionsResponseVerificationConfigurator templateConfigurator = TemplateActionsResponseVerificationConfigurator.getInstance(this.queueName);
            if (templateConfigurator != null) {
                ThreadContext.setAttribute(ThreadContext.TEMPLATE_ACTION_VERIFICATION_CONFIGURATOR,
                                           templateConfigurator);
            }

            boolean runOnce = true;
            while (runOnce) {

                //wait until this iteration is started
                threadsManager.waitForStart();

                //notify the listeners on start
                onStart();

                ActionTaskResult executionResult = execute();

                switch (executionResult) {
                    case FINISHED:
                        onFinish(null);
                        runOnce = false;
                        break;
                    case PAUSED:
                        onPause();
                        runOnce = true;
                        break;
                    default:
                        // canceled
                        onFinish(null); // we have to notify listeners that the Thread is finished(canceled)
                        runOnce = false;
                }
            }
        } catch (Exception e) {
            //notify the listeners
            onFinish(e);
        } finally {
            ThreadsPerCaller.unregisterThread();
        }
    }

    /**
     * If configured properly, renames thread according to user name 
     * for better user experience when reading the logs
     */
    private void renameThread() {

        if (dataProviders.size() > 0) {

            synchronized (dataProviders) {
                for (ParameterDataProvider dataProvider : dataProviders) {
                    if (dataProvider.getDataConfiguratorClass() == UsernameDataConfig.class) {
                        Thread.currentThread()
                              .setName(dataProvider.getValue(new ArrayList<ArgumentValue>())
                                                   .getValue()
                                                   .toString());

                        break;
                    }
                }
            }
        }
    }

    /**
     * Execute the task
     *
     * @return the new action task state
     */
    public abstract ActionTaskResult execute();

    /**
     * Invoke one iteration of all actions in the queue
     * 
     * @throws InterruptedException
     */
    protected final void invokeActions() throws InterruptedException {

        UserActionsMonitoringAgent userActionsMonitoringAgent = UserActionsMonitoringAgent.getInstance(caller);

        if (log.isDebugEnabled()) {
            log.debug("Starting '" + queueName + "' queue for " + (++nIterations) + "th time");
        }

        //generate the input arguments for all action invokers
        generateInputArguments();

        long queueDuration = 0;
        long actionStartTimestamp = 0;
        long actionEndTimestamp = 0;

        if (this.itManager != null) { // inform a new iteration is starting now
            this.itManager.setIterationStartTime(this, System.currentTimeMillis());
        }

        try {
            for (int i = 0; i < actionInvokers.size(); i++) { // start cycling all actions in this iteration

                ActionInvoker actionInvoker = actionInvokers.get(i);
                Object actionClassInstance = actionClassInstances.get(i);
                ActionMethod actionMethod = actionInvoker.getActionMethod();

                final String actionName = actionInvoker.getActionName();
                final String transferUnit = actionMethod.getTransferUnit();

                // read some settings
                final boolean registerActionExecution = actionMethod.isRegisterActionExecution();
                final boolean registerActionExecutionInQueueExecutionTime = actionMethod.isRegisterActionExecutionInQueueExecutionTime();
                final boolean isTemplateActionMethod = actionMethod instanceof TemplateActionMethod;
                final boolean logCheckpoints = !isTemplateActionMethod
                                               || REGISTER_FULL_AND_NET_ACTION_TIME_FOR_TEMPLATE_ACTIONS;

                // checkpoint name. For template actions by default only network time is tracked.
                // Here "-full" adds total action processing including XML (de)serializations, parameterization
                String checkpointName;
                if (!isTemplateActionMethod) {
                    checkpointName = actionName;
                } else {
                    checkpointName = actionName + "-full";
                }

                // start a checkpoint
                userActionsMonitoringAgent.actionStarted(actionName);
                if (registerActionExecution) {
                    actionStartTimestamp = System.currentTimeMillis();
                    if (logCheckpoints && !isLoggingInBatchMode) {
                        log.startCheckpoint(checkpointName, transferUnit, actionStartTimestamp);
                    }
                }

                // invoke the current action
                Object actionReturnedResult = null;
                try {
                    actionReturnedResult = actionInvoker.invoke(actionClassInstance);
                } catch (Exception e) {
                    // the action failed - end the checkpoint
                    if (registerActionExecution) {
                        if (logCheckpoints) {
                            if (isLoggingInBatchMode) {
                                log.insertCheckpoint(checkpointName, actionStartTimestamp, 0, 0,
                                                     transferUnit, CheckpointResult.FAILED);
                            } else {
                                log.endCheckpoint(checkpointName, 0, CheckpointResult.FAILED);
                            }

                        }
                        QueueExecutionStatistics.getInstance().registerActionExecutionResult(queueName,
                                                                                             actionName,
                                                                                             false);
                        log.insertCheckpoint(ATS_ACTION__QUEUE_EXECUTION_TIME, queueDuration, CheckpointResult.FAILED);
                    }
                    // re-throw the exception
                    throw e;
                } finally {
                    userActionsMonitoringAgent.actionEnded(actionName);
                }

                // the action passed
                if (registerActionExecution) {
                    actionEndTimestamp = System.currentTimeMillis();
                    long responseTimeMs = actionEndTimestamp - actionStartTimestamp;

                    long transferSize = 0;
                    if (transferUnit.length() > 0) {
                        transferSize = (Long) actionReturnedResult;
                    }

                    if (logCheckpoints) {
                        if (isLoggingInBatchMode) {
                            log.insertCheckpoint(checkpointName, actionStartTimestamp, responseTimeMs,
                                                 transferSize, transferUnit, CheckpointResult.PASSED);
                        } else {
                            log.endCheckpoint(checkpointName, transferSize, CheckpointResult.PASSED,
                                              actionEndTimestamp);
                        }
                    }

                    if (registerActionExecutionInQueueExecutionTime) {
                        if (isTemplateActionMethod) { // add net time in queue instead of full processing time
                            if (actionReturnedResult instanceof CompositeResult) {
                                CompositeResult res = (CompositeResult) actionReturnedResult;
                                responseTimeMs = res.getReqRespNetworkTime();
                            }
                        }
                        queueDuration += responseTimeMs;
                    }

                    QueueExecutionStatistics.getInstance().registerActionExecutionResult(queueName,
                                                                                         actionName, true);
                }
            } // end cycling all actions in this iteration

        } catch (Exception e) {
            // We are particularly interested if the thread was interrupted
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof InterruptedException) {
                // the thread was interrupted
                if (this.timedOut) {
                    // the thread was interrupted due to timeout, log the timeout and go to next iteration
                    log.error("Iteration timed out in " + this.timedOutSeconds
                              + " seconds - skipping to next iteration");

                    this.timedOut = false; // reset our flag as we will start another iteration
                    this.timedOutSeconds = 0;
                } else {
                    // the thread interrupted, but not due to timeout, maybe the user cancelled the queue
                    this.externallyInterrupted = true;
                    throw(InterruptedException) cause;
                }
            } else {
                // some kind of generic exception has occurred
                log.error("Exception caught during invocation - skipping to next iteration", e);
            }

            //continue to the next iteration
            return;
        } finally {
            if (this.itManager != null) {
                this.itManager.clearIterationStartTime();
            }
        }

        log.insertCheckpoint(ATS_ACTION__QUEUE_EXECUTION_TIME, queueDuration, CheckpointResult.PASSED);
    }

    /**
     * Sleep only if this will not go beyond the endTimestamp
     *
     * @param endTimestamp
     * @return if slept
     * @throws InterruptedException
     */
    protected boolean sleepBetweenIterations( long endTimestamp ) throws InterruptedException {

        long nextInterval;
        if (intervalBetweenIterations > 0) {
            // constant sleep between each iteration
            nextInterval = intervalBetweenIterations;
        } else if (intervalTimeGenerator != null) {
            // varying sleep
            nextInterval = intervalTimeGenerator.nextInt((int) (maxIntervalBetweenIterations
                                                                - minIntervalBetweenIterations))
                           + minIntervalBetweenIterations;

        } else {
            nextInterval = 0;
        }

        if (System.currentTimeMillis() + nextInterval < endTimestamp) {
            Thread.sleep(nextInterval);
            if (intervalTimeGenerator != null) {
                log.insertCheckpoint("[Time between queue executions]", nextInterval,
                                     CheckpointResult.PASSED);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sleep as defined by the user
     *
     * @throws InterruptedException
     */
    protected void sleepBetweenIterations() throws InterruptedException {

        long nextInterval;
        if (intervalBetweenIterations > 0) {
            // constant sleep between each iteration
            nextInterval = intervalBetweenIterations;
        } else if (intervalTimeGenerator != null) {
            // varying sleep
            nextInterval = intervalTimeGenerator.nextInt((int) (maxIntervalBetweenIterations
                                                                - minIntervalBetweenIterations))
                           + minIntervalBetweenIterations;
        } else {
            nextInterval = 0;
        }

        Thread.sleep(nextInterval);
        if (intervalTimeGenerator != null) {
            log.insertCheckpoint("[Time between queue executions]", nextInterval, CheckpointResult.PASSED);
        }
    }

    /**
     * Evaluates the speed progress
     * If we know the end time, this method returns whether waiting for end of time frame
     * goes beyond the end time
     *
     * @param endTimestamp the end time, or 0 if unknown
     * @return
     * @throws InterruptedException
     */
    protected boolean evaluateSpeedProgress( long endTimestamp ) throws InterruptedException {

        currentIterationsInThisTimeFrame++;
        long timeTillTheEndOfTimeFrame = timeFrameLength
                                         - (System.currentTimeMillis() - timeFrameStartTimestamp);
        if (timeTillTheEndOfTimeFrame > 0
            && currentIterationsInThisTimeFrame >= totalExecutionsPerTimeFrame) {
            // we have reached/exceeded the number of iterations per given time frame

            // if an end time is specified, check whether we will pass by after sleeping
            // till the end of this time frame
            if (endTimestamp > 0 && System.currentTimeMillis() + timeTillTheEndOfTimeFrame >= endTimestamp) {
                return true;
            }

            // we will sleep till the end of this time frame
            Thread.sleep(timeTillTheEndOfTimeFrame);

            // reseting the time frame start time and the number of iterations in the time frame
            timeFrameStartTimestamp = System.currentTimeMillis();
            currentIterationsInThisTimeFrame = 0;

        } else if (timeTillTheEndOfTimeFrame <= 0) {

            if (currentIterationsInThisTimeFrame < totalExecutionsPerTimeFrame) {
                log.warn("We were not able to execute the requested " + totalExecutionsPerTimeFrame
                         + " iterations for " + timeFrameLength / 1000 + " seconds, but only "
                         + currentIterationsInThisTimeFrame + " iterations");
            }

            // reseting the time frame start time and the number of iterations in the time frame
            timeFrameStartTimestamp = System.currentTimeMillis();
            currentIterationsInThisTimeFrame = 0;
        }

        return false;
    }

    /**
     * Generate the input arguments based on the data providers
     *
     */
    protected final void generateInputArguments() {

        if (dataProviders.size() > 0) {

            // We will store here only the arguments which are to be replaced by a data provider.
            // We store all values from all data providers.
            List<ArgumentValue> argumentValues = new ArrayList<ArgumentValue>();

            // Generate the new input arguments - all arguments should be
            // generated at the same time, so other threads do not interfere.
            synchronized (dataProviders) {
                for (ParameterDataProvider dataProvider : dataProviders) {
                    argumentValues.add(dataProvider.getValue(argumentValues));
                }
            }

            // set the arguments as provided
            for (ActionInvoker actionInvoker : actionInvokers) {
                argumentValues = actionInvoker.setArguments(argumentValues);
            }
        }
    }

    /**
     *  Call the onStart handler for all listeners
     */
    private final void onStart() {

        for (ActionTaskListener listener : listeners) {
            listener.onStart();
        }
    }

    protected final void onPause() {

        for (ActionTaskListener listener : listeners) {
            listener.onPause();
        }
    }

    /**
     *  Call the onFinish handler for all listeners
     *  @param throwable when an exception is thrown
     */
    private final void onFinish( Throwable throwable ) {

        for (ActionTaskListener listener : listeners) {
            listener.onFinish(throwable);
        }
    }

    /**
     * Calculate how many times a parameter is:
     *      - present in all data providers
     *      - used in all action invokers
     *
     * Log a warning message if a parameter present in the data providers more time than it is used by the action invokers
     */
    private void checkForUnusedProvidedParameters() {

        // get how many providers provide values for each parameter
        Map<String, Integer> paramsInProvidersMap = new HashMap<String, Integer>();
        for (ParameterDataProvider dataProvider : dataProviders) {
            String paramName = dataProvider.getParameterName();
            if (paramsInProvidersMap.get(paramName) != null) {
                paramsInProvidersMap.put(paramName, paramsInProvidersMap.get(paramName) + 1);
            } else {
                paramsInProvidersMap.put(paramName, 1);
            }
        }

        // get how many times each parameter is used in all actions
        Map<String, Integer> paramsInActionsMap = new HashMap<String, Integer>();
        for (ActionInvoker actionInvoker : actionInvokers) {
            for (String paramName : actionInvoker.getActionMethodParameterNames()) {
                if (paramsInActionsMap.get(paramName) != null) {
                    paramsInActionsMap.put(paramName, paramsInActionsMap.get(paramName) + 1);
                } else {
                    paramsInActionsMap.put(paramName, 1);
                }
            }
        }

        // warn for unused provided parameters
        for (Entry<String, Integer> paramEntry : paramsInProvidersMap.entrySet()) {
            int timesInProviders = paramEntry.getValue();

            //we iterate on the paramsInProviders map, so we need to make
            //sure that a parameter with this name is present in the actions at all
            int timesInActions = 0;
            if (paramsInActionsMap.containsKey(paramEntry.getKey())) {
                timesInActions = paramsInActionsMap.get(paramEntry.getKey());
            }

            //just log a warning if a parameter data provider is not used,
            //as this is not a fatal error and can be ignored
            if (timesInProviders > timesInActions) {
                log.warn("'" + paramEntry.getKey() + "' parameter is provided by " + timesInProviders
                         + " data providers while it is used in only " + timesInActions + " actions");
            }
            // (timesInProviders < timesInActions) is OK for example when you want
            // to use constant in one of the action invocations.
        }
    }
}
