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
package com.axway.ats.agent.core.model;

import com.axway.ats.agent.core.exceptions.InternalComponentException;

/**
 * This interface should be implemented by the component
 * under test, it is responsible for restoring a clean
 * environment after the component did some work. This
 * may include deleting content in certain tables, restoring
 * files on the file system or any other necessary activity
 */
public interface EnvironmentCleanupHandler {

    /**
     * Clean the component environment
     */
    public void clean() throws InternalComponentException;

}
