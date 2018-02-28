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
package com.axway.ats.agent.core.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.axway.ats.agent.core.BaseTest;
import com.axway.ats.core.utils.IoUtils;

public class Test_ActionClassGenerator extends BaseTest {

    private static String       descriptorFile;
    private static String       testOutputDirName;
    private static String       testSourceDirName;
    private static String       expectedGeneratedFilesDirName;

    private static final String sourcePackage = "com.axway.ats.agent.core.ant.needed_in_acgen_tests";
    private static final String targetPackage = "test.acgen";

    @BeforeClass
    public static void setUpTest_ActionClassGenerator() throws IOException {

        descriptorFile = Test_ActionClassGenerator.class.getResource( "/agent_descriptor.xml" ).getFile();
        testOutputDirName = ( new File( descriptorFile ) ).getParent() + "/test";
        testSourceDirName = getTestSourcesRootPath( testOutputDirName )
                            + sourcePackage.replace( ".", "/" )/* + "/needed_in_acgen_tests"*/;
        int srcFolderIndex = testSourceDirName.indexOf( RELATIVE_PATH_TO_TEST_SOURCES );
        expectedGeneratedFilesDirName = testSourceDirName.substring( 0, srcFolderIndex ) + "/"
                                        + RELATIVE_PATH_TO_TEST_RESOURCES
                                        + "/ExpectedGeneratedFiles/com/axway/ats/agent/core/ant/needed_in_acgen_tests/";
    }

    /**
     * Get path to resources folder of the same package
     * @param currentDirPath
     * @return
     */
    private static String getTestSourcesRootPath( String currentDirPath ) {

        File currentDir = new File( currentDirPath );
        while( currentDir != null ) {
            if( currentDir.isDirectory() ) {
                String path = currentDir.getAbsolutePath() + "/" + RELATIVE_PATH_TO_TEST_SOURCES + "/com/";
                if( new File( path ).exists() ) {

                    return currentDir.getAbsolutePath() + "/" + RELATIVE_PATH_TO_TEST_SOURCES + "/";
                }
            }
            currentDir = currentDir.getParentFile();
        }
        System.err.println( "Can't find the Project Root directory, searchig backward from dir: "
                            + currentDirPath );
        return "";
    }

    @Before
    public void setUpTestMethod() throws IOException {

        // Clean test folder
        File resourceDir = new File( testOutputDirName );
        if( resourceDir.exists() ) {
            deleteFolder( resourceDir );
        }
    }

