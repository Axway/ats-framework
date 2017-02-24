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
package com.axway.ats.rbv.filesystem.rules;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.filesystem.rules.FileContentRule;
import com.axway.ats.rbv.model.RbvException;

@RunWith(PowerMockRunner.class)
public class Test_FileContentRule extends BaseTest {

    private static final String EXPRESSION = "Some expression";
    private static final String RULE_NAME  = "FileContentRule";

    private FilePackage         testFilePackage;

    @Before
    public void setUp() {

        testFilePackage = createMock( FilePackage.class );
    }

    @Test
    public void isMatch() throws Exception {

        expect( testFilePackage.grep( EXPRESSION, false ) ).andReturn( new String[]{ EXPRESSION } );
        replayAll();

        FileContentRule rule = new FileContentRule( EXPRESSION, RULE_NAME, false, true );
        MetaData metaData = new FileSystemMetaData( testFilePackage );

        assertTrue( rule.isMatch( metaData ) );
        verifyAll();
    }

    @Test
    public void isMatchNegative() throws Exception {

        expect( testFilePackage.grep( EXPRESSION, false ) ).andReturn( new String[0] );
        replayAll();

        FileContentRule rule = new FileContentRule( EXPRESSION, RULE_NAME, false, true );
        MetaData metaData = new FileSystemMetaData( testFilePackage );

        assertFalse( rule.isMatch( metaData ) );
        verifyAll();
    }

    @Test(expected = RbvException.class)
    public void isMatchNegativeException() throws Exception {

        expect( testFilePackage.grep( EXPRESSION, false ) ).andThrow( new PackageException( "" ) );
        replayAll();

        FileContentRule rule = new FileContentRule( EXPRESSION, RULE_NAME, false, true );
        MetaData metaData = new FileSystemMetaData( testFilePackage );

        rule.isMatch( metaData );
        verifyAll();
    }

    @Test
    public void isMatchRegularExpression() throws Exception {

        expect( testFilePackage.grep( EXPRESSION, true ) ).andReturn( new String[]{ EXPRESSION } );
        replayAll();

        FileContentRule rule = new FileContentRule( EXPRESSION, RULE_NAME, true, true );
        MetaData metaData = new FileSystemMetaData( testFilePackage );

        assertTrue( rule.isMatch( metaData ) );
        verifyAll();
    }

    @Test
    public void getMetaDataKeys() throws Exception {

        replayAll();
        FileContentRule rule = new FileContentRule( EXPRESSION, RULE_NAME, true, true );

        ArrayList<String> expected = new ArrayList<String>();
        expected.add( FileSystemMetaData.FILE_PACKAGE );

        assertTrue( Arrays.equals( expected.toArray(), rule.getMetaDataKeys().toArray() ) );

        verifyAll();
    }
}
