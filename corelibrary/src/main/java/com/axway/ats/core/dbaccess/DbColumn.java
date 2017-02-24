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
package com.axway.ats.core.dbaccess;

/**
 * A class which represents a column in a result set 
 */
public class DbColumn {

    private String tableName;
    private String columnName;
    private String columnType;
    private int    index;

    /**
     * Construct a DbColumn instance
     * 
     * @param tableName     the name of the DB table
     * @param columnName    the name of the column in that table
     */
    public DbColumn( String tableName,
                     String columnName ) {

        this( tableName, columnName, 0 );
    }

    /**
     * Construct a DbColumn instance
     * 
     * @param columnName    the name of the column in that table
     * @param index         the index of the column
     */
    public DbColumn( String columnName,
                     int index ) {

        this( "", columnName, index );
    }

    /**
     * Construct a DbColumn instance
     * 
     * @param tableName     the name of the DB table
     * @param columnName    the name of the column in that table
     * @param index         the index of the column in that table 
     *                      if there are more than one columns with the same name
     */
    public DbColumn( String tableName,
                     String columnName,
                     int index ) {

        this.tableName = tableName;
        this.columnName = columnName;
        this.index = index;
    }

    /**
     * Get the name of the table
     * 
     * @return
     */
    public String getTableName() {

        return tableName;
    }

    /**
     * Set the name of the table
     * 
     * @param tableName
     */
    public void setTableName(
                              String tableName ) {

        this.tableName = tableName;
    }

    /**
     * Get the name of the column
     * 
     * @return
     */
    public String getColumnName() {

        return columnName;
    }

    /**
     * Set the name of the column
     * 
     * @param columnName
     */
    public void setColumnName(
                               String columnName ) {

        this.columnName = columnName;
    }

    /**
     * Get the index - should be different than 0 only if
     * the same column is included more than once in the query result
     * 
     * @return
     */
    public int getIndex() {

        return index;
    }

    /**
     * Set the index - should be different than 0 only if
     * the same column is included more than once in the query result
     * 
     * @param index
     */
    public void setIndex(
                          int index ) {

        this.index = index;
    }

    /**
     * Get the type of the column
     * 
     * @return
     */
    public String getColumnType() {

        return columnType;
    }

    /**
     * Set the type of the column
     * 
     * @param columnType
     */
    public void setColumnType(
                               String columnType ) {

        this.columnType = columnType;
    }
}
