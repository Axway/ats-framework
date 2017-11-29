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
package com.axway.ats.core.filesystem.exceptions;

import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.filesystem.model.FileAttributes;

/**
 * This exception is thrown when an attribute is not supported on the
 * given operating system and its value cannot be set or get
 */
@SuppressWarnings("serial")
public class AttributeNotSupportedException extends FileSystemOperationException {

    /**
     * Constructor
     *
     * @param fileAttribute the fileAttribute which is not supported
     * @param osType the operating system on which the attribute is not supported
     */
    public AttributeNotSupportedException( FileAttributes fileAttribute,
                                           OperatingSystemType osType ) {

        super( "Attribute " + fileAttribute + " is not supported on operating system " + osType );
    }
}
