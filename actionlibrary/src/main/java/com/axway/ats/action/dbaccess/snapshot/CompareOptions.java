/*
 * Copyright 2020 Axway Software
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

import java.util.HashMap;
import java.util.Map;

import com.axway.ats.common.PublicAtsApi;

/**
 * 
 * Additional compare options that affect DatabaseSnapshot comparison.<br>
 * If some error is expected, you can use this class to mark this error as that, expected error, and ATS will silently remove this error.
 * You can also enable/disable throwing of error if some of the expected errors are not found (by default it is enabled)
 * */
@PublicAtsApi
public class CompareOptions {

    public static class Pair<T> {
        T first;
        T second;

        Pair( T first, T second ) {

            this.first = first;
            this.second = second;
        }

        T getFirst() {

            return first;
        }

        T getSecond() {

            return second;
        }
    }

    //private Map<String, Pair<Integer>> expectedTableDifferentNumberOfRows = new HashMap<String, CompareOptions.Pair<Integer>>();

    private Map<String, Pair<Integer>> expectedTableMissingRows   = new HashMap<String, CompareOptions.Pair<Integer>>();

    private boolean                    failOnMissingExpectedError = true;

    @PublicAtsApi
    public CompareOptions() {

    }

    /*public void setExpectedTableDifferentNumberOfRows( String tableName, int minRowsDifference,
                                                       int maxRowsDifference ) {
    
        if (minRowsDifference < 0) {
            throw new IllegalArgumentException("min rows must be positive number, but was " + minRowsDifference);
        }
        if (maxRowsDifference < 0) {
            throw new IllegalArgumentException("max rows must be positive number, but was " + maxRowsDifference);
        }
        if (minRowsDifference > maxRowsDifference) {
            throw new IllegalArgumentException("max rows must be greather or equal to min rows, but instead is less than that!");
        }
    
        expectedTableDifferentNumberOfRows.put(tableName, new Pair<Integer>(minRowsDifference, maxRowsDifference));
    }*/

    /**
     * Specify the total number of different rows for one table between the two snapshots.<br>
     * Those different rows are actually UNIQUE rows for each snapshot, so you have to combine all of the unique rows and set the appropriate min and maxRows
     * For example if some table have 2 unique rows for one snapshot and 1 unique row for the other snapshot, 
     * you can set both MIN and MAX to 3, or just MAX to 3 and MIN to anything lower than maxRows which is still positive number 
     * 
     * @param tableName - the table name
     * @param minRows - the minimum number of the total unique rows in both snapshots. It must be positive number, LOWER or EQUAL to the maxRows
     * @param maxRows - the maximum number of the total unique rows in both snapshots. It must be positive number, GREATER or EQUAL to the minRows
     * @throws IllegalArgumentException if minRows is < 0, minRows > maxRows, maxRows < 0 or maxRows < minRows 
     * */
    @PublicAtsApi
    public void setExpectedTableMissingRowsCount( String tableName, int minRows, int maxRows ) {

        if (minRows < 0) {
            throw new IllegalArgumentException("min rows for table '" + tableName
                                               + "' must be positive number, but is specified to " + minRows);
        }
        if (maxRows < 0) {
            throw new IllegalArgumentException("max rows for table '" + tableName
                                               + "' must be positive number, but is specified to " + maxRows);
        }
        if (minRows > maxRows) {
            throw new IllegalArgumentException("max rows (" + maxRows + ") for table '" + tableName
                                               + "' must be greather or equal to min rows (" + minRows
                                               + "), but instead is less than that!");
        }

        expectedTableMissingRows.put(tableName, new Pair<Integer>(minRows, maxRows));

    }

    /**
     * Whether to throw an error if one or more expected errors are not presented during snapshot comparison
     * @param toggle toggle exception throwing on missing expected error. true by default
     * */
    @PublicAtsApi
    public void setFailOnMissingExpectedError( boolean toggle ) {

        this.failOnMissingExpectedError = toggle;
    }

    public boolean isFailOnMissingExpectedError() {

        return this.failOnMissingExpectedError;
    }

    /*public Map<String, Pair<Integer>> getExpectedTableDifferentNumberOfRows() {
    
        return this.expectedTableDifferentNumberOfRows;
    }*/

    /**
     * Returns all of the user specified min/max for all tables, which should have UNIQUE rows per snapshot.<br>
     * See {@link CompareOptions#setExpectedTableMissingRowsCount(String, int, int)} for more informnation
     * @return map in the format { tableName => Pair(minRows, maxRows) }
     * */
    @PublicAtsApi
    public Map<String, Pair<Integer>> getExpectedTableMissingRows() {

        return this.expectedTableMissingRows;
    }

}
