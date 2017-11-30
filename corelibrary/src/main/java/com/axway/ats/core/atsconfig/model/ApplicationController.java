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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.axway.ats.core.atsconfig.AtsInfrastructureManager.ApplicationStatus;
import com.axway.ats.core.atsconfig.exceptions.AtsManagerException;
import com.axway.ats.core.log.AbstractAtsLogger;
import com.axway.ats.core.ssh.JschSftpClient;
import com.axway.ats.core.ssh.JschSshClient;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

public class ApplicationController extends AbstractApplicationController {

    private static AbstractAtsLogger log = AbstractAtsLogger.getDefaultInstance(ApplicationController.class);

    public ApplicationController( ApplicationInfo applicationInfo ) {

        super(applicationInfo);
    }

    @Override
    public ApplicationStatus getStatus( JschSshClient sshClient,
                                        boolean isTopLevelAction ) throws AtsManagerException {

        if (isTopLevelAction) {
            log.info(TOP_LEVEL_ACTION_PREFIX + "Check status of " + anyApplicationInfo.description);
        } else {
            log.debug("Check status of " + anyApplicationInfo.description);
        }

        // check if running and available via some URL
        if (isURLAvailable((ApplicationInfo) anyApplicationInfo)) {
            return ApplicationStatus.STARTED;
        }

        // not available via URL
        // check if deployed at all
        if (!isApplicationInstalled((ApplicationInfo) anyApplicationInfo)) {

            return ApplicationStatus.NOT_INSTALLED;
        }

        // it is deployed, but not available via URL
        String statusShellCommand = anyApplicationInfo.getStatusCommand();
        if (statusShellCommand != null) {
            try {
                sshClient.connect(anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                                  anyApplicationInfo.host, anyApplicationInfo.sshPort);

                int exitCode = sshClient.execute(statusShellCommand, true);
                if (exitCode == 0) {

                    String stdoutSearchToken = anyApplicationInfo.getStatusCommandStdOutSearchToken();
                    if (stdoutSearchToken != null) {
                        if (Pattern.compile(stdoutSearchToken, Pattern.DOTALL)
                                   .matcher(sshClient.getStandardOutput())
                                   .matches()) {

                            // the application is running but still not accessible via URL, 
                            // maybe it is just starting or there is some problem starting - so let's wait a little longer
                            if (isURLAvailable((ApplicationInfo) anyApplicationInfo, 60)) {
                                // it got successfully started within the timeout period
                                return ApplicationStatus.STARTED;
                            } else {
                                // it will not get started, there is some problem
                                log.warn(anyApplicationInfo.description
                                         + " is running, but it is not available for remote connections. You can check its configuration and log files for more details.");
                                return ApplicationStatus.STOPPED;
                            }
                        } else {
                            log.warn("Execution of '" + statusShellCommand + "' completed with exit code "
                                     + exitCode + ", but we did not match the expected '" + stdoutSearchToken
                                     + "' in STD OUT");
                            return ApplicationStatus.STOPPED;
                        }
                    } else {
                        // we do not have a way to check the status command output, so we assume it is started
                        return ApplicationStatus.STARTED;
                    }
                } else {
                    log.warn("Execution of '" + statusShellCommand + "' completed with exit code "
                             + exitCode);
                    return ApplicationStatus.STOPPED;
                }
            } finally {

                if (isTopLevelAction) {
                    sshClient.disconnect();
                }
            }
        } else {
            // we do not have a way to check the application status by shell command, so we assume it is stopped
            return ApplicationStatus.STOPPED;
        }
    }

