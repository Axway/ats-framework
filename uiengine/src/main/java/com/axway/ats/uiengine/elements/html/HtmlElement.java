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
package com.axway.ats.uiengine.elements.html;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;

/**
 * Used to represent generic HTML element (tag). It could be used to work with generic elements like 
 * <code>span</code>, <code>div</code>, etc.
 * Such elements could be located by any HTML attribute including custom ones.
 * <p>
 * <em>Special cases:</em>
 * <ul>
 *   <li>use <code>_text</code> attribute for finding elements by certain text (in-between tags) 
 *   <li>use <code>_partText</code> attribute for finding elements by partial match of text in-between tags
 *   <li>use <code>_inputType</code> attribute for locating elements by their <code>type</code> attribute. 
 *   _inputType is replaced with <code>type</code> HTML attribute when generating element&apos;'s XPath
 * </ul>
 * </p>
 */
@PublicAtsApi
public abstract class HtmlElement extends UiElement {

    public HtmlElement( UiDriver uiDriver,
                        UiElementProperties properties ) {

        super( uiDriver, properties );
    }

    /**
     * Get element attribute value
     * @return value of the attribute (style/class/...)
     */
    @PublicAtsApi
    public abstract String getAttributeValue(
                                              String attribute );

    /**
     * Get CSS property value
     * @param propertyName CSS property name
     * @return the value of the target CSS property
     */
    @PublicAtsApi
    public abstract String getCssPropertyValue(
                                                String propertyName );

    /**
     * Get text content of the element
     * @return text content of the element
     */
    @PublicAtsApi
    public abstract String getTextContent();

    /**
     * Set the content of the element
     * @param content the new content
     */
    @PublicAtsApi
    public abstract void setTextContent(
                                         String content );

    /**
     * Simulate mouse click action
     */
    @PublicAtsApi
    public abstract void click();

    /**
     * Simulate mouse double click action
     */
    @PublicAtsApi
    public abstract void doubleClick();

    /**
     * Simulate mouse right click action
     */
    @PublicAtsApi
    public abstract void rightClick();

    /**
     * Simulate mouse over
     */
    @PublicAtsApi
    public abstract void mouseOver();

    /**
     * Simulate Enter key
     */
    @PublicAtsApi
    public abstract void pressEnterKey();

    /**
     * Simulate Space key
     */
    @PublicAtsApi
    public abstract void pressSpaceKey();

    /**
     * Simulate Tab key
     */
    @PublicAtsApi
    public abstract void pressTabKey();

    /**
     * Simulate Escape key
     */
    @PublicAtsApi
    public abstract void pressEscapeKey();

    /**
     * Drag and drop an element on top of other element
     * @param targetElement the target element
     */
    @PublicAtsApi
    public abstract void dragAndDropTo(
                                        HtmlElement targetElement );

}
