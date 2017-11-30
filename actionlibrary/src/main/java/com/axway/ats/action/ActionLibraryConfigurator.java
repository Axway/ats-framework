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
package com.axway.ats.action;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.AbstractConfigurator;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.core.CoreLibraryConfigurator;
import com.axway.ats.core.utils.StringUtils;

/**
 * The Action Library configuration class
 */
@PublicAtsApi
public class ActionLibraryConfigurator extends AbstractConfigurator {

    private static final String              PROPERTIES_FILE_NAME                         = "/ats.actionlibrary.properties";

    //the configuration keys
    private static final String              PACKAGE_LOADER_DEFAULT_BOX_KEY               = "actionlibrary.packageloader.defaultbox";

    private static final String              FILE_TRANSFER_VERBOSE_MODE                   = "actionlibrary.filetransfer.verbosemode";
    private static final String              FILE_TRANSFER_CONNECTION_TIMEOUT             = "actionlibrary.filetransfer.connection.timeout";
    private static final String              FILE_TRANSFER_CONNECTION_INTERVAL            = "actionlibrary.filetransfer.connection.interval";
    private static final String              FILE_TRANSFER_CONNECTION_INITIAL_DELAY       = "actionlibrary.filetransfer.connection.initialdelay";

    private static final String              FILE_SYSTEM_COPY_FILE_START_PORT             = "actionlibrary.filesystem.copyfile.start.port";
    private static final String              FILE_SYSTEM_COPY_FILE_END_PORT               = "actionlibrary.filesystem.copyfile.end.port";

    private static final String              MAIL_HOST                                    = "actionlibrary.mail.host";
    private static final String              MAIL_PORT                                    = "actionlibrary.mail.port";
    private static final String              MAIL_TIMEOUT                                 = "actionlibrary.mail.timeout";
    private static final String              MAIL_LOCAL_ADDRESS                           = "actionlibrary.mail.localaddress";

    private static final String              MAIL_SESSION_DEBUG_MODE                      = "actionlibrary.mail.session.debug";

    private static final String              MIMEPACKAGE_MAX_NESTED_LEVEL                 = "actionlibrary.mimepackage.maxnestedlevel";

    private static final String              FILE_SNAPSHOT_CHECK_MODIFICATION_TIME        = "actionlibrary.filesnapshot.check.modificationtime";
    private static final String              FILE_SNAPSHOT_CHECK_SIZE                     = "actionlibrary.filesnapshot.check.size";
    private static final String              FILE_SNAPSHOT_CHECK_MD5                      = "actionlibrary.filesnapshot.check.md5";
    private static final String              FILE_SNAPSHOT_CHECK_PERMISSIONS              = "actionlibrary.filesnapshot.check.permissions";
    private static final String              FILE_SNAPSHOT_SUPPORT_HIDDEN                 = "actionlibrary.filesnapshot.support.hidden";
    private static final String              FILE_SNAPSHOT_CHECK_PROPERTIES_FILES_CONTENT = "actionlibrary.filesnapshot.check.properties.content";
    private static final String              FILE_SNAPSHOT_CHECK_XML_FILES_CONTENT        = "actionlibrary.filesnapshot.check.xml.content";
    private static final String              FILE_SNAPSHOT_CHECK_INI_FILES_CONTENT        = "actionlibrary.filesnapshot.check.ini.content";
    private static final String              FILE_SNAPSHOT_CHECK_TEXT_FILES_CONTENT       = "actionlibrary.filesnapshot.check.text.content";
    private static final String              FILE_SNAPSHOT_INI_FILES_SECTION_START_CHAR   = "actionlibrary.filesnapshot.ini.section.start.char";
    private static final String              FILE_SNAPSHOT_INI_FILES_COMMENT_START_CHAR   = "actionlibrary.filesnapshot.ini.comment.start.char";
    private static final String              FILE_SNAPSHOT_INI_FILES_DELIMETER_CHAR       = "actionlibrary.filesnapshot.ini.delimeter.char";
    private static final String              FILE_SNAPSHOT_XML_FILE_EXTENSIONS            = "actionlibrary.filesnapshot.xml.file.types";
    private static final String              FILE_SNAPSHOT_PROPERTIES_FILE_EXTENSIONS     = "actionlibrary.filesnapshot.properties.file.types";
    private static final String              FILE_SNAPSHOT_INI_FILE_EXTENSIONS            = "actionlibrary.filesnapshot.ini.file.types";
    private static final String              FILE_SNAPSHOT_TEXT_FILE_EXTENSIONS           = "actionlibrary.filesnapshot.text.file.types";

