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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.axway.ats.log.autodb.exceptions.CheckpointAlreadyStartedException;
import com.axway.ats.log.autodb.exceptions.CheckpointNotStartedException;
import com.axway.ats.log.autodb.exceptions.LoadQueueAlreadyStartedException;
import com.axway.ats.log.autodb.exceptions.NoSuchLoadQueueException;
import com.axway.ats.log.autodb.exceptions.ThreadAlreadyRegisteredWithLoadQueueException;
import com.axway.ats.log.autodb.exceptions.ThreadNotRegisteredWithLoadQueue;

/**
 * Keeps queue info as names and DB IDs, 
 * associating running threads and checkpoints running for each thread.
 */
public class LoadQueuesState {

    /**
     * This map holds the IDs of all started queues:
     *     Map<queue name, queue DB ID>
     */
    private Map<String, Integer>             queueNamesToDbIds;

    /**
     * Each thread must first be registered with a queue if a checkpoint
     * is to be started.
     *
     * This map keeps all threads per queue:
     *     Map<queue DB ID, List<thread name>>
     */
    private Map<Integer, List<String>>       threadsPerQueue;

    /**
     * This map keeps current checkpoints per thread. Several started at particular moment are supported.
     * This allows to keep track of nested or interweaving tasks/checkpoints.
     *     Map<thread name, Checkpoint Info>
     */
    private Map<String, Set<CheckpointInfo>> checkpointsPerThread;

    /**
     * Constructor
     */
    public LoadQueuesState() {

        //use tree maps for better performance when searching
        this.queueNamesToDbIds = new TreeMap<String, Integer>();
        this.threadsPerQueue = new TreeMap<Integer, List<String>>();
        this.checkpointsPerThread = new HashMap<String, Set<CheckpointInfo>>();
    }

    /**
     * Add a load queue to the currently executed load queues
     *
     * @param name name of the load queue
     * @param dbId the load queue id
     * @throws LoadQueueAlreadyStartedException if the load queue has already been added
     */
    public synchronized void addLoadQueue( String name, int dbId ) throws LoadQueueAlreadyStartedException {

        if (queueNamesToDbIds.containsKey(name)) {
            throw new LoadQueueAlreadyStartedException(name);
        }

        queueNamesToDbIds.put(name, dbId);
        threadsPerQueue.put(dbId, new ArrayList<String>());
    }

    /**
     * Remove the load queue from the list of active load queues
     *
     * @param name name of the load queue
     * @param id the load queue id
     * @throws NoSuchLoadQueueException if such load queue has not been started
     */
    public synchronized void removeLoadQueue( String name, int id ) throws NoSuchLoadQueueException {

        if (!queueNamesToDbIds.containsKey(name)) {
            throw new NoSuchLoadQueueException(name);
        }

        queueNamesToDbIds.remove(name);
        threadsPerQueue.remove(id);
    }

    /**
     * Check if a load queue with the given name is in the list of active load queues
     *
     * @param name name of the load queue
     * @return true if this load queue is in the list
     */
    public synchronized boolean isLoadQueueRunning( String name ) {

        return queueNamesToDbIds.containsKey(name);
    }

    /**
     * Register the given thread with a load queue. All checkpoints coming from this
     * thread will be assigned to the specified load queue
     *
     * @param threadName name of the thread to register
     * @param loadQueueId the load queue id
     * @throws NoSuchLoadQueueException if such load queue has not been started
     * @throws ThreadAlreadyRegisteredWithLoadQueueException if this thread has already been registered
     */
    public synchronized void registerThreadWithLoadQueue( String threadName,
                                                          int loadQueueId ) throws NoSuchLoadQueueException,
                                                                            ThreadAlreadyRegisteredWithLoadQueueException {

        List<String> threadNames = threadsPerQueue.get(loadQueueId);
        if (threadNames == null) {
            throw new NoSuchLoadQueueException(loadQueueId);
        }

        if (threadNames.contains(threadName)) {
            throw new ThreadAlreadyRegisteredWithLoadQueueException(threadName);
        }

        threadNames.add(threadName);
        clearThreadAllCheckpoints(threadName);
    }

    /**
     * Return the id of the load queue with the specified name
     *
     * @param loadQueueName the name of the load queue to look for
     * @return the load queue id
     * @throws NoSuchLoadQueueException if such load queue has not been started
     */
    public synchronized int getLoadQueueId( String loadQueueName ) throws NoSuchLoadQueueException {

        if (!queueNamesToDbIds.containsKey(loadQueueName)) {
            throw new NoSuchLoadQueueException(loadQueueName);
        }

        return queueNamesToDbIds.get(loadQueueName);
    }

