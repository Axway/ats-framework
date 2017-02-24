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
package com.axway.ats.agent.core.loading;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.core.utils.IoUtils;

public class Test_ReverseDelegationClassLoader extends BaseTest {

    private static File loaderResources;
    private static URL  loaderTestJar;

    @BeforeClass
    public static void initTest_ReverseDelegationClassLoader() throws URISyntaxException,
                                                               MalformedURLException {

        loaderResources = new File( RELATIVE_PATH_TO_TEST_RESOURCES + "/loadertestresources" );
        loaderTestJar = new File( loaderResources.getAbsolutePath() + "/agenttest.jar" ).toURI().toURL();
    }

    @Test
    public void verifyClassLoading() throws ClassNotFoundException {

        ReversedDelegationClassLoader reverseLoader = null;
        try {
            reverseLoader = new ReversedDelegationClassLoader( new URL[]{ loaderTestJar },
                                                               Test_ReverseDelegationClassLoader.class.getClassLoader() );
            Class<?> actionClass = reverseLoader.loadClass( "com.axway.ats.agent.core.ant.component.agenttest.FirstActionClass" );
            assertNotNull( actionClass );
        } finally {
            IoUtils.closeStream( reverseLoader );
        }
    }

    @Test
    public void verifyResourceLoading() throws ClassNotFoundException {

        ReversedDelegationClassLoader reverseLoader = null;
        try {
            reverseLoader = new ReversedDelegationClassLoader( new URL[]{ loaderTestJar },
                                                               Test_ReverseDelegationClassLoader.class.getClassLoader() );
            InputStream resource = reverseLoader.getResourceAsStream( "com/axway/ats/agent/core/ant/component/agenttest/ServerSqlCommon.properties" );
            assertNotNull( resource );

            resource = reverseLoader.getResourceAsStream( "ServerSqlCommon.properties" );
            assertNotNull( resource );
        } finally {
            IoUtils.closeStream( reverseLoader );
        }
    }
}
