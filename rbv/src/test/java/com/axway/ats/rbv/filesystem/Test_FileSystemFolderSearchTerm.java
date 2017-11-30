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
package com.axway.ats.rbv.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.filesystem.FileSystemFolderSearchTerm;

public class Test_FileSystemFolderSearchTerm extends BaseTest {

    @Test
    public void constructorWithNoFolder() {

        FileSystemFolderSearchTerm searchTerm = new FileSystemFolderSearchTerm("/test/", null, true);
        assertEquals("/test/", searchTerm.getPath());
        assertEquals(true, searchTerm.isIncludeDirs());
    }

    @Test
    public void constructorWithFolder() {

        FileSystemFolderSearchTerm searchTerm = new FileSystemFolderSearchTerm("/test/", null, true, false);
        assertEquals("/test/", searchTerm.getPath());
        assertEquals(false, searchTerm.isIncludeDirs());
    }
}
