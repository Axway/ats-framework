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
package com.axway.ats.agent.core.threading.data.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.axway.ats.agent.core.model.EvenLoadDistributingUtils;

/**
 * A class for generating input data based on a given list of values. Consecutive
 * values will be taken from the list
 */
@SuppressWarnings( "serial")
public class ListDataConfig extends AbstractParameterDataConfig {

    private List<?> values;

    /**
     * Constructor - values are generated per thread - each thread will receive
     * the next value from the list
     *
     * @param parameterName the name of the parameter to generate data for
     * @param values the list with values
     */
    public ListDataConfig( String parameterName, List<?> values ) {

        super(parameterName, ParameterProviderLevel.PER_THREAD_STATIC);

        this.values = values;
    }

    /**
     * Constructor - generate data from a list of predefined values
     *
     * @param parameterName the name of the parameter to generate data for
     * @param values the list with values
     * @param parameterProviderLevel the level at which to generate values - it can be
     * per thread or per invocation
     */
    public ListDataConfig( String parameterName, List<?> values,
                           ParameterProviderLevel parameterProviderLevel ) {

        super(parameterName, parameterProviderLevel);

        this.values = values;
    }

    /**
     * Constructor
     *
     * @param parameterName the name of the parameter to generate data for
     * @param values an array of String values
     */
    public ListDataConfig( String parameterName, String[] values ) {

        super(parameterName, ParameterProviderLevel.PER_THREAD_STATIC);

        this.values = Arrays.asList(values);
    }

    /**
     * Constructor
     *
     * @param parameterName the name of the parameter to generate data for
     * @param values an array of String values
     * @param parameterProviderLevel the level at which to generate values - it can be
     * per thread or per invocation
     */
    public ListDataConfig( String parameterName, String[] values,
                           ParameterProviderLevel parameterProviderLevel ) {

        super(parameterName, parameterProviderLevel);

        this.values = Arrays.asList(values);
    }

    /**
     * @return the list with values
     */
    public List<?> getValues() {

        return values;
    }

    @Override
    List<ParameterDataConfig> distribute( int agents ) throws IllegalArgumentException {

        List<ParameterDataConfig> distributedParameterProviders = new ArrayList<ParameterDataConfig>();

        int[] distributionValues = new EvenLoadDistributingUtils().getEvenLoad(this.values.size(), agents);

        if (distributionValues.length == 0) {

            throw new IllegalArgumentException("Could not distribute only " + this.values.size()
                                               + " values of parameter '" + parameterName + "' to " + agents
                                               + " agents! Decrease number of loaders or increase the possible values.");
        } else {

            int lastEndIdx = -1;
            for (int i = 0; i < agents; i++) {
                int startIndex = -1;
                int endIndex = -1;

                if (lastEndIdx == -1) {
                    startIndex = i;
                } else {
                    startIndex = lastEndIdx;
                }

                endIndex = startIndex + distributionValues[i];

                // if we do not create a new instance of ArrayList here,
                // the subList method returns a RandomAccessSublist which is not serializable
                distributedParameterProviders.add(new ListDataConfig(this.parameterName,
                                                                     new ArrayList<Object>(values.subList(startIndex,
                                                                                                          endIndex)),
                                                                     this.parameterProviderLevel));
                lastEndIdx = endIndex; // save the current end index
            }
        }
        return distributedParameterProviders;
    }
}
