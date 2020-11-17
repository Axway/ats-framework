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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.rbv.RbvConfigurator;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.storage.SearchTerm;
import com.axway.ats.rbv.storage.Storage;

/**
 * <p>IMAP storage implementation. The storage is a reference
 * to an IMAP server and provides access to IMAP folders.
 * </p>
 *
 * <p>This class is also responsible for setting properties for the mail session.
 * If you need to investigate <em>underlying JavaMail communication</em> with the IMAP
 * server set the com.axway.ats.rbv.imap.ImapStorage severity in
 * Log4J configuration file to TRACE.
 * </p>
 */
public class ImapStorage implements Storage {

    static Logger            log                = LogManager.getLogger(ImapStorage.class);

    public final static long CONNECTION_TIMEOUT = 10000;
    public final static long TIMEOUT            = 60000;

    private Session          session;
    private String           imapServerHost;

    /**
     * Instantiate the IMAP storage
     *
     * @param imapServerHost    the IMAP server host name
     */
    public ImapStorage( String imapServerHost ) {

        this.imapServerHost = imapServerHost;

        // set the session
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", this.imapServerHost);
        props.setProperty("mail.imap.partialfetch", "false");
        props.setProperty("mail.smtp.connectiontimeout", String.valueOf(CONNECTION_TIMEOUT));
        props.setProperty("mail.smtp.timeout", String.valueOf(TIMEOUT));

        // add custom mail properties from RBVConfigurator
        Map<String, String> customMailProperties = RbvConfigurator.getInstance().getProperties("mail.");
        for (Entry<String, String> mailProperty : customMailProperties.entrySet()) {
            props.setProperty(mailProperty.getKey(), mailProperty.getValue());
        }

        this.session = Session.getInstance(props);
        if (log.isTraceEnabled()) {
            session.setDebug(true);
        }

        log.debug("IMAP session to host '" + imapServerHost + "' initialized");
    }

    public ImapFolder getFolder(
                                 SearchTerm searchTerm ) throws RbvStorageException {

        try {
            if (searchTerm.getClass().getName().equals(ImapFolderSearchTerm.class.getName())) {

                ImapFolderSearchTerm imapSearchTerm = (ImapFolderSearchTerm) searchTerm;
                Store store = session.getStore("imap");

                return new ImapFolder(store,
                                      imapServerHost,
                                      imapSearchTerm.getFolderName(),
                                      imapSearchTerm.getUserName(),
                                      imapSearchTerm.getPassword());
            } else if (searchTerm.getClass().getName().equals(ImapEncryptedFolderSearchTerm.class.getName())) {

                ImapEncryptedFolderSearchTerm imapSearchTerm = (ImapEncryptedFolderSearchTerm) searchTerm;
                Store store = session.getStore("imap");

                return new ImapEncryptedFolder(store,
                                               imapServerHost,
                                               imapSearchTerm.getFolderName(),
                                               imapSearchTerm.getUserName(),
                                               imapSearchTerm.getPassword(),
                                               imapSearchTerm.getEncryptor());
            } else {
                throw new RbvStorageException("Search term " + searchTerm.getClass().getSimpleName()
                                              + " is not supported");
            }

        } catch (NoSuchProviderException e) {
            throw new RuntimeException("Unable to get IMAP store for host " + imapServerHost, e);
        }
    }
}
