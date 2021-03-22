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
package com.axway.ats.log.autodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.core.log.AtsLog4jLogger;
import com.axway.ats.log.autodb.exceptions.CheckpointAlreadyStartedException;
import com.axway.ats.log.autodb.exceptions.CheckpointNotStartedException;
import com.axway.ats.log.autodb.exceptions.LoadQueueAlreadyStartedException;
import com.axway.ats.log.autodb.exceptions.LoggingException;
import com.axway.ats.log.autodb.exceptions.NoSuchLoadQueueException;
import com.axway.ats.log.autodb.exceptions.ThreadAlreadyRegisteredWithLoadQueueException;
import com.axway.ats.log.autodb.exceptions.ThreadNotRegisteredWithLoadQueue;

public class Test_LoadQueueState {

    static {
        AtsLog4jLogger.setLog4JConsoleLoggingOnly();
    }

    private static final String  NAME_THREAD_14000 = "thread-14000";

    private LoadQueuesState      loadQueueState;

    private final long           START_TIME        = System.currentTimeMillis();
    private final CheckpointInfo CHECKPOINT1       = new CheckpointInfo("checkpoint 1", 0, 0, START_TIME);

    @Before
    public void setUp() {

        loadQueueState = new LoadQueuesState();
    }

    @Test
    public void addLoadQueuePositive() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 12343);
    }

    @Test( expected = LoadQueueAlreadyStartedException.class)
    public void addLoadQueueNegative() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 12343);
        loadQueueState.addLoadQueue("load queue 1", 12343);
    }

    @Test
    public void getLoadQueueIdPositive() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 12343);
        loadQueueState.getLoadQueueId("load queue 1");
    }

    @Test( expected = NoSuchLoadQueueException.class)
    public void getLoadQueueIdNegative() throws LoggingException {

        loadQueueState.getLoadQueueId("load queue 1");
    }

    @Test( expected = ThreadNotRegisteredWithLoadQueue.class)
    public void getLoadQueueIdNegativeThreadNotRegistered() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        assertEquals(loadQueueState.getLoadQueueIdForThread(NAME_THREAD_14000), 123);
    }

    @Test
    public void removeLoadQueuePositive() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 12343);
        loadQueueState.removeLoadQueue("load queue 1", 12343);
    }

    @Test( expected = NoSuchLoadQueueException.class)
    public void removeLoadQueueNegative() throws LoggingException {

        loadQueueState.removeLoadQueue("load queue 1", 12343);
    }

    @Test
    public void isLoadQueueRunninPositive() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 12343);

        assertTrue(loadQueueState.isLoadQueueRunning("load queue 1"));
        assertFalse(loadQueueState.isLoadQueueRunning("fasfda"));
    }

    @Test
    public void registerThreadWithLoadQueuePositive() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        loadQueueState.registerThreadWithLoadQueue(NAME_THREAD_14000, 123);
        assertEquals(loadQueueState.getLoadQueueIdForThread(NAME_THREAD_14000), 123);

        loadQueueState.registerThreadWithLoadQueue("thread-14001", 123);
        assertEquals(loadQueueState.getLoadQueueIdForThread("thread-14001"), 123);

        loadQueueState.registerThreadWithLoadQueue("thread-14002", 123);
        assertEquals(loadQueueState.getLoadQueueIdForThread("thread-14002"), 123);
    }

    @Test( expected = NoSuchLoadQueueException.class)
    public void registerThreadWithLoadQueueNegativeNoSuchLoadQueue() throws LoggingException {

        loadQueueState.registerThreadWithLoadQueue(NAME_THREAD_14000, 123);
    }

    @Test( expected = ThreadAlreadyRegisteredWithLoadQueueException.class)
    public void registerThreadWithLoadQueueNegativeThreadAlreadyRegistered() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        loadQueueState.registerThreadWithLoadQueue(NAME_THREAD_14000, 123);
        loadQueueState.registerThreadWithLoadQueue(NAME_THREAD_14000, 123);
    }

    @Test
    public void startEndCheckpointPositive() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        loadQueueState.registerThreadWithLoadQueue(NAME_THREAD_14000, 123);

        //try more than one checkpoint
        for (int i = 0; i < 3; i++) {
            loadQueueState.startCheckpoint(CHECKPOINT1, NAME_THREAD_14000);
            loadQueueState.endCheckpoint(NAME_THREAD_14000, CHECKPOINT1.getName(), START_TIME + 150);
        }
    }

    /**
     * Positive case with two nested checkpoints
     */
    @Test
    public void start2RunningCheckpointsPositive() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        loadQueueState.registerThreadWithLoadQueue(NAME_THREAD_14000, 123);

        //try more than one checkpoint
        loadQueueState.startCheckpoint(CHECKPOINT1, NAME_THREAD_14000);

        CheckpointInfo checkpointInfo2 = new CheckpointInfo("Checkpoint 2", 0, 0, 123L);
        loadQueueState.startCheckpoint(checkpointInfo2, NAME_THREAD_14000);

        loadQueueState.endCheckpoint(NAME_THREAD_14000, checkpointInfo2.getName(), START_TIME + 150);
        loadQueueState.endCheckpoint(NAME_THREAD_14000, CHECKPOINT1.getName(), START_TIME + 150);
    }

    @Test( expected = ThreadNotRegisteredWithLoadQueue.class)
    public void startCheckpointNegativeThreadNotRegistered() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        loadQueueState.startCheckpoint(CHECKPOINT1, NAME_THREAD_14000);
    }

    @Test( expected = CheckpointAlreadyStartedException.class)
    public void startCheckpointNegativeAlreadyStarted() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        loadQueueState.registerThreadWithLoadQueue(NAME_THREAD_14000, 123);

        loadQueueState.startCheckpoint(CHECKPOINT1, NAME_THREAD_14000);
        loadQueueState.startCheckpoint(CHECKPOINT1, NAME_THREAD_14000);
    }

    @Test( expected = ThreadNotRegisteredWithLoadQueue.class)
    public void endCheckpointNegativeThreadNotRegistered() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        loadQueueState.endCheckpoint(NAME_THREAD_14000, "checkpoint1", 0);
    }

    @Test( expected = CheckpointNotStartedException.class)
    public void endCheckpointNegativeAlreadyStarted() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        loadQueueState.registerThreadWithLoadQueue(NAME_THREAD_14000, 123);

        loadQueueState.endCheckpoint(NAME_THREAD_14000, "checkpoint1", 0);
    }

    /**
     * Started one checkpoint and try to end another not started
     * @throws LoggingException
     */
    @Test( expected = CheckpointNotStartedException.class)
    public void endAnotherCheckpointNegativeAlreadyStarted() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        loadQueueState.registerThreadWithLoadQueue(NAME_THREAD_14000, 123);

        loadQueueState.startCheckpoint(CHECKPOINT1, NAME_THREAD_14000);
        loadQueueState.endCheckpoint(NAME_THREAD_14000, "checkpointNotExisting", 0);
    }

    @Test
    public void clearAllPositive() throws LoggingException {

        loadQueueState.addLoadQueue("load queue 1", 123);
        assertTrue(loadQueueState.isLoadQueueRunning("load queue 1"));

        loadQueueState.clearAll();
        assertFalse(loadQueueState.isLoadQueueRunning("load queue 1"));
    }
}
