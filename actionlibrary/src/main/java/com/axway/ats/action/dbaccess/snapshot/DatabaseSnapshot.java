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
package com.axway.ats.action.dbaccess.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotException;
import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotUtils;
import com.axway.ats.common.dbaccess.snapshot.IndexNameMatcher;
import com.axway.ats.common.dbaccess.snapshot.TableDescription;
import com.axway.ats.common.dbaccess.snapshot.equality.EqualityState;
import com.axway.ats.core.dbaccess.DatabaseProviderFactory;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.harness.config.TestBox;

/**
 * Main class for comparing databases. You can specify which tables to compare
 * or not as well as which columns to skip on some tables.
 */
public class DatabaseSnapshot {

    private static Logger       log               = Logger.getLogger( DatabaseSnapshot.class );

    // the snapshot name
    String                      name;

    // the time all meta data is taken
    long                        metadataTimestamp = -1;
    // the time all table contents is taken
    long                        contentTimestamp  = -1;

    Document                    backupXmlFile;

    // DB connection parameters
    private TestBox             testBox;
    private Map<String, Object> customProperties;

    // description of all tables of interest
    List<TableDescription>      tables            = new ArrayList<>();

    // this structure can say how equal the snapshots are
    private EqualityState       equality;

    // our DB connector
    private DbProvider          dbProvider;

    // rules used to specify what to ignore when comparing
    // MAP< table name > < what to skip >
    Map<String, SkipRules>      skipRulesPerTable = new HashMap<String, SkipRules>();

    Set<String>      skipTableContent = new HashSet<String>();

    Map<String, Map<String, String>> skipRows          = new HashMap<>();
    
    //  An interface which tells whether some table index names should be treated as same or not
    private IndexNameMatcher    indexNameMatcher;

