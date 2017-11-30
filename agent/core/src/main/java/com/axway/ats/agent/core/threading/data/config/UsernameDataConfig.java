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
 * A class used to allow setting thread names with provided usernames.The provided usernames are based
 * on a given list of values or on a predefined range.
 */
public class UsernameDataConfig extends AbstractParameterDataConfig {

    private static final long   serialVersionUID = 1L;
    private final static String USERNAME_PARAM   = "username";
    private List<?>             values;
    private String              staticValue;
    private Number              rangeStart;
    private Number              rangeEnd;

    /**
     * Constructor used when providing usernames in the form of Range values
     * 
     * @param staticValue the static value when generating a String argument
     * @param rangeStart the beginning of the range for generating values
     * @param rangeEnd the end of the range for generating values
     */
    public UsernameDataConfig( String staticValue, Integer rangeStart, Integer rangeEnd ) {

        super(USERNAME_PARAM, ParameterProviderLevel.PER_THREAD_STATIC);

        this.staticValue = staticValue;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;

    }

    /**
     * Constructor used when providing a static list of usernames
     * 
     * @param values the list with values
     */
    public UsernameDataConfig( String[] values ) {

        super(USERNAME_PARAM, ParameterProviderLevel.PER_THREAD_STATIC);

        this.values = Arrays.asList(values);
    }

    /**
     * Constructor
     *
     * @param values an array of String values
     */
    private UsernameDataConfig( List<?> values ) {

        super(USERNAME_PARAM, ParameterProviderLevel.PER_THREAD_STATIC);

        this.values = values;
    }

    /**
     * @return the list with values
     */
    public List<?> getValues() {

        return values;
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
    List<ParameterDataConfig> distribute( int agents ) throws IllegalArgumentException {

        if (values != null) {
            return distributeListArguments(agents);
        } else {
            return distributeRangeArguments(agents);
        }
    }

    private List<ParameterDataConfig> distributeListArguments( int agents ) {

        List<ParameterDataConfig> distributedParameterProviders = new ArrayList<ParameterDataConfig>();

        int[] distributionValues = new EvenLoadDistributingUtils().getEvenLoad(this.values.size(), agents);

        if (distributionValues.length == 0) {

            throw new IllegalArgumentException("Could not distribute only " + this.values.size()
                                               + " values of parameter '" + parameterName + "' to " + agents
                                               + " agents! Decrease the number of agents or increase the possible values.");
        } else {

            for (int i = 0; i < agents; i++) {
                int startIndex = i * distributionValues[i];
                // if we do not create a new instance of ArrayList here,
                // the subList method returns a RandomAccessSublist which is not serializable
                distributedParameterProviders.add(new UsernameDataConfig(new ArrayList<Object>(values.subList(startIndex,
                                                                                                              startIndex + distributionValues[i]))));
            }
        }
        return distributedParameterProviders;
    }

    private List<ParameterDataConfig> distributeRangeArguments( int agents ) {

        List<ParameterDataConfig> distributedParameterProviders = new ArrayList<ParameterDataConfig>();

        int[] distributionValues = new EvenLoadDistributingUtils().getEvenLoad(rangeEnd.intValue()
                                                                               - rangeStart.intValue() + 1,
                                                                               agents);
        int numberValuesPerHost = distributionValues[0];
        int numberValuesLastHost = distributionValues[1];

        if (numberValuesPerHost == 0 || numberValuesLastHost == 0) {
            throw new IllegalArgumentException("Could not distribute only "
                                               + (rangeEnd.intValue() - rangeStart.intValue() + 1)
                                               + " values of parameter '" + parameterName + "' to " + agents
                                               + " agents! Decrease the number of agents or increase the possible values.");
        } else {
            for (int i = 0; i < agents; i++) {
                int newRangeStart = rangeStart.intValue() + i * numberValuesPerHost;
                if (i < agents - 1) {
                    distributedParameterProviders.add(new UsernameDataConfig(this.staticValue,
                                                                             newRangeStart,
                                                                             newRangeStart + numberValuesPerHost
                                                                                            - 1));
                } else {
                    distributedParameterProviders.add(new UsernameDataConfig(this.staticValue,
                                                                             newRangeStart,
                                                                             newRangeStart + numberValuesLastHost
                                                                                            - 1));
                }
            }
        }
        return distributedParameterProviders;

    }

    /**
     * In cases when QA specifies user names which will replace the actual thread names,
     * we have to check if the user names are at least as many as the number of threads
     *  
     * @param threadCount
     */
    public void verifyUsernamesAreWEnough( int threadCount ) {

        int paramCount;
        if (getValues() != null) {
            // user names are provided as a static list of values
            paramCount = getValues().size();
        } else {
            // user names are provided as a range of values
            paramCount = rangeEnd.intValue() - rangeStart.intValue() + 1;
        }

        if (paramCount < threadCount) {
            throw new IllegalArgumentException("Username Data Configurator provided only " + paramCount
                                               + " different user names while your thread pattern requires "
                                               + threadCount
                                               + ". You can either provided more user names or do not use a Username Data Configurator");
        }
    }
}
