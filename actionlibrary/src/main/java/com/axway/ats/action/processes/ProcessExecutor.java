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
package com.axway.ats.action.processes;

import com.axway.ats.agent.components.system.operations.clients.InternalProcessOperations;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.process.ProcessExecutorException;
import com.axway.ats.core.process.LocalProcessExecutor;
import com.axway.ats.core.process.model.IProcessExecutor;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

/**
 * Used for running a system processes. <br/>
 * <b>Note</b> that this is not the same as simply running a shell command, as for example
 * the executed process will inherit the environment variables from the parent process.
 *
 * <br/>
 * <b>User guide</b>
 * <a href="https://axway.github.io/ats-framework/Running-external-process.html">page</a>
 * related to this class.
 */
@PublicAtsApi
public class ProcessExecutor {

    private IProcessExecutor processExecutor;
    private boolean          isProcessAlreadyStarted = false;

    /**
     * Constructor for running process on a remote host
     *
     * @param atsAgent the address of the remote ATS agent which will run the process
     * @param command the command to run
     *
     * <p>
     *    <b>Note:</b> If you want to specify port to IPv6 address, the supported format is: <i>[IP]:PORT</i>
     * </p>
     */
    @PublicAtsApi
    public ProcessExecutor( @Validate( name = "atsAgent", type = ValidationType.STRING_SERVER_WITH_PORT) String atsAgent,
                            @Validate( name = "command", type = ValidationType.STRING_NOT_EMPTY) String command ) {

        // validate input parameters
        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        new Validator().validateMethodParameters(new Object[]{ atsAgent, command });

        this.processExecutor = getOperationsImplementationFor(atsAgent, command);
    }

