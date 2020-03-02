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

import java.lang.reflect.Constructor;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.reflect.MethodFinder;
import com.axway.ats.uiengine.internal.driver.SwingDriverInternal;
import com.gargoylesoftware.htmlunit.BrowserVersion;

/**
 * Abstraction of a UI driver
 */
@PublicAtsApi
public abstract class UiDriver {

    /**
     * start the driver
     */
    @PublicAtsApi
    public abstract void start();

    /**
     * stop the driver
     */
    @PublicAtsApi
    public abstract void stop();

    /**
     *
     * @param url the URL to open
     * @return new FirefoxDriver instance
     */
    @PublicAtsApi
    public static FirefoxDriver getFirefoxDriver(
                                                  String url ) {

        return new FirefoxDriver(url);
    }

    /**
     * Get instance of driver working with Firefox browser
     * @param url the URL to open
     * @param browserPath the path to the browser starting file or
     *  the remote Selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     *
     * @return new FirefoxDriver instance
     */
    @PublicAtsApi
    public static FirefoxDriver getFirefoxDriver(
                                                  String url,
                                                  String browserPath ) {

        return new FirefoxDriver(url, browserPath);
    }

    /**
     * Get instance of driver working with Firefox browser
     * @param url the target application URL
     * @param browserPath the browser start path
     * @param remoteSeleniumURL the remote Selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     * @return new FirefoxDriver instance
     */
    @PublicAtsApi
    public static FirefoxDriver getFirefoxDriver(
                                                  String url,
                                                  String browserPath,
                                                  String remoteSeleniumURL ) {

        return new FirefoxDriver(url, browserPath, remoteSeleniumURL);
    }

    /**
     * Get instance of driver working with Internet Explorer browser
     * @param url the URL to open
     * @return new InternetExplorerDriver instance
     */
    @PublicAtsApi
    public static InternetExplorerDriver getInternetExplorerDriver(
                                                                    String url ) {

        return new InternetExplorerDriver(url);
    }

    /**
     * Get instance of driver working with Edge browser
     * @param url the URL to open
     * @param remoteSeleniumURL the remote selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     * @return new EdgeDriver instance
     */
    @PublicAtsApi
    public static EdgeDriver getEdgeDriver(
                                            String url,
                                            String remoteSeleniumURL ) {

        return new EdgeDriver(url, remoteSeleniumURL);
    }

    /**
    *
    * @param url the URL to open
    * @return new EdgeDriver instance
    */
    @PublicAtsApi
    public static EdgeDriver getEdgeDriver(
                                            String url ) {

        return new EdgeDriver(url);
    }

    /**
    *
    * @param url the URL to open
    * @param remoteSeleniumURL the remote selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
    * @return new InternetExplorerDriver instance
    */
    @PublicAtsApi
    public static InternetExplorerDriver getInternetExplorerDriver(
                                                                    String url,
                                                                    String remoteSeleniumURL ) {

        return new InternetExplorerDriver(url, remoteSeleniumURL);
    }

    /**
     * Get instance of driver working with Chrome browser
     * @param url the URL to open
     * @return new ChromeDriver instance
     */
    @PublicAtsApi
    public static ChromeDriver getChromeDriver(
                                                String url ) {

        return new ChromeDriver(url);
    }

    /**
     * Get instance of driver working with Chrome browser
     * @param url the URL to open
     * @param remoteSeleniumURL the remote Selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     * @return new ChromeDriver instance
     */
    @PublicAtsApi
    public static ChromeDriver getChromeDriver(
                                                String url,
                                                String remoteSeleniumURL ) {

        return new ChromeDriver(url, remoteSeleniumURL);
    }

    /**
     * Get instance of driver working with Safari browser
     * @param url the URL to open
     * @return new SafariDriver instance
     */
    @PublicAtsApi
    public static SafariDriver getSafariDriver(
                                                String url ) {

        return new SafariDriver(url);
    }

    /**
     *
     * @param url the URL to open
     * @param remoteSeleniumURL the remote Selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     * @return new SafariDriver instance
     */
    @PublicAtsApi
    public static SafariDriver getSafariDriver(
                                                String url,
                                                String remoteSeleniumURL ) {

        return new SafariDriver(url, remoteSeleniumURL);
    }

    /**
     *
     * @param url the URL to open
     * @return new HiddenBrowserDriver instance
     */
    @PublicAtsApi
    public static HiddenBrowserDriver getHiddenBrowserDriver(
                                                              String url ) {

        return new HiddenBrowserDriver(url);
    }

    /**
     * Get instance of driver working with HiddenBrowser (headless browser)
     * @param url the URL to open
     * @param browserVersion a {@link BrowserVersion} to emulate
     * @return new HiddenBrowserDriver instance
     */
    @PublicAtsApi
    public static HiddenBrowserDriver getHiddenBrowserDriver(
                                                              String url,
                                                              BrowserVersion browserVersion ) {

        return new HiddenBrowserDriver(url, browserVersion);
    }

