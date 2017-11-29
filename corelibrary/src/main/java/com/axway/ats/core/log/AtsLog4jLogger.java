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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Logger sending messages through Log4j
 */
public class AtsLog4jLogger extends AbstractAtsLogger {

    private final Logger log;

    public AtsLog4jLogger( Class<?> callingClass ) {

        if( callingClass == null ) {
            // this will probably never happen, as our code gives calling class when initializing log4j logger
            log = Logger.getLogger( "ATS Logger" );
        } else {
            log = Logger.getLogger( callingClass );
        }
    }

    private AtsLog4jLogger( String callingClassName ) {

        log = Logger.getLogger( callingClassName );
    }

    @Override
    public AbstractAtsLogger newInstance() {

        return new AtsLog4jLogger( log.getName() );
    }

    @Override
    public void setLevel(
                          String level ) {

        log.setLevel( Level.toLevel( level ) );
    }

    @Override
    public void setCallingClass(
                                 Class<?> callingClass ) {

        // do nothing as we get this from the constructor
    }

    @Override
    public void debug(
                       String message ) {

        log.debug( message );
    }

    @Override
    public void info(
                      String message ) {

        log.info( message );
    }

    @Override
    public void warn(
                      String message ) {

        log.warn( message );
    }

    @Override
    public void error(
                       String message ) {

        log.error( message );
    }

    @Override
    public void error(
                       String message,
                       Throwable exception ) {

        log.error( message, exception );
    }

}
