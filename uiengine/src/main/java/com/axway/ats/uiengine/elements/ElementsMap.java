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
package com.axway.ats.uiengine.elements;

import java.io.File;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.configuration.UiEngineConfigurator;
import com.axway.ats.uiengine.exceptions.ElementsMapException;

public class ElementsMap {

    private Logger             log                  = LogManager.getLogger(ElementsMap.class);

    /**
     * The singleton instance.
     */
    private static ElementsMap instance;

    private String             mapFileDocument;
    private Document           document;
    private Node               sectionNode;

    public static final String ATT_ELEMENT_NAME     = "name";

    public static final String ATT_ELEMENT_MAP_ID   = "mapID";

    public static final String ATT_ELEMENT_ID       = "id";

    public static final String ATT_ELEMENT_TYPE     = "type";

    public static final String ATT_ELEMENT_VALUE    = "value";

    public static final String ATT_ELEMENT_FRAME    = "frame";

    public static final String ATT_ELEMENT_FORMID   = "formid";

    public static final String ATT_ELEMENT_FORMNAME = "formname";

    public static final String ATT_ELEMENT_ID_EQ    = "mapID=";

    private ElementsMap() {

    }

    public static ElementsMap getInstance() {

        if (instance == null) {

            instance = new ElementsMap();
        }
        return instance;
    }

    public void loadMapFile( String mapFile, String mapSection ) {

        if (StringUtils.isNullOrEmpty(mapFile)) {
            throw new ElementsMapException("Error loading elements map file. Provided null/empty map file argument");
        }

        if (new File(mapFile).isAbsolute()) {
            mapFileDocument = mapFile;
        } else {
            mapFileDocument = IoUtils.normalizeUnixDir(UiEngineConfigurator.getInstance()
                                                                           .getMapFilesBaseDir())
                              + mapFile;
        }
        mapFileDocument = IoUtils.normalizeUnixFile(mapFileDocument);

        try {
            if (new File(mapFileDocument).exists()) {

                document = DocumentBuilderFactory.newInstance()
                                                 .newDocumentBuilder()
                                                 .parse(new File(mapFileDocument));

                if (log.isDebugEnabled()) {
                    log.debug("Successfully loaded map file '" + new File(mapFileDocument).getAbsolutePath()
                              + "'");
                }
            } else {
                InputStream mapFileIS = getClass().getClassLoader().getResourceAsStream(mapFileDocument);
                if (mapFileIS == null) {
                    throw new ElementsMapException("Map file '" + mapFileDocument
                                                   + "' doesn't exist neither on filesystem nor in classpath");
                }
                document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(mapFileIS);
                if (log.isDebugEnabled()) {
                    log.debug("Successfully loaded map file '" + mapFileDocument + "' from the classpath");
                }
            }
        } catch (Exception e) {
            throw new ElementsMapException("Error loading elements map file", e);
        }

        setMapSection(mapSection);
    }

    public void setMapSection( String mapSection ) {

        if (StringUtils.isNullOrEmpty(mapSection)) {

            throw new ElementsMapException("Error loading elements map section. Provided null/empty map section argument");
        }

        // load the section
        sectionNode = getSectionNode(mapSection);

        checkForDuplicatedMapIds();
    }

    private Node getSectionNode( String mapSection ) {

        NodeList nodeList = document.getElementsByTagName("Section");
        for (int i = 0; i < nodeList.getLength(); i++) {
            String attName = nodeList.item(i)
                                     .getAttributes()
                                     .getNamedItem(ATT_ELEMENT_NAME)
                                     .getNodeValue();
            if (mapSection.equalsIgnoreCase(attName)) {
                return nodeList.item(i);
            }
        }

        throw new ElementsMapException("Node section \"" + mapSection + "\" not found!");
    }

    private void checkForDuplicatedMapIds() {

        // load all elements for this section
        NodeList childNodes = sectionNode.getChildNodes();
        List<String> childNodeMapIds = new ArrayList<String>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                Element element = ((Element) childNodes.item(i));
                String elementMapId = element.getAttribute(ATT_ELEMENT_MAP_ID);
                if (StringUtils.isNullOrEmpty(elementMapId)) {
                    throw new ElementsMapException("Error reading " + ATT_ELEMENT_MAP_ID + " for element '"
                                                   + sectionNode.getNodeName() + "\\" + element.getNodeName()
                                                   + "'");
                }
                if (childNodeMapIds.contains(elementMapId)) {
                    throw new ElementsMapException("Map section '"
                                                   + ((Element) sectionNode).getAttribute("name")
                                                   + "' already contains element with the same mapId '"
                                                   + elementMapId + "'");
                }
                childNodeMapIds.add(elementMapId);
            }
        }
    }

    public String getMapSection() {

        if (sectionNode != null) {
            String mapSec = sectionNode.getAttributes().getNamedItem(ATT_ELEMENT_NAME).getNodeValue();
            if (!StringUtils.isNullOrEmpty(mapSec)) {
                return mapSec;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("No map section is set, no attribute of the current section can be get!");
        }

        return null;
    }

    public UiElementProperties getElementProperties( String mapId ) {

        if (document == null) {
            throw new ElementsMapException("Error loading '" + mapId
                                           + "' element from map. No map file is loaded!");
        }

        Element elementNode = getElement(sectionNode, mapId);

        // load the element properties
        UiElementProperties elementProperties = collectElementProperties(elementNode, mapId);
        elementProperties.addInternalProperty(UiElementProperties.MAP_ID_INTERNAL_PARAM, mapId);
        return elementProperties;
    }

    public UiElementProperties getSubElementProperties( String elementMapId, String subElementMapId ) {

        if (document == null) {
            throw new ElementsMapException("Error loading '" + subElementMapId
                                           + "' sub-element from map. No map file is loaded!");
        }

        Node elementNode = getElement(sectionNode, elementMapId);
        Element subElementNode = getElement(elementNode, subElementMapId);

        // load the element properties
        UiElementProperties elementProperties = collectElementProperties(subElementNode, subElementMapId);
        elementProperties.addInternalProperty(UiElementProperties.MAP_ID_INTERNAL_PARAM, subElementMapId);
        return elementProperties;
    }

    private Element getElement( Node node, String mapId ) {

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                Element element = ((Element) childNodes.item(i));
                if (mapId.equalsIgnoreCase(element.getAttribute(ATT_ELEMENT_MAP_ID))) {
                    return element;
                }
            }
        }
        throw new ElementsMapException("Error searching for '" + mapId + "' element in '" + getMapSection()
                                       + "' section in '" + mapFileDocument + "' file");

    }

    private UiElementProperties collectElementProperties( Element elementNode, String mapId ) {

        UiElementProperties elementProperties = new UiElementProperties();

        NamedNodeMap nodeAttributes = (elementNode).getAttributes();
        for (int i = 0; i < nodeAttributes.getLength(); i++) {
            Node node = nodeAttributes.item(i);
            if (StringUtils.isNullOrEmpty(node.getNodeValue())) {
                throw new ElementsMapException("Error loading '" + mapId + "' element from map. '"
                                               + node.getNodeName() + "' contains an empty value");
            }
            if (StringUtils.isNullOrEmpty(node.getNodeName())) {
                throw new ElementsMapException("Error loading '" + mapId + "' element from map. '"
                                               + node.getNodeValue() + "' contains an empty key");
            }
            elementProperties.addProperty(node.getNodeName(), node.getNodeValue());
        }

        return elementProperties;
    }
}
