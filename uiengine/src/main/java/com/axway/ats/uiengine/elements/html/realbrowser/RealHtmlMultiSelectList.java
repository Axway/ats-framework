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

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlMultiSelectList;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * A Multiple Selection HTML list
 * @see RealHtmlElement
 */
@PublicAtsApi
public class RealHtmlMultiSelectList extends HtmlMultiSelectList {

    //private WebDriver webDriver;

    public RealHtmlMultiSelectList( UiDriver uiDriver,
                                    UiElementProperties properties ) {

        super( uiDriver, properties );
        String[] matchingRules = properties.checkTypeAndRules( this.getClass().getSimpleName(),
                                                               "RealHtml",
                                                               RealHtmlElement.RULES_DUMMY );

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator( matchingRules,
                                                                    properties,
                                                                    new String[]{ "select" },
                                                                    "select" );
        properties.addInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath );

        //webDriver = ( WebDriver ) ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * select a value
     *
     * @param value the value to select
     */
    @Override
    @PublicAtsApi
    public void setValue(
                          String value ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        try {
            WebElement element = RealHtmlElementLocator.findElement( this );
            Select select = new Select( element );
            select.selectByVisibleText( value );
        } catch( NoSuchElementException nsee ) {
            throw new SeleniumOperationException( "Option with label '" + value + "' not found. ("
                                                  + this.toString() + ")" );
        }
        UiEngineUtilities.sleep();
    }

    /**
     * unselect a value
     *
     * @param value the value to unselect
     */
    @Override
    @PublicAtsApi
    public void unsetValue(
                            String value ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        Select select = new Select( element );
        // select.deselectByVisibleText( value ); // this method doesn't throw an exception if the option doesn't exist
        for( WebElement option : select.getOptions() ) {
            if( option.getText().equals( value ) ) {
                if( option.isSelected() ) {
                    option.click();
                    UiEngineUtilities.sleep();
                }
                return;
            }
        }
        throw new SeleniumOperationException( "Option with label '" + value + "' not found. ("
                                                  + this.toString() + ")" );
    }

    /**
     * @return the selected values
     */
    @Override
    @PublicAtsApi
    public String[] getValues() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        Select select = new Select( element );
        List<WebElement> selectedOptions = select.getAllSelectedOptions();
        String[] result = new String[selectedOptions.size()];
        int i = 0;
        for( WebElement selectedOption : selectedOptions ) {
            result[i++] = selectedOption.getText();
        }
        return result;
    }

    /**
     * Verify the specified value is selected
     *
     * @param expectedValue
     */
    @Override
    @PublicAtsApi
    public void verifyValue(
                             String expectedValue ) {

        boolean isSelected = false;

        expectedValue = expectedValue.trim();

        String[] selectedValues = getValues();
        for( String selectedValue : selectedValues ) {
            selectedValue = selectedValue.trim();
            if( selectedValue.equals( expectedValue ) ) {
                isSelected = true;
                break;
            }
        }

        if( !isSelected ) {
            throw new VerifyEqualityException( expectedValue, Arrays.toString( selectedValues ), this );
        }
    }

    /**
     * Verify the specified value is NOT selected
     *
     * @param notExpectedValue
     */
    @Override
    @PublicAtsApi
    public void verifyNotValue(
                                String notExpectedValue ) {

        boolean isSelected = false;

        notExpectedValue = notExpectedValue.trim();

        String[] selectedValues = getValues();
        for( String selectedValue : selectedValues ) {
            selectedValue = selectedValue.trim();
            if( selectedValue.equals( notExpectedValue ) ) {
                isSelected = true;
                break;
            }
        }

        if( isSelected ) {
            throw new VerifyNotEqualityException( notExpectedValue, this );
        }
    }

}
