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
package com.axway.ats.action;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import com.axway.ats.action.dbaccess.DatabaseOperations;
import com.axway.ats.action.dbaccess.model.DatabaseRow;
import com.axway.ats.agent.webapp.client.AgentServicePool;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.harness.config.TestBox;

/**
 * Utility class that can be used to check:
 * <ul>
 * <li>If some ATS Agent/Loader is the same ATS version is the executor
 * <li>If the ATS Log Database is the same version as the executor</li>
 * <li>If some ATS Agent is the same version as the ATS Log Database</li>
 * <li>If some ATS version is greater, equal or less than another one</li>
 * <ul>
 * */
@PublicAtsApi
public class AtsVersionVerifier {

    private static final Logger LOG = Logger.getLogger(AtsVersionVerifier.class);

    public enum Comparison {
        EQUAL(0), NEWER(1), OLDER(-1);

        private int value = 0;

        Comparison( int value ) {

            this.value = value;
        }
    }

    /**
     * Checks if both the executor's and ATS agent's version are the same
     * @param atsAgent - the address of the ATS Agent
     * @return true if versions are the same, false otherwise
     * */
    @PublicAtsApi
    public static boolean verifyExecutorAndAgentVersion( String atsAgent ) {

        LOG.info(String.format("Verify ATS version is EQUAL between local executor and ATS agent at %s ...", atsAgent));
        String executorVersion = getExecutorVersion();
        String agentVersion = getAgentVersion(atsAgent);

        boolean result = verifyVersion(executorVersion, agentVersion, Comparison.EQUAL);

        if (result) {
            LOG.info("SUCCESS! ATS versions are the same!");
        } else {
            LOG.error(String.format("FAIL! ATS versions are different! Executor version is [%s] , while Agent version is [%s]",
                                    executorVersion, agentVersion));
        }

        return result;

    }

    /**
     * Checks if both the executor's and ATS Log database's version are the same
     * @param box - box, containing DB connection properties
     * @param properties - (optional) DB connection properties
     * @return true if versions are the same, false otherwise
     * */
    @PublicAtsApi
    public static boolean verifyExecutorAndDatabaseVersion( TestBox box, Map<String, Object> properties ) {

        LOG.info(String.format("Verify ATS version is EQUAL between local executor and ATS Log DB at %s ...",
                               box.toString()));

        String executorVersion = getExecutorVersion();
        String dbVersion = getDatabaseVersion(box, properties);

        boolean result = verifyVersion(executorVersion, dbVersion, Comparison.EQUAL);

        if (result) {
            LOG.info("SUCCESS! ATS versions are the same!");
        } else {
            LOG.error(String.format("FAIL! ATS versions are different! Executor version is [%s] , while DB version is [%s]",
                                    executorVersion, dbVersion));
        }

        return result;

    }

    /**
     * Checks if both the provided ATS Agent's and ATS Log database's version are the same
     * @param atsAgent - the address of the ATS Agent
     * @param box - box, containing DB connection properties
     * @param properties - (optional) DB connection properties
     * @return true if versions are the same, false otherwise
     * */
    @PublicAtsApi
    public static boolean verifyAgentAndDatabaseVersion( String atsAgent, TestBox box,
                                                         Map<String, Object> properties ) {

        LOG.info(String.format("Verify ATS version is EQUAL between ATS Agent at %s and ATS Log DB at %s ...", atsAgent,
                               box.toString()));

        String agentVersion = getAgentVersion(atsAgent);
        String dbVersion = getDatabaseVersion(box, properties);
        boolean result = verifyVersion(agentVersion, dbVersion, Comparison.EQUAL);

        if (result) {
            LOG.info("SUCCESS! ATS versions are the same!");
        } else {
            LOG.error(String.format("FAIL! ATS versions are different! Agent version is [%s] , while DB version is [%s]",
                                    agentVersion, dbVersion));
        }

        return result;

    }

    /**
     * Perform a check between two ATS Versions
     * @param thisVersion
     * @param thatVersion
     * @param comparison
     * @return true if the evaluation succeeded, false otherwise
     * */
    @PublicAtsApi
    public static boolean verifyVersion( String thisVersion, String thatVersion, Comparison comparison ) {

        DefaultArtifactVersion thisVer = new DefaultArtifactVersion(thisVersion);
        DefaultArtifactVersion thatVer = new DefaultArtifactVersion(thatVersion);
        return thisVer.compareTo(thatVer) == comparison.value;
    }

    private static String getAgentVersion( String atsAgent ) {

        try {
            return AgentServicePool.getInstance().getClient(atsAgent).getAgentVersion();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not get ATS Version for ATS Agent at %s", atsAgent), e);
        }

    }

    private static String getExecutorVersion() {

        return AtsVersion.getAtsVersion();
    }

    private static String getDatabaseVersion( TestBox box, Map<String, Object> properties ) {

        DatabaseOperations dbOps = new DatabaseOperations(box, properties);

        try {
            String query = null;
            if (box.getDbType().equals(DbConnSQLServer.DATABASE_TYPE)) {
                query = "SELECT value from tInternal where [key] = 'version'";
            } else if (box.getDbType().equals(DbConnPostgreSQL.DATABASE_TYPE)) {
                query = "SELECT value from \"tInternal\" where key = 'version'";
            } else {
                throw new UnsupportedOperationException("Could not construct statement query for getting database version for connection of class '"
                                                        + box.getDbType() + "'");
            }
            DatabaseRow[] rows = dbOps.getDatabaseData(query);
            if (rows != null) {
                for (DatabaseRow row : rows) {
                    return row.getCellValue("version");
                }
            }
            throw new RuntimeException(String.format("SQL Select Query '%s' returned 0 (zero) results!", query));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not get ATS Version for ATS Log Database at %s%s",
                                                     box.toString(),
                                                     (properties != null && !properties.isEmpty())
                                                                                                   ? " with custom properties "
                                                                                                     + properties.toString()
                                                                                                   : ""),
                                       e);
        } finally {
            if (dbOps != null) {
                dbOps.disconnect();
            }
        }

    }

}
