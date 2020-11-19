/*
 * Copyright 2017-2020 Axway Software
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
package com.axway.ats.action.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.MultiPartMediaTypes;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.exceptions.RestException;
import com.axway.ats.action.json.JsonText;
import com.axway.ats.action.xml.XmlText;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.SslUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * A utility class for working with REST requests and responses
 *
 * <br><br>
 * <b>User guide</b> page related to this class is
 * <a href="https://axway.github.io/ats-framework/REST-Operations.html">here</a>
 *
 */
@PublicAtsApi
public class RestClient {

    static {
        /*
        * Fix for bug when doing Jersey multithreaded calls over non default SSL protocol version.
        * For example if more than one threads are doing TLSv1.2 calls, it happens that some of them are using the older protocol version TLSv1.
        *
        * The reason is that org.glassfish.jersey.client.internal.HttpUrlConnector@secureConnection checks whether
        * the currently used SSL Socket Factory is the JDK's default one, if yes - then it sets it to the right one as
        * configured by the user. But since the JDK's method for getting the default factory is not synchronized
        * different threads may get different instances of the factory.
        * Jersey checks the instance with the '==' operator and decides that some threads are not using the default factory so it will not set the right one.
        * That is why these threads will use the default factory and currently the default protocol version appears to be TLSv1
        *
        * Found in Jersey 2.22.2, related bug entry is at
        *   https://java.net/jira/browse/JERSEY-3124
        * It is closed as found the actual bug is in the java distribution:
        *   http://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8160347
        *   https://bugs.openjdk.java.net/browse/JDK-8160347
        * it is applicable to java 6, 7, 8 and 9
        *
        * The workaround is to load the JDK's default SSL Socket Factory before any of the work threads did it.
        */
        HttpsURLConnection.getDefaultSSLSocketFactory();
    }

    /**
     * REST debug levels
     */
    public class RESTDebugLevel {
        public static final int NONE       = 0x00;
        public static final int TARGET_URI = 0x01;
        public static final int HEADERS    = 0x02 | TARGET_URI;
        public static final int BODY       = 0x04 | TARGET_URI;
        public static final int ALL        = HEADERS | BODY;
    }

    private int                       debugLevel                         = RESTDebugLevel.TARGET_URI;

    public static final String        APACHE_CONNECTOR_CLASSNAME         = "org.glassfish.jersey.apache.connector.ApacheConnectorProvider";
    public static final String        APACHE_CLIENT_PROPERTIES_CLASSNAME = "org.glassfish.jersey.apache.connector.ApacheClientProperties";

    private static final String       APACHE_HTTP_HEADERS_LOGGER_NAME    = "org.apache.http.headers";
    private static final String       APACHE_HTTP_WIRE_LOGGER_NAME       = "org.apache.http.wire";

    private static final Logger       log                                = LogManager.getLogger(RestClient.class);

    private static final String       COOKIE_HEADER_VALUE_DELIMITER      = "; ";

    private static boolean            verbosityLevelMessageLogged        = false;

    private static boolean            bodyOnlyDebugLevelMessageLogged    = false;

    private Client                    client;
    private WebTarget                 webTarget;
    private Invocation.Builder        invocationBuilder;

    private String                    uri;
    private List<String>              resourcePath                       = new ArrayList<String>();
    private Map<String, List<Object>> requestHeaders                     = new HashMap<String, List<Object>>();

    private Map<String, List<String>> requestParameters                  = new HashMap<String, List<String>>();

    private Object                    requestMediaType;
    private String                    requestMediaCharset;

    private String                    responseMediaType;
    private String                    responseMediaCharset;

    private List<Cookie>              cookies                            = new ArrayList<Cookie>();

    // Basic authorization info
    private String                    username;
    private String                    password;

    private String[]                  supportedProtocols                 = new String[]{ "TLSv1.2" };

    private RestClientConfigurator    clientConfigurator                 = new RestClientConfigurator();

    private boolean                   requestFilterNeedsRegistration     = false;

    private boolean                   requestFilterAlreadyRegistered     = false;

    private boolean                   bufferResponse                     = true;

    private boolean                   invalidateClient                   = true;

    private boolean                   usePooling                         = false;

    /**
     * Constructor not specifying the target URI.
     * You have to specify one using the appropriate set method
     */
    @PublicAtsApi
    public RestClient() {

        this(null);
    }

    /**
     * Constructor specifying the target URI
     *
     * @param uri the target URI
     */
    @PublicAtsApi
    public RestClient( String uri ) {

        setURI(uri);

        initInternalVariables();
    }

    /**
     * In most cases you do not need to use this method, but here are the exceptions:
     *
     * <ul>
     *      <li>If you do not read the response entity</li>
     *      <li>If the response entity is read into an InputStream but you do not call close on that InputStream</li>
     * </ul>
     */
    @PublicAtsApi
    public void disconnect() {

        /* If you don't read the entity, then you need to close the response manually by response.close().
         * Also if the entity is read into an InputStream (by response.readEntity(InputStream.class)),
         * the connection stays open until you finish reading from the InputStream.
         * In that case, the InputStream or the Response should be closed manually at the end of reading from InputStream.
         */
        if (this.client != null) {
            this.client.close();
            this.client = null;
            this.invocationBuilder = null;
            this.webTarget = null;
            this.invalidateClient = true;
            /* After invoking close() on the JerseyClient instance,
             * we must remove it from the map,
             * because all of its resources are deallocated and the client is not usable anymore
             */
        }
    }

    /**
     * This gives you another instance of same client, except session-related information (cookies are not copied)
     *
     * For example if need to work with 2 identical client copies, but each of them pointing to a different HTTP resources.
     * Using this method you will not need to repeat the steps for setting headers, request parameters, authentication info, etc.
     *
     * @return another instance of same client
     */
    @PublicAtsApi
    public RestClient newCopy() {

        RestClient newClient = new RestClient();
        newClient.uri = this.uri;

        newClient.username = this.username;
        newClient.password = this.password;

        newClient.resourcePath = new ArrayList<String>();
        for (String path : this.resourcePath) {
            newClient.resourcePath.add(path);
        }

        newClient.requestHeaders = new HashMap<String, List<Object>>();
        for (Entry<String, List<Object>> entry : this.requestHeaders.entrySet()) {
            newClient.requestHeaders.put(entry.getKey(), entry.getValue());
        }

        newClient.requestParameters = new HashMap<String, List<String>>();
        for (Entry<String, List<String>> requestParameterEntry : this.requestParameters.entrySet()) {
            newClient.requestParameters.put(requestParameterEntry.getKey(),
                                            new ArrayList<String>(requestParameterEntry.getValue()));
        }

        newClient.requestMediaType = this.requestMediaType;
        newClient.requestMediaCharset = this.requestMediaCharset;

        newClient.responseMediaType = this.responseMediaType;
        newClient.responseMediaCharset = this.responseMediaCharset;

        newClient.clientConfigurator = this.clientConfigurator.newCopy();

        newClient.cookies = new ArrayList<Cookie>();
        /*for (Cookie cookie : this.cookies) {
            newClient.cookies.add(cookie);
        }*/
        //newClient.client = this.client;
        //newClient.webTarget = this.webTarget;
        //newClient.invocationBuilder = this.invocationBuilder;

        newClient.supportedProtocols = this.supportedProtocols;

        //newClient.debugLevel = this.debugLevel;
        newClient.setVerboseMode(this.debugLevel);

        newClient.bufferResponse = this.bufferResponse;

        newClient.invalidateClient = true;//this.invalidateClient;

        newClient.usePooling = this.usePooling;

        return newClient;
    }

