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
package com.axway.ats.uiengine.internal.engine;

import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlAlert;
import com.axway.ats.uiengine.elements.html.HtmlButton;
import com.axway.ats.uiengine.elements.html.HtmlCheckBox;
import com.axway.ats.uiengine.elements.html.HtmlConfirm;
import com.axway.ats.uiengine.elements.html.HtmlElement;
import com.axway.ats.uiengine.elements.html.HtmlFileBrowse;
import com.axway.ats.uiengine.elements.html.HtmlLink;
import com.axway.ats.uiengine.elements.html.HtmlMultiSelectList;
import com.axway.ats.uiengine.elements.html.HtmlPrompt;
import com.axway.ats.uiengine.elements.html.HtmlRadioList;
import com.axway.ats.uiengine.elements.html.HtmlSingleSelectList;
import com.axway.ats.uiengine.elements.html.HtmlTable;
import com.axway.ats.uiengine.elements.html.HtmlTextArea;
import com.axway.ats.uiengine.elements.html.HtmlTextBox;
import com.axway.ats.uiengine.utilities.IHtmlElementState;

public interface IHtmlEngine {

    /**
     * @param uiElement the element to work with
     * @return a utility class for checking the state of an HTML element
     */
    public IHtmlElementState getUtilsElementState(
                                                   UiElement uiElement );

    /**
     * @param mapId the element's map id
     * @return a new HTML Text Box instance
     */
    public HtmlTextBox getTextBox(
                                   String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML Text Box instance
     */
    public HtmlTextBox getTextBox(
                                   UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML Text Area instance
     */
    public HtmlTextArea getTextArea(
                                     String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML Text Area instance
     */
    public HtmlTextArea getTextArea(
                                     UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML Check Box instance
     */
    public HtmlCheckBox getCheckBox(
                                     String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML Check Box instance
     */
    public HtmlCheckBox getCheckBox(
                                     UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML Button instance
     */
    public HtmlButton getButton(
                                 String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML Button instance
     */
    public HtmlButton getButton(
                                 UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML Link instance
     */
    public HtmlLink getLink(
                             String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML Link instance
     */
    public HtmlLink getLink(
                             UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML Single Select List instance
     */
    public HtmlSingleSelectList getSingleSelectList(
                                                     String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML Single Select List instance
     */
    public HtmlSingleSelectList getSingleSelectList(
                                                     UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML Multi Select List instance
     */
    public HtmlMultiSelectList getMultiSelectList(
                                                   String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML Multi Select List instance
     */
    public HtmlMultiSelectList getMultiSelectList(
                                                   UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML Radio List instance
     */
    public HtmlRadioList getRadioList(
                                       String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML Radio List instance
     */
    public HtmlRadioList getRadioList(
                                       UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML File Browse element
     */
    public HtmlFileBrowse getFileBrowse(
                                         String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML File Browse element
     */
    public HtmlFileBrowse getFileBrowse(
                                         UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML Table instance
     */
    public HtmlTable getTable(
                               String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML Table instance
     */
    public HtmlTable getTable(
                               UiElementProperties properties );

    /**
     * @param mapId the element's map id
     * @return a new HTML element instance
     */
    public HtmlElement getElement(
                                   String mapId );

    /**
     * @param properties properties describing this element
     * @return a new HTML element instance
     */
    public HtmlElement getElement(
                                   UiElementProperties properties );

    /**
     * @return HtmlAlert
     */
    public HtmlAlert expectAlert();

    /**
     * @return HtmlPrompt
     */
    public HtmlPrompt expectPrompt();

    /**
     * 
     * @return HtmlConfirm
     */
    public HtmlConfirm expectConfirm();

}
