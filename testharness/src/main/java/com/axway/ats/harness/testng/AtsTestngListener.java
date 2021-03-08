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
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener2;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.ClasspathUtils;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.harness.config.CommonConfigurator;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.model.TestCaseResult;

public class AtsTestngListener implements ISuiteListener, IInvokedMethodListener2 {

    /** Skip checking whether ActiveDbAppender is attached.
     *  This is done in order to enable execution of tests when that appender is not attached/presented
     * */
    private static final AtsDbLogger      logger                                = AtsDbLogger.getLogger("com.axway.ats",
                                                                                                        true);

    private static final String           MSG__TEST_PASSED                      = "[TestNG]: TEST PASSED";
    private static final String           MSG__TEST_FAILED                      = "[TestNG]: TEST FAILED";
    private static final String           MSG__TEST_START                       = "[TESTNG]: Start";
    private static final String           MSG__TEST_END                         = "[TESTNG]: End";
    private static final String           MSG__TEST_SKIPPED_DEPENDENCY          = "[TestNG]: TEST SKIPPED due to dependency failure";
    private static final String           MSG__TEST_SKIPPED_CONFIGURATION       = "[TestNG]: TEST SKIPPED due to configuration failure";
    private static final String           MSG__TEST_SKIPPED_UNRECOGNIZED_REASON = "[TestNG]: TEST SKIPPED due to unrecognized failure";

    private final String                  JAVA_FILE_EXTENSION                   = ".java";

    private String                        javaFileContent;
    private String                        projectSourcesFolder;

    /* keeps track if the current testcase name */
    private String                        currentTestcaseName                   = null;
    /* keeps track if the current suite name. Not parallel mode safe */
    private static String                 currentSuiteName                      = null;

    /* keeps track of the test result for the last ended testcase */
    private int                           lastTestcaseResult                    = -1;

    private static boolean                testDescAvailable                     = false;
    private static boolean                afterInvocation                       = false;

    /**
     * Whether traces for listener's run events are enabled. Enabled via {@link AtsSystemProperties#LOG__CACHE_EVENTS_SOURCE_LOCATION}
     */
    private final static boolean          IS_TRACE_ENABLED;
    private final static AtsConsoleLogger ATS_LOGGER;

    static {
        IS_TRACE_ENABLED = AtsSystemProperties.getPropertyAsBoolean(
                                                                    AtsSystemProperties.LOG__CACHE_EVENTS_SOURCE_LOCATION,
                                                                    false);
        ATS_LOGGER = new AtsConsoleLogger(AtsTestngListener.class);
    }

    public AtsTestngListener() {

        ActiveDbAppender.isBeforeAndAfterMessagesLoggingSupported = true;
        if (IS_TRACE_ENABLED) {
            ATS_LOGGER.log(this.hashCode() + ": New ATS TestNG listener instance is created. Exception will follow",
                           new Exception("ATS TestNG listener initialization trace"));
        }
    }

    @Override
    public void beforeInvocation( IInvokedMethod method, ITestResult testResult ) {

    }

    @Override
    public void afterInvocation( IInvokedMethod method, ITestResult testResult ) {

    }

