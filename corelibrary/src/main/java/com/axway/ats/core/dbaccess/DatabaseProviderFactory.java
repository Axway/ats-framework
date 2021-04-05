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
package com.axway.ats.core.dbaccess;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.core.dbaccess.cassandra.CassandraDbProvider;
import com.axway.ats.core.dbaccess.cassandra.DbConnCassandra;
//import com.axway.ats.core.dbaccess.db2.Db2DbProvider;
//import com.axway.ats.core.dbaccess.db2.DbConnDb2;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.core.dbaccess.mariadb.DbConnMariaDB;
import com.axway.ats.core.dbaccess.mariadb.MariaDbDbProvider;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.mssql.MssqlDbProvider;
import com.axway.ats.core.dbaccess.mysql.DbConnMySQL;
import com.axway.ats.core.dbaccess.mysql.MysqlDbProvider;
import com.axway.ats.core.dbaccess.oracle.DbConnOracle;
import com.axway.ats.core.dbaccess.oracle.OracleDbProvider;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.dbaccess.postgresql.PostgreSqlDbProvider;
import com.axway.ats.core.reflect.AmbiguousMethodException;
import com.axway.ats.core.reflect.MethodFinder;
import com.axway.ats.core.utils.XmlUtils;

/**
 * Provides a method to create a suitable database provider
 */
public class DatabaseProviderFactory {

    private static final Logger          log                         = LogManager.getLogger(DatabaseProviderFactory.class);

    private static Map<String, String[]> dbProviders                 = null;

    public static final String           DB_PROVIDERS_RESOURCES_FILE = "ats-core-dbproviders.xml";

