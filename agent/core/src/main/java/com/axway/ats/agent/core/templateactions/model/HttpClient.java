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
package com.axway.ats.agent.core.templateactions.model;

import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HEADER_NAME_ATTRIBUTE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HEADER_VALUE_ATTRIBUTE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_HEADER;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_RESOURCE_FILE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_RESPONSE_FILE_EXPECTED;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_RESPONSE_RESULT;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.axway.ats.agent.core.configuration.ConfigurationSettings;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.templateactions.exceptions.HttpClientException;
import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;
import com.axway.ats.agent.core.templateactions.model.objects.ActionHeader;
import com.axway.ats.common.agent.templateactions.NetworkingStopWatch;
import com.axway.ats.common.io.MimeTypeFileExtensionMapper;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;

/**
 * An HTTP Client used to send and receive HTTP requests and responses
 */
public class HttpClient {

    static final Logger         log                                     = LogManager.getLogger(HttpClient.class);
    public static final Logger  logTimer                                = NetworkingStopWatch.logTimer;
    private static final String FORMAT_PROXY                            = "The Agent template proxy format is http://<host-or-IP>:<port>";

    // minimum number of HTTP response code for which UrlConnection.getErrorStream() should be used instead
    // of UrlConnection.getInputStream()
    // For details see: http://www-01.ibm.com/support/docview.wss?uid=swg21249300, http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6400786
    private static final int    HTTP_MIN_RESPONSE_CODE_FOR_ERROR_STREAM = 400;
    //TODO: chunk length must be configurable
    private static final int    CHUNK_LENGTH                            = 65536;                                                          //64 * 1024

    private static final int    MAX_PARAMETERIZED_RESOURCE_FILE_SIZE    = 1024 * 1024;

    private static String       proxyHost;
    private static int          proxyPort;
    static SSLContext           sslContext                              = null;                                                           // reuse sslContext instead of setting it for each request. This should share connections

    static {
        parseSystemProperties();

        HttpURLConnection.setFollowRedirects(false); // manually follow them via template actions

        // One time initialization for default SSL socket factory (trusts everything)
        initSSL();
    }

    private HttpURLConnection   urlConnection;

    // For tracking request/response (network+server) time
    private NetworkingStopWatch stopWatch;

    // properties for logging purpose only
    private List<ActionHeader>  httpHeaders;

    private byte[]              responseBodyBytes = null;

