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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.axway.ats.uiengine.elements.UiLink;
import com.axway.ats.uiengine.exceptions.BadUiElementPropertyException;
import com.axway.ats.uiengine.exceptions.ErrorMatchingElementRules;

/** rules:
*       id
*       name
*       title
*/
public class Test_Link extends HtmlEngineBaseTest {

    @DataProvider( name = "links")
    public Object[][] provideElementIds() {

        // The commented links do not work as they have href only and
        // we are currently unable to find link by href only
        return new Object[][]{ { "link3" },
                               { "link7" },
                               { "link8" },
                               { "link9" },
                               { "link10" },
                               { "link11" },
                               { "link20" },
                               { "link21" },
                               { "linkTitle1" },
                               { "linkTitle2" },
                               { "linkTitle3" },
                               { "linkTitle4" },
                               { "linkTitle5" }, };
    }

    @Parameters( { "browser", "testFilesLocation" })
    @BeforeMethod
    public void beforeMethod(
                              @Optional( "FF") String browser,
                              @Optional( "") String testFilesLocation ) {

        setTestPage("link.htm", "link", browser, testFilesLocation);
    }

    @Test( dataProvider = "links")
    public void testAllMethods(
                                String linkMapId ) throws Exception {

        UiLink link = webEngine.getLink(linkMapId);
        link.click();
    }

    @Test
    public void notExistingElement() throws Exception {

        UiLink link = webEngine.getLink("link40");
        webEngine.getUtilsElementState(link).verifyNotExist();
    }

    @Test
    public void redirectToAnotherPage() throws Exception {

        String pageSource = webEngine.getPageSource();
        Logger log = LogManager.getLogger(this.getClass());
        log.info("Current page source: " + pageSource);

        UiLink linkOnInitialPage = webEngine.getLink("link2");
        linkOnInitialPage.click();

        // verify we left the initial page
        webEngine.getUtilsElementState(linkOnInitialPage).verifyNotExist();

        // go back to the initial page
        UiLink linkOnNewPage = webEngine.getLink("link");
        linkOnNewPage.click();

        // verify we are back at the initial page
        webEngine.getUtilsElementState(linkOnInitialPage).verifyExist();
    }

    @Test( expectedExceptions = ErrorMatchingElementRules.class)
    public void badRules() throws Exception {

        webEngine.getLink("link50");
    }

    @Test( expectedExceptions = BadUiElementPropertyException.class, expectedExceptionsMessageRegExp = "You can not construct a .*HtmlLink.*")
    public void wrongType() throws Exception {

        webEngine.getLink("link60");
    }

}
