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

package com.axway.ats.action.security;

import java.security.cert.Certificate;
import java.util.Properties;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.SslUtils;

/**
 *  Class containing some certificate utilities methods
 */
@PublicAtsApi
public class CertificateUtilities {

    /**
     * @param host the host
     * @param port the port
     * 
     * @return array with all server-side certificates obtained from direct socket connection
     */
    @PublicAtsApi
    public static Certificate[] getSecurityCertificates(
                                                         String host,
                                                         String port ) {

        return SslUtils.getCertificatesFromSocket( host, port );
    }

    /**
     * Create keystore file 
     * 
     * @param cert the needed certificate for creating the keystore
     * @param keyStoreFullPath the full path where the keystore file will be located
     * @param keyStoreType the type of the keystore file
     * @param keyStorePassword the the password for the keystore
     * 
     * TIP: if the keystoreFullPath, keyStoreType, keyStorePassword are empty we will set the default
     * 
     * @return Properties object with the keyStore location, type and password
     */
    @PublicAtsApi
    public static Properties createKeyStoreFile(
                                                 Certificate cert,
                                                 String fullKeyStorePath,
                                                 String keyStorePassword,
                                                 String keyStoreType ) {

        return SslUtils.createKeyStore( cert, null, null, fullKeyStorePath, keyStorePassword, keyStoreType );
    }
}
