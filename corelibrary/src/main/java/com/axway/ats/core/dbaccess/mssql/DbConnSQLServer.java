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
package com.axway.ats.core.dbaccess.mssql;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.log4j.Logger;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.StringUtils;

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
 * </ul>
 * </p>
 *
 */

public class DbConnSQLServer extends DbConnection {

    private static final Logger                log                                   = Logger.getLogger(DbConnSQLServer.class);

    private static final String                JDBC_DRIVER_VENDOR_KEY                = "com.axway.automation.ats.logdbdriver";
    /**
     * The key for configuring JDBC MsSQL URL prefix string
     */
    private static final String                JDBC_PREFIX_KEY                       = "com.axway.automation.ats.core.dbaccess.mssql_jdbc_prefix";
    /**
     * The key to configure MsSQL JDBC driver class
     */
    private static final String                JDBC_DRIVER_CLASS_KEY                 = "com.axway.automation.ats.core.dbaccess.mssql_jdbc_driver_class";
    /**
     * The key to configure MsSQL JDBC date source class
     */
    private static final String                JDBC_DATASOURCE_CLASS_KEY             = "com.axway.automation.ats.core.dbaccess.mssql_jdbc_datasource_class";

    // jTDS driver settings which are used by default
    private static final String                DEFAULT_JDBC_DRIVER_PREFIX            = "jdbc:jtds:sqlserver://";
    private static final String                DEFAULT_JDBC_DRIVER_CLASS_NAME        = "net.sourceforge.jtds.jdbc.Driver";                                  // JNetDirect com.jnetdirect.jsql.JSQLDriver
    private static final String                DEFAULT_JDBC_DATASOURCE_CLASS_NAME    = "net.sourceforge.jtds.jdbcx.JtdsDataSource";
    // JNetDirect driver settings which are used by default
    private static final String                JNETDIRECT_JDBC_DRIVER_PREFIX         = "jdbc:JSQLConnect://";
    private static final String                JNETDIRECT_JDBC_DRIVER_CLASS_NAME     = "com.jnetdirect.jsql.JSQLDriver";                                    // JNetDirect com.jnetdirect.jsql.JSQLDriver
    private static final String                JNETDIRECT_JDBC_DATASOURCE_CLASS_NAME = "com.jnetdirect.jsql.JSQLPoolingDataSource";

    /**
     * Default DB port
     */
    public static final int                    DEFAULT_PORT                          = 1433;

    /**
     * The connection URL
     */
    private StringBuilder                      url                                   = new StringBuilder();

    private BasicDataSource                    ds;

    private Boolean                            useSSL;

    /**
     * The JDBC driver prefix to construct URL.
     * For example: "jdbc:JSQLConnect://" for JNetDirect or
     * "jdbc:jtds:sqlserver://" for jTDS
     */
    private static String                      jdbcDriverPrefix                      = null;

    /**
     * The JDBC driver class as String
     * For example: "com.jnetdirect.jsql.JSQLDriver" for JNetDirect or
     *   "net.sourceforge.jtds.jdbc.Driver" for jTDS
     */
    private static Class<? extends Driver>     jdbcDriverClass                       = null;

    /**
     * The JDBC class for DataSource
     * For example:
     * <ul>
     *  <li>"net.sourceforge.jtds.jdbcx.JtdsDataSource" for jTDS</li>
     *  <li>"com.jnetdirect.jsql.JSQLPoolingDataSource" for JNetDirect</li>
     * </ul>
     */
    private static Class<? extends DataSource> jdbcDataSourceClass                   = null;

    public static final String                 DATABASE_TYPE                         = "MSSQL";

    /**
     * Constructor
     *
     * @param host host
     * @param db database name
     * @param user login user name
     * @param password login password
     */
    public DbConnSQLServer( String host, String db, String user, String password ) {

        this(host, db, user, password, null);
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

        super(DATABASE_TYPE, host, db, user, password, customProperties);
        updateConnectionSettings();

        if (!useSSL) {
            url.append(jdbcDriverPrefix).append(host).append(":").append(port);

            if (db != null) {
                url.append("/").append(db);
            }
        } else {
            // url prefix is missing the ':jtds:' part in SSL connection
            url.append("jdbc:sqlserver://").append(host).append(":").append(port);

            if (db != null) {
                url.append(";databaseName=")
                   .append(db)
                   .append(";integratedSecurity=false;encrypt=true;trustServerCertificate=true");
            }
        }
    }

