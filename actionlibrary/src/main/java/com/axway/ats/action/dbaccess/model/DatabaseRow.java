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
package com.axway.ats.action.dbaccess.model;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.common.PublicAtsApi;

/**
 * A simple representation of a DB row.
 */
@PublicAtsApi
public class DatabaseRow {

    private List<DatabaseCell> cells;

    public DatabaseRow() {

        cells = new ArrayList<DatabaseCell>();
    }

    public void addCell(
                         DatabaseCell cell ) {

        cells.add( cell );
    }

    /**
     * @param cellName the column(cell) name
     * @return the value of a database cell
     */
    @PublicAtsApi
    public String getCellValue(
                                String cellName ) {

        DatabaseCell cell = getCell( cellName );
        if( cell != null ) {
            return cell.getValue();
        } else {
            return null;
        }
    }

    /**
     * @param cellName the column(cell) name
     * @return a database cell from this database row
     */
    @PublicAtsApi
    public DatabaseCell getCell(
                                 String cellName ) {

        for( DatabaseCell cell : cells ) {
            if( cell.getName().equalsIgnoreCase( cellName ) ) {
                return cell;
            }
        }

        return null;
    }

    /**
     * @return all cells in this database row
     */
    @PublicAtsApi
    public DatabaseCell[] getAllCells() {

        return cells.toArray( new DatabaseCell[cells.size()] );
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append( "{" );
        for( DatabaseCell cell : cells ) {
            sb.append( cell.toString() );
            sb.append( ";" );
        }
        sb.append( "}" );
        return sb.toString();
    }
}
