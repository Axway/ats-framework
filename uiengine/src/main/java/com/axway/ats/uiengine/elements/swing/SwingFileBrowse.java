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

import javax.swing.JFileChooser;

import org.fest.swing.fixture.JFileChooserFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiFileBrowse;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing File Chooser
 */
@PublicAtsApi
public class SwingFileBrowse extends UiFileBrowse {

    static {
        SwingElementLocator.componentsMap.put( SwingFileBrowse.class, JFileChooser.class );
    }

    public SwingFileBrowse( UiDriver uiDriver,
                            UiElementProperties properties ) {

        super( uiDriver, properties );
        // TODO - checkTypeAndRules( "Swing", RULES );
    }

    /**
     * Click the Approve button (labeled "Open" or "Save", by default).
     * This method causes an action event to fire with the command string equal to APPROVE_SELECTION.
     *
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void clickApprove() {

        new SwingElementState( this ).waitToBecomeExisting();

        ( ( JFileChooserFixture ) SwingElementLocator.findFixture( this ) ).approve();
    }

    /**
     * Click the Cancel button
     *
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void clickCancel() {

        new SwingElementState( this ).waitToBecomeExisting();

        ( ( JFileChooserFixture ) SwingElementLocator.findFixture( this ) ).cancel();
    }

    /**
     * Set value in the 'File Name' text field
     *
     * @param value the text value to set
     */
    @Override
    @PublicAtsApi
    public void setValue(
                          String value ) {

        new SwingElementState( this ).waitToBecomeExisting();

        ( ( JFileChooserFixture ) SwingElementLocator.findFixture( this ) ).fileNameTextBox().setText( value );
    }

    /**
     * Get the value of the 'File Name' text field
     *
     * @return the text value of the 'File Name' text field
     */
    @Override
    @PublicAtsApi
    public String getValue() {

        new SwingElementState( this ).waitToBecomeExisting();

        return ( ( JFileChooserFixture ) SwingElementLocator.findFixture( this ) ).fileNameTextBox().text();
    }

    /**
     * Verifies that the value of 'File Name' text field is as expected
     *
     * @param expectedValue the expected text value
     * @throws VerifyEqualityException if the expected text value is different from the actual
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
     * Verifies that the value of 'File Name' text field is different from the expected
     *
     * @param notExpectedValue the NOT expected text value
     * @throws VerifyNotEqualityException if the expected text value is equal with the actual
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
