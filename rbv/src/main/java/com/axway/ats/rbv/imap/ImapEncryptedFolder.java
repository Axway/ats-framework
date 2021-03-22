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
package com.axway.ats.rbv.imap;

import javax.mail.Store;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.Package;
import com.axway.ats.action.security.PackageEncryptor;
import com.axway.ats.rbv.model.RbvStorageException;

public class ImapEncryptedFolder extends ImapFolder {

    private static Logger    log = LogManager.getLogger(ImapEncryptedFolder.class);

    private PackageEncryptor packageEncryptor;

    ImapEncryptedFolder( Store store, String serverHost, String folderName, String userName, String password,
                         PackageEncryptor packageEncryptor ) throws RbvStorageException {

        super(store, serverHost, folderName, userName, password);

        this.packageEncryptor = packageEncryptor;
    }

    @Override
    protected ImapMetaData createImapMetaData( MimeMessage mimeMessage ) throws RbvStorageException {

        try {
            MimePackage encrypterPackage = new MimePackage(mimeMessage);
            log.info(encrypterPackage.getDescription() + " should be encrypted, trying to decrypt ...");
            Package decryptedPackage = packageEncryptor.decrypt(encrypterPackage);
            if (decryptedPackage == null) {
                // unable to understand encryption type
                // message is probably not encrypted at all
                // we will skip it
                log.warn("... skiped " + encrypterPackage.getDescription()
                         + " . Message is probably not encrypted at all");
                return null;
            }

            log.info("... decrytpion successful!");
            ImapMetaData metaData = new ImapMetaData((MimePackage) decryptedPackage);
            return metaData;
        } catch (Exception e) {
            throw new RbvStorageException("Error while decrypting message : ", e);
        }
    }
}
