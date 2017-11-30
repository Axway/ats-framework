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
package com.axway.ats.agent.core.threading.data;

import java.lang.reflect.Constructor;
import java.util.Map;

import com.axway.ats.agent.core.threading.data.config.CustomParameterDataConfig;
import com.axway.ats.agent.core.threading.data.config.FileNamesDataConfig;
import com.axway.ats.agent.core.threading.data.config.ListDataConfig;
import com.axway.ats.agent.core.threading.data.config.ParameterDataConfig;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;
import com.axway.ats.agent.core.threading.data.config.RangeDataConfig;
import com.axway.ats.agent.core.threading.data.config.UsernameDataConfig;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderInitalizationException;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderNotSupportedException;

/**
 * The factory for creating parameter data providers based
 * on a provided configuration
 */
public class ParameterDataProviderFactory {

    /**
     * Private constructor to prevent instantiation
     */
    private ParameterDataProviderFactory() {

    }

    /**
     * Create a data provider based on a configuration provider
     *
     * @param parameterConfig the configuration based on which to create the provider
     * @return the newly created provider
     * @throws ParameterDataProviderNotSupportedException if the provider is not supported
     * @throws ParameterDataProviderInitalizationException if there is an error during provider initialization
     */
    public static ParameterDataProvider createDataProvider(
                                                            ParameterDataConfig parameterConfig ) throws ParameterDataProviderNotSupportedException,
                                                                                                  ParameterDataProviderInitalizationException {

        ParameterDataProvider parameterDataProvider;

        Class<?> parameterConfigClass = parameterConfig.getClass();
        if (parameterConfigClass == RangeDataConfig.class) {
            RangeDataConfig rangeDataConfig = (RangeDataConfig) parameterConfig;

            //there are different types of range providers, so
            //choose the right one, based on the arguments
            if (rangeDataConfig.getStaticValue() != null) {
                parameterDataProvider = new StringRangeParameterDataProvider(rangeDataConfig.getParameterName(),
                                                                             rangeDataConfig.getStaticValue(),
                                                                             (Integer) rangeDataConfig.getRangeStart(),
                                                                             (Integer) rangeDataConfig.getRangeEnd(),
                                                                             parameterConfig.getParameterProviderLevel());
            } else {
                parameterDataProvider = new IntegerRangeParameterDataProvider(rangeDataConfig.getParameterName(),
                                                                              (Integer) rangeDataConfig.getRangeStart(),
                                                                              (Integer) rangeDataConfig.getRangeEnd(),
                                                                              parameterConfig.getParameterProviderLevel());
            }

        } else if (parameterConfigClass == ListDataConfig.class) {
            ListDataConfig listDataConfig = (ListDataConfig) parameterConfig;
            parameterDataProvider = new ListParameterDataProvider(listDataConfig.getParameterName(),
                                                                  listDataConfig.getValues(),
                                                                  listDataConfig.getParameterProviderLevel());

        } else if (parameterConfigClass == FileNamesDataConfig.class) {
            FileNamesDataConfig fileNamesDataConfig = (FileNamesDataConfig) parameterConfig;
            parameterDataProvider = new FileNamesParameterDataProvider(fileNamesDataConfig.getParameterName(),
                                                                       fileNamesDataConfig.getFileContainers(),
                                                                       fileNamesDataConfig.getRecursiveSearch(),
                                                                       fileNamesDataConfig.getReturnFullPath(),
                                                                       fileNamesDataConfig.getParameterProviderLevel());
        } else if (parameterConfigClass == UsernameDataConfig.class) {

            UsernameDataConfig nameDataConfig = (UsernameDataConfig) parameterConfig;

            if (nameDataConfig.getValues() != null) {

                parameterDataProvider = new ListParameterDataProvider(nameDataConfig.getParameterName(),
                                                                      nameDataConfig.getValues(),
                                                                      nameDataConfig.getParameterProviderLevel(),
                                                                      UsernameDataConfig.class);

            } else {
                parameterDataProvider = new StringRangeParameterDataProvider(nameDataConfig.getParameterName(),
                                                                             nameDataConfig.getStaticValue(),
                                                                             (Integer) nameDataConfig.getRangeStart(),
                                                                             (Integer) nameDataConfig.getRangeEnd(),
                                                                             parameterConfig.getParameterProviderLevel(),
                                                                             UsernameDataConfig.class);
            }
        } else if (parameterConfigClass.getGenericSuperclass() == CustomParameterDataConfig.class) {
            // custom data provider
            CustomParameterDataConfig customDataConfig = (CustomParameterDataConfig) parameterConfig;

            try {
                // find the constructor of the custom data provider
                Constructor<? extends CustomParameterDataProvider> constructor = customDataConfig.getDataProviderClass()
                                                                                                 .getDeclaredConstructor(String.class,
                                                                                                                         Map.class,
                                                                                                                         ParameterProviderLevel.class);
                // call the constructor of the custom data provider
                parameterDataProvider = (CustomParameterDataProvider) constructor.newInstance(customDataConfig.getParameterName(),
                                                                                              customDataConfig.getControlTokens(),
                                                                                              customDataConfig.getParameterProviderLevel());

            } catch (Exception e) {
                throw new ParameterDataProviderNotSupportedException(parameterConfigClass);
            }
        } else {
            throw new ParameterDataProviderNotSupportedException(parameterConfigClass);
        }

        //initialize the newly created provider
        parameterDataProvider.initialize();

        //and return it back to the caller
        return parameterDataProvider;
    }
}
