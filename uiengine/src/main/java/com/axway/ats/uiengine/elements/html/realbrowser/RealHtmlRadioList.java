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
import com.axway.ats.uiengine.elements.html.HtmlRadioList;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * A list of HTML Radio Buttons with same name. Only one radio button can be selected at a time.
 * @see RealHtmlElement
 */
@PublicAtsApi
public class RealHtmlRadioList extends HtmlRadioList {

    //private WebDriver webDriver;

    public RealHtmlRadioList( UiDriver uiDriver,
                              UiElementProperties properties ) {

        super( uiDriver, properties );
        String[] matchingRules = properties.checkTypeAndRules( this.getClass().getSimpleName(),
                                                               "RealHtml",
                                                               RealHtmlElement.RULES_DUMMY );

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator( matchingRules,
                                                                    properties,
                                                                    new String[]{ "radio" },
                                                                    "input" );
        properties.addInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath );

        //webDriver = ( WebDriver ) ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * set the selected value
     *
     * @param value the value to select(this is the 'value' attribute of the radio button)
     */
    @PublicAtsApi
    public void select(
                        String value ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this, "[@value='" + value + "']", true );
        if( !element.isEnabled() ) {
            throw new UnsupportedOperationException( "You may not select a disabled element." + toString() );
        }
        element.click();

        UiEngineUtilities.sleep();
    }

    /**
     * @return if this is the selected value(we pass the 'value' attribute of the radio button)
     */
    @PublicAtsApi
    public boolean isSelected(
                               String value ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        return RealHtmlElementLocator.findElement( this, "[@value='" + value + "']", true ).isSelected();
    }

    /**
     * Verify the selected value is as specified
     *
     * @param expectedValue
     */
    @PublicAtsApi
    public void verifyValue(
                             String expectedValue ) {

        if( !isSelected( expectedValue ) ) {
            throw new VerifyEqualityException( expectedValue, this );
        }
    }

    /**
     * Verify the selected value is NOT as specified
     *
     * @param notExpectedValue
     */
    @PublicAtsApi
    public void verifyNotValue(
                                String notExpectedValue ) {

        if( isSelected( notExpectedValue ) ) {
            throw new VerifyNotEqualityException( notExpectedValue, this );
        }
    }

}
