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
package com.axway.ats.common.io;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MimeTypeFileExtensionMapper {

    private final static Logger     log                     = LogManager.getLogger(MimeTypeFileExtensionMapper.class);

    public final static String      GZIP_CONTENT_ENCODING   = "gzip";
    public final static String      GZIP_FILE_EXTENSION     = "gzip";

    private final static Properties contentTypeFileExtProps = new Properties() {
                                                                private static final long serialVersionUID = 1L;
                                                                {
                                                                    try {
                                                                        loadFromXML(MimeTypeFileExtensionMapper.class.getResourceAsStream("MimeTypeFileExtensionMapper.xml"));
                                                                    } catch (Exception e) {
                                                                        log.error("Could not load file with mime types to files extension mappings",
                                                                                  e);
                                                                    }
                                                                }
                                                            };

    public static String getFileExtension(
                                           String contentType ) {

        return getFileExtension(contentType, null);
    }

    public static String getFileExtension(
                                           String contentType,
                                           String contentEncoding ) {

        if (contentType == null) {
            return null;
        }
        int columnIdx = contentType.indexOf(';');
        if (columnIdx > 0) {
            contentType = contentType.substring(0, columnIdx).trim().toLowerCase();
        } else {
            contentType = contentType.toLowerCase();
        }
        contentEncoding = (contentEncoding != null)
                                                    ? contentEncoding.toLowerCase()
                                                    : null;
        String fileExt = contentTypeFileExtProps.getProperty(contentType);
        if (fileExt == null) {
            return null;
        }
        StringBuilder resultStr = new StringBuilder(fileExt);
        if (GZIP_CONTENT_ENCODING.equals(contentEncoding)) {
            resultStr.append(".");
            resultStr.append(GZIP_FILE_EXTENSION);
        }
        return resultStr.toString();
    }

}
