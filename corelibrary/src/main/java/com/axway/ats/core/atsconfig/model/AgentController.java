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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.atsconfig.AtsInfrastructureManager.ApplicationStatus;
import com.axway.ats.core.atsconfig.exceptions.AtsManagerException;
import com.axway.ats.core.log.AbstractAtsLogger;
import com.axway.ats.core.ssh.JschSftpClient;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

public class AgentController extends AbstractApplicationController {

    private static AbstractAtsLogger log = AbstractAtsLogger.getDefaultInstance(AgentController.class);

    private AtsSourceProjectInfo     sourceProjectInfo;
    
    // initial time to wait for the CMD windows to start
	private int 					 initialStartUpLatency = 4;

    public AgentController( AgentInfo agentInfo, AtsSourceProjectInfo sourceProjectInfo,
                            Map<String, String> sshClientConfigurationProperties ) {

        super( agentInfo, sshClientConfigurationProperties );
    }

    @Override
    public ApplicationStatus getStatus( boolean isTopLevelAction ) throws AtsManagerException {

        if (isTopLevelAction) {
            log.info(TOP_LEVEL_ACTION_PREFIX + "Check status of " + anyApplicationInfo.description);
        } else {
            log.debug("Check status of " + anyApplicationInfo.description);
        }

        // check if running and available via WS
        if( isWsdlAvailableNow() ) {

            updateAgentVersion( true );
            return ApplicationStatus.STARTED;
        }

        // not available via WS
        // check if deployed at all
        if( !isAgentInstalled() ) {

            return ApplicationStatus.NOT_INSTALLED;
        }
        updateAgentVersion( false );

        // it is deployed, but not available via WS
        sshClient.connect( anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                           anyApplicationInfo.host, anyApplicationInfo.sshPort,
                           anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword );
        String shellStatusCommand = anyApplicationInfo.getStatusCommand();

        int exitCode = sshClient.execute( shellStatusCommand, true );
        if( exitCode == 0 ) {
            if( sshClient.getStandardOutput().contains( " not running" ) ) {
                log.info( anyApplicationInfo.description + " is not running" );
                return ApplicationStatus.STOPPED;
            } else if( sshClient.getStandardOutput().contains( " is running" ) ) {

                // the agent window is up, maybe it is just starting or
                // there is a some problem starting
                if( isWsdlAvailable() ) {
                    // it got successfully started within the timeout period
                    return ApplicationStatus.STARTED;
                } else {
                    // it will not get started, there is some problem
                    log.warn( anyApplicationInfo.description
                              + " is running, but it is not available for remote connections. You can check the agent's log files for problem details." );
                    return ApplicationStatus.STOPPED;
                }
            }
            throw new AtsManagerException( "Can't parse " + anyApplicationInfo.description
                                           + " status message \"" + sshClient.getStandardOutput() + "\"",
                                           sshClient.getStandardOutput(), sshClient.getErrorOutput() );
        } else {

            throw new AtsManagerException( "Can't get " + anyApplicationInfo.description + " status using "
                                           + ( anyApplicationInfo.isUnix()
                                                                           ? "agent.sh"
                                                                           : "agent.bat" )
                                           + ". The status command exit code is " + exitCode,
                                           sshClient.getStandardOutput(), sshClient.getErrorOutput() );
        }
    }

    @Override
    public ApplicationStatus start( boolean isTopLevelAction ) throws AtsManagerException {

        ApplicationStatus status = getStatus(false);
        if( status != ApplicationStatus.STOPPED ) {
            log.error( ( isTopLevelAction
                                          ? TOP_LEVEL_ACTION_PREFIX
                                          : "" )
                       + "We will not try to start " + anyApplicationInfo.description + " as it is currently "
                       + status.name() );
            return status;
        }

        log.info( ( isTopLevelAction
                                     ? TOP_LEVEL_ACTION_PREFIX
                                     : "" )
                  + "Now we will try to start " + anyApplicationInfo.description );
        sshClient.connect(anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                          anyApplicationInfo.host, anyApplicationInfo.sshPort,
                          anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword);

        String agentStartCommand = anyApplicationInfo.getStartCommand();

        log.info( (isTopLevelAction
                                    ? TOP_LEVEL_ACTION_PREFIX
                                    : "")
                  + anyApplicationInfo.description + " start with: " + agentStartCommand);
        final int startCommandExitCode = sshClient.execute(agentStartCommand, true);
        final String startCommandExecutionResult = sshClient.getLastCommandExecutionResult();
        if (startCommandExitCode == 0 && StringUtils.isNullOrEmpty(sshClient.getErrorOutput())) {

            log.info( (isTopLevelAction
                                        ? TOP_LEVEL_ACTION_PREFIX
                                        : "")
                      + anyApplicationInfo.description
                      + " is probably started, but we will do a quick check");

            if( isWsdlAvailable() ) {
                log.info( (isTopLevelAction
                                            ? TOP_LEVEL_ACTION_PREFIX
                                            : "")
                          + anyApplicationInfo.description + " is successfully started");
                executePostActionShellCommand( anyApplicationInfo, "START",
                                               anyApplicationInfo.getPostStartShellCommand());
                return ApplicationStatus.STARTED;
            }
        }

        throw new AtsManagerException("Can't start " + anyApplicationInfo.description + "\n"
                                      + startCommandExecutionResult
                                      + "\nYou can check the nohup.out file for details");
    }

