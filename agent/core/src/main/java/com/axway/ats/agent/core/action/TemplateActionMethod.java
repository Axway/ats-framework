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
package com.axway.ats.agent.core.action;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.configuration.ConfigurationSettings;
import com.axway.ats.agent.core.configuration.TemplateActionsConfigurator;
import com.axway.ats.agent.core.configuration.TemplateActionsResponseVerificationConfigurator;
import com.axway.ats.agent.core.context.ThreadContext;
import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.templateactions.CompositeResult;
import com.axway.ats.agent.core.templateactions.model.HttpClient;
import com.axway.ats.agent.core.templateactions.model.XmlReader;
import com.axway.ats.agent.core.templateactions.model.XmlUtilities;
import com.axway.ats.agent.core.templateactions.model.objects.ActionHeader;
import com.axway.ats.agent.core.templateactions.model.objects.ActionParser;
import com.axway.ats.agent.core.templateactions.model.objects.ActionResponseObject;
import com.axway.ats.agent.core.threading.AbstractActionTask;
import com.axway.ats.common.agent.templateactions.NetworkingStopWatch;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.model.CheckpointResult;

/**
 * The template action behaves differently than the regular action.
 * The template action java method describes what the user wants to be done, the action body is actually not run at all.
 * The template action describes which XML file to be used for sending requests to the server.
 */
public class TemplateActionMethod extends ActionMethod {

    private static Logger     log                  = LogManager.getLogger(TemplateActionMethod.class);

    private String            actionClassName;                                                    // the java class name
    private String            actionMethodName;                                                   // the java method name

    private String[]          wantedXpathEntries;
    private boolean           returnResponseBodyAsString;

    private boolean           isLoggingInBatchMode = false;

    private final AtsDbLogger autoLogger;

    public TemplateActionMethod( String componentName, String actionName, String actionClassName,
                                 String actionMethodName, Method method, Class<?> actualClass ) {

        super(componentName, actionName, method, actualClass);

        this.actionClassName = actionClassName;
        this.actionMethodName = actionMethodName;
        // Intentionally get logger from AbstractActionTask class so use the same logger for action start/end checkpoints.
        // Otherwise if using current class and changing additivity flag of TemplateActionMethod to false will disable checkpoint logging
        // Also skip check whether db appender is attached
        this.autoLogger = AtsDbLogger.getLogger(AbstractActionTask.class.getName(), true);

        if (ActiveDbAppender.getCurrentInstance() != null) {
            isLoggingInBatchMode = ActiveDbAppender.getCurrentInstance().isBatchMode();
        }
    }

    public void setWantedXpathEntries( String[] wantedXpathEntries ) {

        this.wantedXpathEntries = wantedXpathEntries;
    }

    public void setReturnResponseBodyAsString( boolean returnResponseBodyAsString ) {

        this.returnResponseBodyAsString = returnResponseBodyAsString;
    }

