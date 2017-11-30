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
package com.axway.ats.agent.webapp.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axway.ats.agent.core.configuration.TemplateActionsResponseVerificationConfigurator;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.model.TemplateActionResponseVerificator;
import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.ResponseMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.TextBodyMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.XPathBodyMatcher;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateBodyNodeMatchMode;
import com.axway.ats.agent.core.templateactions.model.matchers.mode.TemplateHeaderMatchMode;
import com.axway.ats.common.PublicAtsApi;

/**
 * A load client for template actions
 * Some notes:
 * <ul>
 *  <li><em>actionName</em> - The name of particular template(XML) file which will be
 *      used in scenario at some step.
 *  </li>
 *  <li><em>stepNumber</em> - template actions contain one or more request/response pairs
 *   which are identified by stepNumber. Numbering starts from 1
 *  </li>
 * </ul>
 */
@PublicAtsApi
public class TemplateLoadClient extends LoadClient {

    /**
     * Header matchers for all responses
     */
    private Set<HeaderMatcher>                             globalHeadersMatchers;

    private Map<String, TemplateActionResponseVerificator> verificators;

    /**
     * Generic constructor.
     * ATS agents(loaders) must be specified later before executing the test
     * steps. Use {@link #addLoaderHost(String)} for this.
     */
    @PublicAtsApi
    public TemplateLoadClient() {

        this(null);
    }

    /**
     * Constructor providing the ATS agents (used as loaders) to run the test steps from
     * @param atsAgents array of used ATS agents
     */
    @PublicAtsApi
    public TemplateLoadClient( String[] atsAgents ) {

        super(atsAgents);

        this.globalHeadersMatchers = new HashSet<HeaderMatcher>();
        this.verificators = new HashMap<String, TemplateActionResponseVerificator>();
    }

    /**
     * Called right before sending the queued actions
     * @throws AgentException
     */
    @Override
    protected void configureAgentLoaders() throws AgentException {

        super.configureAgentLoaders();

        // get the configurator instance
        TemplateActionsResponseVerificationConfigurator verificationConfigurator = new TemplateActionsResponseVerificationConfigurator(queueName);

        // add globally applicable header matchers
        verificationConfigurator.addGlobalHeaderMatchers(globalHeadersMatchers);

        // add action verificators
        for (TemplateActionResponseVerificator verificator : verificators.values()) {
            verificationConfigurator.addActionVerificator(verificator);
        }

        // send the info to all loaders
        for (String loader : loaderAddresses) {
            new TemplateActionsResponseVerificationClient(loader, verificationConfigurator).send();
        }
    }

    /**
     * Skip this header among all action steps.
     * This behavior will be overwritten for action steps where we have explicitly
     * set a rule for matching it.
     *
     * @param headerToSkip
     * @throws InvalidMatcherException
     */
    @PublicAtsApi
    public void skipGlobalHeader( String headerToSkip ) throws InvalidMatcherException {

        globalHeadersMatchers.add(new HeaderMatcher(headerToSkip, null, TemplateHeaderMatchMode.RANDOM));
    }

    /**
     * Skip these headers among all action steps.
     * This behavior will be overwritten for action steps where we have explicitly
     * set a rule for matching it.
     *
     * @param headerToSkip
     * @throws InvalidMatcherException
     */
    @PublicAtsApi
    public void skipGlobalHeaders( String[] headersToSkip ) throws InvalidMatcherException {

        for (String headerToSkip : headersToSkip) {
            skipGlobalHeader(headerToSkip);
        }
    }

    /**
     * Skip the specified header for the specified action step
     *
     * @param actionName the name of the action
     * @param stepNumber the action step number
     * @param headerToSkip the header to skip
     * @throws InvalidMatcherException
     */
    @PublicAtsApi
    public void skipHeader( String actionName, int stepNumber,
                            String headerToSkip ) throws InvalidMatcherException {

        List<HeaderMatcher> stepMatchers = getStepHeaderMatchers(actionName, stepNumber);
        stepMatchers.add(new HeaderMatcher(headerToSkip, null, TemplateHeaderMatchMode.RANDOM));
    }

    /**
     * Skip the specified headers for the specified action step
     *
     * @param actionName the name of the action
     * @param stepNumber the action step number
     * @param headersToSkip the headers to skip
     * @throws InvalidMatcherException
     */
    @PublicAtsApi
    public void skipHeaders( String actionName, int stepNumber,
                             String[] headersToSkip ) throws InvalidMatcherException {

        List<HeaderMatcher> stepMatchers = getStepHeaderMatchers(actionName, stepNumber);
        for (String headerToSkip : headersToSkip) {
            stepMatchers.add(new HeaderMatcher(headerToSkip, null, TemplateHeaderMatchMode.RANDOM));
        }
    }

