/*
 * Copyright 2017-2023 Axway Software
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
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.model.ftp.FtpListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpResponseListener;
import com.axway.ats.core.utils.SslUtils;
import com.axway.ats.core.utils.StringUtils;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.File;
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

/**
 * The {@link FtpsClient} uses the Apache Commons Net component suite for Java
 * ( https://commons.apache.org/proper/commons-net/ ) to initiate and execute FTPS
 * connections to a remote server. <br>
 * <br>
 * The default implementation does *not* verify the server certificate against a
 * local trusted CA store!
 */
@PublicAtsApi
public class FtpsClient extends FtpClient implements IFtpClient {

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

    @PublicAtsApi
    public void setImplicit(boolean implicit) {
        this.implicit = implicit;
    }

    @PublicAtsApi
    public String getProtocol() {
        return protocol;
    }

    @PublicAtsApi
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
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    @PublicAtsApi
    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    @PublicAtsApi
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
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

    @Override
    public void connect(String hostname, String keystoreFile, String keystorePassword, String publicKeyAlias)
            throws FtpException {

        throw new FtpException("Not implemented");

    }

    private void performConnect(String hostname, String userName, String password) throws FtpException {

        try {

            // make a new FTP object for every new connection
            disconnect();
            applyCustomProperties();
            SSLContext sslContext;

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
            sslContextBuilder.loadTrustMaterial(new TrustAllTrustStrategy());
        }

        return sslContextBuilder.build();
    }

    private void applyCustomProperties() throws IllegalArgumentException {

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
    static class TrustAllTrustStrategy implements TrustStrategy {

        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) {

            return true;
        }

    }
}
