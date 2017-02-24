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
 * A Table
 */
@PublicAtsApi
public abstract class UiTable extends UiElement {

    public UiTable( UiDriver uiDriver,
                    UiElementProperties properties ) {

        super( uiDriver, properties );
    }

    /**
     * Get the value of the specified table field
     *
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     * @return
     */
    @PublicAtsApi
    public abstract String getFieldValue(
                                          int row,
                                          int column );

    /**
     * Set the value of the specified table field
     *
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     */
    @PublicAtsApi
    public abstract void setFieldValue(
                                        String value,
                                        int row,
                                        int column );

    /**
     * @return how many rows this table has
     */
    @PublicAtsApi
    public abstract int getRowCount();

    /**
     * @return how many columns this table has
     */
    @PublicAtsApi
    public abstract int getColumnCount();

    /**
     * Verify the field value is as specified
     *
     * @param expectedValue
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     */
    @PublicAtsApi
    public abstract void verifyFieldValue(
                                           String expectedValue,
                                           int row,
                                           int column );

    /**
     * Verify the field value is NOT as specified
     *
     * @param notExpectedValue
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     */
    @PublicAtsApi
    public abstract void verifyNotFieldValue(
                                              String notExpectedValue,
                                              int row,
                                              int column );

    /**
     * Verify the field value matches the specified java regular expression
     *
     * @param expectedValueRegex a java regular expression
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     */
    @PublicAtsApi
    public abstract void verifyFieldValueRegex(
                                                String expectedValueRegex,
                                                int row,
                                                int column );

}
