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
package com.axway.ats.rbv.db;

import com.axway.ats.core.dbaccess.DbColumn;

/**
 * This class represents a database meta data key - this key may
 * include table name, column name and/or column index. Instances of this
 * class are used for searching db meta data
 */
public class DbMetaDataKey {

    private String tableName;
    private String columnName;
    private int    index;

    public DbMetaDataKey( DbColumn dbColumn ) {

        this.tableName = dbColumn.getTableName();
        this.columnName = dbColumn.getColumnName();
        this.index = dbColumn.getIndex();
    }

    public DbMetaDataKey( String columnName ) {

        this.tableName = "";
        this.columnName = columnName;
        this.index = 0;
    }

    /**
     * Construct a DbMetaDataKey instance
     * 
     * @param tableName     the name of the DB table
     * @param columnName    the name of the column in that table
     */
    public DbMetaDataKey( String tableName,
                          String columnName ) {

        this(tableName, columnName, 0);
    }

    /**
     * Construct a DbMetaDataKey instance
     * 
     * @param columnName    the name of the column in that table
     * @param index         the index of the column
     */
    public DbMetaDataKey( String columnName,
                          int index ) {

        this("", columnName, index);
    }

    /**
     * Construct a DbMetaDataKey instance
     * 
     * @param tableName     the name of the DB table
     * @param columnName    the name of the column in that table
     * @param index         the index of the column in that table 
     *                      if there are more than one columns with the same name
     */
    public DbMetaDataKey( String tableName,
                          String columnName,
                          int index ) {

        this.tableName = tableName;
        this.columnName = columnName;
        this.index = index;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        StringBuilder hash = new StringBuilder();

        if (tableName != null && tableName.length() > 0) {
            hash.append(tableName);
            hash.append(".");
        }

        hash.append(columnName);

        if (index > 0) {
            hash.append(".");
            hash.append(index);
        }

        return hash.toString();
    }

    public String getTableName() {

        return tableName;
    }

    public String getColumnName() {

        return columnName;
    }

    public int getIndex() {

        return index;
    }
}
