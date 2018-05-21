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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.ActionWrapper;
import com.axway.ats.agent.webapp.client.AgentException_Exception;
import com.axway.ats.agent.webapp.client.AgentService;
import com.axway.ats.agent.webapp.client.AgentServicePool;
import com.axway.ats.agent.webapp.client.ArgumentWrapper;
import com.axway.ats.agent.webapp.client.InternalComponentException_Exception;
import com.axway.ats.agent.webapp.restservice.api.actions.ActionPojo;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This executor will execute an action or a set of actions on
 * a remote ATS Agent
 */
public class RemoteExecutor extends AbstractClientExecutor {

    private enum ActionRequestType {
        /**
         * Some predefined ATS actions execute static Java methods.
         * In order to differentiate them from the non-static once,
         * we use this value for them and not INIITALIZE_ATS_ACTION
         * */
        DEFAULT_INITIALIZATION,
        /**
         * The request is about to initialize ATS predefined action class
         * */
        INIITALIZE_ATS_ACTION,
        /**
         * The request is about to initialize custom action class
         * */
        INITIALIZE_CUSTOM_ACTION
    }

    protected String   atsAgent;
    protected String   atsAgentSessionId;
    protected int      resourceId = -1;
    protected String   initializeRequestUrl;
    protected String   executeRequestUrl;

    private HttpClient httpClient = new HttpClient();

    /**
     * @param atsAgent the remote agent address
     * @param initializeRequestUrl the URL that will be used to tell the ATS Agent to initialize actions that will be executed via this class
     * @throws AgentException
     */
    public RemoteExecutor( String atsAgent, String initializeRequestUrl ) throws AgentException {

        this(atsAgent, initializeRequestUrl, true);
    }

    /**
     * @param atsAgent the remote agent address
     * @throws AgentException
     */
    public RemoteExecutor( String atsAgent ) throws AgentException {

        this(atsAgent, true);
    }

    /**
     * @param atsAgent the remote agent address
     * @param configureAgent whether we want to send the log configuration to the agent.</br>
     * Pass <i>false</i> when there is chance that the agent is not available - for example 
     * if it was just restarted.
     * @throws AgentException
     */
    public RemoteExecutor( String atsAgent, boolean configureAgent ) throws AgentException {

        // we assume the ATS Agent address here comes with IP and PORT
        this.atsAgent = atsAgent;
        this.atsAgentSessionId = ExecutorUtils.createExecutorId(atsAgent,
                                                                Thread.currentThread().getName());

        if (configureAgent) {
            //configure the remote executor(an ATS agent)
            try {
                TestcaseStateEventsDispacher.getInstance().onConfigureAtsAgents(Arrays.asList(atsAgent));
            } catch (Exception e) {
                // we know for sure this is an AgentException, but as the interface declaration is in Core library,
                // we could not declare the AgentException, but its parent - the regular java Exception
                throw(AgentException) e;
            }
        }
    }

    /**
     * @param atsAgent the remote agent address
     * @param initializeRequestUrl the URL that will be used to tell the ATS Agent to initialize actions that will be executed via this class
     * @param configureAgent whether we want to send the log configuration to the agent.</br>
     * Pass <i>false</i> when there is chance that the agent is not available - for example 
     * if it was just restarted.
     * @throws AgentException
     */
    public RemoteExecutor( String atsAgent, String initializeRequestUrl,
                           boolean configureAgent ) throws AgentException {

        // we assume the ATS Agent address here comes with IP and PORT
        this.atsAgent = atsAgent;
        this.atsAgentSessionId = ExecutorUtils.createExecutorId(atsAgent,
                                                                Thread.currentThread().getName());
        if (StringUtils.isNullOrEmpty(initializeRequestUrl)) {
            // we assume that we have a custom action
            this.initializeRequestUrl = "/actions/";
            this.executeRequestUrl = "/actions/execute";
        } else {
            // this URL is received all the way from the client Action class from which this RemoteExecutor is created
            this.initializeRequestUrl = initializeRequestUrl;
            this.executeRequestUrl = null; // each action will provide proper execution URL
        }

        if (configureAgent) {
            //configure the remote executor(an ATS agent)
            try {
                TestcaseStateEventsDispacher.getInstance().onConfigureAtsAgents(Arrays.asList(atsAgent));
            } catch (Exception e) {
                // we know for sure this is an AgentException, but as the interface declaration is in Core library,
                // we could not declare the AgentException, but its parent - the regular java Exception
                throw(AgentException) e;
            }
        }
    }

    /**
     * Constructor to be used by inheriting classes
     */
    protected RemoteExecutor() {

        this.atsAgent = null;
    }

