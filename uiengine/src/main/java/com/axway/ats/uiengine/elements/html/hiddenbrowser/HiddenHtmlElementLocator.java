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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitWebElement;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.HiddenBrowserDriver;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlNavigator;
import com.axway.ats.uiengine.exceptions.ElementNotFoundException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;

public class HiddenHtmlElementLocator {

    private static Logger log = LogManager.getLogger(HiddenHtmlElementLocator.class);

    public static HtmlUnitWebElement findElement( UiElement uiElement ) {

        return findElement(uiElement, null, true);
    }

    public static HtmlUnitWebElement findElement( UiElement uiElement, String xpathSuffix, boolean verbose ) {

        HiddenBrowserDriver browserDriver = (HiddenBrowserDriver) uiElement.getUiDriver();
        WebDriver webDriver = (WebDriver) browserDriver.getInternalObject(InternalObjectsEnum.WebDriver.name());
        HtmlNavigator.getInstance().navigateToFrame(webDriver, uiElement);

        String xpath = uiElement.getElementProperties()
                                .getInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR);

        String css = uiElement.getElementProperty("_css");

        if (xpathSuffix != null) {
            xpath += xpathSuffix;
        }

        List<WebElement> elements = null;

        if (!StringUtils.isNullOrEmpty(css)) {
            elements = webDriver.findElements(By.cssSelector(css));
        } else {
            elements = webDriver.findElements(By.xpath(xpath));
        }

        if (elements.size() == 0) {

            throw new ElementNotFoundException(uiElement.toString() + " not found.");
        } else if (elements.size() > 1) {
            if (verbose) {

                log.warn("More than one HTML elements were found having properties " + uiElement.toString()
                         + ".Only the first HTML element will be used.");

            }
        }
        HtmlUnitWebElement element = (HtmlUnitWebElement) elements.get(0);
        if (verbose) {

            log.info("Found element: " + element.toString());
        }
        return element;
    }

}
