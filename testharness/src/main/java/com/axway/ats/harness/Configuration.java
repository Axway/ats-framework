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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import com.axway.ats.log.appenders.ActiveDbAppender;

/**
 * Some basic configuration class.<br>
 * <br>
 * It searches for log4j.properties file in the classpath. <br>
 * It searches the key for a value "com.axway.ats.log.appenders.ActiveDBAppender".
 * Let's call it <i>--prefix--</i>.
 * <br>
 * Then it searches for the values of the following keys:
 *
 * <pre>
 * <td>
 * --prefix--.run.os - the OS name, it is used when opens a new run.
 * </td>
 * <td>
 * --prefix--.run.product - the product name, it is used when opens a new run.
 * </td>
 * <td>
 * --prefix--.run.version - the product version, it is used when opens a new run.
 * </td>
 * <td>
 * --prefix--.run.build - the product build, it is used when opens a new run.
 * </td>
 * <td>
 * --prefix--.logexceptions - if should log an error message when a test is aborted due to exception. Any value other than &quot;false&quot; is treated as a &quot;true&quot;.
 * </td>
 * </pre>
 *
 * <b>Note:</b> If any of these steps fail, a default value is used
 */
public class Configuration {

    // log4j keys
    private static final String KEY__RUN_NAME         = ".run.name";

    private static final String KEY__OS_NAME          = ".run.os";

    private static final String KEY__PRODUCT_NAME     = ".run.product";

    private static final String KEY__VERSION_NAME     = ".run.version";

    private static final String KEY__BUID_NAME        = ".run.build";

    public static final String  DEFAULT_RUN_NAME      = "Default Run Name";

    /**
     * Any value other than "false" is treated as "true"
     */
    private static final String KEY__LOG_ON_EXCEPTION = ".logexceptions";

    // Run parameters
    private static String       runName            = DEFAULT_RUN_NAME;

    private static String       osName             = "Default OS";

    private static String       productName        = "Default Product Name";

    private static String       versionName        = "1.0.0";

    private static String       buildName          = "1000";

    // if should log an error message when a test is aborted due to exception
    private static boolean      logOnException        = true;

    /**
     * Loads logging configuration related to Automation DB appender.
     * <em>Note:</em> It is internally used and should not be invoked by the test case developer.
     */
    public static void init() {

        try(InputStream fileIn = Configuration.class.getResourceAsStream( "/log4j.properties" )) {
            if( fileIn == null ) {
                System.err.println( "log4j.properties file not found" );
                return;
            }

            Properties fileProps = new Properties();
            fileProps.load( fileIn );

            // search for the needed key prefix
            String dbAppenderKey = null;
            for( Entry<Object, Object> entry : fileProps.entrySet() ) {
                if( entry.getValue().equals( ActiveDbAppender.class.getName() ) ) {
                    dbAppenderKey = entry.getKey().toString();
                    break;
                }
            }

            if( dbAppenderKey != null ) {
                runName = fileProps.getProperty( dbAppenderKey + KEY__RUN_NAME, runName );
                osName = fileProps.getProperty( dbAppenderKey + KEY__OS_NAME, osName );
                productName = fileProps.getProperty( dbAppenderKey + KEY__PRODUCT_NAME, productName );
                versionName = fileProps.getProperty( dbAppenderKey + KEY__VERSION_NAME, versionName );
                buildName = fileProps.getProperty( dbAppenderKey + KEY__BUID_NAME, buildName );

                if( "false".equals( fileProps.getProperty( dbAppenderKey + KEY__LOG_ON_EXCEPTION ) ) ) {
                    logOnException = false;
                }
            }
        } catch( FileNotFoundException e ) {
            // silently use the default values
        } catch( IOException e ) {
            // silently use the default values
        }
    }

    public static String getRunName() {

        return runName;
    }

    public static String getOsName() {

        return osName;
    }

    public static String getProductName() {

        return productName;
    }

    public static String getVersionName() {

        return versionName;
    }

    public static String getBuildName() {

        return buildName;
    }

    public static boolean getLogOnException() {

        return Configuration.logOnException;
    }
}
