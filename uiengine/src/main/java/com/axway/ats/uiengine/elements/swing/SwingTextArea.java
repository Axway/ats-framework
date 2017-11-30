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
package com.axway.ats.uiengine.elements.swing;

import javax.swing.text.JTextComponent;

import org.fest.swing.fixture.JTextComponentFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiTextArea;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing Text Area/Field
 * <p>
 * Can be identified by:
 * <li>name
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingTextArea extends UiTextArea {

    private static final String[] RULES = { "name", "label", "index", "class" };

    static {
        SwingElementLocator.componentsMap.put(SwingTextArea.class, JTextComponent.class);
    }

    public SwingTextArea( UiDriver uiDriver,
                          UiElementProperties properties ) {

        super(uiDriver, properties);
        checkTypeAndRules("Swing", RULES);
    }

    /**
     * Get text area value
     *
     * @throws VerificationException if the text area element doesn't exist
     */
    @Override
    @PublicAtsApi
    public String getValue() {

        new SwingElementState(this).waitToBecomeExisting();

        return ((JTextComponentFixture) SwingElementLocator.findFixture(this)).text();
    }

    /**
     * Set text area value
     *
     * @param value the value to set
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void setValue(
                          String value ) {

        new SwingElementState(this).waitToBecomeExisting();

        ((JTextComponentFixture) SwingElementLocator.findFixture(this)).setText(value);
    }

    /**
     * Works in same way as the 'set text' method
     * 
     * @param value
     */
    @Override
    @PublicAtsApi
    public void appendValue(
                             String value ) {

        new SwingElementState(this).waitToBecomeExisting();

        ((JTextComponentFixture) SwingElementLocator.findFixture(this)).setText(value);
    }

    /**
     * Verify the Text Area value is as specified
     *
     * @param expectedValue
     * @throws VerificationException if the element doesn't exist
     * @throws VerifyEqualityException if the verification fails
     */
    @Override
    @PublicAtsApi
    public void verifyValue(
                             String expectedValue ) {

        expectedValue = normalizeText(expectedValue);

        String actualText = normalizeText(getValue());
        if (!actualText.equals(expectedValue)) {
            throw new VerifyEqualityException(expectedValue, actualText, this);
        }
    }

    /**
     * Verify the Text Area value is NOT as specified
     *
     * @param notExpectedValue
     * @throws VerificationException if the element doesn't exist
     * @throws VerifyNotEqualityException if the verification fails
     */
    @Override
    @PublicAtsApi
    public void verifyNotValue(
                                String notExpectedValue ) {

        notExpectedValue = normalizeText(notExpectedValue);

        String actualText = normalizeText(getValue());
        if (actualText.equals(notExpectedValue)) {
            throw new VerifyNotEqualityException(notExpectedValue, this);
        }
    }

    private String normalizeText(
                                  String src ) {

        return src.replace("\r\n", "\n").trim();
    }

}
