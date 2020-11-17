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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.exceptions.ElementNotFoundException;

@PublicAtsApi
public class HtmlNavigator {

    private static Logger        log                = LogManager.getLogger(HtmlNavigator.class);

    private static HtmlNavigator instance;

    private WebDriver            lastWebDriver;

    private String               lastFramesLocation = "";

    private HtmlNavigator() {

    }

    public static synchronized HtmlNavigator getInstance() {

        if (instance == null) {
            instance = new HtmlNavigator();
        }
        return instance;
    }

    @PublicAtsApi
    public void navigateToTopFrame(
                                    WebDriver webDriver ) {

        navigateToFrame(webDriver, null);
    }

    @PublicAtsApi
    public void navigateToFrame(
                                 WebDriver webDriver,
                                 UiElement element ) {

        if (lastWebDriver != webDriver) {
            // this is a new WebDriver instance
            lastWebDriver = webDriver;
            lastFramesLocation = "";
        }

        String newFramesLocationProperty = element != null
                                                           ? element.getElementProperty("frame")
                                                           : null;
        try {
            if (newFramesLocationProperty == null) {
                // No frame selection. Go to top frame if not there yet
                if (!"".equals(lastFramesLocation)) {
                    log.debug("Go to TOP frame");
                    webDriver.switchTo().defaultContent();
                    lastFramesLocation = "";
                }
            } else {
                lastFramesLocation = newFramesLocationProperty;
                log.debug("Go to frame: " + newFramesLocationProperty);

                String[] newFramesLocation = newFramesLocationProperty.split("\\->");
                webDriver.switchTo().defaultContent();

                for (String frame : newFramesLocation) {

                    if (frame.startsWith("/") || frame.startsWith("(/")) {

                        WebElement frameElement = webDriver.findElement(By.xpath(frame.trim()));
                        webDriver.switchTo().frame(frameElement);
                    } else {

                        webDriver.switchTo().frame(frame.trim());
                    }
                }
            }
        } catch (NotFoundException nfe) {

            String msg = "Frame not found. Searched by: '"
                         + (element != null
                                            ? element.getElementProperty("frame")
                                            : "")
                         + "'";
            log.debug(msg);
            throw new ElementNotFoundException(msg, nfe);
        }
    }

}
