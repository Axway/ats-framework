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

import org.openqa.selenium.WebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlTextArea;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * An HTML Text Area. It acts like a multiline Text Box.
 * <br><b>Note:</b> all values provided by user or coming from the browser are normalized by
 * replacing all "\r\n" with "\n" and trimming leading and trailing white spaces.
 * @see RealHtmlElement
 */
@PublicAtsApi
public class RealHtmlTextArea extends HtmlTextArea {

    //private WebDriver webDriver;

    public RealHtmlTextArea( UiDriver uiDriver,
                             UiElementProperties properties ) {

        super( uiDriver, properties );
        String[] matchingRules = properties.checkTypeAndRules( this.getClass().getSimpleName(),
                                                               "RealHtml",
                                                               RealHtmlElement.RULES_DUMMY );

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator( matchingRules,
                                                                    properties,
                                                                    new String[]{},
                                                                    "textarea" );
        properties.addInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath );

        //webDriver = ( WebDriver ) ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * Get the Text Area value
     * @return
     */
    @Override
    @PublicAtsApi
    public String getValue() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        return normalizeText( element.getAttribute( "value" ) );
    }

    /**
     * Set the Text Area value
     *
     * @param value
     */
    @Override
    @PublicAtsApi
    public void setValue(
                          String value ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        element.clear();
        element.sendKeys( normalizeText( value ) );

        UiEngineUtilities.sleep();
    }

    /**
     * Append text to the current content of a Text Area
     * 
     * @param value
     */
    @Override
    @PublicAtsApi
    public void appendValue(
                             String value ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        element.sendKeys( normalizeText( value ) );

        UiEngineUtilities.sleep();
    }

    /**
     * Verify the Text Area value is as specified
     *
     * @param expectedValue
     */
    @Override
    @PublicAtsApi
    public void verifyValue(
                             String expectedValue ) {

        expectedValue = normalizeText( expectedValue );

        String actualText = getValue();
        if( !actualText.equals( expectedValue ) ) {
            throw new VerifyEqualityException( expectedValue, actualText, this );
        }
    }

    /**
     * Verify the Text Area value is NOT as specified
     *
     * @param notExpectedValue
     */
    @Override
    @PublicAtsApi
    public void verifyNotValue(
                                String notExpectedValue ) {

        notExpectedValue = normalizeText( notExpectedValue );

        String actualText = getValue();
        if( actualText.equals( notExpectedValue ) ) {
            throw new VerifyNotEqualityException( notExpectedValue, this );
        }
    }

    private String normalizeText(
                                  String src ) {

        return src.replace( "\r\n", "\n" ).trim();
    }
}
