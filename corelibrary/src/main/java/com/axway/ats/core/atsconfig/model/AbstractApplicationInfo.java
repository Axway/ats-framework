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
package com.axway.ats.core.atsconfig.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.atsconfig.AtsProjectConfiguration;
import com.axway.ats.core.atsconfig.exceptions.AtsConfigurationException;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.utils.XmlUtils;

public abstract class AbstractApplicationInfo {

    protected String              alias;
    protected String              host;
    protected String              port;
    protected String              version;

    protected String              home;
    protected String              sftpHome;

    protected int                 sshPort                            = -1;
    protected String              systemUser;
    protected String              systemPassword;
    protected String              sshPrivateKey;
    protected String              sshPrivateKeyPassword;

    protected String              javaExecutable;
    protected String              memory;
    protected int                 startupLatency;
    protected String              postInstallShellCommand;
    protected String              postStartShellCommand;
    protected String              postStopShellCommand;

    protected String              address;
    protected String              description;

    protected List<PathInfo>      paths                              = new ArrayList<PathInfo>();

    private Element               applicationNode;

    private boolean               isUnix;
    private boolean               isLocalHost;

    protected static final String NODE_STATUS_COMMAND                = "status_command";
    protected static final String NODE_START_COMMAND                 = "start_command";
    protected static final String NODE_STOP_COMMAND                  = "stop_command";

    protected static final String NODE_ATTRIBUTE_COMMAND             = "command";
    protected static final String NODE_ATTRIBUTE_STDOUT_SEARCH_TOKEN = "stdout_search_token";
    protected static final String NODE_ATTRIBUTE_URL                 = "url";
    protected static final String NODE_ATTRIBUTE_URL_SEARCH_TOKEN    = "url_search_token";

    protected StatusCommandInfo   statusCommandInfo                  = new StatusCommandInfo();
    protected ShellCommandInfo    startCommandInfo                   = new ShellCommandInfo();
    protected ShellCommandInfo    stopCommandInfo                    = new ShellCommandInfo();

