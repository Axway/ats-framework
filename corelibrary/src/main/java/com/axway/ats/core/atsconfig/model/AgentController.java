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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.atsconfig.AtsInfrastructureManager.ApplicationStatus;
import com.axway.ats.core.atsconfig.exceptions.AtsManagerException;
import com.axway.ats.core.log.AbstractAtsLogger;
import com.axway.ats.core.ssh.JschSftpClient;
import com.axway.ats.core.ssh.JschSshClient;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

public class AgentController extends AbstractApplicationController {

    private static AbstractAtsLogger log = AbstractAtsLogger.getDefaultInstance( AgentController.class );

    private AtsSourceProjectInfo     sourceProjectInfo;

    public AgentController( AgentInfo agentInfo, AtsSourceProjectInfo sourceProjectInfo ) {

        super( agentInfo );
    }

    @Override
    public ApplicationStatus getStatus( JschSshClient sshClient,
                                        boolean isTopLevelAction ) throws AtsManagerException {

        if( isTopLevelAction ) {
            log.info( TOP_LEVEL_ACTION_PREFIX + "Check status of " + anyApplicationInfo.description );
        } else {
            log.debug( "Check status of " + anyApplicationInfo.description );
        }

        // check if running and available via WS
        if( isWsdlAvailable( anyApplicationInfo ) ) {

            updateAgentVersion( anyApplicationInfo, sshClient, false );
            return ApplicationStatus.STARTED;
        }

        // not available via WS
        // check if deployed at all
        if( !isAgentInstalled( anyApplicationInfo ) ) {

            return ApplicationStatus.NOT_INSTALLED;
        }
        updateAgentVersion( anyApplicationInfo, sshClient, false );

        // it is deployed, but not available via WS
        try {
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
                    if( isWsdlAvailable( anyApplicationInfo, 60 ) ) {
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

                throw new AtsManagerException( "Can't get " + anyApplicationInfo.description
                                               + " status using " + ( anyApplicationInfo.isUnix()
                                                                                                  ? "agent.sh"
                                                                                                  : "agent.bat" )
                                               + ". The status command exit code is " + exitCode,
                                               sshClient.getStandardOutput(), sshClient.getErrorOutput() );
            }
        } finally {

            if( isTopLevelAction ) {
                sshClient.disconnect();
            }
        }
    }

    @Override
    public ApplicationStatus start( boolean isTopLevelAction ) throws AtsManagerException {

        JschSshClient sshClient = new JschSshClient();
        try {
            ApplicationStatus status = getStatus( sshClient, false );
            if( status != ApplicationStatus.STOPPED ) {
                log.error( ( isTopLevelAction
                                              ? TOP_LEVEL_ACTION_PREFIX
                                              : "" )
                           + "We will not try to start " + anyApplicationInfo.description
                           + " as it is currently " + status.name() );
                return status;
            }

            log.info( ( isTopLevelAction
                                         ? TOP_LEVEL_ACTION_PREFIX
                                         : "" )
                      + "Now we will try to start " + anyApplicationInfo.description );
            sshClient.connect( anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                               anyApplicationInfo.host, anyApplicationInfo.sshPort,
                               anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword );

            String agentStartCommand = anyApplicationInfo.getStartCommand();

            log.info( ( isTopLevelAction
                                         ? TOP_LEVEL_ACTION_PREFIX
                                         : "" )
                      + anyApplicationInfo.description + " start with: " + agentStartCommand );
            final int startCommandExitCode = sshClient.execute( agentStartCommand, true );
            final String startCommandExecutionResult = sshClient.getLastCommandExecutionResult();
            if( startCommandExitCode == 0 && StringUtils.isNullOrEmpty( sshClient.getErrorOutput() ) ) {

                log.info( ( isTopLevelAction
                                             ? TOP_LEVEL_ACTION_PREFIX
                                             : "" )
                          + anyApplicationInfo.description
                          + " is probably started, but we will do a quick check" );

                boolean isWsdlAvailable = false;
                int startupLatency = anyApplicationInfo.startupLatency;
                if( startupLatency > 0 ) {
                    // some applications do not start quickly and the user can set a static startup latency
                    log.info( TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description + " wait statically "
                              + startupLatency + " seconds for application startup" );
                    try {
                        Thread.sleep( startupLatency * 1000 );
                    } catch( InterruptedException e ) {}
                } else {
                    // The user did not set a static startup latency period, so here we will do it in
                    // a more dynamic way by waiting some time for the WSDL.

                    // If we skip this step, it is possible that we have issued a start command
                    // on the agent, but the PID file is still not present and when
                    // we run getStatus() a little later,  we will still think the agent is not running,
                    // but it just needs some more time
                    isWsdlAvailable = isWsdlAvailable( anyApplicationInfo, 10 );
                }

                if( isWsdlAvailable || getStatus( sshClient, false ) == ApplicationStatus.STARTED ) {
                    log.info( ( isTopLevelAction
                                                 ? TOP_LEVEL_ACTION_PREFIX
                                                 : "" )
                              + anyApplicationInfo.description + " is successfully started" );
                    executePostActionShellCommand( anyApplicationInfo, "START",
                                                   anyApplicationInfo.getPostStartShellCommand() );
                    return ApplicationStatus.STARTED;
                }
            }

            throw new AtsManagerException( "Can't start " + anyApplicationInfo.description + "\n"
                                           + startCommandExecutionResult
                                           + "\nYou can check the nohup.out file for details" );
        } finally {

            sshClient.disconnect();
        }
    }

