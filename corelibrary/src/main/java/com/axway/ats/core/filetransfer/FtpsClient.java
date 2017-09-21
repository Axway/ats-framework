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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.net.ProtocolCommandListener;
import org.apache.log4j.Logger;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
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
 * connections to a remote server. <br/>
 * <br/>
 * The current implementation does *not* verify the server certificate against a
 * local trusted CA store!
 */
public class FtpsClient extends AbstractFileTransferClient implements IFileTransferClient {

    private org.apache.commons.net.ftp.FTPSClient ftpsConnection                 = null;
    @SuppressWarnings("unused")
    private String                                keystorePassphrase             = null;
    @SuppressWarnings("unused")
    private String                                keystoreFile                   = null;

    private static final Logger                   log                            = Logger.getLogger( FtpsClient.class );

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

        if( this.ftpsConnection != null && this.ftpsConnection.isConnected() && this.transferMode != mode ) {
            try {
                log.info( "Set file transfer mode to " + mode );
                if( mode == TransferMode.ASCII ) {
                    if( !this.ftpsConnection.setFileType( org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE ) ) {
                        throw new Exception( "Unable to set transfer mode to ASCII" );
                    }
                } else {
                    if( !this.ftpsConnection.setFileType( org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE ) ) {
                        throw new Exception( "Unable to set transfer mode to BINARY" );
                    }
                }
            } catch( Exception e ) {
                throw new FileTransferException( "Error setting file transfer mode to " + mode, e );
            }
        }

        super.setTransferMode( mode );
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

        log.info( "Connecting to " + hostname + " on port " + this.port + " using username " + userName
                  + " and password " + password );

