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

import java.util.List;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.Rule;

/**
 * Each class that extends the {@link Executor} interface would define
 * a method for the proper evaluation of the {@link List} of {@link MetaData}
 * pieces. It could either work on the whole set of {@link MetaData} pieces (such
 * is the {@link SnapshotExecutor} class) or iterating over the separate pieces
 * (such as the {@link MetaExecutor} class).
 */
public interface Executor {

    /**
     * Evaluates the {@link MetaData} received as a parameter against the rules
     * that were previously set.
     *
     * @param metaData the {@link List} of {@link MetaData} to be verified
     * @return the {@link List} of {@link MetaData} pieces that have been matched
     * @throws RbvException in case an error occurs
     */
    public List<MetaData> evaluate(
                                    List<MetaData> metaData ) throws RbvException;

    /**
     * Adds the root {@link Rule} that would match the {@link MetaData}
     * @param rule the root {@link Rule}
     */
    public abstract void setRootRule(
                                      Rule rule );
}
