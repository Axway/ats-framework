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
import com.axway.ats.rbv.imap.rules.MimePartCountRule;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

public class Test_MimePartCountRule extends BaseTest {

    private MetaData metaData;

    @Before
    public void setUp() throws Exception {

        MimePackage mailMessage = null;
        mailMessage = new MimePackage( Test_MimePartCountRule.class.getResourceAsStream( "mail_with_one_attachment.msg" ) );

        metaData = new ImapMetaData( mailMessage );
    }

    @Test
    public void matchRegularMimePartsCountPositive() throws RbvException {

        MimePartCountRule rule = new MimePartCountRule( 2, false, "matchRegularMimePartsCountPositive", true );

        assertTrue( rule.isMatch( metaData ) );

    }

    @Test
    public void matchAttachmentsCountPositive() throws RbvException {

        MimePartCountRule rule = new MimePartCountRule( 1, true, "matchAttachmentsCountPositive", true );

        assertTrue( rule.isMatch( metaData ) );

    }

    @Test
    public void matchRegularMimePartsCountNegative() throws RbvException {

        MimePartCountRule rule = new MimePartCountRule( 1, false, "matchRegularMimePartsCountNegative", true );

        assertFalse( rule.isMatch( metaData ) );

    }

    @Test
    public void matchAttachmentsCountNegative() throws RbvException {

        MimePartCountRule rule = new MimePartCountRule( 5, true, "matchAttachmentsCountNegative", true );

        assertFalse( rule.isMatch( metaData ) );

    }

    @Test(expected = RbvException.class)
    public void isMatchNullMetaData() throws RbvException {

        MimePartCountRule rule = new MimePartCountRule( 5, true, "isMatchNullMetaData", true );

        assertFalse( rule.isMatch( null ) );

    }

    @Test(expected = MetaDataIncorrectException.class)
    public void isMatchWrongMetaData() throws RbvException {

        MimePartCountRule rule = new MimePartCountRule( 5, true, "isMatchEmptyMetaData", true );

        metaData = new MetaData();
        assertFalse( rule.isMatch( metaData ) );

    }

    @Test(expected = MetaDataIncorrectException.class)
    public void isMatchEmptyMetaData() throws RbvException {

        MimePartCountRule rule = new MimePartCountRule( 5, true, "isMatchEmptyMetaData", true );

        metaData = new ImapMetaData( null );
        assertFalse( rule.isMatch( metaData ) );

    }
}
