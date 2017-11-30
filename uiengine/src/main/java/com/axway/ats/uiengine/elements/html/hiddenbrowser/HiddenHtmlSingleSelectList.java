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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitWebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlSingleSelectList;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.hiddenbrowser.HiddenHtmlElementState;

/**
 * A Single Selection HTML list
 * @see HiddenHtmlElement
 */
@PublicAtsApi
public class HiddenHtmlSingleSelectList extends HtmlSingleSelectList {

    public HiddenHtmlSingleSelectList( UiDriver uiDriver,
                                       UiElementProperties properties ) {

        super(uiDriver, properties);
        String matchingRules[] = properties.checkTypeAndRules(this.getClass().getSimpleName(),
                                                              "HiddenHtml",
                                                              HiddenHtmlElement.RULES_DUMMY);

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator(matchingRules,
                                                                   properties,
                                                                   new String[]{ "select" },
                                                                   "select");
        properties.addInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath);
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

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        HtmlUnitWebElement selectElement = HiddenHtmlElementLocator.findElement(this);
        List<WebElement> optionElements = selectElement.findElements(By.tagName("option"));
        for (WebElement el : optionElements) {
            if (el.getText().equals(value)) {
                ((HtmlUnitWebElement) el).click();
                UiEngineUtilities.sleep();
                return;
            }
        }

        throw new SeleniumOperationException("Option with label '" + value + "' not found. ("
                                             + this.toString() + ")");
    }

    /**
     * @return the single selection value
     */
    @Override
    @PublicAtsApi
    public String getValue() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        HtmlUnitWebElement selectElement = HiddenHtmlElementLocator.findElement(this);
        List<WebElement> optionElements = selectElement.findElements(By.tagName("option"));
        for (WebElement element : optionElements) {
            if (element.isSelected()) {
                return element.getText();
            }
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
        new HiddenHtmlElementState(this).waitToBecomeExisting();

        HtmlUnitWebElement selectElement = HiddenHtmlElementLocator.findElement(this);
        List<WebElement> optionElements = selectElement.findElements(By.tagName("option"));

        if (optionElements.size() > 0) {
            for (WebElement element : optionElements) {
                values.add(element.getText());
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
