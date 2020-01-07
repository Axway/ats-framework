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

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.AbstractElementsFactory;

@PublicAtsApi
public abstract class AbstractEngine {

    protected UiDriver                uiDriver;

    protected AbstractElementsFactory elementsFactory;

    public AbstractEngine( UiDriver uiDriver,
                           AbstractElementsFactory elementsFactory ) {

        this.uiDriver = uiDriver;
        this.elementsFactory = elementsFactory;
    }

    @PublicAtsApi
    public void loadMapFile(
                             String mapFile,
                             String mapSection ) {

        elementsFactory.getElementsMap().loadMapFile( mapFile, mapSection );
    }

    @PublicAtsApi
    public void setMapSection(
                               String mapSection ) {

        elementsFactory.getElementsMap().setMapSection( mapSection );
    }

    /** Retrieve the current map section or null if no map/section is currently loaded
     * */
    @PublicAtsApi
    public String getMapSection() {

        return elementsFactory.getElementsMap().getMapSection();
    }
}
