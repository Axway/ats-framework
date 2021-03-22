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

public class Test_BasicDataProviderWithoutClassAnnotation extends BaseTest {

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class, expectedExceptions = TestNGException.class)
    @TestOptions( dataFile = TestDetails.DATA_FILE2, dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFileFolder_fromClassAnnotation( String user, String pswd, String subject ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    @TestOptions( dataFile = TestDetails.DATA_FILE_IN_CLASSPATH, dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFile_fromClasspath( String user, String pswd, String subject ) {}

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class, expectedExceptions = TestNGException.class)
    @TestOptions( dataSheet = TestDetails.FIRST_TEST_SCENARIO)
    public void dataFile_fromClasspath_negative( String user, String pswd, String subject ) {}
}
