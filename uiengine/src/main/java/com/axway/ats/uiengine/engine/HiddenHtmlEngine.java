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
import com.axway.ats.uiengine.HiddenBrowserDriver;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlAlert;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlButton;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlCheckBox;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlConfirm;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlElement;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlElementsFactory;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlFileBrowse;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlLink;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlMultiSelectList;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlPrompt;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlRadioList;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlSingleSelectList;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlTable;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlTextArea;
import com.axway.ats.uiengine.elements.html.hiddenbrowser.HiddenHtmlTextBox;
import com.axway.ats.uiengine.internal.engine.IHtmlEngine;
import com.axway.ats.uiengine.utilities.hiddenbrowser.HiddenHtmlElementState;

@PublicAtsApi
public class HiddenHtmlEngine extends AbstractHtmlEngine implements IHtmlEngine {

    private HiddenHtmlElementsFactory hiddenHtmlElementsFactory;

    public HiddenHtmlEngine( HiddenBrowserDriver hiddenBrowserDriver ) {

        super( hiddenBrowserDriver, HiddenHtmlElementsFactory.getInstance() );
        this.hiddenHtmlElementsFactory = ( HiddenHtmlElementsFactory ) elementsFactory;
    }

    /**
     * @param uiElement the element to work with
     * @return a utility class for checking the state of element
     */
    @Override
    @PublicAtsApi
    public HiddenHtmlElementState getUtilsElementState(
                                                        UiElement uiElement ) {

        return new HiddenHtmlElementState( uiElement );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlButton getButton(
                                       String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlButton( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlButton getButton(
                                       UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlButton( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlTextBox getTextBox(
                                         String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlTextBox( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlTextBox getTextBox(
                                         UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlTextBox( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlCheckBox getCheckBox(
                                           String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlCheckBox( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlCheckBox getCheckBox(
                                           UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlCheckBox( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlSingleSelectList getSingleSelectList(
                                                           String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenSingleSelectList( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlSingleSelectList getSingleSelectList(
                                                           UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenSingleSelectList( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlFileBrowse getFileBrowse(
                                               String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenFileBrowse( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlFileBrowse getFileBrowse(
                                               UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenFileBrowse( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlTable getTable(
                                     String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenTable( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlTable getTable(
                                     UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenTable( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlLink getLink(
                                   String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenLink( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlLink getLink(
                                   UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenLink( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlMultiSelectList getMultiSelectList(
                                                         String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenMultiSelectList( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlMultiSelectList getMultiSelectList(
                                                         UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenMultiSelectList( properties, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlRadioList getRadioList(
                                             String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenRadioList( mapId, uiDriver );
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlRadioList getRadioList(
                                             UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenRadioList( properties, uiDriver );
    }

    @PublicAtsApi
    public HiddenHtmlTextArea getTextArea(
                                           String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenTextArea( mapId, uiDriver );
    }

    @PublicAtsApi
    public HiddenHtmlTextArea getTextArea(
                                           UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenTextArea( properties, uiDriver );
    }

    @PublicAtsApi
    public HiddenHtmlElement getElement(
                                         String mapId ) {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlElement( mapId, uiDriver );
    }

    @PublicAtsApi
    public HiddenHtmlElement getElement(
                                         UiElementProperties properties ) {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlElement( properties, uiDriver );
    }

    @PublicAtsApi
    public HiddenHtmlAlert expectAlert() {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlAlert( uiDriver );
    }

    @PublicAtsApi
    public HiddenHtmlPrompt expectPrompt() {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlPrompt( uiDriver );
    }

    @PublicAtsApi
    public HiddenHtmlConfirm expectConfirm() {

        return this.hiddenHtmlElementsFactory.getHiddenHtmlConfirm( uiDriver );
    }

}