    /**
     * This method must be called by each constructor and after the execution of each HTTP method
     */
    private void initInternalVariables() {

        ActionLibraryConfigurator actionLibraryConfigurator = ActionLibraryConfigurator.getInstance();

        if (!actionLibraryConfigurator.getRestKeepResourcePath()) {
            resourcePath.clear();
        }
        if (!actionLibraryConfigurator.getRestKeepRequestHeaders()) {
            requestHeaders.clear();
        }
        if (!actionLibraryConfigurator.getRestKeepRequestParameters()) {
            requestParameters.clear();
        }

        // request media type
        if (!actionLibraryConfigurator.getRestKeepRequestMediaType()) {
            requestMediaType = RestMediaType.checkValueIsValid(actionLibraryConfigurator.getRestDefaultRequestMediaType());
        }
        if (!actionLibraryConfigurator.getRestKeepRequestMediaCharset()) {
            requestMediaCharset = actionLibraryConfigurator.getRestDefaultRequestMediaCharset();
        }

        // response media type
        if (!actionLibraryConfigurator.getRestKeepResponseMediaType()) {
            responseMediaType = RestMediaType.checkValueIsValid(actionLibraryConfigurator.getRestDefaultResponseMediaType());
        }
        if (!actionLibraryConfigurator.getRestKeepResponseMediaCharset()) {
            responseMediaCharset = actionLibraryConfigurator.getRestDefaultResponseMediaCharset();
        }
    }

    /**
     * Get the target URI
     *
     * @return the target URI
     */
    public String getURI() {

        return this.uri;
    }

    /**
     * Set the target URI
     *
     * @param uri the target URI
     *
     * @return this client's instance
     */
    public RestClient setURI( String uri ) {

        this.uri = uri;

        // should we clear some cookies as well, since a new URI is set?

        this.invalidateClient = true;

        return this;
    }

    /**
     * Set basic authorization parameters
     *
     * @param username the user name
     * @param password the user password
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient setBasicAuthorization( String username, String password ) {

        this.username = username;
        this.password = password;

        this.invalidateClient = true;

        return this;
    }

    /**
     * Set the encryption protocol to use. Default is TLSv1.2. Others might be: TLSv1, TLSv1.1
     * <br />For more information on names you may see Protocol names in "Additional JSSE Standard Names" section
     * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#jssenames">here</a>
     *
     * @param supportedProtocols preferred protocols. <em>Note</em> that currently only the first one will be used!
     */
    public RestClient setSupportedProtocols( String[] supportedProtocols ) {

        if (supportedProtocols == null) {
            throw new IllegalArgumentException("Supported protocols could not be null");
        } else if (supportedProtocols.length == 0) {
            throw new IllegalArgumentException("Supported protocols could not be empty array");
        } else {
            if (supportedProtocols.length > 1) {
                log.warn("Multiple protocols provided, but only the first one will be used");
            }
            String firstProtocol = supportedProtocols[0];
            if (StringUtils.isNullOrEmpty(firstProtocol)) {
                throw new IllegalArgumentException("The first protocol could not be null/empty");
            } else {
                if (firstProtocol.contains(",")) {
                    throw new IllegalArgumentException("Multi-value protocols '" + supportedProtocols[0]
                                                       + "' are not supported. Please specify only one");
                }
            }
        }

        // trim the first value, because otherwise an error for Unsupported protocol is thrown later
        supportedProtocols[0] = supportedProtocols[0].trim();

        this.supportedProtocols = supportedProtocols;

        this.invalidateClient = true;

        return this;
    }

    /**
     * Get the supported protocols
     * @return
     */
    public String[] getSupportedProtocols() {

        return this.supportedProtocols;
    }

    /**
     * If the URI is not fully specified in the constructor,
     * you can navigate to an internal resource.<br>
     * For example you can pass:
     * <ul>
     *   <li>"company"</li>
     *   <li>"company/department"</li>
     *   <li>new String{"company", "department"}</li>
     * </ul>
     * @param resourcePathArray the resource path(one or many tokens)
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient addResourcePath( String... resourcePathArray ) {

        if (resourcePathArray != null) {
            for (String resourcePathToken : resourcePathArray) {
                for (String token : resourcePathToken.split("/")) {
                    if (token.length() > 0) {
                        this.resourcePath.add(token);
                        this.invalidateClient = true;
                    }
                }
            }
        } else {
            log.warn("Null provided as resource path to add. Skipped");
        }

        return this;
    }

    /**
     * Add a header to the request. Existing header values are kept.
     * If you want to overwrite existing header then use setRequestHeader method
     * 
     * @param name header name
     * @param value header value
     *
     * @return this client's instance
     * @see #setRequestHeader(String, String) setRequestHeader(String, String) method
     */
    @PublicAtsApi
    public RestClient addRequestHeader( String name, String value ) {

        if (hasHeader(name)) {
            List<Object> values = new ArrayList<>(requestHeaders.get(name));
            values.add(value);
            requestHeaders.remove(name);
            requestHeaders.put(name, values);

        } else {
            List<Object> values = new ArrayList<>();
            values.add(value);
            requestHeaders.put(name, values);
        }

        this.invalidateClient = true;

        return this;
    }

