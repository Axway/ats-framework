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
package com.axway.ats.core.dbaccess.mssql;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.core.utils.StringUtils;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

/**
 * <p>Connection descriptor for MSSQL databases.</p>
 *
 * <p>Connection-related properties:</p>
 *
 * <p>The way to configure jdbc driver is via <strong>com.axway.automation.ats.logdbdriver</strong> system property.
 * Possible values:
 * <ul>
 *  <li>"JTDS" - default</li>
 *  <li>"JNetDirect" - to use JNetDirect's JSQLConnect driver</li>
 *  <li>"MSSQL" - to use Official Microsoft SQL Server driver (Note that additional Maven dependency is needed)</li>
 * </ul>
 * </p>
 *
 */

public class DbConnSQLServer extends DbConnection {

    private static final AtsConsoleLogger log                                   = new AtsConsoleLogger(DbConnSQLServer.class);

    public static final String            JDBC_DRIVER_VENDOR_KEY                = "com.axway.automation.ats.logdbdriver";
    /**
     * The key for configuring JDBC MsSQL URL prefix string
     */
    private static final String           JDBC_PREFIX_KEY                       = "com.axway.automation.ats.core.dbaccess.mssql_jdbc_prefix";
    /**
     * The key to configure MsSQL JDBC driver class
     */
    private static final String           JDBC_DRIVER_CLASS_KEY                 = "com.axway.automation.ats.core.dbaccess.mssql_jdbc_driver_class";
    /**
     * The key to configure MsSQL JDBC date source class
     */
    private static final String           JDBC_DATASOURCE_CLASS_KEY             = "com.axway.automation.ats.core.dbaccess.mssql_jdbc_datasource_class";

    // jTDS driver settings which are used by default
    private static final String           DEFAULT_JDBC_DRIVER_PREFIX            = "jdbc:jtds:sqlserver://";
    private static final String           DEFAULT_JDBC_DRIVER_CLASS_NAME        = "net.sourceforge.jtds.jdbc.Driver";                                  // JNetDirect com.jnetdirect.jsql.JSQLDriver
    private static final String           DEFAULT_JDBC_DATASOURCE_CLASS_NAME    = "net.sourceforge.jtds.jdbcx.JtdsDataSource";
    // JNetDirect driver settings which are used by default
    private static final String           JNETDIRECT_JDBC_DRIVER_PREFIX         = "jdbc:JSQLConnect://";
    private static final String           JNETDIRECT_JDBC_DRIVER_CLASS_NAME     = "com.jnetdirect.jsql.JSQLDriver";                                    // JNetDirect com.jnetdirect.jsql.JSQLDriver
    private static final String           JNETDIRECT_JDBC_DATASOURCE_CLASS_NAME = "com.jnetdirect.jsql.JSQLPoolingDataSource";
    // MSSQL driver settings which are used by default
    private static final String           MSSQL_JDBC_DRIVER_PREFIX              = "jdbc:sqlserver://";
    private static final String           MSSQL_JDBC_DRIVER_CLASS_NAME          = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String           MSSQL_JDBC_DATASOURCE_CLASS_NAME      = "com.microsoft.sqlserver.jdbc.SQLServerDataSource";

    /**
     * Default DB port
     */
    public static final int               DEFAULT_PORT                          = 1433;

    /**
     * The connection URL
     */
    private StringBuilder                 url                                   = new StringBuilder();

    private BasicDataSource               ds;

    private Boolean                       useSSL;

    /**
     * The JDBC driver prefix to construct URL.
     * For example: "jdbc:JSQLConnect://" for JNetDirect or
     * "jdbc:jtds:sqlserver://" for jTDS
     */
    private String                        jdbcDriverPrefix                      = null;

    /**
     * The JDBC driver class as String
     * For example: "com.jnetdirect.jsql.JSQLDriver" for JNetDirect or
     *   "net.sourceforge.jtds.jdbc.Driver" for jTDS
     */
    private Class<? extends Driver>       jdbcDriverClass                       = null;

    /**
     * The JDBC class for DataSource
     * For example:
     * <ul>
     *  <li>"net.sourceforge.jtds.jdbcx.JtdsDataSource" for jTDS</li>
     *  <li>"com.jnetdirect.jsql.JSQLPoolingDataSource" for JNetDirect</li>
     * </ul>
     */
    private Class<? extends DataSource>   jdbcDataSourceClass                   = null;

    public static final String            DATABASE_TYPE                         = "MSSQL";

