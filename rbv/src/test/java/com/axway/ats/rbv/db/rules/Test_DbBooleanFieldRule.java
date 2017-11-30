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
package com.axway.ats.rbv.db.rules;

import java.util.ArrayList;

import org.junit.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.db.DbMetaData;
import com.axway.ats.rbv.db.rules.DbBooleanFieldRule;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

/**
 *  Contains unit test for the {@link DbBooleanFieldRule}
 */
public class Test_DbBooleanFieldRule extends BaseTest {

    private static DbBooleanFieldRule ruleTestTrueExpectTrue   = new DbBooleanFieldRule("test",
                                                                                        true,
                                                                                        "ruleTestTrueExpectTrue",
                                                                                        true);
    private static DbBooleanFieldRule ruleTestFalseExpectTrue  = new DbBooleanFieldRule("test",
                                                                                        false,
                                                                                        "ruleTestFalseExpectTrue",
                                                                                        true);
    private static DbBooleanFieldRule ruleTestTrueExpectFalse  = new DbBooleanFieldRule("test",
                                                                                        true,
                                                                                        "ruleTestTrueExpectFalse",
                                                                                        false);
    private static DbBooleanFieldRule ruleTestFalseExpectFalse = new DbBooleanFieldRule("test",
                                                                                        false,
                                                                                        "ruleTestFalseExpectFalse",
                                                                                        false);

    private static DbMetaData         testMetaData;

    @BeforeClass
    public static void setUpTest_DbDateFieldRule() {

        testMetaData = new DbMetaData();
    }

    // --- POSITIVE ---

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchBooleanPositive() throws RbvException {

        testMetaData.putProperty("test", true);
        Assert.assertTrue(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchNumberPositive() throws RbvException {

        testMetaData.putProperty("test", (Number) 1);
        Assert.assertTrue(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchDoublePositive() throws RbvException {

        testMetaData.putProperty("test", (Double) 1.0);
        Assert.assertTrue(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchLongPositive() throws RbvException {

        testMetaData.putProperty("test", (Long) 1L);
        Assert.assertTrue(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchStringPositive() throws RbvException {

        testMetaData.putProperty("test", "1");
        Assert.assertTrue(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchTrueExpectFalsePositive() throws RbvException {

        testMetaData.putProperty("test", "0");
        Assert.assertTrue(ruleTestTrueExpectFalse.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchFalseExpectFalsePositive() throws RbvException {

        testMetaData.putProperty("test", "1");
        Assert.assertTrue(ruleTestFalseExpectFalse.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchFalseExpectTruePositive() throws RbvException {

        testMetaData.putProperty("test", "0");
        Assert.assertTrue(ruleTestFalseExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchPositiveNull() throws RbvException {

        DbBooleanFieldRule rule = new DbBooleanFieldRule("test", null, "ruleTestTrueExpectTrue", true);
        testMetaData.putProperty("test", null);
        Assert.assertTrue(rule.isMatch(testMetaData));
    }

    // --- NEGATIVE ---

    /**
     * Negative test case
     * @throws RbvException
     */
    @Test
    public void testMatchTrueExpectTrueNegative() throws RbvException {

        testMetaData.putProperty("test", "0");
        Assert.assertFalse(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Negative test case
     * @throws RbvException
     */
    @Test
    public void testMatchFalseExpectTrueNegative() throws RbvException {

        testMetaData.putProperty("test", "1");
        Assert.assertFalse(ruleTestFalseExpectTrue.isMatch(testMetaData));
    }

    /**
     * Negative test case
     * @throws RbvException
     */
    @Test
    public void testMatchTrueExpectFalseNegative() throws RbvException {

        testMetaData.putProperty("test", "1");
        Assert.assertFalse(ruleTestTrueExpectFalse.isMatch(testMetaData));
    }

    /**
     * Negative test case
     * @throws RbvException
     */
    @Test
    public void testMatchFalseExpectFalseNegative() throws RbvException {

        testMetaData.putProperty("test", "0");
        Assert.assertFalse(ruleTestFalseExpectFalse.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchNumberNegative() throws RbvException {

        testMetaData.putProperty("test", (Number) 0);
        Assert.assertFalse(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchDoubleNegative() throws RbvException {

        testMetaData.putProperty("test", (Double) 0.0);
        Assert.assertFalse(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchLongNegative() throws RbvException {

        testMetaData.putProperty("test", (Long) 0L);
        Assert.assertFalse(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchStringNegative() throws RbvException {

        testMetaData.putProperty("test", "0");
        Assert.assertFalse(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test( expected = MetaDataIncorrectException.class)
    public void testMatchStringNegativeWrongValue() throws RbvException {

        testMetaData.putProperty("test", "#");
        Assert.assertFalse(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test( expected = MetaDataIncorrectException.class)
    public void testMatchIntegerNegativeWrongValue() throws RbvException {

        testMetaData.putProperty("test", -3);
        Assert.assertFalse(ruleTestTrueExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchNegativeNotNull() throws RbvException {

        DbBooleanFieldRule rule = new DbBooleanFieldRule("test", null, "ruleTestTrueExpectTrue", true);
        testMetaData.putProperty("test", "0");
        Assert.assertFalse(rule.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test
    public void testMatchNegativeNull() throws RbvException {

        DbBooleanFieldRule rule = new DbBooleanFieldRule("test", false, "ruleTestTrueExpectTrue", true);
        testMetaData.putProperty("test", null);
        Assert.assertFalse(rule.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test( expected = MetaDataIncorrectException.class)
    public void testMatchNegativeFloatingPoint() throws RbvException {

        testMetaData.putProperty("test", 0.1F);
        Assert.assertFalse(ruleTestFalseExpectTrue.isMatch(testMetaData));
    }

    /**
     * Positive test case
     * @throws RbvException
     */
    @Test( expected = MetaDataIncorrectException.class)
    public void testMatchNegativeWrongType() throws RbvException {

        testMetaData.putProperty("test", new ArrayList());
        Assert.assertFalse(ruleTestFalseExpectTrue.isMatch(testMetaData));
    }
}
