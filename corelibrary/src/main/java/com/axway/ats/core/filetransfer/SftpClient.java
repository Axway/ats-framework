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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.net.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.SshCipher;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.filetransfer.model.ftp.SftpFileTransferProgressMonitor;
import com.axway.ats.core.filetransfer.model.ftp.SftpListener;
import com.axway.ats.core.filetransfer.model.ftp.SynchronizationSftpTransferListener;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.SslUtils;
import com.axway.ats.core.utils.StringUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

/**
 * Uses the JSch component suite for Java
 * ( http://www.jcraft.com/jsch/ ) to initiate and execute SFTP connections
 * to a remote server.
 */
public class SftpClient extends AbstractFileTransferClient {

    private JSch                                jsch                                = null;
    private Session                             session                             = null;
    private ChannelSftp                         channel                             = null;

    private SftpFileTransferProgressMonitor     debugProgressMonitor                = null;
    private SynchronizationSftpTransferListener synchronizationSftpTransferListener = null;

    private static final Logger                 log                                 = LogManager.getLogger( SftpClient.class );

    private static final String                 USE_ONE_OF_THE_SFTP_CONSTANTS       = "Use one of the SFTP_* constatns for key and values in FileTransferClient class";

    public static final String                  SFTP_USERNAME                       = "SFTP_USERNAME";
    public static final String                  SFTP_CIPHERS                        = "SFTP_CIPHERS";

    private List<SshCipher>                     ciphers;

    private String                              username;
    private String                              hostname;
    private String                              password;

    private String                              keyStoreFile;
    private String                              keyStorePassword;
    private String                              keyAlias;

    private String                              trustStoreFile;
    private String                              trustStorePassword;

    private String                              trustedServerSSLCerfiticateFile;

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

