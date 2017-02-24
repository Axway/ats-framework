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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;

public class Test_FileNamesDataConfig extends BaseTest {

    @Test
    public void gettersPositive() {

        FileNamesDataConfig fileNamesConfig = new FileNamesDataConfig( "user", "C:\\Temp" );

        assertEquals( "C:\\Temp", fileNamesConfig.getFileContainers().get( 0 ).getFolderName() );
        assertEquals( true, fileNamesConfig.getRecursiveSearch() );
        assertEquals( true, fileNamesConfig.getReturnFullPath() );
        assertEquals( "user", fileNamesConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_THREAD, fileNamesConfig.getParameterProviderLevel() );

        fileNamesConfig = new FileNamesDataConfig( "user", "C:\\Temp", false );

        assertEquals( "C:\\Temp", fileNamesConfig.getFileContainers().get( 0 ).getFolderName() );
        assertEquals( true, fileNamesConfig.getRecursiveSearch() );
        assertEquals( false, fileNamesConfig.getReturnFullPath() );
        assertEquals( "user", fileNamesConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_THREAD, fileNamesConfig.getParameterProviderLevel() );

        fileNamesConfig = new FileNamesDataConfig( "user", "C:\\Temp", ParameterProviderLevel.PER_INVOCATION );

        assertEquals( "C:\\Temp", fileNamesConfig.getFileContainers().get( 0 ).getFolderName() );
        assertEquals( true, fileNamesConfig.getRecursiveSearch() );
        assertEquals( true, fileNamesConfig.getReturnFullPath() );
        assertEquals( "user", fileNamesConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_INVOCATION, fileNamesConfig.getParameterProviderLevel() );

        fileNamesConfig = new FileNamesDataConfig( "user",
                                                   "C:\\Temp",
                                                   false,
                                                   ParameterProviderLevel.PER_INVOCATION );

        assertEquals( "C:\\Temp", fileNamesConfig.getFileContainers().get( 0 ).getFolderName() );
        assertEquals( false, fileNamesConfig.getRecursiveSearch() );
        assertEquals( true, fileNamesConfig.getReturnFullPath() );
        assertEquals( "user", fileNamesConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_INVOCATION, fileNamesConfig.getParameterProviderLevel() );

        fileNamesConfig = new FileNamesDataConfig( "user",
                                                   "C:\\Temp",
                                                   false,
                                                   false,
                                                   ParameterProviderLevel.PER_INVOCATION );

        assertEquals( "C:\\Temp", fileNamesConfig.getFileContainers().get( 0 ).getFolderName() );
        assertEquals( false, fileNamesConfig.getRecursiveSearch() );
        assertEquals( false, fileNamesConfig.getReturnFullPath() );
        assertEquals( "user", fileNamesConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_INVOCATION, fileNamesConfig.getParameterProviderLevel() );

        /*
         * Test add folders with different file percentage usage and file name patterns
         */
        fileNamesConfig = new FileNamesDataConfig( "logfile" );
        fileNamesConfig.addFolder( "/var/log/", 60 );
        fileNamesConfig.addFolder( "/tmp/", 40, ".*\\.log" );

        assertEquals( "/var/log/", fileNamesConfig.getFileContainers().get( 0 ).getFolderName() );
        assertEquals( ".*", fileNamesConfig.getFileContainers().get( 0 ).getPattern() );
        assertEquals( 60, fileNamesConfig.getFileContainers().get( 0 ).getPercentage() );
        assertEquals( "/tmp/", fileNamesConfig.getFileContainers().get( 1 ).getFolderName() );
        assertEquals( ".*\\.log", fileNamesConfig.getFileContainers().get( 1 ).getPattern() );
        assertEquals( 40, fileNamesConfig.getFileContainers().get( 1 ).getPercentage() );
        assertEquals( true, fileNamesConfig.getRecursiveSearch() );
        assertEquals( true, fileNamesConfig.getReturnFullPath() );
        assertEquals( "logfile", fileNamesConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_THREAD, fileNamesConfig.getParameterProviderLevel() );
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {

        FileNamesDataConfig fileNamesConfig = new FileNamesDataConfig( "user",
                                                                       "C:\\Temp",
                                                                       false,
                                                                       false,
                                                                       ParameterProviderLevel.PER_INVOCATION );

        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream( byteOutStream );
        objectOutStream.writeObject( fileNamesConfig );

        ObjectInputStream objectInStream = new ObjectInputStream( new ByteArrayInputStream( byteOutStream.toByteArray() ) );
        FileNamesDataConfig deserializedFileNamesConfig = ( FileNamesDataConfig ) objectInStream.readObject();

        assertEquals( "C:\\Temp", fileNamesConfig.getFileContainers().get( 0 ).getFolderName() );
        assertEquals( false, deserializedFileNamesConfig.getRecursiveSearch() );
        assertEquals( false, fileNamesConfig.getReturnFullPath() );
        assertEquals( "user", deserializedFileNamesConfig.getParameterName() );
        assertEquals( ParameterProviderLevel.PER_INVOCATION,
                      deserializedFileNamesConfig.getParameterProviderLevel() );
    }

    @Test
    public void distributeOneHost() throws Exception {

        FileNamesDataConfig fileNamesConfig = new FileNamesDataConfig( "user", "C:\\Temp" );

        // currently we do not split, but clone this configuration
        assertEquals( 1, fileNamesConfig.distribute( 1 ).size() );
        assertEquals( 3, fileNamesConfig.distribute( 3 ).size() );

        for( ParameterDataConfig dataConfig : fileNamesConfig.distribute( 3 ) ) {
            FileNamesDataConfig currentFileNamesConfig = ( FileNamesDataConfig ) dataConfig;

            assertEquals( fileNamesConfig.getFileContainers().get( 0 ).getFolderName(),
                          fileNamesConfig.getFileContainers().get( 0 ).getFolderName() );
            assertEquals( fileNamesConfig.getParameterName(), currentFileNamesConfig.getParameterName() );
            assertEquals( fileNamesConfig.getParameterProviderLevel(),
                          currentFileNamesConfig.getParameterProviderLevel() );
            assertEquals( fileNamesConfig.getRecursiveSearch(), currentFileNamesConfig.getRecursiveSearch() );
            assertEquals( fileNamesConfig.getReturnFullPath(), currentFileNamesConfig.getReturnFullPath() );
        }
    }
}
