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

import javax.swing.JOptionPane;

import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.fixture.JOptionPaneFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.ElementNotFoundException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing Option Pane
 *
 */
@PublicAtsApi
public class SwingOptionPane extends UiElement {

    static {
        SwingElementLocator.componentsMap.put( SwingOptionPane.class, JOptionPane.class );
    }

    public SwingOptionPane( UiDriver uiDriver,
                            UiElementProperties properties ) {

        super( uiDriver, properties );
    }

    /**
     * Click button by its text
     *
     * @param text the text of button to click
     */
    @PublicAtsApi
    public void clickButtonByText(
                                   String text ) {

        new SwingElementState( this ).waitToBecomeExisting();

        try {
            ( ( JOptionPaneFixture ) SwingElementLocator.findFixture( this ) ).buttonWithText( text ).click();
        } catch( ComponentLookupException cle ) {
            throw new ElementNotFoundException( cle.getMessage() );
        }
    }

    /**
     * Setting text in the Text field
     *
     * @param text the text to set
     */
    @PublicAtsApi
    public void setText(
                         String text ) {

        new SwingElementState( this ).waitToBecomeExisting();

        try {
            ( ( JOptionPaneFixture ) SwingElementLocator.findFixture( this ) ).textBox().setText( text );
        } catch( ComponentLookupException cle ) {
            throw new ElementNotFoundException( cle.getMessage() );
        }
    }

    /**
     * Getting the text of the Text field
     *
     * @return the text of the Text field
     */
    @PublicAtsApi
    public String getText() {

        new SwingElementState( this ).waitToBecomeExisting();

        try {
            return ( ( JOptionPaneFixture ) SwingElementLocator.findFixture( this ) ).textBox().text();
        } catch( ComponentLookupException cle ) {
            throw new ElementNotFoundException( cle.getMessage() );
        }
    }

}
