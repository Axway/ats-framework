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

package com.axway.ats.common.dbaccess;

import com.axway.ats.common.PublicAtsApi;

/**
 * Constants used when initializing connection to a Cassandra database
 */
@PublicAtsApi
public class CassandraKeys extends DbKeys {

    /**
     * For some queries with a WHERE clause it is needed to allow the filtering option
     */
    @PublicAtsApi
    public static final String ALLOW_FILTERING = "ALLOW FILTERING";
}
