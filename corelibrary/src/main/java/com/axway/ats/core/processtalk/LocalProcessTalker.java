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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.axway.ats.core.process.ProcessUtils;
import com.axway.ats.expectj.ExpectJ;
import com.axway.ats.expectj.ExpectJException;
import com.axway.ats.expectj.Spawn;
import com.axway.ats.expectj.TimeoutException;
import com.axway.ats.common.process.ProcessTalkException;

public class LocalProcessTalker implements IProcessTalker {

    private ExpectJ expect;
    private Spawn   shell;

    /**
     * If no timeout is given for some operation, we will use the default one.
     * If default timeout is -1, this means there is no any timeout.
     * 
     * If we give some timeout in the constructor of ExpcetJ class, this will mean
     * we set a global timeout for the whole usage. But we do not want this. We only
     * want to set timeout to some of the simple operations(like expectXYZ).
     */
    private int defaultTimeoutSeconds = -1;

    private String command;

    public LocalProcessTalker( String command ) throws ProcessTalkException {

        this.command = command;

        expect = new ExpectJ();

        try {
            shell = expect.spawn( command );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error starting command '" + command + "'", e );
        }
    }

    public void setCommand(
                            String command ) throws ProcessTalkException {

        this.command = command;

        try {
            shell = expect.spawn( command );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error starting command '" + command + "'", e );
        }
    }

    public void setDefaultOperationTimeout(
                                            int defaultTimeoutSeconds ) throws ProcessTalkException {

        if( defaultTimeoutSeconds < -1 ) {
            throw new ProcessTalkException( "Invalid timeout value '" + defaultTimeoutSeconds
                                            + "'. It must be a positive number or -1 for no timeout" );
        } else {
            this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        }
    }

