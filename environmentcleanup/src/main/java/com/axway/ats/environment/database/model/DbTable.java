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

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a single database table - it holds the name
 * of the table and the columns which to exclude from the backup
 */
public class DbTable {

    private String       tableName;
    private List<String> columnsToExclude;
    private boolean      lockTable             = true;
    private boolean      identityColumnPresent = false;

    private String       autoIncrementResetValue;

    /**
     * Constructor - no columns will be excluded from the backup
     *
     * @param tableName name of the table
     */
    public DbTable( String tableName ) {

        this(tableName, new ArrayList<String>());
    }

    /**
     * Constructor
     *
     * @param tableName name of the table
     * @param columnsToExclude list of columns to exclude from the backup
     */
    public DbTable( String tableName,
                    List<String> columnsToExclude ) {

        this(tableName, columnsToExclude, true);
    }

    /**
     * Constructor - no columns will be excluded from the backup
     *
     * @param tableName name of the table
     * @param lockTable parameter if the table must be locked during restore
     */
    public DbTable( String tableName,
                    boolean lockTable ) {

        this(tableName, new ArrayList<String>(), lockTable);
    }

    /**
     * Constructor
     *
     * @param tableName name of the table
     * @param columnsToExclude list of columns to exclude from the backup
     * @param lockTable parameter if the table must be locked during restore
     */
    public DbTable( String tableName,
                    List<String> columnsToExclude,
                    boolean lockTable ) {

        this.tableName = tableName;
        this.columnsToExclude = columnsToExclude;
        this.lockTable = lockTable;
    }

    /**
     * Get the table name
     *
     * @return the table name
     */
    public String getTableName() {

        return tableName;
    }

    /**
     * Get the columns to exclude from the backup
     *
     * @return the columns to exclude from the backup
     */
    public List<String> getColumnsToExclude() {

        return columnsToExclude;
    }

    /**
     *
     * @return if the table will be locked or not during the restore process
     */
    public boolean isLockTable() {

        return lockTable;
    }

    /**
     *
     * @param lockTable is true if the table will be locked during the restore process
     */
    public void setLockTable(
                              boolean lockTable ) {

        this.lockTable = lockTable;
    }

    public String getAutoIncrementResetValue() {

        return autoIncrementResetValue;
    }

    public void setAutoIncrementResetValue(
                                            String autoIncrementResetValue ) {

        this.autoIncrementResetValue = autoIncrementResetValue;
    }

    public boolean isIdentityColumnPresent() {

        return identityColumnPresent;
    }

    public void setIdentityColumnPresent(
                                          boolean identityColumnPresent ) {

        this.identityColumnPresent = identityColumnPresent;
    }

    public DbTable getNewCopy() {

        DbTable newDbTable = new DbTable(this.tableName, this.lockTable);

        List<String> newColumnsToExclude = new ArrayList<String>();
        for (String columnToExclude : this.columnsToExclude) {
            newColumnsToExclude.add(columnToExclude);
        }
        newDbTable.columnsToExclude = newColumnsToExclude;

        newDbTable.identityColumnPresent = this.identityColumnPresent;
        newDbTable.autoIncrementResetValue = this.autoIncrementResetValue;

        return newDbTable;
    }
}
