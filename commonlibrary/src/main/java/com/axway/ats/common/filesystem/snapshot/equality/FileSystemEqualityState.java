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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axway.ats.common.PublicAtsApi;

/**
 * This structure can say how equal the snapshots are
 */
@PublicAtsApi
public class FileSystemEqualityState implements Serializable {

    private static final long serialVersionUID = 1L;

    private String            firstSnapshotName;
    private String            secondSnapshotName;

    // list of different files
    private List<FileTrace>   differences;

    // remember the agent addresses, there are cases we need them
    private String            firstAtsAgent;
    private String            secondAtsAgent;

    public FileSystemEqualityState( String firstSnapshotName, String secondSnapshotName ) {

        this.firstSnapshotName = firstSnapshotName;
        this.secondSnapshotName = secondSnapshotName;

        differences = new ArrayList<FileTrace>();
    }

    /**
     * @return the name of the first snapshot
     */
    @PublicAtsApi
    public String getFirstSnapshotName() {

        return firstSnapshotName;
    }

    /**
     * @return the name of the second snapshot
     */
    @PublicAtsApi
    public String getSecondSnapshotName() {

        return secondSnapshotName;
    }

    /**
     * @return list of differences sorted by their type
     */
    @PublicAtsApi
    public List<FileTrace> getDifferences() {

        Collections.sort(differences, new Comparator<FileTrace>() {
            @Override
            public int compare( FileTrace trace1, FileTrace trace2 ) {

                return trace1.getDifferenceType().toInt() - trace2.getDifferenceType().toInt();
            }
        });

        return differences;
    }
    
    /** 
     * Create and return a map that holds differences from files presented in both snapshots
     * <div>EXAMPLE:
     * <p>{</p>
     * <p>&nbsp;&nbsp;&nbsp;&nbsp;Presence of common.testboxes.client : {snap1 : YES, snap2 : NO},</p>
     * <p>&nbsp;&nbsp;&nbsp;&nbsp;property key 'common.testboxes.client' : {snap1 : AAA, snap2 : BBB},</p>
     * <p>&nbsp;&nbsp;&nbsp;&nbsp;Section [languages], key '00A' : {snap1: 'English', snap1: 'Spanish'}</p>
     * <p>}</p>
     * </div>
     * @return map that holds the differences
     * */

    @PublicAtsApi
    public Map<String, Map<String, String>> getFilesDifferences() {

        Map<String, Map<String, String>> differencesMap = new HashMap<>();
        // iterate over each file trace object
        for (FileTrace diff : getDifferences()) {
            // get the file differences in first snapshot
            Map<String, String> first = diff.getFirstSnapshotDifferencies();
            // get the file differences in second snapshot
            Map<String, String> second = diff.getSecondSnapshotDifferencies();
            // iterate over each file from the first snapshot difference map
            for (Map.Entry<String, String> entry1 : first.entrySet()) {
                // iterate over each file from the second snapshot difference map
                for (Map.Entry<String, String> entry2 : second.entrySet()) {
                    // check if the current two entries contains information for the same file difference
                    if (entry1.getKey().equals(entry2.getKey())) {
                        // create map that will hold information about the status of the current difference for each snapshot
                        Map<String, String> snapToValueMap = new HashMap<String, String>();
                        // put the difference value from the first snapshot to the map
                        snapToValueMap.put(getFirstSnapshotName(), entry1.getValue());
                        // put the difference value from the second snapshot to the map
                        snapToValueMap.put(getSecondSnapshotName(), entry2.getValue());
                        // add the whole information for the current difference to the difference map
                        differencesMap.put(entry1.getKey(), snapToValueMap);
                    }
                }
            }
        }

        return differencesMap;
    }

    public void addDifference( FileTrace difference ) {

        differences.add(difference);
    }

    public String getFirstAtsAgent() {

        return firstAtsAgent;
    }

    public void setFirstAtsAgent( String firstAtsAgent ) {

        this.firstAtsAgent = firstAtsAgent;
    }

    public String getSecondAtsAgent() {

        return secondAtsAgent;
    }

    public void setSecondAtsAgent( String secondAtsAgent ) {

        this.secondAtsAgent = secondAtsAgent;
    }
}
