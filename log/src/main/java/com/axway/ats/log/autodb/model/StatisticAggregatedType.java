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
package com.axway.ats.log.autodb.model;

import java.util.ArrayList;
import java.util.List;

public class StatisticAggregatedType {

    public static int REGULAR = 0;
    public static int AVERAGE = 1;
    public static int SUM     = 2;
    public static int TOTALS  = 4;
    public static int COUNT   = 8;

    // hide the default constructor
    private StatisticAggregatedType() {

    }

    public static boolean isAverage(
                                     int mode ) {

        return (mode & AVERAGE) > 0;
    }

    public static boolean isSum(
                                 int mode ) {

        return (mode & SUM) > 0;
    }

    public static boolean isTotals(
                                    int mode ) {

        return (mode & TOTALS) > 0;
    }

    public static boolean isCount(
                                   int mode ) {

        return (mode & COUNT) > 0;
    }

    public static Integer[] getAllTypes(
                                         int mode ) {

        List<Integer> types = new ArrayList<Integer>();
        if (mode == REGULAR) {
            types.add(REGULAR);
        } else {
            if (isAverage(mode)) {
                types.add(AVERAGE);
            }
            if (isSum(mode)) {
                types.add(SUM);
            }
            if (isTotals(mode)) {
                types.add(TOTALS);
            }
            if (isCount(mode)) {
                types.add(COUNT);
            }
        }
        return types.toArray(new Integer[0]);
    }
}
