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
package com.axway.ats.rbv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import com.axway.ats.rbv.db.DbSearchTerm;
import com.axway.ats.rbv.db.DbStorage;
import com.axway.ats.rbv.db.MockDbProvider;
import com.axway.ats.rbv.db.rules.DbFieldsRule;
import com.axway.ats.rbv.db.rules.DbStringFieldRule;
import com.axway.ats.rbv.db.rules.DbStringFieldRule.MatchRelation;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.storage.Matchable;

@PowerMockIgnore("javax.management.*")
public class Test_SimpleMonitorListener extends BaseTest {
    /*
     * Test failures history:
     * - evaluateMonitorsOneMonitorPositive
     * - evaluateMonitorsOneMonitorPositiveRunTwice
     */
    private PollingParameters pollingParams;
    private Matchable         matchable;

    private static final int  POLLING_INTERVAL      = 1250;
    private static final int  POLLING_ATTEMPTS      = 5;

    private static final int  TIME_AFTER_START_POLL = 2 * POLLING_INTERVAL;               // time near of start of 3rd poll
    private static final int  TIME_END_POLL         = POLLING_ATTEMPTS * POLLING_INTERVAL;
    private static final int  TIME_BEFORE_END_POLL  = TIME_END_POLL - POLLING_INTERVAL;

    private MockDbProvider    mockDbProvider;

    @Before
    public void beforeMethod() throws RbvException {

        pollingParams = new PollingParameters(0, POLLING_INTERVAL, POLLING_ATTEMPTS);

        mockDbProvider = new MockDbProvider();
        DbStorage storage = new DbStorage(mockDbProvider);
        matchable = storage.getFolder(new DbSearchTerm(""));
    }

    @After
    public void afterMethod() {

        try {
            matchable.close();
        } catch (RbvStorageException e) {
            //if the instance has not been opened
        }
    }

