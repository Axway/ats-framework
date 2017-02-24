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
package com.axway.ats.common.dbaccess.snapshot;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.dbaccess.snapshot.equality.EqualityState;

/**
 * Error while working with Database Snapshot
 */
@PublicAtsApi
public class DatabaseSnapshotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private EqualityState     equality;

    public DatabaseSnapshotException( String arg0 ) {

        super( arg0 );
    }

    public DatabaseSnapshotException( Throwable arg0 ) {

        super( arg0 );
    }

    public DatabaseSnapshotException( String arg0, Throwable arg1 ) {

        super( arg0, arg1 );
    }

    public DatabaseSnapshotException( EqualityState equality ) {

        this.equality = equality;
    }

    /**
     * This can be used to retrieve the compare result and then make
     * some custom compare report.
     * 
     * @return the result of compare
     */
    @PublicAtsApi
    public EqualityState getEqualityState() {

        return this.equality;
    }

    @Override
    @PublicAtsApi
    public String getMessage() {

        if( equality == null ) {
            // got a generic exception, not directly concerning the comparision
            return super.getMessage();
        } else {
            String thisSnapshot = equality.getFirstSnapshotName();
            String thatSnapshot = equality.getSecondSnapshotName();

            StringBuilder msg = new StringBuilder();
            msg.append( "Comparing [" );
            msg.append( thisSnapshot );
            msg.append( "] and [" );
            msg.append( thatSnapshot );
            msg.append( "] produced the following unexpected differences:" );

            addInfoAboutTablesInOneSnapshotOnly( msg, equality, thisSnapshot );
            addInfoAboutTablesInOneSnapshotOnly( msg, equality, thatSnapshot );

            addInfoAboutDifferentPrimaryKeys( msg, equality );

            addInfoAboutDifferentNumberOfRows( msg, equality );

            addInfoAboutColumnsInOneSnapshotOnly( msg, equality, thisSnapshot, thatSnapshot );

            addInfoAboutIndexesInOneSnapshotOnly( msg, equality, thisSnapshot, thatSnapshot );

            addInfoAboutRowsInOneSnapshotOnly( msg, equality, thisSnapshot, thatSnapshot );

            msg.append( "\n" );
            return msg.toString();
        }
    }

    private void addInfoAboutTablesInOneSnapshotOnly( StringBuilder msg, EqualityState equality,
                                                      String snapshot ) {

        StringBuilder tablesInThisSnapshotOnly = new StringBuilder();
        List<String> tables = equality.getTablesPresentInOneSnapshotOnly( snapshot );
        if( tables != null ) {
            for( String table : tables ) {
                tablesInThisSnapshotOnly.append( "\n\t" );
                tablesInThisSnapshotOnly.append( table );
            }
        }

        if( tablesInThisSnapshotOnly.length() > 0 ) {
            msg.append( "\nTables present in [" + snapshot + "] only:" );
            msg.append( tablesInThisSnapshotOnly );
        }
    }

    private void addInfoAboutDifferentPrimaryKeys( StringBuilder msg, EqualityState equality ) {

        StringBuilder differentPrimaryKeys = new StringBuilder();
        List<String> primaryKeys = equality.getDifferentPrimaryKeys();
        for( String key : primaryKeys ) {
            differentPrimaryKeys.append( "\n\t" );
            differentPrimaryKeys.append( key );
        }

        if( differentPrimaryKeys.length() > 0 ) {
            msg.append( "\nDifferent primary keys:" );
            msg.append( differentPrimaryKeys );
        }
    }

    private void addInfoAboutDifferentNumberOfRows( StringBuilder msg, EqualityState equality ) {

        StringBuilder differentNumberOfRows = new StringBuilder();
        List<String> numberOfRows = equality.getDifferentNumberOfRows();
        for( String number : numberOfRows ) {
            differentNumberOfRows.append( "\n\t" );
            differentNumberOfRows.append( number );
        }

        if( differentNumberOfRows.length() > 0 ) {
            msg.append( "\nDifferent number of rows" + ":" );
            msg.append( differentNumberOfRows );
        }
    }

    private void addInfoAboutColumnsInOneSnapshotOnly( StringBuilder msg, EqualityState equality,
                                                       String thisSnapshot, String thatSnapshot ) {

        Map<String, List<String>> thisColumnsPerTable = equality.getColumnsPresentInOneSnapshotOnly( thisSnapshot );
        Map<String, List<String>> thatColumnsPerTable = equality.getColumnsPresentInOneSnapshotOnly( thatSnapshot );

        /*
         * Here both lists we work with are coming from same tables, but from different snapshots
         */
        if( thisColumnsPerTable == null ) {
            return;
        }

        Set<Entry<String, List<String>>> tableNamesSet = thisColumnsPerTable.entrySet();

        for( Entry<String, List<String>> thisTableNameEntry : tableNamesSet ) {
            // add different columns for one table at a time
            if( tableNamesSet.size() > 0 ) {
                msg.append( "\nTable columns for '" + thisTableNameEntry.getKey() + "' table:" );
            }

            List<String> thisColumns = thisTableNameEntry.getValue();
            if( thisColumns != null && thisColumns.size() > 0 ) {
                msg.append( "\n\t[" + thisSnapshot + "]:" );
                for( String column : thisColumns ) {
                    msg.append( "\n\t\t" + column );
                }
            }

            List<String> thatColumns = thatColumnsPerTable.get( thisTableNameEntry.getKey() );
            if( thatColumns != null && thatColumns.size() > 0 ) {
                msg.append( "\n\t[" + thatSnapshot + "]:" );
                for( String column : thatColumns ) {
                    msg.append( "\n\t\t" + column );
                }
            }
        }
    }

    private void addInfoAboutIndexesInOneSnapshotOnly( StringBuilder msg, EqualityState equality,
                                                       String thisSnapshot, String thatSnapshot ) {

        Map<String, List<String>> thisIndexesPerTable = equality.getIndexesPresentInOneSnapshotOnly( thisSnapshot );
        Map<String, List<String>> thatIndexesPerTable = equality.getIndexesPresentInOneSnapshotOnly( thatSnapshot );

        /*
         * Here both lists we work with are coming from same tables, but from different snapshots
         */
        if( thisIndexesPerTable == null ) {
            return;
        }

        Set<Entry<String, List<String>>> tableNames = thisIndexesPerTable.entrySet();

        for( Entry<String, List<String>> thisTableNameEntry : tableNames ) {
            // add different indexes for one table at a time
            if( tableNames.size() > 0 ) {
                msg.append( "\nTable indexes for '" + thisTableNameEntry.getKey() + "' table:" );
            }

            List<String> thisIndexes = thisTableNameEntry.getValue();
            if( thisIndexes != null && thisIndexes.size() > 0 ) {
                msg.append( "\n\t[" + thisSnapshot + "]:" );
                for( String index : thisIndexes ) {
                    msg.append( "\n\t\t" + index );
                }
            }

            List<String> thatIndexes = thatIndexesPerTable.get( thisTableNameEntry.getKey() );
            if( thatIndexes != null && thatIndexes.size() > 0 ) {
                msg.append( "\n\t[" + thatSnapshot + "]:" );
                for( String index : thatIndexes ) {
                    msg.append( "\n\t\t" + index );
                }
            }
        }
    }

    private void addInfoAboutRowsInOneSnapshotOnly( StringBuilder msg, EqualityState equality,
                                                    String thisSnapshot, String thatSnapshot ) {

        Map<String, List<String>> thisRowsPerTable = equality.getRowsPresentInOneSnapshotOnly( thisSnapshot );
        Map<String, List<String>> thatRowsPerTable = equality.getRowsPresentInOneSnapshotOnly( thatSnapshot );

        /*
         * Here both lists we work with are coming from same tables, but from different snapshots
         */
        if( thisRowsPerTable == null ) {
            return;
        }

        Set<Entry<String, List<String>>> tableNames = thisRowsPerTable.entrySet();

        for( Entry<String, List<String>> thisTableNameEntry : tableNames ) {
            // add different rows for one table at a time
            if( tableNames.size() > 0 ) {
                msg.append( "\nTable rows for '" + thisTableNameEntry.getKey() + "' table:" );
            }

            List<String> thisRows = thisTableNameEntry.getValue();
            if( thisRows != null && thisRows.size() > 0 ) {
                msg.append( "\n\t[" + thisSnapshot + "]:" );
                for( String row : thisRows ) {
                    msg.append( "\n\t\t" + row );
                }
            }

            List<String> thatRows = thatRowsPerTable.get( thisTableNameEntry.getKey() );
            if( thatRows != null && thatRows.size() > 0 ) {
                msg.append( "\n\t[" + thatSnapshot + "]:" );
                for( String row : thatRows ) {
                    msg.append( "\n\t\t" + row );
                }
            }
        }
    }
}
