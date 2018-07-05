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
package com.axway.ats.agent.webapp.client.executors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.monitoring.queue.ActionExecutionStatistic;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.agent.webapp.client.RestHelper;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.threads.ImportantThread;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.log.model.LoadQueueResult;

/**
 * This class is responsible for executing actions on a
 * remote ATS Agent
 */
public class DistributedLoadExecutor extends RemoteExecutor {

    private List<String> atsAgents;
    private int          queueSequence;
    private RestHelper   restHelper;

    public DistributedLoadExecutor( String name, int sequence, List<String> atsAgents,
                                    ThreadingPattern threadingPattern,
                                    LoaderDataConfig loaderDataConfig ) throws AgentException {

        // we assume the ATS Agent address here comes with IP and PORT
        this.atsAgents = atsAgents;

        this.queueName = name;
        this.queueSequence = sequence;
        this.threadingPattern = threadingPattern;
        this.loaderDataConfig = loaderDataConfig;
        this.restHelper = new RestHelper();

        //configure the remote loaders(ATS agents)
        try {
            TestcaseStateEventsDispacher.getInstance().onConfigureAtsAgents(atsAgents);
        } catch (Exception e) {
            // we know for sure this is an AgentException, but as the interface declaration is in Core library,
            // we could not declare the AgentException, but its parent - the regular java Exception
            throw(AgentException) e;
        }
    }

    @Override
    public void executeActions( List<ActionRequest> actionRequests ) throws AgentException {

        //set the block until completion param to false
        //this way we'll be able to start all the loaders on all boxes at the same time
        //and then wait for all of them to finish
        final boolean blockUntilCompletion = threadingPattern.isBlockUntilCompletion();
        threadingPattern.setBlockUntilCompletion(false);

        int maxHostCount = atsAgents.size();
        // distribute the threading pattern for each agent host
        final List<ThreadingPattern> distributedPatterns = threadingPattern.distribute(maxHostCount);
        if (distributedPatterns.size() < maxHostCount) {
            log.warn("Threading pattern cannot be distributed accross all agents, only the first "
                     + distributedPatterns.size() + " agents will execute");
        }

        // distribute the data configurators for each agent host
        final List<LoaderDataConfig> distributedLoaderDataConfigs = loaderDataConfig.distribute(distributedPatterns.size());

        // start queue in the database and retrieve its ID
        int queueId = retrieveQueueId(queueSequence, getHostsList());

        // populate checkpoint summaries for actions
        populateCheckpointsSummary(queueId, actionRequests);

        //call the web service now
        try {

            //first schedule the loaders on all hosts
            for (int i = 0; i < distributedPatterns.size(); i++) {

                //wrap all the action requests
                List<ActionPojo> actionPojos = new ArrayList<ActionPojo>();
                for (ActionRequest actionRequest : actionRequests) {
                    actionPojos.add(new ActionPojo(actionRequest));
                }

                //schedule the actions, but do not execute
                String sessionId = ExecutorUtils.createExecutorId(atsAgents.get(i), ExecutorUtils.getUserRandomToken(),
                                                                  Thread.currentThread().getName());

                StringBuilder sb = new StringBuilder();
                sb.append("{")
                  .append("\"")
                  .append("sessionId")
                  .append("\":\"")
                  .append(sessionId)
                  .append("\",")
                  .append("\"")
                  .append("queueName")
                  .append("\":\"")
                  .append(queueName)
                  .append("\",")
                  .append("\"")
                  .append("queueId")
                  .append("\":")
                  .append(queueId)
                  .append(",")
                  .append("\"")
                  .append("actions")
                  .append("\":")
                  .append(restHelper.serializeJavaObject(actionPojos))
                  .append(",")
                  .append("\"")
                  .append("threadingPattern")
                  .append("\":")
                  .append(restHelper.serializeJavaObject(distributedPatterns.get(i)))
                  .append(",")
                  .append("\"")
                  .append("threadingPatternClass")
                  .append("\":\"")
                  .append(distributedPatterns.get(i).getClass().getName())
                  .append("\",")
                  .append("\"")
                  .append("loaderDataConfig")
                  .append("\":")
                  .append(restHelper.serializeJavaObject(distributedLoaderDataConfigs.get(i)))
                  .append(",")
                  .append("\"")
                  .append("useSynchronizedIterations")
                  .append("\":")
                  .append(distributedPatterns.get(0)
                                             .isUseSynchronizedIterations())
                  .append("}");

                restHelper.executeRequest(atsAgents.get(i), "/queues/schedule", "PUT", sb.toString(), null, null);
            }

            boolean useSynchronizedIterations = distributedPatterns.get(0).isUseSynchronizedIterations();
            if (useSynchronizedIterations && !blockUntilCompletion) {
                // It is non blocking, but synchronized - we have to wait until the queue finish its execution.
                // We first start the queue. This assures the queue is created before the user have the chance
                // to say "wait for completion"
                startSynchedIterations();

                // and then wait in another thread until the queue finish
                ImportantThread helpThread = new ImportantThread(new Runnable() {
                    @Override
                    public void run() {

                        runSynchedIterations();
                    }
                });
                helpThread.setExecutorId(Thread.currentThread().getName());
                helpThread.setDescription(queueName);
                helpThread.start();
            } else {
                if (useSynchronizedIterations) {
                    // it is blocking - we can wait until the queue finish it execution
                    startSynchedIterations();
                    runSynchedIterations();
                } else {
                    // if blocking - we can wait until the queue finish it execution
                    // if non blocking - as it is not synchronized, we will wait only until the queue is started
                    runNotSynchedIterations(blockUntilCompletion);
                }
            }
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }

        // restore this flag
        threadingPattern.setBlockUntilCompletion(blockUntilCompletion);
    }

