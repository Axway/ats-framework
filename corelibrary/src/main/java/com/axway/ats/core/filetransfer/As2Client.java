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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.log4j.Logger;
import org.openas2.ComponentNotFoundException;
import org.openas2.OpenAS2Exception;
import org.openas2.XMLSession;
import org.openas2.cert.CertificateFactory;
import org.openas2.cert.PKCS12CertificateFactory;
import org.openas2.message.AS2Message;
import org.openas2.message.MessageMDN;
import org.openas2.partner.AS2Partnership;
import org.openas2.partner.Partnership;
import org.openas2.partner.SecurePartnership;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.filetransfer.model.AtsAs2SenderModule;
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.SslUtils;

/**
 * The {@link As2Client} is able to run AS2 file transfers
 */
public class As2Client extends AbstractFileTransferClient implements IFileTransferClient {

    private static final Logger log                             = Logger.getLogger( As2Client.class );

    private static final String USE_ONE_OF_THE_AS2_CONSTANTS    = "Use one of the AS2_* constants for key and values in GenericFileTransferClient class";

    private static String       CONFIGRATION_FILE               = null;

    public static final String  AS2_SENDER_ID                   = "AS2_SENDER_ID";
    public static final String  AS2_SENDER_EMAIL                = "AS2_SENDER_EMAIL";
    public static final String  AS2_RECEIVER_ID                 = "AS2_RECEIVER_ID";

    public static final String  AS2_URL_SUFFIX                  = "AS2_URL_SUFFIX";

    public static final String  AS2_MESSAGE_SUBJECT             = "AS2_MESSAGE_SUBJECT";

    public static final String  AS2_KEYSTORE                    = "AS2_KEYSTORE";
    public static final String  AS2_KEYSTORE_PASSWORD           = "AS2_KEYSTORE_PASSWORD";

    public static final String  AS2_ENCRYPTION_CERT_ALIAS       = "AS2_ENCRYPTION_CERT_ALIAS";
    public static final String  AS2_ENCRYPTION_ALGORITHM        = "AS2_ENCRYPTION_ALGORITHM";
    public static final String  AS2_ENCRYPTION_ALGORITHM__3DES  = "3DES";
    public static final String  AS2_ENCRYPTION_ALGORITHM__RC2   = "RC2";

    public static final String  AS2_SIGNATURE_CERT_ALIAS        = "AS2_SIGNATURE_CERT_ALIAS";
    public static final String  AS2_SIGNATURE_HASH_METHOD       = "AS2_SIGNATURE_HASH_METHOD";
    public static final String  AS2_SIGNATURE_HASH_METHOD__MD5  = "MD5";
    public static final String  AS2_SIGNATURE_HASH_METHOD__SHA1 = "SHA1";

    public static final String  AS2_TRANSFER_OVER_TLS           = "AS2_TRANSFER_OVER_TLS";
    public static final String  AS2_TRANSFER_OVER_TLS__TRUE     = "AS2_TRANSFER_OVER_TLS__TRUE";

    public static final String  AS2_REQUEST_SYNC_MDN            = "AS2_REQUEST_SYNC_MDN";
    public static final String  AS2_REQUEST_SYNC_MDN__TRUE      = "AS2_REQUEST_SYNC_MDN__TRUE";

    private XMLSession          session;
    private Partnership         partnership;

    private String              urlSuffix;

    private String              messageSubject                  = "Test message subject";

    private String              httpUsername;
    private String              httpPassword;

    // list with MDN headers received by the last MDN(if enabled)
    private List<String>        collectedMdnHeaders             = new ArrayList<String>();

