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

package com.axway.ats.rbv.s3;

import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.storage.Matchable;
import com.axway.ats.rbv.storage.SearchTerm;
import com.axway.ats.rbv.storage.Storage;

public class S3Storage implements Storage {

    @Override
    public Matchable getFolder( SearchTerm searchTerm ) throws RbvStorageException {

        if( searchTerm == null ) {
            throw new RbvStorageException( "Search term is null" );
        }

        if( ! ( searchTerm instanceof S3SearchTerm ) ) {
            throw new RbvStorageException( "Class " + searchTerm.getClass().getSimpleName()
                                           + " is not valid S3 search term" );
        }

        S3SearchTerm s3SearchTerm = ( S3SearchTerm ) searchTerm;

        return new S3Folder( s3SearchTerm );
    }

}
