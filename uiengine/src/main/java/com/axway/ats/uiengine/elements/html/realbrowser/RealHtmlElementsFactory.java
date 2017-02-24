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
package com.axway.ats.uiengine.elements.html.realbrowser;

import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.AbstractElementsFactory;
import com.axway.ats.uiengine.elements.UiElementProperties;

/**
 * A factory for HTML elements
 */
public class RealHtmlElementsFactory extends AbstractElementsFactory {

    private static RealHtmlElementsFactory instance;

    private RealHtmlElementsFactory() {

        super();
    }

    synchronized public static RealHtmlElementsFactory getInstance() {

        if( instance == null ) {
            instance = new RealHtmlElementsFactory();
        }
        return instance;
    }

    public RealHtmlButton getHtmlButton(
                                         String mapId,
                                         UiDriver uiDriver ) {

        return getHtmlButton( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlButton getHtmlButton(
                                         UiElementProperties properties,
                                         UiDriver uiDriver ) {

        return new RealHtmlButton( uiDriver, properties );
    }

    public RealHtmlTextBox getHtmlTextBox(
                                           String mapId,
                                           UiDriver uiDriver ) {

        return getHtmlTextBox( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlTextBox getHtmlTextBox(
                                           UiElementProperties properties,
                                           UiDriver uiDriver ) {

        return new RealHtmlTextBox( uiDriver, properties );
    }

    public RealHtmlTextArea getHtmlTextArea(
                                             String mapId,
                                             UiDriver uiDriver ) {

        return getHtmlTextArea( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlTextArea getHtmlTextArea(
                                             UiElementProperties properties,
                                             UiDriver uiDriver ) {

        return new RealHtmlTextArea( uiDriver, properties );
    }

    public RealHtmlCheckBox getHtmlCheckBox(
                                             String mapId,
                                             UiDriver uiDriver ) {

        return getHtmlCheckBox( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlCheckBox getHtmlCheckBox(
                                             UiElementProperties properties,
                                             UiDriver uiDriver ) {

        return new RealHtmlCheckBox( uiDriver, properties );
    }

    public RealHtmlLink getHtmlLink(
                                     String mapId,
                                     UiDriver uiDriver ) {

        return getHtmlLink( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlLink getHtmlLink(
                                     UiElementProperties properties,
                                     UiDriver uiDriver ) {

        return new RealHtmlLink( uiDriver, properties );
    }

    public RealHtmlSingleSelectList getHtmlSingleSelectList(
                                                             String mapId,
                                                             UiDriver uiDriver ) {

        return getHtmlSingleSelectList( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlSingleSelectList getHtmlSingleSelectList(
                                                             UiElementProperties properties,
                                                             UiDriver uiDriver ) {

        return new RealHtmlSingleSelectList( uiDriver, properties );
    }

    public RealHtmlMultiSelectList getHtmlMultiSelectList(
                                                           String mapId,
                                                           UiDriver uiDriver ) {

        return getHtmlMultiSelectList( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlMultiSelectList getHtmlMultiSelectList(
                                                           UiElementProperties properties,
                                                           UiDriver uiDriver ) {

        return new RealHtmlMultiSelectList( uiDriver, properties );
    }

    public RealHtmlRadioList getHtmlRadioList(
                                               String mapId,
                                               UiDriver uiDriver ) {

        return getHtmlRadioList( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlRadioList getHtmlRadioList(
                                               UiElementProperties properties,
                                               UiDriver uiDriver ) {

        return new RealHtmlRadioList( uiDriver, properties );
    }

    public RealHtmlFileBrowse getHtmlFileBrowse(
                                                 String mapId,
                                                 UiDriver uiDriver ) {

        return getHtmlFileBrowse( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlFileBrowse getHtmlFileBrowse(
                                                 UiElementProperties properties,
                                                 UiDriver uiDriver ) {

        return new RealHtmlFileBrowse( uiDriver, properties );
    }

    public RealHtmlTable getHtmlTable(
                                       String mapId,
                                       UiDriver uiDriver ) {

        return getHtmlTable( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlTable getHtmlTable(
                                       UiElementProperties properties,
                                       UiDriver uiDriver ) {

        return new RealHtmlTable( uiDriver, properties );
    }

    public RealHtmlAlert getHtmlAlert(
                                       UiDriver uiDriver ) {

        return new RealHtmlAlert( uiDriver );
    }

    public RealHtmlPrompt getHtmlPrompt(
                                         UiDriver uiDriver ) {

        return new RealHtmlPrompt( uiDriver );
    }

    public RealHtmlConfirm getHtmlConfirm(
                                           UiDriver uiDriver ) {

        return new RealHtmlConfirm( uiDriver );
    }

    public RealHtmlElement getHtmlElement(
                                           String mapId,
                                           UiDriver uiDriver ) {

        return getHtmlElement( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public RealHtmlElement getHtmlElement(
                                           UiElementProperties properties,
                                           UiDriver uiDriver ) {

        return new RealHtmlElement( uiDriver, properties );
    }

}
