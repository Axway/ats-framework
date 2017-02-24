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
import com.axway.ats.rbv.imap.rules.AttachmentNameRule;
import com.axway.ats.rbv.model.RbvException;

public class Test_AttachmentNameRule extends BaseTest {

    private MetaData    metaData;
    private MimePackage mailMessage;

    @Before
    public void setUp() throws PackageException, RbvException {

        mailMessage = new MimePackage( Test_AttachmentNameRule.class.getResourceAsStream( "mail_with_two_attachments.msg" ) );

        metaData = new ImapMetaData( mailMessage );
    }

    @Test
    public void testAttachmentNameRulePositive() throws RbvException {

        //expected true
        AttachmentNameRule rule = new AttachmentNameRule( "24thekilt.jpg",
                                                          0,
                                                          "checkAttachmentNameRuleExpectingTrue_Positive",
                                                          true );

        assertTrue( rule.isMatch( metaData ) );

        //expected false  
        rule = new AttachmentNameRule( "24thekilt.jpg",
                                       1,
                                       "checkAttachmentNameRuleExpectingFalse_Positive",
                                       false );

        assertTrue( rule.isMatch( metaData ) );
    }

    @Test
    public void testAttachmentNameRuleNegative() throws RbvException {

        //expected true
        AttachmentNameRule rule = new AttachmentNameRule( "24thekilt_WRONG.jpg",
                                                          0,
                                                          "checkAttachmentNameRuleExpectingTrue_Negative",
                                                          true );

        assertFalse( rule.isMatch( metaData ) );

        //expected false  
        rule = new AttachmentNameRule( "24thekilt.jpg",
                                       0,
                                       "checkAttachmentNameRuleExpectingFalse_Negative",
                                       false );

        assertFalse( rule.isMatch( metaData ) );
    }

    @Test
    public void testAttachmentNameRuleUsingRegex() throws RbvException {

        AttachmentNameRule rule = new AttachmentNameRule( "24thekilt.*",
                                                          0,
                                                          "checkAttachmentNameRuleUsingRegex",
                                                          true );

        assertTrue( rule.isMatch( metaData ) );
    }

    @Test
    public void testAttachmentNameRuleUsingRegexNegative() throws RbvException {

        AttachmentNameRule rule = new AttachmentNameRule( "24thekilt",
                                                          0,
                                                          "testAttachmentNameRuleUsingRegexNegative",
                                                          true );

        assertFalse( rule.isMatch( metaData ) );
    }

    @Test(expected = RbvException.class)
    public void testAttachmentNameRule_WrongAttachmentIndex() throws RbvException {

        AttachmentNameRule rule = new AttachmentNameRule( "24thekilt.jpg",
                                                          5,
                                                          "testAttachmentNameRule_WrongAttachmentIndex",
                                                          true );

        assertTrue( rule.isMatch( metaData ) );
    }

}