    @Override
    public void beforeInvocation( IInvokedMethod method, ITestResult testResult, ITestContext context ) {

        afterInvocation = false;

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (method.isConfigurationMethod()) { // check if method is @BeforeXXX or @AfterXXX

            if (method.getTestMethod().isBeforeSuiteConfiguration()) {// check if method is @BeforeSuite
                handleBeforeSuite(method, testResult, afterInvocation);

            } else if (method.getTestMethod().isBeforeClassConfiguration()) { // check if method is @BeforeClass

                handleBeforeClass(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isBeforeTestConfiguration()) {// check if method is @BeforeTest

                handleBeforeTest(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isBeforeMethodConfiguration()) {// check if method is @BeforeMethod

                handleBeforeMethod(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isAfterMethodConfiguration()) { // check if method is @AfterMethod

                handleAfterMethod(method, testResult, context, afterInvocation);
            } else if (method.getTestMethod().isAfterClassConfiguration()) { // check if method is @AfterClass

                handleAfterClass(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isAfterTestConfiguration()) { // check if method is @AfterTest

                handleAfterTest(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isAfterSuiteConfiguration()) { // check if method is @AfterSuite

                handleAfterSuite(method, testResult, afterInvocation);
            }

        } else if (method.isTestMethod()) { // check if method is not @BeforeXXX or @AfterXXX

            if (method.getTestMethod().isTest()) { // check if method is @Test

                handleTestMethod(method, testResult, context, afterInvocation);
            }
        }
    }

    @Override
    public void afterInvocation( IInvokedMethod method, ITestResult testResult, ITestContext context ) {

        afterInvocation = true;

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (method.isConfigurationMethod()) { // check if method is @BeforeXXX or @AfterXXX

            if (method.getTestMethod().isBeforeSuiteConfiguration()) {// check if method is @BeforeSuite

                handleBeforeSuite(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isBeforeClassConfiguration()) { // check if method is @BeforeClass

                handleBeforeClass(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isBeforeTestConfiguration()) {// check if method is @BeforeTest

                handleBeforeTest(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isBeforeMethodConfiguration()) {// check if method is @BeforeMethod

                handleBeforeMethod(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isAfterMethodConfiguration()) { // check if method is @AfterMethod

                handleAfterMethod(method, testResult, context, afterInvocation);
            } else if (method.getTestMethod().isAfterClassConfiguration()) { // check if method is @AfterClass

                handleAfterClass(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isAfterTestConfiguration()) { // check if method is @AfterTest
                handleAfterTest(method, testResult, afterInvocation);

            } else if (method.getTestMethod().isAfterSuiteConfiguration()) { // check if method is @AfterSuite

                handleAfterSuite(method, testResult, afterInvocation);
            }
        } else if (method.isTestMethod()) {

            if (method.getTestMethod().isTest()) { // check if method is @Test

                handleTestMethod(method, testResult, context, afterInvocation);
            }
        }
    }

    @Override
    public void onStart( ISuite suite ) {

        if (IS_TRACE_ENABLED) {
            ATS_LOGGER.log(this.hashCode() + ": TestNG start run (suite) event received. Run name: " + suite.getName());
            if (suite instanceof XmlSuite) {
                XmlSuite xmlSuite = (XmlSuite) suite;
                ATS_LOGGER.log(this.hashCode() + ": Suite file: " + xmlSuite.getFileName()
                               + ": Suite files: " + xmlSuite.getSuiteFiles().toString());
            }
        }

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        // get the run name specified by the user
        String runName = CommonConfigurator.getInstance().getRunName();
        if (runName.equals(CommonConfigurator.DEFAULT_RUN_NAME)) {
            // the user did not specify a run name, use the one from TestNG
            runName = suite.getName();
        }

        // start a new run
        String hostNameIp;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostNameIp = addr.getHostName() + "/" + addr.getHostAddress();

        } catch (UnknownHostException uhe) {
            hostNameIp = null;
        }

        logger.startRun(runName, CommonConfigurator.getInstance().getOsName(),
                        CommonConfigurator.getInstance().getProductName(),
                        CommonConfigurator.getInstance().getVersionName(),
                        CommonConfigurator.getInstance().getBuildName(), hostNameIp);

        logSystemInformation();
        logClassPath();

    }

    @Override
    public void onFinish( ISuite suite ) {

        if (IS_TRACE_ENABLED) {
            ATS_LOGGER.log(this.hashCode() + ": End run event received", new Exception("Debugging trace"));
        }

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (currentSuiteName != null) {

            endSuite();
        }

        // end the run
        logger.endRun();

    }

    private void logSystemInformation() {

        StringBuilder systemInformation = new StringBuilder();

        appendMessage(systemInformation, "ATS version: '", AtsVersion.getAtsVersion());
        appendMessage(systemInformation, " os.name: '", System.getProperty("os.name"));
        appendMessage(systemInformation, " os.arch: '", System.getProperty("os.arch"));
        appendMessage(systemInformation, " java.version: '", System.getProperty("java.version"));
        appendMessage(systemInformation, " java.home: '", System.getProperty("java.home"));

        List<String> ipList = new ArrayList<>();
        for (InetAddress ip : HostUtils.getAllIpAddresses()) {
            ipList.add(ip.getHostAddress());
        }

        appendMessage(systemInformation, " IP addresses: '", ipList.toString());

        logger.info("System information : " + systemInformation.toString());
    }

    private void logClassPath() {

        // print JVM classpath if user has enabled it
        if (AtsSystemProperties.getPropertyAsBoolean(AtsSystemProperties.LOG__CLASSPATH_ON_START, false)) {

            StringBuilder classpath = new StringBuilder();

            classpath.append(" Test Executor classpath on \"");
            classpath.append(HostUtils.getLocalHostIP());
            classpath.append("\" : \n");
            classpath.append(new ClasspathUtils().getClassPathDescription());

            logger.info(classpath, true);
        }
    }

    private void appendMessage( StringBuilder message, String valueDesc, String value ) {

        if (!StringUtils.isNullOrEmpty(value)) {
            if (message.length() > 0) {
                message.append(",");
            }
            message.append(valueDesc + value + "'");
        }
    }

    private void startSuite( ITestResult testResult ) {

        Class<?> testClass = testResult.getTestClass().getRealClass();

        String suiteSimpleName = testClass.getSimpleName();

        /* if the TestNG tests are presented in the default package
         * set the package name to 'default'
         */
        String packageName = (testClass.getPackage() != null)
                                                              ? testClass.getPackage().getName()
                                                              : "default";

        // clear the previously saved java file content, since a new suite is about to start
        javaFileContent = null;

        currentSuiteName = suiteSimpleName;//testResult.getTestClass().getRealClass().getName();

        logger.startSuite(packageName, suiteSimpleName);

    }

    private void startTestcase( ITestResult testResult ) {

        Class<?> testClass = testResult.getTestClass().getRealClass();

        String suiteFullName = testClass.getName();
        String suiteSimpleName = testClass.getSimpleName();

        String testName = getTestName(testResult);
        String testInputArguments = getTestInputArguments(testResult);
        String testDescription = getTestDescription(testClass, suiteFullName, testName, testResult);

        // save the current testcase name
        currentTestcaseName = testResult.getMethod().toString();
        //clear the last saved testcase result, since a new testcase is about to start
        lastTestcaseResult = -1;

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
        if (testngDescription != null) {
            if (testngDescription.description().length() > 0) {
                return testngDescription.description();
            }
            // 3. Javadoc for this test method
            if (javaFileContent == null) {
                saveJavaFileContent(testClass);
            }
            if (javaFileContent != null) {
                return parseFileForJavadoc(javaFileContent, testName);
            }
        }

        return null;
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
            fileStream = testClass.getClassLoader()
                                  .getResourceAsStream(
                                                       javaFileName); // if source is also copied in classpath (i.e. next to class file)
            if (fileStream != null) {
                javaFileContent = IoUtils.streamToString(fileStream);

                return;
            } else {
                sourceFolderLocation = AtsSystemProperties.getPropertyAsString(
                                                                               AtsSystemProperties.TEST_HARNESS__TESTS_SOURCE_LOCATION);
                if (sourceFolderLocation == null) {
                    Map<String, String> envMap = System.getenv();
                    sourceFolderLocation = envMap.get(AtsSystemProperties.TEST_HARNESS__TESTS_SOURCE_LOCATION);
                }
                if (sourceFolderLocation == null) {
                    if (projectSourcesFolder != null) {
                        sourceFolderLocation = projectSourcesFolder;
                    } else {

                        if (!testDescAvailable) {
                            URL testClassPath = testClass.getClassLoader()
                                                         .getResource(
                                                                      "."); // this could be null when failsafe maven plugin is used
                            if (testClassPath == null) {
                                testDescAvailable = true;
                                logger.info(
                                            "Test descriptions could not be assigned to the tests, because the test sources folder could not be found. ");

                                return;
                            }
                            URI uri = new URI(testClassPath.getPath());
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
    private static String parseFileForJavadoc( String javaFileContent, String testName ) {

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

    private void updateTestcase( ITestResult testResult ) {

        Class<?> testClass = testResult.getTestClass().getRealClass();

        String suiteFullName = testClass.getName();
        String suiteSimpleName = testClass.getSimpleName();
        String testName = getTestName(testResult);
        String testInputArguments = getTestInputArguments(testResult);
        String testDescription = getTestDescription(testClass, suiteFullName, testName, testResult);

        currentTestcaseName = testResult.getMethod().toString();

        logger.info("[TESTNG]: Start @Test '" + testResult.getTestClass().getRealClass() + "@"
                    + testResult.getName() + "'");

        // by passing -1, the DbEventRequestProcessor will decide the testcasseId
        logger.updateTestcase(-1, suiteFullName, suiteSimpleName, testName, testInputArguments,
                              testDescription, lastTestcaseResult);

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

    private void endTestcaseWithSkipStatus( ITestResult testResult, ITestContext context ) {

        //Check if the test was successfully started, if not - make it started and then end it with failure
        String testName = testResult.getMethod().toString();
        if (!testName.equals(currentTestcaseName)) {
            startTestcase(testResult);
        }

        sendTestEndEventToAgents();

        if (configurationError(context)) {
            // test is skipped due to configuration error
            logger.info(MSG__TEST_SKIPPED_CONFIGURATION, testResult.getThrowable());
        } else if (dependencyError(testResult, context)) {
            // test is skipped due to dependency error
            logger.info(MSG__TEST_SKIPPED_DEPENDENCY, testResult.getThrowable());
        } else {
            // we do not know the exact problem
            logger.fatal(MSG__TEST_SKIPPED_UNRECOGNIZED_REASON, testResult.getThrowable());
        }

        currentTestcaseName = null;
        lastTestcaseResult = TestCaseResult.SKIPPED.toInt();
        // end test case
        logger.endTestcase(TestCaseResult.SKIPPED);

    }

    private void endTestcaseWithFailureStatus( ITestResult testResult ) {

        try {
            //Check if the test was successfully started, if not - make it started and then end it with failure
            String testName = testResult.getMethod().toString();
            if (!testName.equals(currentTestcaseName)) {
                startTestcase(testResult);
            }

            sendTestEndEventToAgents();

            // if this is an assertion error, we need to log it
            Throwable failureException = testResult.getThrowable();
            if (failureException instanceof AssertionError) {
                if (failureException.getMessage() != null) {
                    logger.error(ExceptionUtils.getExceptionMsg(failureException));
                } else {
                    logger.error("Received java.lang.AssertionError with null message");
                }
            } else {
                logger.error(MSG__TEST_FAILED, testResult.getThrowable());
            }

            currentTestcaseName = null;
            lastTestcaseResult = TestCaseResult.FAILED.toInt();
            // end test case
            logger.endTestcase(TestCaseResult.FAILED);
        } catch (Exception e) {
            logger.fatal("UNEXPECTED EXCEPTION IN AtsTestngListener@endTestcaseWithFailureStatus", e);
        }

    }

    private void endTestcaseWithSuccessStatus( ITestResult testResult ) {

        sendTestEndEventToAgents();
        boolean shouldTestFail = TestcaseStateEventsDispacher.getInstance().hasAnyQueueFailed();
        if (shouldTestFail) {
            logger.warn("At least one queue in test failed");
            testResult.setStatus(ITestResult.FAILURE);
            endTestcaseWithFailureStatus(testResult);
            return;
        }
        logger.info(MSG__TEST_PASSED);

        try {

            currentTestcaseName = null;
            lastTestcaseResult = TestCaseResult.PASSED.toInt();
            // end test case
            logger.endTestcase(TestCaseResult.PASSED);
        } catch (Exception e) {
            logger.fatal("UNEXPECTED EXCEPTION IN AtsTestngListener@endTestcaseWithSuccessStatus", e);
        }

    }

    private boolean dependencyError( ITestResult testResult, ITestContext context ) {

        String[] dependentMethods = testResult.getMethod().getMethodsDependedUpon();
        List<ITestResult> failedTests = Arrays.asList(context.getFailedTests()
                                                             .getAllResults()
                                                             .toArray(new ITestResult[context.getFailedTests()
                                                                                             .getAllResults()
                                                                                             .size()]));
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

    private boolean configurationError( ITestContext context ) {

        // check if this is a configuration issue
        List<ITestResult> failedConfigurations = Arrays.asList(context.getFailedConfigurations()
                                                                      .getAllResults()
                                                                      .toArray(
                                                                               new ITestResult[context.getFailedConfigurations()
                                                                                                      .getAllResults()
                                                                                                      .size()]));
        for (ITestResult failedResult : failedConfigurations) {
            if (failedResult.getThrowable() != null) {
                logger.fatal("Configuration failed!", failedResult.getThrowable());
                return true;
            }
        }

        return false;
    }

    private void sendTestEndEventToAgents() {

        if (ActiveDbAppender.getCurrentInstance() != null) {
            // send TestEnd event to all ATS agents
            TestcaseStateEventsDispacher.getInstance().onTestEnd();
        }
    }

    private void endSuite() {

        // end the current suite
        currentSuiteName = null;
        logger.endSuite();

    }

    private void logCondition( IInvokedMethod method, ITestResult testResult, String condition ) {

        logger.info(condition + " '" + testResult.getTestClass().getName() + "@"
                    + method.getTestMethod().getMethodName() + "'");
    }

    private void handleBeforeSuite( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation ) {

        if (!afterInvocation) {
            logCondition(method, testResult, MSG__TEST_START);
        } else {
            logCondition(method, testResult, MSG__TEST_END);
        }
    }

    private void handleBeforeClass( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation ) {

        if (!afterInvocation) {
            if (currentSuiteName == null) {
                // start suite
                startSuite(testResult);

            } else if (!currentSuiteName.equals(testResult.getTestClass()
                                                          .getRealClass()
                                                          .getSimpleName())) {
                                                              endSuite(); // end previously started suite
                                                              startSuite(testResult); // start new suite
                                                          }
            logCondition(method, testResult, MSG__TEST_START);
        } else {
            logCondition(method, testResult, MSG__TEST_END);

        }
    }

    private void handleBeforeTest( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation ) {

        if (!afterInvocation) {
            logCondition(method, testResult, MSG__TEST_START);
        } else {
            logCondition(method, testResult, MSG__TEST_END);
        }
    }

    private void handleBeforeMethod( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation ) {

        if (!afterInvocation) {
            if (currentSuiteName == null) {

                // start suite
                startSuite(testResult);

            } else if (!currentSuiteName.equals(testResult.getTestClass()
                                                          .getRealClass()
                                                          .getSimpleName())) {

                                                              endSuite(); // end previously started suite
                                                              startSuite(testResult); // start new suite
                                                          }

            if (currentTestcaseName == null) {

                // start testcase
                startTestcase(testResult);
            }

            logCondition(method, testResult, MSG__TEST_START);

        } else {
            logCondition(method, testResult, MSG__TEST_END);
        }
    }

    private void handleAfterMethod( IInvokedMethod method, ITestResult testResult, ITestContext context,
                                    Boolean afterInvocation ) {

        if (!afterInvocation) {
            logger.startAfterMethod();

            logCondition(method, testResult, MSG__TEST_START);

        } else {
            if (currentTestcaseName != null) {

                if (testResult.getStatus() == ITestResult.SUCCESS) {

                    endTestcaseWithSuccessStatus(testResult);

                } else if (testResult.getStatus() == ITestResult.FAILURE) {

                    endTestcaseWithFailureStatus(testResult);

                } else if (testResult.getStatus() == ITestResult.SKIP) {

                    endTestcaseWithSkipStatus(testResult, context);
                }
            }

            if (lastTestcaseResult == TestCaseResult.PASSED.toInt()) {
                // the last testcase passed, but if the after method failed or was skipped,
                // the testcase should use the after methods result

                switch (testResult.getStatus()) {
                    case ITestResult.SUCCESS:
                        // the after method and the testcase has the same test result status,
                        // so do not change anything
                        break;
                    case ITestResult.FAILURE:
                        lastTestcaseResult = TestCaseResult.FAILED.toInt();
                        break;
                    case ITestResult.SKIP:
                        lastTestcaseResult = TestCaseResult.SKIPPED.toInt();
                        break;
                    default:
                        throw new RuntimeException("The result of the @AfterMethod is unsupported by ATS");
                }

            } else if (lastTestcaseResult == TestCaseResult.SKIPPED.toInt()) {
                // the testcase was skipped

                if (testResult.getStatus() == ITestResult.FAILURE) {
                    // change the testcase result, only if the after method had failed
                    lastTestcaseResult = TestCaseResult.FAILED.toInt();
                }

            } else if (lastTestcaseResult == TestCaseResult.FAILED.toInt()) {
                // do nothing, the testcase failed and a failed testcase should it be
            } else {
                // should not happen, as before reaching this part of the code, a testcase has to be ended
                // but, just in case, throw an Exception
                throw new RuntimeException(
                                           "It seems that there is no previously ended testcase. Last testcase result is '"
                                           + -1 + "', which is not a valid TestcaseResult value");
            }

            if (testResult.getStatus() == ITestResult.FAILURE) {

                // log the Throwable object from the @AfterMethod
                logger.fatal(testResult.getThrowable().getMessage(), testResult.getThrowable());

            }

            logCondition(method, testResult, MSG__TEST_END);

            logger.endAfterMethod();

            // set new end timestamp and result for the current testcase
            // by passing -1, the DbEventRequestProcessor will decide the testcasseId
            logger.updateTestcase(-1, null, null, null, null, null, lastTestcaseResult);

        }
    }

    private void handleAfterClass( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation ) {

        if (!afterInvocation) {
            if (currentSuiteName == null) {

                logger.startAfterClass();
            }
            logCondition(method, testResult, MSG__TEST_START);
        } else {

            logCondition(method, testResult, MSG__TEST_END);
            if (testResult.getStatus() == ITestResult.FAILURE) {

                // log the Throwable object from the @AfterClass
                logger.fatal(testResult.getThrowable().getMessage(), testResult.getThrowable());
            }
            if (currentSuiteName != null) {

                endSuite();

            } else {
                // the event was received after a suite is already ended
                // which means that we only have to clear the after class mode
                logger.endAfterClass();
            }

        }
    }

    private void handleAfterTest( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation ) {

        if (!afterInvocation) {
            logger.startAfterSuite();
            logCondition(method, testResult, MSG__TEST_START);
            logger.endAfterSuite();
        } else {
            logger.startAfterSuite();
            logCondition(method, testResult, MSG__TEST_END);

            if (testResult.getStatus() == ITestResult.FAILURE) {

                // log the Throwable object from the @AfterTest
                logger.fatal(testResult.getThrowable().getMessage(), testResult.getThrowable());
            }
            logger.endAfterSuite();
        }
    }

    private void handleAfterSuite( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation ) {

        if (!afterInvocation) {
            logger.startAfterSuite();
            logCondition(method, testResult, MSG__TEST_START);
        } else {
            if (testResult.getStatus() == ITestResult.FAILURE) {

                // log the Throwable object from the @AfterMethod
                logCondition(method, testResult, MSG__TEST_END);

                logger.fatal(testResult.getThrowable().getMessage(), testResult.getThrowable());
            }

            logger.endAfterSuite();
        }
    }

    private void handleTestMethod( IInvokedMethod method, ITestResult testResult, ITestContext context,
                                   Boolean afterInvocation ) {

        if (!afterInvocation) {
            if (currentSuiteName == null) {

                // start suite
                startSuite(testResult);

            } else if (!currentSuiteName.equals(testResult.getTestClass()
                                                          .getRealClass()
                                                          .getSimpleName())) {

                                                              endSuite(); // end previously started suite
                                                              startSuite(testResult); // start new suite
                                                          }

            if (currentTestcaseName == null) {

                // start testcase
                startTestcase(testResult);
            } else {

                // update testcase
                updateTestcase(testResult);
            }
        } else {
            if (testResult.getStatus() == ITestResult.SUCCESS) {

                endTestcaseWithSuccessStatus(testResult);

            } else if (testResult.getStatus() == ITestResult.FAILURE) {

                endTestcaseWithFailureStatus(testResult);

            } else if (testResult.getStatus() == ITestResult.SKIP) {

                endTestcaseWithSkipStatus(testResult, context);
            }
        }
    }
}