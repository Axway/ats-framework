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
package com.axway.ats.rbv.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.dbaccess.DbQuery;
import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.core.dbaccess.DbRecordValue;
import com.axway.ats.core.dbaccess.DbRecordValuesList;
import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.MatchableAlreadyOpenException;
import com.axway.ats.rbv.model.MatchableNotOpenException;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.storage.Matchable;

public class DbFolder implements Matchable {

    private static Logger             log = LogManager.getLogger(DbFolder.class);

    private DbQuery                   searchQuery;
    private DbProvider                dbProvider;

    private boolean                   isOpen;
    private boolean                   didPollingOccured;
    private HashMap<String, MetaData> allMetaDataMap;
    private HashMap<String, MetaData> newMetaDataMap;

    DbFolder( DbSearchTerm searchTerm,
              DbProvider dbProvider ) {

        this.isOpen = false;

        this.searchQuery = searchTerm.getDbQuery();
        this.dbProvider = dbProvider;

        this.allMetaDataMap = new HashMap<String, MetaData>();
        this.newMetaDataMap = new HashMap<String, MetaData>();
    }

    public void open() throws RbvStorageException {

        //first check if the folder is already open
        if (isOpen) {
            throw new MatchableAlreadyOpenException("DB folder is already open");
        }

        allMetaDataMap = new HashMap<String, MetaData>();
        newMetaDataMap = new HashMap<String, MetaData>();
        isOpen = true;
        didPollingOccured = false;

        log.debug("Opened " + getDescription());
    }

    public void close() throws RbvStorageException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("DB folder is not open");
        }

        log.debug("Closed " + getDescription());
        isOpen = false;
    }

    public List<MetaData> getAllMetaData() throws RbvException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("DB folder is not open");
        }

        allMetaDataMap.clear();

        List<MetaData> metaDataValues = new ArrayList<MetaData>();
        refresh();

        metaDataValues.addAll(allMetaDataMap.values());
        return metaDataValues;
    }

    public List<MetaData> getNewMetaData() throws RbvException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("DB folder is not open");
        }

        List<MetaData> metaDataValues = new ArrayList<MetaData>();
        refresh();

        metaDataValues.addAll(newMetaDataMap.values());
        return metaDataValues;
    }

    private void refresh() throws RbvException {

        newMetaDataMap = new HashMap<String, MetaData>();

        //store the current meta data map and clear the map holding all meta data
        //this way we will be able to detect any changes including added and removed
        //meta data
        HashMap<String, MetaData> oldMetaDataMap = allMetaDataMap;
        allMetaDataMap = new HashMap<String, MetaData>();

        log.debug("Run DB query '" + this.searchQuery.getQuery() + "'");

        DbRecordValuesList[] queryResults;
        try {
            queryResults = dbProvider.select(this.searchQuery);
        } catch (DbException dbe) {
            throw new RbvException(dbe);
        }

        if (queryResults != null) {
            for (DbRecordValuesList queryResult : queryResults) {
                DbMetaData currentData = new DbMetaData();
                StringBuffer metaDataHash = new StringBuffer();

                for (DbRecordValue recordValue : queryResult) {
                    DbMetaDataKey key = new DbMetaDataKey(recordValue.getDbColumn());
                    Object value = recordValue.getValue();

                    currentData.putProperty(key.toString(), value);

                    //calculate the hash
                    metaDataHash.append(key.toString());
                    metaDataHash.append(recordValue.getValueAsString());
                }

                try {
                    //compute MD5 so we don't keep the whole StringBuffer in memory
                    MessageDigest metaDataHashDigest = MessageDigest.getInstance("MD5");
                    String metaDataSum = new String(metaDataHashDigest.digest(metaDataHash.toString()
                                                                                          .getBytes()));

                    if (!oldMetaDataMap.containsKey(metaDataSum)) {
                        newMetaDataMap.put(metaDataSum, currentData);
                    }

                    //always put the record in the map holding all meta data
                    allMetaDataMap.put(metaDataSum, currentData);

                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }

            didPollingOccured = true;
        }
    }

    public String getMetaDataCounts() throws RbvStorageException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("DB folder is not open");
        }

        if (!didPollingOccured) {
            throw new RbvStorageException("DbFolder.getMetaDataCounts() called before any polling");
        }

        return "Total DB records: " + allMetaDataMap.size() + ", new DB records: " + newMetaDataMap.size();
    }

    public String getDescription() {

        return "DB data with " + dbProvider.getDbConnection().getDescription();
    }
}
