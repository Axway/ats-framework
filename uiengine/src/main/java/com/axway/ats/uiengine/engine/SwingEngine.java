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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fest.swing.fixture.ContainerFixture;
import org.fest.swing.fixture.WindowFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.ElementsMap;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.swing.SwingButton;
import com.axway.ats.uiengine.elements.swing.SwingCheckBox;
import com.axway.ats.uiengine.elements.swing.SwingComboBox;
import com.axway.ats.uiengine.elements.swing.SwingComponent;
import com.axway.ats.uiengine.elements.swing.SwingElementLocator;
import com.axway.ats.uiengine.elements.swing.SwingElementsFactory;
import com.axway.ats.uiengine.elements.swing.SwingFileBrowse;
import com.axway.ats.uiengine.elements.swing.SwingLabel;
import com.axway.ats.uiengine.elements.swing.SwingMenuItem;
import com.axway.ats.uiengine.elements.swing.SwingMultiSelectList;
import com.axway.ats.uiengine.elements.swing.SwingOptionPane;
import com.axway.ats.uiengine.elements.swing.SwingPopupMenu;
import com.axway.ats.uiengine.elements.swing.SwingRadioButton;
import com.axway.ats.uiengine.elements.swing.SwingSingleSelectList;
import com.axway.ats.uiengine.elements.swing.SwingSpinner;
import com.axway.ats.uiengine.elements.swing.SwingTabbedPane;
import com.axway.ats.uiengine.elements.swing.SwingTable;
import com.axway.ats.uiengine.elements.swing.SwingTextArea;
import com.axway.ats.uiengine.elements.swing.SwingToggleButton;
import com.axway.ats.uiengine.elements.swing.SwingTree;
import com.axway.ats.uiengine.exceptions.ElementNotFoundException;
import com.axway.ats.uiengine.internal.driver.SwingDriverInternal;
import com.axway.ats.uiengine.internal.engine.AbstractEngine;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

@PublicAtsApi
public class SwingEngine extends AbstractEngine {

    private static final Logger  log = LogManager.getLogger(SwingEngine.class);
    private SwingElementsFactory swingElementsFactory;

    public SwingEngine( UiDriver uiDriver ) {

        super(uiDriver, SwingElementsFactory.getInstance());
        this.swingElementsFactory = (SwingElementsFactory) elementsFactory;
    }

