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

public class TextBodyMatcher extends ResponseMatcher {

    private static final Logger log              = LogManager.getLogger(TextBodyMatcher.class);

    private static final long   serialVersionUID = 1;

    private boolean             isRegex;
    private boolean             shouldNotBeFound;
    // result from match. Not null after match is performed
    private Boolean             matchResult      = null;
    private String              valueToMatch;

    public TextBodyMatcher( String valueToMatch,
                            boolean isRegex ) {

        this(valueToMatch, isRegex, false);
    }

    /*
     * Constructor
     *
     * @param shouldNotBeFound if true then match will return true if the value is not found, i.e. inverse search result
     */
    public TextBodyMatcher( String valueToMatch,
                            boolean isRegex,
                            boolean shouldNotBeFound ) {

        this.valueToMatch = valueToMatch;
        this.isRegex = isRegex;
        this.shouldNotBeFound = shouldNotBeFound;
    }

    @Override
    public boolean performMatch(
                                 Object expectedObject,
                                 Object actualObject ) {

        String actualText = (String) actualObject;

        markProcessed();

        boolean actualResult = false;
        if (isRegex) {
            if (Pattern.compile(valueToMatch).matcher(actualText).find()) {
                actualResult = true;
            }
        } else {
            if (actualText.contains(valueToMatch)) {
                actualResult = true;
            }
        }

        if (shouldNotBeFound) {
            actualResult = !actualResult;
        }

        if (log.isDebugEnabled()) {

            if (actualResult) {
                log.debug("Matched " + toString());
            } else {
                log.debug("Did not match " + toString() + "\nActual text inspected for match:"
                          + actualText);
            }
        }
        matchResult = new Boolean(actualResult);
        return actualResult;
    }

    @Override
    public String toString() {

        StringBuilder toReturn = new StringBuilder(100);
        toReturn.append("Text ");
        if (isRegex) {
            toReturn.append("regular expression ");
        }
        toReturn.append("matcher with expected value: ");
        if (shouldNotBeFound) {
            toReturn.append("NOT ");
        }
        toReturn.append("'" + valueToMatch + "'");
        if (processed && matchResult != null && matchResult.booleanValue() == false && !shouldNotBeFound
            && !log.isDebugEnabled()) {
            // RegEx does not match as expected
            toReturn.append(". Note that error response printed below is not "
                            + "original and is reformatted with identation and "
                            + "new lines. Increase severity of this class to "
                            + "debug level in order to dump actual response inspected for match");
        }
        return toReturn.toString();
    }
}
