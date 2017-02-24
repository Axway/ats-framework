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
package com.axway.ats.uiengine.elements.mobile;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;

/**
 * An mobile element link - native or HTML
 *
 * <p>
 * Can be identified by:
 * <ul>
 *    <li>id</li>
 *    <li>name</li>
 *    <li>text - the exact text that element displays</li>
 *    <li>partialText - some part of the text that element displays</li>
 *    <li>xpath</li>
 * </ul>
 * </p>
 */
@PublicAtsApi
public class MobileLink extends MobileElement<MobileLink> {

    private static final String[] RULES = { "id", "name", "text", "partialText", "xpath" };

    public MobileLink( UiDriver uiDriver,
                       UiElementProperties properties ) {

        super( uiDriver, properties );
        properties.checkTypeAndRules( this.getClass().getSimpleName(), "Mobile", RULES );
    }

}
