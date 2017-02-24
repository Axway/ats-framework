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
package com.axway.ats.agent.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.monitoring.queue.QueueExecutionStatistics;
import com.axway.ats.agent.core.threading.ParameterizedInputActionClass;
import com.axway.ats.agent.core.threading.data.config.LoaderDataConfig;
import com.axway.ats.agent.core.threading.data.config.ListDataConfig;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;
import com.axway.ats.agent.core.threading.data.config.RangeDataConfig;
import com.axway.ats.agent.core.threading.patterns.AllAtOncePattern;
import com.axway.ats.core.threads.ThreadsPerCaller;

public class DisabledTest_MultiThreadedActionHandlerWithParameterizedInputData extends BaseTest {

    private static List<ActionRequest>        actionRequests;
    private static MultiThreadedActionHandler actionHandler = MultiThreadedActionHandler.getInstance(ThreadsPerCaller.getCaller());

    private static ActionRequest              fileUploadActionRequest;
    private static ListDataConfig[]           parameterDataProviders;

    @BeforeClass
    public static void setUpTest_ActionInvoker() throws AgentException {

        Component component = new Component( TEST_COMPONENT_NAME );
        ComponentActionMap actionMap = new ComponentActionMap( TEST_COMPONENT_NAME );
        actionMap.registerActionClass( ParameterizedInputActionClass.class );
        component.setActionMap( actionMap );

        ComponentRepository componentRepository = ComponentRepository.getInstance();
        componentRepository.clear();
        componentRepository.putComponent( component );

        ActionRequest actionRequest1 = new ActionRequest( TEST_COMPONENT_NAME, "create file",
                                                          new Object[]{ "test" } );
        ActionRequest actionRequest2 = new ActionRequest( TEST_COMPONENT_NAME, "upload file",
                                                          new Object[]{ "test" } );
        ActionRequest actionRequest3 = new ActionRequest( TEST_COMPONENT_NAME, "action long",
                                                          new Object[]{ -120 } );
        actionRequests = new ArrayList<ActionRequest>();
        actionRequests.add( actionRequest1 );
        actionRequests.add( actionRequest2 );
        actionRequests.add( actionRequest3 );

        fileUploadActionRequest = new ActionRequest( TEST_COMPONENT_NAME, "upload file",
                                                     new Object[]{ "fileName" } );

        parameterDataProviders = new ListDataConfig[]{ new ListDataConfig( "fileName",
                                                                           new String[]{ "X1.txt", "X2.txt",
                                                                                         "X3.txt" },
                                                                           ParameterProviderLevel.PER_INVOCATION ),
                                                       new ListDataConfig( "fileName",
                                                                           new String[]{ "Y1.txt", "Y2.txt" },
                                                                           ParameterProviderLevel.PER_INVOCATION ),
                                                       new ListDataConfig( "fileName",
                                                                           new String[]{ "Z1.txt", "Z2.txt",
                                                                                         "Z3.txt", "Z4.txt" },
                                                                           ParameterProviderLevel.PER_INVOCATION ) };
    }

    @Before
    public void setUp() throws AgentException {

        ParameterizedInputActionClass.addedStrings = new HashMap<String, Integer>();
        ParameterizedInputActionClass.addedLongs = new HashSet<Long>();
    }