    static {
        // The OpenAS2 library we use, needs to be feed with an AS2 configuration file and an empty partnership file.
        // Here we create a temporary folder and place these files inside, so OpenAS2 can read them
        String confFileString = IoUtils.normalizeFilePath( As2Client.class.getPackage()
                                                                            .getName()
                                                                            .replace( '.', '/' )
                                                             + "/as2_configuration_file.xml" );
        String thisClassLocation = As2Client.class.getProtectionDomain()
                                                  .getCodeSource()
                                                  .getLocation()
                                                  .getPath();
        if( OperatingSystemType.getCurrentOsType().isUnix() ) {
            if( !thisClassLocation.startsWith( "/" ) ) {
                thisClassLocation = "/" + thisClassLocation;
            }
            thisClassLocation = ":" + thisClassLocation;
        }
        thisClassLocation = IoUtils.normalizeFilePath( thisClassLocation );

        InputStream confFileStream = null;
        try {
            // load the configuration file content
            if( thisClassLocation.endsWith( ".jar" ) ) {
                confFileStream = IoUtils.readFileFromJar( "file" + thisClassLocation, confFileString );
            } else {
                confFileStream = As2Client.class.getClassLoader().getResourceAsStream( confFileString );
            }

            if( confFileStream != null ) {
                // create the temporary folder if does not exist
                String as2ConfigurationTempDirString = IoUtils.normalizeDirPath( IoUtils.normalizeDirPath( AtsSystemProperties.SYSTEM_USER_TEMP_DIR )
                                                                                   + "AS2_CONFIGURATION_TEMP_DIR" );

                File as2ConfigurationTempDir = new File( as2ConfigurationTempDirString );
                if( !as2ConfigurationTempDir.exists() ) {
                    log.info( "Creating folder " + as2ConfigurationTempDir );
                    as2ConfigurationTempDir.mkdir();
                }

                // create the AS2 configuration file if does not exist
                File as2ConfigurationFile = new File( as2ConfigurationTempDirString + "config.xml" );
                if( !as2ConfigurationFile.exists() ) {
                    log.info( "Creating file " + as2ConfigurationFile );
                    OutputStream out = new FileOutputStream( as2ConfigurationFile );
                    byte buf[] = new byte[1024];
                    int len;
                    while( ( len = confFileStream.read( buf ) ) > 0 ) {
                        out.write( buf, 0, len );
                    }
                    IoUtils.closeStream( out );
                    IoUtils.closeStream( confFileStream );
                }

                // create the AS2 partnership file if does not exist
                File as2PartnershipFile = new File( as2ConfigurationTempDirString + "partnerships.xml" );
                if( !as2PartnershipFile.exists() ) {
                    log.info( "Creating file " + as2PartnershipFile );
                    BufferedWriter out = new BufferedWriter( new FileWriter( as2PartnershipFile ) );
                    out.write( "<partnerships></partnerships>" );
                    out.close();
                }
                CONFIGRATION_FILE = as2ConfigurationFile.getAbsolutePath();
            }
        } catch( Exception e ) {
            // we log the error here, the connect method will fail when called
            log.error( "Unable to create the needed configuration info for AS2 protocol transfers", e );
        }
    }

    public As2Client( int portNumber ) {

        super( portNumber );

        partnership = new Partnership();
    }

    /**
     * Connect to a remote host using basic authentication
     *
     * @param hostname
     *            the host to connect to
     * @param httpUsername
     *            user name when using basic HTTP authentication
     * @param httpPassword
     *                 user password when using basic HTTP authentication
     * @throws FileTransferException
     */
    public void connect( String hostname, String httpUsername,
                         String httpPassword ) throws FileTransferException {

        if( CONFIGRATION_FILE == null ) {
            throw new FileTransferException( "AS2 file transfers are not possible due to missing configuration file. See previous errors logged" );
        }

        // make new AS2 object for every new connection
        disconnect();

        try {
            session = new XMLSession( CONFIGRATION_FILE );
        } catch( Exception e ) {
            log.error( "Unable to connect!", e );
            throw new FileTransferException( e );
        }

        applyCustomProperties();

        this.httpUsername = httpUsername;
        this.httpPassword = httpPassword;

        if( isOverTLS() ) {
            hostname = "https://" + hostname + ":" + this.port + "/" + ( urlSuffix != null
                                                                                           ? urlSuffix
                                                                                           : "" );

            /* Next lines make life easier: We and our customers do not have to deal with certificates.
             *
             * The potential problem is that this way we stop looking at certificates for the whole VM,
             * which means if you want to simultaneously run real TLS transfer with our HttpsClient for
             * example - we will skip the certificate verification
             */
            SslUtils.trustAllHttpsCertificates();
            SslUtils.trustAllHostnames();
        } else {
            hostname = "http://" + hostname + ":" + this.port + "/" + ( urlSuffix != null
                                                                                          ? urlSuffix
                                                                                          : "" );
        }
        partnership.setAttribute( AS2Partnership.PA_AS2_URL, hostname );

        log.info( "'" + customProperties.get( AS2_SENDER_ID ) + "' sending to '"
                  + customProperties.get( AS2_RECEIVER_ID ) + "' on '" + hostname + "'" );
    }

