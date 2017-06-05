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
    private String dbName;
    private String dbUser;
    private String dbPass;

    public DbConnectionPojo() {}

    public DbConnectionPojo( long timestamp,
                             String dbHost,
                             String dbName,
                             String dbUser,
                             String dbPass ) {
        this.timestamp = timestamp;
        this.dbHost = dbHost;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
    }

    public String getDbHost() {

        return dbHost;
    }

    public void setDbHost(
                           String dbHost ) {

        this.dbHost = dbHost;
    }

    public String getDbName() {

        return dbName;
    }

    public void setDbName(
                           String dbName ) {

        this.dbName = dbName;
    }

    public String getDbUser() {

        return dbUser;
    }

    public void setDbUser(
                           String dbUser ) {

        this.dbUser = dbUser;
    }

    public String getDbPass() {

        return dbPass;
    }

    public void setDbPass(
                           String dbPass ) {

        this.dbPass = dbPass;
    }

    public long getTimestamp() {

        return timestamp;
    }

    public void setTimestamp(
                              long timestamp ) {

        this.timestamp = timestamp;
    }

}
