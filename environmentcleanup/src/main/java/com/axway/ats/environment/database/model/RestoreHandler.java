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
package com.axway.ats.environment.database.model;

import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;

/**
 * This interface represents database restore handler.
 * A restore handler should be able to restore a database
 * by executing a given script file
 */
public interface RestoreHandler {

    /**
     * Restore the database from a backup script
     * 
     * @param backupFileName                        the name of the backup file
     * @throws DatabaseEnvironmentCleanupException  if the backup file cannot be restored
     */
    public void restore(
                         String backupFileName ) throws DatabaseEnvironmentCleanupException;

    /**
     * Release the database connection
     */
    public void disconnect();
}
