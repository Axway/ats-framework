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
import java.util.Date;
import java.util.List;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.AbstractRule;
import com.axway.ats.rbv.s3.S3MetaData;

public class FileModtimeS3Rule extends AbstractRule {

    protected long srcModtime;
    private long actualTime;

    /**
     * Match with the specified size
     *
     * @param log            The logging object
     * @param sName            The name of the matcher - used for logging
     * @param bExpected     Matcher's expected result
     * @param size             The size with which to compare
     */
    public FileModtimeS3Rule( long modtime, String ruleName, boolean expectedResult ) {

        super( ruleName, expectedResult, MetaData.class );

        this.srcModtime = modtime;
    }

    @Override
    public boolean performMatch( MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        //get the file from the meta data
        Object modTime = metaData.getProperty( S3MetaData.LAST_MODIFIED );
        if( modTime == null ) {
            return false;
        }
        
        actualTime = ( ( Date ) modTime ).getTime() / 1000;
        actualResult = actualTime * 1000 == this.srcModtime;

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects file with mod time " + ( getExpectedResult()
                                                                           ? ""
                                                                           : " different than " )
               + "'" + this.srcModtime + "'";
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
