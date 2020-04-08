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

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.azure.BlobStorageMetaData;
import com.axway.ats.rbv.model.RbvException;

public class BlobSizeBlobStorageRule extends AbstractBlobStorageRule {

    private long srcSize;
    private long destSize;

    public BlobSizeBlobStorageRule( long size, String ruleName, boolean expectedResult ) {

        super(ruleName, expectedResult, MetaData.class);

        this.srcSize = size;
    }

    @Override
    protected boolean performMatch( MetaData metaData ) throws RbvException {

        boolean actuaResult = false;

        //get the file from the meta data
        Object size = metaData.getProperty(BlobStorageMetaData.SIZE);

        if (size == null) {
            return false;
        }

        destSize = (Long) size;

        actuaResult = this.srcSize == destSize;
        return actuaResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects blob with size " + (getExpectedResult()
                                                                      ? ""
                                                                      : "different than ")
               + "'" + this.srcSize + "' and current actual size is '" + this.destSize + "'";
    }

}
