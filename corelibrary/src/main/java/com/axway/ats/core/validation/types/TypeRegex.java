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
package com.axway.ats.core.validation.types;

import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.exceptions.TypeException;

/**
 * The {@link TypeRegex} class introduces the possibility to validate
 * certain regular expressions. An example of using regular expressions
 * is the validation of IP addresses.
 *
 * Created on : Oct 11, 2007
 */
public class TypeRegex extends TypeString {

    private static final String ERROR_MESSAGE_CHECK_TYPE_NOT_SET          = "Argument is validated with invalid call to the regex validator - need to specify a type check. ";
    private static final String ERROR_MESSAGE_INVALID_DATE                = "Argument is not a valid date string of the type DD/MM/YYYY. ";
    private static final String ERROR_MESSAGE_INVALID_IP                  = "Argument is not a valid IP address. ";
    private static final String ERROR_MESSAGE_CONTAINS_WHITESPACE         = "Argument contains whitespace characters. ";
    private static final String ERROR_MESSAGE_CONTAINS_DQ_WHITESPACE      = "Argument contains double quoted whitespace characters. ";
    private static final String ERROR_MESSAGE_INVALID_HOSTNAME            = "Argument is an invalid hostname. ";
    private static final String ERROR_MESSAGE_INVALID_DOMAIN_OR_SUBDOMAIN = "Argument is an invalid domain name. ";
    private static final String ERROR_MESSAGE_INVALID_DOMAIN              = "Argument is an invalid domain name or subdomain. ";
    private static final String ERROR_VALIDATING_REGEX                    = "Unable to verify this string. ";

    // String constant for IPv4 validity check expression
    private static final String IPv4_EXPRESSION                           = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

    // String constant for hostname validity check expression
    private static final String HOSTNAME_EXPRESSION                       = "^[\\p{L}]+[\\p{L}\\d\\_\\.\\-]*([^\\P{L}]+|[\\d]+)$";

    // String constant for domain name validity check expression
    private static final String DOMAIN_ONLY_EXPRESSION                    = "^[a-zA-Z]+([a-zA-Z0-9_\\-]*?\\.)+[a-zA-Z]{2,8}$";

    // String constant for domains and sub-domains validity check expression
    private static final String DOMAIN_OR_SUBDOMAIN_EXPRESSION            = "\\.?([a-zA-Z0-9_\\-]+?\\.)+[a-zA-Z]{2,3}$";

    /**
     * Regular expression pattern string to validate dates in the format "d/m/y" from 1/1/1600 - 31/12/9999.
     * Checks for valid days for a given month and the days are validated for the given month and year.
     * Validates Leap years for all 4 digits years from 1600-9999.
     */
    private static final String DATE_PATTERN                              = "^(?:(31)(\\D)(0?[13578]|1[02])\\2|(29|30)(\\D)(0?[13-9]|1[0-2])\\5|(0?[1-9]|1\\d|2[0-8])(\\D)(0?[1-9]|1[0-2])\\8)((?:1[6-9]|[2-9]\\d)?\\d{2})$|^(29)(\\D)(0?2)\\12((?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:16|[2468][048]|[3579][26])00)$";

    /** Regular expression pattern string to validate if a string contains whitespace or tab characters */
    private static final String WHITESPACE_PATTERN                        = "[^ \t\n]+|^";
    private static final String DOUBLE_QUOTED_WHITESPACE_PATTERN          = "\"[^\"]+\"";

    protected RegexType         typeOfCheck                               = null;

    protected enum RegexType {
        DATE, DOUBLE_QUOTED_WHITESPACE, WHITESPACE, IPv4_ADDRESS, HOSTNAME, DOMAIN, DOMAIN_OR_SUBDOMAIN;
    }

    /** Constructor */
    protected TypeRegex( String paramName, Object val, Object[] args, RegexType check ) {

        super( paramName, val, args );
        this.typeOfCheck = check;
    }

    /**
     * Performs a type-specific validation for the given
     * {@link ValidationType}
     */
    @Override
    public void validate() throws TypeException {

        try {
            super.validate();

        } catch( TypeException e ) {
            throw new TypeException( ERROR_VALIDATING_REGEX + e.getMessage(), e.getParameterName(), e );
        }

        switch( this.typeOfCheck ){
            case DATE:
                if( !this.validatedValue.matches( DATE_PATTERN ) ) {
                    throw new TypeException( ERROR_MESSAGE_INVALID_DATE, this.parameterName );
                }
                return;
            case IPv4_ADDRESS:
                if( !this.validatedValue.matches( IPv4_EXPRESSION ) ) {
                    throw new TypeException( ERROR_MESSAGE_INVALID_IP, this.parameterName );
                }
                return;
            case WHITESPACE:
                if( !this.validatedValue.matches( WHITESPACE_PATTERN ) ) {
                    throw new TypeException( ERROR_MESSAGE_CONTAINS_WHITESPACE, this.parameterName );
                }
                return;
            case DOUBLE_QUOTED_WHITESPACE:
                if( !this.validatedValue.matches( WHITESPACE_PATTERN )
                    && !this.validatedValue.matches( DOUBLE_QUOTED_WHITESPACE_PATTERN ) ) {
                    throw new TypeException( ERROR_MESSAGE_CONTAINS_DQ_WHITESPACE, this.parameterName );
                }
                return;
            case DOMAIN:
                if( !this.validatedValue.matches( DOMAIN_ONLY_EXPRESSION ) ) {
                    throw new TypeException( ERROR_MESSAGE_INVALID_DOMAIN, this.parameterName );
                }
                return;
            case DOMAIN_OR_SUBDOMAIN:
                if( !this.validatedValue.matches( DOMAIN_OR_SUBDOMAIN_EXPRESSION ) ) {
                    throw new TypeException( ERROR_MESSAGE_INVALID_DOMAIN_OR_SUBDOMAIN, this.parameterName );
                }
                return;
            case HOSTNAME:
                if( !this.validatedValue.matches( HOSTNAME_EXPRESSION ) ) {
                    throw new TypeException( ERROR_MESSAGE_INVALID_HOSTNAME, this.parameterName );
                }
                return;
            default:
                throw new TypeException( ERROR_MESSAGE_CHECK_TYPE_NOT_SET, this.parameterName );
        }
    }
}
