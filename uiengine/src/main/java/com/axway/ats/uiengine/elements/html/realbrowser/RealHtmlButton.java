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
package com.axway.ats.uiengine.elements.html.realbrowser;

import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.AbstractRealBrowserDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlButton;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * An HTML Button
 * @see RealHtmlElement
 */
@PublicAtsApi
public class RealHtmlButton extends HtmlButton {

    private WebDriver webDriver;

    public RealHtmlButton( UiDriver uiDriver,
                           UiElementProperties properties ) {

        super( uiDriver, properties );

        // get rules used for finding html element
        String[] matchingRules = properties.checkTypeAndRules( this.getClass().getSimpleName(),
                                                               "RealHtml",
                                                               RealHtmlElement.RULES_DUMMY );

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator( matchingRules,
                                                                    properties,
                                                                    new String[]{ "submit",
                                                                                  "reset",
                                                                                  "button" },
                                                                    "button" );
        properties.addInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath );

        webDriver = ( WebDriver ) ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * Click the button
     */
    @Override
    @PublicAtsApi
    public void click() {

        doClick();
    }

    /**
     * Click button and download file
     */
    @PublicAtsApi
    public void clickAndDownloadFile() {

        log.info( "File will be downloaded in "
                  + UiEngineConfigurator.getInstance().getBrowserDownloadDir() );

        // Just calls click() method and the browser automatically will download the file
        doClick();

        UiEngineUtilities.sleep();
        log.info( "File download has started. Please check for completion." );
    }

    private void doClick() {

        try {
            new RealHtmlElementState( this ).waitToBecomeExisting();
            WebElement element = RealHtmlElementLocator.findElement( this );
            try {
                element.click();
            } catch( ElementNotVisibleException enve ) {
                if( !UiEngineConfigurator.getInstance().isWorkWithInvisibleElements() ) {
                    throw enve;
                }
                ( ( JavascriptExecutor ) webDriver ).executeScript( "arguments[0].click()", element );
            }
        } catch( Exception e ) {
            ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).clearExpectedPopups();
            throw new SeleniumOperationException( this, "click", e );
        }

        UiEngineUtilities.sleep();

        ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).handleExpectedPopups();
    }
}
