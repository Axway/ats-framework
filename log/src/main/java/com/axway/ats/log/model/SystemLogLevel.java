/*
 * Copyright 2017-2021 Axway Software
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

import com.axway.ats.log.Log4j2Utils;

/**
 * This class adds one custom level to the log4j predefined levels
 * The level is called SYSTEM and is used when sending the custom events
 * for starting runs, groups, test cases, etc.
 * This level is higher than the FATAL level, so it is always enabled
 */
public class SystemLogLevel {

    public final static String SYSTEM_NAME = "SYSTEM";
    public final static int    SYSTEM_INT  = Level.FATAL.intLevel() - 50;
    public final static Level  SYSTEM      = Level.forName(SYSTEM_NAME, SYSTEM_INT);

    /**
    Convert the string passed as argument to a level. If the
    conversion fails, then this method returns {@link Level#DEBUG}.</br>
    <strong>FIXME:</strong> Maybe this method should be moved to {@link Log4j2Utils}
    */
    public static Level toLevel(
                                 String sArg ) {

        return toLevel(sArg, Level.DEBUG);
    }

    /**
      Convert an integer passed as argument to a level. If the
      conversion fails, then this method returns {@link Level#DEBUG}.
      </br><strong>FIXME:</strong> Maybe this method should be moved to {@link Log4j2Utils}
    */
    public static Level toLevel(
                                 int val ) {

        return toLevel(val, Level.DEBUG);
    }

    /**
      Convert an integer passed as argument to a level. If the
      conversion fails, then this method returns the specified default.
      </br><strong>FIXME:</strong> Maybe this method should be moved to {@link Log4j2Utils}
    */
    public static Level toLevel(
                                 int val,
                                 Level defaultLevel ) {

        return (val == SYSTEM_INT)
                                   ? SystemLogLevel.SYSTEM
                                   : Level.toLevel(convertyIntValToName(val), defaultLevel);
    }

    private static String convertyIntValToName( int val ) {

        Level[] levels = Level.values();
        for (Level lvl : levels) {
            if (lvl.intLevel() == val) {
                return lvl.name();
            }
        }
        return null; // no problem, log4j2 handles null value for level name
    }

    /**
       Convert the string passed as argument to a level. If the
       conversion fails, then this method returns the value of
       <code>defaultLevel</code>.
       </br><strong>FIXME:</strong> Maybe this method should be moved to {@link Log4j2Utils}
    */
    public static Level toLevel(
                                 String sArg,
                                 Level defaultLevel ) {

        if (sArg == null)
            return defaultLevel;

        String s = sArg.toUpperCase();

        if (SYSTEM_NAME.equals(s)) {
            return SystemLogLevel.SYSTEM;
        } else {
            return Level.toLevel(sArg, defaultLevel);
        }
    }
}
