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
package com.axway.ats.uiengine.utilities.hiddenbrowser;

import org.openqa.selenium.Alert;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitWebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.HiddenBrowserDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiAlert;
import com.axway.ats.uiengine.elements.UiConfirm;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiPrompt;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlElementLocator;
import com.axway.ats.uiengine.exceptions.ElementNotFoundException;
import com.axway.ats.uiengine.exceptions.NotSupportedOperationException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.IHtmlElementState;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;

/**
 * Check the state of an HTML element
 */
@PublicAtsApi
public class HiddenHtmlElementState implements IHtmlElementState {

    private UiElement           element;

    private UiElementProperties elementProperties;

    private WebDriver           webDriver;

    private static final String NOT_SUPPORTED_ACTION_MSG = "This action is not available for Hidden Browser";


    /**
     * @param uiElement the element of interest
     */
    public HiddenHtmlElementState( UiElement uiElement ) {

        this.element = uiElement;
        this.elementProperties = uiElement.getElementProperties();

        HiddenBrowserDriver browserDriver = ( HiddenBrowserDriver ) uiElement.getUiDriver();
        webDriver = ( WebDriver ) browserDriver.getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * Verifies the element exist
     *
     * throws an exception if verification fail
     */
    @PublicAtsApi
    public void verifyExist() {

        boolean exists = isElementPresent();
        if( !exists ) {
            throw new VerificationException( getElementDescription()
                                             + " does not exist while it is expected to exist" );
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
        if( exists ) {
            throw new VerificationException( getElementDescription()
                                             + " exists while it is expected to not exist" );
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

        waitToBecomeExisting( UiEngineConfigurator.getInstance().getElementStateChangeDelay() );
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
            if( isElementPresent() ) {
                return;
            }
            UiEngineUtilities.sleep();
        } while( endTime - System.currentTimeMillis() > 0 );

        throw new VerificationException( "Failed to verify the element exist within " + millis + " ms"
                                         + getElementDescription()+".Please check your HTML page source.You may use HiddenHtmlEngine.getPageSource()." );
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to became non-existing
     *
     * throws an exception if it does not become non-existing
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeNotExisting() {

        waitToBecomeNotExisting( UiEngineConfigurator.getInstance().getElementStateChangeDelay() );
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
            if( !isElementPresent() ) {
                return;
            }
            UiEngineUtilities.sleep();
        } while( endTime - System.currentTimeMillis() > 0 );

        throw new VerificationException( "Failed to verify the element is not existing within " + millis
                                         + " ms" + getElementDescription() );
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become displayed
     *
     * throws an exception if it does not become displayed
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeDisplayed() {

        waitToBecomeDisplayed( UiEngineConfigurator.getInstance().getElementStateChangeDelay() );
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
            if( isElementDisplayed() ) {
                return;
            }

            UiEngineUtilities.sleep();
        } while( endTime - System.currentTimeMillis() > 0 );

        throw new VerificationException( "Failed to verify the element become displayed within " + millis
                                         + " ms" + getElementDescription() );
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become hidden
     *
     * throws an exception if it does not become hidden
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeHidden() {

        waitToBecomeHidden( UiEngineConfigurator.getInstance().getElementStateChangeDelay() );
    }

    /**
     * Waits for a period of time the element to become hidden
     *
     * throws an exception if it does not become hidden for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeHidden(
                                    int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if( !isElementDisplayed() ) {
                return;
            }

            UiEngineUtilities.sleep();
        } while( endTime - System.currentTimeMillis() > 0 );

        throw new VerificationException( "Failed to verify the element become hidden within " + millis
                                         + " ms" + getElementDescription() );
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become enabled
     *
     * throws an exception if it does not become enabled
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeEnabled() {

        waitToBecomeEnabled( UiEngineConfigurator.getInstance().getElementStateChangeDelay() );
    }

    /**
     * Waits for a period of time the element to become enabled
     *
     * throws an exception if it does not become enabled for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeEnabled(
                                     int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if( isElementEnabled() ) {
                return;
            }

            UiEngineUtilities.sleep();
        } while( endTime - System.currentTimeMillis() > 0 );

        throw new VerificationException( "Failed to verify the element become enabled within " + millis
                                         + " ms" + getElementDescription() );
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become disabled
     *
     * throws an exception if it does not become disabled
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeDisabled() {

        waitToBecomeDisabled( UiEngineConfigurator.getInstance().getElementStateChangeDelay() );
    }

    /**
     * Waits for a period of time the element to become disabled
     *
     * throws an exception if it does not become disabled for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeDisabled(
                                      int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if( !isElementEnabled() ) {
                return;
            }

            UiEngineUtilities.sleep();
        } while( endTime - System.currentTimeMillis() > 0 );

        throw new VerificationException( "Failed to verify the element become disabled within " + millis
                                         + " ms" + getElementDescription() );
    }

    private String getElementDescription() {

        return " '" + ( element != null
                                       ? element.toString()
                                       : "Element " + elementProperties.toString() ) + "'";
    }

    @Override
    public boolean isElementPresent() {

        try {

            if( ( element.getUiDriver() instanceof HiddenBrowserDriver )
                && ( element instanceof UiAlert || element instanceof UiPrompt || element instanceof UiConfirm ) ) {
                return false;
            }

            if( element instanceof UiAlert || element instanceof UiConfirm ) {
                Alert dialog = webDriver.switchTo().alert();
                return dialog != null;
            } else if( element instanceof UiPrompt ) {

                Alert prompt = webDriver.switchTo().alert();
                return prompt.getText() != null && !prompt.getText().equals( "false" );
                // return seleniumBrowser.isPromptPresent(); // Not implemented yet
            }

            HiddenHtmlElementLocator.findElement( this.element, null, false );
            return true;
        } catch( NotFoundException nfe ) {
            return false;
        } catch( ElementNotFoundException enfe ) {
            return false;
        }

    }

    @Override
    @PublicAtsApi
    public boolean isElementEnabled() {

        try {
            HtmlUnitWebElement element = HiddenHtmlElementLocator.findElement( this.element );
            return element.isEnabled();
        } catch( ElementNotFoundException nsee ) {
            return false;
        }
    }

    /**
     * Check whether the element is displayed or not
     *
     * @return <code>true</code> if the element is displayed
     */
    @Override
    @PublicAtsApi
    public boolean isElementDisplayed() {

        try {
            HtmlUnitWebElement element = HiddenHtmlElementLocator.findElement( this.element );
            return element.isDisplayed();
        } catch( ElementNotFoundException nsee ) {
            return false;
        }
    }

    @Override
    @PublicAtsApi
    public void focus() {

        throw new NotSupportedOperationException( NOT_SUPPORTED_ACTION_MSG );
    }

    @Override
    @PublicAtsApi
    public void highlightElement() {

        // useless for not visible browsers
    }

}