        this.jsch = new JSch();
    }

    @Override
    public void connect( String hostname, String userName, String password ) throws FileTransferException {

        this.hostname = hostname;
        this.username = userName;
        this.password = password;

        log.info( "Connecting to " + this.hostname + " on port " + this.port + " using username "
                  + this.username + " and password " + this.password );

        if( !StringUtils.isNullOrEmpty( this.keyStoreFile ) ) {
            log.info( "Keystore location set to '" + this.keyStoreFile + "'" );
        }

        if( this.channel != null ) {
            this.channel.disconnect();
        }
        if( this.session != null ) {
            this.session.disconnect();
        }

        applyCustomProperties();
        doConnect();

    }

    @Override
    public void connect( String hostname, String keystoreFile, String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException {

        throw new FileTransferException( "Not implemented" );

    }

    private void doConnect() throws FileTransferException {

        // make new SFTP object for every new connection
        disconnect();

        /* if debug mode is true, we log messages from all levels */
        /* NOTE: Due to logging being global (static), if one thread enables debug mode, all threads will log messages,
         * and if another thread disables it, logging will be stopped for all threads,
         * until at least one thread enables it again
         */
        if( isDebugMode() ) {
            JSch.setLogger( new SftpListener() );
            debugProgressMonitor = new SftpFileTransferProgressMonitor();
        } else {
            debugProgressMonitor = null;
        }

        try {

            addHostKeyRepository();

            if( username != null ) {
                this.session = jsch.getSession( this.username, this.hostname, this.port );
            }

            this.session.setPassword( this.password );

            if( !StringUtils.isNullOrEmpty( this.keyStoreFile ) ) {
                byte[] privateKeyContent = getPrivateKeyContent();
                this.jsch.addIdentity( "client_prvkey", privateKeyContent, null, null );
            }

            //The internally used client version 'SSH-2.0-JSCH-0.1.54' needs to be changed to 'SSH-2.0-OpenSSH_2.5.3'
            this.session.setClientVersion( "SSH-2.0-OpenSSH_2.5.3" );

            /** if no trust server certificate or trust store are provided, do not check if hostname is in known hosts **/
            if( StringUtils.isNullOrEmpty( this.trustedServerSSLCerfiticateFile )
                && StringUtils.isNullOrEmpty( this.trustStoreFile ) ) {
                // skip checking of known hosts and verifying RSA keys
                this.session.setConfig( "StrictHostKeyChecking", "no" );
            }

            // make keyboard-interactive last authentication method
            this.session.setConfig( "PreferredAuthentications", "publickey,password,keyboard-interactive" );
            this.session.setTimeout( this.timeout );
            if( this.ciphers != null && this.ciphers.size() > 0 ) {
                StringBuilder ciphers = new StringBuilder();
                for( SshCipher cipher : this.ciphers ) {
                    ciphers.append( cipher.getSshAlgorithmName() + "," );
                }
                this.session.setConfig( "cipher.c2s", ciphers.toString() );
                this.session.setConfig( "cipher.s2c", ciphers.toString() );
                this.session.setConfig( "CheckCiphers", ciphers.toString() );
            }
            if( this.listener != null ) {
                this.listener.setResponses( new ArrayList<String>() );
                JSch.setLogger( ( com.jcraft.jsch.Logger ) this.listener );
            }

            /* 
             * SFTP reference implementation does not support transfer mode.
               Due to that, JSch does not support it either.
               For now setTransferMode() has no effect. It may be left like that,
               implemented to throw new FileTransferException( "Not implemented" )
               or log warning, that SFTP protocol does not support transfer mode change.
            */

            this.session.connect();
            this.channel = ( ChannelSftp ) this.session.openChannel( "sftp" );
            this.channel.connect();
        } catch (Exception e) {
            String errMessage = "Unable to connect to  " + hostname + " on port " + this.port
                                + " using username " + username + " and password " + password;
            log.error(errMessage, e);
            throw new FileTransferException(e);
        }
    }

    private void addHostKeyRepository() throws Exception {

        if (!StringUtils.isNullOrEmpty(this.trustStoreFile)) {
            HostKeyRepository hostKeyRepository = new DefaultHostKeyRepository();
            KeyStore trustStore = SslUtils.loadKeystore(trustStoreFile, trustStorePassword);
            // iterate over all entries
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (trustStore.isCertificateEntry(alias)) {
                    /** the alias points to a certificate **/
                    Certificate certificate = trustStore.getCertificate(alias);
                    if (certificate != null) {
                        addPublicKeyToHostKeyRepostitory(certificate.getPublicKey(), hostKeyRepository);
                    }
                } else {
                    /** the alias does not point to a certificate, 
                     * but this may mean that it points to a private-public key pair or a certificate chain 
                     */
                    Certificate certificate = trustStore.getCertificate(alias);
                    if (certificate != null) {
                        /**
                         * the certificate was extracted from a private-public key entry
                         * */
                        addPublicKeyToHostKeyRepostitory(certificate.getPublicKey(), hostKeyRepository);
                    } else {
                        /**
                         * the alias points to a certificate chain
                         * */
                        Certificate[] chain = trustStore.getCertificateChain(alias);
                        for (Certificate cert : chain) {
                            addPublicKeyToHostKeyRepostitory(cert.getPublicKey(), hostKeyRepository);
                        }
                    }
                }
            }
            this.jsch.setHostKeyRepository(hostKeyRepository);
        } else {
            if (StringUtils.isNullOrEmpty(this.trustedServerSSLCerfiticateFile)) {
                return;
            } else {
                try {
                    HostKeyRepository hostKeyRepository = new DefaultHostKeyRepository();
                    KeyStore trustStore = KeyStore.getInstance("JKS");
                    trustStore.load(null);
                    trustStore.setCertificateEntry("cert",
                                                   SslUtils.convertFileToX509Certificate(new File(this.trustedServerSSLCerfiticateFile)));
                    addPublicKeyToHostKeyRepostitory(trustStore.getCertificate("cert").getPublicKey(),
                                                     hostKeyRepository);
                    this.jsch.setHostKeyRepository(hostKeyRepository);
                } catch (Exception e) {
                    throw new Exception("Unable to add public key from certificate '"
                                        + this.trustedServerSSLCerfiticateFile
                                        + "' to known host keys", e);
                }
            }
        }

    }

    private void addPublicKeyToHostKeyRepostitory( PublicKey key,
                                                   HostKeyRepository hostKeyRepository ) throws Exception {

        if (!key.getAlgorithm().contains("RSA")) {
            throw new Exception("Only RSA keys are supported!.");
        }

        byte[] opensshKeyContent = convertToOpenSSHKeyFormat((RSAPublicKey) key);

        HostKey hostkey = new HostKey(hostname, HostKey.SSHRSA, opensshKeyContent);
        hostKeyRepository.add(hostkey, null);

    }

    private byte[] convertToOpenSSHKeyFormat( RSAPublicKey key ) throws Exception {

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] name = "ssh-rsa".getBytes("US-ASCII");
        write(name, buf);
        write(key.getPublicExponent().toByteArray(), buf);
        write(key.getModulus().toByteArray(), buf);
        return buf.toByteArray();
    }

    // encode the str byte array to Base64 one, also encode its length
    private void write( byte[] str, OutputStream os ) throws IOException {

        for (int shift = 24; shift >= 0; shift -= 8) {
            os.write( (str.length >>> shift) & 0xFF);
        }
        os.write(str);

    }

    private byte[] getPrivateKeyContent() throws Exception {

        try {
            KeyStore keyStore = SslUtils.loadKeystore(keyStoreFile, keyStorePassword);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, null);

            if (privateKey == null) {
                throw new Exception("The alias '" + keyAlias + "' does not point to an existing key-related entry");
            }

            StringWriter stringWriter = new StringWriter();
            PEMWriter pemWriter = new PEMWriter(stringWriter);
            pemWriter.writeObject(privateKey);
            pemWriter.close();

            byte[] privateKeyPEM = stringWriter.toString().getBytes();

            return privateKeyPEM;
        } catch (Exception e) {
            throw new Exception("Could not get private key content", e);
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

        checkConnected("file upload");

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

        checkConnected("file download");

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

    /**
     * Set a client key store which will be used for authentication
     * @param keystoreFile the key store file path ( Must be in JKS or PKCS12 format )
     * @param keystorePassword the key store password
     *
     * **/
    @Override
    public void setKeystore( String keystoreFile, String keystorePassword, String alias ) {

        this.keyStoreFile = keystoreFile;
        this.keyStorePassword = keystorePassword;
        this.keyAlias = alias;

    }

    /**
     * Set a client trust store which will be used for validating trust server certificates
     * @param truststoreFile the trust store file path ( Must be in JKS or PKCS12 format )
     * @param truststorePassword the trust store password
     * 
     * <p><b>Note that call to this method will override any effect from both this method and setTrustedServerSSLCertificate</b></p>
     * **/
    @Override
    public void setTrustStore( String truststoreFile, String truststorePassword ) {

        this.trustStoreFile = truststoreFile;
        this.trustStorePassword = truststorePassword;

        // invalidate any previously set trust server certificate
        if (!StringUtils.isNullOrEmpty(this.trustedServerSSLCerfiticateFile)) {
            log.warn("Previously set trust server certificate '" + this.trustedServerSSLCerfiticateFile
                     + "' will be overridden and only certificates from truststore '" + truststoreFile
                     + "' will be used for validation");
            this.trustedServerSSLCerfiticateFile = null;
        }

    }

    /**
     * Set a client certificate which will be used for authentication
     * @param certificateFile the trust server certificate file path (must be a .PEM file)
     * 
     * <p><b>Note that call to this method will override any effect from both this method and setTrustStore</b></p>
     * **/
    @Override
    public void setTrustedServerSSLCertificate( String certificateFile ) {

        this.trustedServerSSLCerfiticateFile = certificateFile;

        // invalidate any previously set trust store
        if (!StringUtils.isNullOrEmpty(this.trustStoreFile)) {
            log.warn("Previously set trust store '" + this.trustStoreFile
                     + "' will be overridden and only the certificate '" + trustedServerSSLCerfiticateFile
                     + "' will be used for validation");
            this.trustStoreFile = null;
            this.trustStorePassword = null;
        }
    }

    /**
     * This method exposes the underlying SFTP connection.
     * Since the implementation can be change at any time, users must not use this method directly.
     * */
    public ChannelSftp getInternalFtpsClient() {

        return this.channel;

    }

    private void checkConnected( String operation ) {

        // the current implementation works always with 'channel' and 'session'
        // to it is enough to check one of them
        if (this.channel == null || !this.channel.isConnected()) {
            throw new FileTransferException("Cannot do " + operation + " when not connected");
        }
    }

    /**
     * Default implementation of HostKeyRepository that check if there are trusted server certificates in the presented trust store
     * */
    class DefaultHostKeyRepository implements HostKeyRepository {

        Map<String, Set<byte[]>> knownHostsMap = new HashMap<>();

        @Override
        public void remove( String host, String type, byte[] key ) {

            host = host.replace("[", "").replace("]", "").split(":")[0];

            Set<byte[]> keys = knownHostsMap.get(host);
            keys.remove(key);
        }

        @Override
        public void remove( String host, String type ) {

            host = host.replace("[", "").replace("]", "").split(":")[0];

            knownHostsMap.remove(host);

        }

        @Override
        public String getKnownHostsRepositoryID() {

            return Thread.currentThread().getName() + "SFTP_KnownHostsRepository";
        }

        @Override
        public HostKey[] getHostKey( String host, String type ) {

            host = host.replace("[", "").replace("]", "").split(":")[0];

            Set<byte[]> keys = knownHostsMap.get(host);

            HostKey[] hostKeys = new HostKey[keys.size()];
            Iterator<byte[]> it = keys.iterator();
            int i = 0;
            while (it.hasNext()) {
                try {
                    hostKeys[i++] = new HostKey(host, HostKey.SSHRSA, it.next());
                } catch (JSchException e) {
                    throw new RuntimeException("Unable to get hostkey for host '" + host + "'");
                }
            }
            return hostKeys;
        }

        @Override
        public HostKey[] getHostKey() {

            List<HostKey[]> hostKeysList = new ArrayList<>();
            Iterator<String> it = knownHostsMap.keySet().iterator();
            int size = 0;
            while (it.hasNext()) {
                HostKey[] hostKeysEntry = getHostKey(it.next(), "public key");
                hostKeysList.add(hostKeysEntry);
                size += hostKeysEntry.length;
            }

            HostKey[] hostKeys = new HostKey[size];
            int i = 0;
            for (HostKey[] keys : hostKeysList) {
                for (HostKey key : keys) {
                    hostKeys[i++] = key;
                }
            }

            return hostKeys;
        }

        @Override
        public int check( String host, byte[] key ) {

            host = host.replace("[", "").replace("]", "").split(":")[0]; // get only the IP address of the server

            if (knownHostsMap.get(host) == null) {
                log.error("The presented trust store certificates could not match any of the server provided ones");
                return HostKeyRepository.NOT_INCLUDED;
            }
            Set<byte[]> keys = knownHostsMap.get(host);
            for (byte[] key1 : keys) {
                key1 = Base64.decodeBase64(key1); // we must decode the key from the client trust store first
                if (Arrays.equals(key, key1)) {
                    log.info("Server certificate trusted.");
                    return HostKeyRepository.OK;
                }
            }
            log.error("The presented trust store certificates could not match any of the server provided ones");
            return HostKeyRepository.NOT_INCLUDED;

        }

        @Override
        public void add( HostKey hostkey, UserInfo ui ) {

            Set<byte[]> keys = knownHostsMap.get(hostkey.getHost());
            if (keys == null) {
                keys = new HashSet<>();
            }
            keys.add(hostkey.getKey().getBytes());
            knownHostsMap.put(hostkey.getHost(), keys);

        }

    }

}
