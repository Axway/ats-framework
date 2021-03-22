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
package com.axway.ats.uiengine.elements.html;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.uiengine.elements.ElementsMap;
import com.axway.ats.uiengine.elements.UiElementProperties;

/**
 * Builds paths for referring an HTML element
 */
public class HtmlElementLocatorBuilder {

    private static final Logger   log                      = LogManager.getLogger(HtmlElementLocatorBuilder.class);

    public static final String    PROPERTY_ELEMENT_LOCATOR = "htmlElementLocator";

    public static final String    XPATH                    = "xpath";

    private static final String[] INTERNAL_ATTRIBUTES      = new String[]{ "_text",
                                                                           "_partText",
                                                                           "_inputType",
                                                                           "_css" };

    /**
     * Returns the element's XPath
     *
     * @param properties
     * @param htmlType
     * @param isInput
     * @return
     */
    public static String buildXpathLocator(
                                            String[] rules,
                                            UiElementProperties properties,
                                            String[] htmlTypes,
                                            String htmlNodeName ) {

        /*if( Arrays.asList( rules ).contains( XPATH ) && properties.getProperty( XPATH ) != null ) {
        
            return properties.getProperty( XPATH ).trim();
        }*/

        if (properties.getProperty(XPATH) != null) {
            return properties.getProperty(XPATH).trim();
        }

        StringBuilder xpath = new StringBuilder("//");

        final String formId = properties.getProperty(ElementsMap.ATT_ELEMENT_FORMID);
        final String formName = properties.getProperty(ElementsMap.ATT_ELEMENT_FORMNAME);
        if (formId != null) {
            xpath.append("form[@id='");
            xpath.append(formId);
            xpath.append("']/");
        } else if (formName != null) {
            xpath.append("form[@name='");
            xpath.append(formName);
            xpath.append("']/");
        }

        xpath.append(htmlNodeName);

        if ("input".equals(htmlNodeName) || "button".equals(htmlNodeName)) {

            // to convert the value of 'type' attribute to lower case we are using this function:
            // translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')
            // NOTE: there is another function to lower case 'lower-case()', but it's not in XPath 1.0
            // which is the version of XPath used in Firefox
            xpath.append("[translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')=");
            boolean firstTime = true;
            for (String htmlType : htmlTypes) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    xpath.append("or");
                }
                xpath.append("'");
                xpath.append(htmlType.toLowerCase());
                xpath.append("'");
            }
            xpath.append("]");
        }

        // adding rules to xpath
        for (String rule : rules) {

            boolean foundRule = true;
            // split if rule is to use several attributes combined like "name and value" ("name,value" in RULES)
            String[] ruleAttributes = rule.split("[\\s\\,]+");
            for (String ruleAttribute : ruleAttributes) {
                if (properties.getProperty(ruleAttribute) == null) {
                    foundRule = false;
                }

                if (foundRule) {

                    // ignore attributes starting with double underscore
                    if (ruleAttribute.startsWith("__")) {
                        log.debug("Ignoring attribute " + ruleAttribute + " as a helper one");
                        continue;
                    }

                    if (ruleAttribute.startsWith("_")) {
                        if (!Arrays.asList(INTERNAL_ATTRIBUTES).contains(ruleAttribute)) {
                            log.warn("Found HTML property [" + ruleAttribute + " = "
                                     + properties.getProperty(ruleAttribute)
                                     + "] , which starts with underscore , but is not known by ATS. "
                                     + "It will be used to locate HTML element " + htmlNodeName);
                        }
                    }

                    if ("_text".equals(ruleAttribute)) {
                        xpath.append("[");
                        xpath.append("text()");
                        xpath.append("='");
                        xpath.append(properties.getProperty(ruleAttribute));
                        xpath.append("']");
                    } else if ("_partText".equals(ruleAttribute)) {
                        xpath.append("[");
                        xpath.append("contains(text()");
                        xpath.append(", '");
                        xpath.append(properties.getProperty(ruleAttribute));
                        xpath.append("')]");
                    } else if ("_inputType".equals(ruleAttribute)) {
                        xpath.append("[@");
                        xpath.append("type");
                        xpath.append("='");
                        xpath.append(properties.getProperty(ruleAttribute));
                        xpath.append("']");
                    } else if ("_css".equals(ruleAttribute)) {
                        // do nothing
                    } else {
                        xpath.append("[@");
                        xpath.append(ruleAttribute);
                        xpath.append("='");
                        xpath.append(properties.getProperty(ruleAttribute));
                        xpath.append("']");
                    }

                    if (!"href".equalsIgnoreCase(rule)) { //if the rule is 'href' don't stop and append another rules
                        break;
                    }

                }

            }
        }

        if ("button".equals(htmlNodeName))

        {

            // we have to search for input elements too eg. //button[@type="button" or ...] | //input[@type="button" or ...]
            xpath.append(" | " + xpath.toString().replace("//button[", "//input["));
        } else if ("a".equals(htmlNodeName)) {

            // link title is the text of the "a" node (link), we need to do the following replace
            // here we are using normalize-space() function to trim the text of the link, because
            // text() = ' someText ' does't work. This is a problem with spaces in at the beginning and/or at the end.
            return xpath.toString().replace("@title", "normalize-space(text())");
        }

        return xpath.toString();
    }
}
