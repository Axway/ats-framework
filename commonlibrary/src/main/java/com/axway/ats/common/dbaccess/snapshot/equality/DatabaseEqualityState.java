/*
 * Copyright 2017-2020 Axway Software
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
 * This structure says how equal the snapshots are
 */
@PublicAtsApi
public class DatabaseEqualityState {

    private String                                 firstSnapshotName;
    private String                                 secondSnapshotName;

    // <snapshot, table> we are using TreeMap in order to use save order regardless of the JDK
    private Map<String, List<String>>              tablePresentInOneSnapshotOnly  = new TreeMap<>();

    // list of primary key columns which are different for same table in both snapshots
    // NOTE: currently we do not support more than one primary key per table
    // < snapshot, < table, key > >
    private Map<String, Map<String, String>>       differentPrimaryKeys           = new TreeMap<>();

    // list tables with different size
    // < snapshot, < table, number of rows > >
    private Map<String, Map<String, Integer>>      differentNumberOfRows          = new TreeMap<>();

    // <snapshot <table, row values>> we are using TreeMap in order to use save order regardless of the JDK
    private Map<String, Map<String, List<String>>> rowPresentInOneSnapshotOnly    = new TreeMap<>();

    // <snapshot <table, <columns> >> we are using TreeMap in order to use save order regardless of the JDK
    private Map<String, Map<String, List<String>>> columnPresentInOneSnapshotOnly = new TreeMap<>();

    // <snapshot <table, <indexes> >> we are using TreeMap in order to use save order regardless of the JDK
    private Map<String, Map<String, List<String>>> indexPresentInOneSnapshotOnly  = new TreeMap<>();

    public DatabaseEqualityState( String firstSnapshotName, String secondSnapshotName ) {

        this.firstSnapshotName = firstSnapshotName;
        this.secondSnapshotName = secondSnapshotName;
    }

    /**
     * @return the name of the first snapshot
     */
    @PublicAtsApi
    public String getFirstSnapshotName() {

        return firstSnapshotName;
    }

    /**
     * @return the name of the second snapshot
     */
    @PublicAtsApi
    public String getSecondSnapshotName() {

        return secondSnapshotName;
    }

    /**
     * @return whether some differences are found
     */
    @PublicAtsApi
    public boolean hasDifferences() {

        return tablePresentInOneSnapshotOnly.size() > 0 || differentPrimaryKeys.size() > 0
               || differentNumberOfRows.size() > 0 || rowPresentInOneSnapshotOnly.size() > 0
               || columnPresentInOneSnapshotOnly.size() > 0 || indexPresentInOneSnapshotOnly.size() > 0;
    }

    /**
     * @param snapshot snapshot name
     * @return tables present in one snapshot only
     */
    @PublicAtsApi
    public List<String> getTablesPresentInOneSnapshotOnly( String snapshot ) {

        List<String> tablesPerSnapshot = tablePresentInOneSnapshotOnly.get(snapshot);;
        if (tablesPerSnapshot == null) {
            tablesPerSnapshot = new ArrayList<String>();
        }

        return tablesPerSnapshot;
    }

    public void addTablePresentInOneSnapshotOnly( String snapshotName, String table ) {

        List<String> tablesPerSnapshot = tablePresentInOneSnapshotOnly.get(snapshotName);
        if (tablesPerSnapshot == null) {
            tablesPerSnapshot = new ArrayList<String>();
            tablePresentInOneSnapshotOnly.put(snapshotName, tablesPerSnapshot);
        }

        tablesPerSnapshot.add(table);
    }

    /**
     * Get list of tables with different primary keys <br>
     * Note that the list of tables is the same for both snapshots
     * 
     * @param snapshot snapshot name
     * @return tables with different primary keys
     */
    @PublicAtsApi
    public List<String> getTablesWithDifferentPrimaryKeys( String snapshot ) {

        List<String> tables = new ArrayList<>();
        if (differentPrimaryKeys.containsKey(snapshot)) {
            tables.addAll(differentPrimaryKeys.get(snapshot).keySet());
        }

        return tables;
    }

    /**
     * Get the different primary keys per table and snapshot. <br>
     * Note that we currently do not support more than one primary key difference
     * 
     * @param snapshot snapshot name
     * @param table table name
     * @return the different primary keys
     */
    @PublicAtsApi
    public String getDifferentPrimaryKeys( String snapshot, String table ) {

        String primaryKey = null;

        Map<String, String> keysPerTable = differentPrimaryKeys.get(snapshot);
        if (keysPerTable != null) {
            primaryKey = keysPerTable.get(table);
        }

        return primaryKey != null
                                  ? primaryKey
                                  : "";
    }

