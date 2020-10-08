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
import com.axway.ats.agent.core.action.CallerRelatedInfoRepository;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.process.ProcessExecutorException;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.process.model.IProcessExecutor;

import java.util.Map;

public class RemoteProcessExecutor implements IProcessExecutor {

    private String                    atsAgent;
    private String                    internalId;

    private String                    standardOutputFile;
    private String                    errorOutputFile;
    private String                    workDirectory;
    private boolean                   logStandardOutput;
    private boolean                   logErrorOutput;

    private InternalProcessOperations remoteProcessOperations;

    public RemoteProcessExecutor( String atsAgent,
                                  String command,
                                  String... commandArguments ) {

        this.atsAgent = atsAgent;
        this.remoteProcessOperations = new InternalProcessOperations(atsAgent);
        try {
            this.internalId = this.remoteProcessOperations.initProcessExecutor(command, commandArguments);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public void execute() {

        execute(true);
    }

    @Override
    public void execute(
                         boolean waitForCompletion ) {

        try {
            this.remoteProcessOperations.startProcess(internalId,
                                                      workDirectory,
                                                      standardOutputFile,
                                                      errorOutputFile,
                                                      logStandardOutput,
                                                      logErrorOutput,
                                                      waitForCompletion);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public void kill() {

        try {
            this.remoteProcessOperations.killProcess(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public void killAll() {

        try {
            this.remoteProcessOperations.killProcessAndItsChildren(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public int getProcessId() {

        try {
            return this.remoteProcessOperations.getProcessId(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public int getExitCode() {

        try {
            return this.remoteProcessOperations.getProcessExitCode(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getStandardOutput() {

        try {
            return this.remoteProcessOperations.getProcessStandardOutput(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getErrorOutput() {

        try {
            return this.remoteProcessOperations.getProcessErrorOutput(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public void setStandardOutputFile(
                                       String standardOutputFile ) {

        this.standardOutputFile = standardOutputFile;
    }

    @Override
    public void setErrorOutputFile(
                                    String errorOutputFile ) {

        this.errorOutputFile = errorOutputFile;
    }

    @Override
    public void setLogStandardOutput(
                                      boolean logStandardOutput ) {

        this.logStandardOutput = logStandardOutput;
    }

    @Override
    public void setLogErrorOutput(
                                   boolean logErrorOutput ) {

        this.logErrorOutput = logErrorOutput;
    }

    @Override
    public void setWorkDirectory(
                                  String workDirectory ) {

        this.workDirectory = workDirectory;
    }

    @Override
    public String setEnvVariable(String variableName, String variableValue ) {

        try {
            return this.remoteProcessOperations.setEnvVariable(internalId, variableName, variableValue);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override public String removeEnvVariable( String variableName ) {

        try {
            return this.remoteProcessOperations.removeEnvVariable(internalId, variableName);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public void appendToEnvVariable(
                                     String variableName,
                                     String variableValueToAppend ) {

        try {
            this.remoteProcessOperations.appendToEnvVariable(internalId, variableName, variableValueToAppend);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getEnvVariable(
                                  String variableName ) {

        try {
            return this.remoteProcessOperations.getEnvVariable(internalId, variableName);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public Map<String, String> getEnvVariables() {

        try {
            return this.remoteProcessOperations.getEnvVariables(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getCurrentStandardOutput() {

        try {
            return this.remoteProcessOperations.getProcessCurrentStandardOutput(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getCurrentErrorOutput() {

        try {
            return this.remoteProcessOperations.getProcessCurrentErrorOutput(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public boolean isStandardOutputFullyRead() {

        try {
            return this.remoteProcessOperations.isStandardOutputFullyRead(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public boolean isErrorOutputFullyRead() {

        try {
            return this.remoteProcessOperations.isErrorOutputFullyRead(internalId);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    /**
     * The Process Executor instance on a remote agent may keep lots of
     * output data.
     *  
     * Here, when this object is garbage collected, we ask the agent to
     * discard its related Process Executor instance.
     * 
     * Of course this does not guarantee the prevention of Out of memory errors on the agent, 
     * but it is still some form of unattended cleanup.
     */
    @Override
    protected void finalize() throws Throwable {

        TestcaseStateEventsDispacher.getInstance()
                                    .cleanupInternalObjectResources(atsAgent,
                                                                    CallerRelatedInfoRepository.KEY_PROCESS_EXECUTOR
                                                                              + internalId);

        super.finalize();
    }
}
