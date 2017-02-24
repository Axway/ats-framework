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
package com.axway.ats.core.filetransfer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.core.filetransfer.model.IFileTransferClient;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * The {@link HttpClient} uses the Apache HTTP Components to initiate and execute
 * HTTP connections to a remote server.
 */
public class HttpClient extends AbstractFileTransferClient implements IFileTransferClient {

    /**
     * The actual underlying client used - either shared one or a new copy depending on useSharedPool flag
     */
    protected CloseableHttpClient httpClient;
    protected HttpClientBuilder   httpBuilder;

    /**
     * The http context. If it's <code>null</code> a default context will be used.
     */
    private HttpContext           httpContext;

    /**
     * Used to compare previous and current connect() iterations. If not different the same connection can be reused (if keep-alive)
     */
    protected String              hostname;
    protected String              username;
    protected String              userpass;

    protected boolean             prevConnectionInitializationPerformed            = false;

    protected final Logger        log;

    private int                   socketBufferSize;
    public final static String    HTTP_HTTPS_SOCKET_BUFFER_SIZE                    = "HTTP_SOCKET_BUFFER_SIZE";                                                                    // used both for send and receive
    public final static int       HTTP_HTTPS_SOCKET_BUFFER_SIZE_MAX_VALUE          = 4 * 1024 * 1024;
    private final static int      DEFAULT_SOCKET_BUFFER_SIZE                       = 8196;                                                                                         // default socket buffer size for read/write data

    public final static String    HTTP_HTTPS_UPLOAD_METHOD                         = "HTTP_HTTPS_UPLOAD_METHOD";
    public final static String    HTTP_HTTPS_UPLOAD_METHOD__PUT                    = "HTTP_HTTPS_UPLOAD_METHOD__PUT";
    public final static String    HTTP_HTTPS_UPLOAD_METHOD__POST                   = "HTTP_HTTPS_UPLOAD_METHOD__POST";

    private boolean               preemptiveBasicAuthentication;
    public final static String    HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION       = "HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION";
    public final static String    HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION__TRUE = "HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION__TRUE";

    public final static String    HTTP_HTTPS_UPLOAD_CONTENT_TYPE                   = "HTTP_HTTPS_UPLOAD_CONTENT_TYPE";
    public final static String    DEFAULT_HTTP_HTTPS_UPLOAD_CONTENT_TYPE           = "application/octet-stream";
    /**
     * Constant for setting socket read timeout in milliseconds
     */
    public final static String    HTTP_HTTPS_SOCKET_READ_TIMEOUT                   = "HTTP_HTTPS_SOCKET_READ_TIMEOUT";

    public final static String    HTTP_HTTPS_REQUEST_HEADER                        = "HTTP_HTTPS_REQUEST_HEADER";

    private static final String   USE_ONE_OF_THE_HTTP_HTTPS_CONSTANTS              = "Use one of the HTTP_HTTPS_* constants for key and values in GenericFileTransferClient class";

    // Headers given by the customer
    private List<Header>          requestHeaders                                   = new ArrayList<>();

    /**
     * Constructor
     * By default each connection is closed in disconnect() so no pooling is used.
     * Pooling between threads does not work when using different users.
     *
     * @param portNumber the port number to use when connecting
     */
    public HttpClient( int portNumber ) {

        super( portNumber );
        log = Logger.getLogger( this.getClass() );
    }

