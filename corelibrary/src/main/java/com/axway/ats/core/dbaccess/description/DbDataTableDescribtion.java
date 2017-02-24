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
package com.axway.ats.core.dbaccess.description;

import java.util.ArrayList;

public class DbDataTableDescribtion {
    public String                             name;

    public ArrayList<DbDataColumnDescribtion> columns;

    public DbDataTableDescribtion() {

        name = "";
        columns = new ArrayList<DbDataColumnDescribtion>();
    }

    public DbDataTableDescribtion( String name ) {

        this();
        this.name = name;

    }

    public boolean equals(
                           DbDataTableDescribtion table ) {

        boolean result = true;
        if( name.equals( table.getName() ) && ( columns.size() == table.columns.size() ) ) {
            for( DbDataColumnDescribtion col : columns ) {
                if( !col.equals( table.columns.get( columns.indexOf( col ) ) ) ) {
                    result = false;
                }
            }
        } else {
            result = false;
        }
        return result;
    }

    public ArrayList<DbDataColumnDescribtion[]> getDifferences(
                                                                DbDataTableDescribtion table ) {

        ArrayList<DbDataColumnDescribtion[]> differenColumns = new ArrayList<DbDataColumnDescribtion[]>();

        for( DbDataColumnDescribtion col : columns ) {
            if( !col.equals( table.columns.get( columns.indexOf( col ) ) ) ) {
                differenColumns.add( new DbDataColumnDescribtion[]{ col,
                        table.columns.get( columns.indexOf( col ) ) } );
            }
        }

        return differenColumns;
    }

    public ArrayList<DbDataColumnDescribtion> getColumns() {

        return columns;
    }

    public void setColumns(
                            ArrayList<DbDataColumnDescribtion> columns ) {

        this.columns = columns;
    }

    public String getName() {

        return name;
    }

    public void setName(
                         String name ) {

        this.name = name;
    }
}
