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

import javax.swing.JSpinner;

import org.fest.swing.fixture.JSpinnerFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.UiElementException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing Spinner
 * <p>
 * Can be identified by:
 * <li>name
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingSpinner extends UiElement {

    private static final String[] RULES = { "label,visible", "label", "name,visible", "name", "index" };

    static {
        SwingElementLocator.componentsMap.put( SwingSpinner.class, JSpinner.class );
    }

    public SwingSpinner( UiDriver uiDriver, UiElementProperties properties ) {

        super( uiDriver, properties );
        checkTypeAndRules( "Swing", RULES );
    }

    /**
     * Set spinner text value (entering and committing the given text in the JSpinner)
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void setValue( String value ) {

        new SwingElementState( this ).waitToBecomeExisting();

        JSpinnerFixture spinnerFixture = ( JSpinnerFixture ) SwingElementLocator.findFixture( this );

        int delayBetweenEvents = spinnerFixture.robot.settings().delayBetweenEvents();
        try {
            // enterTextAndCommit() method sets the text value using the Robot, so we will speed it up
            String delay = AtsSystemProperties.getPropertyAsString( AtsSystemProperties.UI_ENGINE__SWING_ROBOT_DELAY_BETWEEN_EVENTS );
            if( delay != null ) {
                int ms = -1;
                try {
                    ms = Integer.parseInt( delay );
                } catch( NumberFormatException ex ) {
                    log.error( "Illegal robot dealy between events specified! Will be used default one", ex );
                }
                if( ms >= 0 ) {
                    spinnerFixture.robot.settings().delayBetweenEvents( ms );
                }
            } else {
                spinnerFixture.robot.settings().delayBetweenEvents( 100 ); // hardcode to 100ms
            }
            spinnerFixture.enterTextAndCommit( value );
        } finally {
            spinnerFixture.robot.settings().delayBetweenEvents( delayBetweenEvents );
        }
    }

    /**
     * Get spinner text value
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public String getValue() {

        new SwingElementState( this ).waitToBecomeExisting();

        return ( ( JSpinnerFixture ) SwingElementLocator.findFixture( this ) ).text();
    }

    /**
     * Enter text in the spinner text field without committing the value.
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void enterText( String text ) {

        new SwingElementState( this ).waitToBecomeExisting();

        JSpinnerFixture spinnerFixture = ( JSpinnerFixture ) SwingElementLocator.findFixture( this );

        int delayBetweenEvents = spinnerFixture.robot.settings().delayBetweenEvents();
        try {
            // enterText() method sets the text value using the Robot, so we will speed it up
            spinnerFixture.robot.settings().delayBetweenEvents( 10 );
            spinnerFixture.enterText( text );
        } finally {
            spinnerFixture.robot.settings().delayBetweenEvents( delayBetweenEvents );
        }
    }

    /**
     * Click next/increment spinner button
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void clickNext() {

        clickNext( 1 );
    }

    /**
     * Click next/increment spinner button
     * @param times number of times to click. Must be greater than 0
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void clickNext( int times ) {

        if( times <= 0 ) {

            throw new UiElementException( "The number of times to click must be greater than 0", this );
        }

        new SwingElementState( this ).waitToBecomeExisting();

        ( ( JSpinnerFixture ) SwingElementLocator.findFixture( this ) ).increment( times );
    }

    /**
     * Click previous/decrement spinner button
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void clickPrevious() {

        clickPrevious( 1 );
    }

    /**
     * Click previous/decrement spinner button
     * @param times number of times to click. Must be greater than 0
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void clickPrevious( int times ) {

        if( times <= 0 ) {

            throw new UiElementException( "The number of times to click must be greater than 0", this );
        }

        new SwingElementState( this ).waitToBecomeExisting();

        ( ( JSpinnerFixture ) SwingElementLocator.findFixture( this ) ).decrement( times );
    }

}
