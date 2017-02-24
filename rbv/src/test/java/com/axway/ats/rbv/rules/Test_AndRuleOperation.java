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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.filesystem.rules.FileFolderRule;
import com.axway.ats.rbv.filesystem.rules.FilePathRule;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.imap.Test_ImapStorage;
import com.axway.ats.rbv.imap.rules.HeaderRule;
import com.axway.ats.rbv.imap.rules.HeaderRule.HeaderMatchMode;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.AndRuleOperation;

@SuppressWarnings("boxing")
@RunWith(PowerMockRunner.class)
public class Test_AndRuleOperation extends BaseTest {

    private static MetaData metaData;

    @BeforeClass
    public static void setUpTest_AndRuleOperation() throws PackageException, RbvException {

        MimePackage testMessage = new MimePackage( Test_ImapStorage.class.getResourceAsStream( "mail.msg" ) );

        //init the meta data with the test message
        metaData = new ImapMetaData( testMessage );
    }

    @Test
    public void isMatchBothRulesExpectedTruePositive() throws RbvException {

        HeaderRule firstRule = new HeaderRule( "Sender",
                                               "gmail.com",
                                               HeaderMatchMode.FIND,
                                               "isMatchBothRulesExpectedTruePositive",
                                               true );
        HeaderRule secondRule = new HeaderRule( "Sender",
                                                "gmail.com",
                                                HeaderMatchMode.FIND,
                                                "isMatchBothRulesExpectedTruePositive",
                                                true );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( firstRule );
        andRule.addRule( secondRule );

        assertTrue( andRule.isMatch( metaData ) );
    }

    @Test
    public void isMatchOneOfThRulesExpectedFalsePositive() throws RbvException {

        HeaderRule firstRule = new HeaderRule( "Sender",
                                               "gmail.com",
                                               HeaderMatchMode.FIND,
                                               "isMatchOneOfThRulesExpectedFalsePositive",
                                               true );
        HeaderRule secondRule = new HeaderRule( "Sender",
                                                "gmail123.com",
                                                HeaderMatchMode.FIND,
                                                "isMatchOneOfThRulesExpectedFalsePositive",
                                                false );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( firstRule );
        andRule.addRule( secondRule );

        assertTrue( andRule.isMatch( metaData ) );
    }

    @Test
    public void isMatchBothRulesExpectedTrueNegative() throws RbvException {

        HeaderRule firstRule = new HeaderRule( "Sender",
                                               "gmail.com",
                                               HeaderMatchMode.FIND,
                                               "isMatchBothRulesExpectedTrueNegative",
                                               true );
        HeaderRule secondRule = new HeaderRule( "Sender",
                                                "gmail123.com",
                                                HeaderMatchMode.FIND,
                                                "isMatchBothRulesExpectedTrueNegative",
                                                true );
        firstRule.equals( secondRule );
        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( firstRule );
        andRule.addRule( secondRule );

        assertFalse( andRule.isMatch( metaData ) );
    }

    @Test
    public void isMatchOneOfThRulesExpectedFalseNegative() throws RbvException {

        HeaderRule firstRule = new HeaderRule( "Sender",
                                               "gmail.com",
                                               HeaderMatchMode.FIND,
                                               "isMatchOneOfThRulesExpectedFalseNegative",
                                               true );
        HeaderRule secondRule = new HeaderRule( "Sender",
                                                "gmail.com",
                                                HeaderMatchMode.FIND,
                                                "isMatchOneOfThRulesExpectedFalseNegative",
                                                false );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( firstRule );
        andRule.addRule( secondRule );

        assertFalse( andRule.isMatch( metaData ) );
    }

    @Test
    public void isMatchPriority() throws RbvException, PackageException {

        FileSystemMetaData meta = createMock( FileSystemMetaData.class );
        FilePackage pack = createMock( FilePackage.class );
        expect( meta.getFilePackage() ).andReturn( pack );
        expect( pack.isFile() ).andReturn( true );
        // at this point the evaluation should stop since this is the first rule to
        // evaluate and it fails thus the second should not be evaluated at all

        // if the priority does not work then the second rule would fail the unit test
        // because we are not expecting any calls it would do to the mock objects
        replayAll();

        FilePathRule rule = new FilePathRule( "some/path/some.file", "pathRule1", true, 2 );
        FileFolderRule anotherRule = new FileFolderRule( false, "folderRule", true, 1 );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( rule );
        andRule.addRule( anotherRule );

        assertFalse( andRule.isMatch( meta ) );
        verifyAll();
    }
}