    /**
     * Constructor
     *
     * @param host host
     * @param db database name
     * @param user login user name
     * @param password login password
     */
    public DbConnSQLServer( String host, String db, String user, String password ) {

        // since the Collections.singletonMap() returns map that cannot be modified (read-only), we use the single map to create another read-write map
        this(host, db, user, password,
             new HashMap<String, Object>(Collections.singletonMap(DbKeys.DRIVER, DbKeys.SQL_SERVER_DRIVER_JTDS)));
    }

    /**
     * Constructor
     *
     * @param host host
     * @param db database name
     * @param user login user name
     * @param password login password
     * @param customProperties map of custom connection properties
     */
    public DbConnSQLServer( String host,
                            String db,
                            String user,
                            String password,
                            Map<String, Object> customProperties ) {

        this(host, DEFAULT_PORT, db, user, password, customProperties);

    }

    /**
     * Constructor
     *
     * @param host host
     * @param port the database port
     * @param db database name
     * @param user login user name
     * @param password login password
     * @param customProperties map of custom connection properties
     */
    public DbConnSQLServer( String host,
                            int port,
                            String db,
                            String user,
                            String password,
                            Map<String, Object> customProperties ) {

        super(DATABASE_TYPE, host, port, db, user, password, customProperties);
        updateConnectionSettings();

        url.append(jdbcDriverPrefix).append(host).append(":").append(this.port);

        if (!useSSL && db != null) {
            url.append("/").append(db);
        } else {
            if (DEFAULT_JDBC_DRIVER_PREFIX.equals(jdbcDriverPrefix)) {
                if (db != null) {
                    url.append("/").append(db);
                }
                url.append(";ssl=require");
            } else if (MSSQL_JDBC_DRIVER_PREFIX.equals(jdbcDriverPrefix)) {
                // url prefix is missing the ':jtds:' part in SSL connection
                // because the port can be changed after execution of the parent constructor, use this.port, instead of port
                url.append(MSSQL_JDBC_DRIVER_PREFIX).append(host).append(":").append(this.port);

                if (db != null) {
                    url.append(";databaseName=")
                       .append(db)
                       .append(";integratedSecurity=false;encrypt=true;trustServerCertificate=true");
                }
            } else {
                throw new DbException("SSL connection is not possible for the provided driver \""
                                      + System.getProperty(JDBC_DRIVER_CLASS_KEY) + "\".");
            }
        }
        /*if (MSSQL_JDBC_DRIVER_PREFIX.equals(jdbcDriverPrefix)) {
            // force usage of TLSv1.2
            url.append(";sslProtocol=TLSv1.2");
        }*/
    }

    @Override
    protected void initializeCustomProperties(
                                               Map<String, Object> properties ) {

        if (properties != null) {

            if (properties.containsKey(DbKeys.USE_SECURE_SOCKET)
                && "true".equals(properties.get(DbKeys.USE_SECURE_SOCKET))) {
                useSSL = true;
            }

            //read the port if such is set
            Object portValue = properties.get(DbKeys.PORT_KEY);
            if (portValue != null) {
                if (this.port != -1 && this.port != DEFAULT_PORT) {
                    log.warn("New port value found in custom properties. Old value will be overridden");
                }
                this.port = (Integer) portValue;
            }

        }

        if (useSSL == null) {
            useSSL = false;
        }

        if (this.port < 1) {
            this.port = DEFAULT_PORT;
        }
    }

