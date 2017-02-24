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
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

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
import com.axway.ats.rbv.filesystem.rules.FileMd5Rule;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FileMd5Rule.class })
public class Test_FileMD5Rule extends BaseTest {

    public static String             md5sum       = "766d7f6cd12708346544e90a9d69221e";
    public static String             testFilePath = "/tmp/test/md5.txt";

    public static FileSystemMetaData meta;
    public static FilePackage        pack;

    @Before
    public void setUpTest_FileMD5Rule() throws PackageException, RbvException {

        meta = createMock( FileSystemMetaData.class );
        pack = createMock( FilePackage.class );
    }

    @Test
    public void isMatchConstructWithMd5ExpectTruePositive() throws PackageException, RbvException {

        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( md5sum, "isMatchConstructWithMd5ExpectTruePositive", true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertTrue( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchConstructWithMd5ExpectTrueNegative() throws PackageException, RbvException {

        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( "23234234", "isMatchConstructWithMd5ExpectTrueNegative", true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchConstructWithMd5ExpectTrueNegativeEmptyMd5() throws PackageException, RbvException {

        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( "", "isMatchConstructWithMd5ExpectTrueNegativeEmptyMd5", true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchConstructWithMd5ExpectTrueNegativeNullMd5() throws PackageException, RbvException {

        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( null, "isMatchConstructWithMd5ExpectTrueNegativeNullMd5", true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchConstructWithFileExpectTruePositive() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getMd5sum() ).andReturn( md5sum );
        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( null,
                                            testFilePath,
                                            "isMatchConstructWithFileExpectTruePositive",
                                            true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertTrue( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchConstructWithMd5ExpectFalsePositive() throws PackageException, RbvException {

        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( "23234234", "isMatchConstructWithMd5ExpectFalsePositive", false );
        MetaData metaData = new FileSystemMetaData( pack );
        assertTrue( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchConstructWithMd5ExpectFalseNegative() throws PackageException, RbvException {

        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( md5sum, "isMatchConstructWithMd5ExpectFalseNegative", false );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchConstructWithFileExpectFalseNegative() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getMd5sum() ).andReturn( md5sum );
        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( null,
                                            testFilePath,
                                            "isMatchConstructWithFileExpectFalseNegative",
                                            false );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test(expected = MetaDataIncorrectException.class)
    public void isMatchNullMetaDataContent() throws PackageException, RbvException {

        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( "", "isMatchNullMetaDataContent", true );
        MetaData metaData = new FileSystemMetaData( null );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test(expected = MetaDataIncorrectException.class)
    public void isMatchInvalidMetaDataContent() throws PackageException, RbvException {

        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( "", "isMatchInvalidMetaDataContent", true );
        MetaData metaData = new MetaData();
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test(expected = RbvException.class)
    public void isMatchNullMetaData() throws PackageException, RbvException {

        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( md5sum, "isMatchNullMetaData", true );
        assertFalse( rule.isMatch( null ) );

        verifyAll();
    }

    @Test
    public void constructorWithMachine() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getMd5sum() ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( null, testFilePath, "constructorWithMachine", true );
        assertTrue( rule != null );

        verifyAll();
    }

    @Test
    public void constructorWithNullMachine() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getMd5sum() ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( null, testFilePath, "constructorWithNullMachine", true );
        assertTrue( rule != null );

        verifyAll();
    }

    @Test
    public void constructorWithMachineBinaryRepencrypt() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( null,
                                            testFilePath,
                                            true,
                                            "constructorWithMachineBinaryRepencrypt",
                                            true );
        assertTrue( rule != null );

        verifyAll();
    }

    @Test
    public void constructorWithNullMachineBinaryRepencrypt() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getMd5sum( true ) ).andReturn( md5sum );

        replayAll();

        FileMd5Rule rule = new FileMd5Rule( null,
                                            testFilePath,
                                            true,
                                            "constructorWithNullMachineBinaryRepencrypt",
                                            true );
        assertTrue( rule != null );

        verifyAll();
    }

}
