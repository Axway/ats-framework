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
package com.axway.ats.action.smtp;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;

/**
 * SMTP connection manager
 * <strong>Note:</strong>Currently methods are not thread safe.
 */
@PublicAtsApi
public class SmtpManager {

    private static final Logger          log = LogManager.getLogger(SmtpManager.class);

    private Map<Integer, SmtpConnection> connetionPool;
    private int                          connectionId;

    @PublicAtsApi
    public SmtpManager() {

        connetionPool = new HashMap<Integer, SmtpConnection>();
    }

    //--------------------------------------------------------------------------
    /**
     * Creates a new connection to the specified host, using the specified port.
     *
     * @param host      The server hostname
     * @param port      The port to connect to
     * @return          The ID of the newly created connection
     */
    @PublicAtsApi
    public int openConnection(
                               String host,
                               int port ) {

        return openConnections(host, port, 1);
    }

    //--------------------------------------------------------------------------
    /**
     * Creates a new connection to the specified host, using the specified port.
     *
     * @param host      The server hostname
     * @param port      The port to connect to
     * @param bindAddress the local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     * @return          The ID of the newly created connection
     */
    @PublicAtsApi
    public int openConnection(
                               String host,
                               int port,
                               String bindAddress ) {

        return openConnections(host, port, 1, bindAddress);
    }

    //--------------------------------------------------------------------------
    /**
     * Creates a new connection to the specified host, using the specified port.
     *
     * @param host      The server hostname
     * @param port      The port to connect to
     * @param bindAddress the local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     * @param connectionTimeout     The connection timeout in milliseconds
     * @param timeout   The I/O timeout in milliseconds
     * @return          The ID of the newly created connection
     */
    @PublicAtsApi
    public int openConnection(
                               String host,
                               int port,
                               String bindAddress,
                               int connectionTimeout,
                               int timeout ) {

        return openConnections(host, port, 1, bindAddress, connectionTimeout, timeout);
    }

    //--------------------------------------------------------------------------
    /**
     * Creates the specified number of connections to the specified host, using the specified port.
     *
     * @param host        The server hostname
     * @param port        The port to connect to
     * @param numberConnections        The count of connections to open
     * @return            The ID of the last newly created connection
     */
    @PublicAtsApi
    public int openConnections(
                                String host,
                                int port,
                                int numberConnections ) {

        return openConnections(host, port, numberConnections, null);
    }

    //--------------------------------------------------------------------------
    /**
     * Creates the specified number of connections to the specified host, using the specified port.
     *
     * @param host      The server hostname
     * @param port      The port to connect to
     * @param numberConnections     The count of connections to open
     * @param bindAddress the local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     * @return          The ID of the last newly created connection
     */
    @PublicAtsApi
    public int openConnections(
                                String host,
                                int port,
                                int numberConnections,
                                String bindAddress ) {

        return openConnections(host, port, numberConnections, bindAddress, 0, 0);
    }

