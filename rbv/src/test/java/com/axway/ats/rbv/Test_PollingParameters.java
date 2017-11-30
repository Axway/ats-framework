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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.axway.ats.rbv.PollingParameters;

public class Test_PollingParameters extends BaseTest {

    @Test
    public void testGetters() {

        PollingParameters params = new PollingParameters(100, 200, 5);

        assertTrue(params.getInitialDelay() == 100);
        assertTrue(params.getPollInterval() == 200);
        assertTrue(params.getPollAttempts() == 5);
    }

    @Test
    public void testSetters() {

        PollingParameters params = new PollingParameters(10, 20, 1);
        params.setInitialDelay(100);
        params.setPollInterval(200);
        params.setPollAttempts(5);

        assertTrue(params.getInitialDelay() == 100);
        assertTrue(params.getPollInterval() == 200);
        assertTrue(params.getPollAttempts() == 5);
    }
}
