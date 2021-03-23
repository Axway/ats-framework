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

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.util.Store;

import com.axway.ats.action.model.ActionException;
import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.Package;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.SslUtils;
import com.sun.mail.util.BASE64DecoderStream;

public class SMimePackageEncryptor implements PackageEncryptor {

    private static final Logger  LOG                           = LogManager.getLogger(SMimePackageEncryptor.class);

    public static final String   CONTENT_TYPE_MULTIPART_SIGNED = "multipart/signed";

    public static final String   JKS_KEYSTORE_TYPE             = "JKS";
    public static final String   PKCS12_KEYSTORE_TYPE          = "PKCS12";

    private static final String  DECRYPTION_EXCEPTION          = "Unable to decrypt message";
    private static final String  ENCRYPTION_EXCEPTION          = "Unable to encrypt message";
    private static final String  EXCEPTION_WHILE_SIGNING       = "Unable to sign message";
    private static final String  SIGNATURE_EXCEPTION           = "Unable to check message signature";

    private List<String>         certLocations                 = new ArrayList<String>();
    private List<String>         certPasswords                 = new ArrayList<String>();
    private List<String>         aliasOrCNs                    = new ArrayList<String>();

    private String               certLocation;
    private String               certPassword;
    private String               aliasOrCN;

    private ASN1ObjectIdentifier encryptionCipher              = null;
    private String               signatureAlgorithm            = null;

    @PublicAtsApi
    public static class Cipher {

        @PublicAtsApi
        public static final String AES128_CBC      = SMIMEEnvelopedGenerator.AES128_CBC;
        @PublicAtsApi
        public static final String AES192_CBC      = SMIMEEnvelopedGenerator.AES192_CBC;
        @PublicAtsApi
        public static final String AES256_CBC      = SMIMEEnvelopedGenerator.AES256_CBC;
        @PublicAtsApi
        public static final String TRIPLE_DES      = SMIMEEnvelopedGenerator.DES_EDE3_CBC;
        @PublicAtsApi
        public static final String CAMELLIA128_CBC = SMIMEEnvelopedGenerator.CAMELLIA128_CBC;
        @PublicAtsApi
        public static final String CAMELLIA192_CBC = SMIMEEnvelopedGenerator.CAMELLIA192_CBC;
        @PublicAtsApi
        public static final String CAMELLIA256_CBC = SMIMEEnvelopedGenerator.CAMELLIA256_CBC;
        @PublicAtsApi
        public static final String CAST5_CBC       = SMIMEEnvelopedGenerator.CAST5_CBC;
        @PublicAtsApi
        public static final String RC2_CBC         = SMIMEEnvelopedGenerator.RC2_CBC;
        @PublicAtsApi
        public static final String SEED_CBC        = SMIMEEnvelopedGenerator.SEED_CBC;
    }

    @PublicAtsApi
    public static class SignatureAlgorithm {

        @PublicAtsApi
        public static final String MD5             = SMIMESignedGenerator.DIGEST_MD5;
        @PublicAtsApi
        public static final String SHA1            = SMIMESignedGenerator.DIGEST_SHA1;
        @PublicAtsApi
        public static final String SHA224          = SMIMESignedGenerator.DIGEST_SHA224;
        @PublicAtsApi
        public static final String SHA384          = SMIMESignedGenerator.DIGEST_SHA384;
        @PublicAtsApi
        public static final String SHA256          = SMIMESignedGenerator.DIGEST_SHA256;
        @PublicAtsApi
        public static final String SHA512          = SMIMESignedGenerator.DIGEST_SHA512;
        @PublicAtsApi
        public static final String RIPEMD128       = SMIMESignedGenerator.DIGEST_RIPEMD128;
        @PublicAtsApi
        public static final String RIPEMD160       = SMIMESignedGenerator.DIGEST_RIPEMD160;
        @PublicAtsApi
        public static final String RIPEMD256       = SMIMESignedGenerator.DIGEST_RIPEMD256;
        @PublicAtsApi
        public static final String RSA             = SMIMESignedGenerator.ENCRYPTION_RSA;
        @PublicAtsApi
        public static final String DSA             = SMIMESignedGenerator.ENCRYPTION_DSA;
        @PublicAtsApi
        public static final String ECDSA           = SMIMESignedGenerator.ENCRYPTION_ECDSA;
        @PublicAtsApi
        public static final String SHA1_with_RSA   = PKCSObjectIdentifiers.sha1WithRSAEncryption.getId();
        @PublicAtsApi
        public static final String SHA224_with_RSA = PKCSObjectIdentifiers.sha224WithRSAEncryption.getId();
        @PublicAtsApi
        public static final String SHA256_with_RSA = PKCSObjectIdentifiers.sha256WithRSAEncryption.getId();
        @PublicAtsApi
        public static final String SHA384_with_RSA = PKCSObjectIdentifiers.sha384WithRSAEncryption.getId();
        @PublicAtsApi
        public static final String SHA512_with_RSA = PKCSObjectIdentifiers.sha512WithRSAEncryption.getId();

    }