    /**
     * Check the specified header for the specified action step
     *
     * @param actionName the name of the action
     * @param stepNumber the action step number
     * @param headerName the header of interest
     * @param headerValueToMatch the expected header value
     * @param matchMode how to match the header value
     * @throws InvalidMatcherException
     */
    @PublicAtsApi
    public void checkHeader( String actionName, int stepNumber, String headerName, String headerValueToMatch,
                             TemplateHeaderMatchMode matchMode ) throws InvalidMatcherException {

        List<HeaderMatcher> stepMatchers = getStepHeaderMatchers(actionName, stepNumber);
        stepMatchers.add(new HeaderMatcher(headerName, headerValueToMatch, matchMode));
    }

    /**
     * Often the response content length may vary slightly from the recorded value.
     * This is OK because some IDs for example may be a little longer/shorter on each run.
     * Using this method you can specify a value which will define a range(below and above)
     * the recorded value
     *
     * @param offsetValue the match will succeed if the actual value is between the <recorded value - offset> and <recorded value + offset>
     * @throws InvalidMatcherException
     */
    @PublicAtsApi
    public void setGlobalContentLenghtHeaderRange( int offsetValue ) throws InvalidMatcherException {

        globalHeadersMatchers.add(new HeaderMatcher("Content-Length", String.valueOf(offsetValue),
                                                    TemplateHeaderMatchMode.RANGE_OFFSET));
    }

    /**
     * Verify the value of a specified response body node
     *
     * @param actionName the name of the action
     * @param stepNumber the action step number
     * @param nodeXPath XPath specifying the node
     * @param valueToMatch the expected value
     * @param matchMode the match mode
     *
     * @throws InvalidMatcherException
     */
    @PublicAtsApi
    public void checkBodyNode( String actionName, int stepNumber, String nodeXPath, String valueToMatch,
                               TemplateBodyNodeMatchMode matchMode ) throws InvalidMatcherException {

        List<ResponseMatcher> stepMatchers = getStepBodyMatchers(actionName, stepNumber);
        stepMatchers.add(new XPathBodyMatcher(nodeXPath, valueToMatch, matchMode));
    }

    /**
     * Verify the body contains the provided text
     *
     * @param actionName the name of the action
     * @param stepNumber the action step number
     * @param searchedText the text to find. It is not interpreted as RegEx.
     */
    @PublicAtsApi
    public void checkBodyByContainedText( String actionName, int stepNumber, String searchedText ) {

        checkBodyByContainedText(actionName, stepNumber, searchedText, false);
    }

    /**
     * Verify the body contains the provided text
     *
     * @param actionName the name of the action
     * @param stepNumber the action step number
     * @param searchedText the text to find
     * @param isRegEx should searched text be interpreted as RegEx
     */
    @PublicAtsApi
    public void checkBodyByContainedText( String actionName, int stepNumber, String searchedText,
                                          boolean isRegEx ) {

        List<ResponseMatcher> stepMatchers = getStepBodyMatchers(actionName, stepNumber);
        stepMatchers.add(new TextBodyMatcher(searchedText, isRegEx));
    }

    /**
     * Verify the body DOES NOT contain the provided text
     *
     * @param actionName the name of the action
     * @param stepNumber the action step number
     * @param searchedText the text to find
     */
    @PublicAtsApi
    public void checkBodyForNotContainedText( String actionName, int stepNumber, String searchedText,
                                              boolean isRegex ) {

        List<ResponseMatcher> stepMatchers = getStepBodyMatchers(actionName, stepNumber);
        stepMatchers.add(new TextBodyMatcher(searchedText, isRegex, true));
    }

    private List<HeaderMatcher> getStepHeaderMatchers( String actionName, int stepNumber ) {

        TemplateActionResponseVerificator actionVerificator = verificators.get(actionName);
        if (actionVerificator == null) {
            actionVerificator = new TemplateActionResponseVerificator(actionName);
            verificators.put(actionName, actionVerificator);
        }

        return actionVerificator.getStepHeaderMatchers(stepNumber);
    }

    private List<ResponseMatcher> getStepBodyMatchers( String actionName, int stepNumber ) {

        TemplateActionResponseVerificator actionVerificator = verificators.get(actionName);
        if (actionVerificator == null) {
            actionVerificator = new TemplateActionResponseVerificator(actionName);
            verificators.put(actionName, actionVerificator);
        }

        return actionVerificator.getStepBodyMatchers(stepNumber);
    }
}
