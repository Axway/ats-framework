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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
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
import org.testng.internal.TestResult;
import org.testng.xml.XmlSuite.ParallelMode;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.utils.ClasspathUtils;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.harness.config.CommonConfigurator;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.AbstractDbAppender;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.model.TestCaseResult;

public class AtsTestngListener implements ISuiteListener, IInvokedMethodListener2 {

    /** 
     * Skip checking whether ActiveDbAppender is attached. 
     * This is done in order to enable execution of tests when that appender is not attached to the log4j's RootLogger
     **/
    private static final AtsDbLogger logger                                = AtsDbLogger.getLogger("com.axway.ats",
                                                                                                   true);

    private static final String      MSG__TEST_PASSED                      = "[TestNG]: TEST PASSED";

    private static final String      MSG__TEST_FAILED                      = "[TestNG]: TEST FAILED";

    private static final String      MSG__TEST_SKIPPED_DEPENDENCY          = "[TestNG]: TEST SKIPPED due to dependency failure";

    private static final String      MSG__TEST_SKIPPED_CONFIGURATION       = "[TestNG]: TEST SKIPPED due to configuration failure";

    private static final String      MSG__TEST_SKIPPED_UNRECOGNIZED_REASON = "[TestNG]: TEST SKIPPED due to unrecognized failure";

    private static boolean           testDescAvailable                     = false;

    private static Integer           BEFORE_METHOD_INDEX                   = 0;

    private final String             JAVA_FILE_EXTENSION                   = ".java";

    private String                   javaFileContent;
    private String                   projectSourcesFolder;

    private Map<String, Channel>     channels;

    private class Channel {

        /* keeps track of the current testcase name */
        String       currentTestcaseName    = null;

        /* keeps track of the current suite name */
        String       currentSuiteName       = null;

        /* keeps track of the current testcase's end result and its context (context = information for the actual Java method that correlate with the testcase */
        ITestResult  currentTestcaseResult  = null;
        ITestContext currentTestcaseContext = null;

        // the caller that created this channel
        String       callerId               = null;

    }

