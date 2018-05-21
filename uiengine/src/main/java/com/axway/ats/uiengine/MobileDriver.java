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
package com.axway.ats.uiengine;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.process.LocalProcessExecutor;
import com.axway.ats.core.process.model.IProcessExecutor;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.engine.MobileEngine;
import com.axway.ats.uiengine.exceptions.MobileOperationException;
import com.axway.ats.uiengine.exceptions.NotSupportedOperationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.mobile.MobileDeviceUtils;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;

/**
 * A driver used for working with a mobile applications(Android or iOS)
 */
@PublicAtsApi
public class MobileDriver extends UiDriver {

    private static final Logger      log                     = Logger.getLogger(MobileDriver.class);

    @PublicAtsApi
    public static final String       DEVICE_ANDROID_EMULATOR = "Android Emulator";
    @PublicAtsApi
    public static final String       DEVICE_ANDROID          = "Android";
    @PublicAtsApi
    public static final String       DEVICE_IPHONE_SIMULATOR = "iPhone Simulator";
    @PublicAtsApi
    public static final String       DEVICE_IPAD_SIMULATOR   = "iPad Simulator";
    @PublicAtsApi
    public static final String       NATIVE_CONTEXT          = "NATIVE_APP";

    private static final String      ANDROID_HOME_ENV_VAR    = "ANDROID_HOME";
    private static final int         MAX_ADB_RELATED_RETRIES = 2;
    private static final int         DEFAULT_APPIUM_PORT     = 4723;

    private AppiumDriver<WebElement> driver;
    private String                   deviceName              = null;
    private String                   platformVersion         = null;
    private String                   udid                    = null;
    private String                   host                    = null;
    private int                      port                    = -1;
    private MobileEngine             mobileEngine;
    private boolean                  isWorkingRemotely       = false;

    private boolean                  isAndroidAgent          = false;
    private String                   androidHome             = null;
    private String                   adbLocation             = null;
    private Dimension                screenDimensions;

    private MobileDeviceUtils        mobileDeviceUtils;

    public MobileDriver( String deviceName, String platformVersion, String udid ) {

        this(deviceName, platformVersion, udid, null, DEFAULT_APPIUM_PORT);
    }

    public MobileDriver( String deviceName, String platformVersion, String udid, String host ) {

        this(deviceName, platformVersion, udid, host, DEFAULT_APPIUM_PORT);
    }

    public MobileDriver( String deviceName, String platformVersion, String udid, String host, int port ) {

        this.deviceName = deviceName;
        this.platformVersion = platformVersion;
        this.port = port;
        this.udid = udid;

        if (host == null) {
            this.host = HostUtils.LOCAL_HOST_IPv4;
        } else {
            this.host = host;
            this.isWorkingRemotely = !HostUtils.isLocalHost(host);
        }
        this.isAndroidAgent = deviceName.toLowerCase().contains("android");
        this.mobileDeviceUtils = new MobileDeviceUtils(this);
    }

    /**
     *
     * @return whether the target agent is Android, otherwise it is iOS
     */
    public boolean isAndroidAgent() {

        return isAndroidAgent;
    }

    public String getHost() {

        return host;
    }

    public boolean isWorkingRemotely() {

        return isWorkingRemotely;
    }

    /**
     *
     * @param androidHome example: &quot;d:\\android\\android-sdk\\&quot;
     */
    @PublicAtsApi
    public void setAndroidHome( String androidHome ) {

        if (androidHome != null) {

            this.androidHome = androidHome;
            this.adbLocation = IoUtils.normalizeDirPath(this.androidHome) + "platform-tools/";
            String pathToADBExecutable = null;
            if (this.mobileDeviceUtils.isAdbOnWindows()) {
                pathToADBExecutable = this.adbLocation + "adb.exe";
            } else {
                pathToADBExecutable = this.adbLocation + "adb";
            }
            if (!isWorkingRemotely && !new File(pathToADBExecutable).exists()) {
                this.adbLocation = null;
            }
        }
    }

    /**
     * Start session to device
     * @deprecated Use {@link #start(String)} method instead.
     */
    @Override
    @Deprecated
    public void start() {

    }

