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
import com.axway.ats.common.process.ProcessExecutorException;
import com.axway.ats.core.process.model.IProcessExecutor;

public class RemoteProcessExecutor implements IProcessExecutor {

    private String                    atsAgent;

    private String                    standardOutputFile;
    private String                    errorOutputFile;
    private String                    workDirectory;
    private boolean                   logStandardOutput;
    private boolean                   logErrorOutput;

    private InternalProcessOperations remoteProcessOperations;

    public RemoteProcessExecutor( String atsAgent,
                                  String command,
                                  String... commandArguments ) throws AgentException {

        this.atsAgent = atsAgent;
        this.remoteProcessOperations = new InternalProcessOperations(this.atsAgent);
        try {
            this.remoteProcessOperations.initProcessExecutor(command, commandArguments);
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
            this.remoteProcessOperations.startProcess(
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
            this.remoteProcessOperations.killProcess();
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public void killAll() {

        try {
            this.remoteProcessOperations.killProcessAndItsChildren();
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public int getProcessId() {

        try {
            return this.remoteProcessOperations.getProcessId();
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public int getExitCode() {

        try {
            return this.remoteProcessOperations.getProcessExitCode();
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getStandardOutput() {

        try {
            return this.remoteProcessOperations.getProcessStandardOutput();
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getErrorOutput() {

        try {
            return this.remoteProcessOperations.getProcessErrorOutput();
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
    public void setEnvVariable(
                                String variableName,
                                String variableValue ) {

        try {
            this.remoteProcessOperations.setEnvVariable(variableName, variableValue);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public void appendToEnvVariable(
                                     String variableName,
                                     String variableValueToAppend ) {

        try {
            this.remoteProcessOperations.appendToEnvVariable(variableName, variableValueToAppend);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getEnvVariable(
                                  String variableName ) {

        try {
            return this.remoteProcessOperations.getEnvVariable(variableName);
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getCurrentStandardOutput() {

        try {
            return this.remoteProcessOperations.getProcessCurrentStandardOutput();
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public String getCurrentErrorOutput() {

        try {
            return this.remoteProcessOperations.getProcessCurrentErrorOutput();
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public boolean isStandardOutputFullyRead() {

        try {
            return this.remoteProcessOperations.isStandardOutputFullyRead();
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

    @Override
    public boolean isErrorOutputFullyRead() {

        try {
            return this.remoteProcessOperations.isErrorOutputFullyRead();
        } catch (AgentException e) {
            throw new ProcessExecutorException(e);
        }
    }

}
