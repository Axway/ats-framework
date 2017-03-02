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

package com.axway.ats.action.filetransfer;

import com.axway.ats.common.filetransfer.TransferProtocol;
import com.axway.ats.config.AbstractConfigurator;
import com.axway.ats.config.exceptions.NoSuchPropertyException;

/**
 * This class is used to read configuration properties defining custom file transfer clients
 */
public class FileTransferConfigurator extends AbstractConfigurator {

    private static final String ATS_ADAPTERS_FILE = "/ats-adapters.properties";

    private static final String HTTP_FILE_TRANSFER_CLIENT  = "actionlibrary.filetransfer.http.client";
    private static final String HTTPS_FILE_TRANSFER_CLIENT = "actionlibrary.filetransfer.https.client";
    private static final String PESIT_FILE_TRANSFER_CLIENT = "actionlibrary.filetransfer.pesit.client";
    private static final String CUSTOM_FILE_TRANSFER_CLIENT = "actionlibrary.filetransfer.custom.client";
    private static final String CUSTOM_FILE_TRANSFER_PORT = "actionlibrary.filetransfer.custom.port";

    private String httpFileTransferClient;
    private String httpsFileTransferClient;
    private String pesitFileTransferClient;
    private String customFileTransferClient;

    /**
     * The singleton instance for this configurator
     */
    private static FileTransferConfigurator instance;

    static {
        // singleton effect in this way. Not lazily loaded but not so expensive resource
        instance = getNewInstance();
    }

    private FileTransferConfigurator() {

        super();
    }

    /**
     * @return an instance of this configurator
     */
    static FileTransferConfigurator getInstance() {

        return instance; // for sure not null
    }

    /**
     * @return an instance of this configurator
     */
    private static FileTransferConfigurator getNewInstance() {

        FileTransferConfigurator newInstance = new FileTransferConfigurator();

        newInstance.addConfigFileFromClassPath( ATS_ADAPTERS_FILE, true  , false);
        return newInstance;
    }

    @Override
    protected void reloadData() {

        // We load all properties as optional. Error will be thrown when a needed property is requested, but not present.
        try {
            httpFileTransferClient = getProperty( HTTP_FILE_TRANSFER_CLIENT );
        } catch( NoSuchPropertyException e ) {}

        try {
            httpsFileTransferClient = getProperty( HTTPS_FILE_TRANSFER_CLIENT );
        } catch( NoSuchPropertyException e ) {}

        try {
            pesitFileTransferClient = getProperty( PESIT_FILE_TRANSFER_CLIENT );
        } catch( NoSuchPropertyException e ) {}
        
        try {
            customFileTransferClient = getProperty( CUSTOM_FILE_TRANSFER_CLIENT );
        } catch( NoSuchPropertyException e ) {}
        
    }

    /**
     * @return the custom class for the given transfer protocol.
     * @throws FileTransferConfiguratorException if could not load the needed custom class name from the properties file.
     */
    public String getFileTransferClient(
                                         TransferProtocol protocol ) {

        switch( protocol ){
            case HTTP_CUSTOM:
                if( httpFileTransferClient == null ) {
                    throw new FileTransferConfiguratorException( "Uknown custom client for " + protocol
                                                                 + " protocol. Either " + ATS_ADAPTERS_FILE
                                                                 + " file is not in the classpath or "
                                                                 + HTTP_FILE_TRANSFER_CLIENT
                                                                 + " property is missing/empty!" );
                }
                return httpFileTransferClient;
            case HTTPS_CUSTOM:
                if( httpsFileTransferClient == null ) {
                    throw new FileTransferConfiguratorException( "Uknown custom client for " + protocol
                                                                 + " protocol. Either " + ATS_ADAPTERS_FILE
                                                                 + " file is not in the classpath or "
                                                                 + HTTPS_FILE_TRANSFER_CLIENT
                                                                 + " property is missing/empty!" );
                }
                return httpsFileTransferClient;
            case PESIT_CUSTOM:
                if( pesitFileTransferClient == null ) {
                    throw new FileTransferConfiguratorException( "Uknown custom client for " + protocol
                                                                 + " protocol. Either " + ATS_ADAPTERS_FILE
                                                                 + " file is not in the classpath or "
                                                                 + PESIT_FILE_TRANSFER_CLIENT
                                                                 + " property is missing/empty!" );
                }
                return pesitFileTransferClient;
            case CUSTOM:
            	if( customFileTransferClient == null ) {
                    throw new FileTransferConfiguratorException( "Uknown custom client for " + protocol
                                                                 + " protocol. Either " + ATS_ADAPTERS_FILE
                                                                 + " file is not in the classpath or "
                                                                 + CUSTOM_FILE_TRANSFER_CLIENT
                                                                 + " property is missing/empty!" );
                }
            	return customFileTransferClient;
            default:
                throw new FileTransferConfiguratorException( "No custom client implementation for " + protocol
                                                             + " protocol" );
        }
    }
    
    /**
     * @return the custom port for the custom transfer protocol, as specified in ats-adapters.properties.
     */
    public int getPort(){
    	try{
    		String port = getProperty( CUSTOM_FILE_TRANSFER_PORT );
    		return Integer.valueOf(port);
    	}catch (NoSuchPropertyException e) {
    		return -1;
		}
    }
}
