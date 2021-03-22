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
package com.axway.ats.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.axway.ats.config.exceptions.ConfigurationException;

/**
 * Presentation of a configuration resource.
 * Internally it is a list of key-value parameters.
 */
public class ConfigurationResource {

    private static final Logger log = LogManager.getLogger(ConfigurationResource.class);

    private Properties          properties;

    public ConfigurationResource() {

        properties = new Properties();
    }

    public String getProperty(
                               String name ) {

        String propertyValue = properties.getProperty(name);
        if (propertyValue != null) {
            propertyValue = propertyValue.trim();
        }

        return propertyValue;
    }

    public Map<String, String> getProperties(
                                              String prefix ) {

        HashMap<String, String> result = new HashMap<String, String>();

        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            String key = (String) property.getKey();
            String value = (String) property.getValue();
            if (value != null) {
                value = value.trim();
            }

            if (key.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.put(key, value);
            }
        }

        return result;
    }

    public Set<Entry<Object, Object>> getProperties() {

        return properties.entrySet();
    }

    public void setProperty(
                             String name,
                             String value ) {

        properties.setProperty(name, value);
    }

    public void loadFromXmlFile(
                                 InputStream resourceStream,
                                 String resourceIdentifier ) {

        try {
            DOMParser parser = new DOMParser();

            // Required settings from the DomParser
            parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false); // otherwise
            parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            parser.setFeature("http://apache.org/xml/features/allow-java-encodings", true);
            parser.parse(new InputSource(resourceStream));

            Document doc = parser.getDocument();
            Element rootElement = doc.getDocumentElement();

            //cleanup the properties
            properties.clear();

            //init the current element path
            LinkedList<String> currentElementPath = new LinkedList<String>();

            //start reading the DOM
            NodeList rootElementChildren = rootElement.getChildNodes();
            for (int i = 0; i < rootElementChildren.getLength(); i++) {
                Node rootElementChild = rootElementChildren.item(i);
                if (rootElementChild.getNodeType() == Node.ELEMENT_NODE) {
                    readXmlElement(currentElementPath, (Element) rootElementChild);
                }
            }
        } catch (SAXException e) {
            throw new ConfigurationException("Error while parsing config file '" + resourceIdentifier + "'",
                                             e);
        } catch (IOException ioe) {
            throw new ConfigurationException("Error while parsing config file '" + resourceIdentifier + "'",
                                             ioe);
        }
    }

    /**
     * This method will read a tree of elements and their attributes
     * 
     * @param element the root element of the tree
     */
    private void readXmlElement(
                                 LinkedList<String> currentElementPath,
                                 Element element ) {

        //append this node element to the current path
        currentElementPath.add(element.getNodeName());

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                readXmlElement(currentElementPath, (Element) childNode);
            }
        }

        //read all attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);

            String propertyName = getCurrentXmlElementPath(currentElementPath) + attribute.getName();
            String propertyValue = attribute.getValue();

            //put in the properties table
            properties.put(propertyName, propertyValue);

            log.debug("Added property with name '" + propertyName + "' and value '" + propertyValue + "'");
        }

        //after we are done with the node, remove it from the path
        currentElementPath.removeLast();
    }

    /**
     * Get the current element path as string
     * 
     * @return the element path as string
     */
    private String getCurrentXmlElementPath(
                                             LinkedList<String> currentElementPath ) {

        StringBuilder result = new StringBuilder();

        for (String element : currentElementPath) {
            result.append(element);
            result.append(".");
        }

        return result.toString();
    }

    public void loadFromPropertiesFile(
                                        InputStream resourceStream,
                                        String resourceIdentifier ) {

        try {
            properties.load(resourceStream);
        } catch (IOException ioe) {
            throw new ConfigurationException("Exception while reading configuration file '"
                                             + resourceIdentifier + "'", ioe);
        }
    }
}
