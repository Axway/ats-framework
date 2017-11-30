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
package com.axway.ats.rbv.rules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.imap.Test_ImapStorage;
import com.axway.ats.rbv.imap.rules.HeaderRule;
import com.axway.ats.rbv.imap.rules.HeaderRule.HeaderMatchMode;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.OrRuleOperation;

public class Test_OrRuleOperation extends BaseTest {

    private static MetaData metaData;

    @BeforeClass
    public static void setUpTest_AndRuleOperation() throws PackageException, RbvException {

        MimePackage testMessage = new MimePackage(Test_ImapStorage.class.getResourceAsStream("mail.msg"));

        //init the meta data with the test message
        metaData = new ImapMetaData(testMessage);
    }

    @Test
    public void isMatchBothRulesExpectedTrue() throws RbvException {

        HeaderRule firstRule = new HeaderRule("Sender",
                                              "gmail.com",
                                              HeaderMatchMode.FIND,
                                              "isMatchBothRulesExpectedTrue1",
                                              true);
        HeaderRule secondRule = new HeaderRule("Sender",
                                               "gmail.com",
                                               HeaderMatchMode.FIND,
                                               "isMatchBothRulesExpectedTrue2",
                                               true);

        OrRuleOperation andRule = new OrRuleOperation();
        andRule.addRule(firstRule);
        andRule.addRule(secondRule);

        assertTrue(andRule.isMatch(metaData));
    }

    @Test
    public void isMatchOneOfThRulesExpectedFalse() throws RbvException {

        HeaderRule firstRule = new HeaderRule("Sender",
                                              "gmail.com",
                                              HeaderMatchMode.FIND,
                                              "isMatchOneOfThRulesExpectedFalse1",
                                              true);
        HeaderRule secondRule = new HeaderRule("Sender",
                                               "gmail123.com",
                                               HeaderMatchMode.FIND,
                                               "isMatchOneOfThRulesExpectedFalse2",
                                               false);

        OrRuleOperation andRule = new OrRuleOperation();
        andRule.addRule(firstRule);
        andRule.addRule(secondRule);

        assertTrue(andRule.isMatch(metaData));
    }

    @Test
    public void isMatchOnlyOneRulePasses() throws RbvException {

        HeaderRule firstRule = new HeaderRule("Sender",
                                              "gmail.com",
                                              HeaderMatchMode.FIND,
                                              "isMatchOnlyOneRulePasses1",
                                              true);
        HeaderRule secondRule = new HeaderRule("Sender",
                                               "gmail123.com",
                                               HeaderMatchMode.FIND,
                                               "isMatchOnlyOneRulePasses2",
                                               true);

        OrRuleOperation andRule = new OrRuleOperation();
        andRule.addRule(firstRule);
        andRule.addRule(secondRule);

        assertTrue(andRule.isMatch(metaData));
    }

    @Test
    public void isMatchNoneOfTheRulesPasses() throws RbvException {

        HeaderRule firstRule = new HeaderRule("Sender",
                                              "gmail.com",
                                              HeaderMatchMode.FIND,
                                              "isMatchNoneOfTheRulesPasses1",
                                              false);
        HeaderRule secondRule = new HeaderRule("Sender",
                                               "gmail123.com",
                                               HeaderMatchMode.FIND,
                                               "isMatchNoneOfTheRulesPasses2",
                                               true);

        OrRuleOperation andRule = new OrRuleOperation();
        andRule.addRule(firstRule);
        andRule.addRule(secondRule);

        assertFalse(andRule.isMatch(metaData));
    }

}
