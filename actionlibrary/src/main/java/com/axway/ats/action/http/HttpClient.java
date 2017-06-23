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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.gss.GssClient;
import com.axway.ats.core.gss.spnego.SPNegoSchemeFactory;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * This is a client-side class for sending HTTP messages to an endpoint. It is possible
 * to use this client to send any type of message content to the endpoint, e.g. SOAP, plain XML,
 * JSON, binary, text etc. The following functionality is supported:-
 * <ul>
 *   <li>HTTP POST, PUT, GET, HEAD and DELETE</li>
 *   <li>HTTP Basic authentication (preemptive, or server challenge)</li>
 *   <li>HTTP Digest authentication</li>
 *   <li>SPNEGO (Kerberos) authentication</li>
 *   <li>SSL including mutual authentication of client and server sides</li>
 *   <li>Multipart requests and responses</li>
 * </ul>
 * <p>
 * The following is some sample code which shows how to use this class to send a SOAP request
 * over HTTP and receive the response:-
 * <pre>
 *    import org.apache.commons.io.IoUtils;
 *    import org.w3c.dom.Document;
 *
 *    HTTPClient httpClient = new HTTPClient();
 *    // Set the request URL
 *    httpClient.setURL("http://localhost:80/test");
 *    // Add HTTP headers
 *    httpClient.addRequestHeader("My-Header", "Some value");
 *    // Add credentials for HTTP Basic
 *    httpClient.setAuthorization("admin", "password");
 *    // Set the XML request body
 *    byte[] requestBody = IoUtils.readFileToByteArray(new File("c:/requests/SoapRequest.xml"));
 *    httpClient.setRequestBody(requestBody, "text/xml");
 *    // Send request via HTTP POST
 *    HTTPResponse response = httpClient.post();
 *    // Get response details
 *    int statusCode = response.getStatusCode();
 *    String statusMessage = response.getStatusMessage();
 *    HTTPHeader[] responseHeaders = response.getHeaders();
 *    byte[] responseBody = response.getBody();
 *    String responseBodyString = response.getBodyAsString();
 *    Document dom = response.getBodyAsXML();
 * </pre>
 *
 * <p>
 * In order to use SSL where we trust all server-side certificates, the URL
 * just needs to be modified so that the scheme is set to 'https' and the port
 * to the appropriate SSL port at the endpoint. If no trusted certificates
 * are specified, the HTTPClient will trust all server-side certificates.
 * <p>
 * In order to use SSL where we trust only certain server-side certificates(s), the
 * following code should be used:-
 * <pre>
 * httpClient.setURL("https://localhost:443/test");
 * List&lt;File&gt; trustCertFiles = new ArrayList&lt;File&gt;();
 * trustCertFiles.add(new File("c:/certs/server1.pem"));
 * trustCertFiles.add(new File("c:/certs/rootca.pem"));
 * httpClient.setTrustedServerSSLCertificates(trustCertFiles);
 * </pre>
 * Note, one or more server-side certificates may be trusted. If a chain of
 * certificates is passed to the HTTPClient, then the endpoint must present
 * a matching chain in order to to be trusted. If unrelated certificate(s) are
 * passed to the HTTPClient, then the endpoint must present one of the trusted
 * certificates.
 *
 * <p>
 * In order to use SSL with mutual authentication, this additional line of code
 * must be run to pass the client-side private key and certificate to the HTTPClient:-
 * <pre>
 * httpClient.setClientSSLCertificate(new File("c:/certs/client.p12"), "password");
 * </pre>
 *
 * <p>
 * The following code enables SPNEGO authentication:
 * <pre>
 * httpClient.setAuthorization("User1@AXWAY.COM", "password");
 * httpClient.setKerberosServicePrincipal("Service1@AXWAY.COM", "NT_USER_NAME");
 * httpClient.setKrb5ConfFile(new File("c:/kerberos/krb5.conf"));
 * </pre>
 * Alternatively, the Kerberos client password may be loaded from a keytab file:-
 * <pre>
 * httpClient.setKerberosAuthorization("User1@AXWAY.COM", new File("c:/keytabs/user1.keytab"));
 * </pre>
 */
@PublicAtsApi
public class HttpClient {
    /**
     * The type of authentication, preemptive or non-preemptive. Preemptive is only supported for HTTP Basic.
     */
    @PublicAtsApi
    public enum AuthType {
        /**
         * The Authorization header is sent after the server challenge is received, this is the default.
         * The sever challenge will determine what type of authentication the HTTPClient will send,
         * i.e. HTTP Basic, HTTP digest or SPNEGO.
         */
        demand,
        /**
         * A HTTP Basic Authorization header is sent upfront without waiting for a server-side 401 challenge.
         */
        always
    }

    /**
     * HTTP debug levels
     */
    @PublicAtsApi
    public class HttpDebugLevel {
        public static final int HEADERS = 0x01;
        public static final int BODY    = 0x02;
        public static final int NONE    = 0x00;
        public static final int ALL     = HEADERS | BODY;
    }

    private int                       debugLevel            = HttpDebugLevel.NONE;

    private String                    url;
    private String                    actualUrl;
    private List<String>              resourcePath          = new ArrayList<String>();

    private int                       connectTimeoutSeconds = 0;
    private int                       readTimeoutSeconds    = 0;

    protected AuthType                authType              = AuthType.demand;
    // Basic authorization info
    protected String                  username;
    protected String                  password;

    private CloseableHttpClient       httpClient;

    // the request body(in single or many parts)
    private HttpEntity                requestBody;
    private List<HttpBodyPart>        requestBodyParts      = new ArrayList<HttpBodyPart>();

    private Map<String, List<String>> requestParameters     = new HashMap<String, List<String>>();

