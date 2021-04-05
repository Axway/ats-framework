/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.environment.database;

import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.cassandra.CassandraDbProvider;
import com.axway.ats.core.dbaccess.cassandra.DbConnCassandra;
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
import com.axway.ats.environment.database.exceptions.DatabaseEnvironmentCleanupException;
import com.axway.ats.environment.database.model.BackupHandler;
import com.axway.ats.environment.database.model.RestoreHandler;

/**
 * This factory takes care to provide the correct implementation of the
 * {@link BackupHandler} and {@link RestoreHandler} interfaces, depending
 * on the type of database. The database type is passed as a string
 * parameter to the factory methods.
 */
public class EnvironmentHandlerFactory {

    private static EnvironmentHandlerFactory instance;

    private EnvironmentHandlerFactory() {

        // Empty default constructor, required so that the factory be a singleton
    }

    /**
     * Returns the instance of the factory, since this factory is supposed
     * to be a singleton there may be only one instance of it created.
     *
     * @return the instance of the {@link EnvironmentHandlerFactory}
     */
    public synchronized static EnvironmentHandlerFactory getInstance() {

        if (instance == null) {
            instance = new EnvironmentHandlerFactory();
        }

        return instance;
    }

    /**
     * Returns a new instance of the type {@link BackupHandler}, this method
     * takes care of instantiating the proper type of {@link BackupHandler}
     * depending on the parameters passed
     *
     * @param dbConnection the database connection to use
     * @return a new instance of the {@link BackupHandler}
     *
     * @throws DatabaseEnvironmentCleanupException
     */
    public BackupHandler createDbBackupHandler(
                                                DbConnection dbConnection ) {

        switch (dbConnection.getDbType()) {
            case DbConnMySQL.DATABASE_TYPE: {
                DbConnMySQL mysqlConnection = (DbConnMySQL) dbConnection;
                return new MysqlEnvironmentHandler(mysqlConnection, new MysqlDbProvider(mysqlConnection));
            }
            case DbConnMariaDB.DATABASE_TYPE: {
                DbConnMariaDB mariaDbConnection = (DbConnMariaDB) dbConnection;
                return new MariaDbEnvironmentHandler(mariaDbConnection, new MariaDbDbProvider(mariaDbConnection));
            }
            case DbConnOracle.DATABASE_TYPE: {
                DbConnOracle oracleConnection = (DbConnOracle) dbConnection;
                return new OracleEnvironmentHandler(oracleConnection,
                                                    new OracleDbProvider(oracleConnection));
            }
            case DbConnSQLServer.DATABASE_TYPE: {
                DbConnSQLServer mssqlConnection = (DbConnSQLServer) dbConnection;
                return new MssqlEnvironmentHandler(mssqlConnection, new MssqlDbProvider(mssqlConnection));
            }
            case DbConnPostgreSQL.DATABASE_TYPE: {
                DbConnPostgreSQL dbConnPostgreSQL = (DbConnPostgreSQL) dbConnection;
                return new PostgreSqlEnvironmentHandler(dbConnPostgreSQL, new PostgreSqlDbProvider(dbConnPostgreSQL));
            }
            case DbConnCassandra.DATABASE_TYPE: {
                DbConnCassandra cassandraConnection = (DbConnCassandra) dbConnection;
                return new CassandraEnvironmentHandler(cassandraConnection,
                                                       new CassandraDbProvider(cassandraConnection));
            }
            default: {
                //should never happen
                throw new IllegalArgumentException(dbConnection.getDbType() + " connections not supported");
            }
        }
    }

    /**
     * Returns a new instance of the type {@link RestoreHandler}, this method
     * takes care of instantiating the proper type of {@link RestoreHandler}
     * depending on the parameters passed
     *
     * @param dbConnection the database connection
     * @return a new instance of the {@link RestoreHandler}
     *
     * @throws DatabaseEnvironmentCleanupException
     */
    public RestoreHandler createDbRestoreHandler(
                                                  DbConnection dbConnection ) {

        switch (dbConnection.getDbType()) {
            case DbConnMySQL.DATABASE_TYPE: {
                DbConnMySQL mysqlConnection = (DbConnMySQL) dbConnection;
                return new MysqlEnvironmentHandler(mysqlConnection, new MysqlDbProvider(mysqlConnection));
            }
            case DbConnMariaDB.DATABASE_TYPE: {
                DbConnMariaDB mariaDbConnection = (DbConnMariaDB) dbConnection;
                return new MariaDbEnvironmentHandler(mariaDbConnection, new MariaDbDbProvider(mariaDbConnection));
            }
            case DbConnOracle.DATABASE_TYPE: {
                DbConnOracle oracleConnection = (DbConnOracle) dbConnection;
                return new OracleEnvironmentHandler(oracleConnection,
                                                    new OracleDbProvider(oracleConnection));
            }
            case DbConnSQLServer.DATABASE_TYPE: {
                DbConnSQLServer mssqlConnection = (DbConnSQLServer) dbConnection;
                return new MssqlEnvironmentHandler(mssqlConnection, new MssqlDbProvider(mssqlConnection));
            }
            case DbConnPostgreSQL.DATABASE_TYPE: {
                DbConnPostgreSQL dbConnPostgreSQL = (DbConnPostgreSQL) dbConnection;
                return new PostgreSqlEnvironmentHandler(dbConnPostgreSQL, new PostgreSqlDbProvider(dbConnPostgreSQL));
            }

            case DbConnCassandra.DATABASE_TYPE: {
                DbConnCassandra cassandraConnection = (DbConnCassandra) dbConnection;
                return new CassandraEnvironmentHandler(cassandraConnection,
                                                       new CassandraDbProvider(cassandraConnection));
            }
            default: {
                //should never happen
                throw new IllegalArgumentException(dbConnection.getDbType() + " connections not supported");
            }
        }
    }
}