    /**
     * Constructor providing snapshot name and connection parameters
     * 
     * @param name Snapshot name. Used as identifier for comparison results
     * @param testBox the DB connection parameters
     */
    @PublicAtsApi
    public DatabaseSnapshot( String name, TestBox testBox ) {

        this( name, testBox, null );
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

        if( StringUtils.isNullOrEmpty( name ) ) {
            throw new DatabaseSnapshotException( "Invalid snapshot name '" + name + "'" );
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

        for( String table : tables ) {
            skipRulesPerTable.put( table, new SkipRules() );
        }
    }
    
    /**
     * Specify a column which values will not be read when comparing the table content.
     * </br>Note: the column meta information(like column type and indexes it participates into) is still compared 
     * 
     * @param tableName table name
     * @param columnName column
     */
    @PublicAtsApi
    public void skipTableColumn( String tableName, String columnName ) {

        skipRulesPerTable.put( tableName, new SkipRules( columnName ) );
    }

    /**
     * Specify columns which values will not be read when comparing the table content.
     * </br>Note: the column meta information(like column type and indexes it participates into) is still compared 
     * 
     * @param tableName table name
     * @param columnNames columns
     */
    @PublicAtsApi
    public void skipTableColumns( String tableName, String... columnNames ) {

        skipRulesPerTable.put( tableName, new SkipRules( columnNames ) );
    }
    
    @PublicAtsApi
    public void skipTableContent(
                                   String... tables ) {

        for( String table : tables ) {
            skipTableContent.add( table );
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

        Map<String, String> skipRowsForThisTable = skipRows.get( table );
        if( skipRowsForThisTable == null ) {
            skipRowsForThisTable = new HashMap<>();
            skipRows.put( table, skipRowsForThisTable );
        }
        skipRowsForThisTable.put( column, value );
    }

    /**
     * Provide instance of this interface which will define 
     * whether some table index names should be treated as same or not.</br></br>
     * 
     * <b>Note:</b> If not used, the index names are compared as regular text.
     * 
     * @param indexNameMatcher the custom implementation.
     */
    @PublicAtsApi
    public void setIndexNameMatcher( IndexNameMatcher indexNameMatcher ) {

        this.indexNameMatcher = indexNameMatcher;
    }

    /**
     * Provide a java regular expression which will define 
     * whether some table index names should be treated as same or not.</br></br>
     * <b>Note:</b> The regular expression is applied on the index names,
     * the first matched subsequences of both index names are compared for equality.</br>
     * 
     * In other words, we compare whatever is returned by the {@link Matcher#find()} method 
     * when applied on both index names.</br></br>
     * 
     * 
     * <b>Note:</b> If not used, the index names are compared as regular text.
     * 
     * @param indexNameRegex a java regular expression
     */
    @PublicAtsApi
    public void setIndexNameMatcher( final String indexNameRegex ) {

        this.indexNameMatcher = new IndexNameMatcher() {
            @Override
            public boolean isSame( String table, String firstName, String secondName ) {

                Pattern pattern = Pattern.compile( indexNameRegex );

                Matcher matcher1 = pattern.matcher( firstName );
                if( matcher1.find() ) {
                    firstName = firstName.substring( matcher1.start(), matcher1.end() );
                }

                Matcher matcher2 = pattern.matcher( secondName );
                if( matcher2.find() ) {
                    secondName = secondName.substring( matcher2.start(), matcher2.end() );
                }

                return firstName.equals( secondName );
            }
        };
    }

    /**
     * Take a database snapshot</br>
     * <b>NOTE:</b> We will get only meta data about the tables in the database. 
     * No table content is loaded at this moment as this may cause memory issues.
     * The content of each table is loaded when needed while comparing this snapshot with another one, or while
     * saving the snapshot into a file.
     */
    @PublicAtsApi
    public void takeSnapshot() {

        this.metadataTimestamp = System.currentTimeMillis();

        dbProvider = DatabaseProviderFactory.getDatabaseProvider( testBox.getDbType(), testBox.getHost(),
                                                                  testBox.getDbName(), testBox.getDbUser(),
                                                                  testBox.getDbPass(), testBox.getDbPort(),
                                                                  customProperties );

        log.info( "Start taking database meta information for snapshot [" + name + "] from "
                  + dbProvider.getDbConnection().getDescription() );

        tables = dbProvider.getTableDescriptions();
        if( tables.size() == 1 ) {
            log.warn( "No tables found for snapshot [" + name + "]" );
        }

        for( Entry<String, SkipRules> tableToSkip : skipRulesPerTable.entrySet() ) {
            if( tableToSkip.getValue().isSkipWholeTable() ) {
                for( TableDescription table : tables ) {

                    if( table.getName().equalsIgnoreCase( tableToSkip.getKey() ) ) {
                        tables.remove( table );
                        break;
                    }
                }
            }
        }

        for( TableDescription table : tables ) {
            table.setSnapshotName( this.name );
        }

        log.info( "End taking database meta information for snapshot with name " + name );
    }

    /**
     * Compare both snapshots
     * Both snapshots will be loaded from the database and compared table by table.
     * But if they were saved in files, they will be loaded from the files and compared table by table
     * 
     * @param that the snapshot to compare to
     * @throws DatabaseSnapshotException
     */
    @PublicAtsApi
    public void compare( DatabaseSnapshot that ) throws DatabaseSnapshotException {

        try {
            if( that == null ) {
                throw new DatabaseSnapshotException( "Snapshot to compare is null" );
            }
            if( this.name.equals( that.name ) ) {
                throw new DatabaseSnapshotException( "You are trying to compare snapshots with same name: "
                                                     + this.name );
            }
            if( this.metadataTimestamp == -1 ) {
                throw new DatabaseSnapshotException( "You are trying to compare snapshots but [" + this.name
                                                     + "] snapshot is still not created" );
            }
            if( that.metadataTimestamp == -1 ) {
                throw new DatabaseSnapshotException( "You are trying to compare snapshots but [" + that.name
                                                     + "] snapshot is still not created" );
            }

            log.debug( "Comparing snapshots [" + this.name + "] taken on "
                       + DatabaseSnapshotUtils.dateToString( this.metadataTimestamp ) + " and [" + that.name
                       + "] taken on " + DatabaseSnapshotUtils.dateToString( that.metadataTimestamp ) );

            this.equality = new EqualityState( this.name, that.name );

            // make copies of the table info, as we will remove from these lists,
            // but we do not want to remove from the original table info lists
            List<TableDescription> thisTables = new ArrayList<TableDescription>( this.tables );
            List<TableDescription> thatTables = new ArrayList<TableDescription>( that.tables );

            // Here we put all skip rules into one list. It doesn't make sense to ask people 
            // to add same skip rules for same tables in both snapshot instances.
            Map<String, SkipRules> allSkipRules = mergeSkipRules( that.skipRulesPerTable );
            
            Set<String> allTablesContentSkip = mergeTableContentToSkip(that.skipTableContent);
            
            Map<String, Map<String, String>> allSkipRows = mergeSkipRows( that.skipRows );

            Set<String> allTablesToSkip = getAllTablesToSkip( allSkipRules );

            // We can use just one index name matcher
            IndexNameMatcher actualIndexNameMatcher = mergeIndexNameMatchers( that.indexNameMatcher );

            compareTables( this.name, thisTables, that.name, thatTables, that.dbProvider, allTablesToSkip,
                           allSkipRules, allTablesContentSkip, allSkipRows, actualIndexNameMatcher, that.backupXmlFile, equality );

            if( equality.hasDifferences() ) {
                // there are some unexpected differences
                throw new DatabaseSnapshotException( equality );
            } else {
                log.info( "Successful verification" );
            }
        } finally {
            // close the database connections
            disconnect( this.dbProvider, "after comparing database snapshots" );
            if( that != null ) {
                disconnect( that.dbProvider, "after comparing database snapshots" );
            }
        }
    }

    /**
     * Save a snapshot into a file.</br>
     * <b>NOTE:</b> This is the moment when the contents of each table is read from the database 
     * one at a time and is saved into the file.
     * 
     * @param backupFile the backup file name
     */
    @PublicAtsApi
    public void saveToFile( String backupFile ) {

        backupXmlFile = new DatabaseSnapshotBackupUtils().saveToFile( this, backupFile );

        // close the database connection
        disconnect( this.dbProvider, "after saving database snapshot into " + backupFile );
    }

    /**
     * Load a snapshot from a file
     * 
     * @param newSnapshotName the name of the new snapshot
     * </br>Pass null or empty string if want to use the snapshot name as saved in the file,
     * or provide a new name here
     * @param sourceFile the backup file name
     */
    @PublicAtsApi
    public void loadFromFile( String newSnapshotName, String sourceFile ) {

        backupXmlFile = new DatabaseSnapshotBackupUtils().loadFromFile( newSnapshotName, this, sourceFile );
    }

    /**
     * Compares all tables between two snapshots
     * 
     * @param thisSnapshotName
     * @param thisTables
     * @param thatSnapshotName
     * @param thatTables
     * @param thatDbProvider
     * @param allTablesToSkip
     * @param allSkipRules
     * @param indexNameMatcher
     * @param thatBackupXmlFile
     * @param equality
     */
    private void compareTables( String thisSnapshotName, List<TableDescription> thisTables,
                                String thatSnapshotName, List<TableDescription> thatTables,
                                DbProvider thatDbProvider, Set<String> allTablesToSkip,
                                Map<String, SkipRules> allSkipRules, Set<String> allTablesContentSkip,
								Map<String, Map<String, String>> allRowsToSkip,
                                IndexNameMatcher indexNameMatcher, Document thatBackupXmlFile, 
                                EqualityState equality ) {

        // make a list of tables present in both snapshots
        List<String> commonTables = getCommonTables( thisSnapshotName, thisTables, thatSnapshotName,
                                                     thatTables, allTablesToSkip );

        for( String tableName : commonTables ) {
            // get tables to compare
            TableDescription thisTable = null;
            TableDescription thatTable = null;
            for( TableDescription table : thisTables ) {
                if( table.getName().equalsIgnoreCase( tableName ) ) {
                    thisTable = table;
                    break;
                }
            }
            for( TableDescription table : thatTables ) {
                if( table.getName().equalsIgnoreCase( tableName ) ) {
                    thatTable = table;
                    break;
                }
            }

            SkipRules tableSkipRules = allSkipRules.get( thisTable.getName() );
            if( tableSkipRules != null && tableSkipRules.isSkipWholeTable() ) {
                // if table is not of interest - skip it
                continue;
            }

            List<String> thisValuesList = null;
            List<String> thatValuesList = null;
            
            if( !allTablesContentSkip.contains( tableName ) ) {
                // load the content of this snapshot's table
                thisValuesList = loadTableData( thisSnapshotName, thisTable, allSkipRules,
                                                             allRowsToSkip, this.dbProvider, backupXmlFile );
                // load the content of that snapshot's table
                thatValuesList = loadTableData( thatSnapshotName, thatTable, allSkipRules, 
                                                             allRowsToSkip, thatDbProvider, thatBackupXmlFile );
            }

            thisTable.compare( thatTable, thisValuesList, thatValuesList, indexNameMatcher, equality );
        }

        thisTables.clear();
    }

    /**
     * The Index Name Matcher can come from first or second snapshot instance.
     * Or maybe there is none provided by the user.
     * 
     * @param thatIndexNameMatcher
     * @return
     */
    private IndexNameMatcher mergeIndexNameMatchers( IndexNameMatcher thatIndexNameMatcher ) {

        if( this.indexNameMatcher != null ) {
            // use THIS matcher
            if( thatIndexNameMatcher != null ) {
                log.warn( "You have provided Index Name Matchers for both snapshots. We selected to use the one from snapshot ["
                          + this.name + "]" );
            }
            return this.indexNameMatcher;
        } else if( thatIndexNameMatcher != null ) {
            // use THAT matcher
            return thatIndexNameMatcher;
        } else {
            // index name matcher is not provided for any snapshot
            // create a default one which matches index name literally
            return new IndexNameMatcher() {
                @Override
                public boolean isSame( String table, String firstName, String secondName ) {

                    return firstName.equals( secondName );
                }
            };
        }
    }

    /**
     * Here we put skip rules into from both snapshots into a single list.
     * It doesn't make sense to ask people to add same skip rules for same tables in both snapshot instances.
     * 
     * @param thatSkipRulesPerTable
     * @return
     */
    private Map<String, SkipRules> mergeSkipRules( Map<String, SkipRules> thatSkipRulesPerTable ) {

        Map<String, SkipRules> allSkipRules = new HashMap<>();
        allSkipRules.putAll( this.skipRulesPerTable );

        for( Entry<String, SkipRules> table : thatSkipRulesPerTable.entrySet() ) {
            if( allSkipRules.containsKey( table.getKey() ) ) {
                SkipRules thisRule = this.skipRulesPerTable.get( table.getKey() );
                SkipRules thatRule = table.getValue();
                if( thisRule.isSkipWholeTable() ) {
                    // nothing to do, the table will be skipped anyway
                } else if( thatRule.isSkipWholeTable() ) {
                    // replace the current rule, so skip the table
                    allSkipRules.put( table.getKey(), thatRule );
                } else {
                    // just columns will be skipped, merge the list of columns
                    for( String column : thatRule.getColumnsToSkip() ) {
                        thisRule.addColumnToSkip( column );
                    }
                }
            } else {
                // no current rule for this table, now we add one
                allSkipRules.put( table.getKey(), table.getValue() );
            }
        }

        return allSkipRules;
    }
    
    private Set<String> mergeTableContentToSkip(
                                                  Set<String> thatSkipTableContent ) {

        Set<String> allTablesContentToSkip = new HashSet<String>();
        allTablesContentToSkip.addAll( this.skipTableContent );
        allTablesContentToSkip.addAll( thatSkipTableContent );
        
        return allTablesContentToSkip;
    }

    private Map<String, Map<String, String>> mergeSkipRows(
                                                            Map<String, Map<String, String>> thatSkipRowsPerTable ) {

        Map<String, Map<String, String>> allSkipRows = new HashMap<>();
        allSkipRows.putAll( this.skipRows );

        for( Entry<String, Map<String, String>> table : thatSkipRowsPerTable.entrySet() ) {
            if( allSkipRows.containsKey( table.getKey() ) ) {
                Map<String, String> thisRow = allSkipRows.get( table.getKey() );
                for( Entry<String, String> column : thisRow.entrySet() ) {
                    if( !table.getValue().containsKey( column.getKey() ) ) {
                        thisRow.put( column.getKey(), table.getValue().get( column.getKey() ) );
                        allSkipRows.put( table.getKey(), thisRow );
                    }
                }
            } else {
                // no current rule for this table, now we add one
                allSkipRows.put( table.getKey(), table.getValue() );
            }
        }

        return allSkipRows;
    }
    
    /**
     * Retrieve all tables that are to be skipped
     * 
     * @param allSkipRules
     * @return
     */
    private Set<String> getAllTablesToSkip( Map<String, SkipRules> allSkipRules ) {

        Set<String> tablesToSkip = new HashSet<>();

        for( String table : allSkipRules.keySet() ) {
            SkipRules rule = allSkipRules.get( table );
            if( rule.isSkipWholeTable() ) {
                tablesToSkip.add( table );
            }
        }

        return tablesToSkip;
    }

    private List<String> getCommonTables( String thisSnapshotName, List<TableDescription> thisTables,
                                          String thatSnapshotName, List<TableDescription> thatTables,
                                          Set<String> tablesToSkip ) {

        // list of names of tables present in both snapshots
        List<String> commonTables = new ArrayList<String>();

        // check if all tables from THIS snapshot are present in the THAT one
        for( TableDescription thisTable : thisTables ) {

            // do not deal with tables that are to be skipped
            if( !tablesToSkip.contains( thisTable.getName() ) ) {

                boolean tablePresentInBothSnapshots = false;
                for( TableDescription thatTable : thatTables ) {
                    if( thatTable.getName().equalsIgnoreCase( thisTable.getName() ) ) {
                        commonTables.add( thisTable.getName() );
                        tablePresentInBothSnapshots = true;
                        break;
                    }
                }

                if( !tablePresentInBothSnapshots ) {
                    equality.addTablePresentInOneSnapshotOnly( thisSnapshotName, thisTable.getName() );
                }
            }
        }

        // check if all tables from THAT snapshot are present in the THIS one
        for( TableDescription thatTable : thatTables ) {

            // do not deal with tables that are to be skipped
            if( !tablesToSkip.contains( thatTable.getName() ) ) {

                boolean tablePresentInBothSnapshots = false;
                for( TableDescription thisTable : thisTables ) {
                    if( thisTable.getName().equalsIgnoreCase( thatTable.getName() ) ) {
                        tablePresentInBothSnapshots = true;
                        break;
                    }
                }

                if( !tablePresentInBothSnapshots ) {
                    equality.addTablePresentInOneSnapshotOnly( thatSnapshotName, thatTable.getName() );
                }
            }
        }

        return commonTables;
    }

    List<String> loadTableData(
                                String snapshotName, TableDescription table,
                                Map<String, SkipRules> allSkipRules,
                                Map<String, Map<String, String>> allSkipRows,
                                DbProvider dbProvider, Document backupXmlFile ) {

        if( dbProvider == null ) {
            // DB provider not specified, use the one from this instance 
            dbProvider = this.dbProvider;
        }

        List<String> valuesList = new ArrayList<String>();
        if( backupXmlFile == null ) {
            // load table row data from database
            String sqlQuery = construcSelectStatement( table, allSkipRules );
            if( sqlQuery != null ) {
                for( DbRecordValuesList rowValues : dbProvider.select( sqlQuery ) ) {
                    // if there are rows for skipping we will find them and remove them from the list
                    String stringRowValue = rowValues.toString();
                    
                    if( allSkipRows != null && !skipRow( allSkipRows.get( table.getName() ),
                                                        stringRowValue ) ) {
                        valuesList.add( stringRowValue );

                    }
                }
                log.debug( "[" + snapshotName + " from database] Loaded " + valuesList.size()
                           + " rows for table " + table.getName() );
            } else {
                log.warn( "Skip table " + table.getName() + " because of excluding all columns." );
            }
            
        } else {
            // load table row data from backup file
            List<Element> dbSnapshotNodeList = DatabaseSnapshotUtils.getChildrenByTagName( backupXmlFile,
                                                                                           DatabaseSnapshotUtils.NODE_DB_SNAPSHOT );
            if( dbSnapshotNodeList.size() != 1 ) {
                throw new DatabaseSnapshotException( "Bad dabase snapshot backup file. It must have 1 '"
                                                     + DatabaseSnapshotUtils.NODE_DB_SNAPSHOT
                                                     + "' node, but it has" + dbSnapshotNodeList.size() );
            }

            for( Element tableNode : DatabaseSnapshotUtils.getChildrenByTagName( dbSnapshotNodeList.get( 0 ),
                                                                                 "TABLE" ) ) {
                String tableName = tableNode.getAttribute( "name" );
                if( table.getName().equalsIgnoreCase( tableName ) ) {

                    List<Element> tableRows = DatabaseSnapshotUtils.getChildrenByTagName( tableNode, "row" );
                    log.debug( "[" + snapshotName + " from file] Loaded " + tableRows.size()
                               + " rows for table " + tableName );
                    for( Element tableRow : DatabaseSnapshotUtils.getChildrenByTagName( tableNode, "row" ) ) {
                        valuesList.add( tableRow.getTextContent() );
                    }
                    break;
                }
            }
        }

        return valuesList;
    }

    private String construcSelectStatement( TableDescription table, Map<String, SkipRules> allSkipRules ) {

        Set<String> allColumns = table.getColumnNames();

        // Search for skip rule for this table. The search must ignore the letters case
        String tableToSkip = null;
        for( String tableName : allSkipRules.keySet() ) {
            if( tableName.equalsIgnoreCase( table.getName() ) ) {
                tableToSkip = tableName;
                break;
            }
        }

        if( tableToSkip == null ) {
            String query = "SELECT * FROM ";
            // no skip rules about this table, all columns are important
            if(table.getSchema() != null){
                query += table.getSchema() + ".";
            }
            return  query + table.getName();
        }

        StringBuilder sql = new StringBuilder( "SELECT" );

        Set<String> columnsToSkip = allSkipRules.get( tableToSkip ).getColumnsToSkip();
        for( String column : allColumns ) {
            // Search for column to skip. The search must ignore the letters case
            boolean skipThisColumn = false;
            for( String columnToSkip : columnsToSkip ) {
                if( columnToSkip.equalsIgnoreCase( column ) ) {
                    skipThisColumn = true;
                    break;
                }
            }

            if( !skipThisColumn ) {
                sql.append( " " + column + "," );
            }
        }
        // no column left for selection, we will skip the table
        if("SELECT".equals( sql.toString() )){
            return null;
        }
        // remove last comma
        sql.setLength( sql.length() - 1 );
        sql.append( " FROM " );
        
        if( table.getSchema() != null ) {
            sql.append( table.getSchema() ).append( "." );
        }
        sql.append( table.getName() );
        return sql.toString();
    }
    
    private boolean skipRow(
                             Map<String, String> skipTableRows,
                             String rowValues ) {

        if( skipTableRows != null ) {
            for( Entry<String, String> skipRowValue : skipTableRows.entrySet() ) {
                if( rowValues.contains( skipRowValue.getKey() + "=" + skipRowValue.getValue() ) ) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void disconnect( DbProvider dbProvider, String when ) {

        // the DB provider is null if the snapshot is loaded from file, then do compare, then come in here
        if( dbProvider != null ) {
            // close the database connections
            try {
                dbProvider.disconnect();
            } catch( Exception e ) {
                log.warn( "Error diconnecting " + dbProvider.getDbConnection().getDescription() + " " + when,
                          e );
            }
        }
    }
}
