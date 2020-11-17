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
package com.axway.ats.core.filesystem.snapshot.matchers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Element;

import com.axway.ats.core.filesystem.snapshot.XmlNode;

/**
 * Used to decide whether some XML nodes are to be skipped
 */
public class SkipXmlNodeMatcher extends SkipContentMatcher {

    private static final Logger        log = LogManager.getLogger(SkipXmlNodeMatcher.class);

    // list of node matchers
    private List<NodeValueMatcher>     nodeValueMatchers;
    private List<NodeAttributeMatcher> nodeAttributeMatchers;

    public SkipXmlNodeMatcher( String directoryAlias, String filePath ) {
        super(directoryAlias, filePath);

        this.nodeAttributeMatchers = new ArrayList<>();
        this.nodeValueMatchers = new ArrayList<>();
    }

    public List<NodeAttributeMatcher> getNodeAttributeMatchers() {

        return nodeAttributeMatchers;
    }

    public void addNodeAttributeMatcher( String xpath, String attributeKey, String attributeValue,
                                         MATCH_TYPE type ) {

        NodeAttributeMatcher newMatcher = new NodeAttributeMatcher(xpath, attributeKey, attributeValue,
                                                                   type);

        // check if there is already such matcher, we do not want to repeat
        for (NodeAttributeMatcher matcher : nodeAttributeMatchers) {
            if (matcher.getDescription().equalsIgnoreCase(newMatcher.getDescription())) {
                return; // do nothing
            }
        }

        nodeAttributeMatchers.add(newMatcher);
    }

    public List<NodeValueMatcher> getNodeValueMatchers() {

        return nodeValueMatchers;
    }

    public void addNodeValueMatcher( String xpath, String value, MATCH_TYPE type ) {

        NodeValueMatcher newMatcher = new NodeValueMatcher(xpath, value, type);

        // check if there is already such matcher, we do not want to repeat
        for (NodeValueMatcher matcher : nodeValueMatchers) {
            if (matcher.getDescription().equalsIgnoreCase(newMatcher.getDescription())) {
                return; // do nothing
            }
        }

        nodeValueMatchers.add(newMatcher);
    }

    public void addNodeValueMatchers( List<NodeValueMatcher> nodeMatchers ) {

        this.nodeValueMatchers.addAll(nodeMatchers);
    }

    public void process( String snapshot, XmlNode xmlNode ) {

        List<Element> matchedNodes = new ArrayList<>();

        // cycle all node attribute matchers
        for (NodeAttributeMatcher matcher : nodeAttributeMatchers) {
            matchedNodes.addAll(matcher.getMatchingNodes(snapshot, xmlNode));
        }

        // cycle all node value matchers
        for (NodeValueMatcher matcher : nodeValueMatchers) {
            matchedNodes.addAll(matcher.getMatchingNodes(snapshot, xmlNode));
        }

        for (Element matchedNode : matchedNodes) {
            xmlNode.removeChild(matchedNode);
        }
    }

    private String getDescription() {

        return "skip XML node matcher";
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("SkipXmlNodeMatcher");
        sb.append("\n\tDir alias: " + directoryAlias);
        sb.append("\n\tFile path: " + filePath);
        for (NodeAttributeMatcher matcher : nodeAttributeMatchers) {
            sb.append(matcher.toString());
        }
        for (NodeValueMatcher matcher : nodeValueMatchers) {
            sb.append(matcher.toString());
        }
        return sb.toString();
    }

    public class NodeAttributeMatcher implements Serializable {

        private static final long serialVersionUID = 1L;

        private String            xpath;

        private String            attributeKey;
        private String            attributeValue;

        private MATCH_TYPE        matchType;

        NodeAttributeMatcher( String xpath, String attributeKey, String attributeValue,
                              MATCH_TYPE matchType ) {
            this.xpath = xpath;
            this.attributeKey = attributeKey;
            this.attributeValue = attributeValue;
            this.matchType = matchType;
        }

