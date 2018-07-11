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
package com.axway.ats.agent.webapp.restservice.model.pojo;

public class DbConnectionPojo extends BasePojo {

    private long   timestamp;
    private String dbHost;
    private String dbPort;
    private String dbName;
    private String dbUser;
    private String dbPass;
    // As of writing this code, only batch and empty (non-batch) are available as modes
    private String mode;
    private int    loggingThreshold;
    private String maxNumberLogEvents;

    public DbConnectionPojo() {}

    public DbConnectionPojo( long timestamp, String dbHost, String dbPort, String dbName, String dbUser, String dbPass,
                             String mode, int loggingThreshold, String maxNumberLogEvents ) {

        this.timestamp = timestamp;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
        this.mode = mode;
        this.loggingThreshold = loggingThreshold;
        this.maxNumberLogEvents = maxNumberLogEvents;
    }

    public String getDbHost() {

        return dbHost;
    }

    public void setDbHost( String dbHost ) {

        this.dbHost = dbHost;
    }

    public String getDbPort() {

        return dbPort;
    }

    public void setDbPort( String dbPort ) {

        this.dbPort = dbPort;
    }

    public String getDbName() {

        return dbName;
    }

    public void setDbName( String dbName ) {

        this.dbName = dbName;
    }

    public String getDbUser() {

        return dbUser;
    }

    public void setDbUser( String dbUser ) {

        this.dbUser = dbUser;
    }

    public String getDbPass() {

        return dbPass;
    }

    public void setDbPass( String dbPass ) {

        this.dbPass = dbPass;
    }

    public long getTimestamp() {

        return timestamp;
    }

    public void setTimestamp( long timestamp ) {

        this.timestamp = timestamp;
    }

    public int getLoggingThreshold() {

        return loggingThreshold;
    }

    public void setLoggingThreshold( int loggingThreshold ) {

        this.loggingThreshold = loggingThreshold;
    }

    public String getMode() {

        return mode;
    }

    public void setMode( String mode ) {

        this.mode = mode;
    }

    public String getMaxNumberLogEvents() {

        return maxNumberLogEvents;
    }

    public void setMaxNumberLogEvents( String maxNumberLogEvents ) {

        this.maxNumberLogEvents = maxNumberLogEvents;
    }

}