    /**
     * NOTE: This method must not use log4j2 for logging as this may cause locking issues
     */
    @Override
    public DataSource getDataSource() {

        if (jdbcDataSourceClass.getName().equals(DEFAULT_JDBC_DATASOURCE_CLASS_NAME)) {
            // jTDS - default SQL server driver. By default we have it in class path
            // jTDS does not provide connection pool so make one using Apache Commons DBCP
            ds = new BasicDataSource();

            // max number of active connections
            Integer maxTotal = AtsSystemProperties.getPropertyAsNumber("dbcp.maxTotal");
            if (maxTotal == null) {
                maxTotal = 8;
            } else {
                log.info("Max number of active connections is "
                         + maxTotal);
            }
            ds.setMaxTotal(maxTotal);

            // wait time for new connection
            Integer maxWaitMillis = AtsSystemProperties.getPropertyAsNumber("dbcp.maxWaitMillis");
            if (maxWaitMillis == null) {
                maxWaitMillis = 60 * 1000;
            } else {
                log.info("Connection creation wait is "
                         + maxWaitMillis
                         + " msec");
            }
            ds.setMaxWaitMillis(maxWaitMillis);

            String logAbandoned = System.getProperty("dbcp.logAbandoned");
            if (logAbandoned != null && ("true".equalsIgnoreCase(logAbandoned))
                || "1".equalsIgnoreCase(logAbandoned)) {
                String removeAbandonedTimeoutString = System.getProperty("dbcp.removeAbandonedTimeout");
                int removeAbandonedTimeout = (int) ds.getMaxWaitMillis() / (2 * 1000);
                if (!StringUtils.isNullOrEmpty(removeAbandonedTimeoutString)) {
                    removeAbandonedTimeout = Integer.parseInt(removeAbandonedTimeoutString);
                }
                log.info(
                         "Will log and remove abandoned connections if not cleaned in "
                         + removeAbandonedTimeout
                         + " sec");
                // log not closed connections
                ds.setLogAbandoned(true); // issue stack trace of not closed connection
                ds.setAbandonedUsageTracking(true);
                ds.setLogExpiredConnections(true);
                ds.setRemoveAbandonedTimeout(removeAbandonedTimeout);
                ds.setRemoveAbandonedOnBorrow(true);
                ds.setRemoveAbandonedOnMaintenance(true);
                ds.setAbandonedLogWriter(new PrintWriter(System.err));
            }
            ds.setValidationQuery("SELECT 1");
            ds.setDriverClassName(getDriverClass().getName());
            ds.setUsername(user);
            ds.setPassword(password);
            ds.setUrl(getURL());
            return ds;
        } else if (jdbcDataSourceClass.getName().equals(JNETDIRECT_JDBC_DATASOURCE_CLASS_NAME)) {
            DataSource ds = null;
            try {
                ds = jdbcDataSourceClass.newInstance();
                // FIXME these methods are not standard so error might occur with non-tested driver
                Method setServerName = jdbcDataSourceClass.getMethod("setServerName", String.class);
                setServerName.invoke(ds, this.host);
                Method setPortNumber = jdbcDataSourceClass.getMethod("setPortNumber", int.class);
                setPortNumber.invoke(ds, this.port);
                Method setDatabase = null;
                try {
                    setDatabase = jdbcDataSourceClass.getMethod("setDatabase", String.class);
                } catch (NoSuchMethodException nsme) {
                    // The method name could differ in the different drivers
                    setDatabase = jdbcDataSourceClass.getMethod("setDatabaseName", String.class);
                }
                setDatabase.invoke(ds, this.db);
            } catch (Exception e) {
                throw new DbException("Error while configuring data source '" + jdbcDataSourceClass.getName()
                                      + "' for use", e);
            }
            return ds;
        } else if (jdbcDataSourceClass.getName().equals(MSSQL_JDBC_DATASOURCE_CLASS_NAME)) {
            SQLServerDataSource ds = null;
            try {
                ds = new SQLServerDataSource();
                ds.setServerName(this.host);
                ds.setPortNumber(this.port);
                ds.setDatabaseName(this.db);
                ds.setUser(this.user);
                ds.setPassword(this.password);
                ds.setEncrypt(false); // do not encrypt the traffic/connection
                ds.setSSLProtocol("TLSv1.2"); // force usage of TLSv1.2, even if the connection will not be encrypted
                ds.setTrustServerCertificate(true); // trust the server certificate
                ds.setIntegratedSecurity(false);
                ds.setURL(this.getURL()); // if the previous properties (encrypt, ssl protocol, etc) are specified in the URL, the URL values will be used
            } catch (Exception e) {
                throw new DbException("Error while configuring data source '" + jdbcDataSourceClass.getName()
                                      + "' for use", e);
            }
            return ds;
        } else {
            DataSource ds = null;
            try {
                ds = jdbcDataSourceClass.newInstance();
                // FIXME these methods are not standard so error might occur with non-tested driver
                Method setServerName = jdbcDataSourceClass.getMethod("setServerName", String.class);
                setServerName.invoke(ds, this.host);
                Method setPortNumber = jdbcDataSourceClass.getMethod("setPortNumber", int.class);
                setPortNumber.invoke(ds, this.port);
                Method setDatabase = null;
                setDatabase = jdbcDataSourceClass.getMethod("setDatabaseName", String.class);
                setDatabase.invoke(ds, this.db);
                try {
                    Method setUrl = jdbcDataSourceClass.getMethod("setUrl", String.class);
                    setUrl.invoke(ds, this.url.toString());
                } catch (NoSuchMethodException nsme) {
                    // The method name could differ in the different drivers
                    Method setURL = jdbcDataSourceClass.getMethod("setURL", String.class);
                    setURL.invoke(ds, this.url.toString());
                }
            } catch (Exception e) {
                throw new DbException("Error while configuring data source '" + jdbcDataSourceClass.getName()
                                      + "' for use", e);
            }
            return ds;
        }
    }