    public void connect( String hostname, String keystoreFile, String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException {

        throw new FileTransferException( "Not implemented" );

    }

    /**
     * Disconnect from the remote host
     *
     * @throws FileTransferException
     */
    public void disconnect() throws FileTransferException {

        if( this.session != null ) {
            try {
                session.getProcessor().stopActiveModules();
                session = null;
            } catch( ComponentNotFoundException e ) {
                log.error( "Unable to disconnect", e );
                throw new FileTransferException( e );
            }
        }
    }

    @Override
    protected void performDownloadFile( String localFile, String remoteDir,
                                        String remoteFile ) throws FileTransferException {

        throw new FileTransferException( "Not implemented" );
    }

    private static int counter = 0;

    private String getUniqueSuffix() {

        return Thread.currentThread().getName() + "-" + ( ++counter );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void performUploadFile( String localFile, String remoteDir,
                                      String remoteFile ) throws FileTransferException {

        AS2Message message = new AS2Message();
        message.setMessageID( getUniqueSuffix() );
        message.setSubject( messageSubject );
        message.setPartnership( partnership );

        try {
            // the file to transfer is attached to the message
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.attachFile( new File( localFile ) );
            message.setData( mimeBodyPart );

            // send the message
            AtsAs2SenderModule senderModule = ( ( AtsAs2SenderModule ) session.getProcessor()
                                                                              .getModules()
                                                                              .get( 0 ) );
            senderModule.setHttpBasicAuthentication( this.httpUsername, this.httpPassword );
            senderModule.handle( "AS2 upload", message, new HashMap<String, String>() );

            if( isRequestingSyncMdn() ) {

                MessageMDN mdn = message.getMDN();
                if( mdn == null ) {
                    throw new FileTransferException( "Did not receive a SYNC MDN" );
                } else {
                    // we have MDN, collect its headers
                    collectedMdnHeaders.clear();

                    Enumeration<Header> headers = mdn.getHeaders().getAllHeaders();
                    while( headers.hasMoreElements() ) {
                        Header header = headers.nextElement();
                        collectedMdnHeaders.add( header.getName() + ":" + header.getValue() );
                    }
                }
            }
        } catch( IOException e ) {
            log.error( "Unable to find the file that needs to be uploaded!", e );
            throw new FileTransferException( e );
        } catch( OpenAS2Exception e ) {
            log.error( "Unable to upload file!", e );
            throw new FileTransferException( e );
        } catch( MessagingException e ) {
            log.error( "Unable to upload file!", e );
            throw new FileTransferException( e );
        }

        log.info( "Successfully uploaded '" + localFile + "' between '"
                  + customProperties.get( AS2_SENDER_ID ) + "' and " + customProperties.get( AS2_RECEIVER_ID )
                  + "'" );
    }

    @Override
    public String executeCommand( String command ) throws FileTransferException {

        throw new FileTransferException( "Not implemented" );
    }

    /**
     * @return array of all the MDN headers received by the last MDN
     */
    @Override
    public String[] getResponses() {

        return collectedMdnHeaders.toArray( new String[collectedMdnHeaders.size()] );
    }

    @Override
    protected TransferListener addListener(

                                            int progressEventNumber ) {

        return null;
    }

    @Override
    protected void removeListener( TransferListener listener ) {

    }

    @Override
    protected void finalize() throws Throwable {

        this.disconnect();

        super.finalize();
    }

    @Override
    public void addCustomProperty( String key, Object value ) throws IllegalArgumentException {

        if( key.equals( AS2_SENDER_ID ) || key.equals( AS2_SENDER_EMAIL ) || key.equals( AS2_RECEIVER_ID )
            || key.equals( AS2_URL_SUFFIX ) || key.equals( AS2_MESSAGE_SUBJECT ) || key.equals( AS2_KEYSTORE )
            || key.equals( AS2_KEYSTORE_PASSWORD ) || key.equals( AS2_ENCRYPTION_CERT_ALIAS )
            || key.equals( AS2_ENCRYPTION_ALGORITHM ) || key.equals( AS2_SIGNATURE_CERT_ALIAS )
            || key.equals( AS2_SIGNATURE_HASH_METHOD ) ) {
            customProperties.put( key, value );
        }

        else if( key.equals( AS2_TRANSFER_OVER_TLS ) ) {
            if( AS2_TRANSFER_OVER_TLS__TRUE.equals( value ) ) {
                customProperties.put( key, value );
            } else {
                throw new IllegalArgumentException( "AS2_TRANSFER_OVER_TLS property currently accepts only "
                                                    + " the value of constant AS2_TRANSFER_OVER_TLS__TRUE. "
                                                    + "The default behavior is disabled TLS. Value '" + key
                                                    + "' is not recognized. "
                                                    + USE_ONE_OF_THE_AS2_CONSTANTS );
            }
        }

        else if( key.equals( AS2_REQUEST_SYNC_MDN ) ) {
            if( AS2_REQUEST_SYNC_MDN__TRUE.equals( value ) ) {
                customProperties.put( key, value );
            } else {
                throw new IllegalArgumentException( "AS2_REQUEST_SYNC_MDN property currently accepts only "
                                                    + " the value of constant AS2_REQUEST_SYNC_MDN__TRUE. "
                                                    + "Value '" + key + "' is not recognized. "
                                                    + USE_ONE_OF_THE_AS2_CONSTANTS );
            }
        }

        else {
            throw new IllegalArgumentException( "Unknown property with key '" + key + "' is passed. "
                                                + USE_ONE_OF_THE_AS2_CONSTANTS );
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        partnership.setAttribute( AS2Partnership.PA_AS2_MDN_OPTIONS,
                                  "signed-receipt-protocol=optional,pkcs7-signature;signed-receipt-micalg=optional,sha1,md5" );

        String keystore = null;
        String keystorePassword = null;

        String encryptionCertAlias = null;
        String encryptionAlgorithm = null;

        String signatureCertAlias = null;
        String signatureHashMethod = null;

        Set<Entry<String, Object>> customPropertiesSet = customProperties.entrySet();
        String value;
        for( Entry<String, Object> customPropertyEntry : customPropertiesSet ) {
            value = customPropertyEntry.getValue().toString();
            if( customPropertyEntry.getKey().equals( AS2_SENDER_ID ) ) {
                partnership.setSenderID( AS2Partnership.PID_AS2, value );
            } else if( customPropertyEntry.getKey().equals( AS2_SENDER_EMAIL ) ) {
                partnership.setSenderID( Partnership.PID_EMAIL, value );
            } else if( customPropertyEntry.getKey().equals( AS2_RECEIVER_ID ) ) {
                partnership.setReceiverID( AS2Partnership.PID_AS2, value );
            } else if( customPropertyEntry.getKey().equals( AS2_URL_SUFFIX ) ) {
                if( value.startsWith( "/" ) ) {
                    // remove this leading character, we add it anyway implicitly
                    value = value.substring( 1 );
                }
                urlSuffix = value;
            }

            else if( customPropertyEntry.getKey().equals( AS2_MESSAGE_SUBJECT ) ) {
                messageSubject = value;
            }

            // keystore parameters, needed when encrypting/signing a message
            else if( customPropertyEntry.getKey().equals( AS2_KEYSTORE ) ) {
                keystore = value;
            } else if( customPropertyEntry.getKey().equals( AS2_KEYSTORE_PASSWORD ) ) {
                keystorePassword = value;
            }

            // parameters used to encrypt a message
            else if( customPropertyEntry.getKey().equals( AS2_ENCRYPTION_CERT_ALIAS ) ) {
                encryptionCertAlias = value;
            } else if( customPropertyEntry.getKey().equals( AS2_ENCRYPTION_ALGORITHM ) ) {
                if( value.equals( AS2_ENCRYPTION_ALGORITHM__3DES )
                    || value.equals( AS2_ENCRYPTION_ALGORITHM__RC2 ) ) {
                    encryptionAlgorithm = value;
                } else {
                    throw new IllegalArgumentException( "Unknown encryption algorithm '" + value + "'" );
                }
            }

            // parameters used to sign a message
            else if( customPropertyEntry.getKey().equals( AS2_SIGNATURE_CERT_ALIAS ) ) {
                signatureCertAlias = value;
            } else if( customPropertyEntry.getKey().equals( AS2_SIGNATURE_HASH_METHOD ) ) {
                if( value.equals( AS2_SIGNATURE_HASH_METHOD__SHA1 )
                    || value.equals( AS2_SIGNATURE_HASH_METHOD__MD5 ) ) {
                    signatureHashMethod = value;
                } else {
                    throw new IllegalArgumentException( "Unknown signature hash method '" + value + "'" );
                }
            }

            // parameter used route the transfer over TLS
            else if( customPropertyEntry.getKey().equals( AS2_TRANSFER_OVER_TLS ) ) {
                // we do nothing here, this property is needed prior to calling this method,
                // so it is used when needed
            } else if( customPropertyEntry.getKey().equals( AS2_REQUEST_SYNC_MDN ) ) {
                // we do nothing here
            } else {
                throw new IllegalArgumentException( "Unknown property with key '" + customPropertyEntry.getKey() + "' is passed. "
                                                    + USE_ONE_OF_THE_AS2_CONSTANTS );
            }
        }

        if( encryptionAlgorithm != null || signatureHashMethod != null ) {
            appplyKeystoreProperties( keystore, keystorePassword );
        }
        if( encryptionAlgorithm != null ) {
            appplyEncryptionProperties( encryptionCertAlias, encryptionAlgorithm );
        }
        if( signatureHashMethod != null ) {
            appplySignatureProperties( signatureCertAlias, signatureHashMethod );
        }
    }

    private void appplyKeystoreProperties( String encryptionKeystore,
                                           String encryptionKeystorePassword ) throws IllegalArgumentException {

        if( encryptionKeystore == null ) {
            throw new IllegalArgumentException( "Cannot encrypt/sign as keystore file is not specified" );
        }
        if( encryptionKeystorePassword == null ) {
            throw new IllegalArgumentException( "Cannot encrypt/sign as keystore file password is not specified" );
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put( PKCS12CertificateFactory.PARAM_FILENAME, encryptionKeystore );
        map.put( PKCS12CertificateFactory.PARAM_PASSWORD, encryptionKeystorePassword );
        PKCS12CertificateFactory certFactory = new PKCS12CertificateFactory();
        try {
            certFactory.init( session, map );
        } catch( OpenAS2Exception e ) {
            throw new IllegalArgumentException( "Error initializing PKCS#12 certificate factory", e );
        }
        session.setComponent( CertificateFactory.COMPID_CERTIFICATE_FACTORY, certFactory );

    }

    private void appplyEncryptionProperties( String encryptionCertAlias,
                                             String encryptionAlgorithm ) throws IllegalArgumentException {

        if( encryptionCertAlias == null ) {
            throw new IllegalArgumentException( "Cannot encrypt as certificate alias is not specified" );
        }

        if( encryptionAlgorithm == null ) {
            throw new IllegalArgumentException( "Cannot encrypt as encryption algorithm is not specified" );
        }

        partnership.setReceiverID( SecurePartnership.PID_X509_ALIAS, encryptionCertAlias );
        partnership.setAttribute( SecurePartnership.PA_ENCRYPT, encryptionAlgorithm );
    }

    private void appplySignatureProperties( String signatureCertAlias,
                                            String signatureHashMethod ) throws IllegalArgumentException {

        if( signatureCertAlias == null ) {
            throw new IllegalArgumentException( "Cannot sign as signature alias is not specified" );
        }

        if( signatureHashMethod == null ) {
            throw new IllegalArgumentException( "Cannot sign as signature hash method is not specified" );
        }

        partnership.setSenderID( SecurePartnership.PID_X509_ALIAS, signatureCertAlias );
        partnership.setAttribute( SecurePartnership.PA_SIGN, signatureHashMethod );
    }

    private boolean isOverTLS() {

        boolean isOverTLS = false;
        if( customProperties.containsKey( AS2_TRANSFER_OVER_TLS ) ) {
            String isOverTLSString = customProperties.get( AS2_TRANSFER_OVER_TLS ).toString();
            isOverTLS = isOverTLSString.equals( AS2_TRANSFER_OVER_TLS__TRUE );
        }

        return isOverTLS;
    }

    private boolean isRequestingSyncMdn() {

        // we do not need to check the value for this key, as currently it is just one acceptable value
        return customProperties.containsKey( AS2_REQUEST_SYNC_MDN );
    }
}