    @Override
    public ApplicationStatus stop( boolean isTopLevelAction ) throws AtsManagerException {

        ApplicationStatus status = getStatus(false);
        if( status != ApplicationStatus.STARTED ) {
            log.error( ( isTopLevelAction
                                          ? TOP_LEVEL_ACTION_PREFIX
                                          : "" )
                       + "We will not try to stop " + anyApplicationInfo.description + " as it is currently "
                       + status.name() );
            return status;
        }

        log.info( ( isTopLevelAction
                                     ? TOP_LEVEL_ACTION_PREFIX
                                     : "" )
                  + "Now we will try to stop " + anyApplicationInfo.description );
        sshClient.connect(anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                          anyApplicationInfo.host, anyApplicationInfo.sshPort,
                          anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword);
        String agentStopCommand = anyApplicationInfo.getStopCommand();

        log.info(TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description + " stop with: "
                 + agentStopCommand);
        final int stopCommandExitCode = sshClient.execute(agentStopCommand, true);
        final String stopCommandExecutionResult = sshClient.getLastCommandExecutionResult();
        if (stopCommandExitCode == 0 && StringUtils.isNullOrEmpty(sshClient.getErrorOutput())) {

            log.info(anyApplicationInfo.description
                     + " is probably stopped, but we will do a quick check");

            if (getStatus(false) == ApplicationStatus.STOPPED) {
                log.info( (isTopLevelAction
                                            ? TOP_LEVEL_ACTION_PREFIX
                                            : "")
                          + anyApplicationInfo.description + " is successfully stopped");
                executePostActionShellCommand( anyApplicationInfo, "STOP",
                                               anyApplicationInfo.getPostStopShellCommand());
                return ApplicationStatus.STOPPED;
            }
        }

        throw new AtsManagerException("Can't stop " + anyApplicationInfo.description + "\n"
                                      + stopCommandExecutionResult
                                      + "\nYou can check the nohup.out file for details");
    }

    @Override
    public ApplicationStatus restart() throws AtsManagerException {

        ApplicationStatus status = getStatus(false);
        if( status != ApplicationStatus.STOPPED && status != ApplicationStatus.STARTED ) {
            log.error( TOP_LEVEL_ACTION_PREFIX + "We will not try to restart "
                       + anyApplicationInfo.description + " as it is currently " + status.name() );
            return status;
        }

        log.info( TOP_LEVEL_ACTION_PREFIX + "Now we will try to restart " + anyApplicationInfo.description );
        sshClient.connect(anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                          anyApplicationInfo.host, anyApplicationInfo.sshPort,
                          anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword);

        String agentRestartCommand = ((AgentInfo) anyApplicationInfo).getRestartCommand();

        log.info(TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description + " restart with: "
                 + agentRestartCommand);
        final int restartCommandExitCode = sshClient.execute(agentRestartCommand, true);
        final String restartCommandExecutionResult = sshClient.getLastCommandExecutionResult();
        if (restartCommandExitCode == 0 && StringUtils.isNullOrEmpty(sshClient.getErrorOutput())) {

            // Wait for some time to bring up the CMD window
            int startupLatency = anyApplicationInfo.startupLatency;
            if (startupLatency > 0) {
                log.info(TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description + " wait "
                         + initialStartUpLatency + " seconds for the CMD window to show up");
                try {
                    Thread.sleep(initialStartUpLatency * 1000);
                } catch (InterruptedException e) {}
            }

            log.info(TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description
                     + " is probably started, but we will do a quick check");

            if( isWsdlAvailable() ) {
                log.info(TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description
                         + " is successfully restarted");
                executePostActionShellCommand( anyApplicationInfo, "START",
                                               anyApplicationInfo.getPostStartShellCommand());
                return ApplicationStatus.STARTED;
            }
        }

        throw new AtsManagerException("Can't restart " + anyApplicationInfo.description + "\n"
                                      + restartCommandExecutionResult
                                      + "\nYou can check the nohup.out file for details");
    }

