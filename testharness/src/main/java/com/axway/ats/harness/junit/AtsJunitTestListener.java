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
package com.axway.ats.harness.junit;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.harness.config.CommonConfigurator;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.ActiveDbAppender;
import com.axway.ats.log.model.TestCaseResult;

/**
 * <p>JUnit test listener. Not thread safe/parallel support.</p>
 *
 * <p>This listener should be added to some JUnit runner</p>
 *
 * @see org.junit.runner.notification.RunListener
 */
@PublicAtsApi
public class AtsJunitTestListener extends RunListener {

    private static final Logger      log                              = LogManager.getLogger(AtsJunitTestListener.class);

    /*
     * skip checking whether ActiveDbAppender is attached in order for test execution to proceed
     * Note that additional check in each of the methods check once again whether that appender is attached
     * */
    private static final AtsDbLogger logger                           = AtsDbLogger.getLogger("com.axway.ats", true);

    private static final String      MSG__TEST_PASSED                 = "[JUnit]: TEST PASSED";

    private static final String      MSG__TEST_FAILED                 = "[JUnit]: TEST FAILED";

    private static final String      MSG__TEST_FAILED_ASSERTION_ERROR = "[JUnit]: ASSERTION ERROR. TEST FAILED";

    private static final String      MSG__TEST_IGNORED                = "[JUnit]: TEST IGNORED due to @Ignore annotation";

    /**
     * Keeps track of last suite name (class with test methods)
     */
    private static String            lastSuiteName;

    /**
     * Keeps track of last started method. Used because listener is not notified for successfully ended method.
     * Only testFinished is invoked then and {@link Description} does not provide test result status.
     */
    private String                   lastStartedMethod;

    /**
     * keep status about last running method because there is not notification that test passes.
     * If test finishes and this is not set to true (in testFailure()) than it is assumed that the test has passed.
     */
    private boolean                  lastStartedMethodIsFailing;

    /**
    *
    * @see org.junit.runner.notification.RunListener#testRunStarted(org.junit.runner.Description)
    */
    @Override
    public void testRunStarted( Description description ) throws Exception {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (log.isDebugEnabled()) { // currently always returns null for Description parameter
            log.debug("testRunStarted(): Called before any test is run. Description of all tests expected: "
                      + description);
        }
        String runNameSysProp = AtsSystemProperties.getPropertyAsString(AtsSystemProperties.TEST_HARNESS__JUNIT_RUN_NAME,
                                                                        "JUnit run(nameless)");

        String hostNameIp = "";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostNameIp = addr.getHostName() + "/" + addr.getHostAddress();

        } catch (UnknownHostException uhe) {
            hostNameIp = null;
        }

