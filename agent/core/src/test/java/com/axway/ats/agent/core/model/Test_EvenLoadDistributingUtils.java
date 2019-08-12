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
package com.axway.ats.agent.core.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class Test_EvenLoadDistributingUtils {

    @Test
    public void ONE_CHANNELS_10_VALUES() {

        int[] distributedVALUES = new EvenLoadDistributingUtils().getEvenLoad( 10, 1 );
        assertEquals( 10, distributedVALUES[0] );
    }

    @Test
    public void TEN_CHANNELS_96_VALUES() {

        int[] distributedVALUES = new EvenLoadDistributingUtils().getEvenLoad( 96, 10 );
        assertEquals( "[9, 10, 9, 10, 9, 10, 9, 10, 10, 10]", Arrays.toString( distributedVALUES ) );
    }

    @Test
    public void NINE_CHANNELS_15_VALUES() {

        int[] distributedVALUES = new EvenLoadDistributingUtils().getEvenLoad( 15, 9 );
        assertEquals( "[1, 2, 1, 2, 1, 2, 2, 2, 2]", Arrays.toString( distributedVALUES ) );
    }

      
    @Test
    public void TEN_CHANNELS_10_VALUES() {

        int[] distributedVALUES = new EvenLoadDistributingUtils().getEvenLoad( 10, 10 );
        assertEquals( "[1, 1, 1, 1, 1, 1, 1, 1, 1, 1]", Arrays.toString( distributedVALUES ) );
    }

    @Test
    public void TEN_CHANNELS_3_VALUES() {

        int[] distributedVALUES = new EvenLoadDistributingUtils().getEvenLoad( 3, 10 );
        assertEquals( "[0, 1, 0, 1, 0, 1, 0, 0, 0, 0]", Arrays.toString( distributedVALUES ) );
    }

    @Test
    public void TEN_CHANNELS_2_VALUES() {

        int[] distributedVALUES = new EvenLoadDistributingUtils().getEvenLoad( 2, 10 );
        assertEquals( "[0, 1, 0, 1, 0, 0, 0, 0, 0, 0]", Arrays.toString( distributedVALUES ) );
    }

}
