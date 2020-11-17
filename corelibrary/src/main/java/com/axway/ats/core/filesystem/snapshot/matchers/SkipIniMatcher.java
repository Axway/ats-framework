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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;

/**
 * Used to decide whether some INI properties are to be skipped due to some
 * match in their section, key or value
 */
public class SkipIniMatcher extends SkipContentMatcher {

    private static final Logger log = LogManager.getLogger(SkipIniMatcher.class);

    public enum MATCH_ENTITY {
        SECTION, KEY, VALUE;
    }

    // < section, match type >
    private Map<String, MATCH_TYPE>              sectionsMap;
    // < section, <key, match type> >
    private Map<String, Map<String, MATCH_TYPE>> keysMap;
    // < section, <value, match type> >
    private Map<String, Map<String, MATCH_TYPE>> valuesMap;

    public SkipIniMatcher( String directoryAlias, String filePath ) {

        super(directoryAlias, filePath);

        this.sectionsMap = new HashMap<>();
        this.keysMap = new HashMap<>();
        this.valuesMap = new HashMap<>();
    }

    public String getDirectoryAlias() {

        return directoryAlias;
    }

    public Map<String, MATCH_TYPE> getSectionsMap() {

        return sectionsMap;
    }

    public Map<String, Map<String, MATCH_TYPE>> getKeysMap() {

        return keysMap;
    }

    public Map<String, Map<String, MATCH_TYPE>> getValuesMap() {

        return valuesMap;
    }

    public SkipIniMatcher addSectionCondition( String section, MATCH_TYPE type ) {

        log.debug("File " + filePath + ": Adding a whole section " + section + " for " + getDescription()
                  + " matched as '" + type + "'");
        sectionsMap.put(section, type);
        return this;
    }

    public SkipIniMatcher addKeyCondition( String section, String key, MATCH_TYPE type ) {

        Map<String, MATCH_TYPE> keysForOneSection = keysMap.get(section);
        if (keysForOneSection == null) {
            keysForOneSection = new HashMap<>();
            keysMap.put(section, keysForOneSection);
        }

        if (keysForOneSection.containsKey(key)) {
            if (keysForOneSection.get(key) != type) {
                StringBuilder sb = new StringBuilder();
                sb.append("File " + filePath + ": ");
                sb.append("There is already a " + getDescription() + " for section " + section + " and key '"
                          + key + "'. ");
                sb.append("Current key would be matched by '" + keysForOneSection.get(key) + "' ");
                sb.append("but we change this, so it will be matched by '" + type.getDescription() + "'");

                log.warn(sb.toString());
            }
        } else if (log.isDebugEnabled()) {
            log.debug("File " + filePath + ": Adding a section " + section + " for " + getDescription()
                      + " with key '" + key + "' matched as '" + type + "'");
        }

        keysForOneSection.put(key, type);

        return this;
    }

    public SkipIniMatcher addValueCondition( String section, String value, MATCH_TYPE type ) {

        Map<String, MATCH_TYPE> valuesForOneSection = valuesMap.get(section);
        if (valuesForOneSection == null) {
            valuesForOneSection = new HashMap<>();
            valuesMap.put(section, valuesForOneSection);
        }

        if (valuesForOneSection.containsKey(value)) {
            if (valuesForOneSection.get(value) != type) {
                StringBuilder sb = new StringBuilder();
                sb.append("File " + filePath + ": ");
                sb.append("There is already a " + getDescription() + " for section " + section
                          + " and value '" + value + "'. ");
                sb.append("Current value would be matched by '" + valuesForOneSection.get(value) + "' ");
                sb.append("but we change this, so it will be matched by '" + type.getDescription() + "'");

                log.warn(sb.toString());
            }
        } else if (log.isDebugEnabled()) {
            log.debug("File " + filePath + ": Adding a section " + section + " for " + getDescription()
                      + " with value '" + value + "' matched as '" + type + "'");
        }

        valuesForOneSection.put(value, type);

        return this;
    }

