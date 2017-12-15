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

package com.axway.ats.action.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * An HTTP(s) client which inherits see the fully functional @see {@link HttpClient} but is targeted
 * in achieving simple file uploads and downloads.</br></br>
 * Its main methods follow the trivial <b>connect -> upload/download -> disconnect</b> logic
 */
@PublicAtsApi
public class FileTransferHttpClient extends HttpClient implements IFileTransferClient {

    private static final Logger log                                   = Logger.getLogger(FileTransferHttpClient.class);

    private String              hostname;
    private int                 port;

    private static final int    DEFAULT_HTTP_PORT                     = 80;

    public final static String  UPLOAD_METHOD                         = "HTTP_HTTPS_UPLOAD_METHOD";
    public final static String  UPLOAD_METHOD__PUT                    = "HTTP_HTTPS_UPLOAD_METHOD__PUT";
    public final static String  UPLOAD_METHOD__POST                   = "HTTP_HTTPS_UPLOAD_METHOD__POST";
    private String              uploadMethod;

    public final static String  PREEMPTIVE_BASIC_AUTHENTICATION       = "HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION";
    public final static String  PREEMPTIVE_BASIC_AUTHENTICATION__TRUE = "HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION__TRUE";

    public final static String  UPLOAD_CONTENT_TYPE                   = "HTTP_HTTPS_UPLOAD_CONTENT_TYPE";
    private ContentType         contentType;

    public final static String  REQUEST_HEADER                        = "HTTP_HTTPS_REQUEST_HEADER";

    public final static String  SOCKET_READ_TIMEOUT                   = "HTTP_HTTPS_SOCKET_READ_TIMEOUT";

    public final static String  SOCKET_BUFFER_SIZE                    = "HTTP_SOCKET_BUFFER_SIZE";                         // used both for send and receive
    private final static int    SOCKET_BUFFER_SIZE_MAX_VALUE          = 4 * 1024 * 1024;
    private final static int    DEFAULT_SOCKET_BUFFER_SIZE            = 8196;

    // internal client id used to determine if another internal client is needed
    private String              clientId;

    /**
     * Basic constructor. URL will be provided later.
     */
    @PublicAtsApi
    public FileTransferHttpClient() {
        super();

        setDefaults();
    }

    /**
     * Constructor providing the target URl
     * 
     * @param url the URL
     */
    @PublicAtsApi
    public FileTransferHttpClient( String url ) {
        super(url);

        setDefaults();
    }

    private void setDefaults() {

        this.url = "";
        this.port = DEFAULT_HTTP_PORT;

        this.uploadMethod = UPLOAD_METHOD__PUT;
        this.contentType = ContentType.APPLICATION_OCTET_STREAM;

        this.socketBufferSize = DEFAULT_SOCKET_BUFFER_SIZE;
    }

    /**
     * If working over SSL
     * @param isOverSsl true for HTTPS and false for HTTP. Default value is false.
     */
    @PublicAtsApi
    public void setOverSsl( boolean isOverSsl ) {

        super.isOverSsl = isOverSsl;
    }

    /**
     * Connect to a remote host using basic authentication.
     * 
     * @param hostname the host to connect to
     * @param username the user name
     * @param password the password for the provided user name
     * @throws FileTransferException
     */
    @PublicAtsApi
    @Override
    public void connect( String hostname, String username, String password ) throws FileTransferException {

        if (username != null) {
            log.info("Connecting to " + hostname + " on port " + this.port + " using username " + username
                     + " and password " + password);
        } else {
            log.info("Connecting to " + hostname + " on port " + this.port);
        }

        // calculate client id, so can distinguish between clients with different connection parameters
        String newClientId = "host-" + hostname + "_user-" + username + "_pass-" + password;

        this.hostname = hostname;
        this.username = username;
        this.password = password;

        boolean needToInitializeClient = true;
        if (clientId != null) {
            // there's been a previous connection
            if (!newClientId.equalsIgnoreCase(clientId)) {
                // the previous connection used different parameters, we have to disconnect 
                disconnect();
            } else {
                needToInitializeClient = false;
            }
        }

        if (needToInitializeClient) {
            initialzeHttpClient();
            this.clientId = newClientId; // remember the client id for future connections
        }
    }

