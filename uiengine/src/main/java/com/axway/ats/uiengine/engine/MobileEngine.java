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
package com.axway.ats.uiengine.engine;

import java.time.Duration;

import io.appium.java_client.ios.IOSDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.MobileDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.AbstractElementsFactory;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.mobile.MobileButton;
import com.axway.ats.uiengine.elements.mobile.MobileCheckBox;
import com.axway.ats.uiengine.elements.mobile.MobileElement;
import com.axway.ats.uiengine.elements.mobile.MobileElementFinder;
import com.axway.ats.uiengine.elements.mobile.MobileElementsFactory;
import com.axway.ats.uiengine.elements.mobile.MobileLabel;
import com.axway.ats.uiengine.elements.mobile.MobileLink;
import com.axway.ats.uiengine.elements.mobile.MobileTextBox;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.internal.engine.AbstractEngine;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.mobile.FileInfo;
import com.axway.ats.uiengine.utilities.mobile.MobileDeviceUtils;
import com.axway.ats.uiengine.utilities.mobile.MobileElementState;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;

/**
 * Engine operating with mobile application/device
 */
@PublicAtsApi
public class MobileEngine extends AbstractEngine {
    private static final Logger LOG = LogManager.getLogger(MobileEngine.class);

    private AppiumDriver<? extends WebElement> appiumDriver;

    private MobileElementsFactory mobileElementsFactory;

    private MobileDeviceUtils     mobileDeviceUtils;

    private Device                device;

    public MobileEngine( UiDriver uiDriver,
                         MobileDeviceUtils mobileDeviceUtils ) {

        this(uiDriver, MobileElementsFactory.getInstance());
        this.mobileElementsFactory = (MobileElementsFactory) elementsFactory;
        this.mobileDeviceUtils = mobileDeviceUtils;
        this.device = new Device();
    }

    public MobileEngine( UiDriver uiDriver,
                         AbstractElementsFactory elementsFactory ) {

        super(uiDriver, elementsFactory);

        MobileDriver mobileDriver = (MobileDriver) uiDriver;
        appiumDriver = (AppiumDriver<? extends WebElement>) mobileDriver.getInternalObject(
                InternalObjectsEnum.WebDriver.name());
    }

    /**
     * Get current application as String
     */
    @PublicAtsApi
    public String getApplicationName() {

        if (appiumDriver instanceof AndroidDriver) {
            return ((AndroidDriver<?>) appiumDriver).currentActivity();
        }
        return ""; //TODO: return the current activity/bundleId for IOSDriver too
    }

    /**
     * Get access to button element (native/or HTML one)
     * @param mapId the element's map id
     * @return a new {@link MobileButton} instance
     */
    @PublicAtsApi
    public MobileButton getButton(
                                   String mapId ) {

        return this.mobileElementsFactory.getMobileButton(mapId, uiDriver);
    }

    /**
     * Get access to button element (native/or HTML one)
     * @param properties properties describing this element
     * @return a new {@link MobileButton} instance
     */
    @PublicAtsApi
    public MobileButton getButton(
                                   UiElementProperties properties ) {

        return this.mobileElementsFactory.getMobileButton(properties, uiDriver);
    }

    /**
     * Get access to text box element (native/or HTML one where text edit is accepted)
     * @param mapId the element's map id
     * @return a new {@link MobileTextBox} instance
     */
    @PublicAtsApi
    public MobileTextBox getTextBox(
                                     String mapId ) {

        return mobileElementsFactory.getMobileTextBox(mapId, uiDriver);
    }

    /**
     * Get access to text box element (native/or HTML one where text edit is accepted)
     * @param properties properties describing this element
     * @return a new {@link MobileTextBox} instance
     */
    @PublicAtsApi
    public MobileTextBox getTextBox(
                                     UiElementProperties properties ) {

        return mobileElementsFactory.getMobileTextBox(properties, uiDriver);
    }

    /**
     * Get access to text label element (native/or HTML one where text edit is not accepted)
     * @param mapId the element's map id
     * @return a new {@link MobileLabel} instance
     */
    @PublicAtsApi
    public MobileLabel getLabel(
                                 String mapId ) {

        return mobileElementsFactory.getMobileLabel(mapId, uiDriver);
    }

