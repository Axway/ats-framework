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

import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.NoCompatibleMethodFoundException;
import com.axway.ats.agent.core.exceptions.NoSuchActionException;
import com.axway.ats.agent.core.exceptions.NoSuchComponentException;
import com.axway.ats.agent.core.threading.exceptions.ActionTaskLoaderException;
import com.axway.ats.agent.core.threading.exceptions.ThreadingPatternNotSupportedException;

public interface QueueLoader {

    /**
     * Schedules the action queue, i.e all threads are created
     * 
     * @throws ActionExecutionException 
     * @throws ActionTaskLoaderException 
     * @throws NoCompatibleMethodFoundException 
     * @throws NoSuchActionException 
     * @throws NoSuchComponentException 
     * @throws ThreadingPatternNotSupportedException 
     */
    public void scheduleThreads( String caller,
                                 boolean isUseSynchronizedIterations ) throws ActionExecutionException,
                                                                       ActionTaskLoaderException,
                                                                       NoSuchComponentException,
                                                                       NoSuchActionException,
                                                                       NoCompatibleMethodFoundException,
                                                                       ThreadingPatternNotSupportedException;

    /**
     * Starts the action queue, i.e. all threads are started
     * 
     * @throws ActionExecutionException
     * @throws ActionTaskLoaderException
     */
    public void start() throws ActionExecutionException, ActionTaskLoaderException;

    /**
     * Resumes a paused action queue, i.e. all threads
     * run their next iteration
     * 
     * @throws ActionExecutionException
     * @throws ActionTaskLoaderException
     */
    public void resume() throws ActionExecutionException, ActionTaskLoaderException;

    /**
     * Cancels the action queue by canceling all its threads
     */
    public void cancel();

    /**
     * Waits until the action queue is finished, i.e all threads 
     * have completed all their iterations
     */
    public void waitUntilFinished();

    /**
     * Waits until the action queue is paused, i.e. all threads 
     * have completed the current iteration
     * 
     * @return If the queue was paused and if so - the client knows it has to resume 
     * the queue for another iteration.
     * <br>In case the queue has finished - it will not try to resume it
     */
    public boolean waitUntilPaused();

    /**
     * Returns the current queue state
     * 
     * @return
     */
    public ActionTaskLoaderState getState();
}
