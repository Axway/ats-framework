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

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiFileBrowse;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;

/**
 * Html File Browse
 */
@PublicAtsApi
public abstract class HtmlFileBrowse extends UiFileBrowse {

    public HtmlFileBrowse( UiDriver uiDriver, UiElementProperties properties ) {

        super( uiDriver, properties );
    }

    /**
    *
    * @param webDriver {@link WebDriver} instance
    * @param value the file input value to set
    */
    protected void setFileInputValue( WebDriver webDriver, String value ) {

        String locator = this.getElementProperties()
                             .getInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR );

        String css = this.getElementProperty( "_css" );

        WebElement element = null;

        if( !StringUtils.isNullOrEmpty( css ) ) {
            element = webDriver.findElement( By.cssSelector( css ) );
        } else {
            element = webDriver.findElement( By.xpath( locator ) );
        }

        try {
            element.sendKeys( value );
        } catch( ElementNotVisibleException enve ) {

            if( !UiEngineConfigurator.getInstance().isWorkWithInvisibleElements() ) {
                throw enve;
            }
            // try to make the element visible overriding some CSS properties
            // but keep in mind that it can be still invisible using another CSS and/or JavaScript techniques
            String styleAttrValue = element.getAttribute( "style" );
            JavascriptExecutor jsExec = ( JavascriptExecutor ) webDriver;
            try {
                jsExec.executeScript( "arguments[0].setAttribute('style', arguments[1]);",
                                      element,
                                      "display:'block'; visibility:'visible'; top:'auto'; left:'auto'; z-index:999;"
                                               + "height:'auto'; width:'auto';" );
                element.sendKeys( value );
            } finally {
                jsExec.executeScript( "arguments[0].setAttribute('style', arguments[1]);", element,
                                      styleAttrValue );
            }

        } catch( InvalidElementStateException e ) {
            throw new SeleniumOperationException( e.getMessage(), e );
        }
    }

}