    /**
     * Connect to a remote host using basic authentication.
     * A new HTTP client will be created each time when at least one of hostname, username or password is changed
     *
     * @param newHostname the host to connect to
     * @param newUserName the user name
     * @param newPassword the password for the provided user name
     * @throws FileTransferException
     */
    @Override
    public void connect( String newHostname, String newUserName,
                         String newPassword ) throws FileTransferException {

        boolean createNewHttpClient = true;
        if( prevConnectionInitializationPerformed ) {
            // check for difference with prev. connection parameters
            if( hostname != null && hostname.equals( newHostname )
                && ( ( username != null && username.equals( newUserName ) )
                     || ( username == null && newUserName == null ) ) ) {
                createNewHttpClient = false;
            } else {
                disconnect(); // cleanup connection and HttpClient with different host and/or credentials
            }
        }
        this.hostname = newHostname;
        this.username = newUserName;
        this.userpass = newPassword;

        if( newUserName != null ) {
            log.info( "Connecting to " + newHostname + " on port " + this.port + " using username "
                      + newUserName + " and password " + newPassword ); // testing soft. so password can be logged
        } else {
            log.info( "Connecting to " + newHostname + " on port " + this.port );
        }

        // DO NOT make new HTTPClient object for every new connection - for possible connection reuse
        //disconnect();

        if( httpClient == null // disconnect performed
            || createNewHttpClient ) {
            // new HTTP client needed
            applyCustomProperties();
            createNewHttpClientWithCredentials( newHostname, newUserName, newPassword );
        }
        prevConnectionInitializationPerformed = true;
    }

    @Override
    public void connect( String hostname, String keystoreFile, String keystorePassword,
                         String publicKeyAlias ) throws FileTransferException {

        throw new IllegalStateException( "Keystores could be used used only for HTTPS connections. Use HTTPSClient class" );
    }

    /**
     * Disconnect from the remote host. It is not recommended to be invoked if
     * there will be subsequent( multiple) iterations to the same host with keep-alive connections.
     * This will speed initial connection creation, especially for HTTPS case.
     *
     * @throws FileTransferException
     */
    @Override
    public void disconnect() throws FileTransferException {

        if( this.httpClient != null ) {
            // when the client instance is no longer needed, shut down the connection manager to ensure
            // immediate deallocation of all system resources
            try {
                this.httpClient.close();
            } catch( IOException ioe ) {
                throw new FileTransferException( "Client instance was not closed.", ioe );
            } finally {
                this.httpClient = null;
            }
        }
    }

    @Override
    protected void performDownloadFile( String localFile, String remoteDir,
                                        String remoteFile ) throws FileTransferException {

        checkClientInitialized();
        final String getUrl = constructDownloadUrl( remoteDir, remoteFile );
        log.info( "Downloading " + getUrl );
        HttpGet httpGetMethod = new HttpGet( getUrl );

        HttpEntity responseEntity = null;
        OutputStream outstream = null;
        boolean errorSavingFile = true;
        try {
            // add headers specified by the user
            addRequestHeaders( httpGetMethod );

            // download the file
            HttpResponse response = httpClient.execute( httpGetMethod, httpContext );
            if( 200 != response.getStatusLine().getStatusCode() ) {
                throw new FileTransferException( "Downloading " + getUrl + " returned "
                                                 + response.getStatusLine() );
            }

            // save the file
            responseEntity = response.getEntity();
            if( responseEntity != null ) {
                outstream = new FileOutputStream( localFile );
                responseEntity.writeTo( outstream );
                outstream.flush();
                errorSavingFile = false;
            } else {
                throw new FileTransferException( "No file present in the response" );
            }
        } catch( ClientProtocolException e ) {
            log.error( "Unable to download file!", e );
            throw new FileTransferException( e );
        } catch( IOException e ) {
            log.error( "Unable to download file!", e );
            throw new FileTransferException( e );
        } finally {
            if( responseEntity != null ) {
                IoUtils.closeStream( outstream );
                if( errorSavingFile ) {
                    try {
                        // We were not able to properly stream the entity content
                        // to the local file system. The next line consumes the
                        // entity content closes the underlying stream.
                        EntityUtils.consume( responseEntity );
                    } catch( IOException e ) {
                        // we tried our best to release the resources
                    }
                }
            }
        }

        log.info( "Successfully downloaded '" + localFile + "' from '" + remoteDir + remoteFile + "' at "
                  + this.hostname );
    }

