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

import javax.swing.JList;

import org.fest.swing.fixture.JListFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiSingleSelectList;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Single Selection Swing List
 * <p>
 * Can be identified by:
 * <li>name
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingSingleSelectList extends UiSingleSelectList {

    private static final String[] RULES = { "label,visible", "label", "name,visible", "name", "index" };

    static {
        SwingElementLocator.componentsMap.put( SwingSingleSelectList.class, JList.class );
    }

    public SwingSingleSelectList( UiDriver uiDriver,
                                  UiElementProperties properties ) {

        super( uiDriver, properties );
        checkTypeAndRules( "Swing", RULES );
    }

    /**
     * Get SingleSelectList value
     *
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public String getValue() {

        new SwingElementState( this ).waitToBecomeExisting();

        String[] selections = ( ( JListFixture ) SwingElementLocator.findFixture( this ) ).selection();
        if( selections.length > 0 ) {
            return selections[0];
        }
        return "";
    }

    /**
     * Set SingleSelectList value
     *
     * @param value the value to set
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void setValue(
                          String value ) {

        new SwingElementState( this ).waitToBecomeExisting();

        ( ( JListFixture ) SwingElementLocator.findFixture( this ) ).selectItem( value );
    }

    /**
     * Get SingleSelectList available values
     *
     * @return {@link String} array with all the available values
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public String[] getAvailableValues() {

        new SwingElementState( this ).waitToBecomeExisting();

        return ( ( JListFixture ) SwingElementLocator.findFixture( this ) ).contents();
    }

    /**
     * Clears the selection
     *
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void clearSelection() {

        new SwingElementState( this ).waitToBecomeExisting();

        ( ( JListFixture ) SwingElementLocator.findFixture( this ) ).clearSelection();
    }

    /**
     * Verify the selected value is as specified
     *
     * @param expectedValue
     * @throws VerificationException if the element doesn't exist
     * @throws VerifyEqualityException if the verification fails
     */
    @Override
    @PublicAtsApi
    public void verifyValue(
                             String expectedValue ) {

        String actualText = getValue();
        if( !actualText.equals( expectedValue ) ) {

            throw new VerifyEqualityException( expectedValue, actualText, this );
        }
    }

    /**
     * Verify the selected value is NOT as specified
     *
     * @param notExpectedValue
     * @throws VerificationException if the element doesn't exist
     * @throws VerifyNotEqualityException if the verification fails
     */
    @Override
    @PublicAtsApi
    public void verifyNotValue(
                                String notExpectedValue ) {

        String actualText = getValue();
        if( actualText.equals( notExpectedValue ) ) {

            throw new VerifyNotEqualityException( notExpectedValue, this );
        }
    }

}
