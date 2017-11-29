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
package com.axway.ats.uiengine.elements.html.hiddenbrowser;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlButton;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.hiddenbrowser.HiddenHtmlElementState;
import com.axway.ats.uiengine.utilities.hiddenbrowser.HiddenHtmlElementUtils;

/**
 * An HTML Button
 * @see HiddenHtmlElement
 */
@PublicAtsApi
public class HiddenHtmlButton extends HtmlButton {

    private UiDriver uiDriver;

    public HiddenHtmlButton( UiDriver uiDriver,
                             UiElementProperties properties ) {

        super( uiDriver, properties );
        this.uiDriver = uiDriver;
        String matchingRules[] = properties.checkTypeAndRules( this.getClass().getSimpleName(),
                                                               "HiddenHtml",
                                                               HiddenHtmlElement.RULES_DUMMY );

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator( matchingRules,
                                                                    properties,
                                                                    new String[]{ "submit",
                                                                                  "reset",
                                                                                  "button" },
                                                                    "button" );
        properties.addInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath );
    }

    @Override
    @PublicAtsApi
    public void click() {

        new HiddenHtmlElementState( this ).waitToBecomeExisting();

        HiddenHtmlElementUtils.mouseClick( HiddenHtmlElementLocator.findElement( this ) );
        
        UiEngineUtilities.sleep();
    }

    /**
     * Click button and download file
     */
    @PublicAtsApi
    public void clickAndDownloadFile() {

        new HiddenHtmlElementState( this ).waitToBecomeExisting();

        new HiddenHtmlElement( uiDriver,
                               new UiElementProperties().addProperty( "xpath",
                                                                      properties.getInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR ) ) ).clickAndDownloadFile();
        
        UiEngineUtilities.sleep();
    }

}