    private static final String              REST_DEFAULT_REQUEST_MEDIA_TYPE              = "actionlibrary.rest.default.request.media.type";
    private static final String              REST_DEFAULT_REQUEST_MEDIA_CHARSET           = "actionlibrary.rest.default.request.media.charset";
    private static final String              REST_DEFAULT_RESPONSE_MEDIA_TYPE             = "actionlibrary.rest.default.response.media.type";
    private static final String              REST_DEFAULT_RESPONSE_MEDIA_CHARSET          = "actionlibrary.rest.default.response.media.charset";

    private static final String              REST_KEEP_REQUEST_MEDIA_TYPE                 = "actionlibrary.rest.keep.request.media.type";
    private static final String              REST_KEEP_REQUEST_MEDIA_CHARSET              = "actionlibrary.rest.keep.request.media.charset";
    private static final String              REST_KEEP_RESPONSE_MEDIA_TYPE                = "actionlibrary.rest.keep.response.media.type";
    private static final String              REST_KEEP_RESPONSE_MEDIA_CHARSET             = "actionlibrary.rest.keep.response.media.charset";
    private static final String              REST_KEEP_RESOURCE_PATH                      = "actionlibrary.rest.keep.response.resource.path";
    private static final String              REST_KEEP_REQUEST_HEADERS                    = "actionlibrary.rest.keep.request.headers";
    private static final String              REST_KEEP_REQUEST_PARAMETERS                 = "actionlibrary.rest.keep.request.parameters";

    private static final String              HTTP_KEEP_REQUEST_HEADERS                    = "actionlibrary.http.keep.request.headers";
    private static final String              HTTP_KEEP_REQUEST_PARAMETERS                 = "actionlibrary.http.keep.request.parameters";
    private static final String              HTTP_KEEP_REQUEST_BODY                       = "actionlibrary.http.keep.request.body";

    public FileSnapshots                     snapshots                                    = new FileSnapshots();

    /**
     * The singleton instance for this configurator
     */
    private static ActionLibraryConfigurator instance;

    private ActionLibraryConfigurator( String configurationSource ) {

        super();

        //add the resource to the repository
        addConfigFileFromClassPath(configurationSource, true, false);
    }

    @PublicAtsApi
    public static synchronized ActionLibraryConfigurator getInstance() {

        if (instance == null) {
            instance = new ActionLibraryConfigurator(PROPERTIES_FILE_NAME);
        }
        instance.reloadData();

        return instance;
    }

    /**
     * Get the default message box to be used when loading packages
     * 
     * @return the default message box name
     */
    @PublicAtsApi
    public String getDefaultMessagesBox() {

        return getProperty(PACKAGE_LOADER_DEFAULT_BOX_KEY);
    }

    /**
     * Set the default message box to be used when loading packages
     * 
     * @param defaultMessageBox the default message box name
     */
    @PublicAtsApi
    public void setDefaultMessagesBox(
                                       String defaultMessageBox ) {

        setTempProperty(PACKAGE_LOADER_DEFAULT_BOX_KEY, defaultMessageBox);
    }

    /**
     * Get the file transfer verbose mode
     * 
     * @return the file transfer verbose mode
     */
    @PublicAtsApi
    public boolean getFileTransferVerboseMode() {

        return getBooleanProperty(FILE_TRANSFER_VERBOSE_MODE);
    }

    /**
     * Set the file transfer verbose mode
     * 
     * @param verbose mode the file transfer verbose mode
     */
    @PublicAtsApi
    public void setFileTransferVerboseMode(
                                            boolean verboseMode ) {

        setTempProperty(FILE_TRANSFER_VERBOSE_MODE, Boolean.toString(verboseMode));
    }

