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

import com.axway.ats.core.atsconfig.AtsInfrastructureManager.ApplicationStatus;
import com.axway.ats.core.atsconfig.exceptions.AtsManagerException;
import com.axway.ats.core.log.AbstractAtsLogger;
import com.axway.ats.core.ssh.JschSshClient;
import com.axway.ats.core.utils.StringUtils;

public abstract class AbstractApplicationController {

    private AbstractAtsLogger         log;

    protected static final String     TOP_LEVEL_ACTION_PREFIX = "***** ";

    protected AbstractApplicationInfo anyApplicationInfo;

    public AbstractApplicationController( AbstractApplicationInfo anyApplicationInfo ) {

        this.log = AbstractAtsLogger.getDefaultInstance(getClass());

        this.anyApplicationInfo = anyApplicationInfo;
    }

    public AbstractApplicationInfo getApplicationInfo() {

        return this.anyApplicationInfo;
    }

    public abstract ApplicationStatus getStatus( JschSshClient sshClient,
                                                 boolean isTopLevelAction ) throws AtsManagerException;

    public abstract ApplicationStatus start( JschSshClient sshClient,
                                             boolean isTopLevelAction ) throws AtsManagerException;

    public abstract ApplicationStatus stop( JschSshClient sshClient,
                                            boolean isTopLevelAction ) throws AtsManagerException;

    public abstract ApplicationStatus restart( JschSshClient sshClient ) throws AtsManagerException;

    public void executeShellCommand( JschSshClient sshClient, AbstractApplicationInfo info,
                                              String command ) {

        if (!info.isUnix()) {
            command = "cmd.exe /c \"" + command + "\"";
        }

        log.info( "Run '" + command + "' on " + info.alias );
        try {
            sshClient.connect( info.systemUser, info.systemPassword, info.host, info.sshPort );
            sshClient.execute( command, false );
        } finally {
            sshClient.disconnect();
        }
    }

    /**
     * Execute post start/stop/install/upgrade shell command, if any
     *
     * @param applicationInfo application information
     * @throws AtsManagerException
     */
    protected void executePostActionShellCommand( JschSshClient parentSshClient,
                                                  AbstractApplicationInfo applicationInfo, String actionName,
                                                  String shellCommand ) throws AtsManagerException {

        if (!StringUtils.isNullOrEmpty(shellCommand)) {

            log.info("Executing post '" + actionName + "' shell command: " + shellCommand);
            JschSshClient sshClient = parentSshClient.newFreshInstance();
            try {
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
            } finally {
                sshClient.disconnect();
            }
        }
    }
}
