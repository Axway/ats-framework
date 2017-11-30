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

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.swing.edt.GuiActionRunner.execute;
import static org.fest.swing.format.Formatting.format;
import static org.fest.util.Strings.concat;
import static org.fest.util.Systems.LINE_SEPARATOR;

import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JMenuItem;

import org.fest.swing.core.BasicComponentFinder;
import org.fest.swing.core.ComponentMatcher;
import org.fest.swing.core.ComponentPrinter;
import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.fest.swing.driver.JMenuItemMatcher;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.fixture.JMenuItemFixture;
import org.fest.swing.hierarchy.ComponentHierarchy;
import org.fest.swing.hierarchy.SingleComponentHierarchy;

/**
 * This class is based on {@link BasicComponentFinder},
 * the only difference is that we are not using {@link LinkedHashSet} for the founded components,
 * but {@link ArrayList} and every time the component index is the same (in the component hierarchy tree),
 * and we are able to search by index.
 *
 */
public class SwingElementFinder {

    public static <T extends Component> T find(
                                                Robot robot,
                                                Container root,
                                                GenericTypeMatcher<T> m ) {

        ComponentHierarchy hierarchy = robot.hierarchy();
        List<Component> found = null;
        if (root == null) {
            found = find(hierarchy, m);
        } else {
            found = find(new SingleComponentHierarchy(root, hierarchy), m);
        }
        if (found.isEmpty()) {
            throw componentNotFound(robot, hierarchy, m);
        }
        if (found.size() > 1) {
            throw multipleComponentsFound(found, m);
        }
        Component component = found.iterator().next();
        return m.supportedType().cast(component);
    }

    public static List<Component> find(
                                        ComponentHierarchy hierarchy,
                                        Container root,
                                        GenericTypeMatcher<Component> m ) {

        List<Component> found = null;
        if (root == null) {
            found = find(hierarchy, m);
        } else {
            found = find(new SingleComponentHierarchy(root, hierarchy), m);
        }
        return found;
    }

    public static JMenuItemFixture menuItemWithPath(
                                                     Robot robot,
                                                     Container root,
                                                     String... path ) {

        ComponentMatcher m = new JMenuItemMatcher(path);
        Component item = robot.finder().find(root, m);
        assertThat(item).as(format(item)).isInstanceOf(JMenuItem.class);

        return new JMenuItemFixture(robot, (JMenuItem) item);
    }

    private static ComponentLookupException componentNotFound(
                                                               Robot robot,
                                                               ComponentHierarchy h,
                                                               ComponentMatcher m ) {

        String message = concat("Unable to find component using matcher ", m, ".");
        message = concat(message,
                         LINE_SEPARATOR,
                         LINE_SEPARATOR,
                         "Component hierarchy:",
                         LINE_SEPARATOR,
                         formattedHierarchy(robot.printer(), root(h)));
        throw new ComponentLookupException(message);
    }

    private static ComponentLookupException multipleComponentsFound(
                                                                     Collection<Component> found,
                                                                     ComponentMatcher m ) {

        StringBuilder message = new StringBuilder();
        message.append("Found more than one component using matcher ")
               .append(m)
               .append(".")
               .append(LINE_SEPARATOR)
               .append(LINE_SEPARATOR)
               .append("Found:");
        appendComponents(message, found);
        if (!found.isEmpty()) {
            message.append(LINE_SEPARATOR);
        }
        throw new ComponentLookupException(message.toString(), found);
    }

    private static void appendComponents(
                                          final StringBuilder message,
                                          final Collection<Component> found ) {

        execute(new GuiTask() {
            protected void executeInEDT() {

                for (Component c : found)
                    message.append(LINE_SEPARATOR).append(format(c));
            }
        });
    }

    private static String formattedHierarchy(
                                              ComponentPrinter printer,
                                              Container root ) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out, true);
        printer.printComponents(printStream, root);
        printStream.flush();

        return new String(out.toByteArray());
    }

    private static Container root(
                                   ComponentHierarchy h ) {

        if (h instanceof SingleComponentHierarchy) {
            return ((SingleComponentHierarchy) h).root();
        }
        return null;
    }

    private static List<Component> find(
                                         ComponentHierarchy h,
                                         ComponentMatcher m ) {

        List<Component> found = new ArrayList<Component>();
        for (Object o : rootsOf(h)) {
            find(h, m, (Component) o, found);
        }
        return found;
    }

    private static void find(
                              ComponentHierarchy h,
                              ComponentMatcher m,
                              Component root,
                              List<Component> found ) {

        for (Component c : childrenOfComponent(root, h)) {
            find(h, m, c, found);
        }
        if (isMatching(root, m) && !found.contains(root)) {
            found.add(root);
        }
    }

    private static boolean isMatching(
                                       final Component c,
                                       final ComponentMatcher m ) {

        return execute(new GuiQuery<Boolean>() {
            protected Boolean executeInEDT() {

                return m.matches(c);
            }
        });
    }

    private static Collection<? extends Component> rootsOf(
                                                            final ComponentHierarchy h ) {

        return execute(new GuiQuery<Collection<? extends Component>>() {
            protected Collection<? extends Component> executeInEDT() {

                return h.roots();
            }
        });
    }

    private static Collection<Component> childrenOfComponent(
                                                              final Component c,
                                                              final ComponentHierarchy h ) {

        return execute(new GuiQuery<Collection<Component>>() {
            protected Collection<Component> executeInEDT() {

                return h.childrenOf(c);
            }
        });
    }
}
