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
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.filesystem.rules.FileGidRule;
import com.axway.ats.rbv.model.RbvException;

@SuppressWarnings( "boxing")
@RunWith( PowerMockRunner.class)
@PrepareForTest( { FileGidRule.class })
public class Test_FileGIDRule extends BaseTest {

    public static FileGidRule        rule;
    public static FileSystemMetaData meta;
    public static FilePackage        pack;

    @Before
    public void setUpTest_FilePathRule() {

        meta = createMock(FileSystemMetaData.class);
        pack = createMock(FilePackage.class);
    }

    @Test
    public void getRuleDescFile() {

        rule = new FileGidRule(1, "ruleName", true);

        assertEquals(rule.getRuleDescription(), "which expects file with GID '1'");
    }

    @Test
    public void getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(FileSystemMetaData.FILE_PACKAGE);

        rule = new FileGidRule(1, "ruleName", true);

        assertEquals(rule.getMetaDataKeys(), metaKeys);
    }

    @Test
    public void performMatch() throws RbvException, PackageException {

        expect(meta.getFilePackage()).andReturn(pack);
        expect(pack.getGid()).andReturn(1L);

        replayAll();

        rule = new FileGidRule(1, "ruleName", true);
        assertTrue(rule.performMatch(meta));

        verifyAll();
    }

    @Test
    public void performMatchNotSupported() throws RbvException, PackageException {

        expect(meta.getFilePackage()).andReturn(pack);
        expect(pack.getGid()).andReturn(FilePackage.ATTRIBUTE_NOT_SUPPORTED);

        replayAll();

        rule = new FileGidRule(1, "ruleName", true);
        assertTrue(rule.performMatch(meta));

        verifyAll();
    }

    @Test( expected = RbvException.class)
    public void performMatchNegativeWxception() throws RbvException, PackageException {

        expect(meta.getFilePackage()).andReturn(pack);
        expect(pack.getGid()).andThrow(new PackageException(""));

        replayAll();

        rule = new FileGidRule(1, "ruleName", true);
        rule.performMatch(meta);

        verifyAll();
    }

    @Test
    public void performMatchOtherFile() throws Exception {

        FilePackage remotePack = createMock(FilePackage.class);

        expectNew(FilePackage.class, null, null, "/root/file.name").andReturn(remotePack);
        expect(remotePack.getGid()).andReturn(1L);

        expect(meta.getFilePackage()).andReturn(pack);
        expect(pack.getGid()).andReturn(1L);

        replayAll();

        rule = new FileGidRule(null, "/root/file.name", "ruleName", true);
        assertTrue(rule.performMatch(meta));

        verifyAll();
    }

    @Test
    public void performMatchOtherFileNull() throws Exception {

        FilePackage remotePack = createMock(FilePackage.class);

        expectNew(FilePackage.class, null, null, "/root/file.name").andReturn(remotePack);
        expect(remotePack.getGid()).andReturn(1L);

        expect(meta.getFilePackage()).andReturn(pack);
        expect(pack.getGid()).andReturn(1L);

        replayAll();

        rule = new FileGidRule(null, "/root/file.name", "ruleName", true);
        assertTrue(rule.performMatch(meta));

        verifyAll();
    }

    @Test( expected = RbvException.class)
    public void performMatchOtherFileException() throws Exception {

        FilePackage remotePack = createMock(FilePackage.class);

        expectNew(FilePackage.class, null, null, "/root/file.name").andReturn(remotePack);
        expect(remotePack.getGid()).andThrow(new PackageException(""));

        replayAll();

        rule = new FileGidRule(null, "/root/file.name", "ruleName", true);

        verifyAll();
    }
}
