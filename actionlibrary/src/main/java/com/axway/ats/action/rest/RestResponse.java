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
package com.axway.ats.action.rest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.axway.ats.action.exceptions.VerificationException;
import com.axway.ats.common.xml.XMLException;
import com.axway.ats.action.json.JsonText;
import com.axway.ats.action.xml.XmlText;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;

/**
 * A representation of a REST response
 */
@PublicAtsApi
public class RestResponse {

    private static final Logger log               = Logger.getLogger( RestResponse.class );

    public static final int     MAX_RESPONSE_SIZE = 10485760;                              //10MB

    private Response            response;

    RestResponse( Response response ) {

        this.response = response;
    }

    /**
     * @return the status code
     */
    @PublicAtsApi
    public int getStatusCode() {

        return response.getStatus();
    }

    /**
     * @return the status message
     */
    @PublicAtsApi
    public String getStatusMessage() {

        return response.getStatusInfo().toString();
    }

    /**
     * Returns Content-Length as integer if present. In other cases returns -1.
     * @return response's content length in bytes
     */
    @PublicAtsApi
    public int getContentLength() {

        return response.getLength();
    }

    /**
     * Return the body as a String
     *
     * @return the body as a String
     */
    @PublicAtsApi
    public String getBodyAsString() {

        checkResponseBodyStatus();

        return response.readEntity( String.class );
    }

    /**
     * Return the response body as any java Object
     *
     * @param theClass the type of the object to return
     * @return the body as an object
     */
    @PublicAtsApi
    public <T> T getBodyAsObject( Class<T> theClass ) {

        checkResponseBodyStatus();

        return response.readEntity( theClass );
    }

    /**
     * Return the response body as JSON text
     *
     * @return the body as JSON text
     */
    @PublicAtsApi
    public JsonText getBodyAsJson() {

        checkResponseBodyStatus();

        String jsonText = response.readEntity( String.class );
        if( !StringUtils.isNullOrEmpty( jsonText ) ) {

            return new JsonText( jsonText.trim() );
        } else {
            log.warn( "JSON response is empty, we return a null " + JsonText.class.getSimpleName()
                      + " object" );
            return null;
        }
    }

    /**
     * Return the response body as XML text
     *
     * @return the body as XML text
     * @throws XMLException 
     */
    @PublicAtsApi
    public XmlText getBodyAsXml() throws XMLException {

        checkResponseBodyStatus();

        String xmlText = response.readEntity( String.class );
        if( !StringUtils.isNullOrEmpty( xmlText ) ) {

            return new XmlText( xmlText.trim() );
        } else {
            log.warn( "JSON response is empty, we return a null " + XmlText.class.getSimpleName()
                      + " object" );
            return null;
        }
    }

    /**
     * Return the body as an InputStream. 
     * The user is responsible for closing the returned stream.
     * 
     * @return the body as an InputStream
     */
    @PublicAtsApi
    public InputStream getBodyAsInputStream() {

        checkResponseBodyStatus();

        return response.readEntity( InputStream.class );
    }

    /**
     * Get all response headers.
     *
     * @return the response headers
     */
    @PublicAtsApi
    public RestHeader[] getHeaders() {

        List<RestHeader> headers = new ArrayList<RestHeader>();

        MultivaluedMap<String, Object> respHeaders = response.getHeaders();
        for( Entry<String, List<Object>> respHeaderEntry : respHeaders.entrySet() ) {
            headers.add( RestHeader.constructRESTHeader( respHeaderEntry.getKey(), respHeaderEntry.getValue() ) );
        }

        return headers.toArray( new RestHeader[headers.size()] );
    }

    /**
     * Get a response header
     *
     * @param name the header name
     *  It is case-insensitive according to HTTP/1.1 specification (rfc2616#sec4.2)
     * @return the header
     */
    @PublicAtsApi
    public RestHeader getHeader( String name ) {

        MultivaluedMap<String, Object> respHeaders = response.getHeaders();
        for( String hName : respHeaders.keySet() ) {
            if( hName.equalsIgnoreCase( name ) ) {
                return ( RestHeader.constructRESTHeader( name, respHeaders.get( name ) ) );
            }
        }

        return null;
    }

    /**
     * Get the values of a response header.
     * It returns null if no header is found with the provided name.
     *
     * @param name the header name.
     *  It is case-insensitive according to HTTP/1.1 specification (rfc2616#sec4.2)
     * @return the header values
     */
    @PublicAtsApi
    public String[] getHeaderValues( String name ) {

        MultivaluedMap<String, Object> respHeaders = response.getHeaders();
        for( String hName : respHeaders.keySet() ) {
            if( hName.equalsIgnoreCase( name ) ) {
                List<String> values = new ArrayList<String>();
                for( Object valueObject : respHeaders.get( name ) ) {
                    values.add( valueObject.toString() );
                }
                return values.toArray( new String[values.size()] );
            }
        }

        return null;
    }

