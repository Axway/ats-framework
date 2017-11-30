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
package com.axway.ats.uiengine.elements;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;

/**
 * A Text Area that can be edited
 */
@PublicAtsApi
public abstract class UiTextArea extends UiElement {

    public UiTextArea( UiDriver uiDriver,
                       UiElementProperties properties ) {

        super(uiDriver, properties);
    }

    /**
     * Set the Text Area value
     * 
     * @param value
     */
    @PublicAtsApi
    public abstract void setValue(
                                   String value );

    /**
     * Append text to the current content of a Text Area
     * 
     * @param value
     */
    @PublicAtsApi
    public abstract void appendValue(
                                      String value );

    /**
     * Get the Text Area value 
     * @return
     */
    @PublicAtsApi
    public abstract String getValue();

    /**
     * Verify the Text Area value is as specified
     *  
     * @param expectedValue
     */
    @PublicAtsApi
    public abstract void verifyValue(
                                      String expectedValue );

    /**
     * Verify the Text Area value is NOT as specified
     * 
     * @param notExpectedValue
     */
    @PublicAtsApi
    public abstract void verifyNotValue(
                                         String notExpectedValue );

}