    @Test
    public void paremterizedStringDataPositive() throws Exception {

        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        loaderDataConfig.addParameterConfig( new RangeDataConfig( "fileName", "file{0}.txt", 1, 300 ) );
        loaderDataConfig.addParameterConfig( new RangeDataConfig( "fileName", "file{0}.txt", 1, 300 ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 5, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actionRequests, pattern, loaderDataConfig );

        assertEquals( 10, ParameterizedInputActionClass.getAddedStringsCount() );
        for( int i = 1; i < 6; i++ ) {
            assertEquals( 2, ( int ) ParameterizedInputActionClass.addedStrings.get( "file" + i + ".txt" ) );
        }
    }

    @Test
    public void paremterizedStringWithWrapUp() throws Exception {

        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        loaderDataConfig.addParameterConfig( new RangeDataConfig( "fileName", "file{0}.txt", 1, 4 ) );
        loaderDataConfig.addParameterConfig( new RangeDataConfig( "fileName", "file{0}.txt", 1, 4 ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 5, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actionRequests, pattern, loaderDataConfig );

        assertEquals( 10, ParameterizedInputActionClass.getAddedStringsCount() );

        //"file1.txt" one should be passed twice per method
        assertEquals( 4, ( int ) ParameterizedInputActionClass.addedStrings.get( "file1.txt" ) );

        for( int i = 2; i < 5; i++ ) {
            assertEquals( 2, ( int ) ParameterizedInputActionClass.addedStrings.get( "file" + i + ".txt" ) );
        }
    }

    @Test
    public void paremterizedIntegerDataPositive() throws Exception {

        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        loaderDataConfig.addParameterConfig( new RangeDataConfig( "milliseconds", 1, 300 ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 5, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actionRequests, pattern, loaderDataConfig );

        assertEquals( 5, ParameterizedInputActionClass.addedLongs.size() );
        for( int i = 1; i < 6; i++ ) {
            assertTrue( ParameterizedInputActionClass.addedLongs.contains( new Long( i ) ) );
        }
    }

    @Test
    public void paremterizedIntegerWithWrapUp() throws Exception {

        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        loaderDataConfig.addParameterConfig( new RangeDataConfig( "milliseconds", 1, 4 ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 5, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actionRequests, pattern, loaderDataConfig );

        assertEquals( 4, ParameterizedInputActionClass.addedLongs.size() );
        for( int i = 1; i < 5; i++ ) {
            assertTrue( ParameterizedInputActionClass.addedLongs.contains( new Long( i ) ) );
        }
    }

    @Test
    public void paremterizedVariousDataPositive() throws Exception {

        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        loaderDataConfig.addParameterConfig( new RangeDataConfig( "fileName", "file{0}.txt", 1, 300 ) );
        loaderDataConfig.addParameterConfig( new RangeDataConfig( "fileName", "file{0}.txt", 1, 300 ) );
        loaderDataConfig.addParameterConfig( new RangeDataConfig( "milliseconds", 1, 4 ) );

        AllAtOncePattern pattern = new AllAtOncePattern( 5, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actionRequests, pattern, loaderDataConfig );

        //check the strings
        assertEquals( 10, ParameterizedInputActionClass.getAddedStringsCount() );
        for( int i = 1; i < 6; i++ ) {
            assertEquals( 2, ( int ) ParameterizedInputActionClass.addedStrings.get( "file" + i + ".txt" ) );
        }

        //check the integers
        assertEquals( 4, ParameterizedInputActionClass.addedLongs.size() );
        for( int i = 1; i < 5; i++ ) {
            assertTrue( ParameterizedInputActionClass.addedLongs.contains( new Long( i ) ) );
        }
    }

    @Test
    public void oneThreadWithTwoInvocations() throws Exception {

        int nThreads = 1;
        int nInvocations = 2;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
            loaderDataConfig.addParameterConfig( parameterDataProviders[i] );
        }

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "Y2.txt" ) );
    }

    @Test
    public void oneThreadWithThreeInvocations() throws Exception {

        int nThreads = 1;
        int nInvocations = 3;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
            loaderDataConfig.addParameterConfig( parameterDataProviders[i] );
        }

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );

        //        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "Y1.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "Y2.txt" ) );

