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

import java.io.File;

import com.axway.ats.common.filesystem.FileSystemOperationException;

/**
 * Exception thrown when a file does not exist
 */
@SuppressWarnings( "serial")
public class FileDoesNotExistException extends FileSystemOperationException {

    /**
     * Constructor
     *
     * @param fileName name of the file which does not exist
     */
    public FileDoesNotExistException( String fileName ) {

        super("File '" + fileName + "' does not exist");
    }

    /**
     * Constructor
     *
     * @param file the file which does not exist
     */
    public FileDoesNotExistException( File file ) {

        super("File '" + file.getPath() + "' does not exist");
    }
}
