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
 * The {@link TypeString} class introduces the NOT_EMPTY validation
 * functionality. It also makes sure the value is of {@link String} type
 * so that any subtype of this can cast to {@link String} safely.
 *
 * Created on : Oct 11, 2007
 */
public class TypeString extends TypeObject {

    private static final String ERROR_MESSAGE_EMPTY         = "Empty string is not a valid value for this argument. ";
    private static final String ERROR_MESSAGE_CAST_EXCPTION = "This arguments needs to be a string. ";
    private static final String ERROR_VALIDATING_STRING     = "Unable to validate this string. ";

    protected String            validatedValue              = null;

    /** Constructor */
    protected TypeString( String paramName,
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
            throw new TypeException(ERROR_VALIDATING_STRING + e.getMessage(), e.getParameterName(), e);
        }

        // check both if the string is empty and if this
        // is a string at all
        try {
            this.validatedValue = (String) this.value;
            if (! (this.validatedValue.length() > 0)) {
                throw new TypeException(ERROR_MESSAGE_EMPTY, this.parameterName);
            }
        } catch (ClassCastException e) {
            throw new TypeException(ERROR_MESSAGE_CAST_EXCPTION, this.parameterName);
        }
    }
}
