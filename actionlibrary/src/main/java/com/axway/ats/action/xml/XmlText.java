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
package com.axway.ats.action.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.axway.ats.common.xml.XMLException;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;

/**
 * A parser for XML REST body.
 *
 * <br /> <b>Note:</b> Many of the supported methods return the instance of this object
 * which allows chaining the code like this:
 * <pre>
 *   new XMLText()
 *     .add("//person[@id=\"1.1\"]/name", "John")
 *     .add("//person[@id=\"1.1\"]/age", "20")
 *     .add("//person[@id=\"1.1\"]/sex", "Male");
 * </pre>
 * More info: {@link https://axway.github.io/ats-framework/REST-Operations---Parsing-XML-body.html}
 */
@PublicAtsApi
public class XmlText {

    private static final Logger log = LogManager.getLogger(XmlText.class);

    private Element             root;

    /**
     * Constructor which accepts the text content
     *
     * @param xmlText the content
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText( String xmlText ) throws XMLException {
        init(xmlText);
    }

    /**
     * Constructor which accepts the file , containing the text content
     *
     * @param xmlFile the file , containing the text content
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText( File xmlFile ) throws XMLException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(xmlFile))) {
            String line = null;
            while ( (line = br.readLine()) != null) {
                sb.append(line);
            }
            init(sb.toString());
        } catch (IOException | XMLException e) {
            throw new XMLException("Error parsing XML file: " + xmlFile.getAbsolutePath(), e);
        }
    }

    private XmlText( Element root ) throws XMLException {
        this.root = DocumentHelper.createElement("root");

        DocumentHelper.createDocument(this.root);

        this.root = root.createCopy();

        DocumentHelper.createDocument(this.root);
    }

    private void init(
                       String xmlText ) throws XMLException {

        Document document = null;
        try {
            document = new SAXReader().read(new StringReader(xmlText));
        } catch (DocumentException e) {
            throw new XMLException("Error parsing XML text:\n" + xmlText, e);
        }

        this.root = document.getRootElement();
    }

    /**
     * Add XML element from XMLText or String
     *
     * @param xpath XPath , pointing to a XML element
     * @param object the XML element
     * @return this instance
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText add(
                        String xpath,
                        Object object ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {
            throw new XMLException("Null/empty xpath is not allowed.");
        }

        if (object == null) {
            throw new XMLException("Null object is not allowed.");
        }

        Element newElement = null;

        Element parent = findElement(xpath);

        if (parent == null) {
            throw new XMLException("'" + xpath + "' is not a valid path");
        }

        if (object instanceof XmlText) {
            newElement = ((XmlText) object).root;
        }

        if (object instanceof String) {
            newElement = new XmlText((String) object).root;
        }

        if (newElement == null) {
            throw new XMLException("Given object for adding to xml document is from invallid class instance '"
                                   + object.getClass().getSimpleName() + "'. "
                                   + "Use String or XMLText instances only.");
        }

        parent.add(newElement);

        return this;
    }

    /**
     * Remove a XML element
     *
     * @param xpath XPath , pointing to a XML element
     * @return this instance
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText remove(
                           String xpath ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {
            throw new XMLException("Null/empty xpath is not allowed.");
        }

        Element element = findElement(xpath);

        if (element != null) {

            if (element.isRootElement()) {
                throw new XMLException("You cannot remove the root element of the XML document.");
            }

            element.detach();
        } else {
            throw new XMLException("'" + xpath + "' is not a valid path");
        }

        return this;
    }

    /**
     * Replace a XML element.
     *
     * @param xpath XPath , pointing to a XML element
     * @param object the new XML element
     * @return this instance
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText replace(
                            String xpath,
                            Object object ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {
            throw new XMLException("Null/empty xpath is not allowed.");
        }

        if (object == null) {
            throw new XMLException("Null object is not allowed for replacement."
                                   + "If you want to remove existing XML element, use XMLText.remove().");
        }

        Element newElement = null;

        if (object instanceof XmlText) {
            newElement = ((XmlText) object).root;
        }

        if (object instanceof String) {
            if (StringUtils.isNullOrEmpty((String) object)) {

                throw new XMLException("Null/empty String object is not allowed for replacement."
                                       + "If you want to remove existing XML element, use XMLText.remove().");

            }
            newElement = new XmlText((String) object).root;
        }

        if (newElement == null) {
            throw new XMLException("Given object for replacing an existing one is from invallid class instance. "
                                   + "Use String or XMLText instances only.");
        }

        Element oldElement = findElement(xpath);

        if (oldElement != null) {

            if (oldElement.isRootElement()) {
                throw new XMLException("You cannot replace the root element of the XML document.");
            }

            Element parent = oldElement.getParent();
            if (parent != null) {
                parent.elements().set(parent.elements().indexOf(oldElement), newElement);
            } else {
                throw new XMLException("Parent for element with xpath '" + xpath + "' could not be found.");
            }

        } else {
            throw new XMLException("'" + xpath + "' is not a valid path");
        }

        return this;
    }

    /**
     * Change the text of a XML element.
     *
     * @param xpath XPath , pointing to a XML element
     * @param text the new text for a XML element
     * @return this instance
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText setText(
                            String xpath,
                            String text ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {
            throw new XMLException("Null/empty xpath is not allowed.");
        }

        if (StringUtils.isNullOrEmpty(text)) {
            throw new XMLException("Null/empty text is not allowed.");
        }

        Element element = findElement(xpath);

        if (element == null) {
            throw new XMLException("'" + xpath + "' is not a valid path");
        }

        element.setText(text);

        return this;
    }

    /**
     * Append text to a XML element.
     *
     * @param xpath XPath , pointing to a XML element
     * @param text the text , which will be appended to a XML element
     * @return this instance
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText appendText(
                               String xpath,
                               String text ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {
            throw new XMLException("Null/empty xpath is not allowed.");
        }

        if (StringUtils.isNullOrEmpty(text)) {
            throw new XMLException("Null/empty text is not allowed.");
        }

        Element element = findElement(xpath);

        if (element == null) {
            throw new XMLException("'" + xpath + "' is not a valid path");
        }

        element.addText(text);

        return this;
    }

    /**
     * @param xpath XPath , pointing to a XML element
     * @return XML Text which is part of the initial XML text
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText get(
                        String xpath ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {
            throw new XMLException("Null/empty xpath is not allowed.");
        }

        Element element = findElement(xpath);

        if (element == null) {
            throw new XMLException("'" + xpath + "' is not a valid path");
        }

        return new XmlText(element);

    }

    /**
     * @param xpath XPath , pointing to a XML element
     * @return a String XML value
     * @throws XMLException
     */
    @PublicAtsApi
    public String getString(
                             String xpath ) throws XMLException {

        Object object = get(xpath);

        Element root = ((XmlText) object).root;

        if (root.isTextOnly()) {

            object = root.getText().trim();

        } else {

            throw new XMLException("'" + xpath + "' does not point to a String value:\n"
                                   + object.toString());

        }

        return (String) object;
    }