        logger.startRun(runNameSysProp /* no suite name in JUnit */, CommonConfigurator.getInstance().getOsName(),
                        CommonConfigurator.getInstance().getProductName(),
                        CommonConfigurator.getInstance().getVersionName(),
                        CommonConfigurator.getInstance().getBuildName(), hostNameIp);
        super.testRunStarted(description);
    }

    /**
    *
    * @see org.junit.runner.notification.RunListener#testRunFinished(org.junit.runner.Result)
    */
    @Override
    public void testRunFinished( Result result ) throws Exception {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("testRunFinished(): result " + "| failure count: " + result.getFailureCount()
                      + "| ignored count: " + result.getIgnoreCount() + "| tests run count: "
                      + result.getRunCount() + "| Run time: " + result.getRunTime() + "ms.");
        }
        if (lastSuiteName != null) {
            logger.endSuite();
            lastSuiteName = null; // Run finished. A lastSuiteName should not be visible between runs
        }
        // end the run
        logger.endRun();
        super.testRunFinished(result);
    }

    /**
     * (non-Javadoc)
     * @see org.junit.runner.notification.RunListener#testAssumptionFailure(org.junit.runner.notification.Failure)
     */
    @Override
    public void testAssumptionFailure( Failure failure ) {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("testAssumptionFailure(): Test failed: " + failure.toString() + "| Description: "
                      + failure.getDescription());
        }
        log.info("Test assumption failure received. It will be stored in DB as test failed event.");
        try {
            testFailure(failure);
        } catch (Exception e) {
            log.error("Error while processing testFailure event", e);
        }
        super.testAssumptionFailure(failure);
    }

    /**
     * @see org.junit.runner.notification.RunListener#testFinished(org.junit.runner.Description)
     */
    @Override
    public void testFinished( Description description ) throws Exception {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("testFinished(): description: " + description.getDisplayName() + "| is suite: "
                      + description.isSuite() + "| is test: " + description.isTest() + "| test class: "
                      + description.getClassName() + "| test method: " + description.getMethodName()
                      + "| children: " + description.getChildren());
        }

        if (!lastStartedMethodIsFailing && lastStartedMethod.equals(description.getMethodName())) {
            // successfully run method
            logger.info(MSG__TEST_PASSED);
            try {
                // end a test case and scenario
                logger.endTestcase(TestCaseResult.PASSED);
                sendTestEndEventToAgents();
            } catch (Exception e) {
                String msg = "UNEXPECTED EXCEPTION IN AutoLogTestListener@testFinished()";
                try {
                    logger.fatal(msg, e);
                } catch (Exception e1) {
                    log.error("UNEXPECTED EXCEPTION IN JUnit AutoLogTestListener@testFinished");
                    e.printStackTrace();
                    // log in console because DB logging might fail
                    log.error("The message above could not be logged:");
                    e1.printStackTrace();
                }
            }
        } else {
            // Failure is already tracked in testFailure()
        }
        lastStartedMethodIsFailing = false; // reset status for next test to be run
        super.testFinished(description);
    }

    /**
     *
     * @see org.junit.runner.notification.RunListener#testStarted(org.junit.runner.Description)
     */
    @Override
    public void testStarted( Description description ) throws Exception {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (log.isDebugEnabled()) {

            log.debug("testStarted(): Called when an atomic test is about to be started. Description generally class and method: "
                      + description); //, new Exception( "debugging trace" ) );

        }
        lastStartedMethodIsFailing = false;
        Class<?> testClass = description.getTestClass(); //testResult.getTestClass().getRealClass();

        String suiteName;
        String suiteSimpleName;
        String tcName = getTestName(description);
        // Update last started method. Note that testStarted() is invoked even before @Before methods
        lastStartedMethod = description.getMethodName(); // TODO: check for overridden methods
        String tcDescription = getTestDescription(description);

        suiteName = testClass.getName(); // assuming not JUnit 3 class "TestSuite"
        suiteSimpleName = testClass.getSimpleName();

        // check if we need to start a new group
        if (!suiteName.equals(lastSuiteName)) {
            if (lastSuiteName != null) {
                logger.endSuite();
            }

            String packName = suiteName.substring(0, suiteName.lastIndexOf('.'));
            logger.startSuite(packName, suiteSimpleName);
            lastSuiteName = suiteName;
        }

        // start a scenario and test case
        logger.startTestcase(suiteName, suiteSimpleName, tcName, "", tcDescription);

        // send TestStart event to all ATS agents
        TestcaseStateEventsDispacher.getInstance().onTestStart();

        logger.info("[JUnit]: Starting " + suiteName + "@" + tcName);
        super.testStarted(description);
    }

    /**
     *
     * @see org.junit.runner.notification.RunListener#testFailure(org.junit.runner.notification.Failure)
     */
    @Override
    public void testFailure( Failure failure ) throws Exception {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("testFailure(): Test failed: " + failure.toString() + "| Description: "
                      + failure.getDescription());
        }
        try {
            lastStartedMethodIsFailing = true;
            //TODO check if failure.getDescription() represents several methods. This might be in case of exception
            // in @BeforeClass

            // if this is an assertion error, we need to log it
            Throwable failureException = failure.getException();
            if (failureException instanceof AssertionError) {
                logger.error(MSG__TEST_FAILED_ASSERTION_ERROR, failureException); //.getMessage() );
            } else {
                logger.error(MSG__TEST_FAILED, failureException);
            }

            //open a performance test scenario and test case
            logger.endTestcase(TestCaseResult.FAILED);
            sendTestEndEventToAgents();
        } catch (Exception e) {
            try {
                logger.fatal("UNEXPECTED EXCEPTION IN AutoLogTestListener@onTestFailure", e);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } finally {
            super.testFailure(failure);
        }
    }

    /**
     *
     * @see org.junit.runner.notification.RunListener#testIgnored(org.junit.runner.Description)
     */
    @Override
    public void testIgnored( Description description ) throws Exception {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.info("testIgnored(): description: " + description.getDisplayName() + "| is test: "
                     + description.isTest() + "| test class: " + description.getClassName()
                     + "| test method: " + description.getMethodName() + "| children: 0? =>"
                     + description.getChildren());
        }
        // manually invoking testStarted()
        testStarted(description);

        String ignoreDetails = "";
        Ignore ignoreAnnotation = getMethod(description).getAnnotation(Ignore.class);
        if (ignoreAnnotation != null) {
            ignoreDetails = ignoreAnnotation.value();
        }
        if (ignoreDetails.isEmpty()) {
            logger.info(MSG__TEST_IGNORED);
        } else {
            logger.info(MSG__TEST_IGNORED + " Details: " + ignoreDetails);
        }

        //open a performance test scenario and test case
        logger.endTestcase(TestCaseResult.SKIPPED);
        sendTestEndEventToAgents();

        super.testIgnored(description);
    }

    private void sendTestEndEventToAgents() {

        // send TestEnd event to all ATS agents
        TestcaseStateEventsDispacher.getInstance().onTestEnd();
    }

    private String getTestName( Description description ) {

        return description.getMethodName();
    }

    private String getTestDescription( Description description ) {

        String tcDescription = description.getDisplayName();
        // TODO: TestHarness' @Description annotation - in TestNG package currently

        return tcDescription;
    }

    /**
     * Get reference to the Method described by {@link Description}.
     * @param description
     * @return
     */
    private Method getMethod( Description description ) {

        if (description == null) {
            throw new IllegalArgumentException();
        }

        // supposing only one test with given method name, i.e. there is not overriding
        Method testCaseMethod = null;
        try {
            testCaseMethod = description.getTestClass()
                                        .getDeclaredMethod(description.getMethodName() /* parameterTypes */ );
        } catch (Exception e) {// NoSuchMethodException
            log.error("Could not get test method named " + description.getMethodName()
                      + " in order to inspect it for Description annotation.", e);
        }
        return testCaseMethod;
    }

}
