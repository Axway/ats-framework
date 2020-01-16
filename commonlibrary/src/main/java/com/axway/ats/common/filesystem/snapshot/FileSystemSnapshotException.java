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
package com.axway.ats.common.filesystem.snapshot;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filesystem.snapshot.equality.DifferenceType;
import com.axway.ats.common.filesystem.snapshot.equality.FileSystemEqualityState;
import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;

/**
 * Error while working with File System Snapshot
 */
@PublicAtsApi
public class FileSystemSnapshotException extends RuntimeException {

    private static final long       serialVersionUID = 1L;

    private FileSystemEqualityState equality;

    public FileSystemSnapshotException( String arg0 ) {

        super(arg0);
    }

    public FileSystemSnapshotException( Throwable arg0 ) {

        super(arg0);
    }

    public FileSystemSnapshotException( String arg0, Throwable arg1 ) {

        super(arg0, arg1);
    }

    public FileSystemSnapshotException( FileSystemEqualityState equality ) {

        this.equality = equality;
    }

    /**
     * This can be used to retrieve the compare result and then make
     * some custom compare report.
     * 
     * @return the result of compare
     */
    @PublicAtsApi
    public FileSystemEqualityState getEqualityState() {

        return this.equality;
    }

    @Override
    @PublicAtsApi
    public String getMessage() {

        if (equality == null) {
            // got a generic exception, not directly concerning the comparision
            return super.getMessage();
        } else {
            StringBuilder msg = new StringBuilder();
            msg.append("Comparing [");
            msg.append(equality.getFirstSnapshotName());
            msg.append("] and [");
            msg.append(equality.getSecondSnapshotName());
            msg.append("] produced the following unexpected differences:");

            // tell all differences sorted by their type
            DifferenceType diffType = null;
            DifferenceType nextDiffType;
            for (FileTrace diff : equality.getDifferences()) {
                nextDiffType = diff.getDifferenceType();
                if (nextDiffType != diffType) {
                    // add new difference type
                    msg.append("\n\n");
                    msg.append(nextDiffType.getDescription(diff.getFirstSnapshot(),
                                                           diff.getSecondSnapshot()));

                    diffType = nextDiffType;
                }

                // add new difference
                msg.append("\n");
                msg.append(diff.toString());
            }
            msg.append("\n");
            return msg.toString();
        }
    }

    /**
     * Get the all directories which are present in just one of the snapshots.
     * <br>Note that <b>null</b> is returned if provide a not existing snapshot name.
     *
     * @param snapshot snapshot name
     * @return list of matching directories
     */
    public List<String> getDirectoriesPresentInOneSnapshotOnly( String snapshot ) {

        DifferenceType searchedDiffType;
        if (equality.getFirstSnapshotName().equals(snapshot)) {
            searchedDiffType = DifferenceType.DIR_PRESENT_IN_FIRST_SNAPSHOT_ONLY;
        } else if (equality.getSecondSnapshotName().equals(snapshot)) {
            searchedDiffType = DifferenceType.DIR_PRESENT_IN_SECOND_SNAPSHOT_ONLY;
        } else {
            return null;
        }

        List<String> dirs = new ArrayList<String>();
        for (FileTrace diff : equality.getDifferences()) {
            if (diff.getDifferenceType() == searchedDiffType) {
                dirs.add(diff.toString());
            }
        }
        return dirs;
    }

    /**
     * Get the all files which are present in just one of the snapshots.
     * <br>Note that <b>null</b> is returned if provide a not existing snapshot name.
     *
     * @param snapshot snapshot name
     * @return list of matching files
     */
    public List<String> getFilesPresentInOneSnapshotOnly( String snapshot ) {

        DifferenceType searchedDiffType;
        if (equality.getFirstSnapshotName().equals(snapshot)) {
            searchedDiffType = DifferenceType.FILE_PRESENT_IN_FIRST_SNAPSHOT_ONLY;
        } else if (equality.getSecondSnapshotName().equals(snapshot)) {
            searchedDiffType = DifferenceType.FILE_PRESENT_IN_SECOND_SNAPSHOT_ONLY;
        } else {
            return null;
        }

        List<String> files = new ArrayList<String>();
        for (FileTrace diff : equality.getDifferences()) {
            if (diff.getDifferenceType() == searchedDiffType) {
                files.add(diff.toString());
            }
        }
        return files;
    }

    /**
     * @return list of files which are present in both snapshots, but are not same files.
     */
    public List<String> getDifferentFilesPresentInBothSnapshots() {

        List<String> files = new ArrayList<String>();
        for (FileTrace diff : equality.getDifferences()) {
            if (diff.getDifferenceType() == DifferenceType.DIFFERENT_FILES) {
                files.add(diff.toString());
            }
        }
        return files;
    }
}