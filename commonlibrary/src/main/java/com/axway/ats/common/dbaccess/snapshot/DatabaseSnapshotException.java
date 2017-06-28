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

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.dbaccess.snapshot.equality.DatabaseEqualityState;

/**
 * Error while working with Database Snapshot
 */
@PublicAtsApi
public class DatabaseSnapshotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private DatabaseEqualityState     equality;

    public DatabaseSnapshotException( String arg0 ) {

        super( arg0 );
    }

    public DatabaseSnapshotException( Throwable arg0 ) {

        super( arg0 );
    }

    public DatabaseSnapshotException( String arg0, Throwable arg1 ) {

        super( arg0, arg1 );
    }

    public DatabaseSnapshotException( DatabaseEqualityState equality ) {

        this.equality = equality;
    }

    /**
     * This can be used to retrieve the compare result and then make
     * some custom compare report.
     * 
     * @return the result of compare
     */
    @PublicAtsApi
    public DatabaseEqualityState getEqualityState() {

        return this.equality;
    }

    @Override
    @PublicAtsApi
    public String getMessage() {

        if( equality == null ) {
            // got a generic exception, not directly concerning the comparision
            return super.getMessage();
        } else {
            String firstSnapshot = equality.getFirstSnapshotName();
            String secondSnapshot = equality.getSecondSnapshotName();

            StringBuilder msg = new StringBuilder();
            msg.append( "Comparing [" );
            msg.append( firstSnapshot );
            msg.append( "] and [" );
            msg.append( secondSnapshot );
            msg.append( "] produced the following unexpected differences:" );

            addInfoAboutTablesInOneSnapshotOnly( msg, equality, firstSnapshot );
            addInfoAboutTablesInOneSnapshotOnly( msg, equality, secondSnapshot );

            addInfoAboutDifferentPrimaryKeys( msg, equality, firstSnapshot, secondSnapshot );

            addInfoAboutDifferentNumberOfRows( msg, equality, firstSnapshot, secondSnapshot );

            addInfoAboutColumnsInOneSnapshotOnly( msg, equality, firstSnapshot, secondSnapshot );

            addInfoAboutIndexesInOneSnapshotOnly( msg, equality, firstSnapshot, secondSnapshot );

            addInfoAboutRowsInOneSnapshotOnly( msg, equality, firstSnapshot, secondSnapshot );

            msg.append( "\n" );
            return msg.toString();
        }
    }

    private void addInfoAboutTablesInOneSnapshotOnly( StringBuilder msg, DatabaseEqualityState equality,
                                                      String snapshot ) {

        StringBuilder tablesInFirstSnapshotOnly = new StringBuilder();
        for( String table : equality.getTablesPresentInOneSnapshotOnly( snapshot ) ) {
            tablesInFirstSnapshotOnly.append( "\n\t" );
            tablesInFirstSnapshotOnly.append( table );
        }

        if( tablesInFirstSnapshotOnly.length() > 0 ) {
            msg.append( "\nTables present in [" + snapshot + "] only:" );
            msg.append( tablesInFirstSnapshotOnly );
        }
    }

    private void addInfoAboutDifferentPrimaryKeys( StringBuilder msg, DatabaseEqualityState equality,
                                                   String firstSnapshot, String secondSnapshot ) {

        List<String> tables = equality.getTablesWithDifferentPrimaryKeys( firstSnapshot );
        if( tables.size() > 0 ) {
            msg.append( "\nDifferent primary keys:" );

            for( String table : tables ) {
                msg.append( "\n\ttable '" + table + "', primary key column in [" + firstSnapshot + "] is '"
                            + equality.getDifferentPrimaryKeys( firstSnapshot, table ) + "', while in ["
                            + secondSnapshot + "] is '"
                            + equality.getDifferentPrimaryKeys( secondSnapshot, table ) + "'" );
            }
        }
    }

    private void addInfoAboutDifferentNumberOfRows( StringBuilder msg, DatabaseEqualityState equality,
                                                    String firstSnapshot, String secondSnapshot ) {

        List<String> tables = equality.getTablesWithDifferentNumberOfRows( firstSnapshot );
        if( tables.size() > 0 ) {
            msg.append( "\nDifferent number of rows" + ":" );

            for( String table : tables ) {
                msg.append( "\n\ttable '" + table + "', "
                            + equality.getDifferentNumberOfRows( firstSnapshot, table ) + " rows in ["
                            + firstSnapshot + "] and "
                            + equality.getDifferentNumberOfRows( secondSnapshot, table ) + " in ["
                            + secondSnapshot + "]" );
            }
        }
    }

    private void addInfoAboutColumnsInOneSnapshotOnly( StringBuilder msg, DatabaseEqualityState equality,
                                                       String firstSnapshot, String secondSnapshot ) {

        List<String> tables = equality.getTablesWithColumnsPresentInOneSnapshotOnly( firstSnapshot );
        if( tables.size() > 0 ) {
            for( String table : tables ) {
                // add different columns for one table at a time
                msg.append( "\nTable columns for '" + table + "' table:" );

                List<String> firstColumns = equality.getColumnsPresentInOneSnapshotOnlyAsStrings( firstSnapshot,
                                                                                                  table );
                msg.append( "\n\t[" + firstSnapshot + "]:" );
                for( String column : firstColumns ) {
                    msg.append( "\n\t\t" + column );
                }

                List<String> secondColumns = equality.getColumnsPresentInOneSnapshotOnlyAsStrings( secondSnapshot,
                                                                                                   table );
                msg.append( "\n\t[" + secondSnapshot + "]:" );
                for( String column : secondColumns ) {
                    msg.append( "\n\t\t" + column );
                }
            }
        }
    }

    private void addInfoAboutIndexesInOneSnapshotOnly( StringBuilder msg, DatabaseEqualityState equality,
                                                       String firstSnapshot, String secondSnapshot ) {

        List<String> tables = equality.getTablesWithIndexesPresentInOneSnapshotOnly( firstSnapshot );
        if( tables.size() > 0 ) {
            for( String table : tables ) {
                // add different indexes for one table at a time
                msg.append( "\nTable indexes for '" + table + "' table:" );

                List<String> firstIndexes = equality.getIndexesPresentInOneSnapshotOnlyAsStrings( firstSnapshot,
                                                                                                  table );
                msg.append( "\n\t[" + firstSnapshot + "]:" );
                for( String index : firstIndexes ) {
                    msg.append( "\n\t\t" + index );
                }

                List<String> secondIndexes = equality.getIndexesPresentInOneSnapshotOnlyAsStrings( secondSnapshot,
                                                                                                   table );
                msg.append( "\n\t[" + secondSnapshot + "]:" );
                for( String index : secondIndexes ) {
                    msg.append( "\n\t\t" + index );
                }
            }
        }
    }

    private void addInfoAboutRowsInOneSnapshotOnly( StringBuilder msg, DatabaseEqualityState equality,
                                                    String firstSnapshot, String secondSnapshot ) {

        List<String> tables = equality.getTablesWithRowsPresentInOneSnapshotOnly( firstSnapshot );
        if( tables.size() > 0 ) {
            for( String table : tables ) {
                // add different rows for one table at a time
                msg.append( "\nTable rows for '" + table + "' table:" );

                List<String> firstRows = equality.getRowsPresentInOneSnapshotOnlyAsStrings( firstSnapshot,
                                                                                            table );
                msg.append( "\n\t[" + firstSnapshot + "]:" );
                for( String row : firstRows ) {
                    msg.append( "\n\t\t" + row );
                }

                List<String> secondRows = equality.getRowsPresentInOneSnapshotOnlyAsStrings( secondSnapshot,
                                                                                             table );
                msg.append( "\n\t[" + secondSnapshot + "]:" );
                for( String row : secondRows ) {
                    msg.append( "\n\t\t" + row );
                }
            }
        }
    }
}
