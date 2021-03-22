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

package com.axway.ats.harness.testng.dataproviders;

import java.io.InputStream;
import java.lang.reflect.Method;

import com.axway.ats.core.utils.IoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.harness.testng.exceptions.DataProviderException;

/**
 * Provides test data, by reading it from an excel spreadsheet. This implementation uses the {@link ExcelParser} to
 * parse the data from the spreadsheet.
 */
@PublicAtsApi
public class ExcelDataProvider extends BasicDataProvider implements IDataProvider {

    Logger sLog = LogManager.getLogger(ExcelDataProvider.class);

    /**
     * Returns a set of test data, depending on the {@link Method} that requires it. This specific implementation
     * searches for the excel spread sheet in the same directory structure, that the calling method's class is in (i.e.
     * com/axway/some_package/tests).
     *
     * @param m the {@link Method} that requires the test data
     * @return a two dimensional array of Object elements
     * @throws ConfigurationException
     * @throws NoSuchPropertyException
     * @throws {@link DataProviderException) if there is a problem getting the data
     *
     * @see com.axway.ats.harness.testng.dataproviders.IDataProvider#fetchDataBlock(java.lang.reflect.Method)
     */
    public Object[][] fetchDataBlock(
                                      Method m ) throws DataProviderException, NoSuchPropertyException,
                                                 ConfigurationException {

        InputStream dataFileInputStream = null;
        try {
            dataFileInputStream = getDataFileInputStream(m);
            String dataSheet = getDataSheet(m);

            ExcelParser excelParser = new ExcelParser(dataFileInputStream, dataSheet);
            Object[][] data = excelParser.getDataBlock(m);

            //When the number of test method input arguments is different than the number of columns
            //in the table feeding this test method a friendly RuntimeException exception is thrown

            if (data.length != 0) {
                if (data[0].length != m.getParameterTypes().length) {
                    throw new RuntimeException("Unable to load data. Expected " + m.getParameterTypes().length
                                               + " number of parameters while received " + data[0].length + "!");
                }
            }
            return data;
        } finally {
            IoUtils.closeStream(dataFileInputStream, "Error closing data provider stream for method " + m.getName());
        }

    }
}
