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
 * The {@link TypeRange} class introduces the functionality
 * to check if a given integer value is within a certain
 * range defined by the arguments passed.
 *
 * Created on : Oct 11, 2007
 */
public class TypeRange extends TypeNumber {

    private static final String ERROR_MESSAGE_WRONG_RANGE = "Argument has a wrong predefined range. ";
    private static final String ERROR_VALIDATING_RANGE    = "Unable to verify this number. ";

    private double              lowerLimit                = Double.MIN_VALUE;
    private double              upperLimit                = Double.MAX_VALUE;

    /** Constructor */
    protected TypeRange( String paramName,
                         Object val,
                         Object[] args ) {

        super(paramName, val, args);
    }

    /**
     * Performs a type-specific validation for the given
     * {@link ValidationType}
     */
    @Override
    public void validate() throws TypeException {

        try {
            super.validate();

        } catch (TypeException e) {
            throw new TypeException(ERROR_VALIDATING_RANGE + e.getMessage(), this.parameterName, e);
        }

        // check if arguments are in fact numbers
        Double min = extractNumber(this.arguments[0]);
        Double max = extractNumber(this.arguments[1]);

        if (min == null || max == null) {
            throw new TypeException(ERROR_MESSAGE_WRONG_RANGE, this.parameterName);
        }

        this.lowerLimit = min.doubleValue();
        this.upperLimit = max.doubleValue();

        if (this.upperLimit <= this.lowerLimit) {
            throw new TypeException(ERROR_MESSAGE_WRONG_RANGE, this.parameterName);
        }

        if (this.validatedValue <= this.upperLimit && this.validatedValue >= this.lowerLimit) {
            return;
        }

        throw new TypeException("Argument [" + this.validatedValue
                                + "] is not within the predefined range: " + "[" + this.lowerLimit + ","
                                + this.upperLimit + "]", this.parameterName);
    }
}
