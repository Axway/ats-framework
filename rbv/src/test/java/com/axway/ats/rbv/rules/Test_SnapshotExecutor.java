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
package com.axway.ats.rbv.rules;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.db.DbMetaData;
import com.axway.ats.rbv.db.rules.DbStringFieldRule;
import com.axway.ats.rbv.executors.SnapshotExecutor;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.AndRuleOperation;
import com.axway.ats.rbv.rules.Rule;

/**
 * Unit tests for the {@link SnapshotExecutor} object
 * 
 * TODO : add other types of verifications (email, file and etc.)
 */
public class Test_SnapshotExecutor extends BaseTest {

    private List<MetaData>      snapshot;
    private List<MetaData>      newData;

    // --- TEST DATA ---

    // source data    
    private static final String TABLE_NAME       = "table";
    private static final String COLUMN_NAME_1    = "first_column";
    private static final String COLUMN_NAME_2    = "second_column";

    private static final String META_KEY_1       = TABLE_NAME + "." + COLUMN_NAME_1;
    private static final String META_KEY_2       = TABLE_NAME + "." + COLUMN_NAME_2;
    private static final String META_KEY_3       = "";
    private static final String META_KEY_4       = null;
    private static final String META_VALUE_1     = "typical value";
    private static final String META_VALUE_2     = "another value";
    private static final String META_VALUE_3     = "";
    private static final String META_VALUE_4     = null;

    // modified data
    private static final String MOD_META_VALUE_1 = "typicalvalue";
    private static final String MOD_META_VALUE_2 = "anothervalue";
    private static final String MOD_META_VALUE_3 = "there-is-text-now-here";

