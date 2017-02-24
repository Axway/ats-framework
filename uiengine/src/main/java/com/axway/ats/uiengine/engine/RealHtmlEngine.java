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

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.AbstractRealBrowserDriver;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlAlert;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlButton;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlCheckBox;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlConfirm;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlElement;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlElementsFactory;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlFileBrowse;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlLink;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlMultiSelectList;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlPrompt;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlRadioList;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlSingleSelectList;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlTable;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlTextArea;
import com.axway.ats.uiengine.elements.html.realbrowser.RealHtmlTextBox;
import com.axway.ats.uiengine.internal.engine.IHtmlEngine;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * Engine operating over HTML application 
 */
@PublicAtsApi
public class RealHtmlEngine extends AbstractHtmlEngine implements IHtmlEngine {

    private RealHtmlElementsFactory htmlElementsFactory;

    public RealHtmlEngine( AbstractRealBrowserDriver realBrowserDriver ) {

        super( realBrowserDriver, RealHtmlElementsFactory.getInstance() );
        this.htmlElementsFactory = ( RealHtmlElementsFactory ) elementsFactory;
    }

    /**
     * @param uiElement the element to work with
     * @return a utility class for checking the state of an HTML element
     */
    @PublicAtsApi
    public RealHtmlElementState getUtilsElementState( UiElement uiElement ) {

        return new RealHtmlElementState( uiElement );
    }

    @Override
    @PublicAtsApi
    public RealHtmlButton getButton( String mapId ) {

        return htmlElementsFactory.getHtmlButton( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlButton getButton( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlButton( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlTextBox getTextBox( String mapId ) {

        return htmlElementsFactory.getHtmlTextBox( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlTextBox getTextBox( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlTextBox( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlTextArea getTextArea( String mapId ) {

        return htmlElementsFactory.getHtmlTextArea( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlTextArea getTextArea( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlTextArea( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlCheckBox getCheckBox( String mapId ) {

        return htmlElementsFactory.getHtmlCheckBox( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlCheckBox getCheckBox( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlCheckBox( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlLink getLink( String mapId ) {

        return htmlElementsFactory.getHtmlLink( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlLink getLink( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlLink( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlSingleSelectList getSingleSelectList( String mapId ) {

        return htmlElementsFactory.getHtmlSingleSelectList( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlSingleSelectList getSingleSelectList( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlSingleSelectList( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlMultiSelectList getMultiSelectList( String mapId ) {

        return htmlElementsFactory.getHtmlMultiSelectList( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlMultiSelectList getMultiSelectList( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlMultiSelectList( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlRadioList getRadioList( String mapId ) {

        return htmlElementsFactory.getHtmlRadioList( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlRadioList getRadioList( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlRadioList( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlFileBrowse getFileBrowse( String mapId ) {

        return htmlElementsFactory.getHtmlFileBrowse( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlFileBrowse getFileBrowse( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlFileBrowse( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlTable getTable( String mapId ) {

        return htmlElementsFactory.getHtmlTable( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlTable getTable( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlTable( properties, uiDriver );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PublicAtsApi
    public RealHtmlElement getElement( String mapId ) {

        return htmlElementsFactory.getHtmlElement( mapId, uiDriver );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PublicAtsApi
    public RealHtmlElement getElement( UiElementProperties properties ) {

        return htmlElementsFactory.getHtmlElement( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlAlert expectAlert() {

        return htmlElementsFactory.getHtmlAlert( uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlPrompt expectPrompt() {

        return htmlElementsFactory.getHtmlPrompt( uiDriver );
    }

    @Override
    @PublicAtsApi
    public RealHtmlConfirm expectConfirm() {

        return htmlElementsFactory.getHtmlConfirm( uiDriver );
    }

}
