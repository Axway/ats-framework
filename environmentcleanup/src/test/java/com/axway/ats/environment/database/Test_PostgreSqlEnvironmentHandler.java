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

import com.axway.ats.environment.BaseTest;
import com.axway.ats.environment.database.model.DbTable;
import org.junit.Assert;
import org.junit.Test;


public class Test_PostgreSqlEnvironmentHandler extends BaseTest {

    @Test
    public void testFullTableNameGeneration() {

        final String tableName = "table1";
        final String schemaName = "schema1";
        DbTable dbT = new DbTable(tableName, schemaName);
        final String expectedFullTAbleName = "\"" + schemaName + "\".\"" + tableName + "\"";

        // no quotes - put to schema and table
        String fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected escaping", expectedFullTAbleName, fullNameGenerated);

        // no schema specified - put "public". Table w/o quotes specified
        dbT = new DbTable(tableName);
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected public in front", "\"public\".\"" + tableName + "\"", fullNameGenerated);

        // no schema specified - put "public". Table with quotes specified
        dbT = new DbTable("\"" + tableName + "\"");
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected public in front", "\"public\".\"" + tableName + "\"", fullNameGenerated);

        // no schema quotes
        dbT = new DbTable( "\"" + tableName + "\"", schemaName);
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected schema quoting", expectedFullTAbleName, fullNameGenerated);

        // no table quotes
        dbT = new DbTable(  tableName , "\"" + schemaName + "\"");
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected talbe quoting", expectedFullTAbleName, fullNameGenerated);


        // no table leading quote
        dbT = new DbTable(  tableName + "\"", schemaName);
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected table partial escaping", expectedFullTAbleName, fullNameGenerated);

        // no table ending quote
        dbT = new DbTable("\"" + tableName, schemaName);
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected table partial escaping", expectedFullTAbleName, fullNameGenerated);

        // no schema leading quote
        dbT = new DbTable(  tableName , schemaName + "\"");
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected schema partial escaping", expectedFullTAbleName, fullNameGenerated);

        // no schema ending quote
        dbT = new DbTable(tableName, "\"" + schemaName);
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected schema partial escaping", expectedFullTAbleName, fullNameGenerated);

        // no table and schema ending quotes
        dbT = new DbTable("\"" + tableName, "\"" + schemaName);
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected table and schema partial escaping", expectedFullTAbleName, fullNameGenerated);

        // no table and schema ending quotes
        dbT = new DbTable( tableName + "\"",  schemaName +  "\"");
        fullNameGenerated = PostgreSqlEnvironmentHandler.getFullTableName(dbT);
        Assert.assertEquals("Expected table and schema partial escaping", expectedFullTAbleName, fullNameGenerated);

    }

}
