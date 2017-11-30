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

import com.axway.ats.rbv.rules.AbstractRule;
import com.axway.ats.rbv.rules.AndRuleOperation;
import com.axway.ats.rbv.rules.Rule;
import com.axway.ats.rbv.rules.RuleOperation;

/**
 * This abstract class makes sure that some basic actions
 * were taken before the actual instance is created
 */
public abstract class BasicExecutor implements Executor {
    protected Rule rootRule = null;

    /**
     * Default constructor
     */
    public BasicExecutor() {

        this.rootRule = new AndRuleOperation();
    }

    public String getLastRuleName() {

        if (rootRule instanceof RuleOperation) {
            return ((RuleOperation) rootRule).getLastRuleName();
        } else {
            return ((AbstractRule) rootRule).getRuleName();
        }
    }
}
