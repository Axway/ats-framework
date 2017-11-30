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
 * The {@link TypeObject} class is also a base class for any validation
 * type object. It introduces the NOT_NULL validation functionality.
 *
 * Created on : Oct 11, 2007
 */
public class TypeObject extends BaseType {

    private static final String ERROR_MESSAGE_NULL = "Null value is not a valid value for this argument. ";

    /** Constructor */
    protected TypeObject( Object val ) {

        super(val);
    }

    /** Constructor */
    protected TypeObject( String paramName,
                          Object val,
                          Object[] args ) {

        super(paramName, val, args);
    }

    /**
     * Performs a type-specific validation for the given
     * {@link ValidationType}
     *
     */
    @Override
    public void validate() throws TypeException {

        if (this.value == null) {
            throw new TypeException(ERROR_MESSAGE_NULL, this.parameterName);
        }
    }
}
