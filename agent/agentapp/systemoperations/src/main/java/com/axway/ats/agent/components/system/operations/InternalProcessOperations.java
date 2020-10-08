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
import com.axway.ats.core.process.LocalProcessExecutor;

import java.util.HashMap;
import java.util.Map;

public class InternalProcessOperations extends CallerRelatedAction {

    private static final String OBJECT_KEY_PREFIX = CallerRelatedInfoRepository.KEY_PROCESS_EXECUTOR;

    public InternalProcessOperations( String caller ) {

        super(caller);
    }

    @Action( name = "Internal Process Operations init Process Executor")
    public String initProcessExecutor(
                                       @Parameter( name = "command") String command,
                                       @Parameter( name = "commandArguments") String[] commandArguments ) {

        // Add a new instance into the repository and return its unique counter 
        // which will be used in next calls
        return dataRepo.addObject(OBJECT_KEY_PREFIX, new LocalProcessExecutor(caller,
                                                                              command,
                                                                              commandArguments));
    }

    @Action( name = "Internal Process Operations kill External Process")
    public int killExternalProcess(
                                    @Parameter( name = "startCommandSnippet") String startCommandSnippet ) {

        return LocalProcessExecutor.killProcess(startCommandSnippet);
    }

    /**
     *
     * @param internalProcessId internal process id (the unique id for the current LocalProcessExecutor)
     * @param workDirectory the working directory. If null, the working directory of the current Java process will be used
     * @param standardOutputFile the standard output file name. If null, the standard output will not be sent to a file
     * @param errorOutputFile the error output file name. If null, the error output will not be sent to a file
     * @param logStandardOutput whether to log the standard output
     * @param logErrorOutput whether to log the error output
     * @param waitForCompletion whether to wait for a process completion
     */
    @Action( name = "Internal Process Operations start Process")
    public void startProcess(
                              @Parameter( name = "internalProcessId") String internalProcessId,
                              @Parameter( name = "workDirectory") String workDirectory,
                              @Parameter( name = "standardOutputFile") String standardOutputFile,
                              @Parameter( name = "errorOutputFile") String errorOutputFile,
                              @Parameter( name = "logStandardOutput") boolean logStandardOutput,
                              @Parameter( name = "logErrorOutput") boolean logErrorOutput,
                              @Parameter( name = "waitForCompletion") boolean waitForCompletion ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);

        processExecutor.setWorkDirectory(workDirectory);
        processExecutor.setStandardOutputFile(standardOutputFile);
        processExecutor.setErrorOutputFile(errorOutputFile);
        processExecutor.setLogStandardOutput(logStandardOutput);
        processExecutor.setLogErrorOutput(logErrorOutput);

        processExecutor.execute(waitForCompletion);
    }

    @Action( name = "Internal Process Operations get Process Standard Output")
    public String getProcessStandardOutput(
                                            @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.getStandardOutput();
    }

    @Action( name = "Internal Process Operations get Process Error Output")
    public String getProcessErrorOutput(
                                         @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.getErrorOutput();
    }

    @Action( name = "Internal Process Operations get Process Current Standard Output")
    public String getProcessCurrentStandardOutput(
                                                   @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.getCurrentStandardOutput();
    }

    @Action( name = "Internal Process Operations get Process Current Error Output")
    public String getProcessCurrentErrorOutput(
                                                @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.getCurrentErrorOutput();
    }

    @Action( name = "Internal Process Operations get Process Exit Code")
    public int getProcessExitCode(
                                   @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.getExitCode();
    }

    @Action( name = "Internal Process Operations get Process Id")
    public int getProcessId(
                             @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.getProcessId();
    }

    @Action( name = "Internal Process Operations kill Process")
    public void killProcess(
                             @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        processExecutor.kill();
    }

    @Action( name = "Internal Process Operations kill Process And Its Children")
    public void killProcessAndItsChildren(
                                           @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        processExecutor.killAll();
    }

    @Action( name = "Internal Process Operations set Env Variable")
    public String setEnvVariable(
                                @Parameter( name = "internalProcessId") String internalProcessId,
                                @Parameter( name = "variableName") String variableName,
                                @Parameter( name = "variableValue") String variableValue ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.setEnvVariable(variableName, variableValue);
    }

    @Action( name = "Internal Process Operations remove Env Variable")
    public String removeEnvVariable(
            @Parameter( name = "internalProcessId") String internalProcessId,
            @Parameter( name = "variableName") String variableName) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.removeEnvVariable(variableName);
    }

    @Action( name = "Internal Process Operations append To Env Variable")
    public void appendToEnvVariable(
                                     @Parameter( name = "internalProcessId") String internalProcessId,
                                     @Parameter( name = "variableName") String variableName,
                                     @Parameter( name = "variableValueToAppend") String variableValueToAppend ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        processExecutor.appendToEnvVariable(variableName, variableValueToAppend);
    }

    @Action( name = "Internal Process Operations get Env Variable")
    public String getEnvVariable(
                                  @Parameter( name = "internalProcessId") String internalProcessId,
                                  @Parameter( name = "variableName") String variableName ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.getEnvVariable(variableName);
    }

    @Action( name = "Internal Process Operations get Env Variables")
    public Map<String,String> getEnvVariables(
            @Parameter( name = "internalProcessId") String internalProcessId) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        Map<String,String> mapInt = processExecutor.getEnvVariables(); // some internal impl, not serializable
        HashMap<String,String> hMap = new HashMap<>(mapInt); // clone and serializable
        return hMap;
    }

    @Action( name = "Internal Process Operations is Standard Output Fully Read")
    public boolean isStandardOutputFullyRead(
                                              @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.isStandardOutputFullyRead();
    }

    @Action( name = "Internal Process Operations is Error Output Fully Read")
    public boolean isErrorOutputFullyRead(
                                           @Parameter( name = "internalProcessId") String internalProcessId ) {

        LocalProcessExecutor processExecutor = (LocalProcessExecutor) dataRepo.getObject(OBJECT_KEY_PREFIX
                                                                                         + internalProcessId);
        return processExecutor.isErrorOutputFullyRead();
    }

}