    public ApplicationStatus upgrade( ApplicationStatus previousStatus ) throws AtsManagerException {

        // we enter here when the agent is STARTED or STOPPED
        log.info(TOP_LEVEL_ACTION_PREFIX + "Now we will try to perform full upgrade on "
                 + anyApplicationInfo.description);

        String agentZip = sourceProjectInfo.getAgentZip();
        if (StringUtils.isNullOrEmpty(agentZip)) {
            throw new AtsManagerException("The agent zip file is not specified in the configuration");
        }

        // extract agent.zip to a temporary local directory
        String agentFolder = IoUtils.normalizeDirPath(extractAgentZip(agentZip));

        sftpClient.connect(anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                           anyApplicationInfo.host, anyApplicationInfo.sshPort,
                           anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword);

        if (!sftpClient.isRemoteFileOrDirectoryExisting(anyApplicationInfo.sftpHome)) {

            throw new AtsManagerException("The " + anyApplicationInfo.description
                                          + " is not installed in " + anyApplicationInfo.sftpHome
                                          + ". You must install it first.");
        }

        if (previousStatus == ApplicationStatus.STARTED) {
            // agent is started, stop it before the upgrade
            log.info("We must stop the agent prior to upgrading");
            try {
                stop( false );
            } catch (AtsManagerException e) {
                throw new AtsManagerException("Canceling upgrade as could not stop the agent", e);
            }
        }

        // cleanup the remote directories content
        List<String> preservedPaths = getPreservedPathsList(anyApplicationInfo.paths);
        sftpClient.purgeRemoteDirectoryContents(anyApplicationInfo.sftpHome, preservedPaths);

        anyApplicationInfo.markPathsUnchecked();
        updateAgentFolder( sftpClient, agentFolder, "" );

        for (PathInfo pathInfo : anyApplicationInfo.getUnckeckedPaths()) {

            if (pathInfo.isUpgrade()) {
                if (pathInfo.isFile()) {
                    String fileName = IoUtils.getFileName(pathInfo.getSftpPath());
                    String filePath = sourceProjectInfo.findFile(fileName);
                    if (filePath == null) {
                        log.warn("File '" + fileName
                                 + "' can not be found in the source project libraries,"
                                 + " so we can not upgrade it on the target agent");
                        continue;
                    }
                    int lastSlashIdx = pathInfo.getSftpPath().lastIndexOf('/');
                    if (lastSlashIdx > 0) {
                        sftpClient.makeRemoteDirectories(pathInfo.getSftpPath()
                                                                 .substring(0, lastSlashIdx));
                    }
                    sftpClient.uploadFile(filePath, pathInfo.getSftpPath());
                } else {
                    // TODO: upgrade directory
                }
            }
        }

        // make agent start file to be executable
        makeScriptsExecutable();

        // execute post install shell command, if any
        executePostActionShellCommand( anyApplicationInfo, "INSTALL",
                                       anyApplicationInfo.postInstallShellCommand);

        if (previousStatus == ApplicationStatus.STARTED) {
            log.info("We stopped the agent while upgrading. We will start it back on");
            ApplicationStatus newStatus = start( false );

            log.info(TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description
                     + " is successfully upgraded");
            return newStatus;
        } else {
            // agent status was not changed in this method

            log.info(TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description
                     + " is successfully upgraded");
            return ApplicationStatus.STOPPED;
        }
    }

    
	/**
	 * Check whether the Agent WSDL is available/accessible,
	 * but wait for some time if needed
	 *
	 * @return <code>true</code> if the Agent WSDL is available/accessible
	 */
    private boolean isWsdlAvailable() {

        // the startup latency period can be specified by the user
        int startupLatency = anyApplicationInfo.startupLatency;
        if( startupLatency < 1 ) {
            // user did not set a startup latency period, use some default value
            startupLatency = 10;
        }

        log.info( "We will wait for up to " + startupLatency + " seconds for "
                  + anyApplicationInfo.getDescription() + " to get remotely available" );

        long nanoTimeout = TimeUnit.SECONDS.toNanos( startupLatency );
        long startTime = System.nanoTime();
        while( ( System.nanoTime() - startTime ) < nanoTimeout ) {

            if( isWsdlAvailableNow() ) {
                return true;
            }
            try {
                Thread.sleep( 1000 );
            } catch( InterruptedException e ) {}
        }

        return false;
    }
    
