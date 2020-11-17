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
package com.axway.ats.action.dbaccess.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.action.dbaccess.snapshot.CompareOptions.Pair;
import com.axway.ats.action.dbaccess.snapshot.rules.SkipColumns;
import com.axway.ats.action.dbaccess.snapshot.rules.SkipContent;
import com.axway.ats.action.dbaccess.snapshot.rules.SkipIndexAttributes;
import com.axway.ats.action.dbaccess.snapshot.rules.SkipRows;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotException;
import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotUtils;
import com.axway.ats.common.dbaccess.snapshot.IndexMatcher;
import com.axway.ats.common.dbaccess.snapshot.TableDescription;
import com.axway.ats.common.dbaccess.snapshot.equality.DatabaseEqualityState;
import com.axway.ats.core.dbaccess.DatabaseProviderFactory;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.postgresql.PostgreSqlDbProvider;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.harness.config.TestBox;

/**
 * Main class for comparing databases. You can specify which tables to compare
 * or not as well as which columns to skip on some tables.
 */
public class DatabaseSnapshot {

    private static Logger            log                         = LogManager.getLogger(DatabaseSnapshot.class);

    // the snapshot name
    String                           name;

    // the time all meta data is taken
    long                             metadataTimestamp           = -1;
    // the time all table contents is taken
    long                             contentTimestamp            = -1;

    Document                         backupXmlFile;

    // DB connection parameters
    private TestBox                  testBox;
    private Map<String, Object>      customProperties;

    // description of all tables of interest
    List<TableDescription>           tables                      = new ArrayList<>();

    // this structure can say how equal the snapshots are
    private DatabaseEqualityState    equality;

    // our DB connector
    private DbProvider               dbProvider;

    // specifies which table columns to be skipped
    // MAP< table name(in lower case) , columns to skip >
    Map<String, SkipColumns>         skipColumnsPerTable         = new HashMap<>();

    // list of tables which content is not of importance
    // MAP< table name(in lower case) , content to skip >
    Map<String, SkipContent>         skipContentPerTable         = new HashMap<>();

    // specifies which table rows to be skipped
    // MAP< table name(in lower case) , rows to skip >
    Map<String, SkipRows>            skipRowsPerTable            = new HashMap<>();

    Map<String, Set<Properties>>     skipIndexesPerTable         = new HashMap<>();

    // MAP< table name(in lower case) , index attributes to skip >
    Map<String, SkipIndexAttributes> skipIndexAttributesPerTable = new HashMap<>();

    //  An interface which tells whether some table index should be treated as same or not
    private IndexMatcher             indexMatcher;

    /**
     * Constructor providing snapshot name and connection parameters
     * 
     * @param name Snapshot name. Used as identifier for comparison results
     * @param testBox the DB connection parameters
     */
    @PublicAtsApi
    public DatabaseSnapshot( String name, TestBox testBox ) {

        this(name, testBox, null);
    }

    /**
     * Constructor providing snapshot name and connection parameters
     * 
     * @param name Snapshot name. Used as identifier for comparison results
     * @param testBox the DB connection parameters
     * @param customProperties some connection parameters specific for a particular DB provider
     */
    @PublicAtsApi
    public DatabaseSnapshot( String name, TestBox testBox, Map<String, Object> customProperties ) {

        if (StringUtils.isNullOrEmpty(name)) {
            throw new DatabaseSnapshotException("Invalid snapshot name '" + name + "'");
        }
        this.name = name.trim();

        this.testBox = testBox;
        this.customProperties = customProperties;
    }

    /**
     * Specify tables which are not to be checked at all
     * 
     * @param tables one or many tables
     */
    @PublicAtsApi
    public void skipTables( String... tables ) {

        for (String table : tables) {
            skipColumnsPerTable.put(table.toLowerCase(), new SkipColumns(table));
        }
    }

    /**
     * Specify a column which values will not be read when comparing the table content.
     * <br>Note: the column meta information(like column type and indexes it participates into) is still compared
     * 
     * @param table table name
     * @param column column
     */
    @PublicAtsApi
    public void skipTableColumn( String table, String column ) {

        skipColumnsPerTable.put(table.toLowerCase(), new SkipColumns(table, column));
    }

    /**
     * Specify columns which values will not be read when comparing the table content.
     * <br>Note: the column meta information(like column type and indexes it participates into) is still compared
     * 
     * @param table table name
     * @param columns columns
     */
    @PublicAtsApi
    public void skipTableColumns( String table, String... columns ) {

        skipColumnsPerTable.put(table.toLowerCase(), new SkipColumns(table, columns));
    }

    /**
     * Specify table(s) which content (rows) will not be checked.
     * Note that table meta-data is still checked.
     * 
     * @param tables one or many tables
     */
    @PublicAtsApi
    public void skipTableContent( String... tables ) {

        for (String table : tables) {
            skipContentPerTable.put(table.toLowerCase(), new SkipContent(table, false));
        }
    }

    /**
     * Specify table(s) which content (rows) will not be checked, but
     * we will check whether the number of rows is same. <br>
     * Some prefer to use this method for tables with changing binary data
     * (for example some certificates), but they still
     * want to verify the number of rows(for example number of certificates) is not changed. <br>
     * Note that table meta-data is also checked.
     * 
     * @param tables one or many tables
     */
    @PublicAtsApi
    public void skipTableContentWithCheckForNumberOfRows( String... tables ) {

        for (String table : tables) {
            skipContentPerTable.put(table.toLowerCase(), new SkipContent(table, true));
        }
    }

