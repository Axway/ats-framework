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

public class EvenLoadDistributingUtils {

    /**
     * Splits some values evenly(as much as possible) on a number of channels
     * 
     * @param numberValues the number of values to split
     * @param numberChannels
     * @return array with [number of values for all channels without the last one, number of values for the last channel]
     */
    public int[] getEvenLoad(
                              int numberValues,
                              int numberChannels ) {

        int[] channelValues = new int[numberChannels];
        for (int i = numberChannels; i > 0; i--) {
            int iterations = (int) Math.round((float) numberValues / i);
            numberValues -= iterations;
            channelValues[i - 1] = iterations;
        }

        return channelValues;
    }
}