    /**
    * 
    * @param url the URL to open
    * @param browserPath the browser absolute path
    * @return new PhantomJSDriver instance
    */
    @PublicAtsApi
    @Deprecated
    public static PhantomJsDriver getPhantomJSDriver(
                                                      String url,
                                                      String browserPath ) {

        return new PhantomJsDriver(url, browserPath);
    }

    /**
     *
     * @return new RobotDriver instance
     */
    @PublicAtsApi
    public static RobotDriver getRobotDriver() {

        return new RobotDriver();
    }

    /**
     * Get instance of driver working with Swing UI application
     * @param mainClassOfTestedApplication the class containing the "main" method
     * @return new SwingDriver instance
     */
    @PublicAtsApi
    public static SwingDriver getSwingDriver(
                                              Class<?> mainClassOfTestedApplication ) {

        return new SwingDriverInternal(mainClassOfTestedApplication);
    }

    /**
     *
     * @param mainClassOfTestedApplication the class containing the "main" method
     * @param windowTitle the window title
     * @return new SwingDriver instance
     */
    @PublicAtsApi
    public static SwingDriver getSwingDriver(
                                              Class<?> mainClassOfTestedApplication,
                                              String windowTitle ) {

        return new SwingDriverInternal(mainClassOfTestedApplication, windowTitle);
    }

    /**
    *
    * @param windowTitle the window title
    * @return new SwingDriver instance
    */
    @PublicAtsApi
    public static SwingDriver getSwingDriver(
                                              String windowTitle ) {

        return new SwingDriverInternal(windowTitle);
    }

    /**
     * Driver for working with JNLP applications
     * @param jnlpLocation JNLP location. It can be local JNLP file path or a remote url
     * @param cacheEnabled whether the JNLP is cached or not
     * @return new SwingDriver instance
     */
    @PublicAtsApi
    public static SwingDriver getSwingDriver(
                                              String jnlpLocation,
                                              boolean cacheEnabled ) {

        return new SwingDriverInternal(jnlpLocation, cacheEnabled);
    }

    /**
     * Driver for working with mobile applications
     * @param deviceName the kind of mobile device or emulator to use. You can use one of {@link MobileDriver} driver type constants.<br>
     *  For example: MobileDriver.DEVICE_ANDROID_EMULATOR, MobileDriver.DEVICE_IPHONE_SIMULATOR or iPhone Retina 4-inch, Galaxy S4, ... <br>
     *  On iOS, this should be one of the valid devices returned by instruments with <i>instruments -s devices</i>. <br>
     * @param platformVersion mobile OS version. For example: 8.1, 4.4 ... For Android it could be skipped (null)
     * @param udid unique device identifier of the connected physical device or null for emulator/simulator usage
     * @param host the host address of the Appium server. null may be passed if you want to work with localhost emulator
     * @return new MobileDriver instance
     */
    @PublicAtsApi
    public static MobileDriver getMobileDriver(
                                                String deviceName,
                                                String platformVersion,
                                                String udid,
                                                String host ) {

        return new MobileDriver(deviceName, platformVersion, udid, host);
    }

    /**
     * Driver for working with mobile applications
     * @param deviceName the kind of mobile device or emulator to use. You can use one of {@link MobileDriver} driver type constants.<br>
     *  For example: MobileDriver.DEVICE_ANDROID_EMULATOR, MobileDriver.DEVICE_IPHONE_SIMULATOR or iPhone Retina 4-inch, Galaxy S4, ... <br>
     *  On iOS, this should be one of the valid devices returned by instruments with <i>instruments -s devices</i>. <br>
     * @param platformVersion mobile OS version. For example: 11.4, 8.1 ... For Android it could be skipped (null)
     * @param udid unique device identifier of the connected physical device or null for emulator/simulator usage
     * @param host the host address of the Appium server. null may be passed if you want to work with localhost emulator
     * @param port the port number of the Appium server
     * @return new MobileDriver instance
     */
    @PublicAtsApi
    public static MobileDriver getMobileDriver(
                                                String deviceName,
                                                String platformVersion,
                                                String udid,
                                                String host,
                                                int port ) {

        return new MobileDriver(deviceName, platformVersion, udid, host, port);
    }

    /**
    * This method allows you to load your own driver implementation which you can use as any other UI Engine driver
    * @param driverClassName the full class name of the custom driver class
    * @param parameterTypes the parameters that the constructor requires 
    * @param constructorArguments the arguments, which will be passed to the constructor
    * @return new UiDriver instance
    */
    @PublicAtsApi
    public static UiDriver getCustomDriver(
                                            String driverClassName,
                                            Class<?>[] parameterTypes,
                                            Object[] constructorArguments ) throws Exception {

        UiDriver driver = null;
        try {
            Class<?> clss = Class.forName(driverClassName);
            Constructor<?> constructor = new MethodFinder(clss).findConstructor(parameterTypes);
            driver = (UiDriver) constructor.newInstance(constructorArguments);
        } catch (Exception e) {
            throw new Exception("Error while loading custom driver '" + driverClassName + "'", e);
        }

        return driver;
    }
}