    /**
     * Load database providers from resource files
     */
    static {
        dbProviders = new HashMap<>();
        /*
         * load all resource files this DB_PROVIDERS_RESOURCES_FILE (declared
         * above) name
         */
        Enumeration<URL> configResources = null;
        try {
            configResources = ClassLoader.getSystemResources(DB_PROVIDERS_RESOURCES_FILE);
        } catch (IOException e) {
            log.error("Unable to obtain " + DB_PROVIDERS_RESOURCES_FILE + "file(s) from classpath", e);
        }
        /*
         * Iterate over all found resource files, and add provider data to map
         */
        if (configResources != null) {
            while (configResources.hasMoreElements()) {
                URL url = configResources.nextElement();
                Document doc = null;
                try {
                    log.info("Loading provider(s) config data from " + url.toString());
                    doc = XmlUtils.loadXMLFile(url.openStream());
                } catch (IOException | SAXException e) {
                    log.error("Unable to parse provider(s) config data from " + url.toString(), e);
                }
                /*
                 * If an error was NOT encountered, doc object will NOT be null, so
                 * we can proceed with parsing the provider(s) config data
                 */
                if (doc != null) {
                    NodeList children = doc.getElementsByTagName("providers").item(0).getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        Node child = children.item(i);
                        String alias = null;
                        String dbProviderClassName = null;
                        String dbConnectionClassName = null;
                        /*
                         * we are only interested in attributes from child nodes
                         * with name 'provider'
                         */
                        if ("provider".equals(child.getNodeName())) {
                            NamedNodeMap attributes = child.getAttributes();
                            for (int attrIdx = 0; attrIdx < attributes.getLength(); attrIdx++) {
                                String name = attributes.item(attrIdx).getNodeName();
                                String value = attributes.item(attrIdx).getNodeValue();
                                if ("alias".equals(name)) {
                                    alias = value;
                                }
                                if ("provider".equals(name)) {
                                    dbProviderClassName = value;
                                }
                                if ("connection".equals(name)) {
                                    dbConnectionClassName = value;
                                }
                            }
                            if (dbProviders.containsKey(alias)) {
                                log.warn("Provider with alias '" + alias + "' already loaded to map. "
                                         + "Replacing previous config data for this provider.");
                            }
                            dbProviders.put(alias,
                                            new String[]{ dbConnectionClassName, dbProviderClassName });
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a database provider suitable for the given input arguments
     * 
     * @param dbType
     * @param dbHost
     * @param dbName
     * @param dbUser
     * @param dbPass
     * @param dbPort
     * @param customProperties
     * @return
     */
    public static synchronized DbProvider getDatabaseProvider( String dbType, String dbHost, String dbName,
                                                               String dbUser, String dbPass, int dbPort,
                                                               Map<String, Object> customProperties ) {

        DbProvider dbProvider;

        if (dbType == null) {
            throw new IllegalArgumentException("Database type is not provided");
        }
        dbType = dbType.toUpperCase();

        if (dbName == null) {
            if ("MSSQL".equals(dbType)) {
                log.warn("Database name is empty! The connection will be made to the admin database!");
            } else {
                throw new IllegalArgumentException("Database name is not provided");
            }
        }

        // if the DB port is explicitly specified, then overwrite the
        // eventual custom property value
        if (dbPort > 0) {
            if (customProperties == null) {
                customProperties = new HashMap<String, Object>();
            }
            customProperties.put(DbKeys.PORT_KEY, dbPort);
        }

        // load a custom data provider
        if (dbProviders.containsKey(dbType)) {
            String[] classNames = dbProviders.get(dbType);

            DbConnection dbConnection = loadDbConnection(classNames[0], dbType, dbHost, dbName, dbUser,
                                                         dbPass, dbPort, customProperties);

            return loadCustomDbProvider(classNames[1], dbConnection);

        }
        // load a common ATS supported data provider
        switch (dbType) {
            case DbConnSQLServer.DATABASE_TYPE:
                dbProvider = new MssqlDbProvider((DbConnSQLServer) createDbConnection(dbType, dbHost, dbPort,
                                                                                      dbName, dbUser, dbPass,
                                                                                      customProperties));
                break;

            case DbConnPostgreSQL.DATABASE_TYPE:
                dbProvider = new PostgreSqlDbProvider((DbConnPostgreSQL) createDbConnection(dbType, dbHost, dbPort,
                                                                                            dbName, dbUser, dbPass,
                                                                                            customProperties));
                break;

            case DbConnMySQL.DATABASE_TYPE:
                dbProvider = new MysqlDbProvider((DbConnMySQL) createDbConnection(dbType, dbHost, dbPort,
                                                                                  dbName, dbUser, dbPass,
                                                                                  customProperties));
                break;

            case DbConnMariaDB.DATABASE_TYPE:
                dbProvider = new MariaDbDbProvider((DbConnMariaDB) createDbConnection(dbType, dbHost, dbPort,
                                                                                      dbName, dbUser, dbPass,
                                                                                      customProperties));
                break;

            case DbConnOracle.DATABASE_TYPE:
                dbProvider = new OracleDbProvider((DbConnOracle) createDbConnection(dbType, dbHost, dbPort,
                                                                                    dbName, dbUser, dbPass,
                                                                                    customProperties));
                break;

            case DbConnCassandra.DATABASE_TYPE:
                dbProvider = new CassandraDbProvider((DbConnCassandra) createDbConnection(dbType, dbHost, dbPort,
                                                                                          dbName, dbUser, dbPass,
                                                                                          customProperties));
                break;
            default: {
                // should never happen
                throw new IllegalArgumentException("Database type " + dbType + " not supported");
            }
        }

        return dbProvider;
    }

    private static DbProvider loadCustomDbProvider( String className, DbConnection dbConnection ) {

        Class<?> dbProviderClass = null;
        // load db provider class
        try {
            dbProviderClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new DbException("Unable to get database provider for type '" + dbConnection.getDbType()
                                  + "'", e);
        }

        // load db provider constructor
        Constructor<?> constructorDbProvider = null;
        try {
            constructorDbProvider = new MethodFinder(dbProviderClass).findConstructor(new Class[]{ DbConnection.class });
        } catch (NoSuchMethodException | AmbiguousMethodException e) {
            throw new DbException("Unable to get database provider for type '" + dbConnection.getDbType()
                                  + "'", e);
        }

        // create DbProvider class for this dbType
        try {
            return (DbProvider) constructorDbProvider.newInstance(new Object[]{ dbConnection });
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new DbException("Unable to get database provider for type '" + dbConnection.getDbType()
                                  + "'", e);
        }
    }

    private static DbConnection loadDbConnection( String className, String dbType, String dbHost,
                                                  String dbName, String dbUser, String dbPass, int dbPort,
                                                  Map<String, Object> customProperties ) {

        Class<?> dbConnectionClass = null;
        // load db connection class
        try {
            dbConnectionClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new DbException("Unable to get database provider for type '" + dbType + "'", e);
        }

        // load db connection constructor
        Constructor<?> constructor = null;
        try {
            constructor = new MethodFinder(dbConnectionClass).findConstructor(new Class[]{ String.class,
                                                                                           String.class,
                                                                                           String.class,
                                                                                           String.class,
                                                                                           Map.class });
        } catch (NoSuchMethodException | AmbiguousMethodException e) {
            throw new DbException("Unable to get database provider for type '" + dbType + "'", e);
        }

        // create DbConnection class for this dbType
        DbConnection dbConnection = null;
        try {
            dbConnection = (DbConnection) constructor.newInstance(new Object[]{ dbHost, dbName, dbUser,
                                                                                dbPass,
                                                                                customProperties });
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new DbException("Unable to get database provider for type '" + dbType + "'", e);
        }

        return dbConnection;
    }

    /**
     * Create a new database connection
     * 
     * @param dbType
     *            the type of the database
     * @param host
     *            the host to connect to
     * @param port
     *            the port to connect to
     * @param database
     *            the database name
     * @param user
     *            the login user name
     * @param password
     *            the login password
     * @return database connection descriptor
     */
    public static DbConnection createDbConnection( String dbType, String host, int port, String database, String user,
                                                   String password ) {

        return createDbConnection(dbType, host, port, database, user, password, null);
    }

    /**
     * Create a new database connection
     * 
     * @param dbType the type of the database
     * @param host the host to connect to
     * @param database the database name
     * @param user the login user name
     * @param password the login password
     * @param customProperties a set of custom properties for the connection
     * @return database connection descriptor
     */
    public static DbConnection createDbConnection( String dbType, String host, int port, String database, String user,
                                                   String password, Map<String, Object> customProperties ) {

        dbType = dbType.toUpperCase();

        switch (dbType) {
            case DbConnMySQL.DATABASE_TYPE: {
                return new DbConnMySQL(host, port, database, user, password, customProperties);
            }
            case DbConnMariaDB.DATABASE_TYPE: {
                return new DbConnMariaDB(host, port, database, user, password, customProperties);
            }
            case DbConnSQLServer.DATABASE_TYPE: {
                return new DbConnSQLServer(host, port, database, user, password, customProperties);
            }
            case DbConnPostgreSQL.DATABASE_TYPE: {
                return new DbConnPostgreSQL(host, port, database, user, password, customProperties);
            }
            case DbConnOracle.DATABASE_TYPE: {
                return new DbConnOracle(host, port, database, user, password, customProperties);
            }
            case DbConnCassandra.DATABASE_TYPE: {
                return new DbConnCassandra(host, port, database, user, password, customProperties);
            }
            default: {
                // should never happen
                throw new IllegalArgumentException("Database type " + dbType + " not supported");
            }
        }
    }

}
