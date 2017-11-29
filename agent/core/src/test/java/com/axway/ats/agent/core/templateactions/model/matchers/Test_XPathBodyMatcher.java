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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.axway.ats.agent.core.context.ThreadContext;
import com.axway.ats.agent.core.templateactions.TemplateActionsBaseTest;
import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateBodyNodeMatchMode;

public class Test_XPathBodyMatcher extends TemplateActionsBaseTest {

    private static final String TEST_ACTIONS_HOME = TEST_RESOURCES_HOME + "TestMatchers/";

    private Document            doc;
    private Node                responseNode;

    private static final String XPATH1            = "//HTTP_RESPONSE/AMF_OBJECT/body/AMF_OBJECT[1]/subject";
    private static final String VALUE1            = "test subject";

    private static final String XPATH_NOT_UNIQUE  = "//HTTP_RESPONSE/AMF_OBJECT/body/AMF_OBJECT[1]/long";

    private static final String XPATH_NUMBER      = "//HTTP_RESPONSE/AMF_OBJECT/body/AMF_OBJECT[1]/long[1]";
    private static final String XPATH_BAD_NUMBER  = "//HTTP_RESPONSE/AMF_OBJECT/body/AMF_OBJECT[1]/long[2]";

    private static final String XPATH_TEXT        = "//HTTP_RESPONSE/AMF_OBJECT/body/AMF_OBJECT[1]/text";

    @Before
    public void beforeMethod() throws Exception {

        doc = DocumentBuilderFactory.newInstance()
                                    .newDocumentBuilder()
                                    .parse( new File( TEST_ACTIONS_HOME + "Test_XPathBodyMatcher.xml" ) );
        responseNode = doc.getFirstChild()
                          .getFirstChild()
                          .getNextSibling()
                          .getFirstChild()
                          .getNextSibling()
                          .getNextSibling()
                          .getNextSibling();
    }

    @Test
    public void gettersAndSetters() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH1, VALUE1, TemplateBodyNodeMatchMode.EQUALS );

        assertEquals( XPATH1, matcher.getXpath() );
        assertEquals( VALUE1, matcher.getMatcherValue() );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeValueMissingSplitterChar() throws Exception {

        new XPathBodyMatcher( XPATH1, "100", TemplateBodyNodeMatchMode.RANGE );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeValueSplitterCharMoreThanOnce() throws Exception {

        new XPathBodyMatcher( XPATH1, "100-200-300", TemplateBodyNodeMatchMode.RANGE );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeBadMinValue() throws Exception {

        new XPathBodyMatcher( XPATH1, "a-100", TemplateBodyNodeMatchMode.RANGE );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeBadMaxValue() throws Exception {

        new XPathBodyMatcher( XPATH1, "100-a", TemplateBodyNodeMatchMode.RANGE );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeMinValueBiggerThanMaxValue() throws Exception {

        new XPathBodyMatcher( XPATH1, "100-90", TemplateBodyNodeMatchMode.RANGE );
    }

    @Test
    public void notUniqueXpath() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH_NOT_UNIQUE,
                                                         VALUE1,
                                                         TemplateBodyNodeMatchMode.EQUALS );
        assertFalse( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void extractValue() throws Exception {

        final String paramName = this.getClass().getSimpleName() + "-newValue1";

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH1,
                                                         "${=" + paramName + "} subject",
                                                         TemplateBodyNodeMatchMode.EXTRACT );
        assertNull( ThreadContext.getAttribute( paramName ) );
        assertTrue( matcher.performMatch( null, responseNode ) );
        assertEquals( "test", ThreadContext.getAttribute( paramName ) );
    }

    @Test
    public void extractValueNegative() throws Exception {

        final String paramName = this.getClass().getSimpleName() + "-newValue2";

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH1,
                                                         "${=" + paramName + "} subjectttt",
                                                         TemplateBodyNodeMatchMode.EXTRACT );

        assertNull( ThreadContext.getAttribute( paramName ) );
        assertFalse( matcher.performMatch( null, responseNode ) );
        assertNull( ThreadContext.getAttribute( paramName ) );
    }

    @Test
    public void matchEquals() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH1, VALUE1, TemplateBodyNodeMatchMode.EQUALS );
        assertTrue( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchEqualsNegative() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH1,
                                                         "test subject2",
                                                         TemplateBodyNodeMatchMode.EQUALS );
        assertFalse( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchContains() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH1,
                                                         "test sub",
                                                         TemplateBodyNodeMatchMode.CONTAINS );
        assertTrue( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchContainsNegative() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH1,
                                                         "test subbb",
                                                         TemplateBodyNodeMatchMode.CONTAINS );
        assertFalse( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchRange() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH_NUMBER,
                                                         "90-110",
                                                         TemplateBodyNodeMatchMode.RANGE );
        assertTrue( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchRangeNegative() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH_NUMBER,
                                                         "110-200",
                                                         TemplateBodyNodeMatchMode.RANGE );
        assertFalse( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchRangeBadActualValue() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH_BAD_NUMBER,
                                                         "100-200",
                                                         TemplateBodyNodeMatchMode.RANGE );
        assertFalse( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchList() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH_TEXT,
                                                         "ABC,EFG,XYZ",
                                                         TemplateBodyNodeMatchMode.LIST );
        assertTrue( matcher.performMatch( null, responseNode ) );

        matcher = new XPathBodyMatcher( XPATH_TEXT, "EFG", TemplateBodyNodeMatchMode.LIST );
        assertTrue( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchListNegative() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH_TEXT,
                                                         "ABC,EFGH,XYZ",
                                                         TemplateBodyNodeMatchMode.LIST );
        assertFalse( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchRegex() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH_TEXT,
                                                         ".*EF.*",
                                                         TemplateBodyNodeMatchMode.REGEX );
        assertTrue( matcher.performMatch( null, responseNode ) );
    }

    @Test
    public void matchRegexNegative() throws Exception {

        XPathBodyMatcher matcher = new XPathBodyMatcher( XPATH_TEXT,
                                                         ".*AAA.*",
                                                         TemplateBodyNodeMatchMode.REGEX );
        assertFalse( matcher.performMatch( null, responseNode ) );
    }

}