    @Override
    public ApplicationStatus stop(

                                   boolean isTopLevelAction ) throws AtsManagerException {

        JschSshClient sshClient = new JschSshClient();
        try {
            ApplicationStatus status = getStatus( sshClient, false );
            if( status != ApplicationStatus.STARTED ) {
                log.error( ( isTopLevelAction
                                              ? TOP_LEVEL_ACTION_PREFIX
                                              : "" )
                           + "We will not try to stop " + anyApplicationInfo.description
                           + " as it is currently " + status.name() );
                return status;
            }

            log.info( ( isTopLevelAction
                                         ? TOP_LEVEL_ACTION_PREFIX
                                         : "" )
                      + "Now we will try to stop " + anyApplicationInfo.description );
            sshClient.connect( anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                               anyApplicationInfo.host, anyApplicationInfo.sshPort,
                               anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword );
            String agentStopCommand = anyApplicationInfo.getStopCommand();

            log.info( TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description + " stop with: "
                      + agentStopCommand );
            final int stopCommandExitCode = sshClient.execute( agentStopCommand, true );
            final String stopCommandExecutionResult = sshClient.getLastCommandExecutionResult();
            if( stopCommandExitCode == 0 && StringUtils.isNullOrEmpty( sshClient.getErrorOutput() ) ) {

                log.info( anyApplicationInfo.description
                          + " is probably stopped, but we will do a quick check" );

                if( getStatus( sshClient, false ) == ApplicationStatus.STOPPED ) {
                    log.info( ( isTopLevelAction
                                                 ? TOP_LEVEL_ACTION_PREFIX
                                                 : "" )
                              + anyApplicationInfo.description + " is successfully stopped" );
                    executePostActionShellCommand( anyApplicationInfo, "STOP",
                                                   anyApplicationInfo.getPostStopShellCommand() );
                    return ApplicationStatus.STOPPED;
                }
            }

            throw new AtsManagerException( "Can't stop " + anyApplicationInfo.description + "\n"
                                           + stopCommandExecutionResult
                                           + "\nYou can check the nohup.out file for details" );
        } finally {

            sshClient.disconnect();
        }
    }

    @Override
    public ApplicationStatus restart() throws AtsManagerException {

        JschSshClient sshClient = new JschSshClient();
        try {
            ApplicationStatus status = getStatus( sshClient, false );
            if( status != ApplicationStatus.STOPPED && status != ApplicationStatus.STARTED ) {
                log.error( TOP_LEVEL_ACTION_PREFIX + "We will not try to restart "
                           + anyApplicationInfo.description + " as it is currently " + status.name() );
                return status;
            }

            log.info( TOP_LEVEL_ACTION_PREFIX + "Now we will try to restart "
                      + anyApplicationInfo.description );
            sshClient.connect( anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                               anyApplicationInfo.host, anyApplicationInfo.sshPort,
                               anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword );

            String agentRestartCommand = ( ( AgentInfo ) anyApplicationInfo ).getRestartCommand();

            log.info( TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description + " restart with: "
                      + agentRestartCommand );
            final int restartCommandExitCode = sshClient.execute( agentRestartCommand, true );
            final String restartCommandExecutionResult = sshClient.getLastCommandExecutionResult();
            if( restartCommandExitCode == 0 && StringUtils.isNullOrEmpty( sshClient.getErrorOutput() ) ) {

                if( !anyApplicationInfo.isUnix() ) {
                    // Windows only - wait for some time to bring up the CMD
                    // window
                    int startupLatency = anyApplicationInfo.startupLatency;
                    if( startupLatency > 0 ) {
                        log.info( TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description + " wait "
                                  + startupLatency + " seconds for the CMD window to show up" );
                        try {
                            Thread.sleep( startupLatency * 1000 );
                        } catch( InterruptedException e ) {}
                    }
                }

                log.info( TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description
                          + " is probably started, but we will do a quick check" );

                if( getStatus( sshClient, false ) == ApplicationStatus.STARTED ) {
                    log.info( TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description
                              + " is successfully restarted" );
                    executePostActionShellCommand( anyApplicationInfo, "START",
                                                   anyApplicationInfo.getPostStartShellCommand() );
                    return ApplicationStatus.STARTED;
                }
            }

            throw new AtsManagerException( "Can't restart " + anyApplicationInfo.description + "\n"
                                           + restartCommandExecutionResult
                                           + "\nYou can check the nohup.out file for details" );
        } finally {

            sshClient.disconnect();
        }
    }