    public void addDifferentPrimaryKeys( String firstSnapshotName, String secondSnapshotName,
                                         String firstPrimaryKey, String secondPrimaryKey, String table ) {

        addDifferentPrimaryKey(firstSnapshotName, firstPrimaryKey, table);
        addDifferentPrimaryKey(secondSnapshotName, secondPrimaryKey, table);
    }

    private void addDifferentPrimaryKey( String snapshot, String primaryKey, String table ) {

        Map<String, String> _differentPrimaryKeys = differentPrimaryKeys.get(snapshot);
        if (_differentPrimaryKeys == null) {
            _differentPrimaryKeys = new TreeMap<>();
            differentPrimaryKeys.put(snapshot, _differentPrimaryKeys);
        }
        _differentPrimaryKeys.put(table, primaryKey);
    }

    /**
     * Get list of tables with different number of rows <br>
     * Note that the list of tables is the same for both snapshots
     * 
     * @param snapshot snapshot name
     * @return tables with different number of rows
     */
    @PublicAtsApi
    public List<String> getTablesWithDifferentNumberOfRows( String snapshot ) {

        List<String> tables = new ArrayList<>();
        if (differentNumberOfRows.containsKey(snapshot)) {
            tables.addAll(differentNumberOfRows.get(snapshot).keySet());
        }

        return tables;
    }

    /**
     * Get the number of rows for the given table and snapshot. <br>
     * 
     * @param snapshot snapshot name
     * @param table table name
     * @return number of rows
     */
    @PublicAtsApi
    public Integer getDifferentNumberOfRows( String snapshot, String table ) {

        Integer numberRows = null;

        Map<String, Integer> numberRowsPerTable = differentNumberOfRows.get(snapshot);
        if (numberRowsPerTable != null) {
            numberRows = numberRowsPerTable.get(table);
        }

        return numberRows;
    }

    public void addDifferentNumberOfRows( String firstSnapshotName, String secondSnapshotName,
                                          int firstNumberOfRows, int secondNumberOfRows, String table ) {

        addDifferentNumberOfRows(firstSnapshotName, table, firstNumberOfRows);
        addDifferentNumberOfRows(secondSnapshotName, table, secondNumberOfRows);
    }

    private void addDifferentNumberOfRows( String snapshot, String table, Integer numberOfRows ) {

        Map<String, Integer> _differentNumberOfRows = differentNumberOfRows.get(snapshot);
        if (_differentNumberOfRows == null) {
            _differentNumberOfRows = new TreeMap<>();
            differentNumberOfRows.put(snapshot, _differentNumberOfRows);
        }
        _differentNumberOfRows.put(table, numberOfRows);
    }

    /**
     * Get list of tables with unique columns <br>
     * 
     * @param snapshot snapshot name
     * @return tables with unique columns
     */
    @PublicAtsApi
    public List<String> getTablesWithColumnsPresentInOneSnapshotOnly( String snapshot ) {

        return breakIntoTables(columnPresentInOneSnapshotOnly, snapshot);
    }

    /**
     * Get a list of unique columns for the given table and snapshot. <br>
     * 
     * We return a list each item of which represents a column description.
     * The list contains a map where the key is a column attribute name while the value
     * is a column attribute value.
     * 
     * @param snapshot snapshot name
     * @param table table name
     * @return list of unique columns
     */
    @PublicAtsApi
    public List<Map<String, String>> getColumnsPresentInOneSnapshotOnly( String snapshot, String table ) {

        return breakIntoEntityAttributes(columnPresentInOneSnapshotOnly, snapshot, table, ", ");
    }

    /**
     * Get a list of unique columns for the given table and snapshot. <br>
     * 
     * We return a list each item of which is a String describing a column.
     * 
     * @param snapshot snapshot name
     * @param table table name
     * @return list of unique columns
     */
    @PublicAtsApi
    public List<String> getColumnsPresentInOneSnapshotOnlyAsStrings( String snapshot, String table ) {

        List<String> result = new ArrayList<>();

        Map<String, List<String>> columnsPerTable = columnPresentInOneSnapshotOnly.get(snapshot);
        if (columnsPerTable != null) {
            result = columnsPerTable.get(table);
        }

        return result;
    }