    @Override
    public Class<? extends Driver> getDriverClass() {

        return jdbcDriverClass;
    }

    @Override
    public String getURL() {

        return url.toString();
    }

    @Override
    public String getDescription() {

        StringBuilder description = new StringBuilder("MSSQL connection to ");
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

        if (ds != null) {
            try {
                ds.close();
            } catch (Exception e) {
                throw new DbException("Unable to close database source", e);
            }
        }
    }

    /**
     * Get latest JDBC connection settings.
     * Alternative for the logdbdriver property is to specify all three system properties:
     *
     * <p>The alternative is to specify manually all three needed properties:</p>
     *
     * <p>JDBC URL prefix system property <strong>com.axway.automation.ats.core.dbaccess.mssql_jdbc_prefix</strong> with possible values:
     * <ul>
     *  <li>"jdbc:jtds:sqlserver://" for jTDS</li>
     *  <li>"jdbc:JSQLConnect://" for JSQLConnect </li>
     * </ul>
     * </p>
     *
     * <p>JDBC driver-related system property <strong>com.axway.automation.ats.core.dbaccess.mssql_jdbc_driver_class</strong> with possible values:
     * <ul>
     *  <li>"net.sourceforge.jtds.jdbc.Driver" for jTDS</li>
     *  <li>"com.jnetdirect.jsql.JSQLDriver" for JSQLConnect </li>
     * </ul>
     * </p>
     *
     * <p>DataSource system property <strong>com.axway.automation.ats.core.dbaccess.mssql_jdbc_datasource_class</strong> with possible values:
     * <ul>
     *  <li>"net.sourceforge.jtds.jdbcx.JtdsDataSource" for jTDS</li>
     *  <li>"com.jnetdirect.jsql.JSQLPoolingDataSource" for JSQLConnect</li>
     * </ul>
     * </p>
     *
     * <p>
     * By default jTDS is used. If you want to use JSQLConnect you should add this
     * to your Java VM invocation command line:<br>
     * <code>
     * -Dcom.axway.automation.ats.core.dbaccess.mssql_jdbc_prefix=jdbc:JSQLConnect://
     * -Dcom.axway.automation.ats.core.dbaccess.mssql_jdbc_driver_class=com.jnetdirect.jsql.JSQLDriver
     * -Dcom.axway.automation.ats.core.dbaccess.mssql_jdbc_datasource_class=com.jnetdirect.jsql.JSQLPoolingDataSource
     * </code>
     * </p>
     */
    private void updateConnectionSettings() {

        String value = System.getProperty(JDBC_DRIVER_VENDOR_KEY);
        if (this.customProperties != null) {
            if (this.customProperties.containsKey(DbKeys.DRIVER)) {
                // even if the value is null/empty, it will be used
                String driverValueFromCustProps = (String) this.customProperties.get(DbKeys.DRIVER);
                if (driverValueFromCustProps != null) {
                    if (!driverValueFromCustProps.equals(value)) {
                        log.warn("Overriding DB driver for connection '" + this.getDescription() + "' from '" + value
                                 + "' to '" + driverValueFromCustProps + "'");
                    }
                }
                value = driverValueFromCustProps;
            }
        }
        MsSQLJDBCDriverVendor vendor = null;// MsSQLJDBCDriverVendor.JTDS; // default version
        if (!StringUtils.isNullOrEmpty(value)) {
            log.info("Setting DB driver to '" + value + "' for connection '" + this.getDescription() + "'");
            value = value.trim().toUpperCase();
            try {
                vendor = MsSQLJDBCDriverVendor.valueOf(value);
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                for (MsSQLJDBCDriverVendor enumValue : MsSQLJDBCDriverVendor.values()) {
                    sb.append(enumValue.toString() + ",");
                }
                sb.delete(sb.length() - 1, sb.length()); // remove trailing comma
                throw new DbException("Illegal value '" + value
                                      + "' is specified for Log DB driver. Supported are: " + sb.toString()
                                      + ". No DB logging will be performed.", e);
            }
            switch (vendor) {
                case JTDS:
                    System.setProperty(JDBC_PREFIX_KEY, DEFAULT_JDBC_DRIVER_PREFIX);
                    System.setProperty(JDBC_DRIVER_CLASS_KEY, DEFAULT_JDBC_DRIVER_CLASS_NAME);
                    System.setProperty(JDBC_DATASOURCE_CLASS_KEY, DEFAULT_JDBC_DATASOURCE_CLASS_NAME);
                    break;
                case JNETDIRECT:
                    System.setProperty(JDBC_PREFIX_KEY, JNETDIRECT_JDBC_DRIVER_PREFIX);
                    System.setProperty(JDBC_DRIVER_CLASS_KEY, JNETDIRECT_JDBC_DRIVER_CLASS_NAME);
                    System.setProperty(JDBC_DATASOURCE_CLASS_KEY, JNETDIRECT_JDBC_DATASOURCE_CLASS_NAME);
                    break;
                case MSSQL:
                    System.setProperty(JDBC_PREFIX_KEY, MSSQL_JDBC_DRIVER_PREFIX);
                    System.setProperty(JDBC_DRIVER_CLASS_KEY, MSSQL_JDBC_DRIVER_CLASS_NAME);
                    System.setProperty(JDBC_DATASOURCE_CLASS_KEY, MSSQL_JDBC_DATASOURCE_CLASS_NAME);
                    break;
                default:
                    // not expected. Just in case if enum is updated w/o implementation here
                    throw new DbException("Not implemented support for MsSQL driver type "
                                          + vendor.toString());
            }
        }

        // alternative way if JDBC driver is not in enum - the 3 properties can be specified explicitely.
        // The class loading check is also here.

        // JDBC prefix like "jdbc:jtds:sqlserver://" from URL jdbc:jtds:sqlserver://SERVER-NAME:port/DB-NAME
        value = System.getProperty(JDBC_PREFIX_KEY);
        if (value != null) {
            jdbcDriverPrefix = value.trim();
        }
        if (StringUtils.isNullOrEmpty(jdbcDriverPrefix)) { // including empty string after trim
            jdbcDriverPrefix = DEFAULT_JDBC_DRIVER_PREFIX;
        }
        log.debug("MsSQL connection: Using JDBC driver prefix: " + jdbcDriverPrefix);

        // JDBC driver class
        value = System.getProperty(JDBC_DRIVER_CLASS_KEY);
        String className = null;
        if (value != null) {
            className = value.trim();
        }
        if (StringUtils.isNullOrEmpty(className)) { // including empty string after trim
            className = DEFAULT_JDBC_DRIVER_CLASS_NAME;
        }
        boolean isClassLoaded = loadClass(className); // check for availability
        if (isClassLoaded) {
            try {
                jdbcDriverClass = (Class<? extends Driver>) Class.forName(className);
            } catch (ClassNotFoundException e) {
                log.error(e); // Not expected. Already checked in loadClass()
            } catch (ClassCastException e) {
                throw new DbException("Class with name '" + className
                                      + "' is not a valid java.sql.Driver class");
            }
            log.debug("Using JDBC driver class: " + className);
        } else {
            throw new DbException("Could not load MsSQL JDBC driver class with name '" + className + "'");
        }

        // JDBC DataSource class
        value = System.getProperty(JDBC_DATASOURCE_CLASS_KEY);
        className = null;
        if (value != null) {
            className = value.trim();
        }
        if (StringUtils.isNullOrEmpty(className)) { // including empty string after trim
            className = DEFAULT_JDBC_DATASOURCE_CLASS_NAME;
        }
        isClassLoaded = loadClass(className); // check for availability
        if (isClassLoaded) {
            try {
                jdbcDataSourceClass = (Class<? extends DataSource>) Class.forName(className);
            } catch (ClassNotFoundException e) {
                log.error(e); // Not expected. Already checked in loadClass()
            } catch (ClassCastException e) {
                throw new DbException("Class with name '" + className
                                      + "' is not a valid javax.sql.DataSource class");
            }
            log.debug("Using JDBC data source class: " + className);
        } else {
            throw new DbException("Could not load MsSQL JDBC DataSource class with name '" + className
                                  + "'");
        }

    }

    /**
     * Tries to load class passed as argument. Does not throw exception if not found.
     * @return true if class is found and loaded
     */
    private boolean loadClass( String someClass ) {

        try {
            Class.forName(someClass); // try to load the class
            return true;
        } catch (ClassNotFoundException e) {
            log.error("Could not load DB access related class '" + someClass
                      + "'. Check that it is specified correctly and that it is in the classpath", e);
            return false;
        }
    }

    /**
     * Supported MsSQL JDBC drivers
     */
    enum MsSQLJDBCDriverVendor {
        JTDS, JNETDIRECT, MSSQL
    }
}
