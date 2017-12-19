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
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.AbstractRule;
import com.axway.ats.rbv.s3.S3MetaData;

public class FileSizeS3Rule extends AbstractRule {

    private long srcSize;

    /**
     * Match with the specified size
     *
     * @param sName            The name of the matcher - used for logging
     * @param bExpected     Matcher's expected result
     * @param size             The size with which to compare
     */
    public FileSizeS3Rule( long size, String ruleName, boolean expectedResult ) {

        super( ruleName, expectedResult, MetaData.class );

        this.srcSize = size;
    }

    @Override
    protected boolean performMatch( MetaData metaData ) throws RbvException {

        boolean actuaResult = false;

        //get the file from the meta data
        Object size = metaData.getProperty( S3MetaData.SIZE );

        if( size == null ) {
            return false;
        }

        long destSize = ( Long ) size;

        actuaResult = this.srcSize == destSize;
        return actuaResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects file with size " + ( getExpectedResult()
                                                                       ? ""
                                                                       : "different than " )
               + "'" + this.srcSize + "'";
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
