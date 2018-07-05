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
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.process.ProcessTalkException;
import com.axway.ats.core.processtalk.IProcessTalker;

public class RemoteProcessTalker implements IProcessTalker {

    private String                atsAgent;

    private InternalProcessTalker remoteProcessTalker;

    public RemoteProcessTalker( String atsAgent,
                                String command ) throws AgentException {

        this.atsAgent = atsAgent;
        this.remoteProcessTalker = new InternalProcessTalker(this.atsAgent);
        try {
            this.remoteProcessTalker.initProcessTalker(command);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void setCommand(
                            String command ) {

        try {
            remoteProcessTalker.setCommand(command);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void setDefaultOperationTimeout(
                                            int defaultTimeoutSeconds ) {

        try {
            remoteProcessTalker.setDefaultOperationTimeout(defaultTimeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public String getPendingToMatchContent() {

        try {
            return remoteProcessTalker.getPendingToMatchContent();
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public String getCurrentStandardOutContents() {

        try {
            return remoteProcessTalker.getCurrentStandardOutContents();
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public String getCurrentStandardErrContents() {

        try {
            return remoteProcessTalker.getCurrentStandardErrContents();
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expect(
                        String pattern ) {

        try {
            remoteProcessTalker.expect(pattern);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expect(
                        String pattern,
                        int timeoutSeconds ) {

        try {
            remoteProcessTalker.expect( pattern, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectErr( String pattern, int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectErr( pattern, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public void expectByRegex(
                               String pattern ) {

        try {
            remoteProcessTalker.expectByRegex( pattern);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectByRegex(
                               String pattern,
                               int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectByRegex( pattern, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectErrByRegex( String pattern, int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectErrByRegex( pattern, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public int expectAny(
                          String[] patterns ) {

        try {
            return remoteProcessTalker.expectAny( patterns);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectAny(
                          String[] patterns,
                          int timeoutSeconds ) {

        try {
            return remoteProcessTalker.expectAny( patterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectErrAny( String[] patterns, int timeoutSeconds ) {

        try {
            return remoteProcessTalker.expectErrAny( patterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectAnyByRegex(
                                 String[] regexPatterns ) {

        try {
            return remoteProcessTalker.expectAnyByRegex( regexPatterns);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectAnyByRegex(
                                 String[] regexPatterns,
                                 int timeoutSeconds ) {

        try {
            return remoteProcessTalker.expectAnyByRegex( regexPatterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public int expectErrAnyByRegex( String[] regexPatterns, int timeoutSeconds ) {

        try {
            return remoteProcessTalker.expectErrAnyByRegex( regexPatterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectAll(
                           String[] patterns ) {

        try {
            remoteProcessTalker.expectAll( patterns);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectAll(
                           String[] patterns,
                           int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectAll( patterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectErrAll( String[] patterns, int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectErrAll( patterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public void expectAllByRegex(
                                  String[] regexPatterns ) {

        try {
            remoteProcessTalker.expectAllByRegex( regexPatterns);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectAllByRegex(
                                  String[] regexPatterns,
                                  int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectAllByRegex( regexPatterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectErrAllByRegex( String[] regexPatterns, int timeoutSeconds ) {

        try {
            remoteProcessTalker.expectErrAllByRegex( regexPatterns, timeoutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public void send(
                      String text ) {

        try {
            remoteProcessTalker.send( text);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void sendEnterKey() {

        try {
            remoteProcessTalker.sendEnterKey();
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
            remoteProcessTalker.sendEnterKeyInLoop(
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
            remoteProcessTalker.expectClose();
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void expectClose(
                             int timeOutSeconds ) {

        try {
            remoteProcessTalker.expectClose( timeOutSeconds);
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public boolean isClosed() {

        try {
            return remoteProcessTalker.isClosed();
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }

    }

    @Override
    public int getExitValue() {

        try {
            return remoteProcessTalker.getExitValue();
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void killExternalProcess() {

        try {
            remoteProcessTalker.killExternalProcess();
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }

    @Override
    public void killExternalProcessWithChildren() {

        try {
            remoteProcessTalker.killExternalProcessWithChildren();
        } catch (AgentException e) {
            throw new ProcessTalkException(e);
        }
    }
}
