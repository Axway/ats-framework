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
package com.axway.ats.common.dbaccess.snapshot;

import com.axway.ats.common.PublicAtsApi;

/**
 * User can implement this interface in order to tell
 * whether some table index names should be treated as
 * same or not.
 * 
 * It's been found that on MS SQL the index names contain
 * some randomly generated suffix. For example "... __3213E83F43E2A1B9".
 * 
 * Using this interface you can point such index names as same as long
 * as the leading name part is the same.
 */
@PublicAtsApi
public interface IndexNameMatcher {

    /**
     * Whether some table index names should be treated as same or not
     *  
     * @param table table name
     * @param firstName first index name
     * @param secondName second index name
     * @return
     */
    @PublicAtsApi
    public boolean isSame( String table, String firstName, String secondName );
}
