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

import javax.swing.JRadioButton;

import org.fest.swing.fixture.JRadioButtonFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing Radio Button
 * <p>
 * Can be identified by:
 * <li>name
 * <li>text
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingRadioButton extends UiElement {

    private static final String[] RULES = { // the order here is important in order to find the largest set of properties matching properties in map file
            "label,visible" /* used for labelFor */,
            "label" /* used for labelFor */,
            "name,text,visible",
            "name,text",
            "name,visible",
            "name",
            "text,visible",
            "text",
            "index"                    };

    static {
        SwingElementLocator.componentsMap.put( SwingRadioButton.class, JRadioButton.class );
    }

    public SwingRadioButton( UiDriver uiDriver,
                             UiElementProperties properties ) {

        super( uiDriver, properties );
        checkTypeAndRules( "Swing", RULES );
    }

    /**
     * Check if the radio button is selected or not
     *
     * @return if the radio button is selected or not
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public boolean isSelected() {

        new SwingElementState( this ).waitToBecomeExisting();

        return ( ( JRadioButtonFixture ) SwingElementLocator.findFixture( this ) ).component().isSelected();
    }

    /**
     * Select radio button
     *
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void select() {

        new SwingElementState( this ).waitToBecomeExisting();

        ( ( JRadioButtonFixture ) SwingElementLocator.findFixture( this ) ).check();
    }

    /**
     * Verify the selected value is as specified
     *
     * @param expectedValue
     * @throws VerificationException if the element doesn't exist or the verification fails
     */
    @PublicAtsApi
    public void verifySelected() {

        if( !isSelected() ) {
            throw new VerificationException( toString() + " is NOT selected." );
        }
    }

    /**
     * Verify the selected value is NOT as specified
     *
     * @param notExpectedValue
     * @throws VerificationException if the element doesn't exist or the verification fails
     */
    @PublicAtsApi
    public void verifyNotSelected() {

        if( isSelected() ) {
            throw new VerificationException( toString() + " is selected." );
        }
    }

}
