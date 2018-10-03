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
package com.axway.ats.agent.core.templateactions.model;

import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HEADER_NAME_ATTRIBUTE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HEADER_VALUE_ATTRIBUTE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_ACTION;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_ACTIONS;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_HEADER;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_RESOURCE_FILE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_RESPONSE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_RESPONSE_RESULT;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.axway.ats.agent.core.configuration.ConfigurationSettings;
import com.axway.ats.agent.core.configuration.TemplateActionsResponseVerificationConfigurator;
import com.axway.ats.agent.core.context.ThreadContext;
import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;
import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.ResponseMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.XPathBodyMatcher;
import com.axway.ats.agent.core.templateactions.model.objects.ActionParser;
import com.axway.ats.agent.core.templateactions.model.objects.ActionResponseObject;
import com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;

/**
 * It contains the XML definition tokens and some commonly used XML methods.
 */
public class XmlUtilities {

    private static final Logger   log                              = Logger.getLogger(XmlUtilities.class);

    public static final int       MAX_RESPONSE_BODY_BYTES_TO_PRINT = 512 * 1024;

    /*
     * Content-Type prefixes of the printable contents
     */
    private static final String[] PRINTABLE_CONTENT_TYPES          = new String[]{ "text/", "application/xml",
                                                                                   "application/xhtml+xml", };

    private boolean               matchFilesBySize;
    private boolean               matchFilesByContent;

    public XmlUtilities() {

        Boolean templateActionsMatchFilesBySize = ConfigurationSettings.getInstance()
                                                                       .isTemplateActionsMatchFilesBySize();
        if (templateActionsMatchFilesBySize != null) {
            matchFilesBySize = templateActionsMatchFilesBySize.booleanValue();
        }
        Boolean templateActionsMatchFilesByContent = ConfigurationSettings.getInstance()
                                                                          .isTemplateActionsMatchFilesByContent();
        if (templateActionsMatchFilesByContent != null) {
            matchFilesByContent = templateActionsMatchFilesByContent.booleanValue();
        }
    }

    public static Node getFirstChildNode( Node parentNode, String childNodeName ) {

        NodeList childrenNodes = parentNode.getChildNodes();
        for (int iChildren = 0; iChildren < childrenNodes.getLength(); iChildren++) {
            Node childNode = childrenNodes.item(iChildren);
            if (childNodeName.equalsIgnoreCase(childNode.getNodeName())) {
                return childNode;
            }
        }
        return null;
    }

    public String getNodeAttribute( Node node, String attributeName ) {

        NamedNodeMap attMap = node.getAttributes();
        for (int i = 0; i < attMap.getLength(); i++) {
            Node attNode = attMap.item(i);
            if (attributeName.equalsIgnoreCase(attNode.getNodeName())) {
                return attNode.getNodeValue();
            }
        }
        return null;
    }

    /**
     * Applies new cookie values for every cookie in the 'Cookie' header (if there is a new value
     * from the previous responses)
     *
     * @param cookiesToModify the cookies string to modify. Containing one or more cookies separated with ';' char
     * @return the cookies string with the new values
     */
    public String applyUserParametersInCookieHeader( String cookiesToModify ) {

        StringBuffer newCookiesString = new StringBuffer(cookiesToModify.length());
        Matcher cookieMatcher = HeaderMatcher.COOKIE_VALUE_PATTERN.matcher(cookiesToModify);
        while (cookieMatcher.find()) {
            String cookieName = cookieMatcher.group(1);
            if (cookieName != null) {

                Object newCookieValue = ThreadContext.getAttribute(ThreadContext.COOKIE_VAR_PREFFIX
                                                                   + cookieName);
                if (newCookieValue != null
                    && !HeaderMatcher.COOKIE_ATTRIBUTE_NAMES_TO_SKIP.contains(cookieName.toLowerCase())) {

                    cookieMatcher.appendReplacement(newCookiesString,
                                                    "$1=\"" + newCookieValue.toString() + "\"");
                }
            }
        }
        cookieMatcher.appendTail(newCookiesString);

        return newCookiesString.toString();
    }

