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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.TestNG;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.utils.ClasspathUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.harness.config.CommonConfigurator;
import com.axway.ats.log.AtsDbLogger;
import com.axway.ats.log.appenders.ActiveDbAppender;

/**
 * @deprecated Use {@link AtsTestngListener} class instead.
 * <br><br>
 * Suite listener to capture test events from TestNG<br>
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
@Deprecated
public class AtsTestngSuiteListener implements ISuiteListener {

    /*
     * skip checking whether ActiveDbAppender is attached in order for test execution to proceed
     * Note that additional check in each of the methods check once again whether that appender is attached
     * */
    private static final AtsDbLogger logger = AtsDbLogger.getLogger("com.axway.ats", true);

    public void onStart( ISuite suite ) {

        if (!ActiveDbAppender.isAttached) {
            return;
        }
        
        // get the run name specified by the user
        String runName = CommonConfigurator.getInstance().getRunName();
        if (runName.equals(CommonConfigurator.DEFAULT_RUN_NAME)) {
            // the user did not specify a run name, use the one from TestNG
            runName = suite.getName();
        }

        // the following is needed in case when more than one RUN are executed sequentially
        // we need to clear some temporary data in the other listener we use
        TestNG testNgInstance = TestNG.getDefault();
        // cleanup the class level listener
        new AtsTestngClassListener().resetTempData();
        // cleanup the test level listener
        for (ITestListener listener : testNgInstance.getTestListeners()) {
            if (listener instanceof AtsTestngTestListener) {
                ((AtsTestngTestListener) listener).resetTempData();
            }
        }

        // start a new run
        String hostNameIp = "";
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

    public void onFinish( ISuite suite ) {
        
        if (!ActiveDbAppender.isAttached) {
            return;
        }

        // clear the lastSuiteName
        AtsTestngTestListener.resetLastSuiteName();
        /*
         * If not patched testNG is used then we will have one suite left to end before we end the run, unless
         * no suite was ever started
         */
        if (AtsTestngClassListener.getLastSuiteName() == null) {
            logger.endSuite();
        }

        // end the run
        logger.endRun();
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