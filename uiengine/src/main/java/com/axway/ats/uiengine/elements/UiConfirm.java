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
 * JavaScript Confirm. Only one prompt can be present at a time.
 */
@PublicAtsApi
public abstract class UiConfirm extends UiElement {

    public UiConfirm( UiDriver uiDriver ) {

        super( uiDriver, null );
    }

    /**
     * Clicks OK button
     */
    @PublicAtsApi
    public abstract void clickOk();

    /**
     * Clicks OK button if the expected confirm text matches with the actual
     */
    @PublicAtsApi
    public abstract void clickOk(
                                  String expectedConfirmText );

    /**
     * Clicks Cancel button
     */
    @PublicAtsApi
    public abstract void clickCancel();

    /**
     * Clicks OK button if the expected confirm text matches with the actual
     */
    @PublicAtsApi
    public abstract void clickCancel(
                                      String expectedConfirmText );

}
