/*
 * Copyright 2017-2020 Axway Software
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

import java.util.Properties;

import com.axway.ats.common.PublicAtsApi;

/**
 * User can implement this interface in order to tell<br>
 * whether some table index names should be treated as<br>
 * same or not.<br>
 * <br><br>
 * It's been found that on MS SQL/ Oracle the index names contain<br>
 * some randomly generated suffix. For example "... __3213E83F43E2A1B9".<br>
 * <br><br>
 * Using this interface you can point such index names as same as long<br>
 * as the leading name part is the same.<br>
 * <br><br>
 * Or if the name is not enough, you can compare each of the index's properties and determine if those indexes are the same one
 * 
 * The order of index  matching is:
 * 1. Check only the indexes names ( isSame( String table, String firstName, String secondName ) ) and if there is a match, consider the indexes to be the same.
 * 2. If the name-only check is false, use the properties-based check ( isSame( String table, Properties firstProperties, Properties secondProperties ) ) for final result.
 * 
 * This means, that when you are implementing your own IndexMatcher and you are interested only in using the property-based matching, you need to return false for the name-only check 
 */
@PublicAtsApi
public interface IndexMatcher {

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

    
    /**
     * Whether some table index should be treated as same or not
     * 
     * @param table table name
     * @param firstProperties first index properties
     * @param secondProperties second index properties
     * @return
     */
    @PublicAtsApi
    public boolean isSame( String table, Properties firstProperties, Properties secondProperties );
}