    @Override
    public Object executeAction( ActionRequest actionRequest ) throws AgentException {

        String requestUrl = null;
        String requestMethod = null;
        String requestBody = null;
        JsonObject responseBody = null;
        Object result = null;
        ActionRequestType actionRequestType = null;
        if (resourceId < 0) {
            actionRequestType = initializeAction(actionRequest);
            if (actionRequestType == ActionRequestType.INIITALIZE_ATS_ACTION) {
                /* Since each ATS predefined action class is initialized before any of its actions is called,
                 * we can just safely return here
                 */
                return result; // will always return null
            }
        }

        if (actionRequestType == ActionRequestType.INITIALIZE_CUSTOM_ACTION) {
            /*
             * Since this is a request from a custom action
             * we must not only register the action class,
             * but execute the action as well
             * */
            requestUrl = constructAbsoluteRequestUrl(this.executeRequestUrl);
            requestMethod = "POST";
            requestBody = createCustomActionRequestBody(actionRequest);

        } else {
            if (!StringUtils.isNullOrEmpty(actionRequest.getRequestUrl())) {
                /* if the action is ATS predefined one and its action class is already registered on the ATS agent,
                 * we end up here and just execute the action request
                 */

                requestUrl = constructAbsoluteRequestUrl(actionRequest.getRequestUrl());
                requestMethod = actionRequest.getRequestMethod();
                requestBody = actionRequest.getRequestBody();
            } else {
                /* this is custom action that which class was already initialized
                 */
                requestUrl = constructAbsoluteRequestUrl(this.executeRequestUrl);
                requestMethod = "POST";
                requestBody = createCustomActionRequestBody(actionRequest);
            }

        }

        log.info("Start executing action '" + actionRequest.getActionName() + "' with arguments "
                 + StringUtils.methodInputArgumentsToString(actionRequest.getArguments()));

        responseBody = httpClient.executeRequest(requestUrl,
                                                 requestMethod,
                                                 requestBody);

        if (actionRequest.getReturnType() != null) {
            // the action returns some result, so cast it to the appropriate class and return it
            result = httpClient.gson.fromJson(responseBody.get("action_result"), actionRequest.getReturnType());
        }

        log.info("Successfully executed action '" + actionRequest.getActionName() + "'");

        return result;

    }

    /**
     * Initializes a action class via action request
     * @param actionRequest the action request that contains information about the action
     * @return the action request type {@link RemoteExecutor.ActionRequestType}
     * */
    private ActionRequestType initializeAction( ActionRequest actionRequest ) {

        log.info("Start registration of action class for action '" + actionRequest.getActionName() + "' with arguments "
                 + StringUtils.methodInputArgumentsToString(actionRequest.getArguments()));

        String requestUrl = null;
        String requestMethod = null;
        String requestBody = null;
        JsonObject responseBody = null;
        ActionRequestType actionRequestType = null;
        if (StringUtils.isNullOrEmpty(actionRequest.getRequestUrl())) {
            // this is an Action request from a custom/third-party action
            requestUrl = constructAbsoluteRequestUrl("actions");
            requestMethod = "PUT";
            requestBody = createCustomActionRequestBody(actionRequest);
            actionRequestType = ActionRequestType.INITIALIZE_CUSTOM_ACTION;
        } else {
            // this is an Action request from an ATS predefined action
            if (this.initializeRequestUrl.equals(actionRequest.getRequestUrl())) {
                requestUrl = constructAbsoluteRequestUrl(actionRequest.getRequestUrl());
                requestMethod = actionRequest.getRequestMethod();
                requestBody = actionRequest.getRequestBody();
                actionRequestType = ActionRequestType.INIITALIZE_ATS_ACTION;
            } else {
                requestUrl = constructAbsoluteRequestUrl(this.initializeRequestUrl);
                requestMethod = "PUT";
                requestBody = "{\"defaultInitialization\":true}";
                actionRequestType = ActionRequestType.DEFAULT_INITIALIZATION;
            }
        }

        responseBody = httpClient.executeRequest(requestUrl,
                                                 requestMethod,
                                                 requestBody);

        // we assume that the request will contain resourceId field
        resourceId = responseBody.get("resourceId").getAsInt();

        log.info("Successfully registered action class for action '" + actionRequest.getActionName() + "'");

        return actionRequestType;

    }

