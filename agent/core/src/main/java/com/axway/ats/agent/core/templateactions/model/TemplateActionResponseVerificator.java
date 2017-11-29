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
package com.axway.ats.agent.core.templateactions.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.ResponseMatcher;

public class TemplateActionResponseVerificator implements Serializable {

    private static final long                   serialVersionUID = 1L;

    // Map<step index for this action, list of header matchers>
    private Map<Integer, List<HeaderMatcher>>   headerMatchersMap;

    // Map<step index for this action, list of body matchers>
    private Map<Integer, List<ResponseMatcher>> bodyMatchersMap;

    private String                              actionName;

    public TemplateActionResponseVerificator( String actionName ) {

        this.actionName = actionName;
        this.bodyMatchersMap = new HashMap<Integer, List<ResponseMatcher>>();
        this.headerMatchersMap = new HashMap<Integer, List<HeaderMatcher>>();
    }

    public String getActionName() {

        return this.actionName;
    }

    /**
     * Gets header matchers from Java test. If empty, creates new one
     * @param stepIndex
     * @return
     */
    public List<HeaderMatcher> getStepHeaderMatchers(
                                                      int stepIndex ) {

        List<HeaderMatcher> headerMatchersForThisStep = headerMatchersMap.get( stepIndex );
        if( headerMatchersForThisStep == null ) {
            headerMatchersForThisStep = new ArrayList<HeaderMatcher>();
            headerMatchersMap.put( stepIndex, headerMatchersForThisStep );
        }

        return headerMatchersForThisStep;
    }

    public void addStepHeaderMatchers(
                                       int stepIndex,
                                       List<HeaderMatcher> stepMatchers ) {

        headerMatchersMap.put( stepIndex, stepMatchers );
    }

    public List<ResponseMatcher> getStepBodyMatchers(
                                                      int stepIndex ) {

        List<ResponseMatcher> bodyMatchersForThisStep = bodyMatchersMap.get( stepIndex );
        if( bodyMatchersForThisStep == null ) {
            List<ResponseMatcher> bodyMatchers = new ArrayList<ResponseMatcher>();
            bodyMatchersMap.put( stepIndex, bodyMatchers );
            return bodyMatchers;
        } else {
            return bodyMatchersMap.get( stepIndex );
        }
    }

    public void addStepBodyMatchers(
                                     int stepIndex,
                                     List<ResponseMatcher> stepMatchers ) {

        bodyMatchersMap.put( stepIndex, stepMatchers );
    }
}
