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
package com.axway.ats.action.ftp;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.model.ftp.FtpListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpResponseListener;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.SslUtils;
import com.axway.ats.core.utils.StringUtils;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@link FtpsClient} uses the Apache Commons Net component suite for Java
 * ( https://commons.apache.org/proper/commons-net/ ) to initiate and execute FTPS
 * connections to a remote server. <br>
 * <br>
 * The default implementation does *not* verify the server certificate against a
 * local trusted CA store!
 */
@PublicAtsApi
public class FtpsClient extends AbstractFtpClient implements IFtpClient {
    private org.apache.commons.net.ftp.FTPSClient client = null;

    private static final Logger log = Logger.getLogger(FtpsClient.class);

    public static final String USE_ONE_OF_THE_FTPS_CONSTANTS = "Use one of the FTPS_* constants for key and values in GenericFileTransferClient class";

    /**
     * Key for a setting FTPS connection type.
     */
    public static final String FTPS_CONNECTION_TYPE = "FTPS_CONNNECTION_TYPE";
    public static final Integer FTPS_CONNECTION_TYPE__IMPLICIT = 1;
    public static final Integer FTPS_CONNECTION_TYPE__AUTH_SSL = 2;
    public static final Integer FTPS_CONNECTION_TYPE__AUTH_TLS = 3;

    public static final String FTPS_ENCRYPTION_PROTOCOLS = "FTPS_ENCRYPTION_PROTOCOLS";

    private boolean implicit = false;
    private String protocol = "TLSv1.2";

    private String keyStoreFile;

    private String keyStorePassword;

    private String trustStoreFile;
    private String trustStorePassword;

    private String trustedServerSSLCertificateFile;

    static {
        // Adds *once* BouncyCastle provider as the first one, before any default JRE providers.
        SslUtils.registerBCProvider();
    }

    /**
     * Constructor
     *
     */
    @PublicAtsApi
    public FtpsClient() {

        super();
    }

    PublicAtsApi
    public boolean isImplicit() {
        return implicit;
    }

    PublicAtsApi
    public void setImplicit(boolean implicit) {
        this.implicit = implicit;
    }

    PublicAtsApi
    public String getProtocol() {
        return protocol;
    }

    PublicAtsApi
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @PublicAtsApi
    public String getTrustedServerSSLCertificateFile() {
        return trustedServerSSLCertificateFile;
    }

    @PublicAtsApi
    public void setTrustedServerSSLCertificateFile(String trustedServerSSLCertificateFile) {
        this.trustedServerSSLCertificateFile = trustedServerSSLCertificateFile;
    }

    @PublicAtsApi
    public String getTrustStoreFile() {
        return trustStoreFile;
    }

    @PublicAtsApi
    public void setTrustStoreFile(String trustStoreFile) {
        this.trustStoreFile = trustStoreFile;
    }

    @PublicAtsApi
    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    @PublicAtsApi
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    @PublicAtsApi
    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    @PublicAtsApi
    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    @PublicAtsApi
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    @PublicAtsApi
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    @PublicAtsApi
    public boolean isConnected(){
        return this.client.isConnected();
    }

    public void addProtocolCommandListener(ProtocolCommandListener listener) {
        this.client.addProtocolCommandListener(listener);
    }

    public void removeProtocolListener(ProtocolCommandListener listener) {
        this.client.removeProtocolCommandListener(listener);
    }

    /**
     * Set the {@link TransferMode} that this protocol should use
     *
     * @param mode
     * @throws FtpException
     */
    @PublicAtsApi
    @Override
    public void setTransferMode(TransferMode mode) throws FtpException {

        if (this.client != null && this.client.isConnected() && this.transferMode != mode) {
            try {
                log.info("Set file transfer mode to " + mode);
                if (mode == TransferMode.ASCII) {
                    if (!this.client.setFileType(org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE)) {
                        throw new Exception("Unable to set transfer mode to ASCII");
                    }
                } else {
                    if (!this.client.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)) {
                        throw new Exception("Unable to set transfer mode to BINARY");
                    }
                }
            } catch (Exception e) {
                throw new FtpException("Error setting file transfer mode to " + mode, e);
            }
        }

        super.setTransferMode(mode);
    }

    /**
     * Connect to a remote host using basic authentication
     *
     * @param hostname the host to connect to
     * @param userName the username
     * @param password the password for the provided username
     * @throws FtpException
     */
    @PublicAtsApi
    @Override
    public void connect(String hostname, String userName, String password) throws FtpException {

        log.info(
                "Connecting to " + hostname + " on port " + this.port + " using username " + userName + " and password " + password);

        if (!StringUtils.isNullOrEmpty(this.keyStoreFile)) {
            log.info("Keystore location set to '" + this.keyStoreFile + "'");
        }

        if (!StringUtils.isNullOrEmpty(this.trustStoreFile)) {
            log.info("Truststore location set to '" + this.trustStoreFile + "'");
        }

        if (!StringUtils.isNullOrEmpty(this.trustedServerSSLCertificateFile)) {
            log.info("Trust server certificate set to '" + this.trustedServerSSLCertificateFile + "'");
        }

        performConnect(hostname, userName, password);
    }

    public void applyCustomProperties() throws IllegalArgumentException {

        Set<Map.Entry<String, Object>> customPropertiesSet = customProperties.entrySet();
        Object value;
        boolean clientTypeIsSet = false;
        for (Map.Entry<String, Object> customPropertyEntry : customPropertiesSet) {
            value = customPropertyEntry.getValue();
            if (customPropertyEntry.getKey().equals(FTPS_CONNECTION_TYPE)) {
                clientTypeIsSet = true;
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
                    clientTypeIsSet = false;
                    throw new IllegalArgumentException(
                            "Unknown value '" + value + "' for FTPS connection type. " + "Check value used in addCustomProperty() method. Use one of the GenericFileTransferClient.FTPS_CONNECTION_TYPE__* constants for value");
                }
            } else if (customPropertyEntry.getKey().equals(FTPS_ENCRYPTION_PROTOCOLS)) {
                // currently we can set just one protocol
                String[] encryptionProtocols = parseCustomProperties(value.toString());
                protocol = encryptionProtocols[0];
            } else {
                throw new IllegalArgumentException(
                        "Unknown property with key '" + customPropertyEntry.getKey() + "' is passed. " + USE_ONE_OF_THE_FTPS_CONSTANTS);
            }
        }
        if (!clientTypeIsSet) { // explicitly set the default connection type
            log.debug("Using by default the FTPS connection type AUTH_TLS");
            implicit = false;
            protocol = "TLSv1.2";
        }

    }

    @Override
    public void connect(String hostname, String keystoreFile, String keystorePassword, String publicKeyAlias)
            throws FtpException {

        throw new FtpException("Not implemented");

    }

    /**
     * Disconnect from the remote host
     *
     * @throws FtpException
     */
    @PublicAtsApi
    @Override
    public void disconnect() throws FtpException {

        if (this.client != null && this.client.isConnected()) {
            try {
                this.client.disconnect();
                this.client = null;
            } catch (IOException e) {
                throw new FtpException(e);
            }
        }

    }

    @PublicAtsApi
    @Override
    public String executeCommand(String command) {
        return this.executeCommand(command, (InputStream) null);
    }

    @PublicAtsApi
    @Override
    public String executeCommand(String command, InputStream localData) {

        String result = null;
        try {
            if (this.passivePort == -1) {
                this.pasv();
            }
            int replyCode = this.client.sendCommand(command);
            if (replyCode == 150) { // data connection opened
                Socket dataSocket = null;
                InputStream dataInputStream;
                OutputStream dataOutputStream;
                try {
                    dataSocket = new Socket(this.client.getRemoteAddress().getHostAddress(), this.passivePort);
                    if (localData != null) {
                        dataOutputStream = dataSocket.getOutputStream();
                        IoUtils.copyStream(localData, dataOutputStream);
                        dataOutputStream.flush();
                    } else {
                        dataInputStream = dataSocket.getInputStream();
                        StringBuilder sb = new StringBuilder();
                        int i;
                        while ((i = dataInputStream.read()) != -1) {
                            sb.append((char) i);
                        }
                        result = sb.toString();
                    }
                } finally {
                    if (dataSocket != null) {
                        dataSocket.close();
                    }
                    this.passivePort = -1;
                    replyCode = this.client.getReply();
                }
            } else if (replyCode >= 300 && replyCode < 400) { // command sequence started, server waiting for further FTP commands
                return getAllReplyLinesAsString();
            } else if (replyCode >= 400) {
                throw new FileTransferException(constructExecutionErrorMessage(command, null));
            } else if (replyCode >= 200 && replyCode < 300) {
                this.passivePort = -1;
                return getAllReplyLinesAsString();
            }
        } catch (Exception e) {
            this.passivePort = -1;
            throw new FileTransferException(constructExceptionMessage(command, null), e);
        }

        return result;

    }

    /**
     * Currently not supporting commands requiring opening of data connection
     * @param command the command to run
     * @return String representing the return code
     * @throws FileTransferException
     */
    @Override
    public Object executeCommand(String command, Object[] arguments) {
        throw new FileTransferException("Not implemented. Use " + this.getClass()
                .getName() + ".executeCommand(" + String.class.getName() + ", " + InputStream.class + ") instead");
    }

    @Override
    public void logAllReplyLines() {
        log.info("REPLY: " + getAllReplyLinesAsString());
    }

    @PublicAtsApi
    @Override
    public String help() {

        try {
            this.client.help();
        } catch (Exception e) {
            throw new FtpException(constructExceptionMessage("HELP", null), e);
        }

        return getAllReplyLinesAsString();
    }

    @PublicAtsApi
    @Override
    public String pwd() {

        try {
            this.client.pwd();
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("PWD", null), e);
        }
        String result = getAllReplyLinesAsString();
        String[] tokens = result.split(" ");
        return tokens[1].replace("\"", "");
    }

    @PublicAtsApi
    @Override
    public void cwd(String directory) {

        try {
            this.client.cwd(directory);
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("CWD", null), e);
        }
    }

    @PublicAtsApi
    @Override
    public String cdup() {

        try {
            this.client.cdup();
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("CDUP", null), e);
        }
        String result = getAllReplyLinesAsString();
        String[] tokens = result.split(" ");
        return tokens[1];
    }

    @PublicAtsApi
    @Override
    public void mkd(String directory) {

        try {
            this.client.mkd(directory);
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("MKD", null), e);
        }

    }

    @PublicAtsApi
    @Override
    public void rmd(String pathName) {

        try {
            this.client.rmd(pathName);
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("RMD", null), e);
        }
    }

    @PublicAtsApi
    @Override
    public long size(String file) {

        try {
            return Long.parseLong(this.client.getSize(file));
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("SIZE", null), e);
        }

    }

    @PublicAtsApi
    @Override
    public List<String> list(String directory) {

        List<String> fileNames = new ArrayList<>();
        String result = executeCommand("LIST " + directory);
        if (StringUtils.isNullOrEmpty(result)) {
            return fileNames;
        }
        String[] tokens = result.split("\n");
        for (String token : tokens) {
            fileNames.add(token.substring(0, token.length() - 1));
        }
        return fileNames;
    }

    @PublicAtsApi
    @Override
    public List<String> listFileNames(String directory) {

        try {
            return Arrays.stream(this.client.listNames(directory)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("LIST", null), e);
        }
    }

    @PublicAtsApi
    @Override
    public String mlst(String directory) {
        try {
            if (StringUtils.isNullOrEmpty(directory)) {
                this.client.mlst();
            } else {
                this.client.mlst(directory);
            }
        } catch (Exception e) {
            throw new FtpException(constructExceptionMessage("MLST", null), e);
        }
        return getAllReplyLines()[1];
    }

    @PublicAtsApi
    @Override
    public int getLastReplyCode() {
        return this.client.getReplyCode();
    }

    @PublicAtsApi
    @Override
    public List<String> mlsd(String directory) {

        List<String> fileNames = new ArrayList<>();
        String result = executeCommand("MLSD " + directory);
        if (StringUtils.isNullOrEmpty(result)) {
            return fileNames;
        }
        String[] tokens = result.split("\n");
        for (String token : tokens) {
            fileNames.add(token.substring(0, token.length() - 1));
        }
        return fileNames;
    }

    @PublicAtsApi
    @Override
    public List<String> nlst(String directory) {

        List<String> fileNames = new ArrayList<>();
        String result = executeCommand("NLST " + directory);
        if (StringUtils.isNullOrEmpty(result)) {
            return fileNames;
        }
        String[] tokens = result.split("\n");
        for (String token : tokens) {
            fileNames.add(token.substring(0, token.length() - 1));
        }
        return fileNames;
    }

    @PublicAtsApi
    @Override
    public void appe(String file, String content) {

        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(content.getBytes());
            executeCommand("APPE " + file, bais);
        } catch (Exception e) {
            throw new RuntimeException(constructExceptionMessage("APPE ", new String[] { file }), e);
        } finally {
            IoUtils.closeStream(bais);
        }
    }

    @PublicAtsApi
    @Override
    public void dele(String file) {

        try {
            this.client.deleteFile(file);
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("DELE", null), e);
        }

    }

    @PublicAtsApi
    @Override
    public void rename(String from, String to) {

        try {
            this.client.rename(from, to);
        } catch (IOException e) {
            throw new FileTransferException(constructExceptionMessage("RENAME", null), e);
        }
    }

    @PublicAtsApi
    @Override
    public int pasv() {

        if (this.passivePort != -1) {
            log.warn("Already in passive mode");
            return this.passivePort;
        }
        try {
            int reply = this.client.pasv();
            if (reply >= 400) {
                throw new RuntimeException(constructExecutionErrorMessage("PASV", null));
            }
            this.passivePort = extractPassivePort(getAllReplyLinesAsString());
            return this.passivePort;
        } catch (Exception e) {
            throw new RuntimeException(constructExceptionMessage("PASV", null), e);
        }
    }

    @PublicAtsApi
    @Override
    public void storeFile(String localFile, String remoteDir, String remoteFile) {

        checkPausedTransferRunning(false);

        performUploadFile(localFile, remoteDir, remoteFile);
    }

    @PublicAtsApi
    @Override
    public void retrieveFile(String localFile, String remoteDir, String remoteFile) {

        checkPausedTransferRunning(false);

        performDownloadFile(localFile, remoteDir, remoteFile);
    }

    @PublicAtsApi
    public boolean isDebugMode() {

        return debugMode;
    }

    @PublicAtsApi
    public void setDebugMode(boolean turnDebug) {

        debugMode = turnDebug;
    }

    @PublicAtsApi
    @Override
    protected void performDownloadFile(String localFile, String remoteDir, String remoteFile)
            throws FileTransferException {

        FileOutputStream fos = null;
        try {
            String remoteFileAbsPath;
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
            fos = new FileOutputStream(localFile);
            if (!this.client.retrieveFile(remoteFileAbsPath, fos)) {
                throw new FileTransferException(
                        "Unable to retrieve " + remoteDir + "/" + remoteFile + " from " + this.client.getPassiveHost() + " as a" + localFile);
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
        log.info(
                "Successfully downloaded '" + localFile + "' from '" + remoteDir + remoteFile + "', host " + client.getPassiveHost());
    }

    @PublicAtsApi
    @Override
    protected String[] getAllReplyLines() {
        return this.client.getReplyStrings();
    }

    @PublicAtsApi
    @Override
    protected void performUploadFile(String localFile, String remoteDir, String remoteFile)
            throws FileTransferException {

        FileInputStream fis = null;

        try {
            String remoteFileAbsPath;
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
            fis = new FileInputStream(localFile);
            if (!this.client.storeFile(remoteFileAbsPath, fis)) {
                throw new FileTransferException(
                        "Unable to store " + localFile + " to " + this.client.getPassiveHost() + " as a " + (remoteDir.endsWith(
                                "/") ? remoteDir : remoteDir + "/") + remoteFile);
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
        log.info(
                "Successfully uploaded '" + localFile + "' to '" + remoteDir + remoteFile + "', host " + client.getPassiveHost());
    }

    @Override
    protected void finalize() throws Throwable {

        // ensure the connection is terminated
        this.disconnect();

        this.listener = null;

        super.finalize();
    }

    private void performConnect(String hostname, String userName, String password) throws FtpException {

        try {

            // make a new FTP object for every new connection
            disconnect();
            applyCustomProperties();
            SSLContext sslContext = null;

            try {
                sslContext = createSSLContext();
            } catch (Exception e) {
                throw new Exception("Error while creating SSL context", e);
            }

            this.client = new org.apache.commons.net.ftp.FTPSClient(this.implicit, sslContext);

            if (this.listener != null) {
                this.listener.setResponses(new ArrayList<>());
                this.client.addProtocolCommandListener(((FtpResponseListener) listener));
            }
            /* if debug mode is true, we log messages from all levels */
            if (isDebugMode()) {
                this.client.addProtocolCommandListener(new FtpListener());
            }

            this.client.setConnectTimeout(this.timeout);
            // connect to the host
            this.client.connect(hostname, this.port);
            // login to the host
            if (!this.client.login(userName, password)) {
                throw new Exception("Invalid username and/or password. ");
            }
            // set transfer mode
            if (this.transferMode == TransferMode.ASCII) {
                if (!this.client.setFileType(org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE)) {
                    throw new Exception("Unable to set transfer mode to ASCII");
                }
            } else {
                if (!this.client.setFileType(org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE)) {
                    throw new Exception("Unable to set transfer mode to BINARY");
                }
            }
            // initial fix - always use passive mode
            this.client.enterLocalPassiveMode();
        } catch (Exception e) {
            String errMessage = "Unable to connect to  " + hostname + " on port " + this.port + " using username " + userName + " and password " + password;
            log.error(errMessage, e);
            throw new FtpException(e);
        }
    }

    private String printCertificateInfo(X509Certificate cert) {

        StringBuilder sb = new StringBuilder();

        sb.append("  Checking SSL Server certificate :\n").append("  Subject DN: ").append(cert.getSubjectDN())
                .append("\n").append("  Signature Algorithm: ").append(cert.getSigAlgName()).append("\n")
                .append("  Valid from: ").append(cert.getNotBefore()).append("\n").append("  Valid until: ")
                .append(cert.getNotAfter()).append("\n").append("  Issuer: ").append(cert.getIssuerDN()).append("\n");

        return sb.toString();
    }

    private int extractPassivePort(String reply) {

        String[] tokens = reply.split("\\(");
        if (tokens.length == 2) {
            String[] addressTokens = tokens[1].split(",");
            if (addressTokens.length == 6) {
                int p1 = Integer.parseInt(addressTokens[4]);
                int p2 = Integer.parseInt(addressTokens[5].split("\\)")[0]);
                return (p1 * 256) + p2;
            }
        }
        throw new RuntimeException("Could not obtain passive port from reply '" + reply + "'");
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
            /* load the trust store **/
            KeyStore trustStore = SslUtils.loadKeystore(trustStoreFile, trustStorePassword);

            sslContextBuilder.loadTrustMaterial(trustStore,
                    /*
                      for better error message logging, we provide custom TrustStrategy, instead of the default one
                      */
                    new FtpsClient.DefaultTrustStrategy(trustStore));
        } else if (!StringUtils.isNullOrEmpty(trustedServerSSLCertificateFile)) {
            // load the client certificate content
            final X509Certificate trustedServerCertificate = SslUtils.convertFileToX509Certificate(
                    new File(this.trustedServerSSLCertificateFile));
            // create a trust store and add the client certificate
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null);
            trustStore.setCertificateEntry("client_certificate", trustedServerCertificate);
            // add the trust store to the SSL builder
            sslContextBuilder.loadTrustMaterial(trustStore,
                    /*
                      for better error message logging, we provide custom TrustStrategy, instead of the default one
                      */
                    new FtpsClient.DefaultTrustStrategy(trustStore));
        } else {
            // since no trust store is specified, we will trust all certificates
            sslContextBuilder.loadTrustMaterial(new FtpsClient.TrustAllTrustStrategy());
        }

        return sslContextBuilder.build();
    }


    /**
     * Trust Strategy that trusts all the certificates in the presented trust store
     * */
    class DefaultTrustStrategy implements TrustStrategy {

        private List<Certificate> certificates = new ArrayList<>();

        public DefaultTrustStrategy(KeyStore trustStore) throws Exception {
            /* get all certificates from the trust store **/
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (trustStore.isCertificateEntry(alias)) {
                    /* the alias points to a certificate **/
                    certificates.add(trustStore.getCertificate(alias));
                } else {
                    /* the alias does not point to a certificate,
                      but this may mean that it points to a private-public key pair or a certificate chain
                     */
                    Certificate certificate = trustStore.getCertificate(alias);
                    if (certificate != null) {
                        /*
                          the certificate was extracted from a private-public key entry
                          */
                        certificates.add(certificate);
                    } else {
                        /*
                          the alias points to a certificate chain
                          */
                        Certificate[] chain = trustStore.getCertificateChain(alias);
                        certificates.addAll(Arrays.asList(chain));
                    }
                }
            }
        }

        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {

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
                    }
                }

            }

            throw new CertificateException(
                    "The presented trust store certificates could not match any of the server provided ones");
        }

    }

    /**
     * TrustStrategy that trust any/all certificate/s
     * **/
    class TrustAllTrustStrategy implements TrustStrategy {

        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) {

            return true;
        }

    }
}