    /**
     * Get the file transfer connection timeout
     * 
     * @return the file transfer connection timeout
     */
    @PublicAtsApi
    public long getFileTransferConnectionTimeout() {

        return getLongProperty(FILE_TRANSFER_CONNECTION_TIMEOUT);
    }

    /**
     * Set the file transfer connection timeout
     * 
     * @param defaultMessageBox the file transfer connection timeout
     */
    @PublicAtsApi
    public void setFileTransferConnectionTimeout(
                                                  long timeout ) {

        setTempProperty(FILE_TRANSFER_CONNECTION_TIMEOUT, Long.toString(timeout));
    }

    /**
     * Set the file transfer connection interval
     * 
     * @param defaultMessageBox the file transfer connection timeout
     */
    @PublicAtsApi
    public void setFileTransferConnectionInterval(
                                                   long interval ) {

        setTempProperty(FILE_TRANSFER_CONNECTION_INTERVAL, Long.toString(interval));
    }

    /**
     * Get the file transfer connection interval
     * 
     * @return the file transfer connection timeout
     */
    @PublicAtsApi
    public long getFileTransferConnectionInterval() {

        return getLongProperty(FILE_TRANSFER_CONNECTION_INTERVAL);
    }

    /**
     * Set the file transfer connection initial delay
     * 
     * @param defaultMessageBox the file transfer connection timeout
     */
    @PublicAtsApi
    public void setFileTransferConnectionInitialDelay(
                                                       long delay ) {

        setTempProperty(FILE_TRANSFER_CONNECTION_INITIAL_DELAY, Long.toString(delay));
    }

    /**
     * Get the file transfer connection initial delay
     * 
     * @return the file transfer connection timeout
     */
    @PublicAtsApi
    public long getFileTransferConnectionInitialDelay() {

        return getLongProperty(FILE_TRANSFER_CONNECTION_INITIAL_DELAY);
    }

    /**
     * Set the default HTTPS encryption protocols, for example "TLSv1.2".
     * You can specify more than one by using ',' as a delimiter
     * 
     * @param protocol the encryption protocols
     */
    @PublicAtsApi
    public void setFileTransferDefaultHttpsEncryptionProtocols(
                                                                String protocols ) {

        CoreLibraryConfigurator.getInstance().setFileTransferDefaultHttpsEncryptionProtocols(protocols);
    }

    /**
     * Get the default HTTPS encryption protocols
     * 
     * @return the encryption protocols
     */
    @PublicAtsApi
    public String getFileTransferDefaultHttpsEncryptionProtocols() {

        return CoreLibraryConfigurator.getInstance().getFileTransferDefaultHttpsEncryptionProtocols();
    }

    /**
     * Set the default HTTPS encryption cipher suites.
     * You can specify more than one by using ',' as a delimiter
     * 
     * @param protocol the cipher suites
     */
    @PublicAtsApi
    public void setFileTransferDefaultHttpsCipherSuites(
                                                         String cipherSuites ) {

        CoreLibraryConfigurator.getInstance().setFileTransferDefaultHttpsCipherSuites(cipherSuites);
    }

    /**
     * Get the default HTTPS encryption cipher suites
     * 
     * @return the cipher suites
     */
    @PublicAtsApi
    public String getFileTransferDefaultHttpsCipherSuites() {

        return CoreLibraryConfigurator.getInstance().getFileTransferDefaultHttpsCipherSuites();
    }

    /**
     * Set the starting point (lowest port number) 
     * to try to allocate for non-local file system copy operations
     * 
     * @param startPort starting range port
     */
    @PublicAtsApi
    public void setCopyFileStartPort(
                                      int startPort ) {

        setTempProperty(FILE_SYSTEM_COPY_FILE_START_PORT, Integer.toString(startPort));
    }

    /**
     * Get the starting point (lowest port number) for non-local file system copy operations
     * 
     * @return starting range port
     */
    @PublicAtsApi
    public String getCopyFileStartPort() {

        return getProperty(FILE_SYSTEM_COPY_FILE_START_PORT);
    }

