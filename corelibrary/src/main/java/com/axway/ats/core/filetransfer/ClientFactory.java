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

import org.apache.log4j.Logger;

import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferProtocol;

/**
 * The {@link ClientFactory} returns the corresponding implementation of the {@link IFileTransferClient}
 * interface depending on the {@link TransferProtocol} supplied
 */
public class ClientFactory {

    private static Logger        log     = Logger.getLogger( ClientFactory.class );

    private static ClientFactory factory = null;

    /**
     * @return the instance of the {@link ClientFactory}
     */
    public synchronized static final ClientFactory getInstance() {

        if( factory == null ) {
            factory = new ClientFactory();
        }
        return factory;
    }

    private ClientFactory() {

        // makes this class a singleton 
    }

    /**
     * Returns the {@link IFileTransferClient} that handles transfers via the specified {@link TransferProtocol}. Since 
     * no port is provided the default port would be used when initiating the connection. 
     * 
     * @return the {@link IFileTransferClient} that handles transfers via the specified {@link TransferProtocol}
     * @param protocol the {@link TransferProtocol} to use
     * @param keystoreFile the file containing the key store
     * @param keystringPassphrase the pass phrase to the key store
     * @throws Exception
     * @see {@link TransferProtocol}
     */
    public final IFileTransferClient getClient(
                                               TransferProtocol protocol,
                                               String keystoreFile,
                                               String keystringPassphrase ) throws Exception {

        switch( protocol ){
            case FTPS:
                return new FtpsClient( protocol.getDefaultPort() );
            case HTTPS:
                return new HttpsClient( protocol.getDefaultPort() );
            default:
                throw new Exception( "No implementation for the " + protocol + " is currently available" );
        }
    }

    /**
     * Returns a product specific {@link IFileTransferClient} that handles transfers via the specified {@link TransferProtocol}.
     * 
     * @param protocol the {@link TransferProtocol} to use
     * @param port a custom port to use when connecting
     * @return the {@link IFileTransferClient} that handles transfers via the specified {@link TransferProtocol}
     * @throws FileTransferException 
     * @see {@link TransferProtocol}
     */
    public IFileTransferClient getClient(
                                         TransferProtocol protocol,
                                         int port ) throws FileTransferException {

        switch( protocol ){
            case FTP:
                return new FtpClient( port );
            case FTPS:
                return new FtpsClient( port );
            case SFTP:
                return new SftpClient( port );
            case HTTP:
                return new HttpClient( port );
            case HTTPS:
                return new HttpsClient( port );
            default:
                throw new FileTransferException( "No implementation for the " + protocol
                                                       + " protocol is currently available" );
        }
    }
    
    /**
     * Returns a product specific {@link IFileTransferClient} that handles transfers via the specified {@link TransferProtocol}.
     * 
     * @param protocol the {@link TransferProtocol} to use
     * @param port a custom port to use when connecting
     * @param customFileTransferClient the class name of the custom client
     * @return the {@link IFileTransferClient} that handles transfers via the specified {@link TransferProtocol}
     * @throws FileTransferException 
     * @see {@link TransferProtocol}
     */
    public IFileTransferClient getClient( String customFileTransferClient,
                                          int port ) throws FileTransferException {

        // load the custom client
        IFileTransferClient client = loadCustomClient( customFileTransferClient );
        client.setCustomPort( port );

        return client;
    }

    /**
     * Instantiates the custom file transfer client
     * 
     * @param customFileTransferClient the class name of the custom client
     * @return
     * @throws FileTransferException
     */
    private IFileTransferClient loadCustomClient(
                                                 String customFileTransferClient )
                                                                                  throws FileTransferException {

        try {
            // load the custom client class
            Class<?> customFileTransferClientClass = this.getClass()
                                                         .getClassLoader()
                                                         .loadClass( customFileTransferClient );

            // make an instance of the custom client
            IFileTransferClient client = ( IFileTransferClient ) customFileTransferClientClass.newInstance();

            log.info( "Loaded custom file transfer client: " + customFileTransferClient );
            return client;
        } catch( ClassNotFoundException e ) {
            throw new FileTransferException( "Custom file transfer client not found in the classpath: "
                                                   + customFileTransferClient );
        } catch( ClassCastException e ) {
            throw new FileTransferException( "Wrong type of the custom file transfer client: "
                                                   + customFileTransferClient );
        } catch( InstantiationException e ) {
            throw new FileTransferException( "Unable to create an instance of the custom file transfer client: "
                                                   + customFileTransferClient );
        } catch( IllegalAccessException e ) {
            throw new FileTransferException( "Unable to create an instance of the custom file transfer client: "
                                                   + customFileTransferClient );
        }
    }
}