    @Override
    protected void performUploadFile( String localFile, String remoteDir,
                                      String remoteFile ) throws FileTransferException {

        checkClientInitialized();
        final String uploadUrl = constructUploadUrl( remoteDir, remoteFile );
        log.info( "Uploading " + uploadUrl );

        HttpEntityEnclosingRequestBase uploadMethod;
        Object uploadMethodObject = customProperties.get( HTTP_HTTPS_UPLOAD_METHOD );
        if( uploadMethodObject == null
            || !uploadMethodObject.toString().equals( HTTP_HTTPS_UPLOAD_METHOD__POST ) ) {
            uploadMethod = new HttpPut( uploadUrl );
        } else {
            uploadMethod = new HttpPost( uploadUrl );
        }

        String contentType = DEFAULT_HTTP_HTTPS_UPLOAD_CONTENT_TYPE;
        Object contentTypeObject = customProperties.get( HTTP_HTTPS_UPLOAD_CONTENT_TYPE );
        if( contentTypeObject != null ) {
            contentType = contentTypeObject.toString();
        }
        FileEntity fileUploadEntity = new FileEntity( new File( localFile ),
                                                      ContentType.parse( contentType ) );
        uploadMethod.setEntity( fileUploadEntity );

        HttpResponse response = null;
        try {
            // add headers specified by the user
            addRequestHeaders( uploadMethod );

            // upload the file
            response = httpClient.execute( uploadMethod, httpContext );
            int responseCode = response.getStatusLine().getStatusCode();
            if( responseCode < 200 || responseCode > 206 ) {
                // 201 Created - the file is now present on the remote location
                // 204 No Content - there was a file with same name on same location, we replaced it
                throw new FileTransferException( "Uploading '" + uploadUrl + "' returned '"
                                                 + response.getStatusLine() + "'" );
            }
        } catch( ClientProtocolException e ) {
            log.error( "Unable to upload file!", e );
            throw new FileTransferException( e );
        } catch( IOException e ) {
            log.error( "Unable to upload file!", e );
            throw new FileTransferException( e );
        } finally {
            // the UPLOAD returns response body on error
            if( response != null && response.getEntity() != null ) {
                HttpEntity responseEntity = response.getEntity();
                // ensure that the entity content has been fully consumed and
                // the underlying stream has been closed
                try {
                    EntityUtils.consume( responseEntity );
                } catch( IOException e ) {
                    // we tried our best to release the resources
                }
            }
        }

        log.info( "Successfully uploaded '" + localFile + "' to '" + remoteDir + remoteFile + "', host "
                  + this.hostname );
    }

    @Override
    protected void finalize() throws Throwable {

        // ensure the connection is terminated
        this.disconnect();

        this.listener = null;

        super.finalize();
    }

    @Override
    public String executeCommand( String command ) throws FileTransferException {

        throw new FileTransferException( "Not implemented" );
    }

    @Override
    protected TransferListener addListener(

                                            int progressEventNumber ) {

        return null;
    }

    @Override
    protected void removeListener( TransferListener listener ) {

    }

    /**
     * Add Cookie
     *
     * @param name cookie name
     * @param value cookie value
     * @param domain cookie domain
     * @param isSecure whether the cookie is secure or not
     * @param expirationDate cookie expiration date
     * @param path cookie path
     */
    public void addCookie( String name, String value, String domain, String path, Date expirationDate,
                           boolean isSecure ) {

        if( httpContext == null ) {
            httpContext = new BasicHttpContext();
        }
        BasicCookieStore cookieStore = ( BasicCookieStore ) httpContext.getAttribute( HttpClientContext.COOKIE_STORE );
        if( cookieStore == null ) {
            cookieStore = new BasicCookieStore();
            httpContext.setAttribute( HttpClientContext.COOKIE_STORE, cookieStore );
        }

        BasicClientCookie cookie = new BasicClientCookie( name, value );
        cookie.setDomain( domain );
        cookie.setPath( path );
        cookie.setExpiryDate( expirationDate );
        cookie.setSecure( isSecure );
        cookieStore.addCookie( cookie );
    }

    /**
     * Remove a Cookie by name and path
     *
     * @param name cookie name
     * @param path cookie path
     */
    public void removeCookie( String name, String path ) {

        if( httpContext != null ) {

            BasicCookieStore cookieStore = ( BasicCookieStore ) httpContext.getAttribute( HttpClientContext.COOKIE_STORE );
            if( cookieStore != null ) {

                List<Cookie> cookies = cookieStore.getCookies();
                cookieStore.clear();
                for( Cookie cookie : cookies ) {
                    if( !cookie.getName().equals( name ) || !cookie.getPath().equals( path ) ) {
                        cookieStore.addCookie( cookie );
                    }
                }
            }
        }
    }

