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

@SuppressWarnings( "boxing")
public class FileGroupNameRule extends AbstractRule {

    private String group;

    public FileGroupNameRule( String group,
                              String ruleName,
                              boolean expectedResult ) {

        super(ruleName, expectedResult, FileSystemMetaData.class);

        //init members
        this.group = group;
    }

    public FileGroupNameRule( String atsAgent,
                              String filePath,
                              String group,
                              boolean expectedResult ) throws RbvException {

        super(group, expectedResult, FileSystemMetaData.class);

        // get source file's group
        try {
            FilePackage file = new FilePackage(atsAgent, filePath);
            this.group = file.getGroupName();
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
                //get destination file's group
                String destGroup = file.getGroupName();

                // if either of the group values (expected and actual) is the NOT_SUPPORTED
                // value then we are not able to verify them and we should return true
                if (this.group == null || destGroup == null) {
                    actualResult = true;
                } else {
                    actualResult = destGroup.equals(this.group);
                }
            } catch (PackageException pe) {
                throw new RbvStorageException(pe);
            }
        }

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects file with Group Name " + (getExpectedResult()
                                                                            ? ""
                                                                            : "different than ")
               + "'"
               + this.group + "'";
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(FileSystemMetaData.FILE_PACKAGE);
        return metaKeys;
    }
}
