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

import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.AbstractElementsFactory;
import com.axway.ats.uiengine.elements.UiElementProperties;

/**
 * A factory for mobile device UI elements
 */
public class MobileElementsFactory extends AbstractElementsFactory {

    private static MobileElementsFactory instance;

    private MobileElementsFactory() {

        super();
    }

    synchronized public static MobileElementsFactory getInstance() {

        if( instance == null ) {
            instance = new MobileElementsFactory();
        }
        return instance;
    }

    public MobileButton getMobileButton(
                                         String mapId,
                                         UiDriver uiDriver ) {

        return getMobileButton( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public MobileButton getMobileButton(
                                         UiElementProperties properties,
                                         UiDriver uiDriver ) {

        return new MobileButton( uiDriver, properties );
    }

    public MobileTextBox getMobileTextBox(
                                           String mapId,
                                           UiDriver uiDriver ) {

        return getMobileTextBox( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public MobileTextBox getMobileTextBox(
                                           UiElementProperties properties,
                                           UiDriver uiDriver ) {

        return new MobileTextBox( uiDriver, properties );
    }

    public MobileCheckBox getMobileCheckBox(
                                             String mapId,
                                             UiDriver uiDriver ) {

        return getMobileCheckBox( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public MobileCheckBox getMobileCheckBox(
                                             UiElementProperties properties,
                                             UiDriver uiDriver ) {

        return new MobileCheckBox( uiDriver, properties );
    }

    public MobileLink getMobileLink(
                                     String mapId,
                                     UiDriver uiDriver ) {

        return getMobileLink( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public MobileLink getMobileLink(
                                     UiElementProperties properties,
                                     UiDriver uiDriver ) {

        return new MobileLink( uiDriver, properties );
    }

    public MobileLabel getMobileLabel(
                                       String mapId,
                                       UiDriver uiDriver ) {

        return getMobileLabel( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public MobileLabel getMobileLabel(
                                       UiElementProperties properties,
                                       UiDriver uiDriver ) {

        return new MobileLabel( uiDriver, properties );
    }

    public MobileElement<?> getMobileElement(
                                              String mapId,
                                              UiDriver uiDriver ) {

        return getMobileElement( elementsMap.getElementProperties( mapId ), uiDriver );
    }

    public MobileElement<?> getMobileElement(
                                              UiElementProperties properties,
                                              UiDriver uiDriver ) {

        return new MobileElement<MobileElement<?>>( uiDriver, properties );
    }

}
