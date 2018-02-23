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
package com.axway.ats.core.filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.SshCipher;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.filetransfer.model.ftp.SftpFileTransferProgressMonitor;
import com.axway.ats.core.filetransfer.model.ftp.SftpListener;
import com.axway.ats.core.filetransfer.model.ftp.SynchronizationSftpTransferListener;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.SslUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Uses the JSch component suite for Java
 * ( http://www.jcraft.com/jsch/ ) to initiate and execute SFTP connections
 * to a remote server.
 */
public class SftpClient extends AbstractFileTransferClient {

    public static final String                  SFTP_USERNAME                       = "SFTP_USERNAME";
    public static final String                  SFTP_CIPHERS                        = "SFTP_CIPHERS";
    private static final Logger                 log                                 = Logger.getLogger(SftpClient.class);
    private static final String                 USE_ONE_OF_THE_SFTP_CONSTANTS       = 
            "Use one of the SFTP_* constatns for key and values in GenericFileTransferClient class";

    private JSch                                jsch                                = null;
    private Session                             session                             = null;
    private ChannelSftp                         channel                             = null;

    private SftpFileTransferProgressMonitor     debugProgressMonitor                = null;
    private SynchronizationSftpTransferListener synchronizationSftpTransferListener = null;

    private List<SshCipher>                     ciphers;

    private String                              username;
    private String                              hostname;
    private String                              password;
    private String                              keystoreFile;
    private String                              keystorePassword;
    private String                              publicKeyAlias;

    
    static {
        // Adds *once* BoncyCastle provider as the first one, before any default JRE providers.
        SslUtils.registerBCProvider();
    }
    /**
     * Constructor
     *
     */
    public SftpClient() {

        super();
    }

    @Override
    public void connect( String hostname, String userName, String password ) throws FileTransferException {

        this.hostname = hostname;
        this.username = userName;
        this.password = password;

        log.info("Connecting to " + this.hostname + " on port " + this.port + " using username "
                 + this.username + " and password " + this.password);

        if (this.channel != null) {
            this.channel.disconnect();
        }
        if (this.session != null) {
            this.session.disconnect();
        }

        applyCustomProperties();
        doConnect(null);

    }

    @Override
    public void connect( String hostname, String keystoreFile, String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException {

        this.hostname = hostname;
        this.keystoreFile = keystoreFile;
        this.keystorePassword = keystorePassword;
        this.publicKeyAlias = publicKeyAlias;

        log.info("Connecting to " + this.hostname + " on port " + this.port + " using keystore file "
                 + this.keystoreFile + " and public key alias " + this.publicKeyAlias);

        if (this.channel != null) {
            this.channel.disconnect();
        }
        if (this.session != null) {
            this.session.disconnect();
        }

        applyCustomProperties();
        doConnect(publicKeyAlias);

    }

    private void doConnect( String publicKeyAlias ) throws FileTransferException {

        // make new SFTP object for every new connection
        disconnect();

        this.jsch = new JSch();

        /* if debug mode is true, we log messages from all levels 
         * NOTE: Due to logging being global (static), if one thread enables debug mode, 
         * all threads will log messages,
         * and if another thread disables it, logging will be stopped for all threads,
         * until at least one thread enables it again
         */
        if (isDebugMode()) {
            JSch.setLogger(new SftpListener());
            debugProgressMonitor = new SftpFileTransferProgressMonitor();
        } else {
            debugProgressMonitor = null;
        }

        try {
            if (username != null) {
                this.session = jsch.getSession(this.username, this.hostname, this.port);
                this.session.setPassword(this.password);
            } else {
                this.jsch.addIdentity(this.keystoreFile, this.publicKeyAlias,
                                      this.keystorePassword.getBytes());
                this.session = this.jsch.getSession(this.username, this.hostname, this.port);
            }

            /*
             * At the time of writing this code, the used JSCH library is version 0.1.50
             * and connection attempts fail when running with Java 7.
             * The internally used client version 'SSH-2.0-JSCH-0.1.54' needs to be changed to 'SSH-2.0-OpenSSH_2.5.3'
             */
            this.session.setClientVersion("SSH-2.0-OpenSSH_2.5.3");

            // skip checking of known hosts and verifying RSA keys
            this.session.setConfig("StrictHostKeyChecking", "no");
            // make keyboard-interactive last authentication method
            this.session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive");
            this.session.setTimeout(this.timeout);
            if (this.ciphers != null && this.ciphers.size() > 0) {
                StringBuilder ciphers = new StringBuilder();
                for (SshCipher cipher : this.ciphers) {
                    ciphers.append(cipher.getSshAlgorithmName() + ",");
                }
                this.session.setConfig("cipher.c2s", ciphers.toString());
                this.session.setConfig("cipher.s2c", ciphers.toString());
                this.session.setConfig("CheckCiphers", ciphers.toString());
            }
            if (this.listener != null) {
                this.listener.setResponses(new ArrayList<String>());
                JSch.setLogger((com.jcraft.jsch.Logger) this.listener);
            }

            /* SFTP reference implementation does not support transfer mode.
               Due to that, JSch does not support it either.
               For now setTransferMode() has no effect. It may be left like that,
               implemented to throw new FileTransferException( "Not implemented" )
               or log warning, that SFTP protocol does not support transfer mode change.
            */

            this.session.connect();
            this.channel = (ChannelSftp) this.session.openChannel("sftp");
            this.channel.connect();
        } catch (JSchException e) {
            throw new FileTransferException("Unable to connect!", e);
        }
    }

    @Override
    public void disconnect() throws FileTransferException {

        if (this.channel != null && this.channel.isConnected()) {
            this.channel.disconnect();
        }
        if (this.session != null && this.session.isConnected()) {
            this.session.disconnect();
        }

    }

    @Override
    public String executeCommand( String command ) throws FileTransferException {

        throw new FileTransferException("Not implemented");
    }

    @Override
    protected void performUploadFile( String localFile, String remoteDir,
                                      String remoteFile ) throws FileTransferException {

        FileInputStream fis = null;

        try {
            String remoteFileAbsPath = null;
            remoteDir = remoteDir.replace("\\", "/");
            remoteFile = remoteFile.replace("\\", "/");

            if (remoteDir.endsWith("/") && remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir.substring(0, remoteDir.length() - 2) + remoteFile;
            } else if (!remoteDir.endsWith("/") && !remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir + "/" + remoteFile;
            } else {
                remoteFileAbsPath = remoteDir + remoteFile;
            }
            // upload the file
            File file = new File(localFile);
            fis = new FileInputStream(file);
            if (synchronizationSftpTransferListener != null) {
                this.channel.put(fis, remoteFileAbsPath, synchronizationSftpTransferListener);
            } else if (isDebugMode() && debugProgressMonitor != null) {
                debugProgressMonitor.setTransferMetadata(localFile, remoteFileAbsPath, file.length());
                this.channel.put(fis, remoteFileAbsPath, debugProgressMonitor);
            } else {
                this.channel.put(fis, remoteFileAbsPath);
            }
        } catch (SftpException e) {
            log.error("Unable to upload file!", e);
            throw new FileTransferException(e);
        } catch (FileNotFoundException e) {
            log.error("Unable to find the file that needs to be uploaded!", e);
            throw new FileTransferException(e);
        } finally {
            // close the file input stream
            IoUtils.closeStream(fis, "Unable to close the file stream after successful upload!");
        }

        if (remoteDir != null && !remoteDir.endsWith("/")) {
            remoteDir += "/";
        }
        log.info("Successfully uploaded '" + localFile + "' to '" + remoteDir + remoteFile + "', host "
                 + this.hostname);

    }

    @Override
    protected void performDownloadFile( String localFile, String remoteDir,
                                        String remoteFile ) throws FileTransferException {

        FileOutputStream fos = null;
        try {
            String remoteFileAbsPath = null;
            remoteDir = remoteDir.replace("\\", "/");
            remoteFile = remoteFile.replace("\\", "/");

            if (remoteDir.endsWith("/") && remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir.substring(0, remoteDir.length() - 2) + remoteFile;
            } else if (!remoteDir.endsWith("/") && !remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir + "/" + remoteFile;
            } else {
                remoteFileAbsPath = remoteDir + remoteFile;
            }
            // download the file
            File file = new File(localFile);
            fos = new FileOutputStream(localFile);
            if (isDebugMode() && debugProgressMonitor != null) {
                debugProgressMonitor.setTransferMetadata(localFile, remoteFileAbsPath, file.length());
                this.channel.get(remoteFileAbsPath, fos, debugProgressMonitor);
            } else {
                this.channel.get(remoteFileAbsPath, fos);
            }
        } catch (SftpException e) {
            log.error("Unable to download " + localFile, e);
            throw new FileTransferException(e);
        } catch (FileNotFoundException e) {
            log.error("Unable to create " + localFile, e);
            throw new FileTransferException(e);
        } finally {
            // close the file output stream
            IoUtils.closeStream(fos, "Unable to close the file stream after successful download!");
        }
        if (remoteDir != null && !remoteDir.endsWith("/")) {
            remoteDir += "/";
        }
        log.info("Successfully downloaded '" + localFile + "' from '" + remoteDir + remoteFile + "', host "
                 + this.hostname);

    }

    @Override
    protected TransferListener addListener( int progressEventNumber ) {

        SynchronizationSftpTransferListener listener = new SynchronizationSftpTransferListener(this,
                                                                                               progressEventNumber);
        this.synchronizationSftpTransferListener = listener;

        return listener;
    }

    @Override
    protected void removeListener( TransferListener listener ) {

        this.debugProgressMonitor = null;

    }

    @Override
    protected void finalize() throws Throwable {

        // ensure the connection is terminated
        this.disconnect();

        this.listener = null;

        super.finalize();
    }

    @Override
    public void enableResponseCollection( boolean enable ) {

        // not implemented
    }

    @Override
    public String[] getResponses() {

        // not implemented
        return new String[]{};
    }

    @Override
    public void addCustomProperty( String key, Object value ) throws IllegalArgumentException {

        if (key.equals(SFTP_CIPHERS)) {
            customProperties.put(key, value);
        } else if (key.equals(SFTP_USERNAME)) {
            username = value.toString();
        } else {
            throw new IllegalArgumentException("Unknown property with key '" + key + "' is passed. "
                                               + USE_ONE_OF_THE_SFTP_CONSTANTS);
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        Set<Entry<String, Object>> customPropertiesSet = customProperties.entrySet();
        Object value;
        for (Entry<String, Object> customPropertyEntry : customPropertiesSet) {
            value = customPropertyEntry.getValue();
            if (customPropertyEntry.getKey().equals(SFTP_CIPHERS)) {

                if (value instanceof SshCipher) {
                    addCipher((SshCipher) value);
                } else if (value instanceof SshCipher[]) {
                    for (SshCipher cipher : (SshCipher[]) value) {
                        addCipher(cipher);
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported '" + SFTP_CIPHERS + "' value type");
                }
            } else {
                throw new IllegalArgumentException("Unknown property with key '" + customPropertyEntry.getKey()
                                                   + "' is passed. "
                                                   + USE_ONE_OF_THE_SFTP_CONSTANTS);
            }
        }
    }

    private void addCipher( SshCipher cipher ) {

        if (this.ciphers == null) {
            this.ciphers = new ArrayList<SshCipher>();
        }
        log.debug("Adding cipher " + cipher + " to the SFTP connection configuration");
        this.ciphers.add(cipher);
    }
}
