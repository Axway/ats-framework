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
package com.axway.ats.core.dbaccess.oracle;

import java.security.cert.Certificate;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.common.dbaccess.OracleKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.SslUtils;

import oracle.jdbc.pool.OracleDataSource;

/**
 * Connection descriptor for Oracle databases
 */
public class DbConnOracle extends DbConnection {

    private static Logger       log              = LogManager.getLogger(DbConnOracle.class);

    /**
     * Default DB protocol
     */
    private static final String DEFAULT_PROTOCOL = "TCP";

    /**
     * Default DB port
     */
    public static final int     DEFAULT_PORT     = 1521;

    /**
     * Default SID
     */
    private static final String DEFAULT_SID      = "ORCL";

    /**
     * The JDBC Oracle prefix string
     */
    private static final String URL_PREFIX       = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=";

    /**
     * The SID to be used for connecting to the database - use the service name if present and if the service
     * name also does not exist is using the default SID
     */
    private String              sid;

    /**
     * The Service Name to be used for connection to the database
     * If the SID exists it will be used and the service name will be skipped
     */
    private String              serviceName;

    /**
     * The protocol to be used for connecting to the database. 
     * If not set the default one with no encryption will be used
     */
    private String              protocol;

    private Boolean             useEncryption;

    /**
     * The connection URL
     */
    private String              url;

    private OracleDataSource    dataSource;

    public static final String  DATABASE_TYPE    = "ORACLE";

    /**
     * Constructor
     *
     * @param host host
     * @param schemaName the name of the Oracle schema
     * @param user login user name
     * @param password login password
     */
    public DbConnOracle( String host, String schemaName, String user, String password ) {

        this(host, schemaName, user, password, null);
    }

    /**
     * Constructor
     *
     * @param host host
     * @param schemaName the name of the Oracle schema
     * @param user login user name
     * @param password login password
     * @param customProperties map of custom properties
     */
    public DbConnOracle( String host, String schemaName, String user, String password,
                         Map<String, Object> customProperties ) {

        this(host, DEFAULT_PORT, schemaName, user, password, customProperties);

    }

    /**
     * Constructor
     *
     * @param host host
     * @param port port
     * @param schemaName the name of the Oracle schema
     * @param user login user name
     * @param password login password
     * @param customProperties map of custom properties
     */
    public DbConnOracle( String host, int port, String schemaName, String user, String password,
                         Map<String, Object> customProperties ) {

        super(DATABASE_TYPE, host, port, schemaName, user, password, customProperties);

        //set the URL
        if (this.sid != null) {

            this.url = new StringBuffer().append(URL_PREFIX)
                                         .append(this.protocol)
                                         .append(")(HOST=")
                                         .append(host)
                                         .append(")(PORT=")
                                         .append(this.port)
                                         .append("))(CONNECT_DATA=(SID=")
                                         .append(this.sid)
                                         .append(")))")
                                         .toString();
        } else {
            // using service name
            if (this.serviceName != null) {

                this.url = new StringBuffer().append(URL_PREFIX)
                                             .append(this.protocol)
                                             .append(")(HOST=")
                                             .append(host)
                                             .append(")(PORT=")
                                             .append(this.port)
                                             .append("))(CONNECT_DATA=(SERVICE_NAME=")
                                             .append(this.serviceName)
                                             .append(")))")
                                             .toString();
            } else {
                throw new DbException("Service Name or SID is not specified");
            }
        }
    }

