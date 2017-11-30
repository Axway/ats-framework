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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.uiengine.HiddenBrowserDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElement;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.exceptions.ElementNotFoundException;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.hiddenbrowser.HiddenHtmlElementState;
import com.axway.ats.uiengine.utilities.hiddenbrowser.HiddenHtmlElementUtils;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Used to represent generic HTML element (tag). It could be used to work with generic elements like 
 * <code>span</code>, <code>div</code>, etc.
 * 
 * For more info check documentation of {@link HtmlElement}
 */
@PublicAtsApi
public class HiddenHtmlElement extends HtmlElement {

    /**
     *  used by all HiddenHtmlXYZ classes for passing it to UiElementProperties's method checkTypeAndRules()
     */
    protected static final String[] RULES_DUMMY               = new String[]{};

    private static final Pattern    urlFileNamePattern        = Pattern.compile("^.*[\\/\\\\]+([^\\/\\\\]+\\.[\\w\\d\\$\\-]{1,8})$");
    private static final Pattern    contentDispositionPattern = Pattern.compile("^.*filename\\=\\\"?([^\\\"\\;]+)\\\"?\\;?$");
    private static final int        BUFFER_LENGTH             = 1024;

    private HtmlUnitDriver          htmlUnitDriver;

    public HiddenHtmlElement( UiDriver uiDriver,
                              UiElementProperties properties ) {

        super(uiDriver, properties);
        String matchingRules[] = properties.checkTypeAndRules(this.getClass().getSimpleName(),
                                                              "HiddenHtml",
                                                              RULES_DUMMY);

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator(matchingRules,
                                                                   properties,
                                                                   new String[]{},
                                                                   "*");
        properties.addInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath);

