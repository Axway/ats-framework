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
package com.axway.ats.uiengine.engine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.TargetLocator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.AbstractHtmlDriver;
import com.axway.ats.uiengine.AbstractRealBrowserDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.AbstractElementsFactory;
import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlNavigator;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.internal.engine.AbstractEngine;
import com.axway.ats.uiengine.internal.engine.IHtmlEngine;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.BaseFrameElement;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Engine operating over HTML application
 */
@PublicAtsApi
public abstract class AbstractHtmlEngine extends AbstractEngine implements IHtmlEngine {

    private static Logger log = LogManager.getLogger(AbstractHtmlEngine.class);

    private WebDriver     webDriver;

    private String        mainWindowHandle;

    public AbstractHtmlEngine( UiDriver uiDriver,
                               AbstractElementsFactory elementsFactory ) {

        super(uiDriver, elementsFactory);

        AbstractHtmlDriver htmlDriver = (AbstractHtmlDriver) uiDriver;
        webDriver = (WebDriver) htmlDriver.getInternalObject(InternalObjectsEnum.WebDriver.name());
        mainWindowHandle = webDriver.getWindowHandle();
    }

    /**
     * Navigate to another URL from within the same already started browser
     * @param url of the page to go
     */
    @PublicAtsApi
    public void goToPage(
                          String url ) {

        AbstractRealBrowserDriver abstractUiDriver = null;
        // only from the main window we have to go to another pages
        goToMainWindow();
        HtmlNavigator.getInstance().navigateToTopFrame(webDriver);

        log.info("Go to URL: " + url);
        try {
            webDriver.get(url);
            if (this.uiDriver instanceof AbstractRealBrowserDriver) {
                abstractUiDriver = (AbstractRealBrowserDriver) uiDriver;
                abstractUiDriver.handleExpectedPopups();
                abstractUiDriver.waitForPageLoaded(webDriver,
                                                   UiEngineConfigurator.getInstance()
                                                                       .getWaitPageToLoadTimeout());
            }
        } finally {
            if (abstractUiDriver != null)
                abstractUiDriver.clearExpectedPopups();
        }
    }

    /**
     *
     * @return the current URL
     */
    @PublicAtsApi
    public String getCurrentUrl() {

        if (webDriver == null) {
            return null;
        }
        return webDriver.getCurrentUrl();
    }

    /**
     * Get page source as String
     *
     * @return page source
     */
    @PublicAtsApi
    public String getPageSource() {

        if (webDriver == null) {
            return "";
        }
        return webDriver.getPageSource();
    }

    /**
     * Returns the titles of all windows that the browser knows about
     * @return array of all window titles
     */
    @PublicAtsApi
    public String[] getWindowTitles() {

        Set<String> availableWindows = webDriver.getWindowHandles();
        String[] windowTitles = new String[availableWindows.size()];
        if (!availableWindows.isEmpty()) {
            String currentWindowHandle = webDriver.getWindowHandle();
            int i = 0;
            for (String windowHandle : availableWindows) {
                windowTitles[i++] = webDriver.switchTo().window(windowHandle).getTitle();
            }
            webDriver.switchTo().window(currentWindowHandle);
        }
        return windowTitles;
    }

    /**
     * Going to window/tab with given title
     * @param windowTitle window title or name (the value of "window.name" defined when the window was opened)
     */
    @PublicAtsApi
    public void goToWindow(
                            String windowTitle ) {

        goToWindow(windowTitle, UiEngineConfigurator.getInstance().getElementStateChangeDelay());
    }

    /**
     * Going to window/tab with given title
     * @param windowTitle window title or name (the value of "window.name" defined when the window was opened)
     * @param timeoutInSeconds timeout (in seconds) for waiting the target window to appear
     */
    @PublicAtsApi
    public void goToWindow(
                            final String windowTitle,
                            long timeoutInSeconds ) {

        if (windowTitle == null) {
            log.info("Go to main window/tab");

            goToMainWindow();
        } else {

            log.info("Go to window/tab with title/name '" + windowTitle + "'");
            switchToWindowByTitle(windowTitle, timeoutInSeconds, false);
        }
    }