    private Channel getChannel() {

        String threadId = Thread.currentThread().getId() + "";

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

    /**
     * The types of TestNG methods that this listener is interested in.
     * <br>
     * Other types as @BeforeTest are still supported, but are of no interest to this listener/class
     * */
    private enum METHOD_TYPE {
        BEFORE_SUITE, BEFORE_CLASS, BEFORE_METHOD, TEST, AFTER_METHOD, AFTER_CLASS, AFTER_SUITE
    }

    public AtsTestngListener() {

        ActiveDbAppender.isBeforeAndAfterMessagesLoggingSupported = true;

        channels = Collections.synchronizedMap(new HashMap<String, Channel>());//new HashMap<>();
    }

    @Override
    public void beforeInvocation( IInvokedMethod method, ITestResult testResult ) {}

    @Override
    public void afterInvocation( IInvokedMethod method, ITestResult testResult ) {}

    @Override
    public void beforeInvocation( IInvokedMethod method, ITestResult testResult, ITestContext context ) {

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        Channel channel = getChannel();

        /*
         * Close any previous testcase, started from the current channel
         * Close any previous suite, started from the current channel
         * */
        if (isTestngMethod(method, METHOD_TYPE.BEFORE_SUITE)) {

            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

            if (channel.currentSuiteName != null) {
                endSuite(channel);
            }

            logger.info("[TESTNG]: START @BeforeSuite '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");
        }

        /*
         * Close any previous testcase, started from the current channel
         * Close any previous suite, started from the current channel
         * Start new suite
         */
        if (isTestngMethod(method, METHOD_TYPE.BEFORE_CLASS)) {

            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

            if (channel.currentSuiteName != null) {
                endSuite(channel);
            }

            channel.currentSuiteName = testResult.getTestClass()
                                                 .getRealClass()
                                                 .getName();
            startSuite(channel, testResult);

            logger.info("[TESTNG]: START @BeforeClass '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");

        }

        /*
         * Close any previous testcase, if channel's lastTescaseResult is not null
         * Close any previous suite, started from the current channel, if its name is different from the current one (the one retrieved from the testResult parameter)
         * Start new suite if necessary
         * Start new testcase
         */
        if (isTestngMethod(method, METHOD_TYPE.BEFORE_METHOD)) {

            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

            if (channel.currentSuiteName != null) {
                if (!channel.currentSuiteName.equals(testResult.getTestClass()
                                                               .getRealClass()
                                                               .getName())) {
                    endSuite(channel);
                }
            }

            if (channel.currentSuiteName == null) {
                startSuite(channel, testResult);
            }

            if (channel.currentTestcaseName == null) {
                startTestcase(channel, testResult, context, true);
            }

            logger.info("[TESTNG]: START @BeforeMethod '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");

        }

        /*
         * Close previous testcase if we have an end result for it
         * Check if suite is already started. 
         * - If not - start in. 
         * - If yes - check if the current @Test method is from the same Java class as the already started suite. 
         * - - If not - end the suite and start a new one
         * Start testcase if such does not exist, or update testcase if such was started from a @BeforeMethod
         * */
        if (isTestngMethod(method, METHOD_TYPE.TEST)) {

            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

            if (channel.currentSuiteName != null) {
                if (!channel.currentSuiteName.equals(testResult.getTestClass()
                                                               .getRealClass()
                                                               .getName())) {
                    endSuite(channel);
                }
            }

            if (channel.currentSuiteName == null) {
                startSuite(channel, testResult);
            }

            if (channel.currentTestcaseName == null) {
                startTestcase(channel, testResult, context, false);

            } else {
                updateTestcase(channel, testResult, context); // the testcase was started from a @BeforeMethod apparently
            }

            logger.info("[TESTNG]: START @Test '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");

        }

        if (isTestngMethod(method, METHOD_TYPE.AFTER_METHOD)) {

            logger.info("[TESTNG]: START @AfterMethod '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");
        }

        /*
         * Close any opened testcase from that channel
         * */
        if (isTestngMethod(method, METHOD_TYPE.AFTER_CLASS)) {

            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

            logger.info("[TESTNG]: START @AfterClass '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");
        }

        /*
         * Close testcase if the channel has an opened one
         * Close suite if the channel has an opened one
         * */
        if (isTestngMethod(method, METHOD_TYPE.AFTER_SUITE)) {

            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

            if (channel.currentSuiteName != null) {
                endSuite(channel);
            }

            logger.info("[TESTNG]: START @AfterSuite '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");
        }

    }

    @Override
    public void afterInvocation( IInvokedMethod method, ITestResult testResult, ITestContext context ) {

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        Channel channel = getChannel();

        if (isTestngMethod(method, METHOD_TYPE.BEFORE_SUITE)) {

            logger.info("[TESTNG]: END @BeforeSuite '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");
        }

        if (isTestngMethod(method, METHOD_TYPE.BEFORE_CLASS)) {

            logger.info("[TESTNG]: END @BeforeClass '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");

        }

        if (isTestngMethod(method, METHOD_TYPE.BEFORE_METHOD)) {

            logger.info("[TESTNG]: END @BeforeMethod '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");
        }

        /*
         * Save the testcase's result and context
         * */
        if (isTestngMethod(method, METHOD_TYPE.TEST)) {

            logger.info("[TESTNG]: END @Test '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");

            channel.currentTestcaseResult = testResult;
            channel.currentTestcaseContext = context;

        }

        /*
         * Close the channel's testcase if we have a result for it
         * */
        if (isTestngMethod(method, METHOD_TYPE.AFTER_METHOD)) {

            logger.info("[TESTNG]: END @AfterMethod '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");

            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

        }

        /*
         * Close channel's testcase if such is still opened
         * Close channel's suite
         * */
        if (isTestngMethod(method, METHOD_TYPE.AFTER_CLASS)) {

            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

            logger.info("[TESTNG]: END @AfterClass '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");

            if (channel.currentSuiteName != null) {
                endSuite(channel);
            }

        }

        /*
         * Close channel's testcase if such is still opened
         * Close channel's suite
         * */
        if (isTestngMethod(method, METHOD_TYPE.AFTER_SUITE)) {

            if (channel.currentTestcaseName != null) { // we have open testcase
                if (channel.currentTestcaseResult != null) { // the opened testcase has end result.
                    endTestcase(channel);
                }
            }

            if (channel.currentSuiteName != null) {
                endSuite(channel);
            }

            logger.info("[TESTNG]: END @AfterSuite '" + testResult.getTestClass().getRealClass() + "@"
                        + method.getTestMethod().getMethodName() + "'");
        }

    }

    @Override
    public void onStart( ISuite suite ) {

        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (!ParallelMode.NONE.name().equalsIgnoreCase(suite.getParallel())) {
            AbstractDbAppender.parallel = true;
        }

        // get the run name specified by the user
        String runName = CommonConfigurator.getInstance().getRunName();
        if (runName.equals(CommonConfigurator.DEFAULT_RUN_NAME)) {
            // the user did not specify a run name, use the one from TestNG
            runName = suite.getName();
        }

        // start a new run
        String hostNameIp = "";
        try {
            hostNameIp = HostUtils.getLocalHostName() + "/" + HostUtils.getLocalHostIP();

        } catch (Throwable t) {
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
                endTestcase(channel);
            }

            // end suite
            if (channel.currentSuiteName != null) {
                endSuite(channel);
            }
        }

        // clear channels for the ended run
        channels.clear();

        // end the run
        logger.endRun();

    }

