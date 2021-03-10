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

import java.lang.reflect.Array;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class representing a value in a recordset
 */
public class DbRecordValue {

    private static final Logger log                         = LogManager.getLogger(DbRecordValue.class);

    private DbColumn            dbColumn;
    private Object              value;

    // flag, used to determine whether ojdbc (Oracle JDBC) driver is in JAVA class path
    private static boolean      oracleJdbcDriverInClassPath = false;

    static {
        try {
            Class<?> clazz = Class.forName(oracle.jdbc.OracleDriver.class.getName());
            if (clazz != null) {
                oracleJdbcDriverInClassPath = true;
            } else {
                oracleJdbcDriverInClassPath = false;
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            oracleJdbcDriverInClassPath = false;
        }
    }

    /**
     * @param tableName     name of the DB table
     * @param columnName    name of the column in that table
     * @param value         the value returned by the query
     */
    public DbRecordValue( String tableName,
                          String columnName,
                          Object value ) {

        this.dbColumn = new DbColumn(tableName, columnName);
        this.value = value;
    }

    /**
     * @param dbColumn      the db column that contains the value
     * @param value         the value returned by the query
     */
    public DbRecordValue( DbColumn dbColumn,
                          Object value ) {

        this.dbColumn = dbColumn;
        this.value = value;
    }

    public Object getValue() {

        return value;
    }

    public void setValue(
                          String value ) {

        this.value = value;
    }

    public String getValueAsString() {

        // handle null value
        if (value == null) {
            return null;
        }

        // handle array
        if (value.getClass().isArray()) {
            StringBuilder valueAsString = new StringBuilder();

            valueAsString.append("[ ");
            for (int i = 0; i < Array.getLength(value); i++) {
                valueAsString.append(Array.get(value, i));
                valueAsString.append(", ");
            }

            if (valueAsString.toString().endsWith(", ")) {
                valueAsString.delete(valueAsString.length() - 2, valueAsString.length());
            }

            valueAsString.append(" ]");

            return valueAsString.toString();
        }

        if (oracleJdbcDriverInClassPath) {
            // handle oracle time stamp
            if (value instanceof oracle.sql.TIMESTAMP) {
                try {
                    return ((oracle.sql.TIMESTAMP) value).toJdbc().toString();
                } catch (SQLException e) {
                    // If get here, it is likely to break some functionality like Database Snapshots.
                    // Then we will have to revise this logic.
                    log.warn("There was error while parsing value as oracle.sql.TIMESTAMP."
                             + "We will instead return the value as " + value.toString() + " .", e);
                    return value.toString();
                }
            }
        }

        return value.toString();
    }

    public DbColumn getDbColumn() {

        return dbColumn;
    }
}