    /**
     * Go to window/tab with same title. Note that order of same titled windows is not guaranteed. 
     * @param timeoutInSeconds timeout (in seconds) for waiting the target window to appear
     */
    @PublicAtsApi
    public void goToAnotherWindow(long timeoutInSeconds ) {

        final String windowTitle = webDriver.getTitle();
        log.info("Go to another window/tab with title/name '" + windowTitle + "'");
        switchToWindowByTitle(windowTitle, timeoutInSeconds, true);
    }
    
    /**
     * Go to main window/tab
     */
    @PublicAtsApi
    public void goToMainWindow() {

        webDriver.switchTo().window(mainWindowHandle);
    }

    /**
     * Go to first window/tab without title, skipping the main window
     */
    @PublicAtsApi
    public void goToFirstWindowWithoutTitle() {

        switchToWindowByTitle("", UiEngineConfigurator.getInstance().getElementStateChangeDelay(), false);
    }

    private void switchToWindowByTitle(
                                        final String windowTitle, long timeoutInSeconds, final boolean checkForNotCurrentWindow) {

        ExpectedCondition<Boolean> expectation = new ExpectedCondition<Boolean>() {
            public Boolean apply(
                                  WebDriver driver ) {

                return switchToWindowByTitle(windowTitle, checkForNotCurrentWindow);
            }
        };
        Wait<WebDriver> wait = new WebDriverWait(webDriver, timeoutInSeconds);
        try {
            wait.until(expectation);
        } catch (Exception e) {
            throw new SeleniumOperationException("Timeout waiting for Window with title '" + windowTitle
                                                 + "' to appear.", e);
        }
    }

    /**
     * 
     * @param searchTitle window title to match
     * @param checkForNotCurrentWindow - if true makes sure that new window is not current one
     * @return
     */
    private boolean switchToWindowByTitle(
                                           String searchTitle, boolean checkForNotCurrentWindow ) {

        Set<String> availableWindows = webDriver.getWindowHandles();
        if (!availableWindows.isEmpty()) {
            /*
             * Some people are complaining this code would not work as it throws errors when traversing all the windows. 
             * Unfortunately we cannot point directly to the window we want, but have to first switch to the next one, 
             * then check its title to verify if this is the one we want.
             * Here we traverse through the list of windows in backwards order in effort to avoid the issue as they do
             * not want to go to the main window when calling this method.
             * This is ugly, but works for now.
             * 
             * The returned Set by Selenium is an actual LinkedHashSet<String> so it is ordered indeed.
             */
            String[] availableWindowsArray = availableWindows.toArray(new String[availableWindows.size()]);
            if (searchTitle != null ) {
                searchTitle = searchTitle.trim();
            }
            String initialWindowId = webDriver.getWindowHandle();

            for (int i = availableWindowsArray.length - 1; i >= 0; i--) {
                String winId = availableWindowsArray[i];
                String windowTitle = webDriver.switchTo().window(winId).getTitle();
                if (checkForNotCurrentWindow && winId.equals(initialWindowId)) {
                    // skip - initial window found
                } else {
                    if ( windowTitle == null && (searchTitle == null || searchTitle.isEmpty()) ) { 
                            return true;
                    } else if (windowTitle == null || (windowTitle != null && searchTitle == null)) {
                        // skip - no match
                    } else  { // windowTitle !=null && title != null
                        if ( windowTitle.trim().equalsIgnoreCase(searchTitle) ) {
                            return true;
                        }
                    }
                }
                // no match - new iteration     
                webDriver.switchTo().window(initialWindowId);
                    
            }
        }
        return false;
    }
    

    /**
     * Close the active window/tab
     */
    @PublicAtsApi
    public void closeActiveWindow() {

        webDriver.close();
    }