    /**
     * Set the ending point (highest port number) 
     * to try to allocate for non-local file system copy operations
     * 
     * @param endPort ending range port
     */
    @PublicAtsApi
    public void setCopyFileEndPort(
                                    int endPort ) {

        setTempProperty(FILE_SYSTEM_COPY_FILE_END_PORT, Integer.toString(endPort));
    }

    /**
     * Get the ending point (highest port number) for non-local file system copy operations
     * 
     * @return ending range port
     */
    @PublicAtsApi
    public String getCopyFileEndPort() {

        return getProperty(FILE_SYSTEM_COPY_FILE_END_PORT);
    }

    /**
     * Set the mail(SMTP) Server
     * 
     * @param mailHost the mail Server
     */
    @PublicAtsApi
    public void setMailHost(
                             String mailHost ) {

        setTempProperty(MAIL_HOST, mailHost);
    }

    /**
     * Get the mail(SMTP) Server
     * 
     * @return the mail Server
     */
    @PublicAtsApi
    public String getMailHost() {

        return getProperty(MAIL_HOST);
    }

    /**
     * Set the port of the mail Server
     * 
     * @param mailPort the port of the mail Server
     */
    @PublicAtsApi
    public void setMailPort(
                             long mailPort ) {

        setTempProperty(MAIL_PORT, Long.toString(mailPort));
    }

    /**
     * Get the port of the mail Server
     * 
     * @return the port of the mail Server
     */
    @PublicAtsApi
    public long getMailPort() {

        return getLongProperty(MAIL_PORT);
    }

    /**
     * Set the mail sending timeout.
     * It is used for example when sending a mail over SMTP.
     * 
     * @param mailTimeout the mail send timeout
     */
    @PublicAtsApi
    public void setMailTimeout(
                                long mailTimeout ) {

        setTempProperty(MAIL_TIMEOUT, Long.toString(mailTimeout));
    }

    /**
     * Set the mail local host address. This is the local address to bind to when creating the SMTP socket. 
     * Defaults to the address picked by the Socket class. 
     * Should not normally need to be set, but useful with multi-homed hosts where it's important to pick a particular local address to bind to.
     * 
     * @param localAddress the local address
     */
    @PublicAtsApi
    public void setMailLocalAddress(
                                     String localAddress ) {

        setTempProperty(MAIL_LOCAL_ADDRESS, localAddress);
    }

    /**
     * Get the mail local address
     * @return the mail local address
     */
    @PublicAtsApi
    public String getMailLocalAddress() {

        return getProperty(MAIL_LOCAL_ADDRESS);
    }

    /**
     * Get the mail sending timeout.
     * It is used for example when sending a message over SMTP.
     * 
     * @return the mail sending timeout
     */
    @PublicAtsApi
    public long getMailTimeout() {

        return getLongProperty(MAIL_TIMEOUT);
    }

    /**
     * Set mail session in debug mode
     * 
     * @param debugMode the mail session debug mode
     */
    @PublicAtsApi
    public void setMailSessionDebugMode(
                                         boolean debugMode ) {

        setTempProperty(MAIL_SESSION_DEBUG_MODE, Boolean.toString(debugMode));
    }

    /**
     * Get if the mail session is in debug mode
     * 
     * @return
     */
    @PublicAtsApi
    public boolean getMailSessionDebugMode() {

        return getBooleanProperty(MAIL_SESSION_DEBUG_MODE);
    }

    /**
     * Get the level of nested packages we parse when loading a MIME package
     * 
     * @return
     */
    @PublicAtsApi
    public int getMimePackageMaxNestedLevel() {

        return getIntegerProperty(MIMEPACKAGE_MAX_NESTED_LEVEL);
    }

    /**
     * Set the level of nested packages we parse when loading a MIME package 
     * 
     * @param maxNestedLevel the max nested level
     */
    @PublicAtsApi
    public void setMimePackageMaxNestedLevel(
                                              int maxNestedLevel ) {

        setTempProperty(MIMEPACKAGE_MAX_NESTED_LEVEL, Integer.toString(maxNestedLevel));
    }

