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
package com.axway.ats.core.atsconfig;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.axway.ats.core.atsconfig.exceptions.AtsConfigurationException;
import com.axway.ats.core.atsconfig.model.AbstractApplicationInfo;
import com.axway.ats.core.atsconfig.model.AgentInfo;
import com.axway.ats.core.atsconfig.model.ApplicationInfo;
import com.axway.ats.core.atsconfig.model.AtsSourceProjectInfo;
import com.axway.ats.core.atsconfig.model.ShellCommand;
import com.axway.ats.core.utils.XmlUtils;

public class AtsProjectConfiguration {

    private static final String          NODE_ATS_PROJECT                                     = "ats_project";

    private static final String          NODE_SOURCE_PROJECT                                  = "src_project";
    private static final String          NODE_ATS_AGENT                                       = "dst_agent";
    private static final String          NODE_APPLICATION                                     = "dst_application";
    private static final String          NODE_SHELL_COMMANDS                                  = "shell_commands";

    // global values for all agents
    private static final String          NODE_APPLICATION_GLOBAL_PROPERTIES                   = "application_global_properties";
    public static final String           NODE_APPLICATION_GLOBAL_PROPERTY_PORT                = "port";
    public static final String           NODE_APPLICATION_GLOBAL_PROPERTY_UNIX_JAVA_EXEC      = "unix_java_exec";
    public static final String           NODE_APPLICATION_GLOBAL_PROPERTY_WIN_JAVA_EXEC       = "win_java_exec";
    public static final String           NODE_APPLICATION_GLOBAL_PROPERTY_WIN_STARTUP_LATENCY = "win_startup_latency";

    // agent properties, they overwrite the global values
    public static final String           NODE_APPLICATION_PROPERTY_HOST                       = "host";
    public static final String           NODE_APPLICATION_PROPERTY_PORT                       = "port";
    public static final String           NODE_APPLICATION_PROPERTY_SSH_PORT                   = "sshPort";
    public static final String           NODE_APPLICATION_PROPERTY_SYSTEM_USER                = "systemUser";
    public static final String           NODE_APPLICATION_PROPERTY_SYSTEM_PASSWORD            = "systemPassword";
    public static final String           NODE_APPLICATION_PROPERTY_SSH_PRIVATE_KEY            = "sshPrivateKey";
    public static final String           NODE_APPLICATION_PROPERTY_SSH_PRIVATE_KEY_PASSWORD   = "sshPrivateKeyPassword";
    public static final String           NODE_APPLICATION_PROPERTY_JAVA_EXEC                  = "java_exec";
    public static final String           NODE_APPLICATION_PROPERTY_MEMORY                     = "memory";
    public static final String           NODE_APPLICATION_PROPERTY_STARTUP_LATENCY            = "startup_latency";
    public static final String           NODE_APPLICATION_PROPERTY_POST_INSTALL_COMMAND       = "post_install_shell_command";
    public static final String           NODE_APPLICATION_PROPERTY_POST_START_COMMAND         = "post_start_shell_command";
    public static final String           NODE_APPLICATION_PROPERTY_POST_STOP_COMMAND          = "post_stop_shell_command";

    // common attributes
    public static final String           NODE_ATTRIBUTE_ALIAS                                 = "alias";
    public static final String           NODE_ATTRIBUTE_HOME                                  = "home";
    public static final String           NODE_ATTRIBUTE_PATH                                  = "path";
    public static final String           NODE_ATTRIBUTE_FILE                                  = "file";
    public static final String           NODE_ATTRIBUTE_FOLDER                                = "folder";

    private AtsSourceProjectInfo         sourceProject;
    private Map<String, AgentInfo>       agents                                               = new LinkedHashMap<String, AgentInfo>();
    private Map<String, ApplicationInfo> applications                                         = new LinkedHashMap<String, ApplicationInfo>();
    private List<ShellCommand>           shellCommands                                        = new ArrayList<ShellCommand>();

    private String                       atsConfigurationFile;
    private Document                     doc;

    public AtsProjectConfiguration( String atsConfigurationFile ) throws AtsConfigurationException {

        this.atsConfigurationFile = atsConfigurationFile;

        loadConfigurationFile();
    }