    public AbstractApplicationInfo( String alias, Element applicationNode,
                                    Map<String, String> defaultValues ) throws AtsConfigurationException {

        this.applicationNode = applicationNode;

        try {
            // basic application info
            this.alias = alias;
            this.host = XmlUtils.getMandatoryAttribute(applicationNode,
                                                       AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_HOST);
            this.isLocalHost = HostUtils.isLocalHost(host);

            // application port
            this.port = XmlUtils.getAttribute(applicationNode,
                                              AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_PORT); // application port
            if (port == null) {
                port = defaultValues.get(AtsProjectConfiguration.NODE_APPLICATION_GLOBAL_PROPERTY_PORT); // global applications port
                if (port == null) {
                    port = String.valueOf(AtsSystemProperties.DEFAULT_AGENT_PORT_VALUE); // default port
                }
            }

            // set description - often used in messages displayed to user
            address = host;
            if (port.length() > 0) {
                address = address + ":" + port;
            }
            description = (this instanceof AgentInfo
                                                     ? "ATS agent"
                                                     : "Application");

            this.home = XmlUtils.getMandatoryAttribute(applicationNode,
                                                       AtsProjectConfiguration.NODE_ATTRIBUTE_HOME);
            this.isUnix = this.home.charAt(0) == '/';
            if (this.isUnix) {
                sftpHome = IoUtils.normalizeUnixDir(home);
            } else {
                sftpHome = IoUtils.normalizeUnixDir(home.substring(home.indexOf(':') + 1));
            }

            String sshPortString = XmlUtils.getAttribute(applicationNode,
                                                         AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SSH_PORT);
            if (sshPortString != null) {
                try {
                    this.sshPort = Integer.parseInt(sshPortString);
                } catch (NumberFormatException nfe) {
                    throw new AtsConfigurationException("Invalid SSH Port number '" + sshPortString + "'");
                }
            }
            this.systemUser = XmlUtils.getMandatoryAttribute(applicationNode,
                                                             AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SYSTEM_USER);
            this.systemPassword = XmlUtils.getAttribute(applicationNode,
                                                        AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SYSTEM_PASSWORD);
            this.sshPrivateKey = XmlUtils.getAttribute(applicationNode,
                                                       AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SSH_PRIVATE_KEY);
            this.sshPrivateKeyPassword = XmlUtils.getAttribute(applicationNode,
                                                               AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SSH_PRIVATE_KEY_PASSWORD);

            // java executable
            this.javaExecutable = XmlUtils.getAttribute(applicationNode,
                                                        AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_JAVA_EXEC); // application java executable
            if (javaExecutable == null) {
                // global java executable
                if (this.isUnix) {
                    javaExecutable = defaultValues.get(AtsProjectConfiguration.NODE_APPLICATION_GLOBAL_PROPERTY_UNIX_JAVA_EXEC);
                } else {
                    javaExecutable = defaultValues.get(AtsProjectConfiguration.NODE_APPLICATION_GLOBAL_PROPERTY_WIN_JAVA_EXEC);
                }
                if (javaExecutable == null) {
                    javaExecutable = ""; // default value
                }
            }

            // agent memory
            this.memory = XmlUtils.getAttribute(applicationNode,
                                                AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_MEMORY);

            // post 'Install' shell command
            List<Element> postInstallCommandElements = XmlUtils.getChildrenByTagName(applicationNode,
                                                                                     AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_POST_INSTALL_COMMAND);
            if (postInstallCommandElements.size() > 0) {

                this.postInstallShellCommand = postInstallCommandElements.get(0).getTextContent();
                if (this.postInstallShellCommand != null && this.postInstallShellCommand.trim().isEmpty()) {
                    this.postInstallShellCommand = null;
                }
            }

            // post 'Start' shell command
            List<Element> postStartCommandElements = XmlUtils.getChildrenByTagName(applicationNode,
                                                                                   AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_POST_START_COMMAND);
            if (postStartCommandElements.size() > 0) {

                this.postStartShellCommand = postStartCommandElements.get(0).getTextContent();
                if (this.postStartShellCommand != null && this.postStartShellCommand.trim().isEmpty()) {
                    this.postStartShellCommand = null;
                }
            }

            // post 'Stop' shell command
            List<Element> postStopCommandElements = XmlUtils.getChildrenByTagName(applicationNode,
                                                                                  AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_POST_STOP_COMMAND);
            if (postStopCommandElements.size() > 0) {

                this.postStopShellCommand = postStopCommandElements.get(0).getTextContent();
                if (this.postStopShellCommand != null && this.postStopShellCommand.trim().isEmpty()) {
                    this.postStopShellCommand = null;
                }
            }

            // startup latency
            String startupLatencyString = XmlUtils.getAttribute(applicationNode,
                                                                AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_STARTUP_LATENCY);
            if (startupLatencyString == null) {
                // global startup latency
                if (!this.isUnix) {
                    startupLatencyString = defaultValues.get(AtsProjectConfiguration.NODE_APPLICATION_GLOBAL_PROPERTY_WIN_STARTUP_LATENCY);
                }
                if (startupLatencyString == null) {
                    startupLatencyString = "0"; // default value
                }
            }
            try {
                startupLatency = Integer.parseInt(startupLatencyString);
            } catch (NumberFormatException nfe) {
                startupLatency = 0;
            }

            // specific folder destinations
            for (Element folderPathNode : XmlUtils.getChildrenByTagName(applicationNode,
                                                                        AtsProjectConfiguration.NODE_ATTRIBUTE_FOLDER)) {
                this.paths.add(new PathInfo(folderPathNode, false, this.home, sftpHome, isUnix));
            }
            // specific file destinations
            for (Element filePathNode : XmlUtils.getChildrenByTagName(applicationNode,
                                                                      AtsProjectConfiguration.NODE_ATTRIBUTE_FILE)) {
                this.paths.add(new PathInfo(filePathNode, true, this.home, sftpHome, isUnix));
            }

            loadMoreInfo(applicationNode);
        } catch (AtsConfigurationException e) {
            throw new AtsConfigurationException("Error instantiating " + getClass().getSimpleName()
                                                + " with alias '" + alias + "' from XML", e);
        }
    }

    /**
     * Load any specific data for the actual implementation
     * @param applicationNode
     */
    protected abstract void loadMoreInfo( Element applicationNode );

    public String getAlias() {

        return alias;
    }

