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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlAlert;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlConfirm;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlPrompt;
import com.axway.ats.uiengine.engine.RealHtmlEngine;
import com.axway.ats.uiengine.exceptions.ElementNotFoundException;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.internal.realbrowser.ExpectedAlert;
import com.axway.ats.uiengine.internal.realbrowser.ExpectedConfirm;
import com.axway.ats.uiengine.internal.realbrowser.ExpectedPrompt;
import com.axway.ats.uiengine.internal.realbrowser.IExpectedPopup;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * A driver operating over a real UI browser
 */
@PublicAtsApi
public abstract class AbstractRealBrowserDriver extends AbstractHtmlDriver {

    public static enum BrowserType {
        Chrome, FireFox, Edge, Safari, Opera, InternetExplorer, PhantomJS

    }

    private static Logger         log                 = LogManager.getLogger(AbstractRealBrowserDriver.class);

    private String                url;
    private String                browserPath;
    private String                remoteSeleniumURL;

    protected BrowserType         browserType;
    protected WebDriver           webDriver;

    private Queue<IExpectedPopup> expectedPopupsQueue = new LinkedList<IExpectedPopup>();

    static {
        try {
            String jarPath = AbstractRealBrowserDriver.class.getResource("AbstractRealBrowserDriver.class")
                                                            .getPath();
            if (jarPath.indexOf("!/") == -1) { // UIEngine project is directly referred , so classes are not packed yet.
                log.info("UIEngine classes seem not to be in JAR file. Supported browser versions could not be obtained.");
            } else {
                jarPath = IoUtils.normalizeFilePath(jarPath.substring(0, jarPath.indexOf("!/")));

                String pomContent = IoUtils.streamToString(IoUtils.readFileFromJar(jarPath,
                                                                                   "META-INF\\maven\\com.axway.ats.framework\\ats-uiengine\\pom.xml"));
                String propertyName = "selenium-supported.browser.versions";
                String supportedBrowserVersions = pomContent.substring(pomContent.indexOf("<"
                                                                                          + propertyName
                                                                                          + ">")
                                                                       + propertyName.length() + 2,
                                                                       pomContent.indexOf("</"
                                                                                          + propertyName
                                                                                          + ">"));
                log.info("Supported browser versions: " + supportedBrowserVersions);
            }
        } catch (Exception e) {
            log.warn("Unable to get the supported browser drivers by Selenium.", e);
        }
    }

    /**
     * @param browserType The Browser kind. Check {@link BrowserType}
     * @param url the target application URL
     * @param browserPath full path to a real UI browser or the remote selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     */
    public AbstractRealBrowserDriver( BrowserType browserType,
                                      String url,
                                      String browserPath ) {

        this.browserType = browserType;
        this.url = url;
        if (browserPath != null && browserPath.toLowerCase().startsWith("http")) {
            this.remoteSeleniumURL = browserPath;
        } else {
            this.browserPath = browserPath;
        }
    }

    /**
     * @param browserType The Browser kind. Check {@link BrowserType}
     * @param url the target application URL
     * @param browserPath full path to a real UI browser
     * @param remoteSeleniumURL the remote selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     */
    public AbstractRealBrowserDriver( BrowserType browserType,
                                      String url,
                                      String browserPath,
                                      String remoteSeleniumURL ) {

        this.browserType = browserType;
        this.url = url;
        this.browserPath = browserPath;
        this.remoteSeleniumURL = remoteSeleniumURL;
    }

