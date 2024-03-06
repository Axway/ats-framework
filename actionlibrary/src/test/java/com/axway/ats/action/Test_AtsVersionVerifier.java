/*
 * Copyright 2021-2023 Axway Software
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
package com.axway.ats.action;

import org.junit.Assert;
import org.junit.Test;

import com.axway.ats.action.AtsVersionVerifier.Comparison;

public class Test_AtsVersionVerifier extends BaseTest {
    
    @Test
    public void test_verifyVersion() {

        String thisVersion = "4.0.7-M4-1-SNAPSHOT";
        String thatVersion = "4.0.7-M4";

        Assert.assertFalse(AtsVersionVerifier.verifyVersion(thisVersion, thatVersion, Comparison.EQUAL));

        Assert.assertFalse(AtsVersionVerifier.verifyVersion(thisVersion, thatVersion, Comparison.OLDER));

        Assert.assertTrue(AtsVersionVerifier.verifyVersion(thisVersion, thatVersion, Comparison.NEWER));

    }
    @Test
    public void test_verifyVersion4Dots() {

        String thisNewerVersion = "4.0.10.7";
        String thatVersion = "4.0.10.3-SNAPSHOT";

        Assert.assertFalse(AtsVersionVerifier.verifyVersion(thisNewerVersion, thatVersion, Comparison.EQUAL));

        Assert.assertFalse(AtsVersionVerifier.verifyVersion(thisNewerVersion, thatVersion, Comparison.OLDER));

        Assert.assertTrue(AtsVersionVerifier.verifyVersion(thisNewerVersion, thatVersion, Comparison.NEWER));

    }

}