    /**
     * Return the id of the load queue which this thread is assigned to
     * @param threadName this thread name
     * @return
     * @throws ThreadNotRegisteredWithLoadQueue
     */
    public synchronized int getLoadQueueIdForThread( String threadName ) throws ThreadNotRegisteredWithLoadQueue {

        for (Entry<Integer, List<String>> loadQueueEntry : threadsPerQueue.entrySet()) {
            List<String> threads = loadQueueEntry.getValue();
            for (String thread : threads) {
                if (thread.equals(threadName)) {
                    return loadQueueEntry.getKey();
                }
            }
        }

        throw new ThreadNotRegisteredWithLoadQueue(threadName);
    }

    /**
     * Start a checkpoint
     *
     * @param startedCheckpointInfo info about this checkpoint. It is expected to already be persisted in the DB
     * @param threadName name of the thread which start the checkpoint
     * @throws ThreadNotRegisteredWithLoadQueue if the thread which tries to start the checkpoint
     * is not registered with the checkpoint
     * @throws CheckpointAlreadyStartedException if the checkpoint has been started in this thread already
     */
    public synchronized void startCheckpoint( CheckpointInfo startedCheckpointInfo,
                                              String threadName ) throws ThreadNotRegisteredWithLoadQueue,
                                                                  CheckpointAlreadyStartedException {

        Set<CheckpointInfo> currentCheckpointInfoSet = checkpointsPerThread.get(threadName);
        if (currentCheckpointInfoSet == null) {
            throw new ThreadNotRegisteredWithLoadQueue(threadName);
        }
        CheckpointInfo checkpointInfoWithThisName = null;
        for (CheckpointInfo checkpointInfo : currentCheckpointInfoSet) {
            if (checkpointInfo.getName().equals(startedCheckpointInfo.getName())) {
                checkpointInfoWithThisName = checkpointInfo;
                break;
            }
        }

        if (checkpointInfoWithThisName != null && checkpointInfoWithThisName.isRunning()) {
            throw new CheckpointAlreadyStartedException(startedCheckpointInfo.getName(), threadName);
        }

        registerCheckpointWithThread(threadName, startedCheckpointInfo);
    }

    /**
     * End a checkpoint
     *
     * @param threadName name of the thread which ends the checkpoint
     * @param checkpointName the name of the checkpoint
     * @param endTime the time at which this checkpoint ended
     * @throws ThreadNotRegisteredWithLoadQueue if the thread which tries to end the checkpoint
     * is not registered with the checkpoint
     * @throws CheckpointNotStartedException if the checkpoint has not been started at all
     */
    public synchronized CheckpointInfo endCheckpoint( String threadName, String checkpointName,
                                                      long endTime ) throws ThreadNotRegisteredWithLoadQueue,
                                                                     CheckpointNotStartedException {

        Set<CheckpointInfo> currentCheckpointInfoSet = checkpointsPerThread.get(threadName);
        if (currentCheckpointInfoSet == null) {
            throw new ThreadNotRegisteredWithLoadQueue(threadName);
        }

        Iterator<CheckpointInfo> iterator = currentCheckpointInfoSet.iterator();
        CheckpointInfo currentCheckpointInfo = null, tempCheckpoint = null;
        while (iterator.hasNext() && currentCheckpointInfo == null) {
            tempCheckpoint = (CheckpointInfo) iterator.next();
            if (checkpointName.equals(tempCheckpoint.getName())) {
                currentCheckpointInfo = tempCheckpoint;
            }
        }

        if (currentCheckpointInfo == null || !currentCheckpointInfo.isRunning()) {
            throw new CheckpointNotStartedException(checkpointName, threadName);
        }

        // clean current checkpoint from the set associated to this thread,
        // but return this checkpoint as we need it when ending the checkpoint in the DB
        clearThreadCheckpoint(threadName, currentCheckpointInfo);

        return currentCheckpointInfo;
    }

    /**
     * Clear all load queue data
     */
    public synchronized void clearAll() {

        queueNamesToDbIds.clear();
        threadsPerQueue.clear();
        checkpointsPerThread.clear();
    }

    private void registerCheckpointWithThread( String threadName,
                                               CheckpointInfo checkpointInfo ) throws CheckpointAlreadyStartedException {

        Set<CheckpointInfo> set = checkpointsPerThread.get(threadName);
        if (set == null) {
            set = new HashSet<CheckpointInfo>();
            checkpointsPerThread.put(threadName, set);
        }
        boolean newlyAdded = set.add(checkpointInfo);
        if (!newlyAdded) {
            throw new CheckpointAlreadyStartedException(checkpointInfo.getName(), threadName);
        }
    }

    private void clearThreadCheckpoint( String threadName, CheckpointInfo checkpointInfoForRemove ) {

        Set<CheckpointInfo> set = checkpointsPerThread.get(threadName);
        set.remove(checkpointInfoForRemove);
    }

    private void clearThreadAllCheckpoints( String threadName ) {

        Set<CheckpointInfo> set = checkpointsPerThread.get(threadName);
        if (set != null) {
            set.clear();
        } else { // initialize - put empty set
            set = new HashSet<CheckpointInfo>();
            checkpointsPerThread.put(threadName, set);
        }
    }

}
