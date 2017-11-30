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
package com.axway.ats.uiengine.utilities.swing;

import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.swing.SwingElementLocator;
import com.axway.ats.uiengine.exceptions.ElementNotFoundException;
import com.axway.ats.uiengine.exceptions.NotSupportedOperationException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;

@PublicAtsApi
public class SwingElementState {

    private UiElement           element;

    private UiElementProperties elementProperties;

    private static final int    SLEEP_PERIOD = 100;

    private Exception           lastNotFoundException;

    /**
     * @param uiElement the element of interest
     */
    public SwingElementState( UiElement uiElement ) {

        this.element = uiElement;
        this.elementProperties = uiElement.getElementProperties();
    }

    /**
     * Verifies the element exist
     *
     * @throws VerificationException if the verification fails
     */
    @PublicAtsApi
    public void verifyExist() {

        boolean exists = isElementPresent();
        if (!exists) {
            throw new VerificationException(getElementDescription()
                                            + " does not exist while it is expected to exist",
                                            lastNotFoundException);
        }
    }

    /**
     * Verifies the element does NOT exist
     *
     * @throws VerificationException if the verification fails
     */
    @PublicAtsApi
    public void verifyNotExist() {

        boolean exists = isElementPresent();
        if (exists) {
            throw new VerificationException(getElementDescription()
                                            + " exists while it is expected to not exist",
                                            lastNotFoundException);
        }
    }

    /**
     * Verifies the element is visible
     *
     * @throws VerificationException if the verification fails
     */
    @PublicAtsApi
    public void verifyVisible() {

        boolean visible = isElementVisible();
        if (!visible) {
            throw new VerificationException(getElementDescription()
                                            + " is invisible while it is expected to be visible",
                                            lastNotFoundException);
        }
    }

    /**
     * Verifies the element is not visible
     *
     * @throws VerificationException if the verification fails
     */
    @PublicAtsApi
    public void verifyNotVisible() {

        boolean visible = isElementVisible();
        if (visible) {
            throw new VerificationException(getElementDescription()
                                            + " is visible while it is expected to be invisible",
                                            lastNotFoundException);
        }
    }

    /**
     * Verifies the element is enabled
     *
     * @throws VerificationException if the verification fails
     */
    @PublicAtsApi
    public void verifyEnabled() {

        boolean enabled = isElementEnabled();
        if (!enabled) {
            throw new VerificationException(getElementDescription()
                                            + " is disabled while it is expected to be enabled",
                                            lastNotFoundException);
        }
    }

    /**
     * Verifies the element is disabled
     *
     * @throws VerificationException if the verification fails
     */
    @PublicAtsApi
    public void verifyDisabled() {

        boolean enabled = isElementEnabled();
        if (enabled) {
            throw new VerificationException(getElementDescription()
                                            + " is enabled while it is expected to be disabled",
                                            lastNotFoundException);
        }
    }

    /**
     * Verifies the element is editable
     *
     * @throws VerificationException if the verification fails
     */
    @PublicAtsApi
    public void verifyEditable() {

        boolean editable = isElementEditable();
        if (!editable) {
            throw new VerificationException(getElementDescription()
                                            + " is not editable while it is expected to be editable",
                                            lastNotFoundException);
        }
    }

