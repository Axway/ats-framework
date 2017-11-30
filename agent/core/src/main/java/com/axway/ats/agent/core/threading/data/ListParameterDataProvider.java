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

/**
 * This parameter provider implementation will pick the next value from a list
 */
public class ListParameterDataProvider extends AbstractParameterDataProvider {

    private final List<?> values;
    private final int     valuesSize;
    private int           currentIndex;

    ListParameterDataProvider( String parameterName,
                               List<?> values,
                               ParameterProviderLevel parameterProviderLevel ) {

        super(parameterName, parameterProviderLevel);

        this.values = values;
        this.valuesSize = values.size();
        this.currentIndex = 0;
    }

    ListParameterDataProvider( String parameterName,
                               List<?> values,
                               ParameterProviderLevel parameterProviderLevel,
                               Class<?> dataProviderInstance ) {

        super(parameterName, parameterProviderLevel);

        this.values = values;
        this.valuesSize = values.size();
        this.currentIndex = 0;
        this.dataProviderInstance = dataProviderInstance;
    }

    @Override
    protected void doInitialize() throws ParameterDataProviderInitalizationException {

        currentIndex = 0;
    }

    @Override
    protected ArgumentValue generateNewValuePerInvocation(
                                                           List<ArgumentValue> alreadyResolvedValues ) {

        if (currentIndex >= valuesSize) {
            currentIndex = 0;
        }

        return new ArgumentValue(parameterName, values.get(currentIndex++));
    }

    @Override
    protected ArgumentValue generateNewValuePerThread(
                                                       long currentThreadId,
                                                       List<ArgumentValue> alreadyResolvedValues ) {

        Integer valueIndexPerThread = perThreadIndexes.get(currentThreadId);
        if (valueIndexPerThread != null) { // isStaticValue = false

            valueIndexPerThread++;
        } else {
            valueIndexPerThread = currentIndex;
        }
        if (valueIndexPerThread >= valuesSize) {
            valueIndexPerThread = 0;
        }
        perThreadIndexes.put(currentThreadId, valueIndexPerThread);
        return new ArgumentValue(parameterName, values.get(valueIndexPerThread));
    }

    @Override
    protected ArgumentValue generateNewValuePerThreadStatic(
                                                             long currentThreadId,
                                                             List<ArgumentValue> alreadyResolvedValues ) {

        Integer valueIndexPerThread = perThreadIndexes.get(currentThreadId);
        if (valueIndexPerThread != null) {
            return new ArgumentValue(parameterName, values.get(valueIndexPerThread));
        }
        if (currentIndex >= valuesSize) {
            currentIndex = 0;
        }
        valueIndexPerThread = currentIndex++;
        if (valueIndexPerThread >= valuesSize) {
            valueIndexPerThread = 0;
        }
        perThreadIndexes.put(currentThreadId, valueIndexPerThread);
        return new ArgumentValue(parameterName, values.get(valueIndexPerThread));
    }

}
