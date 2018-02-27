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
package com.axway.ats.rbv.s3.rules;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.Rule;
import com.axway.ats.rbv.s3.S3MetaData;

/**
 *  This {@link Rule} help identify files from folders
 */
public class FileFolderS3Rule extends AbstractS3Rule {

    private boolean isFileExpected;

    /**
     * Constructor
     * 
     * @param isFile true for file, false for folder
     * @param ruleName the name of the {@link Rule}
     * @param expectedResult the expected result
     */
    public FileFolderS3Rule( boolean isFile, String ruleName, boolean expectedResult ) {

        super( ruleName, expectedResult, MetaData.class );

        //initialize members
        this.isFileExpected = isFile;
    }

    /**
     * Constructor, allowing a priority to be set for this {@link Rule}
     * 
     * @param isFile true for file, false for folder
     * @param ruleName the name of the {@link Rule}
     * @param expectedResult the expected result
     * @param priority the priority of this {@link Rule}
     */
    public FileFolderS3Rule( boolean isFile, String ruleName, boolean expectedResult, int priority ) {

        super( ruleName, expectedResult, MetaData.class, priority );

        //initialize members
        this.isFileExpected = isFile;
    }

    @Override
    public boolean performMatch( MetaData metaData ) throws RbvException {

        if( metaData instanceof MetaData ) {
            // get the file from the meta data
            Object file = metaData.getProperty( S3MetaData.FILE_NAME );

            if( file != null && !StringUtils.isNullOrEmpty( ( String ) file ) 
                    && this.isFileExpected ) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected String getRuleDescription() {

        return new StringBuilder().append( "which expects a '" )
                                  .append( this.isFileExpected
                                                               ? "file"
                                                               : "folder" )
                                  .append( "'" )
                                  .toString();
    }
}
