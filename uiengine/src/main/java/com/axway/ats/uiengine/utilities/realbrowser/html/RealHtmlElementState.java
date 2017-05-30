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
package com.axway.ats.uiengine.utilities.realbrowser.html;

import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.AbstractRealBrowserDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiAlert;
import com.axway.ats.uiengine.elements.UiConfirm;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiPrompt;
import com.axway.ats.uiengine.elements.html.HtmlNavigator;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlElementLocator;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.IHtmlElementState;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;

/**
 * Check the state of an HTML element
 */
@PublicAtsApi
public class RealHtmlElementState implements IHtmlElementState {

    private WebDriver           webDriver;

    private UiElementProperties elementProperties;

    private UiElement           element;

    /**
     * @param uiElement the element of interest
     */
    public RealHtmlElementState( UiElement uiElement ) {

        this.element = uiElement;
        this.elementProperties = uiElement.getElementProperties();
        this.webDriver = ( WebDriver ) ( ( AbstractRealBrowserDriver ) uiElement.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * Moves the focus to the specified element.
     * <b>Note:</b> This is somewhat breakable as
     * the browser window needs to be the active system window, otherwise the keyboard
     * events will go to another application.
     */
    @PublicAtsApi
    public void focus() {
            
        // retrieve browser focus
        webDriver.switchTo().window( webDriver.getWindowHandle() );
        // now focus the target element
        WebElement webElement = RealHtmlElementLocator.findElement( element );
        if( "input".equals( webElement.getTagName() ) ) {
            webElement.sendKeys( "" );
        } else {
            new Actions( webDriver ).moveToElement( webElement ).perform();
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
                highlightElement( false );
                return;
            }

            UiEngineUtilities.sleep();
        } while( endTime - System.currentTimeMillis() > 0 );

        throw new VerificationException( "Failed to verify the element exist within "
                                         + millis
                                         + " ms"
                                         + getElementDescription()
                                         + ".Please check your HTML page source.You may use RealHtmlEngine.getPageSource()." );
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

        highlightElement( false );

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
                highlightElement( false );
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
                highlightElement( false );
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
                highlightElement( false );
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
                highlightElement( false );
                return;
            }

            UiEngineUtilities.sleep();
        } while( endTime - System.currentTimeMillis() > 0 );

        throw new VerificationException( "Failed to verify the element become disabled within " + millis
                                         + " ms" + getElementDescription() );
    }

    /**
     * Tries to briefly change the element's background color to yellow. Does not work on all elements.
     * It is useful for debugging purposes.
     */
    @PublicAtsApi
    public void highlightElement() {

        highlightElement( true );
    }

    private void highlightElement(
                                   boolean disregardConfiguration ) {

        if( webDriver instanceof PhantomJSDriver ) {
            // it is headless browser
            return;
        }

        if( disregardConfiguration || UiEngineConfigurator.getInstance().getHighlightElements() ) {

            try {
                WebElement webElement = RealHtmlElementLocator.findElement( element );
                String styleAttrValue = webElement.getAttribute( "style" );

                JavascriptExecutor js = ( JavascriptExecutor ) webDriver;
                js.executeScript( "arguments[0].setAttribute('style', arguments[1]);",
                                  webElement,
                                  "background-color: #ff9; border: 1px solid yellow; box-shadow: 0px 0px 10px #fa0;" ); // to change text use: "color: yellow; text-shadow: 0 0 2px #f00;"
                Thread.sleep( 500 );
                js.executeScript( "arguments[0].setAttribute('style', arguments[1]);",
                                  webElement,
                                  styleAttrValue );
            } catch( Exception e ) {
                // swallow this error as highlighting is not critical
            }
        }
    }

    private String getElementDescription() {

        return " '" + ( element != null
                                       ? element.toString()
                                       : "Element " + elementProperties.toString() ) + "'";
    }

    @PublicAtsApi
    public boolean isElementPresent() {

        // with the current Selenium implementation we do not know whether the opened modal dialog
        // is alert, prompt or confirmation
        if( element instanceof UiAlert ) {

            return getAlert() != null;
        } else if( element instanceof UiPrompt ) {

            Alert prompt = getAlert();
            return prompt != null && prompt.getText() != null;
        } else if( element instanceof UiConfirm ) {

            Alert confirm = getAlert();
            return confirm != null && confirm.getText() != null;
        }

        HtmlNavigator.getInstance().navigateToFrame( webDriver, element );

        return RealHtmlElementLocator.findElements( element ).size() > 0;
    }

    /**
     *
     * @return {@link Alert} object representing HTML alert, prompt or confirmation modal dialog
     */
    private Alert getAlert() {

        try {
            return this.webDriver.switchTo().alert();
        } catch( NoAlertPresentException e ) {
            return null;
        }
    }

    /**
     *
     * @return <code>true</code> if the element is enabled
     */
    @Override
    @PublicAtsApi
    public boolean isElementEnabled() {

        return RealHtmlElementLocator.findElement( element ).isEnabled();
    }

    /**
     * Check whether the element is displayed or not
     *
     * @return <code>true</code> if the element is displayed
     */
    @Override
    @PublicAtsApi
    public boolean isElementDisplayed() {

        return RealHtmlElementLocator.findElement( element ).isDisplayed();
    }
}
