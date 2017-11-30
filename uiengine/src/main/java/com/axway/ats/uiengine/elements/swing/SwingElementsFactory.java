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

import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.AbstractElementsFactory;
import com.axway.ats.uiengine.elements.UiElementProperties;

public class SwingElementsFactory extends AbstractElementsFactory {

    private static SwingElementsFactory instance;

    private SwingElementsFactory() {

        super();
    }

    synchronized public static SwingElementsFactory getInstance() {

        if (instance == null) {
            instance = new SwingElementsFactory();
        }
        return instance;
    }

    public SwingButton getSwingButton(
                                       String mapId,
                                       UiDriver uiDriver ) {

        return getSwingButton(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingButton getSwingButton(
                                       UiElementProperties properties,
                                       UiDriver uiDriver ) {

        return new SwingButton(uiDriver, properties);
    }

    public SwingTextArea getSwingTextArea(
                                           String mapId,
                                           UiDriver uiDriver ) {

        return getSwingTextArea(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingTextArea getSwingTextArea(
                                           UiElementProperties properties,
                                           UiDriver uiDriver ) {

        return new SwingTextArea(uiDriver, properties);
    }

    public SwingMenuItem getSwingMenuItem(
                                           String mapId,
                                           UiDriver uiDriver ) {

        return getSwingMenuItem(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingMenuItem getSwingMenuItem(
                                           UiElementProperties properties,
                                           UiDriver uiDriver ) {

        return new SwingMenuItem(uiDriver, properties);
    }

    public SwingPopupMenu getSwingPopupMenu(
                                             String mapId,
                                             UiDriver uiDriver ) {

        return getSwingPopupMenu(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingPopupMenu getSwingPopupMenu(
                                             UiElementProperties properties,
                                             UiDriver uiDriver ) {

        return new SwingPopupMenu(uiDriver, properties);
    }

    public SwingTree getSwingTree(
                                   String mapId,
                                   UiDriver uiDriver ) {

        return getSwingTree(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingTree getSwingTree(
                                   UiElementProperties properties,
                                   UiDriver uiDriver ) {

        return new SwingTree(uiDriver, properties);
    }

    public SwingSingleSelectList getSwingSingleSelectList(
                                                           String mapId,
                                                           UiDriver uiDriver ) {

        return getSwingSingleSelectList(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingSingleSelectList getSwingSingleSelectList(
                                                           UiElementProperties properties,
                                                           UiDriver uiDriver ) {

        return new SwingSingleSelectList(uiDriver, properties);
    }

    public SwingComboBox getSwingComboBox(
                                           String mapId,
                                           UiDriver uiDriver ) {

        return getSwingComboBox(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingComboBox getSwingComboBox(
                                           UiElementProperties properties,
                                           UiDriver uiDriver ) {

        return new SwingComboBox(uiDriver, properties);
    }

    public SwingMultiSelectList getSwingMultiSelectList(
                                                         String mapId,
                                                         UiDriver uiDriver ) {

        return getSwingMultiSelectList(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingMultiSelectList getSwingMultiSelectList(
                                                         UiElementProperties properties,
                                                         UiDriver uiDriver ) {

        return new SwingMultiSelectList(uiDriver, properties);
    }

    public SwingCheckBox getSwingCheckBox(
                                           String mapId,
                                           UiDriver uiDriver ) {

        return getSwingCheckBox(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingCheckBox getSwingCheckBox(
                                           UiElementProperties properties,
                                           UiDriver uiDriver ) {

        return new SwingCheckBox(uiDriver, properties);
    }

    public SwingToggleButton getSwingToggleButton(
                                                   String mapId,
                                                   UiDriver uiDriver ) {

        return getSwingToggleButton(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingToggleButton getSwingToggleButton(
                                                   UiElementProperties properties,
                                                   UiDriver uiDriver ) {

        return new SwingToggleButton(uiDriver, properties);
    }

    public SwingRadioButton getSwingRadioButton(
                                                 String mapId,
                                                 UiDriver uiDriver ) {

        return getSwingRadioButton(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingRadioButton getSwingRadioButton(
                                                 UiElementProperties properties,
                                                 UiDriver uiDriver ) {

        return new SwingRadioButton(uiDriver, properties);
    }

    public SwingTable getSwingTable(
                                     String mapId,
                                     UiDriver uiDriver ) {

        return getSwingTable(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingTable getSwingTable(
                                     UiElementProperties properties,
                                     UiDriver uiDriver ) {

        return new SwingTable(uiDriver, properties);
    }

    public SwingLabel getSwingLabel(
                                     String mapId,
                                     UiDriver uiDriver ) {

        return getSwingLabel(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingLabel getSwingLabel(
                                     UiElementProperties properties,
                                     UiDriver uiDriver ) {

        return new SwingLabel(uiDriver, properties);
    }

    public SwingSpinner getSwingSpinner(
                                         String mapId,
                                         UiDriver uiDriver ) {

        return getSwingSpinner(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingSpinner getSwingSpinner(
                                         UiElementProperties properties,
                                         UiDriver uiDriver ) {

        return new SwingSpinner(uiDriver, properties);
    }

    public SwingTabbedPane getSwingTabbedPane(
                                               String mapId,
                                               UiDriver uiDriver ) {

        return getSwingTabbedPane(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingTabbedPane getSwingTabbedPane(
                                               UiElementProperties properties,
                                               UiDriver uiDriver ) {

        return new SwingTabbedPane(uiDriver, properties);
    }

    public SwingComponent getSwingComponent(
                                             String mapId,
                                             UiDriver uiDriver ) {

        return getSwingComponent(elementsMap.getElementProperties(mapId), uiDriver);
    }

    public SwingComponent getSwingComponent(
                                             UiElementProperties properties,
                                             UiDriver uiDriver ) {

        return new SwingComponent(uiDriver, properties);
    }

    public SwingOptionPane getSwingOptionPane(
                                               UiDriver uiDriver ) {

        return new SwingOptionPane(uiDriver, null);
    }

    public SwingFileBrowse getSwingFileBrowse(
                                               UiDriver uiDriver ) {

        return new SwingFileBrowse(uiDriver, null);
    }

}
