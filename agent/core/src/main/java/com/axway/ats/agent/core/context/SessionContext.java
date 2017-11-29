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
package com.axway.ats.agent.core.context;

import java.util.HashMap;

import com.axway.ats.agent.core.exceptions.ContextException;

public class SessionContext extends Context {

    private static String                          currentContextId;
    private static HashMap<String, SessionContext> contexts = new HashMap<String, SessionContext>();

    private SessionContext() {

    }

    public static synchronized SessionContext getCurrentContext() throws ContextException {

        if( currentContextId == null || !contexts.containsKey( currentContextId ) ) {
            throw new ContextException( "No session context has been set - you need to first enter a session context" );
        }

        return contexts.get( currentContextId );
    }

    public static synchronized void setCurrentContext(
                                                       String id ) {

        currentContextId = id;

        if( !contexts.containsKey( id ) ) {
            contexts.put( id, new SessionContext() );
        }
    }

    public static synchronized void exitCurrentContext() {

        contexts.remove( currentContextId );
        currentContextId = null;
    }
}
