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

public class FilePermRule extends AbstractRule {

    protected long sourcePermissions;

    /**
     * Match with the specified permissions
     *
     * @param log            The logging object
     * @param sName            The name of the matcher - used for logging
     * @param bExpected     Matcher's expected result
     * @param perm             The permissions with which to compare
     */

    public FilePermRule( long permissions,
                         String ruleName,
                         boolean expectedResult ) {

        super( ruleName, expectedResult, FileSystemMetaData.class );
        this.sourcePermissions = permissions;
    }

    /**
     * Match with the permissions of the specified file.
     *
     * @param atsAgent the address of the remote ATS agent
     * @param filePath the path name of the file for comparison
     * @param ruleName the rule name
     * @param expectedResult the expected result
     * @throws RbvException
     */
    public FilePermRule( String atsAgent,
                         String filePath,
                         String ruleName,
                         boolean expectedResult ) throws RbvException {

        super( ruleName, expectedResult, FileSystemMetaData.class );

        try {
            //get source file's permissions
            FilePackage file = new FilePackage( atsAgent, filePath );
            this.sourcePermissions = file.getPermissions();

        } catch( PackageException pe ) {
            throw new RbvStorageException( pe );
        }
    }

    @Override
    public boolean performMatch(
                                 MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        if( metaData instanceof FileSystemMetaData ) {
            //get the file from the meta data
            FilePackage file = ( ( FileSystemMetaData ) metaData ).getFilePackage();

            try {
                long destPerm = file.getPermissions();

                if( this.sourcePermissions == FilePackage.ATTRIBUTE_NOT_SUPPORTED
                    && destPerm == FilePackage.ATTRIBUTE_NOT_SUPPORTED ) {
                    actualResult = true;
                } else {
                    actualResult = destPerm == this.sourcePermissions;
                }
            } catch( PackageException pe ) {
                throw new RbvStorageException( pe );
            }
        }
        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects file with permissions " + ( getExpectedResult()
                                                                             ? ""
                                                                             : "different than " ) + "'"
               + this.sourcePermissions + "'";
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add( FileSystemMetaData.FILE_PACKAGE );
        return metaKeys;
    }
}
