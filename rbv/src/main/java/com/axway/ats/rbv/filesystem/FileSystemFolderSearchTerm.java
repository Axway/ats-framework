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

import com.axway.ats.rbv.storage.SearchTerm;

public class FileSystemFolderSearchTerm implements SearchTerm {

    private String  path;
    private String  fileName;
    private boolean isRegExp;
    private boolean includeDirs;

    public FileSystemFolderSearchTerm( String path,
                                       String fileName,
                                       boolean isRegExp ) {

        this( path, fileName, isRegExp, true );
    }

    public FileSystemFolderSearchTerm( String path,
                                       String fileName,
                                       boolean isRegExp,
                                       boolean includeSubDirs ) {

        this.path = path;
        this.fileName = fileName;
        this.isRegExp = isRegExp;
        this.includeDirs = includeSubDirs;
    }

    public boolean isIncludeDirs() {

        return includeDirs;
    }

    public String getPath() {

        return path;
    }

    public String getFileName() {

        return fileName;
    }

    public boolean isRegExp() {

        return isRegExp;
    }

}
