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
package com.axway.ats.core.ssh.model;

public class FileEntry {

    private String  name;
    private String  path;
    private String  parentPath;
    private boolean isDirectory;
    private long    lastModificationTime;
    private long    size;

    /**
     * 
     * @param name file or directory
     * @param path the file/directory path
     * @param isDirectory whether it is a directory or not
     */
    public FileEntry( String name,
                      String path,
                      boolean isDirectory ) {

        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
    }

    /**
     * 
     * @return the file/directory name
     */
    public String getName() {

        return name;
    }

    /**
     * 
     * @param name the file/directory name
     */
    public void setName(
                         String name ) {

        this.name = name;
    }

    /**
     * 
     * @return the file/directory path
     */
    public String getPath() {

        return path;
    }

    /**
     * 
     * @param path the file/directory path
     */
    public void setPath(
                         String path ) {

        this.path = path;
    }

    /**
     * 
     * @return the file/directory parent path
     */
    public String getParentPath() {

        return parentPath;
    }

    /**
     * 
     * @param parentPath the file/directory parent path
     */
    public void setParentPath(
                               String parentPath ) {

        this.parentPath = parentPath;
    }

    /**
     * 
     * @return <code>true</code> if it is a directory
     */
    public boolean isDirectory() {

        return isDirectory;
    }

    /**
     * 
     * @param isDirectory whether it is a directory or not
     */
    public void setDirectory(
                              boolean isDirectory ) {

        this.isDirectory = isDirectory;
    }

    /**
     * 
     * @return the last modification time
     */
    public long getLastModificationTime() {

        return lastModificationTime;
    }

    /**
     * 
     * @param lastModificationTime the last modification time
     */
    public void setLastModificationTime(
                                         long lastModificationTime ) {

        this.lastModificationTime = lastModificationTime;
    }

    /**
     * 
     * @return the size in bytes
     */
    public long getSize() {

        return size;
    }

    /**
     * 
     * @param size the size in bytes
     */
    public void setSize(
                         long size ) {

        this.size = size;
    }

}
