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

import java.util.Map;
import java.util.Map.Entry;

import com.axway.ats.core.atsconfig.AtsInfrastructureManager.ApplicationStatus;
import com.axway.ats.core.atsconfig.exceptions.AtsManagerException;
import com.axway.ats.core.log.AbstractAtsLogger;
import com.axway.ats.core.ssh.JschSftpClient;
import com.axway.ats.core.ssh.JschSshClient;
import com.axway.ats.core.utils.StringUtils;

public abstract class AbstractApplicationController {

    private AbstractAtsLogger         log;

    protected static final String     TOP_LEVEL_ACTION_PREFIX = "***** ";

    protected AbstractApplicationInfo anyApplicationInfo;
    
    protected JschSshClient           sshClient;
    protected JschSftpClient          sftpClient;

    public AbstractApplicationController( AbstractApplicationInfo anyApplicationInfo,
                                          Map<String, String> sshClientConfigurationProperties ) {

        this.log = AbstractAtsLogger.getDefaultInstance(getClass());

        this.anyApplicationInfo = anyApplicationInfo;
        
        this.sshClient = new JschSshClient();
        for( Entry<String, String> entry : sshClientConfigurationProperties.entrySet() ) {
            this.sshClient.setConfigurationProperty( entry.getKey(), entry.getValue() );
        }

        this.sftpClient = new JschSftpClient();
        for( Entry<String, String> entry : sshClientConfigurationProperties.entrySet() ) {
            this.sftpClient.setConfigurationProperty( entry.getKey(), entry.getValue() );
        }
    }
    
    public AbstractApplicationInfo getApplicationInfo() {

        return this.anyApplicationInfo;
    }

    public abstract ApplicationStatus getStatus( boolean isTopLevelAction ) throws AtsManagerException;

    public abstract ApplicationStatus start( boolean isTopLevelAction ) throws AtsManagerException;

    public abstract ApplicationStatus stop( boolean isTopLevelAction ) throws AtsManagerException;

    public abstract ApplicationStatus restart() throws AtsManagerException;

    /**
     * Execute a shell command on the host where the application is located
     *
     * @param info about the agent/application to run command from
     * @param command the command to run
     * @return information about the execution result containing exit code, STD OUT and STD ERR
     */
    public String executeShellCommand( AbstractApplicationInfo info, String command ) {

        if (!info.isUnix()) {
            command = "cmd.exe /c \"" + command + "\"";
        }

        log.info( "Run '" + command + "' on " + info.alias );
        sshClient.connect( info.systemUser, info.systemPassword, info.host, info.sshPort );
        sshClient.execute( command, false );
        
        return sshClient.getLastCommandExecutionResult();
    }

    /**
     * Execute post start/stop/install/upgrade shell command, if any
     *
     * @param applicationInfo application information
     * @throws AtsManagerException
     */
    protected void executePostActionShellCommand( AbstractApplicationInfo applicationInfo, String actionName,
                                                  String shellCommand ) throws AtsManagerException {

        if (!StringUtils.isNullOrEmpty(shellCommand)) {

            log.info("Executing post '" + actionName + "' shell command: " + shellCommand);
            sshClient.connect(applicationInfo.systemUser, applicationInfo.systemPassword,
                              applicationInfo.host, applicationInfo.sshPort);
            int exitCode = sshClient.execute(shellCommand, true);
            if (exitCode != 0) {
                throw new AtsManagerException("Unable to execute the post '" + actionName
                                              + "' shell command '" + shellCommand + "' on application '"
                                              + applicationInfo.getAlias() + "'. The error output is"
                                              + (StringUtils.isNullOrEmpty(sshClient.getErrorOutput())
                                                                                                       ? " empty."
                                                                                                       : ":\n"
                                                                                                         + sshClient.getErrorOutput()));
            }
            log.info("The output of shell command \"" + shellCommand + "\" is"
                     + (StringUtils.isNullOrEmpty(sshClient.getStandardOutput())
                                                                                 ? " empty."
                                                                                 : ":\n"
                                                                                   + sshClient.getStandardOutput()));
        }
    }

    public void disconnect() {

        sshClient.disconnect();
        sftpClient.disconnect();
    }
}
