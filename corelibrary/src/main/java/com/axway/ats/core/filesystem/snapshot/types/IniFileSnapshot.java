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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.filesystem.snapshot.equality.FileSystemEqualityState;
import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;
import com.axway.ats.core.filesystem.snapshot.matchers.FindRules;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipContentMatcher.MATCH_TYPE;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipIniMatcher;

/**
 * Compares the content of INI files
 */
public class IniFileSnapshot extends ContentFileSnapshot {

    private static final Logger  log              = LogManager.getLogger(IniFileSnapshot.class);

    private static final long    serialVersionUID = 1L;

    private List<SkipIniMatcher> matchers         = new ArrayList<>();

    private enum StartCharType {
        SECTION, COMMENT, PROPERTY
    }

    public IniFileSnapshot( SnapshotConfiguration configuration, String path, FindRules fileRule,
                            List<SkipIniMatcher> fileMatchers ) {
        super(configuration, path, fileRule);

        if (fileMatchers == null) {
            fileMatchers = new ArrayList<SkipIniMatcher>();
        }

        for (SkipIniMatcher matcher : fileMatchers) {
            this.matchers.add(matcher);
        }
    }

    IniFileSnapshot( String path, long size, long timeModified, String md5, String permissions ) {
        super(path, size, timeModified, md5, permissions);
    }

