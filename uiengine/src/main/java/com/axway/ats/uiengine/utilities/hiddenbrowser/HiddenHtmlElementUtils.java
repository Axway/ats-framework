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
package com.axway.ats.uiengine.utilities.hiddenbrowser;

import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.htmlunit.HtmlUnitKeyboard;
import org.openqa.selenium.htmlunit.HtmlUnitMouse;
import org.openqa.selenium.htmlunit.HtmlUnitWebElement;
import org.openqa.selenium.interactions.Coordinates;

import com.axway.ats.common.PublicAtsApi;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;

@PublicAtsApi
public class HiddenHtmlElementUtils {

    private static Logger log = LogManager.getLogger(HiddenHtmlElementUtils.class);

    @PublicAtsApi
    public static void mouseClick(
                                   HtmlUnitWebElement webElement ) {

        Method getElementForOperationMethod = null;
        boolean getElementForOperationMethodAccessible = false;
        Method moveOutIfNeededMethod = null;
        boolean moveOutIfNeededMethodAccessible = false;
        Method updateActiveElementMethod = null;
        boolean updateActiveElementMethodAccessible = false;
        try {
            // Allow invoking click on non-visible elements. Prevents HtmlUnit check for currently visible element
            // TODO: remove this possibility by removing UiEngineConfigurator#workWithInvisibleElements()
            // change access modifiers of some methods
            getElementForOperationMethod = HtmlUnitMouse.class.getDeclaredMethod("getElementForOperation",
                                                                                 Coordinates.class);
            getElementForOperationMethodAccessible = getElementForOperationMethod.isAccessible();
            getElementForOperationMethod.setAccessible(true);

            moveOutIfNeededMethod = HtmlUnitMouse.class.getDeclaredMethod("moveOutIfNeeded",
                                                                          DomElement.class);
            moveOutIfNeededMethodAccessible = moveOutIfNeededMethod.isAccessible();
            moveOutIfNeededMethod.setAccessible(true);

            updateActiveElementMethod = HtmlUnitMouse.class.getDeclaredMethod("updateActiveElement",
                                                                              DomElement.class);
            updateActiveElementMethodAccessible = updateActiveElementMethod.isAccessible();
            updateActiveElementMethod.setAccessible(true);

            // get the target element
            HtmlUnitDriver htmlUnitDriver = (HtmlUnitDriver) webElement.getWrappedDriver();
            HtmlUnitMouse mouse = (HtmlUnitMouse) htmlUnitDriver.getMouse();
            HtmlElement element = (HtmlElement) getElementForOperationMethod.invoke(mouse,
                                                                                    webElement.getCoordinates());
            moveOutIfNeededMethod.invoke(mouse, element);

            if (htmlUnitDriver.isJavascriptEnabled()) {
                if (! (element instanceof HtmlInput)) {
                    element.focus();
                }
                element.mouseOver();
                element.mouseMove();
            }
            HtmlUnitKeyboard keyboard = (HtmlUnitKeyboard) htmlUnitDriver.getKeyboard();
            element.click(keyboard.isShiftPressed(), keyboard.isCtrlPressed(), keyboard.isAltPressed());

            updateActiveElementMethod.invoke(mouse, element);

        } catch (IOException ioe) {

            throw new WebDriverException(ioe);
        } catch (ScriptException e) {

            // we need only our exception if such exists
            Throwable uiEngineException = e.getCause();
            while (uiEngineException != null
                   && !uiEngineException.getClass().getName().toLowerCase().contains("com.axway.ats")) {
                uiEngineException = uiEngineException.getCause();
            }
            if (uiEngineException != null) {
                throw(RuntimeException) uiEngineException;
            }

            // Log the exception with level WARN, because in the main Selenium implementation
            // (HtmlUnitMouse.click(coordinates)) the exception is even skipped
            log.warn("Script error while clicking web element. " + webElement.toString(), e);
        } catch (Exception e) {

            throw new RuntimeException(e);
        } finally {
            // Restore accessibility modifier
            if (getElementForOperationMethod != null) {
                getElementForOperationMethod.setAccessible(getElementForOperationMethodAccessible);
            }
            if (moveOutIfNeededMethod != null) {
                moveOutIfNeededMethod.setAccessible(moveOutIfNeededMethodAccessible);
            }
            if (updateActiveElementMethod != null) {
                updateActiveElementMethod.setAccessible(updateActiveElementMethodAccessible);
            }
        }
    }

}