    /**
     * Allows skipping rows in a table that contain some value at some column.
     * <p>
     * It causes checks on each row whether it matches the expected value at the specified column.
     * On match, this row is not loaded from the database.
     * <p>
     * Note: you can use a regular expression for value to match.
     * 
     * @param table the table
     * @param column the column where the value will be searched
     * @param value the value to match
     */
    @PublicAtsApi
    public void skipTableRows( String table, String column, String value ) {

        SkipRows skipRowsForThisTable = skipRowsPerTable.get(table.toLowerCase());
        if (skipRowsForThisTable == null) {
            skipRowsForThisTable = new SkipRows(table);
            skipRowsPerTable.put(table.toLowerCase(), skipRowsForThisTable);
        }
        skipRowsForThisTable.addRowToSkip(column, value);
    }

    /**
     * Allows skipping index in table that match all of the provided properties
     * @param table - the table name
     * @param indexProperties - the index properties. For the index properties' names see one of the [Oracle|Mssql|MySQL|Postgresql]DbProvider.IndexProperties , according to the database type
     * */
    @PublicAtsApi
    public void skipTableIndex( String table, Properties indexProperties ) {

        Set<Properties> skipIndexesForThisTable = skipIndexesPerTable.get(table);
        if (skipIndexesForThisTable == null) {
            skipIndexesForThisTable = new HashSet<Properties>();
            skipIndexesPerTable.put(table, skipIndexesForThisTable);
        }
        skipIndexesForThisTable.add(indexProperties);

    }

    /**
     * Allows skipping attributes of some index of some table
     * 
     * @param table the table
     * @param index the index
     * @param attribute the index attribute name
     */
    @PublicAtsApi
    public void skipIndexAttributes( String table, String index, String attribute ) {

        SkipIndexAttributes skipIndexAttributes = this.skipIndexAttributesPerTable.get(table.toLowerCase());
        if (skipIndexAttributes == null) {
            skipIndexAttributes = new SkipIndexAttributes(table.toLowerCase());
            this.skipIndexAttributesPerTable.put(table.toLowerCase(), skipIndexAttributes);
        }
        skipIndexAttributes.setAttributeToSkip(index.toLowerCase(), attribute);
    }

    /**
     * Provide instance of this interface which will define 
     * whether some table index should be treated as same or not.<br><br>
     * 
     * <b>Note:</b> If not used, the index are compared by their names as regular text.<br>
     * 
     * @param indexMatcher the custom implementation.
     */
    @PublicAtsApi
    public void setIndexMatcher( IndexMatcher indexMatcher ) {

        this.indexMatcher = indexMatcher;
    }

    /**
     * Provide a java regular expression which will define 
     * whether some table index should be treated as same or not.<br><br>
     * <b>Note:</b> The regular expression is applied on the index names,
     * the first matched subsequences of both index names are compared for equality.<br>
     * 
     * In other words, we compare whatever is returned by the {@link Matcher#find()} method 
     * when applied on both index names.<br><br>
     * 
     * 
     * <b>Note:</b> If not used, the indexes are compared against their names as regular text, e.g. whether indexOneName.equals(indexTwoName) is true or false<br>
     * Also note that if there are different properties for some indexes, 
     * use {@link DatabaseSnapshot#setIndexMatcher(IndexMatcher)} 
     * where you can skip the default comparison of index properties by overriding the {@link IndexMatcher#isSame(String, Properties, Properties)} method
     * 
     * 
     * @param indexNameRegex a java regular expression
     */
    @PublicAtsApi
    public void setIndexMatcher( final String indexNameRegex ) {

        this.indexMatcher = new IndexMatcher() {
            @Override
            public boolean isSame( String table, String firstName, String secondName ) {

                Pattern pattern = Pattern.compile(indexNameRegex);

                Matcher matcher1 = pattern.matcher(firstName);
                if (matcher1.find()) {
                    firstName = firstName.substring(matcher1.start(), matcher1.end());
                }

                Matcher matcher2 = pattern.matcher(secondName);
                if (matcher2.find()) {
                    secondName = secondName.substring(matcher2.start(), matcher2.end());
                }

                return firstName.equals(secondName);
            }

            @Override
            public boolean isSame( String table, Properties firstProperties, Properties secondProperties ) {

                // always false, since REGEX for name is wanted
                return false;
            }
        };
    }

    /**
     * Take a database snapshot<br>
     * <b>NOTE:</b> We will get only meta data about the tables in the database. 
     * No table content is loaded at this moment as this may cause memory issues.
     * The content of each table is loaded when needed while comparing this snapshot with another one, or while
     * saving the snapshot into a file.
     */
    @PublicAtsApi
    public void takeSnapshot() {

        this.metadataTimestamp = System.currentTimeMillis();

        dbProvider = DatabaseProviderFactory.getDatabaseProvider(testBox.getDbType(), testBox.getHost(),
                                                                 testBox.getDbName(), testBox.getDbUser(),
                                                                 testBox.getDbPass(), testBox.getDbPort(),
                                                                 customProperties);

        log.info("Start taking database meta information for snapshot [" + name + "] from "
                 + dbProvider.getDbConnection().getDescription());

        // load info about all present tables
        tables = dbProvider.getTableDescriptions(getSkippedTables());
        if (tables.size() == 0) {
            log.warn("No tables found for snapshot [" + name + "]");
        }

        // remove the tables that are to be skipped
        for (String tableToSkip : getAllTablesToSkip(skipColumnsPerTable)) {
            for (TableDescription table : tables) {
                if (table.getName().equalsIgnoreCase(tableToSkip)) {
                    tables.remove(table);
                    break;
                }
            }
        }

        // remove indexes that are to be skipped
        skipIndexes();

        // we have loaded all index info, strip some if needed
        stripIndexAttributes(tables);

        for (TableDescription table : tables) {
            table.setSnapshotName(this.name);
        }

        // in case we have made a backup file, now it is time to forget about it
        this.backupXmlFile = null;

        log.info("End taking database meta information for snapshot with name " + name);
    }

