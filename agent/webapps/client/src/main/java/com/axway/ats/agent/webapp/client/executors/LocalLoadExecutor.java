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
import java.util.List;

import com.axway.ats.agent.core.MultiThreadedActionHandler;
import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.exceptions.NoSuchLoadQueueException;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.core.threads.ImportantThread;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.log.model.LoadQueueResult;

/**
 * NOTE: THIS CODE IS NOT OFFICIALLY EXPOSED NOR IT IS REALLY TESTED
 * 
 * This class is responsible for the execution of an action in multiple threads
 * in the local JVM
 */
public class LocalLoadExecutor extends LocalExecutor {

    private int queueSequence;

    /**
     * @param name the name of the action set
     * @param threadingPattern the threading pattern to use
     * @throws AgentException
     */
    public LocalLoadExecutor( String name, int sequence, ThreadingPattern threadingPattern,
                              LoaderDataConfig loaderDataConfig ) throws AgentException {

        super();

        this.queueName = name;
        this.queueSequence = sequence;
        this.threadingPattern = threadingPattern;
        this.loaderDataConfig = loaderDataConfig;
    }

    @Override
    public Object executeAction( ActionRequest actionRequest ) throws AgentException {

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        actions.add(actionRequest);

        executeActions(actions);

        //can't return anything as each action is executed multiple times
        return null;
    }

    @Override
    public void executeActions( List<ActionRequest> actionRequests ) throws AgentException {

        log.info("Start executing action queue '" + queueName + "'."
                 + (threadingPattern.isBlockUntilCompletion()
                                                              ? " And wait to finish."
                                                              : ""));
        // start queue in the database and retrieve its ID
        int queueId = retrieveQueueId(queueSequence, HostUtils.getLocalHostIP());

        try {
            MultiThreadedActionHandler.getInstance(ThreadsPerCaller.getCaller()).executeActions("LOCAL", queueName,
                                                                                                queueId,
                                                                                                actionRequests,
                                                                                                threadingPattern,
                                                                                                loaderDataConfig);

            if (threadingPattern.isBlockUntilCompletion()) {

                log.endLoadQueue(queueName, LoadQueueResult.PASSED);
                log.info("Finished executing action queue '" + queueName + "'");
            } else {
                // wait in another thread until the queue finish
                ImportantThread helpThread = new ImportantThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            MultiThreadedActionHandler.getInstance(ThreadsPerCaller.getCaller())
                                                      .waitUntilQueueFinish(queueName);
                            log.endLoadQueue(queueName, LoadQueueResult.PASSED);
                            log.info("Finished executing action queue '" + queueName + "'");
                        } catch (NoSuchLoadQueueException e) {
                            log.error("Error waiting for action queue '" + queueName + "' completion", e);
                            log.endLoadQueue(queueName, LoadQueueResult.FAILED);
                        }
                    }
                });
                helpThread.setExecutorId( Thread.currentThread().getName() );
                helpThread.setDescription( queueName );
                helpThread.start();
                log.info("Action queue '" + queueName + "' scheduled for asynchronous execution");
            }
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }
}