    public ApplicationStatus upgrade( ApplicationStatus previousStatus ) throws AtsManagerException {

        // we enter here when the agent is STARTED or STOPPED
        log.info( TOP_LEVEL_ACTION_PREFIX + "Now we will try to perform full upgrade on "
                  + anyApplicationInfo.description );

        String agentZip = sourceProjectInfo.getAgentZip();
        if( StringUtils.isNullOrEmpty( agentZip ) ) {
            throw new AtsManagerException( "The agent zip file is not specified in the configuration" );
        }

        // extract agent.zip to a temporary local directory
        String agentFolder = IoUtils.normalizeDirPath( extractAgentZip( agentZip ) );

        JschSftpClient sftpClient = new JschSftpClient();
        try {
            sftpClient.connect( anyApplicationInfo.systemUser, anyApplicationInfo.systemPassword,
                                anyApplicationInfo.host, anyApplicationInfo.sshPort,
                                anyApplicationInfo.sshPrivateKey, anyApplicationInfo.sshPrivateKeyPassword );

            if( !sftpClient.isRemoteFileOrDirectoryExisting( anyApplicationInfo.sftpHome ) ) {

                throw new AtsManagerException( "The " + anyApplicationInfo.description
                                               + " is not installed in " + anyApplicationInfo.sftpHome
                                               + ". You must install it first." );
            }

            if( previousStatus == ApplicationStatus.STARTED ) {
                // agent is started, stop it before the upgrade
                log.info( "We must stop the agent prior to upgrading" );
                try {
                    stop( false );
                } catch( AtsManagerException e ) {
                    throw new AtsManagerException( "Canceling upgrade as could not stop the agent", e );
                }
            }

            // cleanup the remote directories content
            List<String> preservedPaths = getPreservedPathsList( anyApplicationInfo.paths );
            sftpClient.purgeRemoteDirectoryContents( anyApplicationInfo.sftpHome, preservedPaths );

            anyApplicationInfo.markPathsUnchecked();
            updateAgentFolder( sftpClient, anyApplicationInfo, agentFolder, "" );

            for( PathInfo pathInfo : anyApplicationInfo.getUnckeckedPaths() ) {

                if( pathInfo.isUpgrade() ) {
                    if( pathInfo.isFile() ) {
                        String fileName = IoUtils.getFileName( pathInfo.getSftpPath() );
                        String filePath = sourceProjectInfo.findFile( fileName );
                        if( filePath == null ) {
                            log.warn( "File '" + fileName
                                      + "' can not be found in the source project libraries,"
                                      + " so we can not upgrade it on the target agent" );
                            continue;
                        }
                        int lastSlashIdx = pathInfo.getSftpPath().lastIndexOf( '/' );
                        if( lastSlashIdx > 0 ) {
                            sftpClient.makeRemoteDirectories( pathInfo.getSftpPath()
                                                                      .substring( 0, lastSlashIdx ) );
                        }
                        sftpClient.uploadFile( filePath, pathInfo.getSftpPath() );
                    } else {
                        // TODO: upgrade directory
                    }
                }
            }

            // make agent start file to be executable
            makeScriptsExecutable( anyApplicationInfo );

            // execute post install shell command, if any
            executePostActionShellCommand( anyApplicationInfo, "INSTALL",
                                           anyApplicationInfo.postInstallShellCommand );

            if( previousStatus == ApplicationStatus.STARTED ) {
                log.info( "We stopped the agent while upgrading. We will start it back on" );
                ApplicationStatus newStatus = start( false );

                log.info( TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description
                          + " is successfully upgraded" );
                return newStatus;
            } else {
                // agent status was not changed in this method

                log.info( TOP_LEVEL_ACTION_PREFIX + anyApplicationInfo.description
                          + " is successfully upgraded" );
                return ApplicationStatus.STOPPED;
            }
        } finally {

            sftpClient.disconnect();
        }
    }