    @Override
    public ApplicationStatus start( boolean isTopLevelAction ) throws AtsManagerException {

        JschSshClient sshClient = new JschSshClient();
        try {
            // it must be stopped, before we try to start it
            ApplicationStatus status = getStatus(sshClient, false);
            if (status != ApplicationStatus.STOPPED) {
                log.error( (isTopLevelAction
                                             ? TOP_LEVEL_ACTION_PREFIX
                                             : "")
                           + "We will not try to start " + anyApplicationInfo.description
                           + " as it is currently " + status.name());
                return status;
            }

            // connect
            String command = anyApplicationInfo.getStartCommand();
            log.info( (isTopLevelAction
                                        ? TOP_LEVEL_ACTION_PREFIX
                                        : "")
                      + "Now we will try to start " + anyApplicationInfo.description + " with: " + command);
            sshClient.connect(anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                              anyApplicationInfo.host, anyApplicationInfo.sshPort);

            // execute shell command
            int commandExitCode = sshClient.execute(command, true);

            // check exit code
            // check if there is something in STD ERR
            boolean isStartedOk = false;
            if (commandExitCode == 0 && StringUtils.isNullOrEmpty(sshClient.getErrorOutput())) {

                String stdoutSearchToken = anyApplicationInfo.getStartCommandStdOutSearchToken();
                if (stdoutSearchToken != null) {
                    // we confirm the success by a token in STD OUT
                    if (Pattern.compile(stdoutSearchToken, Pattern.DOTALL)
                               .matcher(sshClient.getStandardOutput())
                               .matches()) {
                        isStartedOk = true;
                    } else {
                        log.error("Execution of '" + command + "' completed with exit code "
                                  + commandExitCode + " and empty STD ERR, but we did not get the expected '"
                                  + stdoutSearchToken + "' in STD OUT");
                    }
                } else {
                    // we confirm the success by checking the status again
                    if (getStatus(sshClient, false) == ApplicationStatus.STARTED) {
                        isStartedOk = true;
                    }
                }
            }

            if (isStartedOk) {
                log.info( (isTopLevelAction
                                            ? TOP_LEVEL_ACTION_PREFIX
                                            : "")
                          + anyApplicationInfo.description + " is successfully started");

                executePostActionShellCommand(anyApplicationInfo, "START",
                                              anyApplicationInfo.getPostStartShellCommand());
                return ApplicationStatus.STARTED;
            } else {
                throw new AtsManagerException("Can't start " + anyApplicationInfo.description + "\n"
                                              + sshClient.getLastCommandExecutionResult());
            }
        } finally {

            sshClient.disconnect();
        }
    }

    @Override
    public ApplicationStatus stop( boolean isTopLevelAction ) throws AtsManagerException {

        JschSshClient sshClient = new JschSshClient();
        try {
            // it must be started, before we try to stop it
            ApplicationStatus status = getStatus(sshClient, false);
            if (status != ApplicationStatus.STARTED) {
                log.error( (isTopLevelAction
                                             ? TOP_LEVEL_ACTION_PREFIX
                                             : "")
                           + "We will not try to stop " + anyApplicationInfo.description
                           + " as it is currently " + status.name());
                return status;
            }

            // connect
            String command = anyApplicationInfo.getStopCommand();
            log.info( (isTopLevelAction
                                        ? TOP_LEVEL_ACTION_PREFIX
                                        : "")
                      + "Now we will try to stop " + anyApplicationInfo.description + " with: " + command);
            sshClient.connect(anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                              anyApplicationInfo.host, anyApplicationInfo.sshPort);

            // execute shell command
            int commandExitCode = sshClient.execute(command, true);

            // check exit code
            // check if there is something in STD ERR
            boolean isStoppedOk = false;
            if (commandExitCode == 0 && StringUtils.isNullOrEmpty(sshClient.getErrorOutput())) {

                String stdoutSearchToken = anyApplicationInfo.getStopCommandStdOutSearchToken();
                if (stdoutSearchToken != null) {
                    // we confirm the success by a token in STD OUT
                    if (Pattern.compile(stdoutSearchToken, Pattern.DOTALL)
                               .matcher(sshClient.getStandardOutput())
                               .matches()) {
                        isStoppedOk = true;
                    } else {
                        log.error("Execution of '" + command + "' completed with exit code "
                                  + commandExitCode + " and empty STD ERR, but we did not get the expected '"
                                  + stdoutSearchToken + "' in STD OUT");
                    }
                } else {
                    // we confirm the success by checking the status again
                    if (getStatus(sshClient, false) == ApplicationStatus.STOPPED) {
                        isStoppedOk = true;
                    }
                }
            }

            if (isStoppedOk) {
                log.info( (isTopLevelAction
                                            ? TOP_LEVEL_ACTION_PREFIX
                                            : "")
                          + anyApplicationInfo.description + " is successfully stopped");
                executePostActionShellCommand(anyApplicationInfo, "STOP",
                                              anyApplicationInfo.getPostStopShellCommand());
                return ApplicationStatus.STOPPED;
            } else {
                throw new AtsManagerException("Can't stop " + anyApplicationInfo.description + "\n"
                                              + sshClient.getLastCommandExecutionResult());
            }
        } finally {

            sshClient.disconnect();
        }
    }

