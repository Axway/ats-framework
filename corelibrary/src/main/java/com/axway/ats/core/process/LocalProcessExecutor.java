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
package com.axway.ats.core.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.axway.ats.common.process.ProcessExecutorException;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.process.model.IProcessExecutor;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

public class LocalProcessExecutor implements IProcessExecutor {

    private static final Logger log                       = Logger.getLogger(LocalProcessExecutor.class);

    private final static int    MAX_STRING_SIZE           = 100000;                                      // max chars used to limit process output

    private final static String SKIPPED_CHARACTERS        = "... skipped characters ..."
                                                            + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
    private final static int    SKIPPED_CHARACTERS_LENGTH = SKIPPED_CHARACTERS.length();

    private List<String>        commandTokens;
    private String              commandDescription;

    private ProcessOutputReader errorReaderThread;
    private ProcessOutputReader outputReaderThread;

    private String              standardOutputFile;
    private String              errorOutputFile;

    private boolean             logStandardOutput;
    private boolean             logErrorOutput;

    private String              workDirectory;

    private Process             theProcess;
    private ProcessBuilder      processBuilder;

    private boolean             suppressLogMessages;
    private boolean             doNotUseStandardInput;

    public LocalProcessExecutor( String command, String... commandArguments ) {

        this.commandDescription = command;

        this.commandTokens = new ArrayList<String>();
        if (commandArguments == null || commandArguments.length == 0) {

            this.commandTokens.addAll(Arrays.asList(StringUtils.parseCommandLineArguments(command)));
        } else {

            this.commandTokens.add(command);
            for (String commandArgument : commandArguments) {

                this.commandTokens.add(commandArgument);
                this.commandDescription = this.commandDescription + " " + commandArgument;
            }
        }

        this.processBuilder = new ProcessBuilder(this.commandTokens);
    }

    /**
     * Execute process and wait for its completion
     */
    public void execute() {

        execute(true);
    }

    /**
     * Execute process and optionally wait for its completion
     * @param waitForCompletion true - wait to complete
     */
    public void execute( boolean waitForCompletion ) {

        if (!suppressLogMessages) {
            log.info("Executing '" + commandDescription + "'. We will " + (waitForCompletion
                                                                                             ? "wait"
                                                                                             : "not wait")
                     + " for its completion");
        }

        try {
            if (workDirectory != null) {
                processBuilder.directory(new File(workDirectory));
            }
            this.theProcess = processBuilder.start();

            errorReaderThread = new ProcessOutputReader("ERROR OUTPUT", this.theProcess,
                                                        this.theProcess.getErrorStream(), logErrorOutput,
                                                        errorOutputFile);

            outputReaderThread = new ProcessOutputReader("STANDARD OUTPUT", this.theProcess,
                                                         this.theProcess.getInputStream(), logStandardOutput,
                                                         standardOutputFile);

            errorReaderThread.start();
            outputReaderThread.start();

            if (doNotUseStandardInput) {

                IoUtils.closeStream(theProcess.getOutputStream(), "Could not close process input stream");
            }

            if (waitForCompletion) {
                // wait until the external process finish
                theProcess.waitFor();
                if (!suppressLogMessages) {
                    log.info("The execution of '" + commandDescription + "' finished with exit code "
                             + this.theProcess.exitValue());
                }
            }
        } catch (Exception e) {
            String message = "Error executing '" + commandDescription + "': " + e.getMessage();
            log.error(message);
            throw new ProcessExecutorException(message, e);
        } finally {
            if (theProcess != null) { // close input stream for the process since otherwise PIPEs leak
                // this code should be repositioned if we add support for writing data to the process
                IoUtils.closeStream(theProcess.getOutputStream(), "Could not close process input stream");
            }
        }
    }

    /**
     * <pre>
     * Closes the process input stream (<b>STDIN</b> (standard input)) just after the process is started.
     * This method must be called <i>before</i> execute() method.
     *
     * Why do you need it?
     * Because, there are processes which wait infinitely on their standard input and if the STDIN is not closed, they hang.
     * In some cases one of "<b>&lt; /dev/null</b>" or "<b>&lt; NUL</b>" will do the same thing, but there are problems on Windows
     * </pre>
     */
    public void doNotUseStandardInput() {

        doNotUseStandardInput = true;
    }

    /**
     * Tries to stop started process. Started child processes are not affected.
     */
    public void kill() {

        new ProcessUtils(theProcess, commandDescription).killProcess();
    }

    /**
     * Kill started process and makes best effort to kill its children processes.<br />
     * Warning is logged if issues is detected.<br />
     * Note that this might be invoked after some time if not sure that all child processes are already started.
     */
    public void killAll() {

        new ProcessUtils(theProcess, commandDescription).killProcessAndItsChildren();
    }

