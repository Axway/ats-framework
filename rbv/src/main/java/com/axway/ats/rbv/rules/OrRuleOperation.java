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

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;

/**
 * This class represents a logical OR expression 
 * between several rules
 */
public class OrRuleOperation extends RuleOperation {

    @Override
    public boolean isMatch(
                            MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        //get all matchers result
        for (Rule rule : rules) {
            if (rule instanceof AbstractRule) {
                lastRuleName = ((AbstractRule) rule).getRuleName();
            }
            if (rule.isMatch(metaData)) {
                actualResult = true;
                break;
            }
        }

        return actualResult;
    }

}
