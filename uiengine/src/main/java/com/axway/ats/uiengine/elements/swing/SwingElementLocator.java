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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fest.swing.core.BasicComponentFinder;
import org.fest.swing.core.BasicRobot;
import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.exception.WaitTimedOutError;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.ComponentFixture;
import org.fest.swing.fixture.ContainerFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JCheckBoxFixture;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JLabelFixture;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JMenuItemFixture;
import org.fest.swing.fixture.JPopupMenuFixture;
import org.fest.swing.fixture.JRadioButtonFixture;
import org.fest.swing.fixture.JSpinnerFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.fest.swing.fixture.JToggleButtonFixture;
import org.fest.swing.fixture.JTreeFixture;
import org.fest.swing.fixture.WindowFixture;
import org.fest.swing.format.Formatting;
import org.fest.swing.timing.Timeout;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.ElementNotFoundException;
import com.axway.ats.uiengine.exceptions.MoreThanOneSuchElementException;
import com.axway.ats.uiengine.internal.driver.SwingDriverInternal;

public class SwingElementLocator {

    private static final Logger                                               log           = LogManager.getLogger(SwingElementLocator.class);

    public static Map<Class<? extends UiElement>, Class<? extends Component>> componentsMap = new HashMap<Class<? extends UiElement>, Class<? extends Component>>();

    public static ComponentFixture<? extends Component> findFixture(
                                                                     UiElement uiElement ) {

        SwingDriverInternal driver = (SwingDriverInternal) uiElement.getUiDriver();
        ContainerFixture<?> containerFixture = (ContainerFixture<?>) driver.getActiveContainerFixture();

        Class<? extends Component> componentClass = componentsMap.get(uiElement.getClass());

        try {

            if (componentClass.equals(JButton.class)) {

                return (ComponentFixture<? extends Component>) new JButtonFixture(containerFixture.robot,
                                                                                  (JButton) findElement(uiElement));
            } else if (componentClass.equals(JTextComponent.class)) {

                return (ComponentFixture<? extends Component>) new JTextComponentFixture(containerFixture.robot,
                                                                                         (JTextComponent) findElement(uiElement));
            } else if (componentClass.equals(JMenuItem.class)) {

                if (uiElement.getElementProperty("path") != null) {

                    return containerFixture.menuItemWithPath(uiElement.getElementProperty("path")
                                                                      .split("[\\,\\/]+"));
                } else {

                    return (ComponentFixture<? extends Component>) new JMenuItemFixture(containerFixture.robot,
                                                                                        (JMenuItem) findElement(uiElement));
                }
            } else if (componentClass.equals(JPopupMenu.class)) {

                return (ComponentFixture<? extends Component>) new JPopupMenuFixture(containerFixture.robot,
                                                                                     (JPopupMenu) findElement(uiElement));
            } else if (componentClass.equals(JTree.class)) {

                return (ComponentFixture<? extends Component>) new JTreeFixture(containerFixture.robot,
                                                                                (JTree) findElement(uiElement));
            } else if (componentClass.equals(JList.class)) {

                return (ComponentFixture<? extends Component>) new JListFixture(containerFixture.robot,
                                                                                (JList) findElement(uiElement));
            } else if (componentClass.equals(JCheckBox.class)) {

                return (ComponentFixture<? extends Component>) new JCheckBoxFixture(containerFixture.robot,
                                                                                    (JCheckBox) findElement(uiElement));
            } else if (componentClass.equals(JToggleButton.class)) {

                return (ComponentFixture<? extends Component>) new JToggleButtonFixture(containerFixture.robot,
                                                                                        (JToggleButton) findElement(uiElement));
            } else if (componentClass.equals(JComboBox.class)) {

                return (ComponentFixture<? extends Component>) new JComboBoxFixture(containerFixture.robot,
                                                                                    (JComboBox) findElement(uiElement));
            } else if (componentClass.equals(JRadioButton.class)) {

                return (ComponentFixture<? extends Component>) new JRadioButtonFixture(containerFixture.robot,
                                                                                       (JRadioButton) findElement(uiElement));
            } else if (componentClass.equals(JTable.class)) {

                return (ComponentFixture<? extends Component>) new JTableFixture(containerFixture.robot,
                                                                                 (JTable) findElement(uiElement));
            } else if (componentClass.equals(JSpinner.class)) {

                return (ComponentFixture<? extends Component>) new JSpinnerFixture(containerFixture.robot,
                                                                                   (JSpinner) findElement(uiElement));
            } else if (componentClass.equals(JTabbedPane.class)) {

                return (ComponentFixture<? extends Component>) new JTabbedPaneFixture(containerFixture.robot,
                                                                                      (JTabbedPane) findElement(uiElement));
            } else if (componentClass.equals(JOptionPane.class)) {

                return (ComponentFixture<? extends Component>) containerFixture.optionPane();
            } else if (componentClass.equals(JLabel.class)) {

                return (ComponentFixture<? extends Component>) new JLabelFixture(containerFixture.robot,
                                                                                 (JLabel) findElement(uiElement));
            } else if (componentClass.equals(Component.class)) {

                return new ComponentFixture<Component>(containerFixture.robot, findElement(uiElement)) {};
            } else if (componentClass.equals(JFileChooser.class)) {

                // TODO - might be searched by name too
                return containerFixture.fileChooser(Timeout.timeout(UiEngineConfigurator.getInstance()
                                                                                        .getElementStateChangeDelay()));
            } else {

                throw new ElementNotFoundException(uiElement.toString() + " not found. No such Fixture");
            }

        } catch (ComponentLookupException cle) {
            throw new ElementNotFoundException(uiElement.toString() + " not found.", cle);
        } catch (WaitTimedOutError exc) { // thrown for OptionPane search, wait for Window (BasicRobot.waitForWindow), AbstractJTableCellWriter, JTreeDriver.waitForChildrenToShowUp, each Pause wait
            throw new ElementNotFoundException(uiElement.toString() + " not found.", exc);
        }
    }

