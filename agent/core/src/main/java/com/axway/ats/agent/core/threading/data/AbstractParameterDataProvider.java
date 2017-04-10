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

import java.util.HashMap;
import java.util.List;

import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderInitalizationException;

/**
 * Abstract class to be extended by all implementations of ParameterDataProvider.
 * This class is responsible for handling when the generation of new values happens
 */
public abstract class AbstractParameterDataProvider implements ParameterDataProvider {

    protected String                 parameterName;
    protected Class<?>               dataProviderInstance;
    protected ParameterProviderLevel parameterProviderLevel;
    
    // can be used when need to keep indexes in order to distribute next parameter values 
    protected HashMap<Long, Integer> perThreadIndexes;
    // can be used when need to keep some special object in order to distribute next parameter values
    protected HashMap<Long, Object>  perThreadObjects;

    public AbstractParameterDataProvider( String parameterName,
                                          ParameterProviderLevel parameterProviderLevel ) {

        this.parameterName = parameterName;
        this.parameterProviderLevel = parameterProviderLevel;
        this.perThreadIndexes = new HashMap<>();
        this.perThreadObjects = new HashMap<>();
    }

    @Override
    public final void initialize() throws ParameterDataProviderInitalizationException {

        this.perThreadIndexes = new HashMap<>();
        this.perThreadObjects = new HashMap<>();

        doInitialize();
    }

    @Override
    public final ArgumentValue getValue(
                                        List<ArgumentValue> previousValues ) {

       ArgumentValue value = null;
       if( parameterProviderLevel.equals( ParameterProviderLevel.PER_THREAD ) ) {

           //return the next value for the current thread
           value = generateNewValuePerThread( Thread.currentThread().getId(), previousValues );
       } else if( parameterProviderLevel.equals( ParameterProviderLevel.PER_THREAD_STATIC ) ) {

           //always return one and the same value per thread
           value = generateNewValuePerThreadStatic( Thread.currentThread().getId(), previousValues );
       } else {
           // PER_INVOCATION
           //each time return a new value
           value = generateNewValuePerInvocation( previousValues );
       }

       return value;
   }

    @Override
    public String getParameterName() {

        return this.parameterName;
    }

    /**
     * Get the data provider instance
     * @return the data provider instance
     */
    public Class<?> getDataConfiguratorClass() {

        return this.dataProviderInstance;
    }

    /**
     * Do any specific initialization
     */
    protected abstract void doInitialize() throws ParameterDataProviderInitalizationException;

    /**
     * Generate a new value for this argument. Used for PER_INVOCATION provider level
     *
     * @param alreadyResolvedValues already resolved values for current queue iteration
     * @return the newly generated values
     */
    protected abstract ArgumentValue generateNewValuePerInvocation(
                                                                   List<ArgumentValue> alreadyResolvedValues );

    /**
     * Generate a new value for this argument. For PER_THREAD provider level
     *
     * @param currentThreadId current thread id
     * @param alreadyResolvedValues already resolved values for current queue iteration
     * @return the newly generated values
     */
    protected abstract ArgumentValue generateNewValuePerThread(
                                                                long currentThreadId,
                                                                List<ArgumentValue> alreadyResolvedValues );

    /**
     * Generate a new value for this argument. For PER_THREAD_STATIC provider level
     *
     * @param currentThreadId current thread id
     * @param alreadyResolvedValues already resolved values for current queue iteration
     * @return the newly generated values
     */
    protected abstract ArgumentValue generateNewValuePerThreadStatic(
                                                                      long currentThreadId,
                                                                      List<ArgumentValue> alreadyResolvedValues );
                    
            
}