    private List<HttpHeader>          requestHeaders        = new ArrayList<HttpHeader>();
    private List<HttpHeader>          actualRequestHeaders  = new ArrayList<HttpHeader>();

    // For SSL
    // The trusted SSL certificates.
    private X509Certificate[]         trustedServerCertificates;
    // For mutual SSL, the client-side SSL private key and certificate.
    private X509Certificate[]         clientSSLCertificateChain;
    private KeyStore                  clientSSLKeyStore;

    private String[]                  supportedProtocols    = new String[]{ "TLSv1.2" };
    private String[]                  supportedCipherSuites;

    // For Kerberos i.e. SPNEGO
    // Can use this or AuthPwd for Kerberos client
    private File                      kerberosClientKeytab;

    private String                    kerberosServicePrincipalName;
    private String                    kerberosServicePrincipalType;
    private File                      krb5ConfFile;

    private String                    responseBodyFilePath;

    /**
     * True if the trustedServerCertificates a chain with root CA at end of array.
     */
    boolean                           isChain               = false;

    private static final Logger       log                   = Logger.getLogger( HttpClient.class );

    /**
     * Constructor.
     */
    @PublicAtsApi
    public HttpClient() {

    }

    /**
     * Constructor providing the URL.
     *
     * @param url
     */
    @PublicAtsApi
    public HttpClient( String url ) {

        setURL( url );
    }

    /**
     * Set the HTTP URL to invoke on the endpoint.
     *
     * @param url The request URL, e.g. 'http://localhost:80/service'
     */
    @PublicAtsApi
    public void setURL( String url ) {

        this.url = url;
    }

    /**
     * Add one or more values for one request(query) parameter.<br/><br/>
     *
     * The following example adds a language request parameter:<br/>
     * http://example.com/?language=eng
     *
     * @param name the parameter name
     * @param values parameter value(s)
     */
    @PublicAtsApi
    public void addRequestParameter( String name, String... values ) {

        List<String> valuesList = new ArrayList<String>();
        for( String value : values ) {
            valuesList.add( value );
        }
        this.requestParameters.put( name, valuesList );
    }

    /**
     * Add a list of values for one request(query) parameter</br></br>
     *
     * The following example adds a language request parameter:</br>
     * http://example.com/?language=eng
     *
     * @param name the parameter name
     * @param values parameter value(s)
     */
    @PublicAtsApi
    public void addRequestParameter( String name, List<String> values ) {

        this.requestParameters.put( name, values );
    }

    /**
     * Add one or more request(query) parameters<br/><br/>
     *
     * @param requestParameters a {@link Map} of parameters. Each parameter has a {@link List} of one or more values.
     */
    @PublicAtsApi
    public void addRequestParameters( Map<String, List<String>> requestParameters ) {

        for( Entry<String, List<String>> valueEntry : requestParameters.entrySet() ) {
            this.requestParameters.put( valueEntry.getKey(), valueEntry.getValue() );
        }
    }
    
    /**
     * Remove a request(also called query) parameter<br/></br/>
     *
     * @param name the name of the parameter
     */
    @PublicAtsApi
    public void removeRequestParameter(String name){

        Iterator<String> keys = this.requestParameters.keySet().iterator();

        while( keys.hasNext() ) {
            String key = keys.next();
            if( key.equals( name ) ) {
                keys.remove();
                return;
            }
        }
        
        log.warn( "Parameter with name '" + name
                  + "' will not be removed since it was not found in request parameters." );
        
    }
    
    /**
     * Remove one or more request(also called query) parameters<br/></br/>
     *
     * @param names the names of the parameters
     */
    @PublicAtsApi
    public void removeRequestParameters(String... names){
        
        Iterator<String> keys = this.requestParameters.keySet().iterator();

        while( keys.hasNext() ) {
            String key = keys.next();
            for( String name : names ) {
                if( key.equals( name ) ) {
                    keys.remove();
                }
            }
        }
        
    }


