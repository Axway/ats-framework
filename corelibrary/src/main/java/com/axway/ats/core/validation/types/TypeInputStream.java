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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.exceptions.TypeException;

/**
 * The {@link TypeInputStream} class introduces the functionality
 * to work with instances of the {@link File} class. It checks
 * wether or not the file is present and available.
 *
 * Created on : Oct 11, 2007
 */
public class TypeInputStream extends TypeObject {

    private static final String ERROR_MESSAGE_CAST_EXCEPTION  = "Argument is not a valid InputStream object. ";
    private static final String ERROR_MESSAGE_UNABLE_TO_READ  = "Argument is validated as a valid InputStream object, but there was an I/O error while reading from it. ";
    private static final String ERROR_MESSAGE_INVALID_STREAM  = "Argument is validated as a valid InputStream object, but it is either null or empty. ";
    private static final String ERROR_VALIDATING_INPUT_STREAM = "Unable to validate this parameter as an Input Stream. ";

    /** Constructor */
    protected TypeInputStream( String paramName,
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
            throw new TypeException( ERROR_VALIDATING_INPUT_STREAM + e.getMessage(), this.parameterName, e );
        }

        InputStream stream = null;

        try {
            stream = ( InputStream ) this.value;
        } catch( ClassCastException e ) {
            throw new TypeException( ERROR_MESSAGE_CAST_EXCEPTION, this.parameterName, e );
        }

        try {
            if( stream != null && stream.available() > 0 ) {
                return;
            }
        } catch( IOException e ) {
            throw new TypeException( ERROR_MESSAGE_UNABLE_TO_READ, this.parameterName, e );
        }

        throw new TypeException( ERROR_MESSAGE_INVALID_STREAM, this.parameterName );
    }
}