    /**
     * Used to extend a regular file snapshot instance to the wider properties snapshot instance.
     * It adds all content check matchers
     * 
     * @param fileSnapshot the instance to extend
     * @return the extended instance
     */
    public IniFileSnapshot getNewInstance( FileSnapshot fileSnapshot ) {

        IniFileSnapshot instance = new IniFileSnapshot(fileSnapshot.path, fileSnapshot.size,
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
        Map<String, Map<String, String>> thisPropertiesMap = loadIniFile(equality.getFirstAtsAgent(),
                                                                         this.getPath());
        Map<String, Map<String, String>> thatPropertiesMap = loadIniFile(equality.getSecondAtsAgent(),
                                                                         that.getPath());

        // remove matched properties
        // we currently call all matchers on both files,
        // so it does not matter if a matcher is provided for first or second snapshot
        for (SkipIniMatcher matcher : this.matchers) {
            matcher.process(fileTrace.getFirstSnapshot(), fileTrace.getSecondSnapshot(), thisPropertiesMap,
                            thatPropertiesMap, fileTrace);
        }
        for (SkipIniMatcher matcher : ((IniFileSnapshot) that).matchers) {
            matcher.process(fileTrace.getFirstSnapshot(), fileTrace.getSecondSnapshot(), thisPropertiesMap,
                            thatPropertiesMap, fileTrace);
        }

        // now compare rest of the properties
        Set<String> sections = new HashSet<>();
        sections.addAll(thisPropertiesMap.keySet());
        sections.addAll(thatPropertiesMap.keySet());
        for (String section : sections) {
            if (!thisPropertiesMap.containsKey(section)) {
                // section not present in THIS list
                fileTrace.addDifference("Presence of section " + section, "NO", "YES");
            } else if (!thatPropertiesMap.containsKey(section)) {
                // section not present in THAT list
                fileTrace.addDifference("Presence of section " + section, "YES", "NO");
            } else {
                // section present in both lists
                // compare its content
                compareSection(section, thisPropertiesMap.get(section), thatPropertiesMap.get(section),
                               equality, fileTrace);
            }
        }

        if (fileTrace.hasDifferencies()) {
            // files are different
            equality.addDifference(fileTrace);
        } else {
            log.debug("Same files: " + this.getPath() + " and " + that.getPath());
        }
    }

    private void compareSection( String section, Map<String, String> thisProperties,
                                 Map<String, String> thatProperties, FileSystemEqualityState equality,
                                 FileTrace fileTrace ) {

        Set<String> keys = new HashSet<>();
        keys.addAll(thisProperties.keySet());
        keys.addAll(thatProperties.keySet());

        for (String key : keys) {
            if (!thisProperties.containsKey(key)) {
                // key not present in THIS list
                fileTrace.addDifference("Section " + section + ", presence of key '" + key + "'", "NO",
                                        "YES");
            } else if (!thatProperties.containsKey(key)) {
                // key not present in THAT list
                fileTrace.addDifference("Section " + section + ", presence of key '" + key + "'", "YES",
                                        "NO");
            } else {
                // key present in both lists
                String thisValue = thisProperties.get(key).trim();
                String thatValue = thatProperties.get(key).trim();

                if (!thisValue.equalsIgnoreCase(thatValue)) {
                    fileTrace.addDifference("Section " + section + ", key '" + key + "'",
                                            "'" + thisValue + "'", "'" + thatValue + "'");
                }
            }
        }
    }

    /**
     * Load INI file content
     * 
     * @param agent agent file is located at
     * @param filePath full file path
     * @return map in the following format: < INI section, <key, value> >
     */
    private Map<String, Map<String, String>> loadIniFile( String agent, String filePath ) {

        // load the file as a String
        String fileContent = loadFileContent(agent, filePath);

        // parse the file content into a map structure
        Map<String, Map<String, String>> propertiesMap = new HashMap<>();
        String section = null;
        for (String line : fileContent.split("\\r?\\n")) {
            line = line.trim();

            if (line.length() == 0) {
                // empty line
                continue;
            }

            switch (getStartCharMeaning(line.charAt(0))) {
                case SECTION:
                    // new section
                    section = line;
                    propertiesMap.put(section, new HashMap<String, String>());
                    break;

                case PROPERTY:
                    // property line for current section

                    Map<String, String> propertiesForThisSection = propertiesMap.get(section);
                    if (propertiesForThisSection == null) {
                        section = "[ATS_DEFAULT_INI_FILE_SECTION]";
                        log.warn("Currently no INI section is defined."
                                 + "\nEither the file is missing its first section or non default characters are used for beginning of section, property or comment."
                                 + "\nFile: " + filePath + "\nFollowing line will go into a section called "
                                 + section + ": '" + line + "'");
                        propertiesForThisSection = propertiesMap.get(section); // check if already used the default section name
                        if (propertiesForThisSection == null) {
                            propertiesForThisSection = new HashMap<String, String>();
                        }
                        propertiesMap.put(section, propertiesForThisSection);
                    }

                    int separatorIndex = line.indexOf(configuration.getIniFileDelimiterCharacter());
                    if (separatorIndex < 1) {
                        // no separator, we treat the whole line as a key, the value will be empty
                        propertiesForThisSection.put(line.trim(), "");
                    } else {
                        // add key and value
                        propertiesForThisSection.put(line.substring(0, separatorIndex).trim(),
                                                     line.substring(separatorIndex + 1).trim());
                    }

                    break;

                default:
                    // commented line
                    continue;
            }
        }

        return propertiesMap;
    }

    private StartCharType getStartCharMeaning( char startChar ) {

        if (startChar == configuration.getIniFileStartSectionCharacter()) {
            return StartCharType.SECTION;
        }

        if (startChar == configuration.getIniFileStartCommentCharacter()) {
            return StartCharType.COMMENT;
        }

        return StartCharType.PROPERTY;
    }

    public String getFileType() {

        return "ini file";
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("ini ");
        sb.append(super.toString());
        for (SkipIniMatcher matcher : matchers) {
            for (Entry<String, MATCH_TYPE> sectionsMap : matcher.getSectionsMap().entrySet()) {
                sb.append("\n\tsection " + sectionsMap.getKey() + " matched by "
                          + sectionsMap.getValue().toString());
            }

            Map<String, Map<String, MATCH_TYPE>> keysMap = matcher.getKeysMap();
            Map<String, Map<String, MATCH_TYPE>> valuesMap = matcher.getValuesMap();
            if (keysMap.size() + valuesMap.size() > 0) {
                sb.append("\n\tproperties:");
            }

            if (keysMap.size() > 0) {
                for (Entry<String, Map<String, MATCH_TYPE>> sectionEntity : keysMap.entrySet()) {
                    for (Entry<String, MATCH_TYPE> propertyEntity : sectionEntity.getValue().entrySet()) {
                        sb.append("\n\t\t" + sectionEntity.getKey() + " key '" + propertyEntity.getKey()
                                  + "' matched by " + propertyEntity.getValue().toString());
                    }
                }
            }

            if (valuesMap.size() > 0) {
                for (Entry<String, Map<String, MATCH_TYPE>> sectionEntity : valuesMap.entrySet()) {
                    for (Entry<String, MATCH_TYPE> propertyEntity : sectionEntity.getValue().entrySet()) {
                        sb.append("\n\t\t" + sectionEntity.getKey() + " value '" + propertyEntity.getKey()
                                  + "' matched by " + propertyEntity.getValue().toString());
                    }
                }
            }
        }
        return sb.toString();
    }
}
