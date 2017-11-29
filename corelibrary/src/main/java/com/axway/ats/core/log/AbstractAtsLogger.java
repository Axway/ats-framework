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
package com.axway.ats.core.log;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used for common log interface
 */
public abstract class AbstractAtsLogger {

    private static AbstractAtsLogger                defaultInstance;
    private static Map<Class<?>, AbstractAtsLogger> loggers = new HashMap<Class<?>, AbstractAtsLogger>();

    public static synchronized AbstractAtsLogger getDefaultInstance( Class<?> callingClass ) {

        AbstractAtsLogger logger = loggers.get( callingClass );
        if( logger == null ) {
            logger = createNewInstance( callingClass );
            loggers.put( callingClass, logger );
        }

        return logger;
    }

    public static synchronized void setDefaultInstance( AbstractAtsLogger newDefaultInstance ) {

        defaultInstance = newDefaultInstance;
    }

    public static synchronized AbstractAtsLogger createNewInstance( Class<?> callingClass ) {

        if( defaultInstance == null ) {
            defaultInstance = new AtsLog4jLogger( callingClass );
        }

        return defaultInstance.newInstance();
    }

    public abstract void setLevel( String level );

    public abstract AbstractAtsLogger newInstance();

    public abstract void setCallingClass( Class<?> callingClass );

    public abstract void debug( String message );

    public abstract void info( String message );

    public abstract void warn( String message );

    public abstract void error( String message );

    public abstract void error( String message, Throwable exception );
}
