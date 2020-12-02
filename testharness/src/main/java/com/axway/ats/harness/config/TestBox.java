/*
 * Copyright 2017-2020 Axway Software
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
import com.axway.ats.core.dbaccess.DatabaseProviderFactory;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.utils.HostUtils;

/**
 * A test box 
 */
@PublicAtsApi
public class TestBox extends Box {

    private static final long serialVersionUID      = 1L;

    /**
     * Constant for specifying that DB port is not set
     */
    public final static int   DB_PORT_NOT_SPECIFIED = 0;

    private String            host;
    /**
     * DB port number. Default is 0 if not specified
     */
    private int               dbPort                = DB_PORT_NOT_SPECIFIED;

    private String            adminUser;
    private String            adminPass;

    private String            dbType;
    private String            dbName;
    private String            dbUser;
    private String            dbPass;

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
     * @return the DB port to be used for connection
     */
    @PublicAtsApi
    public int getDbPort() {

        return dbPort;
    }

    /**
     * Specifies DB port. Should be a string representing a valid integer port number. 
     * @param dbPortStr the DB port number to set
     * @throws IllegalArgumentException if parameter could not be parsed as a valid integer port number 
     */
    @PublicAtsApi
    public void setDbPort(
                           String dbPortStr ) {

        verifyNotNullNorEmptyParameter("dbPort", dbPortStr);
        int dbPortInt = DB_PORT_NOT_SPECIFIED;
        final String errMsg = "'. Expected number within range [" + HostUtils.LOWEST_PORT_NUMBER + ","
                              + HostUtils.HIGHEST_PORT_NUMBER + "]";
        try {
            dbPortStr = dbPortStr.trim();
            dbPortInt = Integer.parseInt(dbPortStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal value for DB port specified: '" + dbPortStr + errMsg);
        }
        if (dbPortInt < HostUtils.LOWEST_PORT_NUMBER || dbPortInt > HostUtils.HIGHEST_PORT_NUMBER) {
            throw new IllegalArgumentException("Illegal number for DB port specified: '" + dbPortInt
                                               + errMsg);
        }
        this.dbPort = dbPortInt;
    }

    /**
     * @return the adminUser
     */
    @PublicAtsApi
    public String getAdminUser() {

        return adminUser;
    }

    /**
     * @param adminUser the adminUser to set
     */
    @PublicAtsApi
    public void setAdminUser(
                              String adminUser ) {

        verifyNotNullNorEmptyParameter("adminUser", adminUser);
        this.adminUser = adminUser;
    }

    /**
     * @return the adminPass
     */
    @PublicAtsApi
    public String getAdminPass() {

        return adminPass;
    }

    /**
     * @param adminPass the adminPass to set
     */
    @PublicAtsApi
    public void setAdminPass(
                              String adminPass ) {

        verifyNotNullNorEmptyParameter("adminPass", adminPass);
        this.adminPass = adminPass;
    }

    /**
     * @return
     */
    @PublicAtsApi
    public String getDbType() {

        return dbType;
    }

    /**
     * @param dbType
     */
    @PublicAtsApi
    public void setDbType(
                           String dbType ) {

        verifyNotNullNorEmptyParameter("dbType", dbType);
        this.dbType = dbType;
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

    @Override
    @PublicAtsApi
    public TestBox newCopy() {

        TestBox newBox = new TestBox();

        newBox.host = this.host;
        newBox.adminUser = this.adminUser;
        newBox.adminPass = this.adminPass;

        newBox.dbPort = this.dbPort;
        newBox.dbType = this.dbType;
        newBox.dbName = this.dbName;
        newBox.dbUser = this.dbUser;
        newBox.dbPass = this.dbPass;

        newBox.properties = this.getNewProperties();

        return newBox;
    }

    /**<strong>INTERNAL<strong> method. Could be changed without notice
     * Create TestBox from {@link DbConnection} object<br>
     * Note that any custom properties from the connection (except the port value) will not be transfered to the TestBox
     * 
     * @param dbConnection - the db connection or null if the dbConnection parameter is null
     * */

    //@PublicAtsApi
    public static TestBox fromDbConnection( DbConnection dbConnection ) {

        if (dbConnection == null) {
            return null; // should an exception IllegalArgumentException be thrown instead ?!?
        }

        String host = dbConnection.getHost();
        int port = dbConnection.getPort();
        String dbName = dbConnection.getDb();
        String dbType = dbConnection.getDbType();
        String userName = dbConnection.getUser();
        String password = dbConnection.getPassword();

        TestBox box = new TestBox();
        box.setHost(host);
        box.setDbPort(port + "");
        box.setDbName(dbName);
        box.setDbUser(userName);
        box.setDbPass(password);
        box.setDbType(dbType);
        return box;
    }

    //@PublicAtsApi
    /**<strong>INTERNAL<strong> method. Could be changed without notice
     * */
    public DbConnection asDbConnection() {

        return DatabaseProviderFactory.createDbConnection(dbType, host, dbPort, dbName, dbUser, dbPass);

    }

    @Override
    @PublicAtsApi
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("Test Box: host = " + host);
        sb.append(", db type = " + dbType);
        sb.append(", db port = " + dbPort);
        sb.append(", db name = " + dbName);
        sb.append(", db user = " + dbUser);
        sb.append(", db pass = " + dbPass);
        sb.append(super.toString());

        return sb.toString();
    }
}