    public void loadConfigurationFile() {

        sourceProject = null;
        agents.clear();
        applications.clear();
        shellCommands.clear();

        try {
            doc = DocumentBuilderFactory.newInstance()
                                        .newDocumentBuilder()
                                        .parse(new File(atsConfigurationFile));
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            throw new AtsConfigurationException("Error reading ATS configuration file '"
                                                + atsConfigurationFile + "'", e);
        }

        Element atsProjectNode = doc.getDocumentElement();
        if (!NODE_ATS_PROJECT.equals(atsProjectNode.getNodeName())) {
            throw new AtsConfigurationException("Bad ATS configuration file. Root node name is expected to be '"
                                                + NODE_ATS_PROJECT + "', but it is '"
                                                + atsProjectNode.getNodeName() + "'");
        }

        // project with source libraries
        List<Element> sourceNodes = XmlUtils.getChildrenByTagName(atsProjectNode, NODE_SOURCE_PROJECT);
        if (sourceNodes.size() != 1) {
            throw new AtsConfigurationException("Bad ATS configuration file. We must have exactly 1 "
                                                + NODE_SOURCE_PROJECT + " node, but we got "
                                                + sourceNodes.size());
        }
        sourceProject = new AtsSourceProjectInfo(sourceNodes.get(0));

        // load the default agent values
        final Map<String, String> agentDefaultValues = readAgentGlobalProperties(atsProjectNode);

        // ATS agents
        List<Element> agentNodes = XmlUtils.getChildrenByTagName(atsProjectNode, NODE_ATS_AGENT);
        for (Element agentNode : agentNodes) {
            String agentAlias = XmlUtils.getMandatoryAttribute(agentNode, NODE_ATTRIBUTE_ALIAS);
            if (agents.containsKey(agentAlias)) {
                throw new AtsConfigurationException("'" + agentAlias + "' is not a unique agent alias");
            }
            agents.put(agentAlias, new AgentInfo(agentAlias, agentNode, agentDefaultValues));
        }
        // make sure we do not have same: host + port or host + path
        checkForDuplicatedHostAndPort(agents);
        checkForDuplicatedHostAndHome(agents);

        // Generic applications
        List<Element> applicationNodes = XmlUtils.getChildrenByTagName(atsProjectNode, NODE_APPLICATION);
        for (Element applicationNode : applicationNodes) {
            String applicationAlias = XmlUtils.getMandatoryAttribute(applicationNode, NODE_ATTRIBUTE_ALIAS);
            if (applications.containsKey(applicationAlias)) {
                throw new AtsConfigurationException("'" + applicationAlias
                                                    + "' is not a unique application alias");
            }
            applications.put(applicationAlias,
                             new ApplicationInfo(applicationAlias, applicationNode, agentDefaultValues));
        }
        // make sure we do not have same: host + port or host + path
        checkForDuplicatedHostAndPort(applications);
        checkForDuplicatedHostAndHome(applications);

        // Shell commands
        List<Element> shellCommandsNodes = XmlUtils.getChildrenByTagName(atsProjectNode,
                                                                         NODE_SHELL_COMMANDS);
        if (shellCommandsNodes.size() > 0) {

            // now get the actual commands
            shellCommandsNodes = XmlUtils.getChildrenByTagName(shellCommandsNodes.get(0), "command");
            for (Element commandNode : shellCommandsNodes) {

                String shellCommandAlias = XmlUtils.getAttribute(commandNode, NODE_ATTRIBUTE_ALIAS);
                String theCommand = commandNode.getTextContent();

                shellCommands.add(new ShellCommand(shellCommandAlias, theCommand));
            }
        }
    }

    private void checkForDuplicatedHostAndPort(
                                                Map<String, ? extends AbstractApplicationInfo> anyApplications ) {

        Map<String, String> addresses = new HashMap<String, String>();

        for (AbstractApplicationInfo anyApplicationInfo : anyApplications.values()) {
            String thisAddress = anyApplicationInfo.getAddress(); // host:port
            if (addresses.containsKey(thisAddress)) {
                // we have duplication, generate meaningful user message
                String thatAlias = null;
                for (Entry<String, String> addressEntry : addresses.entrySet()) {
                    if (addressEntry.getKey().equals(thisAddress)) {
                        thatAlias = addressEntry.getValue();
                        break;
                    }
                }

                throw new AtsConfigurationException( (anyApplicationInfo instanceof AgentInfo
                                                                                              ? "Agents"
                                                                                              : "Applications")
                                                     + " with aliases '" + anyApplicationInfo.getAlias()
                                                     + "' and '" + thatAlias
                                                     + "' have same host and port values: " + thisAddress);
            } else {
                addresses.put(thisAddress, anyApplicationInfo.getAlias());
            }
        }
    }

    private void checkForDuplicatedHostAndHome(
                                                Map<String, ? extends AbstractApplicationInfo> anyApplications ) {

        Map<String, String> homes = new HashMap<String, String>();

        for (AbstractApplicationInfo anyApplicationInfo : anyApplications.values()) {
            String thisHome = anyApplicationInfo.getHost() + anyApplicationInfo.getHome(); // host/port
            if (homes.containsKey(thisHome)) {
                // we have duplication, generate meaningful user message
                String thatAlias = null;
                for (Entry<String, String> homeEntry : homes.entrySet()) {
                    if (homeEntry.getKey().equals(thisHome)) {
                        thatAlias = homeEntry.getValue();
                        break;
                    }
                }
                throw new AtsConfigurationException( (anyApplicationInfo instanceof AgentInfo
                                                                                              ? "Agents"
                                                                                              : "Applications")
                                                     + " with aliases '" + anyApplicationInfo.getAlias()
                                                     + "' and '" + thatAlias
                                                     + "' have same host and home folder values: "
                                                     + thisHome);
            } else {
                homes.put(thisHome, anyApplicationInfo.getAlias());
            }
        }
    }

