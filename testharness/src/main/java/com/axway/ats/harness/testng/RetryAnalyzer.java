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

import java.lang.reflect.Method;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.ActiveDbAppender;

/**
 * Evaluates if a failed test will be re-run.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    /**
     * Do not throw exception if db appender is not attached. (by passing true as a 2nd argument)
     * */
    private static final AtsDbLogger logger = AtsDbLogger.getLogger("com.axway.ats", true);

    private String                   lastTestName;

    /*
     * Number of passed runs of the current test.
     * The value is static, so can easily be retrieved from our TestNG listeners. That is ok as no more
     * than one instance of this class exists at a time.
     */
    private static int               passedRuns; //FIXME: possible problem when tests are run in parallel

    public RetryAnalyzer() {

        lastTestName = null;
        passedRuns = 0;
    }

    @Override
    public boolean retry(
                          final ITestResult testResult ) {

        /* we get here after processing onTestFailed events
        *  this method is executed after StartTestCaseEvent and before EndTestCaseEvent
        *  a check is performed, in DbEventRequestProcessor.endTestCase(), if the current testcase is marked for deletion
        *  for more on how and where all testcases, marked for deletion is performed,
        *  see DbEventRequestProcessor.processEventRequest()
        */

        Class<?> testClass = testResult.getTestClass().getRealClass();

        final String testClassName = testClass.getName();
        final String testName = getTestName(testResult);
        final String fullTestName = testClassName + "@" + testName;

        if (fullTestName.equals(lastTestName)) {
            ++passedRuns;
        } else {
            lastTestName = fullTestName;
            passedRuns = 1;
        }

        if (passedRuns < getMaxRuns(testResult)) {

            // delete the current test
            deleteCurrentTestcase();

            // instruct TestNG to rerun the test
            return true;
        } else {
            // all possible test runs are exhausted, do not rerun
            return false;
        }

    }

    public static int getNumberPassedRuns() {

        return passedRuns;
    }

    private void deleteCurrentTestcase() {

        ActiveDbAppender dbAppender = ActiveDbAppender.getCurrentInstance();

        if (dbAppender != null) {
            logger.deleteTestcase(dbAppender.getTestCaseId());
        }
    }

    private String getTestName(
                                ITestResult result ) {

        String testName = result.getName();

        // check if there is a description annotation and get the test name
        Method testCaseMethod = result.getMethod().getConstructorOrMethod().getMethod();
        Description testCaseDescription = testCaseMethod.getAnnotation(Description.class);
        if (testCaseDescription != null && testCaseDescription.name().length() > 0) {
            testName = testCaseDescription.name();
        }

        return testName;
    }

    /**
     * Check if there is a description annotation and get the max retries value
     * 
     * @param result
     * @return
     */
    private int getMaxRuns(
                            ITestResult result ) {

        /* We search for the user's input about "max runs" of failed tests.
         * If the found value is -1, we keep searching as this is the default value which is invalid.
         */

        // check if max runs comes from the test method
        Method testCaseMethod = result.getMethod().getConstructorOrMethod().getMethod();
        TestOptions testOptions = testCaseMethod.getAnnotation(TestOptions.class);
        if (testOptions != null) {
            int maxRuns = testOptions.maxRuns();
            if (maxRuns != -1) {
                return maxRuns;
            }
        }

        // check if max runs comes from the test class or its parent class
        Class<?> testClass = result.getInstance().getClass();
        while (testClass != null) {
            testOptions = testClass.getAnnotation(TestOptions.class);
            if (testOptions != null) {
                int maxRuns = testOptions.maxRuns();
                if (maxRuns != -1) {
                    return maxRuns;
                }
            }
            // search the parent class
            testClass = testClass.getSuperclass();
        }

        // max runs is not specified
        return -1;
    }
}