    /**
     * Explicitly simulate an event, to trigger the corresponding "onevent" handler.
     * @param element the target element
     * @param eventName the name of the event to fire e.g. click, blur, focus, keydown, keyup, keypress...
     */
    @PublicAtsApi
    public void fireEvent(
                           UiElement element,
                           String eventName ) {

        HtmlNavigator.getInstance().navigateToFrame(webDriver, element);

        String xpath = element.getElementProperties()
                              .getInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR);

        String css = element.getElementProperty("_css");

        WebElement webElement = null;
        if (!StringUtils.isNullOrEmpty(css)) {
            webElement = webDriver.findElement(By.cssSelector(css));
        } else {
            webElement = webDriver.findElement(By.xpath(xpath));
        }

        StringBuilder builder = new StringBuilder();
        // @formatter:off
        builder.append( "function triggerEvent(element, eventType, canBubble, controlKeyDown, altKeyDown, shiftKeyDown, metaKeyDown) {\n"
                        + "        canBubble = (typeof(canBubble) == undefined) ? true : canBubble;\n"
                        + "        if (element.fireEvent && element.ownerDocument && element.ownerDocument.createEventObject) { // IE\n"
                        + "            var evt = this.createEventObject(element, controlKeyDown, altKeyDown, shiftKeyDown, metaKeyDown);\n"
                        + "            element.fireEvent('on' + eventType, evt);\n" + "        } else {\n"
                        + "            var evt = document.createEvent('HTMLEvents');\n"
                        + "            try {\n" + "                evt.shiftKey = shiftKeyDown;\n"
                        + "                evt.metaKey = metaKeyDown;\n"
                        + "                evt.altKey = altKeyDown;\n"
                        + "                evt.ctrlKey = controlKeyDown;\n" + "            } catch (e) {\n"
                        + "                // Nothing sane to do\n" + "            }\n"
                        + "            evt.initEvent(eventType, canBubble, true);\n"
                        + "            element.dispatchEvent(evt);\n" + "        }\n" + "    }\n" );
        // @formatter:on
        builder.append("triggerEvent(arguments[0],'").append(eventName).append("',false);");

