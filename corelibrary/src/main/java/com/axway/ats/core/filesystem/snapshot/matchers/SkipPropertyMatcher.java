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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;

/**
 * Used to decide whether some properties are to be skipped due to some
 * match in there key or value
 */
public class SkipPropertyMatcher extends SkipContentMatcher {

    private static final Logger     log = LogManager.getLogger(SkipPropertyMatcher.class);

    // < property key , match type >
    private Map<String, MATCH_TYPE> keysMap;
    // < property value , match type >
    private Map<String, MATCH_TYPE> valuesMap;

    public SkipPropertyMatcher( String directoryAlias, String filePath ) {

        super(directoryAlias, filePath);

        this.keysMap = new HashMap<>();
        this.valuesMap = new HashMap<>();
    }

    public String getFilePath() {

        return filePath;
    }

    public void setFilePath( String filePath ) {

        this.filePath = filePath;
    }

    public String getDirectoryAlias() {

        return directoryAlias;
    }

    public Map<String, MATCH_TYPE> getKeysMap() {

        return keysMap;
    }

    public Map<String, MATCH_TYPE> getValuesMap() {

        return valuesMap;
    }

    public SkipPropertyMatcher addKeyCondition( String key, MATCH_TYPE type ) {

        if (keysMap.containsKey(key)) {
            if (keysMap.get(key) != type) {
                StringBuilder sb = new StringBuilder();
                sb.append("File " + filePath + ": ");
                sb.append("There is already a " + getDescription() + " for key '" + key + "'. ");
                sb.append("Current key would be matched by '" + keysMap.get(key) + "' ");
                sb.append("but we change this, so it will be matched by '" + type.getDescription() + "'");

                log.warn(sb.toString());
            }
        } else if (log.isDebugEnabled()) {
            log.debug("File " + filePath + ": Adding a " + getDescription() + " with key '" + key
                      + "' matched as '" + type + "'");
        }

        keysMap.put(key, type);

        return this;
    }

    public SkipPropertyMatcher addValueCondition( String value, MATCH_TYPE type ) {

        if (valuesMap.containsKey(value)) {
            if (valuesMap.get(value) != type) {
                StringBuilder sb = new StringBuilder();
                sb.append("File " + filePath + ": ");
                sb.append("There is already a " + getDescription() + " for value '" + value + "'. ");
                sb.append("Current value would be matched by '" + valuesMap.get(value) + "' ");
                sb.append("but we change this, so it will be matched by '" + type.getDescription() + "'");

                log.warn(sb.toString());
            }
        } else if (log.isDebugEnabled()) {
            log.debug("File " + filePath + ": Adding a " + getDescription() + " with value '" + value
                      + "' matched as '" + type + "'");
        }

        valuesMap.put(value, type);

        return this;
    }

    public void process( String thisSnapshot, String thatSnapshot, Properties thisProps, Properties thatProps,
                         FileTrace fileTrace ) {

        if (keysMap.size() > 0) {
            processKeys(thisSnapshot, thisProps);
            processKeys(thatSnapshot, thatProps);
        }

        if (valuesMap.size() > 0) {
            processValues(thisSnapshot, thisProps);
            processValues(thatSnapshot, thatProps);
        }
    }

    private void processKeys( String snapshot, Properties props ) {

        List<String> keysToRemove = new ArrayList<>();

        // cycle all property keys
        // it is possible to match more than one key from the file
        for (String keyFromFile : props.stringPropertyNames()) {

            // cycle all match conditions
            for (Entry<String, MATCH_TYPE> matcherEntry : keysMap.entrySet()) {
                if (matcherEntry.getValue().isRegex()) {
                    // regular expression
                    if (keyFromFile.matches(matcherEntry.getKey())) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + snapshot + "] File " + filePath + ": Removing property '"
                                      + keyFromFile + "=" + props.getProperty(keyFromFile)
                                      + "' as its key matches the '" + matcherEntry.getKey()
                                      + "' regular expression");
                        }

                        keysToRemove.add(keyFromFile);
                        break;
                    }
                } else if (matcherEntry.getValue().isPlainText()) {
                    // just text
                    if (keyFromFile.trim().equalsIgnoreCase(matcherEntry.getKey().trim())) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + snapshot + "] File " + filePath + ": Removing property '"
                                      + keyFromFile + "=" + props.getProperty(keyFromFile)
                                      + "' as its key equals ignoring case the '" + matcherEntry.getKey()
                                      + "' text");
                        }

                        keysToRemove.add(keyFromFile);
                        break;
                    }

                }
                // if( entry.getValue().isContainingText() )
                else {
                    if (keyFromFile.trim()
                                   .toLowerCase()
                                   .contains(matcherEntry.getKey().trim().toLowerCase())) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + snapshot + "] File " + filePath + ": Removing property '"
                                      + keyFromFile + "=" + props.getProperty(keyFromFile)
                                      + "' as its key contains ignoring case the '" + matcherEntry.getKey()
                                      + "' text");
                        }

                        keysToRemove.add(keyFromFile);
                        break;
                    }
                }
            }
        }

        // now remove all matched properties
        for (String keyToRemove : keysToRemove) {
            props.remove(keyToRemove);
        }
    }

    private void processValues( String snapshot, Properties props ) {

        List<String> keysToRemove = new ArrayList<>();

        // cycle all property values
        // it is possible to match more than one value from the file
        for (String keyFromFile : props.stringPropertyNames()) {

            String valueFromFile = props.getProperty(keyFromFile);

            // cycle all match conditions
            for (Entry<String, MATCH_TYPE> matcherEntry : valuesMap.entrySet()) {
                if (matcherEntry.getValue().isRegex()) {
                    // regular expression
                    if (valueFromFile.matches(matcherEntry.getKey())) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + snapshot + "] File " + filePath + ": Removing property '"
                                      + keyFromFile + "=" + valueFromFile + "' as its value matches the '"
                                      + matcherEntry.getKey() + "' regular expression");
                        }

                        keysToRemove.add(keyFromFile);
                        break;
                    }
                } else if (matcherEntry.getValue().isPlainText()) {
                    // just text
                    if (valueFromFile.trim().equalsIgnoreCase(matcherEntry.getKey().trim())) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + snapshot + "] File " + filePath + ": Removing property '"
                                      + keyFromFile + "=" + valueFromFile
                                      + "' as its value equals ignoring case the '" + matcherEntry.getKey()
                                      + "' text");
                        }

                        keysToRemove.add(keyFromFile);
                        break;
                    }

                } else { // if( entry.getValue().isContainingText() )
                    if (valueFromFile.trim()
                                     .toLowerCase()
                                     .contains(matcherEntry.getKey().trim().toLowerCase())) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + snapshot + "] File " + filePath + ": Removing property '"
                                      + keyFromFile + "=" + valueFromFile
                                      + "' as its value contains ignoring case the '" + matcherEntry.getKey()
                                      + "' text");
                        }

                        keysToRemove.add(keyFromFile);
                        break;
                    }
                }
            }
        }

        // now remove all matched properties
        for (String keyToRemove : keysToRemove) {
            props.remove(keyToRemove);
        }
    }

    private String getDescription() {

        return "skip property matcher";
    }
}
