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

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.process.ProcessTalkException;
import com.axway.ats.core.processtalk.IProcessTalker;
import com.axway.ats.core.processtalk.LocalProcessTalker;
import com.axway.ats.core.utils.HostUtils;

/**
 * A class that can "talk" to external process. For example you can run some
 * installer wizard and answer to its questions.
 *
 * <p>It basically does the following:
 * <ol>
 *   <li>starts an external process</li>
 *   <li>sends commands to this process by talking to its INPUT stream</li>
 *   <li>verifies the expected output is produced by the listening to its OUTPUT
 *       streams (either standard output or standard error
 *       <a href="https://en.wikipedia.org/wiki/Standard_streams">streams</a> ).
 *       <em>Note</em> that <code>expectXXX()</code> methods expect output either
 *       from standard output or from standard error stream but not from both
 *       of them simultaneously.
 *   </li>
 *  </ol>
 *
 * <br>
 * <p>For more info and examples check dedicated
 * <a href="https://axway.github.io/ats-framework/Interacting-with-external-process.html">page</a>
 * in ATS User guide.
 * </p>
 */
@PublicAtsApi
public class ProcessTalker {

    private IProcessTalker processTalker;

    /**
     * Basic constructor which sets the command to run, but without setting default operation timeout
     *
     * @param command the command to run
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public ProcessTalker( String atsAgent, String command ) throws ProcessTalkException {

        this.processTalker = getOperationsImplementationFor(atsAgent, command);
    }

    /**
     * Basic constructor which sets the command to run
     *
     * @param command the command to run
     * @param defaultTimeoutSeconds the default operation timeout
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public ProcessTalker( String atsAgent, String command,
                          int defaultTimeoutSeconds ) throws ProcessTalkException {

        this.processTalker = getOperationsImplementationFor(atsAgent, command);

        setDefaultOperationTimeout(defaultTimeoutSeconds);
    }

    /**
     * Basic constructor which sets the command to run, but without setting default operation timeout
     *
     * @param command the command to run
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public ProcessTalker( String command ) throws ProcessTalkException {

        this.processTalker = getOperationsImplementationFor(null, command);
    }

    /**
     * Basic constructor which sets the command to run
     *
     * @param command the command to run
     * @param defaultTimeoutSeconds the default operation timeout
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public ProcessTalker( String command, int defaultTimeoutSeconds ) throws ProcessTalkException {

        this.processTalker = getOperationsImplementationFor(null, command);

        setDefaultOperationTimeout(defaultTimeoutSeconds);
    }

    /**
     * Set the command to run
     *
     * @param command
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void setCommand( String command ) throws ProcessTalkException {

        this.processTalker.setCommand(command);
    }

    /**
     * Set the default operation timeout.
     *
     * If for some operation no timeout is given, we will use the default one provided with this method.
     *
     * Value of -1 means there is no any timeout. Usually this is not very wise as the execution might
     * hang for ever.
     *
     * @param defaultTimeoutSeconds
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void setDefaultOperationTimeout( int defaultTimeoutSeconds ) throws ProcessTalkException {

        this.processTalker.setDefaultOperationTimeout(defaultTimeoutSeconds);
    }

    /**
     * @return the available contents of Standard Out
     */
    @PublicAtsApi
    public String getCurrentStandardOutContents() {

        return this.processTalker.getCurrentStandardOutContents();
    }

    /**
     * @return the available contents of Standard Err, or null if stderr is not available
     */
    @PublicAtsApi
    public String getCurrentStandardErrContents() {

        return this.processTalker.getCurrentStandardErrContents();
    }

    /**
     * @return the output content after the last match
     */
    @PublicAtsApi
    public String getPendingToMatchContent() {

        return this.processTalker.getPendingToMatchContent();
    }