    @PublicAtsApi
    public String getRestDefaultRequestMediaType() {

        return getProperty(REST_DEFAULT_REQUEST_MEDIA_TYPE);
    }

    @PublicAtsApi
    public void setRestDefaultRequestMediaType(
                                                String mediaType ) {

        setTempProperty(REST_DEFAULT_REQUEST_MEDIA_TYPE, mediaType);
    }

    @PublicAtsApi
    public String getRestDefaultRequestMediaCharset() {

        return getProperty(REST_DEFAULT_REQUEST_MEDIA_CHARSET);
    }

    @PublicAtsApi
    public void setRestDefaultRequestMediaCharset(
                                                   String mediaType ) {

        setTempProperty(REST_DEFAULT_REQUEST_MEDIA_CHARSET, mediaType);
    }

    @PublicAtsApi
    public String getRestDefaultResponseMediaType() {

        return getProperty(REST_DEFAULT_RESPONSE_MEDIA_TYPE);
    }

    @PublicAtsApi
    public void setRestDefaultResponseMediaType(
                                                 String mediaType ) {

        setTempProperty(REST_DEFAULT_RESPONSE_MEDIA_TYPE, mediaType);
    }

    @PublicAtsApi
    public String getRestDefaultResponseMediaCharset() {

        return getProperty(REST_DEFAULT_RESPONSE_MEDIA_CHARSET);
    }

    @PublicAtsApi
    public void setRestDefaultResponseMediaCharset(
                                                    String mediaType ) {

        setTempProperty(REST_DEFAULT_RESPONSE_MEDIA_CHARSET, mediaType);
    }

    @PublicAtsApi
    public boolean getRestKeepRequestMediaType() {

        return getBooleanProperty(REST_KEEP_REQUEST_MEDIA_TYPE);
    }

    @PublicAtsApi
    public void setRestKeepRequestMediaType(
                                             boolean keepRequestMediaType ) {

        setTempProperty(REST_KEEP_REQUEST_MEDIA_TYPE, Boolean.toString(keepRequestMediaType));
    }

    @PublicAtsApi
    public boolean getRestKeepRequestMediaCharset() {

        return getBooleanProperty(REST_KEEP_REQUEST_MEDIA_CHARSET);
    }

    @PublicAtsApi
    public void setRestKeepRequestMediaCharset(
                                                boolean keepRequestMediaCharset ) {

        setTempProperty(REST_KEEP_REQUEST_MEDIA_CHARSET, Boolean.toString(keepRequestMediaCharset));
    }

    @PublicAtsApi
    public boolean getRestKeepResponseMediaType() {

        return getBooleanProperty(REST_KEEP_RESPONSE_MEDIA_TYPE);
    }

    @PublicAtsApi
    public void setRestKeepResponseMediaType(
                                              boolean keepResponseMediaType ) {

        setTempProperty(REST_KEEP_RESPONSE_MEDIA_TYPE, Boolean.toString(keepResponseMediaType));
    }

    @PublicAtsApi
    public boolean getRestKeepResponseMediaCharset() {

        return getBooleanProperty(REST_KEEP_RESPONSE_MEDIA_CHARSET);
    }

    @PublicAtsApi
    public void setRestKeepResponseMediaCharset(
                                                 boolean keepResponseMediaCharset ) {

        setTempProperty(REST_KEEP_RESPONSE_MEDIA_CHARSET, Boolean.toString(keepResponseMediaCharset));
    }

    @PublicAtsApi
    public boolean getRestKeepResourcePath() {

        return getBooleanProperty(REST_KEEP_RESOURCE_PATH);
    }

    @PublicAtsApi
    public void setRestKeepResourcePath(
                                         boolean keepResourcePath ) {

        setTempProperty(REST_KEEP_RESOURCE_PATH, Boolean.toString(keepResourcePath));
    }

    @PublicAtsApi
    public boolean getRestKeepRequestHeaders() {

        return getBooleanProperty(REST_KEEP_REQUEST_HEADERS);
    }

    @PublicAtsApi
    public void setRestKeepRequestHeaders(
                                           boolean keepRequestHeaders ) {

        setTempProperty(REST_KEEP_REQUEST_HEADERS, Boolean.toString(keepRequestHeaders));
    }