    /**
     * Get access to text box element (native/or HTML one where text edit is not accepted)
     * @param properties properties describing this element
     * @return a new {@link MobileLabel} instance
     */
    @PublicAtsApi
    public MobileLabel getLabel(
                                 UiElementProperties properties ) {

        return mobileElementsFactory.getMobileLabel(properties, uiDriver);
    }

    /**
     * Get access to check box element (native/or HTML one)
     * @param mapId the element's map id
     * @return a new {@link MobileCheckBox} instance
     */
    @PublicAtsApi
    public MobileCheckBox getCheckBox(
                                       String mapId ) {

        return mobileElementsFactory.getMobileCheckBox(mapId, uiDriver);
    }

    /**
     * Get access to check box element (native/or HTML one)
     * @param properties properties describing this element
     * @return a new {@link MobileCheckBox} instance
     */
    @PublicAtsApi
    public MobileCheckBox getCheckBox(
                                       UiElementProperties properties ) {

        return mobileElementsFactory.getMobileCheckBox(properties, uiDriver);
    }

    /**
     * Get access to link( href) element
     * @param mapId the element's map id
     * @return a new {@link MobileLink} instance
     */
    @PublicAtsApi
    public MobileLink getLink(
                               String mapId ) {

        return mobileElementsFactory.getMobileLink(mapId, uiDriver);
    }

    /**
     * Get access to link element
     * @param properties properties describing this element
     * @return a new {@link MobileLink} instance
     */
    @PublicAtsApi
    public MobileLink getLink(
                               UiElementProperties properties ) {

        return mobileElementsFactory.getMobileLink(properties, uiDriver);
    }

    /**
     * Get access to generic UI element (could be used for div/span or other UI type element)
     * @param mapId the element's map id
     * @return a new {@link MobileElement} instance
     */
    @PublicAtsApi
    public MobileElement<?> getElement(
                                        String mapId ) {

        return mobileElementsFactory.getMobileElement(mapId, uiDriver);
    }

    /**
     * Get access to generic UI element (could be used for div/span or other UI type element)
     * @param properties properties describing this element
     * @return a new {@link MobileElement} instance
     */
    @PublicAtsApi
    public MobileElement<?> getElement(
                                        UiElementProperties properties ) {

        return mobileElementsFactory.getMobileElement(properties, uiDriver);
    }

    /**
     * Check element state
     * @param uiElement the element to work with
     * @return a utility class for checking the state of element
     */
    @PublicAtsApi
    public MobileElementState getUtilsElementState(
                                                    UiElement uiElement ) {

        return new MobileElementState(uiElement);
    }

    @PublicAtsApi
    public String executeScriptAndReturn(
                                          String method,
                                          Object... args ) {

        return (String) this.appiumDriver.executeScript(method, args);
    }

    @PublicAtsApi
    public void executeScript(
                               String method,
                               Object... args ) {

        this.appiumDriver.executeAsyncScript(method, args);
    }

    /**
     * Get page source as String
     *
     * @return page source
     */
    @PublicAtsApi
    public String getPageSource() {

        return this.appiumDriver.getPageSource();
    }

    @PublicAtsApi
    public String getContext() {

        return MobileElementFinder.defaultContext;
    }

    @PublicAtsApi
    public void setContext(
                            String context ) {

        MobileElementFinder.defaultContext = context;
    }

    @PublicAtsApi
    public String[] getAvailableContexts() {

        return (String[]) this.appiumDriver.getContextHandles()
                                           .toArray(new String[this.appiumDriver.getContextHandles()
                                                                                .size()]);
    }

    @PublicAtsApi
    public Device getDevice() {

        return this.device;
    }

    @PublicAtsApi
    public class Device {

        /**
         *  <p>Install application.</p>
         *  <p>
         *      <em>NOTE</em>: 'adb' executable must be in the $PATH variable in the environment of the Appium server<br>
         *      Example:  export PATH=/opt/android/android-sdk_r24.0.2/platform-tools/:$PATH
         *  </p>
         *  @param appPath application absolute path or relative to the started Appium server
         */
        @PublicAtsApi
        public void installApp(
                                String appPath ) {

            appiumDriver.installApp(appPath);
        }

