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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.common.xml.XMLException;

public class Test_XmlText extends BaseTest {

    private static final Logger log = LogManager.getLogger(Test_XmlText.class);

    private static File file;

    private static final StringBuilder body1 = new StringBuilder().append("<owner id=\"1\">")
                                                                  .append("<firstname>Dragoslav</firstname>")
                                                                  .append("<lastname>Slaveykov</lastname>")
                                                                  .append("<email>dslaveykov@a2007.bg</email>")
                                                                  .append("<phone>+359888224406</phone>")
                                                                  .append("</owner>");

    private static final StringBuilder body2 = new StringBuilder().append("<location>")
                                                                  .append("<country>Serbia</country>")
                                                                  .append("<city>")
                                                                  .append("<name>Beograd</name>")
                                                                  .append("<street>")
                                                                  .append("<name>bul. Mihajla Pupina</name>")
                                                                  .append("<number>9</number>")
                                                                  .append("</street>")
                                                                  .append("</city>")
                                                                  .append("</location>");

    private static final StringBuilder body3 = new StringBuilder().append("<engine>")
                                                                  .append("<fuel>gasoline</fuel>")
                                                                  .append("<volume>5000cc</volume>")
                                                                  .append("<power>650hp</power>")
                                                                  .append("</engine>");

    private static XmlText xmlText1;

    private static XmlText xmlText2;

    private static XmlText xmlText3;

    @Before
    public void setUpTest_XmlText() {

        file = new File(Test_XmlText.class.getResource("test.xml").getPath());

        try {
            xmlText1 = new XmlText(body1.toString());

            xmlText2 = new XmlText(body2.toString());

            xmlText3 = new XmlText(body3.toString());
        } catch (XMLException e) {
            log.error(e.getMessage());
        }
    }

