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

import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_RESOURCE_FILE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlReaderException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;
import com.axway.ats.agent.core.templateactions.model.XmlUtilities;
import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.XPathBodyMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateBodyNodeMatchMode;
import com.axway.ats.common.systemproperties.AtsSystemProperties;

/**
 * Currently used for keeping expected request or response (from template) and not an actual one (over the net)
 *
 */
public abstract class AbstractActionObject {
    private final static Logger    log         = LogManager.getLogger(AbstractActionObject.class);

    protected XmlUtilities         xmlUtilities;

    protected String               actionsXmlName;                                            // currently file name as key

    protected List<ActionHeader>   httpHeaders = new ArrayList<ActionHeader>();

    private String                 resourceFile;

    private String                 resourceFileSize;

    private boolean                hasParamsInResourceFile;

    protected ActionParser         actionParser;

    protected List<HeaderMatcher>  httpHeaderMatchers;

    private List<XPathBodyMatcher> xpathBodyMatchers;

    public AbstractActionObject( String actionsXml, ActionParser actionParser ) throws XmlReaderException,
                                                                                XmlUtilitiesException,
                                                                                InvalidMatcherException {

        this.actionsXmlName = actionsXml;
        this.xmlUtilities = new XmlUtilities();

        this.httpHeaderMatchers = new ArrayList<HeaderMatcher>();
        this.xpathBodyMatchers = new ArrayList<XPathBodyMatcher>();
        this.actionParser = actionParser;

        resolveHttpHeaders(actionParser.getActionNodeWithoutBody());
        resolveResourceFile(actionParser.getActionNodeWithoutBody());
    }

    public List<ActionHeader> getHttpHeaders() throws XmlUtilitiesException {

        List<ActionHeader> httpHeadersWithAppliedUserParameters = new ArrayList<ActionHeader>();
        for (ActionHeader header : httpHeaders) {

            String newHeaderValue = null;
            if (header.getHeaderName().equalsIgnoreCase(HeaderMatcher.COOKIE_HEADER_NAME)) {
                newHeaderValue = xmlUtilities.applyUserParametersInCookieHeader(header.getHeaderValue());
            } else {
                newHeaderValue = XmlUtilities.applyUserParameters(header.getHeaderValue());
            }
            httpHeadersWithAppliedUserParameters.add(new ActionHeader(header.getHeaderName(),
                                                                      newHeaderValue));
        }

        return httpHeadersWithAppliedUserParameters;
    }

    public String getResourceFile() {

        return resourceFile;
    }

    public String getResourceFileSize() {

        return resourceFileSize;
    }

    public boolean hasParamsInResourceFile() {

        return hasParamsInResourceFile;
    }

    public List<HeaderMatcher> getHttpHeaderMatchers() {

        return this.httpHeaderMatchers;
    }

    public List<XPathBodyMatcher> getXpathBodyMatchers() {

        return this.xpathBodyMatchers;
    }

    protected abstract void resolveHttpHeaders( Node headersNode ) throws XmlReaderException,
                                                                   InvalidMatcherException;

    private void resolveResourceFile( Node actionRequest ) throws XmlReaderException {

        Node resourceFileNode = XmlUtilities.getFirstChildNode(actionRequest, TOKEN_HTTP_RESOURCE_FILE);
        if (resourceFileNode != null) {
            resourceFile = actionsXmlName.substring(0, actionsXmlName.lastIndexOf('.'))
                           + AtsSystemProperties.SYSTEM_FILE_SEPARATOR + resourceFileNode.getTextContent();
            resourceFileSize = xmlUtilities.getNodeAttribute(resourceFileNode, "size");
            if (Boolean.valueOf(xmlUtilities.getNodeAttribute(resourceFileNode, "hasParams"))) {
                hasParamsInResourceFile = true;
            }
        }
    }

    // TODO: use Deque (non-synchronized) impl. instead of Stack
    protected void resolveXpathEntries( Node node, Stack<String> nodeXpath,
                                        Map<String, Integer> allNodesIndexMap ) throws DOMException,
                                                                                InvalidMatcherException {

        String name = node.getNodeName();

        // calculate the XPath for this node
        String nodeXpathString = name;
        if (nodeXpath.size() > 0) {
            nodeXpathString = nodeXpath.peek().toString() + "/" + nodeXpathString;
        }
        // calculate the index for this node
        Integer nodeIndex = allNodesIndexMap.get(nodeXpathString);
        if (nodeIndex == null) {
            nodeIndex = 1;
        } else {
            nodeIndex = nodeIndex + 1;
        }
        allNodesIndexMap.put(nodeXpathString, nodeIndex);
        // calculate the XPath for this node including its index
        String nodeXpathWithIndexString = nodeXpathString + "[" + nodeIndex + "]";

        nodeXpath.push(nodeXpathWithIndexString);

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {

            Node currentNode = nodeList.item(i);
            if (currentNode != null && currentNode.getNodeType() == Node.TEXT_NODE) {
                String nodeValue = currentNode.getNodeValue();
                if (nodeValue != null) {
                    nodeValue = nodeValue.trim();
                    if (nodeValue.contains("${")) {
                        // this node contains a user parameter we are interested in
                        String xpath = "//" + nodeXpath.peek();
                        TemplateBodyNodeMatchMode matchMode;
                        if (nodeValue.contains("${=")) {
                            // retriever
                            matchMode = TemplateBodyNodeMatchMode.EXTRACT;
                        } else {
                            // matcher
                            if (nodeValue.startsWith("${CONTAINS=")) {
                                nodeValue = nodeValue.substring("${CONTAINS=".length(),
                                                                nodeValue.length() - 1);
                                matchMode = TemplateBodyNodeMatchMode.CONTAINS;
                            } else if (nodeValue.startsWith("${EQUALS=")) {
                                nodeValue = nodeValue.substring("${EQUALS=".length(),
                                                                nodeValue.length() - 1);
                                matchMode = TemplateBodyNodeMatchMode.EQUALS;
                            } else if (nodeValue.startsWith("${RANGE=")) {
                                nodeValue = nodeValue.substring("${RANGE=".length(),
                                                                nodeValue.length() - 1);
                                matchMode = TemplateBodyNodeMatchMode.RANGE;
                            } else if (nodeValue.startsWith("${LIST=")) {
                                nodeValue = nodeValue.substring("${LIST=".length(), nodeValue.length() - 1);
                                matchMode = TemplateBodyNodeMatchMode.LIST;
                            } else if (nodeValue.startsWith("${REGEX=")) {
                                nodeValue = nodeValue.substring("${REGEX=".length(),
                                                                nodeValue.length() - 1);
                                matchMode = TemplateBodyNodeMatchMode.REGEX;
                            } else {
                                // ignore user parameters e.g. ${targetHost}
                                continue;
                            }
                        }
                        XPathBodyMatcher xPathMatcher = new XPathBodyMatcher(xpath, nodeValue, matchMode);
                        log.debug("Extraceted XPath matcher: " + xPathMatcher.toString());
                        xpathBodyMatchers.add(xPathMatcher);
                    }
                }
            } else {
                // a regular node, search its children
                resolveXpathEntries(currentNode, nodeXpath, allNodesIndexMap);
            }
        }
        nodeXpath.pop();
    }
}
