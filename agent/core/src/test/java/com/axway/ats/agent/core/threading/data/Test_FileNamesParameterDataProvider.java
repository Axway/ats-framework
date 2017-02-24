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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderInitalizationException;

public class Test_FileNamesParameterDataProvider extends BaseTest {

    private static String              folderName;

    private static List<FileContainer> fileContainers;

    @BeforeClass
    public static void setUp() throws URISyntaxException {

        URL testFileURL = Test_FileNamesParameterDataProvider.class.getResource( "/testfolder/classloader.html" );
        File testFile = new File( testFileURL.toURI() );
        folderName = testFile.getParent();
    }

    @Before
    public void before() {

        fileContainers = new ArrayList<FileContainer>();
        fileContainers.add( new FileContainer( folderName, 100, ".*" ) );
    }

    @Test
    public void perThreadGenerationRecursive() throws AgentException {

        FileNamesParameterDataProvider dataProvider = new FileNamesParameterDataProvider( "param1",
                                                                                          fileContainers,
                                                                                          false,
                                                                                          true,
                                                                                          ParameterProviderLevel.PER_THREAD );
        dataProvider.initialize();

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).matches( ".*[\\\\/](classloader|readme).html$" ) );
        String firstValue = ( String ) generatedValue.getValue();

        // now we expect the next file for the current thread
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertFalse( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );
    }

    @Test
    public void perInvocationGenerationRecursive() throws AgentException {

        FileNamesParameterDataProvider dataProvider = new FileNamesParameterDataProvider( "param1",
                                                                                          fileContainers,
                                                                                          true,
                                                                                          true,
                                                                                          ParameterProviderLevel.PER_INVOCATION );
        dataProvider.initialize();

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).matches( ".*[\\\\/](classloader|readme|release\\-notes).html" ) );
        String firstValue = ( String ) generatedValue.getValue();

        //make sure new instance is returned for the new invocation
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertFalse( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );
    }

    @Test
    public void perThreadStaticGeneration() throws AgentException {

        FileNamesParameterDataProvider dataProvider = new FileNamesParameterDataProvider( "param1",
                                                                                          fileContainers,
                                                                                          false,
                                                                                          true,
                                                                                          ParameterProviderLevel.PER_THREAD_STATIC );
        dataProvider.initialize();

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).matches( ".*[\\\\/](classloader|readme).html$" ) );
        String firstValue = ( String ) generatedValue.getValue();

        // now we expect the next file for the current thread to be same as the previous
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );
    }

    @Test
    public void perInvocationGenerationRecursiveJustFileName() throws AgentException {

        FileNamesParameterDataProvider dataProvider = new FileNamesParameterDataProvider( "param1",
                                                                                          fileContainers,
                                                                                          true,
                                                                                          false,
                                                                                          ParameterProviderLevel.PER_INVOCATION );
        dataProvider.initialize();

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).matches( "(classloader|readme|release\\-notes).html" ) );
        String firstValue = ( String ) generatedValue.getValue();

        //make sure new instance is returned for the new invocation
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertFalse( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );
    }

    @Test
    public void perThreadGenerationNonRecursive() throws AgentException {

        FileNamesParameterDataProvider dataProvider = new FileNamesParameterDataProvider( "param1",
                                                                                          fileContainers,
                                                                                          false,
                                                                                          true,
                                                                                          ParameterProviderLevel.PER_THREAD );
        dataProvider.initialize();

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).matches( ".*[\\\\/](classloader|readme).html" ) );
        String firstValue = ( String ) generatedValue.getValue();

        //make sure new instance per thread is returned
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertFalse( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );
    }

    @Test
    public void perInvocationGenerationNonRecursive() throws AgentException {

        FileNamesParameterDataProvider dataProvider = new FileNamesParameterDataProvider( "param1",
                                                                                          fileContainers,
                                                                                          false,
                                                                                          true,
                                                                                          ParameterProviderLevel.PER_INVOCATION );
        dataProvider.initialize();

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).matches( ".*[\\\\/](classloader|readme).html" ) );
        String firstValue = ( String ) generatedValue.getValue();

        //make sure new instance is returned for the new invocation
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertFalse( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );

        //make sure we go back to the beginning
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );
    }

    @Test
    public void whenRangeEndIsReachedProviderGoesBackToRangeStart() throws AgentException {

        FileNamesParameterDataProvider dataProvider = new FileNamesParameterDataProvider( "param1",
                                                                                          fileContainers,
                                                                                          false,
                                                                                          true,
                                                                                          ParameterProviderLevel.PER_INVOCATION );
        dataProvider.initialize();

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).matches( ".*[\\\\/](classloader|readme).html" ) );
        String firstValue = ( String ) generatedValue.getValue();

        //make sure new instance is returned for the new invocation
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertFalse( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );

        //make sure we go back to the beginning
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );
    }

    @Test
    public void whenRangeEndIsReachedProviderGoesBackToRangeStartJustFileName() throws AgentException {

        FileNamesParameterDataProvider dataProvider = new FileNamesParameterDataProvider( "param1",
                                                                                          fileContainers,
                                                                                          false,
                                                                                          false,
                                                                                          ParameterProviderLevel.PER_INVOCATION );
        dataProvider.initialize();

        ArgumentValue generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).matches( "(classloader|readme).html" ) );
        String firstValue = ( String ) generatedValue.getValue();

        //make sure new instance is returned for the new invocation
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( "param1", generatedValue.getName() );
        assertFalse( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );

        //make sure we go back to the beginning
        generatedValue = dataProvider.getValue( new ArrayList<ArgumentValue>() );
        assertEquals( dataProvider.getParameterName(), generatedValue.getName() );
        assertTrue( ( ( String ) generatedValue.getValue() ).equals( firstValue ) );
    }

    @Test(expected = ParameterDataProviderInitalizationException.class)
    public void initializeNegativeNoSuchFolder() throws AgentException {

        List<FileContainer> wrongFileContainers = new ArrayList<FileContainer>();
        wrongFileContainers.add( new FileContainer( "/asdfasdf", 100, ".*" ) );
        FileNamesParameterDataProvider dataProvider = new FileNamesParameterDataProvider( "param1",
                                                                                          wrongFileContainers,
                                                                                          true,
                                                                                          true,
                                                                                          ParameterProviderLevel.PER_THREAD );
        dataProvider.initialize();
    }

    @Test
    public void testComplexFileNameRegex() throws AgentException {

        List<FileContainer> fileContainers = new ArrayList<FileContainer>();
        fileContainers.add( new FileContainer( folderName, 100, "${fileNamePart}.*" ) );

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         fileContainers,
                                                                                         false,
                                                                                         true,
                                                                                         ParameterProviderLevel.PER_THREAD );
        fileNamesDP.initialize();

        ArgumentValue previousValue = new ArgumentValue( "fileNamePart", "read" );
        List<ArgumentValue> previousValues = new ArrayList<ArgumentValue>();
        previousValues.add( previousValue );

        ArgumentValue generatedValue = fileNamesDP.getValue( previousValues );
        String value = ( String ) generatedValue.getValue();
        assertTrue( value.matches( ".*[\\\\/]readme.html$" ) );

        // now we expect the next file for the current thread to be same as the previous
        generatedValue = fileNamesDP.getValue( previousValues );
        assertTrue( ( ( String ) generatedValue.getValue() ).equals( value ) );
    }

    @Test
    public void testComplexFileNameRegex_Negative() throws AgentException {

        List<FileContainer> fileContainers = new ArrayList<FileContainer>();
        fileContainers.add( new FileContainer( folderName, 100, "${fileNamePart}.*" ) );

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         fileContainers,
                                                                                         false,
                                                                                         true,
                                                                                         ParameterProviderLevel.PER_THREAD );
        fileNamesDP.initialize();

        ArgumentValue previousValue = new ArgumentValue( "fileNamePart", "wrongFileNamePart" );
        List<ArgumentValue> previousValues = new ArrayList<ArgumentValue>();
        previousValues.add( previousValue );

        try {
            fileNamesDP.getValue( previousValues );
            Assert.fail();
        } catch( RuntimeException re ) {
            assertTrue( re.getMessage()
                          .startsWith( "No files matching regex pattern 'wrongFileNamePart.*' in directory" ) );
        }
    }

    @Test
    public void testGenerateNewValuePerThread() throws AgentException {

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         fileContainers,
                                                                                         false,
                                                                                         true,
                                                                                         ParameterProviderLevel.PER_THREAD );
        fileNamesDP.initialize();

        // simulating threads with IDs 1 and 2
        ArgumentValue arg1 = fileNamesDP.generateNewValuePerThread( 1, new ArrayList<ArgumentValue>() );
        ArgumentValue arg2 = fileNamesDP.generateNewValuePerThread( 2, new ArrayList<ArgumentValue>() );
        ArgumentValue arg12 = fileNamesDP.generateNewValuePerThread( 1, new ArrayList<ArgumentValue>() );
        ArgumentValue arg22 = fileNamesDP.generateNewValuePerThread( 2, new ArrayList<ArgumentValue>() );

        // the first values for the 2 threads must be the same
        assertTrue( arg1.getValue().equals( arg2.getValue() ) );
        // the second values too
        assertTrue( arg12.getValue().equals( arg22.getValue() ) );
        // check if the first and second values for each thread are different
        assertTrue( !arg1.getValue().equals( arg12.getValue() ) );
        assertTrue( !arg2.getValue().equals( arg22.getValue() ) );
    }

    @Test
    public void testGenerateNewValuePerThreadStatic() throws AgentException {

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         fileContainers,
                                                                                         false,
                                                                                         true,
                                                                                         ParameterProviderLevel.PER_THREAD_STATIC );
        fileNamesDP.initialize();

        // simulating threads with IDs 1 and 2
        ArgumentValue arg1 = fileNamesDP.generateNewValuePerThreadStatic( 1, new ArrayList<ArgumentValue>() );
        ArgumentValue arg2 = fileNamesDP.generateNewValuePerThreadStatic( 2, new ArrayList<ArgumentValue>() );
        ArgumentValue arg12 = fileNamesDP.generateNewValuePerThreadStatic( 1, new ArrayList<ArgumentValue>() );
        ArgumentValue arg22 = fileNamesDP.generateNewValuePerThreadStatic( 2, new ArrayList<ArgumentValue>() );

        // each thread must have only one value
        // the files in the directory are 2 so the 2 threads must have different values
        assertTrue( !arg1.getValue().equals( arg2.getValue() ) );
        assertTrue( arg1.getValue().equals( arg12.getValue() ) );
        assertTrue( !arg12.getValue().equals( arg22.getValue() ) );
        assertTrue( arg2.getValue().equals( arg22.getValue() ) );

        // the files in the directory are 2 so the 3-rd thread must have the same value as the first one
        ArgumentValue arg3 = fileNamesDP.generateNewValuePerThreadStatic( 3, new ArrayList<ArgumentValue>() );
        ArgumentValue arg32 = fileNamesDP.generateNewValuePerThreadStatic( 3, new ArrayList<ArgumentValue>() );

        assertTrue( !arg2.getValue().equals( arg3.getValue() ) );
        assertTrue( arg1.getValue().equals( arg3.getValue() ) );
        assertTrue( arg3.getValue().equals( arg32.getValue() ) );
    }

    @Test
    public void testGenerateNewValuePerInvocation() throws AgentException {

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         fileContainers,
                                                                                         true,
                                                                                         true,
                                                                                         ParameterProviderLevel.PER_INVOCATION );
        fileNamesDP.initialize();

        ArgumentValue arg1 = fileNamesDP.generateNewValuePerInvocation( new ArrayList<ArgumentValue>() );
        ArgumentValue arg2 = fileNamesDP.generateNewValuePerInvocation( new ArrayList<ArgumentValue>() );
        ArgumentValue arg3 = fileNamesDP.generateNewValuePerInvocation( new ArrayList<ArgumentValue>() );
        ArgumentValue arg4 = fileNamesDP.generateNewValuePerInvocation( new ArrayList<ArgumentValue>() );

        // the files in the target directory are 3 so only the arg1 and arg4 must have the same values
        assertTrue( !arg1.getValue().equals( arg2.getValue() ) );
        assertTrue( !arg2.getValue().equals( arg3.getValue() ) );
        assertTrue( !arg3.getValue().equals( arg1.getValue() ) );
        assertTrue( arg1.getValue().equals( arg4.getValue() ) );
    }

    @Test
    public void testSimpleFileNameRegex() throws AgentException {

        List<FileContainer> fileContainers = new ArrayList<FileContainer>();
        fileContainers.add( new FileContainer( folderName, 100, ".ea.*ml" ) );

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         fileContainers,
                                                                                         false,
                                                                                         false,
                                                                                         ParameterProviderLevel.PER_THREAD );
        fileNamesDP.initialize();

        ArgumentValue previousValue = new ArgumentValue( "fileNamePart", "read" );
        List<ArgumentValue> previousValues = new ArrayList<ArgumentValue>();
        previousValues.add( previousValue );

        ArgumentValue generatedValue = fileNamesDP.getValue( previousValues );
        String value = ( String ) generatedValue.getValue();
        assertTrue( value.matches( "readme.html$" ) );

        // now we expect the next file for the current thread to be same as the previous
        generatedValue = fileNamesDP.getValue( previousValues );
        assertTrue( ( ( String ) generatedValue.getValue() ).equals( value ) );
    }

    @Test(expected = ParameterDataProviderInitalizationException.class)
    public void testSimpleFileNameRegex_Negative() throws AgentException {

        List<FileContainer> fileContainers = new ArrayList<FileContainer>();
        fileContainers.add( new FileContainer( folderName, 100, ".*testFileName.*" ) );

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         fileContainers,
                                                                                         false,
                                                                                         true,
                                                                                         ParameterProviderLevel.PER_THREAD );
        fileNamesDP.initialize();
    }

    @Test(expected = ParameterDataProviderInitalizationException.class)
    public void noFileContainers() throws AgentException {

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         null,
                                                                                         false,
                                                                                         true,
                                                                                         ParameterProviderLevel.PER_THREAD );
        fileNamesDP.initialize();
    }

    @Test
    public void testFileContainerPercentageUsage_50_50() throws AgentException {

        List<FileContainer> fileContainers = new ArrayList<FileContainer>();
        fileContainers.add( new FileContainer( folderName, 50, ".eadme.*" ) );
        fileContainers.add( new FileContainer( folderName, 50, ".*loader.*" ) );

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         fileContainers,
                                                                                         false,
                                                                                         false,
                                                                                         ParameterProviderLevel.PER_THREAD );
        fileNamesDP.initialize();

        ArgumentValue previousValue = new ArgumentValue( "fileNamePart", "read" );
        List<ArgumentValue> previousValues = new ArrayList<ArgumentValue>();
        previousValues.add( previousValue );

        Map<String, Integer> valuesMap = new HashMap<String, Integer>();
        for( int i = 0; i < 100; i++ ) {
            ArgumentValue generatedValue = fileNamesDP.getValue( previousValues );
            String value = ( String ) generatedValue.getValue();
            int occurance = 0;
            if( valuesMap.containsKey( value ) ) {
                occurance = valuesMap.get( value );
            }
            valuesMap.put( value, occurance + 1 );
        }

        assertTrue( valuesMap.size() == 2 );
        assertTrue( valuesMap.get( "classloader.html" ) == valuesMap.get( "readme.html" ) );
        assertTrue( valuesMap.get( "classloader.html" ) == 50 );
    }

    @Test
    public void testFileContainerPercentageUsage_75_25() throws AgentException {

        List<FileContainer> fileContainers = new ArrayList<FileContainer>();
        fileContainers.add( new FileContainer( folderName, 75, ".eadme.*" ) );
        fileContainers.add( new FileContainer( folderName, 25, ".*loader.*" ) );

        FileNamesParameterDataProvider fileNamesDP = new FileNamesParameterDataProvider( "fileNameParam",
                                                                                         fileContainers,
                                                                                         false,
                                                                                         false,
                                                                                         ParameterProviderLevel.PER_THREAD );
        fileNamesDP.initialize();

        ArgumentValue previousValue = new ArgumentValue( "fileNamePart", "read" );
        List<ArgumentValue> previousValues = new ArrayList<ArgumentValue>();
        previousValues.add( previousValue );

        Map<String, Integer> valuesMap = new HashMap<String, Integer>();
        for( int i = 0; i < 100; i++ ) {
            ArgumentValue generatedValue = fileNamesDP.getValue( previousValues );
            String value = ( String ) generatedValue.getValue();
            int occurance = 0;
            if( valuesMap.containsKey( value ) ) {
                occurance = valuesMap.get( value );
            }
            valuesMap.put( value, occurance + 1 );
        }

        assertTrue( valuesMap.size() == 2 );
        assertTrue( valuesMap.get( "classloader.html" ) == 25 );
        assertTrue( valuesMap.get( "readme.html" ) == 75 );
    }

}
