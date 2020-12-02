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

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;

/**
 * Class representing a single database table - it holds the name
 * of the table and the columns which to exclude from the backup
 */
@PublicAtsApi
public class DbTable {

    private String       tableName;
    private String       schema;
    private List<String> columnsToExclude;
    private boolean      lockTable             = true;
    /**
     * Whether to drop table. If true - > then drop, else if false -> no, else (null) user did not specify it.
     * */
    private Boolean      dropTable             = null;
    private boolean      identityColumnPresent = false;

    private String       autoIncrementResetValue;

    /**
     * Constructor - no columns will be excluded from the backup
     *
     * @param tableName name of the table
     */
    public DbTable( String tableName ) {

        this(tableName, "", new ArrayList<String>());
    }

    /**
     * Constructor - no columns will be excluded from the backup
     *
     * @param tableName name of the table
     * @param schema schema of the table. Note that this is only applicable for <strong>MSSQL</strong> tables
     */
    public DbTable( String tableName, String schema ) {

        this(tableName, schema, new ArrayList<String>());
    }

    /**
     * Constructor
     *
     * @param tableName name of the table
     * @param schema schema of the table
     * @param columnsToExclude list of columns to exclude from the backup
     */
    public DbTable( String tableName,
                    String schema,
                    List<String> columnsToExclude ) {

        this(tableName, schema, columnsToExclude, true, null);
    }

    /**
     * Constructor - no columns will be excluded from the backup
     *
     * @param tableName name of the table
     * @param schema name of the schema
     * @param lockTable parameter if the table must be locked during restore
     * @param dropTable parameter if the table must be recreated during restore
     */
    public DbTable( String tableName,
                    String schema,
                    boolean lockTable,
                    Boolean dropTable ) {

        this(tableName, schema, new ArrayList<String>(), lockTable, dropTable);
    }

    /**
     * Constructor
     *
     * @param tableName name of the table
     * @param schema schema of the table
     * @param columnsToExclude list of columns to exclude from the backup
     * @param lockTable parameter if the table must be locked during restore
     * @param dropTable parameter if the table must be recreated during restore
     */
    public DbTable( String tableName,
                    String schema,
                    List<String> columnsToExclude,
                    boolean lockTable,
                    Boolean dropTable ) {

        this.tableName = tableName;
        this.schema = schema;
        this.columnsToExclude = columnsToExclude;
        this.lockTable = lockTable;
        this.dropTable = dropTable;
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
     * Get the table schema
     *
     * @return the table schema
     */
    public String getTableSchema() {

        return schema;
    }

    /**
     * <p>Set table schema</p>
     * <p>Note: This is only applicable for <strong>MSSQL</strong> tables 
     * @param tableSchema the table schema
     * */
    public void setTableSchema( String tableSchema ) {

        this.schema = tableSchema;
    }

    /**
     * <p>Get the full (schema_name.table_name) table name</p>
     * <p>Note that this is only applicable for <strong>MSSQL</strong> tables</p>
     * */
    public String getFullTableName() {

        return (!StringUtils.isNullOrEmpty(schema)
                                                   ? schema + "."
                                                   : "")
               + tableName;
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

    /**
    * <p>Get whether to drop table</p>
    * <p>Note: If not defined, it will be null</p>
    * @return if the table will be recreated or not during the restore process
    */
    public Boolean isDropTable() {

        return dropTable;
    }

    /**
    *
    * @param dropTable is true if the table will be recreated during the restore process
    */
    public void setDropTable(
                              boolean dropTable ) {

        this.dropTable = dropTable;
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

        DbTable newDbTable = new DbTable(this.tableName, this.schema, this.lockTable, this.dropTable);

        List<String> newColumnsToExclude = new ArrayList<String>();
        for (String columnToExclude : this.columnsToExclude) {
            newColumnsToExclude.add(columnToExclude);
        }
        newDbTable.columnsToExclude = newColumnsToExclude;

        newDbTable.identityColumnPresent = this.identityColumnPresent;
        newDbTable.autoIncrementResetValue = this.autoIncrementResetValue;

        return newDbTable;
    }

    @Override public String toString() {

        return "DbTable{" +
               "tableName='" + tableName + '\'' +
               ", schema='" + schema + '\'' +
               '}';
    }
}
