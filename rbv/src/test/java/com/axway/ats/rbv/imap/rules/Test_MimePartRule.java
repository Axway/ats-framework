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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.imap.rules.MimePartRule;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

public class Test_MimePartRule extends BaseTest {

    private MetaData    metaData;
    private MimePackage mailMessage;
    private MimePackage expectedMailMessage;

    @Before
    public void setUp() throws PackageException, RbvException {

        mailMessage = new MimePackage(Test_MimePartRule.class.getResourceAsStream("mail_with_two_attachments.msg"));

        metaData = new ImapMetaData(mailMessage);

        expectedMailMessage = new MimePackage(Test_MimePartRule.class.getResourceAsStream("mail_with_two_attachments_modified.msg"));
    }

    @Test
    public void isMatchRegularPartPositive() throws RbvException {

        //expected true
        MimePartRule rule = new MimePartRule(expectedMailMessage,
                                             0,
                                             false,
                                             "isMatchRegularPartPositive1",
                                             true);
        assertTrue(rule.isMatch(metaData));

        //expected false
        rule = new MimePartRule(expectedMailMessage, 2, false, "isMatchRegularPartPositive2", false);
        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchRegularPartNegative() throws RbvException {

        //expected true
        MimePartRule rule = new MimePartRule(expectedMailMessage,
                                             2,
                                             false,
                                             "isMatchRegularPartNegative1",
                                             true);
        assertFalse(rule.isMatch(metaData));

        //expected false
        rule = new MimePartRule(expectedMailMessage, 0, false, "isMatchRegularPartNegative2", false);
        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchAttachmentPositive() throws RbvException {

        //expected true
        MimePartRule rule = new MimePartRule(expectedMailMessage,
                                             0,
                                             true,
                                             "isMatchAttachmentPositive1",
                                             true);
        assertTrue(rule.isMatch(metaData));

        //expected false
        rule = new MimePartRule(expectedMailMessage, 1, true, "isMatchAttachmentPositive2", false);
        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchAttachmentNegative() throws RbvException {

        //expected true
        MimePartRule rule = new MimePartRule(expectedMailMessage,
                                             1,
                                             true,
                                             "isMatchAttachmentNegative1",
                                             true);
        assertFalse(rule.isMatch(metaData));

        //expected false
        rule = new MimePartRule(expectedMailMessage, 0, true, "isMatchAttachmentNegative2", false);
        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchRegularPartNoSuchPart() throws PackageException, RbvException {

        mailMessage = new MimePackage(Test_MimePartRule.class.getResourceAsStream("mail_with_one_attachment.msg"));
        metaData = new ImapMetaData(mailMessage);

        //expected true
        MimePartRule rule = new MimePartRule(expectedMailMessage,
                                             2,
                                             false,
                                             "isMatchRegularPartNoSuchPart1",
                                             true);
        assertFalse(rule.isMatch(metaData));

        //expected false
        rule = new MimePartRule(expectedMailMessage, 2, false, "isMatchRegularPartNoSuchPart2", false);
        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchAttachmentNoSuchPart() throws PackageException, RbvException {

        mailMessage = new MimePackage(Test_MimePartRule.class.getResourceAsStream("mail_with_one_attachment.msg"));
        metaData = new ImapMetaData(mailMessage);

        //expected true
        MimePartRule rule = new MimePartRule(expectedMailMessage,
                                             1,
                                             true,
                                             "isMatchAttachmentNoSuchPart1",
                                             true);
        assertFalse(rule.isMatch(metaData));

        //expected false
        rule = new MimePartRule(expectedMailMessage, 1, true, "isMatchAttachmentNoSuchPart2", false);
        assertTrue(rule.isMatch(metaData));
    }

    @Test( expected = RbvException.class)
    public void isMatchNullExpectedMessage() throws RbvException {

        new MimePartRule(null, 0, false, "isMatchNullExpectedMessage", true);
    }

    @Test( expected = RbvException.class)
    public void isMatchNullMetaData() throws RbvException {

        MimePartRule rule = new MimePartRule(expectedMailMessage, 0, false, "isMatchNullMetaData", true);

        assertFalse(rule.isMatch(null));

    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchWrongMetaData() throws RbvException {

        MimePartRule rule = new MimePartRule(expectedMailMessage, 0, false, "isMatchEmptyMetaData", true);

        metaData = new MetaData();
        assertFalse(rule.isMatch(metaData));

    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchEmptyMetaData() throws RbvException {

        MimePartRule rule = new MimePartRule(expectedMailMessage, 0, false, "isMatchEmptyMetaData", true);

        metaData = new ImapMetaData(null);
        assertFalse(rule.isMatch(metaData));

    }
}
