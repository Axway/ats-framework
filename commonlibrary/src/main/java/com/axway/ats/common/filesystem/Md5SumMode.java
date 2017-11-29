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
package com.axway.ats.common.filesystem;

import com.axway.ats.common.PublicAtsApi;

/**
 * Enumeration for MD5 sum mode
 */
@PublicAtsApi
public enum Md5SumMode {

    /**
     * Binary mode - standard md5 sum computation 
     */
    @PublicAtsApi
    BINARY,

    /**
     * ASCII mode means that line endings will be ignored when computing the sum - e.g.
     * one and the same file with Windows and Linux style line endings will have the same
     * MD5 sum. 
     */
    @PublicAtsApi
    ASCII;

    /**
     * Check if the current mode is binary
     */
    public boolean isBinary() {

        if (this == BINARY) {
            return true;
        }

        return false;
    }
}
