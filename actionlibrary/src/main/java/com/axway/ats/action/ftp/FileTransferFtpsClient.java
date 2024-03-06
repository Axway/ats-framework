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
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.utils.StringUtils;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Set;

/**
 * The {@link FileTransferFtpsClient} uses the Apache Commons Net component suite for Java
 * ( https://commons.apache.org/proper/commons-net/ ) to initiate and execute FTPS
 * connections to a remote server. <br>
 * <br>
 * The default implementation does *not* verify the server certificate against a
 * local trusted CA store!
 */
@PublicAtsApi
public class FileTransferFtpsClient extends FileTransferFtpClient implements IFileTransferClient {
    private static final Logger log = Logger.getLogger(FileTransferFtpsClient.class);

    public FileTransferFtpsClient() {
        this.ftpClient = new FtpsClient();
    }

    @PublicAtsApi
    @Override
    public void connect(String hostname, String keystoreFile, String keystorePassword, String publicKeyAlias)
            throws FtpException {
        this.ftpClient = new FtpsClient();
        this.ftpClient.connect(hostname, keystoreFile, keystorePassword, publicKeyAlias);
    }

    /**
     * Set a client key store which will be used for authentication
     * @param keystoreFile the key store file path (Must be in JKS or PKCS12 format)
     * @param keystorePassword the key store password
     * **/
    @PublicAtsApi
    @Override
    public void setKeystore(String keystoreFile, String keystorePassword, String alias) {
        if (this.ftpClient != null) {
            FtpsClient ftpsClient = (FtpsClient) ftpClient;
            ftpsClient.setKeyStoreFile(keystoreFile);
            ftpsClient.setKeyStorePassword(keystorePassword);
        }
    }

    /**
     * Set a client trust store which will be used for validating trust server certificates
     * @param truststoreFile the trust store file path (Must be in JKS or PKCS12 format)
     * @param truststorePassword the trust store password
     *
     * <p><b>Note that call to this method will override any effect from both this method and setTrustedServerSSLCertificate</b></p>
     * **/
    @PublicAtsApi
    @Override
    public void setTrustStore(String truststoreFile, String truststorePassword) {
        if (this.ftpClient != null) {
            FtpsClient ftpsClient = (FtpsClient) ftpClient;
            ftpsClient.setTrustStoreFile(truststoreFile);
            ftpsClient.setTrustStorePassword(truststorePassword);

            // invalidate any previously set trust server certificate
            if (!StringUtils.isNullOrEmpty(ftpsClient.getTrustedServerSSLCertificateFile())) {
                log.warn(
                        "Previously set trust server certificate '" + ftpsClient.getTrustedServerSSLCertificateFile() + "' will be overridden and only certificates from truststore '" + truststoreFile + "' will be used for validation");
                ftpsClient.setTrustedServerSSLCertificateFile(null);
            }
        }
    }

    /**
     * Set a client certificate which will be used for authentication
     * @param certificateFile the trust server certificate file path (must be a .PEM file)
     *
     * <p><b>Note that call to this method will override any effect from both this method and setTrustStore</b></p>
     * **/
    @PublicAtsApi
    @Override
    public void setTrustedServerSSLCertificate(String certificateFile) {
        if (this.ftpClient != null) {
            FtpsClient ftpsClient = (FtpsClient) ftpClient;
            ftpsClient.setTrustedServerSSLCertificateFile(certificateFile);

            // invalidate any previously set trust store
            if (!StringUtils.isNullOrEmpty(ftpsClient.getTrustStoreFile())) {
                log.warn(
                        "Previously set trust store '" + ftpsClient.getTrustStoreFile() + "' will be overridden and only the certificate '" + ftpsClient.getTrustedServerSSLCertificateFile() + "' will be used for validation");
                ftpsClient.setTrustStoreFile(null);
                ftpsClient.setTrustStorePassword(null);
            }
        }
    }

    @PublicAtsApi
    @Override
    public void addCustomProperty(String key, Object value) throws IllegalArgumentException {
        if (key.equals(FtpsClient.FTPS_CONNECTION_TYPE)) {
            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException(
                        "Value '" + value + "' for property key '" + key + "' has not supported type. " + FtpsClient.USE_ONE_OF_THE_FTPS_CONSTANTS);
            } else {
                this.ftpClient.customProperties.put(key, value);
            }
        } else if (key.equals(FtpsClient.FTPS_ENCRYPTION_PROTOCOLS)) {
            this.ftpClient.customProperties.put(key, value);
        } else {
            throw new IllegalArgumentException(
                    "Unknown property with key '" + key + "' is passed. " + FtpsClient.USE_ONE_OF_THE_FTPS_CONSTANTS);
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        Set<Map.Entry<String, Object>> customPropertiesSet = this.ftpClient.customProperties.entrySet();
        Object value;
        boolean clientTypeIsSet = false;
        FtpsClient ftpsClient = (FtpsClient) ftpClient;
        for (Map.Entry<String, Object> customPropertyEntry : customPropertiesSet) {
            value = customPropertyEntry.getValue();
            if (customPropertyEntry.getKey().equals(FtpsClient.FTPS_CONNECTION_TYPE)) {
                clientTypeIsSet = true;
                if (value.equals(FtpsClient.FTPS_CONNECTION_TYPE__IMPLICIT)) {
                    log.debug("Setting FTPS connection type to IMPLICIT_SSL");
                    ftpsClient.setImplicit(true);
                    ftpsClient.setProtocol("SSL");
                } else if (value.equals(FtpsClient.FTPS_CONNECTION_TYPE__AUTH_SSL)) {
                    log.debug("Setting FTPS connection type to AUTH_SSL");
                    ftpsClient.setImplicit(false);
                    ftpsClient.setProtocol("SSL");
                } else if (value.equals(FtpsClient.FTPS_CONNECTION_TYPE__AUTH_TLS)) {
                    log.debug("Setting FTPS connection type to AUTH_TLS");
                    ftpsClient.setImplicit(false);
                    ftpsClient.setProtocol("TLSv1.2");
                } else {
                    throw new IllegalArgumentException(
                            "Unknown value '" + value + "' for FTPS connection type. " + "Check value used in addCustomProperty() method. Use one of the GenericFileTransferClient.FTPS_CONNECTION_TYPE__* constants for value");
                }
            } else if (customPropertyEntry.getKey().equals(FtpsClient.FTPS_ENCRYPTION_PROTOCOLS)) {
                // currently we can set just one protocol
                String[] encryptionProtocols = this.ftpClient.parseCustomProperties(value.toString());
                ftpsClient.setProtocol(encryptionProtocols[0]);
            } else {
                throw new IllegalArgumentException(
                        "Unknown property with key '" + customPropertyEntry.getKey() + "' is passed. " + FtpsClient.USE_ONE_OF_THE_FTPS_CONSTANTS);
            }
        }
        if (!clientTypeIsSet) { // explicitly set the default connection type
            log.debug("Using by default the FTPS connection type AUTH_TLS");
            ftpsClient.setImplicit(false);
            ftpsClient.setProtocol("TLSv1.2");
        }
    }
}
