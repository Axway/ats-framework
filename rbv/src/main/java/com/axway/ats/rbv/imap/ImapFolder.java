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

import java.util.ArrayList;
import java.util.List;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.MatchableAlreadyOpenException;
import com.axway.ats.rbv.model.MatchableNotOpenException;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.storage.Matchable;

@PublicAtsApi
public class ImapFolder implements Matchable {

    private static Logger  log = LogManager.getLogger(ImapFolder.class);

    private boolean        isOpen;
    private Store          store;
    private Folder         folder;
    private String         serverHost;
    private String         folderName;
    private String         userName;
    private String         password;

    private List<MetaData> allMetaDataList;
    private List<MetaData> newMetaDataList;

    private boolean        isInitialPass;

    ImapFolder( Store store,
                String serverHost,
                String folderName,
                String userName,
                String password ) {

        this.isOpen = false;
        this.store = store;
        this.serverHost = serverHost;
        this.folderName = folderName;
        this.userName = userName;
        this.password = password;

        newMetaDataList = new ArrayList<MetaData>();
        allMetaDataList = new ArrayList<MetaData>();
    }

    /**
     *
     * @see com.axway.ats.rbv.storage.Matchable#open()
     */
    public void open() throws RbvStorageException {

        //first check if the folder is already open
        if (isOpen) {
            throw new MatchableAlreadyOpenException(getDescription() + " is already open");
        }

        try {
            isInitialPass = true;

            // create and connect to the user's imap folder
            if (!store.isConnected()) {
                store.connect(serverHost, userName, password);

                log.debug("Connected to store '" + serverHost + "' with user '" + userName
                          + "' and password '" + password + "'");
            }

            if (folder == null || !folder.isOpen()) {
                folder = store.getFolder(folderName);
                folder.open(Folder.READ_WRITE);
            }

            allMetaDataList.clear();
            newMetaDataList.clear();

            isOpen = true;

            log.info("Opened " + getDescription());

        } catch (MessagingException me) {
            throw new RbvStorageException("Could not open " + getDescription(), me);
        }
    }

    /**
     * Closes the IMAP folder
     * @see com.axway.ats.rbv.storage.Matchable#close()
     */
    @PublicAtsApi
    public void close() throws RbvStorageException {

        //first check if the folder is open
        if (!isOpen) {
            throw new MatchableNotOpenException(getDescription() + " is not open");
        }

        try {
            if (store.isConnected()) {
                folder.close(true);
                store.close();

                log.info("Closed " + getDescription());
            }

            isOpen = false;

        } catch (MessagingException me) {
            throw new RbvStorageException("Could not close " + getDescription(), me);
        }
    }

    public List<MetaData> getAllMetaData() throws RbvException {

        //first check if the folder is open
        if (!isOpen) {
            throw new MatchableNotOpenException(getDescription() + " is not open");
        }

        try {
            allMetaDataList.clear();
            newMetaDataList.clear();

            boolean hasNew = folder.hasNewMessages();
            log.debug("Has new messages in folder: " + hasNew);
            Message[] imapMessages = folder.getMessages();

            for (Message imapMessage : imapMessages) {

                ImapMetaData currentMeta = createImapMetaData((MimeMessage) imapMessage);
                if (currentMeta != null) {
                    if (!imapMessage.getFlags().contains(Flags.Flag.FLAGGED)) {
                        newMetaDataList.add(currentMeta);
                    }

                    imapMessage.setFlag(Flags.Flag.FLAGGED, true);

                    allMetaDataList.add(currentMeta);
                }
            }

            //this was the first pass
            isInitialPass = false;

            return allMetaDataList;

        } catch (MessagingException me) {
            throw new RbvStorageException("Could not get meta data from " + getDescription(), me);
        }
    }

    /**
     * Not a public method returning all available messages in the form of java mail message
     *
     * @return
     * @throws RbvException
     */
    Message[] getAllMimeMessages() throws RbvException {

        //first check if the folder is open
        if (!isOpen) {
            throw new MatchableNotOpenException(getDescription() + " is not open");
        }

        try {
            return folder.getMessages();
        } catch (MessagingException me) {
            throw new RbvStorageException("Could not get meta data from " + getDescription(), me);
        }
    }

    public List<MetaData> getNewMetaData() throws RbvException {

        //first check if the folder is open
        if (!isOpen) {
            throw new MatchableNotOpenException(getDescription() + " is not open");
        }

        if (isInitialPass) {
            isInitialPass = false;
            return getAllMetaData();
        }

        try {
            newMetaDataList.clear();

            Message[] imapMessages = folder.getMessages();

            for (Message imapMessage : imapMessages) {
                if (!imapMessage.getFlags().contains(Flags.Flag.FLAGGED)) {
                    imapMessage.setFlag(Flags.Flag.FLAGGED, true);

                    ImapMetaData currentMeta = createImapMetaData((MimeMessage) imapMessage);
                    if (currentMeta != null) {
                        allMetaDataList.add(currentMeta);
                        newMetaDataList.add(currentMeta);
                    }
                }
            }
        } catch (MessagingException me) {
            throw new RbvStorageException("Could not get meta data from " + getDescription(), me);
        }

        return newMetaDataList;
    }

    public String getMetaDataCounts() throws RbvStorageException {

        //first check if the folder is open
        if (!isOpen) {
            throw new MatchableNotOpenException(getDescription() + " is not open");
        }

        return "Total messages: " + allMetaDataList.size() + ", new messages: " + newMetaDataList.size();
    }

    /**
     * Description info for this IMAP folder
     * @see com.axway.ats.rbv.storage.Matchable#getDescription()
     */
    public String getDescription() {

        return "IMAP folder '" + folderName + "' on '" + serverHost + "' for user '" + userName
               + "' with password '" + password + "'";
    }

    /**
     * Cleans up the associated IMAP folder
     * @throws RbvStorageException
     */
    @PublicAtsApi
    public void expunge() throws RbvStorageException {

        //first check if the folder is open
        if (!isOpen) {
            throw new MatchableNotOpenException(getDescription() + " is not open");
        }

        try {
            folder.setFlags(folder.getMessages(), new Flags(Flags.Flag.DELETED), true);
            folder.expunge();
        } catch (MessagingException me) {
            throw new RbvStorageException(me);
        }

        log.info("Expunged " + getDescription());
    }

    /**
     * This method will convert a MIME message to meta data
     *
     * @param mimeMessage   the input MimeMessage instance
     * @return              the MetaData produced
     * @throws RbvStorageException
     */
    protected ImapMetaData createImapMetaData(
                                               MimeMessage mimeMessage ) throws RbvException {

        try {
            MimePackage mimePackage = new MimePackage(mimeMessage);
            ImapMetaData metaData = new ImapMetaData(mimePackage);

            return metaData;

        } catch (PackageException pe) {
            throw new RbvStorageException("Could not get meta data from " + getDescription(), pe);
        }

    }
}
