/*
 * Copyright 2017-2022 Axway Software
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
import java.util.concurrent.TimeUnit;

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
 * <p>The way to configure JDBC driver is via the custom properties argument to the constructor via the {@link DbKeys#DRIVER} property
 * Possible values (see more in {@link DbKeys}):
 * <ul>
 *  <li>"JTDS" - default</li>
 *  <li>"JNetDirect" - to use JNetDirect's JSQLConnect driver</li>
 *  <li>"MSSQL" - to use Official Microsoft SQL Server driver (Note that additional Maven dependency might be needed)</li>
 * </ul>
 * <strong>Note</strong> that JNetDirect have to be added to the class path if you want to use it. ATS does not include it by default.
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
    private static final String           JTDS_JDBC_DRIVER_PREFIX               = "jdbc:jtds:sqlserver://";
    private static final String           JTDS_JDBC_DRIVER_CLASS_NAME           = "net.sourceforge.jtds.jdbc.Driver";
    private static final String           JTDS_JDBC_DATASOURCE_CLASS_NAME       = "net.sourceforge.jtds.jdbcx.JtdsDataSource";
    // JNetDirect driver settings
    private static final String           JNETDIRECT_JDBC_DRIVER_PREFIX         = "jdbc:JSQLConnect://";
    private static final String           JNETDIRECT_JDBC_DRIVER_CLASS_NAME     = "com.jnetdirect.jsql.JSQLDriver";
    private static final String           JNETDIRECT_JDBC_DATASOURCE_CLASS_NAME = "com.jnetdirect.jsql.JSQLPoolingDataSource";
    // MSSQL driver settings
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

    private DataSource                    ds;

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
            if (JTDS_JDBC_DRIVER_PREFIX.equals(jdbcDriverPrefix)) {
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
     * NOTE: This method must not use log4j for logging as this may cause locking issues
     */
    @Override
    public DataSource getDataSource() {

        if (jdbcDataSourceClass.getName().equals(JTDS_JDBC_DATASOURCE_CLASS_NAME)) {
            // jTDS - default SQL server driver. By default, we have it in class path
            // jTDS does not provide connection pool so make one using Apache Commons DBCP
            BasicDataSource localDS = new BasicDataSource();

            // max number of active connections
            Integer maxTotal = AtsSystemProperties.getPropertyAsNumber("dbcp.maxTotal");
            if (maxTotal == null) {
                maxTotal = 8;
            } else {
                log.info("Max number of active connections is "
                         + maxTotal);
            }
            localDS.setMaxTotal(maxTotal);

            // wait time for new connection
            Integer maxWaitMillis = AtsSystemProperties.getPropertyAsNumber("dbcp.maxWaitMillis");
            if (maxWaitMillis == null) {
                maxWaitMillis = 60 * 1000;
            } else {
                log.info("Connection creation wait is "
                         + maxWaitMillis
                         + " msec");
            }
            localDS.setMaxWaitMillis(maxWaitMillis);

            String logAbandoned = System.getProperty("dbcp.logAbandoned");
            if (logAbandoned != null && ("true".equalsIgnoreCase(logAbandoned))
                || "1".equalsIgnoreCase(logAbandoned)) {
                String removeAbandonedTimeoutString = System.getProperty("dbcp.removeAbandonedTimeout");
                int removeAbandonedTimeout = (int) localDS.getMaxWaitMillis() / (2 * 1000);
                if (!StringUtils.isNullOrEmpty(removeAbandonedTimeoutString)) {
                    removeAbandonedTimeout = Integer.parseInt(removeAbandonedTimeoutString);
                }
                log.info(
                         "Will log and remove abandoned connections if not cleaned in "
                         + removeAbandonedTimeout
                         + " sec");
                // log not closed connections
                localDS.setLogAbandoned(true); // issue stack trace of not closed connection
                localDS.setAbandonedUsageTracking(true);
                localDS.setLogExpiredConnections(true);
                localDS.setRemoveAbandonedTimeout(removeAbandonedTimeout);
                localDS.setRemoveAbandonedOnBorrow(true);
                localDS.setRemoveAbandonedOnMaintenance(true);
                localDS.setAbandonedLogWriter(new PrintWriter(System.err));
            }
            localDS.setValidationQuery("SELECT 1");
            localDS.setDriverClassName(getDriverClass().getName());
            localDS.setUsername(user);
            localDS.setPassword(password);
            localDS.setUrl(getURL());
            ds = localDS;
            return localDS;
        } else if (jdbcDataSourceClass.getName().equals(JNETDIRECT_JDBC_DATASOURCE_CLASS_NAME)) {
            //DataSource ds = null;
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
                applyTimeout();
            } catch (Exception e) {
                throw new DbException("Error while configuring data source '" + jdbcDataSourceClass.getName()
                                      + "' for use", e);
            }
            return ds;
        } else if (jdbcDataSourceClass.getName().equals(MSSQL_JDBC_DATASOURCE_CLASS_NAME)) {
            SQLServerDataSource localDS = null;
            try {
                localDS = new SQLServerDataSource();
                localDS.setServerName(this.host);
                localDS.setPortNumber(this.port);
                localDS.setDatabaseName(this.db);
                localDS.setUser(this.user);
                localDS.setPassword(this.password);
                localDS.setEncrypt(false); // do not encrypt the traffic/connection
                localDS.setSSLProtocol("TLSv1.2"); // force usage of TLSv1.2, even if the connection will not be encrypted
                localDS.setTrustServerCertificate(true); // trust the server certificate
                localDS.setIntegratedSecurity(false);
                localDS.setURL(this.getURL()); // if the previous properties (encrypt, ssl protocol, etc) are specified in the URL, the URL values will be used
            } catch (Exception e) {
                throw new DbException("Error while configuring data source '" + jdbcDataSourceClass.getName()
                                      + "' for use", e);
            }
            ds = localDS;
            return localDS;
        } else {
            DataSource localDS = null;
            try {
                localDS = jdbcDataSourceClass.newInstance();
                // FIXME these methods are not standard so error might occur with non-tested driver
                Method setServerName = jdbcDataSourceClass.getMethod("setServerName", String.class);
                setServerName.invoke(localDS, this.host);
                Method setPortNumber = jdbcDataSourceClass.getMethod("setPortNumber", int.class);
                setPortNumber.invoke(localDS, this.port);
                Method setDatabase = null;
                setDatabase = jdbcDataSourceClass.getMethod("setDatabaseName", String.class);
                setDatabase.invoke(localDS, this.db);
                try {
                    Method setUrl = jdbcDataSourceClass.getMethod("setUrl", String.class);
                    setUrl.invoke(localDS, this.url.toString());
                } catch (NoSuchMethodException nsme) {
                    // The method name could differ in the different drivers
                    Method setURL = jdbcDataSourceClass.getMethod("setURL", String.class);
                    setURL.invoke(localDS, this.url.toString());
                }
                applyTimeout();
            } catch (Exception e) {
                throw new DbException("Error while configuring data source '" + jdbcDataSourceClass.getName()
                                      + "' for use", e);
            }
            ds = localDS;
            return localDS;
        }
    }

    @Override
    protected void applyTimeout() {

        if (jdbcDataSourceClass.getName().equals(JTDS_JDBC_DATASOURCE_CLASS_NAME)) {
            /**
             * JTDC uses connectTimeout, socketTimeout and loginTimeout
             * */
            StringBuilder sb = new StringBuilder();

            if (this.timeout == null) {
                // no timeout was specified, use the one from the system property or the default one
                this.timeout = AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_TIMEOUT, DEFAULT_TIMEOUT);
            }

            if (this.timeout < 0) {
                return;
            }

            sb.append("connectTimeout=" + this.timeout + ";");
            sb.append("socketTimeout=" + this.timeout + ";");
            sb.append("loginTimeout=" + this.timeout + ";");

            if (sb.length() > 0) {
                ((BasicDataSource) ds).setConnectionProperties(sb.toString());
            }

        } else if (jdbcDataSourceClass.getName().equals(JNETDIRECT_JDBC_DATASOURCE_CLASS_NAME)) {
            /**
             * JNET uses loginTimeout
             * */
            if (this.timeout == null) {
                this.timeout = AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_TIMEOUT, DEFAULT_TIMEOUT);
            }
            if (this.timeout < 0) {
                return;
            }
            Method loginTimeout = null;
            try {
                loginTimeout = jdbcDataSourceClass.getMethod("loginTimeout", int.class);
                loginTimeout.invoke(ds, this.timeout);
            } catch (Exception e) {
                log.error("Unable to set login timeout. Default value will be used", e);
            }
        } else if (jdbcDataSourceClass.getName().equals(MSSQL_JDBC_DATASOURCE_CLASS_NAME)) {
            if (this.timeout == null) {
                this.timeout = AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_TIMEOUT, DEFAULT_TIMEOUT);
            }
            if (this.timeout < 0) {
                return;
            }
            ((SQLServerDataSource) ds).setSocketTimeout((int) TimeUnit.SECONDS.toMillis(timeout));
            ((SQLServerDataSource) ds).setLoginTimeout((int) TimeUnit.SECONDS.toMillis(timeout));
        } else {
            /**
             * Currently this block is reached when classes from com.microsoft.sqlserver.jdbc package are used.
             * Those classes use socketTimeout.
             * */
            // the amount of time (in seconds) for the connection socket to wait for reading from an established connection before throwing an exception
            if (this.timeout == null) {
                this.timeout = AtsSystemProperties.getPropertyAsNumber(DbKeys.CONNECTION_TIMEOUT, DEFAULT_TIMEOUT);
            }
            if (this.timeout < 0) {
                return;
            }
            try {
                Method setSocketTimeout = jdbcDataSourceClass.getMethod("setSocketTimeout", int.class);
                setSocketTimeout.invoke(ds, this.timeout);
            } catch (Exception e) {
                log.error("Unable to set socket timeout. Default value will be used", e);
            }
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
        // TODO - add if it is using TLS connection

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
                if (ds instanceof BasicDataSource) {
                    ((BasicDataSource) ds).close();
                }
            } catch (Exception e) {
                throw new DbException("Unable to close database source", e);
            }
        }
    }

    /**
     * Get the latest JDBC connection settings.
     * <p>
     * The default driver is {@link DbKeys#SQL_SERVER_DRIVER_JTDS} (jTDS)<br> 
     * If you want to use another driver, add {@link DbKeys#DRIVER} property to the custom properties, when invoking the constructor.<br><br>
     * The available drivers are:<br>
     * <ul>
     * <li>{@link DbKeys#SQL_SERVER_DRIVER_JTDS}</li>
     * <li>{@link DbKeys#SQL_SERVER_DRIVER_MICROSOFT}</li>
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

        // load driver from custom properties
        // set jTDS to be used as a fallback driver
        String driver = MsSQLJDBCDriverVendor.JTDS.name(); // or DbKeys.SQL_SERVER_DRIVER_JTDS
        if (this.customProperties != null) {
            if (this.customProperties.containsKey(DbKeys.DRIVER)) {
                // even if the value is null/empty, it will be used
                String driverValueFromCustProps = (String) this.customProperties.get(DbKeys.DRIVER);
                if (!StringUtils.isNullOrEmpty(driverValueFromCustProps)) {
                    if (!driver.equals(driverValueFromCustProps)) {
                        log.warn("Overriding DB driver for connection '" + this.getDescription() + "' from '" + driver
                                 + "' to '" + driverValueFromCustProps + "'");
                    }
                } else {
                    log.error("Illegal value for SQL Server JDBC driver found (" + driverValueFromCustProps
                              + "). This will cause the connection to fail!");
                }
                driver = driverValueFromCustProps;
            }
        }

        // load vendor enum value
        MsSQLJDBCDriverVendor vendor = null;
        try {
            log.info("Setting DB driver to '" + driver + "' for connection '" + this.getDescription() + "'");
            driver = driver.trim().toUpperCase();
            vendor = MsSQLJDBCDriverVendor.valueOf(driver);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            for (MsSQLJDBCDriverVendor enumValue : MsSQLJDBCDriverVendor.values()) {
                sb.append(enumValue.toString() + ",");
            }
            sb.delete(sb.length() - 1, sb.length()); // remove trailing comma
            throw new DbException("Illegal value '" + driver
                                  + "' is specified for Log DB driver. Supported are: " + sb.toString()
                                  + ". No DB IO will be performed.", e);
        }

        String dataSourceClassName = null;
        String className = null;
        switch (vendor) {
            case JTDS:
                jdbcDriverPrefix = JTDS_JDBC_DRIVER_PREFIX;
                className = JTDS_JDBC_DRIVER_CLASS_NAME;
                dataSourceClassName = JTDS_JDBC_DATASOURCE_CLASS_NAME;
                break;
            case JNETDIRECT:
                jdbcDriverPrefix = JNETDIRECT_JDBC_DRIVER_PREFIX;
                className = JNETDIRECT_JDBC_DRIVER_CLASS_NAME;
                dataSourceClassName = JNETDIRECT_JDBC_DRIVER_CLASS_NAME;
                break;
            case MSSQL:
                jdbcDriverPrefix = MSSQL_JDBC_DRIVER_PREFIX;
                className = MSSQL_JDBC_DRIVER_CLASS_NAME;
                dataSourceClassName = MSSQL_JDBC_DATASOURCE_CLASS_NAME;
                break;
            default:
                // not expected. Just in case if enum is updated w/o implementation here
                throw new DbException("Not implemented support for MsSQL driver type "
                                      + vendor.toString());
        }

        className = className.trim();
        dataSourceClassName = dataSourceClassName.trim();

        // load vendor classes
        log.debug("MsSQL connection: Using JDBC driver prefix: " + jdbcDriverPrefix);
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

        isClassLoaded = loadClass(dataSourceClassName); // check for availability
        if (isClassLoaded) {
            try {
                jdbcDataSourceClass = (Class<? extends DataSource>) Class.forName(dataSourceClassName);
            } catch (ClassNotFoundException e) {
                log.error(e); // Not expected. Already checked in loadClass()
            } catch (ClassCastException e) {
                throw new DbException("Class with name '" + dataSourceClassName
                                      + "' is not a valid javax.sql.DataSource class");
            }
            log.debug("Using JDBC data source class: " + dataSourceClassName);
        } else {
            throw new DbException("Could not load MsSQL JDBC DataSource class with name '" + dataSourceClassName
                                  + "'");
        }

    }

    /*private void updateConnectionSettings() {
    
        // use jTDS as a fallback driver
        String value = DbKeys.SQL_SERVER_DRIVER_JTDS;
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
                                      + ". No DB IO will be performed.", e);
            }
            switch (vendor) {
                case JTDS:
                    System.setProperty(JDBC_PREFIX_KEY, JTDS_JDBC_DRIVER_PREFIX);
                    System.setProperty(JDBC_DRIVER_CLASS_KEY, JTDS_JDBC_DRIVER_CLASS_NAME);
                    System.setProperty(JDBC_DATASOURCE_CLASS_KEY, JTDS_JDBC_DATASOURCE_CLASS_NAME);
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
            jdbcDriverPrefix = JTDS_JDBC_DRIVER_PREFIX;
        }
        log.debug("MsSQL connection: Using JDBC driver prefix: " + jdbcDriverPrefix);

        // JDBC driver class
        value = System.getProperty(JDBC_DRIVER_CLASS_KEY);
        String className = null;
        if (value != null) {
            className = value.trim();
        }
        if (StringUtils.isNullOrEmpty(className)) { // including empty string after trim
            className = JTDS_JDBC_DRIVER_CLASS_NAME;
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
            className = JTDS_JDBC_DATASOURCE_CLASS_NAME;
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

    }*/

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
