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
package com.axway.ats.uiengine;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;

/**
 * <p></p>A driver operating over
 * <a href="http://phantomjs.org/">PhantomJS</a> browser
 * >/p?
 * Will be removed in future versions.
 * <br>
 * This is a headless(no UI) browser.<br />
 * <em>Note</em> that PahntomJS has no recent updates so it is deprecated and will be removed as supported driver.
 *
 */
@PublicAtsApi
@Deprecated( /* since="4.0.7", this attribute is Java 9+ feature */)
public class PhantomJsDriver extends AbstractRealBrowserDriver {

    private static Logger         log                        = LogManager.getLogger(PhantomJsDriver.class);

    /**
     * <pre>
     * Usage:  <b>System.setProperty( PhantomJSDriver.SETTINGS_PROPERTY, "settingName: value" );</b>
     *   or multiple: System.setProperty( PhantomJSDriver.SETTINGS_PROPERTY, "{settingName: value}, {other setting: value 2}" );
     *   for example: System.setProperty( PhantomJSDriver.SETTINGS_PROPERTY, "userAgent: Mozilla/5.0 (Windows NT 5.1; rv:24.0) Gecko/20100101 Firefox/24.0" );
     *
     * Check <a href="https://github.com/ariya/phantomjs/wiki/API-Reference-WebPage#settings-object">here</a> for the available settings (https://github.com/ariya/phantomjs/wiki/API-Reference-WebPage#settings-object)
     * </pre>
     */
    @PublicAtsApi
    public static final String    SETTINGS_PROPERTY          = "phantomjs.page.settings";

    /**
     * <pre>
     * Usage:  <b>System.setProperty( PhantomJSDriver.CUSTOM_HEADERS_PROPERTY, "header name: value" );</b>
     *   or multiple: System.setProperty( PhantomJSDriver.CUSTOM_HEADERS_PROPERTY, "{header X: value X}, {header Y: value Y}" );
     *   for example: System.setProperty( PhantomJSDriver.CUSTOM_HEADERS_PROPERTY, "Accept-Language: en-US,en;q=0.5" );
     * </pre>
     */
    @PublicAtsApi
    public static final String    CUSTOM_HEADERS_PROPERTY    = "phantomjs.page.customHeaders";

    /**
     * HttpOnly cookie names, separated with commas
     * eg: System.setProperty( PhantomJSDriver.HTTP_ONLY_COOKIES_PROPERTY, "JSESSIONID,SSID" );
     */
    @PublicAtsApi
    public static final String    HTTP_ONLY_COOKIES_PROPERTY = "phantomjs.page.httpOnlyCookies";

    /**
     * SSL protocol for the connection handshake. Supported values are: SSLv3, SSLv2, TLSv1, any (default)
     * eg: System.setProperty( PhantomJSDriver.SSL_PROTOCOL_PROPERTY, "TLSv1" );
     */
    @PublicAtsApi
    public static final String    SSL_PROTOCOL_PROPERTY      = "phantomjs.ssl.protocol";

    protected final static String cookiesFile                = AtsSystemProperties.SYSTEM_USER_TEMP_DIR
                                                               + "/phantomjs_cookies.txt";

    protected PhantomJsDriver( String url,
                               String browserPath ) {

        super(AbstractRealBrowserDriver.BrowserType.PhantomJS, url, browserPath);
    }

    public static List<Cookie> getHttpOnlyCookies() {

        List<Cookie> cookies = new ArrayList<Cookie>();
        if (System.getProperty(PhantomJsDriver.HTTP_ONLY_COOKIES_PROPERTY) != null) {

            String[] cookieNames = System.getProperty(PhantomJsDriver.HTTP_ONLY_COOKIES_PROPERTY)
                                         .split(",");
            try {
                String cookiesFileContent = IoUtils.streamToString(IoUtils.readFile(PhantomJsDriver.cookiesFile));
                for (String cookieName : cookieNames) {
                    int cookieIndex = cookiesFileContent.indexOf(cookieName + "=");
                    if (cookieIndex >= 0) {

                        int endIndex = cookiesFileContent.indexOf("\\0", cookieIndex);
                        if (endIndex < 0) {
                            endIndex = cookiesFileContent.indexOf(')', cookieIndex);
                        }
                        String wholeCookieData = cookiesFileContent.substring(cookieIndex, endIndex);
                        String[] cookieParts = wholeCookieData.split(";\\s+");

                        String value = cookieParts[0].substring(cookieParts[0].indexOf('=') + 1);
                        String path = null;
                        String domain = null;
                        boolean isSecure = false;
                        for (String cp : cookieParts) {
                            if ("secure".equalsIgnoreCase(cp)) {
                                isSecure = true;
                            } else if (cp.startsWith("path=")) {
                                path = cp.substring(cp.indexOf('=') + 1);
                            } else if (cp.startsWith("domain=")) {
                                domain = cp.substring(cp.indexOf('=') + 1);
                            }
                        }
                        cookies.add(new Cookie(cookieName, value, domain, path, null, isSecure));
                    }
                }
            } catch (Exception e) {
                log.error("Couldn't parse HttpOnly cookeis, from the temp file: " + cookiesFile, e);
            }
        }
        return cookies;
    }

}
