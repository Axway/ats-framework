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

import java.lang.reflect.Method;

import org.junit.Test;

import com.axway.ats.harness.BaseTest;
import com.axway.ats.harness.testng.exceptions.DataProviderException;

public class Test_ExcelDataProvider extends BaseTest {

    /**
     * negative test
     * 
     * @throws RuntimeException
     * @throws NoSuchMethodException
     * @throws DataProviderException
     */
    @Test( expected = DataProviderException.class)
    public void lessMethodParameters() throws RuntimeException, NoSuchMethodException, DataProviderException {

        Method testMethod = findMethodByNameOnly(TestDetails.class, "test_scenario_1");
        ExcelDataProvider excelDataProvider = new ExcelDataProvider();
        excelDataProvider.fetchDataBlock(testMethod);

    }

    /**
     * negative test
     * 
     * @throws RuntimeException
     * @throws NoSuchMethodException
     * @throws DataProviderException
     */
    @Test( expected = DataProviderException.class)
    public void moreMethodParameters() throws RuntimeException, NoSuchMethodException, DataProviderException {

        Method testMethod = findMethodByNameOnly(TestDetails.class, "dataSheet_usingTheMethodName");
        ExcelDataProvider excelDataProvider = new ExcelDataProvider();
        excelDataProvider.fetchDataBlock(testMethod);

    }

    /**
     * positive test
     * 
     * @throws RuntimeException
     * @throws NoSuchMethodException
     * @throws DataProviderException
     */
    @Test
    public void testNormalReturn() throws RuntimeException, NoSuchMethodException, DataProviderException {

        Method testMethod = findMethodByNameOnly(TestDetails.class, "dataSheet_usingEqualP");
        ExcelDataProvider excelDataProvider = new ExcelDataProvider();
        excelDataProvider.fetchDataBlock(testMethod);

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