    /**
     * Add a INT header to the request
     *
     * This method is deprecated in order to shorten the list of public methods.
     *
     * @param name header name
     * @param value header value
     *
     * @return this client's instance
     * @see #addRequestHeader(String, String) addRequestHeader(String, String)
     * @deprecated
     */
    @Deprecated
    @PublicAtsApi
    public RestClient addRequestHeader( String name, int value ) {

        if (hasHeader(name)) {
            List<Object> values = new ArrayList<>(requestHeaders.get(name));
            values.add(value);
            requestHeaders.remove(name);
            requestHeaders.put(name, values);

        } else {
            List<Object> values = new ArrayList<>();
            values.add(value);
            requestHeaders.put(name, values);
        }

        this.invalidateClient = true;

        return this;
    }

    /**
     * Add a LONG header to the request
     *
     * This method is deprecated in order to shorten the list of public methods.
     * 
     * @param name header name
     * @param value header value
     *
     * @return this client's instance
     * @see #addRequestHeader(String, String) addRequestHeader(String, String)
     * @deprecated
     */
    @Deprecated
    @PublicAtsApi
    public RestClient addRequestHeader( String name, long value ) {

        if (hasHeader(name)) {
            List<Object> values = new ArrayList<>(requestHeaders.get(name));
            values.add(value);
            requestHeaders.remove(name);
            requestHeaders.put(name, values);

        } else {
            List<Object> values = new ArrayList<>();
            values.add(value);
            requestHeaders.put(name, values);
        }

        this.invalidateClient = true;

        return this;
    }

    /**
     * Set header to the request. If there is already such header then it is overwritten.
     *
     * @param name header name
     * @param value header value
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient setRequestHeader( String name, String value ) {

        List<Object> values = new ArrayList<>();
        values.add(value);
        requestHeaders.put(name, values);

        this.invalidateClient = true;

        return this;
    }

    /**
     * Remove a header from the request
     *
     * @param name the name of the request header
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient removeRequestHeader( String name ) {

        boolean atLeastOneHeaderFound = false;

        Iterator<String> keys = this.requestHeaders.keySet().iterator();

        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equalsIgnoreCase(name)) {
                keys.remove();
                atLeastOneHeaderFound = true;
            }
        }

        if (!atLeastOneHeaderFound) {
            log.warn("Header with name '" + name
                     + "' will not be removed since it was not found in request headers.");
        } else {
            this.invalidateClient = true;
        }

        return this;
    }

    /**
     * Remove one or more headers from the request
     *
     * @param names the names of the request headers
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient removeRequestHeaders( String... names ) {

        Iterator<String> keys = this.requestHeaders.keySet().iterator();

        while (keys.hasNext()) {
            String key = keys.next();
            for (String name : names) {
                if (key.equalsIgnoreCase(name)) {
                    keys.remove();
                    this.invalidateClient = true;
                }
            }
        }

        return this;

    }

    /**
     * Add one or more values for one request(also called query) parameter<br><br>
     *
     * The following example adds a language request parameter:<br>
     * http://example.com/?language=eng
     *
     * @param name parameter name
     * @param values parameter value(s)
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient addRequestParameter( String name, String... values ) {

        List<String> valuesList = new ArrayList<String>();

        for (String value : values) {
            valuesList.add(value);
        }

        requestParameters.put(name, valuesList);

        this.invalidateClient = true;

        return this;
    }

    /**
     * Add a list of values for one request(also called query) parameter<br><br>
     *
     * The following example adds a language request parameter:<br>
     * http://example.com/?language=eng
     *
     * @param name parameter name
     * @param values parameter value(s)
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient addRequestParameter( String name, List<String> values ) {

        requestParameters.put(name, values);

        this.invalidateClient = true;

        return this;
    }

    /**
     * Add one or more request(also called query) parameters<br></br/>
     *
     * The following example adds a language request parameter:<br>
     * http://example.com/?language=eng
     *
     * @param requestParameters map with parameter names and values
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient addRequestParameters( Map<String, List<String>> requestParameters ) {

        for (Entry<String, List<String>> valueEntry : requestParameters.entrySet()) {
            this.requestParameters.put(valueEntry.getKey(), valueEntry.getValue());
        }

        this.invalidateClient = true;

        return this;
    }

    /**
     * Remove a request(also called query) parameter<br></br/>
     *
     * @param name the name of the parameter
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient removeRequestParameter( String name ) {

        Iterator<String> keys = this.requestParameters.keySet().iterator();

        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equals(name)) {
                keys.remove();
                this.invalidateClient = true;
                return this;
            }
        }

        log.warn("Parameter with name '" + name
                 + "' will not be removed since it was not found in request parameters.");

        return this;

    }

    /**
     * Remove one or more request(also called query) parameters<br></br/>
     *
     * @param names the names of the parameters
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient removeRequestParameters( String... names ) {

        Iterator<String> keys = this.requestParameters.keySet().iterator();

        while (keys.hasNext()) {
            String key = keys.next();
            for (String name : names) {
                if (key.equals(name)) {
                    keys.remove();
                    this.invalidateClient = true;
                }
            }
        }

        return this;

    }

    /**
     * Add a request cookie
     * @param cookie the cookie to add
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient addCookie( Cookie cookie ) {

        this.cookies.add(cookie);

        this.invalidateClient = true;

        return this;
    }

    /**
     * Clears the current list of cookies
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient clearCookies() {

        this.cookies.clear();

        this.invalidateClient = true;

        return this;
    }

    /**
     * Set the request HTTP media type.
     * This is the value of the "Content-Type" header.<br>
     * <b>Note:</b> You should pass one of the constants defined in RESTMediaType class
     * @param mediaType the request media type
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient setRequestMediaType( String mediaType ) {

        this.requestMediaType = RestMediaType.checkValueIsValid(mediaType);

        switch ((String) this.requestMediaType) {
            case RestMediaType.MULTIPART_FORM_DATA:
                this.requestMediaType = MultiPartMediaTypes.createFormData().toString();
                break;
            case RestMediaType.MULTIPART_MIXED:
                this.requestMediaType = MultiPartMediaTypes.createMixed().toString();
                break;
            default:
                // do nothing
                break;
        }

        this.invalidateClient = true;

        return this;
    }

    /**
     * Set the request HTTP media type and charset.
     * This is the value of the "Content-Type" header.<br>
     * <b>Note:</b> You should pass one of the constants defined in RESTMediaType class
     *
     * @param mediaType the request media type
     * @param mediaCharset the request media charset
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient setRequestMediaType( String mediaType, String mediaCharset ) {

        this.requestMediaType = RestMediaType.checkValueIsValid(mediaType);

        switch ((String) this.requestMediaType) {
            case RestMediaType.MULTIPART_FORM_DATA:
                this.requestMediaType = MultiPartMediaTypes.createFormData().toString();
                break;
            case RestMediaType.MULTIPART_MIXED:
                this.requestMediaType = MultiPartMediaTypes.createMixed().toString();
                break;
            default:
                // do nothing
                break;
        }

        this.requestMediaCharset = mediaCharset;

        this.invalidateClient = true;

        return this;
    }

    /**
     * Set the response HTTP media type.
     * This is the value of the "Accept" header.<br>
     * <b>Note:</b> You should pass one of the constants defined in RESTMediaType class
     * @param mediaType the response media type
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient setResponseMediaType( String mediaType ) {

        this.responseMediaType = RestMediaType.checkValueIsValid(mediaType);

        this.invalidateClient = true;

        return this;
    }

    /**
     * Set the response HTTP media type and charset.
     * These are the values of "Accept" and "Accept-Charset" headers.<br>
     * <b>Note:</b> You should pass one of the constants defined in RESTMediaType class
     * @param mediaType the response media type
     * @param mediaCharset the response media charset
     *
     * @return this client's instance
     */
    @PublicAtsApi
    public RestClient setResponseMediaType( String mediaType, String mediaCharset ) {

        this.responseMediaType = RestMediaType.checkValueIsValid(mediaType);
        this.responseMediaCharset = mediaCharset;

        this.invalidateClient = true;

        return this;
    }

