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

import com.axway.ats.agent.core.action.CallerRelatedAction;
import com.axway.ats.agent.core.action.CallerRelatedInfoRepository;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.core.processtalk.LocalProcessTalker;

public class InternalProcessTalker extends CallerRelatedAction {

    private static final String OBJECT_KEY_PREFIX = CallerRelatedInfoRepository.KEY_PROCESS_TALKER;

    public InternalProcessTalker( String caller ) {

        super( caller );
    }

    @Action
    public String initProcessTalker(
                                     @Parameter(name = "command") String command ) {

        // Add a new instance into the repository and return its unique counter 
        // which will be used in next calls
        return dataRepo.addObject( OBJECT_KEY_PREFIX, new LocalProcessTalker( command ) );
    }

    @Action
    public void setCommand(
                            @Parameter(name = "internalProcessId") String internalProcessId,
                            @Parameter(name = "command") String command ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).setCommand( command );
    }

    @Action
    public void setDefaultOperationTimeout(
                                            @Parameter(name = "internalProcessId") String internalProcessId,
                                            @Parameter(name = "defaultTimeoutSeconds") int defaultTimeoutSeconds ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).setDefaultOperationTimeout( defaultTimeoutSeconds );
    }

    @Action
    public String getPendingToMatchContent(
                                            @Parameter(name = "internalProcessId") String internalProcessId ) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).getPendingToMatchContent();
    }

    @Action
    public String getCurrentStandardOutContents(
                                                 @Parameter(name = "internalProcessId") String internalProcessId ) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).getCurrentStandardOutContents();
    }

    @Action
    public String getCurrentStandardErrContents(
                                                 @Parameter(name = "internalProcessId") String internalProcessId ) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).getCurrentStandardErrContents();
    }

    @Action
    public void expect(
                        @Parameter(name = "internalProcessId") String internalProcessId,
                        @Parameter(name = "pattern") String pattern ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expect( pattern );
    }

    @Action
    public void expectByRegex(
                               @Parameter(name = "internalProcessId") String internalProcessId,
                               @Parameter(name = "pattern") String pattern ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectByRegex( pattern );
    }

    @Action
    public void expect(
                        @Parameter(name = "internalProcessId") String internalProcessId,
                        @Parameter(name = "pattern") String pattern,
                        @Parameter(name = "timeoutSeconds") int timeoutSeconds ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expect( pattern,
                                                                                     timeoutSeconds );
    }
    
    @Action
    public void expectErr(
                        @Parameter(name = "internalProcessId" ) String internalProcessId,
                        @Parameter(name = "pattern") String pattern,
                        @Parameter(name = "timeoutSeconds") int timeoutSeconds) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectErr( pattern,
                                                                                     timeoutSeconds );
    }

    @Action
    public void expectByRegex(
                               @Parameter(name = "internalProcessId") String internalProcessId,
                               @Parameter(name = "pattern") String pattern,
                               @Parameter(name = "timeoutSeconds") int timeoutSeconds ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectByRegex( pattern,
                                                                                            timeoutSeconds );
    }
    
    @Action
    public void expectErrByRegex(
                               @Parameter(name = "internalProcessId" ) String internalProcessId,
                               @Parameter(name = "pattern") String pattern,
                               @Parameter(name = "timeoutSeconds") int timeoutSeconds) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectErrByRegex( pattern,
                                                                                            timeoutSeconds );
    }

    @Action
    public void expectClose(
                             @Parameter(name = "internalProcessId") String internalProcessId ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).expectClose();
    }

    @Action
    public void expectClose(
                             @Parameter(name = "internalProcessId") String internalProcessId,
                             @Parameter(name = "timeOutSeconds") int timeOutSeconds ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectClose( timeOutSeconds );
    }

    @Action
    public void send(
                      @Parameter(name = "internalProcessId") String internalProcessId,
                      @Parameter(name = "text") String text ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).send( text );
    }

    @Action
    public void sendEnterKey(
                              @Parameter(name = "internalProcessId") String internalProcessId ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).sendEnterKey();
    }

    @Action
    public void sendEnterKeyInLoop(
                                    @Parameter(name = "internalProcessId") String internalProcessId,
                                    @Parameter(name = "intermediatePattern") String intermediatePattern,
                                    @Parameter(name = "finalPattern") String finalPattern,
                                    @Parameter(name = "maxLoopTimes") int maxLoopTimes ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).sendEnterKeyInLoop( intermediatePattern,
                                                                                                 finalPattern,
                                                                                                 maxLoopTimes );
    }

    @Action
    public int expectAny(
                          @Parameter(name = "internalProcessId") String internalProcessId,
                          @Parameter(name = "patterns") String[] patterns ) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).expectAny( patterns );
    }

    @Action
    public int expectAny(
                          @Parameter(name = "internalProcessId") String internalProcessId,
                          @Parameter(name = "patterns") String[] patterns,
                          @Parameter(name = "timeoutSeconds") int timeoutSeconds ) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).expectAny( patterns,
                                                                                               timeoutSeconds );
    }
    
    @Action
    public int expectErrAny(
                          @Parameter(name = "internalProcessId" ) String internalProcessId,
                          @Parameter(name = "patterns") String[] patterns,
                          @Parameter(name = "timeoutSeconds") int timeoutSeconds) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).expectErrAny( patterns,
                                                                                               timeoutSeconds );
    }

    @Action
    public int expectAnyByRegex(
                                 @Parameter(name = "internalProcessId") String internalProcessId,
                                 @Parameter(name = "regexPatterns") String[] regexPatterns ) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).expectAnyByRegex( regexPatterns );
    }

    @Action
    public int expectAnyByRegex(
                                 @Parameter(name = "internalProcessId") String internalProcessId,
                                 @Parameter(name = "regexPatterns") String[] regexPatterns,
                                 @Parameter(name = "timeoutSeconds") int timeoutSeconds ) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).expectAnyByRegex( regexPatterns,
                                                                                                      timeoutSeconds );
    }
    
    @Action
    public int expectErrAnyByRegex(
                                 @Parameter(name = "internalProcessId" ) String internalProcessId,
                                 @Parameter(name = "regexPatterns") String[] regexPatterns,
                                 @Parameter(name = "timeoutSeconds") int timeoutSeconds) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).expectErrAnyByRegex( regexPatterns,
                                                                                                      timeoutSeconds );
    }

    @Action
    public void expectAll(
                           @Parameter(name = "internalProcessId") String internalProcessId,
                           @Parameter(name = "patterns") String[] patterns ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectAll( patterns );
    }

    @Action
    public void expectAll(
                           @Parameter(name = "internalProcessId") String internalProcessId,
                           @Parameter(name = "patterns") String[] patterns,
                           @Parameter(name = "timeoutSeconds") int timeoutSeconds ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectAll( patterns,
                                                                                        timeoutSeconds );
    }
    
    @Action
    public void expectErrAll(
                           @Parameter(name = "internalProcessId" ) String internalProcessId,
                           @Parameter(name = "patterns") String[] patterns,
                           @Parameter(name = "timeoutSeconds") int timeoutSeconds) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectErrAll( patterns,
                                                                                        timeoutSeconds );
    }

    @Action
    public void expectAllByRegex(
                                  @Parameter(name = "internalProcessId") String internalProcessId,
                                  @Parameter(name = "regexPatterns") String[] regexPatterns ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectAnyByRegex( regexPatterns );
    }

    @Action
    public void expectAllByRegex(
                                  @Parameter(name = "internalProcessId") String internalProcessId,
                                  @Parameter(name = "regexPatterns") String[] regexPatterns,
                                  @Parameter(name = "timeoutSeconds") int timeoutSeconds ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectAnyByRegex( regexPatterns,
                                                                                               timeoutSeconds );
    }
    
    @Action
    public void expectErrAllByRegex(
                                  @Parameter(name = "internalProcessId" ) String internalProcessId,
                                  @Parameter(name = "regexPatterns") String[] regexPatterns,
                                  @Parameter(name = "timeoutSeconds") int timeoutSeconds) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).expectErrAnyByRegex( regexPatterns,
                                                                                               timeoutSeconds );
    }

    @Action
    public boolean isClosed(
                             @Parameter(name = "internalProcessId") String internalProcessId ) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).isClosed();
    }

    @Action
    public int getExitValue(
                             @Parameter(name = "internalProcessId") String internalProcessId ) {

        return ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                            + internalProcessId ) ).getExitValue();
    }

    @Action
    public void killExternalProcess(
                                     @Parameter(name = "internalProcessId") String internalProcessId ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).killExternalProcess();
    }

    @Action
    public void killExternalProcessWithChildren(
                                                 @Parameter(name = "internalProcessId") String internalProcessId ) {

        ( ( LocalProcessTalker ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                     + internalProcessId ) ).killExternalProcessWithChildren();
    }
}
