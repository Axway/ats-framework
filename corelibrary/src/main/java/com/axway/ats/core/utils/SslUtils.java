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
package com.axway.ats.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.axway.ats.common.dbaccess.OracleKeys;

/**
* This class provide various static methods that relax X509 certificate and hostname verification while using the SSL
* over the HTTP protocol.
*/
public class SslUtils {

    private static final Logger log                                = LogManager.getLogger(SslUtils.class);

    private static final String DEFAULT_PROTOCOL                   = "TLS";

    // not lazy but not size consuming
    private static SslUtils     instance                           = new SslUtils();

    private static boolean      bcProviderAlreadyRegisteredAsFirst = false;

    /**
     * Since java 9, the default keystore type is changed to P12, instead of JKS.<br>
     * This leads to a problem, when BC provider is registered, because the JAVA_HOME/lib/security/cacerts keystore is expected to be p12, but is JKS<br>
     * So we use the Security property "keystore.type" to set the store type to JKS and save the original value in the field below
     * */
    private static String       origKeystoreType                   = null;

    private static SSLContext   trustAllSSlContext;

    // in this list are collected all created keystore files during THIS run
    private static List<String> availableKeyStores                 = new ArrayList<String>();

    /**
     * Hostname verifier.
     */
    private HostnameVerifier    hostnameVerifier;

    /**
     * Thrust managers.
     */
    private TrustManager[]      trustManagers;

    private SslUtils() {

    }

    /**
     * Set the default Hostname Verifier to an instance of a dummy class that trust all host names.
     */
    public static void trustAllHostnames() {

        // Create a verifier that does not validate host names
        if (instance.hostnameVerifier == null) {
            synchronized (instance) {
                if (instance.hostnameVerifier == null) { // if several threads had waited for entering the synchronized block
                    instance.hostnameVerifier = new DefaultHostnameVerifier();
                    // Install the all-trusting host name verifier:
                    HttpsURLConnection.setDefaultHostnameVerifier(instance.hostnameVerifier);
                }
            }
        }
    }

    /**
     * Set the default X509 Trust Manager to an instance of a dummy class that trust all certificates,
     * even the self-signed ones.
     * 
     * It will use the default protocol: TLS
     */
    public static void trustAllHttpsCertificates() {

        trustAllHttpsCertificates(DEFAULT_PROTOCOL);
    }

    /**
     * Set the default X509 Trust Manager to an instance of a dummy class that trust all certificates,
     * even the self-signed ones.
     * 
     * @param protocol e.g. TLS, TLSv1.2
     */
    public static void trustAllHttpsCertificates( String protocol ) {

        // Create a trust manager that does not validate certificate chains
        if (instance.trustManagers == null) {
            synchronized (instance) {
                if (instance.trustManagers == null) { // if several threads had waited for entering the synchronized block
                    instance.trustManagers = new TrustManager[]{ new DefaultTrustManager() };
                    // Install the all-trusting trust manager:
                    try {
                        trustAllSSlContext = SSLContext.getInstance(protocol);
                        trustAllSSlContext.init(null, instance.trustManagers, null);
                    } catch (GeneralSecurityException gse) {
                        throw new IllegalStateException(gse.getMessage());
                    }
                    HttpsURLConnection.setDefaultSSLSocketFactory(trustAllSSlContext.getSocketFactory());
                }
            }
        }
    }

    /**
     * Trust-all SSL context.
     * Optionally specify certificate file to create the keystore from.
     * 
     * It will use the default protocol: TLS
     *
     * @param certFileName
     * @param certPassword
     * @return
     */
    public static SSLContext getSSLContext( String certFileName, String certPassword ) {

        return getSSLContext(certFileName, certPassword, DEFAULT_PROTOCOL);
    }

    /**
     * Trust-all SSL context.
     * Optionally specify certificate file to create the keystore from.
     *
     * @param certFileName
     * @param certPassword
     * @param protocol e.g. TLS, TLSv1.2
     * @return
     */
    public static SSLContext getSSLContext( String certFileName, String certPassword, String protocol ) {

        SSLContext sslContext = null;
        char[] passphrase = null;
        if (certPassword != null) {
            passphrase = certPassword.toCharArray();
        }

        try {
            // First initialize the key and trust material.
            KeyStore ksKeys = KeyStore.getInstance("PKCS12");
            ksKeys.load(null);
            if (certFileName != null && certPassword != null) {
                createKeyStoreFromPemKey(certFileName, certPassword, ksKeys);
            }

            // KeyManagers decide which key material to use.
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, passphrase);

            // TrustManagers decide whether to allow connections.
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

            // Create a trust-all trust manager
            TrustManager[] trustManagers = new TrustManager[]{ new SslUtils.DefaultTrustManager() };
            tmf.init(ksKeys);

            sslContext = SSLContext.getInstance(protocol);
            sslContext.init(kmf.getKeyManagers(), trustManagers, null);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing ssl context", e);
        }

