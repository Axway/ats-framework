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
package com.axway.ats.log.autodb.model;

/**
 * Type of the logging event
 */
public enum LoggingEventType {

    /**
     * Start a run
     */
    START_RUN,

    /**
     * End a run
     */
    END_RUN,

    /**
     * Update a run
     */
    UPDATE_RUN,

    /**
     * Add run meta info
     */
    ADD_RUN_METAINFO,

    /**
     * Start an AfterSuite invocation
     * */
    START_AFTER_SUITE,

    /**
     * End an AfterSuite invocation
     * */
    END_AFTER_SUITE,

    /**
     * Start a suite
     */
    START_SUITE,

    /**
     * End a suite
     */
    END_SUITE,

    /**
     * Update a suite
     */
    UPDATE_SUITE,

    /**
     * Start an AfterClass invocation
     * */
    START_AFTER_CLASS,

    /**
     * End an AfterClass invocation
     * */
    END_AFTER_CLASS,

    /**
     * Start an AfterMethod invocation
     * */
    START_AFTER_METHOD,

    /**
     * End an AfterMethod invocation
     * */
    END_AFTER_METHOD,

    /**
     * Clear scenario meta info
     */
    CLEAR_SCENARIO_METAINFO,

    /**
     * Add scenario meta info
     */
    ADD_SCENARIO_METAINFO,

    /**
     * Start test case
     */
    START_TEST_CASE,

    /**
     * End test case
     */
    END_TEST_CASE,

    /**
     * Update a testcase
     * */
    UPDATE_TEST_CASE,
    /**
     * Delete a test case
     */
    DELETE_TEST_CASE,

    /**
     * Add testcase meta info
     */
    ADD_TESTCASE_METAINFO,

    /**
     * Get the state of the current test case (label and type)
     */
    GET_CURRENT_TEST_CASE_STATE,

    /**
     * Join test case
     */
    JOIN_TEST_CASE,

    /**
     * Leave test case
     */
    LEAVE_TEST_CASE,

    /**
     * Remember performance load queue state
     */
    REMEMBER_LOADQUEUE_STATE,

    /**
     * Clean up performance load queue state
     */
    CLEANUP_LOADQUEUE_STATE,

    /**
     * End performance load queue
     */
    END_LOADQUEUE,

    /**
     * Register a particular thread with a load queue
     */
    REGISTER_THREAD_WITH_LOADQUEUE,

    /**
     * Start a checkpoint
     */
    START_CHECKPOINT,

    /**
     * End checkpoint
     */
    END_CHECKPOINT,

    /**
     * Insert a checkpoint
     */
    INSERT_CHECKPOINT,

    /**
     * Set the checkpoint log level
     */
    SET_CHECKPOINT_LOG_LEVEL,

    /**
     * Insert a system statistic
     */
    INSERT_SYSTEM_STAT,

    /**
     * Insert an user activity statistic
     */
    INSERT_USER_ACTIVITY_STAT,

    /**
     *
     */
    INSERT_MESSAGE
}
