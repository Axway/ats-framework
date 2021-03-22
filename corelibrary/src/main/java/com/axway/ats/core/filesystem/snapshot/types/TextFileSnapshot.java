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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.filesystem.snapshot.equality.FileSystemEqualityState;
import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;
import com.axway.ats.core.filesystem.snapshot.matchers.FindRules;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipContentMatcher.MATCH_TYPE;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipTextLineMatcher;

/**
 * Compares the content of Text files
 */
public class TextFileSnapshot extends ContentFileSnapshot {

    private static final Logger       log              = LogManager.getLogger(TextFileSnapshot.class);

    private static final long         serialVersionUID = 1L;

    private List<SkipTextLineMatcher> matchers         = new ArrayList<>();

    public TextFileSnapshot( SnapshotConfiguration configuration, String path, FindRules fileRule,
                             List<SkipTextLineMatcher> fileMatchers ) {
        super(configuration, path, fileRule);

        if (fileMatchers == null) {
            fileMatchers = new ArrayList<SkipTextLineMatcher>();
        }

        for (SkipTextLineMatcher matcher : fileMatchers) {
            this.matchers.add(matcher);
        }
    }

    TextFileSnapshot( String path, long size, long timeModified, String md5, String permissions ) {
        super(path, size, timeModified, md5, permissions);
    }

    /**
     * Used to extend a regular file snapshot instance to the wider properties snapshot instance.
     * It adds all content check matchers
     * 
     * @param fileSnapshot the instance to extend
     * @return the extended instance
     */
    public TextFileSnapshot getNewInstance( FileSnapshot fileSnapshot ) {

        TextFileSnapshot instance = new TextFileSnapshot(fileSnapshot.path, fileSnapshot.size,
                                                         fileSnapshot.timeModified, fileSnapshot.md5,
                                                         fileSnapshot.permissions);
        instance.matchers = this.matchers;

        return instance;
    }

    @Override
    public void compare( FileSnapshot that, FileSystemEqualityState equality, FileTrace fileTrace ) {

        // first compare the regular file attributes
        fileTrace = super.compareFileAttributes(that, fileTrace, true);

        // now compare the files content

        // load the files
        List<String> thisLines = loadTextFile(equality.getFirstAtsAgent(), this.getPath());
        List<String> thatLines = loadTextFile(equality.getSecondAtsAgent(), that.getPath());

        // remove matched lines
        // we currently call all matchers on both files,
        // so it does not matter if a matcher is provided for first or second snapshot
        for (SkipTextLineMatcher matcher : this.matchers) {
            matcher.process(fileTrace.getFirstSnapshot(), fileTrace.getSecondSnapshot(), thisLines,
                            thatLines, fileTrace);
        }
        for (SkipTextLineMatcher matcher : ((TextFileSnapshot) that).matchers) {
            matcher.process(fileTrace.getFirstSnapshot(), fileTrace.getSecondSnapshot(), thisLines,
                            thatLines, fileTrace);
        }

        // now compare rest of the lines

        // first merge both lists into one containing all unique lines
        Set<String> allLines = new HashSet<>();
        allLines.addAll(thisLines);
        allLines.addAll(thatLines);
        for (String line : allLines) {
            if (!thisLines.contains(line)) {
                // line not present in THIS list
                fileTrace.addDifference("Presence of line " + line, "NO", "YES");
            } else if (!thatLines.contains(line)) {
                // line not present in THAT list
                fileTrace.addDifference("Presence of line " + line, "YES", "NO");
            } else {
                // line present in both lists
                // just go on
            }
        }

        if (fileTrace.hasDifferencies()) {
            // files are different
            equality.addDifference(fileTrace);
        } else {
            log.debug("Same files: " + this.getPath() + " and " + that.getPath());
        }
    }

    /**
     * Load TEXT file content
     * 
     * @param agent agent file is located at
     * @param filePath full file path
     * @return list of file non empty lines
     */
    private List<String> loadTextFile( String agent, String filePath ) {

        // load the file as a String
        String fileContent = loadFileContent(agent, filePath);

        // parse the file content into a list
        List<String> lines = new ArrayList<>();
        for (String line : fileContent.split("\\r?\\n")) {
            line = line.trim();

            if (line.length() > 0) {
                // non empty line
                lines.add(line);
            }
        }

        return lines;
    }

    public String getFileType() {

        return "text file";
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("text ");
        sb.append(super.toString());
        for (SkipTextLineMatcher matcher : matchers) {
            for (Entry<String, MATCH_TYPE> linesMap : matcher.getMatchersMap().entrySet()) {
                sb.append("\n\tline " + linesMap.getKey() + " matched by "
                          + linesMap.getValue().toString());
            }
        }
        return sb.toString();
    }
}
