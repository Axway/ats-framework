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
package com.axway.ats.uiengine.utilities.mobile;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.touch.TouchActions;
import org.openqa.selenium.remote.UnreachableBrowserException;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.MobileDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.mobile.MobileElementFinder;
import com.axway.ats.uiengine.exceptions.MobileOperationException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;

public class MobileElementState {

    private static final int SLEEP_PERIOD = 500; // in ms

    private AppiumDriver<? extends WebElement> appiumDriver;

    private UiElementProperties elementProperties;

    private UiElement element;

    /**
     * @param uiElement the element of interest
     */
    public MobileElementState( UiElement uiElement ) {

        this.element = uiElement;
        this.elementProperties = uiElement.getElementProperties();
        this.appiumDriver = (AppiumDriver<? extends WebElement>) ((MobileDriver) uiElement.getUiDriver()).getInternalObject(
                InternalObjectsEnum.WebDriver.name());
    }

    /**
     * Moves the focus to the specified element. Currently issued with tap
     */
    @PublicAtsApi
    public void focus() {

        try {
            MobileElement mobileElement = (MobileElement) MobileElementFinder.findElement(appiumDriver,
                                                                                          element);
            // use tap to focus
            new TouchActions(appiumDriver).singleTap(mobileElement).perform();
        } catch (Exception se) {
            throw new MobileOperationException("Error trying to set the focus to " + getElementDescription(),
                                               se);
        }
    }

    /**
     * Verifies the element exist
     *
     * throws an exception if verification fail
     */
    @PublicAtsApi
    public void verifyExist() {

        boolean exists = isElementPresent();
        if (!exists) {
            throw new VerificationException(getElementDescription()
                                            + " does not exist while it is expected to exist");
        }
    }

    /**
     * Verifies the element does NOT exist
     *
     * throws an exception if verification fail
     */
    @PublicAtsApi
    public void verifyNotExist() {

        boolean exists = isElementPresent();
        if (exists) {
            throw new VerificationException(getElementDescription()
                                            + " exists while it is expected to not exist");
        }
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become existing
     *
     * throws an exception if it does not become existing
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeExisting() {

        waitToBecomeExisting(UiEngineConfigurator.getInstance().getElementStateChangeDelay());
    }

    /**
     * Waits for a period of time the element to become existing
     *
     * throws an exception if it does not become existing for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeExisting(
            int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if (isElementPresent()) {
                return;
            }

            UiEngineUtilities.sleep(SLEEP_PERIOD);
        } while (endTime - System.currentTimeMillis() > 0);

        throw new VerificationException("Failed to verify the element exist within " + millis + " ms"
                                        + getElementDescription());
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to became non-existing
     *
     * throws an exception if it does not become non-existing
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeNotExisting() {

        waitToBecomeNotExisting(UiEngineConfigurator.getInstance().getElementStateChangeDelay());
    }

    /**
     * Waits for a period of time the element to became non-existing
     *
     * throws an exception if it does not become non-existing for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeNotExisting(
            int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if (!isElementPresent()) {
                return;
            }

            UiEngineUtilities.sleep(SLEEP_PERIOD);
        } while (endTime - System.currentTimeMillis() > 0);

        throw new VerificationException("Failed to verify the element is not existing within " + millis
                                        + " ms" + getElementDescription());
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become displayed
     *
     * throws an exception if it does not become displayed
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeDisplayed() {

        waitToBecomeDisplayed(UiEngineConfigurator.getInstance().getElementStateChangeDelay());
    }

    /**
     * Waits for a period of time the element to become displayed
     *
     * throws an exception if it does not become displayed for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeDisplayed(
            int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if (isElementDisplayed()) {
                return;
            }

            UiEngineUtilities.sleep(SLEEP_PERIOD);
        } while (endTime - System.currentTimeMillis() > 0);

        throw new VerificationException("Failed to verify the element become displayed within " + millis
                                        + " ms" + getElementDescription());
    }

    @PublicAtsApi
    public boolean isElementPresent() {

        try {
            return MobileElementFinder.findElement(appiumDriver, element) != null;
        } catch (UnreachableBrowserException ube) {
            throw new MobileOperationException(
                    "Check if there is connection to the target device and the Appium server is running",
                    ube);
        } catch (Exception e) {

            // element is not present or got error checking if it is present
            return false;
        }
    }

    @PublicAtsApi
    public boolean isElementDisplayed() {

        try {
            WebElement webElement = MobileElementFinder.findElement(appiumDriver, element);
            if (webElement == null) {
                return false;
            } else {
                return webElement.isDisplayed();
            }
        } catch (UnreachableBrowserException ube) {
            throw new MobileOperationException(
                    "Check if there is connection to the target device and the Appium server is running",
                    ube);
        } catch (Exception e) {
            // element is not present or got error checking if it is present
            return false;
        }
    }

    private String getElementDescription() {

        StringBuilder desc = new StringBuilder();
        desc.append(" '");
        if (element != null) {
            desc.append(element.toString());
        } else {
            desc.append("Element ").append(elementProperties.toString());
        }

        // append 'context' if not specified thru the element properties
        if (elementProperties.getProperty("context") == null) {
            desc.append(", context=");
            desc.append(MobileElementFinder.defaultContext);
        }
        desc.append("'");
        return desc.toString();
    }

}
