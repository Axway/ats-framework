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

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.AbstractRealBrowserDriver;
import com.axway.ats.uiengine.PhantomJsDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlFileDownloader;
import com.axway.ats.uiengine.elements.html.HtmlLink;
import com.axway.ats.uiengine.engine.RealHtmlEngine;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * An HTML Link
 * @see RealHtmlElement
 */
@PublicAtsApi
public class RealHtmlLink extends HtmlLink {

    //private WebDriver webDriver;

    public RealHtmlLink( UiDriver uiDriver,
                         UiElementProperties properties ) {

        super(uiDriver, properties);
        String[] matchingRules = properties.checkTypeAndRules(this.getClass().getSimpleName(),
                                                              "RealHtml",
                                                              RealHtmlElement.RULES_DUMMY);

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator(matchingRules,
                                                                   properties,
                                                                   new String[]{ "href" },
                                                                   "a");
        properties.addInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath);

        //webDriver = ( WebDriver ) ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * Click the link
     */
    @Override
    @PublicAtsApi
    public void click() {

        doClick();
    }

    /**
     * Click link and download file
     */
    @PublicAtsApi
    public void clickAndDownloadFile() {

        log.info("File will be downloaded in "
                 + UiEngineConfigurator.getInstance().getBrowserDownloadDir());

        if (getUiDriver() instanceof PhantomJsDriver) {

            try {
                HtmlFileDownloader fileDownloader = new HtmlFileDownloader((PhantomJsDriver) getUiDriver());
                RealHtmlEngine htmlEngine = ((PhantomJsDriver) getUiDriver()).getHtmlEngine();
                fileDownloader.downloadFileFromLink(htmlEngine.getCurrentUrl(),
                                                    htmlEngine.getElement(getElementProperties()));
            } catch (Exception e) {

                throw new SeleniumOperationException(this, "downloadFile", e);
            }
        } else {

            // Just calls click() method and the browser automatically will download the file
            doClick();
        }

        UiEngineUtilities.sleep();
        log.info("File download has started. Please check for completion.");
    }

    private void doClick() {

        new RealHtmlElementState(this).waitToBecomeExisting();
        RealHtmlElementLocator.findElement(this).click();

        UiEngineUtilities.sleep();

        ((AbstractRealBrowserDriver) super.getUiDriver()).handleExpectedPopups();
    }
}
