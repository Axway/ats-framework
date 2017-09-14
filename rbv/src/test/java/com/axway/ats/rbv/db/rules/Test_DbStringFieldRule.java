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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.db.DbMetaData;
import com.axway.ats.rbv.db.rules.DbStringFieldRule;
import com.axway.ats.rbv.db.rules.DbStringFieldRule.MatchRelation;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.NoSuchMetaDataKeyException;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.AndRuleOperation;

public class Test_DbStringFieldRule extends BaseTest {

    private static DbStringFieldRule ruleTest1ExpectTrue  = new DbStringFieldRule( "test1",
                                                                                   "test",
                                                                                   MatchRelation.EQUALS,
                                                                                   "ruleTest1ExpectTrue",
                                                                                   true );
    private static DbStringFieldRule ruleTest2ExpectTrue  = new DbStringFieldRule( "test2",
                                                                                   "test",
                                                                                   MatchRelation.EQUALS,
                                                                                   "ruleTest2ExpectTrue",
                                                                                   true );

    private static DbStringFieldRule ruleTest1ExpectFalse = new DbStringFieldRule( "test1",
                                                                                   "test",
                                                                                   MatchRelation.EQUALS,
                                                                                   "ruleTest1ExpectFalse",
                                                                                   false );
    private static DbStringFieldRule ruleTest2ExpectFalse = new DbStringFieldRule( "test2",
                                                                                   "test",
                                                                                   MatchRelation.EQUALS,
                                                                                   "ruleTest2ExpectFalse",
                                                                                   false );

    private static DbMetaData        testMetaData;

    @BeforeClass
    public static void setUpTest_DbDateFieldRule() {

        testMetaData = new DbMetaData();
    }

