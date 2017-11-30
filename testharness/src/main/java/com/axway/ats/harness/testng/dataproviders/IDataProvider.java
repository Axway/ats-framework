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

import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.harness.testng.exceptions.DataProviderException;

/**
 * Expose the common methods for each separate data provider
 */
public interface IDataProvider {

    /**
     * Returns a set of test data, depending on the {@link Method} that requires it
     * 
     * @param m the {@link Method} that requires the test data
     * @return a two dimensional array of Object elements
     * @throws {@link DataProviderException) if there is a problem getting the data
     */
    public Object[][] fetchDataBlock(
                                      Method m ) throws DataProviderException, NoSuchPropertyException,
                                                 ConfigurationException;
}