    /**
     * Connect to a remote host using secure authentication
     *
     * @param hostname the host to connect to
     * @param keystoreFile the file containing the key store
     * @param keystorePassword the key store password
     * @param publicKeyAlias the public key alias
     * @throws FileTransferException
     */
    @PublicAtsApi
    @Override
    public void connect( String hostname, String keystoreFile, String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException {

        setClientSSLCertificate(new File(keystoreFile), keystorePassword);

        connect(hostname, null, null);
    }

    /**
     * Disconnect
     * 
     * @throws FileTransferException
     */
    @PublicAtsApi
    @Override
    public void disconnect() throws FileTransferException {

        if (this.httpClient != null) {
            // when the client instance is no longer needed, shut down the connection manager to ensure
            // immediate deallocation of all system resources
            try {
                this.httpClient.close();
            } catch (IOException ioe) {
                throw new FileTransferException("Client instance was not closed.", ioe);
            } finally {
                this.httpClient = null;
            }
        }
    }

    /**
     * Upload a file
     * 
     * @param localFile path to the local file
     * @param remoteDir the remote directory
     * @param remoteFile the remote file name
     * @throws FileTransferException
     */
    @PublicAtsApi
    @Override
    public void uploadFile( String localFile, String remoteDir,
                            String remoteFile ) throws FileTransferException {

        checkClientInitialized();

        final String uploadUrl = constructRemoteHostUrl(remoteDir, remoteFile);
        log.info("Uploading " + uploadUrl);

        HttpEntityEnclosingRequestBase httpMethod;
        if (this.uploadMethod.equals(UPLOAD_METHOD__PUT)) {
            httpMethod = new HttpPut(uploadUrl);
        } else {
            httpMethod = new HttpPost(uploadUrl);
        }

        FileEntity fileUploadEntity = new FileEntity(new File(localFile), this.contentType);
        httpMethod.setEntity(fileUploadEntity);

        HttpResponse response = null;
        try {
            // add headers specified by the user
            addHeadersToHttpMethod(httpMethod);

            // upload the file
            response = httpClient.execute(httpMethod, httpContext);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode < 200 || responseCode > 206) {
                // 201 Created - the file is now present on the remote location
                // 204 No Content - there was a file with same name on same location, we replaced it
                throw new FileTransferException("Uploading '" + uploadUrl + "' returned '"
                                                + response.getStatusLine() + "'");
            }
        } catch (ClientProtocolException e) {
            log.error("Unable to upload file!", e);
            throw new FileTransferException(e);
        } catch (IOException e) {
            log.error("Unable to upload file!", e);
            throw new FileTransferException(e);
        } finally {
            // the UPLOAD returns response body on error
            if (response != null && response.getEntity() != null) {
                HttpEntity responseEntity = response.getEntity();
                // ensure that the entity content has been fully consumed and
                // the underlying stream has been closed
                try {
                    EntityUtils.consume(responseEntity);
                } catch (IOException e) {
                    // we tried our best to release the resources
                }
            }
        }

        log.info("Successfully uploaded '" + localFile + "' to '" + remoteDir + remoteFile + "', host "
                 + this.hostname);
    }