    /**
     * Build up {@link MetaData} to use in the test
     * @throws IOException
     * @throws RbvException
     */
    @Before
    public void setUp() throws IOException, RbvException {

        this.snapshot = new ArrayList<MetaData>();
        this.newData = new ArrayList<MetaData>();

        DbMetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 1 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 1 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 1 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 1 );
        this.snapshot.add( meta );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.snapshot.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 3 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 3 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 3 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 3 );
        this.snapshot.add( meta );
        this.newData.add( meta );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchAfterUpdatePositive() throws RbvException {

        DbMetaData newMeta = new DbMetaData();
        newMeta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 2 );
        newMeta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        newMeta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        newMeta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( newMeta );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           META_VALUE_2 + 2,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule_1",
                                                           true );

        DbStringFieldRule matchingRule = new DbStringFieldRule( TABLE_NAME,
                                                                COLUMN_NAME_1,
                                                                MOD_META_VALUE_1 + 2,
                                                                DbStringFieldRule.MatchRelation.EQUALS,
                                                                "match_metakey_1",
                                                                true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, matchingRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result != null && !result.isEmpty() );
    }

    /**
    * UnitTest
    * @throws RbvException
    */
    @Test
    public void matchAfterNoUpdateHasBeenMade() throws RbvException {

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           META_VALUE_2 + 2,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule_1",
                                                           true );

        DbStringFieldRule matchingRule = new DbStringFieldRule( TABLE_NAME,
                                                                COLUMN_NAME_1,
                                                                META_VALUE_1 + 33,
                                                                DbStringFieldRule.MatchRelation.EQUALS,
                                                                "match_metakey_1",
                                                                true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, matchingRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result == null );
    }

    /**
    * UnitTest
    * @throws RbvException
    */
    @Test
    public void matchAfterUpdateNegative() throws RbvException {

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, MOD_META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           MOD_META_VALUE_2 + 2,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule_1",
                                                           true );

        DbStringFieldRule matchingRule = new DbStringFieldRule( TABLE_NAME,
                                                                COLUMN_NAME_1,
                                                                MOD_META_VALUE_1 + 2,
                                                                DbStringFieldRule.MatchRelation.EQUALS,
                                                                "match_metakey_1",
                                                                true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, matchingRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result == null || result.isEmpty() );
    }

    /**
    * UnitTest
    * @throws RbvException
    */
    @Test
    public void matchAfterMultipleUpdatePositive() throws RbvException {

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, MOD_META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           MOD_META_VALUE_2 + 2,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule_1",
                                                           true );

        DbStringFieldRule matchingRule1 = new DbStringFieldRule( TABLE_NAME,
                                                                 COLUMN_NAME_1,
                                                                 MOD_META_VALUE_1 + 2,
                                                                 DbStringFieldRule.MatchRelation.EQUALS,
                                                                 "match_metakey_1",
                                                                 true );
        DbStringFieldRule matchingRule2 = new DbStringFieldRule( TABLE_NAME,
                                                                 COLUMN_NAME_2,
                                                                 MOD_META_VALUE_2 + 2,
                                                                 DbStringFieldRule.MatchRelation.EQUALS,
                                                                 "match_metakey_2",
                                                                 true );
        AndRuleOperation rule = new AndRuleOperation();

        rule.addRule( matchingRule1 );
        rule.addRule( matchingRule2 );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, rule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result != null && !result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchAfterMultipleUpdateNegative() throws RbvException {

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, MOD_META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, MOD_META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           MOD_META_VALUE_2 + 2,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule_1",
                                                           true );

        DbStringFieldRule matchingRule1 = new DbStringFieldRule( TABLE_NAME,
                                                                 COLUMN_NAME_1,
                                                                 MOD_META_VALUE_1 + 2,
                                                                 DbStringFieldRule.MatchRelation.EQUALS,
                                                                 "match_metakey_1",
                                                                 true );
        DbStringFieldRule matchingRule2 = new DbStringFieldRule( TABLE_NAME,
                                                                 COLUMN_NAME_2,
                                                                 MOD_META_VALUE_2 + 2,
                                                                 DbStringFieldRule.MatchRelation.EQUALS,
                                                                 "match_metakey_2",
                                                                 true );
        AndRuleOperation rule = new AndRuleOperation();

        rule.addRule( matchingRule1 );
        rule.addRule( matchingRule2 );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, rule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result == null || result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchAfterDifferentUpdatePositive() throws RbvException {

        // remove first piece of default MetaData
        this.newData = new ArrayList<MetaData>();

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 1 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 1 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 1 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 1 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, MOD_META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 3 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 3 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 3 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 3 );
        this.newData.add( meta );

        DbStringFieldRule keyRule1 = new DbStringFieldRule( TABLE_NAME,
                                                            COLUMN_NAME_2,
                                                            META_VALUE_2 + 1,
                                                            DbStringFieldRule.MatchRelation.EQUALS,
                                                            "key_rule_1",
                                                            true );

        DbStringFieldRule matchingRule1 = new DbStringFieldRule( TABLE_NAME,
                                                                 COLUMN_NAME_1,
                                                                 MOD_META_VALUE_1 + 1,
                                                                 DbStringFieldRule.MatchRelation.EQUALS,
                                                                 "match_metakey_1",
                                                                 true );

        DbStringFieldRule keyRule2 = new DbStringFieldRule( TABLE_NAME,
                                                            COLUMN_NAME_1,
                                                            META_VALUE_1 + 2,
                                                            DbStringFieldRule.MatchRelation.EQUALS,
                                                            "key_rule_2",
                                                            true );

        DbStringFieldRule matchingRule2 = new DbStringFieldRule( TABLE_NAME,
                                                                 COLUMN_NAME_2,
                                                                 MOD_META_VALUE_2 + 2,
                                                                 DbStringFieldRule.MatchRelation.EQUALS,
                                                                 "match_metakey_2",
                                                                 true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule1, matchingRule1 );
        executor.addRule( keyRule2, matchingRule2 );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result != null && !result.isEmpty() );
    }

    /**
     * @throws RbvException
     */
    @Test
    public void matchAfterDifferentUpdateNegative() throws RbvException {

        // remove first piece of default MetaData
        this.newData = new ArrayList<MetaData>();

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 1 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 1 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 1 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 1 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, MOD_META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 3 );
        meta.putProperty( META_KEY_2, MOD_META_VALUE_2 + 3 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 3 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 3 );
        this.newData.add( meta );

        DbStringFieldRule keyRule1 = new DbStringFieldRule( TABLE_NAME,
                                                            COLUMN_NAME_2,
                                                            META_VALUE_2 + 1,
                                                            DbStringFieldRule.MatchRelation.EQUALS,
                                                            "key_rule_1",
                                                            true );

        DbStringFieldRule matchingRule1 = new DbStringFieldRule( TABLE_NAME,
                                                                 COLUMN_NAME_1,
                                                                 MOD_META_VALUE_1 + 1,
                                                                 DbStringFieldRule.MatchRelation.EQUALS,
                                                                 "match_metakey_1",
                                                                 true );

        DbStringFieldRule keyRule2 = new DbStringFieldRule( TABLE_NAME,
                                                            COLUMN_NAME_1,
                                                            META_VALUE_1 + 2,
                                                            DbStringFieldRule.MatchRelation.EQUALS,
                                                            "key_rule_2",
                                                            true );

        DbStringFieldRule matchingRule2 = new DbStringFieldRule( TABLE_NAME,
                                                                 COLUMN_NAME_2,
                                                                 MOD_META_VALUE_2 + 2,
                                                                 DbStringFieldRule.MatchRelation.EQUALS,
                                                                 "match_metakey_2",
                                                                 true );
        List<Rule> rules = new ArrayList<Rule>();
        rules.add( matchingRule1 );
        rules.add( matchingRule2 );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule1, matchingRule1 );
        executor.addRule( keyRule2, matchingRule2 );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result == null || result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchExcludeKeysScenarioPositive() throws RbvException {

        // remove first piece of default MetaData
        this.newData = new ArrayList<MetaData>();

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 1 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 1 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 1 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 1 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, MOD_META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 3 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 3 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 3 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 3 );
        this.newData.add( meta );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           META_VALUE_2 + 1,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule",
                                                           true );

        DbStringFieldRule matchingRule = new DbStringFieldRule( TABLE_NAME,
                                                                COLUMN_NAME_1,
                                                                MOD_META_VALUE_1 + 1,
                                                                DbStringFieldRule.MatchRelation.EQUALS,
                                                                "match_metakey_1",
                                                                true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, matchingRule );

        List<String> list = new ArrayList<String>();
        list.add( META_KEY_2 );
        executor.excludeKeys( list );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result != null && !result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchDifferentSizeOfList() throws RbvException {

        this.newData = new ArrayList<MetaData>();

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           META_VALUE_2 + 2,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule",
                                                           true );

        DbStringFieldRule matchingRule = new DbStringFieldRule( TABLE_NAME,
                                                                COLUMN_NAME_1,
                                                                MOD_META_VALUE_1 + 2,
                                                                DbStringFieldRule.MatchRelation.EQUALS,
                                                                "match_metakey_1",
                                                                true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, matchingRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result == null || result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchDifferentSizeOfPropertiesName() throws RbvException {

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( "Some_other_disturbingly_wrong_name", META_VALUE_4 + 2 );
        this.newData.add( meta );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           META_VALUE_2 + 2,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule",
                                                           true );

        DbStringFieldRule matchingRule = new DbStringFieldRule( TABLE_NAME,
                                                                COLUMN_NAME_1,
                                                                MOD_META_VALUE_1 + 2,
                                                                DbStringFieldRule.MatchRelation.EQUALS,
                                                                "match_metakey_1",
                                                                true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, matchingRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result == null || result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchDifferentSizeOfPropertiesCount() throws RbvException {

        MetaData meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 2 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        meta.putProperty( TABLE_NAME + ".columnfive", META_VALUE_4 + 2 );
        this.newData.add( meta );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           META_VALUE_2 + 2,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule",
                                                           true );

        DbStringFieldRule matchingRule = new DbStringFieldRule( TABLE_NAME,
                                                                COLUMN_NAME_1,
                                                                MOD_META_VALUE_1 + 2,
                                                                DbStringFieldRule.MatchRelation.EQUALS,
                                                                "match_metakey_1",
                                                                true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, matchingRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result == null || result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchGlobalRulePositive() throws RbvException {

        this.newData = new ArrayList<MetaData>();

        DbMetaData meta;

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 1 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 1 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 1 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 3 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 3 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 3 );
        this.newData.add( meta );

        DbStringFieldRule globalRule = new DbStringFieldRule( TABLE_NAME,
                                                              COLUMN_NAME_1,
                                                              MOD_META_VALUE_1 + 33,
                                                              DbStringFieldRule.MatchRelation.EQUALS,
                                                              "global_rule",
                                                              true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.setRootRule( globalRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result != null && !result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchGlobalRuleNegativeOneOfTheRecordsIsNotChanged() throws RbvException {

        this.newData = new ArrayList<MetaData>();

        DbMetaData meta;

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 1 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 1 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 1 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, META_VALUE_1 + 3 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 3 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 3 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 3 );
        this.newData.add( meta );

        DbStringFieldRule globalRule = new DbStringFieldRule( TABLE_NAME,
                                                              COLUMN_NAME_1,
                                                              MOD_META_VALUE_1 + 33,
                                                              DbStringFieldRule.MatchRelation.EQUALS,
                                                              "global_rule",
                                                              true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.setRootRule( globalRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result == null || result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchGlobalRuleAndSnapshotRulesPositive() throws RbvException {

        this.newData = new ArrayList<MetaData>();

        DbMetaData meta;

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 35 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 1 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 1 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 1 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 3 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 3 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 3 );
        this.newData.add( meta );

        DbStringFieldRule globalRule = new DbStringFieldRule( TABLE_NAME,
                                                              COLUMN_NAME_1,
                                                              MOD_META_VALUE_1 + 33,
                                                              DbStringFieldRule.MatchRelation.EQUALS,
                                                              "global_rule",
                                                              true );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           META_VALUE_2 + 1,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule",
                                                           true );

        DbStringFieldRule matchingRule = new DbStringFieldRule( TABLE_NAME,
                                                                COLUMN_NAME_1,
                                                                MOD_META_VALUE_1 + 35,
                                                                DbStringFieldRule.MatchRelation.EQUALS,
                                                                "match_metakey_1",
                                                                true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, matchingRule );
        executor.setRootRule( globalRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result != null && !result.isEmpty() );
    }

    /**
     * UnitTest
     * @throws RbvException
     */
    @Test
    public void matchGlobalRuleAndSnapshotRulesNegative() throws RbvException {

        this.newData = new ArrayList<MetaData>();

        DbMetaData meta;

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 1 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 1 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 1 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 2 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 2 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 2 );
        this.newData.add( meta );

        meta = new DbMetaData();
        meta.putProperty( META_KEY_1, MOD_META_VALUE_1 + 33 );
        meta.putProperty( META_KEY_2, META_VALUE_2 + 3 );
        meta.putProperty( META_KEY_3, META_VALUE_3 + 3 );
        meta.putProperty( META_KEY_4, META_VALUE_4 + 3 );
        this.newData.add( meta );

        DbStringFieldRule globalRule = new DbStringFieldRule( TABLE_NAME,
                                                              COLUMN_NAME_1,
                                                              MOD_META_VALUE_1 + 33,
                                                              DbStringFieldRule.MatchRelation.EQUALS,
                                                              "global_rule",
                                                              true );

        DbStringFieldRule keyRule = new DbStringFieldRule( TABLE_NAME,
                                                           COLUMN_NAME_2,
                                                           META_VALUE_2 + 1,
                                                           DbStringFieldRule.MatchRelation.EQUALS,
                                                           "key_rule",
                                                           true );

        DbStringFieldRule matchingRule = new DbStringFieldRule( TABLE_NAME,
                                                                COLUMN_NAME_1,
                                                                MOD_META_VALUE_1 + 35,
                                                                DbStringFieldRule.MatchRelation.EQUALS,
                                                                "match_metakey_1",
                                                                true );

        SnapshotExecutor executor = new SnapshotExecutor( this.snapshot );
        executor.addRule( keyRule, matchingRule );
        executor.setRootRule( globalRule );

        List<MetaData> result = executor.evaluate( this.newData );

        assertTrue( result == null || result.isEmpty() );
    }
}