    public void setAlias( String alias ) {

        this.alias = alias;
    }

    public String getHost() {

        return host;
    }

    public void setHost( String host ) {

        this.host = host;
        XmlUtils.setAttribute(applicationNode, AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_HOST,
                              this.host);
        this.isLocalHost = HostUtils.isLocalHost(host);
    }

    public String getPort() {

        return port;
    }

    public String getPortToken() {

        String portToken = "";
        if (!StringUtils.isNullOrEmpty(port)) {
            portToken = " -port " + port;
        }

        return portToken;
    }

    public void setPort( String port ) {

        this.port = port;
        XmlUtils.setAttribute(applicationNode, AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_PORT,
                              this.port);
    }

    public String getHome() {

        return home;
    }

    public String getSftpHome() {

        return sftpHome;
    }

    public void setHome( String home ) {

        this.home = home;
        XmlUtils.setAttribute(applicationNode, AtsProjectConfiguration.NODE_ATTRIBUTE_HOME, this.home);
    }

    public String getJavaExecutableToken() {

        String javaExeToken = "";
        if (!StringUtils.isNullOrEmpty(javaExecutable)) {
            javaExeToken = " -java_exec \"" + javaExecutable + "\"";
        }

        return javaExeToken;
    }

    public String getMemoryToken() {

        String memoryToken = "";
        if (!StringUtils.isNullOrEmpty(memory)) {
            memoryToken = " -memory " + memory;
        }

        return memoryToken;
    }

    public String getPostInstallShellCommand() {

        return postInstallShellCommand;
    }

    public void setPostInstallShellCommand( String postInstallShellCommand ) {

        this.postInstallShellCommand = postInstallShellCommand;
    }

    public String getPostStartShellCommand() {

        return postStartShellCommand;
    }

    public void setPostStartShellCommand( String postStartShellCommand ) {

        this.postStartShellCommand = postStartShellCommand;
    }

    public String getPostStopShellCommand() {

        return postStopShellCommand;
    }

    public void setPostStopShellCommand( String postStopShellCommand ) {

        this.postStopShellCommand = postStopShellCommand;
    }

    public int getSSHPort() {

        return sshPort;
    }

    public void setSSHPort( String port ) {

        try {
            this.sshPort = Integer.parseInt(port);
        } catch (NumberFormatException nfe) {
            throw new AtsConfigurationException("Invalid SSH Port number '" + port + "'");
        }
        XmlUtils.setAttribute(applicationNode, AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SSH_PORT,
                              port);
    }

    public String getSystemUser() {

        return systemUser;
    }

    public void setSystemUser( String systemUser ) {

        this.systemUser = systemUser;
        XmlUtils.setAttribute(applicationNode, AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SYSTEM_USER,
                              this.systemUser);
    }

    public String getSystemPassword() {

        return systemPassword;
    }

    public void setSystemPassword( String systemPassword ) {

        this.systemPassword = systemPassword;
        XmlUtils.setAttribute(applicationNode,
                              AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SYSTEM_PASSWORD,
                              this.systemPassword);
    }

    public String getSSHPrivateKey() {

        return sshPrivateKey;
    }

    public void setSSHPrivateKey( String sshPrivateKey ) {

        this.sshPrivateKey = sshPrivateKey;
        XmlUtils.setAttribute(applicationNode,
                              AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SSH_PRIVATE_KEY,
                              this.sshPrivateKey);
    }

    public String getSSHPrivateKeyPassword() {

        return sshPrivateKeyPassword;
    }

    public void setSSHPrivateKeyPassword( String sshPrivateKeyPassword ) {

        this.sshPrivateKeyPassword = sshPrivateKeyPassword;
        XmlUtils.setAttribute(applicationNode,
                              AtsProjectConfiguration.NODE_APPLICATION_PROPERTY_SSH_PRIVATE_KEY_PASSWORD,
                              this.sshPrivateKeyPassword);
    }

    public String getDescription() {

        return description;
    }

    public String getAddress() {

        return address;
    }

    public boolean isUnix() {

        return isUnix;
    }

    public void markPathsUnchecked() {

        for (PathInfo pathInfo : paths) {
            pathInfo.setChecked(false);
        }
    }

    public List<PathInfo> getPaths() {

        return paths;
    }