    @Override
    protected void initializeCustomProperties(
                                               Map<String, Object> properties ) {

        this.port = DEFAULT_PORT;

        if (properties != null && properties.containsKey(DbKeys.USE_SECURE_SOCKET)
            && "true".equals(properties.get(DbKeys.USE_SECURE_SOCKET))) {
            useSSL = true;
        }

        if (useSSL == null) {
            useSSL = false;
        }
    }

    @Override
    public DataSource getDataSource() {

        if (jdbcDataSourceClass.getName().equals(DEFAULT_JDBC_DATASOURCE_CLASS_NAME)) {
            // jTDS - default SQL server driver. By default we have it in class path
            // jTDS does not provide connection pool so make one using Apache Commons DBCP
            ds = new BasicDataSource();

            int maxTotal = 8;
            String maxTotalString = System.getProperty("dbcp.maxTotal");
            if (!StringUtils.isNullOrEmpty(maxTotalString)) {
                maxTotal = Integer.parseInt(maxTotalString);
            }
            ds.setMaxTotal(maxTotal);
            log.info(StringUtils.ATS_CONSOLE_MESSAGE_PREFIX +" Max number of active connections is " + maxTotal);

            long maxWaitMillis = 60 * 1000; // wait 60 sec for new connection
            String maxWaitMillisString = System.getProperty("dbcp.maxWaitMillis");
            if (!StringUtils.isNullOrEmpty(maxWaitMillisString)) {
                maxWaitMillis = Integer.parseInt(maxWaitMillisString);
            }
            ds.setMaxWaitMillis(maxWaitMillis);
            log.info(StringUtils.ATS_CONSOLE_MESSAGE_PREFIX +" Connection creation wait is " + maxWaitMillis
                     + " msec");

            String logAbandoned = System.getProperty("dbcp.logAbandoned");
            if (logAbandoned != null && ("true".equalsIgnoreCase(logAbandoned))
                || "1".equalsIgnoreCase(logAbandoned)) {
                String removeAbandonedTimeoutString = System.getProperty("dbcp.removeAbandonedTimeout");
                int removeAbandonedTimeout = (int) ds.getMaxWaitMillis() / (2 * 1000);
                if (!StringUtils.isNullOrEmpty(removeAbandonedTimeoutString)) {
                    removeAbandonedTimeout = Integer.parseInt(removeAbandonedTimeoutString);
                }
                log.info(StringUtils.ATS_CONSOLE_MESSAGE_PREFIX +" Will log and remove abandoned connections if not cleaned in " + removeAbandonedTimeout
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
        } else {
            DataSource ds = null;
            try {
                ds = jdbcDataSourceClass.newInstance();
                // FIXME these methods are not standard so error might occur with non-tested driver
                Method setServerName = jdbcDataSourceClass.getMethod("setServerName", String.class);
                setServerName.invoke(ds, this.host);
                Method setPortNumber = jdbcDataSourceClass.getMethod("setPortNumber", int.class);
                setPortNumber.invoke(ds, this.port);
                Method setDatabase = jdbcDataSourceClass.getMethod("setDatabase", String.class);
                setDatabase.invoke(ds, this.db);
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

        try {
            ds.close();
        } catch (Exception e) {
            throw new DbException("Unable to close database source", e);
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
     * to your Java VM invocation command line:<br/>
     * <code>
     * -Dcom.axway.automation.ats.core.dbaccess.mssql_jdbc_prefix=jdbc:JSQLConnect://
     * -Dcom.axway.automation.ats.core.dbaccess.mssql_jdbc_driver_class=com.jnetdirect.jsql.JSQLDriver
     * -Dcom.axway.automation.ats.core.dbaccess.mssql_jdbc_datasource_class=com.jnetdirect.jsql.JSQLPoolingDataSource
     * </code>
     * </p>
     */
    private void updateConnectionSettings() {

        String value = System.getProperty(JDBC_DRIVER_VENDOR_KEY);
        MsSQLJDBCDriverVendor vendor = null;// MsSQLJDBCDriverVendor.JTDS; // default version
        if (!StringUtils.isNullOrEmpty(value)) {
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
                log.error(null, e); // Not expected. Already checked in loadClass()
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
                log.error(null, e); // Not expected. Already checked in loadClass()
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
     * Tries to load class passed as argument. does  not throw exception if not found.
     * @return
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
        JTDS, JNETDIRECT
    }
}