    /**
     * Creates the specified number of connections to the specified host, using the specified port.
     * <strong>Note:</strong>This method is not thread safe.
     *
     * @param host      The server hostname
     * @param port      The port to connect to
     * @param numberConnections     The count of connections to open
     * @param bindAddress           The local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     * @param connectionTimeout     The connection timeout in milliseconds
     * @param timeout   The I/O timeout in milliseconds
     * @return          The ID of the last newly created connection
     */
    @PublicAtsApi
    public int openConnections(
                                String host,
                                int port,
                                int numberConnections,
                                String bindAddress,
                                int connectionTimeout,
                                int timeout ) {

        try {
            //create the specified number of connections
            while (0 < numberConnections--) {

                //create the new connection
                connetionPool.put(connectionId++, new SmtpConnection(host,
                                                                     port,
                                                                     bindAddress,
                                                                     connectionTimeout,
                                                                     timeout));
            }

            return connectionId - 1;
        } catch (Exception e) {
            String errorMsg = "Could not establish connection to host '" + host + "' on port '" + port + "'";
            if (bindAddress != null) {
                errorMsg += ", binded to address '" + bindAddress + "'";
            }
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Verify that a connection can be opened
     *
     * @param host      The server hostname
     * @param port      The port to connect to
     */
    @PublicAtsApi
    public void verifyConnectionCanBeOpened(
                                             String host,
                                             int port ) {

        if (canOpenConnection(host, port)) {
            log.info("Verified is able to establish connection to host '" + host + "' on port '" + port
                     + "'");
        } else {
            throw new RuntimeException("Unable to establish connection to host '" + host + "' on port '"
                                       + port + "' while it should");
        }
    }

    /**
     * Verify that a connection can be opened
     *
     * @param host      The server hostname
     * @param port      The port to connect to
     */
    @PublicAtsApi
    public void verifyConnectionCannotBeOpened(
                                                String host,
                                                int port ) {

        if (canOpenConnection(host, port)) {
            throw new RuntimeException("Was able to establish connection to host '" + host + "' on port '"
                                       + port + "' while it should not");
        } else {
            log.info("Verified is NOT able to establish connection to host '" + host + "' on port '" + port
                     + "'");
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Close all connections
     */
    @PublicAtsApi
    public void closeAllConnections() {

        /* If the next line is
         * Set<Integer> connectionIds = connetionPool.keySet();
         * after the first connection is removed from the connectionPool, connectionIds Set is modified
         * as well, so iterating it throws a ConcurrentModificationException.
         *
         * So we need to use a list of connection IDs which do not get modified
         */
        Integer[] connectionIds = connetionPool.keySet().toArray(new Integer[connetionPool.size()]);
        for (Integer connectionId : connectionIds) {
            SmtpConnection connection = connetionPool.get(connectionId);
            connection.quit();
            connetionPool.remove(connectionId);
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Close a specified connection
     *
     * @param id Id of connection to be closed
     */
    @PublicAtsApi
    public void closeConnection(
                                 int id ) {

        SmtpConnection connection = this.getConnection(id);
        if (null != connection) {
            connection.quit();
            connetionPool.remove(id);
        } else {
            log.warn("Connection with id " + id + " not closed as it was not found");
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Verifies if all connections in a specified range are valid
     *
     * @param rangeLo        Start connection id to be tested
     * @param rangeHi        End connection id to be tested
     */
    @PublicAtsApi
    public void verifyConnection(
                                  int rangeLo,
                                  int rangeHi ) {

        if (rangeLo > rangeHi) {
            int temp = rangeLo;
            rangeLo = rangeHi;
            rangeHi = temp;
        }

        for (int i = rangeLo; i <= rangeHi; i++) {
            //check if current connection is valid
            if (this.getConnection(i) == null) {
                throw new RuntimeException("Connection with id '" + i + "' is invalid");
            }
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Verifies if all connections in a specified list are valid
     *
     * @param list        List of connection id's
     */
    @PublicAtsApi
    public void verifyConnection(
                                  int... list ) {

        for (int i = 0; i < list.length; i++) {
            //check if current connection is valid
            if (null == this.getConnection(list[i])) {
                throw new RuntimeException("Connection with id '" + list[i] + "' is invalid");
            }
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Executes a SMTP command
     * @param id
     *   Connection id. Valid input is the range from 0 to number_of_open_connections-1.
     * @param command
     *   command to be executed
     * @return
     *   execution response
     */
    @PublicAtsApi
    public String execCommand(
                               int id,
                               String command ) {

        //check if the connection is valid
        String result = null;
        SmtpConnection connection = this.getConnection(id);
        if (null != connection) {
            //execute the command and fetch the result
            connection.sendCommand(command);
            result = connection.getLastResponse();

            if (result == null) {
                log.debug("No response for command: " + command);
            }
        } else {
            throw new RuntimeException("SMTP Session was not found. Unknown ID: " + id);
        }

        return result;
    }

    //--------------------------------------------------------------------------
    /**
     * Returns the connection for a specified connection id
     * @param id
     *   connection id
     * @return
     *   connection or null if none available
     */
    @PublicAtsApi
    public SmtpConnection getConnection(
                                         int id ) {

        return connetionPool.get(id);
    }

    private boolean canOpenConnection(
                                       String host,
                                       int port ) {

        try {
            SmtpConnection connection = new SmtpConnection(host, port);
            connection.quit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
