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
package com.axway.ats.log.autodb.entities;

public class CheckpointSummary extends DbEntity {

    private static final long serialVersionUID          = 1L;
    private static final int  MAX_TO_STRING_INVOCATIONS = 1000; // to prevent too much logging
    private static       int  toStringInvocationCount   = 0;

    public int    checkpointSummaryId;
    public String name;
    public int    loadQueueId;

    public int numTotal;
    public int numRunning;
    public int numPassed;
    public int numFailed;

    public int    minResponseTime;
    public double avgResponseTime;
    public int    maxResponseTime;

    public double minTransferRate;
    public double avgTransferRate;
    public double maxTransferRate;
    public String transferRateUnit;

    /**
     * Conditional toString only for first several invocations
     * @return null if limit of invocations is reached
     */
    public String limitedToString() {

        if (toStringInvocationCount > MAX_TO_STRING_INVOCATIONS) {
            return null;
        } else {
            toStringInvocationCount++; // synchronization not needed - precise number not important
            return toString();
        }
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder("CheckpointSummary{");
        sb.append("id=").append(checkpointSummaryId);
        sb.append(", name='").append(name).append('\'');
        sb.append(", loadQueueId=").append(loadQueueId);
        sb.append(", numTotal=").append(numTotal);
        sb.append(", numRunning=").append(numRunning);
        sb.append(", numPassed=").append(numPassed);
        sb.append(", numFailed=").append(numFailed);
        sb.append(", minResponseTime=").append(minResponseTime);
        sb.append(", avgResponseTime=").append(avgResponseTime);
        sb.append(", maxResponseTime=").append(maxResponseTime);
        sb.append(", minTransferRate=").append(minTransferRate);
        sb.append(", avgTransferRate=").append(avgTransferRate);
        sb.append(", maxTransferRate=").append(maxTransferRate);
        sb.append(", transferRateUnit='").append(transferRateUnit).append('\'');
        sb.append(", startTimestamp=").append(startTimestamp);
        sb.append(", endTimestamp=").append(endTimestamp);
        sb.append(", timeOffset=").append(timeOffset);
        sb.append('}');
        return sb.toString();
    }
}
