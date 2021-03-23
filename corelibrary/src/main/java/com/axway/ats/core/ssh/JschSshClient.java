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
package com.axway.ats.core.ssh;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.ssh.exceptions.JschSshClientException;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class JschSshClient {

    private static final int    CONNECTION_TIMEOUT = 20000;

    private String              user;
    private String              host;
    private int                 port               = -1;

    private String              command;
    private boolean             ptyEnabled = true;

    private StreamReader        stdoutThread;
    private StreamReader        stderrThread;

    private Session             session;
    private ChannelExec         execChannel;

    // some optional configuration properties
    private Map<String, String> configurationProperties;

    public JschSshClient() {

        this.configurationProperties = new HashMap<>();

        // by default - skip checking of known hosts and verifying RSA keys
        this.configurationProperties.put("StrictHostKeyChecking", "no");
    }

    /**
     *
     * @param user the user name
     * @param password the user password
     * @param host the target host
     */
    public void connect( String user, String password, String host ) {

        connect(user, password, host, -1);
    }

    /**
     *
     * @param user the user name
     * @param password the user password
     * @param host the target host
     * @param port the specific port to use
     */
    public void connect( String user, String password, String host, int port ) {

        connect(user, password, host, port, null, null);
    }

    /**
     *
     * @param user the user name
     * @param password the user password
     * @param host the target host
     * @param port the specific port to use
     * @param privateKey private key location. For example: ~/.ssh/id_rsa
     * @param privateKeyPassword private key passphrase (or null if it hasn't)
     */
    public void connect( String user, String password, String host, int port, String privateKey,
                         String privateKeyPassword ) {

        try {
            // disconnect if needed or stay connected if the host is the same
            if (session != null && session.isConnected()) {

                if (this.host.equals(host) && this.user.equals(user) && this.port == port) {

                    return; // already connected
                } else {
                    disconnect();
                }
            }

            this.user = user;
            this.host = host;
            this.port = port;

            JSch jsch = new JSch();
            jsch.setConfigRepository(new JschConfigRepository(this.host, this.user, this.port,
                                                              this.configurationProperties));
            // apply global configuration properties
            for (Entry<String, String> entry : configurationProperties.entrySet()) {
                if (entry.getKey().startsWith("global.")) {
                    JSch.setConfig(entry.getKey().split("\\.")[1], entry.getValue());
                }
            }
            if (privateKey != null) {
                jsch.addIdentity(privateKey, privateKeyPassword);
            }
            if (port > 0) {
                session = jsch.getSession(user, host, port);
            } else {
                session = jsch.getSession(user, host);
            }
            session.setPassword(password);

            // apply session configuration properties
            for (Entry<String, String> entry : configurationProperties.entrySet()) {
                if (entry.getKey().startsWith("session.")) {
                    session.setConfig(entry.getKey().split("\\.")[1], entry.getValue());
                } else if (!entry.getKey().startsWith("global.")) { // by default if global or session prefix is
                    // missing, we assume it is a session property
                    session.setConfig(entry.getKey(), entry.getValue());
                }
            }

            session.connect(CONNECTION_TIMEOUT);
        } catch (Exception e) {

            throw new JschSshClientException(
                    e.getMessage() + "; Connection parameters are: user '" + user + "' at "
                    + host + " on port " + port,
                    e);
        }
    }

    /**
     * Disconnect the SSH session connection
     */
    public void disconnect() {

        if (stdoutThread != null && stdoutThread.isAlive()) {
            stdoutThread.interrupt();
        }
        if (stderrThread != null && stderrThread.isAlive()) {
            stderrThread.interrupt();
        }
        if (execChannel != null && execChannel.isConnected()) {
            execChannel.disconnect();
        }

        if (session != null) {
            session.disconnect();
        }
    }

    /**
     * Pass configuration property for the internally used SSH client library
     */
    public void setConfigurationProperty( String key, String value ) {

        configurationProperties.put(key, value);
    }

    /**
     *
     * @param command SSH command to execute
     * @return the exit code
     */
    public int execute( String command, boolean waitForCompletion ) {

        try {
            this.command = command;

            execChannel = (ChannelExec) session.openChannel("exec");

            execChannel.setCommand(command);
            execChannel.setInputStream(null);
            execChannel.setPty(
                    ptyEnabled); // Allocate a Pseudo-Terminal. Thus it supports login sessions. (eg. /bin/bash
            // -l)

            execChannel.connect(); // there is a bug in the other method channel.connect( TIMEOUT );

            stdoutThread = new StreamReader(execChannel.getInputStream(), execChannel, "STDOUT");
            stderrThread = new StreamReader(execChannel.getErrStream(), execChannel, "STDERR");
            stdoutThread.start();
            stderrThread.start();

            if (waitForCompletion) {

                stdoutThread.getContent();
                stderrThread.getContent();
                return execChannel.getExitStatus();
            }

        } catch (Exception e) {

            throw new JschSshClientException(e.getMessage(), e);
        } finally {

            if (waitForCompletion && execChannel != null) {
                execChannel.disconnect();
            }
        }

        return -1;
    }

    public void setPtyEnabled( boolean ptyEnabled ) {

        this.ptyEnabled = ptyEnabled;
    }

    /**
     * Returns standard output content
     *
     * @return standard output content
     */
    public String getStandardOutput() {

        return this.stdoutThread.getContent();
    }

    /**
     * Returns error content
     *
     * @return error content
     */
    public String getErrorOutput() {

        return this.stderrThread.getContent();
    }

    /**
     * Returns standard output till the current moment
     *
     * @return standard output till the current moment
     */
    public String getCurrentStandardOutput() {

        return this.stdoutThread.getCurrentContent();
    }

    /**
     * Returns error output content till the current moment
     *
     * @return error output content till the current moment
     */
    public String getCurrentErrorOutput() {

        return this.stderrThread.getCurrentContent();
    }

    /**
     *
     * @return whether the standard output is completely read or is still reading
     */
    public boolean isStandardOutputFullyRead() {

        return this.stdoutThread.isStreamFullyRead();
    }

    /**
     *
     * @return whether the error output is completely read or is still reading
     */
    public boolean isErrorOutputFullyRead() {

        return this.stderrThread.isStreamFullyRead();
    }

    /**
     *
     * @return the exit code of the executed command
     */
    public int getCommandExitCode() {

        if (execChannel != null && isStandardOutputFullyRead() && isErrorOutputFullyRead()) {
            return execChannel.getExitStatus();
        }
        return -1;
    }

    /**
     *
     * @return the result from the last executed command
     */
    public String getLastCommandExecutionResult() {

        StringBuilder sb = new StringBuilder();
        sb.append("Running command '");
        sb.append(command);
        sb.append("' returned exit code ");
        sb.append(getCommandExitCode());

        if (!StringUtils.isNullOrEmpty(getStandardOutput())) {
            sb.append("\nSTDOUT:\n" + getStandardOutput());
        }
        if (!StringUtils.isNullOrEmpty(getErrorOutput())) {
            sb.append("\nSTDERR:\n" + getErrorOutput());
        }

        return sb.toString();
    }

    class StreamReader extends Thread {

        private static final int    READ_BUFFER_SIZE          = 1024;
        private static final int    READ_TIMEOUT              =
                60 * 1000;                                  // in milliseconds
        private static final int    MAX_STRING_SIZE           = 100000;                                     // max chars used to limit process output
        private final        Logger log;
        private final        String SKIPPED_CHARACTERS        = "... skipped characters ..."
                                                                + AtsSystemProperties.SYSTEM_LINE_SEPARATOR;
        private final        int    SKIPPED_CHARACTERS_LENGTH = SKIPPED_CHARACTERS.length();

        private StringBuilder streamContent = new StringBuilder();
        private boolean       readFinished  = false;
        private String        type;
        private InputStream   is;
        private Channel       channel;

        StreamReader( InputStream is, Channel channel, String type ) {

            log = LogManager.getLogger(StreamReader.class.getSimpleName() + " <" + type + ">");

            this.is = is;
            this.type = type;
            this.channel = channel;
        }

        @Override
        public void run() {

            synchronized (this.streamContent) {

                try {

                    String dataToLeave = null;
                    byte[] tmp = new byte[READ_BUFFER_SIZE];
                    while (true) {

                        while (this.is.available() > 0) {

                            int i = this.is.read(tmp, 0, READ_BUFFER_SIZE);
                            if (this.streamContent.length() > MAX_STRING_SIZE) {
                                dataToLeave = this.streamContent
                                        .substring(this.streamContent.length()
                                                   - MAX_STRING_SIZE);
                                this.streamContent.setLength(MAX_STRING_SIZE);
                                this.streamContent.replace(0, SKIPPED_CHARACTERS_LENGTH, SKIPPED_CHARACTERS);
                                this.streamContent.replace(SKIPPED_CHARACTERS_LENGTH,
                                                           dataToLeave.length() + SKIPPED_CHARACTERS_LENGTH,
                                                           dataToLeave);
                            }
                            this.streamContent.append(new String(tmp, 0, i));
                        }
                        if (channel.isClosed() && this.is.available() == 0) {

                            break;
                        }
                        sleep(200);
                    }

                } catch (InterruptedException e) {
                    // ignore it
                } catch (Exception e) {
                    log.error("Error working with the SSL command output", e);
                } finally {

                    IoUtils.closeStream(this.is);
                    this.readFinished = true;
                    this.streamContent.notify();
                }
            }
        }

        boolean isStreamFullyRead() {

            return this.readFinished;
        }

        String getCurrentContent() {

            return this.streamContent.toString();
        }

        String getContent() {

            synchronized (this.streamContent) {

                try {
                    if (!this.readFinished) {
                        this.streamContent.wait(READ_TIMEOUT);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("The " + this.type + " was not read successfully", e);
                }
                if (!this.readFinished) {
                    throw new RuntimeException(
                            "The " + this.type + " was not read in " + READ_TIMEOUT / 1000
                            + " seconds");
                }
                return this.streamContent.toString();
            }
        }
    }
}
