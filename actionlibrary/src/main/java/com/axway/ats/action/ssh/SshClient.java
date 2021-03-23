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
package com.axway.ats.action.ssh;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.ssh.JschSshClient;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * An SSH client class
 *
 * <br><br>
 * <b>User guide</b>
 * <a href="https://axway.github.io/ats-framework/SSH-Operations.html">page</a>
 * related to this class
 */
@PublicAtsApi
public class SshClient implements Closeable {

    private JschSshClient       sshClient;

    // connection parameters
    private String              host;
    private String              user;
    private String              password;
    private int                 port;
    private boolean             ptyEnabled = false;
    private Map<String, String> sshClientConfigurationProperties;

    /**
     * Construct SSH client. It will work on the default port 22
     *
     * @param host the target host
     * @param user the user name
     * @param password the user password
     */
    public SshClient( String host,
                      String user,
                      String password ) {

        this(host, user, password, 22);
    }

    /**
     * Construct SSH client and specify the port to use
     *
     * @param host the target host
     * @param user the user name
     * @param password the user password
     * @param port the specific port to use
     */
    public SshClient( String host,
                      String user,
                      String password,
                      int port ) {

        this.host = host;
        this.user = user;
        this.password = password;
        this.port = port;

        this.sshClientConfigurationProperties = new HashMap<>();
    }

    /**
     * Set configuration property <p/> Currently we use internally JCraft's JSch library which can be configured through
     * this method. <p/> You need to find the acceptable key-value configuration pairs in the JSch documentation. They
     * might be also available in the source code of com.jcraft.jsch.JSch <p/> <p> Example: The default value of
     * "PreferredAuthentications" is "gssapi-with-mic,publickey,keyboard-interactive,password" </p> ATS uses two types
     * of properties to configure the ssh client: <br> <ul> <li>global - equivalent to
     * {@link JSch#setConfig(String, String)}, example <strong>global.RequestTTY</strong>=true</li> <li>session -
     * equivalent to {@link Session#setConfig(String, String)}, example
     * <strong>session.StrictHostKeyChecking</strong>=no <br> Note that if there is no global. or session. prefix, the
     * property is assumed to be a session one</li> </ul> <p/>
     *
     * @param key configuration key
     * @param value configuration value
     */
    public void setSshClientConfigurationProperty( String key, String value ) {

        sshClientConfigurationProperties.put(key, value);
    }

    /**
     * Disconnect the SSH session connection
     */
    @PublicAtsApi
    @Override
    public void close() {

        sshClient.disconnect();
    }

    /**
     * Starts and a command and waits for its completion
     *
     * @param command SSH command to execute
     * @return the exit code
     */
    @PublicAtsApi
    public void execute(
            String command ) {

        sshClient = createNewSshClient();

        sshClient.connect(user, password, host, port);

        sshClient.execute(command, true);
    }

    public void setPtyEnabled( boolean ptyEnabled ) {

        this.ptyEnabled = ptyEnabled;
    }

    /**
     * Returns standard output content
     *
     * @return standard output content
     */
    @PublicAtsApi
    public String getStandardOutput() {

        return sshClient.getStandardOutput();
    }

    /**
     * Returns error output content
     *
     * @return error output content
     */
    @PublicAtsApi
    public String getErrorOutput() {

        return sshClient.getErrorOutput();
    }

    /**
     * @return the exit code of the executed command
     */
    @PublicAtsApi
    public int getCommandExitCode() {

        return sshClient.getCommandExitCode();
    }

    /*
     * If user fail to disconnect, we try when the GC collects this instance
     * (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {

        if (sshClient != null) {
            sshClient.disconnect();
        }
    }

    private JschSshClient createNewSshClient() {

        JschSshClient sshClient = new JschSshClient();
        sshClient.setPtyEnabled(ptyEnabled);
        for (Entry<String, String> entry : sshClientConfigurationProperties.entrySet()) {
            sshClient.setConfigurationProperty(entry.getKey(), entry.getValue());
        }

        return sshClient;
    }
}
