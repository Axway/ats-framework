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
 * The {@link TypeStringFilename} class introduces the NOT_EMPTY validation
 * functionality. It also makes sure the value is of {@link String} type
 * so that any subtype of this can cast to {@link String} safely.
 *
 * Created on : Oct 11, 2007
 */
public class TypeStringFilename extends TypeString {

    private static final String ERROR_VALIDATING_FILENAME = "Unable to validate this string, containing a filename. ";

    /** Constructor */
    protected TypeStringFilename( String paramName,
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

        // --- proper string string validation ---
        try {
            super.validate();

        } catch (TypeException e) {
            throw new TypeException(ERROR_VALIDATING_FILENAME + e.getMessage(), e.getParameterName(), e);
        }

        // --- proper file name validation
        String filename = (String) this.value;
        File file = new File(filename);
        BaseType typeFile = new TypeFile(file);

        try {
            typeFile.validate();
        } catch (TypeException e) {
            throw new TypeException(ERROR_VALIDATING_FILENAME + e.getMessage(), e.getParameterName());
        }
    }
}