    /**
     * Set timeouts. Timeouts default to zero, i.e. no timeout.
     *
     * @param connectTimeoutSeconds The connect timeout
     * @param readTimeoutSeconds The read timeout
     */
    @PublicAtsApi
    public void setTimeouts( int connectTimeoutSeconds, int readTimeoutSeconds ) {

        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    /**
     * If the URI is not fully specified in the constructor,
     * you can navigate to an internal resource.</br>
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
    public void addResourcePath( String... resourcePathArray ) {

        if( resourcePathArray != null ) {
            for( String resourcePathToken : resourcePathArray ) {
                for( String token : resourcePathToken.split( "/" ) ) {
                    if( token.length() > 0 ) {
                        this.resourcePath.add( token );
                    }
                }
            }
        } else {
            log.warn( "Null provided as resource path to add. Skipped" );
        }
    }

    /**
     * Add a user-specified HTTP header. If this header already has a value, the new value will be appended to
     * the list of values for this header.
     *
     * @param name The header name
     * @param value The header value
     */
    @PublicAtsApi
    public void addRequestHeader( String name, String value ) {

        addHeaderToList( requestHeaders, name, value );
    }
    
    /**
     * Remove a a user-specified HTTP header.
     * 
     * @param name the header name
     */
    @PublicAtsApi
    public void removeRequestHeader(
                                     String name ) {

        boolean atLeastOneHeaderFound = false;

        for( int i = 0; i < this.requestHeaders.size(); i++ ) {
            HttpHeader header = this.requestHeaders.get( i );
            if( header.getKey().equalsIgnoreCase( name ) ) {
                this.requestHeaders.remove( i );
                atLeastOneHeaderFound = true;
            }
        }

        if( !atLeastOneHeaderFound ) {
            log.warn( "Header with name '" + name
                      + "' will not be removed since it was not found in request headers." );
        }

    }


    /**
     * Set the user-specified HTTP request headers.
     *
     * @param headers List of HTTP headers. Each HTTP header has a list of one or more values.
     */
    @PublicAtsApi
    public void setRequestHeaders( HttpHeader[] headers ) {

        Collections.addAll( requestHeaders, headers );
    }

    /**
     * Set the request HTTP media type.</br>
     * This is a shortcut method for setting the value of the "Content-Type" header.
     */
    @PublicAtsApi
    public void setRequestMediaType( String mediaType ) {

        addHeaderToList( requestHeaders, "Content-Type", mediaType );
    }

    /**
     * Get the HTTP request headers specified by the user via
     * {@link #addRequestHeader(String, String) addRequestHeader} or
     * {@link #setRequestHeaders(Map) setRequestHeaders}.
     *
     * @return The headers added by the user
     */
    @PublicAtsApi
    public HttpHeader[] getRequestHeaders() {

        return requestHeaders.toArray( new HttpHeader[requestHeaders.size()] );
    }

    /**
     * Get the list of HTTP headers sent by the HTTPClient to the endpoint. This will differ to
     * what the user sets via the {@link #addRequestHeader(String, String) addRequestHeader}
     * or {@link #setRequestHeaders(Map) setRequestHeaders}
     * methods as the HTTPClient
     * will automatically add headers, e.g. Content-Length. Note that the Authorization header
     * will not show up in the list of headers returned from this method even though it will
     * get sent to the endpoint when authentication is being used.
     * This must be called after the request has been sent via
     * {@link com.axway.ats.action.http.HttpClient#post() post()},
     * {@link com.axway.ats.action.http.HttpClient#put() put()},
     * {@link com.axway.ats.action.http.HttpClient#get() get()} or
     * {@link com.axway.ats.action.http.HttpClient#delete() delete()},
     * otherwise it will return an empty map.
     * The map returned should include the user-specified HTTP headers as well as
     * any generated by the HTTPClient.
     *
     * @return The request headers sent to the endpoint
     */
    @PublicAtsApi
    public HttpHeader[] getActualRequestHeaders() {

        return actualRequestHeaders.toArray( new HttpHeader[actualRequestHeaders.size()] );
    }

    /**
     * Get the request(query) string parameters.
     *
     * @return parameters {@link Map}. Each parameter has a {@link List} of one or more values.
     */
    @PublicAtsApi
    public Map<String, List<String>> getRequestParameters() {

        return this.requestParameters;
    }

    /**
     * Set the authentication username and password used for HTTP Basic, HTTP Digest,
     * or SPNEGO authentication.
     *
     * @param username The username
     * @param password The password
     * @param authType Set to 'always' if the HTTP Basic Authorization header should
     *                 be sent preemptively, i.e. without waiting for the server challenge.
     */
    @PublicAtsApi
    public void setAuthorization( String username, String password, AuthType authType ) {

        this.username = username;
        this.password = password;
        this.authType = authType;
    }

    /**
     * Set the authentication username and password used for HTTP Basic, HTTP Digest,
     * or SPNEGO authentication.
     *
     * @param username The username
     * @param password The password
     */
    @PublicAtsApi
    public void setAuthorization( String username, String password ) {

        this.username = username;
        this.password = password;
    }

    /**
     * Set the authentication username and password (via a keytab file) for use
     * with SPNEGO authentication. This can be used as an alternative to
     * {@link #setAuthorization(String, String) setAuthorization} for SPNEGO
     * authentication if a keytab file holds the client's secret key.
     *
     * @param username The username, e.g. 'User1@AXWAY.COM'
     * @param keytab The file containing the client's keytab.
     */
    @PublicAtsApi
    public void setKerberosAuthorization( String username, File keytab ) {

        this.username = username;
        this.kerberosClientKeytab = keytab;
    }

    /**
     * Set the principal name of the Kerberos service that we are authenticating to.
     * This may be any valid Kerberos service principal, it does not need to be an
     * SPN (Service Principal Name) of the form HTTP/host@REALM which is used
     * for browser SPNEGO authentication.
     *
     * @param kerberosServicePrincipalName The service principal name,
     *                                     e.g. 'Service1@AXWAY.COM', 'HTTP/server.axway.com@AXWAY.COM'
     * @param kerberosServicePrincipalType The service principal type. This must be set to one of the following
     *                                     values:-
     *            <ul>
     *            <li>NT_USER_NAME</li>
     *            <li>NT_HOSTBASED_SERVICE</li>
     *            <li>NT_MACHINE_UID_NAME</li>
     *            <li>NT_STRING_UID_NAME</li>
     *            <li>NT_ANONYMOUS</li>
     *            <li>NT_EXPORT_NAME</li>
     *            </ul>
     */
    @PublicAtsApi
    public void setKerberosServicePrincipal( String kerberosServicePrincipalName,
                                             String kerberosServicePrincipalType ) {

        this.kerberosServicePrincipalName = kerberosServicePrincipalName;
        this.kerberosServicePrincipalType = kerberosServicePrincipalType;
    }

    /**
     * Set the krb.conf file. This is required for SPNEGO (Kerberos) authentication.
     * This is a file on disk, the content of which will be something like:-
     * <pre>
     * [libdefaults]
     * default_tkt_enctypes=rc4-hmac
     * default_tgs_enctypes= rc4-hmac
     * default_realm = AXWAY.COM
     * forwardable = true
    
     * [realms]
     * AXWAY.COM = {
     *     kdc = kerberos-dc.axway.com
     * }
     * </pre>
     * @param krb5ConfFile The krb5.conf file
     */
    @PublicAtsApi
    public void setKrb5ConfFile( File krb5ConfFile ) {

        this.krb5ConfFile = krb5ConfFile;
    }

    /**
     * Set the trusted server-side certificate for SSL. If no trusted certificate is set
     * and SSL is used, all server-side certificates will be trusted.
     *
     * @param trustedServerSSLCertificateFile PEM file containing a server SSL certificate
     * @throws HttpException
     */
    @PublicAtsApi
    public void setTrustedServerSSLCertificate( File trustedServerSSLCertificateFile ) throws HttpException {

        trustedServerCertificates = new X509Certificate[]{ convertFileToX509Certificate( trustedServerSSLCertificateFile ) };
    }

    /**
     * Set the trusted server-side certificates for SSL. If no trusted certificates are set
     * and SSL is used, all server-side certificates will be trusted.
     * If a chain of certificates are passed via this method, then the server-side must present
     * an exact match chain of certificates. If the certificates passed via this method are not
     * a certificate chain, the server-side must present one of the certificates in the list of
     * trusted certificates.
     *
     * @param trustedServerSSLCertificateFiles List of PEM files containing a server SSL certificates
     * @return whether the certificates are a chain
     * @throws HttpException
     */
    @PublicAtsApi
    public boolean setTrustedServerSSLCertificates( Collection<File> trustedServerSSLCertificateFiles ) throws HttpException {

        trustedServerCertificates = new X509Certificate[trustedServerSSLCertificateFiles.size()];
        int i = 0;
        for( File file : trustedServerSSLCertificateFiles ) {
            trustedServerCertificates[i++] = convertFileToX509Certificate( file );
        }

        // Check if the certificates are a chain, if they are then
        // the trust manager will check presented cert chain is an exact match to
        // the trustedServerCertificates.
        // The root CA is the last in the chain.
        isChain = trustedServerCertificates.length > 1;

        for( int c = 0; c < trustedServerCertificates.length - 1; c++ ) {
            X509Certificate subjectCert = trustedServerCertificates[c];
            X509Certificate issuerCert = trustedServerCertificates[c + 1];
            if( !subjectCert.getIssuerDN().equals( issuerCert.getSubjectDN() ) ) {
                isChain = false;
                break;
            }
        }

        return isChain;
    }

    /**
     * Get all the trusted certificates.
     *
     * @return An array of certificates
     */
    @PublicAtsApi
    public X509Certificate[] getTrustedServerCertificates() {

        return trustedServerCertificates;
    }

    /**
     * Set the client private key and certificate for SSL mutual authentication.
     *
     * @param clientSSLCertificateP12File A p12 file containing the client's SSL private key and certificate
     * @param password The password for the p12 file
     * @throws HttpException
     */
    @PublicAtsApi
    public void setClientSSLCertificate( File clientSSLCertificateP12File,
                                         String password ) throws HttpException {

        FileInputStream fis = null;
        try {
            clientSSLKeyStore = KeyStore.getInstance( "PKCS12" );
            fis = new FileInputStream( clientSSLCertificateP12File );
            clientSSLKeyStore.load( fis, password.toCharArray() );

            Enumeration<String> e = clientSSLKeyStore.aliases();
            while( e.hasMoreElements() ) {
                String alias = e.nextElement();
                if( clientSSLKeyStore.getCertificateChain( alias ) != null ) {
                    Certificate[] certs = clientSSLKeyStore.getCertificateChain( alias );
                    this.clientSSLCertificateChain = new X509Certificate[certs.length];
                    int i = 0;
                    for( Certificate cert : certs ) {
                        clientSSLCertificateChain[i++] = ( X509Certificate ) cert;
                    }
                }
            }
        } catch( Exception e ) {
            throw new HttpException( "Failed to load client SSL certificate from '"
                                     + ( ( clientSSLCertificateP12File != null )
                                                                                 ? clientSSLCertificateP12File
                                                                                 : "null" )
                                     + "'.", e );
        } finally {
            if( fis != null ) {
                try {
                    fis.close();
                } catch( IOException e ) {
                    log.error( e );
                }
            }
        }
    }

    /**
     * Get the client-side SSL certificate chain, as loaded from the p12 file passed via the
     * {@link #setClientSSLCertificate(File, String) setClientSSLCertificate} method.
     *
     * @return An array of certificates
     */
    @PublicAtsApi
    public X509Certificate[] getClientSSLCertificateChain() {

        return clientSSLCertificateChain;
    }

    /**
     * Set the request body using content in a file.
     *
     * @param file The file containing the request
     * @param contentType The content type e.g. 'text/xml'
     */
    @PublicAtsApi
    public void setRequestBody( File file, String contentType ) {

        requestBody = new FileEntity( file, ContentType.create( contentType ) );
    }

    /**
     * Set the request body using content in a file.
      *
     * @param file The file containing the request
      * @param contentType The content type, e.g. 'text/xml'
     * @param charset The charset, e.g. 'UTF-8'
      */
    @PublicAtsApi
    public void setRequestBody( File file, String contentType, String charset ) {

        requestBody = new FileEntity( file, ContentType.create( contentType, charset ) );
    }

    /**
     * Set the request body using content in a string.
     *
     * @param string The request
     * @param contentType The content type, e.g. 'text/xml'
     */
    @PublicAtsApi
    public void setRequestBody( String string, String contentType ) {

        requestBody = new StringEntity( string, ContentType.create( contentType ) );
    }

    /**
     * Set the request body using content in a string.
     *
     * @param string The request
     * @param contentType The content type, e.g. 'text/xml'
     * @param charset The charset, e.g. 'UTF-8
     */
    @PublicAtsApi
    public void setRequestBody( String string, String contentType, String charset ) {

        requestBody = new StringEntity( string, ContentType.create( contentType, charset ) );
    }

    /**
     * Set the request body using content in a byte array.
     *
     * @param bytes The request
     * @param contentType The content type, e.g. 'text/xml'
     */
    @PublicAtsApi
    public void setRequestBody( byte[] bytes, String contentType ) {

        requestBody = new ByteArrayEntity( bytes, ContentType.create( contentType ) );
    }

    /**
     * Set the request body using content in a byte array.
     *
     * @param bytes The request
     * @param contentType The content type, e.g. 'text/xml'
     * @param charset The charset, e.g. 'UTF-8
     */
    @PublicAtsApi
    public void setRequestBody( byte[] bytes, String contentType, String charset ) {

        requestBody = new ByteArrayEntity( bytes, ContentType.create( contentType, charset ) );
    }

    /**
     * This method is used to create a multipart body. </br>
     * For single body messages the different <i>setRequestBody</i> methods should be used.
     *
     * @param part the part to add to the body of the HTTP request
     */
    @PublicAtsApi
    public void addRequestBodyPart( HttpBodyPart part ) {

        this.requestBodyParts.add( part );
    }

    private void constructRequestBody() {

        // we have work to do here only when using multipart body
        if( requestBodyParts.size() > 0 ) {
            try {
                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                for( HttpBodyPart part : requestBodyParts ) {
                    entityBuilder.addPart( part.getName(), part.constructContentBody() );
                }
                requestBody = entityBuilder.build();
            } catch( Exception e ) {
                throw new HttpException( "Exception trying to create a multipart message.", e );
            }
        }
    }

    /**
     * Get the request body as a string.
     *
     * @return The request body
     *
     * @throws IOException
     */
    @PublicAtsApi
    public String getRequestBodyAsString() throws IOException {

        constructRequestBody();

        if( requestBody == null ) {
            return "";
        } else {
            return new String( getRequestBodyAsBytes() );
        }
    }

    /**
     * Get the request body as a string.
     *
     * @param charset The charset, e.g. 'UTF-8
     * @return The request body
     *
     * @throws IOException
     */
    @PublicAtsApi
    public String getRequestBodyAsString( String charset ) throws IOException {

        constructRequestBody();

        return new String( getRequestBodyAsBytes(), charset );
    }

    /**
     * Get the request body as a byte array.
     *
     * @return The request body
     *
     * @throws IOException
     */
    @PublicAtsApi
    public byte[] getRequestBodyAsBytes() throws IOException {

        constructRequestBody();

        if( requestBody == null ) {
            return null;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        requestBody.writeTo( bos );
        return bos.toByteArray();
    }

    /**
     * Get the request body as an XML Document.
     *
     * @return The request body as a Document
     *
     * @throws IOException
     */
    @PublicAtsApi
    public Document getRequestBodyAsXML() throws HttpException {

        constructRequestBody();

        if( requestBody == null ) {
            return null;
        }

        DocumentBuilderFactory newInstance = DocumentBuilderFactory.newInstance();
        newInstance.setNamespaceAware( true );
        try {
            return newInstance.newDocumentBuilder()
                              .parse( new ByteArrayInputStream( getRequestBodyAsBytes() ) );
        } catch( Exception e ) {
            throw new HttpException( "Exception trying to convert response to Document.", e );
        }
    }

    /**
     * File path for saving response body content
     *
     * @param filePath response file path
     */
    @PublicAtsApi
    public void setResponseBodyFilePath( String filePath ) {

        this.responseBodyFilePath = filePath;
    }

    /**
     * Get the file path where the response body content will be saved
     *
     * @return response file path
     */
    @PublicAtsApi
    public String getResponseBodyFilePath() {

        return this.responseBodyFilePath;
    }

    /**
     * Send the request to the endpoint using a HTTP POST.
     *
     * @return The response
     * @throws HttpException
     */
    @PublicAtsApi
    public HttpResponse post() throws HttpException {

        HttpPost method = new HttpPost( constructURI() );
        constructRequestBody();
        log.info( "We will POST object to " + constructURI() );
        method.setEntity( requestBody );
        return execute( method );
    }

    /**
     * Send the request to the endpoint using a HTTP PUT.
     *
     * @return The response
     * @throws HttpException
     */
    @PublicAtsApi
    public HttpResponse put() throws HttpException {

        HttpPut method = new HttpPut( constructURI() );
        constructRequestBody();
        log.info( "We will PUT object to " + constructURI() );
        method.setEntity( requestBody );
        return execute( method );
    }

    /**
     * Invoke the endpoint URL using a HTTP GET.
     *
     * @return The response
     * @throws HttpException
     */
    @PublicAtsApi
    public HttpResponse get() throws HttpException {

        HttpGet method = new HttpGet( constructURI() );
        log.info( "We will GET from " + constructURI() );
        return execute( method );
    }

    /**
     * Invoke the endpoint URL using a HTTP DELETE.
     *
     * @return The response
     * @throws HttpException
     */
    @PublicAtsApi
    public HttpResponse delete() throws HttpException {

        HttpDelete method = new HttpDelete( constructURI() );
        log.info( "We will DELETE from " + constructURI() );
        return execute( method );
    }

    /**
     * Invoke the endpoint URL using a HTTP HEAD.
     *
     * @return The response
     * @throws HttpException
     */
    @PublicAtsApi
    public HttpResponse head() throws HttpException {

        HttpHead method = new HttpHead( constructURI() );
        return execute( method );
    }

    /**
     * Close HttpClient
     */
    @PublicAtsApi
    public void close() {

        IoUtils.closeStream( this.httpClient, "Failed to close HttpClient" );
    }

    @Override
    protected void finalize() throws Throwable {

        // ensure the connection is terminated
        close();

        super.finalize();
    }

    /**
     * Main execute method that sends request and receives response.
     *
     * @param method The POST/PUT etc. method
     * @return The response
     * @throws HttpException
     */
    private HttpResponse execute( HttpRequestBase httpMethod ) throws HttpException {

        HttpClientContext localContext = null;
        if( httpClient == null ) {
            HttpClientBuilder httpClientBuilder = HttpClients.custom();

            // Add this interceptor to get the values of all HTTP headers in the request.
            // Some of them are provided by the user while others are generated by Apache HTTP Components.
            httpClientBuilder.addInterceptorLast( new HttpRequestInterceptor() {
                @Override
                public void process( HttpRequest request, HttpContext context ) throws HttpException,
                                                                                IOException {

                    Header[] requestHeaders = request.getAllHeaders();
                    actualRequestHeaders = new ArrayList<HttpHeader>();
                    for( Header header : requestHeaders ) {
                        addHeaderToList( actualRequestHeaders, header.getName(), header.getValue() );
                    }
                    if( debugLevel != HttpDebugLevel.NONE ) {
                        logHTTPRequest( requestHeaders, request );
                    }
                }

            } );

            // Setup authentication
            if( !StringUtils.isNullOrEmpty( username ) ) {
                localContext = setupAuthentication( httpClientBuilder );
            }

            // Setup SSL
            if( url.toLowerCase().startsWith( "https" ) ) {
                setupSSL( httpClientBuilder );
            }

            // SocketConfig socketConfig = SocketConfig.copy( SocketConfig.DEFAULT )
            //     .setSoTimeout( socket_timeout ).setTcpNoDelay( true ).setSoReuseAddress( true ).build();
            // httpClientBuilder.setDefaultSocketConfig( socketConfig );

            // all important options are set, now build the HTTP client
            if( AtsSystemProperties.SYSTEM_HTTP_PROXY_HOST != null
                && AtsSystemProperties.SYSTEM_HTTP_PROXY_PORT != null ) {

                HttpHost proxy = new HttpHost( AtsSystemProperties.SYSTEM_HTTP_PROXY_HOST,
                                               Integer.parseInt( AtsSystemProperties.SYSTEM_HTTP_PROXY_PORT ) );
                DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner( proxy );
                httpClient = httpClientBuilder.setRoutePlanner( routePlanner ).build();
            } else {
                httpClient = httpClientBuilder.build();
            }
        }

        // Setup read and connect timeouts
        httpMethod.setConfig( RequestConfig.custom()
                                           .setSocketTimeout( readTimeoutSeconds * 1000 )
                                           .setConnectTimeout( connectTimeoutSeconds * 1000 )
                                           .build() );

        // Add HTTP headers
        addHeadersToHttpMethod( httpMethod );

        // Create response handler
        ResponseHandler<HttpResponse> responseHandler = new ResponseHandler<HttpResponse>() {

            @Override
            public HttpResponse handleResponse( final org.apache.http.HttpResponse response ) throws ClientProtocolException,
                                                                                              IOException {

                int status = response.getStatusLine().getStatusCode();
                Header[] responseHeaders = response.getAllHeaders();
                List<HttpHeader> responseHeadersList = new ArrayList<HttpHeader>();

                for( Header header : responseHeaders ) {
                    addHeaderToList( responseHeadersList, header.getName(), header.getValue() );
                }
                if( ( debugLevel & HttpDebugLevel.HEADERS ) == HttpDebugLevel.HEADERS ) {
                    logHTTPResponse( responseHeaders, response );
                }

                try {
                    HttpEntity entity = response.getEntity();
                    if( entity == null ) {
                        // No response body, generally have '204 No content' status
                        return new HttpResponse( status, response.getStatusLine().getReasonPhrase(),
                                                 responseHeadersList );
                    } else {
                        if( responseBodyFilePath != null ) {

                            FileOutputStream fos = null;
                            try {
                                fos = new FileOutputStream( new File( responseBodyFilePath ), false );
                                entity.writeTo( fos );
                            } finally {
                                IoUtils.closeStream( fos );
                            }
                            return new HttpResponse( status, response.getStatusLine().getReasonPhrase(),
                                                     responseHeadersList );
                        } else {

                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            entity.writeTo( bos );
                            return new HttpResponse( status, response.getStatusLine().getReasonPhrase(),
                                                     responseHeadersList, bos.toByteArray() );
                        }
                    }
                } finally {
                    if( response instanceof CloseableHttpResponse ) {
                        IoUtils.closeStream( ( CloseableHttpResponse ) response,
                                             "Failed to close HttpResponse" );
                    }
                }
            }
        };

        // Send the request as POST/GET etc. and return response.
        try {
            return httpClient.execute( httpMethod, responseHandler, localContext );
        } catch( IOException e ) {
            throw new HttpException( "Exception occurred sending message to URL '" + actualUrl
                                     + "' with a read timeout of " + readTimeoutSeconds
                                     + " seconds and a connect timeout of " + connectTimeoutSeconds
                                     + " seconds.", e );
        } finally {

            // clear internal variables
            ActionLibraryConfigurator actionLibraryConfigurator = ActionLibraryConfigurator.getInstance();
            if( !actionLibraryConfigurator.getHttpKeepRequestHeaders() ) {
                this.requestHeaders.clear();
            }
            if( !actionLibraryConfigurator.getHttpKeepRequestParameters() ) {
                this.requestParameters.clear();
            }
            if( !actionLibraryConfigurator.getHttpKeepRequestBody() ) {
                this.requestBody = null;
            }
            this.responseBodyFilePath = null;
        }
    }

    private void logHTTPRequest( Header[] requestHeaders, HttpRequest request ) {

        StringBuilder requestMessage = new StringBuilder();
        requestMessage.append( "Sending the following requests : \n" );
        requestMessage.append( request.getRequestLine() + "\n" );
        String requestBody = "";
        for( Header header : requestHeaders ) {
            if( ( debugLevel & HttpDebugLevel.HEADERS ) == HttpDebugLevel.HEADERS ) {
                requestMessage.append( header.getName() + ": " + header.getValue() + "\n" );
            }
            if( ( debugLevel & HttpDebugLevel.BODY ) == HttpDebugLevel.BODY
                && !header.getValue().equals( ContentType.APPLICATION_OCTET_STREAM.getMimeType() ) ) {
                try {
                    requestBody = "\n" + getRequestBodyAsString();
                } catch( IOException e ) {
                    log.warn( "Request body can`t be logged." );
                }
            }
        }
        log.info( requestMessage + requestBody );
    }

    private void logHTTPResponse( Header[] responseHeaders, org.apache.http.HttpResponse response ) {

        StringBuilder responseMessage = new StringBuilder();
        responseMessage.append( "Receiving the following response : \n" );
        responseMessage.append( response.getStatusLine() + "\n" );

        for( Header header : responseHeaders ) {
            responseMessage.append( header.getName() + ": " + header.getValue() + "\n" );
        }
        log.info( responseMessage );
    }

    /**
     * Add a header.
     *
     * @param headers The list to add to.
     * @param name The header name
     * @param value The header value
     */
    private void addHeaderToList( List<HttpHeader> headers, String name, String value ) {

        for( HttpHeader header : headers ) {
            if( header.getKey().equalsIgnoreCase( name ) ) {
                header.addValue( value );
                return;
            }
        }

        headers.add( new HttpHeader( name, value ) );
    }

    /**
     * Pass the user-specified HTTP headers to the Apache HTTP Components
     * method object.
     */
    private void addHeadersToHttpMethod( HttpRequestBase httpMethod ) {

        for( HttpHeader header : requestHeaders ) {
            for( String value : header.getValues() ) {
                httpMethod.addHeader( header.getKey(), value );
            }
        }
    }

    private URI constructURI() throws HttpException {

        // navigate to internal resource
        StringBuilder uriBuilder = new StringBuilder( url );
        if( resourcePath.size() > 0 ) {
            if( !url.endsWith( IoUtils.FORWARD_SLASH ) ) {
                uriBuilder.append( IoUtils.FORWARD_SLASH );
            }
            for( String token : resourcePath ) {
                uriBuilder.append( token );
                uriBuilder.append( IoUtils.FORWARD_SLASH );
            }
            // remove the last slash if added resource path tokens
            actualUrl = uriBuilder.substring( 0, uriBuilder.length() - 1 );
        } else {
            actualUrl = uriBuilder.toString();
        }

        try {
            URIBuilder builder = new URIBuilder( actualUrl );
            // add request parameters
            for( Entry<String, List<String>> reqParam : requestParameters.entrySet() ) {
                for( String value : reqParam.getValue() ) {
                    builder.addParameter( reqParam.getKey(), value );
                }
            }
            return builder.build();
        } catch( URISyntaxException e ) {
            throw new HttpException( "Exception occurred when creating URL.", e );
        }
    }

    /**
     * Set up authentication for HTTP Basic/HTTP Digest/SPNEGO.
     *
     * @param httpClientBuilder The client builder
     * @return The context
     * @throws HttpException
     */
    private HttpClientContext setupAuthentication( HttpClientBuilder httpClientBuilder ) throws HttpException {

        HttpClientContext localContext = null;

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials( new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT ),
                                      new UsernamePasswordCredentials( username, password ) );
        httpClientBuilder = httpClientBuilder.setDefaultCredentialsProvider( credsProvider );

        if( authType == AuthType.always ) {
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local
            // auth cache
            BasicScheme basicAuth = new BasicScheme();
            URL uri = null;
            try {
                uri = new URL( url );
            } catch( MalformedURLException e ) {
                throw new HttpException( "Exception occurred creating URL from '" + url + "'.", e );
            }
            HttpHost target = new HttpHost( uri.getHost(), uri.getPort(), uri.getProtocol() );
            authCache.put( target, basicAuth );

            // Add AuthCache to the execution context
            localContext = HttpClientContext.create();
            localContext.setAuthCache( authCache );

        } else {
            if( !StringUtils.isNullOrEmpty( kerberosServicePrincipalName ) ) {
                GssClient gssClient = new GssClient( username, password, kerberosClientKeytab, krb5ConfFile );
                AuthSchemeProvider nsf = new SPNegoSchemeFactory( gssClient, kerberosServicePrincipalName,
                                                                  kerberosServicePrincipalType );
                final Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider> create()
                                                                                       .register( AuthSchemes.SPNEGO,
                                                                                                  nsf )
                                                                                       .build();
                httpClientBuilder.setDefaultAuthSchemeRegistry( authSchemeRegistry );
            }
        }

        return localContext;
    }

    /**
     * Setup SSL. Pass the trusted certificates and client private key and certificate,
     * if applicable.
     *
     * @param httpClientBuilder The client builder
     * @throws HttpException
     */
    private void setupSSL( HttpClientBuilder httpClientBuilder ) throws HttpException {

        try {
            SSLContextBuilder sslContextBuilder = SSLContexts.custom();

            sslContextBuilder.loadTrustMaterial( convertToKeyStore( trustedServerCertificates ),
                                                 new TrustStrategy() {

                                                     @Override
                                                     public boolean isTrusted( X509Certificate[] chain,
                                                                               String authType ) throws CertificateException {

                                                         return checkIsTrusted( chain );
                                                     }
                                                 } );

            if( clientSSLKeyStore != null ) {
                sslContextBuilder.loadKeyMaterial( clientSSLKeyStore, "".toCharArray() );
            }

            SSLContext sslContext = sslContextBuilder.build();
            // Allow all supported protocols
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory( sslContext, supportedProtocols,
                                                                               supportedCipherSuites,
                                                                               new NoopHostnameVerifier() );

            httpClientBuilder.setSSLSocketFactory( sslsf );

        } catch( Exception e ) {
            throw new HttpException( "Exception occurred when setting up SSL.", e );
        }
    }

    /**
     * Set the supported protocols. Default is <em>TLSv1.2</em>.
     *
     * @param supportedProtocols array of supported protocols to use like {"TLSv1", "TLSv1.1", "TLSv1.2"}
     */
    public void setSupportedProtocols( String[] supportedProtocols ) {

        this.supportedProtocols = supportedProtocols;
    }

    /**
     * Get the supported protocols
     * @return
     */
    public String[] getSupportedProtocols() {

        return this.supportedProtocols;
    }

    /**
     * Set the supported SSL Cipher Suites
     * @param supportedCipherSuites
     */
    public void setSupportedCipherSuites( String[] supportedCipherSuites ) {

        this.supportedCipherSuites = supportedCipherSuites;
    }

    /**
     * Get the supported SSL Cipher Suites
     * @return
     */
    public String[] getSupportedCipherSuites() {

        return this.supportedCipherSuites;
    }

    /**
     * Converts a .pem or .der file to a X509Certificate.
     *
     * @param pemFile
     * @return An X509Certificate object
     * @throws HttpException
     */
    private X509Certificate convertFileToX509Certificate( File pemFile ) throws HttpException {

        InputStream is = null;
        try {
            is = new FileInputStream( pemFile );
            CertificateFactory cf = CertificateFactory.getInstance( "X.509" );
            return ( X509Certificate ) cf.generateCertificate( is );
        } catch( Exception e ) {
            throw new HttpException( "Failed to convert file '" + ( ( pemFile != null )
                                                                                        ? pemFile
                                                                                        : "null" )
                                     + "' to X509 certificate.", e );
        } finally {
            if( is != null ) {
                try {
                    is.close();
                } catch( IOException e ) {
                    log.error( e );
                }
            }
        }
    }

    /**
     * Convert an array of certificates to a Java KeyStore.
     *
     * @param certificates The array of certificates
     * @return The Java KeyStore
     * @throws HttpException
     */
    private KeyStore convertToKeyStore( X509Certificate[] certificates ) throws HttpException {

        try {
            KeyStore keystore = KeyStore.getInstance( "JKS" );
            keystore.load( null );
            if( certificates != null ) {
                for( X509Certificate certificate : certificates ) {
                    keystore.setCertificateEntry( certificate.getSubjectDN().getName(), certificate );
                }
            }
            return keystore;
        } catch( Exception e ) {
            throw new HttpException( "Failed to create keystore from certificates", e );
        }
    }

    /**
     * This method is run for SSL to check that we trust the certificate(s) presented by the
     * endpoint.
     *
     * @param chain The certificates presented by the endpoint
     * @return
     */
    private boolean checkIsTrusted( X509Certificate[] chain ) {

        if( trustedServerCertificates == null ) {
            // Trust all as we have not specified what to trust.
            // Default behavior.
            return true;
        }

        // Ensure presented certs are valid.
        for( X509Certificate presentedCert : chain ) {
            try {
                presentedCert.checkValidity(); // throws exception
            } catch( Exception e ) {
                log.error( "Certificate invalid.", e );
                return false;
            }
        }

        // Verify presented chain
        for( int i = 0; i < chain.length - 1; i++ ) {
            X509Certificate subject = chain[i];
            X509Certificate issuer = chain[i + 1];
            try {
                subject.verify( issuer.getPublicKey() );
            } catch( GeneralSecurityException e ) {
                log.error( "Failed to verify certificate.", e );
                return false;
            }
        }

        if( isChain ) {
            // Do an exact match of chains
            if( chain.length != trustedServerCertificates.length ) {
                log.error( "Presented chain of certificates has a different length to trusted chain." );
                return false;
            }
            // Ensure that the presented certs are all to be found in trustedCerts
            for( X509Certificate presentedCert : chain ) {
                printCertificateInfo( presentedCert );

                boolean found = false;
                for( X509Certificate trustedCert : trustedServerCertificates ) {
                    if( presentedCert.equals( trustedCert ) ) {
                        log.info( "Server certificate found in trusted chain." );
                        found = true;
                        break;
                    }
                }

                if( !found ) {
                    log.error( "Failed to find presented chain certificate '" + presentedCert.getSubjectDN()
                               + "' in trusted chain." );
                    return false;
                }
            }
        } else {
            // Check we trust one of the certs on the chain
            for( X509Certificate presentedCert : chain ) {
                printCertificateInfo( presentedCert );

                for( X509Certificate trustedCert : trustedServerCertificates ) {
                    if( presentedCert.equals( trustedCert ) ) {
                        log.info( "Server certificate trusted." );
                        return true;
                    }
                }
            }
            log.error( "Cannot trust any certificate on presented chain." );
            return false;
        }

        return true;
    }

    /**
     * Log the request/response data you need.
     * For example:<pre>client.setHTTPDebug(HTTPDebugLevel.ALL)</pre>
     *
     * @param level HTTP debug level. Use constants in {@link HttpDebugLevel}
     */
    @PublicAtsApi
    public void setVerboseMode( int level ) {

        debugLevel = level;
    }

    private void printCertificateInfo( X509Certificate cert ) {

        log.info( " Checking SSL Server certificate :" );
        log.info( "  Subject DN: " + cert.getSubjectDN() );
        log.info( "  Signature Algorithm: " + cert.getSigAlgName() );
        log.info( "  Valid from: " + cert.getNotBefore() );
        log.info( "  Valid until: " + cert.getNotAfter() );
        log.info( "  Issuer: " + cert.getIssuerDN() );
    }
}