    private String createCustomActionRequestBody( ActionRequest actionRequest ) {

        String[] argumentsTypes = null;
        String[] argumentsValues = null;
        if (actionRequest.getArguments() != null) {
            argumentsTypes = new String[actionRequest.getArguments().length];
            argumentsValues = new String[actionRequest.getArguments().length];
            for (int i = 0; i < actionRequest.getArguments().length; i++) {
                Class<?> argClass = actionRequest.getArguments()[i].getClass();
                argumentsTypes[i] = argClass.getName();
                argumentsValues[i] = httpClient.gson.toJson(actionRequest.getArguments()[i], argClass);
            }
        }

        ActionPojo actionPojo = new ActionPojo(atsAgentSessionId, resourceId, actionRequest.getComponentName(),
                                               actionRequest.getActionName(), argumentsTypes, argumentsValues);
        return httpClient.gson.toJson(actionPojo, ActionPojo.class);
    }

    private String constructAbsoluteRequestUrl( String requestUrl ) {

        String absoluteRequestUrl = HostUtils.getAtsAgentIpAndPort(this.atsAgent);

        if (!absoluteRequestUrl.startsWith("http://")) {
            absoluteRequestUrl = "http://" + absoluteRequestUrl;
        }

        if (!absoluteRequestUrl.endsWith("/")) {
            absoluteRequestUrl += "/";
        }

        // BASE path to the Agent REST API service
        // Maybe it is good to extract the hard-coded String to a constant variable somewhere in the agent's classes (AgentRestApiDefinitions?)
        absoluteRequestUrl += "agentapp/api/v1";

        if (!requestUrl.startsWith("/")) {
            absoluteRequestUrl += "/";
        }

        return absoluteRequestUrl + requestUrl;
    }

