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
package com.axway.ats.core.processtalk;

public interface IProcessTalker {

    public void setCommand(
                            String command );

    public void setDefaultOperationTimeout(
                                            int defaultTimeoutSeconds );

    public String getPendingToMatchContent();

    public String getCurrentStandardOutContents();

    public String getCurrentStandardErrContents();
    
    public void expect(
                        String pattern );

    public void expect(
                        String pattern,
                        int timeoutSeconds );

    public void expectErr(
                           String pattern,
                           int timeoutSeconds );

    public void expectByRegex(
                               String pattern );

    public void expectByRegex(
                               String pattern,
                               int timeoutSeconds );

    public void expectErrByRegex(
                                  String pattern,
                                  int timeoutSeconds );

    public int expectAny(
                          String[] patterns );

    public int expectAny(
                          String[] patterns,
                          int timeoutSeconds );

    public int expectErrAny(
                             String[] patterns,
                             int timeoutSeconds );

    public int expectAnyByRegex(
                                 String[] regexPatterns );

    public int expectAnyByRegex(
                                 String[] regexPatterns,
                                 int timeoutSeconds );

    public int expectErrAnyByRegex(
                                    String[] regexPatterns,
                                    int timeoutSeconds );

    public void expectAll(
                           String[] patterns );

    public void expectAll(
                           String[] patterns,
                           int timeoutSeconds );

    public void expectErrAll(
                              String[] patterns,
                              int timeoutSeconds );

    public void expectAllByRegex(
                                  String[] regexPatterns );

    public void expectAllByRegex(
                                  String[] regexPatterns,
                                  int timeoutSeconds );

    public void expectErrAllByRegex(
                                     String[] regexPatterns,
                                     int timeoutSeconds );

    public void send(
                      String text );

    public void sendEnterKey();

    public void sendEnterKeyInLoop(
                                    String intermediatePattern,
                                    String finalPattern,
                                    int maxLoopTimes );

    public void expectClose();

    public void expectClose(
                             int timeOutSeconds );

    public boolean isClosed();

    public int getExitValue();

    public void killExternalProcess();

    public void killExternalProcessWithChildren();
}
