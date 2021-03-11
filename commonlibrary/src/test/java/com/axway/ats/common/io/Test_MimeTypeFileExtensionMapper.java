/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.common.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class Test_MimeTypeFileExtensionMapper {

    @Test
    public void propertiesAreLoadedAndThereIsValue() {

        assertEquals("html", MimeTypeFileExtensionMapper.getFileExtension("text/html"));
    }

    @Test
    public void nullContentTypeReturnsNull() {

        assertNull(MimeTypeFileExtensionMapper.getFileExtension(null, "alabala"));
    }

    @Test
    public void gzipEncodingIsHonored() {

        assertEquals("html.gzip", MimeTypeFileExtensionMapper.getFileExtension("text/html", "gzip"));
    }

    @Test
    public void illegalContentTypeReturnsNull() {

        assertEquals(null, MimeTypeFileExtensionMapper.getFileExtension("blabla/html", "gzip"));
    }

    @Test
    public void unknownContentEncodingKeepsExtension() {

        assertEquals("html", MimeTypeFileExtensionMapper.getFileExtension("text/html", "alabala"));
    }
}
