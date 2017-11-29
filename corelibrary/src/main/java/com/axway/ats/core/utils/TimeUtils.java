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
package com.axway.ats.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Time relate utility class
 */
public class TimeUtils {

    /**
     * Date formatter with date and time till milliseconds 
     */
    private static final SimpleDateFormat DATE_FORMATTER_TILL_MILLIS = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" );

    /**
     * Get the current date formatted in this way
     * {@link TimeUtils#DATE_FORMATTER_TILL_MILLIS}
     *
     * @return
     */
    public static String getFormattedDateTillMilliseconds() {

        return getFormattedDateTillMilliseconds( new Date() );
    }

    /**
     * Get the date formatted in this way
     * {@link TimeUtils#DATE_FORMATTER_TILL_MILLIS}
     *
     * @return
     */
    public static String getFormattedDateTillMilliseconds( Date date ) {

        return DATE_FORMATTER_TILL_MILLIS.format( date );
    }
}
