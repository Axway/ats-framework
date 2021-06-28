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
package com.axway.ats.uiengine.internal.driver;

import org.fest.swing.fixture.ContainerFixture;
import org.fest.swing.fixture.WindowFixture;

import com.axway.ats.uiengine.SwingDriver;

/**
 * <strong>ATS Internal</strong> Might be changed at any time
 * Class to hide non-public methods
 */
public class SwingDriverInternal extends SwingDriver {

    public SwingDriverInternal( Class<?> mainClassOfTestedApplication ) {

        this( mainClassOfTestedApplication, null );
    }

    /**
     *
     * @param mainClassOfTestedApplication the class containing the "main" method
     * @param windowTitle the window title
     */
    public SwingDriverInternal( Class<?> mainClassOfTestedApplication,
                                String windowTitle ) {

        super( mainClassOfTestedApplication, windowTitle );
    }

    /**
    *
    * @param windowTitle the window title
    */
    public SwingDriverInternal( String windowTitle ) {

        super( windowTitle );
    }

    /**
    *
    * @param jnlpLocation JNLP location. It can be local JNLP file path or a remote url
    * @param cacheEnabled whether the JNLP is cached or not
    */
    public SwingDriverInternal( String jnlpLocation,
                                boolean cacheEnabled ) {

        super( jnlpLocation, cacheEnabled );
    }

    /**
     * <strong>Internal method.</strong> Might be changed at any time.
     * Should not be used from tests<br />
     */
    public WindowFixture<?> getWindowFixture() {

        return windowFixture;
    }

    /**
     * <strong>Internal method.</strong> Might be changed at any time.
     * Should not be used from tests<br />
     * Affects current container to search too.
     */
    public void setWindowFixture(
                                  WindowFixture<?> windowFixture ) {

        // changes current container to search too
        this.windowFixture = windowFixture;
        this.currentContainer = windowFixture;
    }

    /**
     * <strong>Internal method.</strong> Might be changed at any time.
     * Should not be used from tests<br />
     */
    public ContainerFixture<?> getActiveContainerFixture() {

        return currentContainer;
    }

    /**
     * <strong>Internal method.</strong> Might be changed at any time.
     * Should not be used from tests<br />
     * Changes current container as context to search for elements
     */
    public void setActiveContainerFixture(
                                           ContainerFixture<?> containerFixture ) {

        this.currentContainer = containerFixture;
    }

    public String getMainWindowTitle() {

        return windowTitle;
    }

}
