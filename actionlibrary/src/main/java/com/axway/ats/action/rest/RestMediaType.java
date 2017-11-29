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

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;

/**
 * Defines the supported values of the request "Content-Type" and response "Accept" headers
 */
@PublicAtsApi
public class RestMediaType {

    private static final Logger log                         = Logger.getLogger( RestMediaType.class );

    /**
     * text/plain
     */
    @PublicAtsApi
    public static final String  TEXT_PLAIN                  = MediaType.TEXT_PLAIN;

    /**
     * text/xml
     */
    @PublicAtsApi
    public static final String  TEXT_XML                    = MediaType.TEXT_XML;

    /**
     * text/html
     */
    @PublicAtsApi
    public static final String  TEXT_HTML                   = MediaType.TEXT_HTML;

    /**
     * application/json
     */
    @PublicAtsApi
    public static final String  APPLICATION_JSON            = MediaType.APPLICATION_JSON;

    /**
     * application/xml
     */
    @PublicAtsApi
    public static final String  APPLICATION_XML             = MediaType.APPLICATION_XML;

    /**
     * application/x-www-form-urlencoded
     */
    @PublicAtsApi
    public static final String  APPLICATION_FORM_URLENCODED = MediaType.APPLICATION_FORM_URLENCODED;

    /**
     * multipart/form-data
     */
    @PublicAtsApi
    public static final String  MULTIPART_FORM_DATA         = MediaType.MULTIPART_FORM_DATA;

    /**
     * multipart/mixed
     */
    @PublicAtsApi
    public static final String  MULTIPART_MIXED             = "multipart/mixed";

    /**
     * application/octet-stream
     */
    @PublicAtsApi
    public static final String  APPLICATION_OCTET_STREAM    = MediaType.APPLICATION_OCTET_STREAM;

    /**
     * application/vnd.api+json
     */
    @PublicAtsApi
    public static final String  APPLICATION_VND_API_JSON    = "application/vnd.api+json";

    static String checkValueIsValid( String mediaType ) {

        if( StringUtils.isNullOrEmpty( mediaType ) ) {
            return null;
        }

        mediaType = mediaType.trim().toLowerCase();

        if( !TEXT_PLAIN.equals( mediaType ) && !TEXT_XML.equals( mediaType ) && !TEXT_HTML.equals( mediaType )
            && !APPLICATION_JSON.equals( mediaType ) && !APPLICATION_XML.equals( mediaType )
            && !MULTIPART_FORM_DATA.equals( mediaType ) && !APPLICATION_FORM_URLENCODED.equals( mediaType )
            && !MULTIPART_MIXED.equals( mediaType ) && !APPLICATION_OCTET_STREAM.equals( mediaType )
            && !APPLICATION_VND_API_JSON.equals( mediaType ) ) {

            log.warn( "'" + mediaType + "' is not a valid or supported REST media type" );
        }

        return mediaType;
    }

    static MediaType toMediaType( String typeAndSubtype, String charset ) {

        String[] typeTokens = typeAndSubtype.split( "/" );
        return new MediaType( typeTokens[0], typeTokens[1], charset );
    }
}
