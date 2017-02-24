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

import java.lang.reflect.Method;

import org.testng.annotations.DataProvider;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.harness.TestHarnessConfigurator;
import com.axway.ats.harness.testng.exceptions.DataProviderException;

/**
 * Different excel data providers
 */
@PublicAtsApi
public class AtsDataProvider {

    /**
     * @param m The {@link Method} object of the Test Method calling this Data Provider.
     * @return Returns an Object[][] containing the data from the Excel File
     * @throws DataProviderException exceptions related to parsing the Excel file
     * @throws ConfigurationException
     * @throws NoSuchPropertyException
     */
    @DataProvider(name = "ConfigurableDataProvider")
    public static Object[][] getTestData(
                                          Method m ) throws DataProviderException, NoSuchPropertyException,
                                                    ConfigurationException {

        DataProviderType dataProviderType = TestHarnessConfigurator.getInstance().getDataProvider();
        IDataProvider dataProvider = null;

        // TODO: this should probably go in a factory
        switch( dataProviderType ){
            case EXCEL: {
                dataProvider = new ExcelDataProvider();
                break;
            }
            default:
                throw new DataProviderException( "Data provider '" + dataProviderType.toString()
                                                 + "' is not supported" );
        }

        return dataProvider.fetchDataBlock( m );
    }
}
