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
package com.axway.ats.action.objects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.exceptions.DbRecordsException;
import com.axway.ats.core.dbaccess.mysql.DbConnMySQL;
import com.axway.ats.core.dbaccess.mysql.MysqlDbProvider;
import com.axway.ats.harness.config.CommonConfigurator;
import com.axway.ats.harness.config.MessagesBox;

/**
 * This class provides means for loading packages from a database or the file system
 */
@PublicAtsApi
public class PackageLoader {

    private static final Logger log = LogManager.getLogger(PackageLoader.class);

    /**
     * Private constructor to prevent instantiation
     */
    private PackageLoader() {

    }

    /**
     * Load a MIME package from the database. The host from which the packages are loaded is
     * the message box with name "messagesbox" by default
     * 
     * @param packageId the package id
     * @return the loaded MIME package
     * @throws PackageException if the package cannot be loaded
     */
    @PublicAtsApi
    public static MimePackage loadMimePackageFromDb(
                                                     int packageId ) throws PackageException {

        String defaultMessagesBox;
        try {
            defaultMessagesBox = ActionLibraryConfigurator.getInstance().getDefaultMessagesBox();
        } catch (ConfigurationException ce) {
            throw new PackageException("Cannot read the default message box name", ce);
        }

        return loadMimePackageFromDb(packageId, defaultMessagesBox);
    }

    /**
     * Load a MIME package from the database.
     * 
     * @param packageId the package id
     * @param messagesBoxName name of the messages box to load the package from
     * @return the loaded MIME package
     * @throws PackageException if the package cannot be loaded
     */
    @PublicAtsApi
    public static MimePackage loadMimePackageFromDb(
                                                     int packageId,
                                                     String messagesBoxName ) throws PackageException {

        MessagesBox messagesBox;
        try {
            messagesBox = CommonConfigurator.getInstance().getMessagesBox(messagesBoxName);
        } catch (ConfigurationException ce) {
            throw new PackageException("Cannot load data for messages box '" + messagesBoxName + "'", ce);
        }

        return new MimePackage(loadPackageFromDb(packageId,
                                                 messagesBox.getHost(),
                                                 messagesBox.getDbName(),
                                                 messagesBox.getDbTable(),
                                                 messagesBox.getDbUser(),
                                                 messagesBox.getDbPass()));
    }

    /**
     * Load a MIME package from the local file system
     * 
     * @param fileName the name of the file
     * @return the loaded MIME package
     * @throws PackageException if the package cannot be loaded
     */
    @PublicAtsApi
    public static MimePackage loadMimePackageFromFile(
                                                       String fileName ) throws PackageException {

        return new MimePackage(loadPackageFromFile(fileName));
    }

    // Why MySQL?!?
    private static InputStream loadPackageFromDb(
                                                  int packageId,
                                                  String messagesHost,
                                                  String messagesDB,
                                                  String messagestable,
                                                  String messagesUser,
                                                  String messagesPassword ) throws PackageException {

        DbConnMySQL dbConnection = new DbConnMySQL(messagesHost,
                                                   messagesDB,
                                                   messagesUser,
                                                   messagesPassword);
        MysqlDbProvider dbProvider = new MysqlDbProvider(dbConnection);

        try {
            InputStream packageContent = dbProvider.selectValue(messagestable,
                                                                "message_id",
                                                                Integer.toString(packageId),
                                                                "data");

            log.info("Successfully extracted package with id '" + packageId + "' from '" + messagestable
                     + "' DB");

            return packageContent;
        } catch (DbRecordsException dbre) {
            throw new PackageException("Package with id '" + packageId
                                       + "' does not exist in 'messages' DB", dbre);
        } catch (DbException dbe) {
            throw new PackageException("Could not get package with id '" + packageId
                                       + "' from the 'messages' DB", dbe);
        }
    }

    private static InputStream loadPackageFromFile(
                                                    String fileName ) throws PackageException {

        File packageFile = new File(fileName);
        try {
            FileInputStream fileStream = new FileInputStream(packageFile);
            return fileStream;
        } catch (FileNotFoundException fnfe) {
            throw new PackageException("Package '" + fileName + "' does not exist", fnfe);
        }
    }
}