    public List<PathInfo> getUnckeckedPaths() {

        List<PathInfo> uncheckedPaths = new ArrayList<PathInfo>();
        for (PathInfo pathInfo : paths) {
            if (!pathInfo.isChecked()) {
                uncheckedPaths.add(pathInfo);
            }
        }
        return uncheckedPaths;
    }

    public PathInfo getPathInfo(

                                 String absolutePath, boolean isFile, boolean useSftpPath ) {

        for (PathInfo pathInfo : paths) {

            if ( (useSftpPath && normalizePath(isFile, absolutePath).equals(normalizePath(isFile,
                                                                                          pathInfo.getSftpPath())))
                 || (!useSftpPath
                     && normalizePath(isFile,
                                      absolutePath).equals(normalizePath(isFile,
                                                                         pathInfo.getPath())))) {

                return pathInfo;
            }
        }
        return null;
    }

    public boolean isLocalHost() {

        return isLocalHost;
    }

    public String getVersion() {

        return version;
    }

    public void setVersion( String version ) {

        this.version = version;
    }

    public String getStatusCommandUrl() {

        if (StringUtils.isNullOrEmpty(statusCommandInfo.url)) {
            return null;
        } else {
            return statusCommandInfo.url;
        }
    }

    public String getStatusCommandUrlSearchToken() {

        if (StringUtils.isNullOrEmpty(statusCommandInfo.urlSearchToken)) {
            return null;
        } else {
            return statusCommandInfo.urlSearchToken;
        }
    }

    public String getStatusCommand() {

        if (StringUtils.isNullOrEmpty(statusCommandInfo.command)) {
            return null;
        } else {
            return statusCommandInfo.command;
        }
    }

    public String getStatusCommandStdOutSearchToken() {

        if (StringUtils.isNullOrEmpty(statusCommandInfo.stdoutSearchToken)) {
            return null;
        } else {
            return statusCommandInfo.stdoutSearchToken;
        }
    }

    public String getStartCommand() {

        if (StringUtils.isNullOrEmpty(startCommandInfo.command)) {
            return null;
        } else {
            return startCommandInfo.command;
        }
    }

    public String getStartCommandStdOutSearchToken() {

        if (StringUtils.isNullOrEmpty(startCommandInfo.stdoutSearchToken)) {
            return null;
        } else {
            return startCommandInfo.stdoutSearchToken;
        }
    }

    public String getStopCommand() {

        if (StringUtils.isNullOrEmpty(stopCommandInfo.command)) {
            return null;
        } else {
            return stopCommandInfo.command;
        }
    }

    public String getStopCommandStdOutSearchToken() {

        if (StringUtils.isNullOrEmpty(stopCommandInfo.stdoutSearchToken)) {
            return null;
        } else {
            return stopCommandInfo.stdoutSearchToken;
        }
    }

    private String normalizePath( boolean isFile, String path ) {

        if (path == null) {
            return null;
        }

        if (isFile) {
            path = IoUtils.normalizeFilePath(path);
        } else {
            path = IoUtils.normalizeDirPath(path);
        }
        return path.replace("//", "/").replace("\\\\", "\\");
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        if (this instanceof AgentInfo) {
            sb.append("Agent Box '" + alias);
        } else {
            sb.append("Application Box '" + alias);
        }
        sb.append("'\nhost=" + host);
        sb.append("\nport=" + port);
        sb.append("\nhome=" + home);
        if (sshPort > 0) {
            sb.append("\nsshPort=" + sshPort);
        }
        sb.append("\nsystemUser=" + systemUser);
        sb.append("\nsystemPassword=" + systemPassword);
        if (sshPrivateKey != null) {
            sb.append("\nsshPrivateKey=" + sshPrivateKey);
            if (sshPrivateKeyPassword != null) {
                sb.append("\nsshPrivateKeyPassword=" + sshPrivateKeyPassword);
            }
        }

        for (PathInfo path : paths) {
            sb.append("\n " + path);
        }

        return sb.toString();
    }

    class ShellCommandInfo {
        String command;
        String stdoutSearchToken;
    }

    class StatusCommandInfo extends ShellCommandInfo {
        String url;
        String urlSearchToken;
    }
}
