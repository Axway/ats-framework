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
package com.axway.ats.agent.core.templateactions.model.matchers;

import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;

import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.model.XmlUtilities;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateBodyNodeMatchMode;

public class XPathBodyMatcher extends ResponseMatcher {

    private static final Logger       log              = LogManager.getLogger(XPathBodyMatcher.class);

    private static final long         serialVersionUID = 1;

    private TemplateBodyNodeMatchMode matchMode;

    private String                    xpath;

    private String                    valueToMatch;
    // used for RANGE mode
    private int                       minValue;
    private int                       maxValue;
    // used for LIST mode
    private String[]                  listValues;

    public XPathBodyMatcher( String xpath,
                             String valueToMatch,
                             TemplateBodyNodeMatchMode matchMode ) throws InvalidMatcherException {

        this.xpath = xpath;
        this.valueToMatch = valueToMatch;
        this.matchMode = matchMode;

        if (matchMode == TemplateBodyNodeMatchMode.RANGE) {
            String errMsgPrefix = "'" + valueToMatch + "' is not a well formatted numeric range string: ";
            String[] rangeTokens = valueToMatch.split("-");
            if (rangeTokens.length != 2) {
                throw new InvalidMatcherException(errMsgPrefix
                                                  + "The ',' delimiter must be present once, but it is present "
                                                  + rangeTokens.length + " times");
            }
            try {
                minValue = Integer.parseInt(rangeTokens[0].trim());
            } catch (NumberFormatException nfe) {
                throw new InvalidMatcherException(errMsgPrefix + "The minumum value is not a valid number");
            }
            try {
                maxValue = Integer.parseInt(rangeTokens[1].trim());
            } catch (NumberFormatException nfe) {
                throw new InvalidMatcherException(errMsgPrefix + "The maxmimum value is not a valid number");
            }
            if (minValue > maxValue) {
                throw new InvalidMatcherException(errMsgPrefix
                                                  + "The mimium value is bigger than the maxmimum value");
            }
        }

        if (matchMode == TemplateBodyNodeMatchMode.LIST) {
            listValues = valueToMatch.split(",");
        }
    }

    @Override
    public boolean performMatch(
                                 Object expectedObject,
                                 Object actualObject ) {

        Node actualNode = (Node) actualObject;

        markProcessed();

        boolean actualResult = false;
        String actualValue = "";

        try {
            String[][] xpathEntries = XmlUtilities.extractXpathEntries(actualNode, new String[]{ xpath });
            // we know the xpathEntries.length == 1, as we request exactly 1 xpath
            // if xpathEntries[0].length == 0 - no entry was found
            // if xpathEntries[0].length > 1 - more than 1 entry was found, user must specify a more concrete path
            if (xpathEntries[0].length == 1) {

                actualValue = xpathEntries[0][0];
                switch (matchMode) {
                    case CONTAINS:
                        actualResult = actualValue.contains(valueToMatch);
                        break;
                    case EQUALS:
                        actualResult = actualValue.equals(valueToMatch);
                        break;
                    case RANGE:
                        int actualNumericValue;
                        try {
                            actualNumericValue = Integer.parseInt(actualValue);
                            actualResult = minValue <= actualNumericValue && actualNumericValue <= maxValue;
                        } catch (NumberFormatException nfe) {
                            log.warn("The value '" + actualValue + "' of body node '" + xpath
                                     + "' is not a numeric value '");
                        }
                        break;
                    case LIST:
                        for (String listValue : listValues) {
                            if (listValue.equals(actualValue)) {
                                actualResult = true;
                                break;
                            }
                        }
                        break;
                    case REGEX:
                        actualResult = Pattern.compile(valueToMatch).matcher(actualValue).find();
                        break;
                    case EXTRACT:
                        actualResult = extractUserParameter("response body", valueToMatch, actualValue);
                        break;
                }

                if (log.isDebugEnabled()) {
                    if (actualResult) {
                        log.debug("Matched the value '" + actualValue + "' for " + toString());
                    } else {
                        log.debug("Did not match the value '" + actualValue + "' for " + toString());
                    }
                }

            } else {
                log.error("We expect to find exactly 1 element for " + toString() + ". But we found "
                          + xpathEntries[0].length + " elements");
            }
        } catch (Exception e) {
            log.error("Error extracting xpath element for " + toString(), e);
        }

        return actualResult;
    }

    public TemplateBodyNodeMatchMode getMatchMode() {

        return matchMode;
    }

    public String getXpath() {

        return xpath;
    }

    public String getMatcherValue() {

        return valueToMatch;
    }

    @Override
    public String toString() {

        return "XPath body matcher, xpath is '" + xpath + "', matcher value is '" + valueToMatch + "'";
    }

}
