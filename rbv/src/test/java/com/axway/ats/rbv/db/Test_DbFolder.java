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

import java.util.List;

import org.junit.Test;

import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.db.DbSearchTerm;
import com.axway.ats.rbv.db.DbStorage;
import com.axway.ats.rbv.model.MatchableAlreadyOpenException;
import com.axway.ats.rbv.model.MatchableNotOpenException;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.storage.Matchable;

public class Test_DbFolder extends BaseTest {

    @Test
    public void open() throws RbvStorageException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.open();
    }

    @Test
    public void close() throws RbvStorageException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.open();
        folder.close();
    }

    @Test
    public void getAllMetaData() throws Exception {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.open();

        List<MetaData> metaData = folder.getAllMetaData();
        assertEquals( 2, metaData.size() );

        folder.close();
    }

    @Test
    public void getNewMetaData() throws Exception {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.open();

        List<MetaData> metaData = folder.getNewMetaData();
        assertEquals( 2, metaData.size() );

        folder.close();
    }

    @Test
    public void getMetaDataCounts() throws Exception {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.open();
        folder.getNewMetaData();
        assertEquals( "Total DB records: 2, new DB records: 2", folder.getMetaDataCounts() );
        folder.close();
    }

    @Test
    public void getMetaDataCountsSecondPolling() throws Exception {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.open();
        folder.getNewMetaData();
        folder.getNewMetaData();
        assertEquals( "Total DB records: 2, new DB records: 0", folder.getMetaDataCounts() );
        folder.close();
    }

    @Test
    public void getMetaDataCountsSecondPollingDataChanged() throws Exception {

        MockDbProvider dbProvider = new MockDbProvider();

        DbStorage storage = new DbStorage( dbProvider );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.open();
        folder.getNewMetaData();

        //change the meta data for one of the records
        dbProvider.incrementSeed();

        folder.getNewMetaData();
        assertEquals( "Total DB records: 2, new DB records: 2", folder.getMetaDataCounts() );
        folder.close();
    }

    @Test(expected = RbvStorageException.class)
    public void getMetaDataCountsBeforePolling() throws Exception {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.open();
        folder.getMetaDataCounts();
    }

    @Test
    public void getDescription() throws RbvException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.getDescription();
    }

    @Test(expected = MatchableNotOpenException.class)
    public void getAllMetaDataNegativeFolderNotOpen() throws RbvException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.getAllMetaData();
    }

    @Test(expected = MatchableNotOpenException.class)
    public void getNewMetaDataNegativeFolderNotOpen() throws RbvException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.getNewMetaData();
    }

    @Test(expected = MatchableNotOpenException.class)
    public void closeNegativeFolderNotOpen() throws RbvStorageException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.close();
    }

    @Test(expected = MatchableNotOpenException.class)
    public void getMetaDataCountsNegativeFolderNotOpen() throws RbvStorageException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.getMetaDataCounts();
    }

    @Test(expected = MatchableAlreadyOpenException.class)
    public void openNegativeFolderAlreadyOpen() throws RbvStorageException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        Matchable folder = storage.getFolder( new DbSearchTerm( "" ) );

        folder.open();
        folder.open();
    }
}
