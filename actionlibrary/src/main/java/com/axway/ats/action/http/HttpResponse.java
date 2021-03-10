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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.regex.Pattern;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimePart;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import com.axway.ats.action.exceptions.VerificationException;
import com.axway.ats.action.json.JsonText;
import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.xml.XmlText;
import com.axway.ats.common.PublicAtsApi;

/**
 * This class holds the HTTP response from calling HTTP POST, PUT, GET or DELETE, see
 * {@link com.axway.ats.action.http.HttpClient#post() post()}, {@link com.axway.ats.action.http.HttpClient#put() put()},
 * {@link com.axway.ats.action.http.HttpClient#get() get()} and {@link com.axway.ats.action.http.HttpClient#delete() delete()}.
 */
@PublicAtsApi
public class HttpResponse {

    /**
     * The status code e.g. 200
     */
    private int                 statusCode;
    /**
     * The status message e.g. 'OK'
     */
    private String              statusMessage;
    /**
     * The response headers.
     */
    private List<HttpHeader>    headers;
    /**
     * The response body.
     */
    private byte[]              body;

    private static final Logger log = LogManager.getLogger(HttpResponse.class);

    /**
     * Construct a HTTPResponse when there is a response body.
     *
     * @param statusCode The status code e.g. 200
     * @param statusMessage The status message e.g. 'OK'
     * @param headers The response headers
     * @param body The response body
     */
    public HttpResponse( int statusCode,
                         String statusMessage,
                         List<HttpHeader> headers,
                         byte[] body ) {

        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = headers;
        this.body = body;
    }

    /**
     * Construct a HTTPResponse when there is no response body.
     *
     * @param statusCode The status code e.g. 200
     * @param statusMessage The status message e.g. 'OK'
     * @param headers The response headers
     */
    public HttpResponse( int statusCode,
                         String statusMessage,
                         List<HttpHeader> headers ) {

        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = headers;
    }

    /**
     * Get the response status code.
     *
     * @return The status code.
     */
    @PublicAtsApi
    public int getStatusCode() {

        return statusCode;
    }

    /**
     * Get the response status message.
     *
     * @return The status message.
     */
    @PublicAtsApi
    public String getStatusMessage() {

        return statusMessage;
    }

    /**
     * Get all response headers.
     *
     * @return the response headers
     */
    @PublicAtsApi
    public HttpHeader[] getHeaders() {

        return headers.toArray(new HttpHeader[headers.size()]);
    }

    /**
     * Get a response header
     *
     * @param name the header name
     *  It is case-insensitive according to HTTP/1.1 specification (rfc2616#sec4.2)
     * @return the header
     */
    @PublicAtsApi
    public HttpHeader getHeader(
                                 String name ) {

        for (HttpHeader header : headers) {
            if (header.getKey().equalsIgnoreCase(name)) {
                return header;
            }
        }

        return null;
    }

    /**
     * Get the values of a response header.
     * It returns null if no header is found with the provided name.
     *
     * @param name the header name
     *  It is case-insensitive according to HTTP/1.1 specification (rfc2616#sec4.2)
     * @return the header values
     */
    @PublicAtsApi
    public String[] getHeaderValues(
                                     String name ) {

        for (HttpHeader header : headers) {
            if (header.getKey().equalsIgnoreCase(name)) {
                List<String> values = header.getValues();
                return values.toArray(new String[values.size()]);
            }
        }

        return null;
    }

    /**
     * Get the response body as a byte array.
     *
     * @return The body
     */
    @PublicAtsApi
    public byte[] getBody() {

        return body;
    }

    /**
     * Get the response body as a string.
     *
     * @return The body
     */
    @PublicAtsApi
    public String getBodyAsString() {

        if (body == null)
            return null;
        return new String(body);
    }

    /**
     * Get the response body as a string.
     *
     * @param charset The charset
     * @return The body
     */
    @PublicAtsApi
    public String getBodyAsString(
                                   String charset ) {

        if (body == null)
            return null;
        try {
            return new String(body, charset);
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            return getBodyAsString();
        }
    }

