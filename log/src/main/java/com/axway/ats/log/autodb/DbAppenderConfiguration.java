/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.log.autodb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.logging.log4j.Level;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.log.autodb.exceptions.InvalidAppenderConfigurationException;
import com.axway.ats.log.autodb.io.AbstractDbAccess;
import com.axway.ats.log.model.SystemLogLevel;

/**
 * Hold the configuration data for this db appender
 */
public class DbAppenderConfiguration implements Serializable {

    private static final long serialVersionUID                      = 4786587768915142179L;
    //connection parameters
    private String            host;
    private String            port                                  = null;
    private String            database;
    private String            user;
    private String            password;
    private String            mode                                  = "";
    private String            driver                                = DbKeys.SQL_SERVER_DRIVER_JTDS;
    private String            chunkSize;

    // the capacity of our logging queue
    private static final int  DEFAULT_MAX_NUMBER_PENDING_LOG_EVENTS = 100000;
    private String            maxNumberLogEvents                    = String.valueOf(DEFAULT_MAX_NUMBER_PENDING_LOG_EVENTS);

    //are checkpoints enabled
    private boolean           enableCheckpoints                     = true;

    //the effective logging level. Serialized only by int value to prevent classloading issues of Priority/Level classes
    private Level             loggingThreshold;

    public String getHost() {

        return host;
    }

    public void setHost(
                         String host ) {

        if (host != null) {
            this.host = host;
        }
    }

    public String getPort() {

        return port;
    }

    public void setPort( String port ) {

        this.port = port;

    }

    public String getDatabase() {

        return database;
    }

    public void setDatabase(
                             String database ) {

        if (database != null) {
            this.database = database;
        }
    }

    public String getUser() {

        return user;
    }

    public void setUser(
                         String user ) {

        if (user != null) {
            this.user = user;
        }
    }

    public String getPassword() {

        return password;
    }

    public void setPassword(
                             String password ) {

        if (password != null) {
            this.password = password;
        }
    }

    /**
     * Read the "events" parameter value from log4j2.xml.
     * This value will be used for capacity of our logging queue.
     * 
     * Note: the new value cannot be bellow the default capacity.
     * @return logging queue capacity
     */
    public int getMaxNumberLogEvents() {

        int newMaxNumberLogEvents;

        try {
            newMaxNumberLogEvents = Integer.parseInt(maxNumberLogEvents);
        } catch (NumberFormatException nfe) {
            // bad number
            newMaxNumberLogEvents = DEFAULT_MAX_NUMBER_PENDING_LOG_EVENTS;
        }

        if (newMaxNumberLogEvents < DEFAULT_MAX_NUMBER_PENDING_LOG_EVENTS) {
            // default value cannot be decreased
            newMaxNumberLogEvents = DEFAULT_MAX_NUMBER_PENDING_LOG_EVENTS;
        }

        return newMaxNumberLogEvents;
    }

    public void setMaxNumberLogEvents(
                                       String maxNumberLogEvents ) {

        if (maxNumberLogEvents != null) {
            this.maxNumberLogEvents = maxNumberLogEvents;
        }
    }

    public boolean isBatchMode() {

        return "batch".equalsIgnoreCase(mode.trim());
    }

    public void setMode(
                         String mode ) {

        if (mode != null) {
            this.mode = mode;
        }
    }

    public boolean getEnableCheckpoints() {

        return enableCheckpoints;
    }

    public void setEnableCheckpoints(
                                      boolean enableCheckpoints ) {

        this.enableCheckpoints = enableCheckpoints;
    }

    public Level getLoggingThreshold() {

        return loggingThreshold;
    }

    public void setLoggingThreshold(
                                     Level loggingThreshold ) {

        this.loggingThreshold = loggingThreshold;
    }

    public String getDriver() {

        return driver;
    }

    public void setDriver( String driver ) {

        this.driver = driver;
    }

    public String getChunkSize() {

        return chunkSize;
    }

    public void setChunkSize( String chunkSize ) {

        this.chunkSize = chunkSize;
    }

    /**
     * Verify that the configuration is valid - the method will throw runtime exception if
     * the configuration is not valid
     * @throws InvalidAppenderConfigurationException if the configuration is not valid
     */
    public void validate() throws InvalidAppenderConfigurationException {

        if (host == null) {
            throw new InvalidAppenderConfigurationException("host");
        }

        if (port == null) {
            new AtsConsoleLogger(getClass()).warn("Database port (\"port\" property) is not specified in log4j2.xml file, section for ATS ActiveDbAppender. "
                                                  + "Assuming default value for Microsoft SQL Server databases ("
                                                  + DbConnSQLServer.DEFAULT_PORT + ")");
            this.port = DbConnSQLServer.DEFAULT_PORT + "";
        }

        if (database == null) {
            throw new InvalidAppenderConfigurationException("database");
        }

        if (user == null) {
            throw new InvalidAppenderConfigurationException("user");
        }

        if (password == null) {
            throw new InvalidAppenderConfigurationException("password");
        }

        if (driver == null) {
            this.driver = DbKeys.SQL_SERVER_DRIVER_JTDS;
        }

        if (chunkSize == null) {
            this.chunkSize = AbstractDbAccess.DEFAULT_CHUNK_SIZE + "";
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(
                           Object obj ) {

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        DbAppenderConfiguration otherConfig = (DbAppenderConfiguration) obj;
        if (host != null && !host.equals(otherConfig.host)) {
            return false;
        }

        if (port != null && !port.equals(otherConfig.port)) {
            return false;
        }

        if (database != null && !database.equals(otherConfig.database)) {
            return false;
        }

        if (user != null && !user.equals(otherConfig.user)) {
            return false;
        }

        if (password != null && !password.equals(otherConfig.password)) {
            return false;
        }

        if (maxNumberLogEvents != null && !maxNumberLogEvents.equals(otherConfig.maxNumberLogEvents)) {
            return false;
        }

        if (mode != null && !mode.equals(otherConfig.mode)) {
            return false;
        }

        if (enableCheckpoints != otherConfig.enableCheckpoints) {
            return false;
        }

        if (!loggingThreshold.equals(otherConfig.loggingThreshold)) {
            return false;
        }

        if (!driver.equalsIgnoreCase(otherConfig.driver)) { // driver value is case-insensitive
            return false;
        }

        if (!chunkSize.equals(otherConfig.chunkSize)) {
            return false;
        }

        return true;
    }

    /**
     * Custom deserialization of DbAppenderConfiguration
     * @param s serialization stream.
     * @throws IOException if IO exception.
     * @throws ClassNotFoundException if class not found.
     */
    private void readObject(
                             final ObjectInputStream s ) throws IOException, ClassNotFoundException {

        s.defaultReadObject();
        int levelInt = s.readInt();
        loggingThreshold = SystemLogLevel.toLevel(levelInt);
    }

    /**
     * Serialize DbAppenderConfiguration.
     * @param s serialization stream.
     * @throws IOException if exception during serialization.
     */
    private void writeObject(
                              final ObjectOutputStream s ) throws IOException {

        s.defaultWriteObject();
        if (loggingThreshold == null) {
            // should be set in RemoteLoggingConfiguration
            throw new IllegalStateException("Logging level should not be null");
        }
        s.writeInt(loggingThreshold.intLevel());
    }

}