        HiddenBrowserDriver browserDriver = (HiddenBrowserDriver) uiDriver;
        htmlUnitDriver = (HtmlUnitDriver) browserDriver.getInternalObject(InternalObjectsEnum.WebDriver.name());
    }

    /**
     * Get element attribute value
     * @return value of the attribute (style/class/...)
     */
    @Override
    @PublicAtsApi
    public String getAttributeValue(
                                     String attribute ) {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        return HiddenHtmlElementLocator.findElement(this).getAttribute(attribute);
    }

    /**
     * Get CSS property value
     * @param propertyName CSS property name
     * @return the value of the target CSS property
     */
    @Override
    @PublicAtsApi
    public String getCssPropertyValue(
                                       String propertyName ) {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        return HiddenHtmlElementLocator.findElement(this).getCssValue(propertyName);
    }

    /**
     * Get innerText of this element, including sub-elements, without any leading or trailing whitespace.
     *
     * @return innerText of this element, including sub-elements, without any leading or trailing whitespace.
     */
    @Override
    @PublicAtsApi
    public String getTextContent() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        return HiddenHtmlElementLocator.findElement(this).getText();
    }

    /**
     * Set the content of the element
     * @param content the new content
     */
    @PublicAtsApi
    public void setTextContent(
                                String content ) {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        WebElement element = HiddenHtmlElementLocator.findElement(this);
        new Actions(htmlUnitDriver).sendKeys(element, content).perform();
    }

    /**
     * Click the element and download file
     */
    protected void clickAndDownloadFile() {

        WebWindow currentWindow = null;
        Field currentWindowField = null;
        boolean fieldAccessibleState = false;
        try {
            currentWindowField = htmlUnitDriver.getClass().getDeclaredField("currentWindow");
            fieldAccessibleState = currentWindowField.isAccessible();
            currentWindowField.setAccessible(true);
            currentWindow = (WebWindow) currentWindowField.get(htmlUnitDriver);
        } catch (Exception e) {

            throw new SeleniumOperationException("Error retrieving internal Selenium web client", e);
        } finally {
            if (currentWindowField != null) {
                currentWindowField.setAccessible(fieldAccessibleState);
            }
        }

        String elementXPath = properties.getProperty("xpath");
        // find element and download the file
        HtmlPage page = (HtmlPage) currentWindow.getEnclosedPage();
        List<?> foundElementsList = page.getByXPath(elementXPath);
        if (foundElementsList != null && !foundElementsList.isEmpty()) {

            InputStream in = null;
            FileOutputStream fos = null;
            try {
                com.gargoylesoftware.htmlunit.html.HtmlElement element = (com.gargoylesoftware.htmlunit.html.HtmlElement) foundElementsList.get(0);
                Page result = element.click(); // Use generic Page. Exact page type returned depends on the MIME type set in response header

                String fileName = null;
                String contentDisposition = result.getWebResponse()
                                                  .getResponseHeaderValue("Content-Disposition");
                if (contentDisposition != null) {
                    Matcher m = contentDispositionPattern.matcher(contentDisposition);
                    if (m.matches()) {
                        fileName = m.group(1);
                        log.debug("Download file name extracted from the 'Content-Disposition' header is "
                                  + fileName);
                    }
                }
                if (fileName == null) {
                    String url = result.getWebResponse().getWebRequest().getUrl().getFile().trim();
                    Matcher m = urlFileNamePattern.matcher(url);
                    if (m.matches()) {
                        fileName = m.group(1);
                        log.debug("Download file name extracted from the request URL is " + fileName);
                    } else {
                        fileName = String.valueOf(new Date().getTime()) + ".bin";
                        log.debug("Downloaded file name constructed the current timestamp is " + fileName);
                    }
                }
                in = result.getWebResponse().getContentAsStream();
                String fileAbsPath = UiEngineConfigurator.getInstance().getBrowserDownloadDir() + fileName;
                fos = new FileOutputStream(new File(fileAbsPath), false);
                byte[] buff = new byte[BUFFER_LENGTH];
                int len;
                while ( (len = in.read(buff)) != -1) {
                    fos.write(buff, 0, len);
                }
                fos.flush();
                log.info("Downloaded file: " + fileAbsPath);

            } catch (IOException e) {

                throw new SeleniumOperationException("Error downloading file", e);
            } finally {

                IoUtils.closeStream(fos);
                IoUtils.closeStream(in);
            }
        } else {

            throw new ElementNotFoundException("Can't find element by XPath: " + elementXPath);
        }
    }

    /**
     * Simulate mouse click action
     */
    @Override
    @PublicAtsApi
    public void click() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        HiddenHtmlElementUtils.mouseClick(HiddenHtmlElementLocator.findElement(this));
    }

    /**
     * Simulate mouse double click action
     */
    @Override
    @PublicAtsApi
    public void doubleClick() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        WebElement element = HiddenHtmlElementLocator.findElement(this);

        new Actions(htmlUnitDriver).doubleClick(element).perform();
    }

    /**
     * Simulate mouse right click action
     */
    @Override
    @PublicAtsApi
    public void rightClick() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        WebElement element = HiddenHtmlElementLocator.findElement(this);

        new Actions(htmlUnitDriver).contextClick(element).perform();
    }

    /**
     * Simulate mouse over
     */
    @Override
    @PublicAtsApi
    public void mouseOver() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        WebElement element = HiddenHtmlElementLocator.findElement(this);
        new Actions(htmlUnitDriver).moveToElement(element).perform();

        UiEngineUtilities.sleep();
    }

    /**
     * Simulate Enter key
     */
    @Override
    @PublicAtsApi
    public void pressEnterKey() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        WebElement element = HiddenHtmlElementLocator.findElement(this);
        new Actions(htmlUnitDriver).sendKeys(element, "\r").perform();
        //        new Actions( htmlUnitDriver ).sendKeys( element, Keys.RETURN ).perform();
    }

    /**
     * Simulate Space key
     */
    @Override
    @PublicAtsApi
    public void pressSpaceKey() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        WebElement element = HiddenHtmlElementLocator.findElement(this);
        new Actions(htmlUnitDriver).sendKeys(element, Keys.SPACE).perform();
    }

    /**
     * Simulate Tab key
     */
    @Override
    @PublicAtsApi
    public void pressTabKey() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        WebElement element = HiddenHtmlElementLocator.findElement(this);
        new Actions(htmlUnitDriver).sendKeys(element, Keys.TAB).perform();
    }

    /**
     * Simulate Escape key
     */
    @Override
    @PublicAtsApi
    public void pressEscapeKey() {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        WebElement element = HiddenHtmlElementLocator.findElement(this);
        new Actions(htmlUnitDriver).sendKeys(element, Keys.ESCAPE).perform();
    }

    /**
     * Drag and drop an element on top of other element
     * @param targetElement the target element
     */
    @Override
    @PublicAtsApi
    public void dragAndDropTo(
                               HtmlElement targetElement ) {

        new HiddenHtmlElementState(this).waitToBecomeExisting();

        WebElement source = HiddenHtmlElementLocator.findElement(this);
        WebElement target = HiddenHtmlElementLocator.findElement(targetElement);

        Actions actionBuilder = new Actions(htmlUnitDriver);
        Action dragAndDropAction = actionBuilder.clickAndHold(source)
                                                .moveToElement(target, 1, 1)
                                                .release(target)
                                                .build();
        dragAndDropAction.perform();

        // drops the source element in the middle of the target, which in some cases is not doing drop on the right place
        // new Actions( htmlUnitDriver ).dragAndDrop( source, target ).perform();
    }

}