    @Override
    protected void initializeCustomProperties( Map<String, Object> customProperties ) {

        if (customProperties != null && !customProperties.isEmpty()) {

            //read the Service Name if such is set
            Object serviceNameValue = customProperties.get(OracleKeys.SERVICE_NAME_KEY);
            if (serviceNameValue != null) {
                String serviceNameValueString = ((String) serviceNameValue).trim();
                if (serviceNameValueString.length() > 0) {
                    this.serviceName = serviceNameValueString;
                }
            }

            Object protocolValue = customProperties.get(OracleKeys.USE_SECURE_SOCKET);
            if (protocolValue != null) {
                this.useEncryption = Boolean.parseBoolean(protocolValue.toString());
                if (useEncryption) {
                    this.protocol = "TCPS";
                }
            }

            //read the SID if such is set
            Object sidValue = customProperties.get(OracleKeys.SID_KEY);
            if (sidValue != null) {
                String sidValueString = ((String) sidValue).trim();
                if (sidValueString.length() > 0) {
                    this.sid = sidValueString;
                }
            }

            //read the PORT if such is set
            Object portValue = customProperties.get(DbKeys.PORT_KEY);
            if (portValue != null) {
                if (this.port != -1 && this.port != DEFAULT_PORT) {
                    log.warn("New port value found in custom properties. Old value will be overridden");
                }
                this.port = (Integer) portValue;
            }
        }

        if (this.useEncryption == null) {
            this.useEncryption = false;
        }

        //set the default values
        if (this.protocol == null) {
            this.protocol = DEFAULT_PROTOCOL;
        }

        //set the default values
        if (this.port < 1) {
            this.port = DEFAULT_PORT;
        }
        if (this.serviceName != null && this.sid != null) {

            log.warn("Both SID and Service Name are supplied. By default SID will be used.");
        } else if (this.serviceName == null && this.sid == null) {
            this.sid = DEFAULT_SID;
        }
    }

    @Override
    public DataSource getDataSource() {

        try {
            dataSource = new OracleDataSource();
            dataSource.setServerName(this.host);
            dataSource.setUser(this.user);
            dataSource.setPassword(this.password);
            if (db != null && !"".equals(db)) { // case when SID or serviceName is not used
                dataSource.setDatabaseName(this.db);
            }
            dataSource.setURL(this.url);

            //enable connection caching - we'll have pooled connections this way
            dataSource.setConnectionCachingEnabled(true);

            if (useEncryption && this.customProperties != null) {
                if (this.customProperties.containsKey(OracleKeys.KEY_STORE_FULL_PATH)
                    && this.customProperties.containsKey(OracleKeys.KEY_STORE_TYPE)
                    && this.customProperties.containsKey(OracleKeys.KEY_STORE_PASSWORD)) {

                    Properties sslConnectionProperties = new Properties();
                    sslConnectionProperties.setProperty(OracleKeys.KEY_STORE_FULL_PATH,
                                                        this.customProperties.get(OracleKeys.KEY_STORE_FULL_PATH)
                                                                             .toString());
                    sslConnectionProperties.setProperty(OracleKeys.KEY_STORE_TYPE,
                                                        this.customProperties.get(OracleKeys.KEY_STORE_TYPE)
                                                                             .toString());
                    sslConnectionProperties.setProperty(OracleKeys.KEY_STORE_PASSWORD,
                                                        this.customProperties.get(OracleKeys.KEY_STORE_PASSWORD)
                                                                             .toString());
                    // this property is set just in case, later we should remove it
                    sslConnectionProperties.setProperty("javax.net.ssl.trustAnchors",
                                                        this.customProperties.get(OracleKeys.KEY_STORE_PASSWORD)
                                                                             .toString());
                    dataSource.setConnectionProperties(sslConnectionProperties);
                } else {
                    log.info("Not all custom properties starting as DbConnection.KEY_STORE_XXX are set. We will try to prepare a default secure connection to Oracle DB");
                    try {
                        Certificate[] certs = SslUtils.getCertificatesFromSocket(host, String.valueOf(port));
                        dataSource.setConnectionProperties(SslUtils.createKeyStore(certs[0], this.host, this.db, "", "",
                                                                                   ""));
                    } catch (Exception e) {
                        throw new DbException("Secure connection to Oracle DB could not be prepared due to failure in creating default certificate.",
                                              e);
                    }
                }
            }

            return dataSource;
        } catch (SQLException e) {

            throw new DbException("Unable to create database source", e);
        }
    }

    @Override
    public Class<? extends Driver> getDriverClass() {

        return oracle.jdbc.driver.OracleDriver.class;
    }

    @Override
    public String getURL() {

        return url;
    }

    @Override
    public String getDescription() {

        StringBuilder description = new StringBuilder("Oracle connection to ");
        description.append(host);
        description.append(":").append(port);
        description.append("/").append(db);

        return description.toString();
    }

    /**
     *  Closes and releases all idle connections that are currently stored
     *  in the connection pool associated with this data source.
     */
    @Override
    public void disconnect() {

        if( dataSource != null ) {
            try {
                dataSource.close();
            } catch (SQLException e) {
                throw new DbException("Unable to close database source", e);
            }
        }
    }
}
