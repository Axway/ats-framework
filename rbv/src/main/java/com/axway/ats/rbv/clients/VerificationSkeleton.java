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
package com.axway.ats.rbv.clients;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.Monitor;
import com.axway.ats.rbv.PollingParameters;
import com.axway.ats.rbv.RbvConfigurator;
import com.axway.ats.rbv.SimpleMonitorListener;
import com.axway.ats.rbv.executors.Executor;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvVerificationException;
import com.axway.ats.rbv.rules.AndRuleOperation;
import com.axway.ats.rbv.storage.Matchable;

public abstract class VerificationSkeleton {

    protected AndRuleOperation rootRule;
    protected Executor         executor;
    protected Matchable        folder;
    
    protected long pollingInitialDelay = -1;
    protected long  pollingInterval = -1;
    protected int pollingAttempts = -1;
    protected long pollingTimeout = -1;

    protected VerificationSkeleton() {

        rootRule = new AndRuleOperation();
    }

    /**
     * Verify the specified Object exists.
     * <br><b>Note:</b> It returns the matched object back to the user in case 
     * user wants to extract some data from it. 
     * 
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     * 
     * @return the matched object
     * @throws RbvException
     */
    protected List<MetaData> verifyObjectExists() throws RbvException {

        return verify( true, true, false );
    }

    /**
     * Verify the Object does not exist
     * 
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     *  
     * @throws RbvException
     */
    protected void verifyObjectDoesNotExist() throws RbvException {

        verify( false, true, false );
    }

    /**
     * Verify the Object exists for the whole polling duration
     * 
     * <li> At successful poll - it will retry until all polling attempts are over 
     * <li> At unsuccessful poll - it will fail
     * 
     * @throws RbvException
     */
    protected List<MetaData> verifyObjectAlwaysExists() throws RbvException {

        return verify( true, false, true );
    }

    /**
     * Verify the Object does not exist for the whole polling duration
     * 
     * <li> At successful poll - it will retry until all polling attempts are over 
     * <li> At unsuccessful poll - it will fail
     * 
     * @throws RbvException
     */
    protected void verifyObjectNeverExists() throws RbvException {

        verify( false, false, true );
    }

    /**
     * Verify that all rules match using the given expected result
     * 
     * @param expectedResult the expected result
     * @param endOnFirstMatch end on first match or keep testing until all polling attempts are exhausted
     * @param endOnFirstFailure end or first failure or keep testing until all polling attempts are exhausted
     * 
     * @return the matched meta data
     * @throws RbvException on error doing the verifications
     */
    protected List<MetaData> verify(
                                     boolean expectedResult,
                                     boolean endOnFirstMatch,
            boolean endOnFirstFailure ) throws RbvException {

        try {
            
            this.executor.setRootRule( this.rootRule );

            applyConfigurationSettings();

            Monitor monitor = new Monitor( getMonitorName(), this.folder, this.executor,
                                           new PollingParameters( pollingInitialDelay, pollingInterval,
                                                                  pollingAttempts ),
                                           expectedResult, endOnFirstMatch, endOnFirstFailure );

            ArrayList<Monitor> monitors = new ArrayList<Monitor>();
            monitors.add( monitor );

            // run the actual evaluation
            String evaluationProblem = new SimpleMonitorListener( monitors ).evaluateMonitors( pollingTimeout );

            if( !StringUtils.isNullOrEmpty( evaluationProblem ) ) {
                throw new RbvVerificationException( "Verification failed - " + monitor.getLastError() );
            }

            return monitor.getAllMatchedMetaData();
        } catch( ConfigurationException ce ) {
            throw new RbvException( "RBV configuration error", ce );
        }
    }
    
    private void applyConfigurationSettings(){
        
        RbvConfigurator rbvConfigurator = RbvConfigurator.getInstance();
        
        if( pollingInitialDelay < 0 ) {
            pollingInitialDelay = rbvConfigurator.getPollingInitialDelay();
        }
        if( pollingInterval < 0 ) {
            pollingInterval = rbvConfigurator.getPollingInterval();
        }
        if( pollingAttempts < 0 ) {
            pollingAttempts = rbvConfigurator.getPollingAttempts();
        }
        if( pollingTimeout < 0 ) {
            pollingTimeout = rbvConfigurator.getPollingTimeout();
        }
        
    }
    
    /**
     * Set polling initial delay for the current instance only, 
     * set negative value to use the default value as defined in the RBV Configuration
     * 
     * @param pollingInitialDelay the initial delay in milliseconds
     */
    @PublicAtsApi
    public void setPollingInitialDelay( long pollingInitialDelay ) {

        this.pollingInitialDelay = pollingInitialDelay;
    }
    
    /**
     * Set polling attempts for the current instance only, 
     * set negative value to use the default value as defined in the RBV Configuration
     * 
     * @param pollingAttempts the number of attempts
     */
    @PublicAtsApi
    public void setPollingAttempts( int pollingAttempts ) {

        this.pollingAttempts = pollingAttempts;
    }

    /**
     * Set polling interval for the current instance only, 
     * set negative value to use the default value as defined in the RBV Configuration
     * 
     * @param pollingInterval the interval between attempts in milliseconds
     */
    @PublicAtsApi
    public void setPollingInterval( long pollingInterval ) {

        this.pollingInterval = pollingInterval;
    }
    
    /**
     * Set polling timeout for the current instance only, 
     * set negative value to use the default value as defined in the RBV Configuration
     * 
     * @param pollingTimeout the polling timeout in milliseconds
     */
    @PublicAtsApi
    public void setPollingTimeout( long pollingTimeout ) {

        this.pollingTimeout = pollingTimeout;
    }
    
    /**
     * Get the name of the monitor
     * 
     * @return the name of the monitor used
     */
    protected abstract String getMonitorName();

    /**
     * Clear all rules
     */
    public void clearRules() {

        this.rootRule.clearRules();
    }
}
