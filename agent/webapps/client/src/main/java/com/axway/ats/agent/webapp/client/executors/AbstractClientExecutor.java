/*
 * Copyright 2017-2019 Axway Software
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

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.AbstractActionTask;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.patterns.ThreadingPattern;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.io.DbAccessFactory;
import com.axway.ats.log.autodb.io.SQLServerDbWriteAccess;

public abstract class AbstractClientExecutor implements ClientExecutor {

    protected AtsDbLogger                 log;

    private static final String           LOCAL_MACHINE = "127.0.0.1";

    protected String                      queueName;
    protected LoaderDataConfig            loaderDataConfig;
    protected ThreadingPattern            threadingPattern;

    private static SQLServerDbWriteAccess dbAccess;

    protected AbstractClientExecutor() {

        /** since we want to be able to execute actions on the agent even if there is no db appender attached,
         *  we skip the check whether that appender is attached, by passing true as a second argument
         */
        log = AtsDbLogger.getLogger(this.getClass().getName(), true);
    }

    public abstract Object executeAction( ActionRequest actionRequest ) throws AgentException;

    public abstract boolean isComponentLoaded( ActionRequest actionRequest ) throws AgentException;

    public abstract String getAgentHome() throws AgentException;

    public abstract int getNumberPendingLogEvents() throws AgentException;

    public abstract void restore( String componentName, String environmentName,
                                  String folderPath ) throws AgentException;

    public abstract void restoreAll( String environmentName ) throws AgentException;

    public abstract void backup( String componentName, String environmentName,
                                 String folderPath ) throws AgentException;

    public abstract void backupAll( String environmentName ) throws AgentException;

    public void executeActions( List<ActionRequest> actionRequests ) throws AgentException {

        for (ActionRequest actionRequest : actionRequests) {
            executeAction(actionRequest);
        }
    }

    public abstract void waitUntilQueueFinish() throws AgentException;

    /**
     * Start performance queue in the database and retrieve its ID
     *
     * @return the started queue ID
     * @throws AgentException 
     */
    public int retrieveQueueId( int sequence, String hostsList ) throws AgentException {

        if(!ActiveDbAppender.isAttached) {
            throw new AgentException("Unable to retrieve queue id from ATS Log database. Db appender is not attached.");
        }
        
        int queueId;
        try {
            if (dbAccess == null) {
                dbAccess = new DbAccessFactory().getNewDbWriteAccessObject();
            }
            queueId = dbAccess.startLoadQueue(queueName, sequence, hostsList,
                                              threadingPattern.getPatternDescription(),
                                              threadingPattern.getThreadCount(), LOCAL_MACHINE,
                                              Calendar.getInstance().getTimeInMillis(),
                                              ActiveDbAppender.getCurrentInstance().getTestCaseId(), true);

            log.rememberLoadQueueState(queueName, queueId, threadingPattern.getPatternDescription(),
                                       threadingPattern.getThreadCount());
        } catch (DatabaseAccessException e) {
            if (ActiveDbAppender.getCurrentInstance() == null) {
                // The log4j2 DB appender is not attached
                // We assume the user is running a performance test without DB logging
                log.warn("Unable to register a performance queue with name '" + queueName
                         + "' in the loggging database."
                         + " This means the results of running this queue will not be registered in the log DB."
                         + " Check your DB configuration in order to fix this problem.");

                // Return this invalid value will not cause an error on the Test Executor side
                // We also know there will be no error on agent's side as our DB appender will not be present there
                queueId = -1;
            } else {
                throw new AgentException("Unable to register a performance queue with name '" + queueName
                                         + "' in the loggging database. This queue will not run at all.",
                                         e);
            }
        }

        return queueId;
    }

    /**
     * Insert checkpoints summaries for each action request
     * @return the checkpoints IDs
     * @throws AgentException
     */
    public void populateCheckpointsSummary( int loadQueueId,
                                            List<ActionRequest> actionRequests ) throws AgentException {

        try {

            if (dbAccess == null) {
                dbAccess = new DbAccessFactory().getNewDbWriteAccessObject();
            }

            // If user add same action more than once in same queue,
            // we must not populate it more than once
            Set<String> actionNames = new HashSet<>();
            
            for( ActionRequest actionRequest : actionRequests ) {
                if( actionRequest.getRegisterActionExecution() ) {
                    String actionName = actionRequest.getActionName();
                    if( !actionNames.contains( actionName ) ) {
                        actionNames.add( actionName );
                        dbAccess.populateCheckpointSummary( loadQueueId, actionName,
                                                            actionRequest.getTransferUnit(), true );
                    }
                }
            }

            dbAccess.populateCheckpointSummary(loadQueueId, AbstractActionTask.ATS_ACTION__QUEUE_EXECUTION_TIME, "",
                                               true);
        } catch (DatabaseAccessException e) {
            throw new AgentException("Unable to populate checkpoint summary data for queued actions", e);
        }

    }
}
