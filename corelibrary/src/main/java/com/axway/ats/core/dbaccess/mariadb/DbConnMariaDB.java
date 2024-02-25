/*
 * Copyright 2021 Axway Software
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
package com.axway.ats.core.dbaccess.mariadb;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.utils.StringUtils;

/**
 * Connection descriptor for MariaDB databases
 */
public class DbConnMariaDB extends DbConnection {

    public static final String MARIADB_JDBS_DATASOURCE_CLASS_NAME = "org.mariadb.jdbc.MariaDbPoolDataSource";
    private static final Logger log = Logger.getLogger(DbConnMariaDB.class);


    /**
     * Default DB port
     */
    public static final int DEFAULT_PORT = 3306;

    /**
     * The JDBC MariaDB prefix string
     */
    private static final String JDBC_MARIADB_PREFIX = "jdbc:mariadb://";

    public static final String DATABASE_TYPE = "MARIADB";

    public static final String CONNECT_TIMEOUT = "connectTimeout";

    /**
     * The connection URL
     */
    private String url;

    private String serverTimeZone;

    private boolean useSSL;

    /**
     * connectTimeout property in milliseconds as expected by the Connector/J driver.
     * Could be set via customProperties of DBConnection.
     * Default seems to be 30'000ms (30sec).
     * Refer to <a href="https://mariadb.com/docs/connect/programming-languages/java/connect/#connector-j-connect-parameters">MariaDB configuration parameters, connectTimeout</a>
     */
    private String connectTimeout;

    private static String dataSourceClassName = null;

    private static boolean serverTimeZoneWarningLogged = false;

    /**
     * Constructor
     *
     * @param host     host
     * @param db       database name
     * @param user     login username
     * @param password login password
     */
    public DbConnMariaDB(String host, String db, String user, String password) {

        this(host, db, user, password, null);
    }

    /**
     * Constructor
     *
     * @param host             host
     * @param db               database name
     * @param user             login user name
     * @param password         login password
     * @param customProperties map of custom properties
     */
    public DbConnMariaDB(String host, String db, String user, String password,
            Map<String, Object> customProperties) {

        this(host, DEFAULT_PORT, db, user, password, customProperties);
    }

    /**
     * Constructor
     *
     * @param host             host
     * @param port             port
     * @param db               database name
     * @param user             login user name
     * @param password         login password
     * @param customProperties map of custom properties
     */
    public DbConnMariaDB(String host, int port, String db, String user, String password,
            Map<String, Object> customProperties) {

        super(DATABASE_TYPE, host, port, db, user, password, customProperties);
        //initializeCustomProperties(customProperties); // after parent constructor and local fields are initialized

        // should we add other settings like the server time zone for example?
        url = new StringBuilder().append(JDBC_MARIADB_PREFIX)
                .append(host)
                .append(":")
                // because the port can be changed after execution of the parent constructor,
                // use this.port, instead of port
                .append(this.port)
                .append("/")
                .append(db)
                .append(((useSSL)? "?useSSL=true": ""))
                .toString();
    }

    @Override
    protected void initializeCustomProperties(Map<String, Object> customProperties) {

        if (customProperties != null && !customProperties.isEmpty()) {
            // read the port if such is set
            Object portValue = customProperties.get(DbKeys.PORT_KEY);
            if (portValue != null) {
                if (this.port != -1 && this.port != DEFAULT_PORT) {
                    log.warn("New port value found in custom properties. Old value will be overridden");
                }
                this.port = (Integer) portValue;
            }

            // Connect timeout
            Object connTimeout = customProperties.get(CONNECT_TIMEOUT); // TODO: export in DbKeys file
            if (connTimeout != null) {
                this.connectTimeout = (String) connTimeout;
                if (log.isDebugEnabled()) {
                    log.debug("Read property " + CONNECT_TIMEOUT + " with value " + connTimeout + " ms");
                }
            }

            // read the server's timezone
            Object serverTimeZone = customProperties.get(DbKeys.SERVER_TIMEZONE);
            if (serverTimeZone != null) {
                this.serverTimeZone = (String) serverTimeZone;
            }

            Object secProp = customProperties.get(DbKeys.USE_SECURE_SOCKET);
            if ( secProp != null && Boolean.parseBoolean(secProp.toString())) {
                useSSL = true;
            }

        }

        if (this.port < 1) {
            this.port = DEFAULT_PORT;
        }

        if (StringUtils.isNullOrEmpty(this.serverTimeZone)) {
            if (!serverTimeZoneWarningLogged) {
                log.warn("No server timezone specified. This can lead to an exception!");
                DbConnMariaDB.serverTimeZoneWarningLogged = true;
            }
        }
    }

