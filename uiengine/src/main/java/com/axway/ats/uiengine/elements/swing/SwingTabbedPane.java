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

import java.util.regex.Pattern;

import javax.swing.JTabbedPane;

import org.fest.swing.fixture.JTabbedPaneFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.ElementsMap;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.exceptions.UiElementException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing TabbedPane
 * <p>
 * Can be identified by:
 * <li>name
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingTabbedPane extends UiElement {

    private static final String[] RULES = { "label,visible", "label", "name,visible", "name", "index" };

    static {
        SwingElementLocator.componentsMap.put( SwingTabbedPane.class, JTabbedPane.class );
    }

    public SwingTabbedPane( UiDriver uiDriver,
                            UiElementProperties properties ) {

        super( uiDriver, properties );
        checkTypeAndRules( "Swing", RULES );
    }

    /**
     * Select tab by index
     *
     * @param index the tab index to select
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void selectTab(
                           int index ) {

        new SwingElementState( this ).waitToBecomeExisting();

        try {

            ( ( JTabbedPaneFixture ) SwingElementLocator.findFixture( this ) ).selectTab( index );
        } catch( Exception e ) {

            throw new UiElementException( e.getMessage(), this );
        }
    }

    /**
     * Select tab by title
     *
     * @param title the tab title to select
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void selectTab(
                           String title ) {

        new SwingElementState( this ).waitToBecomeExisting();

        try {

            ( ( JTabbedPaneFixture ) SwingElementLocator.findFixture( this ) ).selectTab( title );
        } catch( Exception e ) {

            throw new UiElementException( e.getMessage(), this );
        }
    }

    /**
     * Select tab by regular expression
     *
     * @param regex the tab title regular expression
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void selectTabByRegex(
                                  String regex ) {

        new SwingElementState( this ).waitToBecomeExisting();

        try {

            ( ( JTabbedPaneFixture ) SwingElementLocator.findFixture( this ) ).selectTab( Pattern.compile( regex ) );
        } catch( Exception e ) {

            throw new UiElementException( e.getMessage(), this );
        }
    }

    /**
     * Select tab by title using sub-element properties. The required property is 'title'
     *
     * @param elementProperties the sub-element {@link UiElementProperties} which contains property 'title'
     * @throws VerificationException if the table element doesn't exist
     */
    @PublicAtsApi
    public void selectTabUsingSubElement(
                                          UiElementProperties elementProperties ) {

        new SwingElementState( this ).waitToBecomeExisting();

        try {

            String tabTitle = elementProperties.getProperty( "title" );
            if( tabTitle == null ) {
                throw new UiElementException( "The Sub-Element doesn't have 'title' attribute, "
                                              + "which is required for tab selection.", this );
            }

            ( ( JTabbedPaneFixture ) SwingElementLocator.findFixture( this ) ).selectTab( tabTitle );
        } catch( Exception e ) {

            throw new UiElementException( e.getMessage(), this );
        }
    }

    /**
     * Select tab by title using SubElement described in the map file. The required property is 'title'
     *
     * @param mapId the sub-element mapID. This element must have property 'title'
     * @throws VerificationException if the table element doesn't exist
     */
    @PublicAtsApi
    public void selectTabUsingSubElement(
                                          String mapId ) {

        new SwingElementState( this ).waitToBecomeExisting();

        try {

            String elementMapId = properties.getInternalProperty( UiElementProperties.MAP_ID_INTERNAL_PARAM );
            if( elementMapId == null ) {
                throw new UiElementException( "The element must be in the MAP file", this );
            }
            UiElementProperties elProperties = ElementsMap.getInstance()
                                                          .getSubElementProperties( elementMapId, mapId );

            String tabTitle = elProperties.getProperty( "title" );
            if( tabTitle == null ) {
                throw new UiElementException( "The Sub-Element doesn't have 'title' attribute, "
                                              + "which is required for tab selection.", this );
            }

            ( ( JTabbedPaneFixture ) SwingElementLocator.findFixture( this ) ).selectTab( tabTitle );
        } catch( Exception e ) {

            throw new UiElementException( e.getMessage(), this );
        }
    }

    /**
     *
     * @return the number of tabs in the TabbedPane
     *
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public int getNumberOfTabs() {

        return getTabTitles().length;
    }

    /**
     *
     * @return the titles of all tabs in natural order
     *
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public String[] getTabTitles() {

        new SwingElementState( this ).waitToBecomeExisting();

        return ( ( JTabbedPaneFixture ) SwingElementLocator.findFixture( this ) ).tabTitles();
    }

}
