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
package com.axway.ats.uiengine.elements;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.exceptions.ErrorMatchingElementRules;

/**
 * An abstraction of an element UI Engine can work with
 */
public abstract class UiElement {

    private UiDriver              uiDriver;
    /**
     * The array of property names to actually use for matching concrete element - based on largest match in map file
     */
    private String[]              propsToUseForMatch;

    /**
     * Actual property names and values to match
     */
    protected UiElementProperties properties;

    protected Logger              log;

    public UiElement( UiDriver uiDriver, UiElementProperties properties ) {

        this.uiDriver = uiDriver;
        this.properties = properties;

        log = LogManager.getLogger(this.getClass());
    }

    public UiDriver getUiDriver() {

        return uiDriver;
    }

    public UiElementProperties getElementProperties() {

        return properties;
    }

    public String getElementProperty( String name ) {

        return properties.getProperty(name);
    }

    /*
     * returns description of the element
     */
    @Override
    public String toString() {

        // Check if there are no properties - Alert,Prompt,Confirm elements for example
        if (properties == null) {
            return "Element " + this.getClass().getSimpleName();
        }

        // get the properties in the form '{key1=value1, key2=value2}'
        String allProperties = properties.toString();
        // remove the leading and trailing brackets
        allProperties = allProperties.substring(1);
        allProperties = allProperties.substring(0, allProperties.length() - 1);

        return "Element " + this.getClass().getSimpleName() + ": " + allProperties;
    }

    protected void logAction() {

        String propertiesString = " ";
        // Check if there are no properties - Alert,Prompt,Confirm elements for example
        if (properties != null) {
            if (properties.containsInternalProperty(UiElementProperties.MAP_ID_INTERNAL_PARAM)) {
                propertiesString = ElementsMap.ATT_ELEMENT_ID_EQ
                                   + properties.getInternalProperty(UiElementProperties.MAP_ID_INTERNAL_PARAM);
            } else {
                // get the properties in the form '{key1=value1, key2=value2}'
                propertiesString = properties.toString();
                // remove the leading and trailing brackets
                propertiesString = propertiesString.substring(1);
                propertiesString = propertiesString.substring(0, propertiesString.length() - 1);
            }
        }
        int stIndex = 2;
        String method = Thread.currentThread().getStackTrace()[stIndex].getMethodName();
        while (method.indexOf('$') > -1) {
            method = Thread.currentThread().getStackTrace()[++stIndex].getMethodName();
        }
        log.info(this.getClass().getSimpleName() + "[ " + propertiesString.trim() + " ]." + method + "()");
    }

    public String[] getPropertyNamesToUseForMatch() {

        return propsToUseForMatch;
    }

    /**
     * Check against passed rules and return first (largest in number of properties assumed) matching
     * @param userTypePrefix component type prefix like Swing, Html ...
     * @param rules array of rules( of properties) valid for UiElement identification
     * @throws ErrorMatchingElementRules
     */
    public void checkTypeAndRules( String userTypePrefix, String... rules ) throws ErrorMatchingElementRules {

        this.propsToUseForMatch = getElementProperties().checkTypeAndRules(this.getClass().getSimpleName(),
                                                                           userTypePrefix, rules);
        if (log.isDebugEnabled()) {
            log.debug("Rule with properties to use for element search: "
                      + Arrays.toString(propsToUseForMatch));
        }
    }
}
