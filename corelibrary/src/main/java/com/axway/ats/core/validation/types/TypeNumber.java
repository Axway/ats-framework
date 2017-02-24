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
 * The {@link TypeNumber} class introduces the functionality
 * to work with numbers, specifically makes sure that the
 * value is one of the following types:
 * <ul>
 * <li> {@link Integer} </li>
 * <li> {@link Long} </li>
 * <li> {@link Float} </li>
 * <li> {@link Double} </li>
 * </ul>
 *
 * Created on : Oct 11, 2007
 */
public class TypeNumber extends TypeObject {

    private static final String ERROR_MESSAGE_CAST_EXCEPTION = "Argument is not a valid number value (neither one of the int, long, float or double types). ";

    private static final String ERROR_VALIDATING_NUMBER      = "Unable to validate the parameter as number. ";

    protected double            validatedValue               = 0;

    /** Constructor */
    protected TypeNumber( String paramName,
                          Object val,
                          Object[] args ) {

        super( paramName, val, args );
    }

    /**
     * Performs a type-specific validation for the given
     * {@link ValidationType}
     *
     * @throws TypeException
     */
    @Override
    public void validate() throws TypeException {

        try {
            super.validate();

        } catch( TypeException e ) {
            throw new TypeException( ERROR_VALIDATING_NUMBER, this.parameterName, e );
        }

        // check if this is a number
        Double number = extractNumber( this.value );
        if( number == null ) {
            throw new TypeException( ERROR_MESSAGE_CAST_EXCEPTION, this.parameterName );
        }

        this.validatedValue = number.doubleValue();
    }

    /**
     * Tries to extracts the value as some sort of a number.
     *
     * @param val the value to extract
     * @return    the value as {@link Double}
     */
    protected Double extractNumber(
                                    Object val ) {

        double result = 0;

        try {
            result = Integer.parseInt( val.toString() );
        } catch( NumberFormatException ex ) {
            // Do not handle - still could be some other
            // sort of a number
        }
        try {
            result = Float.parseFloat( val.toString() );
        } catch( NumberFormatException ex ) {
            // Do not handle - still could be some other
            // sort of a number
        }
        try {
            result = Long.parseLong( val.toString() );
        } catch( NumberFormatException ex ) {
            // Do not handle - still could be some other
            // sort of a number
        }
        try {
            result = Double.parseDouble( val.toString() );
        } catch( NumberFormatException ex ) {
            return null;
        }

        return new Double( result );
    }
}
