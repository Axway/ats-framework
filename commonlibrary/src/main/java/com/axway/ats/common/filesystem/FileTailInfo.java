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
package com.axway.ats.common.filesystem;

import com.axway.ats.common.PublicAtsApi;

import java.io.Serializable;

/**
 * Contains composite data about partial read file operation. The most suitable use case is to read new file content from
 * log file. Check information of retrieved data from the getters.
 * <p>Also check FileSystemOperations.readFile().</p>
 */
@PublicAtsApi
public class FileTailInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private long              currentPosition  = 0l;
    private boolean           isFileRotated    = false;
    private String            newContent;

    public FileTailInfo( long currentPosition,
                         boolean isFileRotated,
                         String newContent ) {

        this.currentPosition = currentPosition;
        this.isFileRotated = isFileRotated;
        this.newContent = newContent;
    }

    /**
     * Position/offset from where <em>next</em> read operation should start.
     * @return position for next read
     */
    @PublicAtsApi
    public long getCurrentPosition() {

        return currentPosition;
    }

    /**
     * Flag whether file rotation is detected. Proper detection is not guaranteed because file might have been rotated
     * and the new size is already bigger than the seek position passed.
     * @return <em>true</em> if file rotation is detected, i.e. current file size is smaller than the previous one.
     */
    @PublicAtsApi
    public boolean isFileRotated() {

        return isFileRotated;
    }

    /**
     * Content of the file after the last seek position.
     * @return new part content
     */
    @PublicAtsApi
    public String getNewContent() {

        return newContent;
    }
}
