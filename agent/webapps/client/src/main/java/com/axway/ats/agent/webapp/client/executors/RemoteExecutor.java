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
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
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
import com.axway.ats.agent.webapp.restservice.api.AgentRestApiDefinitions;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.utils.ExecutorUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * This executor will execute an action or a set of actions on
 * a remote ATS Agent
 */
public class RemoteExecutor extends AbstractClientExecutor {

    protected String   atsAgent;
    protected String   atsAgentSessionId;
    private HttpClient client = new HttpClient();

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
     * Constructor to be used by inheriting classes
     */
    protected RemoteExecutor() {

        this.atsAgent = null;
    }

    @Override
    public int initializeAction( ActionRequest actionRequest ) throws AgentException {

        return client.initializeAction(actionRequest);

    }

    @Override
    public void deinitializeAction( int actionId ) throws AgentException {

        client.deinitializeAction(actionId);

    }

    @Override
    public Object executeAction( ActionRequest actionRequest ) throws AgentException {

        return client.executeAction(actionRequest);
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

        public int initializeAction( ActionRequest actionRequest ) throws AgentException {

            log.info("We will initialize action class for action method '" + actionRequest.getActionName() + "' on '"
                     + atsAgent + "'");
            String url = (atsAgent.startsWith("http://")
                                                         ? atsAgent
                                                         : "http://" + atsAgent)
                         + AgentRestApiDefinitions.INITIALIZE_ACTION_CLASS_PATH;
            String[] argumentsTypes = new String[actionRequest.getArguments().length];
            String[] argumentsValues = new String[actionRequest.getArguments().length];

            for (int i = 0; i < actionRequest.getArguments().length; i++) {
                argumentsTypes[i] = actionRequest.getArguments()[i].getClass().getName();
                argumentsValues[i] = gson.toJson(argumentsValues[i]);
            }
            String jsonRequestBody = createJson("initializeAction",
                                                new String[]{ "actionMethodName", "componentName", "argumentsTypes",
                                                              "argumentsValues" },
                                                new Object[]{ actionRequest.getActionName(),
                                                              actionRequest.getComponentName(), argumentsTypes,
                                                              argumentsValues });

            String jsonResponseBody = executeRequest(url, HttpPut.METHOD_NAME, jsonRequestBody);
            JsonElement element = new JsonParser().parse(jsonResponseBody);
            JsonObject object = element.getAsJsonObject();
            if (object.get("error") != null) {
                Exception exception = new Gson().fromJson(object.get("error").toString(), Exception.class);
                throw new AgentException("Error while initializing action '" + actionRequest.getActionName() + "'",
                                         exception);
            }
            JsonPrimitive actionIdPrimitive = object.getAsJsonPrimitive("actionId");
            if (actionIdPrimitive == null) {
                throw new AgentException("No error occured, but actionId was not provided in the response");
            }
            return actionIdPrimitive.getAsInt();
        }

        public void deinitializeAction( int actionId ) throws AgentException {

            log.info("We will initialize action with id '" + actionId + "' on '" + atsAgent + "'");
            String url = (atsAgent.startsWith("http://")
                                                         ? atsAgent
                                                         : "http://" + atsAgent)
                         + AgentRestApiDefinitions.DEINITIALIZE_ACTION_CLASS_PATH + "?actionId=" + actionId;
            String jsonResponseBody = executeRequest(url, HttpDelete.METHOD_NAME, null);
            JsonElement element = new JsonParser().parse(jsonResponseBody);
            JsonObject object = element.getAsJsonObject();
            if (object.get("error") != null) {
                Exception exception = new Gson().fromJson(object.get("error").toString(), Exception.class);
                throw new AgentException("Error while deinitializing action class with id '" + actionId + "'",
                                         exception);
            }
            JsonPrimitive actionIdPrimitive = object.getAsJsonPrimitive("actionId");
            JsonPrimitive statusPrimitive = object.getAsJsonPrimitive("status");
            if (actionIdPrimitive == null || statusPrimitive == null) {
                if (actionIdPrimitive == null) {
                    throw new AgentException("No error occured, but neither actionId, nor status JSON object was provided in the response");
                }
            }

        }

        public String executeAction( ActionRequest actionRequest ) throws AgentException {

            log.info("We will execute action '" + actionRequest.getActionName() + "' on '" + atsAgent + "'");
            String url = (atsAgent.startsWith("http://")
                                                         ? atsAgent
                                                         : "http://" + atsAgent)
                         + AgentRestApiDefinitions.EXECUTE_ACTION_METHOD_PATH;
            String[] argumentsTypes = new String[actionRequest.getArguments().length];
            String[] argumentsValues = new String[actionRequest.getArguments().length];

            for (int i = 0; i < actionRequest.getArguments().length; i++) {
                argumentsTypes[i] = actionRequest.getArguments()[i].getClass().getName();
                argumentsValues[i] = gson.toJson(actionRequest.getArguments()[i]).replace("\"", "'");
            }
            String jsonRequestBody = createJson("executeAction",
                                                new String[]{ "actionMethodName", "componentName", "argumentsTypes",
                                                              "argumentsValues" },
                                                new Object[]{ actionRequest.getActionName(),
                                                              actionRequest.getComponentName(), argumentsTypes,
                                                              argumentsValues });
            String jsonResponseBody = executeRequest(url, HttpPost.METHOD_NAME, jsonRequestBody);
            JsonElement element = new JsonParser().parse(jsonResponseBody);
            JsonObject object = element.getAsJsonObject();
            if (object.get("error") != null) {
                Exception exception = new Gson().fromJson(object.get("error").toString(), Exception.class);
                throw new AgentException("Error while executing action '" + actionRequest.getActionName() + "'",
                                         exception);
            }
            return object.get("result").toString();
        }

        private String createJson( String action, String[] keys, Object[] values ) {

            StringBuilder sb = new StringBuilder();
            sb.append("{");

            if (keys.length != values.length) {
                throw new IllegalArgumentException("Error running '" + action
                                                   + "'. The number of expected keys is " + keys.length
                                                   + ", but we got " + values.length
                                                   + " indeed. Please consult the documentation.");
            }

            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                sb.append("\"").append(key).append("\"").append(": ");
                Object value = values[i];
                if (value == null) {
                    sb.append(value);
                } else {
                    if (value instanceof String) {
                        sb.append("\"").append((String) value).append("\"");
                    } else if (value instanceof Number) {
                        sb.append((Number) value);
                    } else if (value instanceof String[]) {
                        sb.append("[");
                        for (String obj : (String[]) value) {
                            sb.append("\"").append(obj).append("\"").append(",");
                        }
                        if ( ((String[]) value).length > 0) {
                            sb = new StringBuilder(sb.subSequence(0, sb.length() - 1));
                        }
                        sb.append("]");
                    } else if (value instanceof Boolean) {
                        sb.append((Boolean) value);
                    } else if (value instanceof Map) {
                        /* 
                         * In order to serialize java.util.Map object to JSON,
                         * transform it to a JSON array, and each array element will contains a JSON object (the key-value pair of each map entry)
                         * Example: 
                         *     Map<String, String> someMap = new HashMap<String, String>();
                               someMap.put("dbHost", "127.0.0.1");
                               someMap.put("dbPort", "5555");
                           will be transformed to
                           { 
                               ...
                               'map':[
                                 {'key':'dbHost',value':'127.0.0.1'},
                                 {'key':'dbPort',value':'5555'}
                               ]
                               ...
                           }
                         * */
                        sb.append("[");
                        Map<String, String> map = ((Map<String, String>) value);
                        if (map.isEmpty()) {
                            sb.append("]");
                        } else {
                            for (Map.Entry<String, String> mapEntry : map.entrySet()) {
                                sb.append("{");
                                sb.append("\"")
                                  .append("key")
                                  .append("\"")
                                  .append(":")
                                  .append("\"")
                                  .append(mapEntry.getKey())
                                  .append("\"")
                                  .append(",");
                                sb.append("\"")
                                  .append("value")
                                  .append("\"")
                                  .append(":")
                                  .append("\"")
                                  .append(mapEntry.getValue())
                                  .append("\"");
                                sb.append("},");
                            }
                            sb = new StringBuilder(sb.subSequence(0, sb.length() - 1));
                            sb.append("]");
                        }

                    } else {
                        throw new IllegalArgumentException("Error running '" + action
                                                           + "'. Invallid value type '"
                                                           + value.getClass().getSimpleName()
                                                           + "'. String, String[], Number, Boolean and Object are supported.");
                    }
                }

                sb.append(",");
            }

            String json = sb.toString().substring(0, sb.length() - 1);
            json += "}";
            return json;
        }

