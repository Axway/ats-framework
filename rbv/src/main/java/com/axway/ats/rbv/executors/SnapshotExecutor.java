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

package com.axway.ats.rbv.executors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.AbstractRule;
import com.axway.ats.rbv.rules.Rule;

/**
 * This implementation of the {@link Executor} interface not only verifies
 * that the {@link MetaData}, that is given to it, matches some entries in
 * the {@link IMatchable}, but also takes a snapshot before any action is
 * taken. This way it insures that the action didn't affect data, that it
 * is not supposed to. Data that is modified is identified by the {@link Rule}s,
 * nested in the class.<br>
 * <br>
 * The {@link MetaData} is processed in two steps :<br>
 * <ul>
 * <li>Check if the {@link MetaData} matches any of the {@link Rule}s</li>
 * <li>If YES then check each property of the {@link MetaData} against the
 * previously taken snapshot *EXCEPT* the ones that are excluded by the rules(
 * ie. supposed to be changed) or manually excluded by the user</li>
 * <li>If NO then perform an exact match - check if the {@link MetaData}'s
 * properties match the properties of some {@link MetaData} entry in the snapshot</li>
 * </ul>
 *
 */
public class SnapshotExecutor extends BasicExecutor {

    protected List<SnapshotMatchingRule> rules;
    protected Rule                       globalRule;

    private List<MetaData>               snapshot;
    // excluded keys that are specified by the user and should not be considered during the matching process
    private List<String>                 explicitlyExcluded;

    private static final Logger          log                         = LogManager.getLogger(SnapshotExecutor.class);

    // log messages
    private static final String          SNAPSHOT_EVALUATION_START   = "Starting metadata snapshot evaluation ...";
    private static final String          UNSUCCESSFUL_SNAPSHOT_MATCH = "Match not successful! Unable to verify this piece of metadata against the snapshot contents!";
    private static final String          SNAPSHOT_NOT_FULLY_MATCHED  = "Match not successful! The new data does not contain all the entries of the snapshot!";

    /**
     * Constructor
     * @param meta the {@link List} of {@link MetaData} used to make a snapshot
     */
    public SnapshotExecutor( List<MetaData> meta ) {

        this.snapshot = meta;
        this.rules = new ArrayList<SnapshotMatchingRule>();
        this.explicitlyExcluded = new ArrayList<String>();
    }

    /**
     * Evaluates the {@link MetaData} received as a parameter against the rules
     * that were previously set.
     *
     * @param metaData the {@link List} of {@link MetaData} to be verified
     * @return the {@link List} of {@link MetaData} pieces that have been matched
     * @throws RbvException upon an error
     * @see com.axway.ats.rbv.executors.Executor#evaluate(java.util.List)
     */
    public List<MetaData> evaluate(
                                    List<MetaData> metaData ) throws RbvException {

        log.debug(SNAPSHOT_EVALUATION_START);

        // try to match each piece of MetaData to the snapshot
        for (MetaData currentMeta : metaData) {
            if (!this.match(currentMeta)) {
                // if any of the pieces does not match - return no matched data
                return null;
            }
        }

        if (!this.snapshot.isEmpty()) {
            // if some pieces of MetaData from the snapshot weren't found in the
            // new MetaData then the result would still be negative
            log.debug(SNAPSHOT_NOT_FULLY_MATCHED);
            return null;
        }

        return metaData;
    }

    /**
     * Searching rule
     *
     * @param keyRule   this rule is used for identifying the meta data(s)
     *                  which need to be matched with the matching rule
     * @param matchingRule  the matching rule - describes the expected changes to the meta data
     */
    public void addRule(
                         Rule keyRule,
                         Rule matchingRule ) throws RbvException {

        if (keyRule == null || matchingRule == null) {
            throw new RbvException("Neither the key, nor the matching rule can be null");
        }

        //add a snapshot rule
        rules.add(new SnapshotMatchingRule(keyRule, matchingRule, "snapshot_rule" + rules.size(), true));
    }

    /**
     * Excludes the given keys explicitly from the snapshot matching
     * process. Such an action is needed if such {@link MetaData} properties
     * should not be considered in the matching process - such fields as
     * field containing last update time, timestamps and etc.
     *
     * @param metaDataKeys the {@link List} of keys
     */
    public void excludeKeys(
                             List<String> metaDataKeys ) {

        // add to list of excluded keys
        this.explicitlyExcluded.addAll(metaDataKeys);
    }

