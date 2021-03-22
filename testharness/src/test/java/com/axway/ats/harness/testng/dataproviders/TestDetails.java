/*
 * Copyright 2017-2021 Axway Software
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

package com.axway.ats.harness.testng.dataproviders;

import org.testng.annotations.Test;

import com.axway.ats.harness.testng.TestOptions;

/**
 * Some test constants used in this package
 */
public class TestDetails {

    public static final String PATH_TO_DATA_FILE       = "src/test/resources/TestSuite.xls";

    public static final String WRONG_PATH_TO_DATA_FILE = "src/test/resources/TestSute.xls";

    public static final String WRONG_BIFF_FILE         = "src/test/resources/wrongBiffFile.txt";

    public static final String DATA_FILE2              = "DataFile2.xls";

    public static final String DATA_FILE1              = "DataFile1.xls";

    public static final String DATA_FILE_IN_CLASSPATH  = "DataFileInClasspath.xls";

    public static final String DATA_FILES_FOLDER       = "src/test/resources/dataFilesFolder/";

    public static final String FIRST_TEST_SCENARIO     = "test_scenario_1";

    public static final String SECOND_TEST_SCENARIO    = "test_scenario_2";

    public static final String THIRD_TEST_SCENARIO     = "test_scenario_3";

    public static final String FOURTH_TEST_SCENARIO    = "test_scenario_4";

    public static final String FIFTH_TEST_SCENARIO     = "test_scenario_5";

    public static final String SIXTH_TEST_SCENARIO     = "test_scenario_6";

    public static final String SEVENTH_TEST_SCENARIO   = "test_scenario_7";

    public static final String EIGHTH_TEST_SCENARIO    = "test_scenario_8";

    public static final String NINGHT_TEST_SCENARIO    = "test_scenario_9";

    public static final String TENGHT_TEST_SCENARIO    = "test_scenario_10";

    // a method with less parameters than that in the excel data sheet
    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER, dataFile = TestDetails.DATA_FILE1, dataSheet = "test_scenario_1")
    public void test_scenario_1(
                                 String parameter1,
                                 String parameter2 ) {

    }

    // a method with more parameters than that in the excel data sheet
    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER, dataFile = TestDetails.DATA_FILE1, dataSheet = "dataSheet_usingTheMethodName")
    public void dataSheet_usingTheMethodName(
                                              String parameter1,
                                              String parameter2,
                                              String parameter3,
                                              String parameter4 ) {

    }

    // a method with number of parameters equal to that in the excel data sheet
    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER, dataFile = TestDetails.DATA_FILE1, dataSheet = "dataSheet_usingTheMethodName")
    public void dataSheet_usingEqualP(
                                       String parameter1,
                                       String parameter2,
                                       String parameter3 ) {

    }

}
