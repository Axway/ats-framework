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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;

import org.fest.swing.fixture.JPopupMenuFixture;
import org.fest.swing.fixture.JTreeFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiTree;
import com.axway.ats.uiengine.exceptions.UiElementException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing Tree
 * <p>
 * Can be identified by:
 * <li>name
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingTree extends UiTree {

    private static final String[] RULES = { "label,visible", "label", "name,visible", "name", "index" };

    static {
        SwingElementLocator.componentsMap.put(SwingTree.class, JTree.class);
    }

    public SwingTree( UiDriver uiDriver,
                      UiElementProperties properties ) {

        super(uiDriver, properties);
        checkTypeAndRules("Swing", RULES);
    }

    /**
     * Select tree element by index
     *
     * @param rowIndex tree element index to select
     * @throws VerificationException if the tree element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void select(
                        int rowIndex ) {

        new SwingElementState(this).waitToBecomeExisting();

        ((JTreeFixture) SwingElementLocator.findFixture(this)).selectRow(rowIndex);
    }

    /**
     * Select tree elements by labels
     *
     * @param labels the tree element labels
     * @throws VerificationException if the tree element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void select(
                        String... labels ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTreeFixture treeFixture = (JTreeFixture) SwingElementLocator.findFixture(this);
        expandTree(treeFixture, labels); // sometimes it is necessary
        treeFixture.selectPath(buildPath(labels));
    }

    @PublicAtsApi
    public void expand(
                        String... labels ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTreeFixture treeFixture = (JTreeFixture) SwingElementLocator.findFixture(this);
        expandTree(treeFixture, labels);
    }

    @PublicAtsApi
    public void expand(
                        int rowIndex ) {

        new SwingElementState(this).waitToBecomeExisting();

        ((JTreeFixture) SwingElementLocator.findFixture(this)).expandRow(rowIndex);
    }

    @PublicAtsApi
    public void click(
                       String... labels ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTreeFixture treeFixture = (JTreeFixture) SwingElementLocator.findFixture(this);
        expandTree(treeFixture, labels); // sometimes it is necessary
        treeFixture.clickPath(buildPath(labels));
    }

    @PublicAtsApi
    public void click(
                       int rowIndex ) {

        new SwingElementState(this).waitToBecomeExisting();

        ((JTreeFixture) SwingElementLocator.findFixture(this)).clickRow(rowIndex);
    }

    @PublicAtsApi
    public void rightClick(
                            int rowIndex,
                            String... contextMenuItems ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTreeFixture treeFixture = (JTreeFixture) SwingElementLocator.findFixture(this);
        JPopupMenuFixture popUpMenu = treeFixture.showPopupMenuAt(rowIndex);
        popUpMenu.menuItemWithPath(contextMenuItems).click();
    }

    @PublicAtsApi
    public void rightClick(
                            String[] path,
                            String... contextMenuItems ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTreeFixture treeFixture = (JTreeFixture) SwingElementLocator.findFixture(this);
        expandTree(treeFixture, path); // sometimes it is necessary
        JPopupMenuFixture popUpMenu = treeFixture.showPopupMenuAt(buildPath(path));
        popUpMenu.menuItemWithPath(contextMenuItems).click();
    }

    private void expandTree(
                             JTreeFixture treeFixture,
                             String... treeItems ) {

        List<String> path = new ArrayList<String>();
        try {
            for (String treeItem : treeItems) {

                path.add(treeItem);
                treeFixture.expandPath(buildPath(path.toArray(new String[0])));
            }
        } catch (Exception e) {

            throw new UiElementException(e.getMessage(), this);
        }
    }

    private String buildPath(
                              String... nodes ) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.length; i++) {
            sb.append(nodes[i]);
            if (i < nodes.length - 1) {
                sb.append("/");
            }
        }
        return sb.toString();
    }

}
