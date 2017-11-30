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
package com.axway.ats.agent.core.threading.data;

import java.util.List;

import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderInitalizationException;

public class IntegerRangeParameterDataProvider extends AbstractParameterDataProvider {

    private int rangeStart;
    private int rangeEnd;
    private int currentValue;

    /**
     * Constructor - to be used only by the factory
     *
     * @param parameterName
     * @param staticValue
     * @param rangeStart
     * @param rangeEnd
     */
    IntegerRangeParameterDataProvider( String parameterName,
                                       int rangeStart,
                                       int rangeEnd,
                                       ParameterProviderLevel parameterProviderLevel ) {

        super(parameterName, parameterProviderLevel);

        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.currentValue = rangeStart;
    }

    @Override
    protected void doInitialize() throws ParameterDataProviderInitalizationException {

        this.currentValue = rangeStart;
    }

    @Override
    protected ArgumentValue generateNewValuePerInvocation(
                                                           List<ArgumentValue> alreadyResolvedValues ) {

        if (currentValue > rangeEnd) {
            currentValue = rangeStart;
        }

        return new ArgumentValue(parameterName, currentValue++);
    }

    @Override
    protected ArgumentValue generateNewValuePerThread(
                                                       long currentThreadId,
                                                       List<ArgumentValue> alreadyResolvedValues ) {

        Integer valueIndexPerThread = perThreadIndexes.get(currentThreadId);
        if (valueIndexPerThread != null) {

            valueIndexPerThread++;
        } else {
            valueIndexPerThread = currentValue;
        }
        if (valueIndexPerThread > rangeEnd) {
            valueIndexPerThread = rangeStart;
        }
        perThreadIndexes.put(currentThreadId, valueIndexPerThread);
        return new ArgumentValue(parameterName, valueIndexPerThread);
    }

    @Override
    protected ArgumentValue generateNewValuePerThreadStatic(
                                                             long currentThreadId,
                                                             List<ArgumentValue> alreadyResolvedValues ) {

        Integer valueIndexPerThread = perThreadIndexes.get(currentThreadId);
        if (valueIndexPerThread != null) {
            return new ArgumentValue(parameterName, valueIndexPerThread);
        }
        if (currentValue > rangeEnd) {
            currentValue = rangeStart;
        }
        valueIndexPerThread = currentValue++;
        if (valueIndexPerThread > rangeEnd) {
            valueIndexPerThread = rangeStart;
        }
        perThreadIndexes.put(currentThreadId, valueIndexPerThread);
        return new ArgumentValue(parameterName, valueIndexPerThread);
    }
}
