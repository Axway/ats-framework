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
package com.axway.ats.agent.components.system.operations;

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.ActionRequestInfo;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.core.processtalk.LocalProcessTalker;

public class InternalProcessTalker {

    private LocalProcessTalker processTalker;

    public InternalProcessTalker() {

    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers",
            requestMethod = "PUT")
    public void initProcessTalker(
                                   @Parameter(
                                           name = "command") String command ) {

        processTalker = new LocalProcessTalker(command);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/command",
            requestMethod = "POST")
    public void setCommand(

                            @Parameter(
                                    name = "command") String command ) {

        processTalker.setCommand(command);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/timeout",
            requestMethod = "POST")
    public void setDefaultOperationTimeout(

                                            @Parameter(
                                                    name = "defaultTimeoutSeconds") int defaultTimeoutSeconds ) {

        processTalker.setDefaultOperationTimeout(defaultTimeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/pendingToMatch",
            requestMethod = "GET")
    public String getPendingToMatchContent() {

        return processTalker.getPendingToMatchContent();
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/current",
            requestMethod = "GET")
    public String getCurrentStandardOutContents() {

        return processTalker.getCurrentStandardOutContents();
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stderr/current",
            requestMethod = "GET")
    public String getCurrentStandardErrContents() {

        return processTalker.getCurrentStandardErrContents();
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect",
            requestMethod = "POST")
    public void expect(

                        @Parameter(
                                name = "pattern") String pattern ) {

        processTalker.expect(pattern);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect",
            requestMethod = "POST")
    public void expect(

                        @Parameter(
                                name = "pattern") String pattern,
                        @Parameter(
                                name = "timeoutSeconds") int timeoutSeconds ) {

        processTalker.expect(pattern,
                             timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/byRegex",
            requestMethod = "POST")
    public void expectByRegex(

                               @Parameter(
                                       name = "pattern") String pattern ) {

        processTalker.expectByRegex(pattern);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/byRegex",
            requestMethod = "POST")
    public void expectByRegex(

                               @Parameter(
                                       name = "pattern") String pattern,
                               @Parameter(
                                       name = "timeoutSeconds") int timeoutSeconds ) {

        processTalker.expectByRegex(pattern,
                                    timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/any",
            requestMethod = "POST")
    public int expectAny(

                          @Parameter(
                                  name = "patterns") String[] patterns ) {

        return processTalker.expectAny(patterns);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/any",
            requestMethod = "POST")
    public int expectAny(

                          @Parameter(
                                  name = "patterns") String[] patterns,
                          @Parameter(
                                  name = "timeoutSeconds") int timeoutSeconds ) {

        return processTalker.expectAny(patterns,
                                       timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/any/byRegex",
            requestMethod = "POST")
    public int expectAnyByRegex(

                                 @Parameter(
                                         name = "regexPatterns") String[] regexPatterns ) {

        return processTalker.expectAnyByRegex(regexPatterns);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/any/byRegex",
            requestMethod = "POST")
    public int expectAnyByRegex(

                                 @Parameter(
                                         name = "regexPatterns") String[] regexPatterns,
                                 @Parameter(
                                         name = "timeoutSeconds") int timeoutSeconds ) {

        return processTalker.expectAnyByRegex(regexPatterns,
                                              timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/all",
            requestMethod = "POST")
    public void expectAll(

                           @Parameter(
                                   name = "patterns") String[] patterns ) {

        processTalker.expectAll(patterns);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/all",
            requestMethod = "POST")
    public void expectAll(

                           @Parameter(
                                   name = "patterns") String[] patterns,
                           @Parameter(
                                   name = "timeoutSeconds") int timeoutSeconds ) {

        processTalker.expectAll(patterns,
                                timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/all/byRegex",
            requestMethod = "POST")
    public void expectAllByRegex(

                                  @Parameter(
                                          name = "regexPatterns") String[] regexPatterns ) {

        processTalker.expectAnyByRegex(regexPatterns);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stdout/expect/all/byRegex",
            requestMethod = "POST")
    public void expectAllByRegex(

                                  @Parameter(
                                          name = "regexPatterns") String[] regexPatterns,
                                  @Parameter(
                                          name = "timeoutSeconds") int timeoutSeconds ) {

        processTalker.expectAnyByRegex(regexPatterns,
                                       timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/expect/closed",
            requestMethod = "POST")
    public void expectClose() {

        processTalker.expectClose();
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/expect/closed",
            requestMethod = "POST")
    public void expectClose(

                             @Parameter(
                                     name = "timeOutSeconds") int timeOutSeconds ) {

        processTalker.expectClose(timeOutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stderr/expect",
            requestMethod = "POST")
    public void expectErr(

                           @Parameter(
                                   name = "pattern") String pattern,
                           @Parameter(
                                   name = "timeoutSeconds") int timeoutSeconds ) {

        processTalker.expectErr(pattern,
                                timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stderr/expect/byRegex",
            requestMethod = "POST")
    public void expectErrByRegex(

                                  @Parameter(
                                          name = "pattern") String pattern,
                                  @Parameter(
                                          name = "timeoutSeconds") int timeoutSeconds ) {

        processTalker.expectErrByRegex(pattern,
                                       timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stderr/expect/any",
            requestMethod = "POST")
    public int expectErrAny(

                             @Parameter(
                                     name = "patterns") String[] patterns,
                             @Parameter(
                                     name = "timeoutSeconds") int timeoutSeconds ) {

        return processTalker.expectErrAny(patterns,
                                          timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stderr/expect/any/byRegex",
            requestMethod = "POST")
    public int expectErrAnyByRegex(

                                    @Parameter(
                                            name = "regexPatterns") String[] regexPatterns,
                                    @Parameter(
                                            name = "timeoutSeconds") int timeoutSeconds ) {

        return processTalker.expectErrAnyByRegex(regexPatterns,
                                                 timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stderr/expect/all",
            requestMethod = "POST")
    public void expectErrAll(

                              @Parameter(
                                      name = "patterns") String[] patterns,
                              @Parameter(
                                      name = "timeoutSeconds") int timeoutSeconds ) {

        processTalker.expectErrAll(patterns,
                                   timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/content/stderr/expect/all/byRegex",
            requestMethod = "POST")
    public void expectErrAllByRegex(

                                     @Parameter(
                                             name = "regexPatterns") String[] regexPatterns,
                                     @Parameter(
                                             name = "timeoutSeconds") int timeoutSeconds ) {

        processTalker.expectErrAnyByRegex(regexPatterns,
                                          timeoutSeconds);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/send",
            requestMethod = "POST")
    public void send(

                      @Parameter(
                              name = "text") String text ) {

        processTalker.send(text);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/send/enter",
            requestMethod = "POST")
    public void sendEnterKey() {

        processTalker.sendEnterKey();
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/send/enter/loop",
            requestMethod = "POST")
    public void sendEnterKeyInLoop(

                                    @Parameter(
                                            name = "intermediatePattern") String intermediatePattern,
                                    @Parameter(
                                            name = "finalPattern") String finalPattern,
                                    @Parameter(
                                            name = "maxLoopTimes") int maxLoopTimes ) {

        processTalker.sendEnterKeyInLoop(intermediatePattern,
                                         finalPattern,
                                         maxLoopTimes);
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/closed",
            requestMethod = "GET")
    public boolean isClosed() {

        return processTalker.isClosed();
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/exitValue",
            requestMethod = "GET")
    public int getExitValue() {

        return processTalker.getExitValue();
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/kill",
            requestMethod = "POST")
    public void killExternalProcess() {

        processTalker.killExternalProcess();
    }

    @Action
    @ActionRequestInfo(
            requestUrl = "processes/talkers/kill/all",
            requestMethod = "POST")
    public void killExternalProcessWithChildren() {

        processTalker.killExternalProcessWithChildren();
    }
}