    /**
     * Applies all requested by the user changes on the provided string.
     * All user parameters are expected in the form ${user_param_name}
     *
     * @param stringToModify the string to modify
     * @return the modified string
     * @throws XmlUtilitiesException
     */
    public static String applyUserParameters( String stringToModify ) throws XmlUtilitiesException {

        StringBuilder sb = new StringBuilder(stringToModify);

        int paramStartIndex = -1;
        for (String key : ThreadContext.getAttributeNames()) {

            String paramName = "${" + key + "}";
            paramStartIndex = sb.indexOf(paramName);
            if (paramStartIndex > -1) {

                Object paramValue = ThreadContext.getAttribute(key);
                if (paramValue instanceof Queue<?>) {

                    Queue<?> valuesQueue = (Queue<?>) paramValue;
                    while ( (paramStartIndex = sb.indexOf(paramName, paramStartIndex)) > -1) {

                        // replace the first occurrence of the parameter and remove its value from the Queue
                        Object value = valuesQueue.poll();
                        if (value == null) {

                            throw new XmlUtilitiesException("The number of parameters " + paramName
                                                            + " is more than the number of provided values for them.");
                        }
                        sb.replace(paramStartIndex, paramStartIndex + paramName.length(), value.toString());
                    }

                } else {

                    while ( (paramStartIndex = sb.indexOf(paramName, paramStartIndex)) > -1) {

                        sb.replace(paramStartIndex, paramStartIndex + paramName.length(),
                                   paramValue.toString());
                    }
                }
            }
        }

        while ( (paramStartIndex = sb.indexOf("${", paramStartIndex)) > -1) {

            if (sb.indexOf("}", paramStartIndex) > -1 && sb.charAt(paramStartIndex + 2) != '=') {

                // TODO - change to warn after detailed review. Currently it seems this method is applied before new parameters extraction
                log.info("Currently there is no value to replace parameter "
                         + sb.substring(paramStartIndex, sb.indexOf("}", paramStartIndex) + 1)
                         + ". Increase HttpClient's logging severity to 'TRACE' in order to see the current request data.");
            }
            paramStartIndex++;
        }

        return sb.toString();
    }

    /**
     * Array of String[] matching each XPath expression
     * @param node node to search under. Top body node
     * @param wantedXpathEntries
     * @return
     * @throws Exception
     */
    public static String[][] extractXpathEntries( Node node, String[] wantedXpathEntries ) throws Exception {

        List<String[]> values = new ArrayList<String[]>(wantedXpathEntries.length);
        for (String expression : wantedXpathEntries) {
            values.add(getByXpath(node, expression));
        }
        return values.toArray(new String[values.size()][]);
    }

