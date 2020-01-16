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
package com.axway.ats.common.filetransfer;

import java.io.Serializable;

import com.axway.ats.common.PublicAtsApi;

/**
 * This class describes an SSH Cipher. Currently used in SftpClient.<br>
 * It contains some public constants of the frequently used ciphers (predefined {@link SshCipher} instances).
 */
@PublicAtsApi
public class SshCipher implements Serializable {

    private static final long     serialVersionUID = 1L;

    // a long list of SSH/SFTP supported ciphers could be found in a JceEncryptions class

    @PublicAtsApi
    public static final SshCipher AES128_CBC       = new SshCipher("aes128-cbc",
                                                                   "AES/CBC/NoPadding",
                                                                   "BC",
                                                                   16,
                                                                   16);
    @PublicAtsApi
    public static final SshCipher AES192_CBC       = new SshCipher("aes192-cbc",
                                                                   "AES/CBC/NoPadding",
                                                                   "BC",
                                                                   16,
                                                                   24);
    @PublicAtsApi
    public static final SshCipher AES256_CBC       = new SshCipher("aes256-cbc",
                                                                   "AES/CBC/NoPadding",
                                                                   "BC",
                                                                   16,
                                                                   32);
    @PublicAtsApi
    public static final SshCipher AES128_CTR       = new SshCipher("aes128-ctr",
                                                                   "AES/CTR/NoPadding",
                                                                   "BC",
                                                                   16,
                                                                   16);
    @PublicAtsApi
    public static final SshCipher AES192_CTR       = new SshCipher("aes192-ctr",
                                                                   "AES/CTR/NoPadding",
                                                                   "BC",
                                                                   16,
                                                                   24);
    @PublicAtsApi
    public static final SshCipher AES256_CTR       = new SshCipher("aes256-ctr",
                                                                   "AES/CTR/NoPadding",
                                                                   "BC",
                                                                   16,
                                                                   32);
    @PublicAtsApi
    public static final SshCipher _3DES_CBC        = new SshCipher("3des-cbc",
                                                                   "DESede/CBC/NoPadding",
                                                                   null,
                                                                   null,
                                                                   8,
                                                                   24);
    @PublicAtsApi
    public static final SshCipher BLOWFISH_CBC     = new SshCipher("blowfish-cbc",
                                                                   "Blowfish/CBC/NoPadding",
                                                                   null,
                                                                   null,
                                                                   8,
                                                                   16);
    @PublicAtsApi
    public static final SshCipher ARCFOUR128       = new SshCipher("arcfour128",
                                                                   "Arcfour/CBC/NoPadding",
                                                                   "BC",
                                                                   8,
                                                                   16);
    @PublicAtsApi
    public static final SshCipher ARCFOUR256       = new SshCipher("arcfour256",
                                                                   "Arcfour/CBC/NoPadding",
                                                                   "BC",
                                                                   8,
                                                                   32);

    /**
     * Check currently defined ciphers here (SSH RFC4253, [Page 10]): http://www.ietf.org/rfc/rfc4253.txt <br>
     * eg: 3des-cbc, blowfish-cbc, twofish256-cbc, aes256-cbc, aes192-cbc, aes128-cbc...
     *
     */
    private String                sshAlgorithmName;

    /**
     * <pre>
     * Java Cryptographic Extension (JCE) describe ciphers with transformation names.
     * A transformation name is a string that describes the operation (or set of operations) to be performed on
     * the given input to produce some output. A transformation always includes the name of a cryptographic algorithm
     * (eg. DES), and may be followed by a mode and padding scheme.
     *   "algorithm/mode/padding" or just "algorithm"
     * For example, the following are valid transformations:
     *   "DES/CBC/PKCS5Padding", "DES", "AES/CBC/NoPadding", "AES"
     *
     * Check here (Java7): http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher
     * </pre>
     */
    private String                jceAlgorithmName;

    /**
     * The JCE provider name<br>
     * For example "BC" for BouncyCastle or null for the default Sun provider
     */
    public String                 provider;

    /**
     * The cipher block length in bytes
     */
    public int                    blockLength;

    /**
     *  The algorithm key length in bytes
     */
    private int                   keyLength;

    /**
     * The Class that is implementing the current cipher. Can be skipped if the provider can handle that step directly.<br>
     * Basically if something fails, you can try setting this property.
     * */
    private Class<?>              implClass;