    /**
     * Invoke action and if needed return requested XPath entries as String[][]
     *
     */
    @Override
    protected Object doInvoke( Object instance, List<String> parameterNames,
                               Object[] parameterValues ) throws IllegalArgumentException,
                                                          IllegalAccessException, InvocationTargetException,
                                                          ActionExecutionException {

        if (log.isDebugEnabled()) {
            log.debug("Executing '" + actionName + "' with arguments "
                      + StringUtils.methodInputArgumentsToString(parameterValues));
        }

        TemplateActionsResponseVerificationConfigurator responseVerificationConfigurator = (TemplateActionsResponseVerificationConfigurator) ThreadContext.getAttribute(ThreadContext.TEMPLATE_ACTION_VERIFICATION_CONFIGURATOR);
        String actionsXml = getActionsXml();

        //  insert any customer parameters into the thread scope map
        for (int iParameter = 0; iParameter < parameterNames.size(); iParameter++) {

            Object value = parameterValues[iParameter];

            // if the value is an object with more than one values, add them in a Queue
            if (value != null && (value instanceof Iterable || value.getClass().isArray())) {
                Queue<Object> queue = new LinkedList<Object>();
                if (value instanceof Iterable) {
                    for (Object oneValue : (Iterable<?>) value) {
                        queue.add(oneValue);
                    }
                } else {
                    for (Object oneValue : (Object[]) value) {
                        queue.add(oneValue);
                    }
                }
                value = queue;
            }
            ThreadContext.setAttribute(parameterNames.get(iParameter), value);
        }

        long actionStartTimestamp = -1;
        long totalTimeOfAllActionStepsNet = 0;
        long totalTimeOfAllActionStepsBetweenReqAndResp = 0;
        Object objectToReturn = null;
        String checkpointName;
        if (AbstractActionTask.REGISTER_FULL_AND_NET_ACTION_TIME_FOR_TEMPLATE_ACTIONS) {
            checkpointName = actionName + "-net";
        } else {
            checkpointName = actionName;
        }
        try {
            log.info("START running template actions from " + actionsXml);

            actionStartTimestamp = System.currentTimeMillis();
            if (isRegisterActionExecution() && !isLoggingInBatchMode) {
                autoLogger.startCheckpoint(checkpointName, "", actionStartTimestamp);
            }

            XmlReader xmlReader = new XmlReader(actionsXml);
            XmlUtilities xmlUtilities = new XmlUtilities();

            long xmlParsingTime = System.currentTimeMillis() - actionStartTimestamp;
            // this is the actual Action start time, skipping the actionXML parsing time
            actionStartTimestamp += xmlParsingTime;

            int actionNum = 1;
            long currentTimeOfActionStepRequest;
            NetworkingStopWatch stopWatch = new NetworkingStopWatch(actionMethodName);
            while (xmlReader.goToNextAction()) {

                String actionStep = actionMethodName + "[" + actionNum + "]";
                stopWatch.step0_SetNewContext(actionStep);
                String httpUrl = xmlReader.getRequestHttpUrl();
                String httpMethod = xmlReader.getRequestHttpMethod();
                List<ActionHeader> httpHeaders = xmlReader.getRequestHttpHeaders();

                // connect to the specified URL
                HttpClient httpClient = new HttpClient(httpUrl, httpMethod, httpHeaders, stopWatch);
                // send HTTP request
                String fileToSend = xmlReader.getRequestResourceFile();
                httpClient.sendHttpRequest(actionStep, fileToSend,
                                           xmlReader.hasParamsInRequestResourceFile());

                currentTimeOfActionStepRequest = stopWatch.getNetworkingTime();
                // Measure and log time between last data sent and start of receive.
                // Thread could be suspended but this could be server processing time too.
                stopWatch.step5_StartInterimTimer(); // and log request time

                // read response
                ActionResponseObject expectedHttpResponseNode = xmlReader.getResponse();
                // disconnect the connection
                //                httpClient.disconnect(); // TODO: why this is needed. This is also before getting response
                if (xmlReader.isLastAction()
                    && (wantedXpathEntries != null || returnResponseBodyAsString)) {
                    if (returnResponseBodyAsString) {

                        // this is the last action and user wants to extract the response content as string
                        ActionParser actualHttpResponse = xmlUtilities.readActionResponse(httpClient,
                                                                                          actionsXml,
                                                                                          actionNum, true);
                        String contentAsString = actualHttpResponse.getBodyContentAsString();
                        actualHttpResponse.cleanupMembers();
                        objectToReturn = contentAsString;
                        // log response time below and after that return result
                    } else {

                        // this is the last action and user wants to extract some data from the response
                        ActionParser actualHttpResponse = xmlUtilities.readActionResponse(httpClient,
                                                                                          actionsXml,
                                                                                          actionNum, false);
                        String[][] extractedXpathEntries = XmlUtilities.extractXpathEntries(null,
                                                                                            wantedXpathEntries);
                        actualHttpResponse.cleanupMembers();

                        objectToReturn = extractedXpathEntries;
                        // log response time below and after that return result
                    }

                } else {
                    // verify the received response
                    xmlUtilities.verifyResponse(actionsXml, actionMethodName, actionNum,
                                                expectedHttpResponseNode, httpClient,
                                                responseVerificationConfigurator);
                }

                long currentTimeOfActionStepEnd = stopWatch.getNetworkingTime();
                totalTimeOfAllActionStepsNet += currentTimeOfActionStepEnd;
                totalTimeOfAllActionStepsBetweenReqAndResp += stopWatch.getTimeBetweenReqAndResponse();

                if (HttpClient.logTimer.isTraceEnabled()) {
                    HttpClient.logTimer.trace("This action step " + actionStep
                                              + " time between end of send request and start of getting response time took "
                                              + stopWatch.getTimeBetweenReqAndResponse() + " ms");
                    HttpClient.logTimer.trace("This action step " + actionStep
                                              + " response network time took " + (currentTimeOfActionStepEnd
                                                                                  - currentTimeOfActionStepRequest)
                                              + " ms");
                    HttpClient.logTimer.trace("This action step " + actionStep + " total network time took "
                                              + currentTimeOfActionStepEnd + " ms");
                }

                actionNum++;
            }

            if (isRegisterActionExecution()) {
                if (HttpClient.logTimer.isTraceEnabled()) {
                    HttpClient.logTimer.trace("\t    Total net time: " + totalTimeOfAllActionStepsNet
                                              + "(action " + actionsXml + ")| actionStartTimestamp : "
                                              + (actionStartTimestamp - xmlParsingTime)
                                              + "| total time between Req and Resp: "
                                              + totalTimeOfAllActionStepsBetweenReqAndResp);
                }
                if (isLoggingInBatchMode) {
                    autoLogger.insertCheckpoint(checkpointName, actionStartTimestamp - xmlParsingTime,
                                                totalTimeOfAllActionStepsNet, 0L /* transferSize */,
                                                "" /* unit name */, CheckpointResult.PASSED);
                } else {
                    autoLogger.endCheckpoint(checkpointName, 0L /* transferSize */, CheckpointResult.PASSED,
                                             actionStartTimestamp - xmlParsingTime
                                             /* the XML parsing time was previously added to the actionStartTimestamp (but after starting the Checkpoint) */
                                                                                                             + totalTimeOfAllActionStepsNet);
                }
            }
            log.info("COMPLETE running template actions from " + actionsXml);
        } catch (Exception e) {
            if (isRegisterActionExecution()) {
                if (isLoggingInBatchMode) {
                    autoLogger.insertCheckpoint(checkpointName, 0L /* response time */,
                                                CheckpointResult.FAILED);
                } else {
                    autoLogger.endCheckpoint(checkpointName, 0L /* transfer size */,
                                             CheckpointResult.FAILED);
                }
            }
            throw new ActionExecutionException("Error executing a template action", e);
        }

        if (AbstractActionTask.REGISTER_FULL_AND_NET_ACTION_TIME_FOR_TEMPLATE_ACTIONS) {
            // Time between end of request and start of response reading - could be enabled only for some detailed investigations
            autoLogger.insertCheckpoint(actionName + "-betweenReqAndResp", actionStartTimestamp,
                                        totalTimeOfAllActionStepsBetweenReqAndResp, 0L, null,
                                        CheckpointResult.PASSED);
        }

        return new CompositeResult(objectToReturn, totalTimeOfAllActionStepsNet);
    }

    private String getActionsXml() throws ActionExecutionException {

        String templateActionFilesHome = ConfigurationSettings.getInstance().getTemplateActionsFolder();

        if (templateActionFilesHome == null) {
            throw new ActionExecutionException("Cannot execute template actions as the "
                                               + TemplateActionsConfigurator.AGENT__TEMPLATE_ACTIONS_FOLDER_PROPERTY
                                               + " configuration property is not set");
        }
        // normalize the directory path for this system
        templateActionFilesHome = IoUtils.normalizeDirPath(templateActionFilesHome);

        return templateActionFilesHome + componentName + AtsSystemProperties.SYSTEM_FILE_SEPARATOR
               + actionClassName + AtsSystemProperties.SYSTEM_FILE_SEPARATOR + actionMethodName + ".xml";
    }
}