    /**
     * Get the response body as a XML Document. Note that Content-Type must indicate
     * that the body is XML, e.g. "text/xml", "application/soap+xml".
     * For multipart messages, the first part that has a Content-Type that indicates XML, (if any),
     * will be converted to a XML Document and returned.
     *
     * @return The body as a XML Document
     * @throws HttpException
     */
    @PublicAtsApi
    public Document getBodyAsXML() throws HttpException {

        if (body == null) {
            return null;
        }

        String contentType = null;
        for (HttpHeader header : headers) {
            if ("Content-Type".equalsIgnoreCase(header.getKey())) {
                contentType = header.getValue();
                break;
            }
        }

        if (contentType != null) {
            if (contentType.contains("xml")) {
                return getDocument(body);
            } else if (contentType.contains("multipart")) {
                // Get the first part of the multipart that has "xml" in its
                // Content-Type header as a Document.
                try {
                    InputStream is = new ByteArrayInputStream(body);
                    MimePackage mime = new MimePackage(is);
                    List<MimePart> parts = mime.getMimeParts();
                    for (MimePart part : parts) {
                        String[] partContentTypes = part.getHeader("Content-Type");
                        for (String partContentType : partContentTypes) {
                            if (partContentType.contains("xml")) {
                                // We have an XML document
                                MimeBodyPart p = (MimeBodyPart) part;
                                return getDocument(IOUtils.toByteArray(p.getRawInputStream()));
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new HttpException("Error trying to extract main XML document from multipart message.",
                                            e);
                }
            }
        }

        return null;
    }

    /**
     * Get the response body as a XMLText object. Note that Content-Type must indicate
     * that the body is XML, e.g. "text/xml", "application/soap+xml".
     * For multipart messages, the first part that has a Content-Type that indicates XML, (if any),
     * will be converted to a XMLText object and returned.
     *
     * @return The body as a XMLText object
     * @throws HTTPException
     */

    @PublicAtsApi
    public XmlText getBodyAsXmlText() {

        if (body == null) {
            return null;
        }

        String contentType = null;
        for (HttpHeader header : headers) {
            if (header.getKey().equalsIgnoreCase("Content-Type")) {
                contentType = header.getValue();
                break;
            }
        }

        if (contentType != null) {
            if (contentType.contains("xml")) {
                return new XmlText(new String(body));
            } else if (contentType.contains("multipart")) {
                // Get the first part of the multipart that has "xml" in its
                // Content-Type header as a XMLText.
                try {
                    InputStream is = new ByteArrayInputStream(body);
                    MimePackage mime = new MimePackage(is);
                    List<MimePart> parts = mime.getMimeParts();
                    for (MimePart part : parts) {
                        String[] partContentTypes = part.getHeader("Content-Type");
                        for (String partContentType : partContentTypes) {
                            if (partContentType.contains("xml")) {
                                // We have an XMLText object
                                MimeBodyPart p = (MimeBodyPart) part;
                                return new XmlText(new String(IOUtils.toByteArray(p.getRawInputStream())));
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new HttpException("Error while trying to convert multipart message's content to a XMLText object",
                                            e);
                }
            }
        }

        return null;

    }

    /**
     * Get the response body as a JSONText object. Note that Content-Type must indicate
     * that the body is JSON, e.g. "application/json".
     * For multipart messages, the first part that has a Content-Type that indicates JSON, (if any),
     * will be converted to a JSONText object and returned.
     *
     * @return The body as a JSONText object
     * @throws HTTPException
     */

    @PublicAtsApi
    public JsonText getBodyAsJsonText() {

        if (body == null) {
            return null;
        }

        String contentType = null;
        for (HttpHeader header : headers) {
            if (header.getKey().equalsIgnoreCase("Content-Type")) {
                contentType = header.getValue();
                break;
            }
        }

        if (contentType != null) {
            if (contentType.contains("json")) {
                return new JsonText(new String(body));
            } else if (contentType.contains("multipart")) {
                // Get the first part of the multipart that has "json" in its
                // Content-Type header as a JSONText.
                try {
                    InputStream is = new ByteArrayInputStream(body);
                    MimePackage mime = new MimePackage(is);
                    List<MimePart> parts = mime.getMimeParts();
                    for (MimePart part : parts) {
                        String[] partContentTypes = part.getHeader("Content-Type");
                        for (String partContentType : partContentTypes) {
                            if (partContentType.contains("json")) {
                                // We have an JSONText object
                                MimeBodyPart p = (MimeBodyPart) part;
                                return new JsonText(new String(IOUtils.toByteArray(p.getRawInputStream())));
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new HttpException("Error while trying to convert multipart message's content to a JSONText object",
                                            e);
                }
            }
        }

        return null;

    }

    /**
     * Verify that the response contains header exact value
     * Be sure to use the right case, the search is case sensitive
     * 
     * @param header header name 
     * @param value header value 
     * @return 
     */
    @PublicAtsApi
    public HttpResponse verifyHeader( String header, String value ) {

        HttpHeader response = getHeader(header);

        if (response == null) {
            throw new VerificationException("Header \"" + header + "\" does not exist in the response!");
        }

        if (!response.getValue().equals(value)) {
            throw new VerificationException("Header with name \"" + header + "\" has value \""
                                            + response.getValue() + "\" while the expected value is \""
                                            + value + "\"");
        }

        return this;
    }

    /**
     * Verify part of the response body 
     * Be sure to use the right case, the search is case sensitive
     * 
     * @param responseBodyPart response body part, to be verified 
     * @return
     */
    @PublicAtsApi
    public HttpResponse verifyBodyContains( String responseBodyPart ) {

        String responseBody = getBodyAsString();

        if (responseBody != null && responseBody.indexOf(responseBodyPart) >= 0) {
            return this;
        }
        throw new VerificationException("The given response part \"" + responseBodyPart
                                        + "\" is not present in the real response body");
    }

    /**
     * Verify the exact response body 
     * Be sure to use the right case, the search is case sensitive
     * 
     * @param body response body to be verified
     * @return
     */
    @PublicAtsApi
    public HttpResponse verifyBodyMatch( String body ) {

        String responseBody = getBodyAsString();

        if (responseBody != null) {
            if (responseBody.equals(body))
                return this;
        }
        throw new VerificationException("The actual response body is \"" + responseBody
                                        + "\" while the expected is \"" + body + "\"");
    }

    /**
     * Verify response body by REGEX
     * Be sure to use the right case, the search is case sensitive
     * 
     * @param responseBodyRegex REGEX that should verify the response body
     * @return
     */
    @PublicAtsApi
    public HttpResponse verifyBodyRegex( String responseBodyRegex ) {

        String responseBody = getBodyAsString();

        if (responseBody != null) {
            if (Pattern.compile(responseBodyRegex).matcher(responseBody).find())
                return this;
        }
        throw new VerificationException("The given regex \"" + responseBodyRegex
                                        + "\" does not match the body");
    }

    /**
     * Verify response status code
     * 
     * @param statusCode
     * @return
     */
    @PublicAtsApi
    public HttpResponse verifyStatusCode( int statusCode ) {

        if (this.statusCode == statusCode)
            return this;

        throw new VerificationException("Expected status code is: " + statusCode + ", actual status code is: "
                                        + this.statusCode);
    }

    /**
     * Verify response status message
     * Be sure to use the right case, the search is case sensitive
     * 
     * @param statusMessage message contained in the response status message
     * @return
     */
    @PublicAtsApi
    public HttpResponse verifyStatusMessage( String statusMessage ) {

        if (getStatusMessage().equalsIgnoreCase(statusMessage))
            return this;

        throw new VerificationException("Expected status message is: \"" + statusMessage
                                        + "\", actual status message is: \"" + this.statusMessage + "\"");
    }

    /**
     * Convert byte array to Document
     *
     * @param body Byte array
     * @return
     * @throws HttpException
     */
    private Document getDocument(
                                  byte[] body ) throws HttpException {

        DocumentBuilderFactory newInstance = DocumentBuilderFactory.newInstance();
        newInstance.setNamespaceAware(true);
        try {
            return newInstance.newDocumentBuilder().parse(new ByteArrayInputStream(body));
        } catch (Exception e) {
            throw new HttpException("Exception trying to convert response to Document.", e);
        }
    }
}