    @Override
    public void waitUntilQueueFinish() throws AgentException {

        try {
            for (String host : atsAgents) {
                log.info("Waiting until action queue '" + queueName + "' finish its execution on agent '"
                         + host + "'");

                String sessionId = ExecutorUtils.createExecutorId(host, ExecutorUtils.getUserRandomToken(),
                                                                  Thread.currentThread().getName());
                // wait until finished on this host
                restHelper.executeRequest(host, "/queues/opts", "POST",
                                          "{\"sessionId\":\"" + sessionId
                                                                        + "\",\"queueName\":\"" + queueName
                                                                        + "\", \"operation\":\"waitUntilFinish\"}",
                                          null, null);

                log.info("Action queue '" + queueName + "' finished on agent '" + host + "'");
            }
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    /**
     * Cancel all running actions on remote machines
     *
     * @throws AgentException on error
     */
    public void cancelAllActions() {

        for (String host : atsAgents) {
            try {

                log.info("Cancelling action queue '" + queueName + "' on agent '" + host + "'");

                String sessionId = ExecutorUtils.createExecutorId(host, ExecutorUtils.getUserRandomToken(),
                                                                  Thread.currentThread().getName());
                //cancel any running queues
                restHelper.executeRequest(host, "/queues/opts", "POST",
                                          "{\"sessionId\":\"" + sessionId + "\", \"operation\":\"cancelAll\"}",
                                          null, null);

                log.info("Cancelled action queue '" + queueName + "' on agent '" + host + "'");
            } catch (Exception e) {
                log.error("Error cancelling action queue '" + queueName + "' on agent '" + host + "'", e);
            }
        }
    }

    public boolean isQueueRunning( String queueName ) {

        for (String host : atsAgents) {
            try {
                String sessionId = ExecutorUtils.createExecutorId(host, ExecutorUtils.getUserRandomToken(),
                                                                  Thread.currentThread().getName());
                boolean running = (boolean) restHelper.executeRequest(host, "/queues/opts", "POST",
                                                                      "{\"sessionId\":\"" + sessionId
                                                                                                    + "\",\"queueName\":\""
                                                                                                    + queueName
                                                                                                    + "\", \"operation\":\"isRunning\"}",
                                                                      "running", boolean.class);
                if (running) {
                    log.info("Queue with name '" + queueName + "' is still running on " + host);
                    return true;
                }
            } catch (Exception e) {
                log.error("Error checking if an action queue with name '" + queueName
                          + "' is running on agent " + host, e);
            }
        }

        return false;
    }

    // TODO When running non blocking, this method is started from a dedicated thread,
    // so throwing an error does not affect the main test execution thread,
    // so the test does not fail.
    // We can keep this errors in some structure which can be checked when
    // closing the test case, so we can fail it when needed
    private void runNotSynchedIterations( boolean blockUntilCompletion ) {

        log.info("Start executing action queue '" + queueName + "' on " + atsAgents.toString() + ". "
                 + (blockUntilCompletion
                                         ? "And wait to finish."
                                         : "Will not wait to finish."));
        try {
            // start the actions on all loaders
            for (int i = 0; i < atsAgents.size(); i++) {
                String host = atsAgents.get(i);

                String sessionId = ExecutorUtils.createExecutorId(host, ExecutorUtils.getUserRandomToken(),
                                                                  Thread.currentThread().getName());
                restHelper.executeRequest(host, "/queues/opts", "POST",
                                          "{\"sessionId\":\"" + sessionId
                                                                        + "\",\"queueName\":\""
                                                                        + queueName
                                                                        + "\", \"operation\":\"start\"}",
                                          null, null);
            }
        } catch (Exception e) {
            String msg = "Error starting action queue '" + queueName + "'";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        try {
            //wait until finished
            if (blockUntilCompletion) {

                waitUntilQueueFinishOnAllLoaders();
            } else {

                // and then wait in another thread until the queue finish
                ImportantThread helpThread = new ImportantThread(new Runnable() {
                    @Override
                    public void run() {

                        waitUntilQueueFinishOnAllLoaders();
                    }
                });
                helpThread.setExecutorId(Thread.currentThread().getName());
                helpThread.setDescription(queueName);
                helpThread.start();
            }
        } catch (Exception e) {
            String msg = "Error waiting for completion of action queue '" + queueName + "'";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private void waitUntilQueueFinishOnAllLoaders() {

        try {
            log.info("Action queue '" + queueName + "' is running on " + atsAgents.toString());

            for (int i = 0; i < atsAgents.size(); i++) {
                String host = atsAgents.get(i);

                log.info("Waiting until queue '" + queueName + "' finish on '" + host + "'");
                String sessionId = ExecutorUtils.createExecutorId(host, ExecutorUtils.getUserRandomToken(),
                                                                  Thread.currentThread().getName());
                restHelper.executeRequest(host, "/queues/opts", "POST",
                                          "{\"sessionId\":\"" + sessionId
                                                                        + "\",\"queueName\":\""
                                                                        + queueName
                                                                        + "\", \"operation\":\"waitUntilFinish\"}",
                                          null, null);
                log.info("Finished executing action queue '" + queueName + "' on '" + host + "'");
            }

            // the queue finished but there might be some failed actions
            // check if the wanted actions pass rate is met
            LoadQueueResult queueResult = getQueueResult(threadingPattern.getQueuePassRate(), atsAgents);
            TestcaseStateEventsDispacher.getInstance()
                                        .setQueueFinishedAsFailed(queueResult == LoadQueueResult.FAILED);

            // end the queue in the log
            log.endLoadQueue(queueName, queueResult);
            log.info("Finished executing action queue '" + queueName + "' on all agents "
                     + atsAgents.toString());
        } catch (Exception e) {

            String msg = "Error waiting for action queue '" + queueName + "' completion";
            log.error(msg, e);
            log.endLoadQueue(queueName, LoadQueueResult.FAILED);
            cancelAllActions();
            throw new RuntimeException(msg, e);
        }
    }

    private void startSynchedIterations() {

        log.info("Start executing action queue '" + queueName + "' with synchronized iterations on "
                 + atsAgents.toString());

        try {
            for (String host : atsAgents) {
                String sessionId = ExecutorUtils.createExecutorId(host, ExecutorUtils.getUserRandomToken(),
                                                                  Thread.currentThread().getName());
                restHelper.executeRequest(host, "/queues/opts", "POST",
                                          "{\"sessionId\":\"" + sessionId
                                                                        + "\",\"queueName\":\""
                                                                        + queueName
                                                                        + "\", \"operation\":\"start\"}",
                                          null, null);
            }
        } catch (Exception e) {
            String msg = "Error running action queue '" + queueName + "'";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    // TODO When running non blocking, this method is started from a dedicated thread,
    // so throwing an error does not affect the main test execution thread,
    // so the test does not fail.
    // We can keep this errors in some structure which can be checked when
    // closing the test case, so we can fail it when needed
    private void runSynchedIterations() {

        List<String> atsAgentsTmp = new ArrayList<String>(atsAgents);

        // the actions queue is running, control its execution
        try {
            while (true) {

                Iterator<String> agentsIterator = atsAgentsTmp.iterator();
                while (agentsIterator.hasNext()) {
                    String host = agentsIterator.next();
                    String sessionId = ExecutorUtils.createExecutorId(host, ExecutorUtils.getUserRandomToken(),
                                                                      Thread.currentThread().getName());
                    boolean needToRunAgain = (boolean) restHelper.executeRequest(host, "/queues/opts",
                                                                                 "POST",
                                                                                 "{\"sessionId\":\"" + sessionId
                                                                                         + "\",\"queueName\":\""
                                                                                         + queueName
                                                                                         + "\", \"operation\":\"waitUntilPaused\"}",
                                                                                 "paused", boolean.class);
                    if (!needToRunAgain) {
                        agentsIterator.remove();
                    }
                }

                if (atsAgentsTmp.size() == 0) {
                    break;
                }

                // resume the actions on all agents
                for (String agent : atsAgentsTmp) {
                    String sessionId = ExecutorUtils.createExecutorId(agent, ExecutorUtils.getUserRandomToken(),
                                                                      Thread.currentThread().getName());
                    restHelper.executeRequest(agent, "/queues/opts", "POST",
                                              "{\"sessionId\":\"" + sessionId
                                                                             + "\",\"queueName\":\""
                                                                             + queueName
                                                                             + "\", \"operation\":\"resume\"}",
                                              null, null);
                }
            }

            // the queue finished but there might be some failed actions
            // check if the wanted actions pass rate is met
            LoadQueueResult queueResult = getQueueResult(threadingPattern.getQueuePassRate(), atsAgents);
            TestcaseStateEventsDispacher.getInstance()
                                        .setQueueFinishedAsFailed(queueResult == LoadQueueResult.FAILED);

            // end the queue in the log
            log.endLoadQueue(queueName, queueResult);
            log.info("Finished executing action queue '" + queueName + "' on " + atsAgents.toString()
                     + " with synchronized iterations");
        } catch (Exception e) {

            String msg = "Error running action queue '" + queueName + "'";
            log.error(msg, e);
            log.endLoadQueue(queueName, LoadQueueResult.FAILED);
            cancelAllActions();
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * See if the queue has passed or failed based on number of pass and failed iterations
     */
    private LoadQueueResult getQueueResult( float queuePassRate,
                                            List<String> agentsRunningThisQueue ) throws AgentException {

        // the queue has ended, collect info about the executed actions on all agents
        List<ActionExecutionStatistic> queueStatistics = new ArrayList<ActionExecutionStatistic>();
        Iterator<String> agentsIterator = agentsRunningThisQueue.iterator();
        while (agentsIterator.hasNext()) {
            queueStatistics.addAll(getActionExecutionResults(agentsIterator.next(), queueName));
        }

        // sum up statistics for same actions from different queues
        Map<String, ActionExecutionStatistic> summedQueueStatistics = new HashMap<String, ActionExecutionStatistic>();
        for (ActionExecutionStatistic queueStatistic : queueStatistics) {
            String actionName = queueStatistic.getActionName();
            ActionExecutionStatistic summedQueueStatistic = summedQueueStatistics.get(actionName);
            if (summedQueueStatistic == null) {
                summedQueueStatistics.put(actionName, queueStatistic);
            } else {
                // this action is run on more than 1 agent, merge results into a single statistic
                summedQueueStatistic.merge(queueStatistic);
            }
        }

        // calculate the pass rate for this queue
        int maxNumberExecutions = 0; // tells us the number of queue iterations
        int minNumberPassed = Integer.MAX_VALUE; // tells us the number of passed queue iterations
        for (ActionExecutionStatistic statistic : summedQueueStatistics.values()) {

            int executions = statistic.getNumberPassed() + statistic.getNumberFailed();
            if (maxNumberExecutions < executions) {
                maxNumberExecutions = executions;
            }

            int passed = statistic.getNumberPassed();
            if (minNumberPassed > passed) {
                minNumberPassed = passed;
            }
        }

        // % passed iterations
        float actuallyPassed = ((float) minNumberPassed) / maxNumberExecutions * 100;

        if (actuallyPassed >= queuePassRate) {
            return LoadQueueResult.PASSED;
        } else {
            return LoadQueueResult.FAILED;
        }
    }

    /**
     * Get the action execution results for a given queue on a given agent
     */
    public List<ActionExecutionStatistic>
            getActionExecutionResults( String atsAgent, String queueName ) throws AgentException {

        try {
            String sessionId = ExecutorUtils.createExecutorId(atsAgent, ExecutorUtils.getUserRandomToken(),
                                                              Thread.currentThread().getName());
            ActionExecutionStatistic[] result = (ActionExecutionStatistic[]) restHelper.executeRequest(atsAgent,
                                                                                                       "/queues/opts",
                                                                                                       "POST",
                                                                                                       "{\"sessionId\":\""
                                                                                                               + sessionId
                                                                                                               + "\",\"queueName\":\""
                                                                                                               + queueName
                                                                                                               + "\", \"operation\":\"getActionExecutionResults\"}",
                                                                                                       "queue_results",
                                                                                                       ActionExecutionStatistic[].class);

            return Arrays.asList(result);

        } catch (Exception ioe) {
            // log hint for further serialization issue investigation
            log.error(ioe);
            throw new AgentException(ioe);
        }
    }

    private String getHostsList() {

        StringBuilder hostsLists = new StringBuilder();
        for (String host : atsAgents) {
            if (host.startsWith(HostUtils.LOCAL_HOST_IPv4)) {
                host = host.replace(HostUtils.LOCAL_HOST_IPv4, HostUtils.getLocalHostIP());
            } else if (host.startsWith(HostUtils.LOCAL_HOST_NAME)) {
                host = host.replace(HostUtils.LOCAL_HOST_NAME, HostUtils.getLocalHostIP());
            } else if (host.startsWith(HostUtils.LOCAL_HOST_IPv6)) {
                host = host.replace(HostUtils.LOCAL_HOST_IPv6, HostUtils.getLocalHostIP());
            }
            hostsLists.append( (hostsLists.length() > 0
                                                        ? ", "
                                                        : "")
                               + host);
        }

        return hostsLists.toString();
    }
}
