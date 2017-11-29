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
package com.axway.ats.agent.core.threading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;

public class ParameterizedInputActionClass {

    public static HashMap<String, Integer>            addedStrings    = new HashMap<String, Integer>();
    public static LinkedHashMap<String, List<String>> addedParams     = new LinkedHashMap<String, List<String>>();
    public static Set<Long>                           addedLongs      = new HashSet<Long>();
    public static int                                 numThreadsAlive = 0;
    private static List<String>                       lastPoolList;

    @Action(name = "create file")
    public void createFile(
                            @Parameter(name = "fileName") String fileName ) {

        synchronized( addedStrings ) {
            Integer numInvocations = addedStrings.get( fileName );
            if( numInvocations == null ) {
                addedStrings.put( fileName, 1 );
            } else {
                addedStrings.put( fileName, numInvocations + 1 );
            }
        }
    }

    @Action(name = "upload file")
    public void uploadFile(
                            @Parameter(name = "fileName") String fileName ) {

        synchronized( addedStrings ) {
            Integer numInvocations = addedStrings.get( fileName );
            if( numInvocations == null ) {
                addedStrings.put( fileName, 1 );
            } else {
                addedStrings.put( fileName, numInvocations + 1 );
            }
        }
    }

    @Action(name = "action long")
    public void actionLong(
                            @Parameter(name = "milliseconds") long milliseconds ) throws InterruptedException {

        synchronized( addedLongs ) {
            addedLongs.add( milliseconds );
        }
    }

    @Action(name = "action config")
    public void actionConfig(
                              @Parameter(name = "value") String value,
                              @Parameter(name = "waitingTime") String waitingTime )
                                                                                   throws InterruptedException {

        String thread = Thread.currentThread().getName();

        //wait to put the val1 enter first in the map 
        if( addedParams.isEmpty() && !value.equals( "val1" ) ) {
            Thread.sleep( 10 );
        }

        synchronized( addedParams ) {

            if( addedParams.containsKey( thread ) )
                addedParams.get( thread ).add( value );
            else {
                addedParams.put( thread, new ArrayList<String>( Arrays.asList( new String[]{ value } ) ) );
            }
        }

        Thread.sleep( Long.parseLong( waitingTime ) );
    }

    public static int getAddedStringsCount() {

        int addedStringsCount = 0;
        for( Integer count : addedStrings.values() ) {
            addedStringsCount += count;
        }
        return addedStringsCount;
    }

    @Action(name = "get all threads before start")
    public void getAllThreadsBeforeStart() {

        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        lastPoolList = new ArrayList<String>();

        for( Thread th : threads ) {
            String threadName = th.getName();
            if( threadName.startsWith( "pool" ) && th.isAlive() ) {
                lastPoolList.add( th.getName() );
            }
        }
        lastPoolList.remove( Thread.currentThread().getName() );
    }

    @Action(name = "check for remaining threads after test")
    public void checkForRemainingThreadsAfterTest() {

        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        List<String> newPoolList = new ArrayList<String>();

        for( Thread th : threads ) {
            String threadName = th.getName();
            if( threadName.startsWith( "pool" ) && th.isAlive() ) {
                newPoolList.add( threadName );
            }
        }
        newPoolList.removeAll( lastPoolList );
        newPoolList.remove( Thread.currentThread().getName() );

        numThreadsAlive = newPoolList.size();
    }
}