    private void startSuite( Channel channel, ITestResult testResult ) {

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

        channel.currentSuiteName = testClass.getName();

        logger.startSuite(packageName, suiteSimpleName);
    }

    private void endSuite( Channel channel ) {

        String suiteName = channel.currentSuiteName;
        channel.currentSuiteName = null;
        logger.endSuite("End suite '" + suiteName + "'",
                        Integer.parseInt(ExecutorUtils.extractThreadId(channel.callerId)));
    }

    private void startTestcase( Channel channel, ITestResult testResult, ITestContext context,
                                boolean isBeforeMethod ) {

        Class<?> testClass = testResult.getTestClass().getRealClass();

        String suiteFullName = testClass.getName();
        String suiteSimpleName = testClass.getSimpleName();

        String testName = getTestName(testResult);
        String testInputArguments = getTestInputArguments(testResult);
        String testDescription = getTestDescription(testClass, suiteFullName, testName, testResult);

        // save the current testcase name
        channel.currentTestcaseName = testResult.getMethod().toString();

        // start test case
        if (isBeforeMethod) {
            // in parallel tests we do not know the test name in the before method
            // and all the tests are inserted in the database with the same name( the before method name )
            // we have to add unique index after the before method name, so we can distinguish the different test cases
            synchronized (BEFORE_METHOD_INDEX) {
                testName = testName + "_" + BEFORE_METHOD_INDEX++;
            }
        }
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

    private void updateTestcase( Channel channel, ITestResult testResult, ITestContext context ) {

        Class<?> testClass = testResult.getTestClass().getRealClass();

        String suiteFullName = testClass.getName();
        String suiteSimpleName = testClass.getSimpleName();
        String testName = getTestName(testResult);
        String testInputArguments = getTestInputArguments(testResult);
        String testDescription = getTestDescription(testClass, suiteFullName, testName, testResult);

        channel.currentTestcaseName = testResult.getMethod().toString();

        // by passing -1, the DbEventRequestProcessor will decide the testcaseId
        logger.updateTestcase(-1, suiteFullName, suiteSimpleName, testName, testInputArguments,
                              testDescription, 4); // 4 means that the testcase is still running

    }

    private void endTestcase( Channel channel ) {

        // send TestEnd event to all ATS agents
        TestcaseStateEventsDispacher.getInstance().onTestEnd(Arrays.asList(new String[]{ channel.callerId }));

        if (channel.currentTestcaseResult == null) {
            throw new RuntimeException("Could not close testcases '" + channel.currentTestcaseName
                                       + "'. Its has no testcase result");
        }

        Class<?> testClass = channel.currentTestcaseResult.getTestClass().getRealClass();
        String suiteFullName = testClass.getName();
        String testName = getTestName(channel.currentTestcaseResult);
        String testInputArguments = getTestInputArguments(channel.currentTestcaseResult);
        String message = "End test case '" + suiteFullName + "@" + testName + testInputArguments + "'";

        // save the current testcase name
        channel.currentTestcaseName = channel.currentTestcaseResult.getMethod().toString();

        switch (channel.currentTestcaseResult.getStatus()) {
            case TestResult.SUCCESS:
                endPassedTestcase(channel, message);
                break;
            case TestResult.SKIP:
                endSkippedTestcase(channel, message);
                break;
            case TestResult.FAILURE:
                endFailedTestcase(channel, message);
                break;
            default:
                logger.error("Could not close testcase '" + channel.currentSuiteName + "@" + channel.currentTestcaseName
                             + "'. Unsupported test result status (" + channel.currentTestcaseResult.getStatus() + ")");
                break;
        }

    }

    private void endPassedTestcase( Channel channel, String endTestcaseEventMessage ) {

        boolean shouldTestFail = TestcaseStateEventsDispacher.getInstance().hasAnyQueueFailed();
        if (shouldTestFail) {
            String message = "At least one queue in test failed";
            logger.warn(message);
            channel.currentTestcaseResult.setStatus(ITestResult.FAILURE);
            //channel.currentTestcaseResult.setThrowable(new RuntimeException(message)); -> must be tested in order to not break previous behavior
            endFailedTestcase(channel, endTestcaseEventMessage);
            return;
        }
        logger.info(MSG__TEST_PASSED);

        try {
            // clear testcase data
            channel.currentTestcaseName = null;
            channel.currentTestcaseResult = null;
            channel.currentTestcaseContext = null;
            // end test case
            logger.endTestcase(TestCaseResult.PASSED, endTestcaseEventMessage, channel.callerId);
        } catch (Exception e) {
            logger.fatal("UNEXPECTED EXCEPTION IN AtsTestngListener@endPassedTestcase", e);
        }

    }

    private void endSkippedTestcase( Channel channel, String endTestcaseEventMessage ) {

        if (configurationError(channel.currentTestcaseContext)) {
            // test is skipped due to configuration error
            logger.info(MSG__TEST_SKIPPED_CONFIGURATION, channel.currentTestcaseResult.getThrowable());
        } else if (dependencyError(channel.currentTestcaseResult, channel.currentTestcaseContext)) {
            // test is skipped due to dependency error
            logger.info(MSG__TEST_SKIPPED_DEPENDENCY, channel.currentTestcaseResult.getThrowable());
        } else {
            // we do not know the exact problem
            logger.fatal(MSG__TEST_SKIPPED_UNRECOGNIZED_REASON, channel.currentTestcaseResult.getThrowable());
        }

        // clear testcase data
        channel.currentTestcaseName = null;
        channel.currentTestcaseResult = null;
        channel.currentTestcaseContext = null;
        // end test case

        logger.endTestcase(TestCaseResult.SKIPPED, endTestcaseEventMessage, channel.callerId);

    }

    private void endFailedTestcase( Channel channel, String endTestcaseEventMessage ) {

        try {

            // if this is an assertion error, we need to log it
            Throwable failureException = channel.currentTestcaseResult.getThrowable();
            if (failureException instanceof AssertionError) {
                if (failureException.getMessage() != null) {
                    logger.error(ExceptionUtils.getExceptionMsg(failureException));
                } else {
                    logger.error(ExceptionUtils.getExceptionMsg(failureException,
                                                                "Received java.lang.AssertionError with null message"));
                }
            } else {
                logger.error(MSG__TEST_FAILED, failureException);
            }

            // clear testcase data
            channel.currentTestcaseName = null;
            channel.currentTestcaseResult = null;
            channel.currentTestcaseContext = null;
            // end test case
            logger.endTestcase(TestCaseResult.FAILED, endTestcaseEventMessage, channel.callerId);
        } catch (Exception e) {
            logger.fatal("UNEXPECTED EXCEPTION IN AtsTestngListener@endFailedTestcase", e);
        }

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

    private boolean isTestngMethod( IInvokedMethod method, METHOD_TYPE methodType ) {

        switch (methodType) {
            case BEFORE_SUITE:
                return method.getTestMethod().isBeforeSuiteConfiguration();
            case BEFORE_CLASS:
                return method.getTestMethod().isBeforeClassConfiguration();
            case BEFORE_METHOD:
                return method.getTestMethod().isBeforeMethodConfiguration();
            case TEST:
                return method.getTestMethod().isTest();
            case AFTER_METHOD:
                return method.getTestMethod().isAfterMethodConfiguration();
            case AFTER_CLASS:
                return method.getTestMethod().isAfterClassConfiguration();
            case AFTER_SUITE:
                return method.getTestMethod().isAfterSuiteConfiguration();
            default:
                return false;
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
                                                                      .toArray(new ITestResult[context.getFailedConfigurations()
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

                        if (!testDescAvailable) {
                            URL testClassPath = testClass.getClassLoader().getResource("."); // this could be null when failsafe maven plugin is used
                            if (testClassPath == null) {
                                testDescAvailable = true;
                                logger.info("Test descriptions could not be assigned to the tests, because the test sources folder could not be found. ");

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

    private void logSystemInformation() {

        StringBuilder systemInformation = new StringBuilder();

        appendMessage(systemInformation, "ATS version: '", AtsVersion.getAtsVersion());
        appendMessage(systemInformation, " os.name: '", (String) System.getProperty("os.name"));
        appendMessage(systemInformation, " os.arch: '", (String) System.getProperty("os.arch"));
        appendMessage(systemInformation, " java.version: '",
                      (String) System.getProperty("java.version"));
        appendMessage(systemInformation, " java.home: '", (String) System.getProperty("java.home"));

        List<String> ipList = new ArrayList<String>();
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

}