    static {
        SslUtils.registerBCProvider(); // register only once
    }

    /**
     * Create new instance for work with S/MIME mails using specified key store and alias
     *
     * @param  location the location (path and filename) of the file containing the keystore/certificate (chain)
     * @param  password the password to access the given keystore or 'null' if there is no password
     * @param  aliasOrCN the key pair alias or a CN (Common Name) if there is no private key
     */
    @PublicAtsApi
    public SMimePackageEncryptor( String location, String password, String aliasOrCN ) {

        this.certLocations.add(location);
        this.certLocation = location;

        this.certPasswords.add(password);
        this.certPassword = password;

        this.aliasOrCNs.add(aliasOrCN);
        this.aliasOrCN = aliasOrCN;
    }

    /**
     * Create new instance for work with S/MIME mails using specified key store. <br />
     * This constructor is used when the key alias is not known and all the keys
     * in the keystore will be tried for decryption and checking the signature.
     * For the methods which require key alias, like encryption and signing, exception will be thrown
     *
     * @param  location the location (path and filename) of the file containing the keystore
     * @param  password the password to access the given keystore
     */
    @PublicAtsApi
    public SMimePackageEncryptor( String location, String password ) {

        this.certLocations.add(location);
        this.certLocation = location;

        this.certPasswords.add(password);
        this.certPassword = password;
    }

    /**
     * Create new instance for work with S/MIME mails using specified key stores and aliases. <br />
     * 
     * This constructor should be used when sending a mail to more than one recipient. 
     * A list of certificates is passed, so the symmetric key(used to encrypt the mail) will be encrypted 
     * many times(once per certificate). 
     * This way each recipient will be able to decrypt the mail using his own certificate.
     * 
     * @param locations the locations (path and filename) of the files containing the keystore/certificate (chain)
     * @param passwords the passwords to access the given keystores or 'null' if there is no password
     * @param aliasOrCNs the key pair aliases or CNs (Common Name) if there is no private key
     * @throws ActionException 
     */
    @PublicAtsApi
    public SMimePackageEncryptor( List<String> locations, List<String> passwords,
                                  List<String> aliasOrCNs ) throws ActionException {

        if (locations == null || aliasOrCNs == null || locations.size() == 0 || passwords.size() == 0
            || aliasOrCNs.size() == 0 || locations.size() != passwords.size()
            || locations.size() != aliasOrCNs.size()) {
            throw new ActionException("You must provide an equal number of keystore files, passwords and aliases");
        }

        this.certLocations = locations;
        this.certLocation = locations.get(0);

        this.certPasswords = passwords;
        this.certPassword = passwords.get(0);

        this.aliasOrCNs = aliasOrCNs;
        this.aliasOrCN = aliasOrCNs.get(0);
    }

    /**
     * Set cipher ID to be used for encryption algorithm
     * Use member class {@link Cipher} for common values. Current default is AES_128_CBC
     * @param cipher Example {@link SMimePackageEncryptor.Cipher#AES128_CBC}
     */
    public void setEncryptionCipher( String cipher ) {

        this.encryptionCipher = new ASN1ObjectIdentifier(cipher);
    }

