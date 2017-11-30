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

import java.awt.MenuItem;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.fixture.JMenuItemFixture;
import org.fest.swing.fixture.JPopupMenuFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing PopupMenu
 * <p>
 * Can be identified by:
 * <li>name
 * <li>text
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingPopupMenu extends UiElement {

    private static final String[] RULES = { "label,visible", "label", "name,visible", "name", "index" };

    static {
        SwingElementLocator.componentsMap.put(SwingPopupMenu.class, JPopupMenu.class);
    }

    public SwingPopupMenu( UiDriver uiDriver,
                           UiElementProperties properties ) {

        super(uiDriver, properties);
        checkTypeAndRules("Swing", RULES);
    }

    /**
     * Click pop-up element
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void click() {

        new SwingElementState(this).waitToBecomeExisting();

        ((JPopupMenuFixture) SwingElementLocator.findFixture(this)).click();
    }

    /**
     * Click {@link MenuItem} pop-up element by its 'name' attribute value
     *
     * @param menuItemName {@link MenuItem} name attribute value
     */
    @PublicAtsApi
    public void clickMenuItemByName(
                                     String menuItemName ) {

        new SwingElementState(this).waitToBecomeExisting();

        ((JPopupMenuFixture) SwingElementLocator.findFixture(this)).menuItem(menuItemName).click();
    }

    /**
     * Click {@link MenuItem} pop-up element by text (actually it's the 'label' attribute value)
     *
     * @param menuItemText {@link MenuItem} text/label
     */
    @PublicAtsApi
    public void clickMenuItemByText(
                                     String... menuItemText ) {

        new SwingElementState(this).waitToBecomeExisting();

        ((JPopupMenuFixture) SwingElementLocator.findFixture(this)).menuItemWithPath(menuItemText)
                                                                   .click();
    }

    /**
     * Getting menu labels/texts
     *
     * @return an array with the menu labels/texts
     */
    @PublicAtsApi
    public String[] getMenuLabels() {

        new SwingElementState(this).waitToBecomeExisting();

        return ((JPopupMenuFixture) SwingElementLocator.findFixture(this)).menuLabels();
    }

    /**
     * Getting only visible menu labels/texts
     *
     * @return an array with the visible menu labels/texts only
     */
    @PublicAtsApi
    public String[] getVisibleMenuLabels() {

        new SwingElementState(this).waitToBecomeExisting();

        JPopupMenuFixture popupMenuFixture = (JPopupMenuFixture) SwingElementLocator.findFixture(this);
        String[] labels = popupMenuFixture.menuLabels();

        List<String> visibleLabels = new ArrayList<String>(labels.length);
        for (final String label : labels) {

            JMenuItemFixture menuItemFixture = popupMenuFixture.menuItem(new GenericTypeMatcher<JMenuItem>(JMenuItem.class,
                                                                                                           false) {

                @Override
                protected boolean isMatching(
                                              JMenuItem menuItem ) {

                    String text = menuItem.getText();
                    if (text != null && text.equals(label)) {
                        return true;
                    }
                    return false;
                }

            });

            if (menuItemFixture != null && menuItemFixture.component().isVisible()) {
                visibleLabels.add(label);
            }
        }

        return visibleLabels.toArray(new String[0]);
    }
}
