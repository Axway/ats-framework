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

import java.awt.Container;
import java.awt.Window;
import java.net.URL;

import org.fest.swing.core.BasicRobot;
import org.fest.swing.fixture.ContainerFixture;
import org.fest.swing.fixture.WindowFixture;
import org.fest.swing.launcher.ApplicationLauncher;
import org.fest.swing.lock.ScreenLock;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.engine.SwingEngine;
import com.axway.ats.uiengine.exceptions.SwingException;

import netx.jnlp.JNLPFile;
import netx.jnlp.Launcher;
import netx.jnlp.cache.UpdatePolicy;
import netx.jnlp.runtime.ApplicationInstance;

/**
 * A driver used for working with a SWING application
 */
@PublicAtsApi
public class SwingDriver extends UiDriver {

    private Class<?>                                mainClassOfTestedApplication;

    protected String                                windowTitle;

    protected String                                jnlpLocation;
    protected boolean                               isJnlpCached;
    private ApplicationInstance                     jnlpAppInstance;

    /**
     * Current window container. Used also when closing application at the test end
     */
    protected WindowFixture<? extends Window>       windowFixture;

    /**
     * Current container context used for searching of other components
     */
    protected ContainerFixture<? extends Container> currentContainer;

    /**
    *
    * @param windowTitile the window title
    */
    protected SwingDriver( String windowTitile ) {

        this(null, windowTitile);
    }

    /**
     *
     * @param mainClassOfTestedApplication the class containing the "main" method
     */
    protected SwingDriver( Class<?> mainClassOfTestedApplication ) {

        this(mainClassOfTestedApplication, null);
    }

    /**
     *
     * @param mainClassOfTestedApplication the class containing the "main" method
     * @param windowTitile the window title
     */
    protected SwingDriver( Class<?> mainClassOfTestedApplication, String windowTitile ) {

        this.mainClassOfTestedApplication = mainClassOfTestedApplication;
        this.windowTitle = windowTitile;
    }

    /**
     *
     * @param jnlpLocation JNLP location. It can be local JNLP file path or a remote url
     * @param cacheEnabled whether the JNLP is cached or not
     */
    public SwingDriver( String jnlpLocation, boolean cacheEnabled ) {

        this.jnlpLocation = jnlpLocation;
        this.isJnlpCached = cacheEnabled;
    }

    @Override
    @PublicAtsApi
    public void start() {

        if (!StringUtils.isNullOrEmpty(jnlpLocation)) {

            Launcher jnlpLauncher = new Launcher();

            // if it's "true" the JNLP application is started in a sandboxed environment and FEST API is unable to locate it
            jnlpLauncher.setCreateAppContext(false);

            UpdatePolicy updatePolicy = UpdatePolicy.NEVER;
            if (!isJnlpCached) {
                updatePolicy = UpdatePolicy.ALWAYS;
            }
            jnlpLauncher.setUpdatePolicy(updatePolicy);

            try {
                if (jnlpLocation.toLowerCase().startsWith("http")) {
                    jnlpAppInstance = jnlpLauncher.launch(new URL(jnlpLocation));
                } else {
                    JNLPFile jnlpFile = new JNLPFile(new URL(jnlpLocation));
                    jnlpAppInstance = jnlpLauncher.launch(jnlpFile);
                }
            } catch (Exception e) {

                throw new SwingException("Couldn't load JNLP from '" + jnlpLocation + "'", e);
            }

            this.windowTitle = jnlpAppInstance.getTitle();
        }

        BasicRobot.robotWithCurrentAwtHierarchy().cleanUpWithoutDisposingWindows();
        if (this.mainClassOfTestedApplication != null) {

            ApplicationLauncher.application(this.mainClassOfTestedApplication).start();
        }

        // FailOnThreadViolationRepaintManager class forces a test failure if access to Swing components is not performed on the EDT.
        //   FailOnThreadViolationRepaintManager.install();

        getSwingEngine().goToWindow(this.windowTitle, false); // also initializes windowFixture

        // configure the robot
        windowFixture.robot.settings()
                           .delayBetweenEvents(UiEngineConfigurator.getInstance().getCommandDelay());
    }

    @Override
    @PublicAtsApi
    public void stop() {

        try {
            windowFixture.close();
        } catch (Exception e) {
            // if the current window is already closed
        }
        windowFixture.robot.cleanUpWithoutDisposingWindows();

        if (jnlpAppInstance != null && jnlpAppInstance.isRunning()) {
            jnlpAppInstance.destroy();
        }
    }

    /**
     * Cleans up any used resources (keyboard, mouse, open windows and <code>{@link ScreenLock}</code>) used.
     *
     * @param cleanResources if <code>true</code> release UI resources
     */
    @PublicAtsApi
    public void stop( boolean cleanResources ) {

        try {
            windowFixture.close();
        } catch (Exception e) {
            // if the current window is already closed
        }

        if (cleanResources) {
            windowFixture.robot.cleanUp();
        } else {
            windowFixture.robot.cleanUpWithoutDisposingWindows();
        }
    }

    /**
     *
     * @return new SwingEngine instance
     */
    @PublicAtsApi
    public SwingEngine getSwingEngine() {

        return new SwingEngine(this);
    }

    /**
     * Set the delay time between commands<br>
     * <b>NOTE:</b> It also sets this delay time as a default command delay time of {@link UiEngineConfigurator}
     *
     * @param commandDelay a value representing the delay time between commands (in milliseconds)
     */
    @PublicAtsApi
    public void setCommandDelay( int commandDelay ) {

        UiEngineConfigurator.getInstance().setCommandDelay(commandDelay);
        if (windowFixture != null) { // configure the robot - start() already invoked
            windowFixture.robot.settings().delayBetweenEvents(commandDelay);
        }
    }
}