    public void expect(
                        String pattern ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.expect( pattern, false, defaultTimeoutSeconds );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error expecting pattern '" + pattern + "'", e );
        } catch( TimeoutException e ) {
            throw new ProcessTalkException( "Error expecting pattern '" + pattern + "'", e );
        }
    }

    public void expect(
                        String pattern,
                        int timeoutSeconds ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.expect( pattern, false, timeoutSeconds );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error expecting pattern '" + pattern + "'", e );
        } catch( TimeoutException e ) {
            throw new ProcessTalkException( "Error expecting pattern '" + pattern + "'", e );
        }
    }

    public void expectByRegex(
                               String pattern ) throws ProcessTalkException {

        expectByRegex( pattern, defaultTimeoutSeconds );
    }

    public void expectByRegex(
                               String pattern,
                               int timeoutSeconds ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.expect( pattern, true, timeoutSeconds );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error expecting pattern '" + pattern + "'", e );
        } catch( TimeoutException e ) {
            throw new ProcessTalkException( "Error expecting pattern '" + pattern + "'", e );
        }
    }

    public int expectAny(
                          String[] patterns ) throws ProcessTalkException {

        return expectAny( patterns, defaultTimeoutSeconds );
    }

    public int expectAny(
                          String[] patterns,
                          int timeoutSeconds ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            /*
             * In some of the methods we convert String[] into List<String>
             * We cannot simply use Arrays.asList() as it creates a fixed size list
             * and while matching the patterns we cannot remove them from the list
             * so it throws a java.lang.UnsupportedOperationException
             */
            return shell.expectAny( new ArrayList<String>( Arrays.asList( patterns ) ),
                                    false,
                                    timeoutSeconds );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error expecting any of the following patterns '"
                                            + Arrays.toString( patterns ) + "'", e );
        } catch( ExpectJException e ) {
            throw new ProcessTalkException( "Error expecting any of the following patterns '"
                                            + Arrays.toString( patterns ) + "'", e );
        }
    }

    public int expectAnyByRegex(
                                 String[] regexPatterns ) throws ProcessTalkException {

        return expectAnyByRegex( regexPatterns, defaultTimeoutSeconds );
    }

    public int expectAnyByRegex(
                                 String[] regexPatterns,
                                 int timeoutSeconds ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            return shell.expectAny( new ArrayList<String>( Arrays.asList( regexPatterns ) ),
                                    true,
                                    timeoutSeconds );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error expecting any of the following regex patterns '"
                                            + Arrays.toString( regexPatterns ) + "'", e );
        } catch( ExpectJException e ) {
            throw new ProcessTalkException( "Error expecting any of the following regex patterns '"
                                            + Arrays.toString( regexPatterns ) + "'", e );
        }
    }

    public void expectAll(
                           String[] patterns ) throws ProcessTalkException {

        expectAll( patterns, defaultTimeoutSeconds );
    }

    public void expectAll(
                           String[] patterns,
                           int timeoutSeconds ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.expectAll( new ArrayList<String>( Arrays.asList( patterns ) ), false, timeoutSeconds );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error expecting the following patterns '"
                                            + Arrays.toString( patterns ) + "'", e );
        } catch( TimeoutException e ) {
            throw new ProcessTalkException( "Error expecting the following patterns '"
                                            + Arrays.toString( patterns ) + "'", e );
        }
    }

    public void expectAllByRegex(
                                  String[] regexPatterns ) throws ProcessTalkException {

        expectAllByRegex( regexPatterns, defaultTimeoutSeconds );
    }

    public void expectAllByRegex(
                                  String[] regexPatterns,
                                  int timeoutSeconds ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.expectAll( new ArrayList<String>( Arrays.asList( regexPatterns ) ), true, timeoutSeconds );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error expecting the following regex patterns '"
                                            + Arrays.toString( regexPatterns ) + "'", e );
        } catch( TimeoutException e ) {
            throw new ProcessTalkException( "Error expecting the following regex patterns '"
                                            + Arrays.toString( regexPatterns ) + "'", e );
        }
    }

    public void send(
                      String text ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.send( text );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error sending '" + text + "' to the external process", e );
        }
    }

    public void sendEnterKey() throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.sendEnterKey();
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error sending ENTER to the external process", e );
        }
    }

    public void sendEnterKeyInLoop(
                                    String intermediatePattern,
                                    String finalPattern,
                                    int maxLoopTimes ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.sendEnterKeyInLoop( intermediatePattern, finalPattern, maxLoopTimes );
        } catch( IOException e ) {
            throw new ProcessTalkException( "Error sending ENTER in loop to the external process", e );
        } catch( TimeoutException e ) {
            throw new ProcessTalkException( "Error sending ENTER in loop to the external process", e );
        } catch( InterruptedException e ) {
            throw new ProcessTalkException( "Error sending ENTER in loop to the external process", e );
        }
    }

    /**
     * @return true if the process has exited.
     */
    public boolean isClosed() throws ProcessTalkException {

        try {
            return shell.isClosed();
        } catch( Exception e ) {
            throw new ProcessTalkException( "Error checking if the external process is closed.", e );
        }
    }

    public int getExitValue() throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            return shell.getExitValue();
        } catch( ExpectJException e ) {
            throw new ProcessTalkException( "Error getting the external process exit value", e );
        }
    }

    public void expectClose() throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.expectClose();
        } catch( ExpectJException e ) {
            throw new ProcessTalkException( "Error waiting for the external process to finish", e );
        } catch( TimeoutException e ) {
            throw new ProcessTalkException( "Error waiting for the external process to finish", e );
        }
    }

    public void expectClose(
                             int timeOutSeconds ) throws ProcessTalkException {

        checkCommandIsGiven();

        try {
            shell.expectClose( timeOutSeconds );
        } catch( TimeoutException e ) {
            throw new ProcessTalkException( "Error waiting for the external process to finish", e );
        } catch( ExpectJException e ) {
            throw new ProcessTalkException( "Error waiting for the external process to finish", e );
        }
    }

    public String getPendingToMatchContent() {

        return shell.getPendingToMatchContent();
    }

    /**
     * @return the available contents of Standard Out
     */
    public String getCurrentStandardOutContents() {

        try {
            return shell.getCurrentStandardOutContents();
        } catch( Exception e ) {
            throw new ProcessTalkException( "Error getting the current output content.", e );
        }
    }

    /**
     * @return the available contents of Standard Err, or null if stderr is not available
     */
    public String getCurrentStandardErrContents() {

        try {
            return shell.getCurrentStandardErrContents();
        } catch( Exception e ) {
            throw new ProcessTalkException( "Error getting the current error content.", e );
        }
    }

    public void killExternalProcess() {

        checkCommandIsGiven();

        shell.stop();
    }

    public void killExternalProcessWithChildren() {

        checkCommandIsGiven();

        Process systemObject = ( Process ) shell.getSystemObject();
        systemObject.destroy();

        new ProcessUtils( systemObject, command ).killProcess();
        shell.stop();
    }

    private void checkCommandIsGiven() {

        if( shell == null ) {
            throw new ProcessTalkException( "You have not given the command to run" );
        }
    }

    @Override
    protected void finalize() throws Throwable {

        // if this instance is garbage collected - try to kill its children processes
        try {
            killExternalProcessWithChildren();
        } catch( Exception e ) {
            // remain silent
        }

        super.finalize();
    }
}
