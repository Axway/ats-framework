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
package com.axway.ats.agent.core.templateactions.model.objects;

import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HEADER_NAME_ATTRIBUTE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HEADER_VALUE_ATTRIBUTE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_RESPONSE_RESULT;

import java.util.HashMap;
import java.util.Stack;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlReaderException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;
import com.axway.ats.agent.core.templateactions.model.XmlUtilities;
import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateHeaderMatchMode;

/**
 * Currently used for keeping expected response (from template) and not the actual return response
 *
 */
public class ActionResponseObject extends AbstractActionObject {

    /**
     * HTTP response code like 200 OK, 302 Moved, 500 Internal server error
     */
    private String responseCodeResult;

    public ActionResponseObject( String actionsXmlName, ActionParser action ) throws XmlReaderException,
                                                                              XmlUtilitiesException,
                                                                              InvalidMatcherException {

        super(actionsXmlName, action);

        resolveHttpResponseCodeResult(action.getActionNodeWithoutBody());
    }

    @Override
    protected void resolveHttpHeaders( Node actionResponse ) throws XmlReaderException,
                                                             InvalidMatcherException {

        Node[] expectedHeaders = xmlUtilities.getSignificantResponseHeaders(actionResponse);
        for (int iExpected = 0; iExpected < expectedHeaders.length; iExpected++) {
            String headerName = xmlUtilities.getNodeAttribute(expectedHeaders[iExpected],
                                                              TOKEN_HEADER_NAME_ATTRIBUTE);
            String headerValue = xmlUtilities.getNodeAttribute(expectedHeaders[iExpected],
                                                               TOKEN_HEADER_VALUE_ATTRIBUTE);

            TemplateHeaderMatchMode matchMode;
            if (headerValue.contains("${=")) {
                matchMode = TemplateHeaderMatchMode.EXTRACT;
            } else if (headerValue.contains("${")) {
                if (headerValue.startsWith("${CONTAINS=")) {
                    headerValue = headerValue.substring("${CONTAINS=".length(), headerValue.length() - 1);
                    matchMode = TemplateHeaderMatchMode.CONTAINS;
                } else if (headerValue.startsWith("${RANGE=")) {
                    headerValue = headerValue.substring("${RANGE=".length(), headerValue.length() - 1);
                    matchMode = TemplateHeaderMatchMode.RANGE;
                } else if (headerValue.startsWith("${LIST=")) {
                    headerValue = headerValue.substring("${LIST=".length(), headerValue.length() - 1);
                    matchMode = TemplateHeaderMatchMode.LIST;
                } else if (headerValue.startsWith("${REGEX=")) {
                    headerValue = headerValue.substring("${REGEX=".length(), headerValue.length() - 1);
                    matchMode = TemplateHeaderMatchMode.REGEX;
                } else if ("${RANDOM}".equals(headerValue)) {
                    matchMode = TemplateHeaderMatchMode.RANDOM;
                    headerValue = null;
                } else {
                    // the "Location" header can contain some user parameters like ${serverUrl}
                    if (headerName.equalsIgnoreCase(HeaderMatcher.LOCATION_HEADER_NAME)) {
                        // TODO: For that header again we may have variables inside. 
                        // Generally cookies are automatically processed but if there is var. we should effectively use it 
                        // as regex RANDOM area or replace var before whole location header match
                        matchMode = TemplateHeaderMatchMode.EQUALS;
                    } else {
                        throw new InvalidMatcherException(headerValue + " contains unknown matcher key");
                    }
                }
            } else {
                // when nothing is specified - we match the header value as is
                matchMode = TemplateHeaderMatchMode.EQUALS;
            }

            // we need the headers in order to construct the request
            httpHeaders.add(new ActionHeader(headerName, headerValue));

            // we need the headers matcher in order to verify the response
            httpHeaderMatchers.add(new HeaderMatcher(headerName, headerValue, matchMode));
        }
    }

    public String getResponseResult() {

        return responseCodeResult;
    }

    private void resolveHttpResponseCodeResult( Node actionResponse ) throws XmlReaderException {

        Node expectedResultNode = XmlUtilities.getFirstChildNode(actionResponse,
                                                                 TOKEN_HTTP_RESPONSE_RESULT);
        if (expectedResultNode == null) {
            throw new XmlReaderException(actionsXmlName, "No " + TOKEN_HTTP_RESPONSE_RESULT + " node");
        } else {
            responseCodeResult = expectedResultNode.getTextContent();
        }
    }

}
