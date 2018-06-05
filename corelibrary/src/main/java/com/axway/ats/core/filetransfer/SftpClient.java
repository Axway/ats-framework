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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.SshCipher;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.filetransfer.model.ftp.SftpFileTransferProgressMonitor;
import com.axway.ats.core.filetransfer.model.ftp.SynchronizationSftpTransferListener;
import com.axway.ats.core.ssh.exceptions.JschSftpClientException;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.SslUtils;
import com.axway.ats.core.utils.StringUtils;
import com.sshtools.net.SocketTransport;
import com.sshtools.publickey.ConsoleKnownHostsKeyVerification;
import com.sshtools.publickey.SshPublicKeyFileFactory;
import com.sshtools.sftp.SftpStatusException;
import com.sshtools.sftp.TransferCancelledException;
import com.sshtools.ssh.HostKeyVerification;
import com.sshtools.ssh.PasswordAuthentication;
import com.sshtools.ssh.PublicKeyAuthentication;
import com.sshtools.ssh.SshAuthentication;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.components.ComponentManager;
import com.sshtools.ssh.components.SshPublicKey;
import com.sshtools.ssh.components.jce.Ssh2RsaPrivateKey;
import com.sshtools.ssh.components.jce.SshX509RsaSha1PublicKey;
import com.sshtools.ssh2.Ssh2Client;
import com.sshtools.ssh2.Ssh2Context;

/**
 * Uses the JSch component suite for Java
 * ( http://www.jcraft.com/jsch/ ) to initiate and execute SFTP connections
 * to a remote server.
 */
public class SftpClient extends AbstractFileTransferClient {

//    private SftpFileTransferProgressMonitor     debugProgressMonitor                = null;
//    private SynchronizationSftpTransferListener synchronizationSftpTransferListener = null;

    private static final Logger                 log                                 = Logger.getLogger( SftpClient.class );

    private static final String                 USE_ONE_OF_THE_SFTP_CONSTANTS       = "Use one of the SFTP_* constatns for key and values in GenericFileTransferClient class";

    public static final String                  SFTP_USERNAME                       = "SFTP_USERNAME";
    public static final String                  SFTP_CIPHERS                        = "SFTP_CIPHERS";

    private SshConnector                        con;
    private SshClient                           ssh;
    private Ssh2Client                          ssh2;
    private com.sshtools.sftp.SftpClient        sftp;
    
    private SftpFileTransferProgressMonitor     debugProgressMonitor          = null;

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

