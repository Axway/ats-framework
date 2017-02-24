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

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.db.DbMetaData;
import com.axway.ats.rbv.db.rules.DbDateFieldRule;
import com.axway.ats.rbv.db.rules.DbDateFieldRule.MatchRelation;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

/**
 *  Unit tests for the {@link DbDateFieldRule} class
 */
public class Test_DbDateFieldRule extends BaseTest {

    private static DbMetaData      testMetaData;

    private static DbDateFieldRule timestampRule;
    private static Date            today;
    private static Date            tomorrow;

    @BeforeClass
    public static void setUpTest_DbDateFieldRule() {

        testMetaData = new DbMetaData();

        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.MINUTE, 0 );
        calendar.set( Calendar.SECOND, 0 );
        calendar.set( Calendar.MILLISECOND, 0 );
        today = calendar.getTime();

        timestampRule = new DbDateFieldRule( "test",
                                             calendar.getTime(),
                                             MatchRelation.EXACT,
                                             "isMatchTimestamp",
                                             true );
        // go to tomorrow
        calendar.add( Calendar.DAY_OF_YEAR, 1 );
        tomorrow = calendar.getTime();
    }

    @Test
    public void isMatchRelationBeforeDatePositive() throws RbvException {

        testMetaData.putProperty( "test", "2007-10-09 15:00:00" );

        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    "1191941800",
                                                    MatchRelation.BEFORE_DATE,
                                                    "yyyy-MM-dd HH:mm:ss",
                                                    "isMatchRelationBeforeDatePositive",
                                                    true );
        assertTrue( rule.isMatch( testMetaData ) );
    }
    
    @Test
    public void isMatchRelationExactDatePositive() throws RbvException {

        testMetaData.putProperty( "test", "2007-10-09 15:00:00" );

        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    "1191942000",
                                                    MatchRelation.EXACT,
                                                    "yyyy-MM-dd HH:mm:ss",
                                                    "isMatchRelationExactDatePositive",
                                                    true );
        assertTrue( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchRelationBeforeDateNegative() throws RbvException {

        testMetaData.putProperty( "test", "2007-10-09 15:00:00" );

        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    "1191942200",
                                                    MatchRelation.BEFORE_DATE,
                                                    "yyyy-MM-dd HH:mm:ss",
                                                    "isMatchRelationBeforeDateNegative",
                                                    true );
        assertFalse( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchRelationAfterDatePositive() throws RbvException {

        testMetaData.putProperty( "test", "2007-10-09 15:00:00" );

        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    "1191942200",
                                                    MatchRelation.AFTER_DATE,
                                                    "yyyy-MM-dd HH:mm:ss",
                                                    "isMatchRelationAfterDatePositive",
                                                    true );
        assertTrue( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchRelationAfterDateNegative() throws RbvException {

        testMetaData.putProperty( "test", "2007-10-09 15:00:00" );

        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    "1191941800",
                                                    MatchRelation.AFTER_DATE,
                                                    "yyyy-MM-dd HH:mm:ss",
                                                    "isMatchRelationAfterDateNegative",
                                                    true );
        assertFalse( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchNullActualValuePositive() throws RbvException {

        testMetaData.putProperty( "test", null );

        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    null,
                                                    MatchRelation.AFTER_DATE,
                                                    "yyyy-MM-dd HH:mm:ss",
                                                    "isMatchNullActualValue",
                                                    true );
        assertTrue( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchNullActualValueNegative() throws RbvException {

        testMetaData.putProperty( "test", null );

        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    "1191941800",
                                                    MatchRelation.AFTER_DATE,
                                                    "yyyy-MM-dd HH:mm:ss",
                                                    "isMatchNullActualValue",
                                                    true );
        assertFalse( rule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchNullExpectedValueNegative() throws RbvException {

        testMetaData.putProperty( "test", "1191941800" );

        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    null,
                                                    MatchRelation.AFTER_DATE,
                                                    "yyyy-MM-dd HH:mm:ss",
                                                    "isMatchNullActualValue",
                                                    true );
        assertFalse( rule.isMatch( testMetaData ) );
    }

    @Test(expected = RbvException.class)
    public void isMatchWrongActualDateFormat() throws RbvException {

        testMetaData.putProperty( "test", "test123" );

        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    "1191941800",
                                                    MatchRelation.BEFORE_DATE,
                                                    "yyyy-MM-dd HH:mm:ss",
                                                    "isMatchWrongActualDateFormat",
                                                    true );
        rule.isMatch( testMetaData );
    }

    // --- TIMESTAMP tests ---

    @Test
    public void isMatchDatePositive() throws RbvException {

        testMetaData.putProperty( "test", new Timestamp( today.getTime() ) );
        assertTrue( timestampRule.isMatch( testMetaData ) );
    }

    @Test
    public void isMatchDateNegative() throws RbvException {

        testMetaData.putProperty( "test", new Timestamp( tomorrow.getTime() ) );
        assertFalse( timestampRule.isMatch( testMetaData ) );
    }

    @Test(expected = MetaDataIncorrectException.class)
    public void isMatchDateNegativeWrongType() throws RbvException {

        testMetaData.putProperty( "test", tomorrow );
        assertFalse( timestampRule.isMatch( testMetaData ) );
    }
    
    @Test
    public void isMatchDatePositiveBefore() throws RbvException {
        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    tomorrow,
                                                    MatchRelation.BEFORE_DATE,
                                                    "isMatchWrongActualDateFormat",
                                                    true );
        
        testMetaData.putProperty( "test", new Timestamp( today.getTime() ) );
        assertTrue( rule.isMatch( testMetaData ) );
    }
    
    @Test
    public void isMatchDatePositiveAfter() throws RbvException {
        DbDateFieldRule rule = new DbDateFieldRule( "test",
                                                    today,
                                                    MatchRelation.AFTER_DATE,
                                                    "isMatchWrongActualDateFormat",
                                                    true );
        
        testMetaData.putProperty( "test", new Timestamp( tomorrow.getTime() ) );
        assertTrue( rule.isMatch( testMetaData ) );
    }
}