    @PublicAtsApi
    public boolean getRestKeepRequestParameters() {

        return getBooleanProperty(REST_KEEP_REQUEST_PARAMETERS);
    }

    @PublicAtsApi
    public void setRestKeepRequestParameters(
                                              boolean keepRequestParameters ) {

        setTempProperty(REST_KEEP_REQUEST_PARAMETERS, Boolean.toString(keepRequestParameters));
    }

    @PublicAtsApi
    public boolean getHttpKeepRequestHeaders() {

        return getBooleanProperty(HTTP_KEEP_REQUEST_HEADERS);
    }

    @PublicAtsApi
    public void setHttpKeepRequestHeaders(
                                           boolean keepRequestHeaders ) {

        setTempProperty(HTTP_KEEP_REQUEST_HEADERS, Boolean.toString(keepRequestHeaders));
    }

    @PublicAtsApi
    public boolean getHttpKeepRequestParameters() {

        return getBooleanProperty(HTTP_KEEP_REQUEST_PARAMETERS);
    }

    @PublicAtsApi
    public void setHttpKeepRequestParameters(
                                              boolean keepRequestParameters ) {

        setTempProperty(HTTP_KEEP_REQUEST_PARAMETERS, Boolean.toString(keepRequestParameters));
    }

    @PublicAtsApi
    public boolean getHttpKeepRequestBody() {

        return getBooleanProperty(HTTP_KEEP_REQUEST_BODY);
    }

    @PublicAtsApi
    public void setHttpKeepRequestBody(
                                        boolean keepRequestBody ) {

        setTempProperty(HTTP_KEEP_REQUEST_BODY, Boolean.toString(keepRequestBody));
    }

    /**
     * Settings for file snapshots 
     */
    public class FileSnapshots {

        @PublicAtsApi
        public boolean getCheckModificationTime() {

            try {
                return getBooleanProperty(FILE_SNAPSHOT_CHECK_MODIFICATION_TIME);
            } catch (NoSuchPropertyException nspe) {
                return true;
            }
        }

        @PublicAtsApi
        public void setCheckModificationTime( boolean checkModificationTime ) {

            setTempProperty(FILE_SNAPSHOT_CHECK_MODIFICATION_TIME,
                            Boolean.toString(checkModificationTime));
        }

        @PublicAtsApi
        public boolean getCheckFileSize() {

            try {
                return getBooleanProperty(FILE_SNAPSHOT_CHECK_SIZE);
            } catch (NoSuchPropertyException nspe) {
                return true;
            }
        }

        @PublicAtsApi
        public void setCheckFileSize( boolean checkSize ) {

            setTempProperty(FILE_SNAPSHOT_CHECK_SIZE, Boolean.toString(checkSize));
        }

        @PublicAtsApi
        public boolean getCheckFileMd5() {

            try {
                return getBooleanProperty(FILE_SNAPSHOT_CHECK_MD5);
            } catch (NoSuchPropertyException nspe) {
                return true;
            }
        }

        @PublicAtsApi
        public void setCheckFileMd5( boolean checkMd5 ) {

            setTempProperty(FILE_SNAPSHOT_CHECK_MD5, Boolean.toString(checkMd5));
        }

        @PublicAtsApi
        public boolean getCheckFilePermissions() {

            try {
                return getBooleanProperty(FILE_SNAPSHOT_CHECK_PERMISSIONS);
            } catch (NoSuchPropertyException nspe) {
                return true;
            }
        }

        @PublicAtsApi
        public void setCheckFilePermissions( boolean checkPermissions ) {

            setTempProperty(FILE_SNAPSHOT_CHECK_PERMISSIONS, Boolean.toString(checkPermissions));
        }

        @PublicAtsApi
        public boolean getSupportHiddenFiles() {

            try {
                return getBooleanProperty(FILE_SNAPSHOT_SUPPORT_HIDDEN);
            } catch (NoSuchPropertyException nspe) {
                return true;
            }
        }

        @PublicAtsApi
        public void setSupportHiddenFiles( boolean supportHiddenFiles ) {

            setTempProperty(FILE_SNAPSHOT_SUPPORT_HIDDEN, Boolean.toString(supportHiddenFiles));
        }

