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

import com.axway.ats.action.security.PackageEncryptor;

public class ImapEncryptedFolderSearchTerm extends ImapFolderSearchTerm {

    private final PackageEncryptor packageEncryptor;

    /**
     * This constructor will instantiate a search term
     * which references the INBOX IMAP folder for a given user
     * and password
     * 
     * @param userName  the IMAP user name
     * @param password  the password
     * @param packageEncryptor encryptor to use in order to decrypt the message 
     */
    public ImapEncryptedFolderSearchTerm( String userName,
                                          String password,
                                          PackageEncryptor packageEncryptor ) {

        this(userName, password, "INBOX", packageEncryptor);
    }

    /**
     * This constructor will instantiate a search term
     * which references the given IMAP folder for a given user
     * and password
    
     * @param userName      the IMAP user name
     * @param password      the password
     * @param folderName    the IMAP folder name
     */
    public ImapEncryptedFolderSearchTerm( String userName,
                                          String password,
                                          String folderName,
                                          PackageEncryptor packageEncryptor ) {

        super(userName, password, folderName);

        this.packageEncryptor = packageEncryptor;
    }

    public PackageEncryptor getEncryptor() {

        return packageEncryptor;
    }
}
