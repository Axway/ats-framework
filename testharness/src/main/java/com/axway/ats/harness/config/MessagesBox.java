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
package com.axway.ats.harness.config;

import com.axway.ats.common.PublicAtsApi;

@PublicAtsApi
public class MessagesBox extends Box {

    private static final long serialVersionUID = 1L;

    private String            host;

    private String            dbName;
    private String            dbUser;
    private String            dbPass;
    private String            dbTable;

    /**
     * @return the host
     */
    @PublicAtsApi
    public String getHost() {

        return host;
    }

    /**
     * @param host the host to set
     */
    @PublicAtsApi
    public void setHost(
                         String host ) {

        verifyNotNullNorEmptyParameter("host", host);
        this.host = host;
    }

    /**
     * @return
     */
    @PublicAtsApi
    public String getDbName() {

        return dbName;
    }

    /**
     * @param dbName
     */
    @PublicAtsApi
    public void setDbName(
                           String dbName ) {

        verifyNotNullNorEmptyParameter("dbName", dbName);
        this.dbName = dbName;
    }

    /**
     * @return
     */
    @PublicAtsApi
    public String getDbUser() {

        return dbUser;
    }

    /**
     * @param dbUser
     */
    @PublicAtsApi
    public void setDbUser(
                           String dbUser ) {

        verifyNotNullNorEmptyParameter("dbUser", dbUser);
        this.dbUser = dbUser;
    }

    /**
     * @return
     */
    @PublicAtsApi
    public String getDbPass() {

        return dbPass;
    }

    /**
     * @param dbPass
     */
    @PublicAtsApi
    public void setDbPass(
                           String dbPass ) {

        verifyNotNullNorEmptyParameter("dbPass", dbPass);
        this.dbPass = dbPass;
    }

    /**
     * @return the table with messages
     */
    @PublicAtsApi
    public String getDbTable() {

        return dbTable;
    }

    /**
     * @param dbTable the table with messages to set
     */
    @PublicAtsApi
    public void setDbTable(
                            String dbTable ) {

        verifyNotNullNorEmptyParameter("dbTable", dbTable);
        this.dbTable = dbTable;
    }

    @Override
    @PublicAtsApi
    public MessagesBox newCopy() {

        MessagesBox newBox = new MessagesBox();

        newBox.host = this.host;

        newBox.dbName = this.dbName;
        newBox.dbUser = this.dbUser;
        newBox.dbPass = this.dbPass;
        newBox.dbTable = this.dbTable;

        newBox.properties = this.getNewProperties();

        return newBox;
    }

    @Override
    @PublicAtsApi
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("Messages Box: host = " + host);
        sb.append(", db name = " + dbName);
        sb.append(", db user = " + dbUser);
        sb.append(", db pass = " + dbPass);
        sb.append(", db table = " + dbTable);
        sb.append(super.toString());

        return sb.toString();
    }
}
