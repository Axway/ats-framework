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
package com.axway.ats.environment.database.mysql;

import java.sql.Connection;

import com.axway.ats.core.dbaccess.exceptions.DbException;

/**
 * This {@link Enum} takes care for the different naming of the system columns
 * when using different JDBC driver for the MySQL {@link Connection}. Any drivers
 * prior to the 5.1 drivers would, for instance, return <b>COLUMN_NAME</b> instead of 
 * <b>Field</b> in the result set of a <b>SHOW COLUMNS FROM</b> statement.
 */
public enum MysqlColumnNames {
    /** The name of the column containing the table column names */
    COLUMN_NAME,
    /** The name of the column containing the table column types*/
    COLUMN_TYPE,
    /** The name of the column containing the value, determining if the table column is default*/
    DEFAULT_COLUMN;

    /**
     * Returns the name of the column
     * @return the name of the column
     * @throws DbException 
     */
    public String getName(
                           boolean isJDBC4 ) throws DbException {

        switch (this) {
            case COLUMN_NAME:
                if (!isJDBC4) {
                    return "Field";
                }
                return "COLUMN_NAME";
            case COLUMN_TYPE:
                if (!isJDBC4) {
                    return "Type";
                }
                return "COLUMN_TYPE";

            case DEFAULT_COLUMN:
                if (!isJDBC4) {
                    return "Default";
                }
                return "COLUMN_DEFAULT";
            default:
                throw new DbException("No case for type " + this);
        }
    }
}