    /**
     * Expect to match a string from standard output. Comparison is not case-sensitive
     *
     * @param pattern the pattern to match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expect( String pattern ) throws ProcessTalkException {

        this.processTalker.expect(pattern);
    }

    /**
     * Expect to match a string from standard output within given timeout in seconds.
     * Comparison is not case-sensitive
     *
     * @param pattern the pattern to match
     * @param timeoutSeconds timeout waiting for the match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expect( String pattern, int timeoutSeconds ) throws ProcessTalkException {

        this.processTalker.expect(pattern, timeoutSeconds);
    }

    /**
     * Expect to match a string from standard error stream (stderr)
     * within a given timeout in seconds. Comparison is not case-sensitive
     *
     * @param pattern the pattern to match
     * @param timeoutSeconds timeout waiting for the match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectErr(
                           String pattern,
                           int timeoutSeconds ) throws ProcessTalkException {

        this.processTalker.expectErr(pattern, timeoutSeconds);
    }

    /**
     * Expect to match regex pattern to text from standard output stream
     *
     * @param pattern the regex pattern to match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectByRegex( String pattern ) throws ProcessTalkException {

        this.processTalker.expectByRegex(pattern);
    }

    /**
     * Expect to match regex pattern to text from standard output stream
     *
     * @param pattern the regex pattern to match
     * @param timeoutSeconds timeout waiting for the match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectByRegex( String pattern, int timeoutSeconds ) throws ProcessTalkException {

        this.processTalker.expectByRegex(pattern, timeoutSeconds);
    }

    /**
     * Expect to match regex pattern to text from standard error
     *
     * @param pattern the regex pattern to match
     * @param timeoutSeconds timeout waiting for the match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectErrByRegex(
                                  String pattern,
                                  int timeoutSeconds ) throws ProcessTalkException {

        this.processTalker.expectErrByRegex(pattern, timeoutSeconds);
    }

    /**
     * Cycles all of the provided strings and exits when any of them matches
     * text from standard output stream.
     *
     * @param patterns list of patterns to match
     * @return the matched pattern index
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public int expectAny( String[] patterns ) throws ProcessTalkException {

        return this.processTalker.expectAny(patterns);
    }

    /**
     * Cycles all of the provided strings and exits when any of them matches
     * text from standard output stream.
     *
     * @param patterns list of patterns to match
     * @param timeoutSeconds timeout waiting for the match
     * @return the matched pattern index
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public int expectAny( String[] patterns, int timeoutSeconds ) throws ProcessTalkException {

        return this.processTalker.expectAny(patterns, timeoutSeconds);
    }

    /**
     * Cycles all of the provided strings and exits when any of them matches
     * text from standard error stream.
     *
     * @param patterns list of patterns to match
     * @param timeoutSeconds timeout waiting for the match
     * @return the matched pattern index
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public int expectErrAny(
                             String[] patterns,
                             int timeoutSeconds ) throws ProcessTalkException {

        return this.processTalker.expectErrAny(patterns, timeoutSeconds);
    }

    /**
     * Cycles all of the provided regex patterns and exits when any of them matches
     * text from standard output stream.
     *
     * @param regexPatterns list of patterns to match
     * @return the matched pattern index
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public int expectAnyByRegex( String[] regexPatterns ) throws ProcessTalkException {

        return this.processTalker.expectAnyByRegex(regexPatterns);
    }

    /**
     * Cycles all of the provided strings and exits when any of them matches
     * text from standard output stream.
     *
     * @param regexPatterns list of patterns to match
     * @param timeoutSeconds timeout waiting for the match
     * @return the matched pattern index
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public int expectAnyByRegex( String[] regexPatterns, int timeoutSeconds ) throws ProcessTalkException {

        return this.processTalker.expectAnyByRegex(regexPatterns, timeoutSeconds);
    }

    /**
     * Cycles all of the provided regex patterns and exits when any of them matches
     * text from standard error stream.
     *
     * @param regexPatterns list of patterns to match
     * @param timeoutSeconds timeout waiting for the match
     * @return the matched pattern index
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public int expectErrAnyByRegex(
                                    String[] regexPatterns,
                                    int timeoutSeconds ) throws ProcessTalkException {

        return this.processTalker.expectErrAnyByRegex(regexPatterns, timeoutSeconds);
    }

    /**
     * Passes if all of the provided strings match text from standard output of the process.
     * String comparison is case-insensitive.
     *
     * @param patterns list of patterns to match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectAll( String[] patterns ) throws ProcessTalkException {

        this.processTalker.expectAll(patterns);
    }

    /**
     * Passes if all of the provided strings match text from standard output stream
     * of the process. String comparison is case-insensitive.
     *
     * @param patterns list of patterns to match
     * @param timeoutSeconds timeout waiting for the match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectAll( String[] patterns, int timeoutSeconds ) throws ProcessTalkException {

        this.processTalker.expectAll(patterns, timeoutSeconds);
    }

    /**
     * Passes if all of the provided strings match text from standard error stream of
     * the process. String comparison is case-insensitive.
     *
     * @param patterns list of patterns to match
     * @param timeoutSeconds timeout waiting for the match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectErrAll( String[] patterns,
                              int timeoutSeconds ) throws ProcessTalkException {

        this.processTalker.expectErrAll(patterns, timeoutSeconds);
    }

    /**
     * Passes if all of the provided regex patterns match text from standard output
     * stream of the process.
     *
     * @param regexPatterns list of patterns to match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectAllByRegex( String[] regexPatterns ) throws ProcessTalkException {

        this.processTalker.expectAllByRegex(regexPatterns);
    }

    /**
     * Passes if all of the provided regex patterns match text from standard output
     * stream of the process.
     *
     * @param regexPatterns list of patterns to match
     * @param timeoutSeconds timeout waiting for the match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectAllByRegex( String[] regexPatterns, int timeoutSeconds ) throws ProcessTalkException {

        this.processTalker.expectAllByRegex(regexPatterns, timeoutSeconds);
    }

    /**
     * Passes if all of the provided regex patterns match text from standard error
     * stream of the process.
     *
     * @param regexPatterns list of patterns to match
     * @param timeoutSeconds timeout waiting for the match
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectErrAllByRegex(
                                     String[] regexPatterns,
                                     int timeoutSeconds ) throws ProcessTalkException {

        this.processTalker.expectErrAllByRegex(regexPatterns, timeoutSeconds);
    }

    /**
     * Send some text to the external process
     *
     * @param text the text to send
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void send( String text ) throws ProcessTalkException {

        this.processTalker.send(text);
    }

    /**
     * Send ENTER key to the external process
     *
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void sendEnterKey() throws ProcessTalkException {

        this.processTalker.sendEnterKey();
    }

    /**
     * Send ENTER key many times to the external process.
     * This method is convenient when it is expected to have long outputs. For example when reading
     * a license agreement you will get many times the same text('Press enter to continue')
     * and finally you will get some unique text informing you it is over
     *
     * @param intermediatePattern the pattern that is expected to be seen many times
     * @param finalPattern the pattern that says is is over
     * @param maxLoopTimes max number of times to loop
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void sendEnterKeyInLoop( String intermediatePattern, String finalPattern,
                                    int maxLoopTimes ) throws ProcessTalkException {

        this.processTalker.sendEnterKeyInLoop(intermediatePattern, finalPattern, maxLoopTimes);
    }

    /**
     * Expect for the external process to close
     *
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectClose() throws ProcessTalkException {

        this.processTalker.expectClose();
    }

    /**
     * Expect for the external process to close
     *
     * @param timeOutSeconds timeout waiting for this operation
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public void expectClose( int timeOutSeconds ) throws ProcessTalkException {

        this.processTalker.expectClose(timeOutSeconds);
    }

    @PublicAtsApi
    public boolean isClosed() {

        return this.processTalker.isClosed();
    }

    /**
     * @return the exit code of the external process
     * @throws ProcessTalkException
     */
    @PublicAtsApi
    public int getExitValue() throws ProcessTalkException {

        return this.processTalker.getExitValue();
    }

    /**
     * Kill the external process we are talking to. Do not try to kill its children.
     */
    @PublicAtsApi
    public void killExternalProcess() {

        this.processTalker.killExternalProcess();

    }

    /**
     * Kill the external process we are talking to. Try to kill its children as well.
     */
    @PublicAtsApi
    public void killExternalProcessWithChildren() {

        this.processTalker.killExternalProcessWithChildren();
    }

    private IProcessTalker getOperationsImplementationFor( String atsAgent, String command ) {

        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);

        if (HostUtils.isLocalAtsAgent(atsAgent)) {
            return new LocalProcessTalker(command);
        } else {
            return new RemoteProcessTalker(atsAgent, command);
        }
    }
}
