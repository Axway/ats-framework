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

import java.io.InputStream;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.common.dbaccess.snapshot.TableDescription;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.DbReturnModes;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.mysql.DbConnMySQL;
import com.axway.ats.core.validation.exceptions.NumberValidationException;
import com.axway.ats.core.validation.exceptions.ValidationException;

public class MockDbProvider implements DbProvider {

    private int     seed             = 0;

    private boolean useChangedValues = false;

    public void connect() {

        //do nothing
    }

    public void disconnect() {

        //do nothing
    }

    public DbConnection getDbConnection() {

        return new DbConnMySQL("localhost", "non_existing_db", "user", "pass");
    }

    public DbRecordValuesList[] select(
                                        String query ) {

        return this.select(new DbQuery(query, new ArrayList<Object>()));
    }

    public DbRecordValuesList[] select(
                                        DbQuery query ) {

        final int resultCount = 2;

        DbRecordValuesList[] resultSet = new DbRecordValuesList[resultCount];

        for (int i = 0; i < resultCount; i++) {

            DbRecordValuesList result = new DbRecordValuesList();

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Timestamp stamp = new Timestamp(calendar.getTime().getTime());

            result.add(new DbRecordValue("asd", "key0", "value" + i + seed));
            result.add(new DbRecordValue("", "key1", "value" + i + seed));
            result.add(new DbRecordValue("", "key2", "value" + i + seed));
            result.add(new DbRecordValue("numeric", "key", i));
            result.add(new DbRecordValue("binary", "key", new byte[]{ 1, 2, (byte) i }));
            result.add(new DbRecordValue("boolean", "keyString", "0"));
            result.add(new DbRecordValue("boolean", "keyNumber", 1L));
            result.add(new DbRecordValue("Date", "today", stamp));
            if (!useChangedValues) {
                result.add(new DbRecordValue("", "test_key", "test_value"));
            } else {
                result.add(new DbRecordValue("", "test_key", "test_value_changed"));
            }

            resultSet[i] = result;
        }
        if (useChangedValues) {
            useChangedValues = false;
        }
        return resultSet;
    }

    public DbRecordValuesList[] select(
                                        DbQuery dbQuery,
                                        DbReturnModes dbReturnMode ) throws DbException {

        return null;
    }

    public PreparedStatement createPreparedStatement(
                                                      String query ) throws DbException {

        return null;
    }

    public boolean doesTableExist(
                                   String tableName ) throws DbException {

        return false;
    }

    public int executeUpdate(
                              String query ) throws DbException {

        return 0;
    }

    public int insertRow(
                          String tableName,
                          HashMap<String, String> columns ) throws DbException {

        return 0;
    }

    public boolean isReservedWord(
                                   String value ) {

        return false;
    }

    public int rowCount(
                         String tableName ) throws DbException, NumberValidationException {

        return 0;
    }

    public int rowCount(
                         String tableName,
                         String columnNameWhere,
                         String whereValue ) throws DbException, NumberValidationException {

        return 0;
    }

    public int rowCount(
                         String tableName,
                         String whereCondition ) throws DbException, NumberValidationException {

        return 0;
    }

    public InputStream selectValue(
                                    String tableName,
                                    String keyColumn,
                                    String keyValue,
                                    String queryColumn ) throws DbException {

        return null;
    }

    public InputStream selectValue(
                                    String tableName,
                                    String keyColumn,
                                    String keyValue,
                                    String queryColumn,
                                    int recordNumber ) throws DbException {

        return null;
    }

    public InputStream selectValue(
                                    String tableName,
                                    String[] keyColumns,
                                    String[] keyValues,
                                    String queryColumn ) throws DbException, ValidationException {

        return null;
    }

    public InputStream selectValue(
                                    String tableName,
                                    String[] keyColumns,
                                    String[] keyValues,
                                    String queryColumn,
                                    int recordNumber ) throws DbException, ValidationException {

        return null;
    }

    public int updateValue(
                            String tableName,
                            String keyColumn,
                            String keyValue,
                            String updateColumn,
                            String updateValue ) throws DbException {

        return 0;
    }

    public int insertRow(
                          String tableName,
                          Map<String, String> columns ) throws DbException {

        return 0;
    }

    public void incrementSeed() {

        seed++;
    }

    public void useChangedValues() {

        this.useChangedValues = true;
    }

    @Override
    public List<TableDescription> getTableDescriptions( List<String> tablesToSkip ) {

        return null;
    }

    @Override
    public DatabaseMetaData getDatabaseMetaData() throws DbException {

        return null;
    }
}
