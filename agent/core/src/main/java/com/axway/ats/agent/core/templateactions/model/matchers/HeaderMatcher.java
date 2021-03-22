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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.context.ThreadContext;
import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;
import com.axway.ats.agent.core.templateactions.model.XmlUtilities;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateHeaderMatchMode;

public class HeaderMatcher extends ResponseMatcher {

    private static final Logger     log                            = LogManager.getLogger(HeaderMatcher.class);

    private static final long       serialVersionUID               = 1;

    private TemplateHeaderMatchMode matchMode;

    private String                  headerName;
    private String                  headerValueToMatch;

    // used for RANGE mode
    private int                     minValue;
    private int                     maxValue;
    // used for RANGE_OFFSET mode
    private int                     offsetValue;
    // used for LIST mode
    private String[]                listValues;

    // globally skipped headers are optional
    private boolean                 optionalHeader;

    /*
     * The pattern matches every cookie in the "Cookie" header, for example:
     *  JSESSIONID=1xsf41aq; ppSession="U001@Synchrony:134793:CB+6Z++OTpG0PLpaN/6zZ/uN5ta4+GaHNQ=="; TRANSLATED_SESSIONID=zl3c8pzv4p6g
     *
     * For each cookie it matches 3 groups:
     *      group 1 is the cookie name
     *      group 2 is the cookie value, if it's wrapped in double quotes (it can contain spaces and special chars)
     *      group 3 is the cookie value, if it's not wrapped in '"', but ends with ';' or till the end of cookies string
     * One of these groups 2 and 3 is null, according to the each cookie format
     * TODO: Pattern for cookies could be updated to match all possible names as
     * per RFC 2965, Ch 3.1 and referred "token" in  RFC 2616 (HTTP 1.1 spec), Ch 2.2:
     *   token          = 1*<any CHAR except CTLs or separators>
     *   separators     = "(" | ")" | "<" | ">" | "@"
     *                   | "," | ";" | ":" | "\" | <">
     *                   | "/" | "[" | "]" | "?" | "="
     *                   | "{" | "}" | SP | HT
     *
     */
    public static final Pattern     COOKIE_VALUE_PATTERN           = Pattern.compile("\\$?([\\w\\_\\-\\#]+)\\=(?:(?:\\\"([^\\\"]*)\\\")|(?:([^\\;\\\"\\;\\s]+)(?=\\;|$)))");

    public static final String      COOKIE_HEADER_NAME             = "Cookie";
    public static final String      SET_COOKIE_HEADER_NAME         = "Set-Cookie";
    public static final String      CONTENT_LENGTH_HEADER_NAME     = "Content-Length";
    public static final String      TRANSFER_ENCODING_HEADER_NAME  = "Transfer-Encoding";
    public static final String      LOCATION_HEADER_NAME           = "Location";
    public static final Set<String> COOKIE_ATTRIBUTE_NAMES_TO_SKIP = new HashSet<String>(Arrays.asList(new String[]{ "Path".toLowerCase(),
                                                                                                                     "Domain".toLowerCase(),
                                                                                                                     "Version".toLowerCase(),
                                                                                                                     "Expires".toLowerCase(),
                                                                                                                     "Comment".toLowerCase(),
                                                                                                                     "Max-Age".toLowerCase(),
                                                                                                                     "Secure".toLowerCase(),
                                                                                                                     "HttpOnly".toLowerCase() }));

