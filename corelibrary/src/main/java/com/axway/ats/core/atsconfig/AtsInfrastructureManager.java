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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.atsconfig.exceptions.AtsConfigurationException;
import com.axway.ats.core.atsconfig.exceptions.AtsManagerException;
import com.axway.ats.core.atsconfig.model.AbstractApplicationController;
import com.axway.ats.core.atsconfig.model.AgentController;
import com.axway.ats.core.atsconfig.model.AgentInfo;
import com.axway.ats.core.atsconfig.model.ApplicationController;
import com.axway.ats.core.atsconfig.model.ApplicationInfo;
import com.axway.ats.core.atsconfig.model.PathInfo;
import com.axway.ats.core.log.AbstractAtsLogger;
import com.axway.ats.core.ssh.JschSftpClient;
import com.axway.ats.core.ssh.JschSshClient;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class AtsInfrastructureManager {

	private static AbstractAtsLogger log = AbstractAtsLogger.getDefaultInstance(AtsInfrastructureManager.class);

	private static final String TOP_LEVEL_ACTION_PREFIX = "***** ";

	private AtsProjectConfiguration projectConfiguration;

	private Map<String, String> sshClientConfigurationProperties;

	public enum ApplicationStatus {

		STARTED, STOPPED, NOT_INSTALLED, BUSY, UNKNOWN;
	}

	public AtsInfrastructureManager(String atsConfigurationFile) throws AtsConfigurationException {

		this.projectConfiguration = new AtsProjectConfiguration(atsConfigurationFile);

		this.sshClientConfigurationProperties = new HashMap<>();
	}

	public void reloadConfigurationFile() {

		this.projectConfiguration.loadConfigurationFile();
	}

	/**
	 *
	 * @return the ATS project configuration. Parsed from the configuration file
	 */
	public AtsProjectConfiguration getProjectConfiguration() {

		return this.projectConfiguration;
	}

	/**
	 * Set configuration property
	 * <p/>
	 * Currently we use internally JCraft's JSch library which can be configured
	 * through this method.
	 * <p/>
	 * You need to find the acceptable key-value configuration pairs in the JSch
	 * documentation. They might be also available in the source code of
	 * com.jcraft.jsch.JSch
	 * <p/>
	 * <p>
	 * Example: The default value of "PreferredAuthentications" is
	 * "gssapi-with-mic,publickey,keyboard-interactive,password"
	 * </p>
	 * ATS uses two types of properties to configure the ssh client: <br>
	 * <ul>
	 * <li>global - equivalent to {@link JSch#setConfig(String, String)}, example
	 * <strong>global.RequestTTY</strong>=true</li>
	 * <li>session - equivalent to {@link Session#setConfig(String, String)},
	 * example <strong>session.StrictHostKeyChecking</strong>=no <br>
	 * Note that if there is no global. or session. prefix, the property is assumed
	 * to be a session one</li>
	 * </ul>
	 * <p/>
	 * 
	 * @param key
	 *            configuration key
	 * @param value
	 *            configuration value
	 */
	public void setSshClientConfigurationProperty(String key, String value) {

		sshClientConfigurationProperties.put(key, value);
	}

	/**
	 * Start all the ATS Agents declared in the configuration
	 *
	 * @throws AtsManagerException
	 */
	public void startAllAgents() throws AtsManagerException {

		for (Entry<String, AgentInfo> agentData : projectConfiguration.getAgents().entrySet()) {
			startAnyApplication(agentData.getKey(), true);
		}
	}

	/**
	 * Restart all the ATS Agents declared in the configuration
	 *
	 * @throws AtsManagerException
	 */
	public void restartAllAgents() throws AtsManagerException {

		for (Entry<String, AgentInfo> agentData : projectConfiguration.getAgents().entrySet()) {
			restartAnyApplication(agentData.getKey());
		}
	}

	/**
	 * Stop all the ATS Agents declared in the configuration
	 *
	 * @throws AtsManagerException
	 */
	public void stopAllAgents() throws AtsManagerException {

		for (Entry<String, AgentInfo> agentData : projectConfiguration.getAgents().entrySet()) {
			stopAnyApplication(agentData.getKey(), true);
		}
	}

	public ApplicationStatus startAnyApplication(String anyApplicationAlias, boolean isTopLevelAction)
			throws AtsManagerException {

		AbstractApplicationController controller = getController(anyApplicationAlias, sshClientConfigurationProperties);

		try {
			return controller.start(isTopLevelAction);
		} finally {
			controller.disconnect();
		}
	}

	public ApplicationStatus stopAnyApplication(String anyApplicationAlias, boolean isTopLevelAction)
			throws AtsManagerException {

		AbstractApplicationController controller = getController(anyApplicationAlias, sshClientConfigurationProperties);

		try {
			return controller.stop(isTopLevelAction);
		} finally {
			controller.disconnect();
		}
	}

	public ApplicationStatus restartAnyApplication(String anyApplicationAlias) throws AtsManagerException {

		AbstractApplicationController controller = getController(anyApplicationAlias, sshClientConfigurationProperties);

		try {
			return controller.restart();
		} finally {
			controller.disconnect();
		}
	}

	/**
	 * Get the status of ATS Agent by its alias
	 *
	 * @param agentAlias
	 *            the agent alias declared in the configuration
	 * @return the {@link ApplicationStatus}
	 * @throws AtsManagerException
	 */
	public ApplicationStatus getAnyApplicationStatus(String anyApplicationAlias) throws AtsManagerException {

		AbstractApplicationController controller = getController(anyApplicationAlias, sshClientConfigurationProperties);

		try {
			return controller.getStatus(true);
		} finally {
			controller.disconnect();
		}
	}

	/**
	 * Install the ATS Agent by alias
	 *
	 * @param agentAlias
	 *            the agent alias declared in the configuration
	 * @throws AtsManagerException
	 */
	public ApplicationStatus installAgent(String agentAlias) throws AtsManagerException {

		AgentInfo agentInfo = getAgentInfo(agentAlias);

		log.info(TOP_LEVEL_ACTION_PREFIX + "Now we will try to install " + agentInfo.getDescription());

		String agentZip = projectConfiguration.getSourceProject().getAgentZip();
		if (StringUtils.isNullOrEmpty(agentZip)) {
			throw new AtsManagerException("The agent zip source file is not specified in the configuration");
		}

		// extract agent.zip to a temporary local directory
		String agentFolder = IoUtils.normalizeDirPath(extractAgentZip(agentZip));

		JschSftpClient sftpClient = createNewSftpClient();
		try {
			// upload clean agent
			log.info("Upload clean " + agentInfo.getDescription());
			sftpClient.connect(agentInfo.getSystemUser(), agentInfo.getSystemPassword(), agentInfo.getHost(),
					agentInfo.getSSHPort(), agentInfo.getSSHPrivateKey(), agentInfo.getSSHPrivateKeyPassword());

			if (sftpClient.isRemoteFileOrDirectoryExisting(agentInfo.getSftpHome())) {
				sftpClient.purgeRemoteDirectoryContents(agentInfo.getSftpHome());
			}
			sftpClient.uploadDirectory(agentFolder, agentInfo.getSftpHome(), true);

			// upload custom agent dependencies
			log.info("Upload custom agent dependencies");
			for (PathInfo pathInfo : agentInfo.getPaths()) {
				if (pathInfo.isFile()) {
					if (!sftpClient.isRemoteFileOrDirectoryExisting(pathInfo.getSftpPath())) {

						String fileName = IoUtils.getFileName(pathInfo.getSftpPath());
						String filePath = projectConfiguration.getSourceProject().findFile(fileName);
						if (filePath == null) {
							log.warn("File '" + fileName + "' can't be found in the source project libraries,"
									+ " so it can't be uploaded to " + agentInfo.getDescription());
							continue;
						}

						if (!new File(filePath).exists()) {
							log.warn("Local file '" + filePath + "' does not exist on the local system,"
									+ " so it can't be uploaded to " + agentInfo.getDescription());
							continue;
						}

						int lastSlashIdx = pathInfo.getSftpPath().lastIndexOf('/');
						if (lastSlashIdx > 0) {
							sftpClient.makeRemoteDirectories(pathInfo.getSftpPath().substring(0, lastSlashIdx));
						}

						sftpClient.uploadFile(filePath, pathInfo.getSftpPath());
					}
				} else {
					log.warn("Uploading directories into ATS agent is still not supported");
				}
			}

			// make agent start file to be executable
			makeScriptsExecutable(agentInfo);

			// execute post install shell command, if any
			executePostActionShellCommand(agentInfo, "install", agentInfo.getPostInstallShellCommand());

			log.info(TOP_LEVEL_ACTION_PREFIX + "Successfully installed " + agentInfo.getDescription());
			return ApplicationStatus.STOPPED;
		} finally {

			sftpClient.disconnect();
		}
	}

	/**
	 * Light upgrade the ATS Agent by alias. Which means upgrade of specific
	 * directories only
	 *
	 * @param agentAlias
	 *            the agent alias declared in the configuration
	 * @throws AtsManagerException
	 */
	public ApplicationStatus lightUpgradeAgent(String agentAlias, ApplicationStatus previousStatus)
			throws AtsManagerException {

		// we enter here when the agent is STARTED or STOPPED
		ApplicationStatus newStatus = previousStatus;

		AgentInfo agentInfo = getAgentInfo(agentAlias);

		log.info(TOP_LEVEL_ACTION_PREFIX + "Now we will try to perform light upgrade on " + agentInfo.getDescription());

		JschSftpClient sftpClient = createNewSftpClient();
		try {
			sftpClient.connect(agentInfo.getSystemUser(), agentInfo.getSystemPassword(), agentInfo.getHost(),
					agentInfo.getSSHPort(), agentInfo.getSSHPrivateKey(), agentInfo.getSSHPrivateKeyPassword());

			// Stop the agent if at least one of the file upgrades requires it
			if (newStatus == ApplicationStatus.STARTED) {
				for (PathInfo pathInfo : agentInfo.getPaths()) {

					if (pathInfo.isUpgrade() && mustUpgradeOnStoppedAgent(pathInfo.getSftpPath())) {

						log.info("We must stop the agent prior to upgrading " + pathInfo.getPath());
						try {
							newStatus = stopAnyApplication(agentAlias, false);
							break;
						} catch (AtsManagerException e) {
							throw new AtsManagerException("Canceling upgrade as could not stop the agent", e);
						}
					}
				}
			}

			// Do the actual upgrade
			for (PathInfo pathInfo : agentInfo.getPaths()) {

				if (pathInfo.isUpgrade()) {

					if (pathInfo.isFile()) {

						String fileName = IoUtils.getFileName(pathInfo.getSftpPath());
						String filePath = projectConfiguration.getSourceProject().findFile(fileName);
						if (filePath == null) {
							log.warn("File '" + fileName + "' can not be found in the source project libraries,"
									+ " so we can not upgrade it on the target agent");
							continue;
						}
						// create directories to the file, only if not exist
						int lastSlashIdx = pathInfo.getSftpPath().lastIndexOf('/');
						if (lastSlashIdx > 0) {
							sftpClient.makeRemoteDirectories(pathInfo.getSftpPath().substring(0, lastSlashIdx));
						}
						sftpClient.uploadFile(filePath, pathInfo.getSftpPath());
					} else {
						// TODO: upgrade directory
					}
				}
			}

			// Start the agent if we stopped it
			if (previousStatus == ApplicationStatus.STARTED && newStatus == ApplicationStatus.STOPPED) {

				log.info("We stopped the agent while upgrading. Now we will start it back on");
				newStatus = startAnyApplication(agentAlias, false);

				log.info(TOP_LEVEL_ACTION_PREFIX + agentInfo.getDescription() + " is successfully upgraded");
				return newStatus;
			} else {
				// agent status was not changed in this method

				log.info(TOP_LEVEL_ACTION_PREFIX + agentInfo.getDescription() + " is successfully upgraded");
				return previousStatus;
			}
		} finally {

			sftpClient.disconnect();
		}
	}

	/**
	 * Upgrade the ATS Agent by alias
	 *
	 * @param agentAlias
	 *            the agent alias declared in the configuration
	 * @throws AtsManagerException
	 */
	public ApplicationStatus upgradeAgent(String agentAlias, ApplicationStatus previousStatus)
			throws AtsManagerException {

		// we enter here when the agent is STARTED or STOPPED
		AgentInfo agentInfo = getAgentInfo(agentAlias);

		log.info(TOP_LEVEL_ACTION_PREFIX + "Now we will try to perform full upgrade on " + agentInfo.getDescription());

		String agentZip = projectConfiguration.getSourceProject().getAgentZip();
		if (StringUtils.isNullOrEmpty(agentZip)) {
			throw new AtsManagerException("The agent zip file is not specified in the configuration");
		}

		// extract agent.zip to a temporary local directory
		String agentFolder = IoUtils.normalizeDirPath(extractAgentZip(agentZip));

		JschSftpClient sftpClient = createNewSftpClient();
		try {
			sftpClient.connect(agentInfo.getSystemUser(), agentInfo.getSystemPassword(), agentInfo.getHost(),
					agentInfo.getSSHPort(), agentInfo.getSSHPrivateKey(), agentInfo.getSSHPrivateKeyPassword());

			if (!sftpClient.isRemoteFileOrDirectoryExisting(agentInfo.getSftpHome())) {

				throw new AtsManagerException("The " + agentInfo.getDescription() + " is not installed in "
						+ agentInfo.getSftpHome() + ". You must install it first.");
			}

			if (previousStatus == ApplicationStatus.STARTED) {
				// agent is started, stop it before the upgrade
				log.info("We must stop the agent prior to upgrading");
				try {
					stopAnyApplication(agentAlias, false);
				} catch (AtsManagerException e) {
					throw new AtsManagerException("Canceling upgrade as could not stop the agent", e);
				}
			}

			// cleanup the remote directories content
			List<String> preservedPaths = getPreservedPathsList(agentInfo.getPaths());
			sftpClient.purgeRemoteDirectoryContents(agentInfo.getSftpHome(), preservedPaths);

			agentInfo.markPathsUnchecked();
			updateAgentFolder(sftpClient, agentInfo, agentFolder, "");

			for (PathInfo pathInfo : agentInfo.getUnckeckedPaths()) {

				if (pathInfo.isUpgrade()) {
					if (pathInfo.isFile()) {
						String fileName = IoUtils.getFileName(pathInfo.getSftpPath());
						String filePath = projectConfiguration.getSourceProject().findFile(fileName);
						if (filePath == null) {
							log.warn("File '" + fileName + "' can not be found in the source project libraries,"
									+ " so we can not upgrade it on the target agent");
							continue;
						}
						int lastSlashIdx = pathInfo.getSftpPath().lastIndexOf('/');
						if (lastSlashIdx > 0) {
							sftpClient.makeRemoteDirectories(pathInfo.getSftpPath().substring(0, lastSlashIdx));
						}
						sftpClient.uploadFile(filePath, pathInfo.getSftpPath());
					} else {
						// TODO: upgrade directory
					}
				}
			}

			// make agent start file to be executable
			makeScriptsExecutable(agentInfo);

			// execute post install shell command, if any
			executePostActionShellCommand(agentInfo, "install", agentInfo.getPostInstallShellCommand());

			if (previousStatus == ApplicationStatus.STARTED) {
				log.info("We stopped the agent while upgrading. Now we will start it back on");
				ApplicationStatus newStatus = startAnyApplication(agentAlias, false);

				log.info(TOP_LEVEL_ACTION_PREFIX + agentInfo.getDescription() + " is successfully upgraded");
				return newStatus;
			} else {
				// agent status was not changed in this method

				log.info(TOP_LEVEL_ACTION_PREFIX + agentInfo.getDescription() + " is successfully upgraded");
				return ApplicationStatus.STOPPED;
			}
		} finally {

			sftpClient.disconnect();
		}
	}

	/**
	 * Execute a shell command on the host where the application is located
	 *
	 * @param applicationAlias
	 *            the application alias declared in the configuration
	 * @param command
	 *            the command to run
	 * @return information about the execution result containing exit code, STD OUT
	 *         and STD ERR
	 * @throws AtsManagerException
	 */
	public String executeShellCommand(String anyApplicationAlias, String command) throws AtsManagerException {

		AbstractApplicationController controller = getController(anyApplicationAlias, sshClientConfigurationProperties);
		try {
			return controller.executeShellCommand(controller.getApplicationInfo(), command);
		} finally {
			controller.disconnect();
		}
	}

	/**
	 *
	 * @param agentAlias
	 *            the agent alias declared in the configuration
	 * @return the {@link AgentInfo} instance
	 * @throws AtsManagerException
	 */
	private AgentInfo getAgentInfo(String agentAlias) throws AtsManagerException {

		if (!projectConfiguration.getAgents().containsKey(agentAlias)) {

			throw new AtsManagerException("Can't find agent with alias '" + agentAlias + "' in the configuration");
		}
		return projectConfiguration.getAgents().get(agentAlias);
	}

	private AbstractApplicationController getController(String alias,
			Map<String, String> sshClientConfigurationProperties) throws AtsManagerException {

		AgentInfo agentInfo = projectConfiguration.getAgents().get(alias);
		if (agentInfo != null) {
			return new AgentController(agentInfo, projectConfiguration.getSourceProject(),
					sshClientConfigurationProperties);
		}

		ApplicationInfo applicationInfo = projectConfiguration.getApplications().get(alias);
		if (applicationInfo != null) {
			return new ApplicationController(applicationInfo, sshClientConfigurationProperties);
		}

		throw new AtsManagerException("Can't find application with alias '" + alias + "' in the configuration");
	}

	/**
	 *
	 * @param path
	 *            file or directory path
	 * @return <code>true</code> if must stop the agent
	 */
	private boolean mustUpgradeOnStoppedAgent(String path) {

		return !path.contains("/agentactions/");
	}

	/**
	 * @param pathInfos
	 *            a {@link List} with {@link PathInfo}s
	 * @return a {@link List} with the SFTP full paths as {@link String}s
	 */
	private List<String> getPreservedPathsList(List<PathInfo> pathInfos) {

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
	 * @param sftpClient
	 *            {@link JschSftpClient} instance
	 * @param agentInfo
	 *            the current agent information
	 * @param localAgentFolder
	 *            local agent folder
	 * @param relativeFolderPath
	 *            the relative path of the current folder for update
	 * @throws AtsManagerException
	 */
	private void updateAgentFolder(JschSftpClient sftpClient, AgentInfo agentInfo, String localAgentFolder,
			String relativeFolderPath) throws AtsManagerException {

		String remoteFolderPath = agentInfo.getSftpHome() + relativeFolderPath;
		if (!sftpClient.isRemoteFileOrDirectoryExisting(remoteFolderPath)) {
			sftpClient.uploadDirectory(localAgentFolder + relativeFolderPath, remoteFolderPath, true);
			return;
		}

		File localFolder = new File(localAgentFolder + relativeFolderPath);
		File[] localEntries = localFolder.listFiles();
		if (localEntries != null && localEntries.length > 0) {

			for (File localEntry : localEntries) {

				String remoteFilePath = remoteFolderPath + localEntry.getName();
				PathInfo pathInfo = agentInfo.getPathInfo(remoteFilePath, localEntry.isFile(), true);
				if (pathInfo != null) {

					pathInfo.setChecked(true);
					if (!pathInfo.isUpgrade()) {
						log.info("Skipping upgrade of '" + remoteFilePath + "', because its 'upgrade' flag is 'false'");
						continue;
					}
				}

				if (localEntry.isDirectory()) {
					updateAgentFolder(sftpClient, agentInfo, localAgentFolder,
							relativeFolderPath + localEntry.getName() + "/");
				} else {
					String localFilePath = localAgentFolder + relativeFolderPath + localEntry.getName();
					sftpClient.uploadFile(localFilePath, remoteFilePath);
				}
			}
		}
	}

	/**
	 * Make script files executable
	 *
	 * @param agentInfo
	 *            agent information
	 * @throws AtsManagerException
	 */
	private void makeScriptsExecutable(AgentInfo agentInfo) throws AtsManagerException {

		// set executable privileges to the script files
		if (agentInfo.isUnix()) {

			log.info("Set executable priviledges on all 'sh' files in " + agentInfo.getHome());
			JschSshClient sshClient = createNewSshClient();
			try {
				sshClient.connect(agentInfo.getSystemUser(), agentInfo.getSystemPassword(), agentInfo.getHost(),
						agentInfo.getSSHPort(), agentInfo.getSSHPrivateKey(), agentInfo.getSSHPrivateKeyPassword());

				int exitCode = sshClient.execute("chmod a+x " + agentInfo.getHome() + "/*.sh", true);
				if (exitCode != 0) {
					throw new AtsManagerException("Unable to set execute privileges to the shell script files in '"
							+ agentInfo.getHome() + "'");
				}
			} finally {
				sshClient.disconnect();
			}
		}
	}

	/**
	 * Execute post install/upgrade(full) shell command, if any
	 *
	 * @param agentInfo
	 *            agent information
	 * @throws AtsManagerException
	 */
	private void executePostActionShellCommand(AgentInfo agentInfo, String actionName, String shellCommand)
			throws AtsManagerException {

		if (shellCommand != null) {

			log.info("Executing post '" + actionName + "' shell command: " + shellCommand);
			JschSshClient sshClient = createNewSshClient();
			try {
				sshClient.connect(agentInfo.getSystemUser(), agentInfo.getSystemPassword(), agentInfo.getHost(),
						agentInfo.getSSHPort(), agentInfo.getSSHPrivateKey(), agentInfo.getSSHPrivateKeyPassword());

				int exitCode = sshClient.execute(shellCommand, true);
				if (exitCode != 0) {
					throw new AtsManagerException("Unable to execute the post '" + actionName + "' shell command '"
							+ shellCommand + "' on agent '" + agentInfo.getAlias() + "'. The error output is"
							+ (StringUtils.isNullOrEmpty(sshClient.getErrorOutput()) ? " empty."
									: ":\n" + sshClient.getErrorOutput()));
				}
				log.info("The output of shell command \"" + shellCommand + "\" is"
						+ (StringUtils.isNullOrEmpty(sshClient.getStandardOutput()) ? " empty."
								: ":\n" + sshClient.getStandardOutput()));
			} finally {
				sshClient.disconnect();
			}
		}
	}

	/**
	 * Extract the agent zip file in a temporary directory and return its location
	 *
	 * @param agentZipPath
	 *            agent zip file path
	 * @return the directory where the zip file is unzipped
	 * @throws AtsManagerException
	 */
	private String extractAgentZip(String agentZipPath) throws AtsManagerException {

		// the temp path contains the thread name to have uniqueness when running action
		// on more than
		// one agent at same time
		final String tempPath = IoUtils.normalizeDirPath(AtsSystemProperties.SYSTEM_USER_TEMP_DIR + "/ats_tmp/")
				+ Thread.currentThread().getName() + "_";

		if (agentZipPath.toLowerCase().startsWith("http://")) {
			// download from HTTP URL
			agentZipPath = downloadAgentZip(agentZipPath, tempPath);
		}

		File agentZip = new File(agentZipPath);
		if (!agentZip.exists()) {

			throw new AtsManagerException("The agent ZIP file doesn't exist '" + agentZipPath + "'");
		}
		String agentFolderName = tempPath + "agent_" + String.valueOf(agentZip.lastModified());
		File agentFolder = new File(agentFolderName);
		if (!agentFolder.exists()) {

			try {
				log.info("Unzip " + agentZipPath + " into " + agentFolderName);
				IoUtils.unzip(agentZipPath, agentFolderName, true);
			} catch (IOException ioe) {

				throw new AtsManagerException("Unable to unzip the agent ZIP file '" + agentZipPath
						+ "' to the temporary created directory '" + agentFolderName + "'", ioe);
			}
		}

		return agentFolderName;
	}

	private String downloadAgentZip(String agentZipUrl, String tempPath) throws AtsManagerException {

		File downloadedFile = new File(tempPath + "agent.zip");
		downloadedFile.deleteOnExit();

		log.info("Download ATS agent from " + agentZipUrl + " into " + downloadedFile);

		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			URL url = new URL(agentZipUrl);
			URLConnection conn = url.openConnection();

			bis = new BufferedInputStream(conn.getInputStream());
			bos = new BufferedOutputStream(new FileOutputStream(downloadedFile));
			int inByte;
			while ((inByte = bis.read()) != -1) {
				bos.write(inByte);
			}
			return downloadedFile.getPath();
		} catch (Exception e) {
			throw new AtsManagerException("Error downloading agent ZIP file from " + agentZipUrl, e);
		} finally {
			IoUtils.closeStream(bis);
			IoUtils.closeStream(bos);
		}
	}

	private JschSshClient createNewSshClient() {

		JschSshClient sshClient = new JschSshClient();

		for (Entry<String, String> entry : sshClientConfigurationProperties.entrySet()) {
			sshClient.setConfigurationProperty(entry.getKey(), entry.getValue());
		}

		return sshClient;
	}

	private JschSftpClient createNewSftpClient() {

		JschSftpClient sftpClient = new JschSftpClient();
		for (Entry<String, String> entry : sshClientConfigurationProperties.entrySet()) {
			sftpClient.setConfigurationProperty(entry.getKey(), entry.getValue());
		}

		return sftpClient;
	}
}
