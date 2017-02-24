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
package com.axway.ats.rbv.imap.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.NoSuchHeaderException;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.model.RbvException;

/**
 * Matches a MIME header.<br><br>
 * Most important parameters are:
 * <li>header name - the header we are interested of
 * <li>expected value - the expected header value
 * <li>match mode - the way we will compare the expected and actual header value
 * <li>part index - used when the header is contained into a MIME part. The index of the first part is 0
 * <li>header index - when the header is available more than once, this parameter specifies which header value we are targeting.
 * The index of the first header value is 0. Use -1 if want to search among all header values
 * <li>nested package path - used to specify the searched header is placed in a sub MIME package
 */
public class HeaderRule extends AbstractImapRule {

    @PublicAtsApi
    public enum HeaderMatchMode {

        /**
         * Search for the string at the beginning of the header value
         */
        @PublicAtsApi
        LEFT,

        /**
         *  Search for the string at the end of the header value
         */
        @PublicAtsApi
        RIGHT,

        /**
         * Search for the string anywhere in the header value
         */
        @PublicAtsApi
        FIND,

        /**
         * Search for match for the whole header value
         */
        @PublicAtsApi
        EQUALS,

        /**
         * Match the header value using a regular expression
         */
        @PublicAtsApi
        REGEX;
    }

    private String             headerName;
    private String             expectedValue;
    private int                partIndex;
    private int                headerIndex;
    private HeaderMatchMode    matchMode;

    protected static final int PART_MAIN_MESSAGE = -1;

    public HeaderRule( String headerName,
                       String expectedValue,
                       HeaderMatchMode matchMode,
                       String ruleName,
                       boolean expectedResult ) {

        this( new byte[0],
              headerName,
              expectedValue,
              PART_MAIN_MESSAGE,
              -1,
              matchMode,
              ruleName,
              expectedResult );
    }

    public HeaderRule( byte[] nestedPackagePath,
                       String headerName,
                       String expectedValue,
                       HeaderMatchMode matchMode,
                       String ruleName,
                       boolean expectedResult ) {

        this( nestedPackagePath,
              headerName,
              expectedValue,
              PART_MAIN_MESSAGE,
              -1,
              matchMode,
              ruleName,
              expectedResult );
    }

    public HeaderRule( String headerName,
                       String expectedValue,
                       int headerIndex,
                       HeaderMatchMode matchMode,
                       String ruleName,
                       boolean expectedResult ) {

        this( new byte[0],
              headerName,
              expectedValue,
              PART_MAIN_MESSAGE,
              headerIndex,
              matchMode,
              ruleName,
              expectedResult );
    }

    public HeaderRule( String headerName,
                       String expectedValue,
                       int partIndex,
                       int headerIndex,
                       HeaderMatchMode matchMode,
                       String ruleName,
                       boolean expectedResult ) {

        this( new byte[0],
              headerName,
              expectedValue,
              partIndex,
              headerIndex,
              matchMode,
              ruleName,
              expectedResult );
    }

    public HeaderRule( byte[] nestedPackagePath,
                       String headerName,
                       String expectedValue,
                       int partIndex,
                       int headerIndex,
                       HeaderMatchMode matchMode,
                       String ruleName,
                       boolean expectedResult ) {

        super( ruleName, expectedResult, ImapMetaData.class );

        this.headerName = headerName;
        this.expectedValue = expectedValue;
        this.partIndex = partIndex;
        this.headerIndex = headerIndex;
        this.matchMode = matchMode;

        setNestedPackagePath( nestedPackagePath );
    }

    public HeaderRule( String headerName,
                       String expectedValue,
                       HeaderMatchMode matchMode,
                       String ruleName,
                       boolean expectedResult,
                       int priority ) {

        this( new byte[0], headerName, expectedValue, matchMode, ruleName, expectedResult, priority );
    }

    public HeaderRule( byte[] nestedPackagePath,
                       String headerName,
                       String expectedValue,
                       HeaderMatchMode matchMode,
                       String ruleName,
                       boolean expectedResult,
                       int priority ) {

        super( ruleName, expectedResult, ImapMetaData.class, priority );

        this.headerName = headerName;
        this.expectedValue = expectedValue;
        this.partIndex = PART_MAIN_MESSAGE;
        this.headerIndex = -1;
        this.matchMode = matchMode;

        setNestedPackagePath( nestedPackagePath );
    }

    public void setExpectedValue(
                                  String expectedValue ) {

        this.expectedValue = expectedValue;
    }

    @Override
    protected boolean performMatch(
                                    MetaData metaData ) throws RbvException {

        //get the emailMessage
        //the meta data type check already passed, so it is safe to cast
        MimePackage emailMessage = getNeededMimePackage( metaData );

        String[] headerValues;
        try {
            if( headerIndex == -1 ) {
                // we are going to check all header values
                if( partIndex == PART_MAIN_MESSAGE ) {
                    headerValues = emailMessage.getHeaderValues( headerName );
                } else {
                    headerValues = emailMessage.getPartHeaderValues( headerName, partIndex );
                }
            } else {
                // we are going to check a particular header value
                String headerValue;
                if( partIndex == PART_MAIN_MESSAGE ) {
                    headerValue = emailMessage.getHeader( headerName, headerIndex );
                } else {
                    headerValue = emailMessage.getPartHeader( headerName, partIndex, headerIndex );
                }
                headerValues = new String[]{ headerValue };
            }
        } catch( NoSuchHeaderException nshe ) {
            log.debug( "Meta data has no header '" + headerName + "'" );

            //no such header, so return false
            return false;
        } catch( PackageException pe ) {
            throw new RbvException( pe );
        }

        //if there is no such header return false
        boolean actualResult = false;
        if( headerValues == null || headerValues.length == 0 ) {
            log.info( "No header '" + headerName + "' was found" );
        } else {
            for( String headerValue : headerValues ) {
                switch( matchMode ){
                    case LEFT:
                        actualResult = headerValue.startsWith( expectedValue );
                        break;
                    case RIGHT:
                        actualResult = headerValue.endsWith( expectedValue );
                        break;
                    case EQUALS:
                        actualResult = headerValue.equals( expectedValue );
                        break;
                    case FIND:
                        actualResult = headerValue.indexOf( expectedValue ) >= 0;
                        break;
                    case REGEX:
                        actualResult = Pattern.compile( expectedValue ).matcher( headerValue ).find();
                        break;
                }
                log.info( "Actual value for header '" + headerName + "' is '" + headerValue + "'" );

                if( actualResult ) {
                    // we matched a header value, stop iterating the rest of the values
                    break;
                }
            }
        }
        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects header value '" + expectedValue + "' in header '" + headerName
               + "'. Match operation is: " + matchMode;
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add( ImapMetaData.MIME_PACKAGE );
        return metaKeys;
    }
}