    @Override
    public ApplicationStatus restart() throws AtsManagerException {

        throw new AtsManagerException("Not implemented");

    }

    private boolean isURLAvailable( ApplicationInfo anyApplicationInfo, int timeout ) {

        log.info("We will wait up to " + timeout + " seconds for " + anyApplicationInfo.description
                 + " to get remotely available");

        long nanoTimeout = TimeUnit.SECONDS.toNanos(timeout);
        long startTime = System.nanoTime();
        while ( (System.nanoTime() - startTime) < nanoTimeout) {

            if (isURLAvailable(anyApplicationInfo)) {
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }

        return false;
    }

    private boolean isURLAvailable( ApplicationInfo anyApplicationInfo ) {

        String urlString = anyApplicationInfo.getStatusCommandUrl();
        try {
            URL url = new URL(urlString);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setConnectTimeout(10000);
            httpConnection.setRequestMethod("GET");
            httpConnection.connect();

            final int responseCode = httpConnection.getResponseCode();
            if (responseCode == 200) {
                // was able to read from the URL
                final String verificationToken = anyApplicationInfo.getStatusCommandUrlSearchToken();
                if (verificationToken != null) {
                    // check if the response contains the expected token
                    final String responseBody = IoUtils.streamToString( ((InputStream) httpConnection.getContent()));
                    log.debug("Get '" + urlString + "' returned:\n" + responseBody);
                    if (!responseBody.contains(verificationToken)) {
                        log.warn(anyApplicationInfo.description
                                 + " is not available for remote connections. Accessing " + urlString
                                 + " returns '" + responseCode + ": " + responseBody + "'");
                        return false;
                    }
                }
                log.info(anyApplicationInfo.description + " is available for remote connections");
                return true;
            } else {
                log.warn(anyApplicationInfo.description
                         + " is not available for remote connections. Accessing " + urlString + " returns '"
                         + responseCode + ": " + httpConnection.getResponseMessage() + "'");
                return false;
            }
        } catch (Exception e) {
            log.warn(anyApplicationInfo.description
                     + " is not available for remote connections. We cannot access " + urlString
                     + ". Error message is '" + e.getMessage() + "'");
            return false;
        }
    }

    private boolean isApplicationInstalled( ApplicationInfo anyApplicationInfo ) {

        JschSftpClient sftpClient = new JschSftpClient();
        try {
            sftpClient.connect(anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                               anyApplicationInfo.host, anyApplicationInfo.sshPort);
            boolean isDeployed = sftpClient.isRemoteFileOrDirectoryExisting(anyApplicationInfo.getSftpHome());

            log.info(anyApplicationInfo.description + " seems " + (isDeployed
                                                                              ? ""
                                                                              : "not ")
                     + "deployed in " + anyApplicationInfo.getSftpHome());
            return isDeployed;
        } finally {

            sftpClient.disconnect();
        }
    }

}