    /**
     * Check whether the Agent WSDL is available/accessible at the current moment
     *
     * @return <code>true</code> if the Agent WSDL is available/accessible
     */
    private boolean isWsdlAvailableNow(  ) {

        String urlString = "http://" + anyApplicationInfo.getAddress() + "/agentapp/agentservice?wsdl";
        try {
            URL url = new URL(urlString);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setConnectTimeout(10000);
            httpConnection.setRequestMethod("GET");
            httpConnection.connect();

            if (httpConnection.getResponseCode() == 200) {
                log.info(anyApplicationInfo.getDescription() + " is available for remote connections");
                return true;
            } else {
                log.warn(anyApplicationInfo.getDescription() + " is not available for remote connections. Accessing "
                         + urlString + " returns '" + httpConnection.getResponseCode() + ": "
                         + httpConnection.getResponseMessage() + "'");
                return false;
            }
        } catch (Exception e) {
            log.warn(anyApplicationInfo.getDescription()
                     + " is not available for remote connections. We cannot access " + urlString
                     + ". Error message is '" + e.getMessage() + "'");
            return false;
        }
    }

    /**
     *
     * Update the ATS Agent version in {@link AgentInfo}
     *
     * @param tryOverHttp whether we should try to get the info via HTTP
     * @throws AtsManagerException
     */
    private void updateAgentVersion( boolean tryOverHttp ) throws AtsManagerException {

        if (anyApplicationInfo.getVersion() != null) {
            // we already know the version
            return;
        }

        if( tryOverHttp ) {
            // we will try to obtain the version via HTTP request as it is cheap and fast
            final String urlString = "http://" + anyApplicationInfo.getAddress()
                                     + "/agentapp/restservice/configuration/getAtsVersion";
            HttpURLConnection httpConnection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL( urlString );
                httpConnection = ( HttpURLConnection ) url.openConnection();
                httpConnection.setConnectTimeout( 10000 );
                httpConnection.setRequestMethod( "GET" );
                httpConnection.connect();

                if( httpConnection.getResponseCode() == 200 ) {
                    reader = new BufferedReader( new InputStreamReader( httpConnection.getInputStream() ) );
                    // read the response body
                    // it is expected to be just one line with one token
                    StringBuilder body = new StringBuilder();
                    String line = "";
                    while( ( line = reader.readLine() ) != null ) {
                        body.append( line );
                    }

                    // set the version as we found it
                    if( body.length() > 0 ) {
                        anyApplicationInfo.setVersion( body.toString().trim() );
                        return;
                    }
                }
            } catch( Exception e ) {
                // we got some kind of error over HTTP, so we will try with SSH
            } finally {
                IoUtils.closeStream( reader );
                httpConnection.disconnect();
            }
        }
        
        // we will try to obtain the version via SSH executed command
        sshClient.connect( anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                           anyApplicationInfo.host, anyApplicationInfo.sshPort,
                           anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword );
        String shellVersionCommand = ( ( AgentInfo ) anyApplicationInfo ).getVersionCommand();

        int exitCode = sshClient.execute( shellVersionCommand, true );
        String stdout = sshClient.getStandardOutput();
        int versionKeyIndex = stdout.indexOf( AtsVersion.VERSION_KEY + "=" );
        if( exitCode == 0 && versionKeyIndex > -1 ) {
            anyApplicationInfo.setVersion( stdout.substring( versionKeyIndex
                                                             + ( AtsVersion.VERSION_KEY + "=" ).length() )
                                                 .trim() );
            return;
        }