    public void addColumnPresentInOneSnapshotOnly( String snapshotName, String table, String column ) {

        Map<String, List<String>> tablesPerSnapshot = columnPresentInOneSnapshotOnly.get(snapshotName);
        if (tablesPerSnapshot == null) {
            tablesPerSnapshot = new TreeMap<>();
            columnPresentInOneSnapshotOnly.put(snapshotName, tablesPerSnapshot);
        }

        List<String> columnsPerTable = tablesPerSnapshot.get(table);
        if (columnsPerTable == null) {
            columnsPerTable = new ArrayList<>();
            tablesPerSnapshot.put(table, columnsPerTable);
        }

        columnsPerTable.add(column);
    }

    /**
     * Get list of tables with unique indexes <br>
     * 
     * @param snapshot snapshot name
     * @return tables with unique indexes
     */
    @PublicAtsApi
    public List<String> getTablesWithIndexesPresentInOneSnapshotOnly( String snapshot ) {

        return breakIntoTables(indexPresentInOneSnapshotOnly, snapshot);
    }

    /**
     * Get a list of unique indexes for the given table and snapshot. <br>
     * 
     * We return a list each item of which represents an index description.
     * The list contains a map where the key is an index attribute name while the value
     * is an index attribute value.
     * 
     * @param snapshot snapshot name
     * @param table table name
     * @return list of unique indexes
     */
    @PublicAtsApi
    public List<Map<String, String>> getIndexesPresentInOneSnapshotOnly( String snapshot, String table ) {

        return breakIntoEntityAttributes(indexPresentInOneSnapshotOnly, snapshot, table, ", ");
    }

    /**
     * Get a list of unique indexes for the given table and snapshot. <br>
     * 
     * We return a list each item of which is a String describing an index.
     * 
     * @param snapshot snapshot name
     * @param table table name
     * @return list of unique indexes
     */
    @PublicAtsApi
    public List<String> getIndexesPresentInOneSnapshotOnlyAsStrings( String snapshot, String table ) {

        List<String> result = null;

        Map<String, List<String>> indexesPerTable = indexPresentInOneSnapshotOnly.get(snapshot);
        if (indexesPerTable != null) {
            result = indexesPerTable.get(table);
        }

        if (result == null) {
            // no indexes for that table, so return empty list
            result = new ArrayList<>();
        }

        return result;
    }

    public void addIndexPresentInOneSnapshotOnly( String snapshotName, String table, String indexName,
                                                  String index ) {

        Map<String, List<String>> tablesPerSnapshot = indexPresentInOneSnapshotOnly.get(snapshotName);
        if (tablesPerSnapshot == null) {
            tablesPerSnapshot = new TreeMap<>();
            indexPresentInOneSnapshotOnly.put(snapshotName, tablesPerSnapshot);
        }

        List<String> indexesPerTable = tablesPerSnapshot.get(table);
        if (indexesPerTable == null) {
            indexesPerTable = new ArrayList<>();
            tablesPerSnapshot.put(table, indexesPerTable);
        }

        indexesPerTable.add(indexName + ", " + index);
    }

    /**
     * Get list of tables with unique rows <br>
     * 
     * @param snapshot snapshot name
     * @return tables with unique rows
     */
    @PublicAtsApi
    public List<String> getTablesWithRowsPresentInOneSnapshotOnly( String snapshot ) {

        return breakIntoTables(rowPresentInOneSnapshotOnly, snapshot);
    }

    /**
     * Get a list of unique rows for the given table and snapshot. <br>
     * 
     * We return a list each item of which represents a row.
     * The list contains a map where the key is a column name while the value
     * is a value at that column of the row.
     * 
     * @param snapshot snapshot name
     * @param table table name
     * @return list of unique rows
     */
    @PublicAtsApi
    public List<Map<String, String>> getRowsPresentInOneSnapshotOnly( String snapshot, String table ) {

        return breakIntoEntityAttributes(rowPresentInOneSnapshotOnly, snapshot, table, "\\|");
    }

    /**
     * Get a list of unique rows for the given table and snapshot. <br>
     * 
     * We return a list each item of which is a String describing the row content.
     * 
     * @param snapshot snapshot name
     * @param table table name
     * @return list of unique indexes
     */
    @PublicAtsApi
    public List<String> getRowsPresentInOneSnapshotOnlyAsStrings( String snapshot, String table ) {

        List<String> result = new ArrayList<>();

        Map<String, List<String>> rowsPerTable = rowPresentInOneSnapshotOnly.get(snapshot);
        if (rowsPerTable != null) {
            result = rowsPerTable.get(table);
        }

        return result;
    }

