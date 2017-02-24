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

import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.exceptions.TypeException;

/**
 * The {@link TypeFile} class introduces the functionality
 * to work with instances of the {@link File} class. It checks
 * weather or not the file is present and available.
 *
 * Created on : Oct 11, 2007
 */
public class TypeFile extends TypeObject {

    private static final String ERROR_MESSAGE_CAST_EXCEPTION = "Argument is not a valid file object. ";
    private static final String ERROR_MESSAGE_INVALID_FILE   = "The file object point to a non existing file or directory. ";
    private static final String ERROR_VALIDATING_FILE        = "Unable to validate file object. ";

    /** Constructor */
    protected TypeFile( Object val ) {

        super( val );
    }

    /** Constructor */
    protected TypeFile( String paramName,
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
            throw new TypeException( ERROR_VALIDATING_FILE + e.getMessage(), this.parameterName, e );
        }

        File file = null;

        try {
            file = ( File ) this.value;
        } catch( ClassCastException e ) {
            throw new TypeException( ERROR_MESSAGE_CAST_EXCEPTION, this.parameterName, e );
        }

        if( file != null && file.isFile() ) {
            return;
        }

        throw new TypeException( ERROR_MESSAGE_INVALID_FILE, this.parameterName );
    }
}
