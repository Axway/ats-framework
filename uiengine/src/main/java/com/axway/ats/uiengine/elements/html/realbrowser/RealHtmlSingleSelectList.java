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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlSingleSelectList;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * A Single Selection HTML list
 * @see RealHtmlElement
 */
@PublicAtsApi
public class RealHtmlSingleSelectList extends HtmlSingleSelectList {

    //private WebDriver webDriver;

    public RealHtmlSingleSelectList( UiDriver uiDriver,
                                     UiElementProperties properties ) {

        super(uiDriver, properties);
        String[] matchingRules = properties.checkTypeAndRules(this.getClass().getSimpleName(),
                                                              "RealHtml",
                                                              RealHtmlElement.RULES_DUMMY);

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator(matchingRules,
                                                                   properties,
                                                                   new String[]{ "select" },
                                                                   "select");
        properties.addInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath);

        //webDriver = ( WebDriver ) ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * set the single selection value
     *
     * @param value the value to select
     */
    @Override
    @PublicAtsApi
    public void setValue(
                          String value ) {

        new RealHtmlElementState(this).waitToBecomeExisting();

        try {
            WebElement element = RealHtmlElementLocator.findElement(this);
            Select select = new Select(element);
            select.selectByVisibleText(value);
        } catch (NoSuchElementException nsee) {
            throw new SeleniumOperationException("Option with label '" + value + "' not found. ("
                                                 + this.toString() + ")");
        }
        UiEngineUtilities.sleep();
    }

    /**
     * @return the single selection value
     */
    @Override
    @PublicAtsApi
    public String getValue() {

        new RealHtmlElementState(this).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement(this);
        Select select = new Select(element);
        if (!select.getAllSelectedOptions().isEmpty()) {
            return select.getFirstSelectedOption().getText();
        }
        throw new SeleniumOperationException("There is no selected 'option' in " + this.toString());
    }

    /**
     * @return  a list with all possible selection values
     */
    @Override
    @PublicAtsApi
    public List<String> getAllPossibleValues() {

        List<String> values = new ArrayList<String>();
        new RealHtmlElementState(this).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement(this);
        Select select = new Select(element);
        Iterator<WebElement> iterator = select.getOptions().iterator();

        if (!select.getAllSelectedOptions().isEmpty()) {
            while (iterator.hasNext()) {
                values.add(iterator.next().getText());
            }
            return values;
        }
        throw new SeleniumOperationException("There is no selectable 'option' in " + this.toString());
    }

    /**
     * Verify the selected value is as specified
     *
     * @param expectedValue
     */
    @Override
    @PublicAtsApi
    public void verifyValue(
                             String expectedValue ) {

        expectedValue = expectedValue.trim();

        String actualText = getValue().trim();
        if (!actualText.equals(expectedValue)) {
            throw new VerifyEqualityException(expectedValue, actualText, this);
        }
    }

    /**
     * Verify the selected value is NOT as specified
     *
     * @param notExpectedValue
     */
    @Override
    @PublicAtsApi
    public void verifyNotValue(
                                String notExpectedValue ) {

        notExpectedValue = notExpectedValue.trim();

        String actualText = getValue().trim();
        if (actualText.equals(notExpectedValue)) {
            throw new VerifyNotEqualityException(notExpectedValue, this);
        }
    }
}
