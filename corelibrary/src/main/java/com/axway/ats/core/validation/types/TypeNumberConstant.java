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
 * The {@link TypeNumberConstant} class introduces the functionality
 * to check if a given integer value is within a certain
 * range defined by the arguments passed.
 *
 * Created on : Oct 11, 2007
 */
public class TypeNumberConstant extends TypeNumber {

    private static final String ERROR_MESSAGE_NOT_WITHIN_RANGE = "Argument is not within the list of constants. ";
    private static final String ERROR_VALIDATING_CONSTANT      = "Unable to verify this number. ";

    /** Constructor */
    protected TypeNumberConstant( String paramName,
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
            throw new TypeException( ERROR_VALIDATING_CONSTANT + e.getMessage(), e.getParameterName(), e );
        }

        for( int i = 0; i < this.arguments.length; i++ ) {
            Double current = extractNumber( this.arguments[i] );
            if( current == null ) {
                continue;
            }
            if( this.validatedValue == current.doubleValue() ) {
                return;
            }
        }

        throw new TypeException( ERROR_MESSAGE_NOT_WITHIN_RANGE, this.parameterName );
    }
}
