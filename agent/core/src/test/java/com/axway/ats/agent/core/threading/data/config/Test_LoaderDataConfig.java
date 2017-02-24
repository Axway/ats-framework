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
package com.axway.ats.agent.core.threading.data.config;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;

public class Test_LoaderDataConfig extends BaseTest {

    @Test
    public void addGetParameterConfigurations() {

        LoaderDataConfig loaderConfig = new LoaderDataConfig();

        assertEquals( 0, loaderConfig.getParameterConfigurations().size() );

        loaderConfig.addParameterConfig( new RangeDataConfig( "param1", "test1", 5, 10 ) );
        loaderConfig.addParameterConfig( new RangeDataConfig( "param1", "test1", 15, 20 ) );

        assertEquals( 2, loaderConfig.getParameterConfigurations().size() );
    }

    @Test
    public void distributeSeveralHosts() throws Exception {

        LoaderDataConfig loaderConfig = new LoaderDataConfig();

        loaderConfig.addParameterConfig( new RangeDataConfig( "param1", "test1", 10, 12 ) );

        // verify we have the right number of loader configurations
        List<LoaderDataConfig> distributedLoaderConfigs = loaderConfig.distribute( 2 );
        assertEquals( 2, distributedLoaderConfigs.size() );

        // verify the first loader configuration - START
        LoaderDataConfig firstLoaderConfig = distributedLoaderConfigs.get( 0 );

        assertEquals( 1, firstLoaderConfig.getParameterConfigurations().size() );
        RangeDataConfig firstLoaderParameterConfig = ( RangeDataConfig ) firstLoaderConfig.getParameterConfigurations()
                                                                                          .get( 0 );
        assertEquals( 10, firstLoaderParameterConfig.getRangeStart() );
        assertEquals( 10, firstLoaderParameterConfig.getRangeEnd() );
        // verify the first loader configuration - END

        // verify the second loader configuration - START
        LoaderDataConfig secondLoaderConfig = distributedLoaderConfigs.get( 1 );

        assertEquals( 1, secondLoaderConfig.getParameterConfigurations().size() );
        RangeDataConfig secondLoaderParameterConfig = ( RangeDataConfig ) secondLoaderConfig.getParameterConfigurations()
                                                                                            .get( 0 );
        assertEquals( 12, secondLoaderParameterConfig.getRangeStart() );
        assertEquals( 13, secondLoaderParameterConfig.getRangeEnd() );
        // verify the second loader configuration - END
    }

    @Test
    public void distributeOneHost() throws Exception {

        LoaderDataConfig loaderConfig = new LoaderDataConfig();

        loaderConfig.addParameterConfig( new RangeDataConfig( "param1", "test1", 10, 12 ) );

        // verify we have the right number of loader configurations
        List<LoaderDataConfig> distributedLoaderConfigs = loaderConfig.distribute( 1 );
        assertEquals( 1, distributedLoaderConfigs.size() );

        // verify the only loader configuration - START
        LoaderDataConfig firstLoaderConfig = distributedLoaderConfigs.get( 0 );

        assertEquals( 1, firstLoaderConfig.getParameterConfigurations().size() );
        RangeDataConfig firstLoaderParameterConfig = ( RangeDataConfig ) firstLoaderConfig.getParameterConfigurations()
                                                                                          .get( 0 );
        assertEquals( 10, firstLoaderParameterConfig.getRangeStart() );
        assertEquals( 12, firstLoaderParameterConfig.getRangeEnd() );
        // verify the only loader configuration - END
    }
}
