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
package com.axway.ats.environment.database.mysql;

import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import com.axway.ats.core.dbaccess.exceptions.DbException;
import com.axway.ats.environment.BaseTest;

public class Test_MysqlColumnNames extends BaseTest {

    @Test
    public void getNameJDBC4() throws SQLException, DbException {

        Assert.assertEquals("COLUMN_NAME", MysqlColumnNames.COLUMN_NAME.getName(true));
    }

    @Test
    public void getNameJDBC3() throws SQLException, DbException {

        Assert.assertEquals("Field", MysqlColumnNames.COLUMN_NAME.getName(false));
    }

    @Test
    public void getNameJDBC4Type() throws SQLException, DbException {

        Assert.assertEquals("COLUMN_TYPE", MysqlColumnNames.COLUMN_TYPE.getName(true));
    }

    @Test
    public void getNameJDBC3Type() throws SQLException, DbException {

        Assert.assertEquals("Type", MysqlColumnNames.COLUMN_TYPE.getName(false));
    }

    @Test
    public void getNameJDBC4Default() throws SQLException, DbException {

        Assert.assertEquals("COLUMN_DEFAULT", MysqlColumnNames.DEFAULT_COLUMN.getName(true));
    }

    @Test
    public void getNameJDBC3Default() throws SQLException, DbException {

        Assert.assertEquals("Default", MysqlColumnNames.DEFAULT_COLUMN.getName(false));
    }

}
