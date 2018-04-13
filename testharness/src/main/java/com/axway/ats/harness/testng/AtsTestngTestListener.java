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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.annotations.Test;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.model.TestCaseResult;
/**
 * TestNG test listener <br>
 * <br>
 *
 * TestNG stuff: <blockquote>
 *
 * TestNG runs an xml file which has one "suite". <br>
 * "suite" consists of one or more "test". <br>
 * <br>
 *
 * "suite" events are provided by ISuiteListener. <br>
 * "test" events are provided by TestListenerAdapter.
 */
@PublicAtsApi
public class AtsTestngTestListener extends TestListenerAdapter {

    /*
     * skip checking whether ActiveDbAppender is attached in order for test execution to proceed
     * Note that additional check in each of the methods check once again whether that appender is attached
     * */
    private static final AtsDbLogger logger                                = AtsDbLogger.getLogger("com.axway.ats", true);

    private static final String      MSG__TEST_PASSED                      = "[TestNG]: TEST PASSED";

    private static final String      MSG__TEST_FAILED                      = "[TestNG]: TEST FAILED";

    private static final String      MSG__TEST_SKIPPED_DEPENDENCY          = "[TestNG]: TEST SKIPPED due to dependency failure";

    private static final String      MSG__TEST_SKIPPED_CONFIGURATION       = "[TestNG]: TEST SKIPPED due to configuration failure";

    private static final String      MSG__TEST_SKIPPED_UNRECOGNIZED_REASON = "[TestNG]: TEST SKIPPED due to unrecognized failure";

    private final String             JAVA_FILE_EXTENSION                   = ".java";

    private String                   javaFileContent;
    private String                   projectSourcesFolder;

    private String                   lastTestName;
    private static String            lastSuiteName;                                                                                // used when we work with not patched TestNG

    public void resetTempData() {

        lastTestName = null;
        lastSuiteName = null;
    }

    /* (non-Javadoc)
     * @see org.testng.TestListenerAdapter#onTestStart(org.testng.ITestResult)
     */
    @Override
    public void onTestStart( ITestResult testResult ) {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        Class<?> testClass = testResult.getTestClass().getRealClass();

        String suiteFullName = testClass.getName();
        String suiteSimpleName = testClass.getSimpleName();

        String testName = getTestName(testResult);
        String testInputArguments = getTestInputArguments(testResult);
        lastTestName = testResult.getMethod().toString();
        String testDescription = getTestDescription(testClass, suiteFullName, testName, testResult);

        // check if patched testNG is used and start or end suite if needed
        if (AtsTestngClassListener.getLastSuiteName() == null) {
            if (!suiteFullName.equals(lastSuiteName)) { // new suite
                if (lastSuiteName != null) {
                    logger.endSuite();
                }
                /* if the TestNG tests are presented in the default package
                 * set the package name to 'default'
                 */
                String packageName = null;
                if (testClass.getPackage() != null) {
                    packageName = testClass.getPackage().getName();
                } else {
                    packageName = "default";
                }
                logger.startSuite(packageName, suiteSimpleName);
            }
        }
        lastSuiteName = suiteFullName;

        // start test case
        logger.startTestcase(suiteFullName, suiteSimpleName, testName, testInputArguments, testDescription);
        addScenarioMetainfo(testResult);

        // send TestStart event to all ATS agents
        TestcaseStateEventsDispacher.getInstance().onTestStart();

        String testStartMessage = "[TestNG]: Starting " + suiteFullName + "@" + testName + testInputArguments;
        int passedRuns = RetryAnalyzer.getNumberPassedRuns();
        if (passedRuns < 1) {
            logger.info(testStartMessage);
        } else {
            logger.warn(testStartMessage + " for " + (passedRuns + 1) + " time");
        }

        super.onTestStart(testResult);
    }