        performConnect( hostname, userName, password, null, null );
    }

    @Override
    public void connect(
                         String hostname,
                         String keystoreFile,
                         String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException {

        log.info( "Connecting to " + hostname + " on port " + this.port + " using keystore file "
                  + keystoreFile + " and public key alias " + publicKeyAlias );

        performConnect( hostname, publicKeyAlias, null, keystoreFile, keystorePassword );
    }

    private void performConnect(
                                 String hostname,
                                 String userName,
                                 String password,
                                 String keystoreFile,
                                 String keystorePassword ) throws FileTransferException {

        // make new FTP object for every new connection
        disconnect();
        applyCustomProperties();

        //this.ftpsConnection = new FTPSClient( this.protocol, this.implicit);
        this.ftpsConnection = new org.apache.commons.net.ftp.FTPSClient( this.implicit,
                                                                         SslUtils.getSSLContext( keystoreFile,
                                                                                                 keystorePassword,
                                                                                                 this.protocol ) );
        if( this.listener != null ) {
            this.listener.setResponses( new ArrayList<String>() );
            this.ftpsConnection.addProtocolCommandListener( ( ( FtpResponseListener ) listener ) );
        }
        /* if debug mode is true, we log messages from all levels */
        if( isDebugMode() ) {
            this.ftpsConnection.addProtocolCommandListener( new FtpListener() );
        }
        try {
            this.ftpsConnection.setConnectTimeout( this.timeout );
            // connect to the host
            this.ftpsConnection.connect( hostname, this.port );
            // login to the host
            if( !this.ftpsConnection.login( userName, password ) ) {
                throw new Exception( "Invallid username and/or password" );
            }
            // set transfer mode
            if( this.transferMode == TransferMode.ASCII ) {
                if( !this.ftpsConnection.setFileType( org.apache.commons.net.ftp.FTPSClient.ASCII_FILE_TYPE ) ) {
                    throw new Exception( "Unable to set transfer mode to ASCII" );
                }
            } else {
                if( !this.ftpsConnection.setFileType( org.apache.commons.net.ftp.FTPSClient.BINARY_FILE_TYPE ) ) {
                    throw new Exception( "Unable to set transfer mode to BINARY" );
                }
            }

        } catch( Exception e ) {
            String errMessage = "Unable to connect to  " + hostname + " on port " + this.port
                                + " using username " + userName + " and password " + password;
            log.error( errMessage, e );
            throw new FileTransferException( e );
        }
    }

    /**
     * Disconnect from the remote host
     *
     * @throws FileTransferException
     */
    @Override
    public void disconnect() throws FileTransferException {

        if( this.ftpsConnection != null && this.ftpsConnection.isConnected() ) {
            try {
                this.ftpsConnection.disconnect();
                this.ftpsConnection = null;
            } catch( IOException e ) {
                throw new FileTransferException( e );
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

            // change to correct remote directory, but save the one that the
            // client was in before that
            String currentDir = this.ftpsConnection.printWorkingDirectory();
            if( !StringUtils.isNullOrEmpty( currentDir ) ) {
                log.warn( "Unable to get the name of the current working directory." );
            }

            if( !this.ftpsConnection.changeWorkingDirectory( remoteDir ) ) {
                throw new FileTransferException( "Unable to change working directory to " + remoteDir );
            }

            // download the file
            fos = new FileOutputStream( new File( localFile ) );
            if( !this.ftpsConnection.retrieveFile( remoteFile, fos ) ) {
                throw new FileTransferException( "Unable to retrieve " + remoteDir + "/" + remoteFile
                                                 + " from " + this.ftpsConnection.getPassiveHost() + " as a"
                                                 + localFile );
            }

            // go back to the location that the client was before the upload
            // took place
            if( !this.ftpsConnection.changeWorkingDirectory( currentDir ) ) {
                throw new FileTransferException( "Unable to change working directory to " + currentDir );
            }
        } catch( Exception e ) {
            log.error( "Unable to download file " + localFile, e );
            throw new FileTransferException( e );
        } finally {
            // close the file output stream
            IoUtils.closeStream( fos, "Unable to close the file stream after successful download!" );
        }

        if( remoteDir != null && !remoteDir.endsWith( "/" ) ) {
            remoteDir += "/";
        }
        log.info( "Successfully downloaded '" + localFile + "' from '" + remoteDir + remoteFile + "', host "
                  + ftpsConnection.getPassiveHost() );
    }

    @Override
    protected void performUploadFile(
                                      String localFile,
                                      String remoteDir,
                                      String remoteFile ) throws FileTransferException {

        FileInputStream fis = null;

        try {
            // change to correct remote directory, but save the one that the
            // client was in before that
            String currentDir = this.ftpsConnection.printWorkingDirectory();

            if( !StringUtils.isNullOrEmpty( currentDir ) ) {
                log.warn( "Unable to get the name of the current working directory." );
            }

            if( !this.ftpsConnection.changeWorkingDirectory( remoteDir ) ) {
                throw new FileTransferException( "Unable to change working directory to " + remoteDir );
            }

            // upload the file
            fis = new FileInputStream( new File( localFile ) );
            if( !this.ftpsConnection.storeFile( remoteFile, fis ) ) {
                throw new FileTransferException( "Unable to store " + localFile + " to "
                                                 + this.ftpsConnection.getPassiveHost() + " as a" + remoteDir
                                                 + "/" + remoteFile );
            }

            // go back to the location that the client was before the upload
            // took place
            if( !this.ftpsConnection.changeWorkingDirectory( currentDir ) ) {
                throw new FileTransferException( "Unable to change working directory to " + currentDir );
            }
        } catch( Exception e ) {
            log.error( "Unable to upload file!", e );
            throw new FileTransferException( e );
        } finally {
            IoUtils.closeStream( fis, "Unable to close the file stream after successful upload!" );
        }

        if( remoteDir != null && !remoteDir.endsWith( "/" ) ) {
            remoteDir += "/";
        }
        log.info( "Successfully uploaded '" + localFile + "' to '" + remoteDir + remoteFile + "', host "
                  + ftpsConnection.getPassiveHost() );
    }

    @Override
    public String executeCommand(
                                  String command ) throws FileTransferException {

        log.info( "Run '" + command + "'" );
        String returnCode = "";

        try {
            this.ftpsConnection.sendCommand( command );
            returnCode = String.valueOf( this.ftpsConnection.sendCommand( command ) );
        } catch( IOException e ) {
            log.error( "Error running command: '" + command + "'", e );
            throw new FileTransferException( e );
        }

        log.info( "Return code is '" + returnCode + "'" );
        return returnCode;
    }

    @Override
    protected TransferListener addListener(
                                            int progressEventNumber ) {

        SynchronizationFtpTransferListener listener = new SynchronizationFtpTransferListener( this,
                                                                                              progressEventNumber );
        this.ftpsConnection.addProtocolCommandListener( listener );

        return listener;
    }

    @Override
    protected void removeListener(
                                   TransferListener listener ) {

        this.ftpsConnection.removeProtocolCommandListener( ( ProtocolCommandListener ) listener );

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

        if( enable ) {
            this.listener = new FtpResponseListener();
            // If it's connected add the listener to gather the responses
            if( this.ftpsConnection != null ) {
                this.ftpsConnection.addProtocolCommandListener( ( FtpResponseListener ) listener );
            }
        } else {
            // If it's connected remove the listener
            if( this.ftpsConnection != null ) {
                this.ftpsConnection.removeProtocolCommandListener( ( FtpResponseListener ) listener );
            }
            this.listener = null;
        }
    }

    @Override
    public String[] getResponses() {

        if( this.listener == null ) {
            return new String[]{};
        }

        List<String> responses = this.listener.getResponses();

        return responses.toArray( new String[responses.size()] );
    }

    @Override
    public void addCustomProperty(
                                   String key,
                                   Object value ) throws IllegalArgumentException {

        if( key.equals( FTPS_CONNECTION_TYPE ) ) {
            if( ! ( value instanceof Integer ) ) {
                throw new IllegalArgumentException( "Value '" + value + "' for property key '" + key
                                                    + "' has not supported type. "
                                                    + USE_ONE_OF_THE_FTPS_CONSTANTS );
            } else {
                customProperties.put( key, value );
            }
        } else if( key.equals( FTPS_ENCRYPTION_PROTOCOLS ) ) {
            customProperties.put( key, value );
        } else {
            throw new IllegalArgumentException( "Unknown property with key '" + key + "' is passed. "
                                                + USE_ONE_OF_THE_FTPS_CONSTANTS );
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        Set<Entry<String, Object>> customPropertiesSet = customProperties.entrySet();
        Object value;
        boolean ftpsConnectionTypeIsSet = false;
        for( Entry<String, Object> customPropertyEntry : customPropertiesSet ) {
            value = customPropertyEntry.getValue();
            if( customPropertyEntry.getKey().equals( FTPS_CONNECTION_TYPE ) ) {
                ftpsConnectionTypeIsSet = true;
                if( value.equals( FTPS_CONNECTION_TYPE__IMPLICIT ) ) {
                    log.debug( "Setting FTPS connection type to IMPLICIT_SSL" );
                    implicit = true;
                    protocol = "SSL";
                } else if( value.equals( FTPS_CONNECTION_TYPE__AUTH_SSL ) ) {
                    log.debug( "Setting FTPS connection type to AUTH_SSL" );
                    implicit = false;
                    protocol = "SSL";
                } else if( value.equals( FTPS_CONNECTION_TYPE__AUTH_TLS ) ) {
                    log.debug( "Setting FTPS connection type to AUTH_TLS" );
                    implicit = false;
                    protocol = "TLSv1.2";
                } else {
                    ftpsConnectionTypeIsSet = false;
                    throw new IllegalArgumentException( "Unknown value '"
                                                        + value
                                                        + "' for FTPS connection type. "
                                                        + "Check value used in addCustomProperty() method. Use one of the GenericFileTransferClient.FTPS_CONNECTION_TYPE__* constants for value" );
                }
            } else if( customPropertyEntry.getKey().equals( FTPS_ENCRYPTION_PROTOCOLS ) ) {
                // currently we can set just one protocol
                String[] encryptionProtocols = parseCustomProperties( value.toString() );
                protocol = encryptionProtocols[0];
            } else {
                throw new IllegalArgumentException( "Unknown property with key '" + customPropertyEntry.getKey() + "' is passed. "
                                                    + USE_ONE_OF_THE_FTPS_CONSTANTS );
            }
        }
        if( !ftpsConnectionTypeIsSet ) { // set explicitly the default connection type
            log.debug( "Using by default the FTPS connection type AUTH_TLS" );
            implicit = false;
            protocol = "TLSv1.2";
        }

    }

}