    /**
     * @param xpath XPath , pointing to a XML element
     * @return an Integer XML value
     * @throws XMLException
     */
    @PublicAtsApi
    public int getInt(
                       String xpath ) throws XMLException {

        Object object = get(xpath);

        Element root = ((XmlText) object).root;

        if (root.isTextOnly()) {

            object = root.getText().trim();

        } else {

            object = "";

        }

        try {
            return Integer.parseInt( ((String) object).trim());
        } catch (NumberFormatException nfe) {
            throw new XMLException("'" + xpath + "' does not point to an int value:\n" + object.toString());
        }
    }

    /**
     * @param xpath XPath , pointing to a XML element
     * @return a boolean XML value
     * @throws XMLException
     */
    @PublicAtsApi
    public boolean getBoolean(
                               String xpath ) throws XMLException {

        Object object = get(xpath);

        Element root = ((XmlText) object).root;

        if (root.isTextOnly()) {

            object = root.getText().trim();

        } else {

            object = "";

        }

        if ("true".equalsIgnoreCase((String) object)
            || "false".equalsIgnoreCase((String) object)) {
            return Boolean.parseBoolean( ((String) object).trim());
        } else {
            throw new XMLException("'" + xpath + "' does not point to an boolean value:\n"
                                   + object.toString());
        }
    }

    /**
     * @param xpath XPath , pointing to a XML element
     * @return a float XML value
     * @throws XMLException
     */
    @PublicAtsApi
    public float getFloat(
                           String xpath ) throws XMLException {

        Object object = get(xpath);

        Element root = ((XmlText) object).root;

        if (root.isTextOnly()) {

            object = root.getText().trim();

        } else {

            object = "";

        }

        try {
            return Float.parseFloat( ((String) object).trim());
        } catch (NumberFormatException nfe) {
            throw new XMLException("'" + xpath + "' does not point to a float value:\n"
                                   + object.toString());
        }
    }

    /**
     * @param xpath XPath , pointing to a XML element
     * @return a HashMap , containing the attributes and their values
     * @throws XMLException
     */
    @PublicAtsApi
    public Map<String, String> getAttributes(
                                              String xpath ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {

            throw new XMLException("Null/empty xpath is not allowed.");

        }

        Element element = findElement(xpath);

        if (element == null) {

            throw new XMLException("'" + xpath + "' is not a valid path");

        }

        HashMap<String, String> attributes = new HashMap<>(1);

        Iterator<Attribute> it = element.attributeIterator();

        while (it.hasNext()) {
            Attribute attr = it.next();
            attributes.put(attr.getName(), attr.getValue());
        }

        return attributes;

    }

    /**
     * @param xpath XPath , pointing to a XML element
     * @param name the name of the attribute
     * @return the value of the attribute
     * @throws XMLException
     */
    @PublicAtsApi
    public String getAttribute(
                                String xpath,
                                String name ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {

            throw new XMLException("Null/empty xpath is not allowed.");

        }

        if (StringUtils.isNullOrEmpty(name)) {

            throw new XMLException("Null/empty attribute name is not allowed.");

        }

        Element element = findElement(xpath);

        if (element == null) {

            throw new XMLException("'" + xpath + "' is not a valid path");

        }

        String attributeValue = element.attributeValue(name);

        if (attributeValue == null) {

            throw new XMLException("'" + name + "' attribute is not found for XML element with xpath '"
                                   + xpath + "'.");

        }

        return attributeValue;
    }

