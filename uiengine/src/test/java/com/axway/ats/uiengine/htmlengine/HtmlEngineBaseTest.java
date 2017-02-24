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
package com.axway.ats.uiengine.htmlengine;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import com.axway.ats.uiengine.AbstractHtmlDriver;
import com.axway.ats.uiengine.BaseTest;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.engine.AbstractHtmlEngine;

public class HtmlEngineBaseTest extends BaseTest {

    private static String               testPage;

    protected static AbstractHtmlEngine webEngine;
    protected static AbstractHtmlDriver browserDriver;
    private static String               lastBrowser    = "";
    private static String               lastMapSection = "";

    protected static void setTestPage(
                                       String testPage,
                                       String mapSection,
                                       String browser,
                                       String testFilesLocation ) {

        if( testFilesLocation.equals( "" ) ) {
            HtmlEngineBaseTest.testPage = "file:///" + getResourcesFolder() + "htmlfiles/" + testPage;
        } else {
            HtmlEngineBaseTest.testPage = testFilesLocation + "htmlfiles/" + testPage;
        }

        if( !lastBrowser.equals( browser ) ) {

            if( browserDriver != null ) {
                browserDriver.stop();
            }
            if( browser.equalsIgnoreCase( "FF" ) ) {

                browserDriver = UiDriver.getFirefoxDriver( HtmlEngineBaseTest.testPage );
            } else if( browser.equalsIgnoreCase( "IE" ) ) {

                browserDriver = UiDriver.getInternetExplorerDriver( HtmlEngineBaseTest.testPage );
            } else if( browser.equalsIgnoreCase( "HIDDEN" ) ) {

                browserDriver = UiDriver.getHiddenBrowserDriver( HtmlEngineBaseTest.testPage );
            } else {

                throw new RuntimeException( "Unknown browser '" + browser + "'" );
            }
            browserDriver.start();
            webEngine = browserDriver.getHtmlEngine();
        }

        webEngine.goToPage( HtmlEngineBaseTest.testPage );

        if( mapSection != null && !lastMapSection.equals( mapSection ) ) {
            webEngine.loadMapFile( "HtmlEngineElements.map", mapSection );
            lastMapSection = mapSection;
        }
        lastBrowser = browser;
    }

    @BeforeSuite
    public void beforeSuiteHtmlEngineBaseTest() throws Exception {

        UiEngineConfigurator configurator = UiEngineConfigurator.getInstance();

        // set the base folder with the map files
        // configurator.setMapFilesBaseDir( "resources/com/axway/ats/uiengine/maps" );

        // overwrite some configuration properties for faster test execution
        configurator.setCommandDelay( -1 );
        configurator.setElementStateChangeDelay( 2000 );
        configurator.setHighlightElements( false );
    }

    @AfterSuite
    public void afterSuiteHtmlEngineBaseTest() throws Exception {

        browserDriver.stop();
    }
}
