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
import java.io.UnsupportedEncodingException;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import com.axway.ats.common.PublicAtsApi;

/**
 * Holder class used to create multipart request messages to send over HTTP via HTTPClient.
 */
@PublicAtsApi
public class HttpBodyPart {

    // part name
    private String name;

    // part content type
    private String contentType;
    // part content charset
    private String charset;

    // part content used with FILE part
    private String filePath;

    // part content used with TEXT part
    private String content;

    // part content used with BYTE ARRAY part
    private byte[] fileBytes;
    private String fileName;

    /**
     * Create a FILE part for multipart message
     * 
     * @param name part name
     * @param filePath path to file
     * @return the new body part
     */
    @PublicAtsApi
    public static HttpBodyPart createFilePart(
                                               String name,
                                               String filePath ) {

        HttpBodyPart part = new HttpBodyPart();
        part.name = name;
        part.filePath = filePath;
        return part;
    }

    /**
     * Create a FILE part for multipart message
     * 
     * @param name part name
     * @param filePath path to file
     * @param contentType content type
     * @return the new body part
     */
    @PublicAtsApi
    public static HttpBodyPart createFilePart(
                                               String name,
                                               String filePath,
                                               String contentType ) {

        HttpBodyPart part = new HttpBodyPart();
        part.name = name;
        part.filePath = filePath;
        part.contentType = contentType;
        return part;
    }

    /**
     * Create a FILE part for multipart message
     * 
     * @param name part name
     * @param filePath path to file
     * @param contentType content type
     * @param charset content charset
     * @return the new body part
     */
    @PublicAtsApi
    public static HttpBodyPart createFilePart(
                                               String name,
                                               String filePath,
                                               String contentType,
                                               String charset ) {

        HttpBodyPart part = new HttpBodyPart();
        part.name = name;
        part.filePath = filePath;
        part.contentType = contentType;
        part.charset = charset;
        return part;
    }

    /**
     * Create a TEXT part for multipart message
     * 
     * @param name part name
     * @param content part content
     * @return the new body part
     */
    @PublicAtsApi
    public static HttpBodyPart createTextPart(
                                               String name,
                                               String content ) {

        HttpBodyPart part = new HttpBodyPart();
        part.name = name;
        part.content = content;
        return part;
    }

    /**
     * Create a TEXT part for multipart message
     * 
     * @param name part name
     * @param content part content
     * @param contentType content type
     * @return the new body part
     */
    @PublicAtsApi
    public static HttpBodyPart createTextPart(
                                               String name,
                                               String content,
                                               String contentType ) {

        HttpBodyPart part = new HttpBodyPart();
        part.name = name;
        part.content = content;
        part.contentType = contentType;
        return part;
    }

    /**
     * Create a TEXT part for multipart message
     * 
     * @param name part name
     * @param content part content
     * @param contentType content type
     * @param charset content charset
     * @return the new body part
     */
    @PublicAtsApi
    public static HttpBodyPart createTextPart(
                                               String name,
                                               String content,
                                               String contentType,
                                               String charset ) {

        HttpBodyPart part = new HttpBodyPart();
        part.name = name;
        part.content = content;
        part.contentType = contentType;
        part.charset = charset;
        return part;
    }

    /**
     * Create a BYTE ARRAY part for multipart message
     * 
     * @param name part name
     * @param fileBytes the file content
     * @param fileName the file name
     * @return the new body part
     */
    @PublicAtsApi
    public static HttpBodyPart createByteArrayPart(
                                                    String name,
                                                    byte[] fileBytes,
                                                    String fileName ) {

        HttpBodyPart part = new HttpBodyPart();
        part.name = name;
        part.fileBytes = fileBytes;
        part.fileName = fileName;
        return part;
    }

    /**
     * Create a BYTE ARRAY part for multipart message
     * 
     * @param name part name
     * @param fileBytes the file content
     * @param fileName the file name
     * @param contentType content type
     * @return the new body part
     */
    @PublicAtsApi
    public static HttpBodyPart createByteArrayPart(
                                                    String name,
                                                    byte[] fileBytes,
                                                    String fileName,
                                                    String contentType ) {

        HttpBodyPart part = new HttpBodyPart();
        part.name = name;
        part.fileBytes = fileBytes;
        part.fileName = fileName;
        part.contentType = contentType;
        return part;
    }

    /**
     * Create a BYTE ARRAY part for multipart message
     * 
     * @param name part name
     * @param fileBytes the file content
     * @param fileName the file name
     * @param contentType content type
     * @param charset content charset
     * @return the new body part
     */
    @PublicAtsApi
    public static HttpBodyPart createByteArrayPart(
                                                    String name,
                                                    byte[] fileBytes,
                                                    String fileName,
                                                    String contentType,
                                                    String charset ) {

        HttpBodyPart part = new HttpBodyPart();
        part.name = name;
        part.fileBytes = fileBytes;
        part.fileName = fileName;
        part.contentType = contentType;
        part.charset = charset;
        return part;
    }

    String getName() {

        return name;
    }

    ContentBody constructContentBody() throws UnsupportedEncodingException {

        ContentType contentTypeObject = constructContentTypeObject();

        if( filePath != null ) { // FILE part
            if( contentTypeObject != null ) {
                return new FileBody( new File( filePath ), contentTypeObject );
            } else {
                return new FileBody( new File( filePath ) );
            }
        } else if( content != null ) { // TEXT part
            if( contentTypeObject != null ) {
                return new StringBody( content, contentTypeObject );
            } else {
                return new StringBody( content, ContentType.TEXT_PLAIN );
            }
        } else { // BYTE ARRAY part
            if( contentTypeObject != null ) {
                return new ByteArrayBody( this.fileBytes, contentTypeObject, fileName );
            } else {
                return new ByteArrayBody( this.fileBytes, fileName );
            }
        }
    }

    private ContentType constructContentTypeObject() {

        ContentType _contentType = null;
        if( contentType != null ) {
            if( charset != null ) {
                _contentType = ContentType.create( contentType, charset );
            } else {
                _contentType = ContentType.create( contentType );
            }
        }

        return _contentType;
    }
}
