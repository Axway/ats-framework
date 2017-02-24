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
import com.axway.ats.common.systemproperties.AtsSystemProperties;

/**
 * Enumeration for end of line style.
 */
@PublicAtsApi
public enum EndOfLineStyle {

    @PublicAtsApi
    UNIX,

    @PublicAtsApi
    MACOS,

    @PublicAtsApi
    WINDOWS;

    /**
     * Get the termination string for the current OS
     *
     * @return the termination string for the current OS
     */
    public String getTerminationString() {

        switch( this ){
            case UNIX: {
                return "\n";
            }
            case MACOS: {
                return "\r";
            }
            case WINDOWS: {
                return "\r\n";
            }
            default: {
                //should never happen
                throw new IllegalArgumentException( "End of line style " + this + " is not supported" );
            }
        }
    }

    /**
     * Get the end of line style for the current OS
     *
     * @return the end of line style for the current OS
     */
    public static EndOfLineStyle getCurrentOsStyle() {

        String terminationString = AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
        if( "\r\n".equals( terminationString ) ) {
            return WINDOWS;
        } else if( "\n".equals( terminationString ) ) {
            return UNIX;
        } else if( "\r".equals( terminationString ) ) {
            return MACOS;
        } else {
            //should never happen
            throw new IllegalArgumentException( "Termination string of current OS is not supported" );
        }
    }
}
