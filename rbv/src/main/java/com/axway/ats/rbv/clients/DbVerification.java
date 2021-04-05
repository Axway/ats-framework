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
package com.axway.ats.rbv.clients;

import java.util.Date;
import java.util.Map;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.common.dbaccess.OracleKeys;
import com.axway.ats.core.dbaccess.DbProvider;
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
import com.axway.ats.harness.config.TestBox;
import com.axway.ats.rbv.db.DbEncryptor;
import com.axway.ats.rbv.db.DbSearchTerm;
import com.axway.ats.rbv.db.DbStorage;
import com.axway.ats.rbv.db.rules.DbBinaryFieldRule;
import com.axway.ats.rbv.db.rules.DbBooleanFieldRule;
import com.axway.ats.rbv.db.rules.DbDateFieldRule;
import com.axway.ats.rbv.db.rules.DbDateFieldRule.MatchRelation;
import com.axway.ats.rbv.db.rules.DbFieldsRule;
import com.axway.ats.rbv.db.rules.DbNumericFieldRule;
import com.axway.ats.rbv.db.rules.DbStringFieldRule;
import com.axway.ats.rbv.executors.MetaExecutor;
import com.axway.ats.rbv.model.RbvException;

/**
 * Class used for base DB verifications
 * <p>Note that <code>check</code> methods add rules for match whereas actual rules
 * evaluation is done in one of the methods with <code>verify</code> prefix.</p>
 *
 * <br><br>
 * <b>User guide</b> pages related to this class:<br>
 * <a href="https://axway.github.io/ats-framework/Common-test-verifications.html">RBV basics</a>
 * and
 * <a href="https://axway.github.io/ats-framework/Database-verifications.html">Database verification details</a>
 */
@PublicAtsApi
public class DbVerification extends VerificationSkeleton {

    private String        host;
    // A custom encryption provider interface
    protected DbEncryptor dbEncryptor;

    /**
     * Create a DB verification component using the data provided
     *
     * @param testBox the test box
     * @param table   the table to search in
     * @throws RbvException thrown on error
     */
    @PublicAtsApi
    public DbVerification( TestBox testBox,
                           String table ) throws RbvException {

        this(testBox.getHost(),
             testBox.getDbPort(),
             testBox.getDbName(),
             testBox.getDbUser(),
             testBox.getDbPass(),
             new DbSearchTerm("SELECT * FROM " + table),
             testBox.getDbType(),
             testBox.getProperties());
    }

    /**
     * Create a DB verification component using the data provided
     *
     * @param testBox    the test box
     * @param searchTerm the DbTerm which describes the SQL query used for retrieving the data
     * @throws RbvException thrown on error
     */
    @PublicAtsApi
    public DbVerification( TestBox testBox,
                           DbSearchTerm searchTerm ) throws RbvException {

        this(testBox.getHost(),
             testBox.getDbPort(),
             testBox.getDbName(),
             testBox.getDbUser(),
             testBox.getDbPass(),
             searchTerm,
             testBox.getDbType(),
             testBox.getProperties());
    }

    protected DbVerification( String host,
                              int port,
                              String database,
                              String user,
                              String password,
                              DbSearchTerm searchTerm,
                              String dbType,
                              Map<String, Object> customProperties ) throws RbvException {

        super();

        this.host = host;

        // avoid the possible NullPointerException
        if (dbType == null) {
            throw new RbvException("Database type not specified for host " + host);
        }
        DbProvider dbProvider;
        if (port != TestBox.DB_PORT_NOT_SPECIFIED) { // add custom port
            customProperties.put(DbKeys.PORT_KEY, Integer.valueOf(port));
        }
        switch (dbType) {
            case DbConnMySQL.DATABASE_TYPE:
                dbProvider = new MysqlDbProvider(new DbConnMySQL(host,
                                                                 database,
                                                                 user,
                                                                 password,
                                                                 customProperties));
                break;
            case DbConnMariaDB.DATABASE_TYPE:
                dbProvider = new MariaDbDbProvider(new DbConnMariaDB(host,
                                                                     database,
                                                                     user,
                                                                     password,
                                                                     customProperties));
                break;
            case DbConnSQLServer.DATABASE_TYPE:
                dbProvider = new MssqlDbProvider(new DbConnSQLServer(host,
                                                                     database,
                                                                     user,
                                                                     password,
                                                                     customProperties));
                break;
            case DbConnOracle.DATABASE_TYPE:
                Object dbSid = customProperties.get("dbsid");
                if (dbSid != null) {
                    customProperties.put(OracleKeys.SID_KEY, dbSid);
                }
                Object dbServiceName = customProperties.get("dbserviceName");
                if (dbServiceName != null) {
                    customProperties.put(OracleKeys.SERVICE_NAME_KEY, dbServiceName);
                }
                dbProvider = new OracleDbProvider(new DbConnOracle(host,
                                                                   database,
                                                                   user,
                                                                   password,
                                                                   customProperties));
                break;
            case DbConnPostgreSQL.DATABASE_TYPE:
                dbProvider = new PostgreSqlDbProvider(
                                                      new DbConnPostgreSQL(host, port, database, user, password,
                                                                           customProperties));
                break;
            default:
                throw new RbvException("DB Provider '" + dbType + "' not supported!");
        }

        DbStorage storage = new DbStorage(dbProvider);
        folder = storage.getFolder(searchTerm);
        this.executor = new MetaExecutor();
    }

