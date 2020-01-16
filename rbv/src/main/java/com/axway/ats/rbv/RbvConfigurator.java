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
package com.axway.ats.rbv;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.AbstractConfigurator;

/**
 * Class used to configure the behavior of the RBV engine.
 *
 * <br><br>
 * <b>User guide</b> 
 * <a href="https://axway.github.io/ats-framework/Common-test-verifications.html">page</a>
 * for RBV.
 */
@PublicAtsApi
public class RbvConfigurator extends AbstractConfigurator {

    private static final String    RBV_CONFIG_FILE           = "/ats.rbv.properties";

    //the keys of the configuration values in the file
    private static final String    POLLING_INITIAL_DELAY_KEY = "rbv.polling.initialdelay";
    private static final String    POLLING_ATTEMPTS_KEY      = "rbv.polling.attempts";
    private static final String    POLLING_INTERVAL_KEY      = "rbv.polling.interval";
    private static final String    POLLING_TIMEOUT_KEY       = "rbv.polling.timeout";

    /**
     * The singleton instance for this configurator
     */
    private static RbvConfigurator instance;

    private RbvConfigurator( String configurationSource ) {

        super();

        //add the resource to the repository
        addConfigFileFromClassPath(configurationSource, true, false);

    }

    @PublicAtsApi
    public static synchronized RbvConfigurator getInstance() {

        if (instance == null) {
            instance = new RbvConfigurator(RBV_CONFIG_FILE);
        }
        return instance;
    }

    /**
     * Get the polling initial delay
     * 
     * @return the initial delay in milliseconds
     */
    @PublicAtsApi
    public long getPollingInitialDelay() {

        return getLongProperty(POLLING_INITIAL_DELAY_KEY);
    }

    /**
     * Set the polling initial delay
     * 
     * @param pollingInitialDelay the initial delay in milliseconds
     * @return the previous value
     */
    @PublicAtsApi
    public long setPollingInitialDelay(
                                        long pollingInitialDelay ) {

        long currentPollingInitialDelay = getPollingInitialDelay();

        setTempProperty(POLLING_INITIAL_DELAY_KEY, Long.toString(pollingInitialDelay));
        return currentPollingInitialDelay;
    }

    /**
     * Get the polling attempts
     * 
     * @return the number of attempts
     */
    @PublicAtsApi
    public int getPollingAttempts() {

        return getIntegerProperty(POLLING_ATTEMPTS_KEY);
    }

    /**
     * Set the polling attempts
     * 
     * @param pollingAttempts the number of attempts
     * @return the previous value
     */
    @PublicAtsApi
    public int setPollingAttempts(
                                   int pollingAttempts ) {

        int currentPollingAttempts = getPollingAttempts();

        setTempProperty(POLLING_ATTEMPTS_KEY, Integer.toString(pollingAttempts));
        return currentPollingAttempts;
    }

    /**
     * Get the polling interval
     * 
     * @return the interval between attempts in milliseconds
     */
    @PublicAtsApi
    public long getPollingInterval() {

        return getLongProperty(POLLING_INTERVAL_KEY);
    }

    /**
     * Set the polling interval between two attempts
     * 
     * @param pollingInterval the interval between attempts in milliseconds
     * @return the previous value
     */
    @PublicAtsApi
    public long setPollingInterval(
                                    long pollingInterval ) {

        long currentPollingInterval = getPollingInterval();

        setTempProperty(POLLING_INTERVAL_KEY, Long.toString(pollingInterval));
        return currentPollingInterval;
    }

    /**
     * Get the timeout for polling - if this timeout is exceeded
     * polling will be terminated
     * 
     * @return the polling timeout in milliseconds
     * @return the previous value
     */
    @PublicAtsApi
    public long getPollingTimeout() {

        return getLongProperty(POLLING_TIMEOUT_KEY);
    }

    /**
     * Set the timeout for polling - if this timeout is exceeded
     * polling will be terminated
     * 
     * @param pollingTimeout the polling timeout in milliseconds
     */
    @PublicAtsApi
    public long setPollingTimeout(
                                   long pollingTimeout ) {

        long currentPollingTimeout = getPollingTimeout();

        setTempProperty(POLLING_TIMEOUT_KEY, Long.toString(pollingTimeout));
        return currentPollingTimeout;
    }

    @Override
    protected void reloadData() {

        //no need to do anything
    }
}
