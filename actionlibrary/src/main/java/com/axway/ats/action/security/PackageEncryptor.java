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

import com.axway.ats.action.model.ActionException;
import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.Package;

public interface PackageEncryptor {

    /**
     * Encrypts the given package (like {@link MimePackage}) using the
     * arguments provided in {@link PackageEncryptor} implementation constructor
     *
     * @param sourcePackage the package to encrypt
     * @return the encrypted {@link Package}
     * @throws ActionException in case an encryption error occurs
     */
    public Package encrypt(
                            Package sourcePackage ) throws ActionException;

    /**
     * Decrypts a message (like {@link MimePackage}) using the
     * arguments provided in {@link PackageEncryptor} implementation constructor
     *
     * @param sourcePackage the package to decrypt
     * @return the decrypted {@link Package}
     * @throws ActionException in case an decryption error occurs
     */
    public Package decrypt(
                            Package sourcePackage ) throws ActionException;

    /**
     * Signs a {@link Package}.
     *
     * @param sourcePackage the package to sign
     * @return the new signed {@link Package}
     * @throws ActionException in case an error has occurred while signing the message
     */
    public Package sign(
                         Package sourcePackage ) throws ActionException;

    /**
     * Verifies the signature of a package with the public key specified in
     * the implementation constructor
     *
     * @param sourcePackage the package which signature to check
     * @return <code>true</code> if the signature can be confirmed with the current set of key pairs
     * @throws ActionException in case an error has occurred while checking the signature of the message
     */
    public boolean checkSignature(
                                   Package sourcePackage ) throws ActionException;

    /**
     * Get all aliases in a keystore with specified type
     *
     * @param keystoreType the keystore type ( JKS, PKCS12, ... )
     * @return array of all aliases in the keystore
     * @throws ActionException in case an error has occurred while reading the keystore
     */
    public String[] getKeystoreAliases(
                                        String keystoreType ) throws ActionException;
}
