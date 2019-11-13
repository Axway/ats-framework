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
package com.axway.ats.agent.webapp.client;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;

import com.axway.ats.agent.core.context.ApplicationContext;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.agentservice.AgentWsDefinitions;
import com.axway.ats.agent.webapp.client.configuration.AgentConfigurationLandscape;
import com.axway.ats.core.utils.BackwardCompatibility;
import com.axway.ats.core.utils.SslUtils;
import com.sun.xml.ws.client.BindingProviderProperties;

public class AgentServicePool {

    //singleton instance
    private static AgentServicePool       instance;

    //hashmap of all service ports
    private HashMap<String, AgentService> servicePorts;

    // A universe wide ;) unique ID used for maintaining session between Agent and its caller.
    // We use one instance per Test Executor JVM.
    // It is used by the Agent to recognize the caller. 
    @BackwardCompatibility
    private String                        uniqueId;

    @BackwardCompatibility
    private static boolean                useNewUuId = false;

    private AgentServicePool() {

        servicePorts = new HashMap<String, AgentService>();

        //Fix for JWSDP web services library
        //TestNG enables assertion by default and the code for
        //getting a service port fails miserably due to a forgotten
        //assert statement in the JAX-WS implementation which fails
        Thread.currentThread().getContextClassLoader().setPackageAssertionStatus("com.sun.xml.ws", false);
    }

    public static AgentServicePool getInstance() {

        if (instance == null) {
            instance = new AgentServicePool();
        }

        return instance;
    }

    public static void useNewUniqueId() {

        useNewUuId = true;
    }

    public static void useCachedUniqueId() {

        useNewUuId = false;
    }

    public AgentService getClient( String atsAgent ) throws AgentException {

        // we assume the ATS Agent address here comes with IP and PORT

        AgentService servicePort = servicePorts.get(atsAgent);
        if (servicePort == null) {
            servicePort = createServicePort(atsAgent);
            servicePorts.put(atsAgent, servicePort);
        }

        return servicePort;
    }

    private AgentService createServicePort( String host ) throws AgentException {

        try {
            String protocol = AgentConfigurationLandscape.getInstance(host).getConnectionProtocol();
            if (protocol == null) {
                protocol = "http";
            } else {
                SslUtils.trustAllHttpsCertificates();
                SslUtils.trustAllHostnames();
            }

            URL url = this.getClass()
                          .getResource("/META-INF/wsdl/" + AgentWsDefinitions.AGENT_SERVICE_XML_LOCAL_NAME
                                       + ".wsdl");

            Service agentService = Service.create(url,
                                                  new QName(AgentWsDefinitions.AGENT_SERVICE_XML_TARGET_NAMESPACE,
                                                            AgentWsDefinitions.AGENT_SERVICE_XML_LOCAL_NAME));
            AgentService agentServicePort = agentService.getPort(new QName(AgentWsDefinitions.AGENT_SERVICE_XML_TARGET_NAMESPACE,
                                                                           AgentWsDefinitions.AGENT_SERVICE_XML_PORT_NAME),
                                                                 AgentService.class);
            Map<String, Object> ctxt = ((BindingProvider) agentServicePort).getRequestContext();

            // setting ENDPOINT ADDRESS, which defines the web service URL for SOAP communication
            // NOTE: if we specify WSDL URL (...<endpoint_address>?wsdl), the JBoss server returns the WSDL on a SOAP call,
            // but we are expecting a SOAP message response and an exception is thrown.
            // The Jetty server (in ATS agents) is working in both cases.
            ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                     protocol + "://" + host + AgentWsDefinitions.AGENT_SERVICE_ENDPOINT_ADDRESS);
            // setting timeouts
            ctxt.put(BindingProviderProperties.CONNECT_TIMEOUT, 10000); // timeout in milliseconds

            uniqueId = ExecutorUtils.getUUID(useNewUuId);

            // add header with unique session ID
            Map<String, List<String>> requestHeaders = new HashMap<>();
            requestHeaders.put(ApplicationContext.ATS_UID_SESSION_TOKEN,
                               Arrays.asList(uniqueId));
            ctxt.put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);

            return agentServicePort;
        } catch (Exception e) {
            throw new AgentException("Cannot connect to Agent application on host '" + host
                                     + "' check your configuration", e);
        }
    }

}