    /**
     * Verifies the element is not editable
     *
     * @throws VerificationException if the verification fails
     */
    @PublicAtsApi
    public void verifyNotEditable() {

        boolean editable = isElementEditable();
        if (editable) {
            throw new VerificationException(getElementDescription()
                                            + " is editable while it is expected to be not editable",
                                            lastNotFoundException);
        }
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become visible
     *
     * @throws VerificationException if the element does not become visible
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeVisible() {

        int millis = UiEngineConfigurator.getInstance().getElementStateChangeDelay();
        long endTime = System.currentTimeMillis() + millis;
        do {
            if (isElementVisible()) {
                return;
            }
            UiEngineUtilities.sleep(SLEEP_PERIOD);
        } while (endTime - System.currentTimeMillis() > 0);

        throw new VerificationException("Failed to verify that element is visible within " + millis + " ms"
                                        + getElementDescription(), lastNotFoundException);
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become invisible
     *
     * @throws VerificationException if the element does not become invisible
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeNotVisible() {

        int millis = UiEngineConfigurator.getInstance().getElementStateChangeDelay();
        long endTime = System.currentTimeMillis() + millis;
        do {
            if (!isElementVisible()) {
                return;
            }
            UiEngineUtilities.sleep(SLEEP_PERIOD);
        } while (endTime - System.currentTimeMillis() > 0);

        throw new VerificationException("Failed to verify that element is invisible within " + millis
                                        + " ms" + getElementDescription(), lastNotFoundException);
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become enabled
     *
     * @throws VerificationException if the element does not become enabled
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeEnabled() {

        waitToBecomeEnabled(UiEngineConfigurator.getInstance().getElementStateChangeDelay());
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become disabled
     *
     * @throws VerificationException if the element does not become disabled
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeDisabled() {

        waitToBecomeDisabled(UiEngineConfigurator.getInstance().getElementStateChangeDelay());
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to become existing
     *
     * @throws VerificationException if the element does not become existing
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeExisting() {

        waitToBecomeExisting(UiEngineConfigurator.getInstance().getElementStateChangeDelay());
    }

    /**
     * Waits for a period of time the element to become existing
     *
     * @param millis milliseconds to wait
     * @throws VerificationException if the element does not become existing for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeExisting(
                                      int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if (isElementPresent()) {
                return;
            }
            UiEngineUtilities.sleep(SLEEP_PERIOD);
        } while (endTime - System.currentTimeMillis() > 0);

        throw new VerificationException("Failed to verify that element exists within " + millis + " ms"
                                        + getElementDescription(), lastNotFoundException);
    }

    /**
     * Waits for a period of time (check the 'elementStateChangeDelay' property) the element to became non-existing
     *
     * @throws VerificationException if the element does not become non-existing
     * for the default waiting period (check the 'elementStateChangeDelay' property)
     */
    @PublicAtsApi
    public void waitToBecomeNotExisting() {

        waitToBecomeNotExisting(UiEngineConfigurator.getInstance().getElementStateChangeDelay());
    }

    /**
     * Waits for a period of time the element to became non-existing
     *
     * @param millis milliseconds to wait
     * @throws VerificationException if the element does not become non-existing for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeNotExisting(
                                         int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if (!isElementPresent()) {
                return;
            }
            UiEngineUtilities.sleep(SLEEP_PERIOD);
        } while (endTime - System.currentTimeMillis() > 0);

        throw new VerificationException("Failed to verify the element is not existing within " + millis
                                        + " ms" + getElementDescription(), lastNotFoundException);
    }

    /**
     * Waits for a period of time the element to become enabled
     *
     * @param millis milliseconds to wait
     * @throws VerificationException if the element does not become enabled for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeEnabled(
                                     int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if (isElementEnabled()) {
                return;
            }
            UiEngineUtilities.sleep(SLEEP_PERIOD);
        } while (endTime - System.currentTimeMillis() > 0);

        throw new VerificationException("Failed to verify that element is enabled within " + millis + " ms"
                                        + getElementDescription(), lastNotFoundException);
    }

    /**
     * Waits for a period of time the element to become disabled
     *
     * @param millis milliseconds to wait
     * @throws VerificationException if the element does not become disabled for the specified period
     */
    @PublicAtsApi
    public void waitToBecomeDisabled(
                                      int millis ) {

        long endTime = System.currentTimeMillis() + millis;
        do {
            if (!isElementEnabled()) {
                return;
            }
            UiEngineUtilities.sleep(SLEEP_PERIOD);
        } while (endTime - System.currentTimeMillis() > 0);

        throw new VerificationException("Failed to verify that element is disabled within " + millis + " ms"
                                        + getElementDescription(), lastNotFoundException);
    }

    private String getElementDescription() {

        return " '" + (element != null
                                       ? element.toString()
                                       : "Element " + elementProperties.toString())
               + "'";
    }

    /**
     * Check if the element presents or not
     *
     * @return if the element presents or not
     */
    @PublicAtsApi
    public boolean isElementPresent() {

        try {

            SwingElementLocator.findFixture(element);
            return true;
        } catch (ElementNotFoundException nsee) {
            lastNotFoundException = nsee;
            return false;
        }
    }

    /**
     * Check if the element is visible or not
     *
     * @return if the element is visible or not
     */
    @PublicAtsApi
    public boolean isElementVisible() {

        try {

            return SwingElementLocator.findFixture(element).component().isVisible();
        } catch (ElementNotFoundException nsee) {
            lastNotFoundException = nsee;
            return false;
        }
    }

    /**
     * Check if the element is enabled or disabled
     *
     * @return if the element is enabled or disabled
     */
    @PublicAtsApi
    public boolean isElementEnabled() {

        try {

            return SwingElementLocator.findFixture(element).component().isEnabled();
        } catch (ElementNotFoundException nsee) {
            lastNotFoundException = nsee;
            return false;
        }
    }

    /**
     * Check if the element is editable or not
     *
     * @return if the element is editable or not
     */
    @PublicAtsApi
    public boolean isElementEditable() {

        try {

            Component component = SwingElementLocator.findFixture(element).component();
            if (component instanceof JTextComponent) {
                return ((JTextComponent) component).isEditable();
            } else if (component instanceof JComboBox) {
                return ((JComboBox) component).isEditable();
            } else if (component instanceof JTree) {
                return ((JTree) component).isEditable();
            }
            throw new NotSupportedOperationException("Component of type \"" + component.getClass().getName()
                                                     + "\" doesn't have 'editable' state!");
        } catch (ElementNotFoundException nsee) {
            lastNotFoundException = nsee;
            return false;
        }
    }
}