    public void process( String thisSnapshot, String thatSnapshot,
                         Map<String, Map<String, String>> thisPropertiesMap,
                         Map<String, Map<String, String>> thatPropertiesMap, FileTrace fileTrace ) {

        if (sectionsMap.size() > 0) {
            processSections(thisSnapshot, thisPropertiesMap);
            processSections(thatSnapshot, thatPropertiesMap);
        }

        if (keysMap.size() > 0) {
            processKeys(thisSnapshot, thisPropertiesMap);
            processKeys(thatSnapshot, thatPropertiesMap);
        }

        if (valuesMap.size() > 0) {
            processValues(thisSnapshot, thisPropertiesMap);
            processValues(thatSnapshot, thatPropertiesMap);
        }
    }

    private boolean isSectionSkipped( String snapshot, String sectionFromFile ) {

        for (Entry<String, MATCH_TYPE> matcherEntry : sectionsMap.entrySet()) {
            String section = matcherEntry.getKey();
            MATCH_TYPE matchType = matcherEntry.getValue();

            if (matchType.isRegex() && sectionFromFile.matches(section)) {// regular expression
                if (log.isDebugEnabled()) {
                    log.debug("[" + snapshot + "] File " + filePath + ": Removing section " + section
                              + " as it matches the '" + matcherEntry.getKey() + "' regular expression");
                }
                return true;
            } else if (matcherEntry.getValue().isPlainText()
                       && sectionFromFile.trim().equalsIgnoreCase(section.trim())) { // just text
                if (log.isDebugEnabled()) {
                    log.debug("[" + snapshot + "] File " + filePath + ": Removing section " + section
                              + " as equals ignoring case the '" + matcherEntry.getKey()
                              + "' regular expression");
                }
                return true;
            } else if (matcherEntry.getValue().isContainingText()
                       && sectionFromFile.trim().toLowerCase().contains(matcherEntry.getKey()
                                                                                    .trim()
                                                                                    .toLowerCase())) { // just text
                if (log.isDebugEnabled()) {
                    log.debug("[" + snapshot + "] File " + filePath + ": Removing section " + section
                              + " as it contains ignoring case the '" + matcherEntry.getKey()
                              + "' regular expression");
                }
                return true;
            }
        }

        return false;
    }

    private void addKeyToRemove( Map<String, List<String>> keysToRemoveBySection, String section,
                                 String key ) {

        List<String> keys = keysToRemoveBySection.get(section);
        if (keys == null) {
            keys = new ArrayList<>();
            keysToRemoveBySection.put(section, keys);
        }
        keys.add(key);
    }