        try {
            con = SshConnector.createInstance();
        } catch( SshException e ) {
            throw new JschSftpClientException( "Cannot create ssh connector instance.", e.getCause() );
        }
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
            debugProgressMonitor = new SftpFileTransferProgressMonitor();
        } else {
            debugProgressMonitor = null;
        }

        try {
            
            ComponentManager comManager = ComponentManager.getInstance();
            
            // FIXME check setHostKeyVerification method, right now we are using it twice
            ConsoleKnownHostsKeyVerification hostKeyVerification = new ConsoleKnownHostsKeyVerification();
            addHostKeyRepository( hostKeyVerification );
            
            con.getContext().setHostKeyVerification( hostKeyVerification );
            con.getContext().setPreferredPublicKey( Ssh2Context.PUBLIC_KEY_SSHDSS );
            con.getContext().setSocketTimeout( this.timeout );

            if( this.ciphers != null && this.ciphers.size() > 0 ) {
                StringBuilder ciphers = new StringBuilder();
                for( SshCipher cipher : this.ciphers ) {
                    ciphers.append( cipher.getSshAlgorithmName() + "," );
                    addCipherClass( cipher, comManager );
                }
                
                if( ciphers.toString().endsWith( "," ) ) {
                    ciphers.deleteCharAt( ciphers.length() - 1 );
                }
                
                con.getContext().setPreferredCipherCS( ciphers.toString() );
                con.getContext().setPreferredCipherSC( ciphers.toString() );
            }
            // TODO set logger if possible 
            //            if (this.listener != null) {
            //                this.listener.setResponses(new ArrayList<String>());
            //                JSch.setLogger((com.jcraft.jsch.Logger) this.listener);
            //            }
            
            HostKeyVerification hkv = new HostKeyVerification() {
                public boolean verifyHost( String hostname, SshPublicKey key ) {

                    try {
                        log.debug( "The connected host's key (" + key.getAlgorithm() + ") is "
                                   + key.getFingerprint() );
                    } catch( SshException e ) {}
                    return true;
                }
            };

            con.getContext().setHostKeyVerification(hkv);

            SocketTransport t = new SocketTransport( hostname, port );
            t.setTcpNoDelay( true );
            ssh = con.connect( t, username, true );

            // Authenticate the user using password authentication
            PasswordAuthentication pwd = new PasswordAuthentication();
            pwd.setPassword( password );
            if( ssh.authenticate( pwd ) == SshAuthentication.FAILED ) {
                throw new JschSftpClientException( "Cannot connect on host '" + hostname + "' with user: '"
                                                   + username + "', password: '" + password + "' and port: '"
                                                   + port + "'." );
            }
            
            if( keyStoreFile != null && !ssh.isAuthenticated() ) {
                KeyStore keyStore = SslUtils.loadKeystore( keyStoreFile, keyStorePassword );
                RSAPrivateKey prv = ( RSAPrivateKey ) keyStore.getKey( keyAlias,
                                                                       keyStorePassword.toCharArray() );
                X509Certificate x509 = ( X509Certificate ) keyStore.getCertificate( keyAlias );
                
                PublicKeyAuthentication pk = new PublicKeyAuthentication();
                pk.setPublicKey( new SshX509RsaSha1PublicKey( x509 ) );
                pk.setPrivateKey( new Ssh2RsaPrivateKey( prv ) );
                
                if( ssh.authenticate( pk ) == SshAuthentication.FAILED ) {
                    throw new JschSftpClientException( "X509 authentication failed" );
                }
            }
            
            ssh2 = ( Ssh2Client ) ssh;
            sftp = new com.sshtools.sftp.SftpClient( ssh2 );

        } catch( Exception e ) {
            throw new FileTransferException( "Unable to connect!", e );
        }
    }
    
    private void addCipherClass( SshCipher cipher,
                                 ComponentManager comManager ) throws ClassNotFoundException {

        comManager.supportedSsh2CiphersCS().add( cipher.getSshAlgorithmName(),
                                                 Class.forName( cipher.getClassName() ) );
        comManager.supportedSsh2CiphersSC().add( cipher.getSshAlgorithmName(),
                                                 Class.forName( cipher.getClassName() ) );
    }

    private void addHostKeyRepository( ConsoleKnownHostsKeyVerification hostKeyRep ) throws Exception {

        if( !StringUtils.isNullOrEmpty( this.trustStoreFile ) ) {
            KeyStore trustStore = SslUtils.loadKeystore( trustStoreFile, trustStorePassword );
            // iterate over all entries
            Enumeration<String> aliases = trustStore.aliases();
            while( aliases.hasMoreElements() ) {
                String alias = aliases.nextElement();
                if( trustStore.isCertificateEntry( alias ) ) {
                    /** the alias points to a certificate **/
                    Certificate certificate = trustStore.getCertificate( alias );
                    if( certificate != null ) {

                        byte[] opensshKeyContent = convertPubToOpenSsh( ( RSAPublicKey ) certificate.getPublicKey() );
                        SshPublicKey pubKey = SshPublicKeyFileFactory.parse( opensshKeyContent )
                                                                     .toPublicKey();
                        hostKeyRep.allowHost( hostname, pubKey, true );
                    }
                } else {
                    /** the alias does not point to a certificate, 
                     * but this may mean that it points to a private-public key pair or a certificate chain 
                     */
                    Certificate certificate = trustStore.getCertificate( alias );
                    if( certificate != null ) {
                        /**
                         * the certificate was extracted from a private-public key entry
                         * */
                        addPublicKeyToHostKeyRepostitory( certificate.getPublicKey(), hostKeyRep );
                    } else {
                        /**
                         * the alias points to a certificate chain
                         * */
                        Certificate[] chain = trustStore.getCertificateChain( alias );
                        for( Certificate cert : chain ) {
                            addPublicKeyToHostKeyRepostitory( cert.getPublicKey(), hostKeyRep );
                        }
                    }
                }
            }
        } else {
            if( StringUtils.isNullOrEmpty( this.trustedServerSSLCerfiticateFile ) ) {
                return;
            } else {
                try {
                    KeyStore trustStore = KeyStore.getInstance( "JKS" );
                    trustStore.load( null );
                    trustStore.setCertificateEntry( "cert",
                                                    SslUtils.convertFileToX509Certificate( new File( this.trustedServerSSLCerfiticateFile ) ) );
                    addPublicKeyToHostKeyRepostitory( trustStore.getCertificate( "cert" ).getPublicKey(),
                                                      hostKeyRep );
                } catch( Exception e ) {
                    throw new Exception( "Unable to add public key from certificate '"
                                         + this.trustedServerSSLCerfiticateFile + "' to known host keys", e );
                }
            }
        }

    }

    private void
            addPublicKeyToHostKeyRepostitory( PublicKey key,
                                              ConsoleKnownHostsKeyVerification hostKeyRepository ) throws Exception {

        if( !key.getAlgorithm().contains( "RSA" ) ) {
            throw new Exception( "Only RSA keys are supported!." );
        }

        byte[] encodedKey = convertPubToOpenSsh( ( RSAPublicKey ) key ) ;

        SshPublicKey pubKey = SshPublicKeyFileFactory.parse( encodedKey ).toPublicKey();
        hostKeyRepository.allowHost( hostname, pubKey, true );
    }
    
    private byte[] convertPubToOpenSsh( RSAPublicKey rsaPublicKey ) throws Exception {

        ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream( byteOs );
        dos.writeInt( "ssh-rsa".getBytes().length );
        dos.write( "ssh-rsa".getBytes() );
        dos.writeInt( rsaPublicKey.getPublicExponent().toByteArray().length );
        dos.write( rsaPublicKey.getPublicExponent().toByteArray() );
        dos.writeInt( rsaPublicKey.getModulus().toByteArray().length );
        dos.write( rsaPublicKey.getModulus().toByteArray() );
        
        String publicKeyEncoded = "ssh-rsa " + new String( Base64.encodeBase64( byteOs.toByteArray() ) );

        return publicKeyEncoded.getBytes();
    }

    @Override
    public void disconnect() throws FileTransferException {

        if( this.ssh != null && this.ssh.isConnected() ) {
            ssh.disconnect();
        }

        if( this.ssh2 != null && this.ssh2.isConnected() ) {
            ssh.disconnect();
        }

        if( this.sftp != null && !this.sftp.isClosed() ) {
            try {
                sftp.quit();
            } catch( SshException e ) {
                throw new FileTransferException( " SFTP connection cannot be closed. ", e );
            }
        }

    }

    @Override
    public String executeCommand( String command ) throws FileTransferException {

        throw new FileTransferException( "Not implemented" );
    }

    @Override
    protected void performUploadFile( String localFile, String remoteDir,
                                      String remoteFile ) throws FileTransferException {

        FileInputStream fis = null;

        try {
            String remoteFileAbsPath = null;
            remoteDir = remoteDir.replace( "\\", "/" );
            remoteFile = remoteFile.replace( "\\", "/" );

            if( remoteDir.endsWith( "/" ) && remoteFile.endsWith( "/" ) ) {
                remoteFileAbsPath = remoteDir.substring( 0, remoteDir.length() - 2 ) + remoteFile;
            } else if( !remoteDir.endsWith( "/" ) && !remoteFile.endsWith( "/" ) ) {
                remoteFileAbsPath = remoteDir + "/" + remoteFile;
            } else {
                remoteFileAbsPath = remoteDir + remoteFile;
            }
            // upload the file
            File file = new File( localFile );
            fis = new FileInputStream( file );
//            if( synchronizationSftpTransferListener != null ) {
//                this.sftp.put( fis, remoteFileAbsPath, synchronizationSftpTransferListener );
//            } else if( isDebugMode() && debugProgressMonitor != null ) {
//                debugProgressMonitor.setTransferMetadata( localFile, remoteFileAbsPath, file.length() );
//                this.sftp.put( fis, remoteFileAbsPath, debugProgressMonitor );
//            } else {
//            }
            
            if( isDebugMode() && debugProgressMonitor != null ) {
                debugProgressMonitor.setTransferMetadata( localFile, remoteFileAbsPath );
                this.sftp.put( fis, remoteFileAbsPath, debugProgressMonitor );
            } else {
                this.sftp.put( fis, remoteFileAbsPath );
            }
        } catch( TransferCancelledException e ) {
            log.error( "Unable to upload file!", e );
            throw new FileTransferException( e );
        } catch( FileNotFoundException e ) {
            log.error( "Unable to find the file that needs to be uploaded!", e );
            throw new FileTransferException( e );
        } catch( SshException e ) {
            log.error( "Ssh connectio error " + localFile, e );
            throw new FileTransferException( e );
        } catch( SftpStatusException e ) {
            log.error( "Sftp expected status error " + localFile, e );
            throw new FileTransferException( e );
        } finally {
            // close the file input stream
            IoUtils.closeStream( fis, "Unable to close the file stream after successful upload!" );
        }

        if( remoteDir != null && !remoteDir.endsWith( "/" ) ) {
            remoteDir += "/";
        }
        log.info( "Successfully uploaded '" + localFile + "' to '" + remoteDir + remoteFile + "', host "
                  + this.hostname );

    }

    @Override
    protected void performDownloadFile( String localFile, String remoteDir,
                                        String remoteFile ) throws FileTransferException {

        FileOutputStream fos = null;
        try {
            String remoteFileAbsPath = null;
            remoteDir = remoteDir.replace( "\\", "/" );
            remoteFile = remoteFile.replace( "\\", "/" );

            if( remoteDir.endsWith( "/" ) && remoteFile.endsWith( "/" ) ) {
                remoteFileAbsPath = remoteDir.substring( 0, remoteDir.length() - 2 ) + remoteFile;
            } else if( !remoteDir.endsWith( "/" ) && !remoteFile.endsWith( "/" ) ) {
                remoteFileAbsPath = remoteDir + "/" + remoteFile;
            } else {
                remoteFileAbsPath = remoteDir + remoteFile;
            }
            // download the file
            fos = new FileOutputStream( localFile );
//            File file = new File( localFile );
//            if( isDebugMode() && debugProgressMonitor != null ) {
//                debugProgressMonitor.setTransferMetadata( localFile, remoteFileAbsPath, file.length() );
//                this.sftp.get( remoteFileAbsPath, fos, debugProgressMonitor );
//            } else {
//            }
            if( isDebugMode() && debugProgressMonitor != null ) {
                debugProgressMonitor.setTransferMetadata( localFile, remoteFileAbsPath );
                this.sftp.get( remoteFileAbsPath, fos, debugProgressMonitor );
            } else {
                this.sftp.get( remoteFileAbsPath, fos );
            }
        } catch( TransferCancelledException e ) {
            log.error( "Unable to download " + localFile, e );
            throw new FileTransferException( e );
        } catch( FileNotFoundException e ) {
            log.error( "Unable to create " + localFile, e );
            throw new FileTransferException( e );
        } catch( SshException e ) {
            log.error( "Ssh connectio error " + localFile, e );
            throw new FileTransferException( e );
        } catch( SftpStatusException e ) {
            log.error( "Sftp expected status error " + localFile, e );
            throw new FileTransferException( e );
        } finally {
            // close the file output stream
            IoUtils.closeStream( fos, "Unable to close the file stream after successful download!" );
        }
        if( remoteDir != null && !remoteDir.endsWith( "/" ) ) {
            remoteDir += "/";
        }
        log.info( "Successfully downloaded '" + localFile + "' from '" + remoteDir + remoteFile + "', host "
                  + this.hostname );

    }

    @Override
    protected TransferListener addListener( int progressEventNumber ) {

        SynchronizationSftpTransferListener listener = new SynchronizationSftpTransferListener( this,
                                                                                                progressEventNumber );
//        this.synchronizationSftpTransferListener = listener;

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

        if( key.equals( SFTP_CIPHERS ) ) {
            customProperties.put( key, value );
        } else if( key.equals( SFTP_USERNAME ) ) {
            username = value.toString();
        } else {
            throw new IllegalArgumentException( "Unknown property with key '" + key + "' is passed. "
                                                + USE_ONE_OF_THE_SFTP_CONSTANTS );
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        Set<Entry<String, Object>> customPropertiesSet = customProperties.entrySet();
        Object value;
        for( Entry<String, Object> customPropertyEntry : customPropertiesSet ) {
            value = customPropertyEntry.getValue();
            if( customPropertyEntry.getKey().equals( SFTP_CIPHERS ) ) {

                if( value instanceof SshCipher ) {
                    addCipher( ( SshCipher ) value );
                } else if( value instanceof SshCipher[] ) {
                    for( SshCipher cipher : ( SshCipher[] ) value ) {
                        addCipher( cipher );
                    }
                } else {
                    throw new IllegalArgumentException( "Unsupported '" + SFTP_CIPHERS + "' value type" );
                }
            } else {
                throw new IllegalArgumentException( "Unknown property with key '"
                                                    + customPropertyEntry.getKey() + "' is passed. "
                                                    + USE_ONE_OF_THE_SFTP_CONSTANTS );
            }
        }
    }

    private void addCipher( SshCipher cipher ) {

        if( this.ciphers == null ) {
            this.ciphers = new ArrayList<SshCipher>();
        }
        log.debug( "Adding cipher " + cipher + " to the SFTP connection configuration" );
        this.ciphers.add( cipher );
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
        if( !StringUtils.isNullOrEmpty( this.trustedServerSSLCerfiticateFile ) ) {
            log.warn( "Previously set trust server certificate '" + this.trustedServerSSLCerfiticateFile
                      + "' will be overridden and only certificates from truststore '" + truststoreFile
                      + "' will be used for validation" );
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
        if( !StringUtils.isNullOrEmpty( this.trustStoreFile ) ) {
            log.warn( "Previously set trust store '" + this.trustStoreFile
                      + "' will be overridden and only the certificate '" + trustedServerSSLCerfiticateFile
                      + "' will be used for validation" );
            this.trustStoreFile = null;
            this.trustStorePassword = null;
        }
    }

    /**
     * This method exposes the underlying SFTP connection.
     * Since the implementation can be change at any time, users must not use this method directly.
     * */
    public com.sshtools.sftp.SftpClient getInternalFtpsClient() {

        return this.sftp;

    }
}
