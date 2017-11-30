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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;

/**
 * Base class for all rule operations
 */
@SuppressWarnings( "boxing")
public abstract class RuleOperation implements Rule {

    protected Set<Rule> rules;
    protected int       priority = Integer.MAX_VALUE;

    protected String    lastRuleName;

    /**
     * Constructor
     */
    public RuleOperation() {

        this.rules = new TreeSet<Rule>();
    }

    /**
     * Add a rule to the expression
     * 
     * @param rule  rule or rule operation
     */
    public void addRule(
                         Rule rule ) {

        this.rules.add(rule);

        // if the priority of this rule is higher then we need
        // to update this
        if (rule.getPriority() < this.priority) {
            this.priority = rule.getPriority();
        }
    }

    /**
     * Add a list of rules to the expression
     * 
     * @param newRules  rules or rule operations
     */
    public void addRules(
                          List<Rule> newRules ) {

        for (Rule rule : newRules) {
            this.rules.add(rule);

            // if the priority of this rule is higher then we need
            // to update this
            if (rule.getPriority() < this.priority) {
                this.priority = rule.getPriority();
            }
        }
    }

    /**
     * @return the internal list of rules
     */
    public List<Rule> getRules() {

        List<Rule> rulesList = new ArrayList<Rule>();
        rulesList.addAll(this.rules);
        return rulesList;
    }

    /** @see Rule#getPriority() */
    public int getPriority() {

        return this.priority;
    }

    /** @see Comparable#compareTo(Object) */
    public int compareTo(
                          Rule otherRule ) {

        return Integer.valueOf(this.priority).compareTo(otherRule.getPriority());
    }

    public abstract boolean isMatch(
                                     MetaData metaData ) throws RbvException;

    /**
     * @return returns a {@link List} of {@link String} values, representing the
     *         keys of each single unit of data that the rule, or any other nested
     *         in it rules, verifies
     */
    public List<String> getMetaDataKeys() {

        List<String> keys = new ArrayList<String>();
        for (Rule rule : this.rules) {
            keys.addAll(rule.getMetaDataKeys());
        }

        return keys;
    }

    /**
     * Clear all rules set
     */
    public void clearRules() {

        this.rules.clear();
    }

    /**
     * Get the name of the filed rule
     */
    public String getLastRuleName() {

        return this.lastRuleName;
    }
}