    @Override
    @PublicAtsApi
    public void start() {

        try {
            log.info("Starting selenium browser with " + this.getClass().getSimpleName());
            if (browserType == BrowserType.FireFox) {

                com.axway.ats.uiengine.FirefoxDriver firefoxDriver = new com.axway.ats.uiengine.FirefoxDriver(url,
                                                                                                              browserPath,
                                                                                                              remoteSeleniumURL);

                FirefoxProfile profile = null;
                if (firefoxDriver.getProfileName() != null) {

                    profile = new ProfilesIni().getProfile(firefoxDriver.getProfileName());
                    if (profile == null) {
                        throw new SeleniumOperationException("Firefox profile '"
                                                             + firefoxDriver.getProfileName()
                                                             + "' doesn't exist");
                    }
                } else if (firefoxDriver.getProfileDirectory() != null) {

                    File profileDirectory = new File(firefoxDriver.getProfileDirectory());
                    profile = new FirefoxProfile(profileDirectory);
                } else {

                    profile = new FirefoxProfile();
                    String downloadDir = UiEngineConfigurator.getInstance().getBrowserDownloadDir();
                    // If the download dir ends with '/' or '\' browser skips the property and shows the dialog for asking
                    // for default browser. Now will FIX this behavior
                    if (downloadDir.endsWith("/") || downloadDir.endsWith("\\")) {
                        downloadDir = downloadDir.substring(0, downloadDir.length() - 1);
                    }
                    // Following options are described in http://kb.mozillazine.org/Firefox_:_FAQs_:_About:config_Entries
                    profile.setPreference("browser.download.dir", downloadDir);
                    profile.setPreference("browser.download.folderList", 2);
                    profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                                          UiEngineConfigurator.getInstance().getBrowserDownloadMimeTypes());
                    profile.setPreference("plugin.state.java", 2); // set to  "Always Activate"
                    profile.setPreference("plugin.state.flash", 2); // set to  "Always Activate"

                    profile.setAcceptUntrustedCertificates(true);
                }

                DesiredCapabilities capabilities = DesiredCapabilities.firefox();
                capabilities.setCapability(FirefoxDriver.PROFILE, profile);
                setFirefoxProxyIfAvailable(capabilities);

                if (this.browserPath != null) {
                    capabilities.setCapability(FirefoxDriver.BINARY, this.browserPath);
                }

                FirefoxOptions options = UiEngineConfigurator.getInstance().getFirefoxDriverOptions();
                if (options == null) {
                    options = new FirefoxOptions();
                }
                capabilities.setCapability(FirefoxOptions.FIREFOX_OPTIONS, options);

                if (this.remoteSeleniumURL != null) {
                    webDriver = new RemoteWebDriver(new URL(this.remoteSeleniumURL), capabilities);
                } else {
                    webDriver = new FirefoxDriver(capabilities);
                }
            }

            else if (browserType == BrowserType.InternetExplorer) {

                InternetExplorerOptions options = UiEngineConfigurator.getInstance().getInternetExplorerDriverOptions();
                if (options == null) {
                    options = new InternetExplorerOptions(DesiredCapabilities.internetExplorer());
                }

                if (this.remoteSeleniumURL != null) {

                    webDriver = new RemoteWebDriver(new URL(this.remoteSeleniumURL),
                                                    options);
                } else {
                    webDriver = new org.openqa.selenium.ie.InternetExplorerDriver(options);
                }
            }

            else if (browserType == BrowserType.Edge) {

                EdgeOptions options = UiEngineConfigurator.getInstance().getEdgeDriverOptions();
                if (options == null) {
                    options = new EdgeOptions().merge(DesiredCapabilities.edge());
                }

                if (this.remoteSeleniumURL != null) {

                    webDriver = new RemoteWebDriver(new URL(this.remoteSeleniumURL),
                                                    options);
                } else {
                    webDriver = new org.openqa.selenium.edge.EdgeDriver(options);
                }
            }

            else if (browserType == BrowserType.Chrome) {

                DesiredCapabilities capabilities = DesiredCapabilities.chrome();

                // apply Chrome options
                ChromeOptions options = UiEngineConfigurator.getInstance().getChromeDriverOptions();
                if (options == null) {
                    options = new ChromeOptions();
                }

                /* set browser download dir for Chrome Browser */
                String downloadDir = UiEngineConfigurator.getInstance().getBrowserDownloadDir();

                HashMap<String, Object> prefs = new HashMap<String, Object>();
                prefs.put("profile.default_content_settings.popups", 0);
                prefs.put("download.default_directory", downloadDir);
                options.setExperimentalOption("prefs", prefs);

                capabilities.setCapability(ChromeOptions.CAPABILITY, options);

                if (this.remoteSeleniumURL != null) {
                    webDriver = new RemoteWebDriver(new URL(this.remoteSeleniumURL), capabilities);
                } else {
                    webDriver = new org.openqa.selenium.chrome.ChromeDriver(capabilities);
                }
            }

            else if (browserType == BrowserType.Safari) {

                if (this.remoteSeleniumURL != null) {
                    webDriver = new RemoteWebDriver(new URL(this.remoteSeleniumURL),
                                                    DesiredCapabilities.safari());
                } else {
                    webDriver = new org.openqa.selenium.safari.SafariDriver();
                }
            }

            else if (browserType == BrowserType.PhantomJS) {

                DesiredCapabilities capabilities = DesiredCapabilities.phantomjs();
                capabilities.setJavascriptEnabled(true);
                capabilities.setCapability("acceptSslCerts", true);
                capabilities.setCapability("browserConnectionEnabled", true);
                capabilities.setCapability("takesScreenshot", true);

                // See: https://github.com/ariya/phantomjs/wiki/API-Reference-WebPage#settings-object
                if (System.getProperty(PhantomJsDriver.SETTINGS_PROPERTY) != null) {

                    Map<String, String> settings = extractPhantomJSCapabilityValues(System.getProperty(PhantomJsDriver.SETTINGS_PROPERTY));
                    for (Entry<String, String> capability : settings.entrySet()) {

                        capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX
                                                   + capability.getKey(), capability.getValue());
                    }
                }

                // See:  https://github.com/ariya/phantomjs/wiki/API-Reference-WebPage#wiki-webpage-customHeaders
                if (System.getProperty(PhantomJsDriver.CUSTOM_HEADERS_PROPERTY) != null) {

                    Map<String, String> customHeaders = extractPhantomJSCapabilityValues(System.getProperty(PhantomJsDriver.CUSTOM_HEADERS_PROPERTY));
                    for (Entry<String, String> header : customHeaders.entrySet()) {

                        capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX
                                                   + header.getKey(),
                                                   header.getValue());
                    }
                }

