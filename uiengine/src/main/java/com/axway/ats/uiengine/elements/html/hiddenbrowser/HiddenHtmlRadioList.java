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
package com.axway.ats.uiengine.elements.html.hiddenbrowser;

import org.openqa.selenium.htmlunit.HtmlUnitWebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlRadioList;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;

/**
 * A list of HTML Radio Buttons with same name. Only one radio button can be selected at a time.
 * @see HiddenHtmlElement
 */
@PublicAtsApi
public class HiddenHtmlRadioList extends HtmlRadioList {

    public HiddenHtmlRadioList( UiDriver uiDriver,
                                UiElementProperties properties ) {

        super( uiDriver, properties );
        String matchingRules[] = properties.checkTypeAndRules( this.getClass().getSimpleName(),
                                                               "HiddenHtml",
                                                               HiddenHtmlElement.RULES_DUMMY );

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator( matchingRules,
                                                                    properties,
                                                                    new String[]{ "radio" },
                                                                    "input" );
        properties.addInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath );
    }

    /**
     * Select radio button
     *
     * @param value of the button to select
     */
    @PublicAtsApi
    public void select(
                        String value ) {

        HtmlUnitWebElement element = HiddenHtmlElementLocator.findElement( this,
                                                                           "[@value='" + value + "']",
                                                                           true );
        if( !element.isEnabled() ) {

            throw new UnsupportedOperationException( "You may not select a disabled element." + toString() );
        }
        element.click();
        
        UiEngineUtilities.sleep();
    }

    /**
     *
     * @return is the radio button selected
     */
    @PublicAtsApi
    public boolean isSelected(
                               String value ) {

        return HiddenHtmlElementLocator.findElement( this, "[@value='" + value + "']", true ).isSelected();
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
