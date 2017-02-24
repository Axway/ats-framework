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
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.rules.AbstractRule;

public class FileMd5Rule extends AbstractRule {

    protected String  srcMD5;
    protected boolean binaryMode;

    public FileMd5Rule( String md5sum,
                        String ruleName,
                        boolean expectedResult ) {

        super( ruleName, expectedResult, FileSystemMetaData.class );

        this.srcMD5 = md5sum;
        this.binaryMode = true;
    }

    /**
     * Match with the MD5 sum of the specified file.
     *
     * @param atsAgent the address of the remote ATS agent
     * @param file the file for comparison
     * @param binaryMode <code>true</code> for binary mode or <code>false</code> for ASCII mode
     * @param ruleName the rule name
     * @param expectedResult the expected result
     * @throws RbvException
     */
    public FileMd5Rule( String atsAgent,
                        String file,
                        boolean binaryMode,
                        String ruleName,
                        boolean expectedResult ) throws RbvException {

        super( ruleName, expectedResult, FileSystemMetaData.class );

        try {
            FilePackage filePackage = new FilePackage( atsAgent, file );

            this.srcMD5 = filePackage.getMd5sum( binaryMode );
            this.binaryMode = binaryMode;

        } catch( PackageException pe ) {
            throw new RbvStorageException( pe );
        }
    }

    /**
     * Match with the MD5 sum of the specified file.
     *
     * @param atsAgent the address of the remote ATS agent
     * @param file the file for comparison
     * @param ruleName the rule name
     * @param expectedResult the expected result
     * @throws RbvException
     */
    public FileMd5Rule( String atsAgent,
                        String file,
                        String ruleName,
                        boolean expectedResult ) throws RbvException {

        super( ruleName, expectedResult, FileSystemMetaData.class );

        try {
            FilePackage filePackage = new FilePackage( atsAgent, file );

            this.srcMD5 = filePackage.getMd5sum();
            this.binaryMode = true;

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
                String destMD5 = file.getMd5sum( this.binaryMode );
                actualResult = !StringUtils.isNullOrEmpty( destMD5 ) && destMD5.equals( this.srcMD5 );
            } catch( PackageException pe ) {
                throw new RbvStorageException( pe );
            }
        }

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {
        return "which expects file with MD5 sum " + ( getExpectedResult()
                                                                         ? ""
                                                                         : "different than " ) + "'"
               + this.srcMD5 + "'";
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add( FileSystemMetaData.FILE_PACKAGE );
        return metaKeys;
    }
}
