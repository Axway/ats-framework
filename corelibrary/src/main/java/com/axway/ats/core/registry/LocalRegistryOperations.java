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
package com.axway.ats.core.registry;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.registry.model.IRegistryOperations;
import com.axway.ats.common.registry.RegistryOperationsException;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;

/**
 * Operations on the local host Windows registry.
 */
public class LocalRegistryOperations implements IRegistryOperations {

    private static final Logger log                 = LogManager.getLogger(LocalRegistryOperations.class);

    public static String        HKEY_CLASSES_ROOT   = "HKEY_CLASSES_ROOT";
    public static String        HKEY_CURRENT_USER   = "HKEY_CURRENT_USER";
    public static String        HKEY_LOCAL_MACHINE  = "HKEY_LOCAL_MACHINE";
    public static String        HKEY_USERS          = "HKEY_USERS";
    public static String        HKEY_CURRENT_CONFIG = "HKEY_CURRENT_CONFIG";

    public LocalRegistryOperations() {

    }

    /**
     * Check if a key is present in the registry
     *
     * @param keyPath
     * @param keyName
     * @return
     */
    @Override
    public boolean isKeyPresent(
                                 String rootKey,
                                 String keyPath,
                                 String keyName ) {

        try {
            checkKeyExists(rootKey, keyPath, keyName);
            return true;
        } catch (RuntimeException re) {
            return false;
        }
    }

