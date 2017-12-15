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

import java.io.IOException;
import java.net.URL;

import org.openqa.selenium.WebDriver;

import com.axway.ats.action.http.FileTransferHttpClient;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.AbstractHtmlDriver;
import com.axway.ats.uiengine.PhantomJsDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.exceptions.UiElementException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;

public class HtmlFileDownloader {

    private WebDriver webDriver;
    private String    downloadDir                     = UiEngineConfigurator.getInstance()
                                                                            .getBrowserDownloadDir();

    private boolean   mimicWebDriverCookieState       = true;
    private int       httpStatusOfLastDownloadAttempt = 0;

    public HtmlFileDownloader( AbstractHtmlDriver browserDriver ) {

        this((WebDriver) browserDriver.getInternalObject(InternalObjectsEnum.WebDriver.name()));
    }

    public HtmlFileDownloader( WebDriver driverObject ) {

        this.webDriver = driverObject;
    }

    /**
     *
     * @return The download directory where the files will be downloaded to.
     */
    public String getDownloadDir() {

        return this.downloadDir;
    }

    /**
     *
     * @param downloadDir The download directory where the files will be downloaded to.
     */
    public void setDownloadDir( String downloadDir ) {

        this.downloadDir = IoUtils.normalizeDirPath(downloadDir);
    }

    /**
     * Download the file specified in the href attribute of a HtmlElement
     *
     * @param linkElement
     * @return
     * @throws Exception
     */
    public String downloadFileFromLink( String pageUrl, HtmlElement linkElement ) throws Exception {

        if (pageUrl != null) {
            URL url = new URL(pageUrl);
            return downloadFile(url.getProtocol() + "://" + url.getHost()
                                + linkElement.getAttributeValue("href"));
        }

        return downloadFile(linkElement.getAttributeValue("href"));
    }

    /**
     * Gets the HTTP status code of the last download file attempt
     *
     * @return
     */
    public int getHTTPStatusOfLastDownloadAttempt() {

        return this.httpStatusOfLastDownloadAttempt;
    }

    /**
     * Mimic the cookie state of WebDriver (Defaults to true)
     * This will enable you to access files that are only available when logged in.
     * If set to false the connection will be made as an anonymous user
     *
     * @param value
     */
    public void mimicWebDriverCookieState( boolean value ) {

        this.mimicWebDriverCookieState = value;
    }

    /**
     * Perform the file/image download.
     *
     * @param downloadUrl
     * @return download file absolute path
     * @throws IOException
     * @throws FileTransferClientException
     */
    public String downloadFile( String downloadUrl ) throws IOException {

        if (StringUtils.isNullOrEmpty(downloadUrl)) {
            throw new UiElementException("The element you have specified does not link to anything!");
        }

        URL url = new URL(downloadUrl);
        String fileName = url.getPath();
        String remoteFilePath = null;
        if (fileName.indexOf('/') > -1) {
            int separator = fileName.lastIndexOf('/');
            remoteFilePath = fileName.substring(0, separator + 1);
            fileName = fileName.substring(separator + 1);
        }

        FileTransferHttpClient transferClient = new FileTransferHttpClient();
        transferClient.connect(url.getHost(), null, null);

        if (this.mimicWebDriverCookieState) {

            for (org.openqa.selenium.Cookie cookie : this.webDriver.manage().getCookies()) {

                transferClient.addCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(),
                                         cookie.getPath(), cookie.getExpiry(), cookie.isSecure());
            }
            if (this.webDriver instanceof org.openqa.selenium.phantomjs.PhantomJSDriver
                && System.getProperty(PhantomJsDriver.HTTP_ONLY_COOKIES_PROPERTY) != null) {

                for (org.openqa.selenium.Cookie cookie : PhantomJsDriver.getHttpOnlyCookies()) {

                    transferClient.addCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(),
                                             cookie.getPath(), cookie.getExpiry(), cookie.isSecure());
                }
            }
        }

        transferClient.downloadFile(this.downloadDir + fileName, remoteFilePath, fileName);

        return this.downloadDir + fileName;
    }
}
