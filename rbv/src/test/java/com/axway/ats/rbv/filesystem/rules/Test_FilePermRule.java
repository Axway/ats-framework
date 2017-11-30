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
import com.axway.ats.rbv.filesystem.rules.FilePermRule;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

@RunWith( PowerMockRunner.class)
@PrepareForTest( { FilePermRule.class })
public class Test_FilePermRule extends BaseTest {

    public static String             permissions = "777";
    public static String             filePath    = "/tmp/test/permissions.txt";
    public static FilePackage        pack;
    public static FileSystemMetaData meta;

    @Before
    public void setUpTest_FilePermRule() throws PackageException, RbvException {

        pack = createMock(FilePackage.class);
        meta = createMock(FileSystemMetaData.class);
    }

    @Test
    public void isMatchConstructorWithPermissionPositive() throws Exception {

        expect(pack.getPermissions()).andReturn(FilePackage.ATTRIBUTE_NOT_SUPPORTED);

        replayAll();

        FilePermRule rule = new FilePermRule(FilePackage.ATTRIBUTE_NOT_SUPPORTED,
                                             "isMatchConstructorWithPermissionPositive",
                                             true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructorWithPermissionNegative() throws Exception {

        expect(pack.getPermissions()).andReturn(123l);

        replayAll();

        FilePermRule rule = new FilePermRule(456l, "isMatchConstructorWithPermissionNegative", true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructorWithPermissionExpectFalsePositive() throws Exception {

        expect(pack.getPermissions()).andReturn(123l);

        replayAll();

        FilePermRule rule = new FilePermRule(456l,
                                             "isMatchConstructorWithPermissionExpectFalsePositive",
                                             false);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructorWithPermissionExpectFalseNegative() throws Exception {

        expect(pack.getPermissions()).andReturn(FilePackage.ATTRIBUTE_NOT_SUPPORTED);

        replayAll();

        FilePermRule rule = new FilePermRule(FilePackage.ATTRIBUTE_NOT_SUPPORTED,
                                             "isMatchConstructorWithPermissionExpectFalseNegative",
                                             false);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchNullMetaDataContent() throws Exception {

        expect(pack.getPermissions()).andReturn(123l);

        replayAll();

        FilePermRule rule = new FilePermRule(123l, "isMatchNullMetaDataContent", true);
        MetaData metaData = new FileSystemMetaData(null);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchInvalidMetaDataContent() throws Exception {

        expect(pack.getPermissions()).andReturn(123l);

        replayAll();

        FilePermRule rule = new FilePermRule(123l, "isMatchInvalidMetaDataContent", true);
        MetaData metaData = new MetaData();
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test( expected = RbvException.class)
    public void isMatchNullMetaData() throws Exception {

        expect(pack.getPermissions()).andReturn(123l);

        replayAll();

        FilePermRule rule = new FilePermRule(123l, "isMatchNullMetaData", true);
        assertFalse(rule.isMatch(null));

        verifyAll();
    }

    @Test
    public void constructWithMachine() throws Exception {

        expectNew(FilePackage.class, null, null, filePath).andReturn(pack);
        expect(pack.getPermissions()).andReturn(123l);

        replayAll();

        FilePermRule rule = new FilePermRule(null, filePath, "constructWithMachine", true);
        assertTrue(rule != null);

        verifyAll();
    }

    @Test
    public void constructWithNullMachine() throws Exception {

        expectNew(FilePackage.class, null, null, filePath).andReturn(pack);
        expect(pack.getPermissions()).andReturn(123l);

        replayAll();

        FilePermRule rule = new FilePermRule(null, filePath, "constructWithNullMachine", true);
        assertTrue(rule != null);

        verifyAll();
    }
}
