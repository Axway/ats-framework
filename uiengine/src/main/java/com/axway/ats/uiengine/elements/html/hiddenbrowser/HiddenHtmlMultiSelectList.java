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
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitWebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlMultiSelectList;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.hiddenbrowser.HiddenHtmlElementState;

/**
 * A Multiple Selection HTML list
 * @see HiddenHtmlElement
 */
@PublicAtsApi
public class HiddenHtmlMultiSelectList extends HtmlMultiSelectList {

    public HiddenHtmlMultiSelectList( UiDriver uiDriver,
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
     * select a value
     *
     * @param value the value to select
     */
    @Override
    @PublicAtsApi
    public void setValue(
                          String value ) {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        HtmlUnitWebElement selectElement = HiddenHtmlElementLocator.findElement(this);
        if (selectElement.getAttribute("multiple") == null) {
            throw new SeleniumOperationException("Not a multi-select. You may only add a selection to a select that supports multiple selections. ("
                                                 + this.toString() + ")");
        }

        List<WebElement> optionElements = selectElement.findElements(By.tagName("option"));
        for (WebElement el : optionElements) {
            if (el.getText().equals(value)) {
                if (!el.isSelected()) {
                    ((HtmlUnitWebElement) el).click();

                    UiEngineUtilities.sleep();
                }
                return;
            }
        }

        throw new SeleniumOperationException("Option with label '" + value + "' not found. ("
                                             + this.toString() + ")");
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

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        HtmlUnitWebElement selectElement = HiddenHtmlElementLocator.findElement(this);
        List<WebElement> optionElements = selectElement.findElements(By.tagName("option"));
        for (WebElement el : optionElements) {
            if (el.getText().equals(value)) {
                if (el.isSelected()) {
                    ((HtmlUnitWebElement) el).click();

                    UiEngineUtilities.sleep();
                }
                return;
            }
        }
        throw new SeleniumOperationException("Option with label '" + value + "' not found. ("
                                             + this.toString() + ")");
    }

    /**
     * @return the selected value
     */
    @Override
    @PublicAtsApi
    public String[] getValues() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        HtmlUnitWebElement selectElement = HiddenHtmlElementLocator.findElement(this);
        List<String> values = new ArrayList<String>();
        List<WebElement> optionElements = selectElement.findElements(By.tagName("option"));
        for (WebElement element : optionElements) {
            if (element.isSelected()) {
                values.add(element.getText());
            }
        }
        if (values.isEmpty()) {

            throw new SeleniumOperationException("There is no selected 'option' in " + this.toString());
        }
        return values.toArray(new String[0]);
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
        for (String selectedValue : selectedValues) {
            selectedValue = selectedValue.trim();
            if (selectedValue.equals(expectedValue)) {
                isSelected = true;
                break;
            }
        }

        if (!isSelected) {
            throw new VerifyEqualityException(expectedValue, Arrays.toString(selectedValues), this);
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
        for (String selectedValue : selectedValues) {
            selectedValue = selectedValue.trim();
            if (selectedValue.equals(notExpectedValue)) {
                isSelected = true;
                break;
            }
        }

        if (isSelected) {
            throw new VerifyNotEqualityException(notExpectedValue, this);
        }
    }
}