    @Test
    public void generatePositive() throws Exception {

        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, sourcePackage,
                                                                   targetPackage,
                                                                   new HashMap<String, String>() );
        generator.generate();
    }

    @Test(expected = BuildException.class)
    public void generateNegativeNoSuchDescriptor() throws Exception {

        ActionClassGenerator generator = new ActionClassGenerator( "fasdfa", testSourceDirName,
                                                                   testOutputDirName, sourcePackage,
                                                                   targetPackage,
                                                                   new HashMap<String, String>() );
        generator.generate();
    }

    @Test
    public void verifyImportsFromActionReturnTypes() throws Exception {

        verifyImports( "TestWithReturnValuesClass.java" );
    }

    @Test
    public void verifyImportsFromActionInputArgumentTypes() throws Exception {

        verifyImports( "TestWithInputValuesClass.java" );
    }

    private void verifyImports( String srcFileName ) throws Exception {

        // 1. Generate a client stub
        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, sourcePackage,
                                                                   targetPackage,
                                                                   new HashMap<String, String>() );
        generator.generate();

        // 2. Verify its imports generated for the return types of its actions
        File fileToCheck = new File( testOutputDirName + "/" + targetPackage.replace( ".", "/" ) + "/"
                                     + srcFileName );
        if( fileToCheck.exists() ) {
            int testTypeImps = 0;
            int calendarImps = 0;
            int dateImps = 0;
            int listImps = 0;
            int mapImps = 0;
            int localeImps = 0;
            Scanner sc = null;
            try {
                sc = new Scanner( fileToCheck );
                while( sc.hasNextLine() ) {
                    String line = sc.nextLine();
                    if( line.equals( "import java.util.Calendar;" ) ) {
                        calendarImps++;
                    } else if( line.equals( "import java.util.Date;" ) ) {
                        dateImps++;
                    } else if( line.equals( "import java.util.Locale;" ) ) {
                        localeImps++;
                    } else if( line.equals( "import java.util.List;" ) ) {
                        listImps++;
                    } else if( line.equals( "import java.util.Map;" ) ) {
                        mapImps++;
                    } else if( line.equals( "import com.axway.ats.agent.core.ant.needed_in_acgen_tests.TestType;" ) ) {
                        testTypeImps++;

                        // This check is valid for our test class, because it is possible to have imports like
                        // java.lang.annotation.... but now we haven't and we make check to prevent the imports from
                        // java.lang package like String, Long, Integer...
                    } else if( line.startsWith( "import java.lang." ) ) {
                        Assert.fail();
                    }
                }
            } finally {
                if( sc != null )
                    sc.close();
            }
            if( calendarImps == 1 && dateImps == 1 && listImps == 1 && mapImps == 1 && testTypeImps == 1
                && localeImps == 1 ) {
                return;
            }
        }
        Assert.fail();
    }

    @Test
    public void verifyImportsFromMemberClasses() throws Exception {

        // 1. Generate a client stub
        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, sourcePackage,
                                                                   targetPackage,
                                                                   new HashMap<String, String>() );
        generator.generate();

        // 2. Verify its imports generated for the member classes
        File fileToCheck = new File( testOutputDirName + "/" + targetPackage.replace( ".", "/" )
                                     + "/test/TestWithMemberClasses.java" );
        if( fileToCheck.exists() ) {
            int firstMemberClassImps = 0;
            int secondMemberClassImps = 0;
            int thirdMemberClassImps = 0;
            Scanner sc = null;
            try {
                sc = new Scanner( fileToCheck );
                while( sc.hasNextLine() ) {
                    String line = sc.nextLine();
                    if( line.equals( "import test.acgen.test.FirstMemberClass;" ) ) {
                        firstMemberClassImps++;
                    } else if( line.equals( "import test.acgen.test.memberclasses.SecondMemberClass;" ) ) {
                        secondMemberClassImps++;
                    } else if( line.equals( "import test.acgen.TestWithReturnValuesClass;" ) ) {
                        thirdMemberClassImps++;
                    }
                }
            } finally {
                if( sc != null )
                    sc.close();
            }
            // firstMemberClassImps == 0 because it is in the same directory
            if( firstMemberClassImps == 0 && secondMemberClassImps == 1 && thirdMemberClassImps == 1 ) {
                return;
            }
        }
        Assert.fail();
    }

    @Test
    public void verifyPackagesOfTheActionClasses() throws Exception {

        // 1. Generate a client stub
        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, sourcePackage,
                                                                   targetPackage,
                                                                   new HashMap<String, String>() );
        generator.generate();

        //2. Verify action class packages
        String targetDir = testOutputDirName + "/" + targetPackage.replace( ".", "/" );
        String generatedTargetPackage = targetPackage;
        verifyActionClassesPackages( targetDir, generatedTargetPackage );
    }

    @Test
    public void verifyTargetPackage_whenTargetShorterThanSource() throws Exception {

        //1. Generate client stub
        String source = "com.axway.ats";
        String target = "test";
        String generatedTargetPackage = "test.agent.core.ant.needed_in_acgen_tests";
        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, source, target,
                                                                   new HashMap<String, String>() );
        generator.generate();

        //2. Verify action class packages
        String targetDir = testOutputDirName + "/" + generatedTargetPackage.replace( ".", "/" );
        verifyActionClassesPackages( targetDir, generatedTargetPackage );
    }

    @Test
    public void verifyTargetPackage_whenTargetEqualsToSource() throws Exception {

        //1. Generate client stub
        String source = "com.axway.ats.agent.core.ant.needed_in_acgen_tests.test";
        String target = "com.axway.ats.agent.core.ant.needed_in_acgen_tests.test";
        String generatedTargetPackage = "com.axway.ats.agent.core.ant.needed_in_acgen_tests";
        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, source, target,
                                                                   new HashMap<String, String>() );
        generator.generate();

        //2. Verify action class packages
        String targetDir = testOutputDirName + "/" + generatedTargetPackage.replace( ".", "/" );
        verifyActionClassesPackages( targetDir, generatedTargetPackage );
    }

    @Test
    public void verifyTargetPackage_whenTargetLargerThanSource() throws Exception {

        //1. Generate client stub
        String source = "com.axway.ats.agent.core.ant.needed_in_acgen_tests";
        String target = "com.axway.ats.agent.core.ant.needed_in_acgen_tests.client";
        String generatedTargetPackage = "com.axway.ats.agent.core.ant.needed_in_acgen_tests.client";
        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, source, target,
                                                                   new HashMap<String, String>() );
        generator.generate();

        //2. Verify action class packages
        String targetDir = testOutputDirName + "/" + generatedTargetPackage.replace( ".", "/" );
        verifyActionClassesPackages( targetDir, generatedTargetPackage );
    }

    @Test
    public void verifyGeneratedConstants() throws Exception {

        // 1. Generate a client stub
        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, sourcePackage,
                                                                   targetPackage,
                                                                   new HashMap<String, String>() );
        generator.generate();

        // 2. Verify its imports generated for the return types of its actions
        File fileToCheck = new File( testOutputDirName + "/" + targetPackage.replace( ".", "/" )
                                     + "/ActionClassWithConstants.java" );
        if( fileToCheck.exists() ) {
            int intConstantHits = 0;
            int intArrayConstantHits = 0;

            int stringConstantHits = 0;
            int stringArrayConstantHits = 0;

            int enumConstantHits = 0;
            int enumArrayConstantHits = 0;

            // when an action uses enum as input argument, agent creates String constants for all
            // the possible enum values in the form
            // public static final String <ENUM TYPE NAME IN CAPITALS>_<ENUM VALUE IN CAPITALS> = "<ENUM VALUE IN CAPITALS>";
            int enumConstantsFromActionInputParametersHits = 0;

            Scanner sc = null;
            try {
                sc = new Scanner( fileToCheck );
                while( sc.hasNextLine() ) {
                    String line = sc.nextLine().trim();
                    if( line.equals( "public static final int INT_CONSTANT = 1;" ) ) {
                        intConstantHits++;
                    } else if( line.equals( "public static final String STRING_CONSTANT = \"one\";" ) ) {
                        stringConstantHits++;
                    } else if( line.equals( "public static final int[] INT_CONSTANTS = { 1," ) ) {
                        processArrayConstantsDeclaration( sc, "\\d,", "\\d\\};" );
                        intArrayConstantHits++;
                    } else if( line.equals( "public static final String[] STRING_CONSTANTS = { \"one\"," ) ) {
                        processArrayConstantsDeclaration( sc, "\"[A-Za-z0-9]*\",", "\"[A-Za-z0-9]*\"};" );
                        stringArrayConstantHits++;
                    } else if( line.startsWith( "public static final SomeTestEnum ENUM_" ) ) {
                        enumConstantHits++;
                    } else if( line.equals( "public static final SomeTestEnum[] ENUM_CONSTANTS = { ONE," ) ) {
                        processArrayConstantsDeclaration( sc, "[A-Z]*,", "[A-Z]*};" );
                        enumArrayConstantHits++;
                    } else if( line.startsWith( "public static final String SOMETESTENUM_" ) ) {
                        enumConstantsFromActionInputParametersHits++;
                    } else if( line.startsWith( "public static final" ) ) {
                        Assert.fail( "Unexpected constant: ' " + line + "'" );
                    }
                }
            } finally {
                if( sc != null )
                    sc.close();
            }
            if( intConstantHits == 1 && intArrayConstantHits == 1

                && stringConstantHits == 1 && stringArrayConstantHits == 1

                && enumConstantHits == 2 && enumArrayConstantHits == 1

                && enumConstantsFromActionInputParametersHits == 3 ) {
                return;
            }
        }
        Assert.fail();
    }

    @Test
    public void verifyJavaDocExist() throws Exception {

        // 1. Generate a client stub
        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, sourcePackage,
                                                                   targetPackage,
                                                                   new HashMap<String, String>() );
        generator.generate();

        //2. Verify java doc exist
        File fileToCheck = new File( testOutputDirName + "/" + targetPackage.replace( ".", "/" )
                                     + "/TestWithReturnValuesClass.java" );
        if( fileToCheck.exists() ) {

            boolean firstJavaDocCkeck = false;
            boolean secondJavaDocCkeck = false;

            Scanner sc = null;
            try {
                sc = new Scanner( fileToCheck );
                while( sc.hasNextLine() ) {
                    String line = sc.nextLine().trim();
                    if( line.endsWith( "* @return Map with strings and testTypes values" ) ) {
                        firstJavaDocCkeck = true;
                    } else if( line.endsWith( "* @throws AgentException  if an error occurs during action execution" ) ) {
                        secondJavaDocCkeck = true;
                    }
                }
            } finally {
                if( sc != null )
                    sc.close();
            }
            if( firstJavaDocCkeck && secondJavaDocCkeck ) {
                return;
            }
        }
        Assert.fail();
    }

    /**
     * Verify some classes packages
     * These classes are located in different levels in the package tree
     *
     * @param targetDir target directory name
     * @param generatedTargetPackage final target package
     * @throws FileNotFoundException if the client action java fail is not found
     */
    private void verifyActionClassesPackages( String targetDir,
                                              String generatedTargetPackage ) throws IOException {

        //2.1 First File Check
        String expectedPackage = "package " + generatedTargetPackage + ".test;";
        verifyExpectedStringIsFound( expectedPackage, targetDir + "/test/TestWithMemberClasses.java" );

        //2.2 Second File Check
        expectedPackage = "package " + generatedTargetPackage + ".test;";
        verifyExpectedStringIsFound( expectedPackage, targetDir + "/test/FirstMemberClass.java" );

        //2.3 Third File Check
        expectedPackage = "package " + generatedTargetPackage + ";";
        verifyExpectedStringIsFound( expectedPackage, targetDir + "/TestWithReturnValuesClass.java" );

        //2.4 Fourth File Check
        expectedPackage = "package " + generatedTargetPackage + ".test.memberclasses;";
        verifyExpectedStringIsFound( expectedPackage,
                                     targetDir + "/test/memberclasses/SecondMemberClass.java" );
    }

    @Test
    @Ignore// TODO - investigate in detail. It seems that method order is changed when switching to newer JDK
    public void verifyComplexActionClass() throws Exception {

        // 1. Generate a client stub
        ActionClassGenerator generator = new ActionClassGenerator( descriptorFile, testSourceDirName,
                                                                   testOutputDirName, sourcePackage,
                                                                   targetPackage,
                                                                   new HashMap<String, String>() );
        generator.generate();

        //2. Verify generated file is as expected
        File generatedFile = new File( testOutputDirName + "/" + targetPackage.replace( ".", "/" )
                                       + "/ComplexActionClass.java" );
        File templateFile = new File( expectedGeneratedFilesDirName + "ComplexActionClass.java" );

        verifyGeneratedAndTemplateFilesAreEqual( generatedFile, templateFile );
    }

    /**
     * Search in file to find expected string. Otherwise fail with provided message
     * @throws IOException
     * @throws AssertionError if expectedString is not found
     *
     */
    private void verifyExpectedStringIsFound( String expectedString, String filePath ) throws IOException {

        boolean isOK = false;
        File fileToCheck = new File( filePath );
        if( fileToCheck.exists() ) {
            Scanner sc = null;
            try {
                sc = new Scanner( fileToCheck );
                while( sc.hasNextLine() ) {
                    if( sc.nextLine().equals( expectedString ) ) {
                        isOK = true;
                        break;
                    }
                }
            } finally {
                if( sc != null ) {
                    sc.close(); // TODO in Java 7 Scanner implements Closeable
                }
            }
        } else {
            Assert.fail( "File named '" + filePath + "' was not found in order to be checked its contents" );
        }
        if( !isOK ) {
            Assert.fail( "Message string ('" + expectedString + "') was not found in file '"
                         + fileToCheck.getCanonicalPath() + "'" );
        }
    }

    /**
     *
     * @param path file path to be deleted
     * @throws IOException IOException
     */
    private static void deleteFolder( File path ) throws IOException {

        File[] files = path.listFiles();

        for( int i = 0; i < files.length; ++i ) {
            if( files[i].isDirectory() )
                deleteFolder( files[i] );

            files[i].delete();
        }
    }

    private void processArrayConstantsDeclaration( Scanner sc, String arrayConstantDeclarationNextLinePattern,
                                                   String arrayConstantDeclarationLastLinePattern ) {

        final Pattern nextLinePattern = Pattern.compile( arrayConstantDeclarationNextLinePattern );
        final Pattern lastLinePattern = Pattern.compile( arrayConstantDeclarationLastLinePattern );
        while( sc.hasNextLine() ) {
            String arrayDeclationLine = sc.nextLine().trim();
            if( nextLinePattern.matcher( arrayDeclationLine ).matches() ) {
                // this is the next line
            } else if( lastLinePattern.matcher( arrayDeclationLine ).matches() ) {
                // reach the end of the array declaration
                break;
            } else {
                Assert.fail( "Invalid array constant declaration" );
            }
        }
    }

    private void verifyGeneratedAndTemplateFilesAreEqual( File generatedFile,
                                                          File templateFile ) throws Exception {

        final String ERR_PREFIX = "Error comparing " + generatedFile.toString() + " and "
                                  + templateFile.toString() + ":\n";

        BufferedReader inputFileReader = new BufferedReader( new FileReader( generatedFile ) );
        BufferedReader outputFileReader = new BufferedReader( new FileReader( templateFile ) );

        String inputFileLine = null;
        String outputFileLine = null;
        try {

            for( int iLine = 1;; iLine++ ) {
                inputFileLine = readNextNonEmptyLine( inputFileReader );
                outputFileLine = readNextNonEmptyLine( outputFileReader );

                if( iLine == 1 ) {
                    // skip the package line
                    continue;
                }

                if( inputFileLine == null && outputFileLine == null ) {
                    // end of files, verification succeeded
                    return;
                }

                if( inputFileLine != null && outputFileLine != null ) {
                    // compare both lines
                    inputFileLine = inputFileLine.trim();
                    outputFileLine = outputFileLine.trim();
                    if( !inputFileLine.equals( outputFileLine ) ) {
                        Assert.fail( ERR_PREFIX + "found difference at line " + iLine + ".\n"
                                     + generatedFile.toString() + "[" + iLine + "]: '" + inputFileLine + "'\n"
                                     + templateFile.toString() + "[" + iLine + "] : '" + outputFileLine
                                     + "'" );
                    }
                    continue;
                }

                Assert.fail( ERR_PREFIX + "different file lenghts, shorter file is " + iLine
                             + " lines long" );
            }
        } finally {
            IoUtils.closeStream( inputFileReader );
            IoUtils.closeStream( outputFileReader );
        }
    }

    private String readNextNonEmptyLine( BufferedReader fileReader ) throws IOException {

        String fileLine = "";

        while( true ) {
            fileLine = fileReader.readLine();
            if( fileLine == null ) {
                break;
            }

            fileLine = fileLine.trim();
            if( fileLine.length() > 0 ) {
                break;
            }
        }

        return fileLine;
    }
}
