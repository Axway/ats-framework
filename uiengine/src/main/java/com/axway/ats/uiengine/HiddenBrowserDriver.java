/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.uiengine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver.TargetLocator;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.engine.HiddenHtmlEngine;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.RefreshHandler;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;

/**
 * A non-visual Web driver operating over
 * <a href="http://htmlunit.sourceforge.net/">HTML Unit</a> browser.
 * Using Selenium <a href="https://github.com/SeleniumHQ/htmlunit-driver">htmlunit-driver</a> WebDriver.
 *
 * <br>
 * This is a headless (no UI) browser.
 */
@PublicAtsApi
public class HiddenBrowserDriver extends AbstractHtmlDriver {

    private static Logger      log                    = LogManager.getLogger(HiddenBrowserDriver.class);

    public static final String ALLOW_META_REFRESH_TAG = "htmlunit.allow.meta.refresh.tag";

    private String             url;

    private BrowserVersion     browserVersion;

    protected HtmlUnitDriver   webDriver;

    /**
     * To get HiddenBrowserDriver instance use UiDriver.getHiddenBrowserDriver()
     * @param url application url
     */
    protected HiddenBrowserDriver( String url ) {

        this.url = url;
        this.browserVersion = BrowserVersion.FIREFOX_60;
    }

    protected HiddenBrowserDriver( String url, BrowserVersion browserVersion ) {

        this.url = url;
        this.browserVersion = browserVersion;
    }

    @Override
    @PublicAtsApi
    public HiddenHtmlEngine getHtmlEngine() {

        if (webDriver == null) {
            throw new IllegalStateException("Browser driver in not initialized. Either start() method is not invoked or browser initialization had failed.");
        }

        return new HiddenHtmlEngine(this);
    }

    @Override
    @PublicAtsApi
    public void start() {

        webDriver = new HtmlUnitDriver(this.browserVersion);
        webDriver.setJavascriptEnabled(true);

        setProxyIfAvailable();

        fixHtmlUnitBehaviour();

        log.info("Opening URL: " + url);
        webDriver.get(url);
    }

    private void setProxyIfAvailable() {

        if (!StringUtils.isNullOrEmpty(AtsSystemProperties.SYSTEM_HTTP_PROXY_HOST)
            && !StringUtils.isNullOrEmpty(AtsSystemProperties.SYSTEM_HTTP_PROXY_PORT)) {
            webDriver.setProxy(AtsSystemProperties.SYSTEM_HTTP_PROXY_HOST,
                               Integer.parseInt(AtsSystemProperties.SYSTEM_HTTP_PROXY_PORT));
        }
    }

    @Override
    @PublicAtsApi
    public void stop() {

        if (webDriver == null) {
            log.warn("Invoked HiddenBrowserDriver.stop() before start()");
        } else {
            webDriver.quit();
        }
    }

    /**
     * <b>NOTE:</b> This method should not be used directly into the test scripts.
     * The implementation may be changed by the Automation Framework Team without notice.
     * @return Internal Object
     */
    public Object getInternalObject( String objectName ) {

        // NOTE: we use a String argument 'objectName' not directly an InternalObjectsEnum object, because we want to
        // hide from the end users this method and his usage

        switch (InternalObjectsEnum.getEnum(objectName)) {

            case WebDriver:

                //returns current Selenium Web Driver
                return this.webDriver;
            default:
                break;
        }
        return null;
    }

    /**
     * Fixing refresh handler to skip Refresh meta tags
     * Allowing connections to any host, regardless of whether they have valid certificates or not
     * Fixing JSESSIONID cookie value
     * Some applications expect double quotes in the beginning and at the end of the JSESSIONID cookie value
     */
    private void fixHtmlUnitBehaviour() {

        Field webClientField = null;
        boolean fieldAccessibleState = false;
        try {
            TargetLocator targetLocator = webDriver.switchTo();
            webClientField = targetLocator.getClass().getDeclaringClass().getDeclaredField("webClient");
            fieldAccessibleState = webClientField.isAccessible();
            webClientField.setAccessible(true);
            final WebClient webClient = (WebClient) webClientField.get(targetLocator.defaultContent());

            // Allowing connections to any host, regardless of whether they have valid certificates or not
            webClient.getOptions().setUseInsecureSSL(true);

            // Set Http connection timeout (in milliseconds). The default value is 90 seconds, because in Firefox >= 16
            // the "network.http.connection-timeout" property is 90. But this value is not enough for some cases.
            // NOTE: use 0 for infinite timeout
            webClient.getOptions().setTimeout(5 * 60 * 1000);

            webClient.getOptions().setRedirectEnabled(true);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setThrowExceptionOnScriptError(true);
            webClient.getOptions().setPrintContentOnFailingStatusCode(true);

            // Hide CSS Warnings
            webClient.setCssErrorHandler(new SilentCssErrorHandler());

            // Suppress warnings like: "Expected content type ... but got ..."
            webClient.setIncorrectnessListener(new IncorrectnessListener() {

                //                private final Log log = LogFactory.getLog( this.getClass() );

                @Override
                public void notify( final String message, final Object origin ) {

                    //                    log.warn( message );
                }
            });

            if (!Boolean.parseBoolean(System.getProperty(ALLOW_META_REFRESH_TAG))) {

                /*
                 * Fix for refresh meta tags eg. "<meta http-equiv="refresh" content="300">"
                 * The default refresh handler is with Thread.sleep(refreshSecondsFromMetaTag) in the main thread!!!
                     *
                     * Maybe we should check and test this handler: webClient.setRefreshHandler( new ThreadedRefreshHandler() );
                 */
                webClient.setRefreshHandler(new RefreshHandler() {

                    @Override
                    public void handleRefresh( Page page, URL url, int seconds ) throws IOException {

                    }
                });
            }

            /*
             * Fix JSessionId
             */

            // WebConnectionWrapper constructs a WebConnection object wrapping the connection of the WebClient
            // and places itself (in the constructor) as connection of the WebClient.
            new WebConnectionWrapper(webClient) {

                public WebResponse getResponse( WebRequest request ) throws IOException {

                    Cookie jsCookie = webClient.getCookieManager().getCookie("JSESSIONID");
                    if (jsCookie != null && (!jsCookie.getValue().startsWith("\"")
                                             && !jsCookie.getValue().endsWith("\""))) {

                        Cookie newCookie = new Cookie(jsCookie.getDomain(), jsCookie.getName(),
                                                      "\"" + jsCookie.getValue() + "\"", jsCookie.getPath(),
                                                      jsCookie.getExpires(), jsCookie.isSecure());

                        webClient.getCookieManager().removeCookie(jsCookie);
                        webClient.getCookieManager().addCookie(newCookie);
                    }
                    return super.getResponse(request);
                }
            };
        } catch (Exception e) {

            throw new SeleniumOperationException("Error retrieving internal Selenium web client", e);
        } finally {

            if (webClientField != null) {
                webClientField.setAccessible(fieldAccessibleState);
            }
        }
    }

}
