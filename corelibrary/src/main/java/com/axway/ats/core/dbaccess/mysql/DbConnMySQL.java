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
package com.axway.ats.core.dbaccess.mysql;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.reflect.ReflectionUtils;
import com.axway.ats.core.utils.BackwardCompatibility;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.StringUtils;
import com.mysql.cj.conf.PropertyDefinitions.DatabaseTerm;

/**
 * Connection descriptor for MySQL databases
 */
public class DbConnMySQL extends DbConnection {

    private static Logger log = LogManager.getLogger(DbConnMySQL.class);

    public static final  String MYSQL_JDBS_8_DATASOURCE_CLASS_NAME = "com.mysql.cj.jdbc.MysqlConnectionPoolDataSource";
    public static final  String MYSQL_JDBC_5_DATASOURCE_CLASS_NAME = "com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource";
    /**
     * Default DB port
     */
    public static final  int    DEFAULT_PORT                       = 3306;
    /**
     * The JDBC MySQL prefix string
     */
    private static final String JDBC_MYSQL_PREFIX                  = "jdbc:mysql://";
    public static final  String DATABASE_TYPE                      = "MYSQL";

    /**
     * The connection URL
     */
    private String url;
    private String serverTimeZone;

    private static String  dataSourceClassName         = null;
    private static boolean serverTimeZoneWarningLogged = false;

    /**
     * Constructor
     *
     * @param host host
     * @param db database name
     * @param user login user name
     * @param password login password
     */
    public DbConnMySQL( String host, String db, String user, String password ) {

        this(host, db, user, password, null);
    }

    /**
     * Constructor
     *
     * @param host host
     * @param db database name
     * @param user login user name
     * @param password login password
     * @param customProperties map of custom properties
     */
    public DbConnMySQL( String host, String db, String user, String password,
                        Map<String, Object> customProperties ) {

        this(host, DEFAULT_PORT, db, user, password, customProperties);
    }

    /**
     * Constructor
     *
     * @param host host
     * @param port port
     * @param db database name
     * @param user login user name
     * @param password login password
     * @param customProperties map of custom properties
     */
    public DbConnMySQL( String host, int port, String db, String user, String password,
                        Map<String, Object> customProperties ) {

        super(DATABASE_TYPE, host, port, db, user, password, customProperties);

        // should we add other settings like the server time zone for example?
        url = new StringBuilder().append(JDBC_MYSQL_PREFIX)
                                 .append(host)
                                 .append(":")
                                 // because the port can be changed after execution of the parent constructor,
                                 // use this.port, instead of port
                                 .append(this.port)
                                 .append("/")
                                 .append(db)
                                 .toString();
    }

    @Override
    protected void initializeCustomProperties( Map<String, Object> customProperties ) {

        if (customProperties != null && !customProperties.isEmpty()) {
            //read the port if such is set
            Object portValue = customProperties.get(DbKeys.PORT_KEY);
            if (portValue != null) {
                if (this.port != -1 && this.port != DEFAULT_PORT) {
                    log.warn("New port value found in custom properties. Old value will be overridden");
                }
                this.port = (Integer) portValue;
            }

            // read the server's timezone
            Object serverTimeZone = customProperties.get(DbKeys.SERVER_TIMEZONE);
            if (serverTimeZone != null) {
                this.serverTimeZone = (String) serverTimeZone;
            }
        }

        if (this.port < 1) {
            this.port = DEFAULT_PORT;
        }

        if (StringUtils.isNullOrEmpty(this.serverTimeZone)) {
            if (!serverTimeZoneWarningLogged) {
                log.warn("No server timezone specified. This can lead to an exception!");
                serverTimeZoneWarningLogged = true;
            }
        }
    }

    @Override
    public DataSource getDataSource() {

        @BackwardCompatibility
        DataSource dataSource = createDataSource();

        return dataSource;
    }

