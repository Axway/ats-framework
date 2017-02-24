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

import com.axway.ats.common.PublicAtsApi;

/**
 * Type of found difference
 */
@PublicAtsApi
public enum DifferenceType {

    @PublicAtsApi DIR_PRESENT_IN_FIRST_SNAPSHOT_ONLY(1),

    @PublicAtsApi DIR_PRESENT_IN_SECOND_SNAPSHOT_ONLY(2),

    @PublicAtsApi FILE_PRESENT_IN_FIRST_SNAPSHOT_ONLY(3),

    @PublicAtsApi FILE_PRESENT_IN_SECOND_SNAPSHOT_ONLY(4), @PublicAtsApi DIFFERENT_FILES(5);

    private int value;

    DifferenceType( int value ) {

        this.value = value;
    }

    /**
     * @return value as needed to sort all differences by their type
     */
    public int toInt() {

        return value;
    }

    @PublicAtsApi
    public String getDescription( String firstSnapshot, String secondSnapshot ) {

        switch( this ){
            case DIR_PRESENT_IN_FIRST_SNAPSHOT_ONLY:
                return "Directory is present in [" + firstSnapshot + "] snapshot only:";
            case DIR_PRESENT_IN_SECOND_SNAPSHOT_ONLY:
                return "Directory is present in [" + secondSnapshot + "] snapshot only:";
            case FILE_PRESENT_IN_FIRST_SNAPSHOT_ONLY:
                return "File is present in [" + firstSnapshot + "] snapshot only:";
            case FILE_PRESENT_IN_SECOND_SNAPSHOT_ONLY:
                return "File is present in [" + secondSnapshot + "] snapshot only:";
            default:
                return "Different files:";
        }
    }
}
