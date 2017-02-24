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
package com.axway.ats.uiengine.engine;

import java.awt.event.KeyEvent;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.system.SystemOperationException;
import com.axway.ats.core.system.LocalSystemOperations;
import com.axway.ats.uiengine.RobotDriver;
import com.axway.ats.uiengine.elements.robot.RobotElementsFactory;
import com.axway.ats.uiengine.exceptions.RobotException;
import com.axway.ats.uiengine.internal.engine.AbstractEngine;

/**
 * Engine able to send keyboard and mouse system commands using Java Robot
 */
@PublicAtsApi
public class RobotEngine extends AbstractEngine {

    private LocalSystemOperations systemOperations;

    public RobotEngine( RobotDriver robotDriver ) {

        super( robotDriver, RobotElementsFactory.getInstance() );

        try {
            this.systemOperations = new LocalSystemOperations();
        } catch( SystemOperationException soe ) {

            throw new RobotException( "Error initializing RobotEngine", soe );
        }
    }

    /**
     * Move the mouse at (X,Y) screen position and then click the mouse button 1
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     */
    @PublicAtsApi
    public void clickAt(
                         int x,
                         int y ) {

        systemOperations.getInputOperations().clickAt( x, y );
    }

    /**
     * Type some text
     *
     * @param text the text to type
     */
    @PublicAtsApi
    public void type(
                      String text ) {

        systemOperations.getInputOperations().type( text );
    }

    /**
     * Type some keys defined in java.awt.event.KeyEvent
     * @param keyCodes the special key codes
     */
    @PublicAtsApi
    public void type(
                      int... keyCodes ) {

        systemOperations.getInputOperations().type( keyCodes );
    }

    /**
     * Type some text but combine them with some keys defined in java.awt.event.KeyEvent
     * <br>It first presses the special key codes(for example Alt + Shift),
     * then it types the provided text and then it releases the special keys in
     * reversed order(for example Shift + Alt )
     *
     * @param text the text to type
     * @param keyCodes the special key codes
     */
    @PublicAtsApi
    public void type(
                      String text,
                      int... keyCodes ) {

        systemOperations.getInputOperations().type( text, keyCodes );
    }

    /**
     * TAB once
     */
    @PublicAtsApi
    public void typeTab() {

        systemOperations.getInputOperations().pressTab();
    }

    /**
     * TAB a number of times
     * @param numberOfTabs how many tabs
     */
    @PublicAtsApi
    public void typeTab(
                         int numberOfTabs ) {

        for( int i = 0; i < numberOfTabs; i++ ) {
            typeTab();
        }
    }

    /**
     * Type the ENTER key
     */
    @PublicAtsApi
    public void typeEnter() {

        systemOperations.getInputOperations().pressEnter();
    }

    /**
     * Press the Escape key
     */
    @PublicAtsApi
    public void pressEsc() {

        systemOperations.getInputOperations().pressEsc();
    }

    /**
     * Press Alt + F4 keys
     */
    @PublicAtsApi
    public void pressAltF4() {

        systemOperations.getInputOperations().pressAltF4();
    }

    /**
     * Type the SPACE key
     */
    @PublicAtsApi
    public void typeSpace() {

        systemOperations.getInputOperations().pressSpace();
    }

    /**
     * Presses a given key. The key should be released using the keyRelease method.
     *
     * @param keyCode Key to press (e.g. {@link KeyEvent}.VK_A)
     */
    @PublicAtsApi
    public void keyPress(
                          int keyCode ) {

        systemOperations.getInputOperations().keyPress( keyCode );
    }

    /**
     * Releases a given key.
     *
     * @param keyCode Key to release (e.g. {@link KeyEvent}.VK_A)
     */
    @PublicAtsApi
    public void keyRelease(
                            int keyCode ) {

        systemOperations.getInputOperations().keyRelease( keyCode );
    }

}
