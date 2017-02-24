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
import java.util.List;

import com.axway.ats.common.PublicAtsApi;

/**
 * We keep here the differences found between two files or directories
 */
@PublicAtsApi
public class FileTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    // compared files or directories
    private String            firstEntityDescription;
    private String            secondEntityDescription;

    // snapshot names
    private String            firstSnapshot;
    private String            secondSnapshot;

    // entity path
    private String            firstEntityPath;
    private String            secondEntityPath;

    // whether comparing files or directories
    private boolean           isComparingFiles;

    // list of differences about these files
    private List<String>      differencies;

    private DifferenceType    differenceType;

    public FileTrace( String firstSnapshot, String firstFilePath, String secondSnapshot,
                      String secondFilePath ) {

        this( firstSnapshot, firstFilePath, secondSnapshot, secondFilePath, true );
    }

    public FileTrace( String firstSnapshot, String firstEntityPath, String secondSnapshot,
                      String secondEntityPath, boolean isComparingFiles ) {

        this.firstSnapshot = firstSnapshot;
        this.firstEntityPath = firstEntityPath;
        this.secondSnapshot = secondSnapshot;
        this.secondEntityPath = secondEntityPath;

        this.isComparingFiles = isComparingFiles;

        this.differencies = new ArrayList<String>();
        parseDifferenceType();
    }

    public FileTrace( String firstFileDescription, String secondFileDescription ) {

        this.firstEntityDescription = firstFileDescription;
        this.secondEntityDescription = secondFileDescription;

        this.differencies = new ArrayList<String>();
        parseDifferenceType();
    }

    private void parseDifferenceType() {

        if( firstEntityPath != null && secondEntityPath == null ) {
            // entity found in first snapshot only
            if( isComparingFiles ) {
                differenceType = DifferenceType.FILE_PRESENT_IN_FIRST_SNAPSHOT_ONLY;
            } else {
                differenceType = DifferenceType.DIR_PRESENT_IN_FIRST_SNAPSHOT_ONLY;
            }
        } else if( firstEntityPath == null && secondEntityPath != null ) {
            // entity found in second snapshot only
            if( isComparingFiles ) {
                differenceType = DifferenceType.FILE_PRESENT_IN_SECOND_SNAPSHOT_ONLY;
            } else {
                differenceType = DifferenceType.DIR_PRESENT_IN_SECOND_SNAPSHOT_ONLY;
            }
        }
    }

    @PublicAtsApi
    public String getFirstSnapshot() {

        return firstSnapshot;
    }

    @PublicAtsApi
    public String getSecondSnapshot() {

        return secondSnapshot;
    }

    public void addDifference( String valueDescription, String srcValue, String dstValue ) {

        differencies.add( valueDescription + ": " + srcValue + " - " + dstValue );

        // files are present, but they have some different attributes
        differenceType = DifferenceType.DIFFERENT_FILES;
    }

    @PublicAtsApi
    public List<String> getDifferencies() {

        return differencies;
    }

    @PublicAtsApi
    public DifferenceType getDifferenceType() {

        return differenceType;
    }

    @Override
    public String toString() {

        StringBuilder msg = new StringBuilder();

        if( firstEntityPath != null && secondEntityPath == null ) {
            // entity found in first snapshot only
            msg.append( firstEntityPath );
        } else if( firstEntityPath == null && secondEntityPath != null ) {
            // entity found in second snapshot only
            msg.append( secondEntityPath );
        } else {
            // files are present, but different
            msg.append( firstEntityDescription );
            msg.append( " - " );
            msg.append( secondEntityDescription );
            msg.append( ":" );

            for( String diff : differencies ) {
                msg.append( "\n\t" );
                msg.append( diff );
            }
        }

        return msg.toString();
    }
}
