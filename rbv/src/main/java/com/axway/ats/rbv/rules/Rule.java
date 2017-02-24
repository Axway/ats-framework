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

import java.util.List;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;

/**
 * Interface which should be implemented by all rules and
 * rule operations
 */
public interface Rule {

    /**
     * Evaluate the rule using the provided meta data
     * 
     * @param metaData          the meta data on which to execute the rule
     * @return                  true in case the rule condition is satisified, false otherwise
     * @throws RbvException     if there was some error during rule evaluation
     */
    public boolean isMatch(
                            MetaData metaData ) throws RbvException;

    /**
     * @return returns a {@link List} of {@link String} values, representing the
     *         keys of each single unit of data that the rule, or any other nested
     *         in it rules, verifies
     */
    public abstract List<String> getMetaDataKeys();

    /**
     * Returns the priority of this {@link Rule}
     * @return the priority of this {@link Rule}
     */
    public int getPriority();
}
