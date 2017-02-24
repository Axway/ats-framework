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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.process.LocalProcessExecutor;
import com.axway.ats.core.process.model.IProcessExecutor;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.uiengine.AbstractRealBrowserDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlFileBrowse;
import com.axway.ats.uiengine.exceptions.RobotException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.UiEngineUtilities;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * An HTML File Select Button
 * @see RealHtmlElement
 */
@PublicAtsApi
public class RealHtmlFileBrowse extends HtmlFileBrowse {

    private WebDriver           webDriver;
    private UiElementProperties properties;

    public RealHtmlFileBrowse( UiDriver uiDriver, UiElementProperties properties ) {

        super( uiDriver, properties );
        this.properties = properties;

        String[] matchingRules = properties.checkTypeAndRules( this.getClass().getSimpleName(), "RealHtml",
                                                               RealHtmlElement.RULES_DUMMY );

        // generate the XPath of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator( matchingRules, properties,
                                                                    new String[]{ "file" }, "input" );
        properties.addInternalProperty( HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath );

        webDriver = ( WebDriver ) ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).getInternalObject( InternalObjectsEnum.WebDriver.name() );
    }

    /**
     * Get the File Select Button value
     * @return
     */
    @Override
    @PublicAtsApi
    public String getValue() {

        new RealHtmlElementState( this ).waitToBecomeExisting();

            return RealHtmlElementLocator.findElement( this ).getAttribute( "value" );
    }

    /**
     * Set the File Select Button value
     *
     * @param value the value to set
     */
    @Override
    @PublicAtsApi
    public void setValue( String value ) {

        new RealHtmlElementState( this ).waitToBecomeExisting();

        setFileInputValue( webDriver, value );

        UiEngineUtilities.sleep();
    }

    /**
     * This method allows you to type in dialog windows. This option is available only for Windows OS
     *
     * @param path add the location of the file, you want to type in the dialog window
     * @throws Exception
     */
    @PublicAtsApi
    public void setValueUsingNativeDialog( String path ) throws Exception {

        if( !OperatingSystemType.getCurrentOsType().isWindows() ) {
            throw new RuntimeException( "This method is only available for Windows machines!" );
        }

        // check if the file exist
        if( !new File( path ).exists() ) {
            throw new FileNotFoundException( "File path \"" + path + "\" is wrong or does not exist!" );
        }

        // ats_file_upload.exe location
        String uploadFileDestination = System.getProperty( "user.dir" ) + "\\ats_file_upload.exe";
        // native window name
        String windowName;
        log.info( "Using native " + path + " to work with native browser dialogs" );

        // check if the ats_file_upload.exe file is already created
        if( !new File( uploadFileDestination ).exists() ) {
            OutputStream os = null;
            InputStream is = null;
            try {
                // get the ats_file_upload.exe file, located in the ats-uiengine.jar
                is = getClass().getClassLoader().getResourceAsStream( "binaries/ats_file_upload.exe" );
                if( is == null ) {

                    throw new FileNotFoundException( "The 'ats_file_upload.exe' file is not found in ats-uiengine.jar!" );
                }

                File uploadFile = new File( uploadFileDestination );
                os = new FileOutputStream( uploadFile );
                IOUtils.copy( is, os );

            } finally {
                IoUtils.closeStream( is );
                IoUtils.closeStream( os );
            }
        }

        if( webDriver instanceof FirefoxDriver ) {
            windowName = " \"File Upload\" ";
        } else if( webDriver instanceof ChromeDriver ) {
            windowName = " \"Open\" ";
        } else {
            throw new RobotException( "Not Implemented for your browser! Currently Firefox and "
                                      + "Chrome are supported." );
        }

        // add the browse button properties
        ( ( AbstractRealBrowserDriver ) super.getUiDriver() ).getHtmlEngine()
                                                             .getElement( properties )
                                                             .click();

        // run the ats_file_upload.exe file
        IProcessExecutor proc = new LocalProcessExecutor( HostUtils.LOCAL_HOST_IPv4,
                                                          uploadFileDestination + windowName + path );
        proc.execute();

        // check if there is any error, while executing the ats_file_upload.exe file
        if( proc.getExitCode() != 0 ) {
            log.error( "AutoIT process for native browser interaction failed with exit code: "
                       + proc.getExitCode() + ";" );
            log.error( "Output stream data: " + proc.getStandardOutput() + ";" );
            log.error( "Error stream data: " + proc.getErrorOutput() );
            throw new RobotException( "AutoIT process for native browser interaction failed." );
        }
    }

    /**
     * Verify the File Select Button value is as specified
     *
     * @param expectedValue
     */
    @Override
    @PublicAtsApi
    public void verifyValue( String expectedValue ) {

        expectedValue = expectedValue.trim();

        String actualText = getValue().trim();
        if( !actualText.equals( expectedValue ) ) {
            throw new VerifyEqualityException( expectedValue, actualText, this );
        }
    }

    /**
     * Verify the File Select Button value is NOT as specified
     *
     * @param notExpectedValue
     */
    @Override
    @PublicAtsApi
    public void verifyNotValue( String notExpectedValue ) {

        String actualText = getValue();
        if( actualText.equals( notExpectedValue ) ) {
            throw new VerifyNotEqualityException( notExpectedValue, this );
        }
    }
}
