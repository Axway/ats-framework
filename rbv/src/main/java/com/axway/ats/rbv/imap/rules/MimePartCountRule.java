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

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.model.RbvException;

public class MimePartCountRule extends AbstractImapRule {

    private int     expectedNumParts;
    private boolean lookForAttachments;

    public MimePartCountRule( int expectedNumParts,
                              boolean lookForAttachments,
                              String ruleName,
                              boolean expectedResult ) {

        this( new int[0], expectedNumParts, lookForAttachments, ruleName, expectedResult );
    }

    public MimePartCountRule( int[] nestedPackagePath,
                              int expectedNumParts,
                              boolean lookForAttachments,
                              String ruleName,
                              boolean expectedResult ) {

        super( ruleName, expectedResult, ImapMetaData.class );

        this.expectedNumParts = expectedNumParts;
        this.lookForAttachments = lookForAttachments;

        setNestedPackagePath( nestedPackagePath );
    }

    @Override
    protected boolean performMatch(
                                    MetaData metaData ) throws RbvException {

        //get the emailMessage
        //the meta data type check already passed, so it is safe to cast
        MimePackage emailMessage = getNeededMimePackage( metaData );

        int actualNumParts;
        if( lookForAttachments ) {
            actualNumParts = emailMessage.getAttachmentPartCount();
        } else {
            actualNumParts = emailMessage.getRegularPartCount();
        }

        log.debug( "Actual number of parts is " + actualNumParts );

        boolean actualResult = actualNumParts == expectedNumParts;

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects message with " + expectedNumParts + " MIME parts ("
               + ( lookForAttachments
                                     ? "attachments"
                                     : "regular" ) + ")";
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add( ImapMetaData.MIME_PACKAGE );
        return metaKeys;
    }
}