    private DataSource createDataSource() {

        Class<?> mysqlDataSourceClass = null;
        Object dataSourceInstance = null;
        Throwable mysql8Exception = null;
        Throwable mysql5Exception = null;
        if (dataSourceClassName == null) {
            try {
                mysqlDataSourceClass = Class.forName(MYSQL_JDBS_8_DATASOURCE_CLASS_NAME);
                dataSourceClassName = MYSQL_JDBS_8_DATASOURCE_CLASS_NAME;
            } catch (Exception e) {
                mysql8Exception = e;
                try {
                    mysqlDataSourceClass = Class.forName(MYSQL_JDBC_5_DATASOURCE_CLASS_NAME);
                    dataSourceClassName = MYSQL_JDBC_5_DATASOURCE_CLASS_NAME;

                } catch (Throwable t) {
                    mysql5Exception = t;
                }
            }
        } else {
            // usiNG already cached data source class name
            try {
                mysqlDataSourceClass = Class.forName(dataSourceClassName);
            } catch (Throwable t) {
                log.error("Error while creating MySQL data source class, using the already cached class '"
                          + dataSourceClassName + "'", t);
                mysqlDataSourceClass = null;
            }

        }

        if (mysqlDataSourceClass == null) {
            // actually it is tested with these versions, but 6.xx.xx maybe also be used with the 5.1.xx logic
            StringBuilder sb = new StringBuilder();
            sb.append(
                    "Could not load any MySQL datasource class. Check if your classpath contains either mysql-connector-java.jar with version 5.1.xx or 8.xx.xx\n")
              .append("Exception are:")
              .append("\n\t" + ExceptionUtils.getExceptionMsg(mysql8Exception, "MySQL JDBC 8.xx exception"))
              .append("\n\t" + ExceptionUtils.getExceptionMsg(mysql5Exception, "MySQL JDBC 5.1.xx exception"));
            throw new RuntimeException(sb.toString());
        } else {
            log.info("MySQL datasource class will be '" + dataSourceClassName
                     + "'. Begin datasource configuration");
            try {
                dataSourceInstance = mysqlDataSourceClass.getDeclaredConstructor().newInstance();
                // set server name
                ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass, "setServerName",
                                                                       new Class<?>[]{ String.class }, true),
                                             dataSourceInstance, new Object[]{ this.host });
                // set port
                ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass, "setPort",
                                                                       new Class<?>[]{ int.class }, true),
                                             dataSourceInstance, new Object[]{ this.port });
                // set db name
                ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass, "setDatabaseName",
                                                                       new Class<?>[]{ String.class }, true),
                                             dataSourceInstance, new Object[]{ this.db });
                // set user name
                ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass, "setUser",
                                                                       new Class<?>[]{ String.class }, true),
                                             dataSourceInstance, new Object[]{ this.user });

                // set user password
                ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass, "setPassword",
                                                                       new Class<?>[]{ String.class }, true),
                                             dataSourceInstance, new Object[]{ this.password });
                // set other stuff
                ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass, "setAllowMultiQueries",
                                                                       new Class<?>[]{ boolean.class }, true),
                                             dataSourceInstance, new Object[]{ true });

                if (mysqlDataSourceClass.getName().equals(MYSQL_JDBS_8_DATASOURCE_CLASS_NAME)) {
                    // tell MySQL, that you want connection.getMetaData().getTables() to return tables only from the connection's (table) schema
                    ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass, "setPassword",
                                                                           new Class<?>[]{ String.class }, true),
                                                 dataSourceInstance, new Object[]{ this.password });
                    ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass,
                                                                           "setUseInformationSchema",
                                                                           new Class<?>[]{ boolean.class }, true),
                                                 dataSourceInstance, new Object[]{ true });
                    ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass, "setDatabaseTerm",
                                                                           new Class<?>[]{ String.class }, true),
                                                 dataSourceInstance, new Object[]{ DatabaseTerm.SCHEMA.name() });

                    if (StringUtils.isNullOrEmpty(this.serverTimeZone)) {
                        // try to find the time zone
                        this.serverTimeZone = getServerTimeZone((DataSource) dataSourceInstance);
                        // special handling for EEST so we are IANA compatible
                        if (!StringUtils.isNullOrEmpty(this.serverTimeZone)) {
                            String dateString = "00:00:00 <Z> 2020";
                            dateString = dateString.replace("<Z>", this.serverTimeZone);
                            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss Z yyyy");
                            Date date = sdf.parse(dateString);
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(date);

                            //String zoneId = calendar.getTimeZone().getID();
                            //ZonedDateTime zdt = LocalDateTime.now().atZone(ZoneId.of(zoneId));
                            this.serverTimeZone = calendar.getTimeZone().getID();
                        }

                    }

                    if (!StringUtils.isNullOrEmpty(this.serverTimeZone)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Server time zone set to: " + this.serverTimeZone);
                        }
                        // set the server's time zone or actually what the client will "think" the server time zone is
                        ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(mysqlDataSourceClass,
                                                                               "setServerTimezone",
                                                                               new Class<?>[]{ String.class }, true),
                                                     dataSourceInstance, new Object[]{ this.serverTimeZone });
                    }

                }

                log.info("Done configuring the MySQL datasource.");
                return (DataSource) dataSourceInstance;
            } catch (Throwable t) {
                throw new DbException("Error while configuring MySQL's data source instance", t);
            }
        }

    }

    // TODO
    /*
     * 
     * new MysqlConnectionPoolDataSource();// do not use connection pool (com.axway.ats.core.dbaccess.ConnectionPool)
    dataSource.setServerName(this.host);
    dataSource.setPort(this.port);
    dataSource.setDatabaseName(this.db);
    dataSource.setUser(this.user);
    dataSource.setPassword(this.password);
    try {
        // tell MySQL, that you want connection.getMetaData().getTables() to return tables only from the connection's (table) schema
        dataSource.setUseInformationSchema(true);
        dataSource.setDatabaseTerm(DatabaseTerm.SCHEMA.name());
    } catch (SQLException e) {
        throw new DbException("Error while enabling use information schema flag", e);
    }
    try {
        dataSource.setServerTimezone(this.serverTimeZone);
    } catch (SQLException e) {
        throw new DbException("Error while setting server timezone to " + this.serverTimeZone, e);
    }
    try {
        dataSource.setAllowMultiQueries(true);
    } catch (SQLException e) {
        throw new DbException("Error while enabling multiple queries flag", e);
    }
     * 
     * */

    private String getServerTimeZone( DataSource dataSourceInstance ) {

        String sql = "SHOW GLOBAL VARIABLES WHERE Variable_name LIKE '%system_time_zone%';";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String serverTimeZone = null;
        try {
            // set the time zone to UTC, so we can actually connect to the MySql server
            ReflectionUtils.invokeMethod(ReflectionUtils.getMethod(dataSourceInstance.getClass(),
                                                                   "setServerTimezone",
                                                                   new Class<?>[]{ String.class }, true),
                                         dataSourceInstance, new Object[]{ "UTC" });
            conn = dataSourceInstance.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                serverTimeZone = rs.getString("Value");
                break;
            }
        } catch (Throwable t) {
            log.error("Unable to get server variable system_time_zone", t);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(conn, stmt);
        }
        return serverTimeZone;

    }

    public static String getDataSourceClassName() {

        return dataSourceClassName;
    }

    @Override
    public Class<? extends Driver> getDriverClass() {

        return com.mysql.jdbc.Driver.class;
    }

    @Override
    public String getURL() {

        return url;
    }

    @Override
    public String getDescription() {

        StringBuilder description = new StringBuilder("MySQL connection to ");
        description.append(host);
        description.append(":").append(port);
        description.append("/").append(db);

        return description.toString();
    }

    @Override
    public void disconnect() {

        //data source is not cached so there is nothing to be closed
    }
}
