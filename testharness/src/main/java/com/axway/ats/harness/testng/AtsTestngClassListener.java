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

import org.testng.ITestClass;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.ActiveDbAppender;

/**
 * <b>NOTE:</b> This listener can not work with an official TestNG distribution!
 * 
 * Optional ATS listener invoked from TestNG.
 * Used to allow capturing log events from within @BeforeClass and @AfterClass methods.
 */
public class AtsTestngClassListener {

    private static final AtsDbLogger logger = AtsDbLogger.getLogger( "com.axway.ats" );
    private static String           lastSuiteName;

    /**
     * Invoked when tests from a new class are about to start 
     * @param testClass the class under test
     */
    public void onStart( ITestClass testClass ) {

        if( ActiveDbAppender.getCurrentInstance() != null ) {

            String suiteName = testClass.getName();
            String suiteSimpleName = suiteName.substring( suiteName.lastIndexOf( '.' ) + 1 );

            String packageName = getPackageName( suiteName );
            logger.startSuite( packageName, suiteSimpleName );
            lastSuiteName = suiteName;
        }
    }

    /**
     * Invoked after all tests from a given class had been invoked 
     */
    public void onFinish() {

        if( ActiveDbAppender.getCurrentInstance() != null ) {
            if( lastSuiteName != null ) {
                logger.endSuite();
                lastSuiteName = null;
            } else { // should not happen
                logger.error( "Wrong test listeners state",
                              new RuntimeException( "AtsTestngClassListener.onFinish() method is invoked but seems that onStart() is not invoked first" ) );
            }
        }
    }

    public void resetTempData() {

        AtsTestngClassListener.lastSuiteName = null;
    }

    public static String getLastSuiteName() {

        return lastSuiteName;
    }

    public static String getPackageName( String className ) {

        if( !StringUtils.isNullOrEmpty( className ) ) {
            int lastDotInx = className.lastIndexOf( '.' );
            if( lastDotInx <= 0 ) {
                return "";
            } else {
                return className.substring( 0, lastDotInx );
            }
        } else {
            return "";
        }
    }
}