        return sslContext;
    }

    /**
     * Trust-all SSL context.
     * 
     * @return trust all hostnames and certificates {@link SSLContext} instance
     */
    public static SSLContext getTrustAllSSLContext() {

        trustAllHostnames();
        trustAllHttpsCertificates();

        return trustAllSSlContext;
    }

    private static void createKeyStoreFromPemKey(
                                                  String clientCert,
                                                  String clientPass,
                                                  KeyStore store ) throws Exception {

        try {
            // Load CA Chain file
            // CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // X509Certificate cert = (X509Certificate) cf.generateCertificate(new FileInputStream(caCert));
            store.load(null);

            // Load client's public and private keys from PKCS12 certificate
            KeyStore inputKeyStore = KeyStore.getInstance("PKCS12");
            FileInputStream fis = new FileInputStream(clientCert);
            char[] nPassword = null;
            if ( (clientPass == null) || "".equals(clientPass.trim())) {
                nPassword = null;
            } else {
                nPassword = clientPass.toCharArray();
            }
            inputKeyStore.load(fis, nPassword);
            fis.close();
            store.load(null, ( (clientPass != null)
                                                    ? clientPass.toCharArray()
                                                    : null));
            Enumeration<String> en = inputKeyStore.aliases();
            while (en.hasMoreElements()) { // we are reading just one certificate.
                String keyAlias = en.nextElement();
                if (inputKeyStore.isKeyEntry(keyAlias)) {
                    Key key = inputKeyStore.getKey(keyAlias, nPassword);
                    Certificate[] certChain = inputKeyStore.getCertificateChain(keyAlias);
                    store.setKeyEntry("outkey",
                                      key,
                                      ( (clientPass != null)
                                                             ? clientPass.toCharArray()
                                                             : null),
                                      certChain);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error creating keystore from Pem key", e);
        }
    }

    /**
     * Create keystore file 
     * 
     * @param cert the needed certificate for creating the keystore
     * @param host the host name. If not null it is appended to keystore file name
     * @param databaseName the database name. If not null it is appended to keystore file name
     * @param keyStoreFullPath the full path where the keystore file will be located
     * @param keyStoreType the type of the keystore file
     * @param keyStorePassword the the password for the keystore
     * 
     * TIP: if the keystoreFullPath, keyStoreType, keyStorePassword are empty we will set the default
     * 
     * @return Properties object with the keyStore location, type and password
     */
    public synchronized static Properties createKeyStore(
                                                          Certificate cert,
                                                          String host,
                                                          String databaseName,
                                                          String keyStoreFullPath,
                                                          String keyStoreType,
                                                          String keyStorePassword ) {

        if (StringUtils.isNullOrEmpty(keyStoreType) && StringUtils.isNullOrEmpty(keyStorePassword)
            && StringUtils.isNullOrEmpty(keyStoreFullPath)) {
            // all parameters are empty
            String tmpDir = System.getProperty("java.io.tmpdir");
            if (!tmpDir.endsWith(File.separator)) {
                tmpDir += File.separator;
            }
            keyStoreFullPath = tmpDir + "ats_TempKeyStore_" + host + "_"
                               + databaseName + ".jks";
            keyStorePassword = "password";
            keyStoreType = "JKS";
        } else if (StringUtils.isNullOrEmpty(keyStoreType) || StringUtils.isNullOrEmpty(keyStorePassword)
                   || StringUtils.isNullOrEmpty(keyStoreFullPath)) {
            // at least one parameter is empty
            throw new IllegalArgumentException("All keystore parameters should be not be empty!");
        }
        Properties props = new Properties();
        if (!availableKeyStores.contains(keyStoreFullPath)) {
            try (FileOutputStream fos = new FileOutputStream(keyStoreFullPath)) {
                KeyStore ks = KeyStore.getInstance(keyStoreType);

                // create the keystore file
                ks.load(null, keyStorePassword.toCharArray());
                ks.setCertificateEntry("certificate", cert);
                ks.store(fos, keyStorePassword.toCharArray());
                fos.close();
            } catch (Exception e) {
                throw new RuntimeException("No keystore was created!", e);
            }
            availableKeyStores.add(keyStoreFullPath);
        }
        props.put(OracleKeys.KEY_STORE_FULL_PATH, keyStoreFullPath);
        props.put(OracleKeys.KEY_STORE_TYPE, keyStoreType);
        props.put(OracleKeys.KEY_STORE_PASSWORD, keyStorePassword);

        return props;
    }

    /**
     * @param host the host
     * @param port the port
     * 
     * @return array with all server-side certificates obtained from direct socket connection
     */
    public static synchronized Certificate[] getCertificatesFromSocket( String host, String port ) {

        TrustManager[] trustAllCerts = new TrustManager[]{ new DefaultTrustManager() {} };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, Integer.valueOf(port));
            sslSocket.startHandshake();
            return sslSocket.getSession().getPeerCertificates();
        } catch (Exception e) {
            throw new RuntimeException("Could not get certificate of secure socket to " + host + ":" + port + ".!", e);
        }
    }

    /**
     * Converts a .pem or .der file to a X509Certificate.
     *
     * @param pemFile
     * @return An X509Certificate object
     * @throws Exception 
     * @throws HttpException
     */
    public static X509Certificate convertFileToX509Certificate( File pemFile ) throws Exception {

        InputStream is = null;
        try {
            is = new FileInputStream(pemFile);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            throw new Exception("Failed to convert file '" + ( (pemFile != null)
                                                                                 ? pemFile
                                                                                 : "null")
                                + "' to X509 certificate.", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * Load a keystore
     * 
     * @param keystoreFile ( Must be in JKS or PKCS12 format )
     * @param keystorePassword
     * @return
     */
    public static KeyStore loadKeystore( String keystoreFile, String keystorePassword ) {

        if (log.isDebugEnabled()) {
            log.debug("Load keystore '" + keystoreFile + "' file with '" + keystorePassword + "' password");
        }

        KeyStore keystore = null;
        try (FileInputStream fis = new FileInputStream(new File(keystoreFile))) {
            keystore = KeyStore.getInstance("JKS");
            keystore.load(fis, keystorePassword.toCharArray());
            return keystore;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error loading JKS keystore '" + keystoreFile + "' file with '" + keystorePassword
                          + "' password. Maybe its type is not JKS:\n" + e);
            }
        }

        try (FileInputStream fis = new FileInputStream(new File(keystoreFile))) {
            keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, keystorePassword.toCharArray());
            return keystore;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error loading PKCS12 keystore '" + keystoreFile + "' file with '"
                          + keystorePassword + "' password. Maybe its type is not PKCS12:\n"
                          + e);
            }
        }

        throw new RuntimeException("Error loading keystore '" + keystoreFile + "' file with '"
                                   + keystorePassword + "' password");
    }

    /**
     * Loads a public-private key pair
     * 
     * @param keystoreFile
     * @param keystorePassword
     * @param publicKeyAlias
     * @return
     */
    public static KeyPair loadKeyPair( String keystoreFile, String keystorePassword, String publicKeyAlias ) {

        KeyStore keystore = loadKeystore(keystoreFile, keystorePassword);

        PublicKey publicKey = loadPublicKey(keystore, publicKeyAlias);
        PrivateKey privateKey = loadPrivateKey(keystore, keystorePassword, publicKeyAlias);

        return new KeyPair(publicKey, privateKey);
    }

    /**
     * Load a public key
     * 
     * @param keystore
     * @param publicKeyAlias
     * @return
     */
    public static PublicKey loadPublicKey( KeyStore keystore, String publicKeyAlias ) {

        Certificate certificate;
        try {
            certificate = keystore.getCertificate(publicKeyAlias);
        } catch (KeyStoreException e) {
            throw new RuntimeException("Error loading public key for alias '" + publicKeyAlias + "'", e);
        }

        if (certificate == null) {
            throw new RuntimeException("Error loading public key for alias '" + publicKeyAlias
                                       + "': Given alias does not exist or does not contain a certificate.");
        }

        if (log.isDebugEnabled()) {
            log.debug("Loaded public key for alias '" + publicKeyAlias + "'");
        }
        return certificate.getPublicKey();
    }

    /**
     * Load a private key
     * 
     * @param keystore
     * @param keystorePassword
     * @param publicKeyAlias
     * @return
     */
    public static PrivateKey loadPrivateKey( KeyStore keystore, String keystorePassword,
                                             String publicKeyAlias ) {

        PrivateKey privateKey = null;
        try {
            privateKey = (PrivateKey) keystore.getKey(publicKeyAlias, keystorePassword.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException("Error loading private key for alias '" + publicKeyAlias + "'", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Loaded private key for alias '" + publicKeyAlias + "'");
        }
        return privateKey;
    }

    /**
     * This class implements a hostname verificator, trusting any host name.
     */
    public static class DefaultHostnameVerifier implements HostnameVerifier {

        /**
         * Always return true,
         * indicating that the host name is an acceptable match with the server's authentication scheme.
         *
         * @param hostname the host name.
         * @param session the SSL session used on the connection to host.
         * @return the true boolean value indicating the host name is trusted.
         */
        public boolean verify( String hostname, SSLSession session ) {

            return true;
        }
    }

    /**
     * This class allow any X509 certificates to be used to authenticate the remote side of a secure socket,
     * including self-signed certificates.
     */
    public static class DefaultTrustManager implements X509TrustManager {

        /**
         * Empty array of certificate authority certificates.
         */
        private static final X509Certificate[] acceptedIssuers = new X509Certificate[]{};

        /**
         * Always trust for client SSL chain peer certificate chain with any authType authentication types.
         *
         * @param chain the peer certificate chain.
         * @param authType the authentication type based on the client certificate.
         */
        public void checkClientTrusted( X509Certificate[] chain, String authType ) {

            // no exception is thrown, so everything is trusted
        }

        /**
         * Always trust for server SSL chain peer certificate chain with any authType exchange algorithm types.
         *
         * @param chain the peer certificate chain.
         * @param authType the key exchange algorithm used.
         */
        public void checkServerTrusted( X509Certificate[] chain, String authType ) {

            // no exception is thrown, so everything is trusted
        }

        /**
         * Return an empty array of certificate authority certificates which are trusted for authenticating peers.
         *
         * @return a empty array of issuer certificates.
         */
        public X509Certificate[] getAcceptedIssuers() {

            return acceptedIssuers;
        }
    }

    /**
     * Registers Bouncy Castle (BC) as <em>first security provider</em> before any other providers 
     * coming with the Java runtime. This is done once, if not already applied.
     * <br>ATS calls this method internally when it is supposed to be needed.
     * 
     * <br><br><b>Note:</b> This is a static operation. All working threads will be affected.
     * The method itself is not thread-safe.
     * 
     * <br><br><b>Note:</b> Effective set of provider is done only once per Java runtime.
     *   Currently subsequent invocations do not check whether provider is removed meanwhile and 
     *   this way could be forcefully set other security provider.  
     */
    public static void registerBCProvider() {

        if (bcProviderAlreadyRegisteredAsFirst) {
            return; // do nothing. Provider is already registered as first one.
        }

        boolean needToInsert = true;
        boolean needToRemove = false;

        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            if (providers[i].getName().equalsIgnoreCase(BouncyCastleProvider.PROVIDER_NAME)) {
                if (i == 0) {
                    needToInsert = false;
                } else {
                    needToRemove = true;
                }
                break;
            }
        }

        if (needToInsert) {
            if (needToRemove) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            }
            Provider bcProvider = new BouncyCastleProvider();
            Security.insertProviderAt(bcProvider, 1);
            bcProviderAlreadyRegisteredAsFirst = true;
            log.info("Bouncy Castle security provider is registered as first in the list of available providers");

            origKeystoreType = Security.getProperty("keystore.type");

            Security.setProperty("keystore.type", "jks");
            log.info("Default keystore type set to: JKS");
        }
    }

    /**
     * Unregisters Bouncy Castle security provider
     */
    public static void unregisterBCProvider() {

        Provider[] providers = Security.getProviders();

        for (int i = 0; i < providers.length; i++) {
            if (providers[i].getName().equalsIgnoreCase(BouncyCastleProvider.PROVIDER_NAME)) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                bcProviderAlreadyRegisteredAsFirst = false;
                log.info("Bouncy Castle security provider is unregistered from the list of available providers");

                Security.setProperty("keystore.type", "jks");
                log.info("Default keystore type revert back to: " + origKeystoreType);

                return;
            }
        }
    }
}
