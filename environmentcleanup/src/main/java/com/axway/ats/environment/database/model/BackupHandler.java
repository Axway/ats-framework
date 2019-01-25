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
 * This interface represents database backup handler.
 * A backup handler should be able to backup given tables,
 * excluding any number of columns which are declared to have
 * default values.
 */
public interface BackupHandler {

    /**
     * Add a table to backup
     * 
     * @param dbTable   an instance of DbTable describing the database table
     * @throws DatabaseEnvironmentCleanupException
     */
    public void addTable(
                          DbTable dbTable ) throws DatabaseEnvironmentCleanupException;

    /**
     * Choose whether to include "DELETE FROM *" SQL statements in 
     * the backup - the default value should be true. If this option is
     * turned on, tables will be deleted prior to inserting data in them
     * 
     * @param includeDeleteStatements   enable or disable
     * @throws DatabaseEnvironmentCleanupException
     */
    public void setIncludeDeleteStatements(
                                            boolean includeDeleteStatements )
                                                                              throws DatabaseEnvironmentCleanupException;

    /**
     * Enable or disable the foreign key check prior to restoring the
     * database - default value should be true
     * 
     * @param foreignKeyCheck   enable or disable
     * @throws DatabaseEnvironmentCleanupException
     */
    public void setForeignKeyCheck(
                                    boolean foreignKeyCheck ) throws DatabaseEnvironmentCleanupException;

    /**
     * Choose whether to lock the tables during restore - default
     * value should be true, as other processes might modify the tables
     * during restore, which may lead to inconsistency
     * 
     * @param lockTables    enable or disable
     * @throws DatabaseEnvironmentCleanupException
     */
    public void setLockTables(
                               boolean lockTables ) throws DatabaseEnvironmentCleanupException;

    /**
     * Choose whether to recreate the tables during restore - default
     * value is false
     * 
     * @param dropTable    enable or disable
     * @see com.axway.ats.environment.database.model.BackupHandler#setDropTables(boolean)
     */
    public void setDropTables( boolean dropTable );

    /**
     * Choose whether to exclude the tables' content during backup and only backup the tables' metadata - defaul
     * value is false, which means that both the tables' metadata and content will be backed up
     * 
     * @param skipTablesContent - true to skip, false otherwise
     * @see com.axway.ats.environment.database.model.BackupHandler#setSkipTablesContent(boolean)
     */
    public void setSkipTablesContent( boolean skipTablesContent );

    /**
     * Create the database backup for the selected tables
     * 
     * @param backupFileName                        the name of the backup file
     * @throws DatabaseEnvironmentCleanupException  if the backup file cannot be created
     */
    public void createBackup( String backupFileName ) throws DatabaseEnvironmentCleanupException;

    /**
     * Release the database connection
     */
    public void disconnect();

}
