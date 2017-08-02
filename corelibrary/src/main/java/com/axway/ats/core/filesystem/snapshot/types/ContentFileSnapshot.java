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
package com.axway.ats.core.filesystem.snapshot.types;

import java.lang.reflect.Method;

import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;
import com.axway.ats.core.filesystem.snapshot.matchers.FindRules;

public abstract class ContentFileSnapshot extends FileSnapshot {

    private static final long serialVersionUID = 1L;

    public ContentFileSnapshot( SnapshotConfiguration configuration, String path, FindRules fileRule ) {
        // we are interested in file content
        // do not check MD5 and size
        super( configuration.setCheckMD5( false ).setCheckSize( false ), path, fileRule );
    }

    ContentFileSnapshot( String path, long size, long timeModified, String md5, String permissions ) {
        super( path, size, timeModified, md5, permissions );

        if( configuration != null ) {
            configuration.setCheckMD5( false );
            configuration.setCheckSize( false );
        }
    }

    /**
     * Load the file as a String
     * 
     * @param agent agent file is located at
     * @param filePath full file path
     * @return the file content
     */
    protected String loadFileContent( String agent, String filePath ) {

        String fileContent;
        if( agent == null ) {
            // It is a local file
            fileContent = new LocalFileSystemOperations().readFile( filePath, "UTF-8" );
        } else {
            // It is a remote file.
            // As we need to use Action Library code in order to get the file content, here we use
            // java reflection, so do not need to introduce compile dependency
            try {
                Class<?> fileSystemOperationsClass = Class.forName( "com.tumbleweed.automation.actions.filesystem.RemoteFileSystemOperations" );
                Object fileSystemOperationsInstance = fileSystemOperationsClass.getConstructor( String.class )
                                                                               .newInstance( agent );

                //call the printIt method
                Method readFileMethod = fileSystemOperationsClass.getDeclaredMethod( "readFile", String.class,
                                                                                     String.class );
                fileContent = readFileMethod.invoke( fileSystemOperationsInstance, filePath, "UTF-8" )
                                            .toString();
            } catch( Exception e ) {
                // this will cancel the comparison
                // the other option is to add a difference to the FileTrace object, instead of throwing an exception here
                throw new FileSystemOperationException( "Error loading '" + filePath + "' file from "
                                                        + agent );
            }
        }

        return fileContent;
    }
}
