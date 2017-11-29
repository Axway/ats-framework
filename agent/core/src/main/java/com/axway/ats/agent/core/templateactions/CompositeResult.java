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
package com.axway.ats.agent.core.templateactions;

/**
 * Object collecting possible result from execution of template action and 
 * the total net processing time. 
 */
public class CompositeResult {
    long   reqRespNetworkTime;
    Object returnResult;

    /**
     * New object
     * @param returnResult object returned
     * @param reqRespNetworkTime total network time for sending requests and 
     *  receiving responses (including server think time)
     */
    public CompositeResult( Object returnResult,
                            long reqRespNetworkTime ) {

        this.returnResult = returnResult;
        this.reqRespNetworkTime = reqRespNetworkTime;
    }

    public long getReqRespNetworkTime() {

        return reqRespNetworkTime;
    }

    public Object getReturnResult() {

        return returnResult;
    }
}
