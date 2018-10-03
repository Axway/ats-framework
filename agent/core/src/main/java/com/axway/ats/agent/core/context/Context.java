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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A basic context.
 * 
 * It supports manipulating a shared map of attributes. 
 */
abstract class Context {

    private Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();

    public Object getAttribute(
                                String name ) {

        return attribs.get( name );
    }

    public void removeAttribute(
                                 String name ) {

        attribs.remove( name );
    }

    public void setAttribute(
                              String name,
                              Object object ) {

        attribs.put( name, object );
    }
}
