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
package com.axway.ats.core.process.model;

import java.util.Map;
public interface IProcessExecutor {

    /**
     * Execute process and wait for its completion
     */
    public void execute();

    /**
     * Execute process and optionally wait for its completion
     * @param waitForCompletion true - wait to complete
     */
    public void execute(
                         boolean waitForCompletion );

    /**
     * Tries to stop started process. Started child processes are not affected.
     */
    public void kill();

    /**
     * Kill started process and makes best effort to kill its children processes.<br />
     * Warning is logged if issues is detected.<br />
     * Note that this might be invoked after some time if not sure that all child processes are already started.
     */
    public void killAll();

    /**
     * Tries to get the ID of the process started.
     * <em>Note:</em>Currently may work only on some UNIX variants with SUN/Oracle JDK.
     * @return The ID of the process. -1 in case of error.
     */
    public int getProcessId();

    /**
     * Returns exit code of the executed process. Sometimes like on some Linux
     * versions the process should be invoked after making sure that it has already completed.<br />
     * Otherwise an exception is thrown by the VM.
     * @return Exit code. Generally 0/zero means completed successfully.
     */
    public int getExitCode();

    /**
     * Returns standard output content
     *
     * @return standard output content
     * @throws Exception if it is not already open by the process. I.e. some sleep might be needed.
     */
    public String getStandardOutput();

    /**
     * Returns error content
     *
     * @return error content
     * @throws Exception if it is not already open by the process. I.e. some sleep might be needed.
     */
    public String getErrorOutput();

    /**
     * Returns standard output till the current moment
     *
     * @return standard output till the current moment
     * @throws Exception if it is not already open by the process. I.e. some sleep might be needed.
     */
    public String getCurrentStandardOutput();

    /**
     * Returns error output content till the current moment
     *
     * @return error output content till the current moment
     * @throws Exception if it is not already open by the process. I.e. some sleep might be needed.
     */
    public String getCurrentErrorOutput();

    /**
    *
    * @return whether the standard output is completely read or is still reading
    */
    public boolean isStandardOutputFullyRead();

    /**
     *
     * @return whether the error output is completely read or is still reading
     */
    public boolean isErrorOutputFullyRead();

    public void setStandardOutputFile(
                                       String standardOutputFile );

    public void setErrorOutputFile(
                                    String errorOutputFile );

    /**
     * Log output to corresponding log4j2 appenders
     * @param logStandardOutput
     */
    public void setLogStandardOutput(
                                      boolean logStandardOutput );

    /**
     * Log error to corresponding log4j2 appenders
     * @param logErrorOutput
     */
    public void setLogErrorOutput(
                                   boolean logErrorOutput );

    public void setWorkDirectory(
                                  String workDirectory );

    /**
     * Sets value to provided environment variable name
     * @param variableName the name of the environment variable. Upper case is preferred but naming depends on the underlying OS support
     * @param variableValue value of the environment variable
     * @return old value if variable already existed
     */
    public String setEnvVariable(
                                String variableName,
                                String variableValue );

    /**
     * Removes environment variable. Support depends on the JVM and OS used
     * @param variableName the name of the environment variable. Upper case is preferred but naming depends on the underlying OS support
     * @return old value if variable already existed
     */
    public String removeEnvVariable( String variableName );

    public void appendToEnvVariable(
                                     String variableName,
                                     String variableValueToAppend );

    public String getEnvVariable(
                                  String variableName );

    public Map<String,String> getEnvVariables();
}
