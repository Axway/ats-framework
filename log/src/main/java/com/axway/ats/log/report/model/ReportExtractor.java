/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.log.report.model;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.io.SQLServerDbReadAccess;
import com.axway.ats.log.report.exceptions.ReportExtractorException;

/**
 * Extracts runs from provided database host and name
 */
public class ReportExtractor {

    private SQLServerDbReadAccess dbReadAccess;

    /**
     * @param dbHost the database host to get runs from
     * @param dbName the database name to get runs from
     * @param dbUser the user name used for authentication to the database
     * @param dbPassword the user password used for authentication to the database
     */
    public ReportExtractor( String dbHost,
                            String dbName,
                            String dbUser,
                            String dbPassword ) {

        DbConnSQLServer dbConnection = new DbConnSQLServer(dbHost, dbName, dbUser, dbPassword);
        dbReadAccess = new SQLServerDbReadAccess(dbConnection);
    }

    /**
     * extract the specified runs
     * 
     * @param runIds the ids of the runs to extract
     * @return the runs
     */
    public List<RunWrapper> extract(
                                     int[] runIds ) {

        if (runIds.length == 0) {
            final String errMsg = "You need to specify at least one run id!";
            throw new ReportExtractorException(errMsg);
        }

        return extractRunEntities(runIds);
    }

    /**
     * extract the runs
     * 
     * @param runIds
     * @return
     */
    private List<RunWrapper> extractRunEntities(
                                                 int[] runIds ) {

        StringBuilder whereClause = new StringBuilder();
        whereClause.append(" WHERE runId IN (");
        for (int runId : runIds) {
            whereClause.append(runId).append(", ");
        }
        whereClause.setLength(whereClause.lastIndexOf(","));
        whereClause.append(")");

        List<Run> dbRuns;
        try {
            dbRuns = dbReadAccess.getRuns(0, 10000, whereClause.toString(), "runId", true, 0);
        } catch (DatabaseAccessException e) {
            throw new ReportExtractorException("Error loading runs " + whereClause, e);
        }

        List<RunWrapper> runs = new ArrayList<RunWrapper>();
        for (Run dbRun : dbRuns) {

            // load this run's suites
            List<Suite> suites;
            try {
                suites = dbReadAccess.getSuites(0,
                                                10000,
                                                "WHERE runId=" + dbRun.runId,
                                                "suiteId",
                                                true, 0);
            } catch (DatabaseAccessException e) {
                throw new ReportExtractorException("Error loading suites for run with id " + dbRun.runId,
                                                   e);
            }

            RunWrapper run = new RunWrapper(dbRun, suites);
            runs.add(run);
        }

        return runs;
    }
}
