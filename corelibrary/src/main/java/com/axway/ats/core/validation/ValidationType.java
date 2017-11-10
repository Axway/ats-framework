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
package com.axway.ats.core.validation;

import java.io.File;
import java.io.InputStream;

/**
 * Contains an enumeration of all the types of parameters
 * that the validation framework can currently validate
 * 
 * Created on : Oct 10, 2007
 */
public enum ValidationType {

    /** None */
    NONE,

    /** Non-null {@link Object} */
    NOT_NULL,

    /** Non-empty {@link String}, ex. "" */
    STRING_NOT_EMPTY,

    /** {@link String} with no white spaces in it, ex. "a a"*/
    STRING_NO_WHITESPACES,

    /** {@link String} with no double quoted white spaces, ex. " a "a" a" */
    STRING_NO_DOUBLEQUOTED_WHITESPACE,

    /** Path and / or filename of an exiting {@link File}*/
    STRING_EXISTING_FILE,

    /** 
     * {@link String} containing a valid date, ex. DD/MM/YYYY from 1/1/1600 to 31/12/9999<br>
     * The verification bears in mind leap years, specific numbers of days 
     * for each month and is able to verify any date within the period
     */
    STRING_DATE,

    /** 
     * {@link String} containing a valid email address, ex. "someone@example.com"<br>
     * <br>
     * Note that the JavaMail's implementation of email address validation is<br>
     * somewhat limited. The Javadoc says "The current implementation checks many,<br>
     * but not all, syntax rules.". For example, the address a@ is correctly<br>
     * flagged as invalid, but the address "a"@ is considered<br>
     * valid by JavaMail, even though it is not valid according to RFC 822.<br>
     */
    STRING_EMAIL_ADDRESS,

    /**
     * {@link String} containing a valid IP address, ex. "192.168.1.1"<br>
     * Uses a regular expression.
     */
    STRING_IP,

    /** {@link String} containing a valid hostname, ex. "" */
    STRING_HOST_NAME,

    /** {@link String} containing a valid CIDR */
    STRING_CIDR,

    /** {@link String} containing a valid server address */
    STRING_SERVER_ADDRESS,

    /** {@link String} containing a valid server address with port */
    STRING_SERVER_WITH_PORT,

    /** {@link String} containing a valid server address or CIDR */
    STRING_SERVER_OR_CIDR,

    /** {@link String} containing a valid domain name */
    STRING_DOMAIN_NAME,

    /** {@link String} containing a valid domain or subdomain name */
    STRING_DOMAIN_OR_SUBDOMAIN,

    /** 
     * {@link String} that needs to be one of the given constants */
    STRING_CONSTANT,

    /** 
     * {@link String} that needs to be one of the given constants,
     * ignoring the case they are in
     */
    STRING_CONSTANT_IGNORE_CASE,

    /** number containing a valid port number */
    NUMBER_PORT_NUMBER,

    /** number containing a valid unix timestamp for a full hour */
    NUMBER_TIMESTAMP_FULL_HOUR,

    /** number containing a valid unix timestamp for a full day */
    NUMBER_TIMESTAMP_FULL_DAY,

    /** number within a certain range */
    NUMBER_WITHIN_RANGE,

    /** number greater than e certain number */
    NUMBER_GREATER_THAN,

    /** positive (including 0) int number */
    NUMBER_POSITIVE,

    /** positive (excluding 0) int number */
    NUMBER_GREATER_THAN_ZERO,

    /** number is a constant */
    NUMBER_CONSTANT,

    /** an existing {@link File} */
    FILE_EXISTING,

    /** an {@link InputStream} that is open and contains data */
    INPUT_STREAM_NOT_EMPTY;
}