    /**
     * Clear all cookies
     */
    public void clearCookies() {

        if( httpContext != null ) {
            BasicCookieStore cookieStore = ( BasicCookieStore ) httpContext.getAttribute( HttpClientContext.COOKIE_STORE );
            if( cookieStore != null ) {
                cookieStore.clear();
            }
        }
    }

    @Override
    public void addCustomProperty( String key, Object value ) throws IllegalArgumentException {

        if( HTTP_HTTPS_SOCKET_BUFFER_SIZE.equals( key ) ) {
            String intMsg = "Not an integer value(positive) provided for parameter with key " + key;
            if( ! ( value instanceof Integer ) && ! ( value instanceof String ) ) {
                throw new IllegalArgumentException( intMsg );
            } else {
                int intValue = -1;
                if( value instanceof String ) {
                    try {
                        intValue = Integer.parseInt( ( String ) value );
                    } catch( NumberFormatException ex ) {
                        throw new IllegalArgumentException( intMsg );
                    }
                } else {
                    intValue = ( ( Integer ) value ).intValue();
                }
                if( intValue < 0 ) {
                    throw new IllegalArgumentException( intMsg );
                } else {
                    if( intValue > HTTP_HTTPS_SOCKET_BUFFER_SIZE_MAX_VALUE ) {
                        throw new IllegalArgumentException( "Too big value specified for socket buffer size "
                                                            + intValue + ". Maximum accepted is "
                                                            + HTTP_HTTPS_SOCKET_BUFFER_SIZE_MAX_VALUE );
                    }
                }
                customProperties.put( key, new Integer( intValue ) );
            }
        } else if( HTTP_HTTPS_UPLOAD_METHOD.equals( key ) ) {
            String uploadMethodString = value.toString();
            if( HTTP_HTTPS_UPLOAD_METHOD__PUT.equals( uploadMethodString )
                || HTTP_HTTPS_UPLOAD_METHOD__POST.equals( uploadMethodString ) ) {
                customProperties.put( key, uploadMethodString );
            } else {
                throw new IllegalArgumentException( "Ivalid value specified for HTTP(S) upload method: "
                                                    + uploadMethodString
                                                    + ". Use one of the GenericTransferClient.HTTP_HTTPS_UPLOAD_METHOD__* constants for value" );
            }
        } else if( HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION.equals( key ) ) {

            this.preemptiveBasicAuthentication = HTTP_HTTPS_PREEMPTIVE_BASIC_AUTHENTICATION__TRUE.equals( value.toString() );
        } else if( HTTP_HTTPS_UPLOAD_CONTENT_TYPE.equals( key ) ) {
            String contentTypeValue = value.toString();
            if( !StringUtils.isNullOrEmpty( contentTypeValue ) ) {
                customProperties.put( key, contentTypeValue );
            } else {
                throw new IllegalArgumentException( "Null or empty value specified for HTTP(S) upload content type" );
            }
        } else if( HTTP_HTTPS_SOCKET_READ_TIMEOUT.equals( key ) ) {
            final String errorMsg = "Not supported value for HTTP(S) socket timeout. "
                                    + "Specify value as Integer or String number in milliseconds.";
            int socketReadTimeout = -1;
            if( value instanceof Number ) {
                socketReadTimeout = ( ( Number ) value ).intValue();
            } else if( value instanceof String ) {
                try {
                    socketReadTimeout = Integer.parseInt( ( String ) value );
                } catch( NumberFormatException ex ) {
                    throw new IllegalArgumentException( errorMsg );
                }
            } else {
                throw new IllegalArgumentException( errorMsg );
            }
            if( socketReadTimeout < 0 ) {
                throw new IllegalArgumentException( "Illegal value. Negative number is specified for socket read timeout" );
            } else {
                // 0 to wait indefinitely
                customProperties.put( key, new Integer( socketReadTimeout ) );
            }
        } else if( HTTP_HTTPS_REQUEST_HEADER.equals( key ) ) {
            // add some request header

            String headerString = ( String ) value;

            int separatorIndex = headerString.indexOf( ':' );
            if( separatorIndex < 1 ) {
                throw new IllegalArgumentException( "Custom parameter with key " + HTTP_HTTPS_REQUEST_HEADER
                                                    + " requires a String value in the form '<header key>:<header value>' while '"
                                                    + value + "' is passed" );
            } else {
                requestHeaders.add( new BasicHeader( headerString.substring( 0, separatorIndex ),
                                                     headerString.substring( separatorIndex + 1 ) ) );
            }
        } else {
            throw new IllegalArgumentException( "Unknown property with key '" + key + "' is passed. "
                                                + USE_ONE_OF_THE_HTTP_HTTPS_CONSTANTS );
        }
    }