    public HeaderMatcher( String headerName,
                          String headerValueToMatch,
                          TemplateHeaderMatchMode matchMode ) throws InvalidMatcherException {

        super();
        if (headerName == null || headerName.trim().length() == 0) {
            throw new InvalidMatcherException("The header name can not be null or empty string: '"
                                              + headerName + "'");
        }

        if (matchMode == TemplateHeaderMatchMode.RANGE) {
            String errMsgPrefix = "The value '" + headerValueToMatch + "' of header '" + headerName
                                  + "' is not a well formatted numeric range string: ";
            String[] rangeTokens = headerValueToMatch.split("-");
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
        } else if (matchMode == TemplateHeaderMatchMode.RANGE_OFFSET) {
            try {
                offsetValue = Integer.parseInt(headerValueToMatch.trim());
            } catch (NumberFormatException nfe) {
                throw new InvalidMatcherException("The value '" + headerValueToMatch + "' of header '"
                                                  + headerName + "' is not a valid number");
            }
            if (offsetValue < 1) {
                throw new InvalidMatcherException("The value '" + headerValueToMatch + "' of header '"
                                                  + headerName + "' must be a number greater than 1");
            }
        } else if (matchMode == TemplateHeaderMatchMode.LIST) {
            listValues = headerValueToMatch.split(",");
        }

        this.headerName = headerName;
        this.headerValueToMatch = headerValueToMatch; // not correct for OFFSET type but it is overridden by actual length in template
        this.matchMode = matchMode;
    }

    /**
     * Copy constructor for deep cloning
     * @param other
     */
    public HeaderMatcher( HeaderMatcher other ) {

        super(other);
        // immutable objects so no new copy needed
        this.headerName = other.headerName;
        this.headerValueToMatch = other.headerValueToMatch;
        this.matchMode = other.matchMode;
        this.minValue = other.minValue;
        this.maxValue = other.maxValue;
        this.optionalHeader = other.optionalHeader;
        this.offsetValue = other.offsetValue;
        this.listValues = other.listValues;
    }

    public String getHeaderName() {

        return this.headerName;
    }

    public String getHeaderValueToMatch() {

        return this.headerValueToMatch;
    }

    public boolean isOptionalHeader() {

        return optionalHeader;
    }

    public void setOptionalHeader(
                                   boolean optionalHeader ) {

        this.optionalHeader = optionalHeader;
    }

    public boolean isMergingMatcher() {

        return this.matchMode == TemplateHeaderMatchMode.RANGE_OFFSET;
    }

    public void mergeTo(
                         HeaderMatcher that ) throws InvalidMatcherException {

        // we expect that isMergingMatcher returns true, we make sure this is always true before calling this method
        if (this.matchMode == TemplateHeaderMatchMode.RANGE_OFFSET) {
            // check the base value is a number
            try {
                Integer.parseInt(that.headerValueToMatch);
            } catch (NumberFormatException nfe) {
                throw new InvalidMatcherException("Can not create a RANGE_OFFSET header matcher as the value '"
                                                  + headerValueToMatch
                                                  + "' of header '"
                                                  + headerName
                                                  + "' is not a numeric value");
            }

            this.headerValueToMatch = that.headerValueToMatch;
        } else {
            throw new InvalidMatcherException("Can not merge header matcher:\n" + this.toString()
                                              + "\n to header matcher:\n" + that.toString());
        }
    }