    @Override
    public DataSource getDataSource() {

        DataSource dataSource = createDataSource();

        return dataSource;
    }

    private DataSource createDataSource() {

        try {
            // construct URL
            StringBuilder sb = new StringBuilder();
            sb.append(JDBC_MARIADB_PREFIX)
                    .append(this.getHost())
                    .append(":")
                    .append(this.getPort())
                    .append("/")
                    .append(this.getDb())
                    .append("?user=")
                    .append(this.getUser())
                    .append("&password=")
                    .append(this.getPassword())
                    .append("&allowMultiQueries=true");

            if (useSSL) {
                sb.append("&useSSL=true&trustServerCertificate=true"); // temp
                // Optionally, if you want (but it is not recommended), you can add trustServerCertificate=true
                // This will prevent failure, when server certificate is not provided to the client
                // More information here -> https://mariadb.com/kb/en/using-tls-ssl-with-mariadb-java-connector/#one-way-ssl-authentication
                log.info("SSL is enabled with trustServerCertificate!");
            }

            if (!StringUtils.isNullOrEmpty(connectTimeout)) {
                sb.append("&" + CONNECT_TIMEOUT + "=" + connectTimeout);
                if (log.isDebugEnabled()) {
                    log.debug("Added connection timeout!");
                }
            }

            if (StringUtils.isNullOrEmpty(this.serverTimeZone)) {
                // try to find the time zone
                this.serverTimeZone = getServerTimeZone(new String(sb));
                // special handling for EEST so we are IANA compatible
                if (!StringUtils.isNullOrEmpty(this.serverTimeZone)) {
                    String dateString = "00:00:00 <Z> 2020";
                    dateString = dateString.replace("<Z>", this.serverTimeZone);
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss Z yyyy");
                    Date date = sdf.parse(dateString);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);

                    // String zoneId = calendar.getTimeZone().getID();
                    // ZonedDateTime zdt = LocalDateTime.now().atZone(ZoneId.of(zoneId));
                    this.serverTimeZone = calendar.getTimeZone().getID();
                }
            }

            if (!StringUtils.isNullOrEmpty(this.serverTimeZone)) {
                if (log.isDebugEnabled()) {
                    log.debug("Server time zone set to: " + this.serverTimeZone);
                }
                sb.append("&serverTimezone=" + this.serverTimeZone);
            }

            MariaDbPoolDataSource ds = new MariaDbPoolDataSource(sb.toString());

            log.info("MariaDB datasource class will be '" + ds.getClass()
                    + "'. Begin datasource configuration");

            // here set any additional configuration options, that cannot be passed via the URL

            log.info("Done configuring the MariaDB datasource.");
            return ds;
        } catch (Exception e) {
            throw new DbException("Error while creating MariaDB datasource", e);
        }

    }

    private String getServerTimeZone(String url) {

        String sql = "SHOW GLOBAL VARIABLES WHERE Variable_name LIKE '%system_time_zone%';";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String serverTimeZone = null;
        MariaDbPoolDataSource ds = null;
        try {
            url += "&serverTimezone=UTC";
            if (!StringUtils.isNullOrEmpty(connectTimeout)) {
                url = url + "&" + CONNECT_TIMEOUT + "=" + connectTimeout;
                if (log.isDebugEnabled()) {
                    log.debug("Added connection timeout!");
                }
            }
            ds = new MariaDbPoolDataSource(url);
            conn = ds.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                serverTimeZone = rs.getString("Value");
                break;
            }
        } catch (Exception ex) {
            log.error("Unable to get server variable system_time_zone", ex);
        } finally {
            DbUtils.closeResultSet(rs);
            DbUtils.close(conn, stmt);
            if (ds != null) {
                ds.close();
            }
        }
        return serverTimeZone;

    }

    public static String getDataSourceClassName() {

        return dataSourceClassName;
    }

    @Override
    public Class<? extends Driver> getDriverClass() {

        return org.mariadb.jdbc.Driver.class;
    }

    @Override
    public String getURL() {

        return url;
    }

    @Override
    public String getDescription() {

        StringBuilder description = new StringBuilder("MariaDB connection to ");
        description.append(host).append(':').append(port);
        description.append('/').append(db).append(',');
        if (!useSSL) {
            description.append(" not");
        }
        description.append(" using SSL");

        return description.toString();
    }

    @Override
    public void disconnect() {

        // data source is not cached so there is nothing to be closed
    }
}