    /**
     * Constructor for running process on the local host
     *
     * @param command the command to run
     */
    @PublicAtsApi
    public ProcessExecutor( @Validate( name = "command", type = ValidationType.STRING_NOT_EMPTY) String command ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ command });

        this.processExecutor = getOperationsImplementationFor(null, command);
    }

    /**
     * Constructor for running process on a remote host
     *
     * @param atsAgent the address of the remote ATS agent which will run the process
     * @param command the command to run
     * @param commandArguments command arguments
     *
     * <p>
     *    <b>Note:</b> If you want to specify port to IPv6 address, the supported format is: <i>[IP]:PORT</i>
     * </p>
     */
    @PublicAtsApi
    public ProcessExecutor( @Validate( name = "atsAgent", type = ValidationType.STRING_SERVER_WITH_PORT) String atsAgent,
                            @Validate( name = "command", type = ValidationType.STRING_NOT_EMPTY) String command,
                            @Validate( name = "commandArguments", type = ValidationType.NONE) String[] commandArguments ) {

        // validate input parameters
        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        new Validator().validateMethodParameters(new Object[]{ atsAgent, command, commandArguments });

        this.processExecutor = getOperationsImplementationFor(atsAgent, command, commandArguments);
    }

    /**
     * Constructor for running process on the local host
     *
     * @param command the command to run
     * @param commandArguments command arguments
     */
    @PublicAtsApi
    public ProcessExecutor( @Validate( name = "command", type = ValidationType.STRING_NOT_EMPTY) String command,
                            @Validate( name = "commandArguments", type = ValidationType.NONE) String[] commandArguments ) {

        // validate input parameters
        new Validator().validateMethodParameters(new Object[]{ command, commandArguments });

        this.processExecutor = getOperationsImplementationFor(null, command, commandArguments);
    }

    /**
     * Execute process and wait for its completion
     */
    @PublicAtsApi
    public void execute() {

        execute(true);
    }

    /**
     * Execute process and optionally wait for its completion
     *
     * @param waitForCompletion true - wait to complete
     */
    @PublicAtsApi
    public void execute( boolean waitForCompletion ) {

        isProcessAlreadyStarted = true;
        this.processExecutor.execute(waitForCompletion);
    }

    /**
     * Tries to stop the started process. Eventually started child processes are not affected.
     */
    @PublicAtsApi
    public void kill() {

        this.processExecutor.kill();
    }

    /**
     * Kill the started process and makes best effort to kill its children processes as well.<br />
     * Warning is logged if issues is detected.<br />
     *
     * <b>Note</b> that sometimes it makes sense to give the original process some time to run prior to requesting its killing,
     * as it may take time until some of the child processes are actually started.
     */
    @PublicAtsApi
    public void killAll() {

        this.processExecutor.killAll();
    }

    /**
     * Killing external process(such not started by us) on the local host. <br/>
     * The process is found by a token which is contained in the start command.
     * No regex supported.
     *
     * @param startCommandSnippet the token to search for in the process start command.
     * The minimum allowed length is 2 characters
     * @return the number of killed processes
     */
    @PublicAtsApi
    public static int killExternalProcess( String startCommandSnippet ) {

        try {
            return LocalProcessExecutor.killProcess(startCommandSnippet);
        } catch (Exception e) {
            throw new ProcessExecutorException(e);
        }
    }

    /**
     * Killing external process(such not started by us) on a remote host. <br/>
     * The process is found by a token which is contained in the start command.
     * No regex supported.
     *
     * @param atsAgent the address of the remote ATS agent which will run the kill command
     * @param startCommandSnippet the token to search for in the process start command.
     * The minimum allowed length is 2 characters
     * @return the number of killed processes
     */
    @PublicAtsApi
    public static int
            killExternalProcess( @Validate( name = "atsAgent", type = ValidationType.STRING_SERVER_WITH_PORT) String atsAgent,
                                 @Validate( name = "startCommandSnippet", type = ValidationType.STRING_NOT_EMPTY) String startCommandSnippet ) {

        // validate input parameters
        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        new Validator().validateMethodParameters(new Object[]{ atsAgent, startCommandSnippet });

        try {
            return new InternalProcessOperations(atsAgent).killExternalProcess(startCommandSnippet);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    /**
     * Tries to get the ID of the process started.<br/>
     * <b>Note</b>: Currently works only on some UNIX variants with SUN/Oracle JDK.
     *
     * @return The ID of the process. -1 in case of error.
     */
    @PublicAtsApi
    public int getProcessId() {

        checkIfProcessIsStarted();
        return this.processExecutor.getProcessId();
    }

    /**
     * Returns the exit code of the executed process.<br/>
     * Sometimes like on some Linux versions the process should be invoked after making sure that it
     * has already completed. Otherwise an exception is thrown by the VM.
     *
     * @return Exit code. Generally 0/zero means completed successfully.
     */
    @PublicAtsApi
    public int getExitCode() {

        checkIfProcessIsStarted();
        return this.processExecutor.getExitCode();
    }

    /**
     * Returns the standard output
     *
     * @return standard output
     */
    @PublicAtsApi
    public String getStandardOutput() {

        checkIfProcessIsStarted();
        return this.processExecutor.getStandardOutput();
    }

    /**
     * Returns the error output
     *
     * @return error output
     */
    @PublicAtsApi
    public String getErrorOutput() {

        checkIfProcessIsStarted();
        return this.processExecutor.getErrorOutput();
    }

    /**
     * Returns the standard output till the current moment
     *
     * @return the standard output
     */
    @PublicAtsApi
    public String getCurrentStandardOutput() {

        checkIfProcessIsStarted();
        return this.processExecutor.getCurrentStandardOutput();
    }

    /**
     * Returns the error output till the current moment
     *
     * @return the error output
     */
    @PublicAtsApi
    public String getCurrentErrorOutput() {

        checkIfProcessIsStarted();
        return this.processExecutor.getCurrentErrorOutput();
    }

    /**
     *
     * @return whether the standard output is completely read or is still reading
     */
    @PublicAtsApi
    public boolean isStandardOutputFullyRead() {

        return this.processExecutor.isStandardOutputFullyRead();
    }

    /**
     *
     * @return whether the error output is completely read or is still reading
     */
    @PublicAtsApi
    public boolean isErrorOutputFullyRead() {

        return this.processExecutor.isErrorOutputFullyRead();
    }

    /**
     * Route the standard output into some local file
     *
     * @param standardOutputFile the output file
     */
    @PublicAtsApi
    public void setStandardOutputFile( String standardOutputFile ) {

        this.processExecutor.setStandardOutputFile(standardOutputFile);
    }

    /**
     * Route the error output into some local file
     *
     * @param errorOutputFile the output file
     */
    @PublicAtsApi
    public void setErrorOutputFile( String errorOutputFile ) {

        this.processExecutor.setErrorOutputFile(errorOutputFile);
    }

    /**
     * Route the standard output to the log4j system
     *
     * @param logErrorOutput
     */
    @PublicAtsApi
    public void setLogStandardOutput( boolean logStandardOutput ) {

        this.processExecutor.setLogStandardOutput(logStandardOutput);
    }

    /**
     * Route the error output to the log4j system
     *
     * @param logErrorOutput
     */
    @PublicAtsApi
    public void setLogErrorOutput( boolean logErrorOutput ) {

        this.processExecutor.setLogErrorOutput(logErrorOutput);
    }

    /**
     * Set work directory for the process to run
     *
     * @param workDirectory the work directory
     */
    @PublicAtsApi
    public void setWorkDirectory( String workDirectory ) {

        this.processExecutor.setWorkDirectory(workDirectory);
    }

    /**
     * Set environment variable prior to running the process
     *
     * @param variableName name of the environment variable
     * @param variableValue new value of the environment variable
     */
    @PublicAtsApi
    public void setEnvVariable( String variableName, String variableValue ) {

        this.processExecutor.setEnvVariable(variableName, variableValue);
    }

    /**
     * Append some value to an existing environment variable.
     * If the variable is not present - it will be created. <br/>
     * <b>Note</b>: it is caller's responsibility to use appropriate delimiter when concatenating values.
     *
     * @param variableName name of the environment variable
     * @param variableValueToAppend the value to append
     */
    @PublicAtsApi
    public void appendToEnvVariable( String variableName, String variableValueToAppend ) {

        this.processExecutor.appendToEnvVariable(variableName, variableValueToAppend);
    }

    /**
     * Get the value of environment variable.
     * Will return null in case there is no such variable
     *
     * @param variableName name of the environment variable
     * @return the value of the environment variable
     */
    @PublicAtsApi
    public String getEnvVariable( String variableName ) {

        checkIfProcessIsStarted();
        return this.processExecutor.getEnvVariable(variableName);
    }

    private IProcessExecutor getOperationsImplementationFor( String atsAgent, String command,
                                                             String... commandArgs ) {

        if (HostUtils.isLocalAtsAgent(atsAgent)) {
            return new LocalProcessExecutor(command, commandArgs);
        } else {
            try {
                return new RemoteProcessExecutor(atsAgent, command, commandArgs);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create remote process executor impl object", e);
            }
            
        }
    }

    private void checkIfProcessIsStarted() throws ProcessExecutorException {

        if (!isProcessAlreadyStarted)
            throw new ProcessExecutorException("You first need to start the process with execute(boolean) method!");
    }
}