    /**
     * Verify the expected and actual responses match
     *
     * @param expectedHttpResponseObject
     * @param currentActionRequestNumber current request/response step in this action/xml (starts from 1)
     * @throws Exception
     */
    public void
            verifyResponse( String actionsXml, String actionName, int currentActionRequestNumber,
                            ActionResponseObject expectedHttpResponseObject, HttpClient httpClient,
                            TemplateActionsResponseVerificationConfigurator responseVerificationConfigurator ) throws Exception {

        ActionParser actualHttpResponse = readActionResponse(httpClient, actionsXml,
                                                             currentActionRequestNumber, false);
        if (HttpClient.log.isTraceEnabled()) {
            String causeMsg = "Print response for debugging purposes";
            logActualResponse(causeMsg, actionName, currentActionRequestNumber, actualHttpResponse, false);
        }

        // stepMatchers now are after reading response so not to influence StopWatches
        // list with all body matchers
        List<ResponseMatcher> stepMatchers = new ArrayList<ResponseMatcher>();

        // add all matchers from the XML file
        List<XPathBodyMatcher> xpathBodyMatchers = applyUserParameters(expectedHttpResponseObject.getXpathBodyMatchers());
        stepMatchers.addAll(xpathBodyMatchers);

        // add all matchers from the test case
        TemplateActionResponseVerificator responseVerificator = responseVerificationConfigurator.getActionVerificator(actionName);
        if (responseVerificator != null) {
            // there is a verificator for this action
            stepMatchers.addAll(responseVerificator.getStepBodyMatchers(currentActionRequestNumber));
        }

        try {
            // Compare HTTP response code
            String expectedResponseResult = expectedHttpResponseObject.getResponseResult();
            String actualResponseResult = getFirstChildNode(actualHttpResponse.getActionNodeWithoutBody(),
                                                            TOKEN_HTTP_RESPONSE_RESULT).getTextContent();
            if (!expectedResponseResult.equalsIgnoreCase(actualResponseResult)) {

                String causeMsg = "Expected response result '" + expectedResponseResult
                                  + "' is different than the actual '" + actualResponseResult + "'.";
                logActualResponse(causeMsg, actionName, currentActionRequestNumber, actualHttpResponse,
                                  true);
                throw new XmlUtilitiesException(causeMsg);
            }

            // Compare response headers. It extracts any user parameters if present in the headers.
            verifyResponseHeaders(actionName, currentActionRequestNumber,
                                  expectedHttpResponseObject.getHttpHeaderMatchers(), actualHttpResponse,
                                  responseVerificationConfigurator);

            // Compare response files
            verifyResponseFile(expectedHttpResponseObject, actualHttpResponse.getActionNodeWithoutBody());

            if (!stepMatchers.isEmpty()) {

                // TODO verify the response body here
            }
        } finally {
            actualHttpResponse.cleanupMembers();
        }

        log.info(actionName + "[" + currentActionRequestNumber + "] -> " + "Verified HTTP response");
    }

    /**
     * Filter some headers which constantly change, so can not be matched
     *
     * @param responseNode xml response node
     * @return only the significant header nodes
     */
    public Node[] getSignificantResponseHeaders( Node responseNode ) {

        List<Node> significantHeaders = new ArrayList<Node>();

        Node[] allHeaders = getChildrenNodes(responseNode, TOKEN_HTTP_HEADER);
        for (Node header : allHeaders) {
            String headerName = getNodeAttribute(header, TOKEN_HEADER_NAME_ATTRIBUTE);
            if (HttpClient.log.isTraceEnabled()) {
                HttpClient.log.trace("header found: '" + headerName + "', value: '"
                                     + getNodeAttribute(header, TOKEN_HEADER_VALUE_ATTRIBUTE) + "'");
            }

            // Set-Cookie must always be a significant header
            if (headerName.equalsIgnoreCase(HeaderMatcher.SET_COOKIE_HEADER_NAME)) {
                significantHeaders.add(header);
                continue;
            }

            boolean isSignificantHeader = true;
            for (String nonSignificantHeader : TemplateActionsXmlDefinitions.NON_SIGNIFICANT_HEADERS) {
                if (headerName.equalsIgnoreCase(nonSignificantHeader)) {
                    isSignificantHeader = false;
                    if (HttpClient.log.isTraceEnabled()) {
                        HttpClient.log.trace("header '" + headerName
                                             + "' not loaded from XML file as it is a not important one");
                    }
                    break;
                }
            }
            if (isSignificantHeader) {
                significantHeaders.add(header);
            }
        }
        return significantHeaders.toArray(new Node[significantHeaders.size()]);
    }