    /**
     * Execute any HTTP method without request body
     *
     * @param httpMethod the HTTP method name(for example GET or POST)
     * @return the response
     */
    @PublicAtsApi
    public RestResponse execute( String httpMethod ) {

        return execute(httpMethod, null);
    }

    /**
     * Execute any HTTP method
     *
     * @param httpMethod the HTTP method name(for example GET or POST)
     * @param bodyContent the request body content(pass null if no body required)
     * @return the response
     */
    @PublicAtsApi
    public RestResponse execute( String httpMethod, Object bodyContent ) {

        if (this.invalidateClient) {
            constructInvocationBuilder("execute " + httpMethod
                                       + " against", false);
            this.invalidateClient = false;
        }
        // execute HTTP method
        RestResponse response = null;
        String errorMessage = "Content type is not set! Content type is mandatory for POST or PUT.";
        if (bodyContent != null) {
            if (this.requestMediaType == null) {
                throw new RestException(errorMessage);
            }
            if (this.requestMediaType instanceof String) {
                if (StringUtils.isNullOrEmpty((String) requestMediaType)) {
                    throw new RestException(errorMessage);
                }
                response = new RestResponse(invocationBuilder.method(httpMethod,
                                                                     Entity.entity(getActualBodyObject(bodyContent),
                                                                                   RestMediaType.toMediaType((String) requestMediaType,
                                                                                                             requestMediaCharset)),
                                                                     Response.class),
                                            this.bufferResponse);
            } else if (this.requestMediaType instanceof MediaType) {
                this.requestMediaType = ((MediaType) this.requestMediaType).withCharset(this.requestMediaCharset);
                response = new RestResponse(invocationBuilder.method(httpMethod,
                                                                     Entity.entity(getActualBodyObject(bodyContent),
                                                                                   (MediaType) this.requestMediaType),
                                                                     Response.class),
                                            this.bufferResponse);
            } else {
                throw new IllegalArgumentException("Could not construct Content-Type from object of class '"
                                                   + this.requestMediaType.getClass() + "'");
            }

        } else {
            response = new RestResponse(invocationBuilder.method(httpMethod, Response.class), this.bufferResponse);
        }

        logRESTResponse(response);
        initInternalVariables();

        // return response
        return response;
    }

    /**
     * Execute a GET REST method
     *
     * @return the response
     */
    @PublicAtsApi
    public RestResponse get() {

        if (this.invalidateClient) {
            constructInvocationBuilder("GET from", false);
            this.invalidateClient = false;
        }

        // execute GET
        RestResponse response = new RestResponse(invocationBuilder.get(), this.bufferResponse);

        logRESTResponse(response);
        initInternalVariables();

        // return response
        return response;
    }

    /**
     * Execute a POST method on an object
     *
     * @param object the object to post
     * @return the response
     */
    @PublicAtsApi
    public RestResponse postObject( Object object ) {

        if (this.invalidateClient) {
            constructInvocationBuilder("POST object to", false);
            this.invalidateClient = false;
        }

        // execute POST
        RestResponse response = null;
        String errorMessage = "Content type is not set! Content type is mandatory for POST.";
        if (object != null) {
            if (this.requestMediaType == null) {
                throw new RestException(errorMessage);
            }
            if (this.requestMediaType instanceof String) {
                if (StringUtils.isNullOrEmpty((String) requestMediaType)) {
                    throw new RestException(errorMessage);
                }
                response = new RestResponse(invocationBuilder.method("POST",
                                                                     Entity.entity(getActualBodyObject(object),
                                                                                   RestMediaType.toMediaType((String) requestMediaType,
                                                                                                             requestMediaCharset)),
                                                                     Response.class),
                                            this.bufferResponse);
            } else if (this.requestMediaType instanceof MediaType) {
                this.requestMediaType = ((MediaType) this.requestMediaType).withCharset(this.requestMediaCharset);
                response = new RestResponse(invocationBuilder.method("POST",
                                                                     Entity.entity(getActualBodyObject(object),
                                                                                   (MediaType) this.requestMediaType),
                                                                     Response.class),
                                            this.bufferResponse);
            } else {
                throw new IllegalArgumentException("Could not construct Content-Type from object of class '"
                                                   + this.requestMediaType.getClass() + "'");
            }

        } else {
            response = new RestResponse(invocationBuilder.method("POST", Response.class), this.bufferResponse);
        }

        logRESTResponse(response);
        initInternalVariables();

        // return response
        return response;
    }

    /**
    * Execute a POST method on a form
    *
    * @param restForm the form to post
    * @return the response
    */
    @PublicAtsApi
    public RestResponse postForm( RestForm restForm ) {

        if (this.invalidateClient) {
            constructInvocationBuilder("POST form to", false);
            this.invalidateClient = false;
        }

        // execute POST
        RestResponse response = new RestResponse(invocationBuilder.post(Entity.entity(restForm.getForm(),
                                                                                      MediaType.APPLICATION_FORM_URLENCODED_TYPE)),
                                                 this.bufferResponse);

        logRESTResponse(response);
        initInternalVariables();

        // return response
        return response;
    }

