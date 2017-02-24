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
package com.axway.ats.core.system.model;

import java.awt.event.KeyEvent;

public interface ISystemInputOperations {

    /**
     * Move the mouse at (X,Y) screen position and then click the mouse button 1
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     */
    public void clickAt(
                         int x,
                         int y );

    /**
     * Type some text
     *
     * @param text the text to type
     */
    public void type(
                      String text );

    /**
     * Type some keys defined in java.awt.event.KeyEvent
     * @param keyCodes the special key codes
     */
    public void type(
                      int... keyCodes );

    /**
     * Type some text but combine them with some keys defined in java.awt.event.KeyEvent
     * <br>It first presses the special key codes(for example Alt + Shift),
     * then it types the provided text and then it releases the special keys in
     * reversed order(for example Shift + Alt )
     *
     * @param text the text to type
     * @param keyCodes the special key codes
     */
    public void type(
                      String text,
                      int... keyCodes );

    /**
     * Press the TAB key
     */
    public void pressTab();

    /**
     * Press the SPACE key
     */
    public void pressSpace();

    /**
     * Press the ENTER key
     */
    public void pressEnter();

    /**
     * Press the Escape key
     */
    public void pressEsc();

    /**
     * Press Alt + F4 keys
     */
    public void pressAltF4();

    /**
     * Presses a given key. The key should be released using the keyRelease method.
     *
     * @param keyCode Key to press (e.g. {@link KeyEvent}.VK_A)
     */
    public void keyPress(
                          int keyCode );

    /**
     * Releases a given key.
     *
     * @param keyCode Key to release (e.g. {@link KeyEvent}.VK_A)
     */
    public void keyRelease(
                            int keyCode );
}