    private void skipIndexes() {

        try {
            for (TableDescription tableDesc : tables) {
                if (tableDesc.getIndexes() == null || tableDesc.getIndexes().isEmpty()) {
                    // no indexes, so nothing to skip
                    continue;
                }

                String tableName = tableDesc.getName();
                if (!this.skipIndexesPerTable.containsKey(tableName)) {
                    // no indexes are to be skipped for this table
                    continue;
                }

                if (this.skipIndexesPerTable.get(tableName) == null
                    || this.skipIndexesPerTable.get(tableName).isEmpty()) {
                    log.warn("There are skipped Indexes for table '" + tableName
                             + "', but they have no properties, e.g. empty properties are provided for them, so no skipping of indexes will be performed");
                    continue;
                }

                // begin skipping indexes
                Map<String, String> loadedIndexes = tableDesc.getIndexes();
                Map<String, String> finalIndexes = new HashMap<String, String>();

                // iterate over each of the loaded indexes
                for (String indexKey : loadedIndexes.keySet()) {
                    String indexValue = loadedIndexes.get(indexKey);
                    if (StringUtils.isNullOrEmpty(indexValue)) {
                        // is it possible for index to not have any properties?
                        throw new RuntimeException("Index '" + indexKey
                                                   + "' from table '" + tableName + "' has NO properties loaded!");
                    }
                    // get current index properties
                    Properties indexProperties = tableDesc.getIndexProperties(indexKey);

                    // a set of properties that the user wants to be used in order to be able to check which index is to be skipped
                    Set<Properties> skippedIndexesProperties = this.skipIndexesPerTable.get(tableName);
                    if (!isIndexToBeSkipped(indexProperties, skippedIndexesProperties)) {
                        finalIndexes.put(indexKey, indexValue);
                    }
                }

                //tableDesk.getIndexes().clear();
                tableDesc.setIndexes(finalIndexes);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while skipping indexes", e);
        }

    }

    private boolean isIndexToBeSkipped( Properties indexProperties, Set<Properties> skippedIndexesProperties ) {

        for (Properties skipIndexProperties : skippedIndexesProperties) {

            if (skipIndexProperties == null || skipIndexProperties.isEmpty()) {
                continue;
            }

            boolean found = true;

            for (Map.Entry<Object, Object> entry : skipIndexProperties.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                if (indexProperties.containsKey(key)) {
                    Object val = indexProperties.get(key);

                    // most of the time, if not always, the value will be String, but keep this, just in case we change this in the future
                    if (value.getClass().isPrimitive()) {
                        if (value != val) {
                            found = false;
                            break;
                        }
                    } else {
                        if (!value.equals(val)) {
                            found = false;
                            break;
                        }
                    }
                } else {
                    // the user-provided index property is actually not found in the ones, obtained from DB.
                    // no reason to continue, so the index will not be skipped
                    log.warn("Index property '" + key + "' is not supported for table index for database of type "
                             + this.dbProvider.getDbConnection().getDbType() + "!");
                    found = false;
                    break;
                }
            }

            if (found) {
                return true;
            }

        }
        return false;
    }

    /**
     * @see DatabaseSnapshot#compare(DatabaseSnapshot, CompareOptions)
     * */
    public void compare( DatabaseSnapshot that ) throws DatabaseSnapshotException {

        this.compare(that, null);
    }

    /**
     * Compare both snapshots and throw error if unexpected differences are found.
     * Snapshots are compared table by table. 
     * In the usual case the tables are loaded from database prior to comparing.
     * But if a snapshot was saved into a file, then its tables are loaded from the file, not from the database.
     * 
     * @param that the snapshot to compare to
     * @param compareOptions - (optional) additional options that change the comparison. by default is null, so the compare is as-is (e.g. if there is an error, the comparison fails) 
     * @throws DatabaseSnapshotException
     */
    @PublicAtsApi
    public void compare( DatabaseSnapshot that, CompareOptions compareOptions ) throws DatabaseSnapshotException {

        try {
            if (that == null) {
                throw new DatabaseSnapshotException("Snapshot to compare is null");
            }
            if (this.name.equals(that.name)) {
                throw new DatabaseSnapshotException("You are trying to compare snapshots with same name: "
                                                    + this.name);
            }
            if (this.metadataTimestamp == -1) {
                throw new DatabaseSnapshotException("You are trying to compare snapshots but [" + this.name
                                                    + "] snapshot is still not created");
            }
            if (that.metadataTimestamp == -1) {
                throw new DatabaseSnapshotException("You are trying to compare snapshots but [" + that.name
                                                    + "] snapshot is still not created");
            }

            if (log.isDebugEnabled()) {
                log.debug("Comparing snapshots [" + this.name + "] taken on "
                          + DatabaseSnapshotUtils.dateToString(this.metadataTimestamp) + " and [" + that.name
                          + "] taken on " + DatabaseSnapshotUtils.dateToString(that.metadataTimestamp));
            }

            this.equality = new DatabaseEqualityState(this.name, that.name);

            // make copies of the table info, as we will remove from these lists,
            // but we do not want to remove from the original table info lists
            List<TableDescription> thisTables = new ArrayList<TableDescription>(this.tables);
            List<TableDescription> thatTables = new ArrayList<TableDescription>(that.tables);

            // Merge all skip rules from both snapshot instances, 
            // so it is not needed to add same skip rules in both snapshot instances.

            // merge all columns to be skipped(per table)
            Map<String, SkipColumns> skipColumns = mergeSkipColumns(that.skipColumnsPerTable);

            // merge all content to be skipper(per table)
            Map<String, SkipContent> skipContent = mergeSkipContent(that.skipContentPerTable);

            // merge all rows to be skipped(per table)
            Map<String, SkipRows> skipRows = mergeSkipRows(that.skipRowsPerTable);

            Set<String> tablesToSkip = getAllTablesToSkip(skipColumns);

            // We can use just one index name matcher
            IndexMatcher actualIndexNameMatcher = mergeIndexMatchers(that.indexMatcher);

            compareTables(this.name, thisTables, that.name, thatTables, that.dbProvider, tablesToSkip,
                          skipColumns, skipContent, skipRows, actualIndexNameMatcher, that.backupXmlFile,
                          equality);

            if (compareOptions != null) {
                /*try {
                    // handle expected differences
                    handleExpectedDifferentNumberOfRows(compareOptions, equality);
                } catch (Exception e) {
                    log.error("Error occured while handling different number of rows", e);
                }*/

                try {
                    // handle expected differences
                    handleExpectedMissingRows(compareOptions, equality);
                } catch (Exception e) {
                    log.error("Error occured while handling missing rows", e);
                }
            }

            if (equality.hasDifferences()) {
                // there are some unexpected differences
                throw new DatabaseSnapshotException(equality);
            } else {
                log.info("Successful verification");
            }
        } finally {
            // close the database connections
            disconnect(this.dbProvider, "after comparing database snapshots");
            disconnect(that.dbProvider, "after comparing database snapshots");
        }
    }

    private void handleExpectedMissingRows( CompareOptions compareOptions, DatabaseEqualityState equality ) {

        for (Map.Entry<String, Pair<Integer>> entry : compareOptions.getExpectedTableMissingRows()
                                                                    .entrySet()) {

            String tableName = entry.getKey();
            Pair<Integer> rows = entry.getValue();

            List<Map<String, String>> rowsOnlyInFirstSnapshot = equality.getRowsPresentInOneSnapshotOnly(equality.getFirstSnapshotName(),
                                                                                                         tableName);

            List<Map<String, String>> rowsOnlyInSecondSnapshot = equality.getRowsPresentInOneSnapshotOnly(equality.getSecondSnapshotName(),
                                                                                                          tableName);

            int totalMissingRows = 0;
            if (rowsOnlyInFirstSnapshot != null) {
                totalMissingRows += rowsOnlyInFirstSnapshot.size();
            }
            if (rowsOnlyInSecondSnapshot != null) {
                totalMissingRows += rowsOnlyInSecondSnapshot.size();
            }

            if (totalMissingRows < rows.first) {
                log.error("Table '" + tableName + "' is expected to have at least " + rows.first
                          + " missing rows between snapshots, but instead has " + totalMissingRows);
                continue;
            }

            if (totalMissingRows > rows.second) {
                log.error("Table '" + tableName + "' is expected to have at most " + rows.second
                          + " missing rows between snapshots, but instead has " + totalMissingRows);
                continue;
            }

            // assume that this table is OK
            equality.clearRowsPresentedInOneSnapshotOnly(tableName);

            // and also clear the different rows error for that table (if such errors exists)

            // This is OK to be cleared, since this error is a result of the already expected one (the one we handled above)
            equality.clearDifferentNumberOfRowsForTable(tableName);
        }

    }

    /*private void handleExpectedDifferentNumberOfRows( CompareOptions compareOptions, DatabaseEqualityState equality ) {
    
        for (Map.Entry<String, Pair<Integer>> entry : compareOptions.getExpectedTableDifferentNumberOfRows()
                                                                    .entrySet()) {
            String tableName = entry.getKey();
            Pair<Integer> rows = entry.getValue();
    
            Integer differentNumberOfRowsFirstSnapshot = equality.getDifferentNumberOfRows(equality.getFirstSnapshotName(),
                                                                                           tableName);
            Integer differentNumberOfRowsSecondSnapshot = equality.getDifferentNumberOfRows(equality.getSecondSnapshotName(),
                                                                                            tableName);
    
            int finalDifferentNumberOfRows = 0;
            if (differentNumberOfRowsFirstSnapshot == null) {
                if (differentNumberOfRowsSecondSnapshot == null) {
                    // no different rows
                    if (compareOptions.isFailOnMissingExpectedError()) {
                        throw new RuntimeException("Expected different number of rows for table '" + tableName
                                                   + "' is not produced by the database snapshot comparison!");
                    }
                } else {
                    // use data from the second snapshot
                    finalDifferentNumberOfRows = differentNumberOfRowsSecondSnapshot;
                }
            } else {
                if (differentNumberOfRowsSecondSnapshot == null) {
                    // use data from the first snapshot
                    finalDifferentNumberOfRows = differentNumberOfRowsFirstSnapshot;
                } else {
                    // both snapshots have different rows information
                    finalDifferentNumberOfRows = Math.abs( (differentNumberOfRowsFirstSnapshot
                                                            - differentNumberOfRowsSecondSnapshot));
                }
            }
    
            if (finalDifferentNumberOfRows < rows.first) {
                throw new RuntimeException("Table '" + tableName + "' is expected to have at least " + rows.first
                                           + " different rows, but instead has " + finalDifferentNumberOfRows);
            }
    
            if (finalDifferentNumberOfRows > rows.second) {
                throw new RuntimeException("Table '" + tableName + "' is expected to have at most " + rows.second
                                           + " different rows, but instead has " + finalDifferentNumberOfRows);
            }
    
            // assume that this table is OK
            equality.clearDifferentNumberOfRowsForTable(tableName);
    
        }
    
    }*/

    /**
     * Save a snapshot into a file.<br>
     * <b>NOTE:</b> This is the moment when the contents of each table is read from the database 
     * one at a time and is saved into the file.
     * 
     * @param backupFile the backup file name
     */
    @PublicAtsApi
    public void saveToFile( String backupFile ) {

        backupXmlFile = new DatabaseSnapshotBackupUtils().saveToFile(this, backupFile);

        // close the database connection
        disconnect(this.dbProvider, "after saving database snapshot into " + backupFile);
    }

    /**
     * Load a snapshot from a file
     * 
     * @param newSnapshotName the name of the new snapshot
     * <br>Pass null or empty string if want to use the snapshot name as saved in the file,
     * or provide a new name here
     * @param sourceFile the backup file name
     */
    @PublicAtsApi
    public void loadFromFile( String newSnapshotName, String sourceFile ) {

        backupXmlFile = new DatabaseSnapshotBackupUtils().loadFromFile(newSnapshotName, this, sourceFile);
    }

    /**
     * Compares all tables between two snapshots
     * 
     * @param thisSnapshotName
     * @param thisTables
     * @param thatSnapshotName
     * @param thatTables
     * @param thatDbProvider
     * @param tablesToSkip
     * @param skipColumns
     * @param skipContent
     * @param skipRows
     * @param indexNameMatcher
     * @param thatBackupXmlFile
     * @param equality
     */
    private void compareTables( String thisSnapshotName, List<TableDescription> thisTables,
                                String thatSnapshotName, List<TableDescription> thatTables,
                                DbProvider thatDbProvider, Set<String> tablesToSkip,
                                Map<String, SkipColumns> skipColumns, Map<String, SkipContent> skipContent,
                                Map<String, SkipRows> skipRows,
                                IndexMatcher indexNameMatcher,
                                Document thatBackupXmlFile, DatabaseEqualityState equality ) {

        // make a list of tables present in both snapshots
        List<String> commonTables = getCommonTables(thisSnapshotName, thisTables, thatSnapshotName,
                                                    thatTables, tablesToSkip);

        for (String tableName : commonTables) {
            // get tables to compare
            TableDescription thisTable = null;
            TableDescription thatTable = null;
            for (TableDescription table : thisTables) {
                if (table.getName().equalsIgnoreCase(tableName)) {
                    thisTable = table;
                    break;
                }
            }
            for (TableDescription table : thatTables) {
                if (table.getName().equalsIgnoreCase(tableName)) {
                    thatTable = table;
                    break;
                }
            }

            SkipColumns skipColumnsPerTable = skipColumns.get(thisTable.getName());
            if (skipColumnsPerTable != null && skipColumnsPerTable.isSkipWholeTable()) {
                // if table is not of interest - skip it
                continue;
            }
            List<String> thisValuesList = null;
            List<String> thatValuesList = null;

            int thisNumberOfRows = -1;
            int thatNumberOfRows = -1;
            SkipContent skipContentForThisTable = skipContent.get(tableName.toLowerCase());
            if (skipContentForThisTable != null) {
                if (skipContentForThisTable.isRememberNumberOfRows()) {
                    // we do not compare the content of the tables,
                    // but we still compare the number of rows
                    thisNumberOfRows = loadTableLength(thisSnapshotName, thisTable, this.dbProvider,
                                                       this.backupXmlFile);
                    thatNumberOfRows = loadTableLength(thatSnapshotName, thatTable, thatDbProvider,
                                                       thatBackupXmlFile);
                }
                // else -> we completely do not compare the content of the tables
            } else {
                // we want to compare the content of the tables,
                // so load the table content
                thisValuesList = loadTableData(thisSnapshotName, thisTable, skipColumns, skipRows,
                                               this.dbProvider, this.backupXmlFile);
                thatValuesList = loadTableData(thatSnapshotName, thatTable, skipColumns, skipRows,
                                               thatDbProvider, thatBackupXmlFile);
            }

            // do the actual comparison
            thisTable.compare(thatTable, thisValuesList, thatValuesList, thisNumberOfRows, thatNumberOfRows,
                              indexNameMatcher, equality);
        }

        thisTables.clear();
    }

    /**
     * @return list of tables that are fully skipped(including their meta data)
     */
    private List<String> getSkippedTables() {

        List<String> tablesToSkip = new ArrayList<String>();
        for (SkipColumns tableToSkip : skipColumnsPerTable.values()) {
            if (tableToSkip.isSkipWholeTable()) {
                tablesToSkip.add(tableToSkip.getTable());
            }
        }
        return tablesToSkip;
    }

    /**
     * The Index Matcher can come from first or second snapshot instance.
     * Or maybe there is none provided by the user.
     * 
     * @param thatIndexMatcher
     * @return
     */
    private IndexMatcher mergeIndexMatchers( IndexMatcher thatIndexMatcher ) {

        if (this.indexMatcher != null) {
            // use THIS matcher
            if (thatIndexMatcher != null) {
                log.warn("You have provided Index Matchers for both snapshots. We selected to use the one from snapshot ["
                         + this.name + "]");
            }
            return this.indexMatcher;
        } else if (thatIndexMatcher != null) {
            // use THAT matcher
            return thatIndexMatcher;
        } else {
            // index matcher is not provided for any snapshot
            // create a default one which matches index name literally
            return new IndexMatcher() {
                @Override
                public boolean isSame( String table, String firstName, String secondName ) {

                    return firstName.equals(secondName);
                }

                @Override
                public boolean isSame( String table, Properties firstProperties, Properties secondProperties ) {

                    // Should it be false?
                    return false;
                }

            };
        }
    }

    /**
     * Here we merge skip table columns from both snapshots into a single list.
     * It doesn't make sense to ask people to add same skip rules for same tables in both snapshot instances.
     * 
     * @param thatSkipColumnsPerTable
     * @return
     */
    private Map<String, SkipColumns> mergeSkipColumns( Map<String, SkipColumns> thatSkipColumnsPerTable ) {

        // we will return a new instance containing all rules
        Map<String, SkipColumns> allSkipColumnsPerTable = new HashMap<>();

        // first add all rules for this instance
        for (String table : this.skipColumnsPerTable.keySet()) {
            allSkipColumnsPerTable.put(table, this.skipColumnsPerTable.get(table));
        }

        // then add all rules for the other instance
        for (String table : thatSkipColumnsPerTable.keySet()) {
            SkipColumns allSkipColumns = allSkipColumnsPerTable.get(table);
            SkipColumns thatSkipColumns = thatSkipColumnsPerTable.get(table);

            if (allSkipColumns != null) {
                // there is already a rule for this table
                SkipColumns thisSkipColumns = this.skipColumnsPerTable.get(table);
                if (thisSkipColumns.isSkipWholeTable()) {
                    // nothing to do, the table will be skipped anyway
                } else if (thatSkipColumns.isSkipWholeTable()) {
                    // replace the current rule, so skip the table
                    allSkipColumnsPerTable.put(table, thatSkipColumns);
                } else {
                    // just some columns will be skipped, merge the list of columns
                    for (String column : thatSkipColumns.getColumnsToSkip()) {
                        allSkipColumns.addColumnToSkip(column);
                    }
                }
            } else {
                // no current rule for this table, now we add one
                allSkipColumnsPerTable.put(table, thatSkipColumns);
            }
        }

        return allSkipColumnsPerTable;
    }

    /**
     * Retrieve all tables that are to be skipped
     * 
     * @param skipColumns
     * @return
     */
    private Set<String> getAllTablesToSkip( Map<String, SkipColumns> skipColumns ) {

        Set<String> tablesToSkip = new HashSet<>();

        for (String table : skipColumns.keySet()) {
            SkipColumns rule = skipColumns.get(table);
            if (rule.isSkipWholeTable()) {
                tablesToSkip.add(table);
            }
        }

        return tablesToSkip;
    }

    private Map<String, SkipRows> mergeSkipRows( Map<String, SkipRows> thatSkipRowsPerTable ) {

        // we will return a new instance containing all rules
        Map<String, SkipRows> allSkipRowsPerTable = new HashMap<>();

        // first add all rules for this instance
        for (String table : this.skipRowsPerTable.keySet()) {
            allSkipRowsPerTable.put(table, this.skipRowsPerTable.get(table));
        }

        // then add all rules for the other instance
        for (String table : thatSkipRowsPerTable.keySet()) {
            SkipRows allSkipRows = allSkipRowsPerTable.get(table);
            SkipRows thatSkipRows = thatSkipRowsPerTable.get(table);

            if (allSkipRows != null) {
                // there is already a rule for this table
                // we have to merge both rules
                allSkipRows.addRowsToSkip(thatSkipRows.getSkipExpressions());
            } else {
                // no current rule for this table, now we add one
                allSkipRowsPerTable.put(table, thatSkipRows);
            }
        }

        return allSkipRowsPerTable;
    }

    /**
     * Merge skip table content rules from both snapshots into a single list.
     * 
     * @param thatSkipContentPerTable list with table names
     * @return
     */
    private Map<String, SkipContent> mergeSkipContent( Map<String, SkipContent> thatSkipContentPerTable ) {

        // we will return a new instance containing all rules
        Map<String, SkipContent> allSkipContentPerTable = new HashMap<>();

        // first add all rules for this instance
        for (String table : this.skipContentPerTable.keySet()) {
            allSkipContentPerTable.put(table, this.skipContentPerTable.get(table));
        }

        // then add all rules for the other instance
        for (String table : thatSkipContentPerTable.keySet()) {
            SkipContent allSkipContent = allSkipContentPerTable.get(table);
            SkipContent thatSkipContent = thatSkipContentPerTable.get(table);

            if (allSkipContent != null) {
                // there is already a rule for this table
                // we have to merge both rules
                allSkipContent.setRememberNumberOfRows(thatSkipContent.isRememberNumberOfRows());
            } else {
                // no current rule for this table, now we add one
                allSkipContentPerTable.put(table, thatSkipContent);
            }
        }

        return allSkipContentPerTable;
    }

    /**
     * After we have loaded all table indexes, here we remove the not wanted attributes.
     * 
     * The cleaner way would be to not even load all the not wanted attributes, but in such case
     * we would have to pass all those attributes down to all DB provided implementations.
     * 
     * @param tables
     */
    private void stripIndexAttributes( List<TableDescription> tables ) {

        // cycle all tables
        for (TableDescription table : tables) {
            SkipIndexAttributes skipIndexAttributes = this.skipIndexAttributesPerTable.get(table.getName()
                                                                                                .toLowerCase());
            if (skipIndexAttributes != null) {
                // there is some index attribute to be skipped for this table
                // cycle all indexes of this table
                Map<String, String> indexes = table.getIndexes();
                for (String indexName : indexes.keySet()) {
                    List<String> attributes = skipIndexAttributes.getAttributesToSkip(indexName.toLowerCase());
                    if (attributes != null) {
                        // there is some attribute to be skipped for this index
                        // split the index description string into tokens of attributes
                        String indexDescription = indexes.get(indexName);
                        String[] tokens = indexDescription.split(",");
                        indexDescription = ""; // we will update the index description without the stripped attributes
                        boolean firstTime = true;
                        for (String token : tokens) {
                            if (!StringUtils.isNullOrEmpty(token)) {

                                boolean attributeFound = false;
                                for (String attribute : attributes) {
                                    if (token.trim()
                                             .toLowerCase()
                                             .startsWith(attribute.toLowerCase() + "=")) {
                                        attributeFound = true;
                                        break;
                                    }
                                }

                                if (!attributeFound) {
                                    if (firstTime) {
                                        firstTime = false;
                                        indexDescription += token.trim();
                                    } else {
                                        indexDescription += ", " + token.trim();
                                    }
                                }
                            }
                        }
                        indexes.put(indexName, indexDescription);
                    }
                }
            }
        }
    }

    private List<String> getCommonTables( String thisSnapshotName, List<TableDescription> thisTables,
                                          String thatSnapshotName, List<TableDescription> thatTables,
                                          Set<String> tablesToSkip ) {

        // list of names of tables present in both snapshots
        List<String> commonTables = new ArrayList<String>();

        // check if all tables from THIS snapshot are present in the THAT one
        for (TableDescription thisTable : thisTables) {

            // do not deal with tables that are to be skipped
            if (!tablesToSkip.contains(thisTable.getName())) {

                boolean tablePresentInBothSnapshots = false;
                for (TableDescription thatTable : thatTables) {
                    if (thatTable.getName().equalsIgnoreCase(thisTable.getName())) {
                        commonTables.add(thisTable.getName());
                        tablePresentInBothSnapshots = true;
                        break;
                    }
                }

                if (!tablePresentInBothSnapshots) {
                    equality.addTablePresentInOneSnapshotOnly(thisSnapshotName, thisTable.getName());
                }
            }
        }

        // check if all tables from THAT snapshot are present in the THIS one
        for (TableDescription thatTable : thatTables) {

            // do not deal with tables that are to be skipped
            if (!tablesToSkip.contains(thatTable.getName())) {

                boolean tablePresentInBothSnapshots = false;
                for (TableDescription thisTable : thisTables) {
                    if (thisTable.getName().equalsIgnoreCase(thatTable.getName())) {
                        tablePresentInBothSnapshots = true;
                        break;
                    }
                }

                if (!tablePresentInBothSnapshots) {
                    equality.addTablePresentInOneSnapshotOnly(thatSnapshotName, thatTable.getName());
                }
            }
        }

        return commonTables;
    }

    /**
     * Return list with all rows of some particular table
     * 
     * @param snapshotName snapshot name
     * @param table the table of question
     * @param skipColumns skip rules
     * @param skipRows rows to skip
     * @param dbProvider DB connection to use
     * @param backupXmlFile backup file to use
     * @return
     */
    List<String> loadTableData( String snapshotName, TableDescription table,
                                Map<String, SkipColumns> skipColumns, Map<String, SkipRows> skipRows,
                                DbProvider dbProvider, Document backupXmlFile ) {

        List<String> valuesList = new ArrayList<String>();
        if (backupXmlFile == null) {
            // load table row data from database

            if (dbProvider == null) {
                // DB provider not specified, use the one from this instance 
                dbProvider = this.dbProvider;
            }

            String sqlQuery = constructSelectStatement(table, skipColumns);
            if (sqlQuery != null) {
                for (DbRecordValuesList rowValues : dbProvider.select(sqlQuery)) {
                    // if there are rows for skipping we will find them and remove them from the list
                    String stringRowValue = rowValues.toString();

                    // escaping special characters that may 
                    // cause some trouble while saving the snapshot into XML file
                    stringRowValue.replace("&", "&amp;");
                    stringRowValue.replace("<", "&lt;");
                    stringRowValue.replace(">", "&gt;");

                    SkipRows skipRow = skipRows.get(table.getName().toLowerCase());
                    if (skipRow == null || !skipRow.skipRow(stringRowValue)) {
                        valuesList.add(stringRowValue);
                    }
                }
                log.debug("[" + snapshotName + "] Loaded " + valuesList.size() + " rows for table "
                          + table.getName());
            } else {
                log.warn("[" + snapshotName + "] No data will be loaded for table " + table.getName()
                         + " because all its columns are pointed to be skipped");
            }
        } else {
            // load table row data from backup file
            Element tableNode = loadTableNode(table, backupXmlFile);

            List<Element> tableRows = DatabaseSnapshotUtils.getChildrenByTagName(tableNode, "row");
            log.debug("[" + snapshotName + " from file] Loaded " + tableRows.size() + " rows for table "
                      + table.getName());
            for (Element tableRow : DatabaseSnapshotUtils.getChildrenByTagName(tableNode, "row")) {
                valuesList.add(tableRow.getTextContent());
            }
        }

        return valuesList;
    }

    /**
     * Return the number of rows of some particular table
     * 
     * @param snapshotName snapshot name
     * @param table the table of question
     * @param dbProvider DB connection to use
     * @param backupXmlFile backup file to use
     * @return
     */
    int loadTableLength( String snapshotName, TableDescription table, DbProvider dbProvider,
                         Document backupXmlFile ) {

        if (backupXmlFile == null) {
            // load table length from database

            if (dbProvider == null) {
                // DB provider not specified, use the one from this instance 
                dbProvider = this.dbProvider;
            }
            DbRecordValuesList[] dbRecords = dbProvider.select("SELECT COUNT(*) FROM " + table.getName());
            return Integer.parseInt(dbRecords[0].get(0).getValueAsString());
        } else {
            // load table length from backup file
            Element tableNode = loadTableNode(table, backupXmlFile);

            if (tableNode != null) {
                String numberRowsString = tableNode.getAttribute(DatabaseSnapshotUtils.ATTR_TABLE_NUMBER_ROWS);
                if (numberRowsString != null && numberRowsString.trim().length() > 0) {
                    // table length is provided as an attribute
                    int numberRows;
                    try {
                        numberRows = Integer.parseInt(numberRowsString);
                    } catch (NumberFormatException nfe) {
                        throw new DatabaseSnapshotException(DatabaseSnapshotUtils.ATTR_TABLE_NUMBER_ROWS
                                                            + " attribute of table " + table.getName()
                                                            + " is not a number: " + numberRowsString);
                    }
                    if (numberRows < 0) {
                        throw new DatabaseSnapshotException(DatabaseSnapshotUtils.ATTR_TABLE_NUMBER_ROWS
                                                            + " attribute of table " + table.getName()
                                                            + " is not a positive number: "
                                                            + numberRowsString);
                    } else {
                        return numberRows;
                    }
                } else {
                    // count the number of rows
                    return loadTableData(snapshotName, table, null, null, dbProvider, backupXmlFile).size();
                }
            } else {
                throw new DatabaseSnapshotException("Table " + table.getName()
                                                    + " not found in backup file ");
            }
        }
    }

    /**
     * Load the XML node for some particular table
     * @param table
     * @param backupXmlFile
     * @return
     */
    private Element loadTableNode( TableDescription table, Document backupXmlFile ) {

        // load table row data from backup file
        List<Element> dbSnapshotNodeList = DatabaseSnapshotUtils.getChildrenByTagName(backupXmlFile,
                                                                                      DatabaseSnapshotUtils.NODE_DB_SNAPSHOT);
        if (dbSnapshotNodeList.size() != 1) {
            throw new DatabaseSnapshotException("Bad dabase snapshot backup file. It must have 1 '"
                                                + DatabaseSnapshotUtils.NODE_DB_SNAPSHOT
                                                + "' node, but it has" + dbSnapshotNodeList.size());
        }

        for (Element tableNode : DatabaseSnapshotUtils.getChildrenByTagName(dbSnapshotNodeList.get(0),
                                                                            "TABLE")) {
            String tableName = tableNode.getAttribute("name");
            if (table.getName().equalsIgnoreCase(tableName)) {
                return tableNode;
            }
        }

        return null;
    }

    private String constructSelectStatement( TableDescription table, Map<String, SkipColumns> skipColumns ) {

        Set<String> columns = table.getColumnNames();

        // Search for skip columns for this table. The search must ignore the letters case
        SkipColumns skipColumnsForThisTable = null;
        for (String tableName : skipColumns.keySet()) {
            if (tableName.equalsIgnoreCase(table.getName())) {
                skipColumnsForThisTable = skipColumns.get(tableName);
                break;
            }
        }

        if (skipColumnsForThisTable == null) {
            String query = "SELECT * FROM ";
            if (this.dbProvider instanceof PostgreSqlDbProvider) {
                List<String> sortedColumns = new ArrayList<>(columns);
                Collections.sort(sortedColumns);
                query = "SELECT " + String.join(", ", sortedColumns) + " FROM ";
            }
            // all columns are important
            if (table.getSchema() != null) {
                query += table.getSchema() + ".";
            }
            return query + table.getName();
        } else {
            // some columns must be skipped
            StringBuilder sql = new StringBuilder("SELECT");

            // add all important columns
            for (String column : columns) {
                if (!skipColumnsForThisTable.isSkipColumn(column)) {
                    sql.append(" " + column + ",");
                }
            }

            if ("SELECT".equals(sql.toString())) {
                // user has specified to skip all table columns one by one,
                // so no column are left for selection, so we won't select anything
                return null;
            } else {
                // remove last comma
                sql.setLength(sql.length() - 1);
                sql.append(" FROM ");

                if (table.getSchema() != null) {
                    sql.append(table.getSchema()).append(".");
                }
                sql.append(table.getName());
                return sql.toString();
            }
        }
    }

    private void disconnect( DbProvider dbProvider, String when ) {

        // the DB provider is null if the snapshot is loaded from file, then do compare, then come in here
        if (dbProvider != null) {
            // close the database connections
            try {
                dbProvider.disconnect();
            } catch (Exception e) {
                log.warn("Error diconnecting " + dbProvider.getDbConnection().getDescription() + " " + when,
                         e);
            }
        }
    }
}