    /**
     * Execute a PUT method on an object
     *
     * @param object the object to put
     * @return the response
     */
    @PublicAtsApi
    public RestResponse putObject( Object object ) {

        if (this.invalidateClient) {
            if (object != null) {
                constructInvocationBuilder("PUT object to", false);
            } else {
                constructInvocationBuilder("PUT object to", true);
            }

            this.invalidateClient = false;
        }

        // execute PUT
        RestResponse response = null;
        String errorMessage = "Content type is not set! Content type is mandatory for PUT.";
        if (object != null) {
            if (this.requestMediaType == null) {
                throw new RestException(errorMessage);
            }
            if (this.requestMediaType instanceof String) {
                if (StringUtils.isNullOrEmpty((String) requestMediaType)) {
                    throw new RestException(errorMessage);
                }
                response = new RestResponse(invocationBuilder.method("PUT",
                                                                     Entity.entity(getActualBodyObject(object),
                                                                                   RestMediaType.toMediaType((String) requestMediaType,
                                                                                                             requestMediaCharset)),
                                                                     Response.class),
                                            this.bufferResponse);
            } else if (this.requestMediaType instanceof MediaType) {
                this.requestMediaType = ((MediaType) this.requestMediaType).withCharset(this.requestMediaCharset);
                response = new RestResponse(invocationBuilder.method("PUT",
                                                                     Entity.entity(getActualBodyObject(object),
                                                                                   (MediaType) this.requestMediaType),
                                                                     Response.class),
                                            this.bufferResponse);
            } else {
                throw new IllegalArgumentException("Could not construct Content-Type from object of class '"
                                                   + this.requestMediaType.getClass() + "'");
            }

        } else {
            response = new RestResponse(invocationBuilder.method("PUT", Response.class), this.bufferResponse);
        }

        logRESTResponse(response);
        initInternalVariables();

        // return response
        return response;
    }

    /**
    * Execute a PUT method on a form
    *
    * @param restForm the form to put
    * @return the response
    */
    @PublicAtsApi
    public RestResponse putForm( RestForm restForm ) {

        if (this.invalidateClient) {
            if (restForm != null) {
                constructInvocationBuilder("PUT form to", false);
            } else {
                constructInvocationBuilder("PUT form to", true);
            }

            this.invalidateClient = false;
        }

        // execute PUT
        RestResponse response;
        if (restForm != null) {
            response = new RestResponse(invocationBuilder.put(Entity.entity(restForm.getForm(),
                                                                            MediaType.APPLICATION_FORM_URLENCODED_TYPE)),
                                        this.bufferResponse);

        } else {
            response = new RestResponse(invocationBuilder.put(null), this.bufferResponse);
        }

        logRESTResponse(response);
        initInternalVariables();

        // return response
        return response;
    }

    /**
    * Execute a DELETE method
    *
    * @return the response
    */
    @PublicAtsApi
    public RestResponse delete() {

        if (this.invalidateClient) {
            constructInvocationBuilder("DELETE from", false);

            this.invalidateClient = false;
        }

        // execute DELETE
        RestResponse response = new RestResponse(invocationBuilder.delete(), this.bufferResponse);

        logRESTResponse(response);
        initInternalVariables();

        // return response
        return response;
    }

    private Object getActualBodyObject( Object bodyContent ) {

        if (bodyContent != null) {
            if (bodyContent instanceof RestForm) {
                return ((RestForm) bodyContent).getForm();
            } else if (bodyContent instanceof JsonText) {
                return ((JsonText) bodyContent).toString();
            } else if (bodyContent instanceof XmlText) {
                return ((XmlText) bodyContent).toString();
            }
        }

        return bodyContent;
    }

    /**
     * Get the client configurator
     * @return the client configurator
     */
    @PublicAtsApi
    public RestClientConfigurator getClientConfigurator() {

        return this.clientConfigurator;
    }

    /**
     * Set the client configurator
     *
     * @param clientConfigurator the client configurator
     */
    @PublicAtsApi
    public RestClient setClientConfigurator( RestClientConfigurator clientConfigurator ) {

        this.clientConfigurator = clientConfigurator;

        return this;
    }

    /**
     * Log the request/response data you need.
     * <pre>client.setRESTDebug(RESTDebugLevel.ALL)</pre>
     *
     * @param level http debug level. Use constants in {@link RESTDebugLevel}
     */
    public RestClient setVerboseMode( int level ) {

        debugLevel = level;
        if (debugLevel != RESTDebugLevel.NONE && debugLevel != RESTDebugLevel.TARGET_URI) {
            requestFilterNeedsRegistration = true;
        }
        return this;
    }

    /**
     * Whether the response body will be buffered <strong>immediately</strong> after the HTTP request is executed by 
     * the RestClient. 
     * <ul>
     *  <li>If <code>false</code> then response body <strong>will not</strong> be consumed and buffered. <br>
     *      This could be useful when developer expects very large response.
     *      One drawback of this technique is that there should be exactly one invokation of method like getBodyAsXYZ()
     *      to consume the body. </li>
     *  <li>If <code>true</code> then body will be consumed and buffered automatically. This allows usage of the 
     *      RestClient without explicitly (in your code) to have to consume the (whole) response body. One such case is if 
     *      you verify only the returned HTTP status code or the headers.<br>
     *      It is highly recommended to set it to <code>true</code> if you are using <code>ApacheConnector</code> 
     *      provider.</li>
     * </ul> 
     * 
     * @param bufferResponse - true/false with behavior explained above. Default is true.
     * 
     */
    @PublicAtsApi
    public RestClient setBufferResponse( boolean bufferResponse ) {

        this.bufferResponse = bufferResponse;
        return this;
    }

    /**
     * Set whether a connection pool will be used to obtain connection.<br>
     * <strong>Note</strong> that this currently works only if ApacheConnectorProvider is registered
     * @param usePooling - true/false with behavior explained above.
     * */
    @PublicAtsApi
    public RestClient setUsePooling( boolean usePooling ) {

        this.usePooling = usePooling;

        return this;
    }

