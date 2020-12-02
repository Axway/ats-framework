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

public class BlobMd5BlobStorageRule extends AbstractBlobStorageRule {

    protected String expectedMD5;
    protected String currentMD5;

    public BlobMd5BlobStorageRule( String md5sum, String ruleName, boolean expectedResult ) {

        super(ruleName, expectedResult, MetaData.class);

        this.expectedMD5 = md5sum;
    }

    @Override
    protected boolean performMatch( MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        //get the file from the meta data
        currentMD5 = (String) metaData.getProperty(BlobStorageMetaData.MD5);
        if (currentMD5 == null) {
            return false;
        }

        if (!StringUtils.isNullOrEmpty(currentMD5)) {
            actualResult = currentMD5.equals(this.expectedMD5);
        }

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects blob with MD5 sum " + (getExpectedResult()
                                                                         ? ""
                                                                         : "different than ")
               + "'" + this.expectedMD5 + "' and actual MD5 is '" + this.currentMD5 + "'";
    }

}
