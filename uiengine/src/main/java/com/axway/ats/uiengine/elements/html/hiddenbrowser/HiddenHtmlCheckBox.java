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
import com.axway.ats.uiengine.elements.html.HtmlCheckBox;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.hiddenbrowser.HiddenHtmlElementState;

/**
 * An HTML Check Box
 * @see HiddenHtmlElement
 */
@PublicAtsApi
public class HiddenHtmlCheckBox extends HtmlCheckBox {

    public HiddenHtmlCheckBox( UiDriver uiDriver,
                               UiElementProperties properties ) {

        super( uiDriver, properties );
        String matchingRules[] = properties.checkTypeAndRules( this.getClass().getSimpleName(),
                                                               "HiddenHtml",
                                                               HiddenHtmlElement.RULES_DUMMY );

        // generate the element locator of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator( matchingRules,
                                                                    properties,
                                                                    new String[]{ "checkbox" },
                                                                    "input" );
        properties.addInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath );
    }

    /**
     * Check the check box
     */
    @Override
    @PublicAtsApi
    public void check() {

        new HiddenHtmlElementState( this ).waitToBecomeExisting();

        HtmlUnitWebElement element = HiddenHtmlElementLocator.findElement( this );
        if( !element.isEnabled() ) {
            throw new UnsupportedOperationException( "You may not check a disabled element." + toString() );
        }
        if( !element.isSelected() ) {

            element.click();
        }
        
        UiEngineUtilities.sleep();
    }

    /**
     * Uncheck the check box
     */
    @Override
    @PublicAtsApi
    public void unCheck() {

        new HiddenHtmlElementState( this ).waitToBecomeExisting();

        HtmlUnitWebElement element = HiddenHtmlElementLocator.findElement( this );
        if( !element.isEnabled() ) {
            throw new UnsupportedOperationException( "You may not uncheck a disabled element." + toString() );
        }
        if( element.isSelected() ) {

            element.click();
        }
        
        UiEngineUtilities.sleep();
    }

    /**
     * Tells whether the check box is checked
     */
    @Override
    @PublicAtsApi
    public boolean isChecked() {

        new HiddenHtmlElementState( this ).waitToBecomeExisting();

        return HiddenHtmlElementLocator.findElement( this ).isSelected();
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
        if( !isActuallyChecked ) {
            throw new VerificationException( "It was expected to have " + this.toString()
                                             + " checked, but it is unchecked indeed" );
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
        if( isActuallyChecked ) {
            throw new VerificationException( "It was expected to have " + this.toString()
                                             + " unchecked, but it is checked indeed" );
        }
    }
}
