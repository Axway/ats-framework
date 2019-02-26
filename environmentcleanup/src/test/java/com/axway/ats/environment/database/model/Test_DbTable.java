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
package com.axway.ats.environment.database.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.axway.ats.environment.BaseTest;
import com.axway.ats.environment.database.model.DbTable;

public class Test_DbTable extends BaseTest {

    @Test
    public void accessors() {

        DbTable dbTable = new DbTable("table1");

        assertEquals("table1", dbTable.getTableName());
        assertEquals(0, dbTable.getColumnsToExclude().size());

        List<String> columns = new ArrayList<String>();
        dbTable = new DbTable("table2", "dbo", columns);

        assertEquals("table2", dbTable.getTableName());
        assertEquals(columns, dbTable.getColumnsToExclude());
    }
}