    /**
     * Download a file
     * 
     * @param localFile path to the local file
     * @param remoteDir the remote directory
     * @param remoteFile the remote file name
     * @throws FileTransferException
     */
    @PublicAtsApi
    @Override
    public void downloadFile( String localFile, String remoteDir,
                              String remoteFile ) throws FileTransferException {

        checkClientInitialized();

        final String downloadUrl = constructRemoteHostUrl(remoteDir, remoteFile);
        log.info("Downloading " + downloadUrl);
        HttpGet httpGetMethod = new HttpGet(downloadUrl);

        HttpEntity responseEntity = null;
        OutputStream outstream = null;
        boolean errorSavingFile = true;
        try {
            // add headers specified by the user
            addHeadersToHttpMethod(httpGetMethod);

            // download the file
            HttpResponse response = httpClient.execute(httpGetMethod, httpContext);
            if (200 != response.getStatusLine().getStatusCode()) {
                throw new FileTransferException("Downloading " + downloadUrl + " returned "
                                                + response.getStatusLine());
            }

            // save the file
            responseEntity = response.getEntity();
            if (responseEntity != null) {
                outstream = new FileOutputStream(localFile);
                responseEntity.writeTo(outstream);
                outstream.flush();
                errorSavingFile = false;
            } else {
                throw new FileTransferException("No file present in the response");
            }
        } catch (ClientProtocolException e) {
            log.error("Unable to download file!", e);
            throw new FileTransferException(e);
        } catch (IOException e) {
            log.error("Unable to download file!", e);
            throw new FileTransferException(e);
        } finally {
            if (responseEntity != null) {
                IoUtils.closeStream(outstream);
                if (errorSavingFile) {
                    try {
                        // We were not able to properly stream the entity content
                        // to the local file system. The next line consumes the
                        // entity content closes the underlying stream.
                        EntityUtils.consume(responseEntity);
                    } catch (IOException e) {
                        // we tried our best to release the resources
                    }
                }
            }
        }

        log.info("Successfully downloaded '" + localFile + "' from '" + remoteDir + remoteFile + "' at "
                 + this.hostname);
    }

    @Override
    protected void finalize() throws Throwable {

        // ensure the connection is terminated
        this.disconnect();

        super.finalize();
    }

    @Override
    public void setCustomPort( int port ) {

        this.port = port;
    }

    @Override
    public void setConnectionTimeout( int newValue ) {

        this.connectTimeoutSeconds = newValue;
    }

    @Override
    public void setTransferMode( TransferMode mode ) throws FileTransferException {

        // not applicable
    }

    @Override
    public String executeCommand( String command ) throws FileTransferException {

        // not applicable
        return null;
    }

    @Override
    public void startUploadAndPause( String localFile, String remoteDir,
                                     String remoteFile ) throws FileTransferException {

        // not implemented
    }

    @Override
    public void resumePausedTransfer() throws FileTransferException {

        // not implemented
    }

    @Override
    public void enableResponseCollection( boolean enable ) {

        // not implemented
    }

    @Override
    public String[] getResponses() {

        // not applicable
        return null;
    }