    @Override
    public boolean isComponentLoaded( ActionRequest actionRequest ) throws AgentException {

        try {
            AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

            //FIXME: swap with ActionWrapper
            return agentServicePort.isComponentLoaded(actionRequest.getComponentName());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getAgentHome() throws AgentException {

        try {
            return AgentServicePool.getInstance().getClientForHost(atsAgent).getAgentHome();
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    public List<String> getClassPath() throws AgentException {

        return AgentServicePool.getInstance().getClientForHost(atsAgent).getClassPath();
    }

    public void logClassPath() throws AgentException {

        AgentServicePool.getInstance().getClientForHost(atsAgent).logClassPath();
    }

    public List<String> getDuplicatedJars() throws AgentException {

        return AgentServicePool.getInstance().getClientForHost(atsAgent).getDuplicatedJars();
    }

    public void logDuplicatedJars() throws AgentException {

        AgentServicePool.getInstance().getClientForHost(atsAgent).logDuplicatedJars();
    }

    @Override
    public int getNumberPendingLogEvents() throws AgentException {

        try {
            return AgentServicePool.getInstance().getClientForHost(atsAgent).getNumberPendingLogEvents();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void restore( String componentName, String environmentName,
                         String folderPath ) throws AgentException {

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

        try {
            agentServicePort.restoreEnvironment(componentName, environmentName, folderPath);
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    @Override
    public void restoreAll( String environmentName ) throws AgentException {

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

        try {
            //passing null will clean all components
            agentServicePort.restoreEnvironment(null, environmentName, null);
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    @Override
    public void backup( String componentName, String environmentName,
                        String folderPath ) throws AgentException {

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

        try {
            agentServicePort.backupEnvironment(componentName, environmentName, folderPath);
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    @Override
    public void backupAll( String environmentName ) throws AgentException {

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

        try {
            //passing null will backup all components
            agentServicePort.backupEnvironment(null, environmentName, null);
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    @Override
    public void waitUntilQueueFinish() throws AgentException {

        /*
         * In real environment, this method is most likely never used
         * as this remote client is used for running a single action, so
         * no performance queues are available.
         *
         * TODO: maybe we can safely throw some error here to say that
         * it is not expected to call this method.
         * Or we can implement an empty body here or in the abstract parent.
         */

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgentSessionId);

        try {
            log.info("Waiting until all queues on host '" + atsAgent + "' finish execution");

            agentServicePort.waitUntilAllQueuesFinish();
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    /**
     * Wrap the action request into an ActionWrapper so it is easily
     * passed to the web service
     *
     * @param actionRequest the action request to wrap
     * @return the action wrapper
     * @throws AgentException on error
     */
    protected final ActionWrapper wrapActionRequest( ActionRequest actionRequest ) throws AgentException {

        Object[] arguments = actionRequest.getArguments();

        List<ArgumentWrapper> wrappedArguments = new ArrayList<ArgumentWrapper>();

        try {
            //wrap the arguments - each argument is serialized as
            //a byte stream
            for (Object argument : arguments) {
                ArgumentWrapper argWrapper = new ArgumentWrapper();

                ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);
                objectOutStream.writeObject(argument);

                argWrapper.setArgumentValue(byteOutStream.toByteArray());
                wrappedArguments.add(argWrapper);
            }
        } catch (IOException ioe) {
            throw new AgentException("Could not serialize input arguments", ioe);
        }

        //construct the action wrapper
        ActionWrapper actionWrapper = new ActionWrapper();
        actionWrapper.setComponentName(actionRequest.getComponentName());
        actionWrapper.setActionName(actionRequest.getActionName());
        actionWrapper.getArgs().addAll(wrappedArguments);

        return actionWrapper;
    }

    /** This class is used to send actions information to the Agent's REST API service **/
    class HttpClient {

        private Gson gson = new Gson();

        public JsonObject executeRequest( String url, String httpMethodName, String requestBody ) {

            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpRequestBase httpRequest = null;
            StringEntity requestBodyEntity = null;
            CloseableHttpResponse httpResponse = null;
            try {

                // add sessionId to the request body
                JsonObject requestBodyObject = new JsonParser().parse(requestBody).getAsJsonObject();
                if (!requestBodyObject.has("sessionId")) {
                    requestBodyObject.addProperty("sessionId", atsAgentSessionId);
                }
                if (resourceId > -1 && !requestBodyObject.has("resourceId")) {
                    // add resourceId to the request body
                    requestBodyObject.addProperty("resourceId", resourceId);
                    requestBody = requestBodyObject.toString();
                }
                requestBody = requestBodyObject.toString();

                if (log.isDebugEnabled()) {
                    log.debug("Executing '" + httpMethodName + "' request to '" + url + "' with body '" + requestBody
                              + "'");
                }

                switch (httpMethodName) {
                    case HttpPost.METHOD_NAME:
                        httpRequest = new HttpPost(url);
                        requestBodyEntity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                        ((HttpPost) httpRequest).setEntity(requestBodyEntity);
                        httpRequest.setHeader("Content-type", "application/json");
                        break;
                    case HttpPut.METHOD_NAME:
                        httpRequest = new HttpPut(url);
                        requestBodyEntity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                        ((HttpPut) httpRequest).setEntity(requestBodyEntity);
                        httpRequest.setHeader("Content-type", "application/json");
                        break;
                    case HttpDelete.METHOD_NAME:
                        url = transformRequestBodyToQueryParams(requestBodyObject, url);
                        httpRequest = new HttpDelete(url);
                        break;
                    case HttpGet.METHOD_NAME:
                        url = transformRequestBodyToQueryParams(requestBodyObject, url);
                        httpRequest = new HttpGet(url);
                        break;
                    case HttpHead.METHOD_NAME:
                        url = transformRequestBodyToQueryParams(requestBodyObject, url);
                        httpRequest = new HttpHead(url);
                        break;
                    default:
                        throw new IllegalArgumentException("HTTP method '" + httpMethodName + "' is not supported");
                }
                httpRequest.setHeader("Accept", "application/json");
                HttpClientContext context = HttpClientContext.create();
                httpResponse = client.execute(httpRequest, context);
                String responseString = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                if (log.isDebugEnabled()) {
                    log.debug("Received HTTP response with body '" + responseString + "'");
                }
                JsonObject jsonObject = new JsonParser().parse(responseString).getAsJsonObject();
                /* 
                 * we assume that if error key is found, an Exception occurred on the agent,
                 * so our actions must not return bodies, containing that key
                 * */
                if (jsonObject.has("error")) {
                    Class<?> exceptionClass = null;
                    try {
                        exceptionClass = Class.forName(jsonObject.get("exceptionClass").getAsString());
                    } catch (Exception e) {
                        // the exception class is not available on the executor, so deserialize it like Exception.class
                        log.error("Could not load Exception class '" + jsonObject.get("exceptionClass").getAsString()
                                  + "'. Agent exception will be thrown as a '" + Exception.class.getName() + "'");
                        exceptionClass = Exception.class;
                    }

                    Exception e = (Exception) exceptionClass.cast(gson.fromJson(jsonObject.get("error"),
                                                                                exceptionClass));
                    throw e;
                }
                return jsonObject;
            } catch (Exception e) {
                throw new RuntimeException("Unable to execute '" + httpMethodName + "' to '" + url + "'", e);
            } finally {
                try {
                    if (httpResponse != null) {
                        httpResponse.close();
                    }
                    if (client != null) {
                        client.close();
                    }
                } catch (IOException e) {
                    log.error("Unable to close HTTP client after action call to ATS agent ('" + url + "')", e);
                }
            }
        }

        private String transformRequestBodyToQueryParams( JsonObject json, String baseUrl ) {

            StringBuilder sb = new StringBuilder(baseUrl + "?");
            Iterator<?> it = json.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                JsonElement value = json.get(key);
                sb.append(key).append("=").append(value.toString().replace("\"", "")).append("&");
            }

            // remove trailing &
            sb.setLength(sb.toString().length() - 1);
            return sb.toString();
        }
    }
}