                if (this.browserPath != null) {

                    capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                                               this.browserPath);
                    System.setProperty(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                                       this.browserPath); // required from the create screenshot method
                }

                // See:  https://github.com/ariya/phantomjs/wiki/API-Reference#command-line-options
                List<String> cliArgsCapabilities = new ArrayList<String>();
                cliArgsCapabilities.add("--web-security=false");
                cliArgsCapabilities.add("--ignore-ssl-errors=true");
                if (System.getProperty(PhantomJsDriver.SSL_PROTOCOL_PROPERTY) != null) {
                    cliArgsCapabilities.add("--ssl-protocol="
                                            + System.getProperty(PhantomJsDriver.SSL_PROTOCOL_PROPERTY));
                } else {
                    cliArgsCapabilities.add("--ssl-protocol=any");
                }
                if (System.getProperty(PhantomJsDriver.HTTP_ONLY_COOKIES_PROPERTY) != null) {

                    cliArgsCapabilities.add("--cookies-file=" + PhantomJsDriver.cookiesFile);
                }
                // cliArgsCapabilities.add( "--local-to-remote-url-access=true" );
                setPhantomJSProxyIfAvailable(cliArgsCapabilities);
                capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCapabilities);
                if (this.remoteSeleniumURL != null) {
                    webDriver = new RemoteWebDriver(new URL(this.remoteSeleniumURL), capabilities);
                } else {
                    webDriver = new org.openqa.selenium.phantomjs.PhantomJSDriver(capabilities);
                }
            }

            log.info("Opening URL: " + url);
            webDriver.get(url);
            if (this instanceof com.axway.ats.uiengine.PhantomJsDriver) {
                webDriver.manage().window().setSize(new Dimension(1280, 1024));
            } else if (! (this instanceof com.axway.ats.uiengine.EdgeDriver)) {
                webDriver.manage().window().maximize();
            }
            int browserActionTimeout = UiEngineConfigurator.getInstance().getBrowserActionTimeout();
            if (browserActionTimeout > 0) {
                webDriver.manage().timeouts().setScriptTimeout(browserActionTimeout, TimeUnit.SECONDS);
            }
            if (! (this instanceof com.axway.ats.uiengine.EdgeDriver)) {
                webDriver.manage().timeouts().pageLoadTimeout(browserActionTimeout, TimeUnit.SECONDS);
            }
            // waiting for the "body" element to be loaded
            waitForPageLoaded(webDriver, UiEngineConfigurator.getInstance().getWaitPageToLoadTimeout());

        } catch (Exception e) {
            throw new SeleniumOperationException("Error starting Selenium", e);
        }
    }

    public void waitForPageLoaded( WebDriver driver, int timeoutInSeconds ) {

        /*InternetExplorer is unable to wait for document's readyState to be complete.*/
        if (this instanceof com.axway.ats.uiengine.InternetExplorerDriver) {
            return;
        }

        ExpectedCondition<Boolean> expectation = new ExpectedCondition<Boolean>() {
            public Boolean apply(
                                  WebDriver driver ) {

                return "complete".equals( ((JavascriptExecutor) driver).executeScript("return document.readyState"));
            }
        };

        Wait<WebDriver> wait = new WebDriverWait(driver, timeoutInSeconds);
        try {
            wait.until(expectation);
        } catch (Exception e) {
            throw new SeleniumOperationException("Timeout waiting for Page Load Request to complete.", e);
        }
    }

    @Override
    @PublicAtsApi
    public void stop() {

        if (webDriver == null) {
            log.warn("Ignoring browser driver stop(). start() method has not been invoked or browser initialization had failed.");
            return;
        }

        if (System.getProperty(PhantomJsDriver.HTTP_ONLY_COOKIES_PROPERTY) != null) {
            new File(PhantomJsDriver.cookiesFile).delete();
        }

        log.info("Stopping selenium browser with " + this.getClass().getSimpleName());
        webDriver.quit();
    }

    /**
     * @return an HtmlEngine instance
     */
    @Override
    @PublicAtsApi
    public RealHtmlEngine getHtmlEngine() {

        if (webDriver == null) {
            throw new IllegalStateException("Browser driver is not initialized. Either start() method is not invoked "
                                            + "or browser initialization had failed.");
        }

        return new RealHtmlEngine(this);
    }

    /**
     * <b>NOTE:</b> This method should not be used directly into the test scripts.
     * The implementation may be changed by the Automation Framework Team without notice.
     * @return Internal Object
     */
    public Object getInternalObject(
                                     String objectName ) {

        //NOTE: we use a String argument 'objectName' not directly an InternalObjectsEnum object, because we want to
        // hide from the end users this method and his usage

        switch (InternalObjectsEnum.getEnum(objectName)) {

            case Engine:
                // returns instance of engine operating over HTML pages
                return new RealHtmlEngine(this);
            case WebDriver:
                // returns current Selenium Web Driver
                return this.webDriver;
            default:
                break;
        }
        return null;
    }

    /**
     * <b>Note:</b> For internal use only
     */
    public void addExpectedPopup(
                                  IExpectedPopup expectedPopup ) {

        expectedPopupsQueue.add(expectedPopup);
    }

    /**
     * <b>Note:</b> For internal use only
     */
    public boolean containsExpectedPopup(
                                          IExpectedPopup expectedPopup ) {

        return expectedPopupsQueue.contains(expectedPopup);
    }

    /**
     * <b>Note:</b> For internal use only
     */
    public void clearExpectedPopups() {

        expectedPopupsQueue.clear();
    }

    /**
     * <b>Note:</b> For internal use only
     */
    public void handleExpectedPopups() {

        while (!expectedPopupsQueue.isEmpty()) {

            IExpectedPopup expectedPopup = expectedPopupsQueue.poll();
            if (expectedPopup instanceof ExpectedAlert) {

                ExpectedAlert expectedAlert = (ExpectedAlert) expectedPopup;

                new RealHtmlElementState(new RealHtmlAlert(this)).waitToBecomeExisting();

                Alert alert = getAlert();
                if (expectedAlert.expectedText != null
                    && !expectedAlert.expectedText.equals(alert.getText())) {

                    throw new VerificationException("The expected alert message was: '"
                                                    + expectedAlert.expectedText
                                                    + "', but actually it is: '" + alert.getText() + "'");
                }
                alert.accept();
            } else if (expectedPopup instanceof ExpectedPrompt) {

                ExpectedPrompt expectedPrompt = (ExpectedPrompt) expectedPopup;

                new RealHtmlElementState(new RealHtmlPrompt(this)).waitToBecomeExisting();

                Alert prompt = getAlert();
                if (expectedPrompt.expectedText != null
                    && !expectedPrompt.expectedText.equals(prompt.getText())) {

                    throw new VerificationException("The expected prompt text was: '"
                                                    + expectedPrompt.expectedText
                                                    + "', but actually it is: '" + prompt.getText() + "'");
                }
                if (expectedPrompt.clickOk) {
                    prompt.sendKeys(expectedPrompt.promptValueToSet);
                    prompt.accept();
                } else {
                    prompt.dismiss();
                }

            } else if (expectedPopup instanceof ExpectedConfirm) {

                ExpectedConfirm expectedConfirm = (ExpectedConfirm) expectedPopup;

                new RealHtmlElementState(new RealHtmlConfirm(this)).waitToBecomeExisting();

                Alert confirm = getAlert();
                if (expectedConfirm.expectedText != null
                    && !expectedConfirm.expectedText.equals(confirm.getText())) {

                    throw new VerificationException("The expected confirmation message was: '"
                                                    + expectedConfirm.expectedText
                                                    + "', but actually it is: '" + confirm.getText() + "'");
                }

                if (expectedConfirm.clickOk) {
                    confirm.accept();
                } else {
                    confirm.dismiss();
                }
            }
            UiEngineUtilities.sleep();
        }
    }

    /**
     *
    * @return {@link Alert} object representing HTML alert, prompt or confirmation modal dialog
     */
    private Alert getAlert() {

        try {
            return this.webDriver.switchTo().alert();
        } catch (NoAlertPresentException e) {
            throw new ElementNotFoundException(e);
        }
    }
    

    private void setFirefoxProxyIfAvailable(
                                             DesiredCapabilities capabilities ) {

        if (!StringUtils.isNullOrEmpty(AtsSystemProperties.SYSTEM_HTTP_PROXY_HOST)
            && !StringUtils.isNullOrEmpty(AtsSystemProperties.SYSTEM_HTTP_PROXY_PORT)) {

            capabilities.setCapability(CapabilityType.PROXY,
                                       new Proxy().setHttpProxy(AtsSystemProperties.SYSTEM_HTTP_PROXY_HOST
                                                                + ':'
                                                                + AtsSystemProperties.SYSTEM_HTTP_PROXY_PORT));
        }
    }

    @Deprecated
    private void setPhantomJSProxyIfAvailable(
                                               List<String> cliArgsCapabilities ) {

        if (!StringUtils.isNullOrEmpty(AtsSystemProperties.SYSTEM_HTTP_PROXY_HOST)
            && !StringUtils.isNullOrEmpty(AtsSystemProperties.SYSTEM_HTTP_PROXY_PORT)) {

            cliArgsCapabilities.add("--proxy=" + AtsSystemProperties.SYSTEM_HTTP_PROXY_HOST + ":"
                                    + AtsSystemProperties.SYSTEM_HTTP_PROXY_PORT);
            // cliArgsCap.add("--proxy-auth=username:password");
            // cliArgsCap.add("--proxy-type=http");
        }
    }

    @Deprecated
    private Map<String, String> extractPhantomJSCapabilityValues(
                                                                 String capabiliesString ) {

       Map<String, String> capabilities = new HashMap<String, String>();
       if (capabiliesString.indexOf('{') > -1) {

           // there are multiple capabilities in format:  {cap 1: value 1}, {cap 2 : value 2}
           Pattern multipleCapablitiesPattern = Pattern.compile("\\{\\s*([^:]+)\\s*:\\s*([^\\}]+)\\},?");
           Matcher matcher = multipleCapablitiesPattern.matcher(capabiliesString);
           while (matcher.find()) {
               capabilities.put(matcher.group(1).trim(), matcher.group(2).trim());
           }
       } else {

           capabilities.put(capabiliesString.substring(0, capabiliesString.indexOf(':')).trim(),
                            capabiliesString.substring(capabiliesString.indexOf(':') + 1).trim());
       }
       return capabilities;
   }


}
