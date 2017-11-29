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
 * A Check Box
 */
@PublicAtsApi
public abstract class UiCheckBox extends UiElement {

    public UiCheckBox( UiDriver uiDriver,
                       UiElementProperties properties ) {

        super( uiDriver, properties );
    }

    /**
     * Check the check box
     */
    @PublicAtsApi
    public abstract void check();

    /**
     * Uncheck the check box
     */
    @PublicAtsApi
    public abstract void unCheck();

    /**
     * Tells whether the check box is checked
     */
    @PublicAtsApi
    public abstract boolean isChecked();

    /**
     * Verify the check box is checked
     * 
     * throws an error if verification fail
     */
    @PublicAtsApi
    public abstract void verifyChecked();

    /**
     * Verify the check box is not checked
     * 
     * throws an error if verification fail
     */
    @PublicAtsApi
    public abstract void verifyNotChecked();
}
