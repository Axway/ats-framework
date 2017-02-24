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

/**
 * A driver operating over Safari browser
 * 
 * 
 * <br/><br/>
 * <b>User guide</b> pages related to this class:<br/> 
 * <a href="https://techweb.axway.com/confluence/display/ATS/UI+Engine">UI Engine basics</a>
 * and
 * <a href="https://techweb.axway.com/confluence/display/ATS/Automating+HTML+and+Javascript+application">testing HTML applications</a>
 */
public class SafariDriver extends AbstractRealBrowserDriver {

    /**
     * To get SafariDriver instance use UiDriver.getSafariDriver()
     *
     * @param url the target application URL
     */
    protected SafariDriver( String url ) {

        super( AbstractRealBrowserDriver.BrowserType.Safari, url, null );
    }

    /**
     * To get SafariDriver instance use UiDriver.getSafariDriver()
     *
     * @param url the target application URL
     * @param remoteSeleniumURL the remote selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     */
    protected SafariDriver( String url,
                            String remoteSeleniumURL ) {

        super( AbstractRealBrowserDriver.BrowserType.Safari, url, null, remoteSeleniumURL );
    }
}