        //        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "Z1.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "Z2.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "Z3.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "Z4.txt" ) );
    }

    @Test
    public void twoThreadsWithTwoInvocations() throws Exception {

        int nThreads = 2;
        int nInvocations = 2;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
            loaderDataConfig.addParameterConfig( parameterDataProviders[i] );
        }

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
    }

    @Test
    public void twoThreadsWithThreeInvocations() throws Exception {

        int nThreads = 2;
        int nInvocations = 3;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
            loaderDataConfig.addParameterConfig( parameterDataProviders[i] );
        }

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "Z3.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "Z4.txt" ) );
    }

    @Test
    public void threeThreadsWithTwoInvocations() throws Exception {

        int nThreads = 3;
        int nInvocations = 2;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
            loaderDataConfig.addParameterConfig( parameterDataProviders[i] );
        }

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
    }

    @Test
    public void threeThreadsWithThreeInvocations() throws Exception {

        int nThreads = 3;
        int nInvocations = 3;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
            loaderDataConfig.addParameterConfig( parameterDataProviders[i] );
        }

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );

        assertNull( ParameterizedInputActionClass.addedStrings.get( "Z4.txt" ) );
    }

    @Test
    public void fourThreadsWithTwoInvocations() throws Exception {

        int nThreads = 4;
        int nInvocations = 2;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
            loaderDataConfig.addParameterConfig( parameterDataProviders[i] );
        }

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 2, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
    }

    @Test
    public void fourThreadsWithThreeInvocations() throws Exception {

        int nThreads = 4;
        int nInvocations = 3;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
            loaderDataConfig.addParameterConfig( parameterDataProviders[i] );
        }

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 2, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
    }

    @Test
    public void paramPresentInDataProvidersMoreTimeThanInTheInvokers() throws Exception {

        // A warning message should be logged saying a parameter is provided more time then it is used in the actions

        int nThreads = 1;
        int nInvocations = 2;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
        }

        loaderDataConfig.addParameterConfig( parameterDataProviders[0] );
        loaderDataConfig.addParameterConfig( parameterDataProviders[1] );
        loaderDataConfig.addParameterConfig( parameterDataProviders[2] );

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "Y2.txt" ) );
    }

    @Test
    public void paramPresentInDataProvidersLessTimeThanInTheInvokers() throws Exception {

        int nThreads = 1;
        int nInvocations = 3;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
        }

        loaderDataConfig.addParameterConfig( parameterDataProviders[0] );
        loaderDataConfig.addParameterConfig( parameterDataProviders[1] );

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "Y2.txt" ) );
    }

    @Test
    public void paramPresentOnlyOnceInDataProviders() throws Exception {

        int nThreads = 1;
        int nInvocations = 3;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
        }

        loaderDataConfig.addParameterConfig( parameterDataProviders[0] );

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
    }

    @Test
    public void paramPresentOnlyOnceInDataProvidersTwoThreads() throws Exception {

        int nThreads = 2;
        int nInvocations = 3;

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        for( int i = 0; i < nInvocations; i++ ) {
            actions.add( fileUploadActionRequest );
        }

        loaderDataConfig.addParameterConfig( parameterDataProviders[0] );

        AllAtOncePattern pattern = new AllAtOncePattern( nThreads, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( nInvocations * nThreads, ParameterizedInputActionClass.getAddedStringsCount() );

        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X1.txt" ) );
        assertEquals( 1, ( int ) ParameterizedInputActionClass.addedStrings.get( "X2.txt" ) );
        assertNull( ParameterizedInputActionClass.addedStrings.get( "X3.txt" ) );
    }

    @Test
    public void paramPresentInDataProvidersButNotInActions() throws Exception {

        List<ActionRequest> actions = new ArrayList<ActionRequest>();
        actions.add( actionRequests.get( 2 ) );

        LoaderDataConfig loaderDataConfig = new LoaderDataConfig();
        loaderDataConfig.addParameterConfig( parameterDataProviders[0] );

        AllAtOncePattern pattern = new AllAtOncePattern( 1, true );
        QueueExecutionStatistics.getInstance().initActionExecutionResults( "test 1" );
        actionHandler.executeActions( "IP", "test 1", -1, actions, pattern, loaderDataConfig );

        assertEquals( 0, ParameterizedInputActionClass.getAddedStringsCount() );
    }
}
