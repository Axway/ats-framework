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

import javax.swing.JComboBox;

import org.fest.swing.exception.LocationUnavailableException;
import org.fest.swing.fixture.JComboBoxFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiComboBox;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.UiElementException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing ComboBox
 * <p>
 * Can be identified by:
 * <li>name
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingComboBox extends UiComboBox {

    private static final String[] RULES = { "label,visible", "label", "name,visible", "name", "index" };

    static {
        SwingElementLocator.componentsMap.put( SwingComboBox.class, JComboBox.class );
    }

    public SwingComboBox( UiDriver uiDriver,
                          UiElementProperties properties ) {

        super( uiDriver, properties );
        checkTypeAndRules( "Swing", RULES );
    }

    /**
     * Get ComboBox value
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public String getValue() {

        new SwingElementState( this ).waitToBecomeExisting();

        return ( ( JComboBoxFixture ) SwingElementLocator.findFixture( this ) ).component()
                                                                               .getSelectedItem()
                                                                               .toString();
    }

    /**
     * Set ComboBox value
     * @param value the ComboBox value to set
     * @throws VerificationException if the element doesn't exist
     */
    @SuppressWarnings("unchecked")
    @Override
    @PublicAtsApi
    public void setValue(
                          String value ) {

        new SwingElementState( this ).waitToBecomeExisting();

        JComboBoxFixture comboBoxFixture = null;
        try {

            comboBoxFixture = ( ( JComboBoxFixture ) SwingElementLocator.findFixture( this ) );
            comboBoxFixture.selectItem( value );
        } catch( LocationUnavailableException lue ) { // an element matching the given text cannot be found

            // if the element is editable we'll enter the new value
            if( comboBoxFixture != null && comboBoxFixture.component().isEditable() ) {

                try {
                    comboBoxFixture.component().addItem( value );
                    comboBoxFixture.selectItem( value );
                } catch( LocationUnavailableException e ) {

                    throw new UiElementException( e.getMessage(), this );
                }
            } else {

                throw new UiElementException( lue.getMessage(), this );
            }
        }
    }

    /**
     * Get ComboBox available values
     *
     * @return {@link String} array with all the available values
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public String[] getAvailableValues() {

        new SwingElementState( this ).waitToBecomeExisting();

        return ( ( JComboBoxFixture ) SwingElementLocator.findFixture( this ) ).contents();
    }

    /**
     * Verify the selected value is as specified
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
