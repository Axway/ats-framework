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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.Rule;

/**
 * The {@link MetaExecutor} is responsible for executing the verification
 * over each piece of {@link MetaData} that was received from the {@link IMatchable}.
 * It contains the root {@link Rule} which would carry all the logic needed to
 * evaluate the {@link MetaData} pieces.
 */
public class MetaExecutor extends BasicExecutor {

    private boolean             endOnFirstMatch = true;
    private static final Logger log             = LogManager.getLogger(MetaExecutor.class);

    /**
     * Evaluates the {@link MetaData} received as a parameter against the rules
     * that were previously set.
     * @param metaData the {@link List} of {@link MetaData} to be verified
     *
     * @see com.axway.ats.rbv.executors.Executor#evaluate(java.util.List)
     */
    public List<MetaData> evaluate(
                                    List<MetaData> metaData ) throws RbvException {

        List<MetaData> matched = new ArrayList<MetaData>();

        for (MetaData currentMeta : metaData) {
            if (this.rootRule.isMatch(currentMeta)) {

                log.info("Matched a meta data!");

                // we matched a piece of MetaData - add it to the
                // collection that would be returned as a result
                matched.add(currentMeta);

                if (this.endOnFirstMatch) {
                    return matched;
                }
            }
        }

        return matched;
    }

    /**
     * @see com.axway.ats.rbv.executors.Executor#setRootRule(com.axway.ats.rbv.rules.Rule)
     */
    public void setRootRule(
                             Rule rule ) {

        this.rootRule = rule;
    }

}