    /**
     * @return the new cookies retrieved from the response
     */
    @PublicAtsApi
    public NewCookie[] getNewCookies() {

        Collection<NewCookie> newCookies = response.getCookies().values();
        return newCookies.toArray( new NewCookie[newCookies.size()] );
    }

    /**
     * Verify that the response contains header exact value.
     * 
     * @param header header name Header name search is case insensitive
     * @param value header value Header value search is case sensitive 
     * @return 
     */
    @PublicAtsApi
    public RestResponse verifyHeader( String header, String value ) {

        RestHeader response = getHeader( header );

        if( response == null ) {
            throw new VerificationException( "Header \"" + header + "\" does not exist in the response!" );
        }

        if( !response.getValue().equals( value ) ) {
            throw new VerificationException( "Header name \"" + header + "\" with value \"" + value
                                             + "\" is not the same as in the header: \""
                                             + response.getValue() );
        }

        return this;
    }

    /**
     * Verify JSON key with its value
     * Be sure to use the right case, the search is case sensitive
     * 
     * @param key  key name
     * @param value key value 
     * @return
     */
    @PublicAtsApi
    public RestResponse verifyJsonBody( String keyPath, String value ) {

        String valueString = getBodyAsJson().get( keyPath ).toString();
        if( valueString == null ) {
            throw new VerificationException( "Key \"" + keyPath + "\" not found" );
        }

        if( !valueString.equals( value ) ) {
            throw new VerificationException( "Key \"" + keyPath + "\" has the value of \"" + valueString
                                             + "\" while it is expected to be \"" + value + "\"" );
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
    public RestResponse verifyBodyContains( String responseBodyPart ) {

        checkResponseBodyStatus();

        String responseBody = response.readEntity( String.class );

        if( responseBody != null && responseBody.indexOf( responseBodyPart ) >= 0 ) {
            return this;
        }
        throw new VerificationException( "The given response part \"" + responseBodyPart
                                         + "\" does not present in the real response body" );
    }

    /**
     * Verify the exact response body 
     * Be sure to use the right case, the search is case sensitive
     * 
     * @param body response body to be verified
     * @return
     */
    @PublicAtsApi
    public RestResponse verifyBodyMatch( String body ) {

        checkResponseBodyStatus();

        String responseBody = response.readEntity( String.class );

        if( responseBody != null ) {
            if( responseBody.equals( body ) )
                return this;
        }
        throw new VerificationException( "The given body \"" + body
                                         + "\" does not match the real response body" );
    }

    /**
     * Verify response body by REGEX
     * Be sure to use the right case, the search is case sensitive
     * 
     * @param responseBodyRegex REGEX that should verify the response body
     * @return
     */
    @PublicAtsApi
    public RestResponse verifyBodyRegex( String responseBodyRegex ) {

        checkResponseBodyStatus();

        String responseBody = response.readEntity( String.class );

        if( responseBody != null ) {
            if( Pattern.compile( responseBodyRegex ).matcher( responseBody ).find() )
                return this;
        }
        throw new VerificationException( "The given regex \"" + responseBodyRegex
                                         + "\" does not match the body" );
    }

    /**
     * Verify response status code
     * 
     * @param statusCode
     * @return
     */
    @PublicAtsApi
    public RestResponse verifyStatusCode( int statusCode ) {

        if( response.getStatus() == statusCode )
            return this;

        throw new VerificationException( "You expect " + statusCode + " but the actual status code is: "
                                         + response.getStatus() );
    }

    /**
     * Verify response status message.  
     * Be sure to use the right case, the search is case sensitive
     * 
     * @param statusMessage message contained in the response status message
     * @return
     */
    @PublicAtsApi
    public RestResponse verifyStatusMessage( String statusMessage ) {

        String realStatusMessage = response.getStatusInfo().toString();
        if( realStatusMessage.equalsIgnoreCase( statusMessage ) )
            return this;

        throw new VerificationException( "Expected status message \"" + statusMessage
                                         + "\",real status message \"" + realStatusMessage + "\"" );
    }

    private void checkResponseBodyStatus() {

        /* prevents java.lang.IllegalStateException: Entity input stream has already been closed. 
         * if the file size is less than MAX_RESPONSE_SIZE
        */
        if( response.getLength() < MAX_RESPONSE_SIZE ) {
            response.bufferEntity();
        }
    }

    @Override
    protected void finalize() throws Throwable {

        /* In many cases the user just inspects the response status code or headers without reading the response entity.
         * This leads to memory leaks because the buffered entity data is not released.
         * Here we make an effort to release that memory.
         * 
         * In cases the response entity is given to the user as a stream, we leave the user the
         * responsibility for closing that stream.
         * 
         * There is no problem to call this code even if the response is already closed.
         */
        response.close();

        super.finalize();
    }
}