    /**
     * Start session to device and load the application <br/>
     * @param appPath the absolute path to the application:
     * <pre>
     *       <b>iOS</b>: absolute path to simulator-compiled .app file or the bundle_id of the desired target on device
     *       <b>Android</b>: absolute path to .apk file
     * </pre>
     */
    @PublicAtsApi
    public void start( String appPath ) {

        log.info("Starting mobile testing session to device: " + getDeviceDescription());
        try {
            // http://appium.io/slate/en/master/?java#appium-server-capabilities
            DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
            desiredCapabilities.setCapability("automationName", "Appium");
            if (isAndroidAgent) {

                // start emulator:          .../sdk/tools/emulator -avd vmname
                // install application:     .../sdk/platform-tools/adb install /usr/local/apps/SecureTransportMobile.apk

                if (this.adbLocation == null) {
                    // try to set Android home and adb location from the ANDROID_HOME environment variable
                    readAndroidHomeFromEnvironment();
                    if (this.adbLocation == null) {
                        throw new MobileOperationException("You must specify a valid Android home location or define "
                                                           + ANDROID_HOME_ENV_VAR
                                                           + " environment variable. The ADB executable must be located in a 'platform-tools/' subfolder");
                    }
                }
                desiredCapabilities.setCapability("platformName", "Android");
            } else {

                desiredCapabilities.setCapability("platformName", "iOS");
            }
            desiredCapabilities.setCapability("deviceName", deviceName);
            desiredCapabilities.setCapability("platformVersion", this.platformVersion);
            if (!StringUtils.isNullOrEmpty(this.udid)) {
                desiredCapabilities.setCapability("udid", this.udid);
            }
            desiredCapabilities.setCapability("app", appPath);
            desiredCapabilities.setCapability("autoLaunch", true);
            desiredCapabilities.setCapability("newCommandTimeout", 30 * 60);
            desiredCapabilities.setCapability("noReset", true); // don’t reset settings and app state before this session
            // desiredCapabilities.setCapability( "fullReset", true ); // clean all Android/iOS settings (iCloud settings), close and uninstall the app

            URL url = new URL("http://" + this.host + ":" + this.port + "/wd/hub");
            if (isAndroidAgent) {
                driver = new AndroidDriver<WebElement>(url, desiredCapabilities);
            } else {
                driver = new IOSDriver<WebElement>(url, desiredCapabilities);
            }
            driver.setLogLevel(Level.ALL);

            // the following timeout only works for NATIVE context, but we will handle it in MobileElementState.
            // Also there is a problem when != 0. In some reason, for iOS only(maybe), this timeout acts as session timeout ?!?
            driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);

            // driver.manage().timeouts().pageLoadTimeout( 30000, TimeUnit.MILLISECONDS ); // UnsupportedCommandException
            // driver.manage().timeouts().setScriptTimeout( 10000, TimeUnit.MILLISECONDS ); // WebDriverException: Not yet implemented

            driver.context(NATIVE_CONTEXT);
            this.screenDimensions = driver.manage().window().getSize(); // must be called in NATIVE context

            mobileEngine = new MobileEngine(this, this.mobileDeviceUtils);
        } catch (Exception e) {
            throw new MobileOperationException("Error starting connection to device and application under test."
                                               + " Check if there is connection to device and the Appium server is running.",
                                               e);
        }
    }

    @Override
    public void stop() {

        log.info("Stopping mobile testing session to device: " + getDeviceDescription());

        driver.quit();
    }

    /**
     * Stop application by package name and the session to device
     * @deprecated Use {@link #stop()} method instead
     *
     * @param applicationPackage application package name
     */
    @PublicAtsApi
    @Deprecated
    public void stop( String applicationPackage ) {

        stop();
    }

    /**
     * Clear application cache
     *
     * @param applicationPackage application package name
     */
    @PublicAtsApi
    public void clearAppCache( String applicationPackage ) {

        if (isAndroidAgent) {

            // Clear application cache using ADB command: ./adb shell pm clear &lt;PACKAGE&gt;<br/>
            // for example: ./adb shell pm clear com.axway.st.mobile

            if (this.adbLocation == null) {
                throw new MobileOperationException("You must specify a valid Android home location or define "
                                                   + ANDROID_HOME_ENV_VAR
                                                   + " environment variable. The ADB executable must be located in a 'platform-tools/' subfolder");
            }
            String[] commandArguments = new String[]{ "shell", "pm", "clear", applicationPackage };
            IProcessExecutor pe = null;
            int numRetries = 0;
            while (numRetries <= MAX_ADB_RELATED_RETRIES) {
                if (numRetries > 0) {
                    log.warn("Retrying to start application action as previous try failed");
                }
                try {
                    pe = this.mobileDeviceUtils.executeAdbCommand(commandArguments, false);
                } catch (Exception e) {
                    throw new MobileOperationException("Unable to clear application cache of '"
                                                       + applicationPackage + "'", e);
                }
                numRetries++;
                if (pe.getExitCode() == 0) {
                    break;
                } else {
                    if (numRetries <= MAX_ADB_RELATED_RETRIES) {
                        log.error("Unable to clear application cache of '" + applicationPackage
                                  + "'. Start command failed (Exit code: " + pe.getExitCode() + ", STDOUT: '"
                                  + pe.getStandardOutput() + "', STDERR: '" + pe.getErrorOutput() + "')");
                        //try to kill ADB and issue start again
                        killAdbServer();
                    } else {
                        throw new MobileOperationException("Unable to clear application cache of '"
                                                           + applicationPackage
                                                           + "'. Clear cache command failed (Exit code: "
                                                           + pe.getExitCode() + ", STDOUT: '"
                                                           + pe.getStandardOutput() + "', STDERR: '"
                                                           + pe.getErrorOutput() + "')");
                    }
                }
            }
        } else {

            //TODO: Find solution. Note that "this.driver.resetApp();" doesn't reset app cache.
            throw new NotSupportedOperationException("Currently clear application cache operation for iOS is not implemented");
        }
    }

    @PublicAtsApi
    public MobileEngine getMobileEngine() {

        return mobileEngine;
    }

    /**
     * <b>NOTE:</b> This method should not be used directly into the test scripts.
     * The implementation may be changed by the Automation Framework Team without notice.
     * @return Internal Object
     */
    public Object getInternalObject( String objectName ) {

        //NOTE: we use a String argument 'objectName' not directly an InternalObjectsEnum object, because we want to
        // hide from the end users this method and his usage

        switch (InternalObjectsEnum.getEnum(objectName)) {

            case WebDriver:
                //returns instance of Appium driver operating over Native/HTML elements
                return this.driver;
            default:
                break;
        }
        return null;
    }

    /**
     *
     * @return android home location
     */
    public String getAndroidHome() {

        return androidHome;
    }

    /**
     *
     * @return adb location
     */
    public String getAdbLocation() {

        return adbLocation;
    }

    /**
     *
     * @param screenshotOnError whether to create screenshot on error
     */
    public void setScreenshotOnError( boolean screenshotOnError ) {

        // driver.setScreenshotOnError( screenshotOnError );
        // TODO: implement it
        throw new NotSupportedOperationException("Not implemented");
    }

    public Dimension getScreenDimensions() {

        return screenDimensions;
    }

    /**
     * Read ANDROID_HOME environment variable and set ADB location
     */
    private void readAndroidHomeFromEnvironment() {

        try {
            setAndroidHome(System.getenv(ANDROID_HOME_ENV_VAR));
        } catch (SecurityException se) {
            log.warn("No access to the process environment. Unable to read environment variable '"
                     + ANDROID_HOME_ENV_VAR + "'");
        }
    }

    /**
     * Start application using ADB command: ./adb shell am start -W -S -n &lt;ACTIVITY&gt;<br/>
     * for example: ./adb shell am start -W -S -n com.axway.st.mobile/.MobileAccessPlus
     *
     * @param activity application activity name
     */
    private void startAndroidApplication( String activity ) {

        log.info("Starting application with activity '" + activity + "' on device: "
                 + getDeviceDescription());

        String[] commandArguments = new String[]{ "shell", "am", "start", "-W", "-S", "-n", activity };
        int numRetries = 0;
        IProcessExecutor pe = null;
        while (numRetries <= MAX_ADB_RELATED_RETRIES) {
            if (numRetries > 0) {
                log.info("Retrying to start application action as previous try failed");
            }
            try {
                pe = this.mobileDeviceUtils.executeAdbCommand(commandArguments, false);
            } catch (Exception e) {
                throw new MobileOperationException("Unable to start Android application with activity '"
                                                   + activity + "'", e);
            }
            numRetries++;
            if (pe.getExitCode() == 0) {
                break;
            } else {
                if (numRetries <= MAX_ADB_RELATED_RETRIES) {
                    log.error("Unable to start Android application with activity '" + activity
                              + "'. Start command failed (Exit code: " + pe.getExitCode() + ", STDOUT: '"
                              + pe.getStandardOutput() + "', STDERR: '" + pe.getErrorOutput() + "')");
                    //try to kill ADB and issue start again
                    killAdbServer();
                } else {
                    throw new MobileOperationException("Unable to start Android application with activity '"
                                                       + activity + "'. Start command failed (STDOUT: '"
                                                       + pe.getStandardOutput() + "', STDERR: '"
                                                       + pe.getErrorOutput() + "')");
                }
            }
        }
    }

    /**
     * Stop application using ADB command: ./adb shell am force-stop  &lt;PACKAGE&gt;<br/>
     * for example: ./adb shell am force-stop com.axway.st.mobile
     *
     * @param applicationPackage application package
     */
    private void stopAndroidApplication( String applicationPackage ) {

        log.info("Stopping application '" + applicationPackage + "' on device: " + getDeviceDescription());

        String[] commandArguments = new String[]{ "shell", "am", "force-stop", applicationPackage };
        IProcessExecutor pe = null;
        int numRetries = 0;
        while (numRetries <= MAX_ADB_RELATED_RETRIES) {
            if (numRetries > 0) {
                log.warn("Retrying to start application action as previous try failed");
            }
            try {
                pe = this.mobileDeviceUtils.executeAdbCommand(commandArguments, false);
            } catch (Exception e) {
                throw new MobileOperationException("Unable to stop Android application with package '"
                                                   + applicationPackage + "'", e);
            }
            numRetries++;
            if (pe.getExitCode() == 0) {
                break;
            } else {
                if (numRetries <= MAX_ADB_RELATED_RETRIES) {
                    log.error("Unable to stop Android application with package '" + applicationPackage
                              + "'. Stop command failed (Exit code: " + pe.getExitCode() + ", STDOUT: '"
                              + pe.getStandardOutput() + "', STDERR: '" + pe.getErrorOutput() + "')");
                    // try to kill ADB and issue stop again
                    killAdbServer();
                } else {
                    throw new MobileOperationException("Unable to stop Android application with package '"
                                                       + applicationPackage
                                                       + "'. Stop command failed (Exit code: "
                                                       + pe.getExitCode() + ", STDOUT: '"
                                                       + pe.getStandardOutput() + "', STDERR: '"
                                                       + pe.getErrorOutput() + "')");
                }
            }
        } // while
    }

    /**
     * <pre>
     * Start iOS application using <b>ios-sim</b> command: ios-sim launch &lt;PATH TO APPLICATION.APP&gt; --timeout 60 --exit<br/>
     * for example: <i>ios-sim launch /tmp/test/MobileAccessPlus.app --timeout 60 --exit</i>
     * <br/>
     * This command also starts the iOS Simulator if it's not already started.
     * <br/>
     * Check here how to install <b>ios-sim</b>: <a href="https://github.com/phonegap/ios-sim#installation">https://github.com/phonegap/ios-sim#installation</a>
     * </pre>
     * @param appPath path to the application .app file
     */
    private void startIOSApplication( String appPath ) {

        log.info("Starting application '" + appPath + "' on device: " + getDeviceDescription());

        String[] commandArguments = new String[]{ "launch", appPath, "--timeout", "60", "--exit" };
        IProcessExecutor pe = null;
        try {
            pe = getProcessExecutorImpl("ios-sim", commandArguments);
            pe.execute();
        } catch (Exception e) {
            throw new MobileOperationException("Unable to start iOS application '" + appPath + "'", e);
        }
        if (pe.getExitCode() != 0) {
            throw new MobileOperationException("Unable to start iOS application '" + appPath
                                               + "'. Start command failed (STDOUT: '"
                                               + pe.getStandardOutput() + "', STDERR: '"
                                               + pe.getErrorOutput() + "')");
        }
    }

    /**
     * Stopping iOS Simulator
     */
    @PublicAtsApi
    public void stopIOSSimulator() {

        log.info("Stopping simulator on: " + getDeviceDescription());

        IProcessExecutor pe = null;
        try {
            // the simulator window was named "iPhone Simulator" till Xcode 6, now it is "iOS Simulator"
            pe = getProcessExecutorImpl("killall", new String[]{ "iPhone Simulator", "iOS Simulator" });
            pe.execute();
        } catch (Exception e) {
            throw new MobileOperationException("Unable to stop iOS Simulator", e);
        }
        if (pe.getExitCode() != 0) {
            throw new MobileOperationException("Unable to stop iOS Simulator. Stop command failed (STDOUT: '"
                                               + pe.getStandardOutput() + "', STDERR: '"
                                               + pe.getErrorOutput() + "')");
        }
    }

    /**
     *
     * @param command the command to run
     * @param commandArguments command arguments
     * @return {@link IProcessExecutor} implementation instance
     */
    private IProcessExecutor getProcessExecutorImpl( String command, String[] commandArguments ) {

        if (isWorkingRemotely) {
            String remoteProcessExecutorClassName = "com.axway.ats.action.processes.RemoteProcessExecutor";
            try {
                Class<?> remotePEClass = Class.forName(remoteProcessExecutorClassName);
                Constructor<?> constructor = remotePEClass.getDeclaredConstructors()[0];
                return (IProcessExecutor) constructor.newInstance(host, command, commandArguments);
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate RemoteProcessExecutor. Check whether ATS Action "
                                           + "library and ATS Agent client are added as dependencies in classpath. "
                                           + "They are needed in order to invoke code on remote machine.",
                                           e);
            }
        }
        return new LocalProcessExecutor(command, commandArguments);
    }

    /**
     *
     * @return device details as {@link String}
     */
    private String getDeviceDescription() {

        return deviceName + (host != null
                                          ? ", host: " + host
                                          : "")
               + (port > 0
                           ? ", port: " + port
                           : "");
    }

    /**
     * Kill misbehaving ADB server in order to bring it back later to normal state. The server is automatically started on next ADB command.
     * Then failed ADB operation (start/stop/clear cache, etc. should be retried.)
     */
    private void killAdbServer() {

        log.info("Trying to restart ADB server on device: " + getDeviceDescription());

        String[] commandArguments = new String[]{ "kill-server" };
        IProcessExecutor pe = null;
        try {
            pe = this.mobileDeviceUtils.executeAdbCommand(commandArguments, false);
        } catch (Exception e) {
            throw new MobileOperationException("Unable to stop ADB server. 'adb kill-server' failed", e);
        }
        if (pe.getExitCode() != 0) {
            log.warn("Unable to stop gracefully the ADB server. Command failed (Exit code: "
                     + pe.getExitCode() + ", STDOUT: '" + pe.getStandardOutput() + "', STDERR: '"
                     + pe.getErrorOutput() + "')");
            log.info("Trying to forcefully terminate ADB process");
            // fallback to taskkill
            try {
                if (OperatingSystemType.getCurrentOsType().isWindows()) {
                    pe = new LocalProcessExecutor("taskkill.exe",
                                                  new String[]{ "/IM", "adb.exe", "/F", "/T" });
                } else {
                    pe = new LocalProcessExecutor("killall",
                                                  new String[]{ "adb" });
                }
                pe.execute();
            } catch (Exception e) {
                log.info("Unable to kill ADB server. Command failed (Exit code: " + pe.getExitCode()
                         + ", STDOUT: '" + pe.getStandardOutput() + "', STDERR: '" + pe.getErrorOutput()
                         + "')");
                throw new MobileOperationException("Unable to stop ADB server with taskkill/killall", e);
            }
            if (pe.getExitCode() != 0) {
                // TODO - research possible error codes for non-existing process to kill
                log.error("Unable to kill ADB server. Command failed (Exit code: " + pe.getExitCode()
                          + ", STDOUT: '" + pe.getStandardOutput() + "', STDERR: '" + pe.getErrorOutput()
                          + "')");
            }

        }
    }
}
