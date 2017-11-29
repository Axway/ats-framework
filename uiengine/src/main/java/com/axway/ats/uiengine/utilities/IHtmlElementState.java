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
package com.axway.ats.uiengine.utilities;

import com.axway.ats.common.PublicAtsApi;

/**
 * Check the state of an HTML element
 */
@PublicAtsApi
public interface IHtmlElementState {

    /**
     * Moves the focus to the specified element.
     * <b>Note:</b> This is somewhat breakable as
     * the browser window needs to be the active system window, otherwise the keyboard
     * events will go to another application.
     */
    @PublicAtsApi
    public void focus();

    /**
     * Verifies the element exist
     *
     * throws an exception if verification fail
     */
    @PublicAtsApi
    public void verifyExist();

    /**
     * Verifies the element does NOT exist
     *
     * throws an exception if verification fail
     */
    @PublicAtsApi
    public void verifyNotExist();

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become existing
     *
     * throws an exception if it does not become existing
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeExisting();

    /**
     * Waits for a period of time the element to become existing
     *
     * throws an exception if it does not become existing for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeExisting(
                                      int millis );

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to became non-existing
     *
     * throws an exception if it does not become non-existing
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeNotExisting();

    /**
     * Waits for a period of time the element to became non-existing
     *
     * throws an exception if it does not become non-existing for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeNotExisting(
                                         int millis );

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become displayed
     *
     * throws an exception if it does not become displayed
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeDisplayed();

    /**
     * Waits for a period of time the element to become displayed
     *
     * throws an exception if it does not become displayed for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeDisplayed(
                                       int millis );

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become hidden
     *
     * throws an exception if it does not become hidden
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeHidden();

    /**
     * Waits for a period of time the element to become hidden
     *
     * throws an exception if it does not become hidden for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeHidden(
                                    int millis );

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become enabled
     *
     * throws an exception if it does not become enabled
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeEnabled();

    /**
     * Waits for a period of time the element to become enabled
     *
     * throws an exception if it does not become enabled for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeEnabled(
                                     int millis );

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become disabled
     *
     * throws an exception if it does not become disabled
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeDisabled();

    /**
     * Waits for a period of time the element to become disabled
     *
     * throws an exception if it does not become disabled for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeDisabled(
                                      int millis );

    /**
     * Tries to briefly change the element's background color to yellow. Does not work on all elements.
     * It is useful for debugging purposes.
     */
    @PublicAtsApi
    public void highlightElement();

    /**
     *
     * @return <code>true</code> if the element exists
     */
    @PublicAtsApi
    public boolean isElementPresent();

    /**
     *
     * @return <code>true</code> if the element is enabled
     */
    @PublicAtsApi
    public boolean isElementEnabled();

    /**
     * Check whether the element is displayed or not
     *
     * @return <code>true</code> if the element is displayed
     */
    @PublicAtsApi
    public abstract boolean isElementDisplayed();
}
