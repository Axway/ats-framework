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

import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.exceptions.BadUiElementPropertyException;
import com.axway.ats.uiengine.exceptions.ErrorMatchingElementRules;

@PublicAtsApi
public class UiElementProperties {

    private Properties         properties            = new Properties();

    private Properties         internalProperties    = new Properties();

    public static final String MAP_ID_INTERNAL_PARAM = "mapID";

    @PublicAtsApi
    public UiElementProperties() {

    }

    public void addInternalProperty(
                                     String name,
                                     String value ) {

        internalProperties.put(name, value);
    }

    public String getInternalProperty(
                                       String name ) {

        String internalPropertyValue = internalProperties.getProperty(name);
        if (internalPropertyValue == null) {
            throw new BadUiElementPropertyException("The needed internal property '" + name
                                                    + "' is not found for element with user properties: "
                                                    + toString());
        }
        return internalPropertyValue;
    }

    public boolean containsInternalProperty(
                                             String name ) {

        return internalProperties.containsKey(name);
    }

    @PublicAtsApi
    public UiElementProperties addProperty(
                                            String name,
                                            String value ) {

        if (name == null || name.trim().length() == 0) {
            throw new BadUiElementPropertyException("Bad property name '" + name + "'");
        }

        if (value == null || value.trim().length() == 0) {
            throw new BadUiElementPropertyException("Bad value for property " + name + ": '" + value + "'");
        }

        properties.put(name, value);
        return this;
    }

    @PublicAtsApi
    public String getProperty(
                               String name ) {

        return properties.getProperty(name);
    }

    /**
     * Number of element's non-internal properties
     * @return the number of element's properties
     */
    public int getPropertiesSize() {

        return properties.size();
    }

    /**
     * For internal use
     * @return get all property keys
     */
    public Set<Object> getPropertiesKeys() {

        return properties.keySet();
    }

    public Properties getPropertiesCopy() {

        return (Properties) properties.clone();
    }

    /**
     * Check types and rules for matching/finding an UI element
     * @param userType the class name, e.g. "HtmlTextBox"
     * @param userTypePrefix the element family prefix, e.g. "Html"
     * @param rules the element identification rules
     * @return the parsed matching rule as String array of attributes which should be found
     */
    public String[] checkTypeAndRules(
                                       String userType,
                                       String userTypePrefix,
                                       String... rules ) throws BadUiElementPropertyException,
                                                         ErrorMatchingElementRules {

        ArrayList<String> matchingRules = new ArrayList<>(1);

        // 1. Check the type if provided by the user
        String providedType = properties.getProperty("type");
        if (providedType != null) {
            if (!userType.substring(userTypePrefix.length()).equalsIgnoreCase(providedType)) {
                throw new BadUiElementPropertyException("You can not construct a " + userType + " from "
                                                        + toString());
            }
        }

        Set<Object> keys = properties.keySet();
        String[] _rules = keys.toArray(new String[keys.size()]);

        for (String rule : _rules) {
            // exclude these rules from the matching rules
            if (rule.equalsIgnoreCase(UiElementProperties.MAP_ID_INTERNAL_PARAM)) {
                continue;
            }
            if (rule.equalsIgnoreCase(ElementsMap.ATT_ELEMENT_TYPE)) {
                continue;
            }
            if (rule.equalsIgnoreCase(ElementsMap.ATT_ELEMENT_FORMID)) {
                continue;
            }
            if (rule.equalsIgnoreCase(ElementsMap.ATT_ELEMENT_FORMNAME)) {
                continue;
            }
            if (rule.equalsIgnoreCase(ElementsMap.ATT_ELEMENT_FRAME)) {
                continue;
            }
            String[] ruleElements = rule.split("[\\s,]+");
            if (hasAllProperties(ruleElements)) {
                for (String ruleElement : ruleElements) {
                    // log found rules
                    //log.info( "rule: " + ruleElement );
                    matchingRules.add(ruleElement);
                }

            }
        }

        if (matchingRules.size() >= 1) {

            return matchingRules.toArray(new String[matchingRules.size()]);
        } else {

            throw new ErrorMatchingElementRules("No html attributes specified for " + userType
                                                + " element.");
        }
    }

    /**
     * Checks if all properties in the rule are found in the actually defined (provided) ones
     * for the specific UI element instance
     * @param ruleWithPropertiesNames
     * @return true if this rule is a match
     */
    private boolean hasAllProperties(
                                      String[] ruleWithPropertiesNames ) {

        for (String propertyName : ruleWithPropertiesNames) {
            if (this.getProperty(propertyName) == null) {
                return false;
            }
        }
        // There could be extra properties but we had found the largest rule
        // (set of properties) that we will use to find element. This is because 
        // largest one is expected to be placed in front
        return true;
    }

    @Override
    public String toString() {

        return properties.toString();
    }
}
