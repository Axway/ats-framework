/*
 * Copyright 2020 Axway Software
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

package com.axway.ats.rbv.azure.rules;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.azure.BlobStorageMetaData;
import com.axway.ats.rbv.model.RbvException;

public class BlobFolderRule extends AbstractBlobStorageRule {

    public static final String CHECK_IS_CONTAINER_RULE_NAME = "checkIsContainer";
    public static final String CHECK_IS_BLOB_RULE_NAME      = "checkIsBlob";
    public static final String CHECK_IS_FOLDER_RULE_NAME    = "checkIsFolder";

    private boolean            isFileExpected;

    public BlobFolderRule( boolean isFile, String ruleName, boolean expectedResult ) {

        super(ruleName, expectedResult, MetaData.class);

        //initialize members
        this.isFileExpected = isFile;
    }

    public BlobFolderRule( boolean isFile, String ruleName, boolean expectedResult, int priority ) {

        super(ruleName, expectedResult, MetaData.class, priority);

        //initialize members
        this.isFileExpected = isFile;
    }

    @Override
    protected boolean performMatch( MetaData metaData ) throws RbvException {

        if (metaData instanceof MetaData) {

            // get the file from the meta data
            Object file = null;
            if (getRuleName().equals(CHECK_IS_CONTAINER_RULE_NAME)) {
                file = metaData.getProperty(BlobStorageMetaData.CONTAINER_NAME);
                return !StringUtils.isNullOrEmpty((String) file);
            } else if (getRuleName().equals(CHECK_IS_BLOB_RULE_NAME)) {
                file = metaData.getProperty(BlobStorageMetaData.BLOB_NAME);
                if (file != null && !StringUtils.isNullOrEmpty((String) file)
                    && this.isFileExpected) {
                    return true;
                }
            } else if (getRuleName().equals(CHECK_IS_FOLDER_RULE_NAME)) {
                throw new RuntimeException("Not implemented " + this.getRuleName());
            }

        }

        return false;
    }

    @Override
    protected String getRuleDescription() {

        return new StringBuilder().append("which expects a '")
                                  .append(this.isFileExpected
                                                              ? "blob"
                                                              : (this.getRuleName()
                                                                     .equals(CHECK_IS_CONTAINER_RULE_NAME))
                                                                                                            ? "container"
                                                                                                            : "folder")
                                  .append("'")
                                  .toString();
    }

}
