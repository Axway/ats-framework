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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.axway.ats.uiengine.MobileDriver;
import com.axway.ats.uiengine.elements.UiElement;

import io.appium.java_client.AppiumDriver;

public class MobileElementFinder {

    public static String defaultContext = MobileDriver.NATIVE_CONTEXT;

    public static WebElement findElement(
                                          AppiumDriver<?> driver,
                                          UiElement uiElement ) {

        // switch the context if needed
        String context = getElementContext( uiElement );
        if( context != null && !context.equals( driver.getContext() ) ) {
            driver.context( context );
        }

        // find the element
        if( uiElement.getElementProperty( "xpath" ) != null ) {

            return driver.findElement( By.xpath( uiElement.getElementProperty( "xpath" ) ) );
        } else if( uiElement.getElementProperty( "id" ) != null ) {

            return driver.findElement( By.id( uiElement.getElementProperty( "id" ) ) );
        } else if( uiElement.getElementProperty( "name" ) != null ) {

            return driver.findElement( By.name( uiElement.getElementProperty( "name" ) ) );
        } else if( uiElement.getElementProperty( "text" ) != null ) {

            return driver.findElement( By.linkText( uiElement.getElementProperty( "text" ) ) );
        } else if( uiElement.getElementProperty( "partialText" ) != null ) {

            return driver.findElement( By.partialLinkText( uiElement.getElementProperty( "partialText" ) ) );
        }
        return null;
    }

    public static String getElementContext(
                                            UiElement uiElement ) {

        // switch the context if needed
        String context = uiElement.getElementProperty( "context" );
        if( context == null ) {
            context = defaultContext;
        }
        return context;
    }
}
