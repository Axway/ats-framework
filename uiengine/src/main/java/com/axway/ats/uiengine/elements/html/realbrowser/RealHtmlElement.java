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
package com.axway.ats.uiengine.elements.html.realbrowser;

import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.AbstractRealBrowserDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElement;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * Used to represent generic HTML element (tag). It could be used to work with generic elements like 
 * <code>span</code>, <code>div</code>, etc.
 * <p>
 * For more info check documentation of {@link HtmlElement}</p>
 */
@PublicAtsApi
public class RealHtmlElement extends HtmlElement {

    /**
     *  used by all HiddenHtmlXYZ classes for passing it to UiElementProperties's method checkTypeAndRules()
     */
    protected static final String[] RULES_DUMMY = new String[]{};

    private WebDriver               webDriver;

    public RealHtmlElement( UiDriver uiDriver,
                            UiElementProperties properties ) {

        super( uiDriver, properties );

        // get rules used for finding html element
        String[] matchingRules = properties.checkTypeAndRules( this.getClass().getSimpleName(),
                                                               "RealHtml",
                                                               RULES_DUMMY );

        // generate the XPath of this HTML element
        String locator = HtmlElementLocatorBuilder.buildXpathLocator( matchingRules,
                                                                      properties,
                                                                      new String[]{},
                                                                      "*" );
        properties.addInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, locator );

        webDriver = ( WebDriver ) ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * Get element attribute value
     * @return value of the attribute (style/class/...)
     */
    @Override
    @PublicAtsApi
    public String getAttributeValue(
                                     String attribute ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        return RealHtmlElementLocator.findElement( this ).getAttribute( attribute );
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

        new RealHtmlElementState( this ).waitToBecomeExisting();

        return RealHtmlElementLocator.findElement( this ).getCssValue( propertyName );
    }

    /**
     * Get innerText of the element
     * @return innerText of the element
     */
    @Override
    @PublicAtsApi
    public String getTextContent() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        return RealHtmlElementLocator.findElement( this ).getText();
    }

    /**
     * Set the content of the element
     * @param content the new content
     */
    @Override
    @PublicAtsApi
    public void setTextContent(
                                String content ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        element.sendKeys( content );
    }

    /**
     * Simulate mouse click action
     */
    @Override
    @PublicAtsApi
    public void click() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        try {
            WebElement element = RealHtmlElementLocator.findElement( this );
            try {
                element.click();
            } catch( ElementNotInteractableException enie ) {
                if( !UiEngineConfigurator.getInstance().isWorkWithInvisibleElements() ) {
                    throw enie;
                }
                ( ( JavascriptExecutor ) webDriver ).executeScript( "arguments[0].click()", element );
            }
        } catch( Exception e ) {
            throw new SeleniumOperationException( this, "click", e );
        }
    }

    /**
     * Simulate mouse double click action
     */
    @Override
    @PublicAtsApi
    public void doubleClick() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );

        new Actions( webDriver ).doubleClick( element ).perform();
    }

    /**
     * Simulate mouse right click action
     */
    @Override
    @PublicAtsApi
    public void rightClick() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );

        new Actions( webDriver ).contextClick( element ).perform();
    }

    /**
     * Simulate mouse over
     */
    @Override
    @PublicAtsApi
    public void mouseOver() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );

        // 1. option
        new Actions( webDriver ).moveToElement( element ).perform();

        // 2. option
        //        element.sendKeys( "" );

        // 3. option
        //        Mouse mouse = ( ( HasInputDevices ) webDriver ).getMouse();
        //        mouse.mouseMove( ( ( RemoteWebElement ) element ).getCoordinates() );

        // 4. option
        //        String javaScript = "var evObj = document.createEvent('MouseEvents');"
        //                            + "evObj.initMouseEvent(\"mouseover\",true, false, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);"
        //                            + "arguments[0].dispatchEvent(evObj);";
        //        JavascriptExecutor js = ( JavascriptExecutor ) webDriver;
        //        js.executeScript( javaScript, element );

        UiEngineUtilities.sleep();
    }

    /**
     * Simulate Enter key
     */
    @Override
    @PublicAtsApi
    public void pressEnterKey() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        if (webDriver instanceof PhantomJSDriver){
            element.sendKeys( Keys.ENTER );
        }
        else{
            element.sendKeys( Keys.RETURN );
        }
    }

    /**
     * Simulate Space key
     */
    @Override
    @PublicAtsApi
    public void pressSpaceKey() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        element.sendKeys( Keys.SPACE );
    }

    /**
     * Simulate Tab key
     */
    @Override
    @PublicAtsApi
    public void pressTabKey() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        element.sendKeys( Keys.TAB );
    }

    /**
     * Simulate Escape key
     */
    @Override
    @PublicAtsApi
    public void pressEscapeKey() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        element.sendKeys( Keys.ESCAPE );
    }

    /**
     * Drag and drop an element on top of other element
     * @param targetElement the target element
     */
    @Override
    @PublicAtsApi
    public void dragAndDropTo(
                               HtmlElement targetElement ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement source = RealHtmlElementLocator.findElement( this );
        WebElement target = RealHtmlElementLocator.findElement( targetElement );

        Actions actionBuilder = new Actions( webDriver );
        Action dragAndDropAction = actionBuilder.clickAndHold( source )
                                                .moveToElement( target, 1, 1 )
                                                .release( target )
                                                .build();
        dragAndDropAction.perform();

        // drops the source element in the middle of the target, which in some cases is not doing drop on the right place
        // new Actions( webDriver ).dragAndDrop( source, target ).perform();
    }

    /**
     * Scroll to this element, so it is viewable.
     * Should be used when working in large pages where scrolling is possible and needed.
     */
    @PublicAtsApi
    public void scrollTo() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        WebElement element = RealHtmlElementLocator.findElement( this );
        ( ( JavascriptExecutor ) webDriver ).executeScript( "arguments[0].scrollIntoView();", element );
    }
}