    public void addRowPresentInOneSnapshotOnly( String snapshotName, String table, String rowValues ) {

        Map<String, List<String>> tablesPerSnapshot = rowPresentInOneSnapshotOnly.get(snapshotName);
        if (tablesPerSnapshot == null) {
            tablesPerSnapshot = new TreeMap<>();
            rowPresentInOneSnapshotOnly.put(snapshotName, tablesPerSnapshot);
        }

        List<String> rowsPerTable = tablesPerSnapshot.get(table);
        if (rowsPerTable == null) {
            rowsPerTable = new ArrayList<>();
            tablesPerSnapshot.put(table, rowsPerTable);
        }

        rowsPerTable.add(rowValues);
    }

    public void clearDifferentNumberOfRowsForTable( String tableName ) {

        // check if the table have different rows
        if (this.getDifferentNumberOfRows(firstSnapshotName, tableName) != null) {
            this.differentNumberOfRows.get(firstSnapshotName).remove(tableName);
            if (this.differentNumberOfRows.get(firstSnapshotName) == null
                || this.differentNumberOfRows.get(firstSnapshotName).isEmpty()) {
                this.differentNumberOfRows.remove(firstSnapshotName);
            }
        }
        if (this.getDifferentNumberOfRows(secondSnapshotName, tableName) != null) {
            this.differentNumberOfRows.get(secondSnapshotName).remove(tableName);
            if (this.differentNumberOfRows.get(secondSnapshotName) == null
                || this.differentNumberOfRows.get(secondSnapshotName).isEmpty()) {
                this.differentNumberOfRows.remove(secondSnapshotName);
            }
        }
    }

    public void clearRowsPresentedInOneSnapshotOnly( String tableName ) {

        if (this.rowPresentInOneSnapshotOnly.containsKey(firstSnapshotName)) {
            if (this.rowPresentInOneSnapshotOnly.get(firstSnapshotName).containsKey(tableName)) {
                this.rowPresentInOneSnapshotOnly.get(firstSnapshotName).remove(tableName);
                if (this.rowPresentInOneSnapshotOnly.get(firstSnapshotName) == null
                    || this.rowPresentInOneSnapshotOnly.get(firstSnapshotName).isEmpty()) {
                    this.rowPresentInOneSnapshotOnly.remove(firstSnapshotName);
                }
            }
        }

        if (this.rowPresentInOneSnapshotOnly.containsKey(secondSnapshotName)) {
            if (this.rowPresentInOneSnapshotOnly.get(secondSnapshotName).containsKey(tableName)) {
                this.rowPresentInOneSnapshotOnly.get(secondSnapshotName).remove(tableName);
                if (this.rowPresentInOneSnapshotOnly.get(secondSnapshotName) == null
                    || this.rowPresentInOneSnapshotOnly.get(secondSnapshotName).isEmpty()) {
                    this.rowPresentInOneSnapshotOnly.remove(secondSnapshotName);
                }
            }
        }
    }

    private List<String> breakIntoTables( Map<String, Map<String, List<String>>> entities, String snapshot ) {

        List<String> tables = new ArrayList<>();
        if (entities.containsKey(snapshot)) {
            tables.addAll(entities.get(snapshot).keySet());
        }

        return tables;
    }

    private List<Map<String, String>>
            breakIntoEntityAttributes( Map<String, Map<String, List<String>>> entities, String snapshot,
                                       String table, String delimeter ) {

        List<Map<String, String>> result = new ArrayList<>();

        Map<String, List<String>> entitiesPerTable = entities.get(snapshot);
        if (entitiesPerTable != null) {
            List<String> allEntitiesAsStrings = entitiesPerTable.get(table);

            if (allEntitiesAsStrings == null) {
                // no entries for the provided table in the provided snapshot
                return result;
            }

            // cycle all entities
            for (String entityAsString : allEntitiesAsStrings) {
                String[] entityAttributesAsString = entityAsString.split(delimeter);

                // cycle all attributes of one entity
                Map<String, String> attributes = new TreeMap<>();
                for (String entityAttributeAsString : entityAttributesAsString) {
                    String[] attributeTokens = entityAttributeAsString.split("=");
                    if (attributeTokens.length == 2) {
                        attributes.put(attributeTokens[0], attributeTokens[1]);
                    } else {
                        // expecting just key without a value
                        attributes.put(attributeTokens[0], "");
                    }
                }
                result.add(attributes);
            }
        }

        return result;
    }
}