    @Override
    public boolean performMatch(
                                 Object expectedObject,
                                 Object actualObject ) {

        String actualText = (String) actualObject;

        markProcessed();

        boolean actualResult = false;
        if (actualText != null) {

            if (headerName.equalsIgnoreCase(SET_COOKIE_HEADER_NAME)) {

                Matcher matcher = COOKIE_VALUE_PATTERN.matcher(actualText);
                while (matcher.find()) {

                    String cookieName = matcher.group(1);
                    if (cookieName != null) {

                        // see the COOKIE_VALUE_PATTERN JavaDoc
                        String cookieValue = matcher.group(2);
                        if (cookieValue == null) {
                            cookieValue = matcher.group(3);
                        }
                        if (!COOKIE_ATTRIBUTE_NAMES_TO_SKIP.contains(cookieName.toLowerCase())) {

                            log.info("New cookie '" + cookieName + "' with value '" + cookieValue + "'");
                            ThreadContext.setAttribute(ThreadContext.COOKIE_VAR_PREFFIX + cookieName,
                                                       cookieValue);
                            actualResult = true;
                        }
                    } else {

                        log.warn("The '" + SET_COOKIE_HEADER_NAME + "' header has unexpected format '"
                                 + actualText + "'");
                    }
                }
            } else {

                int actualHeaderNumericValue;
                switch (matchMode) {

                    case EQUALS:

                        try {
                            String expectedResult = headerValueToMatch;
                            if (headerValueToMatch != null && headerValueToMatch.contains("${")) {
                                expectedResult = XmlUtilities.applyUserParameters(headerValueToMatch);
                            }
                            actualResult = actualText.equals(expectedResult);
                        } catch (XmlUtilitiesException e) {
                            log.error("Can not apply user parameters", e);
                        }
                        break;
                    case CONTAINS:

                        actualResult = actualText.contains(headerValueToMatch);
                        break;
                    case RANGE:

                        try {
                            actualHeaderNumericValue = Integer.parseInt(actualText);
                            actualResult = minValue <= actualHeaderNumericValue
                                           && actualHeaderNumericValue <= maxValue;
                        } catch (NumberFormatException nfe) {
                            log.warn("The value '" + actualText + "' of header '" + headerName
                                     + "' is not a numeric value");
                        }
                        break;
                    case RANGE_OFFSET:

                        int baseValue = Integer.parseInt(headerValueToMatch);
                        if (baseValue >= 0) {
                            try {
                                actualHeaderNumericValue = Integer.parseInt(actualText);
                                actualResult = (baseValue - offsetValue <= actualHeaderNumericValue)
                                               && (actualHeaderNumericValue <= baseValue + offsetValue);
                            } catch (NumberFormatException nfe) {
                                log.warn("The value '" + actualText + "' of header '" + headerName
                                         + "' is not a numeric value");
                            }
                        } else {
                            log.warn("Can not execute a range offset verification as the base value must be greater or equal 0, but it is '"
                                     + baseValue + "'. " + toString());
                        }
                        break;
                    case LIST:

                        for (String listValue : listValues) {
                            if (listValue.equals(actualText)) {
                                actualResult = true;
                                break;
                            }
                        }
                        break;
                    case REGEX:

                        actualResult = Pattern.compile(headerValueToMatch).matcher(actualText).find();
                        break;
                    case RANDOM:

                        actualResult = true;
                        break;
                    case EXTRACT:

                        try {
                            actualResult = extractUserParameter("response header",
                                                                headerValueToMatch,
                                                                actualText);
                        } catch (XmlUtilitiesException e) {
                            log.error("Can not extract user parameter", e);
                        }
                        break;
                }
            }
            if (log.isDebugEnabled()) {
                if (actualResult) {
                    log.debug("Matched the value '" + actualText + "' for " + toString());
                } else {
                    log.debug("Did not match the value '" + actualText + "' for " + toString());
                }
            }
        } else {
            log.error("Header '" + headerName + "' not found. Needed for " + toString());
        }
        return actualResult;
    }

    @Override
    public String toString() {

        String description = "Header matcher: header name is '" + headerName + "', match mode is "
                             + matchMode;
        if (matchMode != TemplateHeaderMatchMode.RANDOM) {
            description += ", header expected value is ";
            if (matchMode == TemplateHeaderMatchMode.RANGE) {
                description += "between " + minValue + " and " + maxValue;
            } else if (matchMode == TemplateHeaderMatchMode.RANGE_OFFSET) {
                int baseValue = Integer.parseInt(headerValueToMatch);
                description += "between " + (baseValue - offsetValue) + " and "
                               + (baseValue + offsetValue);
            } else if (matchMode == TemplateHeaderMatchMode.LIST) {
                description += "one of {" + Arrays.toString(listValues) + "}";
            } else {
                description += "'" + headerValueToMatch + "'";
            }
        }
        return description;
    }

}
