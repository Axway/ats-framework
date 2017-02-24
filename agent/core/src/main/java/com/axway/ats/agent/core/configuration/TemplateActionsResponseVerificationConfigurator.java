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
package com.axway.ats.agent.core.configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.model.TemplateActionResponseVerificator;
import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateHeaderMatchMode;
import com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions;

/**
 * Defines the way to verify the actions responses
 */
public class TemplateActionsResponseVerificationConfigurator implements Configurator {

    private static final long                                                   serialVersionUID = 1L;

    /**
     * Queue name
     */
    private String                                                              queueName;

    /**
     * Header matchers for all responses
     */
    private Set<HeaderMatcher>                                                  globalHeadersMatchers;

    /**
     * Response verificators per action
     */
    private Map<String, TemplateActionResponseVerificator>                      verificators;

    private static Map<String, TemplateActionsResponseVerificationConfigurator> instances        = new HashMap<String, TemplateActionsResponseVerificationConfigurator>();

    public static synchronized TemplateActionsResponseVerificationConfigurator
            getInstance( String queueName ) {

        return instances.get( queueName );
    }

    public TemplateActionsResponseVerificationConfigurator( String queueName ) {

        this.queueName = queueName;

        // we have some headers that are always skipped
        this.globalHeadersMatchers = new HashSet<HeaderMatcher>();
        // The following code seemed to add these headers for second time on the Agent side.
        // Once they are already provided by the Test Executor.
        //        for( String nonSignificantHeader : TemplateActionsXmlDefinitions.NON_SIGNIFICANT_HEADERS ) {
        //            try {
        //                HeaderMatcher globalHeaderMatcher = new HeaderMatcher( nonSignificantHeader, null,
        //                                                                       TemplateHeaderMatchMode.RANDOM );
        //                // these headers are not mandatory for each response
        //                globalHeaderMatcher.setOptionalHeader( true );
        //
        //                globalHeadersMatchers.add( globalHeaderMatcher );
        //            } catch( InvalidMatcherException e ) {
        //                // can't happen as we define the header names
        //            }
        //        }

        this.verificators = new HashMap<String, TemplateActionResponseVerificator>();

        instances.put( queueName, this );
    }

    public void addGlobalHeaderMatchers( Set<HeaderMatcher> globalMatchers ) {

        this.globalHeadersMatchers.addAll( globalMatchers );
    }

    /**
     * Returns new copy because of same multi threaded access
     */
    public Set<HeaderMatcher> getGlobalHeaderMatchers() {

        // FIXME - get new instance only for new thread. Use ThreadContext
        // return new instance because referenced headers are modified in merging steps in XmlUtilities.verifyResponseHeaders()
        //long startTime = System.nanoTime();
        Set<HeaderMatcher> newCopy = new HashSet<HeaderMatcher>( globalHeadersMatchers.size() );
        for( HeaderMatcher headerMatcher : globalHeadersMatchers ) {
            HeaderMatcher newHeaderMatcher = new HeaderMatcher( headerMatcher );
            newCopy.add( newHeaderMatcher );
        }
        // log.debug( "**** TMP FIXME: copy global header matchers with size: " + globalHeadersMatchers.size() + ", done in " + (System.nanoTime()-startTime) + " ms.");
        return newCopy;
    }

    public void addActionVerificator( TemplateActionResponseVerificator verificator ) {

        this.verificators.put( verificator.getActionName(), verificator );
    }

    public TemplateActionResponseVerificator getActionVerificator( String actionName ) {

        return this.verificators.get( actionName );
    }

    @Override
    public void apply() throws ConfigurationException {

        // the configurator has been deserialized, we need to create a new instance here
        TemplateActionsResponseVerificationConfigurator instance = new TemplateActionsResponseVerificationConfigurator( this.queueName );
        instance.verificators = this.verificators;

        for( HeaderMatcher headerMatcher : this.globalHeadersMatchers ) {
            instance.globalHeadersMatchers.add( headerMatcher );
        }
    }

    @Override
    public boolean needsApplying() throws ConfigurationException {

        // always apply
        return true;
    }

    @Override
    public void revert() throws ConfigurationException {

        // do nothing
    }

    @Override
    public String getDescription() {

        return "template actions response verification configuration";
    }
}
