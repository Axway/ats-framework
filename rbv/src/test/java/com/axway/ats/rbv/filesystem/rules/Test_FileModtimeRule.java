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
import com.axway.ats.rbv.filesystem.rules.FileModtimeRule;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FileModtimeRule.class })
public class Test_FileModtimeRule extends BaseTest {

    public static long               modtime      = 1221216297020L;
    public static String             testFilePath = "/tmp/test/modtime.txt";

    public static FilePackage        pack;
    public static FileSystemMetaData meta;

    @Before
    public void setUpTest_FileMD5Rule() throws PackageException, RbvException {

        meta = createMock( FileSystemMetaData.class );
        pack = createMock( FilePackage.class );
    }

    @Test
    public void isMatchConstructWithModTimeExpectTruePositive() throws Exception {

        expect( pack.getModTime() ).andReturn( modtime );

        replayAll();

        FileModtimeRule rule = new FileModtimeRule( modtime,
                                                    "isMatchConstructWithModTimeExpectTruePositive",
                                                    true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertTrue( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchConstructWithModTimeExpectTrueNegativeWrongFile() throws Exception {

        expect( pack.getModTime() ).andReturn( 123L );

        replayAll();

        FileModtimeRule rule = new FileModtimeRule( modtime,
                                                    "isMatchConstructWithModTimeExpectTrueNegativeWrongFile",
                                                    true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();

    }

    @Test
    public void isMatchConstructWithModTimeExpectTrueNegative() throws Exception {

        expect( pack.getModTime() ).andReturn( modtime );

        replayAll();

        FileModtimeRule rule = new FileModtimeRule( 123l,
                                                    "isMatchConstructWithModTimeExpectTrueNegative",
                                                    true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();

    }

    @Test
    public void isMatchConstructWithFileExpectTruePositive() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getModTime() ).andReturn( modtime );
        expect( pack.getModTime() ).andReturn( modtime );

        replayAll();

        FileModtimeRule rule = new FileModtimeRule( null,
                                                    testFilePath,
                                                    "isMatchConstructWithFileExpectTruePositive",
                                                    true );

        MetaData metaData = new FileSystemMetaData( pack );
        assertTrue( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test
    public void isMatchConstructWithFileExpectTrueNegativeWrongFile() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getModTime() ).andReturn( modtime );
        expect( pack.getModTime() ).andReturn( 123l );

        replayAll();

        FileModtimeRule rule = new FileModtimeRule( null,
                                                    testFilePath,
                                                    "isMatchConstructWithFileExpectTrueNegativeWrongFile",
                                                    true );
        MetaData metaData = new FileSystemMetaData( pack );
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();

    }

    @Test(expected = MetaDataIncorrectException.class)
    public void isMatchInvalidMetaDataContent() throws Exception {

        expect( pack.getModTime() ).andReturn( modtime );

        replayAll();

        FileModtimeRule rule = new FileModtimeRule( 123l, "isMatchInvalidMetaDataContent", true );
        MetaData metaData = new MetaData();
        assertFalse( rule.isMatch( metaData ) );

        verifyAll();
    }

    @Test(expected = RbvException.class)
    public void isMatchNullMetaData() throws Exception {

        expect( pack.getModTime() ).andReturn( modtime );

        replayAll();

        FileModtimeRule rule = new FileModtimeRule( 123, "isMatchNullMetaData", true );
        assertFalse( rule.isMatch( null ) );

        verifyAll();
    }

    @Test
    public void constructorWithMachine() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getModTime() ).andReturn( modtime );

        replayAll();

        FileModtimeRule rule = new FileModtimeRule( null, testFilePath, "constructorWithMachine", true );
        assertTrue( rule != null );

        verifyAll();
    }

    @Test
    public void constructorWithMachineNullMachine() throws Exception {

        expectNew( FilePackage.class, null, null, testFilePath ).andReturn( pack );
        expect( pack.getModTime() ).andReturn( modtime );

        replayAll();

        FileModtimeRule rule = new FileModtimeRule( null,
                                                    testFilePath,
                                                    "constructorWithMachineNullMachine",
                                                    true );
        assertTrue( rule != null );

        verifyAll();

    }
}
