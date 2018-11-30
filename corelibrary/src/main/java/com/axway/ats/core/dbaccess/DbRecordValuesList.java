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

import java.util.ArrayList;
import java.util.Iterator;

import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.StringUtils;

@SuppressWarnings( "serial")
public class DbRecordValuesList extends ArrayList<DbRecordValue> {

    public Object get( String columnName ) throws DbException {

        DbRecordValue foundValue = null;

        Iterator<DbRecordValue> elementsIter = iterator();
        while (elementsIter.hasNext()) {
            DbRecordValue recordValue = elementsIter.next();
            if (recordValue.getDbColumn().getColumnName().equals(columnName)) {
                if (foundValue == null) {
                    foundValue = recordValue;
                } else {
                    // FIXME: Is this possible?
                    throw new DbException("Ambiguous column name '" + columnName
                                          + "' - the returned record contains more that one column with this name, you need to specify table or index");
                }
            }
        }

        if (foundValue == null) {
            throw new DbException("Unable to extract a column by the name of '" + columnName
                                  + "' - no such column exists in the result set");
        }

        return foundValue.getValue();
    }

    public Object get( String tableName, String columnName ) throws DbException {

        DbRecordValue foundValue = null;

        Iterator<DbRecordValue> elementsIter = iterator();
        while (elementsIter.hasNext()) {
            DbRecordValue recordValue = elementsIter.next();
            if (recordValue.getDbColumn().getColumnName().equals(columnName)
                && recordValue.getDbColumn().getTableName().equals(tableName)) {
                if (foundValue == null) {
                    foundValue = recordValue;
                } else {
                    throw new DbException("Ambigous column name '" + tableName + "." + columnName
                                          + "' - the returned record contains more that one column with this name, you need to specify index");
                }
            }
        }

        return foundValue.getValue();
    }

    public Object get( String columnName, int index ) throws DbException {

        DbRecordValue foundValue = null;

        Iterator<DbRecordValue> elementsIter = iterator();
        while (elementsIter.hasNext()) {
            DbRecordValue recordValue = elementsIter.next();
            if (recordValue.getDbColumn().getColumnName().equals(columnName)
                && recordValue.getDbColumn().getIndex() == index) {
                if (foundValue == null) {
                    foundValue = recordValue;
                } else {
                    throw new DbException("There are two columns with the same name and the same index");
                }
            }
        }

        return foundValue.getValue();
    }

    @Override
    public String toString() {

        StringBuilder sBuilder = new StringBuilder();

        Iterator<DbRecordValue> elementsIter = iterator();
        while (elementsIter.hasNext()) {
            DbRecordValue recordValue = elementsIter.next();
            String recordValueString = recordValue.getValueAsString();

            ColumnDescription columnDescription = new ColumnDescription(recordValue.getDbColumn()
                                                                                   .getColumnName(),
                                                                        recordValue.getDbColumn()
                                                                                   .getColumnType());

            if (recordValueString != null) {
                if (columnDescription.getType() != null && (columnDescription.isTypeBinary())) {
                    // we can get very long data here, so we use MD5 checksum instead
                    recordValueString = "md5:" + StringUtils.md5sum(recordValueString);
                }
            } else {
                recordValueString = "NULL";
            }
            sBuilder.append(recordValue.getDbColumn().getColumnName());
            sBuilder.append("=");
            sBuilder.append(recordValueString);
            sBuilder.append("|");
        }
        
        sBuilder.setLength(sBuilder.length() - 1);

        return sBuilder.toString();
    }
}