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
import com.axway.ats.rbv.imap.rules.StringInMimePartRule;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

public class Test_StringInMimePartRule extends BaseTest {
    private MetaData    metaData;
    private MimePackage mailMessage;

    @Before
    public void setUp() throws PackageException, RbvException {

        mailMessage = new MimePackage(Test_StringInMimePartRule.class.getResourceAsStream("mail_with_text_plain_attachment.msg"));

        metaData = new ImapMetaData(mailMessage);

    }

    @Test
    public void isMatchRegularPartPositive() throws RbvException {

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("US AIR FORCE",
                                                             false,
                                                             2,
                                                             false,
                                                             "isMatchRegularPartPositive1",
                                                             true);
        assertTrue(rule.isMatch(metaData));

        //expected false
        rule = new StringInMimePartRule("asdfasdf", false, 2, false, "isMatchRegularPartPositive2", false);
        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchRegularPartNegative() throws RbvException {

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("US asdfasdf FORCE",
                                                             false,
                                                             2,
                                                             false,
                                                             "isMatchRegularPartNegative1",
                                                             true);
        assertFalse(rule.isMatch(metaData));

        //expected false
        rule = new StringInMimePartRule("US AIR FORCE",
                                        false,
                                        2,
                                        false,
                                        "isMatchRegularPartNegative2",
                                        false);
        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchAttachmentPositive() throws RbvException {

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("You are now ready to run TOMTOM",
                                                             false,
                                                             0,
                                                             true,
                                                             "isMatchAttachmentPositive1",
                                                             true);
        assertTrue(rule.isMatch(metaData));

        //expected false
        rule = new StringInMimePartRule("asdfasdf", false, 0, true, "isMatchAttachmentPositive2", false);
        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchAttachmentNegative() throws RbvException {

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("afasf",
                                                             false,
                                                             0,
                                                             true,
                                                             "isMatchAttachmentNegative1",
                                                             true);
        assertFalse(rule.isMatch(metaData));

        //expected false
        rule = new StringInMimePartRule("You are now ready to run TOMTOM 6",
                                        false,
                                        0,
                                        true,
                                        "isMatchAttachmentNegative2",
                                        false);
        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchRegexRegularPartPositive() throws RbvException {

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("US .* FORCE",
                                                             true,
                                                             2,
                                                             false,
                                                             "isMatchRegexRegularPartPositive1",
                                                             true);
        assertTrue(rule.isMatch(metaData));

        //expected false
        rule = new StringInMimePartRule("US .? FORCE",
                                        true,
                                        2,
                                        false,
                                        "isMatchRegexRegularPartPositive2",
                                        false);
        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchRegexRegularPartNegative() throws RbvException {

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("US .? FORCE",
                                                             true,
                                                             2,
                                                             false,
                                                             "isMatchRegexRegularPartNegative1",
                                                             true);
        assertFalse(rule.isMatch(metaData));

        //expected false
        rule = new StringInMimePartRule("US .* FORCE",
                                        true,
                                        2,
                                        false,
                                        "isMatchRegexRegularPartNegative2",
                                        false);
        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchRegexAttachmentPositive() throws RbvException {

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("You .* now",
                                                             true,
                                                             0,
                                                             true,
                                                             "isMatchRegexAttachmentPositive1",
                                                             true);
        assertTrue(rule.isMatch(metaData));

        //expected false
        rule = new StringInMimePartRule("asdas.?", true, 0, true, "isMatchRegexAttachmentPositive2", false);
        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchRegexAttachmentNegative() throws RbvException {

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("asd.?",
                                                             true,
                                                             0,
                                                             true,
                                                             "isMatchRegexAttachmentNegative1",
                                                             true);
        assertFalse(rule.isMatch(metaData));

        //expected false
        rule = new StringInMimePartRule("You .* now",
                                        true,
                                        0,
                                        true,
                                        "isMatchRegexAttachmentNegative2",
                                        false);
        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchRegularPartUTF8Positive() throws RbvException, PackageException {

        MimePackage utf8Package = new MimePackage();
        utf8Package.addPart("Изчерпателна информация", MimePackage.PART_TYPE_TEXT_PLAIN, "utf-8");

        ImapMetaData utf8MetaData = new ImapMetaData(utf8Package);

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("Изчерпателна",
                                                             false,
                                                             0,
                                                             false,
                                                             "isMatchRegularPartPositive1",
                                                             true);
        assertTrue(rule.isMatch(utf8MetaData));

        //expected false
        rule = new StringInMimePartRule("Изчерпатслна",
                                        false,
                                        0,
                                        false,
                                        "isMatchRegularPartPositive2",
                                        false);
        assertTrue(rule.isMatch(utf8MetaData));
    }

    @Test
    public void isMatchRegexRegularPartUTF8Positive() throws RbvException, PackageException {

        MimePackage utf8Package = new MimePackage();
        utf8Package.addPart("Изчерпателна информация", MimePackage.PART_TYPE_TEXT_PLAIN, "utf-8");

        ImapMetaData utf8MetaData = new ImapMetaData(utf8Package);

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule(".*информация$",
                                                             true,
                                                             0,
                                                             false,
                                                             "isMatchRegexRegularPartPositive1",
                                                             true);
        assertTrue(rule.isMatch(utf8MetaData));

        //expected false
        rule = new StringInMimePartRule("Изчерпателна.*яа",
                                        true,
                                        0,
                                        false,
                                        "isMatchRegexRegularPartPositive2",
                                        false);
        assertTrue(rule.isMatch(utf8MetaData));
    }

    @Test
    public void isMatchRegularPartUTF8WholeMessagePositive() throws RbvException, PackageException {

        MimePackage utf8Package = new MimePackage(Test_StringInMimePartRule.class.getResourceAsStream("mail_with_one_attachment.msg"));
        utf8Package.addPart("Изчерпателна информация", MimePackage.PART_TYPE_TEXT_PLAIN, "utf-8");

        ImapMetaData utf8MetaData = new ImapMetaData(utf8Package);

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule("Изчерпателна",
                                                             false,
                                                             "isMatchRegularPartPositive1",
                                                             true);
        assertTrue(rule.isMatch(utf8MetaData));

        //expected false
        rule = new StringInMimePartRule("Изчерпатслна", false, "isMatchRegularPartPositive2", false);
        assertTrue(rule.isMatch(utf8MetaData));
    }

    @Test
    public void isMatchRegexRegularPartUTF8WholeMessagePositive() throws RbvException, PackageException {

        MimePackage utf8Package = new MimePackage(Test_StringInMimePartRule.class.getResourceAsStream("mail_with_one_attachment.msg"));
        utf8Package.addPart("Изчерпателна информация", MimePackage.PART_TYPE_TEXT_PLAIN, "utf-8");

        ImapMetaData utf8MetaData = new ImapMetaData(utf8Package);

        //expected true
        StringInMimePartRule rule = new StringInMimePartRule(".*информация$",
                                                             true,
                                                             "isMatchRegexRegularPartPositive1",
                                                             true);
        assertTrue(rule.isMatch(utf8MetaData));

        //expected false
        rule = new StringInMimePartRule("Изчерпателна.*яа", true, "isMatchRegexRegularPartPositive2", false);
        assertTrue(rule.isMatch(utf8MetaData));
    }

    @Test( expected = RbvException.class)
    public void isMatchNullMetaData() throws RbvException {

        StringInMimePartRule rule = new StringInMimePartRule("US AIR FORCE",
                                                             false,
                                                             2,
                                                             false,
                                                             "isMatchNullMetaData",
                                                             false);

        assertFalse(rule.isMatch(null));

    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchWrongMetaData() throws RbvException {

        StringInMimePartRule rule = new StringInMimePartRule("US AIR FORCE",
                                                             false,
                                                             2,
                                                             false,
                                                             "isMatchEmptyMetaData",
                                                             false);

        metaData = new MetaData();
        assertFalse(rule.isMatch(metaData));
    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchEmptyMetaData() throws RbvException {

        StringInMimePartRule rule = new StringInMimePartRule("US AIR FORCE",
                                                             false,
                                                             2,
                                                             false,
                                                             "isMatchEmptyMetaData",
                                                             false);

        metaData = new ImapMetaData(null);
        assertFalse(rule.isMatch(metaData));
    }
}
