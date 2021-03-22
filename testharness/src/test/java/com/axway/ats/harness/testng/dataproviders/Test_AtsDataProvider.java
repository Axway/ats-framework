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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.harness.BaseTest;
import com.axway.ats.harness.testng.exceptions.DataProviderException;

/**
 * Unit tests for the {@link AtsDataProvider} class
 */
public class Test_AtsDataProvider extends BaseTest {

    Logger sLog = LogManager.getLogger(Test_AtsDataProvider.class);

    /**
     * @throws NoSuchMethodException
     * @throws DataProviderException
     * @throws ConfigurationException 
     * @throws NoSuchPropertyException 
     * 
     */
    @Test
    public void verifyExcelProviderPositive() throws NoSuchMethodException, DataProviderException,
                                              NoSuchPropertyException, ConfigurationException {

        Method testMethod = findMethodByNameOnly(ExcelProviderTest.class, "test_scenario_1");
        Object[][] testData = AtsDataProvider.getTestData(testMethod);

        assertEquals(10, testData.length);
        assertEquals(9, testData[0].length);
        assertEquals("Pavel Georgiev/pgeorgiev@tmwd.com", testData[0][0]);
    }

    /**
     * @throws NoSuchMethodException
     * @throws DataProviderException
     * @throws ConfigurationException 
     * @throws NoSuchPropertyException 
     * 
     */
    @Test( expected = DataProviderException.class)
    public void verifyProviderNegativeNoDataFile() throws NoSuchMethodException, DataProviderException,
                                                   NoSuchPropertyException, ConfigurationException {

        Method testMethod = findMethodByNameOnly(AtsProviderNoExcelTest.class, "wrong");
        AtsDataProvider.getTestData(testMethod);
    }

    /**
     * @throws NoSuchMethodException
     * @throws DataProviderException
     * @throws ConfigurationException 
     * @throws NoSuchPropertyException 
     * 
     */
    @Test( expected = DataProviderException.class)
    public void verifyProviderNegativeNoSheetInExcel() throws NoSuchMethodException, DataProviderException,
                                                       NoSuchPropertyException, ConfigurationException {

        Method testMethod = findMethodByNameOnly(ExcelProviderTest.class, "wrong");
        AtsDataProvider.getTestData(testMethod);
    }

    private Method findMethodByNameOnly(
                                         Class<?> classToSearch,
                                         String methodName ) throws NoSuchMethodException {

        for (Method classMethod : classToSearch.getDeclaredMethods()) {
            if (classMethod.getName().equals(methodName)) {
                return classMethod;
            }
        }

        throw new NoSuchMethodException(methodName);
    }
}
