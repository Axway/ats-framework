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

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.context.ThreadContext;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;
import com.axway.ats.agent.core.templateactions.model.XmlUtilities;

public abstract class ResponseMatcher implements Serializable {

    public static final long serialVersionUID = 1L;

    protected boolean        processed        = false;

    public abstract boolean performMatch(
                                          Object expectedObject,
                                          Object actualObject );

    protected ResponseMatcher() {

        // keep previous behavior
    }

    /**
     * Copy constructor used for deep cloning
     * FIXME - revise cloning issue with global header matchers
     */
    protected ResponseMatcher( ResponseMatcher other ) {

        // keep previous behavior
        this.processed = other.processed; // another instance
    }

    @Override
    public abstract String toString();

    public boolean wasProcessed() {

        return this.processed;
    }

    protected void markProcessed() {

        this.processed = true;
    }

    protected boolean extractUserParameter(
                                            String description,
                                            String valueToMatch,
                                            String text ) throws XmlUtilitiesException {

        Logger log = LogManager.getLogger(this.getClass());

        // apply the current user parameters
        valueToMatch = XmlUtilities.applyUserParameters(valueToMatch);

        //FIXME: Limited to one variable only
        int findex = valueToMatch.indexOf("${=") + "${=".length();
        String userParameterName = valueToMatch.substring(findex, valueToMatch.indexOf("}", findex));

        // TODO check the regex accuracy. currently escaped *, ., *, ^, -, ?  and $, {, }
        String patternToMatch = valueToMatch.replace("(", "\\(")
                                            .replace(")", "\\)")
                                            .replace("*", "\\*")
                                            .replace(".", "\\.")
                                            .replace("^", "\\^")
                                            .replace("-", "\\-")
                                            .replace("|", "\\|")
                                            .replace("?", "\\?")
                                            .replaceAll("\\$\\{\\=.*\\}", "(.*)");
        // escape $ { and }, and ${RANDOM} after we have replaced ${=...}
        patternToMatch = patternToMatch.replace("${RANDOM}", ".*")
                                       // match this place as value for the variable
                                       .replace("$", "\\$")
                                       .replace("{", "\\{")
                                       .replace("}", "\\}");

        Matcher matcher = Pattern.compile(patternToMatch).matcher(text);
        if (matcher.find() && matcher.groupCount() == 1) {
            String userParameterValue = matcher.group(1);
            if (log.isDebugEnabled()) {
                log.debug("Extracted new user parameter from " + description + ": '" + userParameterName
                          + "' = '" + userParameterValue + "'");
            }
            ThreadContext.setAttribute(userParameterName, userParameterValue);
            return true;
        } else {
            log.error("Could not extract the expected user parameter '" + userParameterName + "' from "
                      + text + " for " + toString());
            return false;
        }
    }

}