    @Override
    public void applyCustomProperties() throws IllegalArgumentException {

        Object value = customProperties.get( HTTP_HTTPS_SOCKET_BUFFER_SIZE );
        if( value == null ) {
            socketBufferSize = DEFAULT_SOCKET_BUFFER_SIZE;
        } else {
            socketBufferSize = ( Integer ) value;
        }
        log.trace( "HTTP(S) buffer size set to " + socketBufferSize + " bytes" );
    }

    /**
     * Performs some supported HTTP request. Currently <i>read only<i> requests 
     * are supported: GET, HEAD and OPTIONS
     * 
     * @param requestedHostRelativeUrl location/query without host and port like: "/my_dir/res?myParam=1"
     * @param httpMethodName any of the currently supported HTTP methods: GET, HEAD and OPTIONS
     * @param needResponse whether caller wants to get the contents returned from this request,
     * if false - then null is returned
     * @return the response content if present and requested by the caller
     * @throws FileTransferClientException
     */
    public String performHttpRequest( String requestedHostRelativeUrl, String httpMethodName,
                                      boolean needResponse ) throws FileTransferException {

        checkClientInitialized();
        final String getUrl = constructGetUrl( requestedHostRelativeUrl );

        HttpRequestBase httpMethod = null;
        if( !StringUtils.isNullOrEmpty( httpMethodName ) ) {

            httpMethodName = httpMethodName.trim().toUpperCase();
            switch( httpMethodName ){
                case "GET":
                    httpMethod = new HttpGet( getUrl );
                    break;
                case "HEAD":
                    httpMethod = new HttpHead( getUrl );
                    break;
                case "OPTIONS":
                    httpMethod = new HttpOptions( getUrl );
                    break;
            }
        }

        if( httpMethod == null ) {
            throw new IllegalArgumentException( "This method supports only GET, HEAD and OPTIONS methods while you have provided '"
                                                + httpMethodName + "'" );
        }

        log.info( "Performing " + httpMethodName + " request to: " + getUrl );

        addRequestHeaders( httpMethod );

        return ( String ) processHttpRequest( httpMethod, needResponse, true );
    }

    /**
     * Perform GET request based on the host and port specified before
     *
     * @param requestedHostRelativeUrl location/query without host and port like: "/my_dir/res?myParam=1"
     * @param needResponse whether caller needs the contents returned from this request
     * @throws FileTransferException
     */
    public String performGetRequest( String requestedHostRelativeUrl,
                                     boolean needResponse ) throws FileTransferException {

        checkClientInitialized();
        final String getUrl = constructGetUrl( requestedHostRelativeUrl );
        log.info( "Performing GET request to: " + getUrl );
        HttpGet getMethod = new HttpGet( getUrl );

        addRequestHeaders( getMethod );

        return ( String ) processHttpRequest( getMethod, needResponse, true );
    }

    public InputStream performGetRequest( String requestedHostRelativeUrl ) throws FileTransferException {

        checkClientInitialized();
        final String getUrl = constructGetUrl( requestedHostRelativeUrl );
        log.info( "Performing GET request to: " + getUrl );
        HttpGet getMethod = new HttpGet( getUrl );

        addRequestHeaders( getMethod );

        return ( InputStream ) processHttpRequest( getMethod, true, false );
    }