    private void constructInvocationBuilder( String descriptionToken,
                                             boolean suppressHttpComplianceValidation ) {

        constructUrl();

        boolean hasThirdPartyConnector = this.clientConfigurator.getConnectorProvider() != null;
        if (hasThirdPartyConnector) {

            boolean isApache = APACHE_CONNECTOR_CLASSNAME.equals(this.clientConfigurator.getConnectorProvider()
                                                                                        .getClass()
                                                                                        .getName());

            if (isApache) {

                // configure wire logging
                Logger headersLogger = LogManager.getLogger(APACHE_HTTP_HEADERS_LOGGER_NAME);
                Logger wireLogger = LogManager.getLogger(APACHE_HTTP_WIRE_LOGGER_NAME);

                if (headersLogger.isDebugEnabled() || wireLogger.isDebugEnabled()) {
                    if (!verbosityLevelMessageLogged) {
                        verbosityLevelMessageLogged = true;
                        log.info("Rest client's verbose mode will not be applied because "
                                 + "either '" + APACHE_HTTP_HEADERS_LOGGER_NAME + "' or '"
                                 + APACHE_HTTP_WIRE_LOGGER_NAME
                                 + "' Log4J logger is set to DEBUG or greater LOG level. "
                                 + "Headers " + (wireLogger.isDebugEnabled()
                                                                             ? "and body"
                                                                             : "")
                                 + " (for both request and response) will be logged");
                    }
                } else {
                    if ( (this.debugLevel & RESTDebugLevel.ALL) == RESTDebugLevel.ALL) {
                        Configurator.setLevel(headersLogger.getName(), Level.OFF);
                        Configurator.setLevel(wireLogger.getName(), Level.DEBUG);
                    } else if ( (this.debugLevel & RESTDebugLevel.BODY) == RESTDebugLevel.BODY) {
                        Configurator.setLevel(headersLogger.getName(), Level.OFF);
                        Configurator.setLevel(wireLogger.getName(), Level.OFF);
                        if (!bodyOnlyDebugLevelMessageLogged) {
                            bodyOnlyDebugLevelMessageLogged = true;
                            log.info("Debug level is set to BODY only. "
                                     + "Both '" + APACHE_HTTP_HEADERS_LOGGER_NAME + "' and '"
                                     + APACHE_HTTP_WIRE_LOGGER_NAME + "' Log4J loggers will be disabled.");
                        }
                    } else if ( (this.debugLevel & RESTDebugLevel.HEADERS) == RESTDebugLevel.HEADERS) {
                        Configurator.setLevel(headersLogger.getName(), Level.DEBUG);
                        Configurator.setLevel(wireLogger.getName(), Level.OFF);
                    } else if ( (this.debugLevel & RESTDebugLevel.TARGET_URI) == RESTDebugLevel.TARGET_URI) {
                        Configurator.setLevel(headersLogger.getName(), Level.OFF);
                        Configurator.setLevel(wireLogger.getName(), Level.OFF);
                    } else if ( (this.debugLevel & RESTDebugLevel.NONE) == RESTDebugLevel.NONE) {
                        Configurator.setLevel(headersLogger.getName(), Level.OFF);
                        Configurator.setLevel(wireLogger.getName(), Level.OFF);
                    }
                }

                constructApacheConnectorInvocationBuilder(descriptionToken,
                                                          suppressHttpComplianceValidation);
            } else {
                constructThirdPartyConnectorInvocationBuilder(descriptionToken,
                                                              suppressHttpComplianceValidation);
            }

        } else {

            constructHttpUrlConnectionInvocationBuilder(descriptionToken,
                                                        suppressHttpComplianceValidation);
        }
    }

    private void constructHttpUrlConnectionInvocationBuilder( String descriptionToken,
                                                              boolean suppressHttpComplianceValidation ) {

        // create the client config object
        ClientConfig clientConfig = createClientConfig(suppressHttpComplianceValidation);

        // create the client builder
        ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);

        // handle HTTPS requests
        if (isHttps()) {
            // configure Trust-all SSL context

            checkSupportedProtocols();

            SSLContext sslContext = SslUtils.getSSLContext(clientConfigurator.getCertificateFileName(),
                                                           clientConfigurator.getCertificateFilePassword(),
                                                           supportedProtocols[0]);

            clientBuilder = clientBuilder.sslContext(sslContext)
                                         .hostnameVerifier(new SslUtils.DefaultHostnameVerifier());
        }

        // now create the client
        createClient(clientBuilder);

