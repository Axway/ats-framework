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

import com.axway.ats.common.PublicAtsApi;

/**
 * A driver operating over Firefox browser
 *
 *
 * <br/><br/>
 * <b>User guide</b> pages related to this class:<br/>
 * <a href="https://techweb.axway.com/confluence/display/ATS/UI+Engine">UI Engine basics</a>
 * and
 * <a href="https://techweb.axway.com/confluence/display/ATS/Automating+HTML+and+Javascript+application">testing HTML applications</a>
 */
@PublicAtsApi
public class FirefoxDriver extends AbstractRealBrowserDriver {

    private String profileDirectory;
    private String profileName;

    /**
     * To get FirefoxDriver instance use UiDriver.getFirefoxDriver()
     * @param url the target application URL
     */
    protected FirefoxDriver( String url ) {

        super( AbstractRealBrowserDriver.BrowserType.FireFox, url, null );
    }

    /**
     * To get FirefoxDriver instance use UiDriver.getFirefoxDriver()
     * @param url the target application URL
     * @param browserPath the browser start path or the remote selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     */
    protected FirefoxDriver( String url,
                             String browserPath ) {

        super( AbstractRealBrowserDriver.BrowserType.FireFox, url, browserPath );
    }

    /**
     * To get FirefoxDriver instance use UiDriver.getFirefoxDriver()
     * @param url the target application URL
     * @param browserPath the browser start path
     * @param remoteSeleniumURL the remote selenium hub URL (eg. http://10.11.12.13:4444/wd/hub/)
     */
    protected FirefoxDriver( String url,
                             String browserPath,
                             String remoteSeleniumURL ) {

        super( AbstractRealBrowserDriver.BrowserType.FireFox, url, browserPath, remoteSeleniumURL );
    }

    /**
     * @param profileDirectory Firefox profile directory (absolute path)
     */
    public void setProfileDirectory(
                                     String profileDirectory ) {

        this.profileDirectory = profileDirectory;
    }

    /**
     * @return the Firefox profile directory (absolute path)
     */
    public String getProfileDirectory() {

        return profileDirectory;
    }

    /**
     * Set profile name
     * <pre>
     * <b>NOTE</b>: If there is a problem sending a profile to a remote Selenium hub
     * try to specify the profile name in the selenium hub start command:
     *      <i>java -jar selenium-server-standalone.jar -Dwebdriver.firefox.profile="profile name"</i>
     * </pre>
     *
     * @param profileName Firefox profile name
     */
    public void setProfileName(
                                String profileName ) {

        this.profileName = profileName;
    }

    /**
     * @return the Firefox profile name
     */
    public String getProfileName() {

        return profileName;
    }
}