    /**
     * Perform HTTP POST request based on the host and port specified before
     *
     * @param requestedHostRelativeUrl location/query without host and port like: "/my_dir/res?myParam=1"
     * @param needResponse whether caller needs the contents returned from this request
     * @param paramsMap map of parameters to be sent with this POST request
     * @throws FileTransferException
     */
    public String performPostRequest( String requestedHostRelativeUrl, boolean needResponse,
                                      HashMap<String, String> paramsMap ) throws FileTransferException {

        checkClientInitialized();
        final String getUrl = constructGetUrl( requestedHostRelativeUrl );
        log.info( "Performing POST request to: " + getUrl );
        HttpPost postMethod = new HttpPost( getUrl );

        addRequestHeaders( postMethod );

        if( paramsMap != null && paramsMap.size() > 0 ) {

            List<NameValuePair> parameters = new ArrayList<NameValuePair>( paramsMap.size() );
            for( Entry<String, String> paramEntry : paramsMap.entrySet() ) {
                log.info( "Add parameter " + paramEntry.getKey() + " and value: " + paramEntry.getValue() );
                parameters.add( new BasicNameValuePair( paramEntry.getKey(), paramEntry.getValue() ) );
            }
            UrlEncodedFormEntity sendEntity = null;
            try {
                sendEntity = new UrlEncodedFormEntity( parameters, "UTF-8" );
            } catch( UnsupportedEncodingException ex ) {
                throw new FileTransferException( ex );
            }
            postMethod.setEntity( sendEntity );
        }

        return ( String ) processHttpRequest( postMethod, needResponse, true );
    }

    private void addRequestHeaders( HttpRequestBase httpMethod ) throws FileTransferException {

        // pass user credentials with the very first headers
        if( preemptiveBasicAuthentication ) {
            if( this.username == null ) {
                throw new FileTransferException( "We cannot set user credentials as the user name is not set" );
            }

            try {
                BasicScheme schema = new BasicScheme( Charset.forName( "US-ASCII" ) );
                Header authenticationHeader = schema.authenticate(
                                                                   // here we make 'empty' http request, just so we could authenticate the credentials
                                                                   new UsernamePasswordCredentials( this.username,
                                                                                                    this.userpass ),
                                                                   new HttpGet(), httpContext );
                httpMethod.addHeader( authenticationHeader );
            } catch( AuthenticationException ae ) {
                throw new FileTransferException( "Unable to add Basic Authentication header", ae );
            }
        }

        // Add the rest of the request headers
        for( Header header : requestHeaders ) {
            httpMethod.setHeader( header );
        }
    }

    protected String constructDownloadUrl( String remoteDir, String remoteFile ) {

        return "http://" + this.hostname + ":" + this.port + getPathPlusFile( remoteDir, remoteFile );
    }

    protected String constructGetUrl( String requestedHostRelativeUrl ) {

        return "http://" + this.hostname + ":" + this.port + requestedHostRelativeUrl;
    }

    protected String constructUploadUrl( String remoteDir, String remoteFile ) {

        return "http://" + this.hostname + ":" + this.port + getPathPlusFile( remoteDir, remoteFile );
    }

    /**
     * Constructs path+file string by fixing not allowed combinations.
     */
    private String getPathPlusFile( String remoteDir, String remoteFile ) {

        if( !remoteDir.startsWith( "/" ) ) {
            remoteDir = "/" + remoteDir;
        }
        if( !remoteDir.endsWith( "/" ) ) {
            remoteDir = remoteDir + "/";
        }
        if( remoteFile.startsWith( "/" ) ) {
            log.warn( "File for upload starts with slash('/') character which should be part of the path!" );
            if( remoteFile.length() > 1 ) {
                remoteFile = remoteFile.substring( 1 );
            }
        }
        return remoteDir + remoteFile;
    }

