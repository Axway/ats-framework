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
package com.axway.ats.action.dbaccess;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.axway.ats.action.dbaccess.DatabaseOperations;
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

public class MockGeneralDatabaseOperations extends DatabaseOperations {

    public MockGeneralDatabaseOperations() {

        super( new MockDbProvider() );
    }

}

class MockDbProvider implements DbProvider {

    @Override
    public int executeUpdate(
                              String query ) throws DbException {

        return 0;
    }

    @Override
    public DbConnection getDbConnection() {

        return new DbConnMySQL( "localhost", "db", "user", "pass" );
    }

    @Override
    public int insertRow(
                          String tableName,
                          Map<String, String> columns ) throws DbException {

        return 0;
    }

    @Override
    public boolean isReservedWord(
                                   String value ) {

        return false;
    }

    @Override
    public int rowCount(
                         String tableName ) throws DbException, NumberValidationException {

        return 0;
    }

    @Override
    public int rowCount(
                         String tableName,
                         String columnNameWhere,
                         String whereValue ) throws DbException, NumberValidationException {

        return 0;
    }

    @Override
    public int rowCount(
                         String tableName,
                         String whereCondition ) throws DbException, NumberValidationException {

        return 0;
    }

    @Override
    public DbRecordValuesList[] select(
                                        String query ) throws DbException {

        return this.select( new DbQuery( query, new ArrayList<Object>() ) );
    }

    @Override
    public DbRecordValuesList[] select(
                                        DbQuery dbQuery ) throws DbException {

        DbRecordValuesList[] resultSet = null;
        DbRecordValuesList result = new DbRecordValuesList();
        result.add( new DbRecordValue( "asd", "key0", "value00" ) );

        //testing db exception
        if( dbQuery.getQuery().contains( "wrongTableName" ) ) {
            throw new DbException( "wrong table name" );

            //test more than one result values
        } else if( dbQuery.getQuery().contains( "moreValues" ) ) {
            resultSet = new DbRecordValuesList[2];
            resultSet[0] = result;
            DbRecordValuesList sresult = new DbRecordValuesList();
            sresult.add( new DbRecordValue( "dfg", "key0", "value11" ) );
            resultSet[1] = result;

            //testing with DatabaseRow and DatabaseCell classes
        } else if( dbQuery.getQuery().contains( "tableWithManyRows" ) ) {
            if( dbQuery.getQuery().contains( "*" ) ) {
                resultSet = new DbRecordValuesList[2];

                DbRecordValuesList firstDbRow = new DbRecordValuesList();
                firstDbRow.add( new DbRecordValue( "tableWithManyRows", "firstColumnName", "value01" ) );
                firstDbRow.add( new DbRecordValue( "tableWithManyRows", "secondColumnName", "value02" ) );
                resultSet[0] = firstDbRow;

                DbRecordValuesList secondDbRow = new DbRecordValuesList();
                secondDbRow.add( new DbRecordValue( "tableWithManyRows", "firstColumnName", "value11" ) );
                secondDbRow.add( new DbRecordValue( "tableWithManyRows", "secondColumnName", "value12" ) );
                resultSet[1] = secondDbRow;
            } else {
                resultSet = new DbRecordValuesList[2];

                DbRecordValuesList firstDbRow = new DbRecordValuesList();
                firstDbRow.add( new DbRecordValue( "tableWithManyRows", "selectColumnName", "value1" ) );
                resultSet[0] = firstDbRow;

                DbRecordValuesList secondDbRow = new DbRecordValuesList();
                secondDbRow.add( new DbRecordValue( "tableWithManyRows", "selectColumnName", "value2" ) );
                resultSet[1] = secondDbRow;
            }

            //testing no value result
        } else if( !dbQuery.getQuery().contains( "nullValue" ) ) {
            resultSet = new DbRecordValuesList[1];
            resultSet[0] = result;
        }

        return resultSet;
    }

    @Override
    public DbRecordValuesList[] select(
                                        DbQuery dbQuery,
                                        DbReturnModes dbReturnMode ) throws DbException {

        return null;
    }

    @Override
    public InputStream selectValue(
                                    String tableName,
                                    String keyColumn,
                                    String keyValue,
                                    String queryColumn ) throws DbException {

        return null;
    }

    @Override
    public InputStream selectValue(
                                    String tableName,
                                    String keyColumn,
                                    String keyValue,
                                    String queryColumn,
                                    int recordNumber ) throws DbException {

        return null;
    }

    @Override
    public InputStream selectValue(
                                    String tableName,
                                    String[] keyColumns,
                                    String[] keyValues,
                                    String queryColumn ) throws DbException, ValidationException {

        return null;
    }

    @Override
    public InputStream selectValue(
                                    String tableName,
                                    String[] keyColumns,
                                    String[] keyValues,
                                    String queryColumn,
                                    int recordNumber ) throws DbException, ValidationException {

        return null;
    }

    @Override
    public void disconnect() {

    }

	@Override
	public List<TableDescription> getTableDescriptions() {
		return null;
	}
}
