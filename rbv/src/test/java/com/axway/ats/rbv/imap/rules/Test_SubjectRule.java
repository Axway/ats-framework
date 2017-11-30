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
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.imap.Test_ImapStorage;
import com.axway.ats.rbv.imap.rules.SubjectRule;
import com.axway.ats.rbv.imap.rules.SubjectRule.SubjectMatchMode;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

public class Test_SubjectRule extends BaseTest {

    private MetaData metaData;

    @Before
    public void setUp() throws Exception {

        MimePackage mailMessage = new MimePackage(Test_ImapStorage.class.getResourceAsStream("mail.msg"));
        metaData = new ImapMetaData(mailMessage);
    }

    @Test
    public void isMatchModeEqualsPositive() throws RbvException {

        SubjectRule rule = new SubjectRule("LOTERIE MONDIALE.",
                                           SubjectMatchMode.EQUALS,
                                           "isMatchModeEqualsPositive",
                                           true);

        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchModeFindPositive() throws RbvException {

        SubjectRule rule = new SubjectRule("LOTERIE MONDIALE.",
                                           SubjectMatchMode.FIND,
                                           "isMatchModeFindPositive",
                                           true);

        //the whole string
        assertTrue(rule.isMatch(metaData));

        //part of the string
        rule = new SubjectRule("LOTERIE", SubjectMatchMode.FIND, "isMatchModeFindPositive", true);
        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchModeLeftPositive() throws RbvException {

        SubjectRule rule = new SubjectRule("LOTERIE", SubjectMatchMode.LEFT, "isMatchModeLeftPositive", true);

        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchModeRightPositive() throws RbvException {

        SubjectRule rule = new SubjectRule("MONDIALE.",
                                           SubjectMatchMode.RIGHT,
                                           "isMatchModeRightPositive",
                                           true);

        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchModeRegexPositive() throws RbvException {

        SubjectRule rule = new SubjectRule(".*MONDIALE.",
                                           SubjectMatchMode.REGEX,
                                           "isMatchModeRegexPositive",
                                           true);

        assertTrue(rule.isMatch(metaData));
    }

    @Test
    public void isMatchModeEqualsNegative() throws RbvException {

        SubjectRule rule = new SubjectRule("MONDIALE.",
                                           SubjectMatchMode.EQUALS,
                                           "isMatchModeEqualsNegative",
                                           true);

        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchModeFindNegative() throws RbvException {

        SubjectRule rule = new SubjectRule("MONDIALE.123",
                                           SubjectMatchMode.FIND,
                                           "isMatchModeFindNegative",
                                           true);

        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchModeLeftNegative() throws RbvException {

        SubjectRule rule = new SubjectRule("MONDIALE.",
                                           SubjectMatchMode.LEFT,
                                           "isMatchModeLeftNegative",
                                           true);

        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchModeRightNegative() throws RbvException {

        SubjectRule rule = new SubjectRule("MONDIALE",
                                           SubjectMatchMode.RIGHT,
                                           "isMatchModeRightNegative",
                                           true);

        assertFalse(rule.isMatch(metaData));
    }

    @Test
    public void isMatchModeRegexNegative() throws RbvException {

        SubjectRule rule = new SubjectRule("LOTERIE MONDIALE..+",
                                           SubjectMatchMode.REGEX,
                                           "isMatchModeRegexNegative",
                                           true);

        assertFalse(rule.isMatch(metaData));
    }

    @Test( expected = RbvException.class)
    public void isMatchNullMetaData() throws RbvException {

        SubjectRule rule = new SubjectRule("infos1.mercatoloterie@gmail.com",
                                           SubjectMatchMode.FIND,
                                           "isMatchNullMetaData",
                                           true);

        assertFalse(rule.isMatch(null));

    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchWrongMetaData() throws RbvException {

        SubjectRule rule = new SubjectRule("infos1.mercatoloterie@gmail.com",
                                           SubjectMatchMode.FIND,
                                           "isMatchEmptyMetaData",
                                           true);
        metaData = new MetaData();
        assertFalse(rule.isMatch(metaData));
    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchEmptyMetaData() throws RbvException {

        SubjectRule rule = new SubjectRule("infos1.mercatoloterie@gmail.com",
                                           SubjectMatchMode.FIND,
                                           "isMatchEmptyMetaData",
                                           true);
        metaData = new ImapMetaData(null);
        assertFalse(rule.isMatch(metaData));
    }
}
