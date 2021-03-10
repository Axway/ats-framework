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

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

/**
 */
@SuppressWarnings( "boxing")
public abstract class AbstractRule implements Rule, Comparable<AbstractRule> {

    private static final int                LOWEST_PRIORITY = Integer.MAX_VALUE;
    protected final Logger                  log;
    private static final Random             randomIds       = new Random();

    private final boolean                   expectedResult;
    private final String                    ruleName;
    private final int                       ruleUniqueId;
    private final Class<? extends MetaData> metaDataClass;
    private int                             priority;

    /**
     * @param ruleName
     * @param expectedResult
     */
    public AbstractRule( String ruleName,
                         boolean expectedResult ) {

        this(ruleName, expectedResult, MetaData.class, LOWEST_PRIORITY);
    }

    /**
     * Construct a new rule
     * 
     * @param ruleName
     * @param expectedResult
     * @param metaDataClass
     */
    public AbstractRule( String ruleName,
                         boolean expectedResult,
                         Class<? extends MetaData> metaDataClass ) {

        this(ruleName, expectedResult, metaDataClass, LOWEST_PRIORITY);
    }

    /**
     * Construct a new rule
     * 
     * @param ruleName
     * @param expectedResult
     * @param metaDataClass
     */
    public AbstractRule( String ruleName,
                         boolean expectedResult,
                         Class<? extends MetaData> metaDataClass,
                         int priority ) {

        this.log = LogManager.getLogger(this.getClass());
        this.ruleName = ruleName;
        this.ruleUniqueId = randomIds.nextInt();
        this.expectedResult = expectedResult;
        this.metaDataClass = metaDataClass;
        this.priority = priority;
    }

    public final boolean isMatch(
                                  MetaData metaData ) throws RbvException {

        if (metaData == null) {
            throw new RbvException("Meta data passed is null");
        }

        if (!metaDataClass.isInstance(metaData)) {
            throw new MetaDataIncorrectException("Meta data is incorrect - expected instance of "
                                                 + metaDataClass.getName());
        }

        final String ruleDescription = "'" + ruleName + "' " + getRuleDescription();
        log.info("Starting evaluation of rule " + ruleDescription);
        log.info("Processing meta data " + metaData.toString());

        boolean actualResult = performMatch(metaData);
        if (actualResult == expectedResult) {
            log.info("Matched rule " + ruleDescription);
        } else {
            log.info("Did not match rule " + ruleDescription);
        }

        return actualResult == expectedResult;
    }

    /**
     * The actual matching should be performed here
     * 
     * @param metaData
     * @return
     * @throws RbvException
     */
    protected abstract boolean performMatch(
                                             MetaData metaData ) throws RbvException;

    /**
     * Get the description of the rule, used for logging
     * 
     * @return
     */
    protected abstract String getRuleDescription();

    protected boolean getExpectedResult() {

        return this.expectedResult;
    }

    /** @see Rule#getPriority() */
    public int getPriority() {

        return this.priority;
    }

    /** @see Comparable#compareTo(Object) */
    public int compareTo(
                          AbstractRule otherRule ) {

        int result = Integer.valueOf(this.priority).compareTo(otherRule.getPriority());
        // even if the priority of these rules are the same we need to be able to
        // distinguish between different rules (for instance when putting rules with
        // the same priority but different names in a Set we need to be able to put both
        // rules in the Set even if they have the same priority) ...
        if (result == 0) {
            // thanks to the unique rule id users can use more than one rule with same name and priority
            return (this.ruleName + this.ruleUniqueId).compareTo(otherRule.ruleName
                                                                 + otherRule.ruleUniqueId);
        }
        return result;
    }

    public String getRuleName() {

        return ruleName;
    }
}
