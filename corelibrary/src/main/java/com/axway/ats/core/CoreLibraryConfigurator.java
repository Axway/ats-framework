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
package com.axway.ats.core;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.AbstractConfigurator;

/**
 * The Core Library configuration class.
 * Note that this class is not meant to be used by the users directly and it does not load values from some configuration file.
 * 
 * Currently users use the available public configurators which internally pass the new values to this private configurator
 */
public class CoreLibraryConfigurator extends AbstractConfigurator {

    public static final String             FILE_TRANSFER_HTTPS_DEFAULT_ENCRYPTION_PROTOCOLS = "actionlibrary.filetransfer.https.default.encryption.protocol";
    public static final String             FILE_TRANSFER_HTTPS_DEFAULT_CIPHER_SUITES        = "actionlibrary.filetransfer.https.default.cipher.suites";

    /**
     * The singleton instance for this configurator
     */
    private static CoreLibraryConfigurator instance;

    private CoreLibraryConfigurator() {

        super();
    }

    @PublicAtsApi
    public static synchronized CoreLibraryConfigurator getInstance() {

        if( instance == null ) {
            instance = new CoreLibraryConfigurator();
        }
        instance.reloadData();

        return instance;
    }

    /**
     * Set the default HTTPS encryption protocols, for example "TLSv1.2".
     * You can specify more than one by using ',' as a delimiter
     * 
     * @param protocol the encryption protocols
     */
    @PublicAtsApi
    public void setFileTransferDefaultHttpsEncryptionProtocols(
                                                                String protocols ) {

        setTempProperty( FILE_TRANSFER_HTTPS_DEFAULT_ENCRYPTION_PROTOCOLS, protocols );
    }

    /**
     * Get the default HTTPS encryption protocols
     * 
     * @return the encryption protocols
     */
    @PublicAtsApi
    public String getFileTransferDefaultHttpsEncryptionProtocols() {

        return getOptionalProperty( FILE_TRANSFER_HTTPS_DEFAULT_ENCRYPTION_PROTOCOLS );
    }

    /**
     * Set the default HTTPS encryption cipher suites.
     * You can specify more than one by using ',' as a delimiter
     * 
     * @param protocol the cipher suites
     */
    @PublicAtsApi
    public void setFileTransferDefaultHttpsCipherSuites(
                                                         String cipherSuites ) {

        setTempProperty( FILE_TRANSFER_HTTPS_DEFAULT_CIPHER_SUITES, cipherSuites );
    }

    /**
     * Get the default HTTPS encryption cipher suites
     * 
     * @return the cipher suites
     */
    @PublicAtsApi
    public String getFileTransferDefaultHttpsCipherSuites() {

        return getOptionalProperty( FILE_TRANSFER_HTTPS_DEFAULT_CIPHER_SUITES );
    }

    @Override
    protected void reloadData() {

        // nothing to do here
    }
}
