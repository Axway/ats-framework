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
import java.util.List;

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