        createInvocationBuilder(descriptionToken);
    }

    private boolean isHttps() {

        return this.uri.toLowerCase().startsWith("https");
    }

    private void constructApacheConnectorInvocationBuilder( String descriptionToken,
                                                            boolean suppressHttpComplianceValidation ) {

        // create the client config object
        ClientConfig clientConfig = createClientConfig(suppressHttpComplianceValidation);

        // check if user had specified custom connection manager and custom connection factory
        boolean hasConnectionManager = this.clientConfigurator.getConnectionManager() != null;
        boolean hasConnectionFactory = this.clientConfigurator.getConnectionFactory() != null;
        // handle HTTPS requests
        Registry registry = null;
        if (isHttps()) {
            // configure Trust-all SSL context
            if (!hasConnectionManager) {

            }
            registry = constructRegistry();

        }

        HttpClientConnectionManager connectionManager = null;
        HttpConnectionFactory connectionFactory = null;
        if (hasConnectionManager) {
            connectionManager = this.clientConfigurator.getConnectionManager();
            if (hasConnectionFactory) {
                connectionFactory = this.clientConfigurator.getConnectionFactory();
            } else {
                throw new RuntimeException("Connection manager was specified, but connection factory was not. "
                                           + "Provide both if you want to use custom connection manager");
            }
        } else {
            if (hasConnectionFactory) {
                connectionFactory = this.clientConfigurator.getConnectionFactory();
            } else {
                connectionFactory = new ManagedHttpClientConnectionFactory();
            }
            if (registry != null) {
                if (usePooling) {
                    connectionManager = new PoolingHttpClientConnectionManager(registry, connectionFactory);
                } else {
                    connectionManager = new BasicHttpClientConnectionManager(registry, connectionFactory);
                }

            } else {
                if (usePooling) {
                    connectionManager = new PoolingHttpClientConnectionManager(connectionFactory);
                } else {
                    connectionManager = new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory> create()
                                                                                            .register("http",
                                                                                                      PlainConnectionSocketFactory.getSocketFactory())
                                                                                            .register("https",
                                                                                                      SSLConnectionSocketFactory.getSocketFactory())
                                                                                            .build(),
                                                                             connectionFactory);
                }
            }

        }

        if (connectionManager != null && connectionManager instanceof PoolingHttpClientConnectionManager) {
            ((PoolingHttpClientConnectionManager) connectionManager).setValidateAfterInactivity(10 * 1000); // 10 sec
        }

        try {
            Class<?> apacheClientProperties = Class.forName(RestClient.APACHE_CLIENT_PROPERTIES_CLASSNAME);
            clientConfig.property((String) apacheClientProperties.getDeclaredField("CONNECTION_MANAGER")
                                                                 .get(null),
                                  connectionManager);
        } catch (Exception e) {
            throw new RuntimeException("Could not set validity timeinterval for '"
                                       + connectionManager.getClass().getName() + "'", e);
        }

        // create the client builder
        ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);

        // now create the client
        createClient(clientBuilder);

        createInvocationBuilder(descriptionToken);
    }

    private Registry constructRegistry() {

        try {
            SSLContextBuilder builder = SSLContextBuilder.create();

            builder.useProtocol(this.supportedProtocols[0]);

            if (!StringUtils.isNullOrEmpty(clientConfigurator.getCertificateFileName())) {
                builder.loadKeyMaterial(SslUtils.loadKeystore(clientConfigurator.getCertificateFileName(),
                                                              clientConfigurator.getCertificateFilePassword()),
                                        clientConfigurator.getCertificateFilePassword().toCharArray());
            }

            // Trust all certificates
            builder.loadTrustMaterial(new TrustStrategy() {
                @Override
                public boolean isTrusted( X509Certificate[] chain, String authType ) throws CertificateException {

                    return true;
                }
            });
            SSLContext sslContext = builder.build();

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                                                                              new NoopHostnameVerifier());

            Registry registry = RegistryBuilder.create().register("https", sslsf).build();

            return registry;
        } catch (Exception e) {
            throw new RuntimeException("Unable to setup SSL context for REST client with Apache connector provider", e);
        }
    }

    private void constructThirdPartyConnectorInvocationBuilder( String descriptionToken,
                                                                boolean suppressHttpComplianceValidation ) {

        // create the client config object
        ClientConfig clientConfig = createClientConfig(suppressHttpComplianceValidation);

        // create the client builder
        ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);

        // handle HTTPS requests
        if (isHttps()) {
            // configure Trust-all SSL context

            checkSupportedProtocols();

            SSLContext sslContext = SslUtils.getSSLContext(clientConfigurator.getCertificateFileName(),
                                                           clientConfigurator.getCertificateFilePassword(),
                                                           supportedProtocols[0]);

            clientBuilder = clientBuilder.sslContext(sslContext)
                                         .hostnameVerifier(new SslUtils.DefaultHostnameVerifier());
        }

        // now create the client
        createClient(clientBuilder);

        createInvocationBuilder(descriptionToken);

    }

    private void checkSupportedProtocols() {

        final String HTTPS_PROTOCOLS_KEY = "https.protocols";

        String systemPropertyProtocols = System.getProperty(HTTPS_PROTOCOLS_KEY);

        if (StringUtils.isNullOrEmpty(systemPropertyProtocols)) {
            return;
        }

        if (systemPropertyProtocols.contains(" ")) {
            // 0x20 = space (" ")
            log.error("The system property '" + HTTPS_PROTOCOLS_KEY + " (" + systemPropertyProtocols + ")"
                      + "' have invalid character - space (0x20)");
        }

        String[] systemPropertyProtocolsArray = systemPropertyProtocols.split(Pattern.quote(","));

        String supportedProtocol = this.supportedProtocols[0];
        boolean found = false;
        for (String protocol : systemPropertyProtocolsArray) {
            if (protocol.equalsIgnoreCase(supportedProtocol)) {
                found = true;
                break;
            }
        }

        if (!found) {
            log.error("The provided HTTP protocol '" + supportedProtocol
                      + "' not found in the value of the system property '" + HTTPS_PROTOCOLS_KEY + " ( "
                      + systemPropertyProtocols + ")"
                      + "'. This can lead to an error");
        }

    }

    private void createInvocationBuilder( String descriptionToken ) {

        webTarget = client.target(this.uri);

        // navigate to internal resource
        for (String token : resourcePath) {
            webTarget = webTarget.path(token);
        }

        // add request parameters
        for (Entry<String, List<String>> requestParamEntry : requestParameters.entrySet()) {
            for (String requestParamValue : requestParamEntry.getValue()) {
                webTarget = webTarget.queryParam(requestParamEntry.getKey(), requestParamValue);
            }
        }
        if ( (debugLevel & RESTDebugLevel.TARGET_URI) == RESTDebugLevel.TARGET_URI) {
            log.info("We will " + descriptionToken + " " + webTarget.getUri());
        }

        invocationBuilder = webTarget.request();

        // set response media type
        if (!StringUtils.isNullOrEmpty(responseMediaType)) {
            invocationBuilder = invocationBuilder.accept(RestMediaType.toMediaType(responseMediaType,
                                                                                   responseMediaCharset));
        }

        // add request headers
        for (Entry<String, List<Object>> requestHeaderEntry : requestHeaders.entrySet()) {
            List<Object> headerValues = requestHeaderEntry.getValue();
            for (Object headerValue : headerValues) {
                if (requestHeaderEntry.getKey().equalsIgnoreCase("content-length")) {
                    if (isApacheConnectorProviderInUse()) {
                        log.warn("Apache connector does not support setting custom Content-Length header. "
                                 + "An automatic one will be added by the underlying connector provider");
                        continue;
                    }
                }
                invocationBuilder.header(requestHeaderEntry.getKey(), headerValue);
            }
        }

        // add request cookies
        StringBuilder sb = new StringBuilder();
        for (Cookie cookie : cookies) {
            sb.append(cookieToHeaderValue(cookie) + COOKIE_HEADER_VALUE_DELIMITER);
        }

        invocationBuilder.header("Cookie", sb.toString());

    }

    /**
     * Convert Cookie to a HTTP Header value
     * @param cookie - the cookie
     * @return a proper String representation of that cookie, suitable for sending to the HTTP server as a header
     * */
    private String cookieToHeaderValue( Cookie cookie ) {

        String name = cookie.getName();
        String value = cookie.getValue();

        StringBuilder sb = new StringBuilder();

        sb.append(name + "=" + value);

        return sb.toString();
    }

    private ClientConfig createClientConfig(
                                             boolean suppressHttpComplianceValidation ) {

        ClientConfig clientConfig = new ClientConfig();

        // register third-party connector provider
        if (this.clientConfigurator.getConnectorProvider() != null) {

            Map<String, Object> connectorProviderProperties = this.clientConfigurator.getConnectorProviderProperties();
            if (connectorProviderProperties != null
                && !connectorProviderProperties.isEmpty()) {
                for (Entry<String, Object> propEntry : connectorProviderProperties.entrySet()) {
                    clientConfig.property(propEntry.getKey(), propEntry.getValue());
                }
            }
            try {
                clientConfig.connectorProvider(clientConfigurator.getConnectorProvider());
            } catch (Exception e) {
                throw new RuntimeException("Unable to register connector provider '"
                                           + clientConfigurator.getConnectorProvider().getClass().getName() + "'");
            }
        }

        // attach any configuration providers instances or classes
        // (e.g. features or individual entity providers, filters or interceptors)
        for (Object provider : clientConfigurator.getProviders()) {
            clientConfig.register(provider);
        }
        for (Class<?> providerClass : clientConfigurator.getProviderClasses()) {
            clientConfig.register(providerClass);
        }

        // attach any configuration properties
        Map<String, Object> properties = clientConfigurator.getProperties();
        for (Entry<String, Object> propertyEntry : properties.entrySet()) {
            clientConfig.property(propertyEntry.getKey(), propertyEntry.getValue());
        }

        if (suppressHttpComplianceValidation == true) { // not default value
            if (properties.containsKey(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION)) { // user provided value found
                boolean userProvidedValue = (boolean) properties.get(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION);
                if (userProvidedValue != suppressHttpComplianceValidation) {
                    // ignore our value
                    log.info("You are executing PUT with null body and SUPPRESS_HTTP_COMPLIANCE_VALIDATION is set to false. Expect operation to fail.");
                } else {
                    // set our value
                    log.warn("You are executing PUT operation with null body. Expect the client implementation to complain.");
                    clientConfig.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION,
                                          suppressHttpComplianceValidation);
                }
            } else { // user provided value not found
                // set our value
                log.warn("You are executing PUT operation with null body. Expect the client implementation to complain.");
                clientConfig.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION,
                                      suppressHttpComplianceValidation);
            }
        }

        // basic authorization
        if (username != null) {
            clientConfig.register(HttpAuthenticationFeature.basic(username, password));
        }

        return clientConfig;
    }

    private URL constructUrl() {

        if (StringUtils.isNullOrEmpty(this.uri)) {
            throw new IllegalArgumentException("Null or empty target URI. Please specify a valid one");
        }

        URL url;
        try {
            url = new URL(this.uri);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Please specify a valid URI. You have provided '" + this.uri
                                               + "'");
        }

        return url;
    }

    /*private KeyStore convertToKeyStore( String certificateFileName ) {
    
        try {
            X509Certificate[] certificates = new X509Certificate[]{ SslUtils.convertFileToX509Certificate(new File(certificateFileName)) };
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            if (certificates != null) {
                for (X509Certificate certificate : certificates) {
                    keystore.setCertificateEntry(certificate.getSubjectDN().getName(), certificate);
                }
            }
            return keystore;
        } catch (Exception e) {
            throw new RestException("Failed to create keystore from certificate", e);
        }
    }*/

    private void createClient( ClientBuilder newClientBuilder ) {

        client = newClientBuilder.build();
        if (requestFilterNeedsRegistration && !requestFilterAlreadyRegistered) {
            RequestFilter requestFilter = new RequestFilter();
            client.register(requestFilter);
            requestFilterNeedsRegistration = false;
            requestFilterAlreadyRegistered = true;
        }
    }

    private boolean isApacheConnectorProviderInUse() {

        boolean hasThirdPartyConnector = this.clientConfigurator.getConnectorProvider() != null;
        if (hasThirdPartyConnector) {
            boolean isApache = APACHE_CONNECTOR_CLASSNAME.equals(this.clientConfigurator.getConnectorProvider()
                                                                                        .getClass()
                                                                                        .getName());
            return isApache;
        } else {
            return false;
        }

    }

    private void logRESTResponse( RestResponse response ) {

        if (debugLevel == RESTDebugLevel.NONE || debugLevel == RESTDebugLevel.TARGET_URI
            || isApacheConnectorProviderInUse()) {
            return;
        }

        StringBuilder responseMessage = new StringBuilder();
        responseMessage.append("Receiving the following response: \n");
        if ( (debugLevel & RESTDebugLevel.HEADERS) == RESTDebugLevel.HEADERS) {

            for (RestHeader headerName : response.getHeaders()) {
                responseMessage.append(headerName.getKey() + ": " + headerName.getValue() + "\n");
            }
        }
        if ( (debugLevel & RESTDebugLevel.BODY) == RESTDebugLevel.BODY
             && response.getContentLength() != -1) {
            // log response body
            if (response.getContentLength() <= RestResponse.RESPONSE_SIZE_BIG_WARN) {
                responseMessage.append("Body: " + response.getBodyAsString() + "\n");
            } else {
                // if the content-length is greater than RESTResponse.MAX_RESPONSE_SIZE, truncate the response's body
                responseMessage.append("Body: "
                                       + response.getBodyAsString()
                                                 .substring(0,
                                                            RestResponse.RESPONSE_SIZE_BIG_WARN)
                                       + "... [Response body truncated.]" + "\n");
            }
        }
        if (responseMessage.length() > 0) {
            responseMessage.delete(responseMessage.length() - 1, responseMessage.length());
            log.info(responseMessage);
        }
    }

    /**
     * Checks if request header with the given key/name has already been added
     * @param name the header key/name
     * @return true if header with that key/name was already added, false otherwise
    */
    private boolean hasHeader( String name ) {

        if (StringUtils.isNullOrEmpty(name)) {
            throw new RestException("Error while adding request header. Header name/key is null or empty.");
        }

        return requestHeaders.containsKey(name);
    }

    private class RequestFilter implements ClientRequestFilter {

        private Logger log = LogManager.getLogger(RestClient.class);

        @Override
        public void filter( ClientRequestContext context ) throws IOException {

            if (debugLevel == RESTDebugLevel.NONE || debugLevel == RESTDebugLevel.TARGET_URI
                || isApacheConnectorProviderInUse()) {
                return;
            }

            MultivaluedMap<String, Object> reqHeaders = context.getHeaders();
            StringBuilder requestMessage = new StringBuilder();
            requestMessage.append("Sending the following request: \n");
            if ( (debugLevel & RESTDebugLevel.HEADERS) == RESTDebugLevel.HEADERS) {
                requestMessage.append(context.getMethod() + " " + context.getUri() + " \n");

                for (Entry<String, List<Object>> reqHeaderEntry : reqHeaders.entrySet()) {
                    requestMessage.append(reqHeaderEntry.getKey() + ": "
                                          + Arrays.toString(reqHeaderEntry.getValue().toArray()) + " \n");
                }
            }
            if ( (debugLevel & RESTDebugLevel.BODY) == RESTDebugLevel.BODY && context.hasEntity()) {
                // log request body
                Object entity = context.getEntity();
                if (entity instanceof Form) {
                    requestMessage.append("Body: " + ((Form) entity).asMap());
                } else {
                    requestMessage.append("Body: " + entity.toString());
                }
            }
            log.info(requestMessage);
        }

    }
}
