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
package com.axway.ats.action.objects.model;

import com.axway.ats.common.PublicAtsApi;

@PublicAtsApi
public enum PackagePriority {

    @PublicAtsApi
    LOW,

    @PublicAtsApi
    NORMAL,

    @PublicAtsApi
    HIGH;

    @Override
    public String toString() {

        String lowerCaseStr = super.toString().toLowerCase();

        //only the first character should be upper case
        return lowerCaseStr.substring( 0, 1 ).toUpperCase() + lowerCaseStr.substring( 1 );
    }

    public int toInt() throws PackageException {

        switch( this ){
            case LOW:
                return 3;
            case NORMAL:
                return 2;
            case HIGH:
                return 1;
            default:
                throw new PackageException( "Unsupported priority: " + this );
        }
    }
}