    @SuppressWarnings( "unchecked")
    private static <T extends Component> T findElement(
                                                        UiElement uiElement ) {

        SwingDriverInternal driver = (SwingDriverInternal) uiElement.getUiDriver();
        ContainerFixture<? extends Container> containerFixture = driver.getActiveContainerFixture();

        Class<? extends Component> componentClass;
        String exactClassName = uiElement.getElementProperties().getProperty("class");
        if (exactClassName != null) {

            try {
                componentClass = (Class<? extends Component>) SwingElementLocator.class.getClassLoader()
                                                                                       .loadClass(exactClassName);
            } catch (ClassNotFoundException ex) {
                throw new ElementNotFoundException("Could not load UI Component class named "
                                                   + exactClassName
                                                   + ". Probably it is not in the classpath or the name is invalid. Cause message: "
                                                   + ex.getMessage(),
                                                   ex);
            }
        } else {

            componentClass = componentsMap.get(uiElement.getClass());
        }

        try {

            boolean requireVisible = true;
            if (uiElement.getElementProperty("visible") != null) {

                requireVisible = Boolean.parseBoolean(uiElement.getElementProperty("visible").trim());
            }

            // Finding components by their associated labels ( someJLabel.setLabelFor( someComponent ) )
            if (uiElement.getElementProperty("label") != null) {

                return (T) containerFixture.robot.finder()
                                                 .findByLabel(containerFixture.component(),
                                                              uiElement.getElementProperty("label"),
                                                              componentClass,
                                                              requireVisible);
            }
            return (T) SwingElementFinder.find(containerFixture.robot,
                                               containerFixture.component(),
                                               buildLocator(componentClass,
                                                            uiElement.getElementProperties(),
                                                            requireVisible,
                                                            uiElement.getPropertyNamesToUseForMatch()));
        } catch (ComponentLookupException cle) {

            if (cle.getMessage().startsWith("Found more than one ")) {

                throw new MoreThanOneSuchElementException(cle.getMessage() + "\n" + uiElement.toString(),
                                                          cle);
            }
            Window win = driver.getWindowFixture().component();
            Container currentContainer = containerFixture.component();

            String winTitle;
            if (win instanceof Dialog) {
                winTitle = "'" + ((Dialog) win).getTitle() + "'";
            } else if (win instanceof Frame) {
                winTitle = "'" + ((Frame) win).getTitle() + "'";
            } else {
                winTitle = "N/A";
            }
            String containerMsg = null;
            if (win.equals(currentContainer)) {
                containerMsg = "[same as window]";
            }
            if (log.isDebugEnabled()) {
                // more verbose trace
                throw new ElementNotFoundException(uiElement.toString() + " not found.\n"
                                                   + "Current container: "
                                                   + (containerMsg != null
                                                                           ? containerMsg
                                                                           : currentContainer.toString())
                                                   + "\n" + "Current window: window title " + winTitle
                                                   + "( details: " + win.toString() + ")", cle);
            } else {
                // /light message
                throw new ElementNotFoundException(uiElement.toString() + " not found.\n"
                                                   + "Current container name: "
                                                   + (containerMsg != null
                                                                           ? containerMsg
                                                                           : currentContainer.getName())
                                                   + "\n" + "Current window: window title " + winTitle, cle);
            }
        }
    }

