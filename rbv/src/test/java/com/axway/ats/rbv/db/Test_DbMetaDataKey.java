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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.axway.ats.core.dbaccess.DbColumn;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.db.DbMetaDataKey;

public class Test_DbMetaDataKey extends BaseTest {

    @Test
    public void verifyConstructorDbColumn() {

        DbMetaDataKey metaKey;

        metaKey = new DbMetaDataKey( new DbColumn( "table", "column", 13 ) );
        assertEquals( metaKey.getTableName(), "table" );
        assertEquals( metaKey.getColumnName(), "column" );
        assertEquals( metaKey.getIndex(), 13 );
        assertEquals( metaKey.toString(), "table.column.13" );

        metaKey = new DbMetaDataKey( new DbColumn( "column", 5 ) );
        assertEquals( metaKey.getTableName(), "" );
        assertEquals( metaKey.getColumnName(), "column" );
        assertEquals( metaKey.getIndex(), 5 );
        assertEquals( metaKey.toString(), "column.5" );

        metaKey = new DbMetaDataKey( new DbColumn( "table", "column" ) );
        assertEquals( metaKey.getTableName(), "table" );
        assertEquals( metaKey.getColumnName(), "column" );
        assertEquals( metaKey.getIndex(), 0 );
        assertEquals( metaKey.toString(), "table.column" );
    }

    @Test
    public void verifyConstructorColumnOnly() {

        DbMetaDataKey metaKey;

        metaKey = new DbMetaDataKey( "column" );
        assertEquals( metaKey.getTableName(), "" );
        assertEquals( metaKey.getColumnName(), "column" );
        assertEquals( metaKey.getIndex(), 0 );
        assertEquals( metaKey.toString(), "column" );
    }

    @Test
    public void verifyConstructorTableAndColumn() {

        DbMetaDataKey metaKey;

        metaKey = new DbMetaDataKey( "table", "column" );
        assertEquals( metaKey.getTableName(), "table" );
        assertEquals( metaKey.getColumnName(), "column" );
        assertEquals( metaKey.getIndex(), 0 );
        assertEquals( metaKey.toString(), "table.column" );
    }

    @Test
    public void verifyConstructorColumnAndIndex() {

        DbMetaDataKey metaKey;

        metaKey = new DbMetaDataKey( "column", 15 );
        assertEquals( metaKey.getTableName(), "" );
        assertEquals( metaKey.getColumnName(), "column" );
        assertEquals( metaKey.getIndex(), 15 );
        assertEquals( metaKey.toString(), "column.15" );
    }

    @Test
    public void verifyConstructorTableColumnAndIndex() {

        DbMetaDataKey metaKey;

        metaKey = new DbMetaDataKey( "table", "column", 15 );
        assertEquals( metaKey.getTableName(), "table" );
        assertEquals( metaKey.getColumnName(), "column" );
        assertEquals( metaKey.getIndex(), 15 );
        assertEquals( metaKey.toString(), "table.column.15" );
    }

    @Test
    public void verifyConstructorNullTableColumnAndIndex() {

        DbMetaDataKey metaKey;

        metaKey = new DbMetaDataKey( null, "column", 15 );
        assertEquals( metaKey.getTableName(), null );
        assertEquals( metaKey.getColumnName(), "column" );
        assertEquals( metaKey.getIndex(), 15 );
        assertEquals( metaKey.toString(), "column.15" );
    }
}
