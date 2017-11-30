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
import java.util.List;

import com.axway.ats.agent.core.model.EvenLoadDistributingUtils;

/**
 * A class for generating input data based on a predefined
 * range. The type of the arguments supported are String and all Integer
 * compatible numeric arguments
 */
@SuppressWarnings( "serial")
public class RangeDataConfig extends AbstractParameterDataConfig {

    private String staticValue;
    private Number rangeStart;
    private Number rangeEnd;

    /**
     * Constructor - generate data for String arguments. New values are generated per thread.
     *
     * @param parameterName the name of the parameter to generate data for
     * @param staticValue the static value when generating a String argument
     * @param rangeStart the beginning of the range for generating values
     * @param rangeEnd the end of the range for generating values
     */
    public RangeDataConfig( String parameterName,
                            String staticValue,
                            Integer rangeStart,
                            Integer rangeEnd ) {

        super(parameterName, ParameterProviderLevel.PER_THREAD_STATIC);

        this.staticValue = staticValue;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    /**
     * Constructor - generate data for String arguments - there is no upper
     * limit of the generated values. New values are generated per thread.
     *
     * @param parameterName the name of the parameter to generate data for
     * @param staticValue the static value when generating a String argument
     * @param rangeStart the beginning of the range for generating values
     */
    public RangeDataConfig( String parameterName,
                            String staticValue,
                            Integer rangeStart ) {

        super(parameterName, ParameterProviderLevel.PER_THREAD_STATIC);

        this.staticValue = staticValue;
        this.rangeStart = rangeStart;
        this.rangeEnd = Integer.MAX_VALUE;
    }

    /**
     * Constructor - generate data for String arguments
     *
     * @param parameterName the name of the parameter to generate data for
     * @param staticValue the static value when generating a String argument
     * @param rangeStart the beginning of the range for generating values
     * @param rangeEnd the end of the range for generating values
     * @param paramProviderLevel the level at which new values for the argument are
     * generated - per thread or per invocation
     */
    public RangeDataConfig( String parameterName,
                            String staticValue,
                            Integer rangeStart,
                            Integer rangeEnd,
                            ParameterProviderLevel paramProviderLevel ) {

        super(parameterName, paramProviderLevel);

        this.staticValue = staticValue;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    /**
     * Constructor - generate data for Integer or compatible arguments. New values are generated per thread.
     *
     * @param parameterName the name of the parameter to generate data for
     * @param rangeStart the beginning of the range for generating values
     * @param rangeEnd the end of the range for generating values
     */
    public RangeDataConfig( String parameterName,
                            Integer rangeStart,
                            Integer rangeEnd ) {

        this(parameterName,
             (Number) rangeStart,
             (Number) rangeEnd,
             ParameterProviderLevel.PER_THREAD_STATIC);
    }

    /**
     * Constructor - generate data for Integer or compatible arguments -
     * there is no upper limit of the generated values. New values are generated per thread.
     *
     * @param parameterName the name of the parameter to generate data for
     * @param rangeStart the beginning of the range for generating values
     */
    public RangeDataConfig( String parameterName,
                            Integer rangeStart ) {

        this(parameterName,
             (Number) rangeStart,
             (Number) Integer.MAX_VALUE,
             ParameterProviderLevel.PER_THREAD_STATIC);
    }

    /**
     * Constructor - generate data for Integer or compatible arguments
     *
     * @param parameterName the name of the parameter to generate data for
     * @param rangeStart the beginning of the range for generating values
     * @param rangeEnd the end of the range for generating values
     * @param paramProviderLevel the level at which new values for the argument are
     */
    public RangeDataConfig( String parameterName,
                            Integer rangeStart,
                            Integer rangeEnd,
                            ParameterProviderLevel paramProviderLevel ) {

        this(parameterName, (Number) rangeStart, (Number) rangeEnd, paramProviderLevel);
    }

    /**
     * Private constructor for internal use
     *
     * @param parameterName the name of the parameter to generate data for
     * @param rangeStart the beginning of the range for generating values
     * @param rangeEnd the end of the range for generating values
     */
    private RangeDataConfig( String parameterName,
                             Number rangeStart,
                             Number rangeEnd,
                             ParameterProviderLevel paramProviderLevel ) {

        super(parameterName, paramProviderLevel);

        this.staticValue = null;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    /**
     * Get the static value when replacing String arguments
     *
     * @return the static value, null if not set (in this case Integer argument is assumed)
     */
    public String getStaticValue() {

        return staticValue;
    }

    /**
     * Get the beginning of the range for generating values
     *
     * @return the beginning of the range for generating values
     */
    public Number getRangeStart() {

        return rangeStart;
    }

    /**
     * Get the end of the range for generating values
     *
     * @return the end of the range for generating values
     */
    public Number getRangeEnd() {

        return rangeEnd;
    }

    @Override
    List<ParameterDataConfig> distribute(
                                          int agents ) {

        List<ParameterDataConfig> distributedParameterProviders = new ArrayList<ParameterDataConfig>();

        int[] distributionValues = new EvenLoadDistributingUtils().getEvenLoad(rangeEnd.intValue()
                                                                               - rangeStart.intValue() + 1,
                                                                               agents);
        if (distributionValues.length == 0) {

            throw new IllegalArgumentException("Could not distribute only "
                                               + (rangeEnd.intValue() - rangeStart.intValue() + 1)
                                               + " values of parameter '"
                                               + parameterName
                                               + "' to "
                                               + agents
                                               + " agents! Decrease number ot loaders or increase the possible values.");
        } else {
            for (int i = 0; i < agents; i++) {
                int newRangeStart = rangeStart.intValue() + i * distributionValues[i];
                distributedParameterProviders.add(new RangeDataConfig(this.parameterName,
                                                                      this.staticValue,
                                                                      newRangeStart,
                                                                      newRangeStart
                                                                                     + distributionValues[i] - 1,
                                                                      this.parameterProviderLevel));
            }
        }

        return distributedParameterProviders;
    }
}
