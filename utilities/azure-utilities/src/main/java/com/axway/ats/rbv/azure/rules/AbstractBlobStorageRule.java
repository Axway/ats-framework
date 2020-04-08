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

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.azure.BlobStorageMetaData;
import com.axway.ats.rbv.rules.AbstractRule;

public abstract class AbstractBlobStorageRule extends AbstractRule {

    public AbstractBlobStorageRule( String ruleName,
                                         boolean expectedResult ) {

        super(ruleName, expectedResult);
    }

    public AbstractBlobStorageRule( String ruleName,
                                         boolean expectedResult,
                                         Class<? extends MetaData> metaDataClass ) {

        super(ruleName, expectedResult, metaDataClass);

    }

    public AbstractBlobStorageRule( String ruleName,
                                         boolean expectedResult,
                                         Class<? extends MetaData> metaDataClass,
                                         int priority ) {

        super(ruleName, expectedResult, metaDataClass, priority);

    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(BlobStorageMetaData.CONTAINER_NAME);
        metaKeys.add(BlobStorageMetaData.MD5);
        metaKeys.add(BlobStorageMetaData.BLOB_NAME);
        metaKeys.add(BlobStorageMetaData.LAST_MODIFIED);
        metaKeys.add(BlobStorageMetaData.SIZE);

        return metaKeys;
    }

}
