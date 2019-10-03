/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.axway.ats.common.xml.XMLException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 *
 * Utility class with some basic methods working with XML
 *
 */
public final class XmlUtils {

    private XmlUtils() {

    }

    /**
     * Loads an XML file from an InputStream.
     * <br>
     * Note: the source stream is closed internally 
     *
     * @param configurationFileStream the source stream
     * @return the loaded XML document
     * @throws IOException for IO error
     * @throws SAXException for parsing exception
     */
    public static Document loadXMLFile( InputStream configurationFileStream ) throws IOException,
                                                                                     SAXException {

        try {
            DOMParser parser = getDomParser();
            parser.parse(new InputSource(configurationFileStream));

            return parser.getDocument();
        } finally {
            IoUtils.closeStream(configurationFileStream);
        }
    }

    /**
     * Loads an XML file from a String.
     *
     * @param xmlContentsStr the source file as String
     * @return the loaded XML document
     * @throws IOException for IO error
     * @throws SAXException for parsing exception
     */
    public static Document loadXML( String xmlContentsStr ) throws IOException,
                                                                   SAXException {

        DOMParser parser = getDomParser();
        parser.parse(xmlContentsStr);

        return parser.getDocument();

    }


    /**
     *
     * @param parent the parent {@link Element}
     * @param name the tag name to search for
     * @return {@link List} with matched {@link Element}s
     */
    public static List<Element> getChildrenByTagName( Element parent, String name ) {

        List<Element> nodeList = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                nodeList.add((Element) child);
            }
        }

        return nodeList;
    }

    /**
     *
     * @param node the node {@link Element}
     * @param attributeName the attribute name
     * @return the attribute value
     * @throws XMLException if the mandatory attribute is missing or its value is empty
     */
    public static String getMandatoryAttribute( Element node,
                                                String attributeName ) throws XMLException {

        String attributeValue = getAttribute(node, attributeName);
        if (StringUtils.isNullOrEmpty(attributeValue)) {
            throw new XMLException(node.getNodeName() + " is missing mandatory attribute '"
                                   + attributeName + "'");
        }

        return attributeValue;
    }

    /**
     *
     * @param node the node {@link Element}
     * @param attributeName the attribute name
     * @param defaultValue a default attribute value
     * @return the boolean attribute value
     */
    public static boolean getBooleanAttribute( Element node, String attributeName, boolean defaultValue ) {

        String attributeValue = getAttribute(node, attributeName);
        if (attributeValue == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(attributeValue);
    }

    /**
     *
     * @param node the node {@link Element}
     * @param attributeName the attribute name
     * @param defaultValue a default attribute value
     * @return the attribute value
     */
    public static String getAttribute( Element node, String attributeName, String defaultValue ) {

        String attributeValue = getAttribute(node, attributeName);
        if (attributeValue == null) {
            attributeValue = defaultValue;
        }
        return attributeValue;
    }

    /**
     *
     * @param node the node {@link Element}
     * @param attributeName the attribute name
     * @return the attribute value
     */
    public static String getAttribute( Element node, String attributeName ) {

        String attributeValue = null;
        Node attributeNode = node.getAttributes().getNamedItem(attributeName);
        if (attributeNode != null) {
            attributeValue = attributeNode.getNodeValue();
        }
        return attributeValue;
    }

    /**
     *
     * @param node the node {@link Element}
     * @param attributeName the attribute name
     * @param attributeValue the attribute value
     */
    public static void setAttribute( Element node, String attributeName, String attributeValue ) {

        node.setAttribute(attributeName, attributeValue);
    }

    private static DOMParser getDomParser() throws SAXNotRecognizedException, SAXNotSupportedException {

        DOMParser parser = new DOMParser();

        // Required settings from the DomParser
        parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
        parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
        parser.setFeature("http://apache.org/xml/features/allow-java-encodings", true);
        return parser;
    }
}
