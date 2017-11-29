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
/**
 *
 */
package com.axway.ats.agent.core.threading;

/**
 * The purpose of this thread is to be located and joined to the main thread in any time
 *
 */
public class ImportantThread extends Thread {

    private static final String NAME_PREFIX = "ATS-";

    private String              description;

    public ImportantThread() {

        super();
        setName( NAME_PREFIX + getName() );
    }

    public ImportantThread( Runnable target ) {

        super( target );
        setName( NAME_PREFIX + getName() );
    }

    public ImportantThread( String name ) {

        super( name );
    }

    public ImportantThread( Runnable target,
                            String name ) {

        super( target, name );
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(
                                String description ) {

        this.description = description;
    }

}
