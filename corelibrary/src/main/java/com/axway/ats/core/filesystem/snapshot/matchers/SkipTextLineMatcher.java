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
package com.axway.ats.core.filesystem.snapshot.matchers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;

/**
 * Used to decide whether some TEXT lines are to be skipped due to some content
 */
public class SkipTextLineMatcher extends SkipContentMatcher {

    private static final Logger     log = LogManager.getLogger(SkipTextLineMatcher.class);

    // < matcher, match type >
    private Map<String, MATCH_TYPE> matchersMap;

    public SkipTextLineMatcher( String directoryAlias, String filePath ) {

        super(directoryAlias, filePath);

        this.matchersMap = new HashMap<>();
    }

    public String getDirectoryAlias() {

        return directoryAlias;
    }

    public Map<String, MATCH_TYPE> getMatchersMap() {

        return matchersMap;
    }

    public SkipTextLineMatcher addLineCondition( String matcher, MATCH_TYPE type ) {

        log.debug("File " + filePath + ": Adding a matcher " + matcher + " for " + getDescription()
                  + " matched as '" + type + "'");
        matchersMap.put(matcher, type);
        return this;
    }

    public void process( String thisSnapshot, String thatSnapshot, List<String> thisLines,
                         List<String> thatLines, FileTrace fileTrace ) {

        if (matchersMap.size() > 0) {
            processLines(thisSnapshot, thisLines);
            processLines(thatSnapshot, thatLines);
        }
    }

    private void processLines( String snapshot, List<String> lines ) {

        // remove matching lines
        Iterator<String> it = lines.iterator();
        while (it.hasNext()) {
            String line = it.next();
            if (isLineSkipped(snapshot, line)) {
                it.remove();
            }
        }
    }

    private boolean isLineSkipped( String snapshot, String lineFile ) {

        for (Entry<String, MATCH_TYPE> matcherEntry : matchersMap.entrySet()) {
            String lineMatcher = matcherEntry.getKey();
            MATCH_TYPE matchType = matcherEntry.getValue();

            if (matchType.isRegex() && lineFile.matches(lineMatcher)) {// regular expression
                if (log.isDebugEnabled()) {
                    log.debug("[" + snapshot + "] File " + filePath + ": Removing line " + lineMatcher
                              + " as it matches the '" + matcherEntry.getKey() + "' regular expression");
                }
                return true;
            } else if (matcherEntry.getValue().isPlainText()
                       && lineFile.trim().equalsIgnoreCase(lineMatcher.trim())) { // just text
                if (log.isDebugEnabled()) {
                    log.debug("[" + snapshot + "] File " + filePath + ": Removing line " + lineMatcher
                              + " as equals ignoring case the '" + matcherEntry.getKey()
                              + "' regular expression");
                }
                return true;
            } else if (matcherEntry.getValue().isContainingText()
                       && lineFile.trim().toLowerCase().contains(matcherEntry.getKey()
                                                                             .trim()
                                                                             .toLowerCase())) { // just text
                if (log.isDebugEnabled()) {
                    log.debug("[" + snapshot + "] File " + filePath + ": Removing line " + lineMatcher
                              + " as it contains ignoring case the '" + matcherEntry.getKey()
                              + "' regular expression");
                }
                return true;
            }
        }

        return false;
    }

    private String getDescription() {

        return "skip text line matcher";
    }
}