    /**
     * Check whether the Agent WSDL is available/accessible
     *
     * @param hostAddress the host address
     * @param timeout the timeout in seconds
     * @return <code>true</code> if the Agent WSDL is available/accessible
     */
    private boolean isWsdlAvailable( AbstractApplicationInfo agentInfo, int timeout ) {

        log.info( "We will wait up to " + timeout + " seconds for " + agentInfo.getDescription()
                  + " to get remotely available" );

        long nanoTimeout = TimeUnit.SECONDS.toNanos( timeout );
        long startTime = System.nanoTime();
        while( ( System.nanoTime() - startTime ) < nanoTimeout ) {

            if( isWsdlAvailable( agentInfo ) ) {
                return true;
            }
            try {
                Thread.sleep( 1000 );
            } catch( InterruptedException e ) {}
        }

        return false;
    }

    /**
     * Check whether the Agent WSDL is available/accessible
     *
     * @param hostAddress the host address
     * @return <code>true</code> if the Agent WSDL is available/accessible
     */
    private boolean isWsdlAvailable( AbstractApplicationInfo agentInfo ) {

        String urlString = "http://" + agentInfo.getAddress() + "/agentapp/agentservice?wsdl";
        try {
            URL url = new URL( urlString );
            HttpURLConnection httpConnection = ( HttpURLConnection ) url.openConnection();
            httpConnection.setConnectTimeout( 10000 );
            httpConnection.setRequestMethod( "GET" );
            httpConnection.connect();

            if( httpConnection.getResponseCode() == 200 ) {
                log.info( agentInfo.getDescription() + " is available for remote connections" );
                return true;
            } else {
                log.warn( agentInfo.getDescription() + " is not available for remote connections. Accessing "
                          + urlString + " returns '" + httpConnection.getResponseCode() + ": "
                          + httpConnection.getResponseMessage() + "'" );
                return false;
            }
        } catch( Exception e ) {
            log.warn( agentInfo.getDescription()
                      + " is not available for remote connections. We cannot access " + urlString
                      + ". Error message is '" + e.getMessage() + "'" );
            return false;
        }
    }

    /**
     *
     * Update the ATS Agent version in {@link AgentInfo}
     *
     * @param agentInfo the agent info declared in the configuration
     * @param sshClient {@link JschSshClient} instance. If it is null, the new one will be created
     * @param isTopLevelAction whether to disconnect the SSH session connection
     * @throws AtsManagerException
     */
    private void updateAgentVersion( AbstractApplicationInfo agentInfo, JschSshClient sshClient,
                                     boolean isTopLevelAction ) throws AtsManagerException {

        if( agentInfo.getVersion() != null ) {
            // we already know the version
            return;
        }

        try {
            sshClient.connect( agentInfo.systemUser, agentInfo.systemPassword, agentInfo.host,
                               agentInfo.sshPort, agentInfo.sshPrivateKey, agentInfo.sshPrivateKeyPassword );
            String shellVersionCommand = ( ( AgentInfo ) agentInfo ).getVersionCommand();

            int exitCode = sshClient.execute( shellVersionCommand, true );
            String stdout = sshClient.getStandardOutput();
            int versionKeyIndex = stdout.indexOf( AtsVersion.VERSION_KEY + "=" );
            if( exitCode == 0 && versionKeyIndex > -1 ) {
                agentInfo.setVersion( stdout.substring( versionKeyIndex
                                                        + ( AtsVersion.VERSION_KEY + "=" ).length() )
                                            .trim() );
                return;
            }

            // maybe the agent is old one and have no 'version' option
            if( !stdout.toLowerCase().startsWith( "usage" ) ) {
                log.info( "Unable to parse " + agentInfo.getDescription() + " 'version' output:\n" + stdout
                          + " (stderr: \"" + sshClient.getErrorOutput() + "\")" );
            }
        } finally {
            if( isTopLevelAction ) {
                sshClient.disconnect();
            }
        }
    }

