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
package com.axway.ats.action.registry;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.registry.LocalRegistryOperations;
import com.axway.ats.core.registry.model.IRegistryOperations;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

/**
 * Operations on the Windows registry.
 * If an ATS Agent is given(by the appropriate constructor), we are working remotely.
 *
 * <br/><br/>Note: On error all methods in this class are likely to throw RegistryOperationsException
 *
 * <br/>
 * <b>User guide</b>
 * <a href="https://axway.github.io/ats-framework/Windows-Registry-Operations.html">page</a>
 * related to this class
 */
@PublicAtsApi
public class RegistryOperations {

    /**
     * HKEY_CLASSES_ROOT
     */
    public static String        HKEY_CLASSES_ROOT   = LocalRegistryOperations.HKEY_CLASSES_ROOT;

    /**
     * HKEY_CURRENT_USER
     */
    public static String        HKEY_CURRENT_USER   = LocalRegistryOperations.HKEY_CURRENT_USER;

    /**
     * HKEY_LOCAL_MACHINE
     */
    public static String        HKEY_LOCAL_MACHINE  = LocalRegistryOperations.HKEY_LOCAL_MACHINE;

    /**
     * HKEY_USERS
     */
    public static String        HKEY_USERS          = LocalRegistryOperations.HKEY_USERS;

    /**
     * HKEY_CURRENT_CONFIG
     */
    public static String        HKEY_CURRENT_CONFIG = LocalRegistryOperations.HKEY_CURRENT_CONFIG;

    private IRegistryOperations registryOperationsImpl;

    private String              rootKey;

    /**
     * Constructor when working on the local host
     *
     * @param rootKey the root key, use one of the predefined HKEY_* values
     */
    @PublicAtsApi
    public RegistryOperations( @Validate( name = "rootKey", type = ValidationType.STRING_NOT_EMPTY) String rootKey ) {

        new Validator().validateMethodParameters(new Object[]{ rootKey });

        this.rootKey = rootKey;
        this.registryOperationsImpl = getOperationsImplementationFor(null);
    }

    /**
     * Constructor when working on a remote host
     *
     * @param atsAgent the remote ATS agent
     * @param rootKey the root key, use one of the predefined HKEY_* values
     */
    @PublicAtsApi
    public RegistryOperations( @Validate( name = "atsAgent", type = ValidationType.STRING_SERVER_WITH_PORT) String atsAgent,
                               @Validate( name = "rootKey", type = ValidationType.STRING_NOT_EMPTY) String rootKey ) {

        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        new Validator().validateMethodParameters(new Object[]{ atsAgent, rootKey });

        this.rootKey = rootKey;
        this.registryOperationsImpl = getOperationsImplementationFor(atsAgent);
    }

    /**
     * Check if a key is present in the registry
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     * @return if the key exists
     */
    @PublicAtsApi
    public boolean isKeyPresent( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                                 @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName });

        return registryOperationsImpl.isKeyPresent(rootKey, keyPath, keyName);
    }

    /**
     * Get String registry value(REG_SZ)
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     * @return the key value
     */
    @PublicAtsApi
    public String getStringValue( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                                  @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName });

        return registryOperationsImpl.getStringValue(rootKey, keyPath, keyName);
    }

    /**
     * Get Integer registry value(REG_DWORD)
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     * @return the key value
     */
    @PublicAtsApi
    public int getIntValue( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                            @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName });

        return registryOperationsImpl.getIntValue(rootKey, keyPath, keyName);
    }

    /**
     * Get Long registry value(REG_QWORD)
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     * @return the key value
     */
    @PublicAtsApi
    public long getLongValue( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                              @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName });

        return registryOperationsImpl.getLongValue(rootKey, keyPath, keyName);
    }

    /**
     * Get Binary registry value(REG_BINARY)
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     * @return the key value
     */
    @PublicAtsApi
    public byte[] getBinaryValue( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                                  @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName });

        return registryOperationsImpl.getBinaryValue(rootKey, keyPath, keyName);
    }

    /**
     * Creates path in the registry.
     *
     * Will fail if the path exists already.
     *
     * If want to create a nested path(for example "Software\\MyCompany\\MyApplication\\Configuration"),
     * it is needed to call this method for each path token(first for "Software\\MyCompany", then for "Software\\MyCompany\\MyApplication" etc)
     *
     * @param keyPath path to the key
     */
    @PublicAtsApi
    public void createPath( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath });

        registryOperationsImpl.createPath(rootKey, keyPath);
    }

    /**
     * Applies a String(REG_SZ) value on the specified key.
     *
     * The key will be created if does not exist.
     * The key type will be changed if needed.
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     * @param keyValue the new key value
     */
    @PublicAtsApi
    public void setStringValue( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                                @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName,
                                @Validate( name = "keyValue", type = ValidationType.NOT_NULL) String keyValue ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName, keyValue });

        registryOperationsImpl.setStringValue(rootKey, keyPath, keyName, keyValue);
    }

    /**
     * Applies a Integer(REG_DWORD) value on the specified key.
     *
     * The key will be created if does not exist.
     * The key type will be changed if needed.
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     * @param keyValue the new key value
     */
    @PublicAtsApi
    public void setIntValue( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                             @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName,
                             @Validate( name = "keyValue", type = ValidationType.NONE) int keyValue ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName, keyValue });

        registryOperationsImpl.setIntValue(rootKey, keyPath, keyName, keyValue);
    }

    /**
     * Applies a Long(REG_QWORD) value on the specified key.
     *
     * The key will be created if does not exist.
     * The key type will be changed if needed.
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     * @param keyValue the new key value
     */
    @PublicAtsApi
    public void setLongValue( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                              @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName,
                              @Validate( name = "keyValue", type = ValidationType.NONE) long keyValue ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName, keyValue });

        registryOperationsImpl.setLongValue(rootKey, keyPath, keyName, keyValue);
    }

    /**
     * Applies a Binary(REG_BINARY) value on the specified key.
     *
     * The key will be created if does not exist.
     * The key type will be changed if needed.
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     * @param keyValue the new key value
     */
    @PublicAtsApi
    public void setBinaryValue( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                                @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName,
                                @Validate( name = "keyValue", type = ValidationType.NOT_NULL) byte[] keyValue ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName, keyValue });

        registryOperationsImpl.setBinaryValue(rootKey, keyPath, keyName, keyValue);
    }

    /**
     * Delete a registry key
     *
     * @param keyPath path to the key
     * @param keyName name of the key
     */
    @PublicAtsApi
    public void deleteKey( @Validate( name = "keyPath", type = ValidationType.STRING_NOT_EMPTY) String keyPath,
                           @Validate( name = "keyName", type = ValidationType.STRING_NOT_EMPTY) String keyName ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ keyPath, keyName });

        registryOperationsImpl.deleteKey(rootKey, keyPath, keyName);
    }

    private IRegistryOperations getOperationsImplementationFor( String atsAgent ) {

        if (HostUtils.isLocalAtsAgent(atsAgent)) {
            return new LocalRegistryOperations();
        } else {
            try {
                return new RemoteRegistryOperations(atsAgent);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create remote registry operations impl object", e);
            }
            
        }
    }
}
