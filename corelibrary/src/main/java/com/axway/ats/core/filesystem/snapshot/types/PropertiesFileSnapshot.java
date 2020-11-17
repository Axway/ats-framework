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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.common.filesystem.snapshot.equality.FileSystemEqualityState;
import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;
import com.axway.ats.core.filesystem.snapshot.matchers.FindRules;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipPropertyMatcher;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipContentMatcher.MATCH_TYPE;
import com.axway.ats.core.utils.IoUtils;

/**
 * Compares the content of property files
 */
public class PropertiesFileSnapshot extends ContentFileSnapshot {

    private static final Logger       log              = LogManager.getLogger(PropertiesFileSnapshot.class);

    private static final long         serialVersionUID = 1L;

    private List<SkipPropertyMatcher> matchers         = new ArrayList<>();

    public PropertiesFileSnapshot( SnapshotConfiguration configuration, String path, FindRules fileRule,
                                   List<SkipPropertyMatcher> fileMatchers ) {
        super(configuration, path, fileRule);

        if (fileMatchers == null) {
            fileMatchers = new ArrayList<SkipPropertyMatcher>();
        }
        for (SkipPropertyMatcher matcher : fileMatchers) {
            this.matchers.add(matcher);
        }
    }

    PropertiesFileSnapshot( String path, long size, long timeModified, String md5, String permissions ) {
        super(path, size, timeModified, md5, permissions);
    }

    /**
     * Used to extend a regular file snapshot instance to the wider properties snapshot instance.
     * It adds all content check matchers
     * 
     * @param fileSnapshot the instance to extend
     * @return the extended instance
     */
    public PropertiesFileSnapshot getNewInstance( FileSnapshot fileSnapshot ) {

        PropertiesFileSnapshot instance = new PropertiesFileSnapshot(fileSnapshot.path, fileSnapshot.size,
                                                                     fileSnapshot.timeModified,
                                                                     fileSnapshot.md5,
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
        Properties thisProps = loadPropertiesFile(equality.getFirstAtsAgent(), this.getPath());
        Properties thatProps = loadPropertiesFile(equality.getSecondAtsAgent(), that.getPath());

        // remove matched properties
        // we currently call all property matchers on both files,
        // so it does not matter if a matcher is provided for first or second snapshot
        for (SkipPropertyMatcher matcher : this.matchers) {
            matcher.process(fileTrace.getFirstSnapshot(), fileTrace.getSecondSnapshot(), thisProps,
                            thatProps, fileTrace);
        }
        for (SkipPropertyMatcher matcher : ((PropertiesFileSnapshot) that).matchers) {
            matcher.process(fileTrace.getFirstSnapshot(), fileTrace.getSecondSnapshot(), thisProps,
                            thatProps, fileTrace);
        }

        // now compare rest of the properties
        Set<String> keys = new HashSet<>();
        keys.addAll(thisProps.stringPropertyNames());
        keys.addAll(thatProps.stringPropertyNames());

        for (String key : keys) {
            if (!thisProps.containsKey(key)) {
                // key not present in THIS list
                fileTrace.addDifference("Presence of " + key, "NO", "YES");
            } else if (!thatProps.containsKey(key)) {
                // key not present in THAT list
                fileTrace.addDifference("Presence of " + key, "YES", "NO");
            } else {
                // key present in both lists
                String thisValue = thisProps.getProperty(key).trim();
                String thatValue = thatProps.getProperty(key).trim();

                if (!thisValue.equalsIgnoreCase(thatValue)) {
                    fileTrace.addDifference("property key '" + key + "'", "'" + thisValue + "'",
                                            "'" + thatValue + "'");
                }
            }
        }

        if (fileTrace.hasDifferencies()) {
            // files are different
            equality.addDifference(fileTrace);
        } else {
            log.debug("Same files: " + this.getPath() + " and " + that.getPath());
        }

    }

    private Properties loadPropertiesFile( String agent, String filePath ) {

        // load the file as a String
        String fileContent = loadFileContent(agent, filePath);

        Properties properties = new Properties();
        StringReader reader = new StringReader(fileContent);
        try {
            properties.load(new StringReader(fileContent));
        } catch (IOException ex) {
            // this will cancel the comparison
            // the other option is to add a difference to the FileTrace object, instead of throwing an exception here
            throw new FileSystemOperationException("Error loading '" + filePath + "' properties file.");
        } finally {
            IoUtils.closeStream(reader, "Could not close a stream while reading from '" + filePath + "'");
        }

        return properties;
    }

    public String getFileType() {

        return "properties file";
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("properties ");
        sb.append(super.toString());
        for (SkipPropertyMatcher matcher : matchers) {
            Map<String, MATCH_TYPE> keysMap = matcher.getKeysMap();
            Map<String, MATCH_TYPE> valuesMap = matcher.getValuesMap();
            if (keysMap.size() + valuesMap.size() > 0) {
                sb.append("\n\tproperties:");
            }
            if (keysMap.size() > 0) {
                for (Entry<String, MATCH_TYPE> entity : keysMap.entrySet()) {
                    sb.append("\n\t\tkey " + entity.getKey() + " matched by "
                              + entity.getValue().toString());
                }
            }
            if (valuesMap.size() > 0) {
                for (Entry<String, MATCH_TYPE> entity : valuesMap.entrySet()) {
                    sb.append("\n\t\tvalue " + entity.getKey() + " matched by "
                              + entity.getValue().toString());
                }
            }
        }
        return sb.toString();
    }
}