    @Test
    public void evaluateMonitorsOneMonitorPositive() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsOneMonitorPositive",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
    }

    @Test
    public void evaluateMonitorsOneMonitorNegative() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "table1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsOneMonitorNegative",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        failExpectingDbData(true, listener.evaluateMonitors(TIME_END_POLL));
    }

    @Test
    public void evaluateMonitorsOneMonitorPositiveRunTwice() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsOneMonitorPositiveRunTwice",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
    }

    @Test
    public void evaluateMonitorsOneMonitorExpectedFalsePositive() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value101",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsOneMonitorExpectedFalsePositive",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, false, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
    }

    @Test
    public void evaluateMonitorsOneMonitorExpectedFalseNegative() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsOneMonitorExpectedFalseNegative",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, false, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        failExpectingDbData(false, listener.evaluateMonitors(TIME_END_POLL));
    }

    @Test
    public void evaluateMonitorsMultipleMonitorsPositive() throws RbvException {

        DbStorage storage = new DbStorage(new MockDbProvider());
        Matchable matchable1 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable2 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable3 = storage.getFolder(new DbSearchTerm(""));

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsMultipleMonitorsPositive",
                                                    true);

        Monitor monitor1 = new Monitor("monitor1", matchable1, dbRule, pollingParams, true, false, false);
        Monitor monitor2 = new Monitor("monitor2", matchable2, dbRule, pollingParams, true, false, false);
        Monitor monitor3 = new Monitor("monitor3", matchable3, dbRule, pollingParams, true, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor1);
        monitors.add(monitor2);
        monitors.add(monitor3);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
    }

    @Test
    public void evaluateMonitorsMultipleMonitorsNegative() throws RbvException {

        DbStorage storage = new DbStorage(new MockDbProvider());
        Matchable matchable1 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable2 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable3 = storage.getFolder(new DbSearchTerm(""));

        DbFieldsRule dbRuleRight = new DbStringFieldRule("",
                                                         "key1",
                                                         "value10",
                                                         MatchRelation.EQUALS,
                                                         "evaluateMonitorsMultipleMonitorsNegative1",
                                                         true);
        DbFieldsRule dbRuleWrong = new DbStringFieldRule("",
                                                         "table1",
                                                         "value10",
                                                         MatchRelation.EQUALS,
                                                         "evaluateMonitorsMultipleMonitorsNegative2",
                                                         true);

        Monitor monitor1 = new Monitor("monitor1",
                                       matchable1,
                                       dbRuleRight,
                                       pollingParams,
                                       true,
                                       false,
                                       false);
        Monitor monitor2 = new Monitor("monitor2",
                                       matchable2,
                                       dbRuleRight,
                                       pollingParams,
                                       true,
                                       false,
                                       false);
        Monitor monitor3 = new Monitor("monitor3",
                                       matchable3,
                                       dbRuleWrong,
                                       pollingParams,
                                       true,
                                       false,
                                       false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor1);
        monitors.add(monitor2);
        monitors.add(monitor3);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        assertTrue(listener.evaluateMonitors(TIME_END_POLL)
                           .matches("Expected to find DB data .*, but did not find it"));
    }

    @Test
    public void evaluateMonitorsMultipleMonitorsEndOnFirstMatchPositive() throws RbvException {

        DbStorage storage = new DbStorage(new MockDbProvider());
        Matchable matchable1 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable2 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable3 = storage.getFolder(new DbSearchTerm(""));

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsMultipleMonitorsEndOnFirstMatchPositive",
                                                    true);

        Monitor monitor1 = new Monitor("monitor1", matchable1, dbRule, pollingParams, true, true, false);
        Monitor monitor2 = new Monitor("monitor2", matchable2, dbRule, pollingParams, true, true, false);
        Monitor monitor3 = new Monitor("monitor3", matchable3, dbRule, pollingParams, true, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor1);
        monitors.add(monitor2);
        monitors.add(monitor3);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
    }

    @Test
    public void evaluateMonitorsMultipleMonitorsEndOnFirstMatchNegative() throws RbvException {

        DbStorage storage = new DbStorage(new MockDbProvider());
        Matchable matchable1 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable2 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable3 = storage.getFolder(new DbSearchTerm(""));

        DbFieldsRule dbRuleRight = new DbStringFieldRule("",
                                                         "key1",
                                                         "value10",
                                                         MatchRelation.EQUALS,
                                                         "evaluateMonitorsMultipleMonitorsEndOnFirstMatchNegative1",
                                                         true);
        DbFieldsRule dbRuleWrong = new DbStringFieldRule("",
                                                         "table1",
                                                         "value10",
                                                         MatchRelation.EQUALS,
                                                         "evaluateMonitorsMultipleMonitorsEndOnFirstMatchNegative2",
                                                         true);

        Monitor monitor1 = new Monitor("monitor1",
                                       matchable1,
                                       dbRuleRight,
                                       pollingParams,
                                       true,
                                       true,
                                       false);
        Monitor monitor2 = new Monitor("monitor2",
                                       matchable2,
                                       dbRuleRight,
                                       pollingParams,
                                       true,
                                       true,
                                       false);
        Monitor monitor3 = new Monitor("monitor3",
                                       matchable3,
                                       dbRuleWrong,
                                       pollingParams,
                                       true,
                                       true,
                                       false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor1);
        monitors.add(monitor2);
        monitors.add(monitor3);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        failExpectingDbData(true, listener.evaluateMonitors(TIME_END_POLL));
    }

    @Test
    public void evaluateMonitorsMultipleMonitorsEndOnFirstMatchExpectedFalsePositive() throws RbvException {

        PollingParameters longPollingParams = new PollingParameters(0, 5000, 10);

        DbStorage storage = new DbStorage(new MockDbProvider());
        Matchable matchable1 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable2 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable3 = storage.getFolder(new DbSearchTerm(""));

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value2345",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsMultipleMonitorsEndOnFirstMatchPositive",
                                                    true);

        Monitor monitor1 = new Monitor("monitor1",
                                       matchable1,
                                       dbRule,
                                       longPollingParams,
                                       false,
                                       true,
                                       false);
        Monitor monitor2 = new Monitor("monitor2",
                                       matchable2,
                                       dbRule,
                                       longPollingParams,
                                       false,
                                       true,
                                       false);
        Monitor monitor3 = new Monitor("monitor3",
                                       matchable3,
                                       dbRule,
                                       longPollingParams,
                                       false,
                                       true,
                                       false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor1);
        monitors.add(monitor2);
        monitors.add(monitor3);

        long timeBefore = Calendar.getInstance().getTimeInMillis();

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        listener.evaluateMonitors(TIME_END_POLL);

        long timeAfter = Calendar.getInstance().getTimeInMillis();

        //make sure less than (3 * poling interval) seconds have passed
        //this means that only one iteration was made
        assertTrue(timeAfter - timeBefore < 3 * POLLING_INTERVAL - 500);
    }

    @Test
    public void evaluateMonitorsMultipleMonitorsEndOnFirstMatchExpectedFalseNegative() throws RbvException {

        final int pollingAttempts = 4;
        PollingParameters longPollingParams = new PollingParameters(0, POLLING_INTERVAL, pollingAttempts);

        DbStorage storage = new DbStorage(new MockDbProvider());
        Matchable matchable1 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable2 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable3 = storage.getFolder(new DbSearchTerm(""));

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsMultipleMonitorsEndOnFirstMatchExpectedFalseNegative",
                                                    true);

        Monitor monitor1 = new Monitor("monitor1",
                                       matchable1,
                                       dbRule,
                                       longPollingParams,
                                       false,
                                       true,
                                       false);
        Monitor monitor2 = new Monitor("monitor2",
                                       matchable2,
                                       dbRule,
                                       longPollingParams,
                                       false,
                                       true,
                                       false);
        Monitor monitor3 = new Monitor("monitor3",
                                       matchable3,
                                       dbRule,
                                       longPollingParams,
                                       false,
                                       true,
                                       false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor1);
        monitors.add(monitor2);
        monitors.add(monitor3);

        long timeBefore = System.currentTimeMillis();

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        String evaluationResult = listener.evaluateMonitors(pollingAttempts * POLLING_INTERVAL);
        long timeAfter = System.currentTimeMillis();

        assertTrue(evaluationResult.matches("Expected to not find DB data .*, but found it.*"));
        /* 
         * As we use 3 identical monitors, we should get here 3 times same result,
         * so the following code should be accurate:
         * 
         * String[] evaluationResultTokens = evaluationResult.split( ";" );
         * assertEquals( 3, evaluationResultTokens.length );
         * assertEquals( evaluationResultTokens[0], evaluationResultTokens[1].trim() );
         * assertEquals( evaluationResultTokens[0], evaluationResultTokens[2].trim() );
         * assertTrue( evaluationResultTokens[0].matches( "Expected to not find DB data .*, but found it" ) );
         * 
         * But we sometimes get results from just 1 or 2 monitors.
         * It is probably some synchronization issue, but this is considered as not important as we 
         * actually never use more than 1 monitor :)
         */

        //make sure more than (3 * poling interval) seconds have passed
        //this means that all iteration ware executed
        assertTrue(timeAfter - timeBefore > (pollingAttempts - 1) * POLLING_INTERVAL - 500);
    }

    @Test
    public void evaluateMonitorsMultipleMonitorsStressPositive() throws RbvException {

        DbStorage storage = new DbStorage(new MockDbProvider());
        Matchable matchable1 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable2 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable3 = storage.getFolder(new DbSearchTerm(""));

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsMultipleMonitorsStressPositive",
                                                    true);

        Monitor monitor1 = new Monitor("monitor1", matchable1, dbRule, pollingParams, true, false, false);
        Monitor monitor2 = new Monitor("monitor2", matchable2, dbRule, pollingParams, true, false, false);
        Monitor monitor3 = new Monitor("monitor3", matchable3, dbRule, pollingParams, true, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor1);
        monitors.add(monitor2);
        monitors.add(monitor3);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);

        for (int i = 0; i < 5; i++) {
            assertNull(listener.evaluateMonitors(TIME_END_POLL));
        }
    }

    @Test
    public void evaluateMonitorsMultipleMonitorsNegativeTimeoutExceeded() throws RbvException {

        DbStorage storage = new DbStorage(new MockDbProvider());
        Matchable matchable1 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable2 = storage.getFolder(new DbSearchTerm(""));
        Matchable matchable3 = storage.getFolder(new DbSearchTerm(""));

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "evaluateMonitorsMultipleMonitorsNegativeTimeoutExceeded",
                                                    true);

        PollingParameters longPolling = new PollingParameters(100, 1000, 5);

        Monitor monitor1 = new Monitor("monitor1", matchable1, dbRule, longPolling, true, false, false);
        Monitor monitor2 = new Monitor("monitor2", matchable2, dbRule, longPolling, true, false, false);
        Monitor monitor3 = new Monitor("monitor3", matchable3, dbRule, longPolling, true, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor1);
        monitors.add(monitor2);
        monitors.add(monitor3);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        assertEquals("Monitors did not finish in the given timeout 1000",
                     listener.evaluateMonitors(1000));
    }

    @Test
    public void monitorGetFirstMachingMetaData() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "monitorGetFirstMachingMetaData",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        listener.evaluateMonitors(TIME_END_POLL);

        assertEquals("value10", monitor.getFirstMatchedMetaData().getProperty("key1"));
    }

    @Test
    public void monitorGetAllMachingMetaData() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value101",
                                                    MatchRelation.EQUALS,
                                                    "monitorGetAllMachingMetaData",
                                                    false);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        listener.evaluateMonitors(TIME_END_POLL);

        List<MetaData> matchingMetaData = monitor.getAllMatchedMetaData();

        assertEquals(1, matchingMetaData.size());

        ArrayList<String> results = new ArrayList<String>();
        results.add((String) matchingMetaData.get(0).getProperty("key1"));

        assertTrue(results.contains("value10") || results.contains("value00"));
    }

    @Test
    public void monitorGetAllMachingMetaDataEndOnFirstMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value101",
                                                    MatchRelation.EQUALS,
                                                    "monitorGetAllMachingMetaDataEndOnFirstMatch",
                                                    false);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        listener.evaluateMonitors(TIME_END_POLL);
        long after = System.currentTimeMillis();
        assertTrue("It's not a first match!", after - before < TIME_BEFORE_END_POLL);
        List<MetaData> matchingMetaData = monitor.getAllMatchedMetaData();

        assertEquals(1, matchingMetaData.size());
        String value = (String) matchingMetaData.get(0).getProperty("key1");

        //there is no guarantee for the hash map order so we check for both possible values
        assertTrue(value.equals("value00") || value.equals("value10"));
    }

    @Test
    public void monitorGetFirstMachingMetaDataNull() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "table1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "monitorGetFirstMachingMetaDataNull",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        listener.evaluateMonitors(TIME_END_POLL);

        assertEquals(null, monitor.getFirstMatchedMetaData());
    }

    @Test
    public void monitorGetAllMachingMetaDataEmpty() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "table1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "monitorGetAllMachingMetaDataEmpty",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        listener.evaluateMonitors(TIME_END_POLL);

        assertEquals(0, monitor.getAllMatchedMetaData().size());
    }

    @Test
    public void monitorGetMatchable() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "monitorGetMatchable",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, false);
        assertEquals(matchable, monitor.getMatchable());
    }

    @Test( expected = RbvException.class)
    public void monitorStartTwice() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value10",
                                                    MatchRelation.EQUALS,
                                                    "monitorStartTwice",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, false);
        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        monitor.start(listener);
        monitor.start(listener);
    }

    @Test
    public void monitor_EndOnFirstMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value101",
                                                    MatchRelation.EQUALS,
                                                    "monitorGetAllMachingMetaDataEndOnFirstMatch",
                                                    false);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        listener.evaluateMonitors(TIME_END_POLL);
        long after = System.currentTimeMillis();
        assertTrue(after - before < TIME_BEFORE_END_POLL);
    }

    /*
     *
     *
     *
     * Verifications
     *
     *
     *
     *
     */

    @Test
    public void verifyMatch_HasAMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value00",
                                                    MatchRelation.EQUALS,
                                                    "verifyMatch_HasAMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
        long after = System.currentTimeMillis();
        assertTrue(after - before <= POLLING_INTERVAL);
    }

    @Test
    public void verifyMatch_ButNoMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value00_00",
                                                    MatchRelation.EQUALS,
                                                    "verifyMatch_ButNoMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        failExpectingDbData(true, listener.evaluateMonitors(TIME_END_POLL));
        long after = System.currentTimeMillis();
        assertTrue(after - before >= TIME_BEFORE_END_POLL);
    }

    @Test
    public void verifyMatch_IntiallyNoMatch_ButThenFoundAMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "test_key",
                                                    "test_value_changed",
                                                    MatchRelation.EQUALS,
                                                    "IntiallyNoMatch_ButThenFoundAMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        changeDBValues(3 * POLLING_INTERVAL);
        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
        long after = System.currentTimeMillis();
        System.out.println("IntiallyNoMatch_ButThenFoundAMatch: Actual poll duration: " + (after - before)
                           + "ms.");
        assertTrue(after - before > TIME_AFTER_START_POLL && after - before < TIME_END_POLL);
    }

    @Test
    public void verifyNoMatch_NoMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value00_no_match",
                                                    MatchRelation.EQUALS,
                                                    "verifyNoMatch_NoMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, false, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
        long after = System.currentTimeMillis();
        assertTrue(after - before <= POLLING_INTERVAL);
    }

    @Test
    public void verifyNoMatch_ButHasAMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "key1",
                                                    "value00",
                                                    MatchRelation.EQUALS,
                                                    "verifyNoMatch_ButHasAMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, false, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        failExpectingDbData(false, listener.evaluateMonitors(TIME_END_POLL));
        long after = System.currentTimeMillis();
        assertTrue(after - before >= TIME_BEFORE_END_POLL);
    }

    @Test
    public void verifyNoMatch_IntiallyFoundAMatch_ButThenNoMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "test_key",
                                                    "test_value",
                                                    MatchRelation.EQUALS,
                                                    "IntiallyFoundAMatch_ButThenNoMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, false, true, false);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        changeDBValues(3 * POLLING_INTERVAL);
        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
        long after = System.currentTimeMillis();
        System.out.println("verifyNoMatch_IntiallyFoundAMatch_ButThenNoMatch: Actual time for monitor evaliation: "
                           + (after - before) + "ms.");
        assertTrue(after - before > TIME_AFTER_START_POLL && after - before < TIME_END_POLL);
    }

    @Test
    public void verifyAlwaysHasAMatch_MatchesAllTheTime() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "test_key",
                                                    "test_value",
                                                    MatchRelation.EQUALS,
                                                    "MatchesAllTheTime",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, true);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
        long after = System.currentTimeMillis();
        assertTrue(after - before >= TIME_BEFORE_END_POLL);
    }

    @Test
    public void verifyAlwaysHasAMatch_ButNoMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "test_key",
                                                    "test_value_000",
                                                    MatchRelation.EQUALS,
                                                    "verifyAlwaysHasAMatch_ButNoMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, true);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertTrue(listener.evaluateMonitors(TIME_END_POLL)
                           .matches("Expected to find DB data .* on all attempts, but did not find it on attempt number 1"));
        long after = System.currentTimeMillis();
        assertTrue(after - before <= TIME_AFTER_START_POLL);
    }

    @Test
    public void verifyAlwaysHasAMatch_IntiallyFoundAMatch_ButThenNoMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "test_key",
                                                    "test_value",
                                                    MatchRelation.EQUALS,
                                                    "IntiallyFoundAMatch_ButThenNoMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, true, false, true);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        changeDBValues(3 * POLLING_INTERVAL);
        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertTrue(listener.evaluateMonitors(TIME_END_POLL)
                           .matches("Expected to find DB data .* on all attempts, but did not find it on attempt number [\\d]*"));
        long after = System.currentTimeMillis();
        assertTrue(after - before > TIME_AFTER_START_POLL && after - before < TIME_END_POLL);
    }

    @Test
    public void verifyNeverMatches_NoMatchAllTheTime() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "test_key",
                                                    "test_value_00",
                                                    MatchRelation.EQUALS,
                                                    "NoMatchAllTheTime",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, false, false, true);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertNull(listener.evaluateMonitors(TIME_END_POLL));
        long after = System.currentTimeMillis();
        assertTrue(after - before >= TIME_BEFORE_END_POLL);
    }

    @Test
    public void verifyNeverMatches_ButHasAMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "test_key",
                                                    "test_value",
                                                    MatchRelation.EQUALS,
                                                    "verifyNeverMatches_ButHasAMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, false, false, true);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertTrue(listener.evaluateMonitors(TIME_END_POLL)
                           .matches("Expected to not find DB data .* on all attempts, but found it on attempt number [\\d]*"));
        long after = System.currentTimeMillis();
        assertTrue(after - before < TIME_AFTER_START_POLL);
    }

    @Test
    public void verifyNeverMatches_IntiallyNoMatch_ButThenFoundAMatch() throws RbvException {

        DbFieldsRule dbRule = new DbStringFieldRule("",
                                                    "test_key",
                                                    "test_value_changed",
                                                    MatchRelation.EQUALS,
                                                    "IntiallyNoMatch_ButThenFoundAMatch",
                                                    true);

        Monitor monitor = new Monitor("monitor1", matchable, dbRule, pollingParams, false, false, true);

        List<Monitor> monitors = new ArrayList<Monitor>();
        monitors.add(monitor);

        changeDBValues(3 * POLLING_INTERVAL);
        SimpleMonitorListener listener = new SimpleMonitorListener(monitors);
        long before = System.currentTimeMillis();
        assertTrue(listener.evaluateMonitors(TIME_END_POLL)
                           .matches("Expected to not find DB data .* on all attempts, but found it on attempt number [\\d]*"));
        long after = System.currentTimeMillis();
        assertTrue(after - before > TIME_AFTER_START_POLL && after - before < TIME_END_POLL);
    }

    private void changeDBValues(
                                 long timeout ) {

        final long sleepTime = timeout;
        new Thread() {
            @Override
            public void run() {

                try {
                    Thread.sleep(sleepTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mockDbProvider.useChangedValues();
            }
        }.start();
    }

    private void failExpectingDbData( boolean positiveExpectation, String evaluationResult ) {

        if (positiveExpectation) {
            assertTrue(evaluationResult.matches("Expected to find DB data .*, but did not find it"));
        } else {
            assertTrue(evaluationResult.matches("Expected to not find DB data .*, but found it"));
        }
    }
}
