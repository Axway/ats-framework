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

import java.util.Date;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.s3.S3MetaData;

public class FileModtimeS3Rule extends AbstractS3Rule {

    protected long srcModtime;
    private long actualTime; // in ms but rounded to 1 sec

    /**
     * Match with the specified modification time
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
        
        // nullifying the last 3 digits, so it will return the same result as from S3Operations method getObject() 
        actualTime = ( ( ( Date ) modTime ).getTime() / 1000 ) * 1000;
        actualResult = actualTime == this.srcModtime;

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects file with modification time " + ( getExpectedResult()
                                                                           ? ""
                                                                           : " different than " )
               + "'" + this.srcModtime + "' and actual modification time '" + this.actualTime + "'";
    }
}
