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

import java.io.Serializable;

import com.axway.ats.common.PublicAtsApi;

/**
 * Contains a basic information for a text match in file.
 */
@PublicAtsApi
public class FileMatchInfo implements Serializable {

    private static final long serialVersionUID     = 1L;

    /**
     * number of matched lines
     */
    @PublicAtsApi
    public int                numberOfMatchedLines = 0;

    /**
     * if there were any matches
     */
    @PublicAtsApi
    public boolean            matched;

    /**
     * the sequence of the last read byte
     */
    @PublicAtsApi
    public long               lastReadByte         = 0l;

    /**
     * the index of the last read line
     */
    @PublicAtsApi
    public int                lastReadLineNumber   = 0;

    /**
     * indexes of the matched lines from the file beginning
     */
    @PublicAtsApi
    public Integer[]          lineNumbers;

    /**
     * matched lines
     */
    @PublicAtsApi
    public String[]           lines;

    /**
     * matched patterns
     */
    @PublicAtsApi
    public String[]           matchedPatterns;

    public FileMatchInfo( int numberOfMatchedLines, int lastReadLineNumber, long lastReadByte, String[] lines,
                          Integer[] lineNumbers, String[] matchedPatterns ) {

        this.numberOfMatchedLines = numberOfMatchedLines;
        this.matched = numberOfMatchedLines > 0;
        this.lastReadLineNumber = lastReadLineNumber;
        this.lastReadByte = lastReadByte;
        this.lines = lines;
        this.lineNumbers = lineNumbers;
        this.matchedPatterns = matchedPatterns;
    }
}
