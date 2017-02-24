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
package com.axway.ats.rbv.filesystem.rules;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.rules.AbstractRule;
import com.axway.ats.rbv.rules.Rule;

/**
 *  This {@link Rule} help identify files from folders
 */
public class FileFolderRule extends AbstractRule {

    private boolean isFileExpected;

    /**
     * Constructor
     * 
     * @param isFile true for file, false for folder
     * @param ruleName the name of the {@link Rule}
     * @param expectedResult the expected result
     */
    public FileFolderRule( boolean isFile,
                           String ruleName,
                           boolean expectedResult ) {

        super( ruleName, expectedResult, FileSystemMetaData.class );

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
    public FileFolderRule( boolean isFile,
                           String ruleName,
                           boolean expectedResult,
                           int priority ) {

        super( ruleName, expectedResult, FileSystemMetaData.class, priority );

        //initialize members
        this.isFileExpected = isFile;
    }

    @Override
    public boolean performMatch(
                                 MetaData metaData ) throws RbvException {

        if( metaData instanceof FileSystemMetaData ) {
            // get the file from the meta data
            FilePackage file = ( ( FileSystemMetaData ) metaData ).getFilePackage();

            try {
                // check if the entity is a file           
                if( file.isFile() == this.isFileExpected ) {
                    return true;
                }

            } catch( PackageException pe ) {
                throw new RbvStorageException( pe );
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

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add( FileSystemMetaData.FILE_PACKAGE );
        return metaKeys;
    }
}
