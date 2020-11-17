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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.NoSuchMimePartException;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.AbstractRule;

public class MimePartRule extends AbstractRule {

    private static Logger log = LogManager.getLogger(MimePartRule.class);

    private long          expectedChecksum;
    private int           partIndex;
    private boolean       isPartAttachment;

    public MimePartRule( MimePackage expectedMessage,
                         int partIndex,
                         boolean isPartAttachment,
                         String ruleName,
                         boolean expectedResult ) throws RbvException {

        super(ruleName, expectedResult, ImapMetaData.class);

        if (expectedMessage == null) {
            throw new RbvException("Expected message passed is null");
        }

        try {
            this.expectedChecksum = expectedMessage.getPartChecksum(partIndex, isPartAttachment);
        } catch (PackageException pe) {
            throw new RbvException(pe);
        }

        this.partIndex = partIndex;
        this.isPartAttachment = isPartAttachment;
    }

    @Override
    protected boolean performMatch(
                                    MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        //get the emailMessage
        //the meta data type check already passed, so it is safe to cast
        MimePackage emailMessage = ((ImapMetaData) metaData).getMimePackage();

        try {
            InputStream actualPartDataStream = null;
            try {
                actualPartDataStream = emailMessage.getPartData(partIndex, isPartAttachment);
            } catch (NoSuchMimePartException e) {
                //if there is no such mime part then the parts do not match
                log.debug("No MIME part at position '" + partIndex + "'");
                return false;
            }

            if (actualPartDataStream != null) {

                long actualChecksum = emailMessage.getPartChecksum(partIndex, isPartAttachment);
                actualResult = (expectedChecksum == actualChecksum);

            } else {
                log.debug("MIME part at position '" + partIndex + "' does not have any content");
                return false;
            }
        } catch (PackageException pe) {
            throw new RbvException(pe);
        }

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects message with matching MIME parts at position " + partIndex + " ("
               + (isPartAttachment
                                   ? "attachment"
                                   : "regular")
               + ")";
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(ImapMetaData.MIME_PACKAGE);
        return metaKeys;
    }
}
