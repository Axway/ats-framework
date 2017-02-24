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

import com.axway.ats.core.validation.exceptions.TypeException;

/**
 * This is the base class for each validation type. It contains the
 * methods and fields common for all the validation types. All validation
 * types must implement a {@link BaseType#validate()} method
 * and also contain a {@link String} name  and an {@link Object} value of
 * the parameter name that is validated.
 *
 * Created on : Oct 10, 2007
 */
public abstract class BaseType {
    protected String   parameterName;
    protected Object   value;
    protected Object[] arguments;

    /**
     * Method validation constructor
     * @param paramName the {@link String} name of the parameter to validate
     * @param val the {@link Object} value of the parameter
     * @param args the array of {@link Object}s representing the validation arguments
     */
    protected BaseType( String paramName, Object val, Object[] args ) {

        this.parameterName = paramName;
        this.value = val;
        this.arguments = args;
    }

    /**
     * Validation constructor
     * @param val the {@link Object} value of the parameter
     */
    protected BaseType( Object val ) {

        this.value = val;
    }

    /**
     * Type specific validation of the values passed. Performs the specific
     * checks and throws a checked {@link Exception} with a specific
     * error message in case the validation has failed
    
     * @throws TypeException if the validation has failed in any way
     */
    public abstract void validate() throws TypeException;

}
