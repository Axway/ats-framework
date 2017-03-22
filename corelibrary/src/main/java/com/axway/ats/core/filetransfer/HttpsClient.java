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

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import com.axway.ats.core.CoreLibraryConfigurator;
import com.axway.ats.core.utils.SslUtils;
import com.axway.ats.common.filetransfer.FileTransferException;

/**
 * The {@link HttpClient} uses the Apache HTTP Components modules to initiate and execute
 * HTTPS connections to a remote server.
 */
public class HttpsClient extends HttpClient {

    private static final int    DEFAULT_HTTPS_PORT             = 443;

    private static final String USE_ONE_OF_THE_HTTPS_CONSTANTS = "Use one of the HTTPS_* constants for key and values in GenericFileTransferClient class";

    public static final String  HTTPS_ENCRYPTION_PROTOCOLS     = "HTTPS_ENCRYPTION_PROTOCOLS";
    private String[]            encryptionProtocols;

    public static final String  HTTPS_CIPHER_SUITES            = "HTTPS_CIPHER_SUITES";
    private String[]            cipherSuites;

    /**
     * Constructor. By default each connection is closed so no pooling is used.<br/>
     * The default port number 443 will be used
     */
    public HttpsClient() {

        this( DEFAULT_HTTPS_PORT );
    }

    /**
     * Constructor. By default each connection is closed so no pooling is used.
     *
     * @param portNumber the port number to use when connecting
     */
    public HttpsClient( int portNumber ) {

        super( portNumber );
    }

    /**
     * Connect to a remote host.
     *
     * @param hostname the host to connect to
     * @throws FileTransferException
     */
    public void connect(
                         String hostname ) throws FileTransferException {

        connect( hostname, null, null );
    }

    /**
     * Connect to a remote host using basic authentication.
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

        super.connect( hostname, userName, password );

        // trust everybody
        try {
            SSLContext sslContext = SslUtils.getTrustAllSSLContext();

            SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(sslContext, encryptionProtocols,
                    cipherSuites, new NoopHostnameVerifier());

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", ssf).build();

            HttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                    socketFactoryRegistry);
            this.httpBuilder.setConnectionManager( connectionManager )
                            .setSchemePortResolver( new DefaultSchemePortResolver() );

            this.httpClient = this.httpBuilder.build();
        } catch (Exception e) {
            throw new FileTransferException("Error setting trust manager", e);
        }
    }

    @Override
    public void connect(
                         String hostname,
                         String keystoreFile,
                         String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException {

        super.connect( hostname, null, null );
        // if server is old or client is with JDK < 1.6.0_22
        //java.lang.System.setProperty( "sun.security.ssl.allowUnsafeRenegotiation", "true" );

        // load keystore
        KeyStore keyStore = SslUtils.loadKeystore( keystoreFile, keystorePassword );
        log.debug( "Keystore " + keystoreFile + " opened successfully." );
        TrustStrategy trustStrategy = new TrustStrategy() {

            public boolean isTrusted(
                                      X509Certificate[] chain,
                                      String authType ) throws CertificateException {

                log.debug( "Always trust strategy invoked for certificate chain conaining: " + chain[0]
                           + "| auth: " + authType );
                return true;
            }
        };
        SSLConnectionSocketFactory sslSocketFactory;
        try {
            SSLContext sslContext = new SSLContextBuilder()
                    .loadKeyMaterial(keyStore, keystorePassword.toCharArray())
                    .loadTrustMaterial(trustStrategy).useProtocol("TLS").build();

            sslSocketFactory = new SSLConnectionSocketFactory(sslContext,
                    new NoopHostnameVerifier() );
        } catch( GeneralSecurityException ex ) {
            throw new FileTransferException( "Could not initialize SSL socket factory. Check concrete datails in exception cause",
                                                   ex );
        }
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslSocketFactory).build();

        HttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        this.httpBuilder.setSSLSocketFactory(sslSocketFactory)
        .setConnectionManager(connectionManager)
        .setSchemePortResolver(new DefaultSchemePortResolver());

        this.httpClient = this.httpBuilder.build();
    }

    @Override
    protected String constructDownloadUrl(
                                           String remoteDir,
                                           String remoteFile ) {

        return "https://" + this.hostname + ":" + this.port + getPathPlusFile(remoteDir, remoteFile);
    }

    @Override
    protected String constructGetUrl(
                                      String requestedHostRelativeUrl ) {

        return "https://" + this.hostname + ":" + this.port + requestedHostRelativeUrl;
    }

    @Override
    protected String constructUploadUrl(
                                         String remoteDir,
                                         String remoteFile ) {

        return "https://" + this.hostname + ":" + this.port + getPathPlusFile(remoteDir, remoteFile);
    }

    @Override
    public void addCustomProperty(
                                   String key,
                                   Object value ) throws IllegalArgumentException {

        try {
            // first let the parent consume this property
            super.addCustomProperty( key, value );
            // the parent consumed this property
            return;
        } catch( IllegalArgumentException iae ) {
            // the parent did not recognize this property, so it consumed here
            if( HTTPS_ENCRYPTION_PROTOCOLS.equals( key ) || HTTPS_CIPHER_SUITES.equals( key ) ) {
                customProperties.put( key, value );
            } else {
                throw new IllegalArgumentException( "Unknown property with key '" + key + "' is passed. "
                                                    + USE_ONE_OF_THE_HTTPS_CONSTANTS );
            }
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        super.applyCustomProperties();

        // get the encryption protocol
        Object encryptionProtocolsValue = customProperties.get( HTTPS_ENCRYPTION_PROTOCOLS );
        if( encryptionProtocolsValue == null ) {
            // user did not specify value for this instance, use default value if present
            encryptionProtocolsValue = CoreLibraryConfigurator.getInstance()
                                                              .getFileTransferDefaultHttpsEncryptionProtocols();
        }
        if( encryptionProtocolsValue != null && encryptionProtocolsValue.toString().trim().length() > 0 ) {
            encryptionProtocols = parseCustomProperties( encryptionProtocolsValue.toString() );
            log.debug( "HTTPS encryption protocol set to '" + encryptionProtocolsValue.toString() + "'" );
        } else {
            encryptionProtocolsValue = null;
        }

        // get the encryption cipher suites
        Object cipherSuitesValue = customProperties.get( HTTPS_CIPHER_SUITES );
        if( cipherSuitesValue == null ) {
            // user did not specify value for this instance, use default value if present
            cipherSuitesValue = CoreLibraryConfigurator.getInstance()
                                                       .getFileTransferDefaultHttpsCipherSuites();
        }
        if( cipherSuitesValue != null && cipherSuitesValue.toString().trim().length() > 0 ) {
            cipherSuites = parseCustomProperties( cipherSuitesValue.toString() );
            log.debug( "HTTPS cipher suites set to '" + cipherSuitesValue.toString() + "'" );
        } else {
            cipherSuitesValue = null;
        }
    }
}
