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
package com.axway.ats.harness.testng;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.TestNG;

import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.harness.config.CommonConfigurator;
import com.axway.ats.harness.testng.exceptions.TestHarnessInitializationException;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.appenders.ReportAppender;

/**
 * When the patched TestNG starts a test/suite/run it loads listeners form suite xml or 
 * from the testng template xml (from the eclipse Preferences window) and after that 
 * it creates an instance from this class and in the constructor we can add additional 
 * listeners if they are not already added.
 */
@Deprecated
public class InitTestHarness {

    private Logger logger = Logger.getLogger( InitTestHarness.class );

    public InitTestHarness() throws TestHarnessInitializationException {

        @SuppressWarnings("deprecation")
        TestNG testNgInstance = TestNG.getDefault();
        if( testNgInstance != null ) {
            //Attaching our listeners only when our appenders are loaded
            if( ActiveDbAppender.getCurrentInstance() != null || ReportAppender.isAppenderActive() ) {

                addListener( testNgInstance,
                             testNgInstance.getTestListeners(),
                             "com.axway.ats.harness.testng.AtsTestngTestListener" );
                addListener( testNgInstance,
                             testNgInstance.getSuiteListeners(),
                             "com.axway.ats.harness.testng.AtsTestngSuiteListener" );
            }

            //Attaching custom test and suite listeners from the ats.config.properties file
            CommonConfigurator configurator = CommonConfigurator.getInstance();
            List<String> customTestListeners = getListeners( configurator,
                                                             "harness.testng.customtestlisteners" );
            List<String> customSuiteListeners = getListeners( configurator,
                                                              "harness.testng.customsuitelisteners" );
            if( customTestListeners.size() > 0 ) {
                for( String listener : customTestListeners ) {
                    addListener( testNgInstance, testNgInstance.getTestListeners(), listener );
                }
            }
            if( customSuiteListeners.size() > 0 ) {
                for( String listener : customSuiteListeners ) {
                    addListener( testNgInstance, testNgInstance.getSuiteListeners(), listener );
                }
            }
        } else {
            throw new TestHarnessInitializationException( "Unable to get TestNG instance." );
        }
    }

    private List<String> getListeners(
                                       CommonConfigurator configurator,
                                       String key ) {

        List<String> listeners = new ArrayList<String>();
        String customListeners = "";
        try {
            customListeners = configurator.getProperty( key );
        } catch( NoSuchPropertyException e ) {
            //It is normal state, the property is optional
            return listeners;
        }
        String[] parts = customListeners.split( "[\\s\\,]+" );
        for( String p : parts ) {
            if( !p.isEmpty() ) {
                listeners.add( p );
            }
        }
        return listeners;
    }

    /**
     * 
     * @param testNG testNG instance
     * @param existingListeners existing listeners list (TestListeners/SuiteListeners/ReportListeners/... list)
     * @param listenerClassName listener canonical name
     */
    private void addListener(
                              TestNG testNG,
                              List<?> existingListeners,
                              String listenerClassName ) {

        Class<?> listenerClass;
        try {
            listenerClass = Class.forName( listenerClassName );
            if( !isListenerAlreadyAttached( existingListeners, listenerClass ) ) {
                Object listener = listenerClass.newInstance();
                testNG.addListener( listener );
                logger.info( "Successfully attached listener \"" + listenerClassName + "\"." );
            } else {
                logger.info( "Skip attaching listener \"" + listenerClassName
                             + "\". It's already attached!" );
            }
        } catch( Exception e ) {
            logger.error( "Error attaching listener: \"" + listenerClassName + "\".", e );
        }
    }

    /**
     * Check if a listener is already attached
     * @param list - list of listeners
     * @param listenerClass - listener class to search in this list
     * @return <code>true</code> if in the list there is an object of class equals to clazz
     */
    private boolean isListenerAlreadyAttached(
                                               List<?> list,
                                               Class<?> listenerClass ) {

        for( Object o : list ) {
            if( o.getClass().equals( listenerClass ) ) {
                return true;
            }
        }
        return false;
    }
}