    /**
     * Perform prepared request and optionally return the response
     * @param httpRequest HttpGet, HttpPost etc
     * @param needResponse do we need the response, if false - then null is returned
     * @return the response from the request as an {@link InputStream} or a {@link String} object
     * @throws FileTransferException
     */
    private Object processHttpRequest( HttpUriRequest httpRequest, boolean needResponse,
                                       boolean returnAsString ) throws FileTransferException {

        HttpEntity responseEntity = null;

        boolean responseNotConsumed = true;
        OutputStream outstream = null;
        String errMsg = "Unable to complete request!";
        String responseAsString = null;
        InputStream responseAsStream = null;
        try {
            // send request
            HttpResponse response = httpClient.execute( httpRequest, httpContext );
            if( 200 != response.getStatusLine().getStatusCode() ) {
                throw new FileTransferException( "Request to '" + httpRequest.getURI() + "' returned "
                                                 + response.getStatusLine() );
            }

            // get the response
            responseEntity = response.getEntity();
            if( !needResponse ) {
                responseNotConsumed = true;
            } else {
                if( responseEntity != null ) {
                    if( !returnAsString ) {
                        responseAsStream = responseEntity.getContent();
                    } else {
                        int respLengthEstimate = ( int ) responseEntity.getContentLength();
                        if( respLengthEstimate < 0 ) {
                            respLengthEstimate = 1024;
                        }
                        outstream = new ByteArrayOutputStream( respLengthEstimate );
                        responseEntity.writeTo( outstream );
                        outstream.flush();
                        responseAsString = outstream.toString();
                    }
                    responseNotConsumed = false;
                }
            }
        } catch( ClientProtocolException e ) {
            log.error( errMsg, e );
            throw new FileTransferException( e );
        } catch( IOException e ) {
            log.error( errMsg, e );
            throw new FileTransferException( e );
        } finally {
            if( responseEntity != null && outstream != null ) {
                IoUtils.closeStream( outstream );
            }

            if( responseNotConsumed ) {
                try {
                    // We were not able to properly stream the entity content to the local file system.
                    // The next line consumes the entity content closes the underlying stream.
                    EntityUtils.consume( responseEntity );
                } catch( IOException e ) {
                    // we tried our best to release the resources
                }
            }
        }

        log.info( "Successfully completed GET request '" + httpRequest.getURI() + "'" );
        if( returnAsString ) {
            return responseAsString;
        }
        return responseAsStream;

    }

    private void createNewHttpClientWithCredentials( String hostname, String userName,
                                                     String password ) throws FileTransferException {

        log.debug( "Creating new HTTP client to host " + hostname );
        httpBuilder = HttpClientBuilder.create();

        RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setConnectTimeout( this.timeout );
        httpBuilder.setDefaultRequestConfig( requestConfig.build() );

        SocketConfig.Builder socket = SocketConfig.custom();
        socket.setRcvBufSize( this.socketBufferSize );
        socket.setSndBufSize( this.socketBufferSize );

        PoolingHttpClientConnectionManager conManager = new PoolingHttpClientConnectionManager();
        // set stale connection check 
        conManager.setValidateAfterInactivity( -1 );
        httpBuilder.setConnectionManager( conManager );
        Object socketTimeout = customProperties.get( HTTP_HTTPS_SOCKET_READ_TIMEOUT );
        if( socketTimeout != null ) {
            // TODO this could be set either with the request config
            socket.setSoTimeout( Integer.parseInt( socketTimeout.toString() ) );
        }
        httpBuilder.setDefaultSocketConfig( socket.build() );
        if( httpClient != null ) { // cleanup previous client
            disconnect();
        }
        if( userName != null ) {
            BasicCredentialsProvider credentials = new BasicCredentialsProvider();
            credentials.setCredentials( new AuthScope( hostname, this.port ),
                                        new UsernamePasswordCredentials( userName, password ) );
            httpBuilder.setDefaultCredentialsProvider( credentials );
        }
        httpClient = httpBuilder.build();
    }

    private void checkClientInitialized() throws FileTransferException {

        if( httpClient == null ) {
            throw new FileTransferException( "Http client is not initialized. Check if any connect method is invoked before." );
        }
    }
}