    // Set trust-all-peers trust manager
    private static void initSSL() throws RuntimeException /* GeneralSecurityException */ {

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{ new DefaultTrustManager() }, null);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Error setting trust-all trust manager", e);
        }
    }

    public HttpClient( String httpUrl, String httpMethod, List<ActionHeader> httpHeaders,
                       NetworkingStopWatch stopWatch ) throws AgentException {

        this.httpHeaders = httpHeaders;

        boolean isPost = "POST".equalsIgnoreCase(httpMethod);
        boolean isPut = "PUT".equalsIgnoreCase(httpMethod);
        boolean isHead = "HEAD".equalsIgnoreCase(httpMethod);
        this.stopWatch = stopWatch;
        try {

            URL url = new URL(httpUrl);
            if (proxyHost != null) {
                urlConnection = (HttpURLConnection) url.openConnection(new Proxy(Type.HTTP,
                                                                                 new InetSocketAddress(proxyHost,
                                                                                                       proxyPort)));
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }
            if (httpUrl.toLowerCase().startsWith("https")) {
                // SSL context is initialized one-time
                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslContext.getSocketFactory());
                ((HttpsURLConnection) urlConnection).setHostnameVerifier(new DefaultHostnameVerifier());

            }

            // TODO - set timeouts from some property. Currently on Java 6 it seems to be 0 - wait forever
            // urlConnection.setConnectTimeout( connectionTimeoutMs);
            // urlConnection.setReadTimeout( readTimeoutMs);

            urlConnection.setRequestMethod(httpMethod);
            urlConnection.setUseCaches(false); // No caching, we want the real thing. Caching is used according to all saved requests/responses in browser traffic
            if (!isHead) {
                urlConnection.setDoInput(true); // Let the run-time system (RTS) know that we want input.
            }
            urlConnection.setDoOutput(isPost || isPut); // Let the RTS know that we want to do output.

            // set the request headers
            for (ActionHeader header : httpHeaders) {
                urlConnection.addRequestProperty(header.getHeaderName(), header.getHeaderValue());
            }
        } catch (MalformedURLException e) {
            throw new AgentException("Error establishing connection", e);
        } catch (ProtocolException e) {
            throw new AgentException("Error establishing connection", e);
        } catch (IOException e) {
            throw new AgentException("Error establishing connection", e);
        }
    }

    public void sendHttpRequest( String actionStep, String fileToSend, boolean hasParams ) throws Exception {

        log.info(actionStep + " -> Sending HTTP request to '" + urlConnection.getURL() + "'");

        if (urlConnection.getDoOutput()) {

            stopWatch.step1_OpenConnectionForRequest();
            OutputStream outputStream = urlConnection.getOutputStream(); // connect here
            stopWatch.step2_OpenedConnectionForRequest();

            if (fileToSend != null) {

                File resourceFile = new File(fileToSend);
                InputStream is = new FileInputStream(resourceFile);
                try {
                    if (hasParams) {

                        if (resourceFile.length() > MAX_PARAMETERIZED_RESOURCE_FILE_SIZE) {

                            throw new HttpClientException("The resource file '" + fileToSend
                                                          + "' marked for parameterization is too large (max_size="
                                                          + MAX_PARAMETERIZED_RESOURCE_FILE_SIZE + ")");
                        }
                        String fileContent = IoUtils.streamToString(is);
                        fileContent = XmlUtilities.applyUserParameters(fileContent);
                        if (log.isTraceEnabled()) {
                            log.trace("Request contents after parameters applied:\n" + fileContent);
                        }
                        stopWatch.step3_StartSendingRequest();
                        try {
                            outputStream.write(fileContent.getBytes());
                            outputStream.flush();
                        } finally {
                            stopWatch.step4_EndSendingRequest();
                        }
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace("Request data has no parameters marked so it is the same as the one in template files");
                        }
                        byte[] buffer = new byte[CHUNK_LENGTH];
                        int numRead = 0;
                        stopWatch.step3_StartSendingRequest();
                        try {
                            while ( (numRead = is.read(buffer, 0, buffer.length)) != -1) {

                                outputStream.write(buffer, 0, numRead);
                                outputStream.flush();
                            }
                        } finally {
                            stopWatch.step4_EndSendingRequest();
                        }

                    }
                } finally {
                    IoUtils.closeStream(is);
                    IoUtils.closeStream(outputStream);
                    if (logTimer.isDebugEnabled()) {
                        logTimer.debug("   Timer: Send file request time: "
                                       + stopWatch.getNetworkingTime());
                    }
                }
            } else {
                stopWatch.step3_StartSendingRequest();
                try {
                    outputStream.write(new byte[0]);
                    outputStream.flush();
                } finally {
                    IoUtils.closeStream(outputStream);
                    stopWatch.step4_EndSendingRequest();
                }
            }
        } else {
            // Request with no body like GET, HEAD, DELETE
            stopWatch.setStateFromBeforeStep1ToAfterStep4(); // skip steps 1-4
        }
    }

    /**
     *
     * @return URL connection
     */
    public HttpURLConnection getUrlConnection() {

        return urlConnection;
    }

    /**
     * Calls the disconnect method of the HTTP URL Connection object
     */
    public void disconnect() {

        urlConnection.disconnect();
    }

    public Node readHeaders( Document dom, Node responseNode, int contentLength ) throws Exception {

        Map<String, List<String>> headersMap = getHeadersMap(urlConnection);

        // add headers
        for (Entry<String, List<String>> headerEntry : headersMap.entrySet()) {

            String headerValue = null;
            String headerName = headerEntry.getKey();
            List<String> headerValues = headerEntry.getValue();
            if (headerValues.size() > 1
                && HeaderMatcher.SET_COOKIE_HEADER_NAME.equalsIgnoreCase(headerName)) {

                // There are more than one "Set-Cookie" headers and we need to merge them in one, separated with ';'
                StringBuilder sb = new StringBuilder();
                for (String value : headerValues) {
                    if (value.endsWith(";")) {
                        sb.append(value);
                    } else {
                        sb.append(value + ';');
                    }
                }
                headerValue = sb.toString();
            } else {

                headerValue = headerValues.get(0);
            }

            if (headerName == null) {
                // this should be the response status header, something like 'HTTP/1.1 200 OK'
                String responseStatusString = headerValue;
                String responseStatus = responseStatusString.substring(responseStatusString.indexOf(' ')
                                                                       + 1, responseStatusString.length());

                // add response result
                Element actionResponseResult = dom.createElement(TOKEN_HTTP_RESPONSE_RESULT);
                actionResponseResult.appendChild(dom.createTextNode(responseStatus));
                responseNode.appendChild(actionResponseResult);
            } else {

                // we will skip "Transfer-Encoding" header because we have to verify the content length
                // and will replace it with "Content-Length" header, which is always expected in the recorded XML files
                if (headerName.equalsIgnoreCase(HeaderMatcher.TRANSFER_ENCODING_HEADER_NAME)
                    && contentLength > -1) {

                    headerName = HeaderMatcher.CONTENT_LENGTH_HEADER_NAME;
                    headerValue = String.valueOf(contentLength);
                }

                Element header = dom.createElement(TOKEN_HTTP_HEADER);
                header.setAttribute(TOKEN_HEADER_NAME_ATTRIBUTE, headerName);
                header.setAttribute(TOKEN_HEADER_VALUE_ATTRIBUTE, headerValue);

                responseNode.appendChild(header);
            }
        }

        return responseNode;
    }

    public byte[] readWholeBodyBytes() throws IOException {

        ByteArrayOutputStream builder;
        stopWatch.step6_StartGetResponseCode(); // Could be right to phase 8. Added to be similar to other response case
        int responseCode = -1;
        try {
            responseCode = urlConnection.getResponseCode();
        } finally {
            stopWatch.step7_EndGetResponseCode();
        }

        InputStream is = null;
        BufferedInputStream bytesBuffer = null;
        stopWatch.step8_stopInterimTimeAndStartReceivingResponseData();
        try {
            if (responseCode >= HTTP_MIN_RESPONSE_CODE_FOR_ERROR_STREAM) {
                // get a chance to see response contents even in case of error status
                is = urlConnection.getErrorStream();
            } else {
                is = urlConnection.getInputStream();
            }

            bytesBuffer = new BufferedInputStream(is);
            builder = new ByteArrayOutputStream();

            /*
             * Here we read all the content body to byte buffer, because we propose that
             *  this is relatively short body, so it is not an issue to load it into the memory
             */
            int byteRead;
            int available;
            byte[] buff;
            for (;;) {
                available = bytesBuffer.available();
                if (available > 0) {

                    buff = new byte[available];
                    bytesBuffer.read(buff, 0, available);
                    builder.write(buff);
                } else if ( (byteRead = bytesBuffer.read()) != -1) {

                    builder.write(byteRead);
                } else { // EOF

                    break;
                }
            }
        } finally {
            IoUtils.closeStream(bytesBuffer); // closes also underlying InputStream
            stopWatch.step9_endReceivingResponseData();
        }
        return builder.toByteArray();
    }

    public Node httpBodyToXml( Document dom, String actionsXml, int actionNum,
                               boolean saveResponseBodyBytes ) throws Exception {

        // save binary file and add a reference to it in the XML
        Element resourceFileNode = null;
        if (urlConnection.getDoInput()) {
            String contentType = urlConnection.getContentType();
            if (contentType == null) {
                log.warn("No 'Content-Type' header in the response");
            }
            String resourceFileExtension = MimeTypeFileExtensionMapper.getFileExtension(contentType,
                                                                                        urlConnection.getContentEncoding());
            if (resourceFileExtension == null) {

                resourceFileExtension = "bin";
                log.warn("Unknown content type: " + contentType
                         + ". Resource file will be saved with 'bin' extension. Request URL: "
                         + urlConnection.getURL());
            }
            resourceFileNode = saveResourceFile(dom, resourceFileExtension, actionsXml, actionNum,
                                                saveResponseBodyBytes);
        }

        return resourceFileNode;
    }

    public byte[] getResponseBodyBytes() {

        return responseBodyBytes;
    }

    private static void parseSystemProperties() {

        String prop = AtsSystemProperties.getPropertyAsString(AtsSystemProperties.AGENT__TEMPLATE_ACTIONS_PROXY_PROPERTY);
        if (prop != null) {
            log.info("Found proxy defined : " + prop);
            String[] parts = prop.split(":");

            if (parts.length == 0 || parts.length > 3) {
                throw new IllegalArgumentException("Illegal proxy specified: '" + prop + "'. "
                                                   + FORMAT_PROXY);
            }
            if (!"http".equalsIgnoreCase(parts[0])) {
                throw new IllegalArgumentException("Only 'http' is supported for templateactions proxy URL."
                                                   + FORMAT_PROXY);
            }

            String port = null;
            if (parts.length == 2) {
                port = "8888";
            } else {
                port = parts[2];
            }
            int portInt = -1;
            try {
                portInt = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid port specified for proxy : " + port + ". "
                                                   + FORMAT_PROXY);
            }
            if (portInt < 1 || portInt > 64 * 1024) {
                throw new IllegalArgumentException("Invalid port specified for proxy : " + port
                                                   + FORMAT_PROXY);
            }
            proxyPort = portInt;

            proxyHost = parts[1];
            proxyHost = proxyHost.replace("//", "");
            log.info("Will use proxy 'http://" + proxyHost + ":" + portInt + "' for template actions");

        } else {
            proxyHost = null;
            proxyPort = -1;
        }
    }

    private Map<String, List<String>> getHeadersMap( URLConnection urlConnection ) throws Exception {

        Map<String, List<String>> headersMap = null;

        try {
            headersMap = urlConnection.getHeaderFields();
        } catch (Exception e) {
            log.warn("Unable to resolve response headers", e);
        }

        if (headersMap == null) {
            // retry once
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {}

            try {
                headersMap = urlConnection.getHeaderFields();
            } catch (Exception e) {
                log.error("Unable to resolve response headers for second and final time", e);
                throw e;
            }
        }

        return headersMap;
    }

    private Element saveResourceFile( Document dom, String resourceFileExtension, String actionsXml,
                                      int actionNum,
                                      boolean saveRespBodyBytesForFileStore ) throws HttpClientException {

        boolean matchFilesByContent = false;
        Boolean templateActionsMatchFilesByContent = ConfigurationSettings.getInstance()
                                                                          .isTemplateActionsMatchFilesByContent();
        if (templateActionsMatchFilesByContent != null) {
            matchFilesByContent = templateActionsMatchFilesByContent.booleanValue();
        }

        String resourceFileName = TOKEN_HTTP_RESPONSE_FILE_EXPECTED + actionNum + "." + resourceFileExtension;
        String resourceFile = null;
        if (matchFilesByContent) {
            String actualResourcesDir = getActualResourcesDir(actionsXml);
            resourceFile = getCurrentThreadDir(actualResourcesDir)
                           + AtsSystemProperties.SYSTEM_FILE_SEPARATOR + resourceFileName;
        }

        long contentLength = 0l;
        InputStream is = null;
        OutputStream fileOutputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            int httpResponseCode = 0;

            stopWatch.step8_stopInterimTimeAndStartReceivingResponseData();
            httpResponseCode = urlConnection.getResponseCode();
            // TODO: consume response via getErrorStream() in this case (status code > 400))
            if (httpResponseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                log.info("HTTP response code: 401 Unauthorized. File " + resourceFileName
                         + " will not be saved.");
                stopWatch.step9_endReceivingResponseData();
                return null;
            }
            boolean saveBodyForUseOrLogging = saveRespBodyBytesForFileStore;
            if (!saveBodyForUseOrLogging) {
                saveBodyForUseOrLogging = log.isTraceEnabled();
            }
            try {
                if (httpResponseCode >= HTTP_MIN_RESPONSE_CODE_FOR_ERROR_STREAM) {
                    // default JDK implementation throws exception for getInputStream() if response code >= 400
                    // to get contents of error like "500 Internal server error" we use this to get a chance to see reason details
                    is = urlConnection.getErrorStream();
                } else {
                    is = urlConnection.getInputStream();
                }
                if (matchFilesByContent) {
                    fileOutputStream = new BufferedOutputStream(new FileOutputStream(resourceFile,
                                                                                     false));
                }

                if (saveBodyForUseOrLogging) {
                    byteArrayOutputStream = new ByteArrayOutputStream(is.available());
                }

                byte[] buffer = new byte[CHUNK_LENGTH];
                int lastBytesRead = 0;
                boolean maxNumberOfBytesForPrintingAlreadyReached = false;
                while ( (lastBytesRead = is.read(buffer, 0, buffer.length)) != -1) {

                    contentLength += lastBytesRead;
                    // write bytes to file, only if we want verification by file content
                    if (matchFilesByContent) {
                        fileOutputStream.write(buffer, 0, lastBytesRead);
                        fileOutputStream.flush();
                    }
                    if (saveBodyForUseOrLogging) {
                        if (saveRespBodyBytesForFileStore
                            || (!maxNumberOfBytesForPrintingAlreadyReached
                                && (contentLength
                                    - lastBytesRead < XmlUtilities.MAX_RESPONSE_BODY_BYTES_TO_PRINT))) {
                            // option to write HTTP response body for investigation. Especially if response code is >= 500
                            byteArrayOutputStream.write(buffer, 0, lastBytesRead);
                        } else {
                            if (!maxNumberOfBytesForPrintingAlreadyReached) {
                                String msg = " ... ( Too big response. Rest truncated)"; // ISO-Latin - 1 byte per char
                                byteArrayOutputStream.write(msg.getBytes(), 0, msg.length());
                                maxNumberOfBytesForPrintingAlreadyReached = true;
                            }
                        }
                    }
                }
            } finally {
                // here we assume that time of possible response write to disk (off by default)
                // and buffer ByteArryaOutputStream writing is minimum so we include it
                stopWatch.step9_endReceivingResponseData();

            }

            if (contentLength == 0) {
                log.info("Response (sequence name: " + resourceFileName
                         + ") will not be saved, because its length is 0");
                return null;
            }

            if (saveBodyForUseOrLogging) {
                responseBodyBytes = byteArrayOutputStream.toByteArray();
            }

            if (matchFilesByContent) {
                log.info("Saved " + resourceFile + " response resource (file) with length "
                         + contentLength);
            } else {
                log.info("Complete response resource read. Sequence name: " + resourceFileName
                         + " with length " + contentLength + " bytes");
            }

        } catch (Exception e) {

            throw new HttpClientException("Error saving HTTP resource file " + (resourceFile != null
                                                                                                     ? resourceFile
                                                                                                     : resourceFileName),
                                          e);
        } finally {
            IoUtils.closeStream(fileOutputStream);
            IoUtils.closeStream(is);
        }

        // create and return the file XML element
        Element resourceFileNode = dom.createElement(TOKEN_HTTP_RESOURCE_FILE);
        resourceFileNode.setAttribute("size", String.valueOf(contentLength));
        if (matchFilesByContent) {
            resourceFileNode.appendChild(dom.createTextNode(resourceFileName));
        }
        return resourceFileNode;
    }

    private String getActualResourcesDir( String actionsXml ) {

        String resourcesDirString = actionsXml.substring(0, actionsXml.length() - ".xml".length())
                                    + AtsSystemProperties.SYSTEM_FILE_SEPARATOR + "actual";
        File resourcesDir = new File(resourcesDirString);
        if (!resourcesDir.exists()) {
            resourcesDir.mkdir();
            log.info("created resources directory: " + resourcesDirString);
        }
        return resourcesDirString;
    }

    private String getCurrentThreadDir( String actualResourcesDir ) {

        String resourcesDirString = actualResourcesDir + AtsSystemProperties.SYSTEM_FILE_SEPARATOR
                                    + Thread.currentThread().getName();
        File resourcesDir = new File(resourcesDirString);
        if (!resourcesDir.exists()) {
            resourcesDir.mkdir();
        }
        return resourcesDirString;
    }

    /**
     * A dummy Trust Manager which does not perform validation of certificates if they come from trusted CA,
     * it just trusts everything
     */
    private static class DefaultTrustManager implements X509TrustManager {

        public void checkClientTrusted( X509Certificate[] cert, String authType ) {

            // no exception is thrown, so everything is trusted
        }

        public void checkServerTrusted( X509Certificate[] cert, String authType ) {

            // no exception is thrown, so everything is trusted

        }

        public X509Certificate[] getAcceptedIssuers() {

            return new X509Certificate[0];
        }
    }

    /**
     * A dummy Hostname verifier
     */
    private static class DefaultHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify( String hostname, SSLSession session ) {

            return true;
        }
    }

    /**
     * Get access to stop watch. Currently used in XmlUtitlities for request/resp. time tracking
     * @return
     */
    public NetworkingStopWatch getNetworkingStopWatch() {

        return stopWatch;
    }
}
