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

    private static final long serialVersionUID = 1L;

    public int                checkpointSummaryId;
    public String             name;

    public int                numTotal;
    public int                numRunning;
    public int                numPassed;
    public int                numFailed;

    public int                minResponseTime;
    public float              avgResponseTime;
    public int                maxResponseTime;

    public float              minTransferRate;
    public float              avgTransferRate;
    public float              maxTransferRate;
    public String             transferRateUnit;
}
