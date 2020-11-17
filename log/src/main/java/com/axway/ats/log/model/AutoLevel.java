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
package com.axway.ats.log.model;

import org.apache.logging.log4j.Level;

/**
 * This class adds one custom level to the log4j predefined levels
 * The level is called SYSTEM and is used when sending the custom events
 * for starting runs, groups, test cases, etc.
 * This level is higher than the FATAL level, so it is always enabled
 */
@SuppressWarnings( "serial")
public class AutoLevel extends Level {

    public final static int       SYSTEM_INT = Level.FATAL_INT + 10000;
    public final static AutoLevel SYSTEM     = new AutoLevel(SYSTEM_INT, "SYSTEM", 6);

    AutoLevel( int level,
               String levelStr,
               int syslogEquivalent ) {

        super(level, levelStr, syslogEquivalent);
    }

    /**
    Convert the string passed as argument to a level. If the
    conversion fails, then this method returns {@link #DEBUG}. 
    */
    public static Level toLevel(
                                 String sArg ) {

        return toLevel(sArg, Level.DEBUG);
    }

    /**
      Convert an integer passed as argument to a level. If the
      conversion fails, then this method returns {@link #DEBUG}.
    
    */
    public static Level toLevel(
                                 int val ) {

        return toLevel(val, Level.DEBUG);
    }

    /**
      Convert an integer passed as argument to a level. If the
      conversion fails, then this method returns the specified default.
    */
    public static Level toLevel(
                                 int val,
                                 Level defaultLevel ) {

        switch (val) {
            case SYSTEM_INT:
                return AutoLevel.SYSTEM;
            default:
                return Level.toLevel(val, defaultLevel);
        }
    }

    /**
       Convert the string passed as argument to a level. If the
       conversion fails, then this method returns the value of
       <code>defaultLevel</code>.  
    */
    public static Level toLevel(
                                 String sArg,
                                 Level defaultLevel ) {

        if (sArg == null)
            return defaultLevel;

        String s = sArg.toUpperCase();

        if ("SYSTEM".equals(s)) {
            return AutoLevel.SYSTEM;
        } else {
            return Level.toLevel(sArg, defaultLevel);
        }
    }
}