    /**
     * Check whether the agent is installed or not. Currently are just checking
     * whether the agentapp.war file is existing on the right place.
     *
     * @param agentAlias the agent alias declared in the configuration
     * @return <code>true</code> if the ATS Agent is installed
     * @throws AtsManagerException
     */
    private boolean isAgentInstalled( AbstractApplicationInfo agentInfo ) throws AtsManagerException {

        JschSftpClient sftpClient = new JschSftpClient();
        try {
            sftpClient.connect( agentInfo.systemUser, agentInfo.systemPassword, agentInfo.host,
                                agentInfo.sshPort, agentInfo.sshPrivateKey, agentInfo.sshPrivateKeyPassword );
            boolean isDeployed = sftpClient.isRemoteFileOrDirectoryExisting( agentInfo.getSftpHome()
                                                                             + "ats-agent/webapp/agentapp.war" );

            log.info( agentInfo.getDescription() + " seems " + ( isDeployed
                                                                            ? ""
                                                                            : "not" )
                      + " deployed in " + agentInfo.getSftpHome() );
            return isDeployed;
        } finally {

            sftpClient.disconnect();
        }
    }

    /**
     * Make script files executable
     *
     * @param agentInfo agent information
     * @throws AtsManagerException
     */
    private void makeScriptsExecutable( AbstractApplicationInfo agentInfo ) throws AtsManagerException {

        // set executable privileges to the script files
        if( agentInfo.isUnix() ) {

            log.info( "Set executable priviledges on all 'sh' files in " + agentInfo.getHome() );
            JschSshClient sshClient = new JschSshClient();
            try {
                sshClient.connect( agentInfo.systemUser, agentInfo.systemPassword, agentInfo.host,
                                   agentInfo.sshPort, agentInfo.sshPrivateKey,
                                   agentInfo.sshPrivateKeyPassword );
                int exitCode = sshClient.execute( "chmod a+x " + agentInfo.getHome() + "/*.sh", true );
                if( exitCode != 0 ) {
                    throw new AtsManagerException( "Unable to set execute privileges to the shell script files in '"
                                                   + agentInfo.getHome() + "'" );
                }
            } finally {
                sshClient.disconnect();
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
        if( pathInfos != null ) {
            for( PathInfo path : pathInfos ) {
                preservedPaths.add( path.getSftpPath() );
            }
        }
        return preservedPaths;
    }

    /**
     *
     * @param sftpClient {@link JschSftpClient} instance
     * @param agentInfo the current agent information
     * @param localAgentFolder local agent folder
     * @param relativeFolderPath the relative path of the current folder for update
     * @throws AtsManagerException
     */
    private void updateAgentFolder( JschSftpClient sftpClient, AbstractApplicationInfo agentInfo,
                                    String localAgentFolder,
                                    String relativeFolderPath ) throws AtsManagerException {

        String remoteFolderPath = agentInfo.getSftpHome() + relativeFolderPath;
        if( !sftpClient.isRemoteFileOrDirectoryExisting( remoteFolderPath ) ) {
            sftpClient.uploadDirectory( localAgentFolder + relativeFolderPath, remoteFolderPath, true );
            return;
        }

        File localFolder = new File( localAgentFolder + relativeFolderPath );
        File[] localEntries = localFolder.listFiles();
        if( localEntries != null && localEntries.length > 0 ) {

            for( File localEntry : localEntries ) {

                String remoteFilePath = remoteFolderPath + localEntry.getName();
                PathInfo pathInfo = agentInfo.getPathInfo( remoteFilePath, localEntry.isFile(), true );
                if( pathInfo != null ) {

                    pathInfo.setChecked( true );
                    if( !pathInfo.isUpgrade() ) {
                        log.info( "Skipping upgrade of '" + remoteFilePath
                                  + "', because its 'upgrade' flag is 'false'" );
                        continue;
                    }
                }

                if( localEntry.isDirectory() ) {
                    updateAgentFolder( sftpClient, agentInfo, localAgentFolder,
                                       relativeFolderPath + localEntry.getName() + "/" );
                } else {
                    String localFilePath = localAgentFolder + relativeFolderPath + localEntry.getName();
                    sftpClient.uploadFile( localFilePath, remoteFilePath );
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

        File agentZip = new File( agentZipPath );
        if( !agentZip.exists() ) {

            throw new AtsManagerException( "The agent ZIP file doesn't exist '" + agentZipPath + "'" );
        }
        final String tempPath = IoUtils.normalizeDirPath( AtsSystemProperties.SYSTEM_USER_TEMP_DIR
                                                            + "/ats_tmp/" );
        String agentFolderName = tempPath + "agent_" + String.valueOf( agentZip.lastModified() );
        File agentFolder = new File( agentFolderName );
        if( !agentFolder.exists() ) {

            try {
                IoUtils.unzip( agentZipPath, agentFolderName, true );
            } catch( IOException ioe ) {

                throw new AtsManagerException( "Unable to unzip the agent ZIP file '" + agentZipPath
                                               + "' to the temporary created directory '" + agentFolderName
                                               + "'", ioe );
            }
        }

        return agentFolderName;
    }
}
