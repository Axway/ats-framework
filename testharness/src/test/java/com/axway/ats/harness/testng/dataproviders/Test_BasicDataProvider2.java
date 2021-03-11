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

import com.axway.ats.harness.BaseTest;
import com.axway.ats.harness.testng.TestOptions;

@TestOptions( dataFileFolder = TestDetails.DATA_FILES_FOLDER + "AnotherFolder2/")
public class Test_BasicDataProvider2 extends BaseTest {

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    public void dataFileFolder_fromClassAnnotat(
                                                 String user,
                                                 String pswd,
                                                 String subject ) {

    }

    @Test( dataProvider = "ConfigurableDataProvider", dataProviderClass = AtsDataProvider.class)
    public void dataDrivenTest2(
                                 String protocol,
                                 int port,
                                 int numberUsers ) {

    }

}
