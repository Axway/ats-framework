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

public class FileOwnerNameRule extends AbstractRule {

    private String owner;

    public FileOwnerNameRule( String owner,
                              String ruleName,
                              boolean expectedResult ) {

        super(ruleName, expectedResult, FileSystemMetaData.class);

        //init members
        this.owner = owner;
    }

    public FileOwnerNameRule( String atsAgent,
                              String filePath,
                              String ruleName,
                              boolean expectedResult ) throws RbvException {

        super(ruleName, expectedResult, FileSystemMetaData.class);

        // get source file's owner name
        try {
            FilePackage file = new FilePackage(atsAgent, filePath);
            this.owner = file.getOwnerName();
        } catch (PackageException pe) {
            throw new RbvStorageException(pe);
        }
    }

    @Override
    public boolean performMatch(
                                 MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        if (metaData instanceof FileSystemMetaData) {
            //get the file from the meta data
            FilePackage file = ((FileSystemMetaData) metaData).getFilePackage();

            try {
                //get destination file's owner name
                String destOwner = file.getOwnerName();

                // if either of the owner name values (expected and actual) is the NOT_SUPPORTED
                // value then we are not able to verify them and we should return true
                if (this.owner == null || destOwner == null) {
                    actualResult = true;
                } else {
                    actualResult = destOwner.equals(this.owner);
                }
            } catch (PackageException pe) {
                throw new RbvStorageException(pe);
            }
        }

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects file with Owner Name " + (getExpectedResult()
                                                                            ? ""
                                                                            : "different than ")
               + "'"
               + this.owner + "'";
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(FileSystemMetaData.FILE_PACKAGE);
        return metaKeys;
    }
}
