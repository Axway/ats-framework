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
import com.axway.ats.core.process.LocalProcessExecutor;

public class InternalProcessOperations {

    private LocalProcessExecutor processExecutor;

    public InternalProcessOperations() {

    }

    @Action(
            name = "Internal Process Operations init Process Executor")
    @ActionRequestInfo(
            requestUrl = "processes/executors",
            requestMethod = "PUT")
    public void initProcessExecutor(
                                     @Parameter(
                                             name = "command") String command,
                                     @Parameter(
                                             name = "commandArguments") String[] commandArguments ) {

        processExecutor = new LocalProcessExecutor(command, commandArguments);
    }

    @Action(
            name = "Internal Process Operations kill External Process")
    @ActionRequestInfo(
            requestUrl = "processes/executors/kill/external",
            requestMethod = "POST")
    public int killExternalProcess(
                                    @Parameter(
                                            name = "startCommandSnippet") String startCommandSnippet ) {

        return LocalProcessExecutor.killProcess(startCommandSnippet);
    }

    /**
     *
     * @param workDirectory the working directory. If null, the working directory of the current Java process will be used
     * @param standardOutputFile the standard output file name. If null, the standard output will not be sent to a file
     * @param errorOutputFile the error output file name. If null, the error output will not be sent to a file
     * @param logStandardOutput whether to log the standard output
     * @param logErrorOutput whether to log the error output
     * @param waitForCompletion whether to wait for a process completion
     */
    @Action(
            name = "Internal Process Operations start Process")
    @ActionRequestInfo(
            requestUrl = "processes/executors/start",
            requestMethod = "POST")
    public void startProcess(
                              @Parameter(
                                      name = "workDirectory") String workDirectory,
                              @Parameter(
                                      name = "standardOutputFile") String standardOutputFile,
                              @Parameter(
                                      name = "errorOutputFile") String errorOutputFile,
                              @Parameter(
                                      name = "logStandardOutput") boolean logStandardOutput,
                              @Parameter(
                                      name = "logErrorOutput") boolean logErrorOutput,
                              @Parameter(
                                      name = "waitForCompletion") boolean waitForCompletion ) {

        processExecutor.setWorkDirectory(workDirectory);
        processExecutor.setStandardOutputFile(standardOutputFile);
        processExecutor.setErrorOutputFile(errorOutputFile);
        processExecutor.setLogStandardOutput(logStandardOutput);
        processExecutor.setLogErrorOutput(logErrorOutput);

        processExecutor.execute(waitForCompletion);
    }

    @Action(
            name = "Internal Process Operations get Process Standard Output")
    @ActionRequestInfo(
            requestUrl = "processes/executors/stdout",
            requestMethod = "GET")
    public String getProcessStandardOutput() {

        return processExecutor.getStandardOutput();
    }

    @Action(
            name = "Internal Process Operations get Process Error Output")
    @ActionRequestInfo(
            requestUrl = "processes/executors/stderr",
            requestMethod = "GET")
    public String getProcessErrorOutput() {

        return processExecutor.getErrorOutput();
    }

    @Action(
            name = "Internal Process Operations get Process Current Standard Output")
    @ActionRequestInfo(
            requestUrl = "processes/executors/stdout/current",
            requestMethod = "GET")
    public String getProcessCurrentStandardOutput() {

        return processExecutor.getCurrentStandardOutput();
    }

    @Action(
            name = "Internal Process Operations get Process Current Error Output")
    @ActionRequestInfo(
            requestUrl = "processes/executors/stderr/current",
            requestMethod = "GET")
    public String getProcessCurrentErrorOutput() {

        return processExecutor.getCurrentErrorOutput();
    }

    @Action(
            name = "Internal Process Operations get Process Exit Code")
    @ActionRequestInfo(
            requestUrl = "processes/executors/exitCode",
            requestMethod = "GET")
    public int getProcessExitCode() {

        return processExecutor.getExitCode();
    }

    @Action(
            name = "Internal Process Operations get Process Id")
    @ActionRequestInfo(
            requestUrl = "processes/executors/pid",
            requestMethod = "GET")
    public int getProcessId() {

        return processExecutor.getProcessId();
    }

    @Action(
            name = "Internal Process Operations kill Process")
    @ActionRequestInfo(
            requestUrl = "processes/executors/kill",
            requestMethod = "POST")
    public void killProcess() {

        processExecutor.kill();
    }

    @Action(
            name = "Internal Process Operations kill Process And Its Children")
    @ActionRequestInfo(
            requestUrl = "processes/executors/kill/all",
            requestMethod = "POST")
    public void killProcessAndItsChildren() {

        processExecutor.killAll();
    }

    @Action(
            name = "Internal Process Operations set Env Variable")
    @ActionRequestInfo(
            requestUrl = "processes/executors/envvars",
            requestMethod = "PUT")
    public void setEnvVariable(

                                @Parameter(
                                        name = "variableName") String variableName,
                                @Parameter(
                                        name = "variableValue") String variableValue ) {

        processExecutor.setEnvVariable(variableName, variableValue);
    }

    @Action(
            name = "Internal Process Operations append To Env Variable")
    @ActionRequestInfo(
            requestUrl = "processes/executors/envvars",
            requestMethod = "POST")
    public void appendToEnvVariable(

                                     @Parameter(
                                             name = "variableName") String variableName,
                                     @Parameter(
                                             name = "variableValueToAppend") String variableValueToAppend ) {

        processExecutor.appendToEnvVariable(variableName, variableValueToAppend);
    }

    @Action(
            name = "Internal Process Operations get Env Variable")
    @ActionRequestInfo(
            requestUrl = "processes/executors/envvars",
            requestMethod = "GET")
    public String getEnvVariable(

                                  @Parameter(
                                          name = "variableName") String variableName ) {

        return processExecutor.getEnvVariable(variableName);
    }

    @Action(
            name = "Internal Process Operations is Standard Output Fully Read")
    @ActionRequestInfo(
            requestUrl = "processes/executors/stdout/fullyread",
            requestMethod = "GET")
    public boolean isStandardOutputFullyRead() {

        return processExecutor.isStandardOutputFullyRead();
    }

    @Action(
            name = "Internal Process Operations is Error Output Fully Read")
    @ActionRequestInfo(
            requestUrl = "processes/executors/stderr/fullyread",
            requestMethod = "GET")
    public boolean isErrorOutputFullyRead() {

        return processExecutor.isErrorOutputFullyRead();
    }

}
