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

import javax.swing.JCheckBox;

import org.fest.swing.fixture.JCheckBoxFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiCheckBox;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing Check Box
 * <p>
 * Can be identified by:
 * <li>name
 * <li>text
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingCheckBox extends UiCheckBox {

    private static final String[] RULES = { "label,visible",
                                            "label",
                                            "name,text,visible",
                                            "name,text",
                                            "name,visible",
                                            "name",
                                            "text,visible",
                                            "text",
                                            "index" };

    static {
        SwingElementLocator.componentsMap.put(SwingCheckBox.class, JCheckBox.class);
    }

    public SwingCheckBox( UiDriver uiDriver,
                          UiElementProperties properties ) {

        super(uiDriver, properties);
        checkTypeAndRules("Swing", RULES);
    }

    /**
     * Check the CheckBox
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void check() {

        new SwingElementState(this).waitToBecomeExisting();

        ((JCheckBoxFixture) SwingElementLocator.findFixture(this)).check();
    }

    /**
     * @return if the ChechBox is checked or not
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public boolean isChecked() {

        new SwingElementState(this).waitToBecomeExisting();

        return ((JCheckBoxFixture) SwingElementLocator.findFixture(this)).component().isSelected();
    }

    /**
     * Uncheck the CheckBox
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void unCheck() {

        new SwingElementState(this).waitToBecomeExisting();

        ((JCheckBoxFixture) SwingElementLocator.findFixture(this)).uncheck();
    }

    /**
     * Verify the check box is checked
     *
     * @throws VerificationException - if the verification fails or the element does not exist
     */
    @Override
    @PublicAtsApi
    public void verifyChecked() {

        boolean isActuallyChecked = isChecked();
        if (!isActuallyChecked) {
            throw new VerificationException("It was expected to have " + this.toString()
                                            + " checked, but it is unchecked indeed");
        }
    }

    /**
     * Verify the check box is not checked
     *
     * @throws VerificationException - if the verification fails or the element does not exist
     */
    @Override
    @PublicAtsApi
    public void verifyNotChecked() {

        boolean isActuallyChecked = isChecked();
        if (isActuallyChecked) {
            throw new VerificationException("It was expected to have " + this.toString()
                                            + " unchecked, but it is checked indeed");
        }
    }

}