    @Test
    public void isMatchRelationEqualsPositive() throws RbvException {

        testMetaData.putProperty( "test", "test*(&%^$A&^%$" );

        DbStringFieldRule rule = new DbStringFieldRule( "test",
                                                        "test*(&%^$A&^%$",
                                                        MatchRelation.EQUALS,
                                                        "isMatchRelationEqualsPositive",
                                                        true );
        assertTrue( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchRelationEqualsNegative() throws RbvException {

        testMetaData.putProperty( "test", "test*(&%^$A&^%$12" );

        DbStringFieldRule rule = new DbStringFieldRule( "test",
                                                        "test*(&%^$A&^%$",
                                                        MatchRelation.EQUALS,
                                                        "isMatchRelationEqualsNegative",
                                                        true );
        assertFalse( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchRelationContainsPositive() throws RbvException {

        DbStringFieldRule rule = new DbStringFieldRule( "test",
                                                        "test*(&%^$A&^%$",
                                                        MatchRelation.CONTAINS,
                                                        "isMatchRelationContainsPositive",
                                                        true );

        testMetaData.putProperty( "test", "test*(&%^$A&^%$123" );
        assertTrue( rule.isMatch( testMetaData ) );

        testMetaData.putProperty( "test", "123test*(&%^$A&^%$" );
        assertTrue( rule.isMatch( testMetaData ) );

        testMetaData.putProperty( "test", "test*(&%^$A&^%$" );
        assertTrue( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchRelationContainsNegative() throws RbvException {

        testMetaData.putProperty( "test", "test*(&%^$A&^" );

        DbStringFieldRule rule = new DbStringFieldRule( "test",
                                                        "test*(&%^$A&^%$",
                                                        MatchRelation.CONTAINS,
                                                        "isMatchRelationContainsNegative",
                                                        true );
        assertFalse( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchRelationRegexPositive() throws RbvException {

        testMetaData.putProperty( "test", "test123" );

        DbStringFieldRule rule = new DbStringFieldRule( "test",
                                                        "test[1-3]+",
                                                        MatchRelation.REGEX_MATCH,
                                                        "isMatchRelationRegexPositive",
                                                        true );
        assertTrue( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchRelationRegexNegative() throws RbvException {

        testMetaData.putProperty( "test", "test123" );

        DbStringFieldRule rule = new DbStringFieldRule( "test",
                                                        "test[1-3]2",
                                                        MatchRelation.REGEX_MATCH,
                                                        "isMatchRelationRegexNegative",
                                                        true );
        assertFalse( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchNullActualValuePositive() throws RbvException {

        testMetaData.putProperty( "test", null );

        DbStringFieldRule rule = new DbStringFieldRule( "test",
                                                        null,
                                                        MatchRelation.CONTAINS,
                                                        "isMatchNullActualValue",
                                                        true );
        assertTrue( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchNullActualValueNegative() throws RbvException {

        testMetaData.putProperty( "test", null );

        DbStringFieldRule rule = new DbStringFieldRule( "test",
                                                        "test[1-3]2",
                                                        MatchRelation.CONTAINS,
                                                        "isMatchNullActualValue",
                                                        true );
        assertFalse( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchNullExpectedValueNegative() throws RbvException {

        testMetaData.putProperty( "test", "asdfas" );

        DbStringFieldRule rule = new DbStringFieldRule( "test",
                                                        null,
                                                        MatchRelation.CONTAINS,
                                                        "isMatchNullActualValue",
                                                        true );
        assertFalse( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchExpectedTruePositive() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test" );

        assertTrue( ruleTest1ExpectTrue.isMatch( metaData ) );
    }

    @Test
    public void isMatchExpectedTrueNegative() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test123" );

        assertFalse( ruleTest1ExpectTrue.isMatch( metaData ) );
    }

    @Test
    public void isMatchExpectedFalsePositive() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test123" );

        assertTrue( ruleTest1ExpectFalse.isMatch( metaData ) );
    }

    @Test
    public void isMatchExpectedFalseNegative() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test" );

        assertFalse( ruleTest1ExpectFalse.isMatch( metaData ) );
    }

    @Test
    public void isMatchExpectedTrueMultipleRulesPositive() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test" );
        metaData.putProperty( "test2", "test" );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( ruleTest1ExpectTrue );
        andRule.addRule( ruleTest2ExpectTrue );

        assertTrue( andRule.isMatch( metaData ) );
    }

    @Test
    public void isMatchExpectedTrueMultipleRulesNegativeNoMetaData() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test123" );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( ruleTest1ExpectTrue );
        andRule.addRule( ruleTest2ExpectTrue );

        assertFalse( andRule.isMatch( metaData ) );
    }

    @Test
    public void isMatchExpectedTrueMultipleRulesNegativeDontMatch() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test" );
        metaData.putProperty( "test2", "test123" );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( ruleTest1ExpectTrue );
        andRule.addRule( ruleTest2ExpectTrue );

        assertFalse( andRule.isMatch( metaData ) );
    }

    @Test
    public void isMatchExpectedFalseMultipleRulesPositiveNoMetaData() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test123" );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( ruleTest1ExpectFalse );
        andRule.addRule( ruleTest2ExpectFalse );

        assertTrue( andRule.isMatch( metaData ) );
    }

    @Test
    public void isMatchExpectedFalseMultipleRulesPositiveDontMatch() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test" );
        metaData.putProperty( "test2", "test123" );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( ruleTest1ExpectFalse );
        andRule.addRule( ruleTest2ExpectFalse );

        assertFalse( andRule.isMatch( metaData ) );
    }

    @Test
    public void isMatchExpectedFalseMultipleRulesNegative() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", "test" );
        metaData.putProperty( "test2", "test" );

        AndRuleOperation andRule = new AndRuleOperation();
        andRule.addRule( ruleTest1ExpectFalse );
        andRule.addRule( ruleTest2ExpectFalse );

        assertFalse( andRule.isMatch( metaData ) );
    }

    @Test(expected = MetaDataIncorrectException.class)
    public void isMatchIncorrectMetaData() throws RbvException {

        DbMetaData metaData = new DbMetaData();
        metaData.putProperty( "test1", new Object() );

        ruleTest1ExpectFalse.isMatch( metaData );
    }
}