    @Test
    public void constructXmlText() {

        String xpath = "//car[@id='1.1']/owner";

        String expectedValue = null;

        String actualValue = "";

        try {

            XmlText xml1 = new XmlText(file);

            expectedValue = xmlText1.toString();

            actualValue = xml1.get(xpath).toString();

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        log.info("root element XPath of xmlText3: " + xmlText3.getRootElementXPath());

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void constructXmlTextInvallidXMLString() {

        String xml = "<root><root>";

        XmlText xmlText = null;

        try {

            xmlText = new XmlText(xml);

        } catch (XMLException e) {

            log.error(e.getMessage());

        }

        assertEquals(null, xmlText);
    }

    @Test
    public void constructXmlTextInvallidXMLFile() {

        File xml = null;
        try {
            xml = File.createTempFile("temp_xml", null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        XmlText xmlText = null;

        try {

            xmlText = new XmlText(xml);

        } catch (XMLException e) {

            log.error(e.getMessage());

        }

        assertEquals(null, xmlText);

        xml.delete();
    }

    @Test
    public void constructXmlTextNullString() {

        XmlText xmlText = null;

        try {

            xmlText = new XmlText(new String());

        } catch (XMLException e) {

            log.error(e.getMessage());

        }

        assertEquals(null, xmlText);
    }

    @Test
    public void getXmlText() {

        String xpath = "/vehicles/cars[2]/car[1]/location";

        try {

            XmlText xml1 = new XmlText(file);

            assertEquals(xmlText2.toString(), xml1.get(xpath).toString());

        } catch (XMLException e) {
            log.error(e.getMessage());
        }
    }

    @Test
    public void getXmlTextInvallidXPath() {

        String xpath = "/vehicles/cars[20]";

        XmlText actualXMLText = null;

        try {

            XmlText xml = new XmlText(file);

            actualXMLText = xml.get(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(null, actualXMLText);
    }

    @Test
    public void getXmlTextEmptyXPath() {

        String xpath = "";

        XmlText actualXMLText = null;

        try {

            XmlText xml = new XmlText(file);

            actualXMLText = xml.get(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(null, actualXMLText);
    }

    @Test
    public void getString() {

        String str1 = "GOLF";

        String str2 = "diesel";

        String str3 = "+359888224406";

        String str4 = "bul. Hr. Botev";

        String str5 = "1400cc";

        String actualStr1 = null;

        String actualStr2 = null;

        String actualStr3 = null;

        String actualStr4 = null;

        String actualStr5 = null;

        try {

            XmlText xml = new XmlText(file);

            actualStr1 = xml.getString("//model[@series='MK4']");

            actualStr2 = xml.getString("/vehicles/cars[2]/car[1]/engine/fuel");

            actualStr3 = xml.getString("//owner[@id='1']/phone");

            actualStr4 = xml.getString("//car[2]/location/city/street/name");

            actualStr5 = xml.getString("//cars[2]/car[2]/engine/volume");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(str1, actualStr1);

        assertEquals(str2, actualStr2);

        assertEquals(str3, actualStr3);

        assertEquals(str4, actualStr4);

        assertEquals(str5, actualStr5);

    }

    @Test
    public void getStringNoStringValue() {

        String actualStr1 = null;

        try {

            XmlText xml = new XmlText(file);

            actualStr1 = xml.getString("//cars[1]/car[3]/location");

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(null, actualStr1);

    }

    @Test
    public void getIntValue() {

        int val1 = 1997;

        int val2 = 2004;

        int val3 = 34;

        int val4 = 4;

        int val5 = 104;

        int actualVal1 = -1;

        int actualVal2 = -1;

        int actualVal3 = -1;

        int actualVal4 = -1;

        int actualVal5 = -1;

        try {

            XmlText xml = new XmlText(file);

            actualVal1 = xml.getInt("//car[1]/date/year");

            actualVal2 = xml.getInt("//cars[2]/car[1]/date/year");

            actualVal3 = xml.getInt("//cars[2]/car[3]/location/city/street/number");

            actualVal4 = xml.getInt("//cars[1]/car[2]/date/month");

            actualVal5 = xml.getInt("//cars[2]/car[2]/location/city/street/number");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(val1, actualVal1);

        assertEquals(val2, actualVal2);

        assertEquals(val3, actualVal3);

        assertEquals(val4, actualVal4);

        assertEquals(val5, actualVal5);
    }

    @Test
    public void getIntValueNoIntValue() {

        int val1 = -1;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getInt("/vehicles/cars[1]/car[1]/owner");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(-1, val1);

    }

    @Test
    public void getIntValueNumberFormatException() {

        int val1 = -1;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getInt("/vehicles/cars[1]/car[1]/engine/volume");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(-1, val1);

    }

    @Test
    public void getIntValueInvallidXPath() {

        int val1 = -1;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getInt(null);

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(-1, val1);
    }

    @Test
    public void getFloatValue() {

        float val1 = -10000f;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getFloat("/vehicles/cars[2]/car[2]/date/year");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(2005f, val1);
    }

    @Test
    public void getFloatValueInvallidXPath() {

        float val1 = -10000f;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getFloat(null);

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(-10000f, val1);
    }

    @Test
    public void getFloatValueNumberFormatException() {

        float val1 = -10000f;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getFloat("//car[2]/engine/power");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(-10000f, val1);
    }

    @Test
    public void getFloatValueNoFloatValue() {

        float val1 = -10000f;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getFloat("/");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(-10000f, val1);
    }

    @Test
    public void getBoolean() {

        boolean val1 = false;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getBoolean("/vehicles/cars[2]/car[3]/used");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(true, val1);
    }

    @Test
    public void getBooleanInvallidXPath() {

        boolean val1 = false;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getBoolean("");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(false, val1);
    }

    @Test
    public void getBooleanNoBooleanValue() {

        boolean val1 = false;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getBoolean("/vehicles/cars[2]/car[2]/status/available");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(false, val1);
    }

    @Test
    public void getBooleanNoBooleanValueMixedContent() {

        boolean val1 = false;

        try {

            XmlText xml = new XmlText(file);

            val1 = xml.getBoolean("/vehicles/cars[2]/car[2]/date");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(false, val1);
    }

    @Test
    public void addXMLText() {

        XmlText xml = null;

        String expectedValue = null;

        String actualValue = null;

        try {

            xml = new XmlText(file);

            expectedValue = xmlText3.toString();

            xml.add("/vehicles/cars[1]/car[3]", xmlText3);

            actualValue = xml.get("/vehicles/cars[1]/car[3]/engine[2]").toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

            expectedValue = "should not see that";
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void addString() {

        XmlText xml = null;

        String expectedValue = null;

        String actualValue = null;

        try {

            xml = new XmlText(file);

            expectedValue = xmlText2.toFormattedString();

            xml.add("/vehicles/cars[2]/car[1]", body2.toString());

            actualValue = xml.get("/vehicles/cars[2]/car[1]/location[2]").toFormattedString();

        } catch (XMLException e) {
            log.error(e.getMessage());

            expectedValue = "should not see that";
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void addInvallidXPath() {

        String expectedValue = null;

        String actualValue = "text";

        try {

            XmlText xml = new XmlText(file);

            xml.add("/vehicles/carss[11]", xmlText3);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void addNullXPath() {

        String expectedValue = null;

        String actualValue = "text";

        try {

            XmlText xml = new XmlText(file);

            xml.add(null, xmlText3);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void addIvallidObject() {

        String expectedValue = null;

        String actualValue = null;

        try {

            XmlText xml = new XmlText(file);

            xml.add("/vehicles/cars[2]/car[1]", new Double(10.0));

            actualValue = "should not see that";

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void addNullObject() {

        String expectedValue = null;

        String actualValue = null;

        try {

            XmlText xml = new XmlText(file);

            xml.add("/vehicles/cars[1]/car[1]", null);

            actualValue = "should not see that";

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void remove() {

        String expectedValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<car>Hello</car>";

        String actualValue = null;

        try {

            XmlText xml = new XmlText("<car>Hello<model>GOLF</model></car>");

            xml.remove("//car/model");

            actualValue = xml.toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void removeInvallidXPath() {

        String expectedValue = null;

        String actualValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<car>Hello</car>";

        try {

            XmlText xml = new XmlText("<car>Hello<model>GOLF</model></car>");

            xml.remove("//car/model[@name='first']");

            actualValue = xml.toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void removeNullXPath() {

        String expectedValue = null;

        String actualValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<car>Hello</car>";

        try {

            XmlText xml = new XmlText("<car>Hello<model>GOLF</model></car>");

            xml.remove(null);

            actualValue = xml.toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void removeRootElement() {

        String expectedValue = null;

        String actualValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<car>Hello</car>";

        try {

            XmlText xml = new XmlText("<car name='a car'>Hello<model>GOLF</model></car>");

            xml.remove("//car[@name='a car']");

            actualValue = xml.toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void replaceWithXMLText() {

        String expectedValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body1.toString();

        String actualValue = null;

        String xpath = "//cars[2]/car[2]/owner";

        try {

            XmlText xml = new XmlText(file);

            xml.replace(xpath, xmlText1);

            actualValue = xml.get(xpath).toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(expectedValue, actualValue);

    }

    @Test
    public void replaceWithString() {

        String expectedValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body1.toString();

        String actualValue = null;

        String xpath = "//cars[2]/car[2]/owner";

        try {

            XmlText xml = new XmlText(file);

            xml.replace(xpath, body1.toString());

            actualValue = xml.get(xpath).toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(expectedValue, actualValue);

    }

    @Test
    public void replaceWithEmptyString() {

        String expectedValue = null;

        String actualValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body1.toString();

        String xpath = "//cars[2]/car[2]/owner";

        try {

            XmlText xml = new XmlText(file);

            xml.replace(xpath, new String());

            actualValue = xml.get(xpath).toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);

    }

    @Test
    public void replaceWithInvallidClass() {

        String expectedValue = null;

        String actualValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body1.toString();

        String xpath = "//cars[2]/car[2]/owner";

        try {

            XmlText xml = new XmlText(file);

            xml.replace(xpath, new StringBuilder(body1));

            actualValue = xml.get(xpath).toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void replaceRootElement() {

        String expectedValue = null;

        String actualValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body2.toString();

        String xpath = "/vehicles";

        try {

            XmlText xml = new XmlText(file);

            xml.replace(xpath, xmlText2);

            actualValue = xml.get("/").toString();

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void replaceWithNullObject() {

        String expectedValue = null;

        String actualValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body2.toString();

        String xpath = "/vehicles/cars[2]/car[1]/location";

        try {

            XmlText xml = new XmlText(file);

            xml.replace(xpath, null);

            actualValue = xml.get(xpath).toString();

            log.info(xml.toFormattedString());

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void replaceInvallidXPath() {

        String expectedValue = null;

        String actualValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body2.toString();

        String xpath = "/vehicles/cars[2]/car[7]/location[1]";

        try {

            XmlText xml = new XmlText(file);

            xml.replace(xpath, xmlText2);

            actualValue = xml.get(xpath).toString();

            log.info(xml.toFormattedString());

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void replaceNullXPath() {

        String expectedValue = null;

        String actualValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body2.toString();

        String xpath = null;

        try {

            XmlText xml = new XmlText(file);

            xml.replace(xpath, xmlText2);

            actualValue = xml.get(xpath).toString();

            log.info(xml.toFormattedString());

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void appendText() {

        String expectedValue = null;

        String actualValue = null;

        String xpath = "//car[@id='2.2']/mark";

        String text = "Sample Text";

        try {

            XmlText xml = new XmlText(file);

            expectedValue = xml.getString(xpath).concat(text);

            xml.appendText(xpath, text);

            actualValue = xml.getString(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());

            expectedValue = "should not see that";

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void appendTextInvallidXPath() {

        String expectedValue = null;

        String actualValue = null;

        String xpath = "//car[@id='2.222']/marks";

        String text = "Sample Text";

        try {

            XmlText xml = new XmlText(file);

            xml.appendText(xpath, text);

            actualValue = xml.getString(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void appendTextNullXPath() {

        String expectedValue = null;

        String actualValue = "text";

        String xpath = null;

        String text = null;

        try {

            XmlText xml = new XmlText(file);

            xml.appendText(xpath, text);

            actualValue = xml.getString(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void appendTextNullText() {

        String expectedValue = null;

        String actualValue = null;

        String xpath = "/vehicles/cars[1]/car[3]/model";

        String text = null;

        try {

            XmlText xml = new XmlText(file);

            xml.appendText(xpath, text);

            actualValue = xml.getString(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void setText() {

        String actualValue = null;

        String xpath = "//car[@id='2.2']/mark";

        String text = "Sample Text";

        try {

            XmlText xml = new XmlText(file);

            xml.setText(xpath, text);

            actualValue = xml.getString(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(text, actualValue);
    }

    @Test
    public void setTextNullText() {

        String expectedValue = "text";

        String actualValue = "text";

        String xpath = "//car[@id='2.2']/mark";

        String text = null;

        try {

            XmlText xml = new XmlText(file);

            xml.setText(xpath, text);

            actualValue = xml.getString(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void setTextInvallidXPath() {

        String expectedValue = null;

        String actualValue = null;

        String xpath = "//car[@id='2.2']/marks";

        String text = "text";

        try {

            XmlText xml = new XmlText(file);

            xml.setText(xpath, text);

            actualValue = xml.getString(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void setTextNullXPath() {

        String expectedValue = null;

        String actualValue = null;

        String xpath = null;

        String text = "text";

        try {

            XmlText xml = new XmlText(file);

            xml.setText(xpath, text);

            actualValue = xml.getString(xpath);

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getAttributes() {

        int expectedValue = 1;

        int actualValue = -1;

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.getAttributes("//cars[1]/car[1]/date").size();

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getAttributesFromElementWithoutAttributes() {

        int expectedValue = 0;

        int actualValue = 100;

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.getAttributes("//cars[1]/car[2]/date/day").size();

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getAttributesInvallidXPath() {

        int expectedValue = 0;

        int actualValue = 100;

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.getAttributes("//cars[1]/car[2]/date/days[1]").size();

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = 0;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getAttributesNullXPath() {

        int expectedValue = 0;

        int actualValue = 100;

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.getAttributes(null).size();

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = 0;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getAttribute() {

        String expectedValue = "dd/mm/yyyy";

        String actualValue = "";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.getAttribute("//cars[1]/car[2]/date", "format");

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getAttributeInvallidName() {

        String expectedValue = null;

        String actualValue = "some attr name";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.getAttribute("//cars[1]/car[2]/date", "invallid attr name");

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getAttributeNullName() {

        String expectedValue = null;

        String actualValue = "some attr name";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.getAttribute("//cars[1]/car[2]/date", null);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getAttributeInvallidXPath() {

        String expectedValue = null;

        String actualValue = "some attr name";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.getAttribute("//cars[2]/car[1]/date[2]", "format");

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getAttributeNullXPath() {

        String expectedValue = null;

        String actualValue = "some attr name";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.getAttribute(null, "format");

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void setAttribute() {

        String expectedValue = "new value";

        String actualValue = null;

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.setAttribute("/vehicles", "new_name", "new value").getAttribute("/vehicles",
                                                                                              "new_name");

        } catch (XMLException e) {
            log.error(e.getMessage());
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test // expected attribute does not check exception message in jUnit
    public void setAttribute_Negative_SpaceInName() {

        boolean expectedExcThrown = false;
        try {
            XmlText xml = new XmlText(file);
            xml.setAttribute("/vehicles", "new name", "new value");
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal character in name: 'new name'.", e.getMessage());
            expectedExcThrown = true;
        }
        if (!expectedExcThrown) {
            fail("Expected exception for illegal character but not thrown");
        }
    }

    @Test
    public void setAttributeNullXPath() {

        String expectedValue = null;

        String actualValue = "some value";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.setAttribute(null, "new name", "new value").getAttribute(null, "new name");

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void setAttributeInvallidXPath() {

        String expectedValue = null;

        String actualValue = "some value";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.setAttribute("/vehicles/cars[1]/car[1]/trucks[1]", "new name", "new value")
                             .getAttribute("/vehicles/cars[1]/car[1]/trucks[1]", "new name");

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void setAttributeNullName() {

        String expectedValue = null;

        String actualValue = "some value";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.setAttribute("/vehicles/cars[1]/car[1]", null, "new value")
                             .getAttribute("/vehicles/cars[1]/car[1]", null);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void setAttributeEmptyValue() {

        String expectedValue = null;

        String actualValue = "some value";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.setAttribute("/vehicles/cars[1]/car[1]", "new name", "")
                             .getAttribute("/vehicles/cars[1]/car[1]", "new name");

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void removeAttribute() {

        String expectedValue = null;

        String actualValue = "dd/mm/yyy";

        String name = "format";

        String xpath = "//car[@id='1.3']/date";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.removeAttribute(xpath, name).getAttribute(xpath, name);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);

    }

    @Test
    public void removeAttributeInvallidXPath() {

        String expectedValue = null;

        String actualValue = "dd/mm/yyy";

        String name = "format";

        String xpath = "//car[@id='1.30']/date";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.removeAttribute(xpath, name).getAttribute(xpath, name);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void removeAttributeNullXPath() {

        String expectedValue = null;

        String actualValue = "dd/mm/yyy";

        String name = "format";

        String xpath = null;

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.removeAttribute(xpath, name).getAttribute(xpath, name);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void removeAttributeInvallidName() {

        String expectedValue = null;

        String actualValue = "dd/mm/yyy";

        String name = "status";

        String xpath = "//car[@id='1.3']/date";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.removeAttribute(xpath, name).getAttribute(xpath, name);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void removeAttributeEmptyName() {

        String expectedValue = null;

        String actualValue = "dd/mm/yyy";

        String name = "";

        String xpath = "//car[@id='1.3']/date";

        try {

            XmlText xml = new XmlText(file);

            actualValue = xml.removeAttribute(xpath, name).getAttribute(xpath, name);

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValue = null;
        }

        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void getElementNames() {

        String[] expectedValues = new String[]{ "firstname", "lastname", "email", "phone" };

        String[] actualValues = new String[]{};

        try {

            XmlText xml = new XmlText(file);

            actualValues = xml.getElementNames("//cars[1]/car[2]/owner");

        } catch (XMLException e) {
            log.error(e.getMessage());

        }

        for (int i = 0; i < expectedValues.length; i++) {
            assertEquals(expectedValues[i], actualValues[i]);
        }
    }

    @Test
    public void getElementNamesInvallidXPath() {

        String[] expectedValues = new String[]{};

        String[] actualValues = new String[]{ "firstname", "lastname", "email", "phone" };

        try {

            XmlText xml = new XmlText(file);

            actualValues = xml.getElementNames("//cars[1]/car[2]/owner[2]");

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValues = new String[]{};
        }

        assertEquals(expectedValues.length, actualValues.length);
    }

    @Test
    public void getElementNamesEmptyXPath() {

        String[] expectedValues = new String[]{};

        String[] actualValues = new String[]{ "firstname", "lastname", "email", "phone" };

        try {

            XmlText xml = new XmlText(file);

            actualValues = xml.getElementNames("");

        } catch (XMLException e) {
            log.error(e.getMessage());

            actualValues = new String[]{};
        }

        assertEquals(expectedValues.length, actualValues.length);
    }

}