    private static <T extends Component> GenericTypeMatcher<T> buildLocator(
                                                                             Class<T> componentClass,
                                                                             final UiElementProperties properties,
                                                                             boolean requireVisible,
                                                                             final String[] propertyNamesToUseForMatch ) {

        // nested class not to be anonymous in stack traces
        class MyGenericTypeMatcher<Type1 extends Component> extends GenericTypeMatcher<Type1> {

            private int currentIndex = 0;

            public MyGenericTypeMatcher( Class<Type1> componentClass,
                                         boolean requireVisible ) {

                super(componentClass, requireVisible);
            }

            /**
             * In addition to the type check in constructor adds check by other component properties
             * @param component other component for comparison
             * @return
             */
            @Override
            protected boolean isMatching(
                                          Component component ) {

                // here we are sure that we do not search by label and
                // also property "visible" and class are tracked additionally by FEST

                // Having several properties means match ALL of them
                int propertiesMatching = 0;

                int currentPropIdxToProcess = 0;
                for (currentPropIdxToProcess = 0; currentPropIdxToProcess < propertyNamesToUseForMatch.length; currentPropIdxToProcess++) {
                    String keyName = propertyNamesToUseForMatch[currentPropIdxToProcess];
                    if ("visible".equals(keyName) || "class".equals(keyName)) { // already considered as parameter in search
                        propertiesMatching++;
                        continue;
                    }

                    String propertyValue = properties.getProperty(keyName);
                    if (propertyValue != null) {

                        if ("name".equals(keyName)) {

                            if (propertyValue.equals(component.getName())) {
                                propertiesMatching++;
                                log.debug("Found element with 'name' property: " + component);
                                continue;
                            } else {
                                return false;
                            }
                        } else if ("text".equals(keyName)) {

                            // Search by specific component properties
                            if (component instanceof JButton) {
                                JButton button = (JButton) component;
                                if (propertyValue.equals(button.getText())) {
                                    propertiesMatching++;
                                    log.debug("Found element by 'text' property: " + button);
                                    continue;
                                } else {
                                    return false;
                                }
                            } else if (component instanceof JMenuItem) {
                                JMenuItem menuItem = (JMenuItem) component;
                                if (propertyValue.equals(menuItem.getText())) {
                                    log.debug("Found element by 'text' property: " + menuItem);
                                    propertiesMatching++;
                                    continue;
                                } else {
                                    return false;
                                }
                            } else if (component instanceof JPopupMenu) {
                                JPopupMenu popupMenu = (JPopupMenu) component;
                                if (propertyValue.equals(popupMenu.getLabel())) {
                                    log.debug("Found element by 'text' property: " + popupMenu);
                                    propertiesMatching++;
                                    continue;
                                } else {
                                    return false;
                                }
                            } else if (component instanceof JToggleButton) {

                                JToggleButton toggleButton = (JToggleButton) component;
                                if (propertyValue.equals(toggleButton.getText())) {
                                    log.debug("Found element by 'text' property: " + toggleButton);
                                    propertiesMatching++;
                                    continue;
                                } else {
                                    return false;
                                }
                            } else if (component instanceof JTextComponent) {

                                JTextComponent textComponent = (JTextComponent) component;
                                if (propertyValue.equals(textComponent.getText())) {

                                    log.debug("Found element by 'text' property: " + textComponent);
                                    propertiesMatching++;
                                    continue;
                                } else {
                                    return false;
                                }
                            } else if (component instanceof JLabel) {

                                JLabel label = (JLabel) component;
                                if (propertyValue.equals(label.getText())) {

                                    log.debug("Found element by 'text' property: " + label);
                                    propertiesMatching++;
                                    continue;
                                }
                            }
                            // Attempt to search for 'text' for unsupported component type
                            return false;
                        } else if ("tooltip".equals(keyName)) {

                            // Search by specific component properties
                            if (component instanceof JButton) {
                                JButton button = (JButton) component;
                                if (propertyValue.equals(button.getToolTipText())) {
                                    propertiesMatching++;
                                    log.debug("Found element by 'tooltip' property: " + button);
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                        } else if ("index".equals(keyName)) {

                            if (Integer.parseInt(propertyValue) == currentIndex++) {

                                propertiesMatching++;
                                continue;
                            }
                        } else {

                            throw new IllegalStateException("Attempt to search for not supported property: "
                                                            + keyName + ", component: " + component);
                        }
                    } // if property != null

                }
                if (propertyNamesToUseForMatch.length == propertiesMatching) {
                    return true;
                } else {
                    if (propertiesMatching > 0 && log.isDebugEnabled()) {

                        log.debug("Not all properties matched. Only " + propertiesMatching + " instead of "
                                  + properties.getPropertiesSize());
                    }
                    return false;
                }
            }

        }

        return new MyGenericTypeMatcher<T>(componentClass, requireVisible);
    }

    /**
     * Change container by specified name or title.
     * For internal use
     * @param driver Swing driver
     * @param containerProperties property with name inside
     * @return the {@link ContainerFixture}
     */
    public static ContainerFixture<?> getContainerFixture(
                                                           SwingDriverInternal driver,
                                                           UiElementProperties containerProperties ) {

        String containerName = containerProperties.getProperty("name");
        final String containerTitle = containerProperties.getProperty("title");

        if (StringUtils.isNullOrEmpty(containerName) && StringUtils.isNullOrEmpty(containerTitle)) {
            throw new IllegalArgumentException("Illegal name/title (empty/null string) passed to search for container");
        }

        ContainerFixture<?> containerFixture = driver.getActiveContainerFixture();
        ContainerFixture<?> windowsFixture = driver.getWindowFixture();
        Robot robot;
        if (containerFixture != null) {
            // use the current robot instance
            robot = containerFixture.robot;
        } else {
            robot = BasicRobot.robotWithCurrentAwtHierarchy();
        }

        if (!StringUtils.isNullOrEmpty(containerName)) {

            try {
                Container cont = BasicComponentFinder.finderWithCurrentAwtHierarchy()
                                                     .findByName(windowsFixture.component(),
                                                                 containerName,
                                                                 Container.class);
                return new ContainerFixture<Container>(robot, cont) {};
            } catch (WaitTimedOutError wtoe) {

                throw new ElementNotFoundException("Unable to find container with name '"
                                                   + containerName
                                                   + "' under current window/dialog. If needed change current window first with swingEngineInstance.setActiveWindow()");
            }
        } else {

            try {
                Container cont = BasicComponentFinder.finderWithCurrentAwtHierarchy()
                                                     .find(windowsFixture.component(),
                                                           new GenericTypeMatcher<Container>(Container.class,
                                                                                             true) {
                                                               @Override
                                                               protected boolean isMatching(
                                                                                             Container component ) {

                                                                   if (component instanceof Dialog) {
                                                                       return ((Dialog) component).getTitle()
                                                                                                  .equals(containerTitle);
                                                                   } else if (component instanceof Frame) {
                                                                       return ((Frame) component).getTitle()
                                                                                                 .equals(containerTitle);
                                                                   } else if (component instanceof JInternalFrame) {
                                                                       return ((JInternalFrame) component).getTitle()
                                                                                                          .equals(containerTitle);
                                                                   }
                                                                   return false;
                                                               }
                                                           });
                return new ContainerFixture<Container>(robot, cont) {};
            } catch (WaitTimedOutError wtoe) {

                throw new ElementNotFoundException("Unable to find container with title '"
                                                   + containerName
                                                   + "' under current window/dialog. If needed change current window first with swingEngineInstance.setActiveWindow()");
            }
        }
    }

    /**
     * Change window by specified name.
     * For internal use
     * @param driver Swing driver
     * @param windowTitle if null look for any visible window
     * @param isDialog should the search be for dialog windows
     * @return the {@link ContainerFixture}
     */
    public static WindowFixture<?> getWindowFixture(
                                                     SwingDriverInternal driver,
                                                     final String windowTitle,
                                                     boolean isDialog ) throws ElementNotFoundException {

        WindowFixture<?> windowFixture = driver.getWindowFixture();
        Robot robot;
        if (windowFixture != null) {
            // use the current robot instance
            robot = windowFixture.robot;
        } else {
            robot = BasicRobot.robotWithCurrentAwtHierarchy();
        }

        try {
            if (windowTitle != null) {
                if (isDialog) {
                    windowFixture = WindowFinder.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class) {
                        protected boolean isMatching(
                                                      Dialog dialog ) {

                            return windowTitle.equals(dialog.getTitle()) && dialog.isShowing();
                        }
                    })
                                                .withTimeout(UiEngineConfigurator.getInstance()
                                                                                 .getElementStateChangeDelay())
                                                .using(robot);
                } else {
                    windowFixture = WindowFinder.findFrame(new GenericTypeMatcher<Frame>(Frame.class) {
                        protected boolean isMatching(
                                                      Frame frame ) {

                            return windowTitle.equals(frame.getTitle()) && frame.isShowing();
                        }
                    })
                                                .withTimeout(UiEngineConfigurator.getInstance()
                                                                                 .getElementStateChangeDelay())
                                                .using(robot);
                }
            } else {
                if (isDialog) {
                    windowFixture = WindowFinder.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class) {
                        protected boolean isMatching(
                                                      Dialog dialog ) {

                            return dialog.isShowing();
                        }
                    })
                                                .withTimeout(UiEngineConfigurator.getInstance()
                                                                                 .getElementStateChangeDelay())
                                                .using(robot);
                } else {
                    windowFixture = WindowFinder.findFrame(new GenericTypeMatcher<Frame>(Frame.class) {
                        protected boolean isMatching(
                                                      Frame frame ) {

                            if (log.isTraceEnabled()) {
                                log.trace("WindowFinder isMatching(): Title: " + frame.getTitle()
                                          + ", Frame + " + frame + ", Owner: " + frame.getOwner());
                            }
                            return frame.isShowing() && frame.getOwner() == null; // owner == null - top frame. Two independent frames are both considered a top ones
                        }
                    })
                                                .withTimeout(UiEngineConfigurator.getInstance()
                                                                                 .getElementStateChangeDelay())
                                                .using(robot);
                }
            }
            return windowFixture;
        } catch (WaitTimedOutError wtoe) {
            throw new ElementNotFoundException("Unable to find "
                                               + (isDialog
                                                           ? "dialog"
                                                           : "frame")
                                               + (windowTitle != null
                                                                      ? " with title '"
                                                                        + windowTitle + "'"
                                                                      : " without title specified (null passed)"),
                                               wtoe);
        }
    }

    /**
     *
     * @param driver Swing driver
     * @return the component hierarchy as {@link String}
     */
    public static String getComponentHierarchy(
                                                SwingDriverInternal driver ) {

        ContainerFixture<?> containerFixture = driver.getActiveContainerFixture();
        Robot robot;
        if (containerFixture != null) {
            // use the current robot instance
            robot = containerFixture.robot;
        } else {
            robot = BasicRobot.robotWithCurrentAwtHierarchy();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        robot.printer().printComponents(new PrintStream(outputStream), ( (containerFixture != null)
                                                                                                    ? containerFixture.component()
                                                                                                    : null));

        return outputStream.toString();
    }

    /**
     * Log the clicked element (its type, properties and index in the component hierarchy from the current container)
     *
     * @param driver {@link SwingDriverInternal} instance
     */
    public static void useComponentInspector(
                                              final SwingDriverInternal driver ) {

        // long eventMask = AWTEvent.MOUSE_MOTION_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK;
        long eventMask = AWTEvent.MOUSE_EVENT_MASK;
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

            public void eventDispatched(
                                         AWTEvent e ) {

                if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getSource() instanceof Component) {

                    Component component = (Component) e.getSource();
                    Class<?> swingClass = findSwingClass(component.getClass());
                    if (swingClass == null) {
                        swingClass = component.getClass();
                        log.warn("Can't find swing class of type \"" + swingClass.getName() + "\"");
                    }
                    String logEntry = "\t[INSPECTOR] "
                                      + getProperties(component).replaceFirst("\\[", " [") + " index="
                                      + calculateIndex(component, swingClass);
                    if (!component.getClass().getName().equals(swingClass.getName())) {
                        logEntry += "\t(extends " + swingClass.getName() + ")";
                    }
                    log.info(logEntry);
                }
            }

            private String getProperties(
                                          Component c ) {

                String properties = Formatting.inEdtFormat(c);
                if (c instanceof JButton) {
                    String tooltip = ((JButton) c).getToolTipText();
                    if (!StringUtils.isNullOrEmpty(tooltip)) {
                        int lastBrIndex = properties.lastIndexOf(']');
                        if (lastBrIndex > 0) {
                            properties = properties.substring(0, lastBrIndex) + ", tooltip='" + tooltip
                                         + "'" + properties.substring(lastBrIndex);
                        } else {
                            return c.getClass().getName() + " [tooltip='" + tooltip + "']";
                        }
                    }
                }

                return properties;
            }

            private Class<?> findSwingClass(
                                             Class<?> clazz ) {

                if (clazz == null) {
                    return null;
                }
                if (clazz.getName().startsWith("javax.swing")) {
                    return clazz;
                }
                return findSwingClass(clazz.getSuperclass());
            }

            @SuppressWarnings( "unchecked")
            private int calculateIndex(
                                        final Component component,
                                        Class<?> swingClass ) {

                ContainerFixture<?> containerFixture = driver.getActiveContainerFixture();
                Robot robot;
                if (containerFixture != null) {
                    // use the current robot instance
                    robot = containerFixture.robot;
                } else {
                    robot = BasicRobot.robotWithCurrentAwtHierarchy();
                }

                List<Component> found = SwingElementFinder.find(robot.hierarchy(),
                                                                ( (containerFixture != null)
                                                                                             ? containerFixture.component()
                                                                                             : null),
                                                                new GenericTypeMatcher<Component>((Class<Component>) swingClass,
                                                                                                  true) {

                                                                    @Override
                                                                    protected boolean isMatching(
                                                                                                  Component c ) {

                                                                        return true;
                                                                    }
                                                                });
                return found.indexOf(component);
            }

        },
                                                        eventMask);
    }

}