    /* (non-Javadoc)
     * @see org.testng.TestListenerAdapter#onTestFailure(org.testng.ITestResult)
     */
    @Override
    public void onTestFailure( ITestResult testResult ) {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        try {
            //Check if the test was successfully started, if not - make it started and then end it with failure
            String testName = testResult.getMethod().toString();
            if (!testName.equals(lastTestName)) {
                onTestStart(testResult);
            }

            sendTestEndEventToAgents();

            // if this is an assertion error, we need to log it
            Throwable failureException = testResult.getThrowable();
            if (failureException instanceof AssertionError) {
                if (failureException.getMessage() != null) {
                    logger.error(failureException.getMessage());
                } else {
                    logger.error("Received java.lang.AssertionError with null message");
                }
            } else {
                logger.error(MSG__TEST_FAILED, testResult.getThrowable());
            }

            // end test case
            logger.endTestcase(TestCaseResult.FAILED);
        } catch (Exception e) {
            logger.fatal("UNEXPECTED EXCEPTION IN AtsTestngTestListener@onTestFailure", e);
        }
        super.onTestFailure(testResult);
    }

    /* (non-Javadoc)
     * @see org.testng.TestListenerAdapter#onTestSuccess(org.testng.ITestResult)
     */
    @Override
    public void onTestSuccess( ITestResult testResult ) {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        sendTestEndEventToAgents();
        boolean shouldTestFail = TestcaseStateEventsDispacher.getInstance().hasAnyQueueFailed();
        if (shouldTestFail) {
            logger.warn("At least one queue in test failed");
            testResult.setStatus(ITestResult.FAILURE);
            onTestFailure(testResult);
            return;
        }
        logger.info(MSG__TEST_PASSED);

        try {
            // end test case
            logger.endTestcase(TestCaseResult.PASSED);
        } catch (Exception e) {
            logger.fatal("UNEXPECTED EXCEPTION IN AtsTestngTestListener@onTestSuccess", e);
        }
        super.onTestSuccess(testResult);
    }

    /* (non-Javadoc)
     * @see org.testng.TestListenerAdapter#onTestSkipped(org.testng.ITestResult)
     */
    @Override
    public void onTestSkipped( ITestResult testResult ) {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        //Check if the test was successfully started, if not - make it started and then end it with failure
        String testName = testResult.getMethod().toString();
        if (!testName.equals(lastTestName)) {
            onTestStart(testResult);
        }

        sendTestEndEventToAgents();

        if (configurationError()) {
            // test is skipped due to configuration error
            logger.info(MSG__TEST_SKIPPED_CONFIGURATION, testResult.getThrowable());
        } else if (dependencyError(testResult)) {
            // test is skipped due to dependency error
            logger.info(MSG__TEST_SKIPPED_DEPENDENCY, testResult.getThrowable());
        } else {
            // we do not know the exact problem
            logger.fatal(MSG__TEST_SKIPPED_UNRECOGNIZED_REASON, testResult.getThrowable());
        }

        // end test case
        logger.endTestcase(TestCaseResult.SKIPPED);

        super.onTestSkipped(testResult);
    }

