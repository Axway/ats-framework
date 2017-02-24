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

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.MobileDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.MobileOperationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.mobile.MobileElementState;

import io.appium.java_client.AppiumDriver;

/**
 * A single line mobile Text Box element - native or HTML one
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
public class MobileTextBox extends MobileElement<MobileTextBox> {

    private static final String[] RULES = { "id", "name", "text", "partialText", "xpath" };

    private AppiumDriver<?>       appiumDriver;

    public MobileTextBox( UiDriver uiDriver,
                          UiElementProperties properties ) {

        super( uiDriver, properties );
        properties.checkTypeAndRules( this.getClass().getSimpleName(), "Mobile", RULES );

        appiumDriver = ( AppiumDriver<?> ) ( ( MobileDriver ) super.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * Get the Text Box value
     * @return
     */
    @PublicAtsApi
    public String getValue() {

        new MobileElementState( this ).waitToBecomeExisting();

        try {
            return MobileElementFinder.findElement( appiumDriver, this ).getAttribute( "value" );
        } catch( Exception se ) {
            throw new MobileOperationException( this, "getValue", se );
        }
    }

    /**
     * Set the Text Box value
     *
     * @param value
     */
    @PublicAtsApi
    public MobileTextBox setValue(
                                   String value ) {

        new MobileElementState( this ).waitToBecomeExisting();

        try {
            WebElement textElement = MobileElementFinder.findElement( appiumDriver, this );
            textElement.clear();
            textElement.sendKeys( value );
        } catch( Exception se ) {
            throw new MobileOperationException( this, "setValue", se );
        }

        UiEngineUtilities.sleep(); // think time
        return this;
    }

    @PublicAtsApi
    public void pressEnterKey() {

        new MobileElementState( this ).waitToBecomeExisting();

        try {
            WebElement textElement = MobileElementFinder.findElement( appiumDriver, this );
            textElement.sendKeys( "\n" );
        } catch( Exception se ) {
            throw new MobileOperationException( this, "pressEnterKey", se );
        }

        UiEngineUtilities.sleep(); // think time
    }

    /**
     * Verify the Text Box value is as specified
     *
     * @param expectedValue
     */
    @PublicAtsApi
    public void verifyValue(
                             String expectedValue ) {

        expectedValue = expectedValue.trim();

        String actualText = getValue().trim();
        if( !actualText.equals( expectedValue ) ) {
            throw new VerifyEqualityException( expectedValue, actualText, this );
        }
    }

    /**
     * Verify the Text Box value is NOT as specified
     *
     * @param notExpectedValue
     */
    @PublicAtsApi
    public void verifyNotValue(
                                String notExpectedValue ) {

        String actualText = getValue();
        if( actualText.equals( notExpectedValue ) ) {
            throw new VerifyNotEqualityException( notExpectedValue, this );
        }
    }

}