    /**
     *
     * @param sshAlgorithmName the SSH algorithm name
     * @param jceAlgorithmName the JCE algorithm name
     * @param blockLength the cipher block length in bytes
     * @param keyLength the algorithm key length in bytes
     */
    @PublicAtsApi
    public SshCipher( String sshAlgorithmName,
                      String jceAlgorithmName,
                      int blockLength,
                      int keyLength ) {

        this(sshAlgorithmName, jceAlgorithmName, null, null, blockLength, keyLength);
    }

    /**
    *
    * @param sshAlgorithmName the SSH algorithm name
    * @param jceAlgorithmName the JCE algorithm name
    * @param implClass the Class that implements the cipher
    * @param blockLength the cipher block length in bytes
    * @param keyLength the algorithm key length in bytes
    */
    @PublicAtsApi
    public SshCipher( String sshAlgorithmName,
                      String jceAlgorithmName,
                      Class<?> implClass,
                      int blockLength,
                      int keyLength ) {

        this(sshAlgorithmName, jceAlgorithmName, null, implClass, blockLength, keyLength);
    }

    /**
    *
    * @param sshAlgorithmName the SSH algorithm name
    * @param jceAlgorithmName the JCE algorithm name
    * @param provider the JCE provider name
    * @param blockLength the cipher block length in bytes
    * @param keyLength the algorithm key length in bytes
    */
    @PublicAtsApi
    public SshCipher( String sshAlgorithmName,
                      String jceAlgorithmName,
                      String provider,
                      int blockLength,
                      int keyLength ) {

        this(sshAlgorithmName, jceAlgorithmName, provider, null, blockLength, keyLength);
    }

    /**
     *
     * @param sshAlgorithmName the SSH algorithm name
     * @param jceAlgorithmName the JCE algorithm name
     * @param provider the JCE provider name
     * @param implClass the Class that implements the cipher
     * @param blockLength the cipher block length in bytes
     * @param keyLength the algorithm key length in bytes
     */
    @PublicAtsApi
    public SshCipher( String sshAlgorithmName,
                      String jceAlgorithmName,
                      String provider,
                      Class<?> implClass,
                      int blockLength,
                      int keyLength ) {

        this.sshAlgorithmName = sshAlgorithmName;
        this.jceAlgorithmName = jceAlgorithmName;
        this.provider = provider;
        this.blockLength = blockLength;
        this.keyLength = keyLength;
    }

    /**
     * @return the SSH algorithm name
     */
    @PublicAtsApi
    public String getSshAlgorithmName() {

        return sshAlgorithmName;
    }

    /**
     * @return the JCE algorithm name
     */
    @PublicAtsApi
    public String getJceAlgorithmName() {

        return jceAlgorithmName;
    }

    /**
     *
     * @return the JCE provider name
     */
    @PublicAtsApi
    public String getProvider() {

        return provider;
    }

    /**
     *
     * @return the cipher block length in bytes
     */
    @PublicAtsApi
    public int getBlockLength() {

        return blockLength;
    }

    /**
     * @return the key length in bytes
     */
    @PublicAtsApi
    public int getKeyLength() {

        return keyLength;
    }

    /**
     * 
     * @return the implementation class
     */
    @PublicAtsApi
    public Class<?> getImplClass() {

        return implClass;
    }

    /**
     * @param sshAlgorithmName SSH algorithm name
     */
    @PublicAtsApi
    public void setSshAlgorithmName(
                                     String sshAlgorithmName ) {

        this.sshAlgorithmName = sshAlgorithmName;
    }

    /**
     * @param jceAlgorithmName JCE algorithm name
     */
    @PublicAtsApi
    public void setJceAlgorithmName(
                                     String jceAlgorithmName ) {

        this.jceAlgorithmName = jceAlgorithmName;
    }

    /**
     *
     * @param provider the JCE provider name
     */
    @PublicAtsApi
    public void setProvider(
                             String provider ) {

        this.provider = provider;
    }

    /**
     *
     * @param blockLength the cipher block length in bytes
     */
    @PublicAtsApi
    public void setBlockLength(
                                int blockLength ) {

        this.blockLength = blockLength;
    }

    /**
     * @param keyLength the key length in bytes
     */
    @PublicAtsApi
    public void setKeyLength(
                              int keyLength ) {

        this.keyLength = keyLength;
    }

    /**
     * @param implClass the implementation class
     */
    @PublicAtsApi
    public void setImplClass( Class<?> implClass ) {

        this.implClass = implClass;
    }

    @Override
    public String toString() {

        return sshAlgorithmName + " (" + jceAlgorithmName + ", " + keyLength + " bytes)";
    }

}
