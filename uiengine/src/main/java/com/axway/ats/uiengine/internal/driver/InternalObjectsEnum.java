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

/**
 * For internal use only. May be changed without notice.
 */
public enum InternalObjectsEnum {

    /**
     * instance of engine operating over HTML pages
     */
    Engine,
    /**
     * Web Driver (Selenium web drivers, PhantomJS web driver, Appium driver)
     */
    WebDriver,
    /**
     * Object for internal use in some HTML-related classes
     * It is accessible through @AbstractHtmlDriver.getInternalObject() method
     */
    Object;

    public static InternalObjectsEnum getEnum(
                                               String name ) {

        for( InternalObjectsEnum e : InternalObjectsEnum.values() ) {
            if( name.equals( e.name() ) ) {
                return e;
            }
        }
        return null;
    }
}