    private void processSections( String snapshot, Map<String, Map<String, String>> props ) {

        // remove sections that are marked for removal

        Iterator<Map.Entry<String, Map<String, String>>> it = props.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Map<String, String>> entry = it.next();
            String section = entry.getKey();

            if (isSectionSkipped(snapshot, section)) {
                it.remove();
                continue;
            }
        }
    }

    private void processKeys( String snapshot, Map<String, Map<String, String>> props ) {

        // list of keys to remove(per section)
        Map<String, List<String>> keysToRemoveBySection = new HashMap<>();

        // cycle all sections from the file
        for (String section : props.keySet()) {

            // find same section in the list of matchers
            Map<String, MATCH_TYPE> keyMatchersForOneSection = keysMap.get(section);
            if (keyMatchersForOneSection == null) {
                // no matchers for this section
                continue;
            }

            // check the properties in this section
            Map<String, String> propertiesForOneSectionFromFile = props.get(section);
            for (String keyFromFile : propertiesForOneSectionFromFile.keySet()) {

                // cycle all match conditions
                for (Entry<String, MATCH_TYPE> matcherEntry : keyMatchersForOneSection.entrySet()) {

                    String keyToMatch = matcherEntry.getKey().trim();
                    MATCH_TYPE matchType = matcherEntry.getValue();

                    if (matchType.isRegex()) {
                        // regular expression
                        if (keyFromFile.matches(keyToMatch)) {
                            if (log.isDebugEnabled()) {
                                log.debug("[" + snapshot + "] File " + filePath + ", section " + section
                                          + ": Removing property '" + keyFromFile + "="
                                          + propertiesForOneSectionFromFile.get(keyFromFile)
                                          + "' as its key matches the '" + keyToMatch
                                          + "' regular expression");
                            }

                            addKeyToRemove(keysToRemoveBySection, section, keyFromFile);
                            break;
                        }
                    } else if (matchType.isPlainText()) {
                        // just text
                        if (keyFromFile.trim().equalsIgnoreCase(keyToMatch)) {
                            if (log.isDebugEnabled()) {
                                log.debug("[" + snapshot + "] File " + filePath + ", section " + section
                                          + ": Removing property '" + keyFromFile + "="
                                          + propertiesForOneSectionFromFile.get(keyFromFile)
                                          + "' as its key equals ignoring case the '" + keyToMatch
                                          + "' text");
                            }

                            addKeyToRemove(keysToRemoveBySection, section, keyFromFile);
                            break;
                        }

                    } else {
                        if (keyFromFile.trim().toLowerCase().contains(keyToMatch.toLowerCase())) {
                            if (log.isDebugEnabled()) {
                                log.debug("[" + snapshot + "] File " + filePath + ", section " + section
                                          + ": Removing property '" + keyFromFile + "="
                                          + propertiesForOneSectionFromFile.get(keyFromFile)
                                          + "' as its key contains ignoring case the '" + keyToMatch
                                          + "' text");
                            }

                            addKeyToRemove(keysToRemoveBySection, section, keyFromFile);
                            break;
                        }
                    }
                }
            }
        }

        // now remove all matched properties
        for (Entry<String, List<String>> matcherEntry : keysToRemoveBySection.entrySet()) {
            for (String key : matcherEntry.getValue()) {
                props.get(matcherEntry.getKey()).remove(key);
            }
        }
    }

    private void processValues( String snapshot, Map<String, Map<String, String>> props ) {

        // list of keys to remove(per section)
        Map<String, List<String>> keysToRemoveBySection = new HashMap<>();

        // cycle all sections from the file
        for (String section : props.keySet()) {

            // find same section in the list of matchers
            // <property value, match type>
            Map<String, MATCH_TYPE> valueMatchersForOneSection = valuesMap.get(section);
            if (valueMatchersForOneSection == null) {
                // no matchers for this section
                continue;
            }

            // check the properties in this section
            Map<String, String> propertiesForOneSectionFromFile = props.get(section);
            for (String keyFromFile : propertiesForOneSectionFromFile.keySet()) {
                String valueFromFile = propertiesForOneSectionFromFile.get(keyFromFile).trim();

                // cycle all match conditions
                for (Entry<String, MATCH_TYPE> matcherEntry : valueMatchersForOneSection.entrySet()) {
                    String valueToMatch = matcherEntry.getKey().trim();
                    MATCH_TYPE matchType = matcherEntry.getValue();

                    if (matchType.isRegex()) {
                        // regular expression
                        if (valueFromFile.matches(valueToMatch)) {
                            if (log.isDebugEnabled()) {
                                log.debug("[" + snapshot + "] File " + filePath + ": Removing property '"
                                          + keyFromFile + "=" + valueFromFile
                                          + "' as its value matches the '" + valueToMatch
                                          + "' regular expression");
                            }

                            addKeyToRemove(keysToRemoveBySection, section, keyFromFile);
                            break;
                        }
                    } else if (matchType.isPlainText()) {
                        // just text
                        if (valueFromFile.equalsIgnoreCase(valueToMatch)) {
                            if (log.isDebugEnabled()) {
                                log.debug("[" + snapshot + "] File " + filePath + ": Removing property '"
                                          + keyFromFile + "=" + valueFromFile
                                          + "' as its value equals ignoring case the '" + valueToMatch
                                          + "' text");
                            }

                            addKeyToRemove(keysToRemoveBySection, section, keyFromFile);
                            break;
                        }

                    } else { // if( entry.getValue().isContainingText() )
                        if (valueFromFile.toLowerCase().contains(valueToMatch.toLowerCase())) {
                            if (log.isDebugEnabled()) {
                                log.debug("[" + snapshot + "] File " + filePath + ": Removing property '"
                                          + keyFromFile + "=" + valueFromFile
                                          + "' as its value contains ignoring case the '" + valueToMatch
                                          + "' text");
                            }

                            addKeyToRemove(keysToRemoveBySection, section, keyFromFile);
                            break;
                        }
                    }
                }
            }
        }

        // now remove all matched properties
        for (Entry<String, List<String>> matcherEntry : keysToRemoveBySection.entrySet()) {
            for (String key : matcherEntry.getValue()) {
                props.get(matcherEntry.getKey()).remove(key);
            }
        }
    }

    private String getDescription() {

        return "skip ini property matcher";
    }
}
