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
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.elements.html.HtmlPrompt;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.internal.realbrowser.ExpectedPrompt;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;

/**
 * An HTML Prompt
 */
@PublicAtsApi
public class RealHtmlPrompt extends HtmlPrompt {

    private AbstractRealBrowserDriver driver;

    private ExpectedPrompt            expectedPrompt;

    public RealHtmlPrompt( UiDriver uiDriver ) {

        super( uiDriver );
        driver = ( ( AbstractRealBrowserDriver ) super.getUiDriver() );
    }

    @Override
    @PublicAtsApi
    public void clickOk(
                         String promptValue ) {

        expectedPrompt = new ExpectedPrompt( promptValue, true );
        driver.addExpectedPopup( expectedPrompt );
    }

    @Override
    @PublicAtsApi
    public void clickOk(
                         String expectedText,
                         String promptValue ) {

        expectedPrompt = new ExpectedPrompt( expectedText, promptValue, true );
        driver.addExpectedPopup( expectedPrompt );
    }

    @Override
    @PublicAtsApi
    public void clickCancel() {

        expectedPrompt = new ExpectedPrompt( null, false );
        driver.addExpectedPopup( expectedPrompt );
    }

    @Override
    @PublicAtsApi
    public void clickCancel(
                             String expectedText ) {

        expectedPrompt = new ExpectedPrompt( expectedText, null, false );
        driver.addExpectedPopup( expectedPrompt );
    }

    @Override
    @PublicAtsApi
    public void verifyProcessed() {

        long millis = UiEngineConfigurator.getInstance().getElementStateChangeDelay();
        long endTime = System.currentTimeMillis() + millis;
        do {
            if( !driver.containsExpectedPopup( expectedPrompt ) ) {
                return;
            }
            UiEngineUtilities.sleep();
        } while( endTime - System.currentTimeMillis() > 0 );

        throw new VerificationException( "Failed to verify the Prompt is processed within " + millis + " ms" );
    }

}
