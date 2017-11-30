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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.MobileDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiCheckBox;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.MobileOperationException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.mobile.MobileElementState;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;

/**
 * A mobile Check Box element - native or HTML one
 * <p>
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
public class MobileCheckBox extends UiCheckBox {

    private static final String[] RULES = { "id", "name", "text", "partialText", "xpath" };

    private AppiumDriver<?>       appiumDriver;

    public MobileCheckBox( UiDriver uiDriver,
                           UiElementProperties properties ) {

        super(uiDriver, properties);
        properties.checkTypeAndRules(this.getClass().getSimpleName(), "Mobile", RULES);

        appiumDriver = (AppiumDriver<?>) ((MobileDriver) super.getUiDriver()).getInternalObject(InternalObjectsEnum.WebDriver.name());
    }

    /**
     * Check the check box
     */
    @Override
    @PublicAtsApi
    public void check() {

        new MobileElementState(this).waitToBecomeExisting();

        try {
            WebElement checkboxElement = MobileElementFinder.findElement(appiumDriver, this);
            if (!checkboxElement.isSelected()) {
                if (appiumDriver instanceof AndroidDriver) {

                    // checkboxElement.click(); // throwing exception (on Android) with message: Element is not clickable at point (x,y). Other element would receive the click
                    new Actions(appiumDriver).moveToElement(checkboxElement).click().perform();
                } else {

                    checkboxElement.click();
                }
            }
        } catch (Exception se) {
            throw new MobileOperationException(this, "check", se);
        }

        UiEngineUtilities.sleep();
    }

    /**
     * Uncheck the check box
     */
    @Override
    @PublicAtsApi
    public void unCheck() {

        new MobileElementState(this).waitToBecomeExisting();

        try {
            WebElement checkboxElement = MobileElementFinder.findElement(appiumDriver, this);
            if (checkboxElement.isSelected()) {
                if (appiumDriver instanceof AndroidDriver) {

                    // checkboxElement.click(); // throwing exception (on Android) with message: Element is not clickable at point (x,y). Other element would receive the click
                    new Actions(appiumDriver).moveToElement(checkboxElement).click().perform();
                } else {

                    checkboxElement.click();
                }
            }
        } catch (Exception se) {
            throw new MobileOperationException(this, "unCheck", se);
        }

        UiEngineUtilities.sleep();
    }

    /**
     * Tells whether the check box is checked
     */
    @Override
    @PublicAtsApi
    public boolean isChecked() {

        new MobileElementState(this).waitToBecomeExisting();

        try {
            return MobileElementFinder.findElement(appiumDriver, this).isSelected();
        } catch (Exception se) {
            throw new MobileOperationException(this, "isChecked", se);
        }
    }

    /**
     * Verify the check box is checked
     *
     * throws an error if verification fail
     */
    @Override
    @PublicAtsApi
    public void verifyChecked() {

        boolean isActuallyChecked = isChecked();
        if (!isActuallyChecked) {
            throw new VerificationException("It was expected to have " + this.toString()
                                            + " checked, but it is unchecked indeed");
        }
    }

    /**
     * Verify the check box is not checked
     *
     * throws an error if verification fail
     */
    @Override
    @PublicAtsApi
    public void verifyNotChecked() {

        boolean isActuallyChecked = isChecked();
        if (isActuallyChecked) {
            throw new VerificationException("It was expected to have " + this.toString()
                                            + " unchecked, but it is checked indeed");
        }
    }
}