    @Override
    public void addCustomProperty( String key, Object value ) throws IllegalArgumentException {

        if (SOCKET_BUFFER_SIZE.equals(key)) {
            final String errorMsg = "An integer value between 0 and " + SOCKET_BUFFER_SIZE_MAX_VALUE
                                    + " is expected for parameter with key " + key
                                    + ", but you have provided '" + value + "'";
            int _socketBufferSize = -1;
            if (value instanceof String) {
                try {
                    _socketBufferSize = Integer.parseInt((String) value);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(errorMsg);
                }
            } else if (value instanceof Integer) {
                _socketBufferSize = ((Integer) value).intValue();
            } else {
                throw new IllegalArgumentException(errorMsg);
            }
            if (_socketBufferSize < 0 || _socketBufferSize > SOCKET_BUFFER_SIZE_MAX_VALUE) {
                throw new IllegalArgumentException(errorMsg);
            } else {
                this.socketBufferSize = _socketBufferSize;
            }
        } else if (SOCKET_READ_TIMEOUT.equals(key)) {
            final String errorMsg = "A non negative integer value is expected for parameter with key " + key
                                    + ", but you have provided '" + value + "'";
            int _socketTimeout = -1;
            if (value instanceof String) {
                try {
                    _socketTimeout = Integer.parseInt((String) value);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(errorMsg);
                }
            } else if (value instanceof Integer) {
                _socketTimeout = ((Integer) value).intValue();
            } else {
                throw new IllegalArgumentException(errorMsg);
            }
            if (_socketTimeout < 0) {
                throw new IllegalArgumentException(errorMsg);
            } else {
                this.readTimeoutSeconds = _socketTimeout;
            }
        } else if (UPLOAD_METHOD.equals(key)) {
            String _uploadMethod = value.toString();
            if (UPLOAD_METHOD__PUT.equalsIgnoreCase(_uploadMethod)
                || UPLOAD_METHOD__POST.equalsIgnoreCase(_uploadMethod)) {
                this.uploadMethod = _uploadMethod.toUpperCase();
            } else {
                throw new IllegalArgumentException("Ivalid value specified for parameter with key " + key
                                                   + ": '" + _uploadMethod
                                                   + "'. Use one of the FileTransferClient.HTTP_HTTPS_UPLOAD_METHOD__* constants for value");
            }
        } else if (PREEMPTIVE_BASIC_AUTHENTICATION.equals(key)) {
            authType = PREEMPTIVE_BASIC_AUTHENTICATION__TRUE.equalsIgnoreCase(value.toString())
                                                                                                ? AuthType.always
                                                                                                : AuthType.demand;
        } else if (UPLOAD_CONTENT_TYPE.equals(key)) {
            String _contentType = value.toString();
            if (!StringUtils.isNullOrEmpty(_contentType)) {
                try {
                    this.contentType = ContentType.parse(_contentType);
                } catch (ParseException pe) {
                    throw new IllegalArgumentException("Invalid value specified for HTTP(S) upload content type",
                                                       pe);
                }
            } else {
                throw new IllegalArgumentException("Null or empty value specified for HTTP(S) upload content type");
            }
        } else if (REQUEST_HEADER.equals(key)) {
            // add some request header

            String headerString = (String) value;

            int separatorIndex = headerString.indexOf(':');
            if (separatorIndex < 1) {
                throw new IllegalArgumentException("Custom parameter with key " + REQUEST_HEADER
                                                   + " requires a String value in the form '<header key>:<header value>' while '"
                                                   + value + "' is passed");
            } else {
                requestHeaders.add(new HttpHeader(headerString.substring(0, separatorIndex),
                                                  headerString.substring(separatorIndex + 1)));
            }
        } else {
            throw new IllegalArgumentException("Unknown property with key '" + key + "' is passed. "
                                               + "Use one of the HTTP_HTTPS_* constants for key and values in GenericFileTransferClient class");
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        // not used
    }

    /**
     * @return whether debug mode is on
     */
    @Override
    public boolean isDebugMode() {

        return debugLevel != HttpClient.HttpDebugLevel.NONE;
    }

    /**
     * Turns debug mode. Headers and body are logged in the console
     * @param turnDebug true to set debug mode on
     */
    @Override
    public void setDebugMode( boolean turnDebug ) {

        if (turnDebug) {
            debugLevel = HttpClient.HttpDebugLevel.ALL;
        } else {
            debugLevel = HttpClient.HttpDebugLevel.NONE;
        }
    }

    private String constructRemoteHostUrl( String remoteDir, String remoteFile ) {

        return "http" + (isOverSsl
                                   ? "s"
                                   : "")
               + "://" + this.hostname + ":" + this.port + getPathPlusFile(remoteDir, remoteFile);
    }

    /**
     * Constructs path+file string by fixing not allowed combinations.
     */
    private String getPathPlusFile( String remoteDir, String remoteFile ) {

        if (!remoteDir.startsWith("/")) {
            remoteDir = "/" + remoteDir;
        }
        if (!remoteDir.endsWith("/")) {
            remoteDir = remoteDir + "/";
        }
        if (remoteFile.startsWith("/")) {
            log.warn("File name starts with slash('/') character which should be part of the path!");
            if (remoteFile.length() > 1) {
                remoteFile = remoteFile.substring(1);
            }
        }
        return remoteDir + remoteFile;
    }

    private void checkClientInitialized() throws FileTransferException {

        if (httpClient == null) {
            throw new FileTransferException("Http client is not initialized. Was a connect method invoked?");
        }
    }
}