    /**
     * Killing process by start command snippet
     *
     * @param startCommandSnippet start command snippet. The minimum allowed length is 2 characters
     * @return the number of killed processes
     */
    public static int killProcess( String startCommandSnippet ) {

        if (startCommandSnippet == null || startCommandSnippet.length() < 2) {

            throw new IllegalStateException("The process start command snippet is invalid. The minimum allowed length is 2 characters");
        }

        int numberOfKilled = 0;
        int startParsingLine = 0;
        LocalProcessExecutor pExecutor;
        if (OperatingSystemType.getCurrentOsType().isUnix()) {

            String command = "ps -Ao pid,args | grep '" + startCommandSnippet
                             + "' | grep -v grep && echo --- && ps -Ao pid,args | grep '"
                             + startCommandSnippet
                             + "' | grep -v grep | awk '{print $1}' | xargs kill -9 2>&1";
            if (OperatingSystemType.getCurrentOsType() == OperatingSystemType.HP_UX) {
                // Some commands in HP-UX, like 'ps', has different usage depending on which selected behavior: UNIX95,XPG2,XPG3,XPG4
                //      UNIX95 = Unix 95 behavior
                //      XPG4   = X/Open's Portability Guide Issue 4
                // The UNIX95 variable (when set) simply alters the way the 'ps' command functions. In our case enabling '-A' and '-o' options
                pExecutor = new LocalProcessExecutor("/bin/sh", "-c",
                                                     "export UNIX95=XPG4 && " + command);
            } else {
                pExecutor = new LocalProcessExecutor("/bin/sh", "-c", command);
            }
        } else if (OperatingSystemType.getCurrentOsType().isWindows()) {

            pExecutor = new LocalProcessExecutor("cmd",
                                                 "/c",
                                                 "wmic process where (commandline like \"%"
                                                       + startCommandSnippet
                                                       + "%\") get commandLine,processId <NUL && echo --- <NUL && wmic process where (commandline like \"%"
                                                       + startCommandSnippet + "%\") call terminate <NUL");
            startParsingLine = 1;
        } else {

            throw new IllegalStateException("Not supported operating system type. Report the case to ATS team");
        }

        pExecutor.setSuppressLogMessages(true);
        pExecutor.doNotUseStandardInput();
        pExecutor.execute(true);

        String output = pExecutor.getStandardOutput();

        String[] lines = output.split(AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        if (lines.length > startParsingLine) {

            for (int i = startParsingLine; i < lines.length && !"---".equals(lines[i].trim()); i++) {

                String line = lines[i].trim();
                if (line.isEmpty()
                    || (OperatingSystemType.getCurrentOsType().isWindows() && line.contains("wmic "))) {
                    continue;
                }
                numberOfKilled++;
                String pid = null;
                String startCommand = null;
                //NOTE:
                //  on Linux, if the 'args' option is not last 'ps -o' option, the start command is stripped and we can't 'grep' from the whole command
                //  on Windows, no matter of the columns order in 'wmic' command (for example "processId,commandLine") the output is always the same, the PID column is the last one
                if (OperatingSystemType.getCurrentOsType().isWindows()) {
                    int lastSpaceIndex = line.lastIndexOf(' ');
                    pid = line.substring(lastSpaceIndex).trim();
                    startCommand = line.substring(0, lastSpaceIndex);
                } else {
                    int firstSpaceIndex = line.indexOf(' ');
                    pid = line.substring(0, firstSpaceIndex).trim();
                    startCommand = line.substring(firstSpaceIndex).trim();
                }
                log.info("Killing process with PID " + pid + " and start command: " + startCommand);
            }
        }

        // try to remove the temp file that gets created from running wmic command
        if (OperatingSystemType.getCurrentOsType().isWindows()) {

            try {
                // look in the current working directory
                File f = new File("TempWmicBatchFile.bat");
                // sometimes the file appears after 5-20ms, that is why we will try for 1sec max
                int retries = 100;
                // the loop will brake when the temporary file exists and the delete operation is successful
                // or if the number of retries exceeded
                while (! (f.exists() && f.delete()) && retries-- > 0) {
                    Thread.sleep(10);
                }
            } catch (Exception e) {}
        }

        return numberOfKilled;
    }

    /**
     * Tries to get the ID of the process started.
     * <em>Note:</em>Currently may work only on some UNIX variants with SUN/Oracle JDK.
     * @return The ID of the process. -1 in case of error.
     */
    public int getProcessId() {

        return new ProcessUtils(theProcess, commandDescription).getProcessId();
    }

    /**
     * Returns exit code of the executed process. Sometimes like on some Linux
     * versions the process should be invoked after making sure that it has already completed.<br />
     * Otherwise an exception is thrown by the VM.
     * @return Exit code. Generally 0/zero means completed successfully.
     */
    public int getExitCode() {

        return this.theProcess.exitValue();
    }

    /**
     * Returns the full standard output content.</br></br>
     *
     * <b>Note:</b> This is a blocking operation which will:
     * <ul>
     *  <li>return the standard output <i>after</i> the full completion of the external process.
     *  <li>throw a {@link ProcessExecutorException} if the external process do not complete within 1 minute
     * </ul>
     *
     * @return standard output content
     * @throws Exception if it is not already open by the process. I.e. some sleep might be needed.
     */
    public String getStandardOutput() {

        return this.outputReaderThread.getContent();
    }

    /**
     * Returns the full error output content.</br></br>
     *
     * <b>Note:</b> This is a blocking operation which will:
     * <ul>
     *  <li>return the error output <i>after</i> the full completion of the external process.
     *  <li>throw a {@link ProcessExecutorException} if the external process do not complete within 1 minute
     * </ul>
     *
     * @return error content
     * @throws Exception if it is not already open by the process. I.e. some sleep might be needed.
     */
    public String getErrorOutput() {

        return this.errorReaderThread.getContent();
    }

    /**
     * Returns standard output till the current moment
     *
     * @return standard output till the current moment
     * @throws Exception if it is not already open by the process. I.e. some sleep might be needed.
     */
    public String getCurrentStandardOutput() {

        return this.outputReaderThread.getCurrentContent();
    }

    /**
     * Returns error output content till the current moment
     *
     * @return error output content till the current moment
     * @throws Exception if it is not already open by the process. I.e. some sleep might be needed.
     */
    public String getCurrentErrorOutput() {

        return this.errorReaderThread.getCurrentContent();
    }

    public void setStandardOutputFile( String standardOutputFile ) {

        this.standardOutputFile = standardOutputFile;
    }

    public void setErrorOutputFile( String errorOutputFile ) {

        this.errorOutputFile = errorOutputFile;
    }

    /**
     *
     * @return whether the standard output is completely read or is still reading
     */
    public boolean isStandardOutputFullyRead() {

        return this.outputReaderThread.isStreamFullyRead();
    }

    /**
     *
     * @return whether the error output is completely read or is still reading
     */
    public boolean isErrorOutputFullyRead() {

        return this.errorReaderThread.isStreamFullyRead();
    }

    /**
     * Log output to corresponding log4j appenders
     * @param logErrorOutput
     */
    public void setLogStandardOutput( boolean logStandardOutput ) {

        this.logStandardOutput = logStandardOutput;
    }

    /**
     * Log error to corresponding log4j appenders
     * @param logErrorOutput
     */
    public void setLogErrorOutput( boolean logErrorOutput ) {

        this.logErrorOutput = logErrorOutput;
    }

    public void setWorkDirectory( String workDirectory ) {

        this.workDirectory = workDirectory;
    }

    public void setEnvVariable( String variableName, String variableValue ) {

        this.processBuilder.environment().put(variableName, variableValue);
    }

    public void appendToEnvVariable( String variableName, String variableValueToAppend ) {

        Map<String, String> env = this.processBuilder.environment();
        if (env.containsKey(variableName)) {
            variableValueToAppend = env.get(variableName) + variableValueToAppend;
        }
        env.put(variableName, variableValueToAppend);
    }

    public String getEnvVariable( String variableName ) {

        return this.processBuilder.environment().get(variableName);
    }

    /**
     * Whether to suppress the log messages
     *
     * @param suppressLogMessages whether to suppress the log messages
     */
    private void setSuppressLogMessages( boolean suppressLogMessages ) {

        this.suppressLogMessages = suppressLogMessages;
    }

    class ProcessOutputReader extends Thread {

        private final Logger        log;

        private static final int    READ_TIMEOUT = 60 * 1000;                  // in milliseconds

        private final StringBuilder content      = new StringBuilder();

        private BufferedWriter      bufWriterStream;

        private boolean             logOutput;

        private InputStream         is;

        private String              type;

        private Process             externalProcess;

        private CountDownLatch      countdownLatchForExternalProcessCompletion;

        ProcessOutputReader( String type, Process externalProcess, InputStream is,
                             boolean logOutput, String outputFile ) {

            log = Logger.getLogger(ProcessOutputReader.class.getSimpleName() + " <" + type + ">");

            this.externalProcess = externalProcess;

            this.is = is;
            this.type = type;

            // will we send the output to the logging system
            this.logOutput = logOutput;

            // will we send the output to some file
            if (outputFile != null) {
                try {
                    this.bufWriterStream = new BufferedWriter(new FileWriter(outputFile));
                } catch (IOException ioe) {
                    String message = "Error connecting to local output file";
                    log.error(message, ioe);
                    throw new ProcessExecutorException(message, ioe);
                }
            }

            countdownLatchForExternalProcessCompletion = new CountDownLatch(1);
        }

        /**
         * @return whether the external process is over
         */
        private boolean isExternalProcessOver() {

            // we have to check whether the external process is over, we do this by asking about the exit code
            try {
                externalProcess.exitValue();
                // it is over
                return true;
            } catch (IllegalThreadStateException e) {
                // not a really elegant way to understand the external process is still running
                return false;
            }
        }

        @Override
        public void run() {

            BufferedReader bufReaderStream = null;
            try {
                String line = null;
                String dataToLeave = null;
                bufReaderStream = new BufferedReader(new InputStreamReader(is));

                while (true) {

                    // wait for data available in the stream we are attached to
                    // or exit if the external process is over
                    while (!bufReaderStream.ready()) {
                        // no bytes available to read

                        if (isExternalProcessOver()) {
                            // the external process is over, exit this thread
                            log.debug("External process is over, stop reading its stream for " + type); // STANDARD or ERROR OUTPUT
                            return;
                        } else {
                            /*
                             * We sleep here for some time before cycle again.
                             *      If sleep time is too short - too often we will get exceptions when asking for the exit code a few lines above.
                             *      If sleep time is too long - it may take a long after external process is over and the moment we exit this thread.
                             */
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ee) {
                                // continue with next iteration
                            }
                        }
                    }

                    // read next line from the stream we are attached to
                    // it cannot return null as we know there is data available
                    // This call potentially could block if there are chars available but CR/LF could not be there yet
                    line = bufReaderStream.readLine();

                    // append to internal content buffer
                    // limit buffer to about MAX_STRING_SIZE + current line length
                    if (this.content.length() > MAX_STRING_SIZE) {
                        dataToLeave = this.content.substring(this.content.length() - MAX_STRING_SIZE);
                        this.content.setLength(MAX_STRING_SIZE);
                        this.content.replace(0, SKIPPED_CHARACTERS_LENGTH, SKIPPED_CHARACTERS);
                        this.content.replace(SKIPPED_CHARACTERS_LENGTH,
                                             dataToLeave.length() + SKIPPED_CHARACTERS_LENGTH, dataToLeave);
                    }
                    this.content.append(line);
                    this.content.append(AtsSystemProperties.SYSTEM_LINE_SEPARATOR);

                    // send to the logging system
                    if (this.logOutput) {
                        log.debug(line);
                    }

                    // append to some file
                    if (this.bufWriterStream != null) {
                        this.bufWriterStream.write(line);
                        this.bufWriterStream.newLine();
                        this.bufWriterStream.flush();
                    }
                }
            } catch (IOException ioe) {
                log.error("Error working with the process output", ioe);
            } finally {
                // release the file handles
                IoUtils.closeStream(this.bufWriterStream);
                IoUtils.closeStream(bufReaderStream);

                countdownLatchForExternalProcessCompletion.countDown();
            }
        }

        boolean isStreamFullyRead() {

            return countdownLatchForExternalProcessCompletion.getCount() < 1;
        }

        /**
         * Called by the parent thread.
         *
         * Get the output right away, no blocking
         *
         * @return
         */
        String getCurrentContent() {

            return this.content.toString();
        }

        /**
         * Called by the parent thread.
         *
         * Get the whole output. If needed it blocks until the external process is over or throws an exception on timeout
         *
         * @return
         */
        String getContent() {

            long start = System.currentTimeMillis();
            long timeout = READ_TIMEOUT;
            boolean isExternalProcessOver = false;
            do {
                try {

                    isExternalProcessOver = countdownLatchForExternalProcessCompletion.await(timeout,
                                                                                             TimeUnit.MILLISECONDS);
                    timeout = 0; // this will stop another wait cycle
                } catch (InterruptedException e1) {
                    timeout = start + READ_TIMEOUT - System.currentTimeMillis();
                    if (timeout > 0) {
                        log.warn("Process output reader thread was interrupted while waiting for external process execution. We will wait again, now for "
                                 + timeout + " ms");
                    }
                }
            } while (timeout > 0);

            if (isExternalProcessOver) {
                // external process is over, give back the whole output
                return this.content.toString();
            } else {
                // the external process is not over yet, we hit timeout
                throw new ProcessExecutorException("The " + this.type + " was not fully read in "
                                                   + READ_TIMEOUT / 1000
                                                   + " seconds as the external process was not over yet.");
            }
        }
    }
}
