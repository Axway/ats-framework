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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.exceptions.TypeException;

/**
 * The {@link TypeEmailAddress} class introduces the validation 
 * functionality for validating email addresses.
 * 
 * TODO : rewrite implementation so that email addresses are
 * validated with the help of regular expressions?
 * 
 * Created on : Oct 11, 2007
 */
public class TypeEmailAddress extends TypeString {
    private static final String ERROR_MESSAGE_INVALID_EMAIL = "Argument is not a valid EMAIL address. ";

    /** Constructor */
    protected TypeEmailAddress( String paramName,
                                Object val,
                                Object[] args ) {

        super( paramName, val, args );
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
            throw new TypeException( ERROR_MESSAGE_INVALID_EMAIL + e.getMessage(), this.parameterName, e );
        }

        try {
            String email = ( String ) this.value;
            // Note that the JavaMail's implementation of email address validation is
            // somewhat limited. The Javadoc says "The current implementation checks many,
            // but not all, syntax rules.". For example, the address a@ is correctly
            // flagged as invalid, but the address "a"@ is considered
            // valid by JavaMail, even though it is not valid according to RFC 822.
            new InternetAddress( email, true );
        } catch( AddressException ae ) {
            throw new TypeException( ERROR_MESSAGE_INVALID_EMAIL, this.parameterName, ae );
        }
    }
}