        List<Element> getMatchingNodes( String snapshot, XmlNode xmlNode ) {

            // cycle all nodes that come for this XPath
            List<Element> matchedNodes = new ArrayList<>();

            List foundNodeObjects = xmlNode.getnode().selectNodes(this.xpath);
            if (foundNodeObjects != null) {
                for (Object foundNodeObject : foundNodeObjects) {
                    Element node = (Element) foundNodeObject;

                    String message = null;
                    Attribute attribute = node.attribute(attributeKey);
                    if (attribute != null) {
                        String attributeValue = attribute.getValue();
                        if (matchType == MATCH_TYPE.TEXT
                            && attributeValue.equalsIgnoreCase(this.attributeValue)) {
                            // equals text
                            message = "equals ignoring case '" + attributeValue + "'";
                        } else if (matchType == MATCH_TYPE.CONTAINS_TEXT
                                   && attributeValue.toLowerCase()
                                                    .contains(this.attributeValue.toLowerCase())) {
                            // contains text
                            message = "contains ignoring case '" + attributeValue + "'";
                        } else if (attributeValue.matches(this.attributeValue)) {
                            // matches regex
                            message = "matches the '" + attributeValue + "' regular expression";
                        }

                        if (message != null) {
                            if (log.isDebugEnabled()) {

                                log.debug("[" + snapshot + "] File " + filePath + ": Removing XML node "
                                          + new XmlNode(xmlNode, node).getSignature("")
                                          + " as its attribute '" + this.attributeKey + "="
                                          + this.attributeValue + "' has a value that " + message);
                            }
                            matchedNodes.add(node);
                        }
                    }
                }
            }

            return matchedNodes;
        }

        private String getDescription() {

            return "node attribute matcher: xpath=" + xpath + "; attributeKey=" + attributeKey
                   + "; attributeValue=" + attributeValue + "; match type=" + matchType.getDescription();
        }

        public String getXpath() {

            return xpath;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append("\n\tNode Attribute Matcher");
            sb.append("\n\t\txpath: " + xpath);
            sb.append("\n\t\tattributeKey: " + attributeKey);
            sb.append("\n\t\tattributeValue: " + attributeValue);
            if (matchType != null) {
                sb.append("\n\t\tmatchType: " + matchType.toString());
            }

            return sb.toString();
        }
    }

    public class NodeValueMatcher implements Serializable {

        private static final long serialVersionUID = 1L;

        private String            xpath;
        private String            value;

        private MATCH_TYPE        matchType;

        NodeValueMatcher( String xpath, String value, MATCH_TYPE matchType ) {
            this.xpath = xpath;
            this.value = value;
            this.matchType = matchType;
        }

        List<Element> getMatchingNodes( String snapshot, XmlNode xmlNode ) {

            // cycle all nodes that come for this XPath
            List<Element> matchedNodes = new ArrayList<>();

            List foundNodeObjects = xmlNode.getnode().selectNodes(this.xpath);
            if (foundNodeObjects != null) {
                for (Object foundNodeObject : foundNodeObjects) {
                    Element node = (Element) foundNodeObject;

                    String message = null;
                    String nodeValue = node.getStringValue();
                    if (matchType == MATCH_TYPE.TEXT && nodeValue.equalsIgnoreCase(value)) {
                        // equals text
                        message = "equals ignoring case '" + value + "'";
                    } else if (matchType == MATCH_TYPE.CONTAINS_TEXT
                               && nodeValue.toLowerCase().contains(this.value.toLowerCase())) {
                        // contains text
                        message = "contains ignoring case '" + value + "'";
                    } else if (nodeValue.matches(value)) {
                        // matches regex
                        message = "matches the '" + value + "' regular expression";
                    }

                    if (message != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + snapshot + "] File " + filePath + ": Removing XML node "
                                      + new XmlNode(xmlNode, node).getSignature("") + " as its value '"
                                      + nodeValue + "' " + message);
                        }
                        matchedNodes.add(node);
                    }
                }
            }

            return matchedNodes;
        }

        private String getDescription() {

            return "node matcher: xpath=" + xpath + "; value=" + value + "; match type="
                   + matchType.getDescription();
        }

        public String getXpath() {

            return xpath;
        }

        public String getValue() {

            return value;
        }

        public MATCH_TYPE getMatchType() {

            return matchType;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append("\n\tNode Value Matcher");
            sb.append("\n\t\txpath: " + xpath);
            sb.append("\n\t\tvalue: " + value);
            if (matchType != null) {
                sb.append("\n\t\tmatchType: " + matchType.toString());
            }

            return sb.toString();
        }
    }
}