    // matches the current piece of MetaData to the predefined set of rules
    // and the snapshot taken at the beginning
    private boolean match(
                           MetaData metaData ) throws RbvException {

        //iterate through all matching rules from the hashmap
        for (SnapshotMatchingRule snapShotRule : rules) {

            if (snapShotRule.getKeyRule().isMatch(metaData)) {

                //if the key rule and the snapshot rule match the meta data, we don't need more checks
                if (snapShotRule.isMatch(metaData)) {
                    return true;
                } else {
                    //if the key rule matched, but the matching rule did not match,
                    //then the expected change did not occur, so return false
                    return false;
                }
            }
        }

        //this means that we should be matched by the global rule (if we have it)
        if (globalRule != null) {
            if (globalRule.isMatch(metaData) && snapshotMatch(metaData, globalRule.getMetaDataKeys())) {
                return true;
            }
        } else {
            if (snapshotMatch(metaData, new ArrayList<String>())) {
                return true;
            }
        }

        // check that both new MetaData is matched and no records were lost
        return false;
    }

    // locate this piece of MetaData in the snapshot
    private boolean snapshotMatch(
                                   MetaData newMeta,
                                   List<String> excludedKeys ) throws RbvException {

        // iterate over all of the entries of the snapshot
        for (Iterator<MetaData> iterator = this.snapshot.iterator(); iterator.hasNext();) {
            MetaData metaData = iterator.next();
            boolean isMatch = true;

            if (!checkPropertiesMapping(metaData, newMeta)) {
                // if both pieces of MetaData DO NOT have the same set of
                // properties then they should not be matched
                continue;
            }

            // iterate over all the properties of this MetaData entry and
            // compare their values to the ones supplied by the new MetaData
            for (String value : metaData.getKeys()) {
                // check if this key should be evaluated at all
                if (shouldEvaluateKey(value, excludedKeys)) {
                    if (!compare(metaData.getProperty(value), newMeta.getProperty(value))) {
                        isMatch = false;
                    }
                }
            }

            // if the current snapshot entry matches the new MetaData
            if (isMatch) {
                iterator.remove();
                log.debug("Matched meta data: " + metaData);
                return true;
            }
        }

        log.debug(UNSUCCESSFUL_SNAPSHOT_MATCH);
        log.debug("--- Debug dump --- ");
        log.debug("Metadata contents : " + newMeta);

        return false;
    }

    // checks if both instances contain the same set of properties
    private boolean checkPropertiesMapping(
                                            MetaData metaData,
                                            MetaData newMeta ) {

        // check if the properties count is the same
        if (metaData.getKeys().size() != newMeta.getKeys().size()) {
            return false;
        }

        // check if each of the properties has the same name
        for (String value : metaData.getKeys()) {
            if (!newMeta.getKeys().contains(value)) {
                return false;
            }
        }

        return true;
    }

    private boolean shouldEvaluateKey(
                                       String value,
                                       List<String> excludedKeys ) {

        // if this key is excluded explicitly then it should not be
        // evaluated at all
        if (this.explicitlyExcluded.contains(value)) {
            return false;
        }

        // otherwise see if the key is not excluded by any of the nested rules
        if (excludedKeys.contains(value)) {
            return false;
        }

        return true;
    }

    // compare two Object instances, given that any of them might be null
    // and still using their equals() method
    private boolean compare(
                             Object data1,
                             Object data2 ) {

        if (data1 == null) {
            if (data2 == null) {
                return true;
            }
            return false;
        }

        if (data2 == null) {
            return false;
        }

        return data1.equals(data2);
    }

    /**
     * @see com.axway.ats.rbv.executors.BasicExecutor#setRootRole(com.axway.ats.rbv.rules.Rule)
     */
    public void setRootRule(
                             Rule rootRule ) {

        this.globalRule = rootRule;
    }

    /**
     * An inner class for matching a key rule and a "real" rule against a meta data
     */
    private class SnapshotMatchingRule extends AbstractRule {

        private Rule         keyRule;
        private Rule         matchingRule;
        private List<String> excludedKeys;

        public SnapshotMatchingRule( Rule keyRule,
                                     Rule matchingRule,
                                     String ruleName,
                                     boolean expectedResult ) {

            super(ruleName, expectedResult);

            this.keyRule = keyRule;
            this.matchingRule = matchingRule;
            this.excludedKeys = matchingRule.getMetaDataKeys();
        }

        @Override
        protected String getRuleDescription() {

            return "with key rule";
        }

        @Override
        protected boolean performMatch(
                                        MetaData metaData ) throws RbvException {

            //check if the matchingRule will match
            if (!matchingRule.isMatch(metaData)) {
                return false;
            }

            //now compare with the snapshot
            if (!snapshotMatch(metaData, excludedKeys)) {
                return false;
            }

            return true;
        }

        public List<String> getMetaDataKeys() {

            return matchingRule.getMetaDataKeys();
        }

        public Rule getKeyRule() {

            return keyRule;
        }

    }
}
