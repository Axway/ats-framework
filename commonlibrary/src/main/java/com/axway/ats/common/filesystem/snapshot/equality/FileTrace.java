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
package com.axway.ats.common.filesystem.snapshot.equality;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import com.axway.ats.common.PublicAtsApi;

/**
 * We keep here the differences found between two files or directories
 */
@PublicAtsApi
public class FileTrace implements Serializable {

    private static final long   serialVersionUID = 1L;

    // snapshot names
    private String              firstSnapshot;
    private String              secondSnapshot;

    // entity path(either files or directories)
    private String              firstEntityPath;
    private String              secondEntityPath;

    // list of differences about these files
    // note that we always have same keys in both maps
    // < difference description, difference value >
    private Map<String, String> firstSnapshotDifferencies;
    private Map<String, String> secondSnapshotDifferencies;

    // describe the file we are dealing with
    private String              entityType;

    private DifferenceType      differenceType;

    public FileTrace( String firstSnapshot, String firstEntityPath, String secondSnapshot,
                      String secondEntityPath, String entityType, boolean isComparingFiles ) {

        this.firstSnapshot = firstSnapshot;
        this.firstEntityPath = firstEntityPath;
        this.secondSnapshot = secondSnapshot;
        this.secondEntityPath = secondEntityPath;

        this.entityType = entityType;

        this.firstSnapshotDifferencies = new TreeMap<>();
        this.secondSnapshotDifferencies = new TreeMap<>();
        parseDifferenceType(isComparingFiles);
    }

    private void parseDifferenceType( boolean isComparingFiles ) {

        if (firstEntityPath != null && secondEntityPath == null) {
            // entity found in first snapshot only
            if (isComparingFiles) {
                differenceType = DifferenceType.FILE_PRESENT_IN_FIRST_SNAPSHOT_ONLY;
            } else {
                differenceType = DifferenceType.DIR_PRESENT_IN_FIRST_SNAPSHOT_ONLY;
            }
        } else if (firstEntityPath == null && secondEntityPath != null) {
            // entity found in second snapshot only
            if (isComparingFiles) {
                differenceType = DifferenceType.FILE_PRESENT_IN_SECOND_SNAPSHOT_ONLY;
            } else {
                differenceType = DifferenceType.DIR_PRESENT_IN_SECOND_SNAPSHOT_ONLY;
            }
        }
    }

    /**
     * @return the name of the first snapshot
     */
    @PublicAtsApi
    public String getFirstSnapshot() {

        return firstSnapshot;
    }

    /**
     * @return the name of the second snapshot
     */
    @PublicAtsApi
    public String getSecondSnapshot() {

        return secondSnapshot;
    }

    /**
     * @return path to the first different entity(file or directory)
     */
    @PublicAtsApi
    public String getFirstEntityPath() {

        return firstEntityPath;
    }

    /**
     * @return path to the second different entity(file or directory)
     */
    @PublicAtsApi
    public String getSecondEntityPath() {

        return secondEntityPath;
    }

    public void addDifference( String valueDescription, String srcValue, String dstValue ) {

        firstSnapshotDifferencies.put(valueDescription, srcValue);
        secondSnapshotDifferencies.put(valueDescription, dstValue);

        // files are present, but they have some different attributes
        differenceType = DifferenceType.DIFFERENT_FILES;
    }

    /**
     * @return whether some differences are found
     */
    @PublicAtsApi
    public boolean hasDifferencies() {

        return firstSnapshotDifferencies.size() + secondSnapshotDifferencies.size() > 0;
    }

    /**
     * Get what is present in the first snapshot only
     * @return map with key (describing the difference) and value (the difference itself)
     */
    @PublicAtsApi
    public Map<String, String> getFirstSnapshotDifferencies() {

        return firstSnapshotDifferencies;
    }

    /**
     * Get what is present in the second snapshot only
     * @return map with key (describing the difference) and value (the difference itself)
     */
    @PublicAtsApi
    public Map<String, String> getSecondSnapshotDifferencies() {

        return secondSnapshotDifferencies;
    }

    /**
     * @return the difference type
     */
    @PublicAtsApi
    public DifferenceType getDifferenceType() {

        return differenceType;
    }

    @Override
    @PublicAtsApi
    public String toString() {

        StringBuilder msg = new StringBuilder();

        if (firstEntityPath != null && secondEntityPath == null) {
            // entity(file or directory) found in first snapshot only
            msg.append(firstEntityPath);
        } else if (firstEntityPath == null && secondEntityPath != null) {
            // entity(file or directory) found in second snapshot only
            msg.append(secondEntityPath);
        } else {
            // files are present in both snapshots, but are different
            msg.append("[" + firstSnapshot + "] ");
            msg.append(entityType + " \"");
            msg.append(firstEntityPath);
            msg.append("\" - ");
            msg.append("[" + secondSnapshot + "] ");
            msg.append(entityType + " \"");
            msg.append(secondEntityPath);
            msg.append("\":");

            for (String diffKey : firstSnapshotDifferencies.keySet()) {
                msg.append("\n\t" + diffKey + ": " + firstSnapshotDifferencies.get(diffKey) + " - "
                           + secondSnapshotDifferencies.get(diffKey));
            }
        }

        return msg.toString();
    }
}
