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

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.s3.S3MetaData;

public class FileMd5S3Rule extends AbstractS3Rule {

    protected String  srcMD5;
    protected String  destMD5;

    public FileMd5S3Rule( String md5sum, String ruleName, boolean expectedResult ) {

        super( ruleName, expectedResult, MetaData.class );

        this.srcMD5 = md5sum;
    }

    @Override
    public boolean performMatch( MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        //get the file from the meta data
        Object destMD5 = metaData.getProperty( S3MetaData.MD5 );
        if( destMD5 == null ) {
            return false;
        }
        
        if( !StringUtils.isNullOrEmpty( ( String ) destMD5 ) ) {
            actualResult = destMD5.equals( this.srcMD5 );
        }

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects file with MD5 sum " + ( getExpectedResult()
                                                                          ? ""
                                                                          : "different than " )
               + "'" + this.srcMD5 + "' and real md5 '" + this.destMD5 + "'";
    }
}
