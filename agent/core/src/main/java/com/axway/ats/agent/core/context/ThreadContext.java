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
import java.util.Map;

/**
 * The context in the scope of a thread
 */
public class ThreadContext {

    public static final String                      COOKIE_VAR_PREFFIX                        = "cookie_";
    public static final String                      TEMPLATE_ACTION_VERIFICATION_CONFIGURATOR = "TEMPLATE_ACTION_VERIFICATION_CONFIGURATOR";

    private static ThreadLocal<Map<String, Object>> threadLocalContext                        = new ThreadLocal<Map<String, Object>>() {
                                                                                                  public Map<String, Object> initialValue() {

                                                                                                      // initially add a map
                                                                                                      return new HashMap<String, Object>();
                                                                                                  }
                                                                                              };

    public static Object getAttribute(
                                       String name ) {

        return threadLocalContext.get().get( name );
    }

    public static String[] getAttributeNames() {

        return threadLocalContext.get()
                                 .keySet()
                                 .toArray( new String[threadLocalContext.get().keySet().size()] );
    }

    public static void removeAttribute(
                                        String name ) {

        threadLocalContext.get().remove( name );
    }

    public static void setAttribute(
                                     String name,
                                     Object object ) {

        if( object == null ) {
            throw new IllegalArgumentException( "Parameter '" + name + "' can not be null" );
        }
        threadLocalContext.get().put( name, object );
    }

    public static void clear() {

        threadLocalContext.get().clear();
    }
}