    private Map<String, String> readAgentGlobalProperties(
                                                           Element atsProjectNode ) {

        Map<String, String> agentGlobalProperties = new HashMap<String, String>();

        List<Element> globalPropertiesNodes = XmlUtils.getChildrenByTagName(atsProjectNode,
                                                                            NODE_APPLICATION_GLOBAL_PROPERTIES);
        if (globalPropertiesNodes.size() >= 1) {
            Element globalPropertiesNode = globalPropertiesNodes.get(0);

            // read port
            List<Element> portElements = XmlUtils.getChildrenByTagName(globalPropertiesNode,
                                                                       NODE_APPLICATION_GLOBAL_PROPERTY_PORT);
            if (portElements.size() >= 1) {
                Node portValueNode = portElements.get(0).getFirstChild();
                if (portValueNode != null) {
                    agentGlobalProperties.put(NODE_APPLICATION_GLOBAL_PROPERTY_PORT,
                                              portValueNode.getNodeValue());
                }
            }

            // read Unix java executable location
            List<Element> unixJavaExeElements = XmlUtils.getChildrenByTagName(globalPropertiesNode,
                                                                              NODE_APPLICATION_GLOBAL_PROPERTY_UNIX_JAVA_EXEC);
            if (unixJavaExeElements.size() >= 1) {
                Node unixJavaExeValueNode = unixJavaExeElements.get(0).getFirstChild();
                if (unixJavaExeValueNode != null) {
                    agentGlobalProperties.put(NODE_APPLICATION_GLOBAL_PROPERTY_UNIX_JAVA_EXEC,
                                              unixJavaExeValueNode.getNodeValue());
                }
            }
            // read Windows java executable location
            List<Element> winJavaExeElements = XmlUtils.getChildrenByTagName(globalPropertiesNode,
                                                                             NODE_APPLICATION_GLOBAL_PROPERTY_WIN_JAVA_EXEC);
            if (winJavaExeElements.size() >= 1) {
                Node winJavaExeValueNode = winJavaExeElements.get(0).getFirstChild();
                if (winJavaExeValueNode != null) {
                    agentGlobalProperties.put(NODE_APPLICATION_GLOBAL_PROPERTY_WIN_JAVA_EXEC,
                                              winJavaExeValueNode.getNodeValue());
                }
            }

            // read Windows startup latency
            List<Element> winStartupLatencyElements = XmlUtils.getChildrenByTagName(globalPropertiesNode,
                                                                                    NODE_APPLICATION_GLOBAL_PROPERTY_WIN_STARTUP_LATENCY);
            if (winStartupLatencyElements.size() >= 1) {
                Node winStartupLatencyValueNode = winStartupLatencyElements.get(0).getFirstChild();
                if (winStartupLatencyValueNode != null) {
                    agentGlobalProperties.put(NODE_APPLICATION_GLOBAL_PROPERTY_WIN_STARTUP_LATENCY,
                                              winStartupLatencyValueNode.getNodeValue());
                }
            }
        }

        return agentGlobalProperties;
    }

    public AtsSourceProjectInfo getSourceProject() {

        return this.sourceProject;
    }

    public Map<String, AgentInfo> getAgents() {

        return this.agents;
    }

    public Map<String, ApplicationInfo> getApplications() {

        return this.applications;
    }

    public List<ShellCommand> getShellCommands() {

        return shellCommands;
    }

    public void save() throws AtsConfigurationException {

        // save the XML file
        try {
            OutputFormat format = new OutputFormat(doc);
            format.setIndenting(true);
            format.setIndent(4);
            format.setLineWidth(1000);

            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(new File(atsConfigurationFile)),
                                                         format);
            serializer.serialize(doc);
        } catch (Exception e) {
            throw new AtsConfigurationException("Error saving ATS configuration in '" + atsConfigurationFile
                                                + "'", e);
        }
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("ATS Project Configuration:");

        sb.append("\n");
        sb.append(sourceProject.toString());

        for (AgentInfo agentInfo : agents.values()) {
            sb.append("\n");
            sb.append(agentInfo.toString());
        }
        for (ApplicationInfo applicationInfo : applications.values()) {
            sb.append("\n");
            sb.append(applicationInfo.toString());
        }

        return sb.toString();
    }
}
