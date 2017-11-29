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

import javax.swing.JButton;

import org.fest.swing.fixture.JButtonFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiButton;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing Button
 * <p>
 * Can be identified by:
 * <li>name
 * <li>text
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingButton extends UiButton {

    private static final String[] RULES = { // the order here is important in order to find the largest set of properties matching properties in map file
            "label,visible" /* used for labelFor */,
            "label" /* used for labelFor */,
            "name,text,visible",
            "name,text",
            "name,visible",
            "name",
            "text,visible",
            "text",
            "tooltip,index",
            "tooltip",
            "index"                    };

    static {
        SwingElementLocator.componentsMap.put( SwingButton.class, JButton.class );
    }

    public SwingButton( UiDriver uiDriver,
                        UiElementProperties properties ) {

        super( uiDriver, properties );
        checkTypeAndRules( "Swing", RULES );
    }

    /**
     * Click button
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void click() {

        new SwingElementState( this ).waitToBecomeExisting();

        ( ( JButtonFixture ) SwingElementLocator.findFixture( this ) ).click();
    }

    /**
     * Check whether the button is Enabled or Disabled
     * @return <code>true</code> if the button is enabled
     */
    @PublicAtsApi
    public boolean isEnabled() {

        new SwingElementState( this ).waitToBecomeExisting();

        return ( ( JButtonFixture ) SwingElementLocator.findFixture( this ) ).target.isEnabled();
    }

}
