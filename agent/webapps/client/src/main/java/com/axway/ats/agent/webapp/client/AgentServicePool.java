/*
 * Copyright 2017-2022 Axway Software
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

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.agentservice.AgentWsDefinitions;
import com.axway.ats.agent.webapp.client.configuration.AgentConfigurationLandscape;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.core.utils.SslUtils;
import com.sun.xml.ws.client.BindingProviderProperties;

public class AgentServicePool {

    //singleton instance
    private static AgentServicePool       instance;

    // map of all service ports
    private HashMap<String, AgentService> servicePorts;

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

    /**
     * Returns client which is concerned only about Agent address
     * without worry about the caller thread.
     *
     * Used when simply running some sort of action on the Agent side.
     *
     * @param agentHost
     * @param testcaseSessionId
     * @return
     * @throws AgentException
     */
    public AgentService getClientForHostAndTestcase( String agentHost,
                                                     String testcaseSessionId ) throws AgentException {

        // It is assumed that the ATS Agent address here comes with IP and PORT
        AgentService servicePort = servicePorts.get( testcaseSessionId );
        if( servicePort == null ) {
            servicePort = createServicePort( agentHost );
            servicePorts.put( testcaseSessionId, servicePort );
        }

        return servicePort;
    }

   /*
    public static void useCachedUniqueId() {

        useNewUuId = false;
    }
    */

    /**
     * Returns client which is concerned about Agent address and the caller thread.
     *
     * Used when controlling the logging on the Agent side.
     *
     * @param atsAgent
     * @return
     * @throws AgentException
     */
    public AgentService getClientForHost( String atsAgent ) throws AgentException {

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
                // TODO - Do not reset this every time
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

            // add headers to allow the Agent distinguish this call among calls from:
            //  - different Executors
            //  - different threads from same Executor(when have parallel tests)
            Map<String, List<String>> headers = new HashMap<>();
            // this header tells the exact Executor
            headers.put(ExecutorUtils.ATS_CALLER_ID,
                        Arrays.asList(com.axway.ats.core.utils.ExecutorUtils.createCallerId()));
            // this header tells the exact thread
            headers.put( com.axway.ats.core.utils.ExecutorUtils.ATS_THREAD_ID, Arrays.asList(Long.toString(Thread.currentThread().getId()) ) );
            ctxt.put( MessageContext.HTTP_REQUEST_HEADERS, headers );

            return agentServicePort;
        } catch (Exception e) {
            throw new AgentException("Cannot connect to Agent application on host '" + host
                                     + "' check your configuration", e);
        }
    }

}
