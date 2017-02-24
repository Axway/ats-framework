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
package com.axway.ats.common.dbaccess.snapshot.equality;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.axway.ats.common.PublicAtsApi;

/**
 * This structure says how equal are the snapshots
 */
@PublicAtsApi
public class EqualityState {

    private String                                 firstSnapshotName;
    private String                                 secondSnapshotName;

    // <snapshot, table> we are using TreeMap in order to use save order regardless of the JDK
    private Map<String, List<String>>              tablePresentInOneSnapshotOnly  = new TreeMap<>();

    // list of primary key columns which are different for same table in both snapshots
    private List<String>                           differentPrimaryKeys           = new ArrayList<>();

    // list tables with different size
    private List<String>                           differentNumberOfRows          = new ArrayList<>();

    // <snapshot <table, row values>> we are using TreeMap in order to use save order regardless of the JDK
    private Map<String, Map<String, List<String>>> rowPresentInOneSnapshotOnly    = new TreeMap<>();

    // <snapshot <table, column>> we are using TreeMap in order to use save order regardless of the JDK
    private Map<String, Map<String, List<String>>> columnPresentInOneSnapshotOnly = new TreeMap<>();

    // <snapshot <table, index>> we are using TreeMap in order to use save order regardless of the JDK
    private Map<String, Map<String, List<String>>> indexPresentInOneSnapshotOnly  = new TreeMap<>();

    public EqualityState( String firstSnapshotName, String secondSnapshotName ) {

        this.firstSnapshotName = firstSnapshotName;
        this.secondSnapshotName = secondSnapshotName;
    }

    @PublicAtsApi
    public String getFirstSnapshotName() {

        return firstSnapshotName;
    }

    @PublicAtsApi
    public String getSecondSnapshotName() {

        return secondSnapshotName;
    }

    @PublicAtsApi
    public boolean hasDifferences() {

        return tablePresentInOneSnapshotOnly.size() > 0 || differentPrimaryKeys.size() > 0
               || differentNumberOfRows.size() > 0 || rowPresentInOneSnapshotOnly.size() > 0
               || columnPresentInOneSnapshotOnly.size() > 0 || indexPresentInOneSnapshotOnly.size() > 0;
    }

    @PublicAtsApi
    public List<String> getTablesPresentInOneSnapshotOnly( String snapshot ) {

        return tablePresentInOneSnapshotOnly.get( snapshot );
    }

    public void addTablePresentInOneSnapshotOnly( String snapshotName, String table ) {

        List<String> tablesPerSnapshot = tablePresentInOneSnapshotOnly.get( snapshotName );
        if( tablesPerSnapshot == null ) {
            tablesPerSnapshot = new ArrayList<String>();
            tablePresentInOneSnapshotOnly.put( snapshotName, tablesPerSnapshot );
        }

        tablesPerSnapshot.add( table );
    }

    @PublicAtsApi
    public List<String> getDifferentPrimaryKeys() {

        return differentPrimaryKeys;
    }

    public void addDifferentPrimaryKeys( String firstSnapshotName, String secondSnapshotName,
                                         String firstPrimaryKey, String secondPrimaryKey, String table ) {

        differentPrimaryKeys.add( "table '" + table + "', primary key column in [" + firstSnapshotName
                                  + "] is '" + firstPrimaryKey + "', while in [" + secondSnapshotName
                                  + "] is '" + secondPrimaryKey + "'" );
    }

    @PublicAtsApi
    public List<String> getDifferentNumberOfRows() {

        return differentNumberOfRows;
    }

    public void addDifferentNumberOfRows( String firstSnapshotName, String secondSnapshotName,
                                          int firstNumberOfRows, int secondNumberOfRows, String table ) {

        differentNumberOfRows.add( "table '" + table + "', " + firstNumberOfRows + " rows in ["
                                   + firstSnapshotName + "] and " + secondNumberOfRows + " in ["
                                   + secondSnapshotName + "]" );

    }

    @PublicAtsApi
    public Map<String, List<String>> getColumnsPresentInOneSnapshotOnly( String snapshot ) {

        return columnPresentInOneSnapshotOnly.get( snapshot );
    }

    public void addColumnPresentInOneSnapshotOnly( String snapshotName, String table, String column ) {

        Map<String, List<String>> tablesPerSnapshot = columnPresentInOneSnapshotOnly.get( snapshotName );
        if( tablesPerSnapshot == null ) {
            tablesPerSnapshot = new TreeMap<>();
            columnPresentInOneSnapshotOnly.put( snapshotName, tablesPerSnapshot );
        }

        List<String> columnsPerTable = tablesPerSnapshot.get( table );
        if( columnsPerTable == null ) {
            columnsPerTable = new ArrayList<>();
            tablesPerSnapshot.put( table, columnsPerTable );
        }

        columnsPerTable.add( column );
    }

    @PublicAtsApi
    public Map<String, List<String>> getIndexesPresentInOneSnapshotOnly( String snapshot ) {

        return indexPresentInOneSnapshotOnly.get( snapshot );
    }

    public void addIndexPresentInOneSnapshotOnly( String snapshotName, String table, String indexName,
                                                  String index ) {

        Map<String, List<String>> tablesPerSnapshot = indexPresentInOneSnapshotOnly.get( snapshotName );
        if( tablesPerSnapshot == null ) {
            tablesPerSnapshot = new TreeMap<>();
            indexPresentInOneSnapshotOnly.put( snapshotName, tablesPerSnapshot );
        }

        List<String> indexesPerTable = tablesPerSnapshot.get( table );
        if( indexesPerTable == null ) {
            indexesPerTable = new ArrayList<>();
            tablesPerSnapshot.put( table, indexesPerTable );
        }

        indexesPerTable.add( indexName + index );
    }

    @PublicAtsApi
    public Map<String, List<String>> getRowsPresentInOneSnapshotOnly( String snapshot ) {

        return rowPresentInOneSnapshotOnly.get( snapshot );
    }

    public void addRowPresentInOneSnapshotOnly( String snapshotName, String table, String rowValues ) {

        Map<String, List<String>> tablesPerSnapshot = rowPresentInOneSnapshotOnly.get( snapshotName );
        if( tablesPerSnapshot == null ) {
            tablesPerSnapshot = new TreeMap<>();
            rowPresentInOneSnapshotOnly.put( snapshotName, tablesPerSnapshot );
        }

        List<String> rowsPerTable = tablesPerSnapshot.get( table );
        if( rowsPerTable == null ) {
            rowsPerTable = new ArrayList<>();
            tablesPerSnapshot.put( table, rowsPerTable );
        }

        rowsPerTable.add( rowValues );
    }
}