        @PublicAtsApi
        public boolean getCheckPropertiesFilesContent() {

            try {
                return getBooleanProperty(FILE_SNAPSHOT_CHECK_PROPERTIES_FILES_CONTENT);
            } catch (NoSuchPropertyException nspe) {
                return true;
            }
        }

        @PublicAtsApi
        public void setCheckPropertiesFilesContent( boolean checkPropertiesFilesContent ) {

            setTempProperty(FILE_SNAPSHOT_CHECK_PROPERTIES_FILES_CONTENT,
                            Boolean.toString(checkPropertiesFilesContent));
        }

        @PublicAtsApi
        public boolean getCheckXmlFilesContent() {

            try {
                return getBooleanProperty(FILE_SNAPSHOT_CHECK_XML_FILES_CONTENT);
            } catch (NoSuchPropertyException nspe) {
                return true;
            }
        }

        @PublicAtsApi
        public void setCheckXmlFilesContent( boolean checkXmlFilesContent ) {

            setTempProperty(FILE_SNAPSHOT_CHECK_XML_FILES_CONTENT,
                            Boolean.toString(checkXmlFilesContent));
        }

        @PublicAtsApi
        public boolean getCheckIniFilesContent() {

            try {
                return getBooleanProperty(FILE_SNAPSHOT_CHECK_INI_FILES_CONTENT);
            } catch (NoSuchPropertyException nspe) {
                return true;
            }
        }

        @PublicAtsApi
        public void setCheckIniFilesContent( boolean checkIniFilesContent ) {

            setTempProperty(FILE_SNAPSHOT_CHECK_INI_FILES_CONTENT,
                            Boolean.toString(checkIniFilesContent));
        }

        @PublicAtsApi
        public boolean getCheckTextFilesContent() {

            try {
                return getBooleanProperty(FILE_SNAPSHOT_CHECK_TEXT_FILES_CONTENT);
            } catch (NoSuchPropertyException nspe) {
                return true;
            }
        }

        @PublicAtsApi
        public void setCheckTextFilesContent( boolean checkTextFilesContent ) {

            setTempProperty(FILE_SNAPSHOT_CHECK_TEXT_FILES_CONTENT,
                            Boolean.toString(checkTextFilesContent));
        }

        @PublicAtsApi
        public char getIniFilesStartSectionChar() {

            try {
                return getCharProperty(FILE_SNAPSHOT_INI_FILES_SECTION_START_CHAR);
            } catch (NoSuchPropertyException nspe) {
                return '['; // use default char
            }
        }

        @PublicAtsApi
        public void setIniFilesStartSectionChar( char startSectionChar ) {

            setTempProperty(FILE_SNAPSHOT_INI_FILES_SECTION_START_CHAR,
                            Character.toString(startSectionChar));
        }

        @PublicAtsApi
        public char getIniFilesStartCommentChar() {

            try {
                return getCharProperty(FILE_SNAPSHOT_INI_FILES_COMMENT_START_CHAR);
            } catch (NoSuchPropertyException nspe) {
                return '#'; // use default char
            }
        }

        @PublicAtsApi
        public void setIniFilesStartCommentChar( char startCommentChar ) {

            setTempProperty(FILE_SNAPSHOT_INI_FILES_COMMENT_START_CHAR,
                            Character.toString(startCommentChar));
        }

        @PublicAtsApi
        public char getIniFilesDelimeterChar() {

            try {
                return getCharProperty(FILE_SNAPSHOT_INI_FILES_DELIMETER_CHAR);
            } catch (NoSuchPropertyException nspe) {
                return '='; // use default char
            }
        }

        @PublicAtsApi
        public void setIniFilesDelimeterChar( char delimeterChar ) {

            setTempProperty(FILE_SNAPSHOT_INI_FILES_DELIMETER_CHAR, Character.toString(delimeterChar));
        }

