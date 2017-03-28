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
package com.axway.ats.rbv.imap.rules;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.NoSuchMimePackageException;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.AbstractRule;

public abstract class AbstractImapRule extends AbstractRule {

    private int[] nestedPackagePath;

    public AbstractImapRule( String ruleName,
                             boolean expectedResult ) {

        super( ruleName, expectedResult );
    }

    public AbstractImapRule( String ruleName,
                             boolean expectedResult,
                             Class<? extends MetaData> metaDataClass ) {

        super( ruleName, expectedResult, metaDataClass );
    }

    public AbstractImapRule( String ruleName,
                             boolean expectedResult,
                             Class<? extends MetaData> metaDataClass,
                             int priority ) {

        super( ruleName, expectedResult, metaDataClass, priority );
    }

    protected void setNestedPackagePath(
                                         int[] nestedPackagePath ) {

        if( nestedPackagePath == null ) {
            this.nestedPackagePath = new int[0];
        } else {
            this.nestedPackagePath = nestedPackagePath;
        }
    }

    protected MimePackage getNeededMimePackage(
                                                MetaData metaData ) throws RbvException {

        MimePackage emailMessage;
        try {
            emailMessage = ( ( ImapMetaData ) metaData ).getMimePackage()
                                                        .getNeededMimePackage( nestedPackagePath );
        } catch( NoSuchMimePackageException nsmpe ) {
            throw new RbvException( nsmpe );
        }

        return emailMessage;
    }
}
