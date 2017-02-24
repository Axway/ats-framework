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
package com.axway.ats.uiengine.utilities.mobile;

import java.util.Date;

public class FileInfo {

    private String  name;
    private String  path;
    private boolean isDirectory = false;
    private long    size        = -1l;
    private Date    modificationDate;

    public FileInfo( String name,
                     String path,
                     boolean isDirectory ) {

        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
    }

    public String getName() {

        return name;
    }

    public boolean isDirectory() {

        return isDirectory;
    }

    public long getSize() {

        return size;
    }

    public void setSize(
                         long size ) {

        this.size = size;
    }

    public Date getModificationDate() {

        return modificationDate;
    }

    public void setModificationDate(
                                     Date modificationDate ) {

        this.modificationDate = modificationDate;
    }

    public String getPath() {

        return path;
    }

    @Override
    public String toString() {

        return ( isDirectory
                            ? "Directory: "
                            : "File: " ) + name + ( size > -1l
                                                              ? ", Size: " + size
                                                              : "" )
               + ( modificationDate != null
                                           ? ", Mod.Date: " + modificationDate
                                           : "" ) + ", Path: " + path;
    }

}