        private String executeRequest( String url, String httpMethodName, String jsonRequestBody ) {

            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpRequestBase httpRequest = null;
            StringEntity requestBody = null;
            CloseableHttpResponse httpResponse = null;
            try {
                switch (httpMethodName) {
                    case HttpPost.METHOD_NAME:
                        httpRequest = new HttpPost(url);
                        requestBody = new StringEntity(jsonRequestBody);
                        ((HttpPost) httpRequest).setEntity(requestBody);
                        httpRequest.setHeader("Content-type", "application/json");
                        break;
                    case HttpPut.METHOD_NAME:
                        httpRequest = new HttpPut(url);
                        requestBody = new StringEntity(jsonRequestBody);
                        ((HttpPut) httpRequest).setEntity(requestBody);
                        httpRequest.setHeader("Content-type", "application/json");
                        break;
                    case HttpDelete.METHOD_NAME:
                        httpRequest = new HttpDelete(url);
                        break;
                    case HttpGet.METHOD_NAME:
                        httpRequest = new HttpGet(url);
                        break;
                    case HttpHead.METHOD_NAME:
                        httpRequest = new HttpHead(url);
                        break;
                    default:
                        throw new IllegalArgumentException("HTTP method '" + httpMethodName + "' is not supported");
                }
                httpRequest.setHeader("Accept", "application/json");
                HttpClientContext context = HttpClientContext.create();
                httpResponse = client.execute(httpRequest, context);
                String responseString = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                return responseString;
            } catch (Exception e) {
                throw new RuntimeException("Unable to execute '" + httpMethodName + "' to '" + url + "'", e);
            } finally {
                try {
                    if (httpResponse != null) {
                        httpResponse.close(); // maybe I must not close it , so after method exited, it is still accessible
                    }
                    if (client != null) {
                        client.close();
                    }
                } catch (IOException e) {
                    log.error("Unable to close HTTP client after action call to ATS agent ('" + url + "')", e);
                }
            }
        }
    }

}
