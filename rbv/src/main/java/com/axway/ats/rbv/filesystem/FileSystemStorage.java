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

import com.axway.ats.rbv.storage.Matchable;
import com.axway.ats.rbv.storage.SearchTerm;
import com.axway.ats.rbv.storage.Storage;

public class FileSystemStorage implements Storage {

    public static final String LOCAL_AGENT = "localhost:0000";

    private String             atsAgent;

    public FileSystemStorage() {

        this.atsAgent = LOCAL_AGENT;
    }

    public FileSystemStorage( String atsAgent ) {

        this.atsAgent = atsAgent;
    }

    public Matchable getFolder(
                                SearchTerm searchTerm ) {

        FileSystemFolderSearchTerm folderSearchTerm = ( FileSystemFolderSearchTerm ) searchTerm;
        return new FileSystemFolder( atsAgent,
                                     folderSearchTerm.getPath(),
                                     folderSearchTerm.getFileName(),
                                     folderSearchTerm.isRegExp(),
                                     folderSearchTerm.isIncludeDirs() );
    }

}
