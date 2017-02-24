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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.db.DbSearchTerm;
import com.axway.ats.rbv.db.DbStorage;
import com.axway.ats.rbv.model.RbvStorageException;

public class Test_DbStorage extends BaseTest {

    @Test
    public void getFolderWithSearchTerm() throws RbvStorageException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        assertNotNull( storage.getFolder( new DbSearchTerm( "" ) ) );
    }

    @Test(expected = RbvStorageException.class)
    public void getFolderNullSearchTerm() throws RbvStorageException {

        DbStorage storage = new DbStorage( new MockDbProvider() );
        storage.getFolder( null );
    }
}
