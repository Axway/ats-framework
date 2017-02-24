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
package com.axway.ats.agent.core.threading.data.config;

/**
 * The level that each parameter data provider works on
 */
public enum ParameterProviderLevel {

    /**
     * Each thread will get any of the ordered values
     * <pre>
     *  For example when we distribute 3 values on 2 threads, we will get:
     *  +-------------------------------+
     *  |    Thread 1   |   Thread 2    |
     *  +-------------------------------+
     *  |       1       |       1       |
     *  |       2       |       2       |
     *  |       3       |       3       |
     *  |       1       |       1       |
     *  +-------------------------------+
     *
     *  When we distribute 2 values on 3 threads, we will get:
     *  +-----------------------------------------------+
     *  |    Thread 1   |   Thread 2    |   Thread 3    |
     *  +-----------------------------------------------+
     *  |       1       |       1       |       1       |
     *  |       2       |       2       |       2       |
     *  |       1       |       1       |       1       |
     *  |       2       |       2       |       2       |
     *  +-----------------------------------------------+
     * </pre>
     */
    PER_THREAD,

    /**
     * A particular value is assigned to each thread
     * <pre>
     *  For example when we distribute 3 values on 2 threads, we will get:
     *  +-------------------------------+
     *  |    Thread 1   |   Thread 2    |
     *  +-------------------------------+
     *  |       1       |       2       |
     *  |       1       |       2       |
     *  |       1       |       2       |
     *  |       1       |       2       |
     *  +-------------------------------+
     *
     *  When we distribute 2 values on 3 threads, we will get:
     *  +-----------------------------------------------+
     *  |    Thread 1   |   Thread 2    |   Thread 3    |
     *  +-----------------------------------------------+
     *  |       1       |       2       |       1       |
     *  |       1       |       2       |       1       |
     *  |       1       |       2       |       1       |
     *  |       1       |       2       |       1       |
     *  +-----------------------------------------------+
     * </pre>
     */
    PER_THREAD_STATIC,

    /**
     * The data is distributed without considering which is the
     * calling thread (new data on every request). The invocation order
     * in time is considered from left to right, top to bottom.
     * <pre>
     *  For example when we distribute 3 values on 2 threads, we will get:
     *  +-------------------------------+
     *  |    Thread 1   |   Thread 2    |
     *  +-------------------------------+
     *  |       1       |       2       |
     *  |       3       |       1       |
     *  |       2       |       3       |
     *  |       1       |       2       |
     *  +-------------------------------+
     *
     *  When we distribute 2 values on 3 threads, we will get:
     *  +-----------------------------------------------+
     *  |    Thread 1   |   Thread 2    |   Thread 3    |
     *  +-----------------------------------------------+
     *  |       1       |       2       |       1       |
     *  |       2       |       1       |       2       |
     *  |       1       |       2       |       1       |
     *  |       2       |       1       |       2       |
     *  +-----------------------------------------------+
     *  </pre>
     */
    PER_INVOCATION;
}
