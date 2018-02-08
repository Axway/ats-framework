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

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.rules.AbstractRule;
import com.axway.ats.rbv.s3.S3MetaData;

public abstract class AbstractS3Rule extends AbstractRule {

    public AbstractS3Rule( String ruleName, boolean expectedResult ) {

        super( ruleName, expectedResult );
    }

    public AbstractS3Rule( String ruleName, boolean expectedResult,
                           Class<? extends MetaData> metaDataClass ) {

        super( ruleName, expectedResult, metaDataClass );
    }

    public AbstractS3Rule( String ruleName, boolean expectedResult, Class<? extends MetaData> metaDataClass,
                           int priority ) {

        super( ruleName, expectedResult, metaDataClass, priority );
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add( S3MetaData.BUCKET_NAME );
        metaKeys.add( S3MetaData.MD5 );
        metaKeys.add( S3MetaData.FILE_NAME );
        metaKeys.add( S3MetaData.LAST_MODIFIED );
        metaKeys.add( S3MetaData.SIZE );

        return metaKeys;
    }
}
