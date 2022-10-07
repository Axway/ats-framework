/*
 * Copyright 2017-2022 Axway Software
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.log.appenders.AbstractDbAppender;
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
     *  This is done in order to enable execution of tests when that appender is not attached
     * to the log4j's RootLogger
     */
    private static final AtsDbLogger      logger             = AtsDbLogger.getLogger("com.axway.ats",
                                                                                                        true);
    /**
     * Whether traces for listener's run events are enabled. Enabled via {@link AtsSystemProperties#LOG__CACHE_EVENTS_SOURCE_LOCATION}
     */
    private final static boolean          IS_TRACE_ENABLED;
    private final static AtsConsoleLogger ATS_LOGGER;

    private static final String MSG__TEST_PASSED             = "[TestNG]: TEST PASSED";
    private static final String MSG__TEST_FAILED             = "[TestNG]: TEST FAILED";
    private static final String MSG__START                   = "[TESTNG]: Start";
    private static final String MSG__END                     = "[TESTNG]: End";
    private static final String MSG__TEST_SKIPPED_DEPENDENCY = "[TestNG]: TEST SKIPPED due to dependency failure";
    private static final String MSG__TEST_SKIPPED_CONFIGURATION       = "[TestNG]: TEST SKIPPED due to configuration failure";
    private static final String MSG__TEST_SKIPPED_UNRECOGNIZED_REASON = "[TestNG]: TEST SKIPPED due to unrecognized failure";
    private static final String JAVA_FILE_EXTENSION                   = ".java";

    /** In parallel tests we do not know the test name in the before method
     * and all the tests are inserted in the database with the same name( the before method name )
     * So it should be added unique index after the before method name in order to distinguish the different test cases
     */
    private static Integer BEFORE_METHOD_INDEX = 0;

    // TODO inspp
    private static boolean testDescAvailable = false;
    private        boolean afterInvocation   = false;
    private        String  javaFileContent;
    private        String  projectSourcesFolder;

    static {
        IS_TRACE_ENABLED = AtsSystemProperties.getPropertyAsBoolean(
                                                                    AtsSystemProperties.LOG__CACHE_EVENTS_SOURCE_LOCATION,
                                                                    false);
        ATS_LOGGER = new AtsConsoleLogger(AtsTestngListener.class);
    }

    /** per thread ID Channels - test context state */
    private Map<String, Channel> channels;

    /** Keeps state for current suite (class), current test - caller, test result */
    private class Channel {

        /* keeps track of the current testcase name */
        String currentTestcaseName = null;

        /* keeps track of the current suite name */
        String currentSuiteName    = null;

        /* keeps track of the current testcase's end result and its context (context = TestNG information for the actual Java method that correlate with the testcase */
        ITestResult  currentTestcaseResult  = null;
        ITestContext currentTestcaseContext = null; // used to check for failed configurations for skipping test

        // the caller that created this channel, thread ID
        String       callerId               = null;

        // TODOs - possible extension with custom status like MANUAL
        // Enum/int lastTestcaseResult = PASSED, SKIPPED, FAILURE, MANUAL

        @Override
        public String toString() {
            return "Channel state. Suite " + currentSuiteName + ", testcase: " + currentTestcaseName + ", caller ID: " + callerId;
        }
    }

    /**
     * The types of TestNG methods that this listener is interested in.
     * <br>
     * Other types as @BeforeTest are still supported, but are of no interest to this listener/class
     * /
    private enum METHOD_TYPE {
        BEFORE_SUITE, BEFORE_CLASS, BEFORE_METHOD, TEST, AFTER_METHOD, AFTER_CLASS, AFTER_SUITE
    } */

    public AtsTestngListener() {

        ActiveDbAppender.isBeforeAndAfterMessagesLoggingSupported = true;
        if (IS_TRACE_ENABLED) {
            ATS_LOGGER.log(this.hashCode() + ": New ATS TestNG listener instance is created. Exception will follow",
                           new Exception("ATS TestNG listener initialization trace"));
        }
        channels = new ConcurrentHashMap<String, Channel>(); // ConcurrentHashMap instead of Collections.synchronizedMap(), check with testng.strict.parallel=true
    }

    @Override
    public void beforeInvocation( IInvokedMethod method, ITestResult testResult ) {
        // used method with 3 params
    }

    @Override
    public void afterInvocation( IInvokedMethod method, ITestResult testResult ) {
         // used method with 3 params
    }

    @Override
    public void beforeInvocation( IInvokedMethod method, ITestResult testResult, ITestContext context ) {

        afterInvocation = false;

        if (!ActiveDbAppender.isAttached) {
            return;
        }
        Channel channel = getChannel();

        if (method.isConfigurationMethod()) { // check if method is @BeforeXXX or @AfterXXX

            if (method.getTestMethod().isBeforeSuiteConfiguration()) { // check if method is @BeforeSuite

                handleBeforeSuite(method, testResult, afterInvocation, channel);
            } else if (method.getTestMethod().isBeforeClassConfiguration()) { // check if method is @BeforeClass

                handleBeforeClass(method, testResult, afterInvocation, channel);
            } else if (method.getTestMethod().isBeforeTestConfiguration()) { // check if method is @BeforeTest

                handleBeforeTest(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isBeforeMethodConfiguration()) { // check if method is @BeforeMethod

                handleBeforeMethod(method, testResult, afterInvocation, channel);
            } else if (method.getTestMethod().isAfterMethodConfiguration()) { // check if method is @AfterMethod

                handleAfterMethod(method, testResult, context, afterInvocation, channel);
            } else if (method.getTestMethod().isAfterClassConfiguration()) { // check if method is @AfterClass

                handleAfterClass(method, testResult, afterInvocation, channel);
            } else if (method.getTestMethod().isAfterTestConfiguration()) { // check if method is @AfterTest

                handleAfterTest(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isAfterSuiteConfiguration()) { // check if method is @AfterSuite

                handleAfterSuite(method, testResult, afterInvocation, channel);
            }
        } else if (method.isTestMethod()) { // check if method is not @BeforeXXX or @AfterXXX

            if (method.getTestMethod().isTest()) { // check if method is @Test
                handleTestMethod(method, testResult, context, afterInvocation, channel);
            }
        } else {
            // unsupported method
            System.err.println("Unsupported method for TestNG " + method.toString());
            //if (ATS_LOGGER. getLog4jLogger().isDebugEnabled())
        }
    }

    @Override
    public void afterInvocation( IInvokedMethod method, ITestResult testResult, ITestContext context ) {

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        afterInvocation = true;
        Channel channel = getChannel();

        if (method.isConfigurationMethod()) { // check if method is @BeforeXXX or @AfterXXX

            if (method.getTestMethod().isBeforeSuiteConfiguration()) {// check if method is @BeforeSuite

                handleBeforeSuite(method, testResult, afterInvocation, channel);
            } else if (method.getTestMethod().isBeforeClassConfiguration()) { // check if method is @BeforeClass

                handleBeforeClass(method, testResult, afterInvocation, channel);
            } else if (method.getTestMethod().isBeforeTestConfiguration()) {// check if method is @BeforeTest

                handleBeforeTest(method, testResult, afterInvocation);
            } else if (method.getTestMethod().isBeforeMethodConfiguration()) {// check if method is @BeforeMethod

                handleBeforeMethod(method, testResult, afterInvocation, channel);
            } else if (method.getTestMethod().isAfterMethodConfiguration()) { // check if method is @AfterMethod

                handleAfterMethod(method, testResult, context, afterInvocation, channel);
            } else if (method.getTestMethod().isAfterClassConfiguration()) { // check if method is @AfterClass

                handleAfterClass(method, testResult, afterInvocation, channel);
            } else if (method.getTestMethod().isAfterTestConfiguration()) { // check if method is @AfterTest
                handleAfterTest(method, testResult, afterInvocation);

            } else if (method.getTestMethod().isAfterSuiteConfiguration()) { // check if method is @AfterSuite

                handleAfterSuite(method, testResult, afterInvocation, channel);
            }
        } else if (method.isTestMethod()) {

            if (method.getTestMethod().isTest()) { // check if method is @Test
                handleTestMethod(method, testResult, context, afterInvocation, channel);
            }
        }
    }

    /**
     * Start new RUN. Inherited from {@link ISuiteListener}
     */
    @Override
    public void onStart( ISuite suite ) {

        if (IS_TRACE_ENABLED) {
            ATS_LOGGER.log(this.hashCode() + ": TestNG start suite (ATS run) event received. Run name: " + suite.getName());
            if (suite instanceof XmlSuite) {
                XmlSuite xmlSuite = (XmlSuite) suite;
                ATS_LOGGER.log(this.hashCode() + ": Suite file: " + xmlSuite.getFileName()
                               + ": Suite files: " + xmlSuite.getSuiteFiles().toString());
            }
        }

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        String pMode =  suite.getParallel();
        if (! (   XmlSuite.ParallelMode.NONE.name().equalsIgnoreCase(pMode)
               || XmlSuite.ParallelMode.FALSE.name().equalsIgnoreCase(pMode))) {
            AbstractDbAppender.parallel = true;
            ATS_LOGGER.info("TestNG parallel test execution mode in suite.xml is set to " + pMode);
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

    /**
     * End the RUN
     */
    @Override
    public void onFinish( ISuite suite ) {

        if (IS_TRACE_ENABLED) {
            ATS_LOGGER.log(this.hashCode() + ": End run event received", new Exception("Debugging trace"));
        }

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        /*
         * Close all testcases and suites for the current run
         * FIXME:
         * - Any message will be logged as a run message, so a message like 'INFO  14:12:03:893 main axway.ats: [TestNG]: TEST PASSED'
         *   will be logged as a RUN message instead of a TESTCASE one
         * */
        for (Channel channel : channels.values()) {
            // end testcase
            if (channel.currentTestcaseName != null) {
                ATS_LOGGER.warn("Closing not closed testcase " + channel.currentTestcaseName);
                endTestcase(channel);
            }

            // end suite
            if (channel.currentSuiteName != null) {
                ATS_LOGGER.warn("Closing suite " + channel.currentSuiteName);
                endSuite(channel);
            }
        }

        // clear channels for the ended run
        channels.clear();

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

    private void startSuite(Channel channel, ITestResult testResult ) {

        Class<?> testClass = testResult.getTestClass().getRealClass();
        String suiteSimpleName = testClass.getSimpleName();

        // if the TestNG tests are presented in the default package set the package name to 'default'
        String packageName = (testClass.getPackage() != null)
                                                              ? testClass.getPackage().getName()
                                                              : "default";

        // clear the previously saved java file content, since a new suite is about to start
        javaFileContent = null;

        channel.currentSuiteName = suiteSimpleName;//testResult.getTestClass().getRealClass().getName();

        logger.startSuite(packageName, suiteSimpleName);

    }

    private void startTestcase(Channel channel, ITestResult testResult, boolean isBeforeMethod ) {

        Class<?> testClass = testResult.getTestClass().getRealClass();

        String suiteFullName = testClass.getName();
        String suiteSimpleName = testClass.getSimpleName();

        String testName = getTestName(testResult);
        String testInputArguments = getTestInputArguments(testResult);
        String testDescription = getTestDescription(testClass, suiteFullName, testName, testResult);

        // save the current testcase name
        channel.currentTestcaseName = testResult.getMethod().toString();

        //clear the last saved testcase result, since a new testcase is about to start
        //channel.lastTestcaseResult = -1;

        // start test case
        if (isBeforeMethod) {
            // in parallel tests we do not know the test name in the before method
            // and all the tests are inserted in the database with the same name( the before method name )
            // we have to add unique index after the before method name, so we can distinguish the different test cases
            synchronized (BEFORE_METHOD_INDEX) {
                testName = testName + "_" + BEFORE_METHOD_INDEX++;
            }
        }

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
                                logger.info( "Test descriptions could not be assigned to the tests, because the test "
                                             + "sources folder could not be found. ");
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
            logger.error("Exception occurred during parsing for javadoc", e);
        }
        return null;
    }

    private void updateTestcase(Channel channel, ITestResult testResult ) {

        Class<?> testClass = testResult.getTestClass().getRealClass();

        String suiteFullName = testClass.getName();
        String suiteSimpleName = testClass.getSimpleName();
        String testName = getTestName(testResult);
        String testInputArguments = getTestInputArguments(testResult);
        String testDescription = getTestDescription(testClass, suiteFullName, testName, testResult);

        channel.currentTestcaseName = testResult.getMethod().toString();

        logger.info("[TESTNG]: Starting again @Test '" + testResult.getTestClass().getRealClass() + "@"
                    + testResult.getName() + "'");

        // by passing -1, the DbEventRequestProcessor will decide the testcaseId
        logger.updateTestcase(-1, suiteFullName, suiteSimpleName, testName, testInputArguments,
                              testDescription, TestCaseResult.RUNNING.toInt());

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

    /**
     * Ends testcase with SUCCESS, FAILURE or SKIP
     * @param channel  Specific channel/thread context in parallel execution
     */
    private void endTestcase(Channel channel/*, ITestResult testResult, ITestContext context */) {
        // send TestEnd event to all ATS agents
        TestcaseStateEventsDispacher.getInstance().onTestEnd(Arrays.asList(new String[]{ channel.callerId }));

        if (channel.currentTestcaseResult == null) {
            throw new RuntimeException("Could not close testcases '" + channel.currentTestcaseName
                                       + "'. Its has no testcase result");
        }

        // TODOs - change severity to DEBUG
        ITestResult testResult = channel.currentTestcaseResult;
        ATS_LOGGER.info("  About to end/close testcase " + channel.currentTestcaseName
                        + " with status " + testResult.getStatus());
        Class<?> testClass = channel.currentTestcaseResult.getTestClass().getRealClass();
        String suiteFullName = testClass.getName();
        String testName = getTestName(channel.currentTestcaseResult);
        String testInputArguments = getTestInputArguments(channel.currentTestcaseResult);
        String message = "End test case '" + suiteFullName + "@" + testName + testInputArguments + "'";

        // save the current testcase name
        //channel.currentTestcaseName = channel.currentTestcaseResult.getMethod().toString();

        switch (channel.currentTestcaseResult.getStatus()) {
            case ITestResult.SUCCESS:
                endTestcaseWithSuccessStatus(channel, message);
                break;
            case ITestResult.SKIP:
                endTestcaseWithSkipStatus(channel, message);
                break;
            case ITestResult.FAILURE:
                endTestcaseWithFailureStatus(channel, message);
                break;
            default:
                logger.error("Could not close testcase '" + channel.currentSuiteName + "@" + channel.currentTestcaseName
                             + "'. Unsupported test result status (" + channel.currentTestcaseResult.getStatus() + ")");
                break;
        }
    }

    private void endTestcaseWithSkipStatus(Channel channel, String endTestcaseEventMessage ) {

        //Check if the test was successfully started, if not - make it started and then end it with failure
        ITestResult testResult = channel.currentTestcaseResult;
        String testName = testResult.getMethod().toString();
        if (!testName.equals(channel.currentTestcaseName)) {
            startTestcase(channel, testResult, false);
        }

        sendTestEndEventToAgents(channel);

        // save the current testcase name
        //channel.currentTestcaseName = channel.currentTestcaseResult.getMethod().toString();

        if (configurationError(testResult.getTestContext())) {
            // test is skipped due to configuration error
            logger.info(MSG__TEST_SKIPPED_CONFIGURATION, testResult.getThrowable());
        } else if (dependencyError(testResult, testResult.getTestContext())) {
            // test is skipped due to dependency error
            logger.info(MSG__TEST_SKIPPED_DEPENDENCY, testResult.getThrowable());
        } else {
            // we do not know the exact problem
            logger.fatal(MSG__TEST_SKIPPED_UNRECOGNIZED_REASON, testResult.getThrowable());
        }

        channel.currentTestcaseName = null;
        channel.currentTestcaseResult = null;
        channel.currentTestcaseContext = null;
        //channel.lastTestcaseResult = TestCaseResult.SKIPPED.toInt();
        // end test case
        logger.endTestcase(TestCaseResult.SKIPPED, endTestcaseEventMessage, channel.callerId);

    }

    private void endTestcaseWithFailureStatus(Channel channel, String endTestcaseEventMessage ) {

        try {
            //Check if the test was successfully started, if not - make it started and then end it with failure
            ITestResult testResult = channel.currentTestcaseResult;
            String testName = testResult.getMethod().toString();
            if (!testName.equals(channel.currentTestcaseName)) {
                startTestcase(channel, testResult, false);
            }
            sendTestEndEventToAgents(channel);

            // if this is an assertion error, we need to log it
            Throwable failureThr = testResult.getThrowable();
            if (failureThr instanceof AssertionError) {
                if (failureThr.getMessage() != null) {
                    logger.error(ExceptionUtils.getExceptionMsg(failureThr));
                } else {
                    logger.error(ExceptionUtils.getExceptionMsg(failureThr,
                                                                "Received java.lang.AssertionError with null message"));
                }
            } else {
                logger.error(MSG__TEST_FAILED, failureThr);
            }

            // clear testcase data
            channel.currentTestcaseName = null;
            channel.currentTestcaseResult = null;
            channel.currentTestcaseContext = null;
            //channel.testcaseResult = TestCaseResult.FAILED.toInt();
            // end test case
            logger.endTestcase(TestCaseResult.FAILED, endTestcaseEventMessage, channel.callerId);
        } catch (Exception e) {
            logger.fatal("UNEXPECTED EXCEPTION IN AtsTestngListener@endTestcaseWithFailureStatus", e);
        }
    }

    private void endTestcaseWithSuccessStatus(Channel channel, String endTestcaseEventMessage ) {

        sendTestEndEventToAgents(channel);
        if (channel.currentTestcaseResult == null) {
            throw new RuntimeException("Could not close testcases '" + channel.currentTestcaseName
                                       + "'. Its has no testcase result");
        }
        boolean shouldTestFail = TestcaseStateEventsDispacher.getInstance().hasAnyQueueFailed();
        if (shouldTestFail) {
            logger.warn("At least one queue in test failed");
            channel.currentTestcaseResult.setStatus(ITestResult.FAILURE);
            //channel.currentTestcaseResult.setThrowable(new RuntimeException(message)); -> must be tested in order to not break previous behavior
            endTestcaseWithFailureStatus(channel, endTestcaseEventMessage);
            return;
        }
        logger.info(MSG__TEST_PASSED);

        try {

            // clear testcase data
            channel.currentTestcaseName = null;
            channel.currentTestcaseResult = null;
            channel.currentTestcaseContext = null;
            //channel.lastTestcaseResult = TestCaseResult.PASSED;
            // end test case
            logger.endTestcase(TestCaseResult.PASSED, endTestcaseEventMessage, channel.callerId);
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

    private void sendTestEndEventToAgents(Channel channel) {

        if (ActiveDbAppender.getCurrentInstance() != null) {
            // send TestEnd event to all ATS agents
            TestcaseStateEventsDispacher.getInstance().onTestEnd(Arrays.asList(new String[]{ channel.callerId }));
        }
    }

    /**
     * End the current suite for the channel.
     *
     * @param channel
     */
    private void endSuite( Channel channel ) {
        // TODOs - change severity to DEBUG
        ATS_LOGGER.info("  About to end suite " + channel.currentSuiteName
                        + " for channel " + channel);
        // end the current suite
        String suiteName = channel.currentSuiteName;
        channel.currentSuiteName = null;
        logger.endSuite("End suite '" + suiteName + "'",
                        Long.parseLong(ExecutorUtils.extractThreadId(channel.callerId)));
    }

    /**
     * End a suite for a particular channel.
     * Called just before ending a run.
     *
     * @param threadName name of the invoked thread
     * @param channel
     */
    /*private void endSuite( String threadName, Channel channel ) {

        channel.currentSuiteName = null;
        logger.endSuite( threadName );
    }*/

    /**
     * Log start/end of particular annotated method - @Test, @BeforeXXX, @AfterXXX
     */
    private void logCondition( IInvokedMethod method, ITestResult testResult, String condition) {
        String prefix = condition + " '" + testResult.getTestClass().getName() + "@"
                        + method.getTestMethod().getMethodName();
        if (logger.isDebugEnabled()) {
            Channel channel = getChannel(); // optionally pass the Channel as method parameter
            logger.debug( prefix + "', channel: " + channel);
        } else if (logger.isInfoEnabled()) {
            if (AbstractDbAppender.parallel) {
                logger.info(prefix + "', channel: " + getChannel().callerId);
            } else {
                logger.info(prefix + "'");
            }
        }
    }

    private void handleBeforeSuite( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation, Channel channel) {

        if (!afterInvocation) {
            /*
             * Close any previous testcase, started from the current channel
             * Close any previous suite, started from the current channel
             */
            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel); // , testResult, testResult.getTestContext()
                }
            }
            if (channel.currentSuiteName != null) {
                endSuite(channel);
            }
            logCondition(method, testResult, MSG__START);
        } else {
            logCondition(method, testResult, MSG__END);
        }

        //        logger.info("[TESTNG]: START @BeforeSuite '" + testResult.getTestClass().getRealClass() + "@"
        //                    + method.getTestMethod().getMethodName() + "'");
    }

    /**
     * Handle @BeforeClass <br />
     */
    private void handleBeforeClass( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation, Channel channel ) {
        if (!afterInvocation) { // start of @BeforeClass
            /* Close any previous testcase, started from the current channel
             * Close any previous suite, started from the current channel
             * Start new suite
             */
            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult == null) { // update result of the opened testcase
                    ATS_LOGGER.warn("Opened testcase " + channel.currentTestcaseName + " with empty result");
                    channel.currentTestcaseResult = testResult;
                }
                endTestcase(channel);
            }

            String thisSuiteName = testResult.getTestClass().getRealClass().getSimpleName();
            if (channel.currentSuiteName != null && !channel.currentSuiteName.equals(thisSuiteName)) {
                // end previously started suite
                endSuite(channel);
            } else {
                if (channel.currentSuiteName.equals(thisSuiteName)) {
                    // suite already started
                } else {
                    channel.currentSuiteName = thisSuiteName;
                    startSuite(channel, testResult); // start new suite
                }
            }
            logCondition(method, testResult, MSG__START);
        } else {
            logCondition(method, testResult, MSG__END);
        }
    }

    private void handleBeforeTest( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation ) {

        if (!afterInvocation) {
            logCondition(method, testResult, MSG__START);
        } else {
            logCondition(method, testResult, MSG__END);
        }
    }

    /*
     * Close any previous testcase, if channel's lastTescaseResult is not null
     * Close any previous suite, started from the current channel, if its name is different from the current one (the one retrieved from the testResult parameter)
     * Start new suite if necessary
     * Start new testcase
     */
    private void handleBeforeMethod( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation,
                                     Channel channel) {
        if (!afterInvocation) { // start BeforeMethod
            if (channel.currentSuiteName == null) {
                // Not open existing suite. Start suite
                startSuite(channel, testResult);
            } else {
                if (channel.currentTestcaseName != null) { // we have open testcase
                    if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                        ATS_LOGGER.warn("BeforeMethod: Not closed previous test " + channel.currentTestcaseName + ". Closing it");
                        endTestcase(channel);
                    }
                }
                if (!channel.currentSuiteName.equals(testResult.getTestClass()
                                                               .getRealClass()
                                                               .getSimpleName())) {

                    endSuite(channel); // end previously started suite
                    startSuite(channel, testResult); // start new suite
                }
            }
            if (channel.currentTestcaseName == null) {
                // start testcase
                startTestcase(channel, testResult, true);
            } // else - test case already started. Probably from another BeforeMethod. Optionally assert for different test case name

            logCondition(method, testResult, MSG__START);
        } else {
            logCondition(method, testResult, MSG__END);
        }
    }

    private void handleAfterMethod( IInvokedMethod method, ITestResult testResult, ITestContext context,
                                    Boolean afterInvocation, Channel channel ) {

        if (!afterInvocation) {
            logger.startAfterMethod();
            logCondition(method, testResult, MSG__START);
            // re-initialize channel as it is zeroed in end of @Test
            if (channel.currentTestcaseName == null) {
                channel.currentTestcaseName = testResult.getMethod().toString();
                channel.currentTestcaseResult = testResult;
                channel.currentTestcaseContext = testResult.getTestContext();
            }
        } else {
            //end of @AfterMethod
            logCondition(method, testResult, MSG__END);
            logger.endAfterMethod();

            // set new end timestamp and result for the current testcase
            // by passing -1, the DbEventRequestProcessor will decide the testcaseId
            logger.updateTestcase(-1, null, null, null, null, null,
                                  testResult.getStatus());
            channel.currentTestcaseName = null;
            channel.currentTestcaseResult = null;
            channel.currentTestcaseContext = null;
        }
    }

    private void handleAfterClass( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation,
                                   Channel channel) {

        if (!afterInvocation) {
            if (channel.currentSuiteName == null) {
                logger.startAfterClass();
            }
            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    ATS_LOGGER.warn("Test named " + channel.currentTestcaseName + " seems not closed at the end of "
                                    + "handleTest() or handleAfterMethod!");
                    //endTestcase(channel);
                }
            }
            logCondition(method, testResult, MSG__START);
        } else {
            /* Close channel's testcase if such is still opened
               Close channel's suite */
            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

            logCondition(method, testResult, MSG__END);

            if (testResult.getStatus() == ITestResult.FAILURE) {
                // log the Throwable object from the @AfterClass
                logger.fatal(testResult.getThrowable().getMessage(), testResult.getThrowable());
            }
            if (channel.currentSuiteName != null) {
                endSuite(channel);
            } else {
                ATS_LOGGER.warn("Suite for method " + method + " is already closed");
                // the event was received after a suite is already ended
                // which means that we only have to clear the after class mode
                logger.endAfterClass();
            }
        }
    }

    private void handleAfterTest( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation ) {

        if (!afterInvocation) {
            // TODOs - add new method to support Before/AfterTest
            logger.startAfterSuite(); // the closest to supported ones?
            logCondition(method, testResult, MSG__START);
            //logger.endAfterSuite();
        } else {
            //logger.startAfterSuite(); the closest to supported ones?
            logCondition(method, testResult, MSG__END);

            if (testResult.getStatus() == ITestResult.FAILURE) {
                // log the Throwable object from the @AfterTest
                logger.fatal(testResult.getThrowable().getMessage(), testResult.getThrowable());
            }
            logger.endAfterSuite();
        }
    }

    private void handleAfterSuite( IInvokedMethod method, ITestResult testResult, Boolean afterInvocation,
                                   Channel channel) {

        if (!afterInvocation) {
            // Close testcase if the channel has an opened one
            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    ATS_LOGGER.warn("Test named " + channel.currentTestcaseName + " seems not closed at the end of "
                                    + "handleTest() or handleAfterMethod!");
                    //endTestcase(channel);
                }
            }
            logger.startAfterSuite();
            logCondition(method, testResult, MSG__START);
        } else {
            if (channel.currentTestcaseName != null) { // check for opened testcase
                ATS_LOGGER.error("Testcase " + channel.currentTestcaseName
                                 + " is not closed as expected after @Test or @AfterMethod!");
            }
            if (testResult.getStatus() == ITestResult.FAILURE) {
                // log the Throwable object from the @AfterSuite
                logger.fatal(testResult.getThrowable().getMessage(), testResult.getThrowable());
            }
            logCondition(method, testResult, MSG__END);
            logger.endAfterSuite();
            endSuite(channel); // Close suite
        }
    }

    /**
     * Close previous testcase if we have an end result for it. <br>
     * <pre>Check if suite is already started.
     * - If not - start in.
     * - If yes - check if the current @Test method is from the same Java class as the already started suite.
     *    -- If not - end the suite and start a new one
     * </pre>
     * Start testcase if such does not exist, or update testcase if such was started from a @BeforeMethod
     */
    private void handleTestMethod( IInvokedMethod method, ITestResult testResult, ITestContext context,
                                   Boolean afterInvocation, Channel channel) {

        if (!afterInvocation) { // before start of @Test method
            // if this is a Test from a new suite
            if (channel.currentSuiteName == null) {
                // start suite
                startSuite(channel, testResult);
            } else {
                if (!channel.currentSuiteName.equals(testResult.getTestClass()
                                                       .getRealClass()
                                                       .getSimpleName())) {
                    endSuite(channel); // end previously started suite
                    startSuite(channel, testResult); // start new suite
                }
            }

            if (channel.currentTestcaseName == null) {
                // start testcase
                startTestcase(channel, testResult, false);
            } else {
                // re-run, update testcase. The testcase could also have been started from a @BeforeMethod
                // Assume currentTestcaseName = current one. Old test method is assumed to be closed in after part
                // - the else part below
                updateTestcase(channel, testResult);
            }
        } else { // after @Test
            // Save the testcase's result and context
            channel.currentTestcaseResult = testResult;
            channel.currentTestcaseContext = context;

            // TODOs - test status might be changed in After
            endTestcase(channel/*, testResult, context*/);
        }
    }

    /** Get the channel (test info) associated with the running thread */
    private Channel getChannel() {

        String threadId = Long.toString(Thread.currentThread().getId());

        Channel channel = channels.get(threadId);
        if (channel == null) {
            if (!AbstractDbAppender.parallel) {
                if (!this.channels.isEmpty()) {
                    // get the first channel from the map
                    return this.channels.get(this.channels.keySet().iterator().next());
                }
            }
            channel = new Channel();
            channel.callerId = ExecutorUtils.createCallerId();
            channels.put(threadId, channel);
        }

        return channel;
    }

}