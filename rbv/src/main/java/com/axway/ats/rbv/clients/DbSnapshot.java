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

package com.axway.ats.rbv.clients;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.harness.config.TestBox;
import com.axway.ats.rbv.db.DbSearchTerm;
import com.axway.ats.rbv.db.rules.DbStringFieldRule;
import com.axway.ats.rbv.executors.SnapshotExecutor;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.Rule;

/**
 * Use this class's functionality is used to check updates to the database's
 * information. It uses a snapshot and checks both the updated values and the
 * old values (saved in this snapshot) that should not be changed.
 */
@PublicAtsApi
public class DbSnapshot extends DbVerification {

    /**
     * Create a DB verification component using the data provided
     * 
     * @param testBox   the test box
     * @param table     the table to search in
     * 
     * @throws RbvException thrown on error
     */
    @PublicAtsApi
    public DbSnapshot( TestBox testBox,
                       String table ) throws RbvException {

        this(testBox.getHost(),
             testBox.getDbPort(),
             testBox.getDbName(),
             testBox.getDbUser(),
             testBox.getDbPass(),
             new DbSearchTerm("SELECT * FROM " + table),
             testBox.getDbType());
    }

    /** Create a DB verification component using the data provided
     * 
     *  @param testBox   the test box
     * @param searchTerm the DbTerm which describes the SQL query used for retrieving the data
     * 
     * @throws RbvException thrown on error
     */
    @PublicAtsApi
    public DbSnapshot( TestBox testBox,
                       DbSearchTerm searchTerm ) throws RbvException {

        this(testBox.getHost(),
             testBox.getDbPort(),
             testBox.getDbName(),
             testBox.getDbUser(),
             testBox.getDbPass(),
             searchTerm,
             testBox.getDbType());
    }

    private DbSnapshot( String host,
                        int port,
                        String database,
                        String user,
                        String password,
                        DbSearchTerm searchTerm,
                        String dbType ) throws RbvException {

        super(host, port, database, user, password, searchTerm, dbType, new HashMap<String, Object>());

        this.folder.open();
        this.executor = new SnapshotExecutor(this.folder.getAllMetaData());
        this.folder.close();
    }

    DbSnapshot( DbSearchTerm searchTerm,
                DbProvider dbProvider ) throws RbvException {

        super(searchTerm, dbProvider);
    }

    /**
     * Exclude the provided {@link List} of keys from the snapshot verification
     * 
     * @param metaDataKeys {@link List} of {@link String} keys
     */
    @PublicAtsApi
    public void excludeKeys(
                             List<String> metaDataKeys ) {

        // add to list of excluded keys
        ((SnapshotExecutor) this.executor).excludeKeys(metaDataKeys);

    }

    /**
     * Exclude the provided key from the snapshot verification
     * 
     * @param metaDataKey {@link String} key to exclude
     */
    @PublicAtsApi
    public void excludeKey(
                            String metaDataKey ) {

        List<String> list = new ArrayList<String>();
        list.add(metaDataKey);
        ((SnapshotExecutor) this.executor).excludeKeys(list);

    }

    /**
     * Verify that a db record with the selected properties exists
     * 
     * @return true if the db record exists and has all the selected properties
     *         false if the db record does not exist or is missing any of the
     *         properties
     * @throws RbvException
     *             on error
     */
    @PublicAtsApi
    public boolean verifyDatabaseUpdated() throws RbvException {

        verifyObjectExists();

        //FIXME: change the method to void once everyone
        //removes their asserts

        return true;
    }

    /**
     * Check that the value of the given field is the same as the given one
     * 
     * @param tableName     the name of the table which the field is part of
     * @param fieldName     the field to check
     * @param value         the value expected
     */
    @PublicAtsApi
    public void checkFieldValueHasChanged(
                                           String keyTableName,
                                           String keyFieldName,
                                           String keyValue,
                                           String tableName,
                                           String fieldName,
                                           String value ) throws RbvException {

        DbStringFieldRule keyRule = new DbStringFieldRule(keyTableName,
                                                          keyFieldName,
                                                          keyValue,
                                                          DbStringFieldRule.MatchRelation.EQUALS,
                                                          "checkKeyValueEquals",
                                                          true);

        DbStringFieldRule matchingRule = new DbStringFieldRule(tableName,
                                                               fieldName,
                                                               value,
                                                               DbStringFieldRule.MatchRelation.EQUALS,
                                                               "checkFieldValueEquals",
                                                               true);
        matchingRule.setDbEncryptor(dbEncryptor);
        checkFieldValueChanged(keyRule, matchingRule);
    }

    private void checkFieldValueChanged(
                                         Rule keyRule,
                                         Rule matchingRule ) throws RbvException {

        ((SnapshotExecutor) this.executor).addRule(keyRule, matchingRule);
    }

    @Override
    protected String getMonitorName() {

        return "snapshot_" + super.getMonitorName();
    }
}