        /**
         * @return files extensions for files treated as Properties files
         */
        public String getPropertiesFileExtensions() {

            String extensions = getOptionalProperty(FILE_SNAPSHOT_PROPERTIES_FILE_EXTENSIONS);
            if (StringUtils.isNullOrEmpty(extensions)) {
                return "";
            } else {
                return extensions;
            }
        }

        /**
         * Set file extensions that will be treated as Properties files.
         * Default extension is '.properties'
         * @param extensions new extensions
         */
        public void setPropertiesFileExtensions( String[] extensions ) {

            StringBuilder extensionsList = new StringBuilder();
            for (String extension : extensions) {
                if (StringUtils.isNullOrEmpty(extension) || extension.contains(",")) {
                    throw new ConfigurationException("File types cannot be empty nor can contain the ',' character. You have supplied '"
                                                     + extension + ",");
                }
                extensionsList.append(extension).append(",");
            }

            setTempProperty(FILE_SNAPSHOT_PROPERTIES_FILE_EXTENSIONS,
                            extensionsList.substring(0, extensionsList.length() - 1));
        }

        /**
         * @return files extensions for files treated as XML files
         */
        public String getXmlFileExtensions() {

            return getOptionalProperty(FILE_SNAPSHOT_XML_FILE_EXTENSIONS);
        }

        /**
         * Set file extensions that will be treated as XML files.
         * Default extension is '.xml'
         * @param extensions new extensions
         */
        public void setXmlFileExtensions( String[] extensions ) {

            StringBuilder extensionsList = new StringBuilder();
            for (String extension : extensions) {
                if (StringUtils.isNullOrEmpty(extension) || extension.contains(",")) {
                    throw new ConfigurationException("File types cannot be empty nor can contain the ',' character. You have supplied '"
                                                     + extension + ",");
                }
                extensionsList.append(extension).append(",");
            }

            setTempProperty(FILE_SNAPSHOT_XML_FILE_EXTENSIONS,
                            extensionsList.substring(0, extensionsList.length() - 1));
        }

        /**
         * @return files extensions for files treated as INI files
         */
        public String getIniFileExtensions() {

            String extensions = getOptionalProperty(FILE_SNAPSHOT_INI_FILE_EXTENSIONS);
            if (StringUtils.isNullOrEmpty(extensions)) {
                return "";
            } else {
                return extensions;
            }
        }

        /**
         * Set file extensions that will be treated as INI files.
         * Default extension is '.ini'
         * @param extensions new extensions
         */
        public void setIniFileExtensions( String... extensions ) {

            StringBuilder extensionsList = new StringBuilder();
            for (String extension : extensions) {
                if (StringUtils.isNullOrEmpty(extension) || extension.contains(",")) {
                    throw new ConfigurationException("File types cannot be empty nor can contain the ',' character. You have supplied '"
                                                     + extension + ",");
                }
                extensionsList.append(extension.trim().toLowerCase()).append(",");
            }

            setTempProperty(FILE_SNAPSHOT_INI_FILE_EXTENSIONS,
                            extensionsList.substring(0, extensionsList.length() - 1));
        }

        /**
         * @return files extensions for files treated as Text files
         */
        public String getTextFileExtensions() {

            String extensions = getOptionalProperty(FILE_SNAPSHOT_TEXT_FILE_EXTENSIONS);
            if (StringUtils.isNullOrEmpty(extensions)) {
                return "";
            } else {
                return extensions;
            }
        }

        /**
         * Set file extensions that will be treated as Text files.
         * Default extension is '.txt'
         * @param extensions new extensions
         */
        public void setTextFileExtensions( String... extensions ) {

            StringBuilder extensionsList = new StringBuilder();
            for (String extension : extensions) {
                if (StringUtils.isNullOrEmpty(extension) || extension.contains(",")) {
                    throw new ConfigurationException("File types cannot be empty nor can contain the ',' character. You have supplied '"
                                                     + extension + ",");
                }
                extensionsList.append(extension.trim().toLowerCase()).append(",");
            }

            setTempProperty(FILE_SNAPSHOT_TEXT_FILE_EXTENSIONS,
                            extensionsList.substring(0, extensionsList.length() - 1));
        }
    }

    @Override
    protected void reloadData() {

        // nothing to do here
    }
}
