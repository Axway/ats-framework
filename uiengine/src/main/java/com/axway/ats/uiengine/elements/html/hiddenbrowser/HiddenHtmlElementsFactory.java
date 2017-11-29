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
package com.axway.ats.uiengine.elements.html.hiddenbrowser;

import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.AbstractElementsFactory;
import com.axway.ats.uiengine.elements.UiElementProperties;

public class HiddenHtmlElementsFactory extends AbstractElementsFactory {

    private static HiddenHtmlElementsFactory instance;

    private HiddenHtmlElementsFactory() {

        super();
    }

    synchronized public static HiddenHtmlElementsFactory getInstance() {

        if( instance == null ) {
            instance = new HiddenHtmlElementsFactory();
        }
        return instance;
    }

    public HiddenHtmlButton getHiddenHtmlButton(
                                                 String mapId,
                                                 UiDriver uiDriver ) {

        return getHiddenHtmlButton( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlButton getHiddenHtmlButton(
                                                 UiElementProperties properties,
                                                 UiDriver uiDriver ) {

        return new HiddenHtmlButton( uiDriver, properties );
    }

    public HiddenHtmlTextBox getHiddenHtmlTextBox(
                                                   String mapId,
                                                   UiDriver uiDriver ) {

        return getHiddenHtmlTextBox( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlTextBox getHiddenHtmlTextBox(
                                                   UiElementProperties properties,
                                                   UiDriver uiDriver ) {

        return new HiddenHtmlTextBox( uiDriver, properties );
    }

    public HiddenHtmlCheckBox getHiddenHtmlCheckBox(
                                                     String mapId,
                                                     UiDriver uiDriver ) {

        return getHiddenHtmlCheckBox( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlCheckBox getHiddenHtmlCheckBox(
                                                     UiElementProperties properties,
                                                     UiDriver uiDriver ) {

        return new HiddenHtmlCheckBox( uiDriver, properties );
    }

    public HiddenHtmlSingleSelectList getHiddenSingleSelectList(
                                                                 String mapId,
                                                                 UiDriver uiDriver ) {

        return getHiddenSingleSelectList( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlSingleSelectList getHiddenSingleSelectList(
                                                                 UiElementProperties properties,
                                                                 UiDriver uiDriver ) {

        return new HiddenHtmlSingleSelectList( uiDriver, properties );
    }

    public HiddenHtmlFileBrowse getHiddenFileBrowse(
                                                     String mapId,
                                                     UiDriver uiDriver ) {

        return getHiddenFileBrowse( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlFileBrowse getHiddenFileBrowse(
                                                     UiElementProperties properties,
                                                     UiDriver uiDriver ) {

        return new HiddenHtmlFileBrowse( uiDriver, properties );
    }

    public HiddenHtmlTable getHiddenTable(
                                           String mapId,
                                           UiDriver uiDriver ) {

        return getHiddenTable( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlTable getHiddenTable(
                                           UiElementProperties properties,
                                           UiDriver uiDriver ) {

        return new HiddenHtmlTable( uiDriver, properties );
    }

    public HiddenHtmlLink getHiddenLink(
                                         String mapId,
                                         UiDriver uiDriver ) {

        return getHiddenLink( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlLink getHiddenLink(
                                         UiElementProperties properties,
                                         UiDriver uiDriver ) {

        return new HiddenHtmlLink( uiDriver, properties );
    }

    public HiddenHtmlMultiSelectList getHiddenMultiSelectList(
                                                               String mapId,
                                                               UiDriver uiDriver ) {

        return getHiddenMultiSelectList( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlMultiSelectList getHiddenMultiSelectList(
                                                               UiElementProperties properties,
                                                               UiDriver uiDriver ) {

        return new HiddenHtmlMultiSelectList( uiDriver, properties );
    }

    public HiddenHtmlRadioList getHiddenRadioList(
                                                   String mapId,
                                                   UiDriver uiDriver ) {

        return getHiddenRadioList( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlRadioList getHiddenRadioList(
                                                   UiElementProperties properties,
                                                   UiDriver uiDriver ) {

        return new HiddenHtmlRadioList( uiDriver, properties );
    }

    public HiddenHtmlTextArea getHiddenTextArea(
                                                 String mapId,
                                                 UiDriver uiDriver ) {

        return getHiddenTextArea( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlTextArea getHiddenTextArea(
                                                 UiElementProperties properties,
                                                 UiDriver uiDriver ) {

        return new HiddenHtmlTextArea( uiDriver, properties );
    }

    public HiddenHtmlElement getHiddenHtmlElement(
                                                   String mapId,
                                                   UiDriver uiDriver ) {

        return getHiddenHtmlElement( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public HiddenHtmlElement getHiddenHtmlElement(
                                                   UiElementProperties properties,
                                                   UiDriver uiDriver ) {

        return new HiddenHtmlElement( uiDriver, properties );
    }

    public HiddenHtmlAlert getHiddenHtmlAlert(
                                               UiDriver uiDriver ) {

        return new HiddenHtmlAlert( uiDriver );
    }

    public HiddenHtmlPrompt getHiddenHtmlPrompt(
                                                 UiDriver uiDriver ) {

        return new HiddenHtmlPrompt( uiDriver );
    }

    public HiddenHtmlConfirm getHiddenHtmlConfirm(
                                                   UiDriver uiDriver ) {

        return new HiddenHtmlConfirm( uiDriver );
    }

}