        /**
         *
         * @param appName <app_package>/.<app_Activity> for Android (e.g. com.axway.st.mobile/.SecureTransportMobile)
         *          or bundle ID for iOS
         * @return <true> if the application is installed or <false> if it is not
         */
        @PublicAtsApi
        public boolean isAppInstalled(
                                       String appName ) {

            return appiumDriver.isAppInstalled(appName);
        }

        /**
         *
         * @param appName <app_package>/.<app_Activity> for Android (e.g. com.axway.st.mobile/.SecureTransportMobile)
         *          or bundle ID for iOS
         */
        @PublicAtsApi
        public void removeApp(
                               String appName ) {

            appiumDriver.removeApp(appName);
        }

        @PublicAtsApi
        public void resetApp() {

            appiumDriver.resetApp();
        }

        @PublicAtsApi
        public void pressHWButtonBack() {

            appiumDriver.navigate().back();
        }

        @PublicAtsApi
        public void pressHWButtonHome() {

            if (appiumDriver instanceof AndroidDriver) {

                // old: ((AndroidDriver<?>) appiumDriver).sendKeyEvent(AndroidKeyCode.HOME);
                // https://stackoverflow.com/questions/51854004/how-to-automate-home-back-up-down-menu-button-at-bottom-of-android-phone-using-a
                ((AndroidDriver<?>) appiumDriver).pressKey(new KeyEvent(AndroidKey.HOME));
            } else {
                // Special workaround - put in background with special duration of -1 to not return it back in foreground 
                // https://discuss.appium.io/t/how-to-press-home-key-button-with-appium-on-ios-real-device/14826
                appiumDriver.runAppInBackground(Duration.ofSeconds(-1));
            }
        }

        /**
         * Runs the current app as a background app for the number of seconds
         * requested. This is a synchronous method, it returns after the back has
         * been returned to the foreground.
         *
         * @param seconds number of seconds to run App in background
         */
        @PublicAtsApi
        public void runAppInBackground(
                                        int seconds ) {

            appiumDriver.runAppInBackground(Duration.ofSeconds(seconds));
        }

        /**
         * Runs the current app as a background app for the number of seconds
         * requested. This is a synchronous method, it returns after the back has
         * been returned to the foreground.
         *
         * @param seconds number of seconds to run App in background
         * @param appName <app_package>/.<app_Activity> for Android (e.g. com.axway.st.mobile/.SecureTransportMobile)
         *          or bundle ID for iOS
         *
         */
        @PublicAtsApi
        public void runAppInBackground(
                                        int seconds,
                                        String appName ) {

            if (appiumDriver instanceof AndroidDriver) {
                // TODO: check behavior with latest impl.: runInBackground

                // we can get the current activity, but not its package
                //String currentActivity = ( ( AndroidDriver<?> ) appiumDriver ).currentActivity();
                pressHWButtonHome();
                UiEngineUtilities.sleep(seconds * 1000);

                // bring application to foreground
                mobileDeviceUtils.executeAdbCommand(new String[]{ "shell", "am", "start", "-n", appName },
                                                    true);
            } else {
                // TODO: alternative is to start app if not started and then bring it in background 
                appiumDriver.runAppInBackground(Duration.ofSeconds(seconds));
            }
        }

        /**
         * Calculate file MD5 sum directly on the mobile device
         *
         * @param filePath file absolute path
         * <br>
         * <b>Note for iOS:</b> You can specify relative path too, by skipping the root slash '/' at the beginning
         *   and pass the path relative to the application data folder<br>
         * For example: <i>"Documents/MyAppFiles/IMG_0001.PNG"</i><br>
         * and we'll internally search for:
         *  <i>"/Users/&lt;username&gt;/Library/Developer/CoreSimulator/Devices/&lt;device_id&gt;/data/Containers/Data/Application/&lt;app_id&gt;/Documents/MyAppFiles/IMG_0001.PNG"</i><br>
         * which is the iOS Simulator application data folder path
         *
         * @return the MD5 sum of the specified file
         */
        @PublicAtsApi
        public String getMD5Sum(
                                 String filePath ) {

            return mobileDeviceUtils.getMD5Sum(filePath);
        }

