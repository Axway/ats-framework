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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.model.RbvException;

public class AttachmentNameRule extends AbstractImapRule {

    private String expectedValue;

    private int    attachmentIndex;

    public AttachmentNameRule( String expectedValue,
                               int attachmentIndex,
                               String ruleName,
                               boolean expectedResult ) {

        this( new byte[0], expectedValue, attachmentIndex, ruleName, expectedResult );
    }

    public AttachmentNameRule( byte[] nestedPackagePath,
                               String expectedValue,
                               int attachmentIndex,
                               String ruleName,
                               boolean expectedResult ) {

        super( ruleName, expectedResult, ImapMetaData.class );

        this.expectedValue = expectedValue;
        this.attachmentIndex = attachmentIndex;

        setNestedPackagePath( nestedPackagePath );
    }

    @Override
    protected boolean performMatch(
                                    MetaData metaData ) throws RbvException {

        //get the emailMessage
        //the meta data type check already passed, so it is safe to cast
        MimePackage emailMessage = getNeededMimePackage( metaData );

        String attachmentFileName;
        try {
            attachmentFileName = emailMessage.getAttachmentFileName( attachmentIndex );
        } catch( PackageException pe ) {
            throw new RbvException( pe );
        }

        //if there is no such file name return false 
        boolean actualResult = false;

        if( attachmentFileName != null ) {
            actualResult = Pattern.compile( expectedValue ).matcher( attachmentFileName ).matches();
            log.info( "Actual attachment file name is '" + attachmentFileName + "'" );
        } else {
            log.info( "No attachment with name that matches '" + expectedValue + "' was found" );
        }

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects file name '" + expectedValue + "'";
    }

    @Override
    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add( ImapMetaData.MIME_PACKAGE );
        return metaKeys;
    }
}
