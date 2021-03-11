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

import org.testng.TestNGException;
import org.testng.annotations.Test;

import com.axway.ats.harness.BaseTest;
import com.axway.ats.harness.testng.TestOptions;

@TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER + "AnotherFolder/")
public class Test_BasicDataProvider extends BaseTest {

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER, dataFile = TestDetails.DATA_FILE1, dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFileFolder_fromMethodAnnotation( String user, String pswd, String subject ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFile = TestDetails.DATA_FILE2, dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFileFolder_fromClassAnnotation( String user, String pswd, String subject ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER, dataFile = TestDetails.DATA_FILE1, dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFile_fromMethodAnnotation( String user, String pswd, String subject ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFile = "Test_BasicDataProvider.xls", dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFile_notInPackage( String user, String pswd, String subject ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFileFolder = "src/test/resources/", dataFile = "com/axway/ats/harness/testng/dataproviders.ExcelProviderTest.xls", dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFile_withinPackage( String arg0, String arg1, String arg2, String arg3, String arg4, String arg5,
                                        String arg6, String arg7, String arg8 ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class, expectedExceptions = TestNGException.class)
    @TestOptions( dataFile = "Test_BasicDataProvider", dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFile_noFileExtension( String user, String pswd, String subject ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class, expectedExceptions = TestNGException.class)
    @TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER, dataFile = TestDetails.DATA_FILE1
                                                                             + "NON_EXISTING_FILE", dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFile_wrongFile( String user, String pswd, String subject ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER, dataFile = TestDetails.DATA_FILE1, dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataSheet_fromMethodAnnotation( String user, String pswd, String subject ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER, dataFile = TestDetails.DATA_FILE1)
    public void dataSheet_usingTheMethodName( String user, String pswd, String subject ) {}
}
