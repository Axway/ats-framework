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
package com.axway.ats.action.processtalk;

import com.axway.ats.agent.components.system.operations.clients.InternalProcessTalker;
import com.axway.ats.agent.core.action.CallerRelatedInfoRepository;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.process.ProcessTalkException;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.processtalk.IProcessTalker;

public class RemoteProcessTalker implements IProcessTalker {

    private String                atsAgent;
    private String                internalId;

    private InternalProcessTalker remoteProcessTalker;

    public RemoteProcessTalker( String atsAgent,
                                String command ) throws AgentException {

        this.atsAgent = atsAgent;
        this.remoteProcessTalker = new InternalProcessTalker(atsAgent);
        try {
            this.internalId = this.remoteProcessTalker.initProcessTalker(command);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void setCommand(
                            String command ) {

        try {
            remoteProcessTalker.setCommand(internalId, command);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void setDefaultOperationTimeout(
                                            int defaultTimeoutSeconds ) {

        try {
            remoteProcessTalker.setDefaultOperationTimeout(internalId, defaultTimeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public String getPendingToMatchContent() {

        try {
            return remoteProcessTalker.getPendingToMatchContent(internalId);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public String getCurrentStandardOutContents() {

        try {
            return remoteProcessTalker.getCurrentStandardOutContents(internalId);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public String getCurrentStandardErrContents() {

        try {
            return remoteProcessTalker.getCurrentStandardErrContents(internalId);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expect(
                        String pattern ) {

        try {
            remoteProcessTalker.expect(internalId, pattern);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expect(
                        String pattern,
                        int timeoutSeconds ) {

        try {
            remoteProcessTalker.expect(internalId, pattern, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectErr( String pattern, int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectErr(internalId, pattern, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public void expectByRegex(
                               String pattern ) {

        try {
            remoteProcessTalker.expectByRegex(internalId, pattern);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectByRegex(
                               String pattern,
                               int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectByRegex(internalId, pattern, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectErrByRegex( String pattern, int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectErrByRegex(internalId, pattern, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public int expectAny(
                          String[] patterns ) {

        try {
            return remoteProcessTalker.expectAny(internalId, patterns);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectAny(
                          String[] patterns,
                          int timeoutSeconds ) {

        try {
            return remoteProcessTalker.expectAny(internalId, patterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectErrAny( String[] patterns, int timeoutSeconds ) {

        try {
            return remoteProcessTalker.expectErrAny(internalId, patterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectAnyByRegex(
                                 String[] regexPatterns ) {

        try {
            return remoteProcessTalker.expectAnyByRegex(internalId, regexPatterns);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectAnyByRegex(
                                 String[] regexPatterns,
                                 int timeoutSeconds ) {

        try {
            return remoteProcessTalker.expectAnyByRegex(internalId, regexPatterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectErrAnyByRegex( String[] regexPatterns, int timeoutSeconds ) {

        try {
            return remoteProcessTalker.expectErrAnyByRegex(internalId, regexPatterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectAll(
                           String[] patterns ) {

        try {
            remoteProcessTalker.expectAll(internalId, patterns);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectAll(
                           String[] patterns,
                           int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectAll(internalId, patterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectErrAll( String[] patterns, int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectErrAll(internalId, patterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public void expectAllByRegex(
                                  String[] regexPatterns ) {

        try {
            remoteProcessTalker.expectAllByRegex(internalId, regexPatterns);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectAllByRegex(
                                  String[] regexPatterns,
                                  int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectAllByRegex(internalId, regexPatterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectErrAllByRegex( String[] regexPatterns, int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectErrAllByRegex(internalId, regexPatterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public void send(
                      String text ) {

        try {
            remoteProcessTalker.send(internalId, text);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void sendEnterKey() {

        try {
            remoteProcessTalker.sendEnterKey(internalId);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void sendEnterKeyInLoop(
                                    String intermediatePattern,
                                    String finalPattern,
                                    int maxLoopTimes ) {

        try {
            remoteProcessTalker.sendEnterKeyInLoop(internalId,
                                                   intermediatePattern,
                                                   finalPattern,
                                                   maxLoopTimes);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectClose() {

        try {
            remoteProcessTalker.expectClose(internalId);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectClose(
                             int timeOutSeconds ) {

        try {
            remoteProcessTalker.expectClose(internalId, timeOutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public boolean isClosed() {

        try {
            return remoteProcessTalker.isClosed(internalId);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public int getExitValue() {

        try {
            return remoteProcessTalker.getExitValue(internalId);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void killExternalProcess() {

        try {
            remoteProcessTalker.killExternalProcess(internalId);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void killExternalProcessWithChildren() {

        try {
            remoteProcessTalker.killExternalProcessWithChildren(internalId);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    /**
     * The Process Talker instance on a remote agent may keep lots of
     * output data.
     *
     * Here, when this object is garbage collected, we ask the agent to
     * discard its related Process Talker instance.
     *
     * Of course this does not guarantee the prevention of Out of memory errors on the agent,
     * but it is still some form of unattended cleanup.
     */
    @Override
    protected void finalize() throws Throwable {

        TestcaseStateEventsDispacher.getInstance().cleanupInternalObjectResources(atsAgent,
                                                                                  CallerRelatedInfoRepository.KEY_PROCESS_TALKER
                                                                                            + internalId);

        super.finalize();
    }
}