    @PublicAtsApi
    public Package encrypt( Package source ) throws ActionException {

        try {
            MimeMessage encryptedMessage = new MimeMessage(Session.getInstance(new Properties()));
            MimeMessage originalMessage = getMimeMessage(source);

            Enumeration<?> hdrEnum = originalMessage.getAllHeaders();
            while (hdrEnum.hasMoreElements()) {
                Header current = (Header) hdrEnum.nextElement();
                encryptedMessage.setHeader(current.getName(), current.getValue());
            }

            KeyStore ks = getKeystore();
            Certificate cer = ks.getCertificate(aliasOrCN);
            SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
            encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator((X509Certificate) cer).setProvider(BouncyCastleProvider.PROVIDER_NAME));

            ASN1ObjectIdentifier encryption = null;
            if (encryptionCipher == null) {
                encryption = CMSAlgorithm.AES128_CBC; //set default. Was CMSAlgorithm.RC2_CBC  
            } else {
                encryption = encryptionCipher;
            }

            MimeBodyPart mp = encrypter.generate(originalMessage,
                                                 new JceCMSContentEncryptorBuilder(encryption).setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                                                                              .build());
            encryptedMessage.setContent(mp.getContent(), mp.getContentType());
            Enumeration<?> mpEnum = mp.getAllHeaders();
            while (mpEnum.hasMoreElements()) {
                Header current = (Header) mpEnum.nextElement();
                encryptedMessage.setHeader(current.getName(), current.getValue());
            }

            encryptedMessage.saveChanges();

            return new MimePackage(encryptedMessage);

        } catch (Exception e) {
            throw new ActionException(ENCRYPTION_EXCEPTION, e);
        }
    }

    private KeyStore getKeystore() throws ActionException {

        KeyStore ks = null;
        try (FileInputStream fis = new FileInputStream(certLocation)) {
            ks = KeyStore.getInstance(PKCS12_KEYSTORE_TYPE, BouncyCastleProvider.PROVIDER_NAME);
            ks.load(fis, certPassword.toCharArray());

            if (aliasOrCN == null) {
                Enumeration<String> aliases = ks.aliases();
                String alias = aliases.nextElement();
                aliasOrCN = alias;
            }

        } catch (Exception e) {
            throw new ActionException(e);
        }
        return ks;
    }

    private MimeMessage getMimeMessage( Package source ) throws ActionException {

        //first make sure we have a MimePackage
        try {
            MimePackage mimePackage = (MimePackage) source;
            return mimePackage.getMimeMessage();
        } catch (ClassCastException cce) {
            throw new ActionException("Source package " + source.getDescription()
                                      + " is not a MIME package");
        }
    }

    @PublicAtsApi
    public Package decrypt( Package sourcePackage ) throws ActionException {

        boolean storeReconnected = false; // for connection management to IMAP store
        if (sourcePackage instanceof MimePackage) {
            try {
                storeReconnected = ((MimePackage) sourcePackage).reconnectStoreIfClosed();
            } catch (MessagingException ex) {
                throw new ActionException("Could not reopen IMAP connection", ex);
            }
        }
        try {
            KeyStore ks = getKeystore();
            RecipientId recId = new JceKeyTransRecipientId((X509Certificate) ks.getCertificate(aliasOrCN));

            MimeMessage msg = getMimeMessage(sourcePackage);
            SMIMEEnveloped m = new SMIMEEnveloped(msg);

            RecipientInformationStore recipients = m.getRecipientInfos();
            RecipientInformation recipient = recipients.get(recId);
            PrivateKey privateKey = (PrivateKey) ks.getKey(aliasOrCN, certPassword.toCharArray());
            JceKeyTransRecipient jceKey = new JceKeyTransEnvelopedRecipient(privateKey).setProvider(BouncyCastleProvider.PROVIDER_NAME);

            MimeBodyPart result = null;
            try {
                result = SMIMEUtil.toMimeBodyPart(recipient.getContent(jceKey));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Successfully decrypted message with subject '" + msg.getSubject()
                              + "' with private key alias: " + aliasOrCN);
                }
            } catch (SMIMEException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Could not decrypt message with subject '" + sourcePackage.getSubject()
                              + "' with private key alias '" + aliasOrCN + "'", e);
                }
            }

            SMIMESigned signedMessage = null;
            MimeMessage decryptedMsg = new MimeMessage(Session.getInstance(new Properties()));
            if (result != null) {
                Object content = result.getContent();
                Enumeration<?> hLineEnum = msg.getAllHeaderLines();
                while (hLineEnum.hasMoreElements()) {
                    decryptedMsg.addHeaderLine((String) hLineEnum.nextElement());
                }
                decryptedMsg.setContent(content, result.getContentType());

                // in order getPlainTextBody getHtmlTextBody to work as they do not work with attachments
                decryptedMsg.removeHeader("Content-Disposition");

                // check if the message is signed
                try {
                    if (content instanceof MimeMultipart) {
                        MimeMultipart multipartContent = (MimeMultipart) content;
                        if (multipartContent.getContentType() != null
                            && multipartContent.getContentType()
                                               .toLowerCase()
                                               .contains(CONTENT_TYPE_MULTIPART_SIGNED)) {

                            signedMessage = new SMIMESigned(multipartContent);
                        }
                    } else if (content instanceof SMIMESigned) {

                        signedMessage = (SMIMESigned) content;
                    } else if (content instanceof BASE64DecoderStream) {
                        // com.sun.mail.util.BASE64DecoderStream - JavaMail API dependency. Seems still available
                        //   in JavaMail 2.0 so not an issue if using other non-Oracle/OpenJDK JVMs
                        signedMessage = new SMIMESigned(decryptedMsg); // will throw exception if not signed
                    }
                } catch (Exception e) {
                    // the message is not signed
                    //log.debug( "Could not construct signed message instance", e );
                }
            }

            if (signedMessage != null) {

                // remove signature from the message
                decryptedMsg.setContent(signedMessage.getContent().getContent(),
                                        signedMessage.getContent().getContentType());

                MimePackage mimePackage = new MimePackage(decryptedMsg);
                // keep the SMIMESigned message for further signature verification
                mimePackage.setSMIMESignedMessage(signedMessage);
                return mimePackage;
            }
            return new MimePackage(decryptedMsg);

        } catch (Exception e) {
            throw new ActionException(DECRYPTION_EXCEPTION, e);
        } finally {

            if (storeReconnected) { // and sourcePackage should be instanceof MimePackage
                try {
                    ((MimePackage) sourcePackage).closeStoreConnection(true);
                } catch (MessagingException ex) {
                    LOG.debug(ex); // do not hide possible exception thrown in catch block
                }
            }
        }

    }

    @PublicAtsApi
    public Package sign( Package sourcePackage ) throws ActionException {

        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            KeyStore ks = getKeystore();
            // TODO wrap exception with possible causes and add some hint
            PrivateKey privateKey = (PrivateKey) ks.getKey(aliasOrCN, certPassword.toCharArray());

            // Get whole certificate chain
            Certificate[] certArr = ks.getCertificateChain(aliasOrCN);
            // Pre 4.0.6 behavior was not to attach full cert. chain X509Certificate cer = (X509Certificate) ks.getCertificate(aliasOrCN);
            if (certArr.length >= 1) {
                LOG.debug("Found certificate of alias: " + aliasOrCN + ". Lenght of cert chain: " + certArr.length
                          + ", child cert:" + certArr[0].toString());
            }

            X509Certificate childCert = (X509Certificate) certArr[0];

            /* Create the SMIMESignedGenerator */
            ASN1EncodableVector attributes = new ASN1EncodableVector();
            attributes.add(new SMIMEEncryptionKeyPreferenceAttribute(
                                                                     new IssuerAndSerialNumber(new X500Name(childCert.getIssuerDN()
                                                                                                                     .getName()),
                                                                                               childCert.getSerialNumber())));

            SMIMECapabilityVector capabilities = new SMIMECapabilityVector();
            capabilities.addCapability(SMIMECapability.aES128_CBC);
            capabilities.addCapability(SMIMECapability.dES_EDE3_CBC);
            capabilities.addCapability(SMIMECapability.rC2_CBC, 128);
            capabilities.addCapability(SMIMECapability.dES_CBC);

            attributes.add(new SMIMECapabilitiesAttribute(capabilities));

            if (signatureAlgorithm == null) { // not specified explicitly 
                // TODO check defaults to be used
                signatureAlgorithm = SignatureAlgorithm.DSA.equals(privateKey.getAlgorithm())
                                                                                              ? "SHA1withDSA"
                                                                                              : "MD5withRSA";
            }

            SMIMESignedGenerator signer = new SMIMESignedGenerator();
            JcaSimpleSignerInfoGeneratorBuilder signerGeneratorBuilder = new JcaSimpleSignerInfoGeneratorBuilder();
            signerGeneratorBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);
            signerGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(attributes));
            signer.addSignerInfoGenerator(signerGeneratorBuilder.build(signatureAlgorithm, privateKey,
                                                                       childCert));

            /* Add the list of certs to the generator */
            List<X509Certificate> certList = new ArrayList<X509Certificate>();
            for (int i = 0; i < certArr.length; i++) { // first add child cert, and CAs
                certList.add((X509Certificate) certArr[i]);
            }

            Store<?> certs = new JcaCertStore(certList);
            signer.addCertificates(certs);

            /* Sign the message */
            Session session = Session.getDefaultInstance(System.getProperties(), null);

            MimeMultipart mm = signer.generate(getMimeMessage(sourcePackage));
            MimeMessage signedMessage = new MimeMessage(session);

            /* Set all original MIME headers in the signed message */
            Enumeration<?> headers = getMimeMessage(sourcePackage).getAllHeaderLines();
            while (headers.hasMoreElements()) {
                signedMessage.addHeaderLine((String) headers.nextElement());
            }

            /* Set the content of the signed message */
            signedMessage.setContent(mm);
            signedMessage.saveChanges();

            return new MimePackage(signedMessage);
        } catch (Exception e) {
            throw new ActionException(EXCEPTION_WHILE_SIGNING, e);
        }
    }

    /**
    *
    * @param signatureAlgorithm  signature algorithm
    * <pre>
    * <b>NOTE:</b> Supported algorithms are available in the {@link SignatureAlgorithm} member class.
    * for example:  <i>SMimePackageEncryptor.SignatureAlgorithm.SHA1</i>
    * </pre>
    */
    public void setSignatureAlgorithm( String signatureAlgorithm ) {

        this.signatureAlgorithm = signatureAlgorithm;
    }

    @PublicAtsApi
    public boolean checkSignature( Package sourcePackage ) throws ActionException {

        return checkSignature(sourcePackage, this.certLocation, this.certPassword, this.aliasOrCN);
    }

    /**
     *
     * @param sourcePackage the package which signature to check
     * @return <code>true</code> if the signature can be confirmed with the current set of key pairs passed with the signature
     * @throws ActionException in case an error has occurred while checking the signature of the message
     */
    @PublicAtsApi
    public static boolean
            checkSignatureWithEmbeddedPublicKey( Package sourcePackage ) throws ActionException {

        return new SMimePackageEncryptor(null, null).checkSignature(sourcePackage, null, null, null);
    }

    @SuppressWarnings( "unchecked")
    private boolean checkSignature( Package sourcePackage, String keystoreLocation, String keystorePassword,
                                    String keystoreAlias ) throws ActionException {

        boolean storeReconnected = false; // for connection management to IMAP store
        if (sourcePackage instanceof MimePackage) {
            try {
                storeReconnected = ((MimePackage) sourcePackage).reconnectStoreIfClosed();
            } catch (MessagingException ex) {
                throw new ActionException("Could not reopen IMAP connection", ex);
            }
        }

        SMIMESigned signedMessage = getSMIMESignedMessage(sourcePackage);
        if (signedMessage == null) {
            throw new ActionException("The message is not signed");
        }

        try {

            // retrieve SignerInformation blocks which contains the signatures
            SignerInformationStore signers = signedMessage.getSignerInfos();
            Iterator<SignerInformation> it = signers.getSigners().iterator();

            if (keystoreLocation == null) { // extract public keys from the signature

                // a Store containing the public key certificates passed in the signature
                Store<?> certs = signedMessage.getCertificates();

                // Note: mail could be signed by multiple users. Currently we search for one/first signature match
                while (it.hasNext()) {

                    SignerInformation signer = it.next();
                    // extract the certificate for current signature - with first certificate only
                    Iterator<?> certIt = certs.getMatches(signer.getSID()).iterator();
                    X509Certificate cert = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                                                            .getCertificate((X509CertificateHolder) certIt.next());

                    // verify that the signature is correct and generated with the current certificate
                    if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                                                              .build(cert))) {
                        return true;
                    }
                }
                LOG.debug("No suitable public key found in the signature to verify it.");

            } else { // load public key from the certificate store file

                KeyStore ks;
                ks = KeyStore.getInstance(PKCS12_KEYSTORE_TYPE, BouncyCastleProvider.PROVIDER_NAME);
                ks.load(new FileInputStream(keystoreLocation), keystorePassword.toCharArray());

                String keyAlias = null;
                if (keystoreAlias == null) {
                    Enumeration<String> aliases = ks.aliases();
                    keyAlias = aliases.nextElement();
                } else {
                    keyAlias = keystoreAlias;
                }

                while (it.hasNext()) {

                    X509Certificate cert = (X509Certificate) ks.getCertificate(keyAlias);
                    Key publicKey = cert.getPublicKey();
                    if (publicKey == null) {
                        throw new Exception("The key for alias '" + keyAlias
                                            + "' was not found in keystore '" + keystoreLocation + "'");
                    }

                    // verify that the signature is correct and generated with the provided certificate
                    if (it.next()
                          .verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                                                          .build(cert))) {
                        return true;
                    }

                }

                LOG.debug("Could not verify the signature with the public key alias: " + keyAlias);
            }

            return false;
        } catch (Exception e) {

            throw new ActionException(SIGNATURE_EXCEPTION, e);
        } finally {

            if (storeReconnected) { // and sourcePackage should be instanceof MimePackage
                try {
                    ((MimePackage) sourcePackage).closeStoreConnection(false);
                } catch (MessagingException ex) {
                    LOG.debug(ex); // do not hide possible exception thrown in catch block
                }
            }
        }
    }

    @PublicAtsApi
    public String[] getKeystoreAliases( String keystoreType ) throws ActionException {

        FileInputStream is = null;
        try {
            is = new FileInputStream(new File(this.certLocation));

            // Load the keystore
            KeyStore keystore = null;
            if (PKCS12_KEYSTORE_TYPE.equalsIgnoreCase(keystoreType)) {

                Provider bcProvider = (BouncyCastleProvider) Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
                if (bcProvider == null || ! (bcProvider instanceof BouncyCastleProvider)) {
                    throw new RuntimeException("BounceCastle security provider seems not to be registered anymore "
                                               + "as it is done on SMimePackageEncryptor loading. It is required in order to use secure "
                                               + "mail operations");
                }
                keystore = KeyStore.getInstance(keystoreType, bcProvider);
            } else {

                keystore = KeyStore.getInstance(keystoreType);
            }
            keystore.load(is, this.certPassword.toCharArray());

            // get the aliases
            List<String> aliases = new ArrayList<String>();
            Enumeration<String> alEnum = keystore.aliases();
            while (alEnum.hasMoreElements()) {
                aliases.add(alEnum.nextElement());
            }
            return aliases.toArray(new String[aliases.size()]);
        } catch (Exception e) {
            throw new ActionException(e);
        } finally {
            IoUtils.closeStream(is);
        }
    }

    private SMIMESigned getSMIMESignedMessage( Package source ) throws ActionException {

        //first make sure we have a MimePackage
        try {
            MimePackage mimePackage = (MimePackage) source;
            return mimePackage.getSMIMESignedMessage();
        } catch (ClassCastException cce) {
            throw new ActionException("Source package " + source.getDescription()
                                      + " is not a MIME package");
        }
    }
}
