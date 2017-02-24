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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.filesystem.rules.FilePathRule;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FilePathRule.class })
public class Test_FilePathRule extends BaseTest {

    public static String             testFileAbsPath     = "/tmp/test/path1.txt";
    public static String             testFileDir  = "/tmp/test/";
    public static String             wrongFilePath = "/tmp/notexistingfile1234TESTTESTETSTEST.txt";

    public static FilePackage        pack;
    public static FileSystemMetaData meta;

    @Before
    public void setUpTest_FilePathRule() throws PackageException, RbvException {

        pack = createMock( FilePackage.class );
        meta = createMock( FileSystemMetaData.class );
    }

    @Test
    public void isMatchExpectedTruePositive() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );

        replayAll();

        FilePathRule rule = new FilePathRule( testFileAbsPath, "isMatchExpectedTruePositive", true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertTrue( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchExpectedTruePositiveRegex() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );
        expect( pack.getName() ).andReturn( testFileAbsPath.substring( testFileAbsPath.lastIndexOf( '/' ) + 1 ) );

        replayAll();

        FilePathRule rule = new FilePathRule( testFileDir,
                                              "p.*1.+xt",
                                              "isMatchExpectedTruePositive",
                                              true,
                                              1 );

        MetaData metaData = new FileSystemMetaData( pack );
        assertTrue( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchExpectedTrueNegative() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );

        replayAll();

        FilePathRule rule = new FilePathRule( wrongFilePath, "isMatchExpectedTrueNegative", true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchExpectedTrueNegativeRegex() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );
        expect( pack.getName() ).andReturn( testFileAbsPath.substring( testFileAbsPath.lastIndexOf( '/' ) + 1 ) );

        replayAll();

        FilePathRule rule = new FilePathRule( wrongFilePath,
                                              "p.*1.+xt",
                                              "isMatchExpectedTrueNegative",
                                              true,
                                              1 );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchExpectedTrueNegativeNullPath() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );

        replayAll();

        FilePathRule rule = new FilePathRule( null, "isMatchExpectedTrueNegativeNullPath", true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchExpectedTrueNegativeNullPathregex() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( null );
        expect( pack.getName() ).andReturn( testFileAbsPath.substring( testFileAbsPath.lastIndexOf( '/' ) + 1 ) );

        replayAll();

        FilePathRule rule = new FilePathRule( null,
                                              "p.*1.+xt",
                                              "isMatchExpectedTrueNegativeNullPath",
                                              true,
                                              1 );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchExpectedFalsePositive() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );

        replayAll();

        FilePathRule rule = new FilePathRule( wrongFilePath, "isMatchExpectedFalsePositive", false );
        MetaData metaData = new FileSystemMetaData( pack );
        assertTrue( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchExpectedFalseNegative() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );

        replayAll();

        FilePathRule rule = new FilePathRule( testFileAbsPath, "isMatchExpectedFalseNegative", false );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void getMetaKeys() {

        FilePathRule rule = new FilePathRule( testFileAbsPath, "isMatchExpectedFalseNegative", false );
        List<String> list = new ArrayList<String>();
        list.add( FileSystemMetaData.FILE_PACKAGE );
        assertTrue( Arrays.equals( rule.getMetaDataKeys().toArray(), list.toArray() ) );
    }

    @Test
    public void priority() {

        FilePathRule rule = new FilePathRule( testFileAbsPath, "isMatchExpectedFalseNegative", false, 1 );
        assertEquals( rule.getPriority(), 1 );
    }

    @Test(expected = MetaDataIncorrectException.class)
    public void isMatchNullMetaDataContent() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );

        replayAll();

        FilePathRule rule = new FilePathRule( testFileAbsPath, "isMatchNullMetaDataContent", true );
        MetaData metaData = new FileSystemMetaData( null );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test(expected = MetaDataIncorrectException.class)
    public void isMatchInvalidMetaDataContent() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );

        replayAll();

        FilePathRule rule = new FilePathRule( testFileAbsPath, "isMatchInvalidMetaDataContent", true );
        MetaData metaData = new MetaData();
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test(expected = RbvException.class)
    public void isMatchNullMetaData() throws Exception {

        expect( pack.getAbsolutePath() ).andReturn( testFileAbsPath );

        replayAll();

        FilePathRule rule = new FilePathRule( testFileAbsPath, "isMatchNullMetaData", true );
        assertFalse( rule.isMatch( null ) );

        verifyAll();
    }
}