    /**
     * Adds attribute to XML element.
     *
     * @param xpath XPath , pointing to a XML element
     * @param name the name of the attribute
     * @param value the value of the attribute
     * @return this instance
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText setAttribute(
                                 String xpath,
                                 String name,
                                 String value ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {

            throw new XMLException("Null/empty xpath is not allowed.");

        }

        if (StringUtils.isNullOrEmpty(name)) {

            throw new XMLException("Null/empty attribute name is not allowed.");

        }

        if (StringUtils.isNullOrEmpty(value)) {

            throw new XMLException("Null/empty attribute value is not allowed.");

        }

        Element element = findElement(xpath);

        if (element == null) {

            throw new XMLException("'" + xpath + "' is not a valid path");

        }

        element.addAttribute(name, value);

        return this;

    }

    /**
     * Removes an attribute from XML element.
     *
     * @param xpath XPath , pointing to a XML element
     * @param name the name of the attribute
     * @return this instance
     * @throws XMLException
     */
    @PublicAtsApi
    public XmlText removeAttribute(
                                    String xpath,
                                    String name ) throws XMLException {

        if (StringUtils.isNullOrEmpty(xpath)) {

            throw new XMLException("Null/empty xpath is not allowed.");

        }

        if (StringUtils.isNullOrEmpty(name)) {

            throw new XMLException("Null/empty attribute name is not allowed.");

        }

        Element element = findElement(xpath);

        if (element == null) {

            throw new XMLException("'" + xpath + "' is not a valid path");

        }

        Attribute attribute = element.attribute(name);

        if (attribute == null) {

            throw new XMLException("'" + name
                                   + "' attribute cannot be found and replaced for element with xpath '"
                                   + xpath + "'.");

        }

        element.attributes().remove(attribute);

        return this;

    }

    /**
     * Returns the XPath of the root XML element.
     *
     * @return the XPath of the root XML element.
     * @throws XMLException
     */
    @PublicAtsApi
    public String getRootElementXPath() {

        return this.root.getUniquePath();
    }

    /**
     * Returns the child names of a XML element
     *
     * @return the XPath of the XML element.
     * @return the child names of a XML element
     * @throws XMLException
     */
    @PublicAtsApi
    public String[] getElementNames(
                                     String xpath ) throws XMLException {

        ArrayList<String> elementNames = new ArrayList<>(1);

        if (StringUtils.isNullOrEmpty(xpath)) {
            throw new XMLException("Null/empty xpath is not allowed.");
        }

        Element element = findElement(xpath);

        if (element == null) {
            throw new XMLException("'" + xpath + "' is not a valid path");
        }

        Iterator<Element> it = element.elementIterator();

        while (it.hasNext()) {
            elementNames.add(it.next().getName());
        }

        return elementNames.toArray(new String[elementNames.size()]);
    }

    /**
     * Returns the child XPaths of a XML element
     *
     * @return the XPath of the XML element.
     * @return the child XPaths of a XML element
     * @throws XMLException
     */
    @PublicAtsApi
    public String[] getElementXPaths(
                                      String xpath ) throws XMLException {

        ArrayList<String> elementXPaths = new ArrayList<>(1);

        if (StringUtils.isNullOrEmpty(xpath)) {
            throw new XMLException("Null/empty xpath is not allowed.");
        }

        Element element = findElement(xpath);

        if (element == null) {
            throw new XMLException("'" + xpath + "' is not a valid path");
        }

        Iterator<Element> it = element.elementIterator();

        while (it.hasNext()) {
            elementXPaths.add(it.next().getUniquePath());
        }

        return elementXPaths.toArray(new String[elementXPaths.size()]);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    @PublicAtsApi
    public String toString() {

        try {
            return getFormattedString(OutputFormat.createCompactFormat());
        } catch (XMLException e) {
            log.error(e.getMessage(), e.getCause());
            return "";
        }
    }

    /**
     * @return the XML as nicely formatted text
     */
    @PublicAtsApi
    public String toFormattedString() {

        try {
            return getFormattedString(OutputFormat.createPrettyPrint());
        } catch (XMLException e) {
            log.error(e.getMessage(), e.getCause());
            return "";
        }

    }

    private String getFormattedString(
                                       OutputFormat format ) throws XMLException {

        StringWriter sw = new StringWriter();
        XMLWriter writer = new XMLWriter(sw, format);
        try {
            writer.write(this.root.getDocument());
        } catch (IOException e) {
            throw new XMLException("Error returning formatted XMLText", e);
        } finally {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {}
        }

        return sw.toString();
    }

    private Element findElement(
                                 String xpath ) {

        if ("/".equals(xpath)) {
            return root;
        }

        return (Element) root.selectSingleNode(xpath);

    }

}