    DbVerification( DbSearchTerm searchTerm,
                    DbProvider dbProvider ) throws RbvException {

        super();

        DbStorage storage = new DbStorage(dbProvider);
        folder = storage.getFolder(searchTerm);
        this.executor = new MetaExecutor();
    }

    /**
     * Specify a custom encryption interface.
     *
     * @param dbEncryptor
     */
    @PublicAtsApi
    public void setDbEncryptor(
                                DbEncryptor dbEncryptor ) {

        this.dbEncryptor = dbEncryptor;
    }

    /**
     * Add rule to check that the value of the given field is the same as the given one
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the value expected (string)
     */
    @PublicAtsApi
    public void checkFieldValueEquals(
                                       String tableName,
                                       String fieldName,
                                       String value ) {

        DbStringFieldRule matchingRule = new DbStringFieldRule(tableName,
                                                               fieldName,
                                                               value,
                                                               DbStringFieldRule.MatchRelation.EQUALS,
                                                               "checkFieldValueEquals",
                                                               true);
        matchingRule.setDbEncryptor(dbEncryptor);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field is the same as the given one
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the value expected (boolean)
     */
    @PublicAtsApi
    public void checkFieldValueEquals(
                                       String tableName,
                                       String fieldName,
                                       boolean value ) {

        DbBooleanFieldRule matchingRule = new DbBooleanFieldRule(tableName,
                                                                 fieldName,
                                                                 value,
                                                                 "checkFieldValueEquals",
                                                                 true);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field is the same as the given one
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the value expected (Date)
     */
    @PublicAtsApi
    public void checkFieldValueEquals(
                                       String tableName,
                                       String fieldName,
                                       Date value ) {

        DbDateFieldRule matchingRule = new DbDateFieldRule(tableName,
                                                           fieldName,
                                                           value,
                                                           MatchRelation.EXACT,
                                                           "checkFieldValueEquals",
                                                           true);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field is the same as the given one
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the value expected (numeric)
     */
    @PublicAtsApi
    public void checkFieldValueEquals(
                                       String tableName,
                                       String fieldName,
                                       Number value ) {

        DbNumericFieldRule matchingRule = new DbNumericFieldRule(tableName,
                                                                 fieldName,
                                                                 value,
                                                                 "checkFieldValueEquals",
                                                                 true);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field is the same as the given one
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the value expected (binary)
     */
    @PublicAtsApi
    public void checkFieldValueEquals(
                                       String tableName,
                                       String fieldName,
                                       byte[] value ) {

        DbBinaryFieldRule matchingRule = new DbBinaryFieldRule(tableName,
                                                               fieldName,
                                                               value,
                                                               "checkFieldValueEquals",
                                                               true);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field is not the same as the given one
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the value expected (string)
     */
    @PublicAtsApi
    public void checkFieldValueDoesNotEqual(
                                             String tableName,
                                             String fieldName,
                                             String value ) {

        DbStringFieldRule matchingRule = new DbStringFieldRule(tableName,
                                                               fieldName,
                                                               value,
                                                               DbStringFieldRule.MatchRelation.EQUALS,
                                                               "checkFieldValueDoesNotEqual",
                                                               false);
        matchingRule.setDbEncryptor(dbEncryptor);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field is not the same as the given one
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the value expected (numeric)
     */
    @PublicAtsApi
    public void checkFieldValueDoesNotEqual(
                                             String tableName,
                                             String fieldName,
                                             Number value ) {

        DbNumericFieldRule matchingRule = new DbNumericFieldRule(tableName,
                                                                 fieldName,
                                                                 value,
                                                                 "checkFieldValueDoesNotEqual",
                                                                 false);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field is not the same as the given one
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the value expected (binary)
     */
    @PublicAtsApi
    public void checkFieldValueDoesNotEqual(
                                             String tableName,
                                             String fieldName,
                                             byte[] value ) {

        DbBinaryFieldRule matchingRule = new DbBinaryFieldRule(tableName,
                                                               fieldName,
                                                               value,
                                                               "checkFieldValueDoesNotEqual",
                                                               false);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field is matched by the given regular expressions
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param regex
     */
    @PublicAtsApi
    public void checkFieldValueRegex(
                                      String tableName,
                                      String fieldName,
                                      String regex ) {

        DbStringFieldRule matchingRule = new DbStringFieldRule(tableName,
                                                               fieldName,
                                                               regex,
                                                               DbStringFieldRule.MatchRelation.REGEX_MATCH,
                                                               "checkFieldValueRegex",
                                                               true);
        matchingRule.setDbEncryptor(dbEncryptor);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field is not matched by the given regular expressions
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param regex
     */
    @PublicAtsApi
    public void checkFieldValueRegexDoesNotMatch(
                                                  String tableName,
                                                  String fieldName,
                                                  String regex ) {

        DbStringFieldRule matchingRule = new DbStringFieldRule(tableName,
                                                               fieldName,
                                                               regex,
                                                               DbStringFieldRule.MatchRelation.REGEX_MATCH,
                                                               "checkFieldValueRegexDoesNotMatch",
                                                               false);
        matchingRule.setDbEncryptor(dbEncryptor);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field contains the given string
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the string that should be contained in the field
     */
    @PublicAtsApi
    public void checkFieldValueContains(
                                         String tableName,
                                         String fieldName,
                                         String value ) {

        DbStringFieldRule matchingRule = new DbStringFieldRule(tableName,
                                                               fieldName,
                                                               value,
                                                               DbStringFieldRule.MatchRelation.CONTAINS,
                                                               "checkFieldValueContains",
                                                               true);
        matchingRule.setDbEncryptor(dbEncryptor);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the value of the given field does not contain the given string
     *
     * @param tableName the name of the table which the field is part of
     * @param fieldName the field to check
     * @param value     the string that should be contained in the field
     */
    @PublicAtsApi
    public void checkFieldValueDoesNotContain(
                                               String tableName,
                                               String fieldName,
                                               String value ) {

        DbStringFieldRule matchingRule = new DbStringFieldRule(tableName,
                                                               fieldName,
                                                               value,
                                                               DbStringFieldRule.MatchRelation.CONTAINS,
                                                               "checkFieldValueDoesNotContain",
                                                               false);
        matchingRule.setDbEncryptor(dbEncryptor);
        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the given timestamp is before the date contained in the given field
     *
     * @param tableName   the name of the table which the field is part of
     * @param fieldName   the field to check
     * @param timestamp   the expected timestamp (UNIX timestamp format)
     * @param datePattern the pattern in which the date is stored in the field - see <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html">Java date patterns</a>
     */
    @PublicAtsApi
    public void checkFieldValueDateBefore(
                                           String tableName,
                                           String fieldName,
                                           long timestamp,
                                           String datePattern ) {

        DbDateFieldRule matchingRule = new DbDateFieldRule(tableName,
                                                           fieldName,
                                                           Long.toString(timestamp),
                                                           DbDateFieldRule.MatchRelation.BEFORE_DATE,
                                                           datePattern,
                                                           "checkFieldValueDateBefore",
                                                           true);

        checkFieldValue(matchingRule);
    }

    /**
     * Add rule to check that the given timestamp is after the date contained in the given field
     *
     * @param tableName   the name of the table which the field is part of
     * @param fieldName   the field to check
     * @param timestamp   the expected timestamp (UNIX timestamp format)
     * @param datePattern the pattern in which the date is stored in the field - see <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html">Java date patterns</a>
     */
    @PublicAtsApi
    public void checkFieldValueDateAfter(
                                          String tableName,
                                          String fieldName,
                                          long timestamp,
                                          String datePattern ) {

        DbDateFieldRule matchingRule = new DbDateFieldRule(tableName,
                                                           fieldName,
                                                           Long.toString(timestamp),
                                                           DbDateFieldRule.MatchRelation.AFTER_DATE,
                                                           datePattern,
                                                           "checkFieldValueDateAfter",
                                                           true);
        checkFieldValue(matchingRule);
    }

    private void checkFieldValue(
                                  DbFieldsRule matchingRule ) {

        rootRule.addRule(matchingRule);
    }

    /**
     * Verify a DB record with the selected properties exists. All check rules
     * added before are evaluated and should pass.
     *
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     *
     * @throws RbvException if at least one check rule added fails
     */
    @PublicAtsApi
    public void verifyDbDataExists() throws RbvException {

        verifyExists();
    }

    /**
     * Verify a DB record with the selected properties does not exist.
     *
     * <li> At the first successful poll - it will succeed
     * <li> At unsuccessful poll - it will retry until all polling attempts are over
     *
     * @throws RbvException if at least one check rule added passes
     */
    @PublicAtsApi
    public void verifyDbDataDoesNotExist() throws RbvException {

        verifyDoesNotExist();
    }

    /**
     * Verify a DB record with the selected properties (all check rules) exists
     * for the whole polling duration
     *
     * <li> At successful poll - it will retry until all polling attempts are over
     * <li> At unsuccessful poll - it will fail
     *
     * @throws RbvException
     */
    @PublicAtsApi
    public void verifyDbDataAlwaysExists() throws RbvException {

        verifyAlwaysExists();
    }

    /**
     * Verify a DB record with the selected properties does not exist for the whole polling duration
     *
     * <li> At successful poll - it will retry until all polling attempts are over
     * <li> At unsuccessful poll - it will fail
     *
     * @throws RbvException
     */
    @PublicAtsApi
    public void verifyDbDataNeverExists() throws RbvException {

        verifyNeverExists();
    }

    @Override
    protected String getMonitorName() {

        return "db_monitor_" + host;
    }
}
