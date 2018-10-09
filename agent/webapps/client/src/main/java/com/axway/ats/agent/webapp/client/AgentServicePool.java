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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;

import org.apache.log4j.Logger;

import com.axway.ats.agent.core.context.ApplicationContext;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.agentservice.AgentWsDefinitions;
import com.axway.ats.agent.webapp.client.configuration.AgentConfigurationLandscape;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.SslUtils;
import com.sun.xml.ws.client.BindingProviderProperties;

public class AgentServicePool {

    private static Logger                 log        = Logger.getLogger(AgentServicePool.class);

    //singleton instance
    private static AgentServicePool       instance;

    //hashmap of all service ports
    private HashMap<String, AgentService> servicePorts;

    // A universe wide ;) unique ID used for maintaining session between Agent and its caller.
    // We use one instance per Test Executor JVM.
    // It is used by the Agent to recognize the caller. 
    private String                        uniqueId;

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
            String protocol = AgentConfigurationLandscape.getInstance( host ).getConnectionProtocol();
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

            // check if new unique id must be generated each time
            if (!useNewUuId) {
                // create temp file containing caller working directory and the unique id
                String userWorkingDirectory = AtsSystemProperties.SYSTEM_USER_HOME_DIR;
                String uuiFileLocation = AtsSystemProperties.SYSTEM_USER_TEMP_DIR
                                         + AtsSystemProperties.SYSTEM_FILE_SEPARATOR + "\\ats_uid.txt";
                File uuiFile = new File(uuiFileLocation);

                // check if the file exist and if exist check if the data we need is in, 
                // otherwise add it to the file 
                if (uuiFile.exists()) {
                    String uuiFileContent = IoUtils.streamToString(IoUtils.readFile(uuiFileLocation));
                    if (uuiFileContent.contains(userWorkingDirectory)) {
                        for (String line : uuiFileContent.split("\n")) {
                            if (line.contains(userWorkingDirectory)) {
                                uniqueId = line.substring(userWorkingDirectory.length()).trim();
                            }
                        }
                    } else {
                        generateNewUUID();
                        new LocalFileSystemOperations().appendToFile(uuiFileLocation,
                                                                     userWorkingDirectory + "\t" + uniqueId + "\n");
                    }
                } else {
                    generateNewUUID();
                    try {
                        uuiFile.createNewFile();
                    } catch (IOException e) {
                        log.warn("Unable to create file '" + uuiFile.getAbsolutePath() + "'");
                    }
                    if (uuiFile.exists()) {
                        new LocalFileSystemOperations().appendToFile(uuiFileLocation,
                                                                     userWorkingDirectory + "\t" + uniqueId + "\n");
                    }
                }
            } else {
                generateNewUUID();
            }

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

    private void generateNewUUID() {

        uniqueId = UUID.randomUUID().toString().trim();
    }
}
