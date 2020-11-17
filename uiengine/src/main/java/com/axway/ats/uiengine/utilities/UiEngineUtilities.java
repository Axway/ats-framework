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
package com.axway.ats.uiengine.utilities;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.uiengine.AbstractRealBrowserDriver;
import com.axway.ats.uiengine.MobileDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.exceptions.NotSupportedOperationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;

import io.appium.java_client.AppiumDriver;

/**
 * Generic utility class
 */
@PublicAtsApi
public class UiEngineUtilities {

    private static final Logger log = LogManager.getLogger(UiEngineUtilities.class);

    /**
     * Sleep for period equal to the UI commands delay as defined in the UI Engine configuration
     */
    @PublicAtsApi
    public static void sleep() {

        sleep(UiEngineConfigurator.getInstance().getCommandDelay());
    }

    /**
     * Sleep for the specified number of milliseconds
     * @param millis
     */
    @PublicAtsApi
    public static void sleep(
                              int millis ) {

        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                // ignore this error
            }
        }
    }

    /**
     * Create a screenshot image (supported image format/type is PNG).<br>
     * If the screenshot image file already exists it will be automatically replaced by the new one.<br>
     * <br>
     * Currently the supported UI drivers are {@link AbstractRealBrowserDriver} and {@link MobileDriver}:
     * <ul>
     *     <li>{@link AbstractRealBrowserDriver} - the method makes a best effort to create a screenshot,
     *     depending on the browser to return the following in order of preference:
     *          <ul>
     *              <li>Entire page</li>
     *              <li>Current window</li>
     *              <li>Visible portion of the current frame</li>
     *              <li>The screenshot of the entire display containing the browser</li>
     *          </ul>
     *     </li>
     *     <li>{@link MobileDriver} - creates a screenshot of the mobile device screen.<br>
     *          <b>NOTE:</b> There is a <a href="https://github.com/selendroid/selendroid/issues/325">known issue</a> on Android Virtual Device with <b>"Use Host GPU"</b> enabled option.
     *          So in order to get a screenshot it should be disabled. Keep in mind that it will affect the performance, because
     *          it is a performance acceleration option.
     *     </li>
     * </ul>
     *
     * @param filePath the screenshot image file path
     * @param uiDriver {@link UiDriver} instance
     */
    @PublicAtsApi
    public static void createScreenshot(
                                         String filePath,
                                         UiDriver uiDriver ) {

        WebDriver webDriver = null;
        if (uiDriver instanceof AbstractRealBrowserDriver) {

            AbstractRealBrowserDriver browserDriver = (AbstractRealBrowserDriver) uiDriver;
            webDriver = (WebDriver) browserDriver.getInternalObject(InternalObjectsEnum.WebDriver.toString());
        } else if (uiDriver instanceof MobileDriver) {

            MobileDriver mobileDriver = (MobileDriver) uiDriver;
            webDriver = (WebDriver) mobileDriver.getInternalObject(InternalObjectsEnum.WebDriver.toString());
            ((AppiumDriver) webDriver).context(MobileDriver.NATIVE_CONTEXT);
        } else {

            throw new NotSupportedOperationException("Currently it is not possible to create a screenshot with driver: "
                                                     + uiDriver.getClass().getSimpleName());
        }
        File scrTmpFile = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE);
        File scrFile = new File(filePath);
        if (scrFile.exists() && !scrFile.delete()) {

            log.warn("The Screenshot image file '"
                     + filePath
                     + "' already exists, but couldn't be deleted. You can find the current Screenshot image here: "
                     + scrTmpFile.getAbsolutePath());
        } else if (!scrTmpFile.renameTo(scrFile)) {

            // if renameTo() fails we will try to copy the file
            try {
                new LocalFileSystemOperations().copyFile(scrTmpFile.getCanonicalPath(),
                                                         scrFile.getCanonicalPath(),
                                                         true);
                scrTmpFile.delete();
            } catch (Exception e) {
                log.warn("Unable to create Screenshot image file: " + filePath);
            }
        }

    }
}