        // maybe the agent is old one and have no 'version' option
        if( !stdout.toLowerCase().startsWith( "usage" ) ) {
            log.info( "Unable to parse " + anyApplicationInfo.getDescription() + " 'version' output:\n"
                      + stdout + " (stderr: \"" + sshClient.getErrorOutput() + "\")" );
        }
    }

    /**
     * Check whether the agent is installed or not. Currently are just checking
     * whether the agentapp.war file is existing on the right place.
     *
     * @return <code>true</code> if the ATS Agent is installed
     * @throws AtsManagerException
     */
    private boolean isAgentInstalled() throws AtsManagerException {

        sftpClient.connect( anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                            anyApplicationInfo.host, anyApplicationInfo.sshPort,
                            anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword );
        boolean isDeployed = sftpClient.isRemoteFileOrDirectoryExisting( anyApplicationInfo.getSftpHome()
                                                                         + "ats-agent/webapp/agentapp.war" );

        log.info( anyApplicationInfo.getDescription() + " seems " + ( isDeployed
                                                                                 ? ""
                                                                                 : "not" )
                  + " deployed in " + anyApplicationInfo.getSftpHome() );
        return isDeployed;
    }

    /**
     * Make script files executable
     *
     * @throws AtsManagerException
     */
    private void makeScriptsExecutable() throws AtsManagerException {

        // set executable privileges to the script files
        if (anyApplicationInfo.isUnix()) {

            log.info( "Set executable priviledges on all 'sh' files in " + anyApplicationInfo.getHome() );
            sshClient.connect( anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                               anyApplicationInfo.host, anyApplicationInfo.sshPort,
                               anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword );
            int exitCode = sshClient.execute("chmod a+x " + anyApplicationInfo.getHome() + "/*.sh", true);
            if (exitCode != 0) {
                throw new AtsManagerException("Unable to set execute privileges to the shell script files in '"
                                              + anyApplicationInfo.getHome() + "'");
            }
        }
    }

    /**
     *
     * @param pathInfos a {@link List} with {@link PathInfo}s
     * @return a {@link List} with the SFTP full paths as {@link String}s
     */
    private List<String> getPreservedPathsList( List<PathInfo> pathInfos ) {

        List<String> preservedPaths = new ArrayList<String>();
        if (pathInfos != null) {
            for (PathInfo path : pathInfos) {
                preservedPaths.add(path.getSftpPath());
            }
        }
        return preservedPaths;
    }

    /**
     *
     * @param sftpClient {@link JschSftpClient} instance
     * @param localAgentFolder local agent folder
     * @param relativeFolderPath the relative path of the current folder for update
     * @throws AtsManagerException
     */
    private void updateAgentFolder( JschSftpClient sftpClient, String localAgentFolder,
                                    String relativeFolderPath ) throws AtsManagerException {

        String remoteFolderPath = anyApplicationInfo.getSftpHome() + relativeFolderPath;
        if (!sftpClient.isRemoteFileOrDirectoryExisting(remoteFolderPath)) {
            sftpClient.uploadDirectory(localAgentFolder + relativeFolderPath, remoteFolderPath, true);
            return;
        }

        File localFolder = new File(localAgentFolder + relativeFolderPath);
        File[] localEntries = localFolder.listFiles();
        if (localEntries != null && localEntries.length > 0) {

            for (File localEntry : localEntries) {

                String remoteFilePath = remoteFolderPath + localEntry.getName();
                PathInfo pathInfo = anyApplicationInfo.getPathInfo( remoteFilePath, localEntry.isFile(),
                                                                    true );
                if (pathInfo != null) {

                    pathInfo.setChecked(true);
                    if (!pathInfo.isUpgrade()) {
                        log.info("Skipping upgrade of '" + remoteFilePath
                                 + "', because its 'upgrade' flag is 'false'");
                        continue;
                    }
                }

                if (localEntry.isDirectory()) {
                    updateAgentFolder( sftpClient, localAgentFolder,
                                       relativeFolderPath + localEntry.getName() + "/" );
                } else {
                    String localFilePath = localAgentFolder + relativeFolderPath + localEntry.getName();
                    sftpClient.uploadFile(localFilePath, remoteFilePath);
                }
            }
        }
    }

    /**
     * Extract the agent zip file in a temporary directory and return its
     * location
     *
     * @param agentZipPath agent zip file path
     * @return the directory where the zip file is unzipped
     * @throws AtsManagerException
     */
    private String extractAgentZip( String agentZipPath ) throws AtsManagerException {

        File agentZip = new File(agentZipPath);
        if (!agentZip.exists()) {

            throw new AtsManagerException("The agent ZIP file doesn't exist '" + agentZipPath + "'");
        }
        final String tempPath = IoUtils.normalizeDirPath(AtsSystemProperties.SYSTEM_USER_TEMP_DIR
                                                         + "/ats_tmp/");
        String agentFolderName = tempPath + "agent_" + String.valueOf(agentZip.lastModified());
        File agentFolder = new File(agentFolderName);
        if (!agentFolder.exists()) {

            try {
                IoUtils.unzip(agentZipPath, agentFolderName, true);
            } catch (IOException ioe) {

                throw new AtsManagerException("Unable to unzip the agent ZIP file '" + agentZipPath
                                              + "' to the temporary created directory '" + agentFolderName
                                              + "'", ioe);
            }
        }

        return agentFolderName;
    }
}