        /**
         * List files and directories on the mobile device
         *
         * @param path a path on the mobile device. It can be a directory or file (if you want to check
         * for specific file existence)
         * <br>
         * <b>Note for iOS:</b> You have to specify a relative path to the application data folder<br>
         * For example: <i>"Documents/MyAppFiles"</i><br>
         * and we'll internally search for files in:
         *  <i>"/Users/&lt;username&gt;/Library/Developer/CoreSimulator/Devices/&lt;device_id&gt;/data/Containers/Data/Application/&lt;app_id&gt;/Documents/MyAppFiles/"</i><br>
         * which is the iOS Simulator application data folder path
         *
         * @return {@link FileInfo} array with all the files and folders from the target path
         */
        @PublicAtsApi
        public FileInfo[] listFiles(
                                     String path ) {

            return mobileDeviceUtils.listFiles(path);
        }

        /**
         * Deletes a directory from the mobile device
         *
         * @param directoryPath the directory for deletion
         * @param recursively whether to delete the internal directories recursively
         *
         * <b>Note for iOS:</b> You can specify relative directory path too, by skipping the root slash '/' at the beginning
         *   and pass the path relative to the application data folder<br>
         * For example: <i>"Documents/MyAppFiles"</i><br>
         * and we'll internally search for:
         *  <i>"/Users/&lt;username&gt;/Library/Developer/CoreSimulator/Devices/&lt;device_id&gt;/data/Containers/Data/Application/&lt;app_id&gt;/Documents/MyAppFiles/"</i><br>
         * which is the iOS Simulator application data folder path
         */
        @PublicAtsApi
        public void deleteDirectory(
                                     String directoryPath,
                                     boolean recursively ) {

            mobileDeviceUtils.deleteDirectory(directoryPath, recursively);
        }

        /**
         * Deletes a file from the mobile device
         *
         * @param filePath the file for deletion
         *
         * <b>Note for iOS:</b> You can specify relative file path too, by skipping the root slash '/' at the beginning
         *   and pass the path relative to the application data folder<br>
         * For example: <i>"Documents/MyAppFiles/fileToDelete"</i><br>
         * and we'll internally search for:
         *  <i>"/Users/&lt;username&gt;/Library/Developer/CoreSimulator/Devices/&lt;device_id&gt;/data/Containers/Data/Application/&lt;app_id&gt;/Documents/MyAppFiles/fileToDelete"</i><br>
         * which is the iOS Simulator application data folder path
         */
        @PublicAtsApi
        public void deleteFile(
                                String filePath ) {

            mobileDeviceUtils.deleteFile(filePath);
        }

        /**
         * Creates directory on mobile device
         *
         * @param directoryPath the directory for creation
         */
        @PublicAtsApi
        public void createDirectory(
                                     String directoryPath ) {

            mobileDeviceUtils.createDirectory(directoryPath);
        }

        /**
         * Copies file to device
         *
         * @param localFilePath local file path
         * @param deviceFilePath file path on the device
         */
        @PublicAtsApi
        public void copyFileTo(
                                String localFilePath,
                                String deviceFilePath ) {

            mobileDeviceUtils.copyFileTo(localFilePath, deviceFilePath);
        }

        /**
         * Copies file from device to local machine
         *
         * @param deviceFilePath file path on the device
         * @param localFilePath local file path
         */
        @PublicAtsApi
        public void copyFileFrom(
                                  String deviceFilePath,
                                  String localFilePath ) {

            mobileDeviceUtils.copyFileFrom(deviceFilePath, localFilePath);
        }

        /**
         * Get clipboard contents with text assumed
         * @return text contents
         */
        @PublicAtsApi
        public String getClipboardText() {

            if (appiumDriver instanceof IOSDriver<?>) {
                return ((IOSDriver<? extends WebElement>) appiumDriver).getClipboardText();
            } else if (appiumDriver instanceof AndroidDriver<?>) {
                return ((AndroidDriver<? extends WebElement>) appiumDriver).getClipboardText();
            } else {
                throw new IllegalStateException("Mobile driver instance not detected as Android or iOS one. "
                                                + "Report an issue. Instance: " + appiumDriver);
            }
        }
    }
}