    /**
     *
     * @param uiElement the element to work with
     * @return a utility class for checking the state of element
     */
    @PublicAtsApi
    public SwingElementState getUtilsElementState(
                                                   UiElement uiElement ) {

        return new SwingElementState(uiElement);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingButton} instance
     */
    @PublicAtsApi
    public SwingButton getButton(
                                  String mapId ) {

        return this.swingElementsFactory.getSwingButton(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingButton} instance
     */
    @PublicAtsApi
    public SwingButton getButton(
                                  UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingButton(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingMenuItem} instance
     */
    @PublicAtsApi
    public SwingMenuItem getMenuItem(
                                      String mapId ) {

        return this.swingElementsFactory.getSwingMenuItem(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingMenuItem} instance
     */
    @PublicAtsApi
    public SwingMenuItem getMenuItem(
                                      UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingMenuItem(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingPopupMenu} instance
     */
    @PublicAtsApi
    public SwingPopupMenu getPopupMenu(
                                        String mapId ) {

        return this.swingElementsFactory.getSwingPopupMenu(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingPopupMenu} instance
     */
    @PublicAtsApi
    public SwingPopupMenu getPopupMenu(
                                        UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingPopupMenu(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingTextArea} instance
     */
    @PublicAtsApi
    public SwingTextArea getTextArea(
                                      String mapId ) {

        return this.swingElementsFactory.getSwingTextArea(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingTextArea} instance
     */
    @PublicAtsApi
    public SwingTextArea getTextArea(
                                      UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingTextArea(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingTree} instance
     */
    @PublicAtsApi
    public SwingTree getTree(
                              String mapId ) {

        return this.swingElementsFactory.getSwingTree(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingTree} instance
     */
    @PublicAtsApi
    public SwingTree getTree(
                              UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingTree(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingSingleSelectList} instance
     */
    @PublicAtsApi
    public SwingSingleSelectList getSingleSelectList(
                                                      String mapId ) {

        return this.swingElementsFactory.getSwingSingleSelectList(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingSingleSelectList} instance
     */
    @PublicAtsApi
    public SwingSingleSelectList getSingleSelectList(
                                                      UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingSingleSelectList(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingMultiSelectList} instance
     */
    @PublicAtsApi
    public SwingMultiSelectList getMultiSelectList(
                                                    String mapId ) {

        return this.swingElementsFactory.getSwingMultiSelectList(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingMultiSelectList} instance
     */
    @PublicAtsApi
    public SwingMultiSelectList getMultiSelectList(
                                                    UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingMultiSelectList(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingComboBox} instance
     */
    @PublicAtsApi
    public SwingComboBox getComboBox(
                                      String mapId ) {

        return this.swingElementsFactory.getSwingComboBox(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingComboBox} instance
     */
    @PublicAtsApi
    public SwingComboBox getComboBox(
                                      UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingComboBox(properties, uiDriver);
    }

    /**
     *
     * @param mapId the element's map id
     * @return a new {@link SwingComponent} instance
     */
    @PublicAtsApi
    public SwingComponent getComponent(
                                        String mapId ) {

        return this.swingElementsFactory.getSwingComponent(mapId, uiDriver);
    }

    /**
     *
     * @param properties properties describing this element
     * @return a new {@link SwingComponent} instance
     */
    @PublicAtsApi
    public SwingComponent getComponent(
                                        UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingComponent(properties, uiDriver);
    }

    /**
    *
    * @return a new {@link SwingOptionPane} instance
    */
    @PublicAtsApi
    public SwingOptionPane getOptionPane() {

        return this.swingElementsFactory.getSwingOptionPane(uiDriver);
    }

    /**
    *
    * @return a new {@link SwingFileBrowse} instance
    */
    @PublicAtsApi
    public SwingFileBrowse getFileBrowse() {

        return this.swingElementsFactory.getSwingFileBrowse(uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingCheckBox} instance
     */
    @PublicAtsApi
    public SwingCheckBox getCheckBox(
                                      String mapId ) {

        return this.swingElementsFactory.getSwingCheckBox(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingCheckBox} instance
     */
    @PublicAtsApi
    public SwingCheckBox getCheckBox(
                                      UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingCheckBox(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingToggleButton} instance
     */
    @PublicAtsApi
    public SwingToggleButton getToggleButton(
                                              String mapId ) {

        return this.swingElementsFactory.getSwingToggleButton(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingToggleButton} instance
     */
    @PublicAtsApi
    public SwingToggleButton getToggleButton(
                                              UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingToggleButton(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingRadioButton} instance
     */
    @PublicAtsApi
    public SwingRadioButton getRadioButton(
                                            String mapId ) {

        return this.swingElementsFactory.getSwingRadioButton(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingRadioButton} instance
     */
    @PublicAtsApi
    public SwingRadioButton getRadioButton(
                                            UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingRadioButton(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingTable} instance
     */
    @PublicAtsApi
    public SwingTable getTable(
                                String mapId ) {

        return this.swingElementsFactory.getSwingTable(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingTable} instance
     */
    @PublicAtsApi
    public SwingTable getTable(
                                UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingTable(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingLabel} instance
     */
    @PublicAtsApi
    public SwingLabel getLabel(
                                String mapId ) {

        return this.swingElementsFactory.getSwingLabel(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingLabel} instance
     */
    @PublicAtsApi
    public SwingLabel getLabel(
                                UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingLabel(properties, uiDriver);
    }

    /**
     * @param mapId the element's map id
     * @return a new {@link SwingSpinner} instance
     */
    @PublicAtsApi
    public SwingSpinner getSpinner(
                                    String mapId ) {

        return this.swingElementsFactory.getSwingSpinner(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingSpinner} instance
     */
    @PublicAtsApi
    public SwingSpinner getSpinner(
                                    UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingSpinner(properties, uiDriver);
    }

    /**
     *
     * @param mapId the element's map id
     * @return a new {@link SwingTabbedPane} instance
     */
    @PublicAtsApi
    public SwingTabbedPane getTabbedPane(
                                          String mapId ) {

        return this.swingElementsFactory.getSwingTabbedPane(mapId, uiDriver);
    }

    /**
     * @param properties properties describing this element
     * @return a new {@link SwingTabbedPane} instance
     */
    @PublicAtsApi
    public SwingTabbedPane getTabbedPane(
                                          UiElementProperties properties ) {

        return this.swingElementsFactory.getSwingTabbedPane(properties, uiDriver);
    }

    /**
     * Set current window to work with.
     * @throws ElementNotFoundException if the frame/dialog is not found or several
     * are found with such title
     */
    @PublicAtsApi
    public void goToMainWindow() {

        goToWindow( ((SwingDriverInternal) uiDriver).getMainWindowTitle(), false);
    }

    /**
     * Set current window to work with. Window can be frame or dialog.
     * @param windowTitle the window title
     * @param isDialog if the window is dialog or not( i.e. frame)
     * @throws ElementNotFoundException if the frame/dialog is not found
     */
    @PublicAtsApi
    public void goToWindow(
                            final String windowTitle,
                            boolean isDialog ) throws ElementNotFoundException {

        WindowFixture<?> newWindowFixture = SwingElementLocator.getWindowFixture((SwingDriverInternal) uiDriver,
                                                                                 windowTitle,
                                                                                 isDialog);
        // there is about 10sec. delay on focus set. Added traces to investigate on other machines
        if (!isDialog) {
            log.debug("Windows fixture found. About to get focus on it ...");
            newWindowFixture.focus();
            log.debug("  ... window focus changed");
        }
        ((SwingDriverInternal) uiDriver).setWindowFixture(newWindowFixture); // changes active container too
    }

    /**
     * Set current window to work with. Window can be frame or dialog.
     * @param windowTitle the window title
     * @param isDialog if the window is dialog or not( i.e. frame)
     * @param timeout maximum time to wait for the window to appear. Measured in milliseconds
     * @throws ElementNotFoundException if the frame/dialog is not found
     */
    @PublicAtsApi
    public void goToWindow(
                            final String windowTitle,
                            boolean isDialog,
                            int timeout ) throws ElementNotFoundException {

        // in getWindowFixture() method we are using ElementStateChangeDelay as a timeout, so now we will change this value
        int stateChangeDelay = UiEngineConfigurator.getInstance().getElementStateChangeDelay();
        UiEngineConfigurator.getInstance().setElementStateChangeDelay(timeout);
        try {
            goToWindow(windowTitle, isDialog);
        } finally {
            UiEngineConfigurator.getInstance().setElementStateChangeDelay(stateChangeDelay);
        }
    }

    /**
     * Set active container under the <strong>current</strong> window
     *
     * @param containerProperties {@link UiElementProperties} which contains the container 'name' or 'title' property
     * @throws ElementNotFoundException if the container element is not found
     */
    @PublicAtsApi
    public void goToContainer(
                               UiElementProperties containerProperties ) {

        ContainerFixture<?> newContainerFixture = SwingElementLocator.getContainerFixture((SwingDriverInternal) uiDriver,
                                                                                          containerProperties);
        ((SwingDriverInternal) uiDriver).setActiveContainerFixture(newContainerFixture);
    }

    /**
     * Set active container under the <strong>current</strong> window
     *
     * @param containerMapId container element mapId. The element requires 'name' or 'title' property.
     * @throws ElementNotFoundException if the container element is not found
     */
    @PublicAtsApi
    public void goToContainer(
                               String containerMapId ) {

        UiElementProperties containerProperties = ElementsMap.getInstance()
                                                             .getElementProperties(containerMapId);
        goToContainer(containerProperties);
    }

    /**
     * Sets active container to the current window, i.e. goes to top of current window.
     */
    @PublicAtsApi
    public void goToMainContainer() {

        WindowFixture<?> winFixture = ((SwingDriverInternal) uiDriver).getWindowFixture();
        ((SwingDriverInternal) uiDriver).setActiveContainerFixture(winFixture);
    }

    /**
     *
     * @return current container component hierarchy as {@link String}
     */
    @PublicAtsApi
    public String getComponentHierarchy() {

        return SwingElementLocator.getComponentHierarchy((SwingDriverInternal) uiDriver);
    }

    /**
     * Whether to use component inspector tool,
     * which logs the selected component type, its available properties and calculates its index
     * in the component hierarchy tree in the scope of the current container.<br>
     * The tool works with mouse click action only.
     *
     */
    @PublicAtsApi
    public void useComponentInspector() {

        SwingElementLocator.useComponentInspector((SwingDriverInternal) uiDriver);
    }

}
