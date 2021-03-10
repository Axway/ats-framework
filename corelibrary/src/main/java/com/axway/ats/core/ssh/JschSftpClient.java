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
package com.axway.ats.core.ssh;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.log.AbstractAtsLogger;
import com.axway.ats.core.ssh.exceptions.JschSftpClientException;
import com.axway.ats.core.ssh.model.FileEntry;
import com.axway.ats.core.utils.IoUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class JschSftpClient {

    // this variable enables the logging
    private static boolean           isVerbose          = AtsSystemProperties
                                                                             .getPropertyAsBoolean(AtsSystemProperties.CORE__JSCH_VERBOSE_MODE,
                                                                                                   false);
    /*
     * If called from ATS plugin, it will log into plugin console, otherwise will
     * log into log4j2
     */
    private static AbstractAtsLogger log                = AbstractAtsLogger.getDefaultInstance(JschSftpClient.class);

    private static final int         CONNECTION_TIMEOUT = 20000;

    private String                   user;
    private String                   host;
    private int                      port               = -1;

    private Session                  session;
    private ChannelSftp              channel;

    // some optional configuration properties
    private Map<String, String>      configurationProperties;

    public JschSftpClient() {

        this.configurationProperties = new HashMap<>();

        // by default - skip checking of known hosts and verifying RSA keys
        this.configurationProperties.put("StrictHostKeyChecking", "no");
    }

    /**
     * Enable/disable DEBUG level log messages
     *
     * @param isVerbose
     */
    public static void setVerboseMode( boolean isVerbose ) {

        JschSftpClient.isVerbose = isVerbose;
    }

    /**
     * Create SFTP session connection
     *
     * @param user the user name
     * @param password the user password
     * @param host the target host
     */
    public void connect( String user, String password, String host ) {

        connect(user, password, host, -1);
    }

    /**
     * Create SFTP session connection
     *
     * @param user the user name
     * @param password the user password
     * @param host the target host
     * @param port the specific port to use
     */
    public void connect( String user, String password, String host, int port ) {

        connect(user, password, host, port, null, null);
    }

    /**
     * Create SFTP session connection
     *
     * @param user the user name
     * @param password the user password
     * @param host the target host
     * @param port the specific port to use
     * @param privateKey private key location. For example: ~/.ssh/id_rsa
     * @param privateKeyPassword private key passphrase (or null if it hasn't)
     */
    public void connect( String user, String password, String host, int port, String privateKey,
                         String privateKeyPassword ) {

        try {
            // disconnect if needed or stay connected if the host is the same
            if (this.session != null && this.session.isConnected()) {

                if (this.host.equals(host) && this.user.equals(user) && this.port == port) {

                    return; // already connected
                } else {
                    disconnect();
                }
            }

            this.user = user;
            this.host = host;
            this.port = port;

            JSch jsch = new JSch();
            jsch.setConfigRepository(new JschConfigRepository(this.host, this.user, this.port,
                                                              this.configurationProperties));
            for (Entry<String, String> entry : configurationProperties.entrySet()) {
                if (entry.getKey().startsWith("global.")) {
                    JSch.setConfig(entry.getKey().split("\\.")[1], entry.getValue());
                }
            }
            if (privateKey != null) {
                jsch.addIdentity(privateKey, privateKeyPassword);
            }
            if (port > 0) {
                this.session = jsch.getSession(user, host, port);
            } else {
                this.session = jsch.getSession(user, host);
            }
            this.session.setPassword(password);

            // apply any configuration properties
            for (Entry<String, String> entry : configurationProperties.entrySet()) {
                if (entry.getKey().startsWith("session.")) {
                    session.setConfig(entry.getKey().split("\\.")[1], entry.getValue());
                } else if (!entry.getKey().startsWith("global.")) { // by default if global or session prefix is
                                                                    // missing, we assume it is a session property
                    session.setConfig(entry.getKey(), entry.getValue());
                }
            }

            this.session.connect(CONNECTION_TIMEOUT);

            this.channel = (ChannelSftp) this.session.openChannel("sftp");
            this.channel.connect(); // there is a bug in the other method channel.connect( TIMEOUT );

        } catch (Exception e) {

            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     * Disconnect the STFP session connection
     */
    public void disconnect() {

        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
    }

    /**
     * Pass configuration property for the internally used SSH client library
     */
    public void setConfigurationProperty( String key, String value ) {

        configurationProperties.put(key, value);
    }

    /**
     *
     * @param fileOrDirPath file or directory path
     * @return <code>true</code> if the remote file or directory exists
     */
    public boolean isRemoteFileOrDirectoryExisting( String fileOrDirPath ) {

        try {
            channel.ls(fileOrDirPath);
            return true;

        } catch (Exception e) {

            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("No such file")) {
                return false;
            }

            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     *
     * @param directoryPath directory path
     * @return <code>true</code> if the directory is empty
     */
    public boolean isDirectoryEmpty( String directoryPath ) {

        try {
            List<LsEntry> entries = new ArrayList<LsEntry>();
            channel.ls(directoryPath, new LsEntrySelector(entries));
            return entries.size() == 0;

        } catch (Exception e) {

            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     *
     * @param directoryPath directory path
     * @return {@link List} of {@link FileEntry} objects corresponding with the target directory file and folder
     *         entries.
     */
    public List<FileEntry> ls( String directoryPath ) {

        try {
            List<LsEntry> entries = new ArrayList<LsEntry>();
            channel.ls(directoryPath, new LsEntrySelector(entries));
            return getFileEntries(entries, directoryPath);

        } catch (Exception e) {

            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     * Upload a file to the remote file system
     *
     * @param sourceFile source/local file path
     * @param destFile destination/remote file path
     */
    public void uploadFile( String sourceFile, String destFile ) {

        if (isVerbose) {
            log.debug("Upload file " + sourceFile + " to " + destFile);
        }

        try {
            channel.put(sourceFile, destFile);

        } catch (Exception e) {
            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     * Download a remote file to the local file system
     *
     * @param destFile destination/remote file path
     * @param sourceFile source/local file path
     */
    public void downloadFile( String destFile, String sourceFile ) {

        if (isVerbose) {
            log.debug("Download file " + sourceFile + " to " + destFile);
        }

        try {
            channel.get(destFile, sourceFile);

        } catch (Exception e) {
            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     * Upload local directory to a the remote file system
     *
     * @param sourceDir source directory path
     * @param destDir destination directory path
     */
    public void uploadDirectory( String sourceDir, String destDir, boolean createParentDirs ) {

        if (isVerbose) {
            log.debug("Upload directory " + sourceDir + " to " + destDir);
        }

        destDir = IoUtils.normalizeUnixDir(destDir);

        File srcDir = new File(sourceDir);
        if (!srcDir.isDirectory()) {

            throw new JschSftpClientException(
                                              "The local directory '" + sourceDir
                                              + "' is not a Directory or doesn't exist");
        }

        if (createParentDirs) {
            makeRemoteDirectories(destDir);
        } else {
            makeRemoteDirectory(destDir);
        }
        try {
            File[] files = srcDir.listFiles();
            if (files != null) {
                for (File file : files) {

                    String remoteFilePath = destDir + file.getName();
                    if (file.isDirectory()) {
                        uploadDirectory(file.getCanonicalPath(), remoteFilePath, false);
                    } else {
                        uploadFile(file.getCanonicalPath(), remoteFilePath);
                    }
                }
            }
        } catch (Exception e) {

            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     * Returns the absolute path of the remote home directory
     *
     * @return the absolute path of the remote home directory
     */
    public String getRemoteHome() {

        try {
            return channel.getHome();

        } catch (Exception e) {
            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     * Create remote directory. One level only
     *
     * @param dirPath the directory path
     */
    public void makeRemoteDirectory( String dirPath ) {

        if (isVerbose) {
            log.debug("Create remote directory " + dirPath);
        }

        try {
            if (!isRemoteFileOrDirectoryExisting(dirPath)) {
                channel.mkdir(dirPath);
            }

        } catch (Exception e) {
            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     * Create remote directories
     *
     * @param dirPath the directory path
     */
    public void makeRemoteDirectories( String dirPath ) {

        if (isVerbose) {
            log.debug("Create remote directory " + dirPath);
        }

        try {
            if (!isRemoteFileOrDirectoryExisting(dirPath)) {

                dirPath = IoUtils.normalizeUnixDir(dirPath);
                int dirSeparatorIndex = 0;
                while ( (dirSeparatorIndex = dirPath.indexOf('/', dirSeparatorIndex + 1)) > 0) {

                    String currentDirPath = dirPath.substring(0, dirSeparatorIndex);
                    if (!isRemoteFileOrDirectoryExisting(currentDirPath)) {

                        channel.mkdir(currentDirPath);
                    }
                }
            }

        } catch (Exception e) {
            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     * Removes a remote directory
     *
     * @param directoryPath the directory path
     * @param isRecursive whether to delete directory entries recursively
     */
    public void removeRemoteDirectory( String directoryPath, boolean isRecursive ) {

        if (isVerbose) {
            log.debug("Delete remote directory " + directoryPath + (isRecursive
                                                                                ? " recursively"
                                                                                : ""));
        }

        try {
            if (isRemoteFileOrDirectoryExisting(directoryPath)) {

                if (isRecursive) {
                    List<FileEntry> entries = ls(directoryPath);
                    for (FileEntry fileEntry : entries) {

                        if (fileEntry.isDirectory()) {
                            removeRemoteDirectory(fileEntry.getPath(), isRecursive);
                        } else {
                            removeRemoteFile(fileEntry.getPath());
                        }
                    }
                }

                channel.rmdir(directoryPath);
            }

        } catch (Exception e) {

            throw new JschSftpClientException(
                                              "Error while deleting directory '" + directoryPath + "'" + (isRecursive
                                                                                                                      ? " recursively"
                                                                                                                      : ""),
                                              e);
        }
    }

    /**
     * Remove remote file
     *
     * @param filePath remote file path
     */
    public void removeRemoteFile( String filePath ) {

        if (isVerbose) {
            log.debug("Delete remote file " + filePath);
        }

        try {
            channel.rm(filePath);

        } catch (Exception e) {
            throw new JschSftpClientException(e.getMessage(), e);
        }
    }

    /**
     * Purge remote directory contents (internal files and directories recursively)
     *
     * @param directoryPath the directory path
     */
    public void purgeRemoteDirectoryContents( String directoryPath ) {

        purgeRemoteDirectoryContents(directoryPath, null);
    }

    /**
     *
     * @param directoryPath the directory path
     * @param preservedPaths a list with preserved files or folders. If the path ends with '/' it is preserved
     *            directory, not a file
     */
    public void purgeRemoteDirectoryContents( String directoryPath, List<String> preservedPaths ) {

        if (isVerbose) {
            log.debug("Cleanup the content of remote directory " + directoryPath);
        }

        try {
            if (isRemoteFileOrDirectoryExisting(directoryPath)) {

                List<FileEntry> entries = ls(directoryPath);
                for (FileEntry fileEntry : entries) {

                    if (fileEntry.isDirectory()) {

                        if (preservedPaths == null || (preservedPaths != null
                                                       && !preservedPaths.contains(IoUtils.normalizeUnixDir(fileEntry.getPath())))) { // skip
                                                                                                                                                                                            // preserved
                                                                                                                                                                                            // directories

                            purgeRemoteDirectoryContents(fileEntry.getPath(), preservedPaths);
                            // the directory may not be empty now, if there are some preserved files/folders
                            // in it
                            if (preservedPaths == null
                                || (preservedPaths != null && isDirectoryEmpty(fileEntry.getPath()))) {

                                removeRemoteDirectory(fileEntry.getPath(), false);
                            }
                        }
                    } else if (preservedPaths == null
                               || (preservedPaths != null && !preservedPaths.contains(fileEntry.getPath()))) { // skip
                                                                                                                                                 // preserved
                                                                                                                                                 // files

                        removeRemoteFile(fileEntry.getPath());
                    }
                }
            }

        } catch (Exception e) {

            throw new JschSftpClientException("Error while purging directory contnets '" + directoryPath + "'", e);
        }
    }

    private List<FileEntry> getFileEntries( List<LsEntry> lsEntries, String parentPath ) {

        List<FileEntry> fileEntries = new ArrayList<FileEntry>();
        for (LsEntry lsEntry : lsEntries) {
            fileEntries.add(getFileEntry(lsEntry, parentPath));
        }
        return fileEntries;
    }

    private FileEntry getFileEntry( LsEntry lsEntry, String parentPath ) {

        FileEntry fileEntry = new FileEntry(lsEntry.getFilename(),
                                            IoUtils.normalizeUnixDir(parentPath) + lsEntry.getFilename(),
                                            lsEntry.getAttrs().isDir());
        fileEntry.setSize(lsEntry.getAttrs().getSize());
        fileEntry.setParentPath(parentPath);
        fileEntry.setLastModificationTime(lsEntry.getAttrs().getMTime());
        return fileEntry;
    }

    /**
     *
     * LsEntry selector, which collects only the internal files and folders without the current directory entry ('.')
     * and its parent ('..')
     */
    class LsEntrySelector implements ChannelSftp.LsEntrySelector {

        private List<LsEntry> entries;

        public LsEntrySelector( List<LsEntry> entries ) {

            this.entries = entries;
        }

        @Override
        public int select( LsEntry lsEntry ) {

            if (!lsEntry.getFilename().endsWith(".")) {
                this.entries.add(lsEntry);
            }
            return ChannelSftp.LsEntrySelector.CONTINUE;
        }
    }
}