        ((JavascriptExecutor) webDriver).executeScript(builder.toString(), webElement);
    }

    @PublicAtsApi
    public void reloadFrames() {

        // real browsers reloads the frames automatically
        if (webDriver instanceof HtmlUnitDriver) {

            Field webClientField = null;
            boolean fieldAccessibleState = false;
            try {
                // Retrieve current WebClient instance (with the current page) from the Selenium WebDriver
                TargetLocator targetLocator = webDriver.switchTo();
                webClientField = targetLocator.getClass().getDeclaringClass().getDeclaredField("webClient");
                fieldAccessibleState = webClientField.isAccessible();
                webClientField.setAccessible(true);
                WebClient webClient = (WebClient) webClientField.get(targetLocator.defaultContent());
                HtmlPage page = (HtmlPage) webClient.getCurrentWindow().getEnclosedPage();

                for (final FrameWindow frameWindow : page.getFrames()) {

                    final BaseFrameElement frame = frameWindow.getFrameElement();

                    // if a script has already changed its content, it should be skipped
                    // use == and not equals(...) to identify initial content (versus URL set to "about:blank")
                    if (frame.getEnclosedPage()
                             .getWebResponse()
                             .getWebRequest()
                             .getUrl() == WebClient.URL_ABOUT_BLANK) {

                        String src = frame.getSrcAttribute();
                        if (src != null && !src.isEmpty()) {
                            final URL url;
                            try {
                                url = ((HtmlPage) frame.getEnclosedPage()).getFullyQualifiedUrl(src);
                            } catch (final MalformedURLException e) {
                                String message = "Invalid src attribute of " + frame.getTagName() + ": url=["
                                                 + src + "]. Ignored.";
                                final IncorrectnessListener incorrectnessListener = webClient.getIncorrectnessListener();
                                incorrectnessListener.notify(message, this);
                                return;
                            }
                            if (isAlreadyLoadedByAncestor(url, ((HtmlPage) frame.getEnclosedPage()))) {
                                String message = "Recursive src attribute of " + frame.getTagName()
                                                 + ": url=[" + src + "]. Ignored.";
                                final IncorrectnessListener incorrectnessListener = webClient.getIncorrectnessListener();
                                incorrectnessListener.notify(message, this);

                                log.info("Frame already loaded: " + frame.toString());
                                return;
                            }
                            try {
                                final WebRequest request = new WebRequest(url);
                                request.setAdditionalHeader("Accept",
                                                            "text/html,application/xhtml+xml,application/xml;q=0.9, text/*;q=0.7, */*;q=0.5");

                                if (frameWindow.getName() == null || frameWindow.getName().isEmpty()) {
                                    frameWindow.setName("frame_" + page.getFrames().indexOf(frameWindow));
                                }
                                webClient.loadWebResponseInto(webClient.loadWebResponse(request),
                                                              frameWindow);
                                log.info("Frame loaded: " + frame.toString());

                            } catch (IOException e) {

                                log.error("Error when getting content for " + frame.getTagName()
                                          + " with src=" + url, e);
                            }
                        }
                    } else {

                        log.info("Frame already loaded: " + frame.toString());
                    }
                }

            } catch (Exception e) {

                throw new SeleniumOperationException("Error retrieving internal Selenium web client", e);
            } finally {

                if (webClientField != null) {
                    webClientField.setAccessible(fieldAccessibleState);
                }
            }
        }
    }

    private boolean isAlreadyLoadedByAncestor(
                                               final URL url,
                                               HtmlPage page ) {

        WebWindow window = page.getEnclosingWindow();
        while (window != null) {
            if (url.sameFile(window.getEnclosedPage().getWebResponse().getWebRequest().getUrl())) {
                return true;
            }
            if (window == window.getParentWindow()) {
                window = null;
            } else {
                window = window.getParentWindow();
            }
        }
        return false;
    }

    /**
     * Retrieve the browser's focus.
     * This is sometimes needed, for example right before using the RobotEngine
     */
    @PublicAtsApi
    public void retrieveBrowserFocus() {

        webDriver.switchTo().window(webDriver.getWindowHandle());
    }

    /**
     * <pre>
     * Gets the result of evaluating the specified JavaScript command. The command/snippet may have multiple lines,
     * but only the result of the last line will be returned. <br>
     *
     * Note that, by default, the command will run in the context of the "selenium" object itself,
     * so <b>this</b> will refer to the Selenium object. <br>
     * Use <b>window</b> to refer to the window of your application,
     *  e.g. <i>var fooEl = window.document.getElementById('foo'); fooEl.name;</i>
     * </pre>
     *
     * @param javaScriptCommand the JavaScript command
     * @return the result of the specified command. The command/snippet may have multiple lines,
     * but only the result of the last line will be returned.
     */
    @PublicAtsApi
    public String executeJavaScript(
                                     String javaScriptCommand ) {

        if (!StringUtils.isNullOrEmpty(javaScriptCommand)) {
            try {
                return String.valueOf( ((JavascriptExecutor) webDriver).executeScript(javaScriptCommand));
            } catch (Exception e) {
                throw new SeleniumOperationException("Failed to execute JavaScript command: \""
                                                     + javaScriptCommand + "\"", e);
            }
        }
        return null;
    }

    /**
     * Returns the number of HTML elements that match the specified xpath<br>
     *  eg. "//table" would give the number of tables
     *
     * @param xpath the xpath expression to evaluate
     * @return matched elements count
     */
    @PublicAtsApi
    public int countElements(
                              String xpath ) {

        if (!StringUtils.isNullOrEmpty(xpath)) {
            try {
                return webDriver.findElements(By.xpath(xpath)).size();
            } catch (Exception e) {
                throw new SeleniumOperationException("Failed to count elements by XPATH \"" + xpath + "\"",
                                                     e);
            }
        }
        return 0;
    }

    /**
     * Returns the number of HTML elements that match the specified css selector<br>
     *  eg. "//table" would give the number of tables
     *
     * @param cssSelector the css selector expression to evaluate
     * @return matched elements count
     */
    @PublicAtsApi
    public int countElementsByCssSelector(
                                           String cssSelector ) {

        if (!StringUtils.isNullOrEmpty(cssSelector)) {
            try {
                return webDriver.findElements(By.cssSelector(cssSelector)).size();

            } catch (Exception e) {
                throw new SeleniumOperationException("Failed to count elements by CSS Selector \""
                                                     + cssSelector + "\"", e);
            }
        }
        return 0;
    }

    /**
     *
     * @param cookieName the name of the cookie. May not be null or an empty string.
     * @param cookieValue the cookie value. May not be null.
     */
    @PublicAtsApi
    public void setCookie(
                           String cookieName,
                           String cookieValue ) {

        Cookie cookie = new Cookie(cookieName, cookieValue);
        webDriver.manage().addCookie(cookie);
    }

    /**
     *
     * @param name the name of the cookie. May not be null or an empty string.
     * @param value the cookie value. May not be null.
     * @param domain the domain the cookie is visible to.
     * @param path the path the cookie is visible to. If left blank or set to null, will be set to
     *        "/".
     * @param expiry the cookie's expiration date; may be null.
     * @param isSecure whether this cookie requires a secure connection.
     */
    @PublicAtsApi
    public void setCookie(
                           String name,
                           String value,
                           String domain,
                           String path,
                           Date expiry,
                           boolean isSecure ) {

        Cookie cookie = new Cookie(name, value, domain, path, expiry, isSecure);
        webDriver.manage().addCookie(cookie);
    }

    /**
     * Get all the cookies for the current domain. This is the equivalent of calling "document.cookie" and parsing the result
     *
     * @return {@link com.axway.ats.uiengine.elements.html.Cookie Cookie}s array
     */
    @PublicAtsApi
    public com.axway.ats.uiengine.elements.html.Cookie[] getCookies() {

        Set<Cookie> cookies = webDriver.manage().getCookies();
        com.axway.ats.uiengine.elements.html.Cookie[] cookiesArr = new com.axway.ats.uiengine.elements.html.Cookie[cookies.size()];
        int i = 0;
        for (Cookie c : cookies) {
            cookiesArr[i++] = new com.axway.ats.uiengine.elements.html.Cookie(c.getName(),
                                                                              c.getValue(),
                                                                              c.getDomain(),
                                                                              c.getPath(),
                                                                              c.getExpiry(),
                                                                              c.isSecure());
        }
        return cookiesArr;
    }

    /**
     * Delete the named cookie from the current domain.<br>
     * This is equivalent to setting the named cookie's expiration date to some time in the past.
     *
     * @param cookieName the name of the cookie. May not be null or an empty string.
     */
    @PublicAtsApi
    public void deleteCookieNamed(
                                   String cookieName ) {

        webDriver.manage().deleteCookieNamed(cookieName);
    }

    /**
     * Delete all the cookies for the current domain
     */
    @PublicAtsApi
    public void deleteAllCookies() {

        webDriver.manage().deleteAllCookies();
    }

    /**
     * Move back a single "item" in the browser's history.
     */
    @PublicAtsApi
    public void clickBrowserBackButton() {

        webDriver.navigate().back();
    }

    /**
     * Move a single "item" forward in the browser's history. Does nothing if we are on the latest page viewed.
     */
    @PublicAtsApi
    public void clickBrowserForwardButton() {

        webDriver.navigate().forward();
    }

    /**
     * Refresh the current page.
     */
    @PublicAtsApi
    public void clickBrowserRefreshButton() {

        webDriver.navigate().refresh();
    }

}
