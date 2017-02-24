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

import java.awt.Component;
import java.awt.Point;

import org.fest.swing.core.ComponentDragAndDrop;
import org.fest.swing.core.MouseClickInfo;
import org.fest.swing.fixture.ComponentFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * Provide generic access to AWT/Swing components<br />
 * Using this class is recommended only when other similar classes
 * in this package do not provide suitable methods.
 * <p>
 * Can be identified by:
 * <li>name and class
 * <li>label
 * <li>label and class
 * <li>text
 * <li>text and class
 * </p>
 */
@PublicAtsApi
public class SwingComponent extends UiElement {

    private static final String[] RULES = { "label,class",
            "name,text,class,visible",
            "name,text,class",
            "name,class,visible",
            "name,class",
            "text,class,visible",
            "text,class",
            "name,visible",
            "name",
            "text,visible",
            "text",
            "index"                    };

    static {
        SwingElementLocator.componentsMap.put( SwingComponent.class, Component.class );
    }

    public SwingComponent( UiDriver uiDriver,
                           UiElementProperties properties ) {

        super( uiDriver, properties );
        checkTypeAndRules( "Swing", RULES );
    }

    /**
     * Generic way to find and AWT/Swing component.<br />
     * Not recommended for wide use. It might be used in case when the component is very custom
     * and does not extend Swing component with similar behavior.
     *
     * @return instance of the found {@link Component}
     */
    @PublicAtsApi
    public Component getNativeComponent() {

        new SwingElementState( this ).waitToBecomeExisting();

        return SwingElementLocator.findFixture( this ).component();
    }

    /**
     * Simulates a user click over the GUI component.
     *
     */
    @PublicAtsApi
    public void click() {

        new SwingElementState( this ).waitToBecomeExisting();

        click( MouseClickInfo.leftButton() );
    }

    /**
     * Simulates a user double click over the GUI component.
     *
     */
    @PublicAtsApi
    public void doubleClick() {

        new SwingElementState( this ).waitToBecomeExisting();

        click( MouseClickInfo.leftButton().times( 2 ) );
    }

    /**
     * Simulates a user right click over the GUI component.
     *
     */
    @PublicAtsApi
    public void rightClick() {

        new SwingElementState( this ).waitToBecomeExisting();

        click( MouseClickInfo.rightButton() );
    }

    /**
     * Simulates a user middle click over the GUI component.
     *
     */
    @PublicAtsApi
    public void middleClick() {

        new SwingElementState( this ).waitToBecomeExisting();

        click( MouseClickInfo.middleButton() );
    }

    /**
     * Focusing the GUI component.
     *
     */
    @PublicAtsApi
    public void focus() {

        new SwingElementState( this ).waitToBecomeExisting();

        ComponentFixture<? extends Component> componentFixture = SwingElementLocator.findFixture( this );

        componentFixture.robot.focus( componentFixture.target );
    }

    /**
     * Simulates a user mouse action over the GUI component.
     *
     * @param mouseClickInfo mouse click information
     */
    private void click(
                        MouseClickInfo mouseClickInfo ) {

        ComponentFixture<? extends Component> componentFixture = SwingElementLocator.findFixture( this );

        componentFixture.robot.click( componentFixture.target,
                                      mouseClickInfo.button(),
                                      mouseClickInfo.times() );

    }

    /**
     * Simulates a user dragging of this component
     *
     */
    @PublicAtsApi
    public void drag() {

        new SwingElementState( this ).waitToBecomeExisting();

        ComponentFixture<? extends Component> componentFixture = SwingElementLocator.findFixture( this );

        ComponentDragAndDrop componentDragAndDrop = new ComponentDragAndDrop( componentFixture.robot );
        componentDragAndDrop.drag( componentFixture.target,
                                   getComponentCenterLocation( componentFixture.target ) );
    }

    /**
     * Simulates a user dropping to this component
     *
     */
    @PublicAtsApi
    public void drop() {

        new SwingElementState( this ).waitToBecomeExisting();

        ComponentFixture<? extends Component> componentFixture = SwingElementLocator.findFixture( this );

        ComponentDragAndDrop componentDragAndDrop = new ComponentDragAndDrop( componentFixture.robot );
        componentDragAndDrop.drop( componentFixture.target,
                                   getComponentCenterLocation( componentFixture.target ) );
    }

    /**
     * Get the component center location {@link Point}
     *
     * @param component the target component
     * @return center location {@link Point}
     */
    private Point getComponentCenterLocation(
                                              Component component ) {

        Point centerPoint = new Point();
        centerPoint.setLocation( component.getX() + component.getWidth() / 2,
                                 component.getY() + component.getHeight() / 2 );
        return centerPoint;
    }

}
