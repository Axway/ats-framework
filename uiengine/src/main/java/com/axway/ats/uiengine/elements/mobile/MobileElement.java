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
package com.axway.ats.uiengine.elements.mobile;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.MobileDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.MobileOperationException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.mobile.MobileElementState;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * An Mobile element.
 * Can be identified by:
 * <ul>
 *    <li>id</li>
 *    <li>name</li>
 *    <li>text - the exact text that element displays</li>
 *    <li>partialText - some part of the text that element displays</li>
 *    <li>xpath</li>
 * </ul>
 * </p>
 */
@PublicAtsApi
public class MobileElement<T> extends UiElement {

    private static final String[] RULES = { "id", "name", "text", "partialText", "xpath" };

    private AppiumDriver<?>       appiumDriver;

    public MobileElement( UiDriver uiDriver,
                          UiElementProperties properties ) {

        super(uiDriver, properties);
        properties.checkTypeAndRules(this.getClass().getSimpleName(), "Mobile", RULES);

        appiumDriver = (AppiumDriver<?>) ((MobileDriver) super.getUiDriver()).getInternalObject(InternalObjectsEnum.WebDriver.name());
    }

    /**
     * Get element attribute value
     * @return value of the attribute (style/class/...).
     * <em>Note:</em> Currently for WebView elements seems to be returning something only if such HTML attribute is explicitly set.
     */
    @PublicAtsApi
    public String getAttributeValue(
                                     String attribute ) {

        new MobileElementState(this).waitToBecomeExisting();

        try {
            WebElement webElement = MobileElementFinder.findElement(appiumDriver, this);
            if (webElement == null) {
                return null;
            } else {
                return MobileElementFinder.findElement(appiumDriver, this).getAttribute(attribute);
            }
        } catch (Exception e) {
            throw new MobileOperationException(this, "getAttributeValue", e);
        }
    }

    /**
     * Get innerText of the element
     * @return innerText of the element
     */
    @PublicAtsApi
    public String getTextContent() {

        new MobileElementState(this).waitToBecomeExisting();

        try {
            return MobileElementFinder.findElement(appiumDriver, this).getText();
        } catch (Exception e) {
            throw new MobileOperationException(this, "getTextContent", e);
        }
    }

    /**
     * Simulate tap/click action
     * @return this mobile element so allows chained actions like element.click().getTextContent()
     */
    @SuppressWarnings( "unchecked")
    @PublicAtsApi
    public T click() {

        long endTime = System.currentTimeMillis()
                       + UiEngineConfigurator.getInstance().getElementStateChangeDelay();

        new MobileElementState(this).waitToBecomeExisting();

        try {
            // the element exists but may be still not clickable (in some cases waitToBecomeDisplayed() is not working and returns true, but it is not visible)
            while (true) {
                try {
                    MobileElementFinder.findElement(appiumDriver, this).click();
                    return (T) this;
                } catch (Exception e) {
                    if (endTime - System.currentTimeMillis() < 0) {
                        throw e;
                    }
                }
                UiEngineUtilities.sleep(500);
            }
        } catch (Exception e) {
            throw new MobileOperationException(this, "click", e);
        }
    }

    //    public T click() {
    //
    //        new MobileElementState( this ).waitToBecomeExisting();
    //
    //        try {
    //            try {
    //                MobileElementFinder.findElement( appiumDriver, this ).click();
    //            } catch( ElementNotVisibleException enve ) { // element is not currently visible and may not be manipulated
    //
    //                new MobileElementState( this ).waitToBecomeDisplayed();
    //                MobileElementFinder.findElement( appiumDriver, this ).click();
    //            }
    //            return ( T ) this;
    //        } catch( Exception e ) {
    //            throw new MobileOperationException( this, "click", e );
    //        }
    //    }

    /**
     * Click/tap the element if exists.
     *
     * @param waitingTimeout timeout in milliseconds to wait for the element to appear
     */
    @PublicAtsApi
    public boolean clickIfExists(
                                  int waitingTimeout ) {

        int currentStateChangeDelay = UiEngineConfigurator.getInstance().getElementStateChangeDelay();
        try {
            UiEngineConfigurator.getInstance().setElementStateChangeDelay(waitingTimeout);
            appiumDriver.manage().timeouts().implicitlyWait(waitingTimeout, TimeUnit.MILLISECONDS);

            long endTime = System.currentTimeMillis() + waitingTimeout;
            new MobileElementState(this).waitToBecomeExisting();

            // the element exists but may be still not clickable
            do {
                try {
                    MobileElementFinder.findElement(appiumDriver, this).click();
                    return true;
                } catch (Exception e) {}
                UiEngineUtilities.sleep(500);
            } while (endTime - System.currentTimeMillis() > 0);

        } catch (VerificationException ve) {
            // do nothing, the element doesn't exist
        } finally {
            UiEngineConfigurator.getInstance().setElementStateChangeDelay(currentStateChangeDelay);
            appiumDriver.manage().timeouts().implicitlyWait(currentStateChangeDelay, TimeUnit.MILLISECONDS);
        }
        return false;
    }

    /**
     * Scroll to element (at the center of the screen)
     *
     * @return this mobile element which allows chained actions
     */
    @SuppressWarnings( "unchecked")
    @PublicAtsApi
    public T scrollTo() {

        try {

            if (MobileElementFinder.getElementContext(this).toUpperCase().startsWith("WEBVIEW")) {

                // in WEBVIEWs the target element exists, while in the NATIVE context it doesn't until we scroll to it
                new MobileElementState(this).waitToBecomeExisting();

                Dimension screenDimensions = ((MobileDriver) getUiDriver()).getScreenDimensions();
                WebElement element = MobileElementFinder.findElement(appiumDriver, this);

                // window.scrollTo(0, element.getLocation().y);    -->  will scroll the element to top-left

                int scrollToY = 0;
                int screenCenter = screenDimensions.getHeight() / 2 + element.getSize().height / 2;
                if (element.getLocation().y < screenCenter) {
                    // the element is located after the screen center if we scroll to (0, element.getLocation().y)
                    // because it is near the bottom of the application => we can't center it, but it is OK on that position
                    scrollToY = element.getLocation().y;
                } else {
                    scrollToY = element.getLocation().y - screenCenter;
                }

                ((JavascriptExecutor) appiumDriver).executeScript("window.scrollTo(0," + scrollToY + ")");
            } else {

                WebElement element = MobileElementFinder.findElement(appiumDriver, this);
                if (getElementProperty("name") != null) {
                    // only works for NATIVE context
                    if (appiumDriver instanceof AndroidDriver) {
                        throw new Exception("scrollTo() not supported yet for Android elements");
                    } else {
                        // iOS case
                        // https://discuss.appium.io/t/scroll-to-swipe-action-in-ios-8/4220/29 May not work to scroll to specific cell in table view
                        element.findElement(MobileBy.IosUIAutomation(".scrollToElementWithPredicate(\"name CONTAINS '" +
                                                                     getElementProperty("name") + "'\")"));
                    }
                } else {
                    throw new Exception("scrollTo() not supported yet for element without name property");
                }
                // TODO Check alternative proposed: https://discuss.appium.io/t/click-non-visible-element-by-scroll-swipe/17227/3
            }

            return (T) this;
        } catch (Exception e) {
            throw new MobileOperationException(this, "scrollTo", e);
        }
    }
}