    /**
     * Get String registry value(REG_SZ)
     *
     * @param keyPath
     * @param keyName
     * @return
     */
    @Override
    public String getStringValue(
                                  String rootKey,
                                  String keyPath,
                                  String keyName ) {

        checkKeyExists(rootKey, keyPath, keyName);
        try {
            return Advapi32Util.registryGetStringValue(getHKey(rootKey), keyPath, keyName);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Registry key is not of type String. "
                                                  + getDescription(rootKey, keyPath, keyName), re);
        }
    }

    /**
     * Get Integer registry value(REG_DWORD)
     *
     * @param keyPath
     * @param keyName
     * @return
     */
    @Override
    public int getIntValue(
                            String rootKey,
                            String keyPath,
                            String keyName ) {

        checkKeyExists(rootKey, keyPath, keyName);

        try {
            return Advapi32Util.registryGetIntValue(getHKey(rootKey), keyPath, keyName);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Registry key is not of type Integer. "
                                                  + getDescription(rootKey, keyPath, keyName), re);
        }
    }

    /**
     * Get Long registry value(REG_QWORD)
     *
     * @param keyPath
     * @param keyName
     * @return
     */
    @Override
    public long getLongValue(
                              String rootKey,
                              String keyPath,
                              String keyName ) {

        checkKeyExists(rootKey, keyPath, keyName);

        try {
            return Advapi32Util.registryGetLongValue(getHKey(rootKey), keyPath, keyName);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Registry key is not of type Long. "
                                                  + getDescription(rootKey, keyPath, keyName), re);
        }
    }

    /**
     * Get Binary registry value(REG_BINARY)
     *
     * @param keyPath
     * @param keyName
     * @return
     */
    @Override
    public byte[] getBinaryValue(
                                  String rootKey,
                                  String keyPath,
                                  String keyName ) {

        checkKeyExists(rootKey, keyPath, keyName);

        try {
            return Advapi32Util.registryGetBinaryValue(getHKey(rootKey), keyPath, keyName);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Registry key is not of type Binary. "
                                                  + getDescription(rootKey, keyPath, keyName), re);
        }
    }

    /**
     * Creates path in the registry.
     *
     * Will fail if the path exists already.
     *
     * If want to create a nested path(for example "Software\\MyCompany\\MyApplication\\Configuration"),
     * it is needed to call this method for each path token(first for "Software\\MyCompany", then for "Software\\MyCompany\\MyApplication" etc)
     *
     * @param keyPath
     */
    @Override
    public void createPath(
                            String rootKey,
                            String keyPath ) {

        log.info("Create regestry key path: " + getDescription(rootKey, keyPath, null));

        int index = keyPath.lastIndexOf("\\");
        if (index < 1) {
            throw new RegistryOperationsException("Invalid path '" + keyPath + "'");
        }

        String keyParentPath = keyPath.substring(0, index);
        String keyName = keyPath.substring(index + 1);

        checkPathDoesNotExist(rootKey, keyPath);

        try {
            Advapi32Util.registryCreateKey(getHKey(rootKey), keyParentPath, keyName);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Couldn't create registry key. "
                                                  + getDescription(rootKey, keyPath, keyName), re);
        }
    }

    /**
     * Applies a String(REG_SZ) value on the specified key.
     *
     * The key will be created if does not exist.
     * The key type will be changed if needed.
     *
     * @param keyPath
     * @param keyName
     * @param keyValue
     */
    @Override
    public void setStringValue(
                                String rootKey,
                                String keyPath,
                                String keyName,
                                String keyValue ) {

        log.info("Set String value '" + keyValue + "' on: " + getDescription(rootKey, keyPath, keyName));

        try {
            Advapi32Util.registrySetStringValue(getHKey(rootKey), keyPath, keyName, keyValue);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Couldn't set registry String value '" + keyValue
                                                  + "' to: "
                                                  + getDescription(rootKey, keyPath, keyName),
                                                  re);
        }
    }

    /**
     * Applies a Integer(REG_DWORD) value on the specified key.
     *
     * The key will be created if does not exist.
     * The key type will be changed if needed.
     *
     * @param keyPath
     * @param keyName
     * @param keyValue
     */
    @Override
    public void setIntValue(
                             String rootKey,
                             String keyPath,
                             String keyName,
                             int keyValue ) {

        log.info("Set Intger value '" + keyValue + "' on: " + getDescription(rootKey, keyPath, keyName));

        try {
            Advapi32Util.registrySetIntValue(getHKey(rootKey), keyPath, keyName, keyValue);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Couldn't set registry Integer value '" + keyValue
                                                  + "' to: "
                                                  + getDescription(rootKey, keyPath, keyName),
                                                  re);
        }
    }

    /**
     * Applies a Long(REG_QWORD) value on the specified key.
     *
     * The key will be created if does not exist.
     * The key type will be changed if needed.
     *
     * @param keyPath
     * @param keyName
     * @param keyValue
     */
    @Override
    public void setLongValue(
                              String rootKey,
                              String keyPath,
                              String keyName,
                              long keyValue ) {

        log.info("Set Long value '" + keyValue + "' on: " + getDescription(rootKey, keyPath, keyName));

        try {
            Advapi32Util.registrySetLongValue(getHKey(rootKey), keyPath, keyName, keyValue);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Couldn't set registry Long value '" + keyValue + "' to: "
                                                  + getDescription(rootKey, keyPath, keyName), re);
        }
    }

    /**
     * Applies a Binary(REG_BINARY) value on the specified key.
     *
     * The key will be created if does not exist.
     * The key type will be changed if needed.
     *
     * @param keyPath
     * @param keyName
     * @param keyValue
     */
    @Override
    public void setBinaryValue(
                                String rootKey,
                                String keyPath,
                                String keyName,
                                byte[] keyValue ) {

        log.info("Set Binary value '" + Arrays.toString(keyValue) + "' on: "
                 + getDescription(rootKey, keyPath, keyName));

        try {
            Advapi32Util.registrySetBinaryValue(getHKey(rootKey), keyPath, keyName, keyValue);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Couldn't set registry binary value to: "
                                                  + getDescription(rootKey, keyPath, keyName), re);
        }
    }

    /**
     * Delete a registry key
     *
     * @param keyPath
     * @param keyName
     */
    @Override
    public void deleteKey(
                           String rootKey,
                           String keyPath,
                           String keyName ) {

        log.info("Delete key: " + getDescription(rootKey, keyPath, keyName));

        try {
            Advapi32Util.registryDeleteValue(getHKey(rootKey), keyPath, keyName);
        } catch (RuntimeException re) {
            throw new RegistryOperationsException("Couldn't delete registry key. "
                                                  + getDescription(rootKey, keyPath, keyName), re);
        }
    }

    private void checkKeyExists(
                                 String rootKey,
                                 String keyPath,
                                 String keyName ) {

        try {
            WinReg.HKEY rootHKey = getHKey(rootKey);
            if (!Advapi32Util.registryValueExists(rootHKey, keyPath, keyName)) {
                throw new RegistryOperationsException("Registry key does not exist. "
                                                      + getDescription(rootKey, keyPath, keyName));
            }
        } catch (Win32Exception e) {
            throw new RegistryOperationsException("Registry key path does not exist. "
                                                  + getDescription(rootKey, keyPath, keyName), e);
        }
    }

    private void checkPathDoesNotExist(
                                        String rootKey,
                                        String keyPath ) {

        if (Advapi32Util.registryKeyExists(getHKey(rootKey), keyPath)) {
            throw new RegistryOperationsException("Registry path already exists. "
                                                  + getDescription(rootKey, keyPath, null));
        }
    }

    private WinReg.HKEY getHKey(
                                 String key ) {

        if (key.equalsIgnoreCase(HKEY_CLASSES_ROOT)) {
            return WinReg.HKEY_CLASSES_ROOT;
        } else if (key.equalsIgnoreCase(HKEY_CURRENT_USER)) {
            return WinReg.HKEY_CURRENT_USER;
        } else if (key.equalsIgnoreCase(HKEY_LOCAL_MACHINE)) {
            return WinReg.HKEY_LOCAL_MACHINE;
        } else if (key.equalsIgnoreCase(HKEY_USERS)) {
            return WinReg.HKEY_USERS;
        } else if (key.equalsIgnoreCase(HKEY_CURRENT_CONFIG)) {
            return WinReg.HKEY_CURRENT_CONFIG;
        } else {
            throw new RegistryOperationsException("Unsupported root key '" + key + "'");
        }
    }

    private String getDescription(
                                   String rootKey,
                                   String keyPath,
                                   String keyName ) {

        return "Root '" + rootKey + "', path '" + keyPath + "'"
               + (keyName != null
                                  ? ", name '" + keyName + "'"
                                  : "");
    }
}
