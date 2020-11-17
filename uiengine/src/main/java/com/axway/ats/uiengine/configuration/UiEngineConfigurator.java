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
package com.axway.ats.uiengine.configuration;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerOptions;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.config.AbstractConfigurator;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.core.utils.IoUtils;

/**
 * This class is used to read a set of properties used by the UI Engine
 */
@PublicAtsApi
public class UiEngineConfigurator extends AbstractConfigurator {

    private static Logger                  log                                   = LogManager.getLogger(UiEngineConfigurator.class);
    // the configuration file this class reads
    private static final String            PROPERTIES_FILE_NAME                  = "/ats.uiengine.properties";

    // the property keys in the configuration file
    private static final String PROPERTY_MAP_FILES_BASE_DIRECTORY     = "uiengine.mapFilesBaseDir";
    private static final String PROPERTY_COMMAND_DELAY                = "uiengine.commandDelay";
    private static final String PROPERTY_ELEMENT_STATE_CHANGE_DELAY   = "uiengine.elementStateChangeDelay";
    private static final String PROPERTY_HIGHLIGHT_ELEMENTS           = "uiengine.highlightElements";
    /**
     * Property to be removed. Requires internal Selenium code
     */
    @Deprecated
    private static final String PROPERTY_WORK_WITH_INVISIBLE_ELEMENTS = "uiengine.workWithInvisibleElements";

    private static final String            PROPERTY_BROWSER_ACTION_TIMEOUT       = "uiengine.browser.action.timeout";
    private static final String            PROPERTY_BROWSER_DOWNLOAD_DIR         = "browser.download.dir";
    private static final String            PROPERTY_BROWSER_DOWNLOAD_MIME_TYPES  = "browser.download.mimeTypes";

    private static final String            PROPERTY_WAIT_PAGE_TO_LOAD_TIMEOUT    = "uiengine.wait.page.to.load.timeout";

    /**
     * Options for starting Chrome Selenium driver.
     * These are not kept in the regular key-value map as it can be a complex object.
     */
    private static ChromeOptions           chromeOptions;

    /**
     * Options for starting Firefox Selenium driver.
     * These are not kept in the regular key-value map as it can be a complex object.
     */
    private static FirefoxOptions          firefoxOptions;

    /**
     * Options for starting Internet Explorer Selenium driver.
     * These are not kept in the regular key-value map as it can be a complex object.
     */
    private static InternetExplorerOptions internetExplorerOptions;

    /**
     * Options for starting Edge Selenium driver.
     * These are not kept in the regular key-value map as it can be a complex object.
     */
    private static EdgeOptions             edgeOptions;

    /**
     * The singleton instance for this configurator
     */
    private static UiEngineConfigurator    instance;

    private UiEngineConfigurator( String configurationSource ) {

        super();

        //add the resource to the repository
        addConfigFileFromClassPath(configurationSource, true, false);
    }

    @PublicAtsApi
    public static synchronized UiEngineConfigurator getInstance() {

        if (instance == null) {
            instance = new UiEngineConfigurator(PROPERTIES_FILE_NAME);
        }
        return instance;
    }

    /**
     * @return the map files' root directory
     */
    @PublicAtsApi
    public String getMapFilesBaseDir() {

        return getProperty(PROPERTY_MAP_FILES_BASE_DIRECTORY);
    }

    /**
     * Overwrite the map files' root directory
     * @param mapFilesBaseDir the map files' root directory
     */
    @PublicAtsApi
    public void setMapFilesBaseDir( String mapFilesBaseDir ) {

        setTempProperty(PROPERTY_MAP_FILES_BASE_DIRECTORY, mapFilesBaseDir);
    }

    /**
     * @return the delay between UI commands
     */
    @PublicAtsApi
    public int getCommandDelay() {

        return getIntegerProperty(PROPERTY_COMMAND_DELAY);
    }

