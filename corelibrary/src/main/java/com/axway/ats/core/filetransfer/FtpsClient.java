/*
 * Copyright 2017-2020 Axway Software
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.commons.net.ProtocolCommandListener;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpResponseListener;
import com.axway.ats.core.filetransfer.model.ftp.SynchronizationFtpTransferListener;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.SslUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * The {@link FtpsClient} uses the Apache Commons Net component suite for Java
 * ( https://commons.apache.org/proper/commons-net/ ) to initiate and execute FTPS 
 * connections to a remote server. <br>
 * <br>
 * The current implementation does *not* verify the server certificate against a
 * local trusted CA store!
 */
public class FtpsClient extends AbstractFileTransferClient {

    private org.apache.commons.net.ftp.FTPSClient ftpsConnection                 = null;

    private static final Logger                   log                            = LogManager.getLogger(FtpsClient.class);

    private static final String                   USE_ONE_OF_THE_FTPS_CONSTANTS  = "Use one of the FTPS_* constants for key and values in GenericFileTransferClient class";

    /**
     * Key for setting FTPS connection type.
     */
    public static final String                    FTPS_CONNECTION_TYPE           = "FTPS_CONNNECTION_TYPE";
    public static final Integer                   FTPS_CONNECTION_TYPE__IMPLICIT = 1;
    public static final Integer                   FTPS_CONNECTION_TYPE__AUTH_SSL = 2;
    public static final Integer                   FTPS_CONNECTION_TYPE__AUTH_TLS = 3;

    public static final String                    FTPS_ENCRYPTION_PROTOCOLS      = "FTPS_ENCRYPTION_PROTOCOLS";

    private boolean                               implicit                       = false;
    private String                                protocol                       = "TLSv1.2";

    private String                                keyStoreFile;
    private String                                keyStorePassword;

    private String                                trustStoreFile;
    private String                                trustStorePassword;

    private String                                trustedServerSSLCerfiticateFile;

    static {
        // Adds *once* BoncyCastle provider as the first one, before any default JRE providers.
        SslUtils.registerBCProvider();
    }

    /**
     * Constructor
     *
     */
    public FtpsClient() {

        super();
    }

    /**
     * Set the {@link TransferMode} that this protocol should use
     *
     * @param mode
     * @throws FileTransferException
     */
    @Override
    public void setTransferMode(
                                 TransferMode mode ) throws FileTransferException {

        if (this.ftpsConnection != null && this.ftpsConnection.isConnected() && this.transferMode != mode) {
            try {
                log.info("Set file transfer mode to " + mode);
                if (mode == TransferMode.ASCII) {
                    if (!this.ftpsConnection.setFileType(org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE)) {
                        throw new Exception("Unable to set transfer mode to ASCII");
                    }
                } else {
                    if (!this.ftpsConnection.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)) {
                        throw new Exception("Unable to set transfer mode to BINARY");
                    }
                }
            } catch (Exception e) {
                throw new FileTransferException("Error setting file transfer mode to " + mode, e);
            }
        }

