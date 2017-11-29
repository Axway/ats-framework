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

import org.junit.After;
import org.junit.Test;

import com.axway.ats.agent.core.context.ThreadContext;
import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateHeaderMatchMode;

public class Test_HeaderMatcher {

    private static final String HEADER1               = "Header1";
    private static final String HEADER_VALUE1         = "Value 1";

    private static final String HEADER_NUMERIC_VALUE1 = "100";

    @After
    public void afterMethod() {

        ThreadContext.clear();
    }

    @Test
    public void gettersAndSetters() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, HEADER_VALUE1, TemplateHeaderMatchMode.EQUALS );

        assertEquals( HEADER1, matcher.getHeaderName() );
        assertEquals( HEADER_VALUE1, matcher.getHeaderValueToMatch() );

        assertFalse( matcher.isOptionalHeader() );
        matcher.setOptionalHeader( true );
        assertTrue( matcher.isOptionalHeader() );

        assertFalse( matcher.isMergingMatcher() );
        matcher = new HeaderMatcher( HEADER1, HEADER_NUMERIC_VALUE1, TemplateHeaderMatchMode.RANGE_OFFSET );
        assertTrue( matcher.isMergingMatcher() );
    }

    @Test(expected = InvalidMatcherException.class)
    public void headerNameIsNull() throws Exception {

        new HeaderMatcher( null, HEADER_VALUE1, TemplateHeaderMatchMode.EQUALS );
    }

    @Test(expected = InvalidMatcherException.class)
    public void headerNameIsEmpty() throws Exception {

        new HeaderMatcher( "", HEADER_VALUE1, TemplateHeaderMatchMode.EQUALS );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeValueMissingSplitterChar() throws Exception {

        new HeaderMatcher( HEADER1, "100", TemplateHeaderMatchMode.RANGE );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeValueSplitterCharMoreThanOnce() throws Exception {

        new HeaderMatcher( HEADER1, "100-200-300", TemplateHeaderMatchMode.RANGE );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeBadMinValue() throws Exception {

        new HeaderMatcher( HEADER1, "a-100", TemplateHeaderMatchMode.RANGE );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeBadMaxValue() throws Exception {

        new HeaderMatcher( HEADER1, "100-a", TemplateHeaderMatchMode.RANGE );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeMinValueBiggerThanMaxValue() throws Exception {

        new HeaderMatcher( HEADER1, "100-90", TemplateHeaderMatchMode.RANGE );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeoffsetBadValue() throws Exception {

        new HeaderMatcher( HEADER1, "100a", TemplateHeaderMatchMode.RANGE_OFFSET );
    }

    @Test(expected = InvalidMatcherException.class)
    public void rangeoffsetValueLessThanOne() throws Exception {

        new HeaderMatcher( HEADER1, "0", TemplateHeaderMatchMode.RANGE_OFFSET );
    }

    @Test
    public void jsessionid() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HeaderMatcher.SET_COOKIE_HEADER_NAME,
                                                   "",
                                                   TemplateHeaderMatchMode.EXTRACT );

        assertTrue( matcher.performMatch( null, "JSESSIONID=1234567890;Path=/" ) );
        assertEquals( "1234567890",
                      ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "JSESSIONID" ) );
    }

    @Test
    public void jsessionidNegative() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HeaderMatcher.SET_COOKIE_HEADER_NAME,
                                                   "",
                                                   TemplateHeaderMatchMode.EXTRACT );

        assertFalse( matcher.performMatch( null, "JSESSIONID=;1234567890;Path=/" ) );
    }

    @Test
    public void multipleCookies() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HeaderMatcher.SET_COOKIE_HEADER_NAME,
                                                   "",
                                                   TemplateHeaderMatchMode.EXTRACT );

        String setCookieHeader = "JSESSIONID=1kjwnen12345; TRANSLATED_SESSIONID=0nwpd7l; ppSession=\"FSD001@8300576:134941:VL2OPg9jyc+Pzv/kt8rA==\"";
        assertTrue( matcher.performMatch( null, setCookieHeader ) );

        assertEquals( "1kjwnen12345",
                      ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "JSESSIONID" ) );
        assertEquals( "0nwpd7l",
                      ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "TRANSLATED_SESSIONID" ) );
        assertEquals( "FSD001@8300576:134941:VL2OPg9jyc+Pzv/kt8rA==",
                      ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "ppSession" ) );
    }

    @Test
    public void multipleCookies_containingSkippedCookies() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HeaderMatcher.SET_COOKIE_HEADER_NAME,
                                                   "",
                                                   TemplateHeaderMatchMode.EXTRACT );

        String setCookieHeader = "JSESSIONID=1kjwnen12345;ppSession=\"FSD001@8300576:134941:VL%202OPg9 jyc+Pzv/kt8rA==\";Path=/ui;HttpOnly";
        assertTrue( matcher.performMatch( null, setCookieHeader ) );

        assertEquals( "1kjwnen12345",
                      ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "JSESSIONID" ) );
        assertEquals( "FSD001@8300576:134941:VL%202OPg9 jyc+Pzv/kt8rA==",
                      ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "ppSession" ) );
        assertEquals( null, ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "Path" ) );
        assertEquals( null, ThreadContext.getAttribute( ThreadContext.COOKIE_VAR_PREFFIX + "HttpOnly" ) );
    }

    @Test
    public void extractValue() throws Exception {

        final String paramName = this.getClass().getSimpleName() + "-newValue1";

        HeaderMatcher matcher = new HeaderMatcher( HEADER1,
                                                   "<double>${=" + paramName + "}.0</double>",
                                                   TemplateHeaderMatchMode.EXTRACT );

        assertNull( ThreadContext.getAttribute( paramName ) );
        assertTrue( matcher.performMatch( null, "<double>1000.0</double>" ) );
        assertEquals( "1000", ThreadContext.getAttribute( paramName ) );
    }

    @Test
    public void extractValueNegative() throws Exception {

        final String paramName = this.getClass().getSimpleName() + "-newValue2";

        HeaderMatcher matcher = new HeaderMatcher( HEADER1,
                                                   "<double>${=" + paramName + "}.0</double>",
                                                   TemplateHeaderMatchMode.EXTRACT );

        assertNull( ThreadContext.getAttribute( paramName ) );
        assertFalse( matcher.performMatch( null, "<integer>1000.0</integer>" ) );
        assertNull( ThreadContext.getAttribute( paramName ) );
    }

    @Test
    public void matchEquals() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "ABC", TemplateHeaderMatchMode.EQUALS );
        assertTrue( matcher.performMatch( null, "ABC" ) );
    }

    @Test
    public void matchEqualsNegative() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "ABC", TemplateHeaderMatchMode.EQUALS );
        assertFalse( matcher.performMatch( null, "ABCD" ) );
    }

    @Test
    public void matchContains() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "ABC", TemplateHeaderMatchMode.CONTAINS );
        assertTrue( matcher.performMatch( null, "ABCDE" ) );
    }

    @Test
    public void matchContainsNegative() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "ABCF", TemplateHeaderMatchMode.CONTAINS );
        assertFalse( matcher.performMatch( null, "ABCDE" ) );
    }

    @Test
    public void matchRange() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "100-200", TemplateHeaderMatchMode.RANGE );
        assertTrue( matcher.performMatch( null, "150" ) );
    }

    @Test
    public void matchRangeNegative() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "100-200", TemplateHeaderMatchMode.RANGE );
        assertFalse( matcher.performMatch( null, "250" ) );
    }

    @Test
    public void matchRangeBadActualValue() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "100-200", TemplateHeaderMatchMode.RANGE );
        assertFalse( matcher.performMatch( null, "250ABC" ) );
    }

    @Test
    public void matchRangeoffset() throws Exception {

        HeaderMatcher initialMatcher = new HeaderMatcher( HEADER1, "100", TemplateHeaderMatchMode.EQUALS );

        HeaderMatcher rangeoffsetMatcher = new HeaderMatcher( HEADER1,
                                                              "10",
                                                              TemplateHeaderMatchMode.RANGE_OFFSET );
        assertTrue( rangeoffsetMatcher.isMergingMatcher() );
        rangeoffsetMatcher.mergeTo( initialMatcher );

        assertTrue( rangeoffsetMatcher.performMatch( null, "105" ) );
    }

    @Test
    public void matchRangeoffsetNegative() throws Exception {

        HeaderMatcher initialMatcher = new HeaderMatcher( HEADER1, "100", TemplateHeaderMatchMode.EQUALS );

        HeaderMatcher rangeoffsetMatcher = new HeaderMatcher( HEADER1,
                                                              "10",
                                                              TemplateHeaderMatchMode.RANGE_OFFSET );
        assertTrue( rangeoffsetMatcher.isMergingMatcher() );
        rangeoffsetMatcher.mergeTo( initialMatcher );

        assertFalse( rangeoffsetMatcher.performMatch( null, "111" ) );
    }

    @Test
    public void matchRangeoffsetBadActualValue() throws Exception {

        HeaderMatcher initialMatcher = new HeaderMatcher( HEADER1, "100", TemplateHeaderMatchMode.EQUALS );

        HeaderMatcher rangeoffsetMatcher = new HeaderMatcher( HEADER1,
                                                              "10",
                                                              TemplateHeaderMatchMode.RANGE_OFFSET );
        assertTrue( rangeoffsetMatcher.isMergingMatcher() );
        rangeoffsetMatcher.mergeTo( initialMatcher );

        assertFalse( rangeoffsetMatcher.performMatch( null, "105ABC" ) );
    }

    @Test(expected = InvalidMatcherException.class)
    public void matchRangeoffsetBadBaseValue() throws Exception {

        HeaderMatcher initialMatcher = new HeaderMatcher( HEADER1, "10AB9", TemplateHeaderMatchMode.EQUALS );

        HeaderMatcher rangeoffsetMatcher = new HeaderMatcher( HEADER1,
                                                              "10",
                                                              TemplateHeaderMatchMode.RANGE_OFFSET );
        assertTrue( rangeoffsetMatcher.isMergingMatcher() );
        rangeoffsetMatcher.mergeTo( initialMatcher );
    }

    @Test
    public void matchRangeoffsetZeroBaseValue() throws Exception {

        HeaderMatcher initialMatcher = new HeaderMatcher( HEADER1, "0", TemplateHeaderMatchMode.EQUALS );

        HeaderMatcher rangeoffsetMatcher = new HeaderMatcher( HEADER1,
                                                              "10",
                                                              TemplateHeaderMatchMode.RANGE_OFFSET );
        assertTrue( rangeoffsetMatcher.isMergingMatcher() );
        rangeoffsetMatcher.mergeTo( initialMatcher );

        assertFalse( rangeoffsetMatcher.performMatch( null, "111" ) );
    }

    @Test(expected = InvalidMatcherException.class)
    public void matchRangeoffsetTryingToMergeWrongMatcher() throws Exception {

        HeaderMatcher initialMatcher = new HeaderMatcher( HEADER1, "100", TemplateHeaderMatchMode.EQUALS );

        HeaderMatcher notRangeoffsetMatcher = new HeaderMatcher( HEADER1, "10", TemplateHeaderMatchMode.REGEX );
        notRangeoffsetMatcher.mergeTo( initialMatcher );
    }

    @Test
    public void matchList() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "ABC,EFG,XYZ", TemplateHeaderMatchMode.LIST );
        assertTrue( matcher.performMatch( null, "EFG" ) );

        matcher = new HeaderMatcher( HEADER1, "EFG", TemplateHeaderMatchMode.LIST );
        assertTrue( matcher.performMatch( null, "EFG" ) );
    }

    @Test
    public void matchListNegative() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "ABC,EFG,XYZ", TemplateHeaderMatchMode.LIST );
        assertFalse( matcher.performMatch( null, "EFGH" ) );
    }

    @Test
    public void matchRegex() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, ".*ABC.*", TemplateHeaderMatchMode.REGEX );
        assertTrue( matcher.performMatch( null, "123ABC890" ) );
    }

    @Test
    public void matchRegexNegative() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, ".*AAA.*", TemplateHeaderMatchMode.REGEX );
        assertFalse( matcher.performMatch( null, "123ABC890" ) );
    }

    @Test
    public void matchRandom() throws Exception {

        HeaderMatcher matcher = new HeaderMatcher( HEADER1, "ABC", TemplateHeaderMatchMode.RANDOM );
        assertTrue( matcher.performMatch( null, "123312321" ) );
    }
}