    /**
     * Overwrite the delay between UI command
     * @param commandDelay the delay between UI command
     */
    @PublicAtsApi
    public void setCommandDelay( int commandDelay ) {

        setTempProperty(PROPERTY_COMMAND_DELAY, Integer.toString(commandDelay));
    }

    /**
     * @return the delay waiting for element's state to change or to be found. In milliseconds
     */
    @PublicAtsApi
    public int getElementStateChangeDelay() {

        return getIntegerProperty(PROPERTY_ELEMENT_STATE_CHANGE_DELAY);
    }

    /**
     * Overwrite the delay waiting for element's state to change or to be found
     * @param elementStateChangeDelay the delay waiting for element's state to change. In milliseconds
     */
    @PublicAtsApi
    public void setElementStateChangeDelay( int elementStateChangeDelay ) {

        setTempProperty(PROPERTY_ELEMENT_STATE_CHANGE_DELAY, Integer.toString(elementStateChangeDelay));
    }

    /**
     * @return whether will try to highlight elements
     */
    @PublicAtsApi
    public boolean getHighlightElements() {

        return getBooleanProperty(PROPERTY_HIGHLIGHT_ELEMENTS);
    }

    /**
     * Overwrite whether will try to highlight elements
     * @param highlightElements whether will try to highlight elements
     */
    @PublicAtsApi
    public void setHighlightElements( boolean highlightElements ) {

        setTempProperty(PROPERTY_HIGHLIGHT_ELEMENTS, Boolean.toString(highlightElements));
    }

    /**
     * @return the timeout in seconds for waiting a browser action to finish
     */
    @PublicAtsApi
    public int getBrowserActionTimeout() {

        try {
            return getIntegerProperty(PROPERTY_BROWSER_ACTION_TIMEOUT);
        } catch (NoSuchPropertyException nspe) {
            log.info("Setting default page load time to 60 seconds.");
            return 60;
        }
    }

    /**
     * Overwrite the timeout in seconds for waiting a browser action to finish
     * This method must be called before starting the browser.
     *
     * @param actionTimeout a timeout in second for waiting a browser action to finish
     */
    @PublicAtsApi
    public void setBrowserActionTimeout( int actionTimeout ) {

        setTempProperty(PROPERTY_BROWSER_ACTION_TIMEOUT, Integer.toString(actionTimeout));
    }

    @Override
    protected void reloadData() {

        //no need to do anything
    }

    /**
     * @return browser download directory (absolute path)
     */
    @PublicAtsApi
    public String getBrowserDownloadDir() {

        String downloadDir = null;
        try {
            downloadDir = getProperty(PROPERTY_BROWSER_DOWNLOAD_DIR);
        } catch (NoSuchPropertyException nspe) {
            log.warn(nspe.getMessage());
        }
        if (downloadDir == null || downloadDir.trim().isEmpty()) {
            try {
                downloadDir = new File(AtsSystemProperties.SYSTEM_USER_TEMP_DIR).getCanonicalPath();
                setBrowserDownloadDir(downloadDir);
            } catch (IOException e) {
                log.error("Can't get canonical path of the system temp directory.");
            }
        }
        return IoUtils.normalizeDirPath(downloadDir);
    }

    /**
     * Overwrite browser download directory (absolute path)
     * @param directory - browser download directory (absolute path)
     */
    @PublicAtsApi
    public void setBrowserDownloadDir( String directory ) {

        setTempProperty(PROPERTY_BROWSER_DOWNLOAD_DIR, directory);
    }

    /**
     * @return MIME types of files (comma separated) which will be automatically saved to disk by the browser
     */
    @PublicAtsApi
    public String getBrowserDownloadMimeTypes() {

        String downloadMimeTypes = null;
        try {
            downloadMimeTypes = getProperty(PROPERTY_BROWSER_DOWNLOAD_MIME_TYPES);
        } catch (NoSuchPropertyException nspe) {
            downloadMimeTypes = "";
            log.warn(nspe.getMessage());
        }
        return downloadMimeTypes;
    }

