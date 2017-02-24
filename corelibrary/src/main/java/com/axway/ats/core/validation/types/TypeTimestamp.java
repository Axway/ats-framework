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
 * The {@link TypeTimestamp} class introduces the functionality
 * to check UNIX-based timestamps. Currently checks if the timestamp
 * is a valid full day or full hour.
 *
 * Created on : Oct 11, 2007
 */
public class TypeTimestamp extends TypeNumber {

    private static final String SPECIFIC_ERROR_MESSAGE_HOUR = "Argument is a wrong hour timestimp. ";
    private static final String SPECIFIC_ERROR_MESSAGE_DAY  = "Argument is a wrong day timestimp. ";

    private static final int    SECONDS_IN_ONE_HOUR         = 60 * 60;
    private static final int    SECONDS_IN_ONE_DAY          = SECONDS_IN_ONE_HOUR * 24;
    private static final String ERROR_VALIDATING_TIMESTAMP  = "Unable to validate this parameter as a timestamp. ";

    protected boolean           isDay                       = false;

    /** Constructor */
    protected TypeTimestamp( String paramName,
                             Object val,
                             Object[] args ) {

        super( paramName, val, args );
    }

    /** Specific Constructor */
    protected TypeTimestamp( String paramName,
                             Object val,
                             Object[] args,
                             boolean day ) {

        super( paramName, val, args );
        this.isDay = day;
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
            throw new TypeException( ERROR_VALIDATING_TIMESTAMP + e.getMessage(), e.getParameterName(), e );
        }

        if( this.validatedValue % SECONDS_IN_ONE_HOUR != 0 ) {
            throw new TypeException( SPECIFIC_ERROR_MESSAGE_HOUR, this.parameterName );
        }

        if( this.isDay && this.validatedValue % SECONDS_IN_ONE_DAY != 0 ) {
            throw new TypeException( SPECIFIC_ERROR_MESSAGE_DAY, this.parameterName );
        }
    }
}