    /**
     * Pretty print XML Node
     * @param node
     * @return
     * @throws XmlUtilitiesException
     */
    public String xmlNodeToString( Node node ) throws XmlUtilitiesException {

        StringWriter sw = new StringWriter();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4");
            transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_LINE_SEPARATOR, "\n");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            throw new XmlUtilitiesException("Error transforming XML node to String", te);
        }
        return sw.toString().trim();
    }

    /**
     *
     * @param xmlString xml string
     * @return xml Document object
     * @throws XmlUtilitiesException
     */
    public static Document stringToXmlDocumentObj( String xmlString ) throws XmlUtilitiesException {

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(new ByteArrayInputStream(xmlString.getBytes()));
        } catch (Exception e) {
            throw new XmlUtilitiesException("Error transforming String to XML document", e);
        }
    }

    public static Node[] getChildrenNodes( Node parentNode, String childrenName ) {

        List<Node> childrenList = new ArrayList<Node>();
        NodeList childrenNodes = parentNode.getChildNodes();
        for (int iChildren = 0; iChildren < childrenNodes.getLength(); iChildren++) {
            Node childNode = childrenNodes.item(iChildren);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (childrenName == null) {
                    // get all children
                    childrenList.add(childNode);
                } else if (childNode.getNodeName().equalsIgnoreCase(childrenName)) {
                    // get children with specified name only
                    childrenList.add(childNode);
                }
            }
        }

        return childrenList.toArray(new Node[childrenList.size()]);
    }

    /**
    *
    * @param RestHelper httpClient
    * @param actionsXml full name (with path) of the actions file. Used for generating relative paths for body contents
    * @param actionNum action number among all ones in the file
    * @param saveResponseBodyBytes whether to save the response body bytes even the resource file content
    * @return XML Node with parsed HTTP response
    * @throws Exception
    */
    public ActionParser readActionResponse( HttpClient httpClient, String actionsXml, int actionNum,
                                            boolean saveResponseBodyBytes ) throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();

        Node httpActions = dom.createElement(TOKEN_HTTP_ACTIONS);
        dom.appendChild(httpActions);
        Node httpAction = dom.createElement(TOKEN_HTTP_ACTION);
        httpActions.appendChild(httpAction);

        Node actualResponseWithoutBodyNode = dom.createElement(TOKEN_HTTP_RESPONSE);
        httpAction.appendChild(actualResponseWithoutBodyNode);

        // read connection input stream bytes
        int contentLength = 0;
        byte[] responseBodyBytes = null;
        int responseCode = -1;
        httpClient.getNetworkingStopWatch().step6_StartGetResponseCode();

        responseCode = httpClient.getUrlConnection().getResponseCode(); // this effectively may send request and wait for response
        httpClient.getNetworkingStopWatch().step7_EndGetResponseCode();

        if (httpClient.getUrlConnection().getDoInput()
            // if the response code is "302 Found" we assume that the response body is empty
            && responseCode != 302) {

            // save the retrieved file
            Node resourceFileNode = httpClient.httpBodyToXml(dom, actionsXml, actionNum,
                                                             saveResponseBodyBytes);
            if (httpClient.getResponseBodyBytes() != null) { // for disc write or error logging
                responseBodyBytes = httpClient.getResponseBodyBytes();
            }
            if (resourceFileNode != null) {
                actualResponseWithoutBodyNode.appendChild(resourceFileNode);
                if ( ((Element) resourceFileNode).hasAttribute("size")) {
                    contentLength = Integer.parseInt( ((Element) resourceFileNode).getAttribute("size"));
                }
            }
        }

        // add headers
        actualResponseWithoutBodyNode = httpClient.readHeaders(dom, actualResponseWithoutBodyNode,
                                                               contentLength);
        ActionParser actionResponse = new ActionParser(actualResponseWithoutBodyNode, responseBodyBytes);
        actionResponse.setContentType(httpClient.getUrlConnection().getContentType());
        return actionResponse;
    }

    /**
     * Headers are read in the following way(the next overwrites the previous):
     *  - headers from XML
     *  - globally disabled headers
     *  - headers from test case
     *
     * @param actionName
     * @param stepIndex
     * @param expectedHeaderMatchers
     * @param actualHttpResponseNode
     * @throws XmlUtilitiesException
     * @throws InvalidMatcherException
     */
    private void
            verifyResponseHeaders( String actionName, int stepIndex,
                                   List<HeaderMatcher> expectedHeaderMatchers,
                                   ActionParser actualHttpResponse,
                                   TemplateActionsResponseVerificationConfigurator verificationConfigurator ) throws XmlUtilitiesException,
                                                                                                              InvalidMatcherException {

        Node[] actualHeaderNodes = getChildrenNodes(actualHttpResponse.getActionNodeWithoutBody(),
                                                    TOKEN_HTTP_HEADER);

        // Collect all header matchers from the XML file and the test code.
        // We keep them in a map, so if same header is specified in the XML and the test, the one coming
        // from the test will get precedence
        Map<String, HeaderMatcher> headerMatchersMap = new HashMap<String, HeaderMatcher>();
        // Collect all matchers coming from the static XML file
        for (HeaderMatcher headerMatcher : expectedHeaderMatchers) {
            headerMatchersMap.put(headerMatcher.getHeaderName(), headerMatcher);
        }
        // Collect all global header matchers, this is coming from the test case
        for (HeaderMatcher globalHeaderMatcher : verificationConfigurator.getGlobalHeaderMatchers()) {
            // header matcher from global rule so we assume it is not significant if not already existing - forceOptionalHeaderIfNotAlreadyExisting=true
            headerMatchersMap = addHeaderMatcherToMap(headerMatchersMap, globalHeaderMatcher, true);
        }

        // Collect all matchers coming from the test case
        // We do not check if this is an important header, this way user can specify to check a header for
        // this action step even if it is classified globally as a not important header
        TemplateActionResponseVerificator responseVerificator = verificationConfigurator.getActionVerificator(actionName);
        if (responseVerificator != null) {
            List<HeaderMatcher> headerMatchers = responseVerificator.getStepHeaderMatchers(stepIndex);
            for (HeaderMatcher headerMatcher : headerMatchers) {
                // header matcher from Java test code so we assume it is significant - forceOptionalHeaderIfNotAlreadyExisting=false
                headerMatchersMap = addHeaderMatcherToMap(headerMatchersMap, headerMatcher, false);
            }
        }

        // Now try to match all available header matchers for this action step
        boolean[] processedHeaderNodes = new boolean[actualHeaderNodes.length];
        for (HeaderMatcher headerMatcher : headerMatchersMap.values()) {
            for (int i = 0; i < actualHeaderNodes.length; i++) {
                String actualHeaderName = getNodeAttribute(actualHeaderNodes[i],
                                                           TOKEN_HEADER_NAME_ATTRIBUTE);
                if (actualHeaderName.equals(headerMatcher.getHeaderName())) {
                    // mark the header node as processed
                    processedHeaderNodes[i] = true;

                    // try to match this header
                    String actualHeaderValue = getNodeAttribute(actualHeaderNodes[i],
                                                                TOKEN_HEADER_VALUE_ATTRIBUTE);
                    if (!headerMatcher.performMatch(null, actualHeaderValue)) {
                        // header did not match
                        String causeMsg = "Did not match header value '" + actualHeaderValue + "' for "
                                          + headerMatcher.toString() + ".";
                        logActualResponse(causeMsg, actionName, stepIndex, actualHttpResponse, true);
                        throw new XmlUtilitiesException(causeMsg);
                    }
                    break;
                }
            }
        }

        // check if some matchers were not processed at all
        // this means some expected header was not received
        for (HeaderMatcher headerMatcher : headerMatchersMap.values()) {
            if (!headerMatcher.wasProcessed() && !headerMatcher.isOptionalHeader()) {
                String causeMsg = "Did not receive the expected header for " + headerMatcher.toString() + ".";
                logActualResponse(causeMsg, actionName, stepIndex, actualHttpResponse, true);
                throw new XmlUtilitiesException(causeMsg);
            }
        }
    }

    private Map<String, HeaderMatcher>
            addHeaderMatcherToMap( Map<String, HeaderMatcher> headerMatchersMap, HeaderMatcher headerMatcher,
                                   boolean forceOptionalHeaderIfNotAlreadyExisting ) throws InvalidMatcherException {

        final String headerName = headerMatcher.getHeaderName();
        if (headerMatcher.isMergingMatcher() && headerMatchersMap.containsKey(headerName)) {
            // this header is already present in the map and we must merge it with the existing one
            // instead of replacing it
            headerMatcher.mergeTo(headerMatchersMap.get(headerName));
            headerMatchersMap.put(headerName, headerMatcher);
        } else {
            if (!headerMatchersMap.containsKey(headerName)) {
                // no such header exists in template/map so far
                // so we should not enforce the global header matcher in the particular request
                headerMatcher.setOptionalHeader(forceOptionalHeaderIfNotAlreadyExisting);
            }
            // add this header to the map, if there is already an existing one for this header name - we will override it
            headerMatchersMap.put(headerName, headerMatcher);
        }
        return headerMatchersMap;
    }

    private void verifyResponseFile( ActionResponseObject expectedHttpResponseNode,
                                     Node actualHttpResponseNode ) throws XmlUtilitiesException {

        String expectedResponseFile = expectedHttpResponseNode.getResourceFile();
        Node actualResponseFileNode = getFirstChildNode(actualHttpResponseNode, TOKEN_HTTP_RESOURCE_FILE);
        if (expectedResponseFile == null && actualResponseFileNode == null) {

            // no file is expected and no file was received
        } else if (expectedResponseFile != null && actualResponseFileNode != null) {
            if (matchFilesBySize || matchFilesByContent /* pre-check before MD5 sum */ ) {

                String expectedFileSize = expectedHttpResponseNode.getResourceFileSize();
                String actualFileSize = getNodeAttribute(actualResponseFileNode, "size");

                if ( (actualFileSize == null && expectedFileSize != null)
                     || (actualFileSize != null && expectedFileSize == null)
                     || (actualFileSize != null && !actualFileSize.equals(expectedFileSize))) {

                    throw new XmlUtilitiesException("The expected response file '" + expectedResponseFile
                                                    + "' has the length of " + expectedFileSize
                                                    + " while the actual has the length of "
                                                    + actualFileSize);
                }

            }
            if (matchFilesByContent) {

                // compare both files using MD5 sums
                String actualResponseFile = expectedResponseFile.substring(0,
                                                                           expectedResponseFile.lastIndexOf(AtsSystemProperties.SYSTEM_FILE_SEPARATOR))
                                            + AtsSystemProperties.SYSTEM_FILE_SEPARATOR + "actual"
                                            + AtsSystemProperties.SYSTEM_FILE_SEPARATOR
                                            + Thread.currentThread().getName()
                                            + AtsSystemProperties.SYSTEM_FILE_SEPARATOR
                                            + actualResponseFileNode.getTextContent();

                String expectedFileMD5Sum;
                String actualFileMD5Sum;
                LocalFileSystemOperations localFileOperations = new LocalFileSystemOperations();

                // optimization - check first file size:

                try {
                    expectedFileMD5Sum = localFileOperations.computeMd5Sum(expectedResponseFile,
                                                                           Md5SumMode.BINARY);
                } catch (Exception e) {
                    throw new XmlUtilitiesException("Error calculating MD5 sum for " + expectedResponseFile,
                                                    e);
                }

                try {
                    actualFileMD5Sum = localFileOperations.computeMd5Sum(actualResponseFile,
                                                                         Md5SumMode.BINARY);
                } catch (Exception e) {
                    throw new XmlUtilitiesException("Error calculating MD5 sum for " + actualResponseFile,
                                                    e);
                }

                if (!expectedFileMD5Sum.equalsIgnoreCase(actualFileMD5Sum)) {
                    throw new XmlUtilitiesException("The expected response file '" + expectedResponseFile
                                                    + "' is not the same as the actual '"
                                                    + actualResponseFile + "'");
                }
            }

        } else {
            throw new XmlUtilitiesException("Expected to " + (expectedResponseFile != null
                                                                                           ? "receive the "
                                                                                             + expectedResponseFile
                                                                                             + " file"
                                                                                           : "not receive a response file")
                                            + ", but " + (actualResponseFileNode != null
                                                                                         ? "received the "
                                                                                           + actualResponseFileNode.getTextContent()
                                                                                           + " file"
                                                                                         : "did not receive a response file"));
        }
    }

    /**
     * Applies the user parameters, but does not modify the original (as they come from the XML file)
     * matchers
     *
     * @param staticXpathBodyMatchers
     * @return
     * @throws InvalidMatcherException
     */
    private List<XPathBodyMatcher>
            applyUserParameters( List<XPathBodyMatcher> staticXpathBodyMatchers ) throws InvalidMatcherException {

        List<XPathBodyMatcher> actualXpathBodyMatchers = new ArrayList<XPathBodyMatcher>();

        for (XPathBodyMatcher matcher : staticXpathBodyMatchers) {
            String matcherValue = matcher.getMatcherValue();
            String attributeName = (String) ThreadContext.getAttribute(matcherValue);
            if (attributeName != null) {
                matcherValue = ThreadContext.getAttribute(attributeName).toString();
            }

            actualXpathBodyMatchers.add(new XPathBodyMatcher(matcher.getXpath(), matcherValue,
                                                             matcher.getMatchMode()));
        }

        return actualXpathBodyMatchers;
    }

    /**
     * Get values matching passed XPath expression
     * @param node
     * @param expression XPath expression
     * @return matching values
     * @throws Exception
     */
    private static String[] getByXpath( Node node, String expression ) throws Exception {

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        XPathExpression xPathExpression = xPath.compile(expression);

        NodeList nlist = (NodeList) xPathExpression.evaluate(node, XPathConstants.NODESET);

        int nodeListSize = nlist.getLength();
        List<String> values = new ArrayList<String>(nodeListSize);
        for (int index = 0; index < nlist.getLength(); index++) {
            Node aNode = nlist.item(index);
            values.add(aNode.getTextContent());
        }
        return values.toArray(new String[values.size()]);
    }

    /**
     * Log the response body content. This method is used for error logging or traces.
     *
     * @param actionName action name
     * @param stepNumber action step number starting from 1
     * @param actualHttpResponse data for the actual response
     * @param isError if this should be logged as error or just for tracing/debugging purposes
     */
    private void logActualResponse( String causeMsg, String actionName, int stepNumber,
                                    ActionParser actualHttpResponse, boolean isError ) {

        try {

            StringBuilder logMsg = new StringBuilder();
            logMsg.append( /*"Response verification failed. " + */causeMsg + "\n Dumping response of action "
                           + actionName + "[" + stepNumber + "]:\n"
                           + xmlNodeToString(actualHttpResponse.getActionNodeWithoutBody()));

            // append missing body
            if (isContentPrintable(actualHttpResponse.getContentType())) {

                StringBuilder bodySB = new StringBuilder();
                if (actualHttpResponse.getBodyContentAsString() == null) {
                    bodySB.append(" null (Increase Log4J severity of " + HttpClient.class.getName()
                                  + " logger to TRACE if you want body contents)");
                } else {
                    bodySB.append(actualHttpResponse.getBodyContentAsString());
                }
                logMsg.append("\nResponse body:\n");
                logMsg.append(bodySB);
            }

            if (isError) {
                HttpClient.log.error(logMsg);
            } else {
                HttpClient.log.trace(logMsg);
            }
        } catch (Exception e) {

            log.error("Error during logging the actual response for " + actionName + "[" + stepNumber + "]",
                      e);
        }
    }

    /**
     *
     * @param contentType the content type
     * @return <code>true</code> if the content is printable or <code>false</code> if it is not
     */
    private boolean isContentPrintable( String contentType ) {

        if (contentType == null) {
            return false;
        }

        contentType = contentType.toLowerCase();
        for (String printableContentType : PRINTABLE_CONTENT_TYPES) {
            if (contentType.startsWith(printableContentType)) {
                return true;
            }
        }

        // the content is not printable
        return false;
    }
}