    private boolean dependencyError( ITestResult testResult ) {

        String[] dependentMethods = testResult.getMethod().getMethodsDependedUpon();
        List<ITestResult> failedTests = getFailedTests();
        for (String dependentMethod : dependentMethods) {
            for (ITestResult failedTestResult : failedTests) {
                String failedMethodName = new StringBuilder().append(failedTestResult.getTestClass()
                                                                                     .getName())
                                                             .append(".")
                                                             .append(failedTestResult.getName())
                                                             .toString();
                if (failedMethodName.equals(dependentMethod)) {
                    logger.error("Dependent method '" + dependentMethod + "' failed!",
                                 failedTestResult.getThrowable());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean configurationError() {

        // check if this is a configuration issue
        List<ITestResult> failedConfigurations = getConfigurationFailures();
        for (ITestResult failedResult : failedConfigurations) {
            if (failedResult.getThrowable() != null) {
                logger.fatal("Configuration failed!", failedResult.getThrowable());
                return true;
            }
        }

        return false;
    }

    private String getTestName( ITestResult result ) {

        String testName = result.getName();

        // check if there is a description annotation and get the test name
        Method testCaseMethod = result.getMethod().getConstructorOrMethod().getMethod();
        Description testCaseDescription = testCaseMethod.getAnnotation(Description.class);
        if (testCaseDescription != null && testCaseDescription.name().length() > 0) {
            testName = testCaseDescription.name();
        }

        return testName;
    }

    private String getTestInputArguments( ITestResult result ) {

        StringBuilder inputArguments = new StringBuilder();

        Object[] inputArgs = result.getParameters();
        inputArguments.append("( ");
        if (inputArgs != null && inputArgs.length > 0) {
            for (Object inputArg : inputArgs) {
                if (inputArg == null) {
                    inputArguments.append("null");
                } else {
                    inputArguments.append(inputArg.toString());
                }
                inputArguments.append(", ");
            }
            inputArguments.delete(inputArguments.length() - 2, inputArguments.length() - 1); //removing the last comma
        }
        inputArguments.append(")");

        return inputArguments.toString();
    }

    private String getTestDescription( Class<?> testClass, String suiteName, String testName,
                                       ITestResult result ) {

        // Look for a test description
        Method testCaseMethod = result.getMethod().getConstructorOrMethod().getMethod();

        // 1. in ATS @Description annotation
        Description atsDescription = testCaseMethod.getAnnotation(Description.class);
        if (atsDescription != null && atsDescription.description().length() > 0) {
            return atsDescription.description();
        }

        // 2. TestNG @Test annotation
        Test testngDescription = testCaseMethod.getAnnotation(Test.class);
        if (testngDescription != null && testngDescription.description().length() > 0) {
            return testngDescription.description();
        }

        // 3. Javadoc for this test method
        if (lastSuiteName == null || !suiteName.equals(lastSuiteName)) {
            saveJavaFileContent(testClass);
        }
        if (javaFileContent != null) {
            return parseFileForJavadoc(javaFileContent, testName);
        }

        return null;
    }

    /**
     * Add some meta info about this scenario.
     * This info is supposed to come from the method's java annotations
     * 
     * @param testResult
     */
    private void addScenarioMetainfo( ITestResult testResult ) {

        // Add TestNG groups as meta info, they come from the @Test annotation
        Method testCaseMethod = testResult.getMethod().getConstructorOrMethod().getMethod();

        Test testAnnotation = testCaseMethod.getAnnotation(Test.class);
        if (testAnnotation != null) {

            // first clear all existing meta info
            logger.clearScenarioMetainfo();

            // then add the new meta info
            String[] groups = testAnnotation.groups();
            if (groups != null && groups.length > 0) {
                for (String group : groups) {
                    logger.addScenarioMetainfo("group", group);
                }
            }
        }
    }

    private void sendTestEndEventToAgents() {

        if (ActiveDbAppender.getCurrentInstance() != null) {
            // send TestEnd event to all ATS agents
            TestcaseStateEventsDispacher.getInstance().onTestEnd();
        }
    }

    /**
     * Save java file content into string variable
     *
     * @param testClass
     */
    private void saveJavaFileContent( Class<?> testClass ) {

        String suiteName = testClass.getName();
        String javaFileName = suiteName.replace('.', '/') + JAVA_FILE_EXTENSION;
        String sourceFolderLocation = "jar";
        InputStream fileStream;

        try {
            fileStream = testClass.getClassLoader().getResourceAsStream(javaFileName); // if source is also copied in classpath (i.e. next to class file)
            if (fileStream != null) {
                javaFileContent = IoUtils.streamToString(fileStream);

                return;
            } else {
                sourceFolderLocation = AtsSystemProperties.getPropertyAsString(AtsSystemProperties.TEST_HARNESS__TESTS_SOURCE_LOCATION);
                if (sourceFolderLocation == null) {
                    Map<String, String> envMap = System.getenv();
                    sourceFolderLocation = envMap.get(AtsSystemProperties.TEST_HARNESS__TESTS_SOURCE_LOCATION);
                }
                if (sourceFolderLocation == null) {
                    if (projectSourcesFolder != null) {
                        sourceFolderLocation = projectSourcesFolder;
                    } else {

                        URI uri = new URI(testClass.getClassLoader().getResource(".").getPath());
                        URI parentUri = uri;
                        String pathToMainFolder = "src/main/java";
                        String pathToTestFolder = "src/test/java";

                        for (int i = 3; i > 0; i--) {//we try maximum 3 level up in the directory

                            parentUri = parentUri.resolve("..");
                            if (new File(parentUri + "src/").exists()) {
                                break;
                            }
                        }

                        String filePath = parentUri.toString() + pathToTestFolder + "/" + javaFileName;
                        File javaFile = new File(filePath);

                        if (javaFile.exists()) {
                            sourceFolderLocation = parentUri + pathToTestFolder;
                            projectSourcesFolder = pathToTestFolder;
                        } else {
                            filePath = parentUri.toString() + pathToMainFolder + "/" + javaFileName;
                            javaFile = new File(filePath);

                            if (javaFile.exists()) {
                                sourceFolderLocation = parentUri + pathToMainFolder;
                                projectSourcesFolder = sourceFolderLocation;
                            }
                        }
                        logger.debug("Source location is set to : " + projectSourcesFolder);
                    }
                }
            }
            // We may also search the Java file with full package path in ./src/main/java and ./src/test/java
            if (sourceFolderLocation != null) {

                javaFileContent = IoUtils.streamToString(new FileInputStream(sourceFolderLocation + "/"
                                                                             + javaFileName));
            } else {
                logger.debug(AtsSystemProperties.TEST_HARNESS__TESTS_SOURCE_LOCATION
                             + " variable is wrong or not set");
            }
        } catch (Exception e) {
            logger.error("File " + javaFileName + " was not found in " + sourceFolderLocation, e);
        }
    }

    /**
     * Search for a javadoc of a test method in a specific file
     *
     * @param javaFileContent  the java file content
     * @param testName the name of the test
     * @return the discovered javadoc content
     */
    public static String parseFileForJavadoc( String javaFileContent, String testName ) {

        BufferedReader reader = null;
        Deque<String> fileChunk = new ArrayDeque<String>(20);
        Pattern p = Pattern.compile("\\s*public\\s+void\\s+" + testName + "\\(.*");
        Matcher m;

        try {
            reader = new BufferedReader(new StringReader(javaFileContent));
            String line;
            while ( (line = reader.readLine()) != null) {
                m = p.matcher(line);
                if (m.matches()) {
                    // method found
                    String javadoc = getJavadoc(fileChunk);
                    if (javadoc != null) {
                        return javadoc;
                    } else {
                        return null;
                    }
                }
                fileChunk.add(line);
            }
        } catch (Exception e) {
            logger.error("Unable to obtain the javadoc for " + testName, e);
        } finally {
            IoUtils.closeStream(reader, null);
        }
        return null;
    }

    /**
    * Search for javadoc in the pointed selection from the java file
    *
    * @param fileChunk pointed selection from the java file
    * @return the test javadoc
    */
    private static String getJavadoc( Deque<String> fileChunk ) {

        try {
            StringBuilder javadoc = new StringBuilder();
            boolean javadocFound = false;
            while (!fileChunk.isEmpty()) {
                String line = fileChunk.pollLast().trim();
                if (!javadocFound && !line.startsWith("@") && !line.endsWith("*/")) {
                    return null;
                }
                if (line.endsWith("*/")) {
                    javadocFound = true;
                } else if (javadocFound) {
                    if (line.startsWith("*")) {
                        line = line.substring(1);
                    } else if (line.startsWith("/**")) {
                        return javadoc.toString();
                    }
                    if ("(non-Javadoc)".equals(line)) {
                        return null;
                    }
                    javadoc.insert(0, line + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                }
            }
        } catch (Exception e) {
            logger.error("Exception occured during parsing for javadoc", e);
        }
        return null;
    }

    /**
     * This is used in AtsTestngSuiteListener.onFinish(), to clear the lastSuiteName (lastSuiteName will be null after this method)
     * 
     * @param lastSuiteName the new value for lastSuiteName
     */
    public static void resetLastSuiteName() {

        AtsTestngTestListener.lastSuiteName = null;
    }
}