    /**
     * Overwrite MIME types of files (comma separated) which will be automatically saved to disk by the browser
     * @param downloadMimeTypes MIME types of files (comma separated) which will be automatically saved
     * to disk by the browser
     */
    @PublicAtsApi
    public void setBrowserDownloadMimeTypes( String downloadMimeTypes ) {

        setTempProperty(PROPERTY_BROWSER_DOWNLOAD_MIME_TYPES, downloadMimeTypes);
    }

    /**
     * @return the timeout in seconds for waiting a web page to get loaded
     */
    @PublicAtsApi
    public int getWaitPageToLoadTimeout() {

        try {
            return getIntegerProperty(PROPERTY_WAIT_PAGE_TO_LOAD_TIMEOUT);
        } catch (NoSuchPropertyException nspe) {
            return -1;
        }
    }

    /**
     * Overwrite the timeout in seconds for waiting a web page to get loaded
     *
     * @param pageLoadTimeout the new timeout
     */
    @PublicAtsApi
    public void setWaitPageToLoadTimeout( int pageLoadTimeout ) {

        setTempProperty(PROPERTY_WAIT_PAGE_TO_LOAD_TIMEOUT, Integer.toString(pageLoadTimeout));
    }

    /**
     *
     * @return <code>true</code> if we will work with the invisible elements too
     * @deprecated To be removed as clicking on invisible elements is not recommended
     */
    @Deprecated
    @PublicAtsApi
    public boolean isWorkWithInvisibleElements() {

        try {
            return getBooleanProperty(PROPERTY_WORK_WITH_INVISIBLE_ELEMENTS);
        } catch (NoSuchPropertyException nspe) {
            return Boolean.FALSE;
        }
    }

    /**
     *
     * @param workWithInvisibleElements whether to work with the invisible elements too
     * @deprecated To be removed as clicking on invisible elements is not recommended
     */
    @Deprecated
    @PublicAtsApi
    public void setWorkWithInvisibleElements( boolean workWithInvisibleElements ) {

        setTempProperty(PROPERTY_WORK_WITH_INVISIBLE_ELEMENTS,
                        Boolean.toString(workWithInvisibleElements));
    }

    /**
     * @return the Chrome options
     */
    @PublicAtsApi
    public ChromeOptions getChromeDriverOptions() {

        return chromeOptions;
    }

    /**
     * Pass options which will be applied when starting a Chrome browser through Selenium
     * @param options Chrome options
     */
    @PublicAtsApi
    public void setChromeDriverOptions( ChromeOptions options ) {

        chromeOptions = options;
    }

    /**
     * @return the Firefox options
     */
    @PublicAtsApi
    public FirefoxOptions getFirefoxDriverOptions() {

        return firefoxOptions;
    }

    /**
     * Pass options which will be applied when starting a Firefox browser through Selenium
     * @param options Firefox options
     */
    @PublicAtsApi
    public void setFirefoxDriverOptions( FirefoxOptions options ) {

        firefoxOptions = options;
    }

    /**
     * Pass options which will be applied when starting a Internet Explorer browser through Selenium
     * @param options Internet Explorer options
     */
    @PublicAtsApi
    public void setInternetExplorerDriverOptions( InternetExplorerOptions options ) {

        internetExplorerOptions = options;
    }

    /**
     * @return the InternetExplorer options
     */
    @PublicAtsApi
    public InternetExplorerOptions getInternetExplorerDriverOptions() {

        return internetExplorerOptions;
    }

    /**
     * Pass options which will be applied when starting a Edge browser through Selenium
     * @param options Internet Explorer options
     */
    @PublicAtsApi
    public void setEdgeDriverOptions( EdgeOptions options ) {

        edgeOptions = options;
    }

    /**
     * @return the Edge options
     */
    public EdgeOptions getEdgeDriverOptions() {

        return edgeOptions;
    }
}
