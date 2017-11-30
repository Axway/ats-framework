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

import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.JList;

import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JPopupMenuFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiMultiSelectList;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Multiple Selection Swing List
 * <p>
 * Can be identified by:
 * <li>name
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingMultiSelectList extends UiMultiSelectList {

    private static final String[] RULES = { "label,visible", "label", "name,visible", "name", "index" };

    static {
        SwingElementLocator.componentsMap.put(SwingMultiSelectList.class, JList.class);
    }

    public SwingMultiSelectList( UiDriver uiDriver, UiElementProperties properties ) {

        super(uiDriver, properties);
        checkTypeAndRules("Swing", RULES);
    }

    /**
     * Get MultiSelectList value
     *
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public String[] getValues() {

        new SwingElementState(this).waitToBecomeExisting();

        return ((JListFixture) SwingElementLocator.findFixture(this)).selection();
    }

    /**
     * Set MultiSelectList value
     *
     * @param value the value to set
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void setValue( String value ) {

        new SwingElementState(this).waitToBecomeExisting();

        JListFixture listFixture = ((JListFixture) SwingElementLocator.findFixture(this));
        listFixture.pressKey(KeyEvent.VK_CONTROL);
        try {
            listFixture.selectItem(value);
        } finally {
            listFixture.releaseKey(KeyEvent.VK_CONTROL);
        }
    }

    /**
     * Unset MultiSelectList value
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void unsetValue( String value ) {

        if (Arrays.asList(getValues()).contains(value)) {

            JListFixture listFixture = ((JListFixture) SwingElementLocator.findFixture(this));
            listFixture.pressKey(KeyEvent.VK_CONTROL);
            try {
                listFixture.clickItem(value);
            } finally {
                listFixture.releaseKey(KeyEvent.VK_CONTROL);
            }
        }

    }

    /**
     * Get MultiSelectList available values
     *
     * @return {@link String} array with all the available values
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public String[] getAvailableValues() {

        new SwingElementState(this).waitToBecomeExisting();

        return ((JListFixture) SwingElementLocator.findFixture(this)).contents();
    }

    /**
     * Clears the selection
     *
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void clearSelections() {

        new SwingElementState(this).waitToBecomeExisting();

        ((JListFixture) SwingElementLocator.findFixture(this)).clearSelection();
    }

    /**
     *
     * @param contextMenuItems context menu items to select
     */
    @PublicAtsApi
    public void rightClick( String... contextMenuItems ) {

        new SwingElementState(this).waitToBecomeExisting();

        JListFixture listFixture = ((JListFixture) SwingElementLocator.findFixture(this));
        JPopupMenuFixture popUpMenu = listFixture.showPopupMenu();
        popUpMenu.menuItemWithPath(contextMenuItems).click();
    }

    /**
     * Verify the specified value is selected
     *
     * @param expectedValue
     * @throws VerificationException if the element doesn't exist
     * @throws VerifyEqualityException if the verification fails
     */
    @Override
    @PublicAtsApi
    public void verifyValue( String expectedValue ) {

        boolean isSelected = false;

        String[] selectedValues = getValues();
        for (String selectedValue : selectedValues) {
            if (selectedValue.equals(expectedValue)) {
                isSelected = true;
                break;
            }
        }
        if (!isSelected && expectedValue != null) {
            throw new VerifyEqualityException(expectedValue, Arrays.toString(selectedValues), this);
        }
    }

    /**
     * Verify the specified value is NOT selected
     *
     * @param notExpectedValue
     * @throws VerificationException if the element doesn't exist
     * @throws VerifyNotEqualityException if the verification fails
     */
    @Override
    @PublicAtsApi
    public void verifyNotValue( String notExpectedValue ) {

        new SwingElementState(this).waitToBecomeExisting();

        JListFixture listFixture = ((JListFixture) SwingElementLocator.findFixture(this));
        String selectedValue = (String) listFixture.component().getSelectedValue();

        if ( (notExpectedValue == null && selectedValue == null)
             || (StringUtils.isNotNullAndEquals(selectedValue, notExpectedValue))) {

            throw new VerifyNotEqualityException(notExpectedValue, this);
        }
    }
}
