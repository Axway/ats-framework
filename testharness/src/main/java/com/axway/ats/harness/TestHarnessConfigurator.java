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

package com.axway.ats.harness;

import com.axway.ats.config.AbstractConfigurator;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.harness.testng.dataproviders.DataProviderType;

/**
 * This class is used to read a set of properties used by the test harness
 */
public class TestHarnessConfigurator extends AbstractConfigurator {

    private static final String PROPERTIES_FILE_NAME = "/ats.harness.properties";

    private static final String PROPERTY_DATA_PROVIDER        = "harness.dataprovider.type";
    private static final String PROPERTY_TEST_CASES_DIRECTORY = "harness.dataprovider.testcasesroot";

    private DataProviderType propertyDataProvider;
    private String           propertyTestcasesRoot;

    /**
     * The singleton instance for this configurator
     */
    private static TestHarnessConfigurator instance;

    private TestHarnessConfigurator( String configurationSource ) {

        super();

        //add the resource to the repository
        addConfigFileFromClassPath( configurationSource, true , false);
    }

    public static synchronized TestHarnessConfigurator getInstance() {

        if( instance == null ) {
            instance = new TestHarnessConfigurator( PROPERTIES_FILE_NAME );
        }

        return instance;
    }

    /** 
     * @return the fully qualified path to the test cases root directory 
     */
    public String getSuitesRootDirectory() {

        return propertyTestcasesRoot;
    }

    /**
     * @return the data provider to be used 
     */
    public DataProviderType getDataProvider() {

        return propertyDataProvider;
    }

    @Override
    protected void reloadData() {

        String propertyDataProviderString = getProperty( PROPERTY_DATA_PROVIDER );
        try {
            propertyDataProvider = Enum.valueOf( DataProviderType.class,
                                                 propertyDataProviderString.toUpperCase() );
        } catch( IllegalArgumentException iae ) {
            throw new ConfigurationException( "Data provider '" + propertyDataProviderString
                                              + "' is not supported" );
        }

        propertyTestcasesRoot = getProperty( PROPERTY_TEST_CASES_DIRECTORY );
    }
}
