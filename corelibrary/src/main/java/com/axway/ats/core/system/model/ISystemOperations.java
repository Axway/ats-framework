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
package com.axway.ats.core.system.model;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.system.SystemOperationException;

public interface ISystemOperations {

    /**
     * Get Operating System type/name
     *
     * @return the {@link OperatingSystemType}
     */
    public OperatingSystemType getOperatingSystemType();

    public String getSystemProperty(
                                     String propertyName );

    /**
     * Get current system time
     *
     * @param inMilliseconds if the time value is in milliseconds or using a specific Date formatter
     * @return the current system time
     */
    public String getTime(
                           boolean inMilliseconds );

    /**
     * Set the system time
     *
     * @param timestamp the timestamp
     * @param inMilliseconds whether the timestamp is in milliseconds or a formatted date string
     */
    public void setTime(
                         String timestamp,
                         boolean inMilliseconds );

    /**
     *
     * @return the current ATS version
     */
    public String getAtsVersion();

    /**
     * Creates screenshot image file.<br>
     * The currently supported image formats/types are PNG, JPG, JPEG, GIF and BMP<br>
     * <br>
     * <b>NOTE:</b> For remote usage, the filePath value must be only the image file extension/format/type eg. ".PNG"
     * The temporary image file with this format will be created
     *
     * @param filePath the screenshot image file path. If the file extension is not specified, the default format PNG will be used
     * @return the created screenshot image file path
     */
    public String createScreenshot(
                                    String filePath );

    /**
    *
    * @param host host address
    * @param port port number
    * @param timeout timeout value in milliseconds
    * @return <code>true</code> if the address is listening on this port
    * @throws SystemOperationException when the target host is unknown
    */
    public boolean isListening(
                                String host,
                                int port,
                                int timeout );

    /**
     *
     * @return system input operations instance
     */
    public ISystemInputOperations getInputOperations();

    /**
     * @return machine hostname
     */
    public String getHostname();

    /**
     * ClassPath list all JARs in current application's ClassPath
     * 
     * @return array of all detected JARs from ClassPath
     */
    public String[] getClassPath();

    /**
     * Log all JARs in current application's ClassPath
     */
    public void logClassPath();

    /**
     * @return list containing all jars in the ClassPath
     */
    public String[] getDuplicatedJars();

    /**
     * Log all duplicated JARs in current application's ClassPath
     */
    public void logDuplicatedJars();

    /**
     * Set the threshold for the ATS DB Appender
     * @param threshold - the threshold ( Level.INFO|DEBUG,etc )
     * */
    public void setAtsDbAppenderThreshold( Level threshold );

    /**
     * Attach file appender to the Root logger
     * @param filepath - path/to/logfile. Can be absolute or relative
     * @param messageFormatPattern - the layout/format of the log messages. For more information see {@link PatternLayout} If not sure what to use, use this one:<br/><br/> 
     * <strong><code>"%-5p %d{HH:mm:ss:SSS} %c{2}: %m%n</code></strong>
     * */
    public void attachFileAppender( String filepath, String messageFormatPattern );
}
