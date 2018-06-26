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
package com.axway.ats.agent.webapp.client;

import java.io.IOException;

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
import org.apache.log4j.Logger;

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class is used whenever a call to the agent must be performed, except:
 * <p>- invoking action ( use {@link com.axway.ats.agent.webapp.client.executors.RemoteExecutor.HttpClient} )</p>
 * <p>- executing monitoring operation (use {@link com.axway.ats.monitoring.RestHelper})</p>
 * 
 * */
public class RestHelper {

    private static final Gson   gson = new Gson();
    private static final Logger log  = Logger.getLogger(RestHelper.class);

    /**
     * Execute REST request to an ATS Agent and get some value from the response.
     * <p> Use this method when a request to the Agent does not come from an Action method invocation. </p>
     * @param url the relative URL of the REST method. The agent IP:Port will be added for the final request URL
     * @param httpMethodName the HTTP method name for performing the REST operation ("POST", "DELETE", etc)
     * @param requestBody the request body ( in JSON )
     * @param responseJsonObjectKey the key, which points to a JSON field in the response's body. This field will be deserialized and return from this method.
     * @param returnType the return type, which will be used to deserialize the response's field, pointed by responseJsonObjectKey argument
     * @throws AgentException
     * 
     * */
    public Object executeRequest( String atsAgent, String url, String httpMethodName, String requestBody,
                                  String responseJsonObjectKey,
                                  Class<?> returnType ) throws AgentException {

        JsonObject jsonObject = executeRequest(constructAbsoluteRequestUrl(atsAgent, url), httpMethodName,
                                               requestBody);
        if (StringUtils.isNullOrEmpty(responseJsonObjectKey)) {
            if (returnType == null) {
                return null;
            } else {
                throw new AgentException("Specified response JSON Object key is null/empty");
            }
        }
        JsonElement jsonElement = jsonObject.get(responseJsonObjectKey);
        if (jsonElement == null) {
            throw new AgentException("There are no JSON element in the response's body, specified by key '"
                                     + responseJsonObjectKey + "'");
        }
        return gson.fromJson(jsonElement, returnType);

    }

    /**
     * Execute REST request.
     * @param url the full URL to the REST method
     * @param httpMethodName the HTTP method name for performing the REST operation ("POST", "DELETE", etc)
     * @param requestBody the request body ( in JSON )
     * @return {@link JsonObject}
     * @throws AgentException
     * */
    private JsonObject executeRequest( String url, String httpMethodName, String requestBody ) throws AgentException {

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpRequestBase httpRequest = null;
        StringEntity requestBodyEntity = null;
        CloseableHttpResponse httpResponse = null;
        try {

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
            if (log.isDebugEnabled()) {
                log.debug("Received HTTP response with body '" + responseString + "'");
            }
            if (!httpResponse.getFirstHeader("Content-type").getValue().equalsIgnoreCase("application/json")) {
                throw new RuntimeException("Response's Content-type is not 'application/json', but '"
                                           + httpResponse.getFirstHeader("Content-type").getValue()
                                           + "'. Status line is '" + httpResponse.getStatusLine().toString());
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
            throw new AgentException("Unable to execute '" + httpMethodName + "' to '" + url + "'", e);
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

    public String serializeJavaObject( Object object ) {

        if (object == null) {
            throw new RuntimeException("Provided object is null");
        }

        return gson.toJson(object);
    }

    public Object deserializeJavaObject( String json, Class<?> clss ) {

        if (StringUtils.isNullOrEmpty(json)) {
            throw new RuntimeException("Provided JSON representation of an object is null/empty");
        }

        if (clss == null) {
            throw new RuntimeException("Provided class is null");
        }

        return gson.fromJson(json, clss);
    }

    private String constructAbsoluteRequestUrl( String atsAgent, String requestUrl ) {

        String absoluteRequestUrl = HostUtils.getAtsAgentIpAndPort(atsAgent);

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
}
