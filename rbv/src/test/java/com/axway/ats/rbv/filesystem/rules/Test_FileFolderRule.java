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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.filesystem.rules.FileFolderRule;
import com.axway.ats.rbv.model.RbvException;
@SuppressWarnings( "boxing")
@RunWith( PowerMockRunner.class)
public class Test_FileFolderRule extends BaseTest {

    public static FileFolderRule     rule;
    public static FileSystemMetaData meta;
    public static FilePackage        pack;

    @Before
    public void setUpTest_FilePathRule() {

        meta = createMock(FileSystemMetaData.class);
        pack = createMock(FilePackage.class);
    }

    @Test
    public void getRuleDescFile() {

        rule = new FileFolderRule(true, "ruleName", true);

        assertEquals(rule.getRuleDescription(), "which expects a 'file'");
    }

    @Test
    public void getRuleDescFolder() {

        rule = new FileFolderRule(false, "ruleName", true);

        assertEquals(rule.getRuleDescription(), "which expects a 'folder'");
    }

    @Test
    public void getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(FileSystemMetaData.FILE_PACKAGE);

        rule = new FileFolderRule(false, "ruleName", true);

        assertEquals(rule.getMetaDataKeys(), metaKeys);
    }

    @Test
    public void performMatch() throws RbvException, PackageException {

        expect(meta.getFilePackage()).andReturn(pack);
        expect(pack.isFile()).andReturn(true);

        replayAll();

        rule = new FileFolderRule(true, "ruleName", true);
        assertTrue(rule.performMatch(meta));

        verifyAll();
    }

    @Test
    public void performMatchNegative() throws RbvException, PackageException {

        expect(meta.getFilePackage()).andReturn(pack);
        expect(pack.isFile()).andReturn(false);

        replayAll();

        rule = new FileFolderRule(true, "ruleName", true);
        assertFalse(rule.performMatch(meta));

        verifyAll();
    }

    @Test( expected = RbvException.class)
    public void performMatchException() throws RbvException, PackageException {

        expect(meta.getFilePackage()).andReturn(pack);
        expect(pack.isFile()).andThrow(new PackageException(""));

        replayAll();

        rule = new FileFolderRule(true, "ruleName", true);
        assertTrue(rule.performMatch(meta));

        verifyAll();
    }
}