        super.setTransferMode(mode);
    }

    /**
     * Connect to a remote host using basic authentication
     *
     * @param hostname the host to connect to
     * @param userName the user name
     * @param password the password for the provided user name
     * @throws FileTransferException
     */
    @Override
    public void connect(
                         String hostname,
                         String userName,
                         String password ) throws FileTransferException {

        log.info("Connecting to " + hostname + " on port " + this.port + " using username " + userName
                 + " and password " + password);

        if (!StringUtils.isNullOrEmpty(this.keyStoreFile)) {
            log.info("Keystore location set to '" + this.keyStoreFile + "'");
        }

        if (!StringUtils.isNullOrEmpty(this.trustStoreFile)) {
            log.info("Truststore location set to '" + this.trustStoreFile + "'");
        }

        if (!StringUtils.isNullOrEmpty(this.trustedServerSSLCerfiticateFile)) {
            log.info("Trust server certificate set to '" + this.trustedServerSSLCerfiticateFile + "'");
        }

        performConnect(hostname, userName, password);
    }

    @Override
    public void connect( String hostname, String keystoreFile, String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException {

        throw new FileTransferException("Not implemented");

    }

    private void performConnect(
                                 String hostname,
                                 String userName,
                                 String password ) throws FileTransferException {

        try {

            // make new FTP object for every new connection
            disconnect();
            applyCustomProperties();
            SSLContext sslContext = null;

            try {
                sslContext = createSSLContext();
            } catch (Exception e) {
                throw new Exception("Error while creating SSL context", e);
            }

            //this.ftpsConnection = new FTPSClient( this.protocol, this.implicit);
            this.ftpsConnection = new org.apache.commons.net.ftp.FTPSClient(this.implicit, sslContext);

            if (this.listener != null) {
                this.listener.setResponses(new ArrayList<String>());
                this.ftpsConnection.addProtocolCommandListener( ((FtpResponseListener) listener));
            }
            /* if debug mode is true, we log messages from all levels */
            if (isDebugMode()) {
                this.ftpsConnection.addProtocolCommandListener(new FtpListener());
            }

            this.ftpsConnection.setConnectTimeout(this.timeout);
            // connect to the host
            this.ftpsConnection.connect(hostname, this.port);
            // login to the host
            if (!this.ftpsConnection.login(userName, password)) {
                throw new Exception("Invalid username and/or password. ");
            }
            // set transfer mode
            if (this.transferMode == TransferMode.ASCII) {
                if (!this.ftpsConnection.setFileType(org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE)) {
                    throw new Exception("Unable to set transfer mode to ASCII");
                }
            } else {
                if (!this.ftpsConnection.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)) {
                    throw new Exception("Unable to set transfer mode to BINARY");
                }
            }
            // initial fix - always use passive mode
            // Currently not working: int replyCode = this.ftpsConnection.pasv();
            this.ftpsConnection.enterLocalPassiveMode();
        } catch (Exception e) {
            String errMessage = "Unable to connect to  " + hostname + " on port " + this.port
                                + " using username " + userName + " and password " + password;
            log.error(errMessage, e);
            throw new FileTransferException(e);
        }
    }

    private SSLContext createSSLContext() throws Exception {

        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
        sslContextBuilder.useProtocol(protocol);

        // add key store
        if (!StringUtils.isNullOrEmpty(keyStoreFile)) {
            // if bouncy castle provider is set as the first one,
            // then the sslContextBuilder try to load the key store from a JKS format,
            // that's why instead we load the key store ourselves and provide it to the builder
            sslContextBuilder.loadKeyMaterial(SslUtils.loadKeystore(keyStoreFile, keyStorePassword),
                                              keyStorePassword.toCharArray());

        }

        // add trust store
        if (!StringUtils.isNullOrEmpty(trustStoreFile)) {
            /** load the trust store **/
            KeyStore trustStore = SslUtils.loadKeystore(trustStoreFile,
                                                        trustStorePassword);

            sslContextBuilder.loadTrustMaterial(trustStore,
                                                /**
                                                 * for better error message logging, we provide custom TrustStrategy, instead of the default one
                                                 * */
                                                new DefaultTrustStrategy(trustStore));
        } else if (!StringUtils.isNullOrEmpty(trustedServerSSLCerfiticateFile)) {
            // load the client certificate content
            final X509Certificate trustedServerCertificate = SslUtils.convertFileToX509Certificate(new File(this.trustedServerSSLCerfiticateFile));
            // create trust store and add the client certificate
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null);
            trustStore.setCertificateEntry("client_certificate", trustedServerCertificate);
            // add the trust store to the SSL builder
            sslContextBuilder.loadTrustMaterial(trustStore,
                                                /**
                                                 * for better error message logging, we provide custom TrustStrategy, instead of the default one
                                                 * */
                                                new DefaultTrustStrategy(trustStore));
        } else {
            // since no trust store is specified, we will trust all certificates
            sslContextBuilder.loadTrustMaterial(new TrustAllTrustStrategy());
        }

        return sslContextBuilder.build();
    }

    /**
     * Disconnect from the remote host
     *
     * @throws FileTransferException
     */
    @Override
    public void disconnect() throws FileTransferException {

        if (this.ftpsConnection != null && this.ftpsConnection.isConnected()) {
            try {
                this.ftpsConnection.disconnect();
                this.ftpsConnection = null;
            } catch (IOException e) {
                throw new FileTransferException(e);
            }
        }

    }

    @Override
    protected void performDownloadFile(
                                        String localFile,
                                        String remoteDir,
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
            fos = new FileOutputStream(new File(localFile));
            if (!this.ftpsConnection.retrieveFile(remoteFileAbsPath, fos)) {
                throw new FileTransferException("Unable to retrieve " + remoteDir + "/" + remoteFile
                                                + " from " + this.ftpsConnection.getPassiveHost() + " as a"
                                                + localFile);
            }
        } catch (Exception e) {
            log.error("Unable to download file " + localFile, e);
            throw new FileTransferException(e);
        } finally {
            // close the file output stream
            IoUtils.closeStream(fos, "Unable to close the file stream after successful download!");
        }

        if (remoteDir != null && !remoteDir.endsWith("/")) {
            remoteDir += "/";
        }
        log.info("Successfully downloaded '" + localFile + "' from '" + remoteDir + remoteFile + "', host "
                 + ftpsConnection.getPassiveHost());
    }

    @Override
    protected void performUploadFile(
                                      String localFile,
                                      String remoteDir,
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
            fis = new FileInputStream(new File(localFile));
            if (!this.ftpsConnection.storeFile(remoteFileAbsPath, fis)) {
                throw new FileTransferException("Unable to store " + localFile + " to "
                                                + this.ftpsConnection.getPassiveHost() + " as a "
                                                + (remoteDir.endsWith("/")
                                                                           ? remoteDir
                                                                           : remoteDir + "/")
                                                + remoteFile);
            }
        } catch (Exception e) {
            log.error("Unable to upload file!", e);
            throw new FileTransferException(e);
        } finally {
            IoUtils.closeStream(fis, "Unable to close the file stream after successful upload!");
        }

        if (remoteDir != null && !remoteDir.endsWith("/")) {
            remoteDir += "/";
        }
        log.info("Successfully uploaded '" + localFile + "' to '" + remoteDir + remoteFile + "', host "
                 + ftpsConnection.getPassiveHost());
    }

    /**
     * Currently not supporting commands requiring opening of data connection
     * @param command the command to run
     * @return String representing the return code
     * @throws FileTransferException
     */
    @Override
    public String executeCommand(
                                  String command ) throws FileTransferException {

        log.info("Run '" + command + "'");
        String returnCode = "";

        try {
            returnCode = String.valueOf(this.ftpsConnection.sendCommand(command));
        } catch (IOException e) {
            log.error("Error running command: '" + command + "'", e);
            throw new FileTransferException(e);
        }

        log.info("Return code is '" + returnCode + "'");
        return returnCode;
    }

    @Override
    protected TransferListener addListener(
                                            int progressEventNumber ) {

        SynchronizationFtpTransferListener listener = new SynchronizationFtpTransferListener(this,
                                                                                             progressEventNumber);
        this.ftpsConnection.addProtocolCommandListener(listener);

        return listener;
    }

    @Override
    protected void removeListener(
                                   TransferListener listener ) {

        this.ftpsConnection.removeProtocolCommandListener((ProtocolCommandListener) listener);

    }

    @Override
    protected void finalize() throws Throwable {

        // ensure the connection is terminated
        this.disconnect();

        this.listener = null;

        super.finalize();
    }

    @Override
    public void enableResponseCollection(
                                          boolean enable ) {

        if (enable) {
            this.listener = new FtpResponseListener();
            // If it's connected add the listener to gather the responses
            if (this.ftpsConnection != null) {
                this.ftpsConnection.addProtocolCommandListener((FtpResponseListener) listener);
            }
        } else {
            // If it's connected remove the listener
            if (this.ftpsConnection != null) {
                this.ftpsConnection.removeProtocolCommandListener((FtpResponseListener) listener);
            }
            this.listener = null;
        }
    }

    @Override
    public String[] getResponses() {

        if (this.listener == null) {
            return new String[]{};
        }

        List<String> responses = this.listener.getResponses();

        return responses.toArray(new String[responses.size()]);
    }

    @Override
    public void addCustomProperty(
                                   String key,
                                   Object value ) throws IllegalArgumentException {

        if (key.equals(FTPS_CONNECTION_TYPE)) {
            if (! (value instanceof Integer)) {
                throw new IllegalArgumentException("Value '" + value + "' for property key '" + key
                                                   + "' has not supported type. "
                                                   + USE_ONE_OF_THE_FTPS_CONSTANTS);
            } else {
                customProperties.put(key, value);
            }
        } else if (key.equals(FTPS_ENCRYPTION_PROTOCOLS)) {
            customProperties.put(key, value);
        } else {
            throw new IllegalArgumentException("Unknown property with key '" + key + "' is passed. "
                                               + USE_ONE_OF_THE_FTPS_CONSTANTS);
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        Set<Entry<String, Object>> customPropertiesSet = customProperties.entrySet();
        Object value;
        boolean ftpsConnectionTypeIsSet = false;
        for (Entry<String, Object> customPropertyEntry : customPropertiesSet) {
            value = customPropertyEntry.getValue();
            if (customPropertyEntry.getKey().equals(FTPS_CONNECTION_TYPE)) {
                ftpsConnectionTypeIsSet = true;
                if (value.equals(FTPS_CONNECTION_TYPE__IMPLICIT)) {
                    log.debug("Setting FTPS connection type to IMPLICIT_SSL");
                    implicit = true;
                    protocol = "SSL";
                } else if (value.equals(FTPS_CONNECTION_TYPE__AUTH_SSL)) {
                    log.debug("Setting FTPS connection type to AUTH_SSL");
                    implicit = false;
                    protocol = "SSL";
                } else if (value.equals(FTPS_CONNECTION_TYPE__AUTH_TLS)) {
                    log.debug("Setting FTPS connection type to AUTH_TLS");
                    implicit = false;
                    protocol = "TLSv1.2";
                } else {
                    ftpsConnectionTypeIsSet = false;
                    throw new IllegalArgumentException("Unknown value '"
                                                       + value
                                                       + "' for FTPS connection type. "
                                                       + "Check value used in addCustomProperty() method. Use one of the GenericFileTransferClient.FTPS_CONNECTION_TYPE__* constants for value");
                }
            } else if (customPropertyEntry.getKey().equals(FTPS_ENCRYPTION_PROTOCOLS)) {
                // currently we can set just one protocol
                String[] encryptionProtocols = parseCustomProperties(value.toString());
                protocol = encryptionProtocols[0];
            } else {
                throw new IllegalArgumentException("Unknown property with key '" + customPropertyEntry.getKey()
                                                   + "' is passed. "
                                                   + USE_ONE_OF_THE_FTPS_CONSTANTS);
            }
        }
        if (!ftpsConnectionTypeIsSet) { // set explicitly the default connection type
            log.debug("Using by default the FTPS connection type AUTH_TLS");
            implicit = false;
            protocol = "TLSv1.2";
        }

    }

    /**
     * Set a client key store which will be used for authentication
     * @param keystoreFile the key store file path ( Must be in JKS or PKCS12 format )
     * @param keystorePassword the key store password
     * **/
    @Override
    public void setKeystore( String keystoreFile, String keystorePassword, String alias ) {

        this.keyStoreFile = keystoreFile;
        this.keyStorePassword = keystorePassword;
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
     * This method exposes the underlying FTPS client.
     * Since the implementation can be change at any time, users must not use this method directly.
     * */
    public org.apache.commons.net.ftp.FTPSClient getInternalFtpsClient() {

        return ftpsConnection;

    }

    private String printCertificateInfo( X509Certificate cert ) {

        StringBuffer sb = new StringBuffer();

        sb.append("  Checking SSL Server certificate :\n")
          .append("  Subject DN: " + cert.getSubjectDN() + "\n")
          .append("  Signature Algorithm: " + cert.getSigAlgName() + "\n")
          .append("  Valid from: " + cert.getNotBefore() + "\n")
          .append("  Valid until: " + cert.getNotAfter() + "\n")
          .append("  Issuer: " + cert.getIssuerDN() + "\n");

        return sb.toString();
    }

    /**
     * Trust Strategy that trust all of the certificates in the presented trust store
     * */
    class DefaultTrustStrategy implements TrustStrategy {

        private List<Certificate> certificates = new ArrayList<Certificate>();

        public DefaultTrustStrategy( KeyStore trustStore ) throws Exception {
            /** get all certificates from the trust store **/
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (trustStore.isCertificateEntry(alias)) {
                    /** the alias points to a certificate **/
                    certificates.add(trustStore.getCertificate(alias));
                } else {
                    /** the alias does not point to a certificate, 
                     * but this may mean that it points to a private-public key pair or a certificate chain 
                     */
                    Certificate certificate = trustStore.getCertificate(alias);
                    if (certificate != null) {
                        /**
                         * the certificate was extracted from a private-public key entry
                         * */
                        certificates.add(certificate);
                    } else {
                        /**
                         * the alias points to a certificate chain
                         * */
                        Certificate[] chain = trustStore.getCertificateChain(alias);
                        for (Certificate cert : chain) {
                            certificates.add(cert);
                        }
                    }
                }
            }
        }

        @Override
        public boolean isTrusted( X509Certificate[] chain, String authType ) throws CertificateException {

            for (X509Certificate presentedCert : chain) {

                if (log.isDebugEnabled()) {
                    String certificateInformation = printCertificateInfo(presentedCert);
                    log.debug(certificateInformation);
                }

                try {
                    presentedCert.checkValidity(); // throws exception
                } catch (Exception e) {
                    log.error("Certificate invalid.", e);
                    return false;
                }

                for (Certificate trustedServerCertificate : certificates) {
                    if (presentedCert.equals(trustedServerCertificate)) {
                        if (!log.isDebugEnabled()) {
                            /*
                             * DEBUG level is not enabled, so the certificate information was not previously logged
                             * */
                            String certificateInformation = printCertificateInfo(presentedCert);
                            log.info(certificateInformation);
                        }
                        log.info("Server certificate trusted.");
                        return true;
                    } else {

                    }
                }

            }

            //log.error("The presented trust store certificates could not match any of the server provided ones");
            throw new CertificateException("The presented trust store certificates could not match any of the server provided ones");
            //return false;
        }

    }

    /**
     * TrustStrategy that trust any/all certificate/s
     * **/
    class TrustAllTrustStrategy implements TrustStrategy {

        @Override
        public boolean isTrusted( X509Certificate[] chain, String authType ) throws CertificateException {

            return true;
        }

    }

}